package uk.hakkaren.wingman

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import java.io.ByteArrayOutputStream

/**
 * 透明中介 Activity：啟動 Android 系統 Photo Picker（免儲存權限），
 * 將選到的聊天截圖交給現有分析流程後立即結束。
 */
class PickerActivity : ComponentActivity() {

    private val picker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            runCatching { readImageBytes(uri) }.onSuccess { bytes ->
                if (bytes != null) FloatingBubbleService.analyzeAlbumImage(bytes)
            }.onFailure {
                val message = if (it is ImageTooLargeException) "圖片過大，請選擇 20 MB 以下的圖片" else "讀取圖片失敗"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        picker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    private fun readImageBytes(uri: Uri): ByteArray? {
        return contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > MAX_IMAGE_BYTES) throw ImageTooLargeException()
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    }

    private class ImageTooLargeException : IllegalArgumentException()

    private companion object {
        const val MAX_IMAGE_BYTES = 20 * 1024 * 1024
    }
}
