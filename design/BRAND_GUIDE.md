# 聊天軍師・孔明帽品牌指南

本文件定義孔明帽的來源、衍生檔角色、背景、留白、小尺寸與 Android 使用方式。正式 UI Token 與 Compose 元件規範實作於 [`WingmanTheme.kt`](../android/app/src/main/java/uk/hakkaren/wingman/ui/WingmanTheme.kt)。

> 規格狀態：這是品牌與輸出規範，不等同於實機驗證報告。遮罩、深色背景、Launcher 與通知列仍須依文末清單實際檢查。

---

## 1. 品牌核心

- 正式標誌是使用者提供的橘色／焦糖色孔明帽圖形；保留五片帽面、白色分隔、中央帽帶與左右垂帶的原始幾何和漸層。
- 品牌氣質：溫暖、機智、可靠、克制。Logo 是辨識點，不是頁面裝飾紋理。
- 官方品牌底色是 **Brand Cream `#F9E9D3`**。
- 不重新描繪、不改比例、不改漸層、不旋轉、不套濃陰影、不加外光，也不把 Logo 改成 emoji 或一般 Material 圖示。

---

## 2. Source of Truth 與檔案角色

| 角色 | 檔案／資源 | 規則 |
|---|---|---|
| 正式原始 Master | [`icon/chat-wingman-icon-master.png`](icon/chat-wingman-icon-master.png) | 1254×1254、不透明 RGB。是原始幾何、顏色與構圖的最高權威；保留原檔，不直接覆寫。 |
| 舊透明擷取輸入 | [`icon/chat-wingman-icon-foreground.png`](icon/chat-wingman-icon-foreground.png) | 原始 RGBA 擷取。透明與半透明邊緣曾保留米色 matte，只作可重現的處理輸入，不直接用於 UI 或深色背景。 |
| 透明 Master | [`icon/chat-wingman-hat-transparent-master.png`](icon/chat-wingman-hat-transparent-master.png) | 1254×1254 RGBA 衍生檔；保留原圖造型並清除透明邊緣 matte。透明用途的正式來源。 |
| Compose UI 品牌圖 | `drawable-nodpi/brand_kongming_hat.png`／`R.drawable.brand_kongming_hat` | 供 Hero、按鈕、Loading、Dialog 使用。由 `WingmanLogo` 統一載入及縮放，不在各畫面另做裁切。 |
| Adaptive 前景 | `drawable-*/ic_launcher_foreground.png` | 108dp layer 的各密度輸出，只供 Launcher Adaptive Icon。 |
| Adaptive 背景 | `@color/launcher_icon_background` | 固定 `#F9E9D3`。 |
| Legacy Launcher | `mipmap-*/ic_launcher.png` | 舊版方形／系統遮罩圖示，不當一般 UI 圖。 |
| Round Launcher | `mipmap-*/ic_launcher_round.png` | 舊版圓形 Launcher 專用，不以方形檔臨時裁圓。 |
| Play Store | [`icon/chat-wingman-icon-play-store.png`](icon/chat-wingman-icon-play-store.png) | 512×512、不透明商店圖；保持與 Master 相同構圖。 |
| Android 13+ themed icon | `drawable/ic_launcher_monochrome.xml` | Launcher 主題化單色 alpha mask；與通知 Small Icon 是不同角色。 |
| 通知 Small Icon | `drawable/ic_stat_kongming_hat.xml` | 24dp 單色實心輪廓，由 Android 系統著色；不可改用全彩 PNG。 |
| Review board | [`icon/chat-wingman-brand-validation.png`](icon/chat-wingman-brand-validation.png) | 供人工比對白、米色、深色、小尺寸及常見 Adaptive mask；存在不代表已通過實機檢查。 |
| 衍生工具 | [`icon/generate_android_assets.py`](icon/generate_android_assets.py) | 從核准來源重建透明、UI、Launcher、Round、Play Store 與 review board；衍生 PNG 不手工逐張修補。 |

### 2.1 權威順序

1. 原始 Master 決定造型、比例、漸層與品牌顏色。
2. 透明 Master 決定去背與透明邊緣。
3. 產生腳本決定 Android 衍生尺寸、位置及可重現輸出。
4. UI／Launcher／通知檔只服務各自場景，不反向成為品牌原稿。

若來源有新版本，先封存舊 Master、更新核准 Master 與透明處理，再一次重建全部衍生檔。不要只替換其中一個密度。

