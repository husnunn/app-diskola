package id.diskola.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityDialogRulesTest {

    @Test
    fun `plain addresses are valid and malformed ones are not`() {
        assertTrue(SecurityDialogRules.isValidEmail("siswa@sekolah.sch.id"))
        assertTrue(SecurityDialogRules.isValidEmail("a.b+c@gmail.com"))
        assertFalse(SecurityDialogRules.isValidEmail(""))
        assertFalse(SecurityDialogRules.isValidEmail("siswa"))
        assertFalse(SecurityDialogRules.isValidEmail("siswa@"))
        assertFalse(SecurityDialogRules.isValidEmail("@gmail.com"))
        assertFalse(SecurityDialogRules.isValidEmail("a b@gmail.com"))
        assertFalse(SecurityDialogRules.isValidEmail("a@gmail"))
    }

    @Test
    fun `verification can be sent only for a valid address`() {
        assertTrue(SecurityDialogRules.canSendVerification("a@b.id"))
        assertFalse(SecurityDialogRules.canSendVerification(""))
        assertFalse(SecurityDialogRules.canSendVerification("nope"))
    }

    @Test
    fun `password change needs all three fields`() {
        assertFalse(SecurityDialogRules.canSubmitPassword("", "b", "b"))
        assertFalse(SecurityDialogRules.canSubmitPassword("a", "", "b"))
        assertFalse(SecurityDialogRules.canSubmitPassword("a", "b", ""))
        assertTrue(SecurityDialogRules.canSubmitPassword("a", "b", "b"))
    }

    @Test
    fun `password validation keeps the legacy texts including the typo`() {
        assertEquals("Kkonfirmasi password baru tidak sesuai", SecurityDialogRules.validatePasswordChange("lama", "baru1", "baru2"))
        assertEquals("Password baru tidak boleh sama dengan password lama", SecurityDialogRules.validatePasswordChange("sama", "sama", "sama"))
        assertNull(SecurityDialogRules.validatePasswordChange("lama", "baru", "baru"))
    }

    @Test
    fun `confirmation mismatch is reported before reuse of the old password`() {
        assertEquals("Kkonfirmasi password baru tidak sesuai", SecurityDialogRules.validatePasswordChange("x", "x", "y"))
    }

    @Test
    fun `the password dialog is mandatory while the wallet is active`() {
        assertTrue(SecurityDialogRules.passwordDialogCancelable(klaspayActive = false))
        assertFalse(SecurityDialogRules.passwordDialogCancelable(klaspayActive = true))
    }
}
