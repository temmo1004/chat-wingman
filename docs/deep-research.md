# 深度研究報告 I：產品空間總調查（2026-07-19）

> **Historical research / 歷史研究快照**：以下保留當時的 vision 競品比較，
> 不代表現行傳輸架構。目前是手機端 OCR，只將文字送到 DeepSeek 後端；
> 實作以 [API 合約](api.md) 為準。
>
> 方法：5 角度並行搜尋 → 22 來源 → 97 claims → 每條 3 票對抗式驗證
> （25 條驗證：18 存活、7 被駁回）。以下每條結論都附一手來源。

## 總結論：差異化站得住

**目前沒有任何產品完整做出「浮動圓球 + 點擊就地截圖 + vision LLM 判讀 +
三風格戀愛回覆 + 聊死指數」的組合。** 最接近的三個先例各缺一角。

## 三個最接近的先例（pitch 對照表）

| 先例 | 有什麼 | 缺什麼 |
|---|---|---|
| [Arc: AI Screen Assistant](https://play.google.com/store/apps/details?id=com.rethink.arc)（Play 上最接近的活競品，10k+ 下載、獨立開發者） | 系統級 overlay、截圖視覺分析、AI 回覆生成 | **邊緣滑出側欄不是圓球**；定位通用生產力，語氣是 Professional/Polite/Concise，零戀愛風格、無軍師解說、無聊死指數 |
| Crystal Ball - Floating AI（com.panda.ai） | 浮動球 + 框選截圖問 AI | 只是通用 chatbot，無回覆風格/聊天分析/複製流程；**其 Play 商店頁 2026-07-19 實測 404**（下架或區域不可用，原因未證實）——overlay 截圖類 app 政策風險的警訊 |
| [CheckMate](https://devpost.com/software/checkmate-a0m89g)（Royal Hackaway v8 得獎，Best Use of Gen AI，2025-02） | overlay 疊在交友 app 上、讀訊息、用西洋棋棋評符號打分 | 走 AccessibilityService + **硬編碼像素座標**，只能跑 Pixel 9 Pro 模擬器；純文字模型無 vision；重評分輕生成 |

**Pitch 用法**：CheckMate 證明「浮動聊天軍師」方向已被黑客松評審買單；
它的單機型限制正好反襯我們「截圖 → vision LLM」的裝置無關優勢。

## 合規結論（評審問就答這個）

- **Google Play / AccessibilityService 路線**：聊天軍師不符 `isAccessibilityTool`
  豁免（政策明文排除 assistants 與 automation tools），要走完整揭露＋同意＋
  Play Console 宣告表單（附影片證據）。（[政策](https://support.google.com/googleplay/android-developer/answer/10964491)）
- **但 Arc 是活體合規模板**：「僅使用者主動觸發時讀屏、截圖經同意、不背景監控、
  在 200+ 銀行/密碼 app 自動停用」——2026-01 Google 收緊審查後仍在架上
- **敏感權限紅線**：overlay 與截圖必須是商店頁明示的核心功能；禁止自動代發訊息
  ——**我們「複製到剪貼簿、用戶自己貼上」的設計正好避開自動操作紅線**
- **LINE 規約**：無明文禁止截圖/overlay 讀屏（LINE 官方甚至自帶對話截圖與
  AI 回覆建議功能）；最接近的是 15(5) 禁止 BOT/作弊工具「不正操作」——
  唯讀截圖工具屬灰色地帶；但 17 條保留不經通知刪帳號權利。
  Pitch 話術：**我們不碰 LINE API、不自動操作 LINE，只做 OS 層截圖＋剪貼簿**

## ⚠️ Pitch 禁用數據（驗證被駁回）

Forbes 那篇 Rizz 報導的數字（150 萬活躍、750 萬累計、月增 30%、近 1 億則回覆、
日下載 2 萬、下載前五交友 app）**全部未通過對抗式驗證，簡報不要引用**。
要講品類熱度就講「Rizz 類 app 大量存在且持續營運」這種定性描述，
或現場開 App Store 榜單截圖。

## 待補缺口

- 歐美競品可信的下載/營收數據（目前沒有可引用的）
- iOS 捷徑路線可行性（→ 深度研究報告 II，進行中；初步訊號：**iOS 不允許捷徑
  在背景靜默截取其他 app 畫面**，穩定做法可能是「使用者手動截圖 → 捷徑抓最新
  照片」，待報告 II 證實）
- 中日韓話術 app 圖譜（本輪沒有存活的 claim）
