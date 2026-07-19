package uk.hakkaren.wingman

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics

/**
 * 用 MediaProjection 抓一張當前螢幕。單次擷取 → 回 Bitmap。
 *
 * 注意（Android 14）：
 * - 必須先啟動 foregroundServiceType=mediaProjection 的前景服務，才能 getMediaProjection()
 * - 每次取得投影都要重新經使用者同意（見 MainActivity 流程）
 * - 用完記得 release()
 */
class ScreenCaptureManager(
    private val projection: MediaProjection,
    private val metrics: DisplayMetrics,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    init {
        // Android 14+ 要求註冊 callback，否則 getMediaProjection 之後會擲例外
        projection.registerCallback(object : MediaProjection.Callback() {}, handler)
    }

    /** 擷取一張畫面。onResult 在主執行緒回呼。 */
    fun captureOnce(onResult: (Bitmap?) -> Unit) {
        val w = metrics.widthPixels
        val h = metrics.heightPixels
        val reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        virtualDisplay = projection.createVirtualDisplay(
            "wingman-capture",
            w, h, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, handler,
        )

        reader.setOnImageAvailableListener({ r ->
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            val bitmap = try {
                val plane = image.planes[0]
                val rowPadding = plane.rowStride - plane.pixelStride * w
                val bmp = Bitmap.createBitmap(
                    w + rowPadding / plane.pixelStride, h, Bitmap.Config.ARGB_8888,
                )
                bmp.copyPixelsFromBuffer(plane.buffer)
                Bitmap.createBitmap(bmp, 0, 0, w, h) // 裁掉 row padding
            } catch (e: Exception) {
                null
            } finally {
                image.close()
            }
            // 一次性：抓到就收工，避免連續回呼
            r.setOnImageAvailableListener(null, null)
            handler.post { onResult(bitmap); teardown() }
        }, handler)
    }

    private fun teardown() {
        virtualDisplay?.release(); virtualDisplay = null
        imageReader?.close(); imageReader = null
    }

    fun release() {
        teardown()
        projection.stop()
    }
}
