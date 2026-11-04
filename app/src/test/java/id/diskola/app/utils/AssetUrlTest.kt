package id.diskola.app.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetUrlTest {

    @Test
    fun isImageFile_recognisesPicturesByExtensionWhateverTheCase() {
        assertTrue(isImageFile("https://dev.assets.diskola.id/attendance-leave/a/b/Y9qK977.png"))
        assertTrue(isImageFile("bukti.JPG"))
        assertTrue(isImageFile("/data/user/0/app/cache/leave_proof_1.jpeg"))
        assertTrue(isImageFile("x.webp"))
    }

    @Test
    fun isImageFile_ignoresQueryAndFragment() {
        assertTrue(isImageFile("https://cdn.example/x/photo.png?X-Amz-Signature=abc.pdf"))
        assertTrue(isImageFile("https://cdn.example/x/photo.jpg#top"))
        assertFalse(isImageFile("https://cdn.example/x/surat.pdf?preview=photo.png"))
    }

    @Test
    fun isImageFile_isFalseForDocumentsAndNames_withoutAnExtension() {
        assertFalse(isImageFile("https://cdn.example/x/surat-dokter.pdf"))
        assertFalse(isImageFile("https://cdn.example/x/surat"))
        assertFalse(isImageFile("https://cdn.example/folder.png/"))
        assertFalse(isImageFile(""))
    }
}
