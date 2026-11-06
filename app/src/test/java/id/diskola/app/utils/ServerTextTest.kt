package id.diskola.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerTextTest {

    @Test
    fun localize_translatesTheKnownEnglishSentenceWhateverItsCaseAndFullStop() {
        val indonesian = "Agenda kalender sekolah tidak tersedia untuk hari ini"
        assertEquals(indonesian, ServerText.localize("School Calendar Event is not available for today"))
        assertEquals(indonesian, ServerText.localize("  school calendar event is not available for today.  "))
    }

    @Test
    fun localize_returnsAnythingElseAsSent() {
        assertEquals("Belum waktunya melaporkan pulang", ServerText.localize("Belum waktunya melaporkan pulang"))
        assertEquals("", ServerText.localize(""))
    }
}
