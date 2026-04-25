"""
subscription.py — 激活码与订阅管理
数据库：finance-backend/subscriptions.db（与 main.py 同目录）
"""

import sqlite3
import random
import string
from datetime import datetime, timedelta, timezone
from contextlib import contextmanager

DB_PATH = "subscriptions.db"

# ─── 初始化 ──────────────────────────────────────────────────

def init_db():
    with _conn() as conn:
        conn.executescript("""
            CREATE TABLE IF NOT EXISTS activation_codes (
                code          TEXT PRIMARY KEY,
                max_uses      INTEGER NOT NULL DEFAULT 1,
                used_count    INTEGER NOT NULL DEFAULT 0,
                duration_days INTEGER NOT NULL DEFAULT 90,
                created_at    TEXT NOT NULL,
                note          TEXT
            );

            CREATE TABLE IF NOT EXISTS code_redemptions (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                code          TEXT NOT NULL,
                uid           TEXT NOT NULL,
                redeemed_at   TEXT NOT NULL,
                expires_at    TEXT NOT NULL,
                UNIQUE(code, uid)
            );

            CREATE TABLE IF NOT EXISTS user_subscriptions (
                uid           TEXT PRIMARY KEY,
                plan          TEXT NOT NULL DEFAULT 'free',
                expires_at    TEXT,
                updated_at    TEXT NOT NULL
            );

            CREATE TABLE IF NOT EXISTS token_quota (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                uid           TEXT NOT NULL,
                type          TEXT NOT NULL,
                granted_at    TEXT NOT NULL,
                expires_at    TEXT NOT NULL
            );

            CREATE TABLE IF NOT EXISTS token_balances (
                uid         TEXT PRIMARY KEY,
                balance     INTEGER NOT NULL DEFAULT 0,
                updated_at  TEXT NOT NULL
            );

            CREATE TABLE IF NOT EXISTS check_in_records (
                uid         TEXT NOT NULL,
                date        TEXT NOT NULL,
                streak      INTEGER NOT NULL DEFAULT 1,
                bonus       INTEGER NOT NULL DEFAULT 0,
                created_at  TEXT NOT NULL,
                PRIMARY KEY (uid, date)
            );

            CREATE TABLE IF NOT EXISTS achievements (
                uid             TEXT NOT NULL,
                achievement_id  TEXT NOT NULL,
                unlocked_at     TEXT NOT NULL,
                PRIMARY KEY (uid, achievement_id)
            );

            CREATE TABLE IF NOT EXISTS usage_limits (
                uid          TEXT NOT NULL,
                type         TEXT NOT NULL,
                period       TEXT NOT NULL,
                count        INTEGER NOT NULL DEFAULT 0,
                updated_at   TEXT NOT NULL,
                PRIMARY KEY (uid, type)
            );
        """)


def add_token_quota(uid: str, type: str):
    now = datetime.now(timezone.utc)
    now_str = now.isoformat()

    if type == "parse":
        expires_at = now.replace(hour=23, minute=59, second=59, microsecond=0)
    elif type == "analyze":
        month_start_next = (now.replace(day=1, hour=0, minute=0, second=0, microsecond=0) + timedelta(days=32)).replace(day=1)
        expires_at = month_start_next - timedelta(seconds=1)
    else:
        raise ValueError(f"Unsupported quota type: {type}")

    with _conn() as conn:
        conn.execute(
            "INSERT INTO token_quota (uid, type, granted_at, expires_at) VALUES (?, ?, ?, ?)",
            (uid, type, now_str, expires_at.isoformat())
        )


def consume_token_quota(uid: str, type: str) -> bool:
    now_str = datetime.now(timezone.utc).isoformat()

    with _conn() as conn:
        row = conn.execute(
            """
            SELECT id FROM token_quota
            WHERE uid = ? AND type = ? AND expires_at > ?
            ORDER BY expires_at ASC
            LIMIT 1
            """,
            (uid, type, now_str)
        ).fetchone()

        if row is None:
            return False

        conn.execute("DELETE FROM token_quota WHERE id = ?", (row["id"],))
        return True


@contextmanager
def _conn():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    try:
        yield conn
        conn.commit()
    finally:
        conn.close()


# ─── 激活码生成 ───────────────────────────────────────────────

# 排除易混淆字符：0/O、1/I/L
_CHARSET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

