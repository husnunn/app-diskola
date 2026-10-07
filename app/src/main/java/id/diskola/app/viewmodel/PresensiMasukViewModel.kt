package id.diskola.app.viewmodel

import android.content.IntentSender
import android.location.Location
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.PresensiCheckData
import id.diskola.app.repository.PresensiRepository
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.PresensiRules
import id.diskola.app.utils.PresensiRules.MasukStatus
import id.diskola.app.utils.location.LocationProvider
import id.diskola.app.utils.location.MockLocationGuard
import id.diskola.app.utils.location.distanceMeters
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/** Result of the last submit, shown as the "Absensi Waktu Berhasil" dialog. */
data class MasukSuccess(val isCheckIn: Boolean) {
    val message: String
        get() = if (isCheckIn) "Pencatatan waktu Masuk Anda telah tersimpan, silahkan melanjutkan jadwal hari ini"
        else "Pencatatan waktu Pulang Anda telah tersimpan, terima kasih"
}

/**
 * Masuk/Pulang sekolah (doc `07` §5). Server decides arrival vs departure and whether a tap is
 * allowed; the client only adds the radius rule (when `absence_setting` enforces it) and — new — a
 * mock-location refusal. The geofence of the legacy app is intentionally not rebuilt.
 */
@HiltViewModel
class PresensiMasukViewModel @Inject constructor(
    private val repository: PresensiRepository,
    private val locationProvider: LocationProvider,
) : BaseViewModel() {

    private val _check = MutableStateFlow<PresensiCheckData?>(null)
    val check: StateFlow<PresensiCheckData?> = _check.asStateFlow()

    private val _checkLoading = MutableStateFlow(true)
    private val _checkFailed = MutableStateFlow(false)

    private val _restricted = MutableStateFlow(true)
    val restricted: StateFlow<Boolean> = _restricted.asStateFlow()

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _success = MutableStateFlow<MasukSuccess?>(null)
    val success: StateFlow<MasukSuccess?> = _success.asStateFlow()

    private val _lastLabel = MutableStateFlow("")
    val lastLabel: StateFlow<String> = _lastLabel.asStateFlow()

    private val school = repository.school
    val schoolLat: Double get() = school.coordinateLatitude
    val schoolLng: Double get() = school.coordinateLongitude
    val radiusMeters: Float get() = PresensiRules.radiusMeters(school.coordinateRadius)

    /** Radius is enforced but the school has no coordinates: nobody can ever be "inside". */
    val schoolNotConfigured: StateFlow<Boolean> = _restricted
        .combine(_checkLoading) { restricted, loading -> !loading && restricted && schoolLat == 0.0 && schoolLng == 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val status: StateFlow<MasukStatus> = combine(_check, _checkLoading, _checkFailed, _restricted, _location) { check, loading, failed, restricted, loc ->
        val distance = loc?.takeIf { schoolLat != 0.0 || schoolLng != 0.0 }
            ?.let { distanceMeters(it.latitude, it.longitude, schoolLat, schoolLng) }
        PresensiRules.masukStatus(
            loading = loading,
            checkFailed = failed,
            allowAttendance = check?.allow_attendance == true,
            buttonLabel = check?.attendance_response_text_button.orEmpty(),
            serverText = check?.attendance_response_text.orEmpty(),
            hasLocation = loc != null,
            isMock = loc?.let(MockLocationGuard::isMock) == true,
            restricted = restricted,
            radius = radiusMeters,
            distanceMeters = distance,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MasukStatus.Loading)

    val userName: String get() = repository.user.name
    val userAvatar: String get() = repository.user.avatar

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _checkLoading.value = true
            _checkFailed.value = false
            try {
                coroutineScope {
                    val check = async { repository.check() }
                    val restricted = async { repository.isRadiusRestricted() }
                    _check.value = check.await()
                    _restricted.value = restricted.await()
                }
                repository.lastRecord()?.let { last ->
                    val time = last.leaveAt.ifBlank { last.attendAt }
                    _lastLabel.value = "Presensi terakhir ${last.date}, $time"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _checkFailed.value = true
                emitError(AppErrorHandler.getMessage(e))
            } finally {
                _checkLoading.value = false
            }
        }
    }

    // ---- Location ----

    /** The "turn on location" dialog to launch, or null when settings are already fine. */
    suspend fun settingsResolution(): IntentSender? = locationProvider.settingsResolution()

    fun isLocationEnabled(): Boolean = locationProvider.isLocationEnabled()

    fun locationUpdates(): Flow<Location> = locationProvider.locationUpdates()

    fun onLocation(location: Location) {
        _location.value = location
    }

    /** One quick fix so the map does not wait for the first 10-second update. */
    fun seedLocation() {
        viewModelScope.launch { locationProvider.freshLocation()?.let { if (_location.value == null) _location.value = it } }
    }

    // ---- Submit ----

    fun submit() {
        val loc = _location.value
        if (loc == null) {
            emitError("Gagal mendapatkan lokasi Anda")
            return
        }
        if (MockLocationGuard.isMock(loc)) {
            emitError(MockLocationGuard.MESSAGE)
            return
        }
        val isCheckIn = _check.value?.isCheckIn == true
        viewModelScope.launch {
            _submitting.value = true
            clearError()
            try {
                if (isCheckIn) repository.checkIn(loc.latitude, loc.longitude) else repository.checkOut(loc.latitude, loc.longitude)
                _success.value = MasukSuccess(isCheckIn)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                // The reason is shown (legacy swallowed it) and the button stays, so a retry is possible.
                emitError(AppErrorHandler.getMessage(e))
            } finally {
                _submitting.value = false
            }
        }
    }
}
