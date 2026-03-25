import json
import os

import yfinance as yf
from dotenv import load_dotenv
from fastapi import FastAPI
from openai import OpenAI

load_dotenv()  # 读取.env文件

app = FastAPI()

client = OpenAI(
    api_key=os.getenv("DASHSCOPE_API_KEY"),
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)


@app.get("/")
def hello():
    return {"message": "后端运行正常"}


@app.get("/parse-expense")
def parse_expense(text: str):
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
    # 清理可能的 markdown 代码块
    result = result.strip()
    if result.startswith("```"):
        result = result.split("```")[1]
        if result.startswith("json"):
            result = result[4:]
    return json.loads(result.strip())


# 自然语言输入


@app.post("/analyze-expenses")
def analyze_expenses(data: dict):
    expenses = data.get("expenses", [])
    month = data.get("month", "本月")

    if not expenses:
        return {"analysis": "本月暂无消费记录，快去记录你的第一笔消费吧！"}

    # 构建消费摘要
    total = sum(e["amount"] for e in expenses)
    category_totals = {}
    for e in expenses:
        cat = e["category"]
        category_totals[cat] = category_totals.get(cat, 0) + e["amount"]

    # 排序
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


# AI分析报告


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
    # symbols 是逗号分隔的字符串，如 "0700.HK,AAPL,600519.SS"
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


# 股市
