package id.diskola.app.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.repository.PoinRepository
import id.diskola.app.repository.PoinStudentSummary
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * "Poin Saya" — one `score-student` call feeds both tabs (the endpoint has no params and returns
 * everything), so there is no per-tab fetch and no Room cache. Loads once in `init` (a screen-level
 * `LaunchedEffect` would double-fetch on re-entry, same lesson as the Guru list screens).
 */
@HiltViewModel
class PoinSiswaViewModel @Inject constructor(
    private val repository: PoinRepository,
) : BaseViewModel() {

    private val _summary = MutableStateFlow<PoinStudentSummary?>(null)
    val summary: StateFlow<PoinStudentSummary?> = _summary.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    private fun load() = launchWithHandling {
        _summary.value = repository.getMyScore()
    }
}