---

## 3. 背景與透明邊緣

### 3.1 核准背景

| 背景 | 使用方式 |
|---|---|
| `#F9E9D3` Brand Cream | 首選品牌保護底、Launcher 背景、浮動球底與小尺寸容器。 |
| `#FFFCF8` Warm White | 首頁背景；使用清邊透明品牌圖。 |
| `#FFFFFF` Surface | Card／Dialog；使用清邊透明品牌圖並保留 clear space。 |
| 深色或照片背景 | 僅使用透明 Master／`brand_kongming_hat`。若帽帶或橘色漸層辨識不穩定，改放在 `#F9E9D3` 不透明保護容器內。 |

### 3.2 深色背景規則

- 禁止直接使用舊 `chat-wingman-icon-foreground.png`；其低 alpha 邊緣曾含米色 RGB，可能在深色背景形成淡色光暈。
- Alpha 為 0 的像素應為透明黑；半透明抗鋸齒像素應延伸鄰近帽子顏色，而不是米色／白色 matte。
- 不用白色描邊掩蓋毛邊，也不把橘色改成亮黃。需要保護時使用完整 `#F9E9D3` 容器。
- 深色、黑色、深藍、照片與高雜訊聊天背景都要在實際渲染尺寸檢查；review board 只能作初步人工比對。
- 不在與帽子相近的橘／棕背景上直接裸放 Logo；使用 Brand Cream 容器建立分離。

---

## 4. Clear Space 與比例

- Clear space 從**可見 alpha 邊界**量起，每側至少為可見 Logo 寬度的 **1/8**，且在 App UI 中不得小於 **8dp**。
- Hero、Loading、Dialog 的外層 layout 必須提供 clear space；PNG 內建透明 padding 不可被視為唯一保證。
- 小型按鈕可把按鈕本身的 8dp 以上圖文間距與容器 padding 計入 clear space，但 Logo 不得貼到文字、邊框或 ripple 裁切邊界。
- Launcher 不套用一般 clear-space 比例，改遵循 Adaptive safe area。
- 永遠等比 `ContentScale.Fit`；禁止 `Crop`、非等比縮放、左右翻轉或任意壓扁帽身。
- 不切掉左右垂帶、最上方帽面或白色分隔；若空間不足，縮小完整標誌或改用單色小圖，不裁切。

---

## 5. Adaptive Launcher Icon

- Foreground layer 的設計座標為 **108×108dp**；背景固定為 `#F9E9D3`。
- 所有關鍵可見內容必須落在中心 **66×66dp 的保證安全圓**內，即距中心不超過 33dp。
- 目前衍生規則以約 `52/108` 的內容高度保留五片帽面及垂帶，同時預留 mask 與視差動畫空間。修改比例後必須重新檢查最遠 opaque pixel，不能只看方形預覽。
- Foreground 外圍保持透明；不可把米色方底烘焙進 Adaptive 前景。
- 必須預覽 circle、rounded square、squircle 與 OEM mask；帽頂、左右帽面及垂帶都不可被裁。
- `android:icon` 使用 `@mipmap/ic_launcher`；`android:roundIcon` 使用 `@mipmap/ic_launcher_round`。Round 資源需要獨立存在，不依賴 Launcher 臨時裁切。
- Android 13+ themed icon 使用 `ic_launcher_monochrome` alpha mask；不要把全彩漸層塞進 monochrome layer。
- 產出 review board 或模擬 mask 不等於完成驗證；至少仍需一台實機 Launcher 檢查靜態與長按／拖曳動畫。

---

## 6. Notification Small Icon

- 使用 `ic_stat_kongming_hat` 單色 VectorDrawable；畫布透明、圖形單一實色，由 Android 依狀態列／通知樣式著色。
- 不含 `#F9E9D3` 圓底、橘色漸層、陰影、半透明照片或細到消失的內部裝飾。
- 五片帽面之間保留可辨認的負空間；帽帶與垂帶在小尺寸需形成耐用輪廓。
- Small Icon 不等於 `ic_launcher_monochrome`：兩者可以共享簡化語彙，但 viewport、縮放與平台角色不同。
- 通知的大圖或訊息內品牌圖若需要全彩，另以適合的大圖資源處理；Small Icon 永遠維持單色。

---

## 7. 小尺寸處理

