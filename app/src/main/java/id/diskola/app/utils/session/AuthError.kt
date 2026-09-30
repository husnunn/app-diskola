package id.diskola.app.utils.session

import id.diskola.app.apiservice.ApiException
import id.diskola.app.utils.AppErrorHandler

/** What a failed auth call should show — doc `02-auth-login-sesi.md` §4.3 "Penanganan error login". */
sealed class AuthError {
    /** 400 + `"Detail login tidak bisa digunakan"` → bottom sheet "Login Gagal" with this exact text. */
    data class InvalidPassword(val message: String) : AuthError()

    /** `error_code == "DEVICE_CONFLICT"` → dialog "Perangkat Lain Terdeteksi", not a bottom sheet. */
    data class DeviceConflict(val message: String) : AuthError()

    /** Everything else → bottom sheet "Login Gagal" with this message. */
    data class Message(val message: String) : AuthError()
}

object AuthErrorMapper {
    private const val DEVICE_CONFLICT_CODE = "DEVICE_CONFLICT"
    private const val DEFAULT_DEVICE_CONFLICT_MESSAGE =
        "Akun sedang aktif di perangkat lain. Silakan logout dari perangkat tersebut terlebih dahulu."
    private const val INVALID_PASSWORD_MESSAGE = "Password yang anda masukkan salah"

    fun map(throwable: Throwable): AuthError {
        val apiException = throwable as? ApiException

        if (apiException?.errorCode == DEVICE_CONFLICT_CODE) {
            return AuthError.DeviceConflict(
                apiException.message?.takeIf { it.isNotBlank() } ?: DEFAULT_DEVICE_CONFLICT_MESSAGE
            )
        }

        if (apiException?.responseCode == 400 && isInvalidLoginDetail(apiException.message)) {
            return AuthError.InvalidPassword(INVALID_PASSWORD_MESSAGE)
        }

        return AuthError.Message(AppErrorHandler.getMessage(throwable))
    }

    /** Case-insensitive `contains`, trailing dot ignored — doc §4.3's exact matching rule. */
    private fun isInvalidLoginDetail(message: String?): Boolean {
        val normalized = message?.trim()?.trimEnd('.')?.lowercase().orEmpty()
        return normalized.contains("detail login tidak bisa digunakan")
    }
}
