package id.diskola.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PoinSearchRulesTest {

    @Test
    fun isSearchable_needsThreeCharactersAfterTrimming() {
        assertFalse(PoinSearchRules.isSearchable(""))
        assertFalse(PoinSearchRules.isSearchable("bu"))
        assertFalse(PoinSearchRules.isSearchable("  bu  "))
        assertTrue(PoinSearchRules.isSearchable("bud"))
        assertTrue(PoinSearchRules.isSearchable("  bud "))
    }

    @Test
    fun idleHint_explainsThePageWhileTheFieldIsEmpty() {
        val hint = PoinSearchRules.idleHint("")
        assertTrue(hint.contains("NISN/NIS/NIK"))
        assertTrue(hint.contains("pelanggaran"))
        assertEquals(hint, PoinSearchRules.idleHint("   "))
    }

    @Test
    fun idleHint_namesTheMissingLengthOnceTypingStarted() {
        assertEquals("Ketik minimal 3 karakter untuk mulai mencari siswa.", PoinSearchRules.idleHint("b"))
        assertEquals("Ketik minimal 3 karakter untuk mulai mencari siswa.", PoinSearchRules.idleHint("bu"))
    }
}
