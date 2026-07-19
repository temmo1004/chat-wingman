# API 合約（前後端共同參考）

後端負責人：bath。此合約敲定後，前端（Android / iOS 捷徑）可先用假資料
並行開發，不必等後端完成。**改這份要先講**，因為兩端都依賴它。

## 架構：手機端 OCR + DeepSeek

DeepSeek API 純文字、不吃圖，所以**讀截圖在 Android 端用 ML Kit OCR**，
後端只收 OCR 出的對話文字、交 DeepSeek 生成回覆。好處：圖片不離開手機、
只送文字、最省、資安最好。

- **保護金鑰**：DeepSeek key 只留後端，客戶端不碰
- **集中 prompt**：C 調的 system prompt 放後端
- **demo 模式**：斷網/API 掛時後端直接回寫死範本

## 端點

### `POST /api/wingman`

**請求**（JSON）：`{ "text": "<OCR 出的對話文字>", "demo": false }`

| 欄位 | 型別 | 必填 | 說明 |
|---|---|---|---|
| text | string | 是 | Android 端 OCR 出的對話文字；建議每行前綴 `對方:` / `我:`（靠左右氣泡判斷） |
| demo | bool | 否 | `true` 時回寫死範本，不打 DeepSeek |

**回應 200**：
```json
{
  "chat_death_index": 72,
  "context": "對方在敷衍，話題快斷了",
  "replies": [
    { "style": "認真", "text": "...", "why": "為什麼這樣回的一句軍師解說" },
    { "style": "幽默", "text": "...", "why": "..." },
    { "style": "曖昧", "text": "...", "why": "..." }
  ]
}
```

| 欄位 | 型別 | 說明 |
|---|---|---|
| chat_death_index | int 0-100 | 聊死指數，前端做成儀表板；越高越快聊死 |
| context | string | 一句話語境判讀 |
| replies | array(3) | 固定三個，順序：認真 / 幽默 / 曖昧 |
| replies[].style | string | `認真` \| `幽默` \| `曖昧` |
| replies[].text | string | 可直接送出的回覆內容 |
| replies[].why | string | 軍師解說（差異化重點，勿省略） |

**錯誤**：`{ "error": "..." }` + 4xx/5xx。客戶端收到錯誤時退回 demo 範本或提示重試。

## 約定

- 後端一律回上面的 JSON 結構；DeepSeek 的原始輸出由後端負責解析成此格式，
  客戶端不碰模型供應商的回應結構
- `replies` 永遠回 3 筆、順序固定，前端可直接對應三張卡片 / 捷徑三選項
- 逾時建議 30s；OCR 文字過長時先去除狀態列、導覽列等非對話雜訊

## 實作備註（後端自行決定技術棧）

- 既有 `line-codex` 是 Flask/Python，沿用最快；金鑰與 prompt 放環境變數
- 可掛在既有 kernel / api.hakkaren.uk 管線上
- system prompt 由 C 提供，後端保留一個 `demo` 範本常數
