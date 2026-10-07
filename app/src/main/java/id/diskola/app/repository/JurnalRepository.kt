package id.diskola.app.repository

import id.diskola.app.apiservice.ApiException
import id.diskola.app.apiservice.JurnalApiService
import id.diskola.app.dataclass.ResponData.JournalCreatedAttendance
import id.diskola.app.dataclass.ResponData.JournalPreviewData
import id.diskola.app.dataclass.ResponData.JournalSaveRequest
import id.diskola.app.dataclass.ResponData.JournalSaveStudent
import id.diskola.app.dataclass.ResponData.JournalUpdateRequest
import id.diskola.app.dataclass.ResponData.JurnalClassItem
import id.diskola.app.dataclass.ResponData.JurnalPlot
import id.diskola.app.dataclass.ResponData.JurnalSubjectItem
import id.diskola.app.dataclass.ResponData.JurnalTeacherItem
import id.diskola.app.dataclass.ResponData.LearningQrRequest
import id.diskola.app.dataclass.ResponData.ListStudentRow
import id.diskola.app.dataclass.ResponData.PlotItem
import id.diskola.app.dataclass.ResponData.ScheduleDetailData
import id.diskola.app.dataclass.ResponData.TeacherCheckInRequest
import id.diskola.app.utils.JurnalRules
import id.diskola.app.utils.PreferenceClass
import id.diskola.app.utils.session.SessionStore
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

const val JURNAL_PICKER_PAGE = 20

/** Raised when the server answers 307 "this journal already exists"; [attendanceIds] names it (first = the journal). */
class JournalExistsException(message: String, val attendanceIds: List<Int>) : Exception(message)

/**
 * Jurnal KBM (doc `07` §7–9). No Room cache: the list is "today" only and is re-read on demand (the legacy
 * cache was keyed by `plot_start_at` and showed nothing offline), and the pickers page straight from the server.
 */
