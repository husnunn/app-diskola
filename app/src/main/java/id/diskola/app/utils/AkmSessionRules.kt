package id.diskola.app.utils

/**
 * Rules and texts of the exam *session* (doc `06` §5.3–§5.6, §10.2, §12): which gates stand between "Mulai" and the exam, when the
 * "exam session is open" marker is cleared, what leaving the exam says. Pure — every rule and every word has a JVM test, because the
 * words are the ones legacy shows and the doc requires them verbatim (§23.2).
 */
object AkmSessionRules {

    // ---- Gates between "Mulai Asesmen" and the exam ----

    /** Legacy order (`AkmDetailPage`): password → terms → online check (strict only) → priming → the session itself. */
    enum class Gate { Password, Terms, OnlineCheck, Priming, Session }

    /**
     * The password dialog is skipped only when the exam has none, or the student already passed it **and** the last session was
     * closed properly: a marker that is still set means the exam was left without handing in (kill, crash, "Ya, keluar"), and the
     * teacher's one-time password then has to be entered again (`startExamWithPasswordGate`, doc 06 §5.3/§12.2).
     */
    fun skipPassword(needsPassword: Boolean, passwordChecked: Boolean, markerActive: Boolean): Boolean =
        !needsPassword || (passwordChecked && !markerActive)

    /** A strict exam must be started online with real internet (a captive portal counts as offline); a lenient one may start offline. */
    fun mayStart(strict: Boolean, onlineValidated: Boolean): Boolean = !strict || onlineValidated

    // ---- The "session open" marker ----

    /** Why a session ended. Only the three that [completes] clear the marker; the rest leave it set on purpose (doc 06 §10.2). */
    enum class EndReason(val completes: Boolean) {
        /** The student pressed "Kumpulkan" and the answers were queued or sent. */
        Submit(true),

        /** The end time passed and the answers were queued. */
        TimeUp(true),

        /** The exam turned out to be handed in already. */
        AlreadyUploaded(true),

        /** "Ya, keluar" (or "Keluar dari ujian"): left without handing in. */
        Abort(false),

        /** The screens were closed some other way without an explicit end. */
        Left(false),
    }

    /** New session of the same exam as the one already running in this process is not "an unclosed previous session". */
    fun isUnclosed(sessionAlreadyRunning: Boolean, markerActive: Boolean): Boolean = !sessionAlreadyRunning && markerActive

    // ---- Texts: terms ("Pemberitahuan !!!") ----

    const val TERMS_TITLE = "Pemberitahuan !!!"
    const val TERMS_OK = "OK"

    private const val TERMS_STRICT_HEAD =
        "• Layar akan dikunci selama ujian. Setelah menekan OK, akan muncul dialog dari sistem — tekan Mengerti/OK agar ujian dapat dimulai.\n" +
            "• Ujian harus dimulai dalam keadaan terhubung internet.\n"
    private const val TERMS_LENIENT_HEAD = "• Pengerjaan ujian tanpa koneksi internet diperbolehkan.\n"
    private const val TERMS_RULES =
        "• Ujian akan dikenakan sanksi apabila Anda keluar dari halaman ujian atau berpindah aplikasi.\n" +
            "• Pastikan koneksi internet stabil saat mengunggah jawaban.\n" +
            "• Selama ujian berlangsung, tidak diperbolehkan mengangkat panggilan telepon.\n\n" +
            "Dengan menekan OK, Anda menyetujui ketentuan di atas. "

    fun termsBody(strict: Boolean): String = (if (strict) TERMS_STRICT_HEAD else TERMS_LENIENT_HEAD) + TERMS_RULES

    // ---- Texts: strict exam started offline ----

    const val OFFLINE_TITLE = "Butuh Koneksi Internet"
    const val OFFLINE_BODY = "Ujian ini harus dimulai dalam keadaan terhubung internet.\n\nAktifkan paket data atau Wi-Fi, lalu coba mulai kembali."
    const val OFFLINE_OK = "Mengerti"

    // ---- Texts: priming screen ("Sebelum Memulai Ujian", `strings.xml` akm_pre_exam_*) ----

