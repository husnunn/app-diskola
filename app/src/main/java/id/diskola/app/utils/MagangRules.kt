package id.diskola.app.utils

import id.diskola.app.dataclass.ResponData.MagangAttendanceItem
import id.diskola.app.dataclass.ResponData.MagangAttendanceState
import id.diskola.app.dataclass.ResponData.MagangScheduleItem
import id.diskola.app.utils.PresensiRules.CardTone
import id.diskola.app.utils.PresensiRules.MasukCard
import id.diskola.app.utils.PresensiRules.MasukStatus
import id.diskola.app.utils.PresensiRules.Proximity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The Magang (internship) decisions that are pure logic, kept here so they can be unit-tested and so every screen applies the same rule.
 * Source: doc `07` §11 and the legacy `pages/magang/` classes (`MagangPage`, `MagangSchedulePage`, `MagangWriteReport`, `MagangReportPage`,
 * `MagangIzin*`). Where this differs from legacy the reason is next to the rule (and in `docs/FLOW_QUESTIONS.md`).
 */
object MagangRules {

    const val REPORT_MIN_CHARS = 100

    /** Legacy: the report photo was refused above 5 MiB, with a message that said "8MB". */
    const val REPORT_IMAGE_MAX_BYTES = 5L * 1024 * 1024

    const val LEAVE_NOTE_MIN = 10
    const val LEAVE_NOTE_MAX = 5000
    const val LEAVE_IMAGE_MAX_BYTES = 2L * 1024 * 1024

    /** "Lihat Selengkapnya" shows for a report longer than this (legacy `> 121`). */
    const val EXCERPT_LIMIT = 121

    const val RECORD_LEAVE = "leave_request"
    const val TYPE_SAKIT = "sakit"
    const val TYPE_IZIN = "izin"

    private val DAY_LABEL = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id"))
    private val IMAGE_URL = Regex("""(?i)https?://\S+\.(png|jpe?g|webp|gif)(?:\?\S+)?""")

    // ---- Dates and times ----

    /** The date part of `yyyy-MM-dd` or `yyyy-MM-dd HH:mm:ss`; null when it is neither (a time alone, or blank). */
    fun dateOf(raw: String?): LocalDate? = raw?.trim()?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** `04 Jun 2026`; what the server sent when it cannot be read as a date. */
    fun dateLabel(raw: String?): String = dateOf(raw)?.format(DAY_LABEL) ?: raw.orEmpty()

    /** `15:00 – 19:15` from `15:00:00` / `19:15:00` (legacy showed the raw strings joined by " : "). */
    fun timeRange(start: String, end: String): String = "${JurnalRules.shortClock(start)} – ${JurnalRules.shortClock(end)}"

    /** Every day of [month], the date strip's cells. */
    fun daysOf(month: YearMonth): List<LocalDate> = (1..month.lengthOfMonth()).map { month.atDay(it) }

    // ---- Schedule (tab Jadwal) ----

    /** The cards of one day (legacy: `WHERE DATE(date) = DATE(:selected)`), newest schedule first when a day has several. */
    fun schedulesOn(items: List<MagangScheduleItem>, day: LocalDate): List<MagangScheduleItem> =
        items.filter { dateOf(it.schedule?.date) == day }

    /** The company named on the card; a schedule the server sent without one still gets a title. */
    fun companyName(item: MagangScheduleItem): String = item.company?.name?.trim().orEmpty().ifEmpty { "Tempat Magang" }

    enum class CardAction { Masuk, LaporanHarian }

    /**
     * The buttons of a schedule card. Legacy put "Laporan Harian" (`can_leave`) and "Masuk" (`can_attend`) in the same spot, so a day
     * with both flags drew them on top of each other; here both are offered, report first, since a student who can leave has already come in.
     */
    fun actions(state: MagangAttendanceState?): List<CardAction> = buildList {
        if (state?.can_leave == true) add(CardAction.LaporanHarian)
        if (state?.can_attend == true) add(CardAction.Masuk)
    }

