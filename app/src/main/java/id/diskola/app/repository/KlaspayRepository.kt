package id.diskola.app.repository

import id.diskola.app.apiservice.ActivateKlaspayRequest
import id.diskola.app.apiservice.ApiException
import id.diskola.app.apiservice.AuthApiService
import id.diskola.app.apiservice.KlaspayApiService
import id.diskola.app.dataclass.ResponData.KlaspayWalletData
import id.diskola.app.dataclass.ResponData.ResetPinRequest
import id.diskola.app.dataclass.ResponData.SetPinRequest
import id.diskola.app.utils.PaymentRules
import id.diskola.app.utils.session.SessionStore
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import timber.log.Timber

/** What the hub shows of `payment/wallet`. */
data class WalletInfo(
    val walletId: String,
    /** "ID Wallet" as the user sees and copies it. */
    val userId: String,
    val name: String,
    val balance: Long,
    val isToppers: Boolean,
    val toppersStatus: String,
)

/** The answer of `mobile/app/payment/check`. */
sealed interface PaymentCheckResult {
    data object Allowed : PaymentCheckResult

    /** [errorTypes] are the keys of the error body's `errors` object (`email`, `password`). */
    data class Rejected(val errorTypes: List<String>, val message: String) : PaymentCheckResult
}

/** `payment/reset-pin`: [verified] means the server already trusts the e-mail and handed back [token] for the new-PIN form. */
data class ResetPinOutcome(val verified: Boolean, val token: String)

/**
 * Klaspay wallet: balance, activation and PIN reset (doc `08-keuangan-pembayaran-klaspay-ppob.md` §3–§5). Errors from the network
 * propagate to the caller, which owns the message shown to the user.
 */
class KlaspayRepository @Inject constructor(
    private val api: KlaspayApiService,
    private val authApi: AuthApiService,
    private val sessionStore: SessionStore,
) {
    /**
     * `payment/wallet`. A wallet that answers means the account is activated, so the session flags are refreshed from it (after the
     * response, unlike legacy). Null when the server sent no wallet.
     */
    suspend fun wallet(): WalletInfo? {
        val response = api.getWallet()
        val data = response.data ?: return null
        if (response.status.isNotBlank() && !response.status.equals("success", ignoreCase = true)) return null
        sessionStore.writeWallet(data.wallet_id, data.is_toppers == true, data.toppers_status)
        return data.toInfo()
    }

    /**
     * Asks the server whether this account may activate its wallet. A rejection that names the e-mail also triggers a fresh
     * verification mail, as legacy did.
     */
    suspend fun preflightCheck(): PaymentCheckResult = try {
        api.checkPaymentStatus()
        PaymentCheckResult.Allowed
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        val types = e.errorTypes.toList()
        if (PaymentRules.shouldResendVerification(e.errorTypes)) resendVerification()
        PaymentCheckResult.Rejected(types, e.message.orEmpty())
    }

    /** Creates the wallet PIN. The password field stays empty, exactly like legacy (the password step is unreachable there). */
    suspend fun activate(pin: String, fromSso: Boolean) {
        api.activateKlaspay(ActivateKlaspayRequest(pin = pin))
        sessionStore.markKlaspayActivated(fromSso = fromSso)
    }

    suspend fun resetPin(email: String): ResetPinOutcome {
        val response = api.resetPin(ResetPinRequest(email))
        return ResetPinOutcome(verified = response.email_verified == true, token = response.reset_token)
    }

    suspend fun setNewPin(pin: String, token: String) {
        api.setNewPin(SetPinRequest(pin = pin, reset_token = token))
    }

    /** A 2xx on the mailed link means the token is still valid. */
    suspend fun validateResetToken(token: String) {
        api.validateResetPinToken(token).close()
    }

    private suspend fun resendVerification() {
        try {
            authApi.verifyEmail()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Gagal mengirim ulang verifikasi email")
        }
    }

    private fun KlaspayWalletData.toInfo() = WalletInfo(
        walletId = wallet_id,
        userId = user_id,
        name = name,
        balance = PaymentRules.balanceOf(balance),
        isToppers = is_toppers == true,
        toppersStatus = toppers_status,
    )
}