    object Priming {
        const val TITLE = "Sebelum Memulai Ujian"
        const val VIOLATIONS_HEADING = "Selama ujian, sistem akan mencatat otomatis bila kamu:"
        val VIOLATIONS = listOf(
            "Membuka aplikasi lain atau keluar dari halaman ujian",
            "Membagi layar dengan aplikasi lain (mode layar terbagi)",
            "Membuka jendela/pop-up aplikasi lain di atas layar ujian",
            "Mengambil tangkapan layar (screenshot)",
            "Merekam layar",
        )
        const val NEUTRAL_HEADING = "Sistem juga mencatat sebagai informasi tambahan:"
        val NEUTRAL = listOf(
            "Koneksi internet terputus cukup lama",
            "Jam/tanggal perangkat berubah",
        )
        const val NEUTRAL_FOOTNOTE = "Dua hal di atas bukan pelanggaran — hanya informasi tambahan untuk sekolah."
        const val SEND_NOTICE = "Semua catatan ini akan dikirim ke pihak sekolah."
        const val LOCKDOWN_MESSAGE =
            "Layar akan dikunci agar kamu tidak berpindah aplikasi. Setelah menekan Mulai Ujian, konfirmasi dialog penyematan layar dari sistem bila muncul."
        const val START = "Mulai Ujian"

        /** The "Dibatalkan" banner on this screen (a deviation from legacy, decided 2026-10-16; informs only). */
        fun watermarkNotice(label: String): String =
            "Jadwal ini ditandai \"$label\" oleh sekolah. Kamu tetap dapat mengerjakan dan mengumpulkan jawaban."
    }

    // ---- Texts and rules: leaving the exam screen (`handleBackNavigation`, doc 06 §12.1) ----

    sealed interface BackAction {
        /** Strict mode: leaving is refused; one button. */
        data class Refuse(val title: String, val message: String, val okLabel: String) : BackAction

        /** Lenient mode: leaving is allowed after a warning; it is recorded as a violation when the next session starts. */
        data class Confirm(val title: String, val message: String, val stayLabel: String, val leaveLabel: String) : BackAction
    }

    fun backAction(strict: Boolean, penaltyOn: Boolean): BackAction =
        if (strict) {
            BackAction.Refuse(
                title = "Ujian Sedang Berlangsung",
                message = "Kamu tidak dapat keluar selama ujian berlangsung.\n\nSelesaikan dengan menekan \"Kumpulkan\", atau tunggu sampai waktu ujian berakhir.",
                okLabel = "Mengerti",
            )
        } else {
            BackAction.Confirm(
                title = "Peringatan",
                message = "Ujian baru dianggap selesai bila kamu menekan \"Kumpulkan\".\n\n" +
                    if (penaltyOn) {
                        "Keluar sekarang tercatat sebagai pelanggaran, dan kamu terkena penalti saat membuka ujian ini lagi."
                    } else {
                        "Keluar sekarang tercatat sebagai pelanggaran dan dilaporkan ke pihak sekolah."
                    },
                stayLabel = "Tetap mengerjakan",
                leaveLabel = "Ya, keluar",
            )
        }

    // ---- Texts: "Mode Terkunci Terlepas" recovery dialog ----

    const val RECOVERY_TITLE = "Mode Terkunci Terlepas"
    const val RECOVERY_MESSAGE = "Mode terkunci ujian tidak aktif. Aktifkan kembali penyematan layar untuk melanjutkan pengerjaan."
    const val RECOVERY_RETRY_MESSAGE = "Penyematan layar belum aktif. Tekan Aktifkan, lalu konfirmasi dialog sistem jika muncul."
    const val RECOVERY_ACTIVATE = "Aktifkan"
    const val RECOVERY_CHECKING = "Memeriksa..."

    /** Not in legacy: offered after [AkmLockdownRules.FAILED_RELOCKS_BEFORE_ESCAPE] failed attempts so nobody is trapped on the dialog. */
    const val RECOVERY_ESCAPE = "Keluar dari ujian"
}
