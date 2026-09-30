package id.diskola.app.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.apiservice.AbsensiApiService
import id.diskola.app.dataclass.ResponData.*
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class AbsensiViewModel @Inject constructor(
    private val absensiApiService: AbsensiApiService
) : BaseViewModel() {

    private val _schedules = MutableStateFlow<List<AttendanceScheduleItem>>(emptyList())
    val schedules: StateFlow<List<AttendanceScheduleItem>> = _schedules.asStateFlow()

    private val _attendanceDetail = MutableStateFlow<AttendanceDetailData?>(null)
    val attendanceDetail: StateFlow<AttendanceDetailData?> = _attendanceDetail.asStateFlow()

    private val _actionSuccess = MutableStateFlow<AttendanceActionData?>(null)
    val actionSuccess: StateFlow<AttendanceActionData?> = _actionSuccess.asStateFlow()

    fun getSchedules(date: String? = null) {
        launchWithHandling {
            val response = absensiApiService.getAttendanceSchedule(date)
            _schedules.value = response.data
        }
    }

    fun getDetail(scheduleId: Int, date: String? = null) {
        launchWithHandling {
            val response = absensiApiService.getAttendanceDetail(scheduleId, date)
            _attendanceDetail.value = response.data
        }
    }

    fun attend(scheduleId: Int, password: String) {
        launchWithHandling {
            val request = AttendRequest(scheduleId, password)
            val response = absensiApiService.attendClass(request)
            _actionSuccess.value = response.data
            // Refresh list after action
            getSchedules()
        }
    }

    fun leave(scheduleId: Int) {
        launchWithHandling {
            val request = LeaveRequest(scheduleId)
            val response = absensiApiService.leaveClass(request)
            _actionSuccess.value = response.data
            // Refresh list after action
            getSchedules()
        }
    }

    // Teacher actions
    fun startAttendance(scheduleId: Int, password: String, lateLimit: Int) {
        launchWithHandling {
            val request = StartAttendanceRequest(scheduleId, password, lateLimit)
            val response = absensiApiService.startAttendance(request)
            _actionSuccess.value = response.data
            getSchedules()
        }
    }

    fun endAttendance(scheduleId: Int) {
        launchWithHandling {
            val request = EndAttendanceRequest(scheduleId)
            val response = absensiApiService.endAttendance(request)
            _actionSuccess.value = response.data
            getSchedules()
        }
    }
}
