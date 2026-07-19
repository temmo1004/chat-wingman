package uk.hakkaren.wingman

import android.content.ComponentName
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import uk.hakkaren.wingman.ui.PermissionUiState
import uk.hakkaren.wingman.ui.WingmanHomeScreen
import uk.hakkaren.wingman.ui.WingmanTheme

/**
 * App Store 風格主頁＋三項系統能力入口。
 * 浮動球、截圖與填字仍由既有 Service / AccessibilityService 負責。
 */
class MainActivity : ComponentActivity() {

    private var overlayGranted by mutableStateOf(false)
    private var captureGranted by mutableStateOf(false)
    private var accessibilityGranted by mutableStateOf(false)

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            captureGranted = true
            FloatingBubbleService.start(this, result.resultCode, data)
            Toast.makeText(this, "孔明帽軍師已浮在畫面上", Toast.LENGTH_SHORT).show()
        } else {
            captureGranted = false
            Toast.makeText(this, "未授權螢幕擷取，尚未啟動浮動球", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshPermissionState()
        setContent {
            WingmanTheme {
                WingmanHomeScreen(
                    permissionState = PermissionUiState(
                        overlayGranted = overlayGranted,
                        captureGranted = captureGranted,
                        accessibilityGranted = accessibilityGranted,
                    ),
                    onTestBubble = ::startWingmanFlow,
                    onOverlayPermission = ::openOverlayPermission,
                    onCapturePermission = ::requestScreenCapture,
                    onAccessibilityPermission = ::openAccessibilitySettings,
                    onUnavailableTab = { tab ->
                        Toast.makeText(this, "${tab}將在下一版開放", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
    }

    private fun refreshPermissionState() {
        overlayGranted = Settings.canDrawOverlays(this)
        accessibilityGranted = isAccessibilityServiceEnabled()
    }

    private fun startWingmanFlow() {
        if (!overlayGranted) {
            Toast.makeText(this, "先開啟顯示在其他 App 上層", Toast.LENGTH_SHORT).show()
            openOverlayPermission()
            return
        }
        requestScreenCapture()
    }

    private fun openOverlayPermission() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun requestScreenCapture() {
        if (!Settings.canDrawOverlays(this)) {
            openOverlayPermission()
            return
        }
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, WingmanAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
