package id.diskola.app.utils

/**
 * The few sentences the backend sends in English that a user reads as they are (the presensi alert and status card).
 * Applied where the text is shown, never where it is read: `HubRules` and others still match the server's original wording.
 * Anything not listed is returned as sent.
 */
object ServerText {

    private val KNOWN = mapOf(
        "school calendar event is not available for today" to "Agenda kalender sekolah tidak tersedia untuk hari ini",
    )

    fun localize(raw: String): String = KNOWN[raw.trim().trimEnd('.').lowercase()] ?: raw

    private val TAG = Regex("<[^>]*>")
    private val NUMERIC_ENTITY = Regex("&#(x[0-9a-fA-F]+|[0-9]+);")
    private val NAMED_ENTITIES = mapOf("&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"", "&apos;" to "'", "&nbsp;" to " ")

    /**
     * A label the server sends as a little HTML (`message_label` of a finished task is `<p>&#10004;</p>`) as the text a person
     * reads (`✔`). Legacy drew it as it came; the tags and the character code were never meant to be seen. Plain text passes unchanged.
     */
    fun plain(html: String): String {
        if (!html.contains('<') && !html.contains('&')) return html
        var text = TAG.replace(html, "")
        text = NUMERIC_ENTITY.replace(text) { m ->
            val code = m.groupValues[1]
            val point = if (code.startsWith("x", ignoreCase = true)) code.drop(1).toIntOrNull(16) else code.toIntOrNull()
            if (point != null && Character.isValidCodePoint(point)) String(Character.toChars(point)) else m.value
        }
        NAMED_ENTITIES.forEach { (entity, char) -> text = text.replace(entity, char) }
        return text.trim()
    }
}
