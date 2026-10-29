package id.diskola.app.utils

/**
 * The tone of `AppDialog` (legacy `widgets/AppDialog.kt`): it picks the badge icon, the colour of the badge and tag, and whether the primary
 * button is the error (red) one.
 */
enum class AppDialogTone {
    INFO, SUCCESS, WARNING, ERROR;

    companion object {
        /**
         * Guesses the tone from the title, exactly like legacy `AppDialogTone.fromTitle`, so the dialogs that pass no tone still get a
         * fitting badge: "Berhasil…/Sukses…" success, "Gagal…/Error…/…kesalahan…" error, "Perhatian…/Peringatan…/Pelanggaran…" warning.
         */
        fun fromTitle(title: CharSequence?): AppDialogTone {
            val t = title?.toString()?.trim()?.lowercase().orEmpty()
            return when {
                t.startsWith("berhasil") || t.startsWith("sukses") -> SUCCESS
                t.startsWith("gagal") || t.startsWith("error") || t.contains("kesalahan") -> ERROR
                t.startsWith("perhatian") || t.startsWith("peringatan") || t.startsWith("pelanggaran") -> WARNING
                else -> INFO
            }
        }
    }
}
