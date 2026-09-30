package id.diskola.app.ui.navigation

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

    // Pushed from the Pembelajaran (Home) tab's "Menu Pembelajaran" grid.
    @Serializable
    data object Absensi : Route

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
    data class MateriList(val subjectId: Int = 0, val subjectName: String = "") : Route

    @Serializable
    data class MateriDetail(
        val title: String = "",
        val description: String = "",
        val teacher: String = "",
        val teacherInitials: String = "",
        val date: String = "",
        val target: String = "",
        val fileName: String = "",
        val fileSize: String = "",
        val link: String = "",
    ) : Route

    @Serializable
    data class UploadMateri(
        val isEdit: Boolean = false,
        val materiId: Int = -1,
        val materiName: String = "",
        val materiDesc: String = "",
        val subjectId: Int = -1,
        val link: String = "",
        val classId: Int = -1,
    ) : Route

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
