package id.diskola.app.utils

/**
 * Validation behind the account-security dialogs shown in front of wallet activation (doc `02-auth-login-sesi.md` §8.2): the
 * "Masukkan Email" dialog and the "Ubah Password" dialog. Texts are the legacy ones, typos included.
 */
object SecurityDialogRules {

    /** `android.util.Patterns.EMAIL_ADDRESS`, reproduced so it can run in a JVM test. */
    private val email = Regex(
        "[a-zA-Z0-9+._%\\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+",
    )

    fun isValidEmail(value: String): Boolean = email.matches(value)

    /** "Verifikasi Sekarang" only reacts to a well-formed address. */
    fun canSendVerification(value: String): Boolean = value.isNotEmpty() && isValidEmail(value)

    /** "Buat Password Baru" is enabled once all three fields have text. */
    fun canSubmitPassword(old: String, new: String, confirmation: String): Boolean =
        old.isNotEmpty() && new.isNotEmpty() && confirmation.isNotEmpty()

    /** The toast to show instead of submitting, or null when the change may go ahead (`ChangePasswordDialog.kt:55-65`). */
    fun validatePasswordChange(old: String, new: String, confirmation: String): String? = when {
        new != confirmation -> "Kkonfirmasi password baru tidak sesuai"
        new == old -> "Password baru tidak boleh sama dengan password lama"
        else -> null
    }

    /** While the wallet is active the password dialog cannot be dismissed ("Nanti Saja" is hidden). */
    fun passwordDialogCancelable(klaspayActive: Boolean): Boolean = !klaspayActive
}
