package id.diskola.app.ui.navigation

import id.diskola.app.dataclass.ResponData.AgendaCheckMode
import id.diskola.app.dataclass.ResponData.PoinFormMode
import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose routes for the whole app (single Activity, one top-level
 * [androidx.navigation.compose.NavHost] plus a nested one for the bottom-nav tabs under [Main]).
 */
sealed interface Route {

    @Serializable
    data object Splash : Route

    @Serializable
    data object Onboarding : Route

    /** Nested-graph marker route for the whole [Auth] flow — lets `LoginScreen`/`PasswordScreen`/
     * `ResetPasswordScreen` share one `LoginViewModel` instance scoped to this graph's own
     * back-stack entry, the same role the legacy Activity-scoped `LoginViewModel` played across
     * `LoginForm`/`LoginProcess`/`ResetPassPage` (doc `02-auth-login-sesi.md` §4, §13). */
    @Serializable
    data object AuthGraph : Route

    /** Auth sub-graph — wrapped once by `AuthScaffold`'s ambient-gradient background. All state
     * (selected school, check-account result, SSO response) lives in the `LoginViewModel`/
     * `SsoViewModel` scoped to this graph's own back-stack entry, not in route arguments — the
     * legacy `LoginForm`/`LoginProcess`/`LoginSso` fragments shared one Activity-scoped ViewModel
     * the same way (doc `02-auth-login-sesi.md` §4). */
    sealed interface Auth : Route {
        @Serializable
        data object Login : Auth

        @Serializable
        data object Password : Auth

        @Serializable
        data object Sso : Auth

        @Serializable
        data object ResetPassword : Auth

        @Serializable
        data class ResetPasswordSent(val email: String = "") : Auth

        /** `isSso=true` skips the password step (doc §6) and shows the "Aktivasi Pin Wallet" alert. */
        @Serializable
        data class KlaspayActivation(val isSso: Boolean = false) : Auth
    }

    /** Host for the bottom-nav shell (Pembelajaran/Pembayaran/Akun) + pushed detail routes. */
    @Serializable
    data object Main : Route

    /** Fase 2 — 3-tab shell (replaces the old Dashboard/Materi/Absensi/Profile 4-tab shell). */
    sealed interface MainTab : Route {
        @Serializable
        data object Pembelajaran : MainTab

        @Serializable
        data object Pembayaran : MainTab

        @Serializable
        data object Akun : MainTab
    }

    // Presensi (daily attendance) — pushed from the Pembelajaran (Home) tab's "Menu Pembelajaran" grid.
    @Serializable
    data object Presensi : Route

    /** Masuk/Pulang sekolah: live map, radius check, submit. */
    @Serializable
    data object PresensiMasuk : Route

    /** Presensi Dinas Luar (teachers): address + note + selfie. */
    @Serializable
    data object PresensiOffsite : Route

    /** Front-camera selfie for Dinas Luar; returns the watermarked file path through the saved state. */
    @Serializable
    data class PresensiOffsiteCamera(val address: String = "", val lat: String = "", val lng: String = "") : Route

    /** `filter`: semua/approved/rejected/pending. */
    @Serializable
    data class IzinList(val filter: String = "semua") : Route

    @Serializable
    data object IzinAdd : Route

    /** Jurnal KBM list. [action] "hadiri"/"isi" + [attendanceId]/[plotId] come from the Hub card and open that row's flow once loaded. */
    @Serializable
    data class Jurnal(val action: String = "", val attendanceId: Int = 0, val plotId: Int = 0) : Route

    /** Student "Verifikasi Jurnal" (map + radius + status). */
    @Serializable
    data class VerifikasiJurnal(val attendanceId: Int = 0, val title: String = "") : Route

    /** Journal form: teacher (hour chips) or student (one locked hour [plotId], shown as [plotLabel]). */
    @Serializable
    data class JurnalForm(val plotId: Int = 0, val plotLabel: String = "") : Route

    /** Scene photo camera; the finished file returns through the saved state ("jurnalPhoto"). */
    @Serializable
    data class JurnalCapture(val address: String = "", val lat: String = "", val lng: String = "") : Route

    @Serializable
    data class JurnalDetail(val attendanceId: Int = 0, val createdAt: String = "", val plot: String = "") : Route

    @Serializable
    data class IzinDetail(val uuid: String = "") : Route

    /** Materi entry point for students — the subject picker. */
    @Serializable
    data object Mapel : Route

    /** Materi entry point for teachers — "Materi Saya", the list of what they uploaded. */
    @Serializable
    data object MateriGuru : Route

    /** Subjects a teacher is assigned to, reached from [MateriGuru]. */
    @Serializable
    data object MapelGuru : Route

    /** Materials within one subject — shared by both roles. */
    @Serializable
    data class MateriList(val subjectId: Int = 0, val subjectName: String = "", val isTeacher: Boolean = false) : Route

