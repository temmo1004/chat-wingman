# 聊天軍師 Chat Wingman 🔮

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
點浮動圓球 🔮
   ↓
app 自動截取當前畫面（MediaProjection API）
   ↓
GPT-4o vision 讀圖 → 判讀語境
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
- AI：截圖 base64 → GPT-4o vision → JSON（三風格＋解說＋聊死指數）
- 複製：ClipboardManager
- iOS 賽後再議（系統無 overlay API，可行路線為 Share Extension）

## 文件

- [市場調查](docs/research.md) — 競品盤點與差異化定位
- [深度研究 I：產品空間](docs/deep-research.md) — 對抗式驗證的競品/合規結論、pitch 禁用數據
- [深度研究 II：iOS 捷徑實作指南](docs/ios-shortcuts.md) — 免寫 app 路線，照做就能動
- [深度研究 III：Accessibility 填字實作](docs/accessibility-injection.md) — ACTION_SET_TEXT 定位填字、LINE 實測缺口、備援階梯
- [API 合約](docs/api.md) — 前後端介面，敲定後可並行開發
- [iOS 捷徑建構指南](ios/wingman-shortcut-build.md) — 打後端版，10 分鐘手建、免放金鑰
- [開發規劃](docs/plan.md) — 架構拆解、分工、時間表、風險備案

## 目錄

- `backend/` — Flask `/api/wingman`（GPT-4o vision + demo 模式）
- `android/` — Kotlin 浮動球 app（球 + 截圖 + 無障礙填字 + 面板）
- `ios/` — 捷徑建構指南 + 後端接點測試腳本
