package id.diskola.app.utils

import id.diskola.app.dataclass.ResponData.CurrentSubjectData
import id.diskola.app.dataclass.ResponData.CurrentUserResponse
import id.diskola.app.dataclass.ResponData.PresensiCheckData
import id.diskola.app.dataclass.ResponData.PresensiSchedule
import id.diskola.app.dataclass.ResponData.TodaySettingData
import id.diskola.app.dataclass.ResponData.UserProgressData

/**
 * Decisions of the Pembelajaran hub that are pure logic, kept Android-free so they are unit-tested. Sources are the
 * legacy `PembelajaranPage.kt` (verify/class-card/buttons), `PresensiViewModel.kt` (attendance banner), `PembelajaranVm.kt`
 * (bell badge) and `SessionGuard.kt` (device check). Texts are the legacy strings, verbatim.
 */
object HubRules {

    const val VERIFY_LABEL = "Verifikasi Data"
    const val NOTICE_TITLE = "Pemberitahuan"
    const val NOT_IN_CLASS_NOTICE = "Anda Belum Terdaftar di Kelas Manapun,\nSilakan Hubungi Admin!!"
    const val NO_SCHEDULE = "Tidak terdapat jadwal"
    const val NO_CLASS_YET = "Belum memiliki kelas"
    const val CTA_PRESENSI = "Presensi"
    const val BELL_NEEDS_WALLET = "Silahkan aktivasi wallet terlebih dahulu untuk mengunakan fitur notifikasi"

    // ---- Verification button / what the card area shows ----

    sealed interface VerifyState {
        /** Verified (or nothing to verify): the class card (students) or the attendance banner (teachers) is shown. */
        data object ShowContent : VerifyState

        /** A student who is in no class: no card and no button, just [NOT_IN_CLASS_NOTICE]. */
        data object StudentNoClass : VerifyState

        /** The enabled "Verifikasi Data" button. */
        data object VerifyAvailable : VerifyState

        /** Request in review or rejected: a disabled button that shows the server's [message]. */
        data class VerifyBlocked(val message: String, val rejected: Boolean) : VerifyState
    }

    /** `userProgress.status` × role × `is_having_class`, in the order of legacy `PembelajaranPage.kt:156-208`. */
    fun verifyState(progress: UserProgressData?, isStudent: Boolean, isHavingClass: Boolean): VerifyState {
        val status = progress?.status.orEmpty().trim().lowercase()
        val message = progress?.message.orEmpty()
        return when {
            status == UserProgressData.APPROVED -> VerifyState.ShowContent
            status.isEmpty() && isStudent && !isHavingClass -> VerifyState.StudentNoClass
            status.isEmpty() && !isHavingClass -> VerifyState.VerifyAvailable
            status.isEmpty() -> VerifyState.ShowContent
            !isHavingClass && (status == UserProgressData.IN_REVIEW || status == UserProgressData.REJECTED) ->
                VerifyState.VerifyBlocked(message.ifBlank { VERIFY_LABEL }, rejected = status == UserProgressData.REJECTED)
            else -> VerifyState.ShowContent
        }
    }

    // ---- Student "Kelas Berlangsung" card ----

    enum class ClassAction { NONE, CHECK_IN, ISI_JURNAL }

    /**
     * Legacy `PembelajaranPage.kt:129-155`: a running class with no attendance row → "Isi Jurnal"; with one and no status
     * yet → "Check In"; with a status → nothing. No class (id 0 or missing) → nothing. A null `id` hides the buttons
     * (legacy let it through its `!= 0` check).
     */
    fun classAction(subject: CurrentSubjectData?, isStudent: Boolean, isHavingClass: Boolean): ClassAction {
        if (!isStudent || !isHavingClass) return ClassAction.NONE
        val current = subject ?: return ClassAction.NONE
        if ((current.id ?: 0) == 0) return ClassAction.NONE
        return when {
            current.attendance_id == null -> ClassAction.ISI_JURNAL
            current.status.isNullOrEmpty() -> ClassAction.CHECK_IN
            else -> ClassAction.NONE
        }
    }

    fun className(subject: CurrentSubjectData?): String = subject?.subject_name?.takeIf { it.isNotBlank() } ?: NO_SCHEDULE

    /** "07:00 - 08:30" exactly as the server sent the plot, hidden while there is no start. */
    fun classTime(subject: CurrentSubjectData?): String? {
        val start = subject?.plot_start_at.orEmpty()
        if (start.isBlank()) return null
        return "$start - ${subject?.plot_end_at.orEmpty()}"
    }

    // ---- Header ----

    /** Teachers show their e-mail under the name, everyone else the class name ("Belum memiliki kelas" when unknown). */
    fun classLine(isTeacher: Boolean, email: String, className: String): String =
        if (isTeacher) email else className.ifBlank { NO_CLASS_YET }

    /** Bell badge text: hidden at 0, "99+" from 99 (legacy `PembelajaranPage.kt:85-93`). */
    fun bellBadge(count: Int): String? = when {
        count <= 0 -> null
        count >= 99 -> "99+"
        else -> count.toString()
    }

    // ---- Session guard ----

    /** Another device holds this account: only when the server enforces it and names a different device. */
    fun deviceMismatch(response: CurrentUserResponse, localDeviceId: String): Boolean =
        response.single_device_login_enabled && response.device_id != null && response.device_id != localDeviceId

