package id.app.education.dataclass.mock

/**
 * UI-only mock model for the Notifikasi feature — there is no backend endpoint for this yet
 * (see `ref/fase2` design handoff). `actionPage` mirrors the design's note that "the action
 * button routes to the target module per the `page` payload" — wire it to a real deep-link
 * router once the backend contract exists.
 */
data class NotificationItem(
    val id: String,
    val icon: String,
    val title: String,
    val description: String,
    val timeLabel: String,
    val unread: Boolean,
    val actionLabel: String? = null,
    val actionPage: String? = null,
    val bannerCount: Int? = null,
)

object MockNotifications {
    val seed = listOf(
        NotificationItem(
            id = "1",
            icon = "quiz",
            title = "Ujian Tengah Semester Dimulai",
            description = "Ujian Tengah Semester mata pelajaran Informatika akan dibuka hari ini pukul 08.00 sampai 09.30. Unduh dan sinkronkan soal terlebih dahulu agar ujian tetap bisa dikerjakan bila koneksi terputus.\n\nUjian memakai password yang dibagikan pengawas di ruang ujian. Membuka aplikasi lain saat ujian berlangsung akan memicu penalti.",
            timeLabel = "5 menit lalu",
            unread = true,
            actionLabel = "Buka Ujian",
            actionPage = "asesmen",
        ),
        NotificationItem(
            id = "2",
            icon = "receipt_long",
            title = "Tagihan SPP Juli Jatuh Tempo",
            description = "Tagihan SPP bulan Juli 2026 sebesar Rp 500.000 sudah lewat jatuh tempo. Segera lakukan pembayaran untuk menghindari denda keterlambatan.",
            timeLabel = "2 jam lalu",
            unread = true,
            actionLabel = "Bayar Sekarang",
            actionPage = "spp",
        ),
        NotificationItem(
            id = "3",
            icon = "auto_stories",
            title = "Materi Baru: Struktur Data",
            description = "Guru Informatika membagikan materi baru berjudul \"Struktur Data — Linked List\" di kelas Informatika.",
            timeLabel = "Kemarin",
            unread = true,
            actionLabel = "Lihat Materi",
            actionPage = "materi",
        ),
        NotificationItem(
            id = "4",
            icon = "account_balance_wallet",
            title = "Top Up Berhasil",
            description = "Top up saldo Klaspay sebesar Rp 1.000.000 melalui Virtual Account BNI berhasil ditambahkan ke akun kamu.",
            timeLabel = "2 hari lalu",
            unread = false,
            actionLabel = "Lihat Riwayat",
            actionPage = "riwayat",
        ),
        NotificationItem(
            id = "5",
            icon = "how_to_reg",
            title = "Presensi Berhasil Dicatat",
            description = "Kehadiran kamu pada mata pelajaran Matematika hari ini sudah tercatat pukul 07.58.",
            timeLabel = "3 hari lalu",
            unread = false,
        ),
        NotificationItem(
            id = "6",
            icon = "campaign",
            title = "Pengumuman Sekolah",
            description = "Libur semester akan dimulai tanggal 20 Desember 2026. Kegiatan belajar mengajar akan kembali normal pada 5 Januari 2027.",
            timeLabel = "1 minggu lalu",
            unread = false,
        ),
    )
}
