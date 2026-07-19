package uk.hakkaren.wingman

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/**
 * 定位目前聊天輸入框並填字。策略是語意查找可編輯節點，不依賴座標或易變的 viewId。
 * 只執行 SET_TEXT / PASTE，絕不尋找或點擊送出按鈕。
 */
class WingmanAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 被動服務：只有使用者按面板「填入」時才查詢目前視窗。
    }

    override fun onInterrupt() = Unit

    /** ACTION_SET_TEXT → Clipboard + ACTION_PASTE → Clipboard-only。 */
    fun fillReply(text: String): FillOutcome {
        val field = rootInActiveWindow?.let(::findEditable)

        if (field != null) {
            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text,
                )
            }
            val set = runCatching {
                field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }.getOrDefault(false)
            if (set) return FillOutcome.SET_TEXT
        }

        // SET_TEXT 失敗或找不到輸入框時，仍先完成剪貼簿備援。
        if (!copyToClipboard(text)) return FillOutcome.FAILED
        if (field == null) return FillOutcome.COPIED

        runCatching { field.performAction(AccessibilityNodeInfo.ACTION_FOCUS) }
        val pasted = runCatching {
            field.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }.getOrDefault(false)
        return if (pasted) FillOutcome.PASTED else FillOutcome.COPIED
    }

    private fun copyToClipboard(text: String): Boolean = runCatching {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(getString(R.string.clipboard_label), text),
        )
    }.isSuccess

    /**
     * 優先目前聚焦的輸入框；否則取可見且面積最大的可編輯節點，
     * 避免部分聊天 App 內的微型隱藏 EditText decoy 被誤選。
     */
    private fun findEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)
        val candidates = ArrayList<AccessibilityNodeInfo>()

        while (pending.isNotEmpty()) {
            val node = pending.removeFirst()
            val supportsSetText = node.actionList.any {
                it.id == AccessibilityNodeInfo.ACTION_SET_TEXT
            }
            val editTextClass = node.className
                ?.toString()
                ?.contains("EditText", ignoreCase = true) == true
            val candidate = node.isEditable && node.isEnabled && node.isVisibleToUser &&
                (supportsSetText || editTextClass)
            if (candidate) {
                if (node.isFocused || node.isAccessibilityFocused) return node
                candidates.add(node)
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(pending::addLast)
            }
        }
        return candidates.maxByOrNull { node ->
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            bounds.width().toLong() * bounds.height().toLong()
        }
    }

    enum class FillOutcome {
        SET_TEXT,
        PASTED,
        COPIED,
        FAILED,
    }

    companion object {
        @Volatile
        var instance: WingmanAccessibilityService? = null
            private set

        /** 未啟用服務時回 FAILED，由呼叫端完成 Clipboard-only 備援。 */
        fun fill(text: String): FillOutcome = instance?.fillReply(text) ?: FillOutcome.FAILED
    }
}
