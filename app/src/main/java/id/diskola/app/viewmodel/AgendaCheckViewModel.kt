package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.AgendaCheckMode
import id.diskola.app.dataclass.ResponData.AgendaDay
import id.diskola.app.dataclass.ResponData.StaffAgendaItem
import id.diskola.app.repository.AgendaRepository
import id.diskola.app.utils.location.LocationProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AgendaLocationState {
    /** Permission not asked/answered yet. */
    data object Idle : AgendaLocationState
    data object Fetching : AgendaLocationState
    data object Unavailable : AgendaLocationState
    data class Ready(
        val latitude: Double,
        val longitude: Double,
        val address: String?,
        val searchingAddress: Boolean,
    ) : AgendaLocationState
}

/**
 * "Lapor Masuk/Pulang Agenda". Unlike legacy, submitting is only possible once a real GPS fix
 * exists (legacy let a stale or missing location through), and the item/gate come from the Room
 * cache by `(date, agendaId)` rather than a `Serializable` extra.
 */
@HiltViewModel
class AgendaCheckViewModel @Inject constructor(
    private val repository: AgendaRepository,
    private val locationProvider: LocationProvider,
) : BaseViewModel() {

    private var started = false
    private lateinit var date: String
    lateinit var mode: AgendaCheckMode
        private set

    private val _item = MutableStateFlow<StaffAgendaItem?>(null)
    val item: StateFlow<StaffAgendaItem?> = _item.asStateFlow()

    private val _day = MutableStateFlow<AgendaDay?>(null)
    val day: StateFlow<AgendaDay?> = _day.asStateFlow()

    private val _location = MutableStateFlow<AgendaLocationState>(AgendaLocationState.Idle)
    val location: StateFlow<AgendaLocationState> = _location.asStateFlow()

    /** CHECK_IN without a school-gate presensi: the screen shows the "presensi dulu" alert and leaves. */
    private val _gateBlocked = MutableStateFlow(false)
    val gateBlocked: StateFlow<Boolean> = _gateBlocked.asStateFlow()

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    private val _notFound = MutableStateFlow(false)
    val notFound: StateFlow<Boolean> = _notFound.asStateFlow()

    fun start(date: String, agendaId: Int, mode: AgendaCheckMode) {
        if (started) return
        started = true
        this.date = date
        this.mode = mode
        viewModelScope.launch {
            val found = repository.getItem(date, agendaId)
            if (found == null) {
                _notFound.value = true
                return@launch
            }
            _day.value = found.first
            _item.value = found.second
            if (mode == AgendaCheckMode.CHECK_IN && found.first.gate.gateIn == null) _gateBlocked.value = true
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) fetchLocation() else _location.value = AgendaLocationState.Unavailable
    }

    fun fetchLocation() {
        viewModelScope.launch {
            _location.value = AgendaLocationState.Fetching
            val fix = locationProvider.currentLocation()
            if (fix == null) {
                _location.value = AgendaLocationState.Unavailable
                return@launch
            }
            _location.value = AgendaLocationState.Ready(fix.latitude, fix.longitude, address = null, searchingAddress = true)
            val address = locationProvider.addressOf(fix.latitude, fix.longitude)
            val current = _location.value
            // A newer fix may have replaced this one while the geocoder was running.
            if (current is AgendaLocationState.Ready && current.latitude == fix.latitude && current.longitude == fix.longitude) {
                _location.value = current.copy(address = address, searchingAddress = false)
            }
        }
    }

    fun submit() {
        val ready = _location.value as? AgendaLocationState.Ready ?: return
        val agenda = _item.value ?: return
        launchWithHandling {
            if (mode == AgendaCheckMode.CHECK_IN) {
                repository.checkIn(date, agenda.agenda_id, ready.latitude, ready.longitude)
            } else {
                repository.checkOut(date, agenda.agenda_id, ready.latitude, ready.longitude)
            }
            _done.value = true
        }
    }
}
