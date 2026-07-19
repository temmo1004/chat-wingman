package uk.hakkaren.wingman

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

/**
 * 透明的中介 Activity：開系統相片選擇器（Photo Picker，免儲存權限），
 * 讀取選到的圖 → 交給 FloatingBubbleService 分析 → 結束。
 * 由長按浮動球啟動（見 FloatingBubbleService.openAlbum）。
 */
class PickerActivity : ComponentActivity() {

    private val picker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) FloatingBubbleService.analyzeAlbumImage(bytes)
            } catch (e: Exception) {
                Toast.makeText(this, "讀取圖片失敗", Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        picker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
}
