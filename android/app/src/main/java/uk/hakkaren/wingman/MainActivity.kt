package uk.hakkaren.wingman

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * 設定精靈：引導開三個權限，然後啟動浮動球。
 * 1) 顯示在其他 app 上層（overlay）  2) 無障礙服務（填字）  3) 螢幕擷取授權
 */
class MainActivity : AppCompatActivity() {

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val data = res.data
        if (res.resultCode == RESULT_OK && data != null) {
            FloatingBubbleService.start(this, res.resultCode, data)
            Toast.makeText(this, "軍師啟動！點浮動球試試", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "未授權螢幕擷取，將以 demo 模式運作", Toast.LENGTH_LONG).show()
            // 仍啟動服務（無截圖 → demo 流程）
            startForegroundService(Intent(this, FloatingBubbleService::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    private fun buildUi(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(48, 96, 48, 48)

        addButton("1. 開啟『顯示在其他 app 上層』") {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"),
                )
            )
        }
        addButton("2. 開啟無障礙服務（填字用）") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        addButton("3. 啟動軍師（授權螢幕擷取）") {
            if (!Settings.canDrawOverlays(this@MainActivity)) {
                Toast.makeText(this@MainActivity, "請先完成步驟 1", Toast.LENGTH_SHORT).show()
                return@addButton
            }
            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projectionLauncher.launch(mpm.createScreenCaptureIntent())
        }
    }

    private fun LinearLayout.addButton(label: String, onClick: () -> Unit) {
        addView(Button(this@MainActivity).apply {
            text = label
            setOnClickListener { onClick() }
        })
    }
}
