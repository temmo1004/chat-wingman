package uk.hakkaren.wingman

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import uk.hakkaren.wingman.ui.OverlayUiState
import uk.hakkaren.wingman.ui.WingmanOverlayPanel
import uk.hakkaren.wingman.ui.WingmanTheme

/**
 * 真實浮動面板的 Compose host。FLAG_NOT_FOCUSABLE 讓 Accessibility 仍能取得聊天 App。
 */
class OverlayPanel(
    private val ctx: Context,
    private val onFill: (Reply) -> Unit,
    private val onCopy: (Reply) -> Unit,
    private val onRefresh: () -> Unit,
    private val onDismissed: () -> Unit,
) {
    private var state by mutableStateOf<OverlayUiState>(OverlayUiState.Loading)
    private var host: OverlayComposeHost? = null

    fun showLoading() {
        state = OverlayUiState.Loading
        ensureShown()
    }

    fun show(result: WingmanResult) {
        state = OverlayUiState.Success(result)
        ensureShown()
    }

    fun showError(message: String) {
        state = OverlayUiState.Error(message)
        ensureShown()
    }

    private fun ensureShown() {
        if (host != null) return
        host = OverlayComposeHost(ctx).also { overlayHost ->
            overlayHost.show {
                WingmanTheme {
                    WingmanOverlayPanel(
                        state = state,
                        onFill = onFill,
                        onCopy = onCopy,
                        onDismiss = ::dismissFromUi,
                        onRefresh = onRefresh,
                    )
                }
            }
        }
    }

    private fun dismissFromUi() {
        dismiss()
        onDismissed()
    }

    fun dismiss() {
        host?.destroy()
        host = null
    }
}
