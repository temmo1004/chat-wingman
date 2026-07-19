# 後端 — 聊天軍師 API

實作 [`docs/api.md`](../docs/api.md) 的 `POST /api/wingman`。

## 跑起來

```bash
cd backend
python3 -m venv venv && source venv/bin/activate
pip install -r requirements.txt
cp .env.example .env      # 填入 DEEPSEEK_API_KEY
export $(grep -v '^#' .env | xargs)   # 或用 python-dotenv
python app.py             # http://localhost:8000
```

## 測試

```bash
# demo 模式（不打 DeepSeek，回寫死範本）
curl -s localhost:8000/api/wingman -H 'content-type: application/json' \
  -d '{"demo":true}' | python3 -m json.tool

# 真的分析手機端 OCR 後的對話文字
curl -s localhost:8000/api/wingman -H 'content-type: application/json' \
  -d '{"text":"對方: 你下班了嗎？\n我: 剛下班"}' | python3 -m json.tool

# 健康檢查
curl -s localhost:8000/health
```

## 結構

- `app.py` — Flask 路由、輸入驗證、DeepSeek 呼叫、輸出對齊合約
- `prompt.py` — system prompt + demo 範本（C 調這裡）
- 金鑰/設定走環境變數，見 `.env.example`

## 部署

掛在既有 kernel / api.hakkaren.uk 管線上；客戶端把 base URL 指到公開位址即可。
Zeabur 部署時記得設 `DEEPSEEK_API_KEY` 環境變數。
