package uk.hakkaren.wingman

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * 透明的中介 Activity：開系統相片選擇器（Photo Picker，免儲存權限），
 * 讀取選到的圖 → 交給 FloatingBubbleService 分析 → 結束。
 * 由長按浮動球啟動（見 FloatingBubbleService.openAlbum）。
 */
class PickerActivity : ComponentActivity() {

    private val picker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) {
            finish()
            return@registerForActivityResult
        }

        lifecycleScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        val width = info.size.width
                        val height = info.size.height
                        val maxDimension = maxOf(width, height)
                        if (maxDimension > MAX_IMAGE_DIMENSION) {
                            val scale = MAX_IMAGE_DIMENSION.toFloat() / maxDimension
                            decoder.setTargetSize(
                                (width * scale).roundToInt().coerceAtLeast(1),
                                (height * scale).roundToInt().coerceAtLeast(1),
                            )
                        }
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                    try {
                        ByteArrayOutputStream().use { output ->
                            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                            output.toByteArray()
                        }
                    } finally {
                        bitmap.recycle()
                    }
                }
                FloatingBubbleService.analyzeAlbumImage(bytes)
            } catch (error: Exception) {
                Toast.makeText(this@PickerActivity, "讀取圖片失敗", Toast.LENGTH_SHORT).show()
            } finally {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        picker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private companion object {
        const val MAX_IMAGE_DIMENSION = 1080
    }
}
