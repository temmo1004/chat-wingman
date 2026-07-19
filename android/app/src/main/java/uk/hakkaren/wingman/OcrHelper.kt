package uk.hakkaren.wingman

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions

/**
 * 手機端 OCR：把聊天截圖辨識成對話文字（免上傳圖片、離線、中文）。
 * 用文字塊的左右位置推斷是誰說的：靠右=我、靠左=對方（聊天氣泡慣例）。
 */
object OcrHelper {

    private val recognizer =
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    /** 辨識後在主執行緒回呼組好的對話文字（每行前綴 對方: / 我:）。 */
    fun extract(bitmap: Bitmap, onResult: (String) -> Unit) {
        val width = bitmap.width
        val task = try {
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
        } catch (error: RuntimeException) {
            Log.w(TAG, "Unable to start OCR", error)
            if (!bitmap.isRecycled) bitmap.recycle()
            onResult("")
            return
        }
        task
            .addOnSuccessListener { visionText ->
                val sb = StringBuilder()
                val blocks = visionText.textBlocks.sortedWith(
                    compareBy(
                        { it.boundingBox?.top ?: Int.MAX_VALUE },
                        { it.boundingBox?.left ?: Int.MAX_VALUE },
                    ),
                )
                for (block in blocks) {
                    val content = block.text
                        .lineSequence()
                        .joinToString(" ") { it.trim() }
                        .trim()
                    if (content.isEmpty()) continue
                    val box = block.boundingBox
                    val prefix = when {
                        box == null -> ""
                        box.centerX() > width * 0.55 -> "我: "
                        box.centerX() < width * 0.45 -> "對方: "
                        else -> ""
                    }
                    sb.append(prefix).append(content).append("\n")
                }
                val out = sb.toString().trim()
                // 不把聊天內容寫入 log；只保留排查流程所需的長度資訊。
                Log.d(TAG, "OCR completed: ${out.length} chars")
                onResult(out)
            }
            .addOnFailureListener {
                Log.w(TAG, "OCR failed", it)
                onResult("")
            }
            .addOnCompleteListener {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
    }

    private const val TAG = "Wingman-OCR"
}
