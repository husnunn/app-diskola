package id.app.education.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.app.education.utils.AppErrorHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

open class BaseViewModel : ViewModel() {

    protected val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    protected val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    protected fun emitError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = ""
    }

    protected fun handleError(
        throwable: Throwable,
        customMessage: String? = null
    ) {
        Timber.e(throwable)
        _errorMessage.value = customMessage ?: AppErrorHandler.getMessage(throwable)
    }

    protected fun launchWithHandling(
        showLoading: Boolean = true,
        showError: Boolean = true,
        customMessage: String? = null,
        block: suspend () -> Unit
    ): Job {
        return viewModelScope.launch {
            if (showLoading) _loading.value = true
            try {
                block()
            } catch (e: CancellationException) {
                Timber.d("Coroutine cancelled: ${e.message}")
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                if (showError) {
                    _errorMessage.value = customMessage ?: AppErrorHandler.getMessage(e)
                }
            } finally {
                if (showLoading) _loading.value = false
            }
        }
    }
}
