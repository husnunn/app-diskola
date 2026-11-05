package id.diskola.app.utils

/**
 * Dates on the student's "Poin Saya": when a point was given and when a calling is scheduled. Same label as the Magang cards
 * (`04 Jun 2026, 14:50`); legacy printed what the server sent (`2026-11-06 10:10:00`, and `date || time` in the photo dialog).
 * Whatever cannot be read is shown as sent, never blank and never an error.
 */
object PoinDateRules {

    /** `05 Nov 2026, 10:07` from the event's [date] and [time]; the date alone without a time; both as sent when the date is not a date. */
    fun eventStamp(date: String, time: String): String {
        val day = date.trim()
        val clock = time.trim()
        if (MagangRules.dateOf(day) == null) return listOf(day, clock).filter { it.isNotEmpty() }.joinToString(" ")
        val label = MagangRules.dateLabel(day)
        // Some servers put the time inside the date field instead of a separate one.
        val shownClock = clock.ifEmpty { day.drop(10).trimStart(' ', 'T') }
        return if (shownClock.isEmpty()) label else "$label, ${JurnalRules.shortClock(shownClock)}"
    }

    /** `06 Nov 2026, 10:10` from `2026-11-06 10:10:00`; as sent when it cannot be read, "" when there is nothing. */
    fun callSchedule(raw: String): String = raw.trim().takeIf { it.isNotEmpty() }?.let { MagangRules.dateTimeLabel(it) }.orEmpty()
}
