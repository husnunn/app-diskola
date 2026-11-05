package id.diskola.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PoinDateRulesTest {

    @Test
    fun eventStamp_joinsTheDateAndTheClockWithoutSeconds() {
        assertEquals("05 Nov 2026, 10:07", PoinDateRules.eventStamp("2026-11-05", "10:07:56"))
        assertEquals("01 Jun 2026, 07:15", PoinDateRules.eventStamp("2026-06-01", "07:15"))
        assertEquals("05 Nov 2026, 10:07", PoinDateRules.eventStamp(" 2026-11-05 ", " 10:07 "))
    }

    @Test
    fun eventStamp_showsTheDateAloneWhenThereIsNoTime() {
        assertEquals("05 Nov 2026", PoinDateRules.eventStamp("2026-11-05", ""))
    }

    @Test
    fun eventStamp_readsATimeThatCameInsideTheDateField() {
        assertEquals("05 Nov 2026, 10:07", PoinDateRules.eventStamp("2026-11-05 10:07:56", ""))
        assertEquals("05 Nov 2026, 10:07", PoinDateRules.eventStamp("2026-11-05T10:07:56", ""))
        // A separate time wins over the one inside the date.
        assertEquals("05 Nov 2026, 08:00", PoinDateRules.eventStamp("2026-11-05 10:07:56", "08:00"))
    }

    @Test
    fun eventStamp_keepsWhatTheServerSentWhenItIsNotADate() {
        assertEquals("kemarin 10:07", PoinDateRules.eventStamp("kemarin", "10:07"))
        assertEquals("kemarin", PoinDateRules.eventStamp("kemarin", ""))
        assertEquals("05 Nov 2026, pagi", PoinDateRules.eventStamp("2026-11-05", "pagi"))
        assertEquals("", PoinDateRules.eventStamp("", ""))
    }

    @Test
    fun callSchedule_formatsTheServersDateTime() {
        assertEquals("06 Nov 2026, 10:10", PoinDateRules.callSchedule("2026-11-06 10:10:00"))
        assertEquals("06 Nov 2026, 10:10", PoinDateRules.callSchedule("2026-11-06T10:10:00"))
        assertEquals("06 Nov 2026", PoinDateRules.callSchedule("2026-11-06"))
    }

    @Test
    fun callSchedule_isBlankForNothingAndAsSentForTheUnreadable() {
        assertEquals("", PoinDateRules.callSchedule(""))
        assertEquals("", PoinDateRules.callSchedule("   "))
        assertEquals("segera", PoinDateRules.callSchedule("segera"))
    }
}
