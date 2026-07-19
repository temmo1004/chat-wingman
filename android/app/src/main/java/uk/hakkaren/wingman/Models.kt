package uk.hakkaren.wingman

/** 對應 docs/api.md 的回應結構。 */
data class WingmanResult(
    val chatDeathIndex: Int,
    val context: String,
    val replies: List<Reply>,
)

data class Reply(
    val style: String,   // 認真 / 幽默 / 曖昧
    val text: String,    // 可直接送出的回覆
    val why: String,     // 軍師解說
)
