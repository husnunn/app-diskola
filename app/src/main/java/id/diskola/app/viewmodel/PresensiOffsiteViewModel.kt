package id.diskola.app.viewmodel

import android.location.Location
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.apiservice.ApiException
import id.diskola.app.repository.OffsiteAvailability
import id.diskola.app.repository.PresensiRepository
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.PresensiRules
import id.diskola.app.utils.location.LocationProvider
import id.diskola.app.utils.location.MockLocationGuard
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Presensi Dinas Luar (teachers only, doc `07` §6): address + keterangan + selfie, sent with a *fresh*
 * GPS fix. Differences from legacy: the keterangan length is counted trimmed, a photo taken before the
 * address was edited is refused (it would not match the burnt-in text), and a mock location is rejected.
 */
@HiltViewModel
class PresensiOffsiteViewModel @Inject constructor(
    private val repository: PresensiRepository,
    private val locationProvider: LocationProvider,
) : BaseViewModel() {

    val isStudent: Boolean get() = repository.isStudent

    private val _availability = MutableStateFlow(OffsiteAvailability.Disabled)
    val availability: StateFlow<OffsiteAvailability> = _availability.asStateFlow()

    /** Replaces the button label when the server answered with its own text (e.g. after a refusal). */
    private val _statusLabel = MutableStateFlow("")
    val statusLabel: StateFlow<String> = _statusLabel.asStateFlow()

    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _photo = MutableStateFlow<File?>(null)
    val photo: StateFlow<File?> = _photo.asStateFlow()
    private var photoAddress: String = ""

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _fieldErrors = MutableStateFlow(PresensiRules.OffsiteErrors())
    val fieldErrors: StateFlow<PresensiRules.OffsiteErrors> = _fieldErrors.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()

    private val _availabilityLoading = MutableStateFlow(true)
    val availabilityLoading: StateFlow<Boolean> = _availabilityLoading.asStateFlow()

    init {
        if (!repository.isStudent) {
            viewModelScope.launch {
                _availability.value = repository.offsiteAvailability()
                _availabilityLoading.value = false
            }
        }
    }

    // ---- Inputs ----

    fun onAddressChange(value: String) { _address.value = value }
    fun onNoteChange(value: String) { _note.value = value }

    fun locationUpdates(): Flow<Location> = locationProvider.locationUpdates()

    /** First fix fills an empty address from the geocoder; later fixes only move the map. */
    fun onLocation(fix: Location) {
        val first = _location.value == null
        _location.value = fix
        if (first && _address.value.isBlank()) {
            viewModelScope.launch {
                locationProvider.addressOf(fix.latitude, fix.longitude)?.let { if (_address.value.isBlank()) _address.value = it }
            }
        }
    }

    fun seedLocation() {
        viewModelScope.launch { locationProvider.freshLocation()?.let { if (_location.value == null) onLocation(it) } }
    }

    /** False (with the legacy messages) when the address is still empty — the photo's watermark needs it. */
    fun canTakePhoto(): Boolean {
        if (_address.value.isBlank()) {
            _fieldErrors.value = _fieldErrors.value.copy(address = "Alamat wajib diisi")
            emitError("Isi alamat terlebih dahulu sebelum mengambil foto")
            return false
        }
        return true
    }

    fun onPhotoCaptured(path: String) {
        _photo.value = File(path)
        photoAddress = _address.value.trim()
        _fieldErrors.value = _fieldErrors.value.copy(photo = null)
    }

    // ---- Submit ----

    fun submit() {
        val availability = _availability.value
        val errors = PresensiRules.validateOffsite(_note.value, _address.value, _photo.value != null, availability.requirePhoto)
        val stale = _photo.value != null && photoAddress != _address.value.trim()
        _fieldErrors.value = if (stale) errors.copy(photo = "Alamat berubah setelah foto diambil. Ambil foto ulang.") else errors
        if (errors.any || stale) return
        val checkIn = availability.isCheckIn
        if (!checkIn && !availability.isCheckOut) {
            _statusLabel.value = "Presensi tidak dapat dilakukan saat ini"
            return
        }
        viewModelScope.launch {
            _submitting.value = true
            clearError()
            try {
                val fix = locationProvider.freshLocation() ?: _location.value
                if (fix == null) {
                    emitError("Gagal mendapatkan lokasi Anda")
                    return@launch
                }
                if (MockLocationGuard.isMock(fix)) {
                    emitError(MockLocationGuard.MESSAGE)
                    return@launch
                }
                val address = _address.value.ifBlank {
                    locationProvider.addressOf(fix.latitude, fix.longitude).orEmpty()
                }
                if (address.isBlank()) {
                    _fieldErrors.value = _fieldErrors.value.copy(address = "Alamat wajib diisi")
                    return@launch
                }
                val response = repository.offsiteSubmit(checkIn, fix.latitude, fix.longitude, address, _note.value, _photo.value)
                if (PresensiRules.offsiteSucceeded(response.code, response.data?.status)) {
                    val data = response.data
                    _success.value = "Presensi dinas luar berhasil (${data?.status.orEmpty()}) pada ${data?.date.orEmpty()} pukul ${data?.time.orEmpty()}."
                } else {
                    // Refused by the server: its own state (label/button) replaces ours, as in legacy.
                    _statusLabel.value = response.data?.attendance_response_text.orEmpty().ifBlank { response.message }
                    response.data?.let { data ->
                        _availability.value = availability.copy(
                            allowAttendance = data.allow_attendance == true,
                            typeAttendance = data.type_attendance.ifBlank { availability.typeAttendance },
                            buttonLabel = data.attendance_response_text_button.ifBlank { availability.buttonLabel },
                        )
                    }
                    if (_statusLabel.value.isBlank()) emitError("Presensi dinas luar gagal")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                Timber.e(e)
                if (e.responseCode == 403) {
                    _availability.value = OffsiteAvailability.Disabled
                    _statusLabel.value = "Presensi dinas luar tidak aktif untuk sekolah ini"
                } else {
                    emitError(e.validationMessages.joinToString("\n").ifBlank { AppErrorHandler.getMessage(e) })
                }
            } catch (e: Exception) {
                Timber.e(e)
                emitError(AppErrorHandler.getMessage(e))
            } finally {
                _submitting.value = false
            }
        }
    }
}
