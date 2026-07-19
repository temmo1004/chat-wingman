# iOS 捷徑建構指南（手機端 OCR + 後端版）

iOS 捷徑先用 Apple「從影像擷取文字」（Extract Text from Image）在手機上做 OCR，
再將辨識到的文字以 JSON 送到我們的 `/api/wingman`。截圖不會上傳，
捷徑也不儲存 DeepSeek 金鑰；`DEEPSEEK_API_KEY` 只留在後端。

Apple 官方資料確認：

- [「從影像擷取文字」支援 Live Text 支援的語言](https://support.apple.com/en-us/106430)
- [「取得 URL 內容」在 POST 時可選 JSON Request Body](https://support.apple.com/en-au/guide/shortcuts/apd58d46713f/ios)

可先用 [`test-request.sh`](test-request.sh) 以 OCR 文字檔驗證相同的 JSON 後端合約。

## 流程總覽

```text
敲擊背面兩下 → 擷取螢幕畫面 → 從影像擷取文字（手機端 Live Text OCR）
→ POST application/json {text, demo} 到 /api/wingman
→ 解析三風格回覆 → 選單選一個 → 拷貝到剪貼簿 → 回聊天長按貼上
```

## 前置條件

- 後端已部署且有 HTTPS 公開 URL（例如 `https://api.example.com`）
- iPhone 的「設定 → 一般 → 語言與地區 → Live Text」已開啟
- 後端環境變數已設定 `DEEPSEEK_API_KEY`；不要把金鑰放入捷徑

## 動作逐步建立

1. **文字 (Text)**：輸入後端網址，例如 `https://api.example.com`。
   接上 **設定變數 (Set Variable)**，命名為 `BASE`。
   之後更換後端只需修改這一格。

2. **擷取螢幕畫面 (Take Screenshot)**：輸入是觸發當下的聊天畫面。
   主方案是用敲擊背面觸發，不要用 Siri，以免其介面入鏡。

3. **從影像擷取文字 (Extract Text from Image)**：
   - 影像 = 上一步的「螢幕畫面」
   - 接上 **設定變數**，命名為 `OCR文字`
   - 可加一個 **如果 (If)** 檢查 `OCR文字` 是否有值；沒有值時顯示
     「沒有辨識到對話文字」並停止捷徑，避免送出空請求。

4. **取得 URL 內容 (Get Contents of URL)**：
   - URL：`BASE` 變數加上 `/api/wingman`
   - 方法：**POST**
   - 要求主體 (Request Body)：**JSON**
   - JSON 欄位：

     | 鍵 | 類型 | 值 |
     |---|---|---|
     | `text` | 文字 | 魔法變數 `OCR文字` |
     | `demo` | 布林值 | `false` |

   選擇 JSON 後，捷徑會以 `application/json` 送出。不要新增 `image`、
   `Authorization` 或任何模型金鑰欄位。

5. **解析回應**：回應是 JSON，用 **取得字典值 (Get Dictionary Value)**
   取得下列鍵路徑，並分別設定變數：

   | Get Value for（鍵路徑） | Set Variable |
   |---|---|
   | `chat_death_index` | `指數` |
   | `replies.1.text` | `認真` |
   | `replies.2.text` | `幽默` |
   | `replies.3.text` | `曖昧` |

   捷徑的清單項目在這個鍵路徑用法中從 1 起算；來源都選「取得 URL 內容」。

6. **從選單選擇 (Choose from Menu)**：
   - 提示：`軍師建議（聊死指數 [指數]）`
   - 三個選項：`🎯 認真：[認真]`、`😏 幽默：[幽默]`、`💘 曖昧：[曖昧]`

7. 每個選項底下各放：
   - **拷貝到剪貼簿 (Copy to Clipboard)** = 對應的回覆變數
   - **顯示通知 (Show Notification)**：`已複製，回聊天長按貼上`

## 綁定觸發

前往「設定 → 輔助使用 → 觸控 → 敲擊背面 → 點兩下」，選擇這支捷徑。
如果系統提供「顯示橫幅 (Show Banner)」選項，請關閉，避免橫幅出現在截圖中。

## Demo 模式

若 DeepSeek 未設金鑰或暫時不可用，但捷徑仍連得到後端，將第 4 步
JSON 的 `demo` 改為 `true`。`text` 仍保留 `OCR文字`，後端會忽略它並回傳
固定範本。Demo 模式可避開模型 API，但仍需要網路能連到後端。

## 已知限制

- Live Text 不是所有地區與語言都可用；若找不到擷取文字動作，先檢查
  系統版本、語言與 Live Text 設定。
- OCR 會依畫面閱讀順序輸出文字，MVP 不會像 Android 版那樣根據左右氣泡
  自動加上「對方:」/「我:」。
- 不要用 Siri 觸發；AssistiveTouch 的畫面按鈕也可能入鏡。
- 分享捷徑前，對 `BASE` 加入匯入問題，避免將私有後端網址寫死。
