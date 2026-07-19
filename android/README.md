# Android — 聊天軍師浮動球

Kotlin 原生。點浮動球 → MediaProjection 截圖 → 打後端 `/api/wingman` →
面板顯示聊死指數 + 三風格卡片 → 點卡片用無障礙填字（退回複製剪貼簿）。

> ⚠️ 此骨架未在本機編譯（開發機無 Android SDK）。請用 **Android Studio** 開
> `android/` 目錄，它會自動產生 gradle wrapper 並 sync。以下標「須實測」的點
> 要在模擬器/實機驗證。

## 開起來

1. Android Studio 開 `android/`，等 Gradle sync
2. 改後端位址：`app/build.gradle.kts` 的 `BACKEND_URL`（部署好的公開 URL；
   本機測可用電腦 IP，模擬器連本機用 `http://10.0.2.2:8000`）
   - 註：連 http 明文需在 manifest application 加 `android:usesCleartextTraffic="true"`，
     或用 https。正式後端走 https 就免這步。
3. 跑到模擬器 → app 內三顆按鈕依序：開 overlay 權限 → 開無障礙 → 授權截圖啟動
4. 點浮動球

## 結構

| 檔案 | 職責 |
|---|---|
| `MainActivity.kt` | 權限精靈：overlay / 無障礙 / MediaProjection 授權 |
| `FloatingBubbleService.kt` | 前景服務、浮動球（可拖曳）、點球串起截圖→API→面板 |
| `ScreenCaptureManager.kt` | MediaProjection 單次截圖 → Bitmap |
| `WingmanApi.kt` | OkHttp 打後端、解析成 `WingmanResult` |
| `OverlayPanel.kt` | 聊死指數 + 三卡片 overlay UI |
| `WingmanAccessibilityService.kt` | 找輸入框、`ACTION_SET_TEXT` 填字（退 `ACTION_PASTE`） |

## 須實測（研究報告 III 的未知數）

- **LINE 輸入框填字**：`ACTION_SET_TEXT` 若無效（LINE 用 Compose/自繪輸入框會
  靜默拒絕），已內建退回剪貼簿 `ACTION_PASTE`。第一步先 `adb shell uiautomator
  dump` 看 LINE 輸入框 className 決定走哪條。
- **demo 無投影路徑**：MainActivity 在使用者拒絕截圖時會啟動無投影的服務——
  **Android 14（API 34）會擋 mediaProjection 型前景服務無 token 啟動**。
  黑客松建議用 **API 33（Android 13）模擬器** demo，MediaProjection 規則較寬。
- **截到浮動球自己**：已在截圖前把 bubble 設 INVISIBLE，實測確認有無殘影。
- 三權限（overlay + 無障礙 + 投影）demo 前全部先開好，別現場開。

## 對應文件

- 資料流與備援：`../docs/plan.md`
- 填字/節點研究：`../docs/accessibility-injection.md`
- API 合約：`../docs/api.md`
