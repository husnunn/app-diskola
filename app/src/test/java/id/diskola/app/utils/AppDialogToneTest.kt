package id.diskola.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDialogToneTest {

    @Test
    fun `success titles`() {
        assertEquals(AppDialogTone.SUCCESS, AppDialogTone.fromTitle("Berhasil Dibatalkan"))
        assertEquals(AppDialogTone.SUCCESS, AppDialogTone.fromTitle("  sukses "))
    }

    @Test
    fun `error titles`() {
        assertEquals(AppDialogTone.ERROR, AppDialogTone.fromTitle("Gagal menyimpan tugas"))
        assertEquals(AppDialogTone.ERROR, AppDialogTone.fromTitle("Error"))
        assertEquals(AppDialogTone.ERROR, AppDialogTone.fromTitle("Terjadi Kesalahan"))
    }

    @Test
    fun `warning titles`() {
        assertEquals(AppDialogTone.WARNING, AppDialogTone.fromTitle("Perhatian"))
        assertEquals(AppDialogTone.WARNING, AppDialogTone.fromTitle("Peringatan"))
        assertEquals(AppDialogTone.WARNING, AppDialogTone.fromTitle("Pelanggaran Terdeteksi"))
    }

    @Test
    fun `anything else is info, including blank and null`() {
        assertEquals(AppDialogTone.INFO, AppDialogTone.fromTitle("Hapus Tugas"))
        assertEquals(AppDialogTone.INFO, AppDialogTone.fromTitle(""))
        assertEquals(AppDialogTone.INFO, AppDialogTone.fromTitle(null))
    }

    @Test
    fun `only the start of the title counts for success and warning`() {
        // "Tugas berhasil dibuat" does not start with "berhasil": legacy treats it as info, and so does this.
        assertEquals(AppDialogTone.INFO, AppDialogTone.fromTitle("Tugas berhasil dibuat"))
        assertEquals(AppDialogTone.INFO, AppDialogTone.fromTitle("Baca peringatan ini"))
    }
}
