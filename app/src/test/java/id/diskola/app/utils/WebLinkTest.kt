package id.diskola.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebLinkTest {

    @Test
    fun `http and https links are opened as they are`() {
        assertEquals("https://youtu.be/D2S-RnqRyEg?si=6WHqW", WebLink.normalize("https://youtu.be/D2S-RnqRyEg?si=6WHqW"))
        assertEquals("http://portal.diskola.id", WebLink.normalize("http://portal.diskola.id"))
        assertEquals("HTTPS://Example.com", WebLink.normalize("HTTPS://Example.com"))
    }

    @Test
    fun `a link typed without a scheme gets https`() {
        assertEquals("https://youtu.be/abc", WebLink.normalize("youtu.be/abc"))
        assertEquals("https://www.youtube.com/watch?v=1", WebLink.normalize("www.youtube.com/watch?v=1"))
        assertEquals("https://localhost:8080/x", WebLink.normalize("localhost:8080/x"))
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        assertEquals("https://youtu.be/abc", WebLink.normalize("  https://youtu.be/abc \n"))
    }

    @Test
    fun `blank input has nothing to open`() {
        assertNull(WebLink.normalize(""))
        assertNull(WebLink.normalize("   "))
    }

    @Test
    fun `links that are not web pages are refused`() {
        assertNull(WebLink.normalize("file:///sdcard/secret.pdf"))
        assertNull(WebLink.normalize("content://media/external/file/1"))
        assertNull(WebLink.normalize("intent://scan/#Intent;scheme=zxing;end"))
        assertNull(WebLink.normalize("javascript:alert(1)"))
        assertNull(WebLink.normalize("tel:+62812345"))
        assertNull(WebLink.normalize("mailto:guru@sekolah.id"))
        assertNull(WebLink.normalize("ftp://files.example.com/a.zip"))
    }
}
