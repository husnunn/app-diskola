package id.diskola.app.utils

import id.diskola.app.apiservice.ApiException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.CancellationException

class AssignmentErrorsTest {

    private fun http(code: Int, body: String): HttpException =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    @Test
    fun classify_422WithFieldErrorsIsValidation() {
        val error = AssignmentErrors.classify(
            ApiException(
                "The given data was invalid",
                422,
                arrayOf("link"),
                fieldErrors = mapOf("link" to listOf("Link tidak valid")),
            ),
            isUpdate = false,
        )
        assertTrue(error is AssignmentError.Validation)
        assertEquals(
            mapOf("link" to listOf("Link tidak valid")),
            (error as AssignmentError.Validation).fieldErrors,
        )
    }

    @Test
    fun classify_422WithoutFieldErrorsFallsBackToMessage() {
        val error = AssignmentErrors.classify(ApiException("Data tidak valid", 422), isUpdate = true)
        assertEquals(AssignmentError.Message("Data tidak valid"), error)
    }

    @Test
    fun classify_404IsNotFoundOnlyForUpdate() {
        assertEquals(
            AssignmentError.NotFound,
            AssignmentErrors.classify(ApiException("Tidak ditemukan", 404), isUpdate = true),
        )
        assertEquals(
            AssignmentError.Message("Tidak ditemukan"),
            AssignmentErrors.classify(ApiException("Tidak ditemukan", 404), isUpdate = false),
        )
    }

    @Test
    fun classify_400ShowsBackendMessage() {
        assertEquals(
            AssignmentError.Message("Tugas sudah dikumpulkan"),
            AssignmentErrors.classify(ApiException("Tugas sudah dikumpulkan", 400), isUpdate = false),
        )
    }

    @Test
    fun classify_413ExplainsFileLimit() {
        val error = AssignmentErrors.classify(
            ApiException("Mohon maaf, terjadi kesalahan", 413),
            isUpdate = false,
        )
        assertEquals(AssignmentError.Message("Ukuran file terlalu besar. Maksimal 12 MB"), error)
    }

    @Test
    fun classify_401HttpExceptionIsUnauthorized() {
        assertEquals(
            AssignmentError.Unauthorized,
            AssignmentErrors.classify(http(401, """{"message":"Unauthenticated."}"""), isUpdate = false),
        )
    }

    @Test
    fun classify_403HttpExceptionIsBlockedWithTraceId() {
        val error = AssignmentErrors.classify(
            http(403, """{"status":"error","message":"Akun diblokir","trace_id":"abc-123"}"""),
            isUpdate = false,
        )
        assertEquals(AssignmentError.Blocked("Akun diblokir", "abc-123"), error)
    }

    @Test
    fun classify_403WithEmptyBodyStillBlocked() {
        val error = AssignmentErrors.classify(http(403, ""), isUpdate = false)
        assertTrue(error is AssignmentError.Blocked)
        assertNull((error as AssignmentError.Blocked).traceId)
    }

    @Test
    fun classify_500UsesGenericMessageNotHttpStatusText() {
        val error = AssignmentErrors.classify(http(500, "<html>boom</html>"), isUpdate = false)
        assertEquals(AssignmentError.Message(AssignmentErrors.DEFAULT_MESSAGE), error)
    }

    @Test
    fun classify_plainIoExceptionKeepsItsMessage() {
        val error = AssignmentErrors.classify(IOException("Unable to resolve host"), isUpdate = false)
        assertEquals(AssignmentError.Message("Unable to resolve host"), error)
    }

    @Test
    fun classify_apiExceptionIsCheckedBeforeIoException() {
        val error = AssignmentErrors.classify(ApiException("pesan backend", 400), isUpdate = false)
        assertEquals(AssignmentError.Message("pesan backend"), error)
    }

    @Test
    fun classify_rethrowsCancellation() {
        try {
            AssignmentErrors.classify(CancellationException("dibatalkan"), isUpdate = false)
            fail("CancellationException harus dilempar ulang")
        } catch (e: CancellationException) {
            assertEquals("dibatalkan", e.message)
        }
    }

    @Test
    fun describeFieldErrors_labelsStudentFilesByIndex() {
        val lines = AssignmentErrors.describeFieldErrors(
            linkedMapOf(
                "file.0" to listOf("Format tidak didukung"),
                "file.2" to listOf("Terlalu besar"),
                "link" to listOf("Link tidak valid"),
                "uploaded" to listOf("Wajib diisi"),
            )
        )
        assertEquals(
            listOf(
                "File ke-1: Format tidak didukung",
                "File ke-3: Terlalu besar",
                "Link: Link tidak valid",
                "Wajib diisi",
            ),
            lines,
        )
    }

    @Test
    fun baseKey_dropsIndexSuffix() {
        assertEquals("link", AssignmentErrors.baseKey("link.0"))
        assertEquals("title", AssignmentErrors.baseKey("title"))
    }

    // ---- where a 422 shows (placeFieldErrors) ----

    @Test
    fun placement_classDaySubjectAndDeadlineShowUnderTheFieldOnly() {
        val placement = AssignmentErrors.placeFieldErrors(
            linkedMapOf(
                "school_class_id" to listOf("Kelas tidak valid"),
                "day" to listOf("Hari tidak valid"),
                "school_subject_schedule_id" to listOf("Jadwal tidak valid"),
                "end_at" to listOf("Waktu berakhir tidak valid"),
            ),
        )
        assertEquals(
            mapOf(
                "school_class_id" to "Kelas tidak valid",
                "day" to "Hari tidak valid",
                "school_subject_id" to "Jadwal tidak valid",
                "end_at" to "Waktu berakhir tidak valid",
            ),
            placement.inline,
        )
        assertTrue("no dialog for field-only errors", placement.dialogLines.isEmpty())
    }

    @Test
    fun placement_textFieldsShowUnderTheFieldAndInTheDialog() {
        val placement = AssignmentErrors.placeFieldErrors(linkedMapOf("title" to listOf("Judul wajib diisi"), "link" to listOf("Link tidak valid")))
        assertEquals(mapOf("title" to "Judul wajib diisi", "link" to "Link tidak valid"), placement.inline)
        assertEquals(listOf("Judul wajib diisi", "Link: Link tidak valid"), placement.dialogLines)
    }

    @Test
    fun placement_filesAndUnknownKeysGoToTheDialogOnly() {
        val placement = AssignmentErrors.placeFieldErrors(
            linkedMapOf("file.0" to listOf("Format tidak didukung"), "uploaded" to listOf("Wajib diisi")),
        )
        assertTrue(placement.inline.isEmpty())
        assertEquals(listOf("File ke-1: Format tidak didukung", "Wajib diisi"), placement.dialogLines)
    }

    @Test
    fun placement_theFirstMessageOfAFieldIsTheOneUnderIt() {
        val placement = AssignmentErrors.placeFieldErrors(linkedMapOf("description" to listOf("Deskripsi wajib diisi", "Terlalu pendek")))
        assertEquals("Deskripsi wajib diisi", placement.inline["description"])
        assertEquals(listOf("Deskripsi wajib diisi", "Terlalu pendek"), placement.dialogLines)
    }

    @Test
    fun placement_nothingToPlaceGivesNothing() {
        val placement = AssignmentErrors.placeFieldErrors(emptyMap())
        assertTrue(placement.inline.isEmpty() && placement.dialogLines.isEmpty())
        assertTrue(AssignmentErrors.placeFieldErrors(mapOf("title" to emptyList())).inline.isEmpty())
    }
}
