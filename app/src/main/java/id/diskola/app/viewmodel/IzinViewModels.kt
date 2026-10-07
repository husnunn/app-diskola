package id.diskola.app.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.diskola.app.dataclass.ResponData.LeaveRequestTable
import id.diskola.app.repository.IzinGate
import id.diskola.app.repository.IzinRepository
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.PresensiRules
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/** "Pengajuan Izin" — Room-backed list; the server is touched on open, on pull-to-refresh and for further pages. */
@HiltViewModel
class IzinListViewModel @Inject constructor(
    private val repository: IzinRepository,
) : BaseViewModel() {

    /** `pending`/`approved`/`rejected`, or null for "Semua". */
    private val _filter = MutableStateFlow<String?>(null)
    val filter: StateFlow<String?> = _filter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<LeaveRequestTable>> = _filter
        .flatMapLatest { repository.observe(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _gate = MutableStateFlow(IzinGate(block = null, rejectedToday = false))
    val gate: StateFlow<IzinGate> = _gate.asStateFlow()

    private var page = 1
    private var hasNext = false
    private var loadingMore = false
    private var started = false

    /** [initialFilter] comes from the route (notification deep links open pre-filtered). */
    fun start(initialFilter: String) {
        if (started) return
        started = true
        _filter.value = initialFilter.takeIf { it in setOf("pending", "approved", "rejected") }
        refresh()
    }

    fun setFilter(value: String?) {
        _filter.value = value
    }

    fun refresh() {
        launchWithHandling {
            hasNext = repository.refresh()
            page = 1
            repository.refreshGate(includeToday = false)
            _gate.value = repository.gate()
        }
    }

    /** Called when the user returns from the add form. */
    fun onChanged() = refresh()

    fun loadMore() {
        if (loadingMore || !hasNext) return
        loadingMore = true
        launchWithHandling(showLoading = false, showError = false) {
            hasNext = repository.loadPage(page + 1)
            page += 1
        }.invokeOnCompletion { loadingMore = false }
    }
}

data class LeaveProof(val file: File, val displayName: String, val mimeType: String)

/** "Tambah Izin" — form with a file (PDF/PNG/JPEG ≤ 2 MB), jenis and keterangan. */
@HiltViewModel
class IzinAddViewModel @Inject constructor(
    private val repository: IzinRepository,
    @ApplicationContext private val context: Context,
) : BaseViewModel() {

    private val _gate = MutableStateFlow(IzinGate(block = null, rejectedToday = false))
    val gate: StateFlow<IzinGate> = _gate.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _proof = MutableStateFlow<LeaveProof?>(null)
    val proof: StateFlow<LeaveProof?> = _proof.asStateFlow()

    /** Inline message under the file hint (file problems), separate from the banner. */
    private val _fileError = MutableStateFlow<String?>(null)
    val fileError: StateFlow<String?> = _fileError.asStateFlow()

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    init {
        viewModelScope.launch {
            _gate.value = repository.gate()
            repository.refreshGate()
            _gate.value = repository.gate()
        }
    }

    fun onStatusChange(value: String) { _status.value = value }
    fun onNoteChange(value: String) { _note.value = value }

    fun onFilePicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val picked = withContext(Dispatchers.IO) { copyToCache(uri) }
            when (picked) {
                is PickResult.Ok -> {
                    _proof.value = picked.proof
                    _fileError.value = null
                }
                is PickResult.Error -> {
                    _proof.value = null
                    _fileError.value = picked.message
                }
            }
        }
    }

    fun submit() {
        val gate = _gate.value
        if (gate.block != null) {
            emitError(gate.block.message)
            return
        }
        val proof = _proof.value
        val error = PresensiRules.validateLeave(
            hasFile = proof != null,
            status = _status.value,
            note = _note.value,
            fileBytes = proof?.file?.length() ?: 0L,
        )
        if (error != null) {
            if (proof == null) _fileError.value = error else emitError(error)
            return
        }
        launchWithHandling(customMessage = null) {
            repository.submit(_status.value.orEmpty(), _note.value, proof!!.file, proof.mimeType)
            _done.value = true
        }
    }

    private sealed interface PickResult {
        data class Ok(val proof: LeaveProof) : PickResult
        data class Error(val message: String) : PickResult
    }

    private fun copyToCache(uri: Uri): PickResult {
        return try {
            val mime = context.contentResolver.getType(uri).orEmpty()
            val extension = when (mime) {
                "application/pdf" -> "pdf"
                "image/png" -> "png"
                "image/jpeg", "image/jpg" -> "jpg"
                else -> return PickResult.Error("Format file tidak didukung. Gunakan PDF, JPG, atau PNG")
            }
            var displayName = "bukti.$extension"
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() }?.let { displayName = it }
            }
            val target = File(context.cacheDir, "leave_proof_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
                ?: return PickResult.Error("File tidak dapat dibaca")
            if (target.length() > PresensiRules.LEAVE_FILE_MAX_BYTES) {
                target.delete()
                return PickResult.Error("Ukuran file maksimal 2 MB")
            }
            PickResult.Ok(LeaveProof(target, displayName, mime))
        } catch (e: Exception) {
            Timber.e(e)
            PickResult.Error("File tidak dapat dibaca")
        }
    }
}

/** "Detail Pengajuan" — Room first; a cold start (notification) syncs once, then looks again. */
@HiltViewModel
class IzinDetailViewModel @Inject constructor(
    private val repository: IzinRepository,
) : BaseViewModel() {

    private val _item = MutableStateFlow<LeaveRequestTable?>(null)
    val item: StateFlow<LeaveRequestTable?> = _item.asStateFlow()

    private val _notFound = MutableStateFlow(false)
    val notFound: StateFlow<Boolean> = _notFound.asStateFlow()

    private var started = false

    fun start(uuid: String) {
        if (started) return
        started = true
        if (uuid.isBlank()) {
            _notFound.value = true
            return
        }
        viewModelScope.launch {
            var found = repository.get(uuid)
            if (found == null) {
                try {
                    repository.refresh()
                    found = repository.get(uuid)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e)
                    emitError(AppErrorHandler.getMessage(e))
                }
            }
            if (found == null) _notFound.value = true else _item.value = found
        }
    }
}
