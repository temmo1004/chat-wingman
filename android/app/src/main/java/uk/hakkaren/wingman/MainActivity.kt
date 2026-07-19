package uk.hakkaren.wingman

import android.content.ComponentName
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.hakkaren.wingman.ui.PermissionUiState
import uk.hakkaren.wingman.ui.WingmanHomeScreen
import uk.hakkaren.wingman.ui.WingmanTheme

/**
 * App Store 風格主頁＋三項系統能力入口。
 * 浮動球、截圖與填字仍由既有 Service / AccessibilityService 負責。
 */
class MainActivity : ComponentActivity() {

    private var overlayGranted by mutableStateOf(false)
    private var accessibilityGranted by mutableStateOf(false)

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            FloatingBubbleService.start(this, result.resultCode, data)
            val message = if (Settings.canDrawOverlays(this)) {
                "孔明帽軍師已浮在畫面上"
            } else {
                "螢幕擷取已授權；開啟上層顯示後即可使用"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "未授權螢幕擷取，尚未啟動浮動球", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        refreshPermissionState()
        setContent {
            val captureGranted by CaptureSessionStatus.active.collectAsStateWithLifecycle()
            WingmanTheme {
                WingmanHomeScreen(
                    permissionState = PermissionUiState(
                        overlayGranted = overlayGranted,
                        captureGranted = captureGranted,
                        accessibilityGranted = accessibilityGranted,
                    ),
                    onTestBubble = ::startWingmanFlow,
                    onOverlayPermission = ::openOverlayPermission,
                    onCapturePermission = ::handleCapturePermission,
                    onAccessibilityPermission = ::openAccessibilitySettings,
                    onUnavailableTab = { tab ->
                        Toast.makeText(this, "$tab 將在下一版開放", Toast.LENGTH_SHORT).show()
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
        if (CaptureSessionStatus.active.value) {
            FloatingBubbleService.showBubble(this)
            Toast.makeText(this, "浮動球已啟動，切回聊天畫面即可使用", Toast.LENGTH_SHORT).show()
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
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun handleCapturePermission() {
        if (CaptureSessionStatus.active.value) {
            Toast.makeText(this, "本次螢幕擷取已授權", Toast.LENGTH_SHORT).show()
        } else {
            requestScreenCapture()
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, WingmanAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabled
            .split(':')
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it == expected }
    }
}
