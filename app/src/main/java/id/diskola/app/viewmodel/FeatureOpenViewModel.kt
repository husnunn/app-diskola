package id.diskola.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.repository.FeatureGateRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The server-side feature switch (`check-feature-availability`) in front of a screen that is opened from somewhere other than the Home tile:
 * the Home tile has its own copy of this in `HomeViewModel`. Checked once per tap; an error counts as available (fail-open, as legacy).
 */
@HiltViewModel
class FeatureOpenViewModel @Inject constructor(
    private val featureGate: FeatureGateRepository,
) : ViewModel() {

    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    private val _unavailable = MutableStateFlow<FeatureUnavailable?>(null)
    val unavailable: StateFlow<FeatureUnavailable?> = _unavailable.asStateFlow()

    /** Runs [onAvailable] when [name] is on; otherwise [unavailable] is set so the screen can say so. */
    fun open(name: String, onAvailable: () -> Unit) {
        if (_checking.value) return
        _checking.value = true
        viewModelScope.launch {
            val result = featureGate.check(name)
            _checking.value = false
            if (result.available) onAvailable() else _unavailable.value = FeatureUnavailable(name, result.message.orEmpty())
        }
    }

    fun dismiss() {
        _unavailable.value = null
    }
}
