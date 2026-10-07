package id.diskola.app.viewmodel

import android.content.IntentSender
import android.location.Location
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.repository.JurnalRepository
import id.diskola.app.repository.PresensiRepository
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.PresensiRules
import id.diskola.app.utils.location.LocationProvider
import id.diskola.app.utils.location.MockLocationGuard
import id.diskola.app.utils.location.distanceMeters
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/** Why the confirm button is (not) available. */
enum class VerifikasiGate { LOADING, WAITING_LOCATION, MOCK, OUTSIDE_RADIUS, NOT_CONFIGURED, READY }

/**
 * Student "Verifikasi Jurnal" (doc `07` §7.3): when `absence_setting` restricts the radius the confirm button
 * exists only inside the school's circle; then a status (Terlaksana / Penugasan / Tidak Terlaksana) is sent with
 * `journal-update`. Like Masuk sekolah it needs a precise fix while open, no background location or geofence.
 */
@HiltViewModel
class VerifikasiJurnalViewModel @Inject constructor(
    private val jurnalRepository: JurnalRepository,
    private val presensiRepository: PresensiRepository,
    private val locationProvider: LocationProvider,
) : BaseViewModel() {

    private val school = presensiRepository.school
    val schoolLat: Double get() = school.coordinateLatitude
    val schoolLng: Double get() = school.coordinateLongitude
    val radiusMeters: Float get() = PresensiRules.radiusMeters(school.coordinateRadius)

    private val _restricted = MutableStateFlow<Boolean?>(null)
    val restricted: StateFlow<Boolean?> = _restricted.asStateFlow()

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    val gate: StateFlow<VerifikasiGate> = combine(_restricted, _location) { restricted, loc ->
        when {
            restricted == null -> VerifikasiGate.LOADING
            !restricted -> VerifikasiGate.READY
            schoolLat == 0.0 && schoolLng == 0.0 -> VerifikasiGate.NOT_CONFIGURED
            loc == null -> VerifikasiGate.WAITING_LOCATION
            MockLocationGuard.isMock(loc) -> VerifikasiGate.MOCK
            distanceMeters(loc.latitude, loc.longitude, schoolLat, schoolLng) > radiusMeters && radiusMeters > 0f -> VerifikasiGate.OUTSIDE_RADIUS
            else -> VerifikasiGate.READY
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VerifikasiGate.LOADING)

    init {
        viewModelScope.launch { _restricted.value = presensiRepository.isRadiusRestricted() }
    }

    suspend fun settingsResolution(): IntentSender? = locationProvider.settingsResolution()
    fun isLocationEnabled(): Boolean = locationProvider.isLocationEnabled()
    fun locationUpdates(): Flow<Location> = locationProvider.locationUpdates()
    fun onLocation(fix: Location) { _location.value = fix }

    fun seedLocation() {
        viewModelScope.launch { locationProvider.freshLocation()?.let { if (_location.value == null) _location.value = it } }
    }

    /** [status] is one of the three session statuses; the gate is re-checked here as well as in the UI. */
    fun submit(attendanceId: Int, status: String) {
        if (gate.value != VerifikasiGate.READY) return
        viewModelScope.launch {
            _loading.value = true
            clearError()
            try {
                jurnalRepository.studentVerify(attendanceId, status)
                _done.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                emitError(AppErrorHandler.getMessage(e))
            } finally {
                _loading.value = false
            }
        }
    }
}
