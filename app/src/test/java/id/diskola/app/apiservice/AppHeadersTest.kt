package id.diskola.app.apiservice

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppHeadersTest {

    private fun request(configure: Request.Builder.() -> Unit = {}): Request =
        Request.Builder().url("https://api.example.test/mobile/x").apply(configure).build()

    @Test
    fun `every request says it comes from android and which version`() {
        val built = request { withAppHeaders("2.1.41") }
        assertEquals("android", built.header("X-App-Platform"))
        assertEquals("2.1.41", built.header("X-App-Version"))
    }

    @Test
    fun `the version is the version name, whatever it looks like`() {
        assertEquals("1.0.0", request { withAppHeaders("1.0.0") }.header("X-App-Version"))
        assertEquals("2.1.41-rc1", request { withAppHeaders("2.1.41-rc1") }.header("X-App-Version"))
    }

    @Test
    fun `applying the headers twice never doubles them`() {
        val built = request { withAppHeaders("1.0.0").withAppHeaders("1.0.0") }
        assertEquals(listOf("android"), built.headers("X-App-Platform"))
        assertEquals(listOf("1.0.0"), built.headers("X-App-Version"))
    }

    @Test
    fun `an endpoint that sets its own header keeps it, and the app headers are only the two`() {
        val built = request {
            header("X-Device-Fingerprint", "abc")
            withAppHeaders("1.0.0")
        }
        assertEquals("abc", built.header("X-Device-Fingerprint"))
        assertEquals(setOf("X-Device-Fingerprint", "X-App-Platform", "X-App-Version").map { it.lowercase() }.toSet(), built.headers.names().map { it.lowercase() }.toSet())
    }

    @Test
    fun `without the call no app header exists`() {
        assertNull(request().header("X-App-Platform"))
        assertNull(request().header("X-App-Version"))
    }

    @Test
    fun `the default version is the one this build was made with`() {
        assertEquals(id.diskola.app.BuildConfig.VERSION_NAME, request { withAppHeaders() }.header("X-App-Version"))
    }
}