def _random_segment(n=4) -> str:
    return "".join(random.choices(_CHARSET, k=n))

def generate_code() -> str:
    return f"SMART-{_random_segment()}-{_random_segment()}-{_random_segment()}"

def create_codes(count: int, max_uses: int = 1, duration_days: int = 90, note: str = "") -> list[str]:
    """批量生成激活码并写入数据库，返回生成的码列表。"""
    now = _now_str()
    codes = []
    with _conn() as conn:
        for _ in range(count):
            for _ in range(10):  # 最多重试10次避免碰撞
                code = generate_code()
                try:
                    conn.execute(
                        "INSERT INTO activation_codes (code, max_uses, duration_days, created_at, note) "
                        "VALUES (?, ?, ?, ?, ?)",
                        (code, max_uses, duration_days, now, note)
                    )
                    codes.append(code)
                    break
                except sqlite3.IntegrityError:
                    continue  # 碰撞，重新生成
    return codes


# ─── 兑换逻辑 ────────────────────────────────────────────────

class RedeemError(Exception):
    pass

def redeem_code(code: str, uid: str) -> dict:
    """
    兑换激活码。成功返回 {"plan": "pro", "expires_at": "..."}。
    失败抛出 RedeemError。
    """
    code = code.strip().upper()

    with _conn() as conn:
        # 1. 查码是否存在
        row = conn.execute(
            "SELECT * FROM activation_codes WHERE code = ?", (code,)
        ).fetchone()
        if row is None:
            raise RedeemError("Invalid activation code.")

        # 2. 查是否已超用量
        if row["used_count"] >= row["max_uses"]:
            raise RedeemError("This code has already been used.")

        # 3. 查该 uid 是否已用过这个码
        dup = conn.execute(
            "SELECT 1 FROM code_redemptions WHERE code = ? AND uid = ?", (code, uid)
        ).fetchone()
        if dup:
            raise RedeemError("You have already used this code.")

        # 4. 计算到期时间
        # 若用户已有未过期的 Pro，则从现有到期时间叠加；否则从现在开始
        now = datetime.now(timezone.utc)
        existing = conn.execute(
            "SELECT expires_at FROM user_subscriptions WHERE uid = ?", (uid,)
        ).fetchone()

        base = now
        if existing and existing["expires_at"]:
            existing_exp = datetime.fromisoformat(existing["expires_at"])
            if existing_exp > now:
                base = existing_exp  # 叠加

        expires_at = base + timedelta(days=row["duration_days"])
        expires_str = expires_at.isoformat()
        now_str = now.isoformat()

        # 5. 写兑换记录
        conn.execute(
            "INSERT INTO code_redemptions (code, uid, redeemed_at, expires_at) VALUES (?, ?, ?, ?)",
            (code, uid, now_str, expires_str)
        )

        # 6. 更新 used_count
        conn.execute(
            "UPDATE activation_codes SET used_count = used_count + 1 WHERE code = ?", (code,)
        )

        # 7. 更新用户订阅状态
        conn.execute(
            """
            INSERT INTO user_subscriptions (uid, plan, expires_at, updated_at)
            VALUES (?, 'pro', ?, ?)
            ON CONFLICT(uid) DO UPDATE SET
                plan = 'pro',
                expires_at = excluded.expires_at,
                updated_at = excluded.updated_at
            """,
            (uid, expires_str, now_str)
        )

    return {"plan": "pro", "expires_at": expires_str}


# ─── 查询订阅状态 ─────────────────────────────────────────────

def get_user_plan(uid: str) -> str:
    """返回 'pro' 或 'free'，自动处理过期。"""
    with _conn() as conn:
        row = conn.execute(
            "SELECT plan, expires_at FROM user_subscriptions WHERE uid = ?", (uid,)
        ).fetchone()

        if row is None:
            return "free"

        if row["plan"] == "pro" and row["expires_at"]:
            exp = datetime.fromisoformat(row["expires_at"])
            if exp <= datetime.now(timezone.utc):
                # 已过期，降级
                now_str = _now_str()
                conn.execute(
                    "UPDATE user_subscriptions SET plan = 'free', expires_at = NULL, updated_at = ? WHERE uid = ?",
                    (now_str, uid)
                )
                return "free"

        return row["plan"]