class JurnalRepository @Inject constructor(
    private val api: JurnalApiService,
    private val sessionStore: SessionStore,
    private val preference: PreferenceClass,
) {
    val isStudent: Boolean get() = sessionStore.isStudent
    val isTeacher: Boolean get() = sessionStore.isTeacher

    // ---- Schedule ----

    suspend fun schedule(date: LocalDate = LocalDate.now()): List<JurnalPlot> = api.schedule(date.toString()).data.orEmpty()

    /** Read on every form open (the spec says so); a failure falls back to the last known value. */
    suspend fun teacherScopeAssigned(): Boolean = try {
        val assigned = api.teacherDataScope().data?.isAssigned == true
        preference.putString(KEY_SCOPE, if (assigned) "assigned" else "all")
        assigned
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        preference.getString(KEY_SCOPE) == "assigned"
    }

    suspend fun captureRequired(): Boolean = try {
        val required = api.captureScope().data?.required == true
        preference.putBoolean(KEY_CAPTURE, required)
        required
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        preference.getBoolean(KEY_CAPTURE)
    }

    suspend fun plots(): List<PlotItem> = api.listPlot().data.orEmpty()

    // ---- Pickers (server paged, 20 at a time) ----

    suspend fun classes(skip: Int, name: String): List<JurnalClassItem> = api.listClass(JURNAL_PICKER_PAGE, skip, name).data.orEmpty()
    suspend fun subjects(skip: Int, name: String): List<JurnalSubjectItem> = api.listSubject(JURNAL_PICKER_PAGE, skip, name).data.orEmpty()
    suspend fun teachers(skip: Int, name: String): List<JurnalTeacherItem> = api.listTeacher(JURNAL_PICKER_PAGE, skip, name).data.orEmpty()

    /** `school_class_id` of the signed-in student — the session's own copy, not the login-time pref that goes stale. */
    val studentClassId: Int get() = sessionStore.student?.classRoomId ?: 0

    // ---- Create ----

    suspend fun createJournal(plotIds: List<Int>, subjectId: Int, classId: Int, objective: String, photo: File?): List<JournalCreatedAttendance> =
        translate {
            api.createJournal(
                plotIds = plotIds.distinct().map { text(it.toString()) },
                subjectId = text(subjectId.toString()),
                classId = text(classId.toString()),
                objective = text(objective.trim()),
                capturePhoto = photo?.let(::photoPart),
            ).data.orEmpty()
        }

    suspend fun createStudentJournal(
        plotIds: List<Int>,
        subjectId: Int,
        teacherUuid: String,
        status: String,
        lat: Double?,
        lng: Double?,
        photo: File?,
    ): List<JournalCreatedAttendance> = translate {
        api.createStudentJournal(
            plotIds = plotIds.distinct().map { text(it.toString()) },
            subjectId = text(subjectId.toString()),
            teacherId = text(teacherUuid),
            classId = text(studentClassId.toString()),
            status = text(status),
            lat = lat?.let { text(it.toString()) },
            lng = lng?.let { text(it.toString()) },
            capturePhoto = photo?.let(::photoPart),
        ).data.orEmpty()
    }

    // ---- Class actions ----

    suspend fun teacherCheckIn(attendanceId: Int, lat: Double?, lng: Double?) = translate {
        api.teacherCheckIn(TeacherCheckInRequest(attendanceId, lat, lng))
    }

    suspend fun studentVerify(attendanceId: Int, status: String) = translate {
        api.studentJournalUpdate(JournalUpdateRequest(attendanceId, status))
    }

    suspend fun submitQr(payload: JurnalRules.QrPayload) = translate {
        api.learningQr(LearningQrRequest(payload.timePlotId, payload.subjectId, payload.classId, payload.teacherId, payload.scheduleId))
    }

    // ---- Detail ----

    suspend fun scheduleDetail(attendanceId: Int): ScheduleDetailData? = api.scheduleDetail(attendanceId).data

    /** Non-teacher fallback when `student_attendances` is empty. */
    suspend fun listStudent(attendanceId: Int): List<ListStudentRow> = api.listStudent(attendanceId).data.orEmpty()

    suspend fun preview(scheduleId: Int, date: String?): JournalPreviewData? = api.journalPreview(scheduleId, date).data

    suspend fun save(scheduleId: Int, objective: String, sessionStatus: String, students: List<Pair<Int, String>>) = translate {
        api.journalSave(
            JournalSaveRequest(
                school_subject_schedule_id = scheduleId,
                learning_objective = objective.trim().ifBlank { null },
                session_status = sessionStatus,
                students = students.map { (id, status) -> JournalSaveStudent(id, status) },
            ),
        )
    }

    // ---- Errors ----

    /** Turns an [ApiException] into the user-facing text of doc `07` §14.3, or into [JournalExistsException] for a 307. */
    private suspend fun <T> translate(block: suspend () -> T): T = try {
        block()
    } catch (e: ApiException) {
        val assigned = preference.getString(KEY_SCOPE) == "assigned"
        if (e.responseCode == 307) {
            throw JournalExistsException(
                JurnalRules.journalErrorMessage(307, e.message, assigned, "Jurnal telah dibuat untuk jadwal ini"),
                JurnalRules.existingJournalIds(e.data),
            )
        }
        val message = JurnalRules.journalErrorMessage(
            code = e.responseCode,
            message = e.validationMessages.joinToString("\n").ifBlank { e.message.orEmpty() },
            scopeAssigned = assigned,
            fallback = "Terjadi kesalahan, silakan coba lagi",
        )
        throw IllegalStateException(message)
    }

    private fun text(value: String): RequestBody = value.toRequestBody("text/plain".toMediaTypeOrNull())

    private fun photoPart(file: File): MultipartBody.Part {
        val mime = if (file.extension.equals("png", ignoreCase = true)) "image/png" else "image/jpeg"
        return MultipartBody.Part.createFormData("capture_photo", file.name, file.asRequestBody(mime.toMediaTypeOrNull()))
    }

    private companion object {
        const val KEY_SCOPE = "teacher_master_data_scope"
        const val KEY_CAPTURE = "journal_capture_required"
    }
}
