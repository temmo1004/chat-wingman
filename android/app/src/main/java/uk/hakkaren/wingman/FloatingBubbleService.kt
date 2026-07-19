package uk.hakkaren.wingman

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
            setImageResource(R.drawable.ic_bubble)
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24; y = 300
        }

        // 拖曳 + 點擊判定
        var downX = 0f; var downY = 0f; var lpX = 0; var lpY = 0
        view.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY; lpX = lp.x; lpY = lp.y; true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = lpX + (e.rawX - downX).toInt()
                    lp.y = lpY + (e.rawY - downY).toInt()
                    wm.updateViewLayout(view, lp); true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = abs(e.rawX - downX) + abs(e.rawY - downY)
                    if (moved < 20) onBubbleTap()  // 幾乎沒移動 → 當作點擊
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
            showPanel(LocalDemo.result)
            return
        }
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
        panel?.dismiss()
        panel = OverlayPanel(this) { reply ->
            // 點卡片：優先用無障礙填字，沒開就退回複製到剪貼簿
            val filled = WingmanAccessibilityService.fill(reply.text)
            if (!filled) {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("wingman", reply.text))
                Toast.makeText(this, "已複製，回聊天長按貼上", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "已填入輸入框，按送出即可", Toast.LENGTH_SHORT).show()
            }
            panel?.dismiss()
        }.also { it.show(result) }
    }

    // ── 前景通知 ─────────────────────────────────────────
    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL, "聊天軍師", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("聊天軍師運作中")
            .setContentText("點浮動球取得回覆建議")
            .setSmallIcon(R.drawable.ic_bubble)
            .build()
    }

    override fun onDestroy() {
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

        fun start(ctx: Context, resultCode: Int, data: Intent) {
            val i = Intent(ctx, FloatingBubbleService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA, data)
            }
            ctx.startForegroundService(i)
        }
    }
}
