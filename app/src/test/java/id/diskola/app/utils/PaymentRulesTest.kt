package id.diskola.app.utils

import id.diskola.app.utils.PaymentRules.ActivationStep
import id.diskola.app.utils.PaymentRules.HubMenuItem
import id.diskola.app.utils.PaymentRules.PpobItem
import id.diskola.app.utils.PaymentRules.ResetPinPlan
import id.diskola.app.utils.PaymentRules.ResetPinResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentRulesTest {

    // ---- hub menu ----

    @Test
    fun `students get Bayar Spp first, everyone else starts at Dana Partisipasi`() {
        assertEquals(listOf(HubMenuItem.BayarSpp, HubMenuItem.DanaPartisipasi, HubMenuItem.PinjamBuku), PaymentRules.hubMenu(isStudent = true))
        assertEquals(listOf(HubMenuItem.DanaPartisipasi, HubMenuItem.PinjamBuku), PaymentRules.hubMenu(isStudent = false))
    }

    @Test
    fun `teachers lose Game and BPJS moves up to fifth place`() {
        val teacher = PaymentRules.ppobItems(isTeacher = true)
        assertEquals(listOf(PpobItem.Pulsa, PpobItem.Listrik, PpobItem.Air, PpobItem.Internet, PpobItem.Bpjs), teacher)
        assertEquals(PpobItem.Bpjs, teacher[4])
        assertEquals(6, PaymentRules.ppobItems(isTeacher = false).size)
        assertTrue(PpobItem.Game in PaymentRules.ppobItems(isTeacher = false))
    }

    @Test
    fun `pinjam buku opens nothing, every other item has a destination`() {
        assertNull(HubMenuItem.PinjamBuku.destination)
        assertEquals(PaymentRules.PaymentDestination.BayarSpp, HubMenuItem.BayarSpp.destination)
        assertEquals(PaymentRules.PaymentDestination.DanaPartisipasi, HubMenuItem.DanaPartisipasi.destination)
        assertEquals(PaymentRules.PaymentDestination.Bpjs, PpobItem.Bpjs.destination)
        assertEquals(PpobItem.entries.size, PpobItem.entries.map { it.destination }.toSet().size)
    }

    // ---- activation preflight ----

    @Test
    fun `preflight asks for the e-mail before the default password before the server`() {
        assertEquals(ActivationStep.NeedEmail, PaymentRules.activationPreflight(isEmailVerified = false, defaultPass = true))
        assertEquals(ActivationStep.NeedEmail, PaymentRules.activationPreflight(isEmailVerified = false, defaultPass = false))
        assertEquals(ActivationStep.NeedPassword, PaymentRules.activationPreflight(isEmailVerified = true, defaultPass = true))
        assertEquals(ActivationStep.CallCheck, PaymentRules.activationPreflight(isEmailVerified = true, defaultPass = false))
    }

    @Test
    fun `after the e-mail dialogs a default password is next, otherwise the flow stops instead of looping`() {
        assertEquals(ActivationStep.NeedPassword, PaymentRules.afterEmailStep(defaultPass = true))
        assertEquals(ActivationStep.Stop, PaymentRules.afterEmailStep(defaultPass = false))
    }

    @Test
    fun `after a password change the checks restart`() {
        assertEquals(ActivationStep.CallCheck, PaymentRules.afterPasswordChanged(isEmailVerified = true))
        assertEquals(ActivationStep.NeedEmail, PaymentRules.afterPasswordChanged(isEmailVerified = false))
    }

    @Test
    fun `payment check errors name the missing step`() {
        assertEquals(ActivationStep.NeedEmail, PaymentRules.stepForCheckError(arrayOf("email")))
        assertEquals(ActivationStep.NeedEmail, PaymentRules.stepForCheckError(arrayOf("password", "email")))
        assertEquals(ActivationStep.NeedPassword, PaymentRules.stepForCheckError(arrayOf("password")))
        assertEquals(ActivationStep.Stop, PaymentRules.stepForCheckError(arrayOf("other")))
        assertEquals(ActivationStep.Stop, PaymentRules.stepForCheckError(emptyArray()))
        assertTrue(PaymentRules.shouldResendVerification(arrayOf("email")))
        assertFalse(PaymentRules.shouldResendVerification(arrayOf("password")))
    }

    // ---- PIN ----

    @Test
    fun `a pin is complete at exactly six digits`() {
        assertTrue(PaymentRules.pinComplete("123456"))
        assertTrue(PaymentRules.pinComplete("000000"))
        assertFalse(PaymentRules.pinComplete("12345"))
        assertFalse(PaymentRules.pinComplete("1234567"))
        assertFalse(PaymentRules.pinComplete("12345a"))
        assertFalse(PaymentRules.pinComplete(""))
    }

    @Test
    fun `confirmation matches only when both are complete and equal`() {
        assertTrue(PaymentRules.pinMatches("123456", "123456"))
        assertFalse(PaymentRules.pinMatches("123456", "123457"))
        assertFalse(PaymentRules.pinMatches("12345", "12345"))
    }

    @Test
    fun `mismatch label appears only after six digits were typed`() {
        assertFalse(PaymentRules.showPinMismatch("123456", "12345"))
        assertFalse(PaymentRules.showPinMismatch("123456", "123456"))
        assertTrue(PaymentRules.showPinMismatch("123456", "654321"))
    }

    @Test
    fun `keypad input is digits only and capped at six`() {
        assertEquals("1", PaymentRules.appendPinDigit("", '1'))
        assertEquals("123456", PaymentRules.appendPinDigit("12345", '6'))
        assertEquals("123456", PaymentRules.appendPinDigit("123456", '7'))
        assertEquals("12", PaymentRules.appendPinDigit("12", 'x'))
        assertEquals("12", PaymentRules.deletePinDigit("123"))
        assertEquals("", PaymentRules.deletePinDigit(""))
    }

    @Test
    fun `new pin validation uses the legacy messages in the legacy order`() {
        assertEquals("pin harus berisi 6 digit angka", PaymentRules.validateNewPin("123", "123"))
        assertEquals("pin harus berisi 6 digit angka", PaymentRules.validateNewPin("", ""))
        assertEquals("pin baru dan konfirmasi tidak sesuai", PaymentRules.validateNewPin("123456", "123457"))
        assertNull(PaymentRules.validateNewPin("123456", "123456"))
    }

    @Test
    fun `save is enabled once both fields have text`() {
        assertFalse(PaymentRules.canSubmitNewPin("", "1"))
        assertFalse(PaymentRules.canSubmitNewPin("1", ""))
        assertTrue(PaymentRules.canSubmitNewPin("1", "2"))
    }

    // ---- reset pin ----

    @Test
    fun `forgot pin needs a verified and known e-mail first`() {
        assertEquals(ResetPinPlan.NeedEmailSetting, PaymentRules.resetPinPlan(isEmailVerified = false, email = "a@b.id"))
        assertEquals(ResetPinPlan.NeedEmailSetting, PaymentRules.resetPinPlan(isEmailVerified = true, email = ""))
        assertEquals(ResetPinPlan.NeedEmailSetting, PaymentRules.resetPinPlan(isEmailVerified = true, email = "   "))
        assertEquals(ResetPinPlan.SendReset, PaymentRules.resetPinPlan(isEmailVerified = true, email = "a@b.id"))
    }

    @Test
    fun `reset result opens the form only for a verified e-mail with a token`() {
        assertEquals(ResetPinResult.OpenSetPin, PaymentRules.resetPinResult(true, "tok"))
        assertEquals(ResetPinResult.ShowMailSent, PaymentRules.resetPinResult(true, ""))
        assertEquals(ResetPinResult.ShowMailSent, PaymentRules.resetPinResult(false, "tok"))
        assertEquals(ResetPinResult.ShowMailSent, PaymentRules.resetPinResult(null, "tok"))
    }

    // ---- money ----

    @Test
    fun `rupiah uses dots and no decimals`() {
        assertEquals("Rp 0", PaymentRules.formatRupiah(0))
        assertEquals("Rp 999", PaymentRules.formatRupiah(999))
        assertEquals("Rp 1.000", PaymentRules.formatRupiah(1_000))
        assertEquals("Rp 1.250.000", PaymentRules.formatRupiah(1_250_000))
        assertEquals("Rp 25.000.000", PaymentRules.formatRupiah(25_000_000))
        assertEquals("-Rp 5.000", PaymentRules.formatRupiah(-5_000))
    }

    @Test
    fun `a balance is read leniently`() {
        assertEquals(150_000L, PaymentRules.balanceOf(150000.0))
        assertEquals(150_000L, PaymentRules.balanceOf(150000.75))
        assertEquals(0L, PaymentRules.balanceOf(null))
        assertEquals(0L, PaymentRules.balanceOf(Double.NaN))
        assertEquals(0L, PaymentRules.balanceOf(Double.POSITIVE_INFINITY))
    }
}
