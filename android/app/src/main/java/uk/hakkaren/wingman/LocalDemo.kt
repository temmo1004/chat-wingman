package uk.hakkaren.wingman

/**
 * 本地寫死範本：後端沒部署 / 斷網時的最終保底，讓 app 純本地也能完整 demo。
 * 內容與 backend/prompt.py 的 DEMO_RESPONSE 對齊。
 */
object LocalDemo {
    val result = WingmanResult(
        chatDeathIndex = 78,
        context = "你問「你下班了嗎？」「今天忙嗎？」；對方只回「剛到家」「還好 哈哈」，話題快斷了",
        replies = listOf(
            Reply("認真", "感覺你今天有點累？如果想聊我隨時都在，不想聊也沒關係～",
                "先關心情緒、給對方台階，把壓力拿掉反而更願意回"),
            Reply("幽默", "我偵測到你的回覆能量只剩 3%，需要幫你充電嗎⚡",
                "用玩笑點破冷場，把尷尬變成兩人的梗，重啟話題"),
            Reply("曖昧", "還好？那我算你今天第幾個讓你想已讀的人？😏",
                "輕推拉抬升溫度，製造專屬感又不會太over"),
        ),
    )
}
