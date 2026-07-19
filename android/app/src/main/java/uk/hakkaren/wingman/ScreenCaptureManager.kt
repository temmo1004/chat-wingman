package uk.hakkaren.wingman

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log

/**
 * 用 MediaProjection 擷取單張畫面。
 *
 * Android 14 的同一份使用者授權只允許呼叫 createVirtualDisplay 一次，因此這個類別
 * 會保留 VirtualDisplay，單次擷取後只卸下 Surface；refresh 時再掛回相同 Surface。
 */
class ScreenCaptureManager(
    private val projection: MediaProjection,
    private val metrics: DisplayMetrics,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var pendingResult: ((Bitmap?) -> Unit)? = null
    private var timeout: Runnable? = null
    private var projectionStopped = false
    private var released = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            projectionStopped = true
            completeCapture(null)
            releaseDisplay()
        }
    }

    init {
        // targetSdk 34 必須在建立 VirtualDisplay 前註冊 callback。
        projection.registerCallback(projectionCallback, handler)
    }

    /** 擷取一張畫面；onResult 保證在主執行緒回呼，失敗或逾時回 null。 */
    fun captureOnce(onResult: (Bitmap?) -> Unit) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { captureOnce(onResult) }
            return
        }
        if (released || projectionStopped || pendingResult != null) {
            onResult(null)
            return
        }

        pendingResult = onResult
        timeout = Runnable {
            Log.w(TAG, "Timed out waiting for a MediaProjection frame")
            completeCapture(null)
        }.also { handler.postDelayed(it, CAPTURE_TIMEOUT_MS) }

        try {
            ensureDisplay()
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unable to create or resume VirtualDisplay", error)
            completeCapture(null)
        }
    }

    private fun ensureDisplay() {
        check(!released && !projectionStopped) { "MediaProjection is no longer available" }

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val reader = imageReader ?: ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            MAX_IMAGES,
        ).also { imageReader = it }

        reader.setOnImageAvailableListener({ source ->
            val image = try {
                source.acquireLatestImage()
            } catch (error: RuntimeException) {
                Log.w(TAG, "ImageReader closed before frame delivery", error)
                completeCapture(null)
                return@setOnImageAvailableListener
            } ?: return@setOnImageAvailableListener
            val bitmap = try {
                imageToBitmap(image, width, height)
            } finally {
                image.close()
            }
            completeCapture(bitmap)
        }, handler)

        val display = virtualDisplay
        if (display == null) {
            virtualDisplay = projection.createVirtualDisplay(
                "wingman-capture",
                width,
                height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler,
            )
        } else {
            display.setSurface(reader.surface)
        }
    }

    private fun imageToBitmap(image: Image, width: Int, height: Int): Bitmap? {
        return try {
            val plane = image.planes.firstOrNull() ?: return null
            val pixelStride = plane.pixelStride
            if (pixelStride <= 0) return null
            val rowPadding = plane.rowStride - pixelStride * width
            val paddedWidth = width + rowPadding.coerceAtLeast(0) / pixelStride
            val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
            plane.buffer.rewind()
            padded.copyPixelsFromBuffer(plane.buffer)
            if (paddedWidth == width) {
                padded
            } else {
                Bitmap.createBitmap(padded, 0, 0, width, height).also { padded.recycle() }
            }
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unable to copy MediaProjection image", error)
            null
        }
    }

    private fun completeCapture(bitmap: Bitmap?) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { completeCapture(bitmap) }
            return
        }
        val callback = pendingResult ?: run {
            bitmap?.recycle()
            return
        }
        pendingResult = null
        timeout?.let(handler::removeCallbacks)
        timeout = null
        pauseDisplay()
        callback(bitmap)
    }

    /** 停止產生 frame，但不 release VirtualDisplay，讓同一份 Android 14 grant 可 refresh。 */
    private fun pauseDisplay() {
        imageReader?.setOnImageAvailableListener(null, null)
        runCatching { virtualDisplay?.setSurface(null) }
        runCatching { imageReader?.acquireLatestImage()?.close() }
    }

    private fun releaseDisplay() {
        imageReader?.setOnImageAvailableListener(null, null)
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
    }

    fun release() {
        if (released) return
        released = true
        timeout?.let(handler::removeCallbacks)
        timeout = null
        pendingResult = null
        releaseDisplay()
        runCatching { projection.unregisterCallback(projectionCallback) }
        runCatching { projection.stop() }
    }

    private companion object {
        const val TAG = "ScreenCaptureManager"
        const val MAX_IMAGES = 2
        const val CAPTURE_TIMEOUT_MS = 3_000L
    }
}