    /** The server's `att_status`, shown only when the card has no button (legacy `!(can_attend || can_leave)`). */
    fun statusText(state: MagangAttendanceState?): String? =
        if (actions(state).isEmpty()) state?.status?.trim()?.takeIf { it.isNotEmpty() } else null

    // ---- Reports (tab Laporan) ----

    fun isLeave(item: MagangAttendanceItem): Boolean = item.record_type == RECORD_LEAVE

    /** Attendance reports, newest check-in first (legacy sorted the same way). Izin rows live in [leavesOf]. */
    fun reportsOf(items: List<MagangAttendanceItem>): List<MagangAttendanceItem> =
        items.filterNot(::isLeave).sortedByDescending { it.check_in_at }

    /**
     * The "Semua" / date-range filter: [from] and [to] inclusive, either may be null (open end). A report whose check-in has no readable
     * date is kept only when no range is set at all.
     */
    fun reportsIn(reports: List<MagangAttendanceItem>, from: LocalDate?, to: LocalDate?): List<MagangAttendanceItem> {
        if (from == null && to == null) return reports
        return reports.filter { report ->
            val day = dateOf(report.check_in_at) ?: return@filter false
            (from == null || !day.isBefore(from)) && (to == null || !day.isAfter(to))
        }
    }

    fun isLongReport(text: String): Boolean = text.length > EXCERPT_LIMIT

    /** The first image link written inside a report's text (legacy showed it as the attachment when `file` was empty). */
    fun firstImageUrl(text: String): String? = IMAGE_URL.find(text)?.value

    /** What the "Lampiran" icon opens: the `file`, else an image link in the text, else nothing. */
    fun attachmentOf(item: MagangAttendanceItem): String? = item.file.takeIf { it.isNotBlank() } ?: firstImageUrl(item.daily_report)

    /** The latitude of a report's location, whichever spelling the server used (legacy: `latitide` unless zero). */
    fun reportLatitude(item: MagangAttendanceItem): Double {
        val typo = coordinate(item.location?.latitide)
        return if (typo != 0.0) typo else coordinate(item.location?.latitude)
    }

    // ---- Writing a report (check-out) ----

    /** `N / min. 100 karakter`, counted on what was typed (legacy). */
    fun reportCounter(notes: String): String = "${notes.length} / min. $REPORT_MIN_CHARS karakter"

    fun reportCounterReached(notes: String): Boolean = notes.length >= REPORT_MIN_CHARS

    /** The button is enabled once there is a picture and enough text; legacy counted the untrimmed text here but the trimmed one on submit. */
    fun canSubmitReport(hasImage: Boolean, notes: String): Boolean = hasImage && notes.trim().length >= REPORT_MIN_CHARS

    /** null = fine. Order as legacy: picture, picture size, notes (trimmed). The size message says 5 MB (legacy said "8MB"). */
    fun validateReport(hasImage: Boolean, imageBytes: Long, notes: String): String? = when {
        !hasImage -> "Harap unggah gambar terlebih dahulu"
        imageBytes > REPORT_IMAGE_MAX_BYTES -> "Foto lebih dari 5 MB"
        notes.trim().length < REPORT_MIN_CHARS -> "Catatan minimal $REPORT_MIN_CHARS karakter (saat ini ${notes.trim().length} karakter)"
        else -> null
    }

    // ---- Izin ----

    /** One izin in the list: from `GET attendance` (`record_type = leave_request`) or from a schedule day that carries one. */
    data class Leave(
        val id: Long,
        val date: String,
        /** `sakit` or `izin` (the server keeps the type in `status`). */
        val type: String,
        val note: String,
        val file: String,
        val companyName: String,
        val companyAddress: String,
    )

    fun leavesFromAttendance(items: List<MagangAttendanceItem>): List<Leave> = items.filter(::isLeave).map {
        Leave(it.id, it.date, it.status, it.note, it.file, it.company?.name.orEmpty(), it.company?.address.orEmpty())
    }

