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
FREE_PARSE_DAILY = 10  # 免费用户自然语言/天
FREE_ANALYZE_MONTHLY = 2  # 免费用户分析报告/月
PRO_PARSE_DAILY = None  # Pro 用户不限
PRO_ANALYZE_MONTHLY = None  # Pro 用户不限
PARSE_PER_MINUTE = 10  # 所有用户每分钟最多10次（防脚本）
MAX_PROMPT_LENGTH = 200  # 自然语言输入最大字符数

# 内存计数器（重启后重置，足够用于限速）
# 格式: { uid: { "date": "2026-04-01", "parse_count": 3 } }
parse_counts = defaultdict(lambda: {"date": "", "count": 0})
# 格式: { uid: { "month": "2026-04", "count": 1 } }
analyze_counts = defaultdict(lambda: {"month": "", "count": 0})
# 每分钟限速: { uid: { "minute": "2026-04-01T12:00", "count": 5 } }
minute_counts = defaultdict(lambda: {"minute": "", "count": 0})


# ─── 工具函数 ────────────────────────────────────────────────


def verify_token(authorization: str | None) -> dict:
    """验证 Firebase ID Token，返回解码后的 claims。未提供 token 返回 guest。"""
    if not authorization or not authorization.startswith("Bearer "):
        return {"uid": None, "role": "guest"}
    token = authorization.removeprefix("Bearer ").strip()
    try:
        decoded = auth.verify_id_token(token)
        role = decoded.get(
            "role", "free"
        )  # 默认 free，Pro 用户需后台手动设置 custom claim
        return {"uid": decoded["uid"], "role": role}
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid or expired token")


def check_rate_limit_parse(uid: str, role: str):
    """检查自然语言解析的限速。"""
    now = datetime.now(timezone.utc)
    today = now.strftime("%Y-%m-%d")
    minute = now.strftime("%Y-%m-%dT%H:%M")

    # 每分钟限速（所有用户）
    m = minute_counts[uid]
    if m["minute"] != minute:
        m["minute"] = minute
        m["count"] = 0
    m["count"] += 1
    if m["count"] > PARSE_PER_MINUTE:
        raise HTTPException(
            status_code=429, detail="Too many requests, please slow down"
        )

    # Pro 用户不限次数
    if role == "pro":
        return

    # 免费用户每日限速
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
    """检查月度分析报告的限速。"""
    now = datetime.now(timezone.utc)
    month = now.strftime("%Y-%m")

    # Pro 用户不限次数
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
def parse_expense(text: str, authorization: str | None = Header(default=None)):
    # 身份验证
    user = verify_token(authorization)
    if user["uid"] is None:
        raise HTTPException(status_code=401, detail="Login required to use AI features")

    # Prompt 长度限制
    if len(text) > MAX_PROMPT_LENGTH:
        raise HTTPException(
            status_code=400,
            detail=f"Input too long (max {MAX_PROMPT_LENGTH} characters)",
        )

    # 限速检查
    check_rate_limit_parse(user["uid"], user["role"])

    response = client.chat.completions.create(
        model="glm-5",
        messages=[
            {
                "role": "user",
                "content": f"""从这句话中提取记账信息，返回JSON格式。
句子："{text}"
返回格式：{{"amount": 金额数字, "category": "分类", "note": "备注"}}
分类只能是：餐饮、交通、购物、娱乐、医疗、其他
只返回JSON，不要任何其他内容。""",
            }
        ],
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
    # 身份验证
    user = verify_token(authorization)
    if user["uid"] is None:
        raise HTTPException(status_code=401, detail="Login required to use AI features")

    # 限速检查
    check_rate_limit_analyze(user["uid"], user["role"])

    expenses = data.get("expenses", [])
    month = data.get("month", "本月")

    if not expenses:
        return {"analysis": "本月暂无消费记录，快去记录你的第一笔消费吧！"}

    total = sum(e["amount"] for e in expenses)
    category_totals = {}
    for e in expenses:
        cat = e["category"]
        category_totals[cat] = category_totals.get(cat, 0) + e["amount"]

    sorted_cats = sorted(category_totals.items(), key=lambda x: x[1], reverse=True)
    category_summary = "、".join([f"{cat}¥{amt:.0f}" for cat, amt in sorted_cats[:5]])

    prompt = f"""你是一个友善的个人财务助手。
根据以下{month}消费数据，生成一段简洁友好的分析报告（150字以内）：

总支出：¥{total:.0f}
消费笔数：{len(expenses)}笔
主要分类：{category_summary}

要求：
1. 指出最大支出类别
2. 给出1-2条实用的省钱建议
3. 语气友好鼓励
4. 不要使用markdown格式
只返回分析文字，不要任何其他内容。"""

    response = client.chat.completions.create(
        model="glm-5", messages=[{"role": "user", "content": prompt}]
    )
    result = response.choices[0].message.content
    return {"analysis": result.strip() if result else "分析生成失败，请稍后重试"}


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
