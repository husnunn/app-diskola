package id.diskola.app.utils

import id.diskola.app.apiservice.ApiErrorParser
import id.diskola.app.apiservice.ApiException
import retrofit2.HttpException
import java.util.concurrent.CancellationException

sealed class AssignmentError {
    data class Validation(
        val fieldErrors: Map<String, List<String>>,
        val message: String? = null,
    ) : AssignmentError()

    object NotFound : AssignmentError()
    data class Message(val text: String) : AssignmentError()
    object Unauthorized : AssignmentError()
    data class Blocked(val message: String, val traceId: String?) : AssignmentError()
}

object AssignmentErrors {

    const val DEFAULT_MESSAGE = "Mohon maaf, terjadi kesalahan"
    private const val BLOCKED_MESSAGE = "Akses Anda diblokir. Silakan hubungi admin sekolah."
    private const val FILE_TOO_LARGE_MESSAGE = "Ukuran file terlalu besar. Maksimal 12 MB"

    fun classify(e: Throwable, isUpdate: Boolean): AssignmentError {
        if (e is CancellationException) throw e
        return when (e) {
            is ApiException -> classifyApi(e, isUpdate)
            is HttpException -> classifyHttp(e)
            else -> AssignmentError.Message(e.localizedMessage.orEmpty().ifBlank { DEFAULT_MESSAGE })
        }
    }

    private fun classifyApi(e: ApiException, isUpdate: Boolean): AssignmentError {
        val message = e.message.orEmpty().ifBlank { DEFAULT_MESSAGE }
        return when (e.responseCode) {
            401 -> AssignmentError.Unauthorized
            403 -> AssignmentError.Blocked(message, e.traceId)
            404 -> if (isUpdate) AssignmentError.NotFound else AssignmentError.Message(message)
            413 -> AssignmentError.Message(FILE_TOO_LARGE_MESSAGE)
            422 ->
                if (e.fieldErrors.isNotEmpty()) AssignmentError.Validation(e.fieldErrors, e.message)
                else AssignmentError.Message(message)
            else -> AssignmentError.Message(message)
        }
    }

    private fun classifyHttp(e: HttpException): AssignmentError {
        val parsed = ApiErrorParser.parse(
            try {
                e.response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }
        )
        return when (e.code()) {
            401 -> AssignmentError.Unauthorized
            403 -> AssignmentError.Blocked(parsed.message ?: BLOCKED_MESSAGE, parsed.traceId)
            else -> AssignmentError.Message(parsed.message ?: DEFAULT_MESSAGE)
        }
    }

    /** `file.N` -> "File ke-(N+1)"; sufiks indeks lain dibuang agar bisa dipetakan ke satu field. */
    fun describeFieldErrors(fieldErrors: Map<String, List<String>>): List<String> =
        fieldErrors.flatMap { (key, messages) ->
            val index = key.substringAfter('.', "").toIntOrNull()
            val label = when (key.substringBefore('.')) {
                "file" -> if (index != null) "File ke-${index + 1}" else "File"
                "link" -> "Link"
                else -> null
            }
            messages.map { if (label != null) "$label: $it" else it }
        }

    fun baseKey(key: String): String = key.substringBefore('.')

    /** Where a 422 puts each message: under its field, in the "Periksa kembali isian" dialog, or both. */
    data class FieldPlacement(
        /** Form key → first message. Keys: `school_class_id`, `day`, `school_subject_id`, `end_at`, `title`, `description`, `link`. */
        val inline: Map<String, String>,
        /** Lines of the dialog; empty ⇒ no dialog. */
        val dialogLines: List<String>,
    )

    private val inlineKeys = setOf(
        "school_class_id", "day", "school_subject_id", "school_subject_schedule_id", "end_at", "title", "description", "link",
    )

    /** The fields whose message is also repeated in the dialog (legacy: the text fields can be long, the dialog lists them in full). */
    private val alsoInDialog = setOf("title", "description", "link")

    /**
     * Class, day, subject and deadline errors show under the field only; title, description and link show under the field **and** in the
     * dialog; everything else (`file.N`, `uploaded`, …) only in the dialog. A 422 about class/day/subject/deadline alone therefore shows no
     * dialog (legacy `showFieldErrors`).
     */
    fun placeFieldErrors(fieldErrors: Map<String, List<String>>): FieldPlacement {
        val inline = LinkedHashMap<String, String>()
        val dialog = LinkedHashMap<String, List<String>>()
        fieldErrors.forEach { (key, messages) ->
            if (messages.isEmpty()) return@forEach
            val base = baseKey(key)
            if (base in inlineKeys) {
                val formKey = if (base == "school_subject_schedule_id") "school_subject_id" else base
                inline.putIfAbsent(formKey, messages.first())
                if (base in alsoInDialog) dialog[key] = messages
            } else {
                dialog[key] = messages
            }
        }
        return FieldPlacement(inline, describeFieldErrors(dialog))
    }
}
