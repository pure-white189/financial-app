import json
import os
from collections import defaultdict
from datetime import datetime, timezone

import firebase_admin
import yfinance as yf
from dotenv import load_dotenv
from fastapi import FastAPI, Header, HTTPException
from firebase_admin import auth, credentials
from openai import OpenAI
from subscription import (
    RedeemError,
    add_token_quota,
    consume_token_quota,
    get_subscription_detail,
    get_user_plan,
    init_db,
    redeem_code,
)

load_dotenv()

# 初始化 Firebase Admin
cred = credentials.Certificate("firebase-service-account.json")
firebase_admin.initialize_app(cred)

# 初始化订阅数据库
init_db()

app = FastAPI()

client = OpenAI(
    api_key=os.getenv("DASHSCOPE_API_KEY"),
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)

# Admin key，用于保护 /admin/* 接口，放在 .env 里
ADMIN_KEY = os.getenv("ADMIN_KEY", "")

# ─── 限速配置 ───────────────────────────────────────────────
FREE_PARSE_DAILY = 10
FREE_ANALYZE_MONTHLY = 2
PARSE_PER_MINUTE = 10
MAX_PROMPT_LENGTH = 200

parse_counts = defaultdict(lambda: {"date": "", "count": 0})
analyze_counts = defaultdict(lambda: {"month": "", "count": 0})
minute_counts = defaultdict(lambda: {"minute": "", "count": 0})


# ─── 语言配置 ────────────────────────────────────────────────


def get_lang_config(lang: str) -> dict:
    """根据 lang 返回对应语言的 prompt 片段和回退文字。"""
    if lang == "en":
        return {
            "parse_instruction": (
                "Extract expense info from this sentence and return JSON.\n"
                'Sentence: "{text}"\n'
                'Format: {{"amount": number, "category": "category", "note": "note"}}\n'
                "Categories must be one of: food, transport, shopping, entertainment, healthcare, other\n"
                "Return JSON only, no other content."
            ),
            "analyze_instruction": (
                "You are a friendly personal finance assistant. "
                "Based on the following expense data for {month}, write a brief friendly analysis (under 150 words):\n\n"
                "Total spending: ${total:.0f}\n"
                "Number of transactions: {count}\n"
                "Top categories: {category_summary}\n\n"
                "Requirements:\n"
                "1. Point out the largest spending category\n"
                "2. Give 1-2 practical money-saving tips\n"
                "3. Keep the tone friendly and encouraging\n"
                "4. No markdown formatting\n"
                "Return analysis text only."
            ),
            "no_expense_reply": "No expenses recorded this month. Start tracking your first transaction!",
            "analyze_failed": "Analysis failed, please try again later.",
            "category_separator": ", ",
            "currency_prefix": "$",
        }
    elif lang == "zh-Hant":
        return {
            "parse_instruction": (
                "從這句話中提取記帳資訊，返回JSON格式。\n"
                '句子："{text}"\n'
                '返回格式：{{"amount": 金額數字, "category": "分類", "note": "備註"}}\n'
                "分類只能是：餐飲、交通、購物、娛樂、醫療、其他\n"
                "只返回JSON，不要任何其他內容。"
            ),
            "analyze_instruction": (
                "你是一個友善的個人財務助手，請用繁體中文回覆。\n"
                "根據以下{month}消費數據，生成一段簡潔友好的分析報告（150字以內）：\n\n"
                "總支出：¥{total:.0f}\n"
                "消費筆數：{count}筆\n"
                "主要分類：{category_summary}\n\n"
                "要求：\n"
                "1. 指出最大支出類別\n"
                "2. 給出1-2條實用的省錢建議\n"
                "3. 語氣友好鼓勵\n"
                "4. 不要使用markdown格式\n"
                "只返回分析文字，不要任何其他內容。"
            ),
            "no_expense_reply": "本月暫無消費記錄，快去記錄你的第一筆消費吧！",
            "analyze_failed": "分析生成失敗，請稍後重試",
            "category_separator": "、",
            "currency_prefix": "¥",
        }
    else:  # 默认简体中文
        return {
            "parse_instruction": (
                "从这句话中提取记账信息，返回JSON格式。\n"
                '句子："{text}"\n'
                '返回格式：{{"amount": 金额数字, "category": "分类", "note": "备注"}}\n'
                "分类只能是：餐饮、交通、购物、娱乐、医疗、其他\n"
                "只返回JSON，不要任何其他内容。"
            ),
            "analyze_instruction": (
                "你是一个友善的个人财务助手。\n"
                "根据以下{month}消费数据，生成一段简洁友好的分析报告（150字以内）：\n\n"
                "总支出：¥{total:.0f}\n"
                "消费笔数：{count}笔\n"
                "主要分类：{category_summary}\n\n"
                "要求：\n"
                "1. 指出最大支出类别\n"
                "2. 给出1-2条实用的省钱建议\n"
                "3. 语气友好鼓励\n"
                "4. 不要使用markdown格式\n"
                "只返回分析文字，不要任何其他内容。"
            ),
            "no_expense_reply": "本月暂无消费记录，快去记录你的第一笔消费吧！",
            "analyze_failed": "分析生成失败，请稍后重试",
            "category_separator": "、",
            "currency_prefix": "¥",
        }