| 顯示尺寸 | 處理 |
|---:|---|
| 64dp 以上 | 使用完整清邊品牌圖，保留原始比例與漸層。 |
| 40–63dp | 使用完整圖但提高光學佔比；確認白色分隔與帽帶仍清楚。 |
| 24–39dp | 使用預備的 compact 配置或置於 Brand Cream 保護底；優先保住五片帽面、帽帶與外輪廓，不銳化或加粗原始漸層圖。 |
| 小於 24dp | 不使用全彩完整 Logo；通知／系統小圖改用核准單色輪廓。 |

- 縮圖後至少要看得出「五片帽面 + 帽帶」，不能只剩無法辨識的橘色塊。
- 不從 Launcher 截圖或從 48px 檔再次放大；所有尺寸從透明 Master 產生。
- 小尺寸的米色保護底是辨識工具，不是另一個 Logo；同區仍遵守使用上限。

---

## 8. 指定 UI 位置

| 場景 | 品牌使用 |
|---|---|
| 首頁「軍師已上線」Hero | 一個 104dp（窄螢幕／大字）或 120dp 主孔明帽；使用 `WingmanLogo` Hero variant。 |
| 「測試浮動球」按鈕 | 一個 28dp compact 孔明帽，配白字與 8dp 圖文間距；它代表即將出現的真實浮動球。 |
| 真實浮動球 | 64×64dp Brand Cream 圓形容器，使用清邊孔明帽；完整容器可拖曳／點擊。 |
| 載入狀態 | 一個 72dp 靜態孔明帽 + Material Progress Indicator；不旋轉或彈跳 Logo。 |
| 使用方法 Dialog | 若需要品牌識別，最多一個主 Logo；不可與 Loading／Hero 同畫面重複堆疊。 |
| Permission、一般 Card | 不放 Logo；使用 Shield、Info、Check 等 Material Icons。 |
| 首頁導覽 | 不提供底部分頁；產品只保留首頁與聊天上的浮動分析面板。 |
| Android 通知列 | 只使用 `ic_stat_kongming_hat` 單色 Small Icon。 |

### 8.1 Logo 密度限制

- 一個視覺區域最多一個 **64dp 以上主 Logo**。
- 同區若已有主 Logo，只能再出現一個具有直接功能意義的 compact Logo，例如「測試浮動球」；不增加裝飾版、浮水印或背景版。
- Overlay 打開後，不在每張回覆卡、每個語氣按鈕或操作列重複孔明帽。
- 品牌圖與相鄰 Material icon 需有清楚層級：Logo 表達「軍師」，Material icon 表達「動作／狀態」。

---

## 9. 產出與變更流程

1. 保留並雜湊核准的原始 Master；不要以衍生圖覆蓋。
2. 從既有 alpha 擷取重建清邊透明 Master，不重畫或改變原圖幾何。
3. 由 `generate_android_assets.py` 產生 UI、Adaptive、Legacy、Round、Play Store 與 review board。
4. Monochrome Launcher 與 Notification Small Icon 依平台規範維持單色 VectorDrawable。
5. 檢視版本差異：尺寸、alpha、色彩、置中、safe area、檔名與資源限定詞。
6. 完成模擬 mask、深淺背景、小尺寸與實機檢查後，再在驗證紀錄中標記通過；不要在本指南預先宣稱已通過。

---

## 10. 品牌驗證清單

- [ ] 原始 Master 仍為 1254×1254 且未被透明衍生檔覆蓋。
- [ ] 透明 Master 在黑、深藍、白、`#FFFCF8` 與 `#F9E9D3` 上沒有米白／白色毛邊。
- [ ] 橘色漸層與焦糖帽帶在所有核准背景仍可辨認。
- [ ] 24、28、32、48、64dp 下仍看得出五片帽面、帽帶與外輪廓。
- [ ] Adaptive 前景關鍵內容位於 66×66dp 保證安全圓內。
- [ ] circle、rounded square、squircle 與至少一台實機 Launcher 未裁切。
- [ ] 方形、圓形、themed icon 與 Play Store 圖各自使用正確資源。
- [ ] 通知 Small Icon 在淺／深系統主題與不同密度上由系統正確著色。
- [ ] Hero、測試按鈕、真實浮動球與 Loading 使用正確版本及尺寸。
- [ ] Permission 與一般 Card 沒有重複放置 Logo，首頁沒有額外分頁導覽。
