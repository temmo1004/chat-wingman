package uk.hakkaren.wingman

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * 常駐前景服務：畫出可拖曳的浮動球。點球 → 截圖 → 打後端 → 彈出面板。
 * 同時持有 MediaProjection（由 MainActivity 授權後透過 Intent 傳入）。
 */
class FloatingBubbleService : Service() {

    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: OverlayPanel? = null
    private var capture: ScreenCaptureManager? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())

        // 從 MainActivity 拿 MediaProjection 授權結果，建立擷取器
        if (capture == null && intent != null) {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
            val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
            if (resultCode != 0 && data != null) {
                val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val projection: MediaProjection = mpm.getMediaProjection(resultCode, data)
                capture = ScreenCaptureManager(projection, displayMetrics())
            }
        }

        if (bubble == null) showBubble()
        return START_STICKY
    }

    private fun displayMetrics(): DisplayMetrics =
        DisplayMetrics().also {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(it)
        }

    // ── 浮動球 ────────────────────────────────────────────
    private fun showBubble() {
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = ImageView(this).apply {
            setImageResource(R.drawable.ic_launcher_foreground)
            scaleType = ImageView.ScaleType.FIT_CENTER
            val iconPadding = (4 * resources.displayMetrics.density).toInt()
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(this@FloatingBubbleService.getColor(R.color.launcher_icon_background))
            }
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            elevation = 12f * resources.displayMetrics.density
            contentDescription = getString(R.string.bubble_content_description)
        }
        val bubbleSize = (64 * resources.displayMetrics.density).toInt()
        val lp = WindowManager.LayoutParams(
            bubbleSize,
            bubbleSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24; y = 300
        }

        // 拖曳 + 短按（截圖）+ 長按（相簿）判定
        var downX = 0f; var downY = 0f; var lpX = 0; var lpY = 0
        var longPressed = false
        val longPress = Runnable { longPressed = true; openAlbum() }
        view.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY; lpX = lp.x; lpY = lp.y
                    longPressed = false
                    view.postDelayed(longPress, 500)  // 長按 500ms → 相簿
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val moved = abs(e.rawX - downX) + abs(e.rawY - downY)
                    if (moved > 20) view.removeCallbacks(longPress)  // 拖曳取消長按
                    lp.x = lpX + (e.rawX - downX).toInt()
                    lp.y = lpY + (e.rawY - downY).toInt()
                    wm.updateViewLayout(view, lp); true
                }
                MotionEvent.ACTION_UP -> {
                    view.removeCallbacks(longPress)
                    val moved = abs(e.rawX - downX) + abs(e.rawY - downY)
                    if (!longPressed && moved < 20) onBubbleTap()  // 短按 → 截當前畫面
                    true
                }
                else -> false
            }
        }
        bubble = view
        wm.addView(view, lp)
    }

    private fun onBubbleTap() {
        // DEMO_ONLY：不截圖、不 OCR、不連網，直接本地範本
        if (BuildConfig.DEMO_ONLY) { showPanel(LocalDemo.result); return }
        val cap = capture ?: run { showPanel(LocalDemo.result); return }
        // 收球避免截到自己
        bubble?.visibility = View.INVISIBLE
        cap.captureOnce { bmp ->
            bubble?.visibility = View.VISIBLE
            if (bmp == null) { showPanel(LocalDemo.result); return@captureOnce }
            ocrThenAnalyze(bmp)
        }
    }

    /** 相簿選的圖：解碼 → OCR → 分析。 */
    private fun analyzeAlbumBitmap(bytes: ByteArray) {
        if (BuildConfig.DEMO_ONLY) { showPanel(LocalDemo.result); return }
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: run { showPanel(LocalDemo.result); return }
        ocrThenAnalyze(bmp)
    }

    // ── OCR → 打後端 → 顯示面板 ───────────────────────────
    /** 手機端 OCR 截圖成對話文字，再送後端。圖片不離開手機。 */
    private fun ocrThenAnalyze(bmp: Bitmap) {
        OcrHelper.extract(bmp) { text -> analyze(text) }
    }

    private fun analyze(text: String) {
        showPanelLoading()  // 先顯示 loading（隊友 Compose 面板 UX）
        scope.launch {
            val result = try {
                withContext(Dispatchers.IO) { WingmanApi.analyze(text, demo = false) }
            } catch (e: Exception) {
                // 失敗保底：後端 demo 模式，再不行退本地範本，斷網也能演
                try { withContext(Dispatchers.IO) { WingmanApi.analyze(null, demo = true) } }
                catch (e2: Exception) { LocalDemo.result }
            }
            showPanel(result)
        }
    }

    private fun showPanel(result: WingmanResult) {
        ensurePanel().show(result)
    }

    private fun showPanelLoading() {
        ensurePanel().showLoading()
    }

    private fun ensurePanel(): OverlayPanel {
        return panel ?: OverlayPanel(
            ctx = this,
            onFill = ::fillReply,
            onCopy = ::copyReply,
            onRefresh = {
                panel?.dismiss()
                panel = null
                onBubbleTap()
            },
            onDismissed = { panel = null },
        ).also { panel = it }
    }

    private fun fillReply(reply: Reply) {
        val filled = WingmanAccessibilityService.fill(reply.text)
        if (!filled) {
            copyReply(reply, dismiss = false)
            Toast.makeText(this, "無法自動填入，已改為複製", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "已填入輸入框，確認後按送出", Toast.LENGTH_SHORT).show()
        }
        panel?.dismiss()
        panel = null
    }

    private fun copyReply(reply: Reply) {
        copyReply(reply, dismiss = true)
    }

    private fun copyReply(reply: Reply, dismiss: Boolean) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("wingman", reply.text))
        if (dismiss) {
            Toast.makeText(this, "已複製回覆", Toast.LENGTH_SHORT).show()
            panel?.dismiss()
            panel = null
        }
    }

    // ── 前景通知 ─────────────────────────────────────────
    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL, "聊天軍師", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("聊天軍師運作中")
            .setContentText("點孔明帽浮動球取得回覆建議")
            .setSmallIcon(R.drawable.ic_bubble)
            .build()
    }

    /** 開系統相片選擇器（不需儲存權限）。由長按浮動球觸發。 */
    private fun openAlbum() {
        val i = Intent(this, PickerActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        bubble?.let { runCatching { wm.removeView(it) } }
        panel?.dismiss()
        capture?.release()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "wingman"
        private const val NOTIF_ID = 1
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"

        @Volatile
        var instance: FloatingBubbleService? = null
            private set

        /** 給 PickerActivity 呼叫：OCR 相簿選的圖 → 分析 → 彈面板。 */
        fun analyzeAlbumImage(png: ByteArray) {
            instance?.analyzeAlbumBitmap(png)
        }

        fun start(ctx: Context, resultCode: Int, data: Intent) {
            val i = Intent(ctx, FloatingBubbleService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA, data)
            }
            ctx.startForegroundService(i)
        }
    }
}