def get_subscription_detail(uid: str) -> dict:
    """返回完整订阅信息供 /subscription-status 接口使用。"""
    plan = get_user_plan(uid)  # 顺便处理过期
    with _conn() as conn:
        row = conn.execute(
            "SELECT plan, expires_at FROM user_subscriptions WHERE uid = ?", (uid,)
        ).fetchone()
        if row is None or row["plan"] == "free":
            return {"plan": "free", "expires_at": None}
        return {"plan": row["plan"], "expires_at": row["expires_at"]}


# ─── 工具 ────────────────────────────────────────────────────

def _now_str() -> str:
    return datetime.now(timezone.utc).isoformat()


# ─── 限速计数持久化 ───────────────────────────────────────────

def get_usage_count(uid: str, usage_type: str, period: str) -> int:
    """获取用户当前周期的使用计数，周期不匹配则视为0。"""
    with _conn() as conn:
        row = conn.execute(
            "SELECT count, period FROM usage_limits WHERE uid = ? AND type = ?",
            (uid, usage_type)
        ).fetchone()
        if row is None or row["period"] != period:
            return 0
        return row["count"]


def increment_usage(uid: str, usage_type: str, period: str):
    """将用户当前周期的使用计数加1，周期变化时自动重置。"""
    now_str = _now_str()
    with _conn() as conn:
        existing = conn.execute(
            "SELECT count, period FROM usage_limits WHERE uid = ? AND type = ?",
            (uid, usage_type)
        ).fetchone()
        if existing is None or existing["period"] != period:
            conn.execute(
                """
                INSERT INTO usage_limits (uid, type, period, count, updated_at)
                VALUES (?, ?, ?, 1, ?)
                ON CONFLICT(uid, type) DO UPDATE SET
                    period = excluded.period,
                    count = 1,
                    updated_at = excluded.updated_at
                """,
                (uid, usage_type, period, now_str)
            )
        else:
            conn.execute(
                "UPDATE usage_limits SET count = count + 1, updated_at = ? WHERE uid = ? AND type = ?",
                (now_str, uid, usage_type)
            )


def set_usage_count(uid: str, usage_type: str, period: str, count: int):
    """强制设置用户计数，供管理员接口使用。"""
    now_str = _now_str()
    with _conn() as conn:
        conn.execute(
            """
            INSERT INTO usage_limits (uid, type, period, count, updated_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(uid, type) DO UPDATE SET
                period = excluded.period,
                count = excluded.count,
                updated_at = excluded.updated_at
            """,
            (uid, usage_type, period, count, now_str)
        )


# ─── 代币余额管理 ─────────────────────────────────────────────

def get_token_balance(uid: str) -> int:
    with _conn() as conn:
        row = conn.execute(
            "SELECT balance FROM token_balances WHERE uid = ?", (uid,)
        ).fetchone()
        return row["balance"] if row else 0


def init_user_if_needed(uid: str):
    """新用户首次登录时初始化后端记录，已存在则跳过。"""
    now_str = _now_str()
    with _conn() as conn:
        conn.execute(
            """
            INSERT OR IGNORE INTO token_balances (uid, balance, updated_at)
            VALUES (?, 0, ?)
            """,
            (uid, now_str)
        )
        conn.execute(
            """
            INSERT OR IGNORE INTO user_subscriptions (uid, plan, expires_at, updated_at)
            VALUES (?, 'free', NULL, ?)
            """,
            (uid, now_str)
        )


def add_tokens(uid: str, amount: int) -> int:
    """增加代币，返回新余额。"""
    now_str = _now_str()
    with _conn() as conn:
        conn.execute(
            """
            INSERT INTO token_balances (uid, balance, updated_at)
            VALUES (?, ?, ?)
            ON CONFLICT(uid) DO UPDATE SET
                balance = balance + excluded.balance,
                updated_at = excluded.updated_at
            """,
            (uid, amount, now_str)
        )
        row = conn.execute(
            "SELECT balance FROM token_balances WHERE uid = ?", (uid,)
        ).fetchone()
        return row["balance"]


