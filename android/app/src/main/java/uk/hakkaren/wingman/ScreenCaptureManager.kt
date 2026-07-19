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
 * 在同一個 MediaProjection session 上保留一個 VirtualDisplay，按需擷取單幀。
 * Android 14 起同一個 MediaProjection 只能建立一次 VirtualDisplay，因此不能每次點球重建。
 */
class ScreenCaptureManager(
    private val projection: MediaProjection,
    private val metrics: DisplayMetrics,
    private val onProjectionStopped: () -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var captureInProgress = false
    private var stopped = false
    private var stopNotified = false

    init {
        projection.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                    handleProjectionStopped()
                }
            },
            handler,
        )
        createCaptureSurface()
    }

    private fun createCaptureSurface() {
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader
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
    }

    /** 擷取一張畫面；重複點擊時沿用同一個 VirtualDisplay。 */
    fun captureOnce(onResult: (Bitmap?) -> Unit) {
        val reader = imageReader
        if (stopped || reader == null || captureInProgress) {
            handler.post { onResult(null) }
            return
        }

        runCatching { reader.acquireLatestImage()?.close() }
        captureInProgress = true
        reader.setOnImageAvailableListener({ availableReader ->
            val image = availableReader.acquireLatestImage()
                ?: return@setOnImageAvailableListener
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val bitmap = try {
                val plane = image.planes[0]
                val rowPadding = plane.rowStride - plane.pixelStride * width
                val padded = Bitmap.createBitmap(
                    width + rowPadding / plane.pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888,
                )
                padded.copyPixelsFromBuffer(plane.buffer)
                Bitmap.createBitmap(padded, 0, 0, width, height).also {
                    if (it !== padded) padded.recycle()
                }
            } catch (_: Exception) {
                null
            } finally {
                image.close()
            }

            availableReader.setOnImageAvailableListener(null, null)
            captureInProgress = false
            handler.post { onResult(bitmap) }
        }, handler)
    }

    private fun handleProjectionStopped() {
        if (stopNotified) return
        stopNotified = true
        stopped = true
        captureInProgress = false
        teardown()
        onProjectionStopped()
    }

    private fun teardown() {
        imageReader?.setOnImageAvailableListener(null, null)
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
    }

    fun release() {
        if (!stopped) {
            runCatching { projection.stop() }
        }
        handleProjectionStopped()
    }
}
