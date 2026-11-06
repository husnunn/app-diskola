package id.diskola.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentNoticesTest {

    @Test
    fun everyActionHasItsOwnTitle() {
        val titles = listOf(
            ContentNotices.tugasSaved(isEdit = false),
            ContentNotices.tugasSaved(isEdit = true),
            ContentNotices.tugasDeleted(),
            ContentNotices.materiSaved(isEdit = false),
            ContentNotices.materiSaved(isEdit = true),
            ContentNotices.materiDeleted(),
        ).map { it.title }
        assertEquals("Six actions, six different titles", 6, titles.toSet().size)
    }

    @Test
    fun createAndEditAreToldApart() {
        assertNotEquals(ContentNotices.tugasSaved(false).body, ContentNotices.tugasSaved(true).body)
        assertNotEquals(ContentNotices.materiSaved(false).body, ContentNotices.materiSaved(true).body)
    }

    @Test
    fun titlesNameTheThingAndSayItSucceeded() {
        assertTrue(ContentNotices.tugasSaved(false).title.startsWith("Tugas Berhasil"))
        assertTrue(ContentNotices.tugasDeleted().title.startsWith("Tugas Berhasil"))
        assertTrue(ContentNotices.materiSaved(true).title.startsWith("Materi Berhasil"))
        assertTrue(ContentNotices.materiDeleted().title.startsWith("Materi Berhasil"))
    }

    @Test
    fun noTitleOrBodyIsBlank() {
        listOf(
            ContentNotices.tugasSaved(false), ContentNotices.tugasSaved(true), ContentNotices.tugasDeleted(),
            ContentNotices.materiSaved(false), ContentNotices.materiSaved(true), ContentNotices.materiDeleted(),
        ).forEach {
            assertTrue(it.title.isNotBlank())
            assertTrue(it.body.isNotBlank())
        }
    }
}
