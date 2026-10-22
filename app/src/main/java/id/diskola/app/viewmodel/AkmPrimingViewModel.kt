package id.diskola.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.repository.AkmRepository
import id.diskola.app.repository.AkmSettingsRepository
import id.diskola.app.ui.navigation.Route
import id.diskola.app.utils.AkmRules
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What the "Sebelum Memulai Ujian" screen needs (doc 06 §5.4): whether strict mode applies (the lock callout shows only then — the
 * switches were refreshed a moment earlier, when the terms were shown) and the "Dibatalkan" mark of the schedule (a deviation from
 * legacy, decided 2026-10-16: this screen marks a cancelled schedule too). Nothing here records a violation or starts the session.
 */
@HiltViewModel
class AkmPrimingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AkmRepository,
    settings: AkmSettingsRepository,
) : ViewModel() {

    val examId: Int = savedStateHandle.toRoute<Route.Akm.Priming>().id

    val strict: Boolean = settings.isStrict()

    private val _watermark = MutableStateFlow(AkmRules.AkmWatermark.None)
    val watermark: StateFlow<AkmRules.AkmWatermark> = _watermark.asStateFlow()

    init {
        viewModelScope.launch { _watermark.value = repository.watermarkOf(examId) }
    }
}
