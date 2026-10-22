package id.diskola.app.apiservice

import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [isBinaryBody] decides whether `ResponseInterceptor` may read a body as UTF-8 text. Getting it wrong corrupts
 * downloads: a PDF served as `application/pdf` used to be re-encoded with U+FFFD and rendered as blank pages.
 */
class ResponseInterceptorTest {

    private fun binary(type: String) = isBinaryBody(type.toMediaType())

    @Test
    fun `pdf and office files are binary`() {
        assertTrue(binary("application/pdf"))
        assertTrue(binary("application/msword"))
        assertTrue(binary("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        assertTrue(binary("application/vnd.ms-excel"))
        assertTrue(binary("application/zip"))
    }

    @Test
    fun `images media and octet-stream are binary`() {
        assertTrue(binary("image/jpeg"))
        assertTrue(binary("video/mp4"))
        assertTrue(binary("audio/mpeg"))
        assertTrue(binary("application/octet-stream"))
        assertTrue(binary("binary/octet-stream"))
    }

    @Test
    fun `json text and xml are read as text`() {
        assertFalse(binary("application/json"))
        assertFalse(binary("application/json; charset=utf-8"))
        assertFalse(binary("application/problem+json"))
        assertFalse(binary("text/html; charset=utf-8"))
        assertFalse(binary("text/plain"))
        assertFalse(binary("application/xml"))
        assertFalse(binary("application/atom+xml"))
        assertFalse(binary("application/x-www-form-urlencoded"))
    }

    @Test
    fun `missing content type keeps the json path`() {
        assertFalse(isBinaryBody(null))
    }

    @Test
    fun `content type match ignores case`() {
        assertTrue(binary("Application/PDF"))
        assertFalse(binary("APPLICATION/JSON"))
    }
}
