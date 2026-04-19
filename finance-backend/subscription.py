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
