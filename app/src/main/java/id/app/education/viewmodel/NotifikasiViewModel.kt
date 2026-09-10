package id.app.education.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import id.app.education.dataclass.mock.MockNotifications
import id.app.education.dataclass.mock.NotificationItem
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI-only for now — there is no notification backend/endpoint yet (see `ref/fase2`). Swap
 * [MockNotifications.seed] for a real repository call once the API exists; the StateFlow shape
 * below is designed so that swap doesn't touch the screen.
 */
@HiltViewModel
class NotifikasiViewModel @Inject constructor() : ViewModel() {

    private val _items = MutableStateFlow(MockNotifications.seed)
    val items: StateFlow<List<NotificationItem>> = _items.asStateFlow()

    private val _unreadOnly = MutableStateFlow(false)
    val unreadOnly: StateFlow<Boolean> = _unreadOnly.asStateFlow()

    val unreadCount: Int get() = _items.value.count { it.unread }

    fun setUnreadOnly(value: Boolean) {
        _unreadOnly.value = value
    }

    fun markRead(id: String) {
        _items.value = _items.value.map { if (it.id == id) it.copy(unread = false) else it }
    }

    fun findById(id: String): NotificationItem? = _items.value.find { it.id == id }
}
