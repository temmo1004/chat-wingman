package uk.hakkaren.wingman

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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

        // 從 MainActivity 拿 MediaProjection 授權結果，建立本次唯一的擷取 session。
        if (intent != null) {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
            val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
            if (resultCode != 0 && data != null) {
                runCatching {
                    val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    val projection: MediaProjection = manager.getMediaProjection(resultCode, data)
                    replaceCaptureSession(projection)
                }.onFailure {
                    CaptureSessionStatus.setActive(false)
                    Toast.makeText(this, "螢幕擷取啟動失敗，請重新授權", Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (bubble == null && Settings.canDrawOverlays(this)) showBubble()
        return START_STICKY
    }

    private fun replaceCaptureSession(projection: MediaProjection) {
        lateinit var nextCapture: ScreenCaptureManager
        nextCapture = ScreenCaptureManager(
            projection = projection,
            metrics = displayMetrics(),
            onProjectionStopped = {
                if (capture === nextCapture) {
                    capture = null
                    CaptureSessionStatus.setActive(false)
                }
            },
        )
        val previousCapture = capture
        capture = nextCapture
        CaptureSessionStatus.setActive(true)
        previousCapture?.release()
    }

    private fun displayMetrics(): DisplayMetrics =
        DisplayMetrics().also {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(it)
        }

    // ── 浮動球 ────────────────────────────────────────────
    private fun showBubble() {
        if (!Settings.canDrawOverlays(this)) return
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = ImageView(this).apply {
            setImageResource(R.drawable.brand_kongming_hat)
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
            x = (24 * resources.displayMetrics.density).toInt()
            y = (240 * resources.displayMetrics.density).toInt()
        }

        // 拖曳 + 短按截圖 + 長按系統相簿判定
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var downX = 0f; var downY = 0f; var lpX = 0; var lpY = 0
        var dragged = false
        var longPressed = false
        val longPress = Runnable {
            longPressed = true
            openAlbum()
        }
        view.setOnClickListener { onBubbleTap() }
        view.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY; lpX = lp.x; lpY = lp.y
                    dragged = false
                    longPressed = false
                    view.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val moved = abs(e.rawX - downX) + abs(e.rawY - downY)
                    if (moved > touchSlop) {
                        dragged = true
                        view.removeCallbacks(longPress)
                    }
                    val positionBounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val metrics = wm.currentWindowMetrics
                        val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                            android.view.WindowInsets.Type.systemBars() or
                                android.view.WindowInsets.Type.displayCutout(),
                        )
                        android.graphics.Rect(
                            insets.left,
                            insets.top,
                            metrics.bounds.width() - insets.right,
                            metrics.bounds.height() - insets.bottom,
                        )
                    } else {
                        displayMetrics().run { android.graphics.Rect(0, 0, widthPixels, heightPixels) }
                    }
                    val minX = positionBounds.left
                    val minY = positionBounds.top
                    val maxX = (positionBounds.right - bubbleSize).coerceAtLeast(minX)
                    val maxY = (positionBounds.bottom - bubbleSize).coerceAtLeast(minY)
                    lp.x = (lpX + (e.rawX - downX).toInt()).coerceIn(minX, maxX)
                    lp.y = (lpY + (e.rawY - downY).toInt()).coerceIn(minY, maxY)
                    wm.updateViewLayout(view, lp); true
                }
                MotionEvent.ACTION_UP -> {
                    view.removeCallbacks(longPress)
                    val moved = abs(e.rawX - downX) + abs(e.rawY - downY)
                    if (moved > touchSlop) dragged = true
                    if (!longPressed && !dragged) view.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.removeCallbacks(longPress)
                    true
                }
                else -> false
            }
        }
        bubble = view
        wm.addView(view, lp)
    }

    private fun onBubbleTap() {
        val cap = capture
        if (cap == null) {
            // 沒有截圖授權 → 走 demo，讓流程仍可 demo
            analyze(null, demo = true)
            return
        }
        // 收球一下避免截到自己（可選：暫時隱藏 bubble）
        bubble?.visibility = View.INVISIBLE
        cap.captureOnce { bmp ->
            bubble?.visibility = View.VISIBLE
            if (bmp == null) {
                Toast.makeText(this, "截圖失敗，改用範例", Toast.LENGTH_SHORT).show()
                analyze(null, demo = true)
            } else {
                val png = ByteArrayOutputStream().use {
                    bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, it); it.toByteArray()
                }
                analyze(png, demo = false)
            }
        }
    }

    // ── 打後端 + 顯示面板 ─────────────────────────────────
    private fun analyze(png: ByteArray?, demo: Boolean) {
        // DEMO_ONLY：完全不碰網路，直接用本地範本（後端未部署時 demo 用）
        if (BuildConfig.DEMO_ONLY) {
            showPanelLoading()
            scope.launch {
                delay(DEMO_LOADING_DELAY_MS)
                showPanel(LocalDemo.result)
            }
            return
        }
        showPanelLoading()
        scope.launch {
            val result = try {
                withContext(Dispatchers.IO) { WingmanApi.analyze(png, demo) }
            } catch (e: Exception) {
                // 失敗保底：再試後端 demo 模式，再不行退本地範本，斷網也能演
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

    /** 長按浮動球時開啟免儲存權限的 Android 系統 Photo Picker。 */
    private fun openAlbum() {
        startActivity(
            Intent(this, PickerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
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
            .setSmallIcon(R.drawable.ic_stat_kongming_hat)
            .build()
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
        capture = null
        CaptureSessionStatus.setActive(false)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "wingman"
        private const val NOTIF_ID = 1
        private const val DEMO_LOADING_DELAY_MS = 600L
        private const val ACTION_SHOW_BUBBLE = "uk.hakkaren.wingman.action.SHOW_BUBBLE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"

        @Volatile
        private var instance: FloatingBubbleService? = null

        /** 由透明 PickerActivity 把使用者選到的聊天截圖送回現有分析面板。 */
        fun analyzeAlbumImage(png: ByteArray) {
            instance?.analyze(png, demo = false)
        }

        fun start(ctx: Context, resultCode: Int, data: Intent) {
            val i = Intent(ctx, FloatingBubbleService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA, data)
            }
            ctx.startForegroundService(i)
        }

        fun showBubble(ctx: Context) {
            val intent = Intent(ctx, FloatingBubbleService::class.java).apply {
                action = ACTION_SHOW_BUBBLE
            }
            ctx.startForegroundService(intent)
        }
    }
}
