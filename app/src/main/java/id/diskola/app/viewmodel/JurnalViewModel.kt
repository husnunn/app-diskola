package id.diskola.app.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.diskola.app.repository.JurnalRepository
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.JurnalRow
import id.diskola.app.utils.JurnalRules
import id.diskola.app.utils.location.LocationProvider
import id.diskola.app.utils.location.MockLocationGuard
import java.time.LocalTime
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/** A result dialog (success or failure) the list screen shows once; [openRow] = open that class's detail on "Ok". */
data class JurnalNotice(val title: String, val message: String, val openRow: JurnalRow? = null)

/**
 * Jurnal KBM list — today's hours and class sessions (doc `07` §7.1). Loads once in `init`; coming back from a
 * child screen refreshes once (the first resume is the open itself). "Mulai kelas" and a QR check-in refresh the
 * list afterwards — legacy left the old button on screen until a manual pull.
 */
@HiltViewModel
class JurnalViewModel @Inject constructor(
    private val repository: JurnalRepository,
    private val locationProvider: LocationProvider,
    @ApplicationContext private val context: Context,
) : BaseViewModel() {

    val isStudent: Boolean get() = repository.isStudent
    val isTeacher: Boolean get() = repository.isTeacher

    private val _rows = MutableStateFlow<List<JurnalRow>>(emptyList())
    val rows: StateFlow<List<JurnalRow>> = _rows.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** An action (Mulai kelas / QR) is in flight — shown as a blocking dialog, unlike the list's pull-to-refresh. */
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _notice = MutableStateFlow<JurnalNotice?>(null)
    val notice: StateFlow<JurnalNotice?> = _notice.asStateFlow()

    private var firstResumeSeen = false

    init {
        refresh()
    }

    fun dismissNotice() {
        _notice.value = null
    }

    fun refresh() {
        launchWithHandling { reload() }
    }

    fun onScreenResumed() {
        if (!firstResumeSeen) {
            firstResumeSeen = true
            return
        }
        refresh()
    }

    private suspend fun reload() {
        _rows.value = JurnalRules.buildRows(repository.schedule(), repository.isStudent, LocalTime.now())
        _loaded.value = true
    }

    /** Teacher "Mulai kelas". Location is best effort; a mock location is refused. */
    fun startClass(row: JurnalRow) {
        val attendanceId = row.attendanceId ?: return
        viewModelScope.launch {
            _busy.value = true
            clearError()
            try {
                val fix = bestEffortLocation()
                if (fix != null && MockLocationGuard.isMock(fix)) {
                    emitError(MockLocationGuard.MESSAGE)
                    return@launch
                }
                repository.teacherCheckIn(attendanceId, fix?.latitude, fix?.longitude)
                runCatching { reload() }
                // refreshed row if the server now has one (created_at/late_at), else the one tapped
                val fresh = _rows.value.firstOrNull { it.attendanceId == attendanceId } ?: row
                _notice.value = JurnalNotice("Absensi Berhasil", "Absensi berhasil", openRow = fresh)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                emitError(AppErrorHandler.getMessage(e))
            } finally {
                _busy.value = false
            }
        }
    }

    /** Student QR check-in: [raw] is what the scanner read. */
    fun submitQr(raw: String) {
        val payload = JurnalRules.parseQr(raw)
        if (payload == null) {
            _notice.value = JurnalNotice("Presensi Gagal", "QR Code tidak valid untuk presensi kelas")
            return
        }
        viewModelScope.launch {
            _busy.value = true
            try {
                repository.submitQr(payload)
                _notice.value = JurnalNotice("Presensi Berhasil", "Selamat datang dan selamat belajar. Belajarlah dengan giat untuk meraih masa depan Anda!")
                runCatching { reload() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _notice.value = JurnalNotice("Presensi Gagal", AppErrorHandler.getMessage(e))
            } finally {
                _busy.value = false
            }
        }
    }

    private suspend fun bestEffortLocation(): Location? {
        val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (!granted) return null
        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) { locationProvider.freshLocation() ?: locationProvider.currentLocation() }
    }

    private companion object {
        const val LOCATION_TIMEOUT_MS = 4_000L
    }
}
