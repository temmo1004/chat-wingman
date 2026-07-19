# 市場調查（2026-07-19）

> **Historical research / 歷史研究快照**：本文保留競品與早期架構參考；
> 現行實作為手機端 OCR + DeepSeek 純文字後端，以 [API 合約](api.md) 為準。
>
> 更深入的多來源對抗式驗證版本見 [deep-research.md](deep-research.md)——
> 含 Arc/Crystal Ball/CheckMate 三大先例對照、Google Play 與 LINE 合規結論、
> 以及 **pitch 禁用的 Rizz 數據清單**。

結論：**「截圖 → AI 回覆建議」已是紅海；「浮動球 + 就地截圖 + 不離開聊天室」
這個組合還沒有專門的聊天軍師產品。差異點就在那顆球上。**

## 一、截圖上傳型（紅海區）

流程全都是「切出去 → 上傳截圖 → 得到回覆 → 切回來」：

| 產品 | 說明 | 連結 |
|---|---|---|
| Rizz AI | 上傳聊天截圖產回覆，iOS/Android 都有 | [App Store](https://apps.apple.com/us/app/rizz-ai-dating-replies/id6476662782) / [Google Play](https://play.google.com/store/apps/details?id=com.prime.aidating) |
| Rizz Plug AI | 上傳截圖即時產生個人化回覆，免註冊 | [官網](https://rizzplugai.com/) |
| SmoothRizz | 同類，網頁版回覆產生器 | [官網](https://www.smoothrizz.com/) |
| YourMove AI | 回覆建議＋交友檔案撰寫，訂閱制 | [競品整理](https://textvibe.app/blog/best-dating-text-helper-apps/) |
| 恋爱话术宝 | 中文話術庫型 | [App Store](https://apps.apple.com/tw/app/id6449920030) |
| 恋爱键盘 | **值得注意**：做成自訂輸入法鍵盤達成「不離開聊天室」，有截圖解析 | [Google Play](https://play.google.com/store/apps/details?id=com.smkeyboardan.app&hl=zh) |

共通點：都有語氣選擇（flirty / funny / bold ≈ 我們的 認真／幽默／曖昧）。
需求已被驗證，但體驗全都要離開聊天室——除了恋爱键盘的輸入法路線
（可放簡報當「我們考慮過的替代方案」）。

## 二、浮動球 AI 助手（存在，但都是通用助手）

| 產品 | 說明 | 連結 |
|---|---|---|
| FloatingAI | 浮動泡泡＋螢幕內容辨識；摘要、草擬回覆、生成點子，萬用定位 | [介紹](https://www.aibucket.io/tools/floatingai) |
| Arc: AI Screen Assistant | 螢幕邊緣滑出的系統級 AI 側欄；摘要／寫作／抽取文字 | [Google Play](https://play.google.com/store/apps/details?id=com.rethink.arc) |
| Wispr Flow | 鍵盤上方浮動球做語音聽寫 | [新聞](https://www.technobezz.com/news/wispr-flow-launches-its-ai-dictation-app-on-android-with-a-floating-bubble-interface) |
| Gemini 浮動泡泡 | **Google 正在測試**（Android 17）把 Gemini overlay 做成常駐浮動泡泡 | [報導](https://www.androidheadlines.com/2026/06/google-gemini-overlay-floating-bubble-update.html) |

Gemini 泡泡是利多：pitch 可說「連 Google 都認為 AI 該以浮動球的形式
活在其他 app 之上，我們把這個形態做到聊天這個垂直場景的極致」。

## 三、開源積木（直接參考）

- [bubbles-for-android](https://github.com/txusballesteros/bubbles-for-android) — chat head 標準函式庫
- [FloatingBubble (Kotlin 範例)](https://github.com/Vitor720/FloatingBubble) — Messenger 式浮動球最小實作
- [Android-ChatHead](https://github.com/henrychuangtw/Android-ChatHead) — 另一個 FB Messenger 式參考
- [OpenDroid](https://github.com/yashab-cyber/opendroid) — Accessibility API 抓螢幕餵 vision LLM 的完整參考（MediaProjection 卡住時的備案路線範例）

## 評審 QA 預備

**Q：跟 Rizz 有什麼不一樣？**
A：那些全都要你離開聊天室去操作；我們是球浮在 LINE 上、點一下就地解決，
少四個步驟。競品的存在證明需求是真的，我們贏在體驗形態。
