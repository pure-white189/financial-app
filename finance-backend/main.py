import json
import os
from collections import defaultdict
from datetime import date as _date
from datetime import datetime, timezone

import firebase_admin
import yfinance as yf
from dotenv import load_dotenv
from fastapi import FastAPI, Header, HTTPException, Security
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from firebase_admin import auth, credentials
from openai import OpenAI
from subscription import (
    RedeemError,
    add_token_quota,
    consume_token_quota,
    get_subscription_detail,
    get_usage_count,
    get_user_plan,
    increment_usage,
    init_db,
    redeem_code,
    set_usage_count,
)

load_dotenv()

# 初始化 Firebase Admin
cred = credentials.Certificate("firebase-service-account.json")
firebase_admin.initialize_app(cred)

# 初始化订阅数据库
init_db()

app = FastAPI(
    title="SmartSpend API",
    description="Personal Finance Assistant Backend — SmartSpend v2.1.1",
    version="2.1.1",
)

client = OpenAI(
    api_key=os.getenv("DASHSCOPE_API_KEY"),
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)

# Admin key，用于保护 /admin/* 接口，放在 .env 里
ADMIN_KEY = os.getenv("ADMIN_KEY", "")

# Swagger UI Bearer token 支持（点击 Authorize 后所有受保护接口自动带 header）
security = HTTPBearer(auto_error=False)

# ─── 限速配置 ───────────────────────────────────────────────
FREE_PARSE_DAILY = 10
FREE_ANALYZE_MONTHLY = 2
PARSE_PER_MINUTE = 10
MAX_PROMPT_LENGTH = 200

minute_counts = defaultdict(lambda: {"minute": "", "count": 0})


# ─── 语言配置 ────────────────────────────────────────────────