    fun leavesFromSchedules(items: List<MagangScheduleItem>): List<Leave> = items.mapNotNull { item ->
        val ref = item.attendance?.leave_request ?: return@mapNotNull null
        val id = ref.id?.takeIf { it > 0 } ?: return@mapNotNull null
        Leave(id, item.schedule?.date.orEmpty(), ref.status, ref.note, ref.file, item.company?.name.orEmpty(), item.company?.address.orEmpty())
    }

    /** Both sources, one row per id; the attendance list wins (legacy `MagangIzinViewModel`), newest day first. */
    fun mergeLeaves(fromAttendance: List<Leave>, fromSchedules: List<Leave>): List<Leave> =
        (fromAttendance + fromSchedules).distinctBy { it.id }.sortedWith(compareByDescending<Leave> { it.date }.thenByDescending { it.id })

    /** The list of one month, optionally one type (`null` = "Semua"). */
    fun leavesIn(leaves: List<Leave>, month: YearMonth, type: String?): List<Leave> = leaves.filter { leave ->
        val day = dateOf(leave.date)
        day != null && YearMonth.from(day) == month && (type == null || leave.type.equals(type, ignoreCase = true))
    }

    /** `Sakit` / `Izin`; any other word capitalised, blank reads as a plain "Pengajuan". */
    fun typeLabel(type: String): String = when (val t = type.trim().lowercase()) {
        "" -> "Pengajuan"
        TYPE_SAKIT -> "Sakit"
        TYPE_IZIN -> "Izin"
        else -> t.replaceFirstChar { it.uppercase() }
    }

    fun todayLeave(leaves: List<Leave>, today: LocalDate): Leave? = leaves.firstOrNull { dateOf(it.date) == today }

    /** Today's schedules the server lets the student file an izin for. */
    fun submittableToday(items: List<MagangScheduleItem>, today: LocalDate): List<MagangScheduleItem> =
        schedulesOn(items, today).filter { it.attendance?.can_submit_leave_request == true }

    /** The yellow banner of the form: already filed today, or nothing to file for; null when the form can be used. */
    fun leaveBanner(todayLeave: Leave?, submittable: List<MagangScheduleItem>): String? = when {
        todayLeave != null -> "Anda sudah mengajukan ${typeLabel(todayLeave.type).lowercase()} hari ini."
        submittable.isEmpty() -> "Tidak ada jadwal magang yang dapat diajukan izin hari ini."
        else -> null
    }

    /** `04 Jun 2026 • 15:00 – 19:15`, the choice shown when more than one schedule can take an izin. */
    fun scheduleChoiceLabel(item: MagangScheduleItem): String {
        val time = item.schedule
        return "${dateLabel(time?.date)} • ${timeRange(time?.start_time.orEmpty(), time?.end_time.orEmpty())}"
    }

    enum class LeaveField { Schedule, File, Type, Note }

    data class LeaveInvalid(val field: LeaveField, val message: String)

    /**
     * null = fine. Order as legacy (the screen checked schedule and file, the ViewModel type, notes, file again): schedule, file, type,
     * notes 10–5000 (trimmed), file size. [fileBytes] is what will be sent (after any compression).
     */
    fun validateLeave(scheduleId: Long, hasFile: Boolean, type: String?, note: String, fileBytes: Long): LeaveInvalid? = when {
        scheduleId <= 0 -> LeaveInvalid(LeaveField.Schedule, "Pilih jadwal magang terlebih dahulu")
        !hasFile -> LeaveInvalid(LeaveField.File, "File bukti wajib diunggah")
        type != TYPE_SAKIT && type != TYPE_IZIN -> LeaveInvalid(LeaveField.Type, "Jenis pengajuan wajib dipilih")
        note.trim().length < LEAVE_NOTE_MIN -> LeaveInvalid(LeaveField.Note, "Keterangan minimal $LEAVE_NOTE_MIN karakter")
        note.trim().length > LEAVE_NOTE_MAX -> LeaveInvalid(LeaveField.Note, "Keterangan maksimal $LEAVE_NOTE_MAX karakter")
        fileBytes > LEAVE_IMAGE_MAX_BYTES -> LeaveInvalid(LeaveField.File, "Ukuran file maksimal 2 MB")
        else -> null
    }