# ─── 工具函数 ────────────────────────────────────────────────


def verify_token(authorization: str | None) -> dict:
    """验证 Firebase ID Token，返回 {uid, role}。role 从订阅数据库查询。"""
    if not authorization or not authorization.startswith("Bearer "):
        return {"uid": None, "role": "guest"}
    token = authorization.removeprefix("Bearer ").strip()
    try:
        decoded = auth.verify_id_token(token)
        uid = decoded["uid"]
        role = get_user_plan(uid)  # 从订阅数据库查，而不是 custom claims
        return {"uid": uid, "role": role}
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid or expired token")


def check_rate_limit_parse(uid: str, role: str):
    """检查限速，通过则返回一个 commit 函数，AI 调用成功后再调用以递增计数。"""
    now = datetime.now(timezone.utc)
    today = now.strftime("%Y-%m-%d")
    minute = now.strftime("%Y-%m-%dT%H:%M")

    m = minute_counts[uid]
    if m["minute"] != minute:
        m["minute"] = minute
        m["count"] = 0
    m["count"] += 1
    if m["count"] > PARSE_PER_MINUTE:
        raise HTTPException(
            status_code=429, detail="Too many requests, please slow down"
        )

    if role == "pro":
        return lambda: None  # Pro 无需计数

    p = parse_counts[uid]
    if p["date"] != today:
        p["date"] = today
        p["count"] = 0
    if p["count"] >= FREE_PARSE_DAILY:
        if consume_token_quota(uid, "parse"):
            return lambda: None
        raise HTTPException(
            status_code=429,
            detail=f"Daily limit reached ({FREE_PARSE_DAILY} uses/day). Upgrade to Pro for unlimited access.",
        )

    def commit():
        if p["date"] != today:
            p["date"] = today
            p["count"] = 0
        p["count"] += 1

    return commit


def check_rate_limit_analyze(uid: str, role: str):
    """检查限速，通过则返回一个 commit 函数，AI 调用成功后再调用以递增计数。"""
    now = datetime.now(timezone.utc)
    month = now.strftime("%Y-%m")

    if role == "pro":
        return lambda: None  # Pro 无需计数

    a = analyze_counts[uid]
    if a["month"] != month:
        a["month"] = month
        a["count"] = 0
    if a["count"] >= FREE_ANALYZE_MONTHLY:
        if consume_token_quota(uid, "analyze"):
            return lambda: None
        raise HTTPException(
            status_code=429,
            detail=f"Monthly limit reached ({FREE_ANALYZE_MONTHLY} analyses/month). Upgrade to Pro for unlimited access.",
        )

    def commit():
        if a["month"] != month:
            a["month"] = month
            a["count"] = 0
        a["count"] += 1

    return commit


# ─── 健康检查 ─────────────────────────────────────────────────


@app.get("/")
def hello():
    return {"message": "后端运行正常"}


# ─── 订阅接口 ─────────────────────────────────────────────────


@app.post("/redeem-code")
def redeem_code_endpoint(data: dict, authorization: str | None = Header(default=None)):
    """
    兑换激活码。
    Body: {"code": "SMART-XXXX-XXXX-XXXX"}
    返回: {"plan": "pro", "expires_at": "2025-07-..."}
    """
    user = verify_token(authorization)
    if user["uid"] is None:
        raise HTTPException(status_code=401, detail="Login required")

    code = data.get("code", "").strip()
    if not code:
        raise HTTPException(status_code=400, detail="Activation code is required")

    try:
        result = redeem_code(code, user["uid"])
        return result
    except RedeemError as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.post("/redeem-tokens")
def redeem_tokens_endpoint(data: dict, authorization: str | None = Header(default=None)):
    user = verify_token(authorization)
    if user["uid"] is None:
        raise HTTPException(status_code=401, detail="Login required")

    quota_type = str(data.get("type", "")).strip().lower()
    if quota_type not in {"parse", "analyze"}:
        raise HTTPException(status_code=400, detail="type must be 'parse' or 'analyze'")

    add_token_quota(user["uid"], quota_type)
    return {"success": True}


@app.get("/subscription-status")
def subscription_status(authorization: str | None = Header(default=None)):
    """
    查询当前用户订阅状态。
    返回: {"plan": "free"|"pro", "expires_at": "ISO8601 或 null"}
    """
    user = verify_token(authorization)
    if user["uid"] is None:
        raise HTTPException(status_code=401, detail="Login required")

    return get_subscription_detail(user["uid"])


@app.get("/usage-status")
def usage_status(authorization: str | None = Header(default=None)):
    user = verify_token(authorization)
    if user["uid"] is None:
        raise HTTPException(status_code=401, detail="Login required")

    uid = user["uid"]
    plan = user["role"]

    now = datetime.now(timezone.utc)
    today = now.strftime("%Y-%m-%d")
    month = now.strftime("%Y-%m")

    p = parse_counts[uid]
    parse_used = p["count"] if p["date"] == today else 0

    a = analyze_counts[uid]
    analyze_used = a["count"] if a["month"] == month else 0

    return {
        "plan": plan,
        "parse": {
            "used": parse_used,
            "limit": None if plan == "pro" else FREE_PARSE_DAILY,
        },
        "analyze": {
            "used": analyze_used,
            "limit": None if plan == "pro" else FREE_ANALYZE_MONTHLY,
        },
    }