def get_lang_config(lang: str, currency: str = "HKD") -> dict:
    """根据 lang 返回对应语言的 prompt 片段和回退文字。"""
    currency_symbols = {"HKD": "HK$", "CNY": "¥", "USD": "US$"}
    symbol = currency_symbols.get(currency, "HK$")
    if lang == "en":
        return {
            "parse_instruction": (
                "Extract expense info from this sentence and return JSON.\n"
                'Sentence: "{text}"\n'
                "Today's date is {today}. Use this as the reference for relative date expressions like 'yesterday', 'this morning', 'last Monday'.\n"
                'Format: {{"amount": number, "category": "category_key", "note": "note", "date": "YYYY-MM-DD or null", "time": "HH:MM or null", "currency": "HKD/CNY/USD/null"}}\n'
                "category must be exactly one of: food, transport, shopping, entertainment, housing, education, medical, other\n"
                "currency: detect from the sentence. Return 'HKD' for HKD/港币/港元/HK$, 'CNY' for CNY/人民币/RMB/¥/rmb, 'USD' for USD/美元/美金/US$/dollar/dollars. If currency is not mentioned, return null.\n"
                "For date/time: extract only if explicitly mentioned in the sentence (e.g. 'yesterday', 'this morning', '3pm'). If not mentioned, return null.\n"
                "Return JSON only, no other content."
            ),
            "analyze_instruction": (
                "You are a friendly personal finance assistant. "
                "Based on the following expense data for {month}, write a brief friendly analysis (under 150 words):\n\n"
                "Total spending: {symbol}{total:.0f}\n"
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
            "currency_prefix": symbol,
        }
    elif lang == "zh-Hant":
        return {
            "parse_instruction": (
                "從這句話中提取記帳資訊，返回JSON格式。\n"
                '句子："{text}"\n'
                "今天的日期是{today}。請以此為基準推算「昨天」「今天早上」「上週」等相對時間。\n"
                '返回格式：{{"amount": 金額數字, "category": "category_key", "note": "備註", "date": "YYYY-MM-DD或null", "time": "HH:MM或null", "currency": "HKD/CNY/USD/null"}}\n'
                "category must be exactly one of: food, transport, shopping, entertainment, housing, education, medical, other\n"
                'currency：從句子中識別貨幣。HKD/港幣/港元/HK$返回"HKD"，CNY/人民幣/RMB/¥返回"CNY"，USD/美元/美金/US$/dollar返回"USD"，未提及返回null。\n'
                "date和time：只在句子中明確提到時間時才提取（如「昨天」「今天早上」「下午3點」）。未提及則返回null。\n"
                "只返回JSON，不要任何其他內容。"
            ),
            "analyze_instruction": (
                "你是一個友善的個人財務助手，請用繁體中文回覆。\n"
                "根據以下{month}消費數據，生成一段簡潔友好的分析報告（150字以內）：\n\n"
                "總支出：{symbol}{total:.0f}\n"
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
            "currency_prefix": symbol,
        }
    else:  # 默认简体中文
        return {
            "parse_instruction": (
                "从这句话中提取记账信息，返回JSON格式。\n"
                '句子："{text}"\n'
                "今天的日期是{today}。请以此为基准推算「昨天」「今天早上」「上周」等相对时间。\n"
                '返回格式：{{"amount": 金额数字, "category": "category_key", "note": "备注", "date": "YYYY-MM-DD或null", "time": "HH:MM或null", "currency": "HKD/CNY/USD/null"}}\n'
                "category must be exactly one of: food, transport, shopping, entertainment, housing, education, medical, other\n"
                'currency：从句子中识别货币。HKD/港币/港元/HK$返回"HKD"，CNY/人民币/RMB/¥返回"CNY"，USD/美元/美金/US$/dollar返回"USD"，未提及返回null。\n'
                'date和time：只在句子中明确提到时间时才提取（如"昨天"、"今天早上"、"下午3点"）。未提及则返回null。\n'
                "只返回JSON，不要任何其他内容。"
            ),
            "analyze_instruction": (
                "你是一个友善的个人财务助手。\n"
                "根据以下{month}消费数据，生成一段简洁友好的分析报告（150字以内）：\n\n"
                "总支出：{symbol}{total:.0f}\n"
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
            "currency_prefix": symbol,
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

    current_count = get_usage_count(uid, "parse", today)
    if current_count >= FREE_PARSE_DAILY:
        if consume_token_quota(uid, "parse"):
            return lambda: None
        raise HTTPException(
            status_code=429,
            detail=f"Daily limit reached ({FREE_PARSE_DAILY} uses/day). Upgrade to Pro for unlimited access.",
        )

    def commit():
        increment_usage(uid, "parse", today)

    return commit


def check_rate_limit_analyze(uid: str, role: str):
    """检查限速，通过则返回一个 commit 函数，AI 调用成功后再调用以递增计数。"""
    now = datetime.now(timezone.utc)
    month = now.strftime("%Y-%m")

    if role == "pro":
        return lambda: None  # Pro 无需计数

    current_count = get_usage_count(uid, "analyze", month)
    if current_count >= FREE_ANALYZE_MONTHLY:
        if consume_token_quota(uid, "analyze"):
            return lambda: None
        raise HTTPException(
            status_code=429,
            detail=f"Monthly limit reached ({FREE_ANALYZE_MONTHLY} analyses/month). Upgrade to Pro for unlimited access.",
        )

    def commit():
        increment_usage(uid, "analyze", month)

    return commit


# ─── 健康检查 ─────────────────────────────────────────────────


@app.get("/")
def hello():
    return {"message": "后端运行正常"}


# ─── 订阅接口 ─────────────────────────────────────────────────


@app.post("/redeem-code")
def redeem_code_endpoint(
    data: dict,
    credentials: HTTPAuthorizationCredentials | None = Security(security),
):
    """
    兑换激活码。
    Body: {"code": "SMART-XXXX-XXXX-XXXX"}
    返回: {"plan": "pro", "expires_at": "2025-07-..."}
    """
    authorization = f"Bearer {credentials.credentials}" if credentials else None
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
def redeem_tokens_endpoint(
    data: dict,
    credentials: HTTPAuthorizationCredentials | None = Security(security),
):
    authorization = f"Bearer {credentials.credentials}" if credentials else None
    user = verify_token(authorization)
    if user["uid"] is None:
        raise HTTPException(status_code=401, detail="Login required")

    quota_type = str(data.get("type", "")).strip().lower()
    if quota_type not in {"parse", "analyze"}:
        raise HTTPException(status_code=400, detail="type must be 'parse' or 'analyze'")

    add_token_quota(user["uid"], quota_type)
    return {"success": True}


@app.get("/subscription-status")
def subscription_status(
    credentials: HTTPAuthorizationCredentials | None = Security(security),
):
    """
    查询当前用户订阅状态。
    返回: {"plan": "free"|"pro", "expires_at": "ISO8601 或 null"}
    """
    authorization = f"Bearer {credentials.credentials}" if credentials else None
    user = verify_token(authorization)
    if user["uid"] is None:
        raise HTTPException(status_code=401, detail="Login required")

    return get_subscription_detail(user["uid"])


@app.get("/usage-status")
def usage_status(
    credentials: HTTPAuthorizationCredentials | None = Security(security),
):
    authorization = f"Bearer {credentials.credentials}" if credentials else None
    user = verify_token(authorization)
    if user["uid"] is None:
        raise HTTPException(status_code=401, detail="Login required")

    uid = user["uid"]
    plan = user["role"]

    now = datetime.now(timezone.utc)
    today = now.strftime("%Y-%m-%d")
    month = now.strftime("%Y-%m")

    parse_used = get_usage_count(uid, "parse", today)
    analyze_used = get_usage_count(uid, "analyze", month)

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


@app.post("/admin/reset-usage")
def admin_reset_usage(data: dict, x_admin_key: str | None = Header(default=None)):
    """
    手动设置某用户的使用计数，方便测试。
    Body: {"uid": "xxx", "parse_count": 9, "analyze_count": 2}
    """
    if not ADMIN_KEY or x_admin_key != ADMIN_KEY:
        raise HTTPException(status_code=403, detail="Invalid admin key")

    uid = data.get("uid", "")
    if not uid:
        raise HTTPException(status_code=400, detail="uid required")

    now = datetime.now(timezone.utc)
    today = now.strftime("%Y-%m-%d")
    month = now.strftime("%Y-%m")

    if "parse_count" in data:
        set_usage_count(uid, "parse", today, int(data["parse_count"]))

    if "analyze_count" in data:
        set_usage_count(uid, "analyze", month, int(data["analyze_count"]))

    return {
        "uid": uid,
        "parse": {"period": today, "count": get_usage_count(uid, "parse", today)},
        "analyze": {"period": month, "count": get_usage_count(uid, "analyze", month)},
    }


# ─── AI 接口 ──────────────────────────────────────────────────


@app.get("/parse-expense")
def parse_expense(
    text: str,
    lang: str = "zh",
    credentials: HTTPAuthorizationCredentials | None = Security(security),
):
    authorization = f"Bearer {credentials.credentials}" if credentials else None
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
    today_str = _date.today().strftime("%Y-%m-%d")
    prompt = lc["parse_instruction"].format(text=text, today=today_str)

    try:
        response = client.chat.completions.create(
            model="glm-5",
            messages=[{"role": "user", "content": prompt}],
        )
    except Exception as e:
        raise HTTPException(status_code=503, detail=f"AI service unavailable: {str(e)}")

    result = response.choices[0].message.content
    if not result or not result.strip():
        raise HTTPException(
            status_code=502, detail="AI returned an empty response, please try again"
        )

    result = result.strip()
    if result.startswith("```"):
        result = result.split("```")[1]
        if result.startswith("json"):
            result = result[4:]

    try:
        parsed = json.loads(result.strip())
    except Exception:
        raise HTTPException(
            status_code=502,
            detail="AI returned an unrecognized format, please try again",
        )

    commit_parse()  # 只有完整成功才计数
    return parsed


@app.post("/analyze-expenses")
def analyze_expenses(
    data: dict,
    credentials: HTTPAuthorizationCredentials | None = Security(security),
):
    authorization = f"Bearer {credentials.credentials}" if credentials else None
    user = verify_token(authorization)
    if user["uid"] is None:
        raise HTTPException(status_code=401, detail="Login required to use AI features")

    commit_analyze = check_rate_limit_analyze(user["uid"], user["role"])

    expenses = data.get("expenses", [])
    month = data.get("month", "本月")
    lang = data.get("lang", "zh")
    currency = data.get("currency", "HKD")

    lc = get_lang_config(lang, currency)

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
        symbol=lc["currency_prefix"],
    )

    response = client.chat.completions.create(
        model="glm-5", messages=[{"role": "user", "content": prompt}]
    )
    result = response.choices[0].message.content
    if result:
        commit_analyze()  # AI 成功返回后才计数

    # 计算推荐类型（基于类别占比，纯后端计算，不依赖 AI）
    rec_type = None
    rec_stat = None
    if category_totals and total > 0:
        food_ratio = category_totals.get("food", 0) / total
        shopping_ratio = category_totals.get("shopping", 0) / total
        entertainment_ratio = category_totals.get("entertainment", 0) / total
        if food_ratio > 0.35:
            rec_type = "food_heavy"
            rec_stat = f"{food_ratio:.0%}"
        elif shopping_ratio > 0.30:
            rec_type = "shopaholic"
            rec_stat = f"{shopping_ratio:.0%}"
        elif entertainment_ratio > 0.25:
            rec_type = "entertainment_heavy"
            rec_stat = f"{entertainment_ratio:.0%}"

    return {
        "analysis": result.strip() if result else lc["analyze_failed"],
        "recommendation_type": rec_type,
        "recommendation_stat": rec_stat,
    }


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


@app.get("/exchange-rate")
def get_exchange_rate(from_currency: str, to_currency: str):
    """
    Fetch live exchange rate using yfinance.
    Example: /exchange-rate?from_currency=CNY&to_currency=HKD
    Returns: {"from": "CNY", "to": "HKD", "rate": 1.081, "timestamp": "..."}
    """
    try:
        if from_currency == to_currency:
            return {
                "from": from_currency,
                "to": to_currency,
                "rate": 1.0,
                "timestamp": datetime.now(timezone.utc).isoformat(),
            }
        symbol = f"{from_currency}{to_currency}=X"
        ticker = yf.Ticker(symbol)
        rate = ticker.fast_info.last_price
        if rate is None:
            raise ValueError("No rate available")
        return {
            "from": from_currency,
            "to": to_currency,
            "rate": round(rate, 6),
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }
    except Exception as e:
        return {"error": str(e), "from": from_currency, "to": to_currency}


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


# ─── 个性化推荐接口 ─────────────────────────────────────────────────


@app.get("/recommendations")
def get_recommendations():
    """返回推荐内容库，无需登录，app 启动时拉取缓存"""
    with open("recommendations.json", "r", encoding="utf-8") as f:
        return json.load(f)
