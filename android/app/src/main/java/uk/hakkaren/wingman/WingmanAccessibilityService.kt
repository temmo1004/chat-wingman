package uk.hakkaren.wingman

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 定位聊天輸入框並填字。策略：語意查找（可編輯 EditText）而非寫死座標/viewId
 * （研究報告 III：viewId 會隨版本改，語意查找較穩）。
 *
 * 填字優先 ACTION_SET_TEXT；失敗（如 Compose 自繪輸入框會靜默拒絕）退 ACTION_PASTE。
 * 送出交給使用者手按——不自動送，避開 Google Play 自動操作紅線。
 */
class WingmanAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* 被動查找，不需常駐監聽 */ }
    override fun onInterrupt() {}

    /** 把回覆填進當前聊天輸入框。回傳是否成功。 */
    fun fillReply(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val field = findEditable(root) ?: return false

        // 1) 優先 ACTION_SET_TEXT
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        if (field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return true

        // 2) 退回剪貼簿 + ACTION_PASTE（相容性較高；target 12+ 剪貼簿讀取受限，安全）
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("wingman", text))
        field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        return field.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }

    /** 深度優先找第一個可編輯節點（通常就是聊天輸入框）。 */
    private fun findEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable && node.isEnabled) return node
        for (i in 0 until node.childCount) {
            findEditable(node.getChild(i))?.let { return it }
        }
        return null
    }

    companion object {
        @Volatile
        var instance: WingmanAccessibilityService? = null
            private set

        /** 給浮動球面板呼叫：無障礙服務沒開時回 false，呼叫端退回「複製到剪貼簿」。 */
        fun fill(text: String): Boolean = instance?.fillReply(text) ?: false
    }
}
