package uk.hakkaren.wingman

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.IntentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot

/**
 * 常駐前景服務：畫出可拖曳的孔明帽浮動球。點擊後截圖、在手機端 OCR，
 * 再把文字交給後端分析並顯示 Compose 面板。
 * MediaProjection 授權只由 MainActivity 傳入；服務本身不保存或要求任何 API Key。
 */
class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubble: View? = null
    private var panel: OverlayPanel? = null
    private var capture: ScreenCaptureManager? = null
    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(serviceJob + Dispatchers.Main)
    private var isAnalyzing = false
    private var analysisToken = 0L

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val data = intent?.let {
            IntentCompat.getParcelableExtra(it, EXTRA_DATA, Intent::class.java)
        }

        // Android 14 不允許 mediaProjection FGS 在沒有新授權 token 時冷啟動。
        // START_NOT_STICKY 也避免系統日後拿空 Intent 重啟而觸發 SecurityException。
        if (capture == null && (resultCode != Activity.RESULT_OK || data == null)) {
            Log.w(TAG, "Ignoring service start without a valid MediaProjection grant")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        try {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )

            // 必須先進入 mediaProjection 型前景服務，再取得 MediaProjection。
            // 每次收到新授權都替換舊 manager，讓使用者停止分享後可以重新授權。
            if (resultCode == Activity.RESULT_OK && data != null) {
                // 新授權代表上一輪已失效；一併關掉可能仍停在 Loading 的舊面板。
                panel?.dismiss()
                panel = null
                val manager = getSystemService(
                    Context.MEDIA_PROJECTION_SERVICE,
                ) as MediaProjectionManager
                val projection: MediaProjection = manager.getMediaProjection(resultCode, data)
                val previousCapture = capture
                capture = ScreenCaptureManager(projection, displayMetrics())
                previousCapture?.release()
                bubble?.visibility = View.VISIBLE
                isAnalyzing = false
                analysisToken++
            }
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unable to start MediaProjection pipeline", error)
            Toast.makeText(this, R.string.capture_service_start_failed, Toast.LENGTH_LONG).show()
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (bubble == null) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.overlay_permission_missing, Toast.LENGTH_LONG).show()
                stopSelf(startId)
                return START_NOT_STICKY
            }
            try {
                showBubble()
            } catch (error: RuntimeException) {
                Log.e(TAG, "Unable to show floating bubble", error)
                Toast.makeText(this, R.string.overlay_unavailable, Toast.LENGTH_LONG).show()
                stopSelf(startId)
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun displayMetrics(): DisplayMetrics = DisplayMetrics().also {
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay
            .getMetrics(it)
    }

    // ── 浮動球 ────────────────────────────────────────────
    private fun showBubble() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density
        val view = BubbleImageView(this).apply {
            // 透明孔明帽前景置於暖米色圓形底；素材與 Launcher adaptive icon 共用。
            setImageResource(R.drawable.ic_launcher_foreground)
            scaleType = ImageView.ScaleType.FIT_CENTER
            val iconPadding = (4 * density).toInt()
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(getColor(R.color.launcher_icon_background))
            }
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            elevation = 12f * density
            contentDescription = getString(R.string.bubble_content_description)
            isClickable = true
            isLongClickable = true
            isFocusable = true
            setOnClickListener { onBubbleTap() }
            setOnLongClickListener {
                openAlbum()
                true
            }
        }
        val bubbleSize = (BUBBLE_SIZE_DP * density).toInt()
        val edgeMargin = (BUBBLE_EDGE_MARGIN_DP * density).toInt()
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val layoutParams = WindowManager.LayoutParams(
            bubbleSize,
            bubbleSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth - bubbleSize - edgeMargin).coerceAtLeast(edgeMargin)
            y = (screenHeight * BUBBLE_INITIAL_Y_RATIO).toInt()
                .coerceIn(edgeMargin, (screenHeight - bubbleSize - edgeMargin).coerceAtLeast(edgeMargin))
        }

        // 超過 touch slop 才視為拖曳；門檻內放開則透過 performClick 觸發分析。
        var downX = 0f
        var downY = 0f
        var initialX = 0
        var initialY = 0
        var hasDragged = false
        var longPressed = false
        val touchSlop = maxOf(
            MIN_CLICK_THRESHOLD_PX,
            ViewConfiguration.get(this).scaledTouchSlop.toFloat(),
        )
        val longPress = Runnable {
            if (!hasDragged) {
                longPressed = true
                view.performLongClick()
            }
        }
        view.setOnTouchListener { touchedView, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    hasDragged = false
                    longPressed = false
                    view.postDelayed(
                        longPress,
                        ViewConfiguration.getLongPressTimeout().toLong(),
                    )
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!hasDragged && hypot(dx.toDouble(), dy.toDouble()) >= touchSlop) {
                        hasDragged = true
                        view.removeCallbacks(longPress)
                    }
                    if (hasDragged) {
                        val maxX = (resources.displayMetrics.widthPixels - bubbleSize - edgeMargin)
                            .coerceAtLeast(edgeMargin)
                        val maxY = (resources.displayMetrics.heightPixels - bubbleSize - edgeMargin)
                            .coerceAtLeast(edgeMargin)
                        layoutParams.x = (initialX + dx.toInt()).coerceIn(edgeMargin, maxX)
                        layoutParams.y = (initialY + dy.toInt()).coerceIn(edgeMargin, maxY)
                        runCatching { windowManager.updateViewLayout(view, layoutParams) }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    view.removeCallbacks(longPress)
                    val moved = abs(event.rawX - downX) + abs(event.rawY - downY)
                    if (!longPressed && !hasDragged && moved < touchSlop * 2) {
                        touchedView.performClick()
                    }
                    hasDragged = false
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    view.removeCallbacks(longPress)
                    hasDragged = false
                    longPressed = false
                    true
                }

                else -> true
            }
        }
        bubble = view
        windowManager.addView(view, layoutParams)
    }

    private fun onBubbleTap() {
        if (isAnalyzing) return

        // 已有結果時先卸下面板，避免舊內容被下一次 MediaProjection 截進去。
        panel?.dismiss()
        panel = null
        isAnalyzing = true
        val token = ++analysisToken

        // 預設展示版完全不截圖、不做網路呼叫，仍保留 Loading → 結果的完整體驗。
        if (BuildConfig.DEMO_ONLY) {
            showPanelLoading()
            analyzeText(null, token)
            return
        }

        val captureManager = capture

        if (captureManager == null) {
            showPanelLoading()
            analyzeText(null, token)
            return
        }

        // 截圖前隱藏孔明帽；取得影格後才顯示 Loading，避免覆蓋物污染截圖。
        bubble?.visibility = View.INVISIBLE
        try {
            captureManager.captureOnce { bitmap ->
                bubble?.visibility = View.VISIBLE
                if (token != analysisToken) {
                    bitmap?.recycle()
                    return@captureOnce
                }
                showPanelLoading()
                if (bitmap == null) {
                    Toast.makeText(
                        this,
                        R.string.capture_failed_using_demo,
                        Toast.LENGTH_SHORT,
                    ).show()
                    analyzeText(null, token)
                } else {
                    ocrAndAnalyze(bitmap, token)
                }
            }
        } catch (error: RuntimeException) {
            Log.e(TAG, "Screenshot request failed", error)
            bubble?.visibility = View.VISIBLE
            showPanelLoading()
            analyzeText(null, token)
        }
    }

    /** 長按浮動球時開啟系統 Photo Picker，不要求讀取整個相簿的權限。 */
    private fun openAlbum() {
        startActivity(
            Intent(this, PickerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** PickerActivity 回傳圖片後，沿用和即時截圖相同的分析與取消權杖流程。 */
    private fun analyzePickedImage(png: ByteArray) {
        if (isAnalyzing) return
        isAnalyzing = true
        val token = ++analysisToken
        showPanelLoading()

        if (BuildConfig.DEMO_ONLY) {
            analyzeText(null, token)
            return
        }

        scope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                BitmapFactory.decodeByteArray(png, 0, png.size)
            }
            if (token != analysisToken) {
                bitmap?.recycle()
                return@launch
            }
            if (bitmap == null) {
                Toast.makeText(
                    this@FloatingBubbleService,
                    R.string.image_decode_failed_using_demo,
                    Toast.LENGTH_SHORT,
                ).show()
                analyzeText(null, token)
            } else {
                ocrAndAnalyze(bitmap, token)
            }
        }
    }

    // ── 後端 + 面板 ──────────────────────────────────────
    private fun ocrAndAnalyze(bitmap: Bitmap, token: Long) {
        OcrHelper.extract(bitmap) { text ->
            if (token != analysisToken) return@extract
            val conversation = text.trim().takeIf(String::isNotEmpty)
            if (conversation == null) {
                Toast.makeText(
                    this,
                    R.string.ocr_failed_using_demo,
                    Toast.LENGTH_SHORT,
                ).show()
            }
            analyzeText(conversation, token)
        }
    }

    private fun analyzeText(text: String?, token: Long) {
        scope.launch {
            if (token != analysisToken) return@launch
            val result = withContext(Dispatchers.IO) { resolveResult(text) }
            if (token != analysisToken) return@launch
            showPanel(result)
            isAnalyzing = false
        }
    }

    /** 真實 API → 後端 demo → LocalDemo；任一錯誤只降級，不往主執行緒拋。 */
    private fun resolveResult(text: String?): WingmanResult {
        if (BuildConfig.DEMO_ONLY) return LocalDemo.result

        val conversation = text?.trim().orEmpty()
        if (conversation.isNotEmpty()) {
            runCatching { WingmanApi.analyze(conversation, demo = false) }
                .onFailure { Log.w(TAG, "Live analysis failed; trying backend demo", it) }
                .getOrNull()
                ?.let { return it }
        }

        return runCatching { WingmanApi.analyze(null, demo = true) }
            .onFailure { Log.w(TAG, "Backend demo failed; using LocalDemo", it) }
            .getOrElse { LocalDemo.result }
    }

    private fun showPanel(result: WingmanResult) {
        runCatching { ensurePanel().show(result) }
            .onFailure {
                Log.e(TAG, "Unable to show success panel", it)
                Toast.makeText(this, R.string.overlay_unavailable, Toast.LENGTH_LONG).show()
            }
    }

    private fun showPanelLoading() {
        runCatching { ensurePanel().showLoading() }
            .onFailure { Log.e(TAG, "Unable to show loading panel", it) }
    }

    private fun ensurePanel(): OverlayPanel {
        return panel ?: OverlayPanel(
            ctx = this,
            onFill = ::fillReply,
            onCopy = ::copyReply,
            onRefresh = ::refreshAnalysis,
            onDismissed = {
                panel = null
                isAnalyzing = false
                analysisToken++
            },
        ).also { panel = it }
    }

    private fun refreshAnalysis() {
        panel?.dismiss()
        panel = null
        isAnalyzing = false
        onBubbleTap()
    }

    private fun fillReply(reply: Reply) {
        val outcome = WingmanAccessibilityService.fill(reply.text)
        val message = when (outcome) {
            WingmanAccessibilityService.FillOutcome.SET_TEXT -> R.string.reply_filled
            WingmanAccessibilityService.FillOutcome.PASTED -> R.string.reply_pasted
            WingmanAccessibilityService.FillOutcome.COPIED -> R.string.reply_copied_fallback
            WingmanAccessibilityService.FillOutcome.FAILED -> {
                if (copyToClipboard(reply.text)) {
                    R.string.reply_copied_fallback
                } else {
                    R.string.reply_copy_failed
                }
            }
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        dismissPanel()
    }

    private fun copyReply(reply: Reply) {
        val copied = copyToClipboard(reply.text)
        Toast.makeText(
            this,
            if (copied) R.string.reply_copied else R.string.reply_copy_failed,
            Toast.LENGTH_SHORT,
        ).show()
        dismissPanel()
    }

    private fun copyToClipboard(text: String): Boolean = runCatching {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(getString(R.string.clipboard_label), text),
        )
    }.isSuccess

    private fun dismissPanel() {
        panel?.dismiss()
        panel = null
        isAnalyzing = false
        analysisToken++
    }

    // ── 前景通知 ─────────────────────────────────────────
    private fun buildNotification(): Notification {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
        return Notification.Builder(this, NOTIFICATION_CHANNEL)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_bubble)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        analysisToken++
        bubble?.let { runCatching { windowManager.removeView(it) } }
        panel?.dismiss()
        capture?.release()
        capture = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "FloatingBubbleService"
        private const val NOTIFICATION_CHANNEL = "wingman"
        private const val NOTIFICATION_ID = 1
        private const val BUBBLE_SIZE_DP = 56
        private const val BUBBLE_EDGE_MARGIN_DP = 18
        private const val BUBBLE_INITIAL_Y_RATIO = 0.38f
        private const val MIN_CLICK_THRESHOLD_PX = 20f
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"

        @Volatile
        var instance: FloatingBubbleService? = null
            private set

        /** 給 PickerActivity 呼叫：分析相簿選取的圖片並顯示建議面板。 */
        fun analyzeAlbumImage(png: ByteArray) {
            instance?.analyzePickedImage(png)
        }

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, FloatingBubbleService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA, data)
            }
            context.startForegroundService(intent)
        }
    }
}

/** Lint 可辨識的 click 語意；拖曳手勢在門檻內會呼叫 performClick。 */
private class BubbleImageView(context: Context) : AppCompatImageView(context) {
    override fun performClick(): Boolean = super.performClick()
}
