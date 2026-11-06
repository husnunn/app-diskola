package id.diskola.app.utils

/** What a success dialog says: the [title] names what happened, the [body] says what it means for the list and the students. */
data class SuccessNotice(val title: String, val body: String)

/**
 * The success dialogs a teacher sees after creating, editing or deleting a task or a material. Legacy said nothing: the form just closed
 * and the list changed, so a teacher could not tell a saved task from a failed one that happened to look the same. Titles start with the
 * noun and end with the action ("Tugas Berhasil Diposting"), like "Jurnal Berhasil Disimpan"; the button names in the forms ("Posting
 * Tugas", "Posting materi", "Simpan Perubahan") are the verbs the titles repeat.
 */
object ContentNotices {

    fun tugasSaved(isEdit: Boolean): SuccessNotice =
        if (isEdit) {
            SuccessNotice("Tugas Berhasil Diperbarui", "Perubahan tugas telah tersimpan dan terlihat oleh siswa.")
        } else {
            SuccessNotice("Tugas Berhasil Diposting", "Tugas telah diposting dan dapat dilihat oleh siswa kelas yang dipilih.")
        }

    fun tugasDeleted(): SuccessNotice =
        SuccessNotice("Tugas Berhasil Dihapus", "Tugas telah dihapus dari daftar tugas Anda dan dari daftar siswa.")

    fun materiSaved(isEdit: Boolean): SuccessNotice =
        if (isEdit) {
            SuccessNotice("Materi Berhasil Diperbarui", "Perubahan materi telah tersimpan dan terlihat oleh siswa.")
        } else {
            SuccessNotice("Materi Berhasil Diposting", "Materi telah diposting dan dapat dilihat oleh siswa yang dituju.")
        }

    fun materiDeleted(): SuccessNotice =
        SuccessNotice("Materi Berhasil Dihapus", "Materi telah dihapus dari daftar materi Anda dan dari daftar siswa.")
}