    /** At pick time: only pictures are accepted as proof. */
    fun leaveFileError(mimeType: String?): String? = if (mimeType?.startsWith("image/") == true) null else "File harus berupa gambar"

    // ---- Check-in (presensi masuk) ----

    const val ATTEND_BUTTON = "Konfirmasi"

    /** `restricted` is "the radius is enforced"; legacy defaulted to true when the server said nothing. */
    fun radiusEnforced(state: MagangAttendanceState?): Boolean = state?.radius_enabled != false

    const val NO_COORDINATES_MESSAGE = "Titik lokasi tempat magang belum diatur. Silakan hubungi pembimbing atau admin sekolah Anda."

    /**
     * What the check-in screen offers. The server decides first: without `can_attend` its own `status` text is shown and there is no button.
     * Then the location: the button waits for a fix (legacy showed it with the radius off and failed with "Gagal mendapatkan lokasi Anda"
     * when tapped), a mock fix is refused, and with the radius on the student must be inside it. A company without coordinates cannot be
     * "inside" for anyone (legacy measured against 0,0), so it says so instead of "outside".
     */
    fun attendStatus(
        state: MagangAttendanceState?,
        companyHasCoordinates: Boolean,
        hasLocation: Boolean,
        isMock: Boolean,
        radius: Float,
        distanceMeters: Float?,
    ): MasukStatus {
        val enforced = radiusEnforced(state)
        val serverText = when {
            state?.can_attend != true -> state?.status?.trim().orEmpty().ifEmpty { "Presensi masuk belum dapat dilakukan" }
            enforced && !companyHasCoordinates -> NO_COORDINATES_MESSAGE
            else -> ""
        }
        return PresensiRules.masukStatus(
            loading = false,
            checkFailed = false,
            allowAttendance = true,
            buttonLabel = ATTEND_BUTTON,
            serverText = serverText,
            hasLocation = hasLocation,
            isMock = isMock,
            restricted = enforced,
            radius = radius,
            distanceMeters = distanceMeters,
        )
    }

    /** The status card of the check-in screen: the school screen's wording, with the company named and legacy's "area yang diizinkan". */
    fun attendCard(status: MasukStatus, locationOff: Boolean, proximity: Proximity): MasukCard? = when (status) {
        MasukStatus.OutsideRadius -> MasukCard(
            CardTone.Error,
            "Anda berada di luar area yang diizinkan",
            (proximity as? Proximity.Outside)?.let { distanceLine(it.meters, it.radius) },
        )
        is MasukStatus.Ready -> when (proximity) {
            is Proximity.Inside -> MasukCard(CardTone.Success, "Anda berada di area yang diizinkan", distanceLine(proximity.meters, proximity.radius))
            Proximity.Unrestricted -> MasukCard(CardTone.Neutral, "Absensi bisa dilakukan dimanapun")
            else -> MasukCard(CardTone.Neutral, "Lokasi Anda sudah ditemukan")
        }
        else -> PresensiRules.masukCard(status, locationOff, proximity)
    }

    private fun distanceLine(meters: Float, radius: Float) =
        "${PresensiRules.formatDistance(meters)} dari lokasi magang · radius ${PresensiRules.formatDistance(radius)}"

    // ---- Locations and errors ----

    /** Legacy: unparsable or missing coordinates read as 0.0 (the company then sits at the equator and nobody is ever "inside"). */
    fun coordinate(raw: String?): Double = raw?.trim()?.toDoubleOrNull() ?: 0.0

    fun hasCoordinates(lat: Double, lng: Double): Boolean = !(lat == 0.0 && lng == 0.0)

    /** The text of a failed send: the server's per-field reasons first, then its message, then [fallback]. */
    fun errorMessage(message: String?, validationMessages: List<String>, fallback: String = "Terjadi kesalahan, silakan coba lagi"): String =
        validationMessages.filter { it.isNotBlank() }.joinToString("\n")
            .ifBlank { message?.takeIf { it.isNotBlank() && it != "null" } ?: fallback }
}
