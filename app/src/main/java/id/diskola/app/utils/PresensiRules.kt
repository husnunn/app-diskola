package id.diskola.app.utils

import id.diskola.app.utils.location.MockLocationGuard
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The daily-presensi decisions that are pure logic (no Android, no I/O), kept here so they can be
 * unit-tested and so every screen applies the same rule. Source: doc `07` §4–6, §10 and the legacy
 * `PresensiViewModel`/`PresensiMasukPage`/`PresensiIzinViewModel`.
 */
object PresensiRules {

    private val SERVER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val CLOCK = DateTimeFormatter.ofPattern("HH:mm")

    const val NOTE_MIN = 10
    const val NOTE_MAX = 500
    const val LEAVE_FILE_MAX_BYTES = 2L * 1024 * 1024
    const val DEFAULT_RADIUS_METERS = 50f

    /** `yyyy-MM-dd HH:mm:ss` → `HH:mm`; anything unparsable is shown as the server sent it. */
    fun toClock(raw: String): String = try {
        LocalDateTime.parse(raw, SERVER_TIME).format(CLOCK)
    } catch (e: Exception) {
        raw
    }

    /** An izin for today that is pending or approved closes the "Lakukan Presensi" button. */
    fun canPerformAttendance(todayLeaveStatus: String?): Boolean =
        todayLeaveStatus.orEmpty().trim().lowercase() !in setOf("pending", "approved")

    /** School radius from the session string; unparsable falls back to the legacy default of 50 m. */
    fun radiusMeters(raw: String): Float = raw.trim().toFloatOrNull() ?: DEFAULT_RADIUS_METERS

    // ---- Masuk / Pulang sekolah ----

    sealed interface MasukStatus {
        data object Loading : MasukStatus

        /** `check` itself failed — legacy mislabelled this as "jam sekolah belum diatur". */
        data object CheckFailed : MasukStatus

        /** The server declined with its own text ([attendance_response_text]); button stays hidden. */
        data class ServerMessage(val text: String) : MasukStatus

        /** `allow_attendance` false or no button label: the school's clock settings are missing. */
        data object ScheduleNotSet : MasukStatus
        data object WaitingLocation : MasukStatus
        data object Mock : MasukStatus
        data object OutsideRadius : MasukStatus
        data class Ready(val buttonLabel: String) : MasukStatus
    }

    /**
     * @param distanceMeters null until a fix exists.
     * @param restricted `absence_setting` (radius enforced).
     * @param radius school radius in meters; ≤ 0 means "cannot be checked" and is permissive (legacy).
     */
    fun masukStatus(
        loading: Boolean,
        checkFailed: Boolean,
        allowAttendance: Boolean,
        buttonLabel: String,
        serverText: String,
        hasLocation: Boolean,
        isMock: Boolean,
        restricted: Boolean,
        radius: Float,
        distanceMeters: Float?,
    ): MasukStatus = when {
        loading -> MasukStatus.Loading
        checkFailed -> MasukStatus.CheckFailed
        serverText.isNotBlank() -> MasukStatus.ServerMessage(serverText)
        !allowAttendance || buttonLabel.isBlank() -> MasukStatus.ScheduleNotSet
        !hasLocation -> MasukStatus.WaitingLocation
        isMock -> MasukStatus.Mock
        restricted && radius > 0f && distanceMeters != null && distanceMeters > radius -> MasukStatus.OutsideRadius
        else -> MasukStatus.Ready(buttonLabel)
    }

    /** How the user stands against the school radius, for the status card; display only — [masukStatus] still decides the button. */
    sealed interface Proximity {
        /** Radius is enforced but cannot be judged: no fix yet, or the radius is ≤ 0 (permissive, like [masukStatus]). */
        data object Unknown : Proximity

        /** `absence_setting` is off: attendance works anywhere. */
        data object Unrestricted : Proximity
        data class Inside(val meters: Float, val radius: Float) : Proximity
        data class Outside(val meters: Float, val radius: Float) : Proximity
    }

    /** Same comparison as [masukStatus]: exactly on the radius is still inside. */
    fun proximity(restricted: Boolean, radius: Float, distanceMeters: Float?): Proximity = when {
        !restricted -> Proximity.Unrestricted
        radius <= 0f || distanceMeters == null -> Proximity.Unknown
        distanceMeters > radius -> Proximity.Outside(distanceMeters, radius)
        else -> Proximity.Inside(distanceMeters, radius)
    }

    /** Whole meters below 1 km ("85 m"), otherwise kilometers with one decimal and a comma ("1,2 km"); a round figure drops the decimal ("10 km"). */
    fun formatDistance(meters: Float): String {
        val whole = Math.round(meters.coerceAtLeast(0f))
        if (whole < 1000) return "$whole m"
        val km = String.format(Locale("id"), "%.1f", whole / 1000f)
        return "${km.removeSuffix(",0")} km"
    }

    enum class CardTone { Neutral, Info, Success, Warning, Error }

    /** What the status card of the Masuk screen says; the screen only picks colours and an icon for [tone]. */
    data class MasukCard(
        val tone: CardTone,
        val title: String,
        val detail: String? = null,
        /** Shows the "Aktifkan lokasi" button inside the card. */
        val offersEnableLocation: Boolean = false,
    )

