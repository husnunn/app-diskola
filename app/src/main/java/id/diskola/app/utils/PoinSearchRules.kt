package id.diskola.app.utils

/** The teacher's "Poin Siswa" student search: how long a query must be and what the page says before there are results. */
object PoinSearchRules {

    const val MIN_QUERY = 3

    /** Whether [query] is long enough to send to the server (blanks around it do not count). */
    fun isSearchable(query: String): Boolean = query.trim().length >= MIN_QUERY

    /** The line under the title of the idle page: the whole idea while the field is empty, the missing length once typing has started. */
    fun idleHint(query: String): String =
        if (query.isBlank()) {
            "Ketik nama atau NISN/NIS/NIK siswa, lalu pilih siswa dari hasil pencarian untuk melihat poinnya dan menambah pelanggaran, prestasi, atau pemanggilan siswa."
        } else {
            "Ketik minimal $MIN_QUERY karakter untuk mulai mencari siswa."
        }
}