    // ---- Attendance banner (teachers / staff) ----

    enum class BannerState { NO_AGENDA, HOLIDAY, OUTSIDE_HOURS, CHECK_IN, CHECK_OUT, COMPLETE }

    data class AttendanceBanner(
        val state: BannerState,
        val title: String,
        /** "Jam kerja hari ini · 07:00 – 15:00", blank when unknown. */
        val workHours: String,
        /** Event name without the "Agenda · " prefix, blank when none (and on holidays). */
        val eventName: String,
        /** "Dari Kalender sekolah" …, only for the two "do it now" states. */
        val source: String,
        val info: String,
        val showCta: Boolean,
    )

    /** Legacy `PresensiViewModel.resolveAttendanceBannerState`. [today] null = no answer; [check] null = no answer. */
    fun attendanceBanner(today: TodaySettingData?, check: PresensiCheckData?): AttendanceBanner {
        val status = today?.status.orEmpty().lowercase()
        val workHours = workHours(today, check?.schedule)
        val source = sourceLabel(check?.schedule)
        val eventName = if (status == "holiday") "" else eventName(today)

        if (today == null || status == "no_agenda" || status.isEmpty()) {
            return AttendanceBanner(
                BannerState.NO_AGENDA, "Jadwal absensi belum diatur", workHours, eventName, "",
                "Hubungi admin sekolah untuk mengatur jam atau kalender absensi.", showCta = false,
            )
        }
        if (status == "holiday") {
            val holiday = today.events.orEmpty().firstOrNull { it.day_off == true }?.name?.takeIf { it.isNotBlank() } ?: "Libur"
            return AttendanceBanner(BannerState.HOLIDAY, "Hari libur", workHours, "", "", holiday, showCta = false)
        }
        if (check == null) return outsideHours(workHours, eventName)

        if (check.allow_attendance == true) {
            return if (check.isCheckIn) {
                AttendanceBanner(BannerState.CHECK_IN, "Wajib absen masuk sekarang", workHours, eventName, source, "", showCta = true)
            } else {
                AttendanceBanner(BannerState.CHECK_OUT, "Wajib absen pulang sekarang", workHours, eventName, source, "", showCta = true)
            }
        }

        val response = check.attendance_response_text.lowercase()
        val complete = check.type_attendance.isBlank() ||
            listOf("lengkap", "selesai", "sudah absen", "sudah melakukan").any { response.contains(it) }
        return if (complete) {
            AttendanceBanner(BannerState.COMPLETE, "Tidak perlu absen saat ini", workHours, eventName, "", "", showCta = false)
        } else {
            outsideHours(workHours, eventName)
        }
    }

    private fun outsideHours(workHours: String, eventName: String) = AttendanceBanner(
        BannerState.OUTSIDE_HOURS, "Di luar jam absensi", workHours, eventName, "",
        "Absensi hanya bisa dilakukan pada jam yang ditentukan.", showCta = false,
    )

    fun eventName(today: TodaySettingData?): String {
        val events = today?.events.orEmpty()
        if (events.isEmpty()) return ""
        if (today?.status.orEmpty().lowercase() == "holiday") {
            return events.firstOrNull { it.day_off == true }?.name?.takeIf { it.isNotBlank() }.orEmpty()
        }
        return events.firstOrNull { it.day_off != true && !it.name.isNullOrBlank() }?.name
            ?: events.firstOrNull { !it.name.isNullOrBlank() }?.name
            ?: ""
    }

    fun workHours(today: TodaySettingData?, schedule: PresensiSchedule?): String {
        val events = today?.events.orEmpty()
        val start = schedule?.start_at?.takeIf { it.isNotBlank() }
            ?: today?.jam_masuk?.takeIf { it.isNotBlank() }
            ?: events.firstOrNull { it.day_off != true && !it.start_time.isNullOrBlank() }?.start_time
        val end = schedule?.end_at?.takeIf { it.isNotBlank() }
            ?: events.firstOrNull { it.day_off != true && !it.end_time.isNullOrBlank() }?.end_time
        if (start.isNullOrBlank() && end.isNullOrBlank()) return ""
        val from = hourMinute(start.orEmpty())
        val to = hourMinute(end.orEmpty())
        return when {
            from.isNotBlank() && to.isNotBlank() -> "Jam kerja hari ini · $from – $to"
            from.isNotBlank() -> "Jam kerja hari ini · $from"
            else -> "Jam kerja hari ini · $to"
        }
    }

    fun sourceLabel(schedule: PresensiSchedule?): String {
        val raw = schedule?.source.orEmpty()
        val source = raw.lowercase()
        return when {
            source.contains("calendar") || source.contains("kalender") -> "Dari Kalender sekolah"
            source.contains("setting") || source.contains("pengaturan") || source.contains("jam") -> "Dari Pengaturan jam"
            raw.isNotBlank() -> "Dari $raw"
            else -> ""
        }
    }

    /** "2026-10-07 07:00:00" / "07:00:00" → "07:00". */
    private fun hourMinute(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        val time = if (trimmed.contains(" ")) trimmed.substringAfterLast(" ").trim() else trimmed
        return if (time.length >= 5 && time[2] == ':') time.substring(0, 5) else time
    }
}
