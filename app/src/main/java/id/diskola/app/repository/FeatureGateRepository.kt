package id.diskola.app.repository

import id.diskola.app.apiservice.PresensiApiService
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

data class FeatureAvailability(val available: Boolean, val message: String?)

/**
 * `mobile/app/check-feature-availability` — the server-side switch that can hide Presensi/Jurnal
 * (doc `07` §1). Any failure counts as *available* (fail-open, same as the legacy app), so a flaky
 * network never locks people out of attendance.
 */
class FeatureGateRepository @Inject constructor(
    private val api: PresensiApiService,
) {
    suspend fun check(name: String): FeatureAvailability = try {
        val data = api.featureAvailability(name).data
        FeatureAvailability(available = data?.available != false, message = data?.message)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        FeatureAvailability(available = true, message = null)
    }

    companion object {
        const val PRESENSI = "presensi"
        const val JURNAL_KBM = "jurnal-kbm"
    }
}
