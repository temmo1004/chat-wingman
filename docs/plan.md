# 開發規劃

## 平台現實

- **Android**：`SYSTEM_ALERT_WINDOW` + 前景 Service + WindowManager，
  Messenger chat head 標準做法，模擬器可 demo → **今天 Android-only，全力做球**
- **iOS**：無 overlay API，做不到浮動球；賽後可行路線是 Share Extension
- Demo 用 Mac 開 Android 模擬器投影，評審看得清楚

## 技術拆解

| 元件 | 做法 | 難度 |
|---|---|---|
| 浮動圓球 | Foreground Service + WindowManager，可拖曳 | 低（標準樣板） |
| 自動截圖 | MediaProjection API（啟動時授權一次，之後點球即截） | 中（權限流程要走對） |
| 回覆面板 | 同一個 overlay window 展開 Compose UI | 低 |
| AI 分析 | 截圖 base64 → GPT-4o vision → JSON（三風格＋解說＋聊死指數） | 低 |
| 複製 | ClipboardManager | 零 |

**備案階梯**：MediaProjection 卡住 → 改「使用者按系統截圖 → 點球 →
app 抓 MediaStore 最新一張截圖」，少一步但穩，demo 照樣成立。

### Accessibility 定位＋填字層（不用寫死座標）

盲人輔助 API（AccessibilityService）給的是整棵 UI 節點樹
（AccessibilityNodeInfo），不是像素——CheckMate 硬編碼 1256px
只能跑單一機型的坑，我們用語意查找直接解掉：

- 找輸入框：遍歷節點樹找 `className == EditText && isEditable`
  （比 viewId 穩，換手機/換聊天 app 都通用）；需要位置時 `getBoundsInScreen()`
- **殺手應用**：對輸入框節點 `ACTION_SET_TEXT`——demo 從「複製→切回→貼上」
  變成「點卡片，字直接出現在 LINE 輸入框」，用戶只按送出
  （送出仍是用戶手按，不踩自動代發紅線）
- 分層：讀內容仍走截圖+vision（看得懂貼圖/照片、任何 app 通用）；
  定位與填字走節點樹；節點讀 TextView 當網路慢時的文字備援
- 坑：FLAG_SECURE app 擋截圖不擋節點樹，反之有些 app 標
  `importantForAccessibility=no`；LINE 節點可讀（TalkBack 用戶在用）

#### 已驗證：LINE 節點可讀（來自我們自己的 fa-wen-xiao-bang-shou）

同帳號既有專案「發文小幫手」已在真 LINE Android app 上實作節點定位，
證明 **LINE 暴露穩定的具名 resource-id**，例如：
`jp.naver.line.android:id/chat_ui_photo_button`（相簿/圖片按鈕）。
它走的是外部 ADB 路線（`uiautomator dump` → regex 抓 resource-id + bounds
→ `adb shell input tap`），軍師 app 改成 on-device 就是
`findAccessibilityNodeInfosByViewId()` + `ACTION_CLICK/ACTION_SET_TEXT`——
**同一棵樹、同一組 id**。關鍵優點：bounds 每次從 dump 動態算，非寫死座標。

實作第一步（省時）：在跑 LINE 的模擬器上 `adb shell uiautomator dump`，
抓輸入框的 resource-id（`jp.naver.line.android:id/...`），軍師 app 直接沿用。
若要純填字免點座標，優先 `ACTION_SET_TEXT` 到該 EditText 節點。

## 分工

| 人 | 負責 | 產出 |
|---|---|---|
| A (bath) | 圓球 Service + MediaProjection + 架構 | 完整資料流 |
| B | overlay 面板 UI（Compose） | 展開動畫、卡片、聊死指數儀表 |
| C | Prompt 工程 + demo 假資料 | 三風格口吻、demo 模式開關（斷網可演） |
| D | 簡報 + demo 排練 | 故事線：快被句點 → 軍師救場 → 約到人 |

## 時間表

1. **第 1 hr**：圓球浮起來、可拖曳、點擊有反應（風險最高，最先做）
2. **第 2-3 hr**：截圖 → API → 面板顯示回覆，全流程打通
3. **第 4-5 hr**：UI 打磨、聊死指數、prompt 調校
4. **最後 1 hr**：只排練不寫 code；範例截圖預跑過確保不翻車

## 風險

- Android 14+：前景服務要宣告 `foregroundServiceType="mediaProjection"`，
  且無投影 token 不能啟動該型服務——**demo 建議用 API 33（Android 13）模擬器**，
  MediaProjection 規則較寬（見 android/README.md）
- 「顯示在其他應用程式上層」權限模擬器要手動開，demo 前先開好
- 現場網路爛／API 掛：C 的 demo 模式開關走本地假資料
- 時間不夠的砍功能順序：人格側寫 → 拍照 → 聊死指數；
  「三風格回覆＋複製」絕不砍

## 加分項（時間夠再做）

- 對象人格側寫：同一人多張截圖累積分析，回覆越來越貼
- 句點危機偵測主動警示