    /** id-based (doc §2.4/§2.9): `MateriDetailViewModel` reads Room first, falls back to the
     * detail API, and shows "Materi Tidak tersedia" for an invalid id — none of which a
     * value-carrying route can support (also what an FCM `menu=theory` deep link needs, S4). */
    @Serializable
    data class MateriDetail(val materiId: Int = 0, val subjectId: Int = 0, val isTeacher: Boolean = false) : Route

    @Serializable
    data class UploadMateri(
        val isEdit: Boolean = false,
        val materiId: Int = -1,
        val subjectId: Int = -1,
    ) : Route

    /** In-app PDF viewer (doc §4.1/§4.6), reached only with a local file path already downloaded
     * by whichever screen opened it (Materi, Tugas; Poin later). */
    @Serializable
    data class PdfViewer(val filePath: String = "", val title: String = "") : Route

    /** Tugas entry point for students — the 3-tab Belum/Sudah/Nilai shell (doc §5.3). */
    @Serializable
    data object TugasSiswa : Route

    /** Tugas entry point for teachers — "List Tugas" + "Penilaian" shell (doc §6.3). */
    @Serializable
    data object TugasGuru : Route

    /** id-based, shared by both roles — `type` tells `TugasDetailViewModel` which student tab to
     * `ensure*FirstPage()` if the task isn't cached yet (0=backlog,1=done,2=scored, doc §5.4). */
    @Serializable
    data class TugasDetail(val tugasId: Int = 0, val type: Int = 0, val isTeacher: Boolean = false) : Route

    @Serializable
    data class UploadTugas(val isEdit: Boolean = false, val tugasId: Int = -1, val editable: Boolean = true) : Route

    @Serializable
    data class TugasTerkumpul(val collectedId: Int = 0) : Route

    @Serializable
    data class TugasScoring(val collectedId: Int = 0, val assignmentId: Int = 0) : Route

    /** Poin entry point for students — "Poin Saya" (Pelanggaran/Prestasi tabs, doc §10). */
    @Serializable
    data object PoinSiswa : Route

    /** Poin entry point for teachers = the student search, and the root the Hasil/Form screens
     * look up to share one `PoinGuruViewModel` (selected student survives the child screens). */
    @Serializable
    data object PoinGuru : Route

    @Serializable
    data object PoinHasil : Route

    @Serializable
    data class PoinForm(val mode: PoinFormMode = PoinFormMode.VIOLATION) : Route

    /** Agenda Mingguan (teachers only) — month strip + the day's sessions (doc §8). */
    @Serializable
    data object AgendaMingguan : Route

    /** Session is read back from the Room cache by `(date, agendaId)` — no Serializable extra. */
    @Serializable
    data class AgendaCheck(val date: String = "", val agendaId: Int = 0, val mode: AgendaCheckMode = AgendaCheckMode.CHECK_IN) : Route

    @Serializable
    data class AgendaDetail(val date: String = "", val agendaId: Int = 0) : Route

    /** Asesmen/AKM, pushed from Home's "Asesmen" tile. Students only. UI-only — no backend yet. */
    sealed interface Akm : Route {
        @Serializable
        data object List : Akm

        @Serializable
        data class Detail(val id: Int = 0) : Akm

        /** Exam lobby — the instruction tree, shown in exam mode. */
        @Serializable
        data class Resume(val id: Int = 0) : Akm

        @Serializable
        data class Question(val id: Int = 0) : Akm

        @Serializable
        data class Score(val id: Int = 0) : Akm

        @Serializable
        data class Explain(val id: Int = 0) : Akm
    }

    // Pushed from Home's notification bell.
    @Serializable
    data object Notifikasi : Route

    @Serializable
    data class NotifikasiDetail(val id: String = "") : Route

    /** Akun subscreens, pushed from the Akun tab. */
    sealed interface AkunSub : Route {
        @Serializable
        data object Setting : AkunSub

        @Serializable
        data object Kontak : AkunSub

        @Serializable
        data object Devices : AkunSub

        @Serializable
        data object Pass : AkunSub

        @Serializable
        data object Card : AkunSub

        @Serializable
        data object About : AkunSub
    }

    /** Klaspay/Pembayaran subscreens, pushed from the Pembayaran tab. UI-only — no backend yet. */
    sealed interface PembayaranSub : Route {
        @Serializable
        data object TopUp : PembayaranSub

        @Serializable
        data object TopUpVa : PembayaranSub

        @Serializable
        data object Transfer : PembayaranSub

        @Serializable
        data object Qr : PembayaranSub

        @Serializable
        data object Riwayat : PembayaranSub

        @Serializable
        data object Spp : PembayaranSub

        @Serializable
        data class Checkout(val billIds: List<String> = emptyList()) : PembayaranSub

        @Serializable
        data class Pin(val amountLabel: String = "", val purpose: String = "") : PembayaranSub

        @Serializable
        data class Success(val amountLabel: String = "") : PembayaranSub
    }
}
