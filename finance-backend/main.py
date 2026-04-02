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

load_dotenv()

# 初始化 Firebase Admin
cred = credentials.Certificate("firebase-service-account.json")
firebase_admin.initialize_app(cred)

app = FastAPI()

client = OpenAI(
    api_key=os.getenv("DASHSCOPE_API_KEY"),
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)

# ─── 限速配置 ───────────────────────────────────────────────
FREE_PARSE_DAILY = 10
FREE_ANALYZE_MONTHLY = 2
PRO_PARSE_DAILY = None
PRO_ANALYZE_MONTHLY = None
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
                'Extract expense info from this sentence and return JSON.\n'
                'Sentence: "{text}"\n'
                'Format: {{"amount": number, "category": "category", "note": "note"}}\n'
                'Categories must be one of: food, transport, shopping, entertainment, healthcare, other\n'
                'Return JSON only, no other content.'
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
                '從這句話中提取記帳資訊，返回JSON格式。\n'
                '句子："{text}"\n'
                '返回格式：{{"amount": 金額數字, "category": "分類", "note": "備註"}}\n'
                '分類只能是：餐飲、交通、購物、娛樂、醫療、其他\n'
                '只返回JSON，不要任何其他內容。'
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
                '从这句话中提取记账信息，返回JSON格式。\n'
                '句子："{text}"\n'
                '返回格式：{{"amount": 金额数字, "category": "分类", "note": "备注"}}\n'
                '分类只能是：餐饮、交通、购物、娱乐、医疗、其他\n'
                '只返回JSON，不要任何其他内容。'
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
    if not authorization or not authorization.startswith("Bearer "):
        return {"uid": None, "role": "guest"}
    token = authorization.removeprefix("Bearer ").strip()
    try:
        decoded = auth.verify_id_token(token)
        role = decoded.get("role", "free")
        return {"uid": decoded["uid"], "role": role}
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid or expired token")


def check_rate_limit_parse(uid: str, role: str):
    now = datetime.now(timezone.utc)
    today = now.strftime("%Y-%m-%d")
    minute = now.strftime("%Y-%m-%dT%H:%M")

    m = minute_counts[uid]
    if m["minute"] != minute:
        m["minute"] = minute
        m["count"] = 0
    m["count"] += 1
    if m["count"] > PARSE_PER_MINUTE:
        raise HTTPException(status_code=429, detail="Too many requests, please slow down")

    if role == "pro":
        return

    p = parse_counts[uid]
    if p["date"] != today:
        p["date"] = today
        p["count"] = 0
    if p["count"] >= FREE_PARSE_DAILY:
        raise HTTPException(
            status_code=429,
            detail=f"Daily limit reached ({FREE_PARSE_DAILY} uses/day). Upgrade to Pro for unlimited access.",
        )
    p["count"] += 1


def check_rate_limit_analyze(uid: str, role: str):
    now = datetime.now(timezone.utc)
    month = now.strftime("%Y-%m")

    if role == "pro":
        return

    a = analyze_counts[uid]
    if a["month"] != month:
        a["month"] = month
        a["count"] = 0
    if a["count"] >= FREE_ANALYZE_MONTHLY:
        raise HTTPException(
            status_code=429,
            detail=f"Monthly limit reached ({FREE_ANALYZE_MONTHLY} analyses/month). Upgrade to Pro for unlimited access.",
        )
    a["count"] += 1


# ─── 接口 ────────────────────────────────────────────────────

@app.get("/")
def hello():
    return {"message": "后端运行正常"}


@app.get("/parse-expense")
def parse_expense(
    text: str,
    lang: str = "zh",
    authorization: str | None = Header(default=None)
):
    user = verify_token(authorization)
    if user["uid"] is None:
        raise HTTPException(status_code=401, detail="Login required to use AI features")

    if len(text) > MAX_PROMPT_LENGTH:
        raise HTTPException(
            status_code=400,
            detail=f"Input too long (max {MAX_PROMPT_LENGTH} characters)",
        )

    check_rate_limit_parse(user["uid"], user["role"])

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
    return json.loads(result.strip())


@app.post("/analyze-expenses")
def analyze_expenses(data: dict, authorization: str | None = Header(default=None)):
    user = verify_token(authorization)
    if user["uid"] is None:
        raise HTTPException(status_code=401, detail="Login required to use AI features")

    check_rate_limit_analyze(user["uid"], user["role"])

    expenses = data.get("expenses", [])
    month = data.get("month", "本月")
    lang = data.get("lang", "zh")  # 从请求体取语言

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
        model="glm-5",
        messages=[{"role": "user", "content": prompt}]
    )
    result = response.choices[0].message.content
    return {"analysis": result.strip() if result else lc["analyze_failed"]}


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
