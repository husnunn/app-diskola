package id.diskola.app.utils

/**
 * Masks credentials before a request/response body reaches Logcat or a log file (doc `08` Q14 — a payment PIN used to be printed
 * in clear text in every build). Handles JSON (`"pin":"123456"`, `"pin":123456`) and form encoding (`pin=123456`); anything else
 * comes back unchanged.
 */
object LogRedactor {

    const val MASK = "***"

    private val keys = listOf(
        "pin", "password", "old_password", "new_password", "password_confirmation",
        "reset_token", "token", "access_token", "refresh_token", "id_token",
    ).joinToString("|")

    private val jsonString = Regex("(\"(?:$keys)\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"", RegexOption.IGNORE_CASE)
    private val jsonNumber = Regex("(\"(?:$keys)\"\\s*:\\s*)-?\\d+(?:\\.\\d+)?", RegexOption.IGNORE_CASE)
    private val form = Regex("((?:^|[?&\\s])(?:$keys)=)[^&\\s]*", RegexOption.IGNORE_CASE)

    fun redact(text: String): String {
        if (text.isEmpty()) return text
        return text
            .replace(jsonString) { it.groupValues[1] + "\"$MASK\"" }
            .replace(jsonNumber) { it.groupValues[1] + "\"$MASK\"" }
            .replace(form) { it.groupValues[1] + MASK }
    }

    /** Header lines that must never be written out. */
    fun isSensitiveHeader(name: String): Boolean =
        name.equals("Authorization", ignoreCase = true) || name.equals("Cookie", ignoreCase = true) || name.equals("Set-Cookie", ignoreCase = true)
}