    /** Wording of every state is the legacy one (`PresensiMasukPage`); only the distance line and the two [MasukStatus.Ready] titles are new. null = still loading. */
    fun masukCard(status: MasukStatus, locationOff: Boolean, proximity: Proximity): MasukCard? = when (status) {
        MasukStatus.Loading -> null
        MasukStatus.CheckFailed -> MasukCard(CardTone.Error, "Gagal memuat data presensi")
        is MasukStatus.ServerMessage -> MasukCard(CardTone.Info, status.text)
        MasukStatus.ScheduleNotSet -> MasukCard(CardTone.Error, "Jadwal presensi belum diatur")
        MasukStatus.WaitingLocation ->
            if (locationOff) MasukCard(CardTone.Warning, "Lokasi perangkat nonaktif", offersEnableLocation = true)
            else MasukCard(CardTone.Info, "Mengambil lokasi…")
        MasukStatus.Mock -> MasukCard(CardTone.Error, MockLocationGuard.MESSAGE)
        MasukStatus.OutsideRadius -> MasukCard(
            CardTone.Error,
            "Anda berada di luar wilayah absensi",
            (proximity as? Proximity.Outside)?.let { distanceLine(it.meters, it.radius) },
        )
        is MasukStatus.Ready -> when (proximity) {
            is Proximity.Inside -> MasukCard(CardTone.Success, "Anda berada di wilayah absensi", distanceLine(proximity.meters, proximity.radius))
            Proximity.Unrestricted -> MasukCard(CardTone.Neutral, "Absensi bisa dilakukan dimanapun")
            else -> MasukCard(CardTone.Neutral, "Lokasi Anda sudah ditemukan")
        }
    }

    private fun distanceLine(meters: Float, radius: Float) = "${formatDistance(meters)} dari titik sekolah · radius ${formatDistance(radius)}"

    /** `schedule.source` of the check response as the legacy screen worded it; blank gives no source at all. */
    fun scheduleSourceLabel(source: String): String = when (source.trim().lowercase(Locale.ROOT)) {
        "" -> ""
        "settings" -> "Pengaturan jam"
        "calendar" -> "Kalender"
        else -> source.trim()
    }

    // ---- Dinas Luar ----

    data class OffsiteErrors(val note: String? = null, val address: String? = null, val photo: String? = null) {
        val any: Boolean get() = note != null || address != null || photo != null
    }

    /** Length is counted on the trimmed text (legacy counted raw, so ten spaces enabled the button). */
    fun validateOffsite(note: String, address: String, hasPhoto: Boolean, requirePhoto: Boolean): OffsiteErrors {
        val trimmed = note.trim()
        return OffsiteErrors(
            note = when {
                trimmed.length < NOTE_MIN -> "Catatan minimal $NOTE_MIN karakter"
                trimmed.length > NOTE_MAX -> "Catatan maksimal $NOTE_MAX karakter"
                else -> null
            },
            address = if (address.isBlank()) "Alamat wajib diisi" else null,
            photo = if (requirePhoto && !hasPhoto) "Foto bukti wajib diunggah" else null,
        )
    }

    fun canSubmitOffsite(allowAttendance: Boolean, loading: Boolean, note: String, hasPhoto: Boolean, requirePhoto: Boolean): Boolean =
        allowAttendance && !loading && note.trim().length >= NOTE_MIN && (hasPhoto || !requirePhoto)

    /** A Dinas Luar response fails when `code` is explicitly 0, or when there is no `code` and no status either. */
    fun offsiteSucceeded(code: Int?, status: String?): Boolean = when (code) {
        null -> !status.isNullOrBlank()
        0 -> false
        else -> true
    }

    // ---- Izin ----

    enum class LeaveBlock(val message: String) {
        CHECKED_IN("Sudah ada presensi masuk hari ini."),
        PENDING("Pengajuan hari ini masih menunggu persetujuan."),
        APPROVED("Pengajuan hari ini sudah disetujui."),
    }

    /** Null = may submit: not yet checked in today, and today's izin is absent, rejected or blank. */
    fun leaveBlock(hasCheckedInToday: Boolean, todayApprovalStatus: String?): LeaveBlock? = when {
        hasCheckedInToday -> LeaveBlock.CHECKED_IN
        todayApprovalStatus.orEmpty().trim().equals("pending", ignoreCase = true) -> LeaveBlock.PENDING
        todayApprovalStatus.orEmpty().trim().equals("approved", ignoreCase = true) -> LeaveBlock.APPROVED
        else -> null
    }

    /** First problem in the order the form reports it (file → jenis → keterangan), or null. */
    fun validateLeave(hasFile: Boolean, status: String?, note: String, fileBytes: Long): String? {
        val trimmed = note.trim()
        return when {
            !hasFile -> "File bukti wajib diunggah"
            status.isNullOrBlank() -> "Jenis pengajuan wajib dipilih"
            trimmed.length < NOTE_MIN -> "Keterangan minimal $NOTE_MIN karakter"
            trimmed.length > NOTE_MAX -> "Keterangan maksimal $NOTE_MAX karakter"
            fileBytes > LEAVE_FILE_MAX_BYTES -> "Ukuran file maksimal 2 MB"
            else -> null
        }
    }
}
