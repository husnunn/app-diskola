package id.diskola.app.utils

import id.diskola.app.apiservice.ApiException
import org.junit.Assert.assertEquals
import org.junit.Test

class AppErrorHandlerTest {

    private fun forbidden(message: String?) = AppErrorHandler.getMessage(ApiException(message = message, responseCode = 403))

    @Test
    fun forbidden_translatesThePermissionLibrarysEnglishSentences() {
        val indonesian = "Akun Anda tidak memiliki akses ke fitur ini."
        // Real body of GET mobile/konseling/score for an account without the counseling role (dev, 2026-11-05).
        assertEquals(indonesian, forbidden("User does not have the right roles."))
        assertEquals(indonesian, forbidden("User does not have the right permissions."))
        assertEquals(indonesian, forbidden("User does not have any of the necessary access rights."))
        assertEquals(indonesian, forbidden("USER DOES NOT HAVE THE RIGHT ROLES."))
    }

    @Test
    fun forbidden_keepsAnyOtherServerMessageAndFallsBackWhenThereIsNone() {
        assertEquals("Akses hanya untuk guru BK.", forbidden("Akses hanya untuk guru BK."))
        assertEquals("Akun Anda tidak memiliki akses ke fitur ini.", forbidden(null))
        assertEquals("Akun Anda tidak memiliki akses ke fitur ini.", forbidden(""))
        assertEquals("Akun Anda tidak memiliki akses ke fitur ini.", forbidden("null"))
    }

    @Test
    fun otherStatusesAreUnchanged() {
        assertEquals("Data tidak ditemukan.", AppErrorHandler.getMessage(ApiException(message = null, responseCode = 404)))
        assertEquals("Sesi Anda sudah habis. Silakan login kembali.", AppErrorHandler.getMessage(ApiException(message = "Unauthenticated.", responseCode = 401)))
    }
}
