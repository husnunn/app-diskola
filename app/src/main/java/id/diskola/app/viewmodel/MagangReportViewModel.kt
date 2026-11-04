package id.diskola.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.diskola.app.repository.MagangRepository
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.MagangRules
import id.diskola.app.utils.PhotoCapture
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The daily report that also checks the student out (doc `07` §11.4): a picture of at most 5 MB and notes of at least 100 characters.
 * A picture that is too big is compressed (legacy refused it, with a message that said 8 MB); one that cannot get under the limit, or
 * cannot be read, is reported. "Laporan tersimpan" is only ever shown after the server accepted the report.
 */
@HiltViewModel
class MagangReportViewModel @Inject constructor(
    private val repository: MagangRepository,
    @ApplicationContext private val context: Context,
) : BaseViewModel() {

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _photo = MutableStateFlow<File?>(null)
    val photo: StateFlow<File?> = _photo.asStateFlow()

    /** The picked picture is being compressed. */
    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean> = _processing.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success.asStateFlow()

    fun onNotesChange(value: String) {
        _notes.value = value
    }

    fun removePhoto() {
        _photo.value = null
    }

    fun reportError(message: String) = emitError(message)

    /** [uri] is what the camera or the gallery returned; null = the user backed out. */
    fun onPhotoPicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _processing.value = true
            clearError()
            val file = PhotoCapture.compress(context, uri, MagangRules.REPORT_IMAGE_MAX_BYTES.toInt())
            _processing.value = false
            if (file == null) emitError("Foto tidak dapat diproses atau lebih dari 5 MB.") else _photo.value = file
        }
    }

    fun submit(scheduleId: Long) {
        val photo = _photo.value
        val invalid = MagangRules.validateReport(photo != null, photo?.length() ?: 0L, _notes.value)
        if (invalid != null || photo == null) {
            emitError(invalid ?: "Harap unggah gambar terlebih dahulu")
            return
        }
        viewModelScope.launch {
            _submitting.value = true
            clearError()
            try {
                repository.sendDailyReport(scheduleId, _notes.value, photo)
                _success.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                // The reason is shown and the form stays as typed, so the student can send it again (legacy said "tersimpan" regardless).
                emitError(AppErrorHandler.getMessage(e))
            } finally {
                _submitting.value = false
            }
        }
    }
}