def deduct_tokens(uid: str, amount: int) -> tuple[bool, int]:
    """扣减代币，余额不足返回 (False, current_balance)，成功返回 (True, new_balance)。"""
    now_str = _now_str()
    with _conn() as conn:
        row = conn.execute(
            "SELECT balance FROM token_balances WHERE uid = ?", (uid,)
        ).fetchone()
        current = row["balance"] if row else 0
        if current < amount:
            return False, current
        conn.execute(
            "UPDATE token_balances SET balance = balance - ?, updated_at = ? WHERE uid = ?",
            (amount, now_str, uid)
        )
        return True, current - amount


# ─── 签到逻辑 ─────────────────────────────────────────────────

STREAK_MILESTONES = {7: 5, 30: 15, 90: 30, 365: 100, 730: 200}


def process_check_in(uid: str) -> dict:
    """
    处理签到。已签到返回 already_checked_in=True。
    成功返回 {already_checked_in, streak, base_tokens, bonus_tokens, new_balance}。
    """
    now = datetime.now(timezone.utc)
    today = now.strftime("%Y-%m-%d")
    from datetime import timedelta
    yesterday = (now - timedelta(days=1)).strftime("%Y-%m-%d")
    now_str = now.isoformat()

    with _conn() as conn:
        # 今天已签到
        existing = conn.execute(
            "SELECT streak FROM check_in_records WHERE uid = ? AND date = ?",
            (uid, today)
        ).fetchone()
        if existing:
            balance = get_token_balance(uid)
            return {
                "already_checked_in": True,
                "streak": existing["streak"],
                "base_tokens": 0,
                "bonus_tokens": 0,
                "new_balance": balance
            }

        # 计算连续天数
        yesterday_row = conn.execute(
            "SELECT streak FROM check_in_records WHERE uid = ? AND date = ?",
            (uid, yesterday)
        ).fetchone()
        streak = (yesterday_row["streak"] + 1) if yesterday_row else 1

        # 计算奖励
        base_tokens = 1
        bonus_tokens = STREAK_MILESTONES.get(streak, 0)
        total = base_tokens + bonus_tokens

        # 写签到记录
        conn.execute(
            "INSERT INTO check_in_records (uid, date, streak, bonus, created_at) VALUES (?, ?, ?, ?, ?)",
            (uid, today, streak, bonus_tokens, now_str)
        )

    # 发放代币（在 _conn() 外调用避免嵌套连接）
    new_balance = add_tokens(uid, total)

    return {
        "already_checked_in": False,
        "streak": streak,
        "base_tokens": base_tokens,
        "bonus_tokens": bonus_tokens,
        "new_balance": new_balance
    }


# ─── 成就逻辑 ─────────────────────────────────────────────────

ACHIEVEMENT_REWARDS = {
    "first_expense": 5,
    "first_budget": 5,
    "first_saving_goal": 5,
    "first_loan": 5,
    "first_sync": 5,
    "first_ai_parse": 5,
    "first_ai_analyze": 10,
    "first_stock": 5,
    "first_income": 5,
    "streak_7": 10,
    "streak_30": 20,
    "streak_90": 30,
    "streak_365": 100,
    "streak_730": 200,
    "budget_1m": 10,
    "budget_3m": 20,
    "budget_6m": 40,
    "budget_9m": 60,
    "budget_12m": 80,
    "budget_18m": 120,
    "budget_24m": 200,
}


def process_achievement(uid: str, achievement_id: str) -> dict:
    """
    解锁成就并发放代币。已解锁返回 already_unlocked=True。
    成功返回 {already_unlocked, achievement_id, tokens_earned, new_balance}。
    """
    now_str = _now_str()
    reward = ACHIEVEMENT_REWARDS.get(achievement_id, 0)

    with _conn() as conn:
        existing = conn.execute(
            "SELECT 1 FROM achievements WHERE uid = ? AND achievement_id = ?",
            (uid, achievement_id)
        ).fetchone()
        if existing:
            balance = get_token_balance(uid)
            return {
                "already_unlocked": True,
                "achievement_id": achievement_id,
                "tokens_earned": 0,
                "new_balance": balance
            }

        conn.execute(
            "INSERT INTO achievements (uid, achievement_id, unlocked_at) VALUES (?, ?, ?)",
            (uid, achievement_id, now_str)
        )

    new_balance = add_tokens(uid, reward) if reward > 0 else get_token_balance(uid)

    return {
        "already_unlocked": False,
        "achievement_id": achievement_id,
        "tokens_earned": reward,
        "new_balance": new_balance
    }


