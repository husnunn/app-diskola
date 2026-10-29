package id.diskola.app.repository

import id.diskola.app.apiservice.PresensiApiService
import id.diskola.app.utils.FeaturePass
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import timber.log.Timber

data class FeatureAvailability(val available: Boolean, val message: String?)

/**
 * `mobile/app/check-feature-availability` — the server-side switch that can hide Presensi/Jurnal
 * (doc `07` §1) and Bayar SPP / Dana Partisipasi (doc `08` §5.6). Any failure counts as *available*
 * (fail-open, same as the legacy app), so a flaky network never locks people out of attendance —
 * but it is logged, so a gate that silently stopped working can be told apart from one that passed.
 *
 * Singleton so the hub's [grantPass] reaches the screen it opens.
 */
@Singleton
class FeatureGateRepository @Inject constructor(
    private val api: PresensiApiService,
) {
    private val pass = FeaturePass()

    suspend fun check(name: String): FeatureAvailability = try {
        val data = api.featureAvailability(name).data
        FeatureAvailability(available = data?.available != false, message = data?.message)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "check-feature-availability name=%s failed, treated as available", name)
        FeatureAvailability(available = true, message = null)
    }

    /** The caller just got "available" for [name] and is about to open the screen behind it. */
    fun grantPass(name: String) = pass.grant(name)

    /** The screen just opened asks whether its caller already got "available" a moment ago (once per [grantPass]). */
    fun consumePass(name: String): Boolean = pass.consume(name)

    companion object {
        const val PRESENSI = "presensi"
        const val JURNAL_KBM = "jurnal-kbm"

        /** Bayar SPP and Dana Partisipasi (doc `08` §5.6, since 21-10-2026). */
        const val PAYMENT = "payment"
    }
}
