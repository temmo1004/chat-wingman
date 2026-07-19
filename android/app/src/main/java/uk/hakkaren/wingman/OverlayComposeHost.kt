package uk.hakkaren.wingman

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/** 讓 Service 的 WindowManager overlay 擁有 Compose 所需的三個 ViewTree owner。 */
class OverlayComposeHost(
    private val context: Context,
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this).apply {
        performAttach()
        performRestore(null)
    }
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun show(content: @Composable () -> Unit) {
        if (composeView != null) return
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@OverlayComposeHost)
            setViewTreeViewModelStoreOwner(this@OverlayComposeHost)
            setViewTreeSavedStateRegistryOwner(this@OverlayComposeHost)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent(content)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM
        }
        try {
            windowManager.addView(view, params)
            composeView = view
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        } catch (error: Throwable) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            view.disposeComposition()
            store.clear()
            throw error
        }
    }

    fun destroy() {
        val view = composeView
        composeView = null
        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
        if (view != null) {
            runCatching { windowManager.removeView(view) }
        }
        store.clear()
    }
}
