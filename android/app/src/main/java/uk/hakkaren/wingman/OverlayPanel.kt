package uk.hakkaren.wingman

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 球展開的回覆面板：聊死指數 + 三張風格卡片。點卡片回呼 onPick。
 * 用 classic Views（overlay 內 Compose 生命週期較麻煩，骨架先求穩）。
 */
class OverlayPanel(
    private val ctx: Context,
    private val onPick: (Reply) -> Unit,
) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: View? = null

    fun show(result: WingmanResult) {
        val view = LayoutInflater.from(ctx).inflate(R.layout.panel, null)

        view.findViewById<TextView>(R.id.deathIndex).text =
            "聊死指數 ${result.chatDeathIndex}"
        view.findViewById<TextView>(R.id.context).text = result.context

        val list = view.findViewById<LinearLayout>(R.id.cards)
        result.replies.forEach { reply ->
            val card = LayoutInflater.from(ctx).inflate(R.layout.reply_card, list, false)
            card.findViewById<TextView>(R.id.style).text = reply.style
            card.findViewById<TextView>(R.id.text).text = reply.text
            card.findViewById<TextView>(R.id.why).text = "軍師：${reply.why}"
            card.setOnClickListener { onPick(reply) }
            list.addView(card)
        }
        view.findViewById<View>(R.id.close).setOnClickListener { dismiss() }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.BOTTOM }

        root = view
        wm.addView(view, lp)
    }

    fun dismiss() {
        root?.let { runCatching { wm.removeView(it) } }
        root = null
    }
}
