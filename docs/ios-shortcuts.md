# iOS 捷徑路線：Live Text OCR + 後端（2026-07-19）

> **現行方案**：iPhone 先在本機將聊天截圖轉成文字，再只把 OCR 文字送給
> `/api/wingman`。截圖不離開手機，模型金鑰不進捷徑。

## 結論先講

iOS 沒有 Android 的跨 app 浮動視窗 API，但可以用「敲擊背面」執行 Apple 捷徑，
完成這條路徑：

```text
擷取螢幕畫面
→ 從影像擷取文字（Apple Live Text，手機端 OCR）
→ 取得 URL 內容（POST + JSON {text, demo}）
→ 後端 DeepSeek 產生三種回覆
→ 從選單選擇
→ 拷貝到剪貼簿
```

完整手建步驟見 [iOS 捷徑建構指南](../ios/wingman-shortcut-build.md)。

## Apple 官方依據

- Apple 在 iOS 15.4 的捷徑更新說明中指出，「Extract Text from Image」支援
  Live Text 所支援的語言。
  [Apple Support：What’s new in Shortcuts in iOS 15.4 and macOS 12.3](https://support.apple.com/en-us/106430)
- Apple 的 Shortcuts 使用手冊說明，「Get Contents of URL」改為 POST、PUT 或
  PATCH 後會顯示 Request Body，並可選 JSON、Form 或 File；我們選 JSON。
  [Apple Support：Request your first API in Shortcuts](https://support.apple.com/en-au/guide/shortcuts/apd58d46713f/ios)
- Live Text 需在「設定 → 一般 → 語言與地區」開啟，且功能不保證在所有
  地區與語言提供。
  [Apple Support：Use Live Text to interact with content in a photo or video](https://support.apple.com/en-asia/guide/iphone/iph37fdd714b/ios)

## 觸發方式

Take Screenshot 擷取的是捷徑觸發當下的畫面，因此觸發界面不應疊在聊天畫面上：

| 觸發方式 | 建議 | 備註 |
|---|---|---|
| **敲擊背面 (Back Tap)** | 主方案 | 免畫面按鈕；如有 Show Banner 選項請關閉 |
| 動作按鈕 | 可行備選 | 僅限有此硬體的機型 |
| AssistiveTouch | 不建議 | 畫面按鈕可能被截進圖片 |
| Siri | 不建議 | Siri 介面可能取代聊天畫面 |

Take Screenshot 的輸出可以直接交給下一個動作；這條流程不需要新增
「儲存到相片」動作。

## 請求合約

捷徑對 `[BASE]/api/wingman` 發出 `POST application/json`：

```json
{
  "text": "從聊天截圖辨識出的文字",
  "demo": false
}
```

- `text` 來自「從影像擷取文字」的輸出。
- `demo=true` 時後端回傳固定範本，不呼叫 DeepSeek；仍需要能連上後端。
- 捷徑不應包含 `image`、模型商的 `Authorization` header 或 API key。
- 後端固定回傳 `chat_death_index`、`context` 與三筆 `replies`，詳見
  [API 合約](api.md)。

## 隱私與金鑰邊界

```text
iPhone：截圖 → Live Text OCR → 只輸出文字
                                  │
                                  └─ HTTPS → 我們的後端 → DeepSeek
                                                    金鑰只在這裡
```

這個邊界與 Android 新架構一致：影像留在裝置上，網路上只傳 OCR 文字。
若分享捷徑，只對 `BASE` 設定匯入問題；不存在要求使用者填模型金鑰的步驟。

## 需要實機驗證的項目

- 中文聊天氣泡的 OCR 順序與準確率，尤其是雙欄、貼圖與複雜背景。
- Choose from Menu 顯示長文字、多行與 emoji 時的截斷行為。
- Back Tap 觸發時的橫幅與權限提示是否進入截圖。
- 後端逾時或錯誤時，捷徑是否顯示可理解的重試提示。

## 歷史研究說明

早期研究曾證明捷徑可將截圖交給外部視覺模型，但那不是現行實作。
專案現在統一採用「手機端 OCR → 後端 DeepSeek」：不傳截圖、不直連模型商、
不在捷徑內儲存金鑰。本文保留研究脈絡，實作以本文與 `docs/api.md` 為準。

## 對 Pitch 的意義

- Android 原生浮動球是完整主體驗；系統不允許 overlay 的 iOS 依然有一條
  零安裝門檻的捷徑路徑。
- 兩端都在手機上讀圖、只送文字，共用同一個後端合約與回覆品質。
- 話術：「Android 用原生球，iOS 用系統捷徑；不同入口，同一顆軍師腦。」
