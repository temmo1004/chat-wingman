# 聊天軍師 Chat Wingman

> 隊伍「四人行」— OpenAI Build Week Community Hackathon Taipei (2026-07-19)

**外掛不在遊戲裡，軍師就在戰場上。**

一顆浮動圓球（Messenger chat head 式）浮在 LINE / 任何聊天 app 上方。
快被句點的時候，點一下球——就地截圖、AI 判讀語境，彈出三種風格的回覆
（認真／幽默／曖昧）＋一句「為什麼這樣回」的軍師解說，點卡片即複製，
貼上送出，全程不離開聊天室。

## 核心流程

```
LINE 聊天中，快被句點
   ↓
點浮動球
   ↓
app 自動截取當前畫面（MediaProjection API）
   ↓
手機端 ML Kit OCR 擷取對話文字（圖片不離開手機）
   ↓
後端 DeepSeek 判讀語境並產生回覆
   ↓
球旁彈出面板：聊死指數 + 三風格回覆卡片 + 軍師解說
   ↓
點卡片 → 複製到剪貼簿 → 面板收合
   ↓
回到輸入框貼上，送出
```

## 差異化三件套

1. **浮動球體驗** — 競品全都要你切出去上傳截圖，我們不離開戰場
2. **軍師解說** — 不只給句子，還教你「為什麼這樣回」；定位是教你聊天，不是幫你聊天
3. **聊死指數** — 對話熱度 0-100 儀表板，快聊死時一目了然

## 技術選型

- **Android 原生（Kotlin + Jetpack Compose）**，產出 APK，模擬器 demo
- 浮動球：`SYSTEM_ALERT_WINDOW` + Foreground Service + WindowManager
- 截圖：MediaProjection API（備案：讀相簿最新一張系統截圖）
- OCR：Android 用手機端 ML Kit；iOS 捷徑用 Apple「從影像擷取文字」（Live Text）
- AI：只將 OCR 文字 POST 到後端，由 DeepSeek 產生 JSON（三風格＋解說＋聊死指數）
- 安全：截圖不上傳；`DEEPSEEK_API_KEY` 只存在後端
- 複製：ClipboardManager
- iOS：系統無 overlay API，現行路線是 Back Tap 觸發的 Apple 捷徑（截圖 → 手機端 OCR → 後端）

## 文件

- [市場調查](docs/research.md) — 競品盤點與差異化定位
- [深度研究 I：產品空間](docs/deep-research.md) — 對抗式驗證的競品/合規結論、pitch 禁用數據
- [深度研究 II：iOS 捷徑路線](docs/ios-shortcuts.md) — Apple Live Text OCR 與 JSON 後端串接
- [深度研究 III：Accessibility 填字實作](docs/accessibility-injection.md) — ACTION_SET_TEXT 定位填字、LINE 實測缺口、備援階梯
- [API 合約](docs/api.md) — 前後端介面，敲定後可並行開發
- [Demo 彩排腳本 & Pitch](docs/demo.md) — 上台流程、差異化答辯、防翻車清單
- [iOS 捷徑建構指南](ios/wingman-shortcut-build.md) — 打後端版，10 分鐘手建、免放金鑰
- [開發規劃](docs/plan.md) — 架構拆解、分工、時間表、風險備案

## 目錄

- `backend/` — Flask `/api/wingman`（DeepSeek 純文字生成 + demo 模式）
- `android/` — Kotlin 浮動球 app（球 + 截圖 + 無障礙填字 + 面板）
- `ios/` — Live Text OCR 捷徑建構指南 + JSON 後端接點測試腳本
