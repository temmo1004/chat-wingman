package uk.hakkaren.wingman

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 打後端 POST /api/wingman。送 OCR 出的「對話文字」（不送圖，圖在手機端 OCR）。
 * 呼叫端請在 IO thread / coroutine 執行（同步阻塞）。
 */
object WingmanApi {

    private val client = OkHttpClient.Builder()
        .callTimeout(35, TimeUnit.SECONDS)
        .build()

    /** @param text OCR 出的對話文字；demo=true 時後端回寫死範本（斷網可演）。 */
    fun analyze(text: String?, demo: Boolean = false): WingmanResult {
        val json = JSONObject()
        if (demo) json.put("demo", true) else json.put("text", text.orEmpty())
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("${BuildConfig.BACKEND_URL}/api/wingman")
            .post(body)
            .build()

        client.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("HTTP ${resp.code}: $raw")
            return parse(raw)
        }
    }

    private fun parse(raw: String): WingmanResult {
        val obj = JSONObject(raw)
        val arr = obj.getJSONArray("replies")
        val replies = ArrayList<Reply>(arr.length())
        for (i in 0 until arr.length()) {
            val r = arr.getJSONObject(i)
            replies.add(
                Reply(
                    style = r.optString("style"),
                    text = r.optString("text"),
                    why = r.optString("why"),
                )
            )
        }
        return WingmanResult(
            chatDeathIndex = obj.optInt("chat_death_index", 50),
            context = obj.optString("context"),
            replies = replies,
        )
    }
}
