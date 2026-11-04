package id.diskola.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.repository.FileOpenRepository
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Brings a stored proof file (a PDF of an izin) into the app so it is read in the in-app viewer instead of being handed to another app:
 * it is downloaded to the app's own files, then [open] gives the copy to the caller to show. Pictures never come here; they are drawn in place.
 */
@HiltViewModel
class ProofFileViewModel @Inject constructor(
    private val files: FileOpenRepository,
) : ViewModel() {

    private val _opening = MutableStateFlow(false)
    val opening: StateFlow<Boolean> = _opening.asStateFlow()

    /** Why the last [open] could not show the file; null when there is nothing to tell. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun dismissError() {
        _error.value = null
    }

    /** Downloads [url] and calls [onPdf] with the local copy; anything that is not a PDF is reported, not handed to another app. */
    fun open(url: String, fileName: String, onPdf: (File) -> Unit) {
        if (_opening.value) return
        viewModelScope.launch {
            _opening.value = true
            _error.value = null
            try {
                val file = files.downloadToAppFiles(url, fileName)
                if (files.isPdf(file.name)) onPdf(file) else _error.value = "Jenis file ini tidak dapat dibuka di dalam aplikasi."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _error.value = "File tidak dapat dibuka. Periksa koneksi internet Anda lalu coba lagi."
            } finally {
                _opening.value = false
            }
        }
    }
}
