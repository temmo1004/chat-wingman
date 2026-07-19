package uk.hakkaren.wingman

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 本次 app process 內的 MediaProjection session 狀態。 */
object CaptureSessionStatus {
    private val _active = MutableStateFlow(false)
    val active = _active.asStateFlow()

    fun setActive(active: Boolean) {
        _active.value = active
    }
}
