package id.diskola.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.diskola.app.repository.MagangRepository
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.MagangRules
import id.diskola.app.utils.MagangRules.LeaveField
import id.diskola.app.utils.PhotoCapture
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/** The proof picked for an izin: the compressed copy that will be sent and the name shown for it. */
data class MagangLeaveProof(val file: File, val displayName: String)

/**
 * "Ajukan Izin" of Magang (doc `07` §11.6). The proof is a picture of at most 2 MB; a larger one is compressed (legacy refused it), a file
 * that is not a picture is refused at pick time. Which schedule the izin belongs to, and whether one may be filed today at all, is decided by
 * the screen from the shared lists; this holds the form and the send. A failed send keeps the form as typed.
 */
@HiltViewModel
class MagangIzinAddViewModel @Inject constructor(
    private val repository: MagangRepository,
    @ApplicationContext private val context: Context,
) : BaseViewModel() {

    private val _type = MutableStateFlow<String?>(null)
    val type: StateFlow<String?> = _type.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _proof = MutableStateFlow<MagangLeaveProof?>(null)
    val proof: StateFlow<MagangLeaveProof?> = _proof.asStateFlow()

    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean> = _processing.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    /** The first problem of the last submit, shown under the field it belongs to. */
    private val _invalid = MutableStateFlow<MagangRules.LeaveInvalid?>(null)
    val invalid: StateFlow<MagangRules.LeaveInvalid?> = _invalid.asStateFlow()

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    fun onTypeChange(value: String) {
        _type.value = value
        clearInvalid(LeaveField.Type)
    }

    fun onNoteChange(value: String) {
        _note.value = value
        clearInvalid(LeaveField.Note)
    }

    fun removeProof() {
        _proof.value = null
    }

    private fun clearInvalid(field: LeaveField) {
        if (_invalid.value?.field == field) _invalid.value = null
    }

    fun onFilePicked(uri: Uri?) {
        if (uri == null) return
        val mime = context.contentResolver.getType(uri)
        MagangRules.leaveFileError(mime)?.let {
            _invalid.value = MagangRules.LeaveInvalid(LeaveField.File, it)
            return
        }
        viewModelScope.launch {
            _processing.value = true
            val file = PhotoCapture.compress(context, uri, MagangRules.LEAVE_IMAGE_MAX_BYTES.toInt())
            _processing.value = false
            if (file == null) {
                _invalid.value = MagangRules.LeaveInvalid(LeaveField.File, "Gambar tidak dapat diproses atau lebih dari 2 MB")
            } else {
                _proof.value = MagangLeaveProof(file, uri.lastPathSegment?.substringAfterLast('/') ?: file.name)
                clearInvalid(LeaveField.File)
            }
        }
    }

    /** [scheduleId] is what the screen resolved (0 = none to choose from). */
    fun submit(scheduleId: Long) {
        val proof = _proof.value
        val invalid = MagangRules.validateLeave(scheduleId, proof != null, _type.value, _note.value, proof?.file?.length() ?: 0L)
        _invalid.value = invalid
        if (invalid != null || proof == null) return
        val type = _type.value ?: return
        viewModelScope.launch {
            _submitting.value = true
            clearError()
            try {
                repository.submitLeave(scheduleId, type, _note.value, proof.file)
                _done.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                emitError(AppErrorHandler.getMessage(e))
            } finally {
                _submitting.value = false
            }
        }
    }
}
