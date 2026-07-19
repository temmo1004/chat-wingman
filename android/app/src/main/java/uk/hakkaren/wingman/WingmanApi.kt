package uk.hakkaren.wingman

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * POST /api/wingman。客戶端只送 multipart 截圖與 locale，API Key 永遠留在後端。
 * 呼叫端須在 IO dispatcher 執行；所有 HTTP/解析例外由服務的降級鏈接住。
 */
object WingmanApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(35, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .build()

    /** @param png 截圖 PNG bytes；demo=true 時請後端回固定合約範本。 */
    fun analyze(png: ByteArray?, demo: Boolean = false): WingmanResult {
        val form = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("locale", "zh-TW")
        if (demo) {
            form.addFormDataPart("demo", "true")
        } else {
            val image = requireNotNull(png) { "png required when demo=false" }
            form.addFormDataPart(
                "image",
                "screen.png",
                image.toRequestBody("image/png".toMediaType()),
            )
        }

        val request = Request.Builder()
            .url("${BuildConfig.BACKEND_URL.trimEnd('/')}/api/wingman")
            .post(form.build())
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $raw")
            return parse(raw)
        }
    }

    /** 缺欄位或 replies 為空時，用 LocalDemo 同語氣欄位補齊，固定回三筆。 */
    private fun parse(raw: String): WingmanResult {
        if (raw.isBlank()) throw IOException("empty response body")
        val json = JSONObject(raw)
        json.safeString("error").takeIf(String::isNotBlank)?.let {
            throw IOException("backend error: $it")
        }

        val incoming = json.optJSONArray("replies") ?: JSONArray()
        val byStyle = mutableMapOf<String, JSONObject>()
        val unstyledByPosition = mutableMapOf<Int, JSONObject>()
        for (index in 0 until incoming.length()) {
            val reply = incoming.optJSONObject(index) ?: continue
            val style = reply.safeString("style")
            if (style in REQUIRED_STYLES) {
                byStyle[style] = reply
            } else {
                unstyledByPosition[index] = reply
            }
        }

        val localByStyle = LocalDemo.result.replies.associateBy(Reply::style)
        val replies = REQUIRED_STYLES.mapIndexed { index, style ->
            val source = byStyle[style] ?: unstyledByPosition[index]
            val local = requireNotNull(localByStyle[style])
            Reply(
                style = style,
                text = source?.safeString("text").orEmpty().ifBlank { local.text },
                why = source?.safeString("why").orEmpty().ifBlank { local.why },
            )
        }

        val deathIndex = when (val value = json.opt("chat_death_index")) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }?.coerceIn(0, 100) ?: DEFAULT_DEATH_INDEX
        return WingmanResult(
            chatDeathIndex = deathIndex,
            context = json.safeString("context").ifBlank { LocalDemo.result.context },
            replies = replies,
        )
    }

    private fun JSONObject.safeString(key: String): String {
        val value = opt(key)
        return if (value == null || value === JSONObject.NULL) "" else value.toString().trim()
    }

    private val REQUIRED_STYLES = listOf("認真", "幽默", "曖昧")
    private const val DEFAULT_DEATH_INDEX = 50
}
