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

    @Test
    fun plain_turnsTheDoneTasksHtmlBadgeIntoItsCheckMark() {
        // Dev sends `message_label: "<p>&#10004;</p>"` for a finished task; the card showed those characters as they were.
        assertEquals("✔", ServerText.plain("<p>&#10004;</p>"))
        assertEquals("✔", ServerText.plain("<p>&#x2714;</p>"))
    }

    @Test
    fun plain_decodesNamedEntitiesAndTrims() {
        assertEquals("Tugas & Ujian <1>", ServerText.plain(" <b>Tugas &amp; Ujian</b> &lt;1&gt; "))
    }

    @Test
    fun plain_leavesPlainTextAndUnknownCodesAlone() {
        assertEquals("Baru", ServerText.plain("Baru"))
        assertEquals("", ServerText.plain(""))
        assertEquals("1 tugas belum di nilai", ServerText.plain("1 tugas belum di nilai"))
        assertEquals("&#99999999;", ServerText.plain("&#99999999;"))
    }
}
