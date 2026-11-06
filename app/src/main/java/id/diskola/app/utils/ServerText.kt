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
}
