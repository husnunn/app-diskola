package id.diskola.app.repository

import id.diskola.app.apiservice.ApiException
import id.diskola.app.apiservice.MagangApiService
import id.diskola.app.dataclass.ResponData.MagangAttendRequest
import id.diskola.app.dataclass.ResponData.MagangAttendanceItem
import id.diskola.app.dataclass.ResponData.MagangScheduleItem
import id.diskola.app.utils.JurnalRules
import id.diskola.app.utils.MagangRules
import id.diskola.app.utils.session.SessionStore
import java.io.File
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Magang (internship): the student's schedule, check-in, daily report, report history and izin. No local cache — legacy wiped its schedule
 * table before every fetch, so offline it showed nothing anyway, and a cache would need a Room migration that today wipes unsent AKM answers.
 * The lists are small and unpaged; the ViewModel keeps what the last fetch returned.
 */
class MagangRepository @Inject constructor(
    private val api: MagangApiService,
    private val sessionStore: SessionStore,
) {

    /** Whose name and photo the check-in screen shows. */
    val user get() = sessionStore.user

    suspend fun schedules(): List<MagangScheduleItem> = api.schedule().data.orEmpty()

    suspend fun attendance(): List<MagangAttendanceItem> = api.attendance().data.orEmpty()

    /** `lat`/`lng` are the student's position at the moment of the tap. */
    suspend fun attend(scheduleId: Long, lat: Double, lng: Double) = translate {
        api.attend(MagangAttendRequest(scheduleId, lat, lng))
    }

    /** Daily report plus check-out. [notes] go as typed (legacy sent them untrimmed, but checked them trimmed). */
    suspend fun sendDailyReport(scheduleId: Long, notes: String, photo: File) = translate {
        api.dailyReport(text(scheduleId.toString()), text(notes.trim()), imagePart(photo))
    }

    /** [type] is `sakit` or `izin`. */
    suspend fun submitLeave(scheduleId: Long, type: String, note: String, proof: File) = translate {
        api.leaveRequest(text(scheduleId.toString()), text(type), text(note.trim()), imagePart(proof))
    }

    /** Server reasons become the message; a lapsed session or a server fault keeps the app-wide wording. */
    private suspend fun <T> translate(block: suspend () -> T): T = try {
        block()
    } catch (e: ApiException) {
        if (JurnalRules.usesGenericError(e.responseCode)) throw e
        throw IllegalStateException(MagangRules.errorMessage(e.message, e.validationMessages))
    }

    private fun text(value: String): RequestBody = value.toRequestBody("text/plain".toMediaTypeOrNull())

    private fun imagePart(file: File): MultipartBody.Part {
        val mime = when (file.extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        return MultipartBody.Part.createFormData("file", file.name, file.asRequestBody(mime.toMediaTypeOrNull()))
    }
}
