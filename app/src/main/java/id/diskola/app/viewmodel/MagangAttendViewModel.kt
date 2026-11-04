package id.diskola.app.viewmodel

import android.content.IntentSender
import android.location.Location
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.MagangScheduleItem
import id.diskola.app.repository.MagangRepository
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.MagangRules
import id.diskola.app.utils.PresensiRules
import id.diskola.app.utils.PresensiRules.MasukStatus
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

/** Everything the check-in screen needs about the company and where the student stands against it. */
data class MagangAttendView(
    val companyName: String,
    val address: String,
    val dateLabel: String,
    val timeLabel: String,
    val lat: Double,
    val lng: Double,
    val radius: Float,
    /** The radius is enforced: the circle is drawn and the student must be inside it. */
    val enforced: Boolean,
    /** Meters to the company, null until there is a fix (or when the company has no coordinates). */
    val distance: Float?,
    val status: MasukStatus,
)

/**
 * Magang check-in ("Presensi Masuk", doc `07` §11.3). The schedule row comes from the shell's shared [MagangViewModel] (the screen hands it
 * in with [setSchedule]); this one owns the live position and the submit. There is no geofence and no background location: the position is
 * followed only while the screen is open.
 */
@HiltViewModel
class MagangAttendViewModel @Inject constructor(
    private val repository: MagangRepository,
    private val locationProvider: LocationProvider,
) : BaseViewModel() {

    private val _schedule = MutableStateFlow<MagangScheduleItem?>(null)
    private val _location = MutableStateFlow<Location?>(null)

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success.asStateFlow()

    val userName: String get() = repository.user.name
    val userAvatar: String get() = repository.user.avatar

    val view: StateFlow<MagangAttendView?> = combine(_schedule, _location) { item, loc -> item?.let { buildView(it, loc) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val location: StateFlow<Location?> = _location.asStateFlow()

    private fun buildView(item: MagangScheduleItem, loc: Location?): MagangAttendView {
        val company = item.company
        val lat = MagangRules.coordinate(company?.latitude)
        val lng = MagangRules.coordinate(company?.longitude)
        val hasCoordinates = MagangRules.hasCoordinates(lat, lng)
        val radius = PresensiRules.radiusMeters(company?.radius.orEmpty())
        val distance = if (loc != null && hasCoordinates) distanceMeters(loc.latitude, loc.longitude, lat, lng) else null
        val time = item.schedule
        return MagangAttendView(
            companyName = MagangRules.companyName(item),
            address = company?.address.orEmpty(),
            dateLabel = MagangRules.dateLabel(time?.date),
            timeLabel = MagangRules.timeRange(time?.start_time.orEmpty(), time?.end_time.orEmpty()),
            lat = lat,
            lng = lng,
            radius = radius,
            enforced = MagangRules.radiusEnforced(item.attendance),
            distance = distance,
            status = MagangRules.attendStatus(
                state = item.attendance,
                companyHasCoordinates = hasCoordinates,
                hasLocation = loc != null,
                isMock = loc?.let(MockLocationGuard::isMock) == true,
                radius = radius,
                distanceMeters = distance,
            ),
        )
    }

    /** The shared list's current row for this schedule; null while it is not there (not loaded, or gone after a refresh). */
    fun setSchedule(item: MagangScheduleItem?) {
        _schedule.value = item
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

    /** Sent only once the screen offers the button, which already means a fix inside the allowed area; the guards are for a late change. */
    fun submit() {
        val item = _schedule.value ?: return
        val loc = _location.value
        if (loc == null) {
            emitError("Gagal mendapatkan lokasi Anda")
            return
        }
        if (MockLocationGuard.isMock(loc)) {
            emitError(MockLocationGuard.MESSAGE)
            return
        }
        viewModelScope.launch {
            _submitting.value = true
            clearError()
            try {
                repository.attend(item.id, loc.latitude, loc.longitude)
                _success.value = true
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