# ─── 管理员接口 ───────────────────────────────────────────────


@app.post("/admin/generate-codes")
def admin_generate_codes(data: dict, x_admin_key: str | None = Header(default=None)):
    """
    生成激活码（需要 X-Admin-Key 请求头）。
    Body: {"count": 5, "max_uses": 1, "duration_days": 90, "note": "batch1"}
    返回: {"codes": [...]}
    """
    if not ADMIN_KEY or x_admin_key != ADMIN_KEY:
        raise HTTPException(status_code=403, detail="Invalid admin key")

    from subscription import create_codes

    count = int(data.get("count", 1))
    max_uses = int(data.get("max_uses", 1))
    duration = int(data.get("duration_days", 90))
    note = str(data.get("note", ""))

    if count < 1 or count > 100:
        raise HTTPException(status_code=400, detail="count must be between 1 and 100")

    codes = create_codes(
        count=count, max_uses=max_uses, duration_days=duration, note=note
    )
    return {"codes": codes}


# ─── AI 接口 ──────────────────────────────────────────────────


@app.get("/parse-expense")
def parse_expense(
    text: str, lang: str = "zh", authorization: str | None = Header(default=None)
):
    user = verify_token(authorization)
    if user["uid"] is None:
        raise HTTPException(status_code=401, detail="Login required to use AI features")

    if len(text) > MAX_PROMPT_LENGTH:
        raise HTTPException(
            status_code=400,
            detail=f"Input too long (max {MAX_PROMPT_LENGTH} characters)",
        )

    commit_parse = check_rate_limit_parse(user["uid"], user["role"])

    lc = get_lang_config(lang)
    prompt = lc["parse_instruction"].format(text=text)

    response = client.chat.completions.create(
        model="glm-5",
        messages=[{"role": "user", "content": prompt}],
    )
    result = response.choices[0].message.content
    if not result or not result.strip():
        return {"amount": 0, "category": "其他", "note": text}
    result = result.strip()
    if result.startswith("```"):
        result = result.split("```")[1]
        if result.startswith("json"):
            result = result[4:]
    parsed = json.loads(result.strip())
    commit_parse()  # AI 成功返回后才计数
    return parsed


@app.post("/analyze-expenses")
def analyze_expenses(data: dict, authorization: str | None = Header(default=None)):
    user = verify_token(authorization)
    if user["uid"] is None:
        raise HTTPException(status_code=401, detail="Login required to use AI features")

    commit_analyze = check_rate_limit_analyze(user["uid"], user["role"])

    expenses = data.get("expenses", [])
    month = data.get("month", "本月")
    lang = data.get("lang", "zh")

    lc = get_lang_config(lang)

    if not expenses:
        return {"analysis": lc["no_expense_reply"]}

    total = sum(e["amount"] for e in expenses)
    category_totals = {}
    for e in expenses:
        cat = e["category"]
        category_totals[cat] = category_totals.get(cat, 0) + e["amount"]

    sorted_cats = sorted(category_totals.items(), key=lambda x: x[1], reverse=True)
    sep = lc["category_separator"]
    prefix = lc["currency_prefix"]
    category_summary = sep.join(
        [f"{cat}{prefix}{amt:.0f}" for cat, amt in sorted_cats[:5]]
    )

    prompt = lc["analyze_instruction"].format(
        month=month,
        total=total,
        count=len(expenses),
        category_summary=category_summary,
    )

    response = client.chat.completions.create(
        model="glm-5", messages=[{"role": "user", "content": prompt}]
    )
    result = response.choices[0].message.content
    if result:
        commit_analyze()  # AI 成功返回后才计数
    return {"analysis": result.strip() if result else lc["analyze_failed"]}


# ─── 股票接口 ─────────────────────────────────────────────────


@app.get("/stock-price")
def get_stock_price(symbol: str):
    try:
        ticker = yf.Ticker(symbol)
        info = ticker.fast_info
        current_price = info.last_price
        if current_price is None:
            return {"error": "无法获取价格", "symbol": symbol}
        return {
            "symbol": symbol,
            "price": round(current_price, 2),
            "currency": info.currency or "HKD",
        }
    except Exception as e:
        return {"error": str(e), "symbol": symbol}


@app.get("/stock-prices")
def get_stock_prices(symbols: str):
    symbol_list = [s.strip() for s in symbols.split(",")]
    results = {}
    for symbol in symbol_list:
        try:
            ticker = yf.Ticker(symbol)
            info = ticker.fast_info
            current_price = info.last_price
            results[symbol] = {
                "price": round(current_price, 2) if current_price else None,
                "currency": info.currency or "HKD",
            }
        except Exception as e:
            results[symbol] = {"error": str(e)}
    return results
