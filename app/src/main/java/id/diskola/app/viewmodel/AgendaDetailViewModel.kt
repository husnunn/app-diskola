package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.AgendaDay
import id.diskola.app.dataclass.ResponData.StaffAgendaItem
import id.diskola.app.repository.AgendaRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** "Detail Agenda" — a read-only snapshot of one cached session. */
@HiltViewModel
class AgendaDetailViewModel @Inject constructor(
    private val repository: AgendaRepository,
) : BaseViewModel() {

    private var started = false

    private val _item = MutableStateFlow<StaffAgendaItem?>(null)
    val item: StateFlow<StaffAgendaItem?> = _item.asStateFlow()

    private val _day = MutableStateFlow<AgendaDay?>(null)
    val day: StateFlow<AgendaDay?> = _day.asStateFlow()

    private val _notFound = MutableStateFlow(false)
    val notFound: StateFlow<Boolean> = _notFound.asStateFlow()

    fun start(date: String, agendaId: Int) {
        if (started) return
        started = true
        viewModelScope.launch {
            val found = repository.getItem(date, agendaId)
            if (found == null) _notFound.value = true else {
                _day.value = found.first
                _item.value = found.second
            }
        }
    }
}
