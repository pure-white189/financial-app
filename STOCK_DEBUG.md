# 股票追踪 - A股调试记录

## 问题描述
- 港股(HK)、美股(US)：价格获取正常
- A股沪市(SS)、深市(SZ)：app端获取失败，显示 ¥0.00

## 已确认正常的部分
- 后端接口正常：
  - http://127.0.0.1:8000/stock-price?symbol=600519.SS → 返回 1408.07
  - http://127.0.0.1:8000/stock-price?symbol=000858.SZ → 待测试
- 防火墙已开放8000端口
- Android 网络权限已配置（INTERNET + ACCESS_NETWORK_STATE）

## 待排查
1. app发出的symbol格式是否正确
   - 用户输入 000858，选"深" → 应拼接为 000858.SZ
   - Logcat AiParser tag 查看实际发出的symbol
2. /stock-prices 批量接口是否支持SZ后缀
   - 在浏览器测试：http://127.0.0.1:8000/stock-prices?symbols=000858.SZ
3. 旧数据market字段可能还是"CN"而不是"SZ"
   - 历史添加的股票market存的是CN，拼接逻辑走了旧分支

## 最可能的原因
历史数据的 stock.market = "CN"，
新代码的 symbol 拼接只处理了 SS/SZ，
CN 走了兼容分支但可能拼接有误

## 解决方向
在新对话里检查 AiExpenseParser.kt 里
fetchStockPrices 发出请求前的 Logcat 日志，
确认实际 symbol 字符串是什么
