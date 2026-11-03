package id.diskola.app.ui.screens.home

import id.diskola.app.viewmodel.HomeProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeMenuGridTest {

    private fun profile(student: Boolean = false, teacher: Boolean = false, hasClass: Boolean = true) = HomeProfile(
        schoolName = "SMK Demo Surabaya",
        schoolLogo = "",
        userName = "Ani Wijaya",
        userAvatar = "",
        classLine = "XI RPL 1",
        isStudent = student,
        isTeacher = teacher,
        isGuest = !student && !teacher,
        isHavingClass = hasClass,
        klaspayActive = false,
    )

    @Test
    fun `student menu ends with E-Raport, which is not built yet`() {
        val tiles = menuTilesFor(profile(student = true))
        assertEquals(
            listOf("Materi", "Tugas", "Presensi", "Jurnal", "Asesmen", "Poin", "Magang", "E-Raport"),
            tiles.map { it.label },
        )
        assertEquals(MenuKey.E_RAPORT, tiles.last().key)
        assertFalse(tiles.last().implemented)
    }

    @Test
    fun `teacher menu ends with E-Raport, which is not built yet`() {
        val tiles = menuTilesFor(profile(teacher = true))
        assertEquals(
            listOf("Materi", "Tugas", "Presensi", "Jurnal", "Agenda Mingguan", "Poin", "E-Raport"),
            tiles.map { it.label },
        )
        assertEquals(MenuKey.E_RAPORT, tiles.last().key)
        assertFalse(tiles.last().implemented)
    }

    @Test
    fun `guest has no E-Raport`() {
        assertTrue(menuTilesFor(profile()).none { it.key == MenuKey.E_RAPORT })
    }

    @Test
    fun `E-Raport locks without a class like every other tile`() {
        assertTrue(menuTilesFor(profile(student = true, hasClass = false)).all { it.locked })
        assertTrue(menuTilesFor(profile(teacher = true, hasClass = false)).all { it.locked })
    }
}
