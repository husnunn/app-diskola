package id.diskola.app.utils

/**
 * Pure rules of the payment module (doc `08-keuangan-pembayaran-klaspay-ppob.md`): the hub menu per role, the wallet-activation
 * checks, PIN entry and reset. Kept free of Android and of the network so each rule has a JVM test.
 */
object PaymentRules {

    const val PIN_LENGTH = 6

    // ---- Hub menu (§5.2) ----

    /** Every payment screen the hub can open — what a sub-phase has built is decided by the caller, not here. */
    enum class PaymentDestination { TopUp, QrPay, Riwayat, Tagihanku, BayarSpp, DanaPartisipasi, Pulsa, Listrik, Air, Internet, Game, Bpjs }

    /** The "Bayar Spp / Dana Partisipasi / Pinjam Buku" grid. [destination] is null for "Pinjam Buku": it only shows a dialog. */
    enum class HubMenuItem(val label: String, val destination: PaymentDestination?) {
        BayarSpp("Bayar Spp", PaymentDestination.BayarSpp),
        DanaPartisipasi("Dana Partisipasi", PaymentDestination.DanaPartisipasi),
        PinjamBuku("Pinjam Buku", null),
    }

    /** The "Pembelian" grid. */
    enum class PpobItem(val label: String, val destination: PaymentDestination) {
        Pulsa("Pulsa", PaymentDestination.Pulsa),
        Listrik("Listrik", PaymentDestination.Listrik),
        Air("Air", PaymentDestination.Air),
        Internet("Internet", PaymentDestination.Internet),
        Game("Game", PaymentDestination.Game),
        Bpjs("BPJS", PaymentDestination.Bpjs),
    }

    /** SPP is for students only (`PembayaranPage.kt:524-534`); everyone else starts at Dana Partisipasi. */
    fun hubMenu(isStudent: Boolean): List<HubMenuItem> =
        if (isStudent) HubMenuItem.entries else HubMenuItem.entries.filter { it != HubMenuItem.BayarSpp }

    /** Game vouchers are hidden for teachers, so BPJS moves up to fifth place (`PembayaranPage.kt:281-291`). */
    fun ppobItems(isTeacher: Boolean): List<PpobItem> =
        if (isTeacher) PpobItem.entries.filter { it != PpobItem.Game } else PpobItem.entries

    // ---- Wallet activation (§3, §5.3) ----

    /** What the activation button has to do next. */
    enum class ActivationStep {
        /** The account has no verified e-mail yet: show the "Masukkan Email" dialog. */
        NeedEmail,

        /** Still on the default password: show the "Ubah Password" dialog. */
        NeedPassword,

        /** Nothing is missing locally: ask the server (`mobile/app/payment/check`). */
        CallCheck,

        /** The user backed out or has to finish elsewhere (e.g. open the verification mail): do nothing more. */
        Stop,
    }

    /** Local checks before `payment/check`: e-mail first, then the default password (`PembayaranPage.kt:177-225`). */
    fun activationPreflight(isEmailVerified: Boolean, defaultPass: Boolean): ActivationStep = when {
        !isEmailVerified -> ActivationStep.NeedEmail
        defaultPass -> ActivationStep.NeedPassword
        else -> ActivationStep.CallCheck
    }

    /**
     * After the e-mail dialogs closed: a default password still has to be replaced. Otherwise stop — retrying would only re-open the
     * e-mail dialog until the user has followed the link in the mail (legacy looped on "Nanti Saja"; see `FLOW_QUESTIONS.md`).
     */
    fun afterEmailStep(defaultPass: Boolean): ActivationStep =
        if (defaultPass) ActivationStep.NeedPassword else ActivationStep.Stop

    /** After the password was replaced the checks run again from the top. */
    fun afterPasswordChanged(isEmailVerified: Boolean): ActivationStep = activationPreflight(isEmailVerified, defaultPass = false)

    /** `payment/check` failed: its error keys name what is missing (`KlaspayAktivasiViewmodel.kt:43-58`). */
    fun stepForCheckError(errorTypes: Array<String>): ActivationStep = when {
        "email" in errorTypes -> ActivationStep.NeedEmail
        "password" in errorTypes -> ActivationStep.NeedPassword
        else -> ActivationStep.Stop
    }

    /** Whether a failed check must also (re)send the verification mail — legacy did so whenever the error named `email`. */
    fun shouldResendVerification(errorTypes: Array<String>): Boolean = "email" in errorTypes

    // ---- PIN (§2.2, §2.3, §4) ----

    fun pinComplete(pin: String): Boolean = pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }

    /** The activation confirmation step: both PINs complete and identical. */
    fun pinMatches(pin: String, confirmation: String): Boolean = pinComplete(pin) && pin == confirmation

    /** The red "not the same" label of the confirmation step shows only once all six digits are typed and differ. */
    fun showPinMismatch(pin: String, confirmation: String): Boolean = pinComplete(confirmation) && pin != confirmation

    /** Keypad input: digits only, at most [PIN_LENGTH]. */
    fun appendPinDigit(pin: String, digit: Char): String =
        if (pin.length >= PIN_LENGTH || digit !in '0'..'9') pin else pin + digit

    fun deletePinDigit(pin: String): String = pin.dropLast(1)

    /** "Atur Pin Baru": the message to show, or null when the new PIN may be saved (`SetPinKlaspayPage.kt:36-40`). */
    fun validateNewPin(pin: String, confirmation: String): String? = when {
        pin.length < PIN_LENGTH -> "pin harus berisi 6 digit angka"
        pin != confirmation -> "pin baru dan konfirmasi tidak sesuai"
        else -> null
    }

    /** The "Simpan Perubahan" button is enabled as soon as both fields have text. */
    fun canSubmitNewPin(pin: String, confirmation: String): Boolean = pin.isNotEmpty() && confirmation.isNotEmpty()

    /** "Lupa pin?": the e-mail has to be verified and known before the server is asked (`ConfirmPinPage.kt:53-88`). */
    enum class ResetPinPlan { NeedEmailSetting, SendReset }

    fun resetPinPlan(isEmailVerified: Boolean, email: String): ResetPinPlan =
        if (!isEmailVerified || email.isBlank()) ResetPinPlan.NeedEmailSetting else ResetPinPlan.SendReset

    /** The server already trusts the e-mail → straight to the new-PIN form; otherwise a mail with the link was sent. */
    enum class ResetPinResult { OpenSetPin, ShowMailSent }

    fun resetPinResult(emailVerified: Boolean?, token: String): ResetPinResult =
        if (emailVerified == true && token.isNotBlank()) ResetPinResult.OpenSetPin else ResetPinResult.ShowMailSent

    // ---- Money ----

    /** `Rp 150.000` — dot as thousands separator, no decimals, minus sign in front for negatives. */
    fun formatRupiah(amount: Long): String {
        val digits = Math.abs(amount).toString().reversed().chunked(3).joinToString(".").reversed()
        return (if (amount < 0) "-Rp " else "Rp ") + digits
    }

    /** A server balance (Int in legacy, but tolerated as Double or absent) as whole rupiah. */
    fun balanceOf(raw: Double?): Long = raw?.takeIf { !it.isNaN() && !it.isInfinite() }?.toLong() ?: 0L
}
