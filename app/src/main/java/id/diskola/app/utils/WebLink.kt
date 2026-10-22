package id.diskola.app.utils

/** Which address a teacher- or student-entered "link" field should open. */
object WebLink {

    private val HAS_SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
    private val NON_WEB_SCHEME = Regex("^(mailto|tel|sms|javascript|intent|file|content|data|geo|market):", RegexOption.IGNORE_CASE)

    /**
     * The http(s) URL to open for [raw], or null when it is blank or points at something that is not a web page
     * (`file://`, `intent:`, `tel:` …). Input without a scheme, e.g. "youtu.be/abc" as typed into the form, gets `https://`.
     */
    fun normalize(raw: String): String? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        if (HAS_SCHEME.containsMatchIn(text)) {
            val scheme = text.substringBefore("://").lowercase()
            return if (scheme == "http" || scheme == "https") text else null
        }
        if (NON_WEB_SCHEME.containsMatchIn(text)) return null
        return "https://$text"
    }
}
