# 深度研究報告 III：Accessibility 定位＋填字實作（2026-07-19）

> 方法：多來源 + 3 票對抗式驗證。結論：**標準 EditText 可行且已被 AOSP CTS
> 證實；但 LINE/IG 本身沒被直接驗到，須現場測**。我們自己的 fa-wen-xiao-bang-shou
> 提供了 LINE 可讀節點的實證，補上這個缺口的一半。

## 好消息（高信心）

- **ACTION_SET_TEXT 對標準原生 EditText 穩定有效**（AOSP CTS 官方測試證實）。
  正確呼叫法：
  ```kotlin
  val args = Bundle().apply {
    putCharSequence(
      AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, replyText)
  }
  node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
  ```
  非可編輯節點會回 false 且不變動——安全。
- **語意查找（`isEditable() && isEnabled()` + EditText 類型檢查）是生產級驗證過的
  策略**（開源 Oversec 就這麼做，零像素座標、零 viewId 依賴）。
- **我們自己的 fa-wen-xiao-bang-shou 已證實 LINE 節點可讀**：LINE Android 暴露
  穩定具名 resource-id（`jp.naver.line.android:id/chat_ui_photo_button`），
  且 bounds 每次動態 dump 計算，非寫死——正確做法的活範例。

## 設計被驗證：填字讓用戶自己送 ✅

研究發現「**填字沒問題，卡的是按送出鈕**」——第三方 app 的 Send 鈕用
Accessibility 點擊常失敗（狀態沒更新）。這正好印證我們的設計：
**AI 填字、使用者手按送出**。我們本來就不打算自動送（避政策紅線），
反而繞開了整個路線最不穩的一環。

## ⚠️ 三個必須知道的風險

1. **Jetpack Compose TextField 是已知失敗案例**：它沒有真正的 EditText buffer，
   所有編輯走 TextFieldState，app 可裝 InputTransformation 靜默拒絕/竄改
   注入的文字——手動打字可以、程式 setText 失敗。**LINE 現在是不是 Compose
   輸入框未知，這是現場第一個要測的**。若 SET_TEXT 無效 → 退回剪貼簿方案。
2. **LINE / IG DM 未被直接驗證**：研究裡沒有一條 claim 直接測到 LINE/IG 的輸入框
   SET_TEXT 行為。fa-wen-xiao-bang-shou 證明「讀得到、點得到」，但「SET_TEXT
   填得進去」仍要現場實測。**這是 demo 前的頭號驗證項**。
3. **Android 17 進階保護模式（AAPM）會整個擋掉**：開啟後系統禁止非無障礙工具
   app 取得 AccessibilityService（還會自動撤銷）。opt-in、預設關，但這條路對
   AAPM 用戶完全失效——pitch 被問到長期上架風險時要誠實提到。

## 備援階梯（SET_TEXT 若在 LINE 失效）

1. 剪貼簿 + `ACTION_PASTE` 到輸入框節點（比 SET_TEXT 相容性高）
   ——**注意：Android 10 以下背景 app 可讀剪貼簿會外洩回覆，我們 target
   Android 12+ 沒問題**
2. 最保底：回到原案「複製到剪貼簿，使用者長按貼上」（純剪貼簿，零 Accessibility）

## 合規（跟報告 I 一致）

- 聊天軍師不符無障礙工具豁免 → 需 **app 內顯著揭露 + 明確同意**（不能只寫在
  商店描述/官網，要有實際同意動作），禁止欺騙性 UI 操作
- 我們「用戶單次觸發、不自動送」的模式站得住，但揭露畫面要做

## 補充查證（第二輪，合成階段故障，僅採信驗證數據）

第二輪研究的合成報告壞掉（回傳空值），但驗證與來源仍可用，補三點：

- **別寫死 resource-id**：「WhatsApp 用 `com.whatsapp:id/entry`/`:id/send` 固定
  viewId 定位」被 0-3 駁回——這類 id 會隨版本改/被混淆。**語意查找（找
  EditText 節點）比 viewId 穩**；LINE 的 `jp.naver.line.android:id/...` 我們現場
  dump 當下有效即可用，但別假設跨版本不變。「填字後自動點送出鈕」在 WhatsApp
  被 0-3 駁回，再次印證：**送出交給用戶**。
- **可抄的開源實作**（AccessibilityService 填字/送訊範本）：
  - [send2zap `GDGService.java`](https://github.com/michelcalacina/send2zap/blob/master/app/src/main/java/com/gdg/manaus/sendtowhatsapp/service/GDGService.java)
  - [Voice-Control `WhatsappAccessibilityService.java`](https://github.com/pochunyan/Voice-Control-mobile/blob/master/app/src/main/java/com/example/tony/smarthelper/WhatsappAccessibilityService.java)
  - [AutoWhatsapp `AutoMsgService.java`](https://github.com/srinivasan-r/AndroidApps/blob/master/AutoWhatsapp/app/src/main/java/com/vs/autowhatsapp/AutoMsgService.java)
- **三服務共存**（Accessibility + overlay 浮動球 + MediaProjection 截圖）：
  - AccessibilityService 走 `BIND_ACCESSIBILITY_SERVICE` 綁定、浮動球用
    `SYSTEM_ALERT_WINDOW`、截圖用 MediaProjection——三者機制獨立，可同時存在
  - **Android 14 起前景服務必須宣告 `foregroundServiceType`**；截圖服務要標
    `mediaProjection`，且 [Android 14 每次啟動投影都要重新取得使用者同意](https://developer.android.com/about/versions/14/behavior-changes-14#media-projection-consent)
    （[服務型別清單](https://developer.android.com/develop/background-work/services/fgs/service-types)）
  - 已知痛點參考：[droidVNC-NG #195](https://github.com/bk138/droidVNC-NG/issues/195)（a11y + 投影共存的實例）

## 實機掃描（MuMu Android 12，2026-07-19）

在 MuMu 模擬器裝了一批真交友軟體，用 `uiautomator dump` 掃各 app 啟動畫面的
輸入框框架：

| App | 啟動畫面輸入框 | 判讀 |
|---|---|---|
| Badoo | 原生 `EditText` | SET_TEXT 應有效 |
| Bumble | 原生 `EditText` | SET_TEXT 應有效 |
| CoffeeMeetsBagel / iPart / JustDating / Yueme / Boo / HeyMandi / Doki / Dopa | 啟動頁是 onboarding、無輸入框 | 未定，須登入進聊天再驗 |

結論：主流交友 app（Badoo/Bumble）用原生輸入框，`ACTION_SET_TEXT` 有機會直接通；
其餘要登入到聊天畫面才能確認框架。無論如何，抓不到 EditText 的一律走
剪貼簿 `ACTION_PASTE` fallback（已內建於 `WingmanAccessibilityService`），
且 demo 走 DEMO_ONLY 本地資料不依賴此路徑。

## 今日實作第一步（省時）

在跑 LINE 的模擬器上：
```bash
adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml
```
抓輸入框的 resource-id（`jp.naver.line.android:id/...`）與 className。
- className 是 `EditText` → SET_TEXT 直接用，穩
- className 是 Compose/自繪 → 立刻切剪貼簿 ACTION_PASTE 備援，別卡在 SET_TEXT
