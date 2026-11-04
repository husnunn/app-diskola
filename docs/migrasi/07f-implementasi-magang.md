# 07f — Implementasi Fase Magang (siswa)

Dikerjakan 2026-11-04 dalam satu fase (jadwal, presensi masuk, laporan harian + presensi keluar, daftar laporan, izin magang). Spesifikasi: `07` §11, §14.4, §16, §19#8–12. Status per item: `07e-gap-magang.md`. Keputusan: `docs/FLOW_QUESTIONS.md` ("2026-11-04 — Fase Magang (siswa)"). Path relatif `app/src/main/java/id/diskola/app/`.

## 1. Arsitektur

| Berkas | Tanggung jawab |
|---|---|
| `apiservice/MagangApiService.kt` | Bertipe (sebelumnya `Map<String, Any>` dan **tidak terdaftar** di Hilt): `schedule`, `attend` (`{id, lat, lng}` Double), `attendance`, multipart `dailyReport` (`id`, `daily_report`, `file`), multipart `leaveRequest` (`id`, `status`, `note`, `file`), `deleteLeaveRequest` (tidak dipakai). Aksi tulis mengembalikan `Unit`. |
| `dataclass/ResponData/MagangModels.kt` | DTO lenient (`@NullToEmptyString`, semua field nullable/default). `company.id` **Long** (server dev mengirim 2149267629 > `Int.MAX_VALUE`); radius dan koordinat dibaca sebagai String (angka juga terbaca). |
| `repository/MagangRepository.kt` | Tanpa cache Room. `translate{}` meneruskan `ApiException` 401/403/5xx apa adanya (pesan umum aplikasi), selain itu alasan server menjadi pesan. Memberi `user` (nama/foto) dari `SessionStore`. |
| `utils/MagangRules.kt` (+ `MagangRulesTest` 35 tes, `MagangModelsParseTest` 5 tes) | Logika murni: tanggal/jam, jadwal per hari, tombol kartu (`actions`, `statusText`), laporan (urut, rentang, "Lihat Selengkapnya", lampiran, penghitung, validasi), izin (gabungan dua sumber, bulan/jenis, banner, validasi 10–5000), status presensi masuk (`attendStatus`: server dulu → posisi → lokasi palsu → radius; `attendCard`), label rentang/tanggal-jam. |
| `viewmodel/MagangViewModel.kt` | Di-scope ke back stack `Route.Magang`, dibagi semua layar anak (`getBackStackEntry` + `hiltViewModel(entry)`). Satu `refresh()` memuat jadwal dan `attendance` bersamaan dengan hasil masing-masing; refresh baru membatalkan yang lama; refresh lagi saat kembali dari layar anak. Menyimpan tanggal terpilih dan rentang laporan. |
| `viewmodel/MagangAttendViewModel.kt` | Posisi hidup, lokasi palsu, tampilan jarak/status (`MagangAttendView`), kirim `attend`. |
| `viewmodel/MagangReportViewModel.kt` | Catatan, foto (kompres ≤ 5 MB), kirim laporan. |
| `viewmodel/MagangIzinAddViewModel.kt` | Jenis, keterangan, bukti gambar (kompres ≤ 2 MB), validasi per kolom, kirim. |
| `ui/screens/magang/` | `MagangScreen` (shell: tab Jadwal/Laporan, "Tambah Izin", Panduan = tur spotlight, picker bulan/rentang), `MagangParts` (kartu jadwal, label bulan), `MagangAttendScreen` + `MagangAttendParts`, `MagangReportScreens` (+ `MagangReportParts`: kartu laporan, pratinjau lampiran dengan zoom, kotak lampiran), `MagangIzinScreens` (daftar, ajukan, detail), `MagangPreviews`. |
| `ui/components/CoachMarks.kt` + `utils/CoachMarkRules.kt` (`CoachMarkRulesTest` 6 tes) | Tur spotlight bersama (padanan `OnboardingGuide` legacy): `CoachStep`, `coachTarget(state, key)`, `CoachMarkOverlay` (layar digelapkan, elemen dipotong + dibingkai, gelembung berpanah, Lanjut/Selesai, Kembali menutup). `UnderlineTabRow` mendapat `tabModifier`. Penempatan gelembung mengikuti legacy (sisi dengan ruang lebih, di atas hanya bila muat). |
| Dipakai ulang / disentuh | `AttendanceMap`, `MapFrame`, `DistancePill`, `MasukStatusCard`, `MasukHero`, `MasukBottomBar`, `ScheduleTile` (Presensi; label peta/jarak kini parameter, `MasukHero`/`ScheduleTile` jadi `internal`); `AppDateRangePickerDialog` (baru di `AppPickerDialogs.kt`); `DateStrip`, `MonthYearPickerDialog`, `AutoTimeGate`, `AppDialog`, `PhotoCapture`, `LocationProvider`, `MockLocationGuard`. |
| Navigasi / Home | `Route.Magang`, `MagangAttend`, `MagangReport`, `MagangReportDetail`, `MagangIzinList`, `MagangIzinAdd`, `MagangIzinDetail`; `HomeScreen` (`onNavigateToMagang`), `HomeMenuGrid` (`implemented = true`), `MainTabsScreen`. |

## 2. Keputusan (user) — `FLOW_QUESTIONS.md`
Cakupan seluruh Magang dalam satu fase; data uji boleh ditulis ke server dev (tiap tombol tulis disebut sebelum ditekan dan dicatat); PR menunggu fase selesai. Sepuluh keputusan awal (tanpa Room, tanpa geofence/lokasi latar, tolak lokasi palsu, "tersimpan" hanya bila sukses, foto dikompres, dua tombol kartu atas-bawah, tanpa unduh avatar marker, tutup+segarkan sesudah presensi, Panduan — awalnya bottom sheet statis, direvisi menjadi tur spotlight seperti legacy —, parsing toleran) dan sembilan tambahan dari uji device (#11–#19: dialog konfirmasi presensi, tombol menunggu posisi, perusahaan tanpa koordinat, format waktu laporan, pemilih rentang, izin kamera, kompres bukti izin, kesalahan per kolom, bukti sebagai gambar).

## 3. Temuan teknis
| # | Temuan | Penanganan |
|---|---|---|
| 1 | `company.id` > `Int.MAX_VALUE` pada data dev membuat **seluruh** daftar jadwal gagal dibaca; layar menampilkan teks Moshi mentah | `Long?` + tes regresi dari baris nyata. Teks mentah itu kelemahan `AppErrorHandler` (cabang `else`): tugas terpisah diusulkan. |
| 2 | Respons `schedule` nyata: `radius` angka (bukan String), `status` berkosakata tetap ("Presensi Masuk", "Belum waktunya presensi masuk", "Waktu presensi masuk sudah lewat", "Belum waktunya presensi pulang", "Presensi hari ini selesai") | Teks `status` ditampilkan apa adanya bila kartu tanpa tombol. |
| 3 | Server menurunkan `can_attend`, `can_submit_leave_request` setelah presensi masuk | Kartu menyegar; form izin terkunci dengan banner legacy. |
| 4 | Server **menerima** `POST leave` walau `can_leave=false` | Gerbang jendela pulang hanya klien; dicatat sebagai risiko di `FLOW_QUESTIONS.md`. |
| 5 | Legacy: laporan "tersimpan" muncul walau gagal; foto > 5 MB ditolak dengan pesan "8MB"; geofence + lokasi latar selalu diminta; avatar marker diunduh tanpa try/catch | Diperbaiki sesuai keputusan di atas. |
| 6 | Manifest mendeklarasikan CAMERA, jadi kamera sistem menolak dibuka tanpa izin runtime | `rememberPermissionState(CAMERA)` sebelum `TakePicture`; pesan "Izin kamera dibutuhkan" bila ditolak. |
| 7 | Picker dokumen Samsung (fallback `PickVisualMedia` di Android 11) tidak menerima ketukan terprogram | Galeri tidak teruji di device; kamera teruji. |
| 8 | Kunci `LazyColumn` dari `id` jadwal berisiko crash bila server mengirim id ganda | Daftar jadwal tanpa kunci; daftar izin sudah unik (`distinctBy`). |
| 9 | `BannerError` hilang sendiri setelah beberapa detik dan, di bagian atas kolom yang bisa tergulir, tak terlihat bila form sudah tergulir (terlihat saat server menolak izin) | Pada form laporan dan izin Magang banner dipindah ke bar bawah, di atas tombol. |
| 10 | Server menolak `leave-request`: "Izin/sakit hanya dapat diajukan pada hari ini" (jadwal besok) dan "Anda sudah melakukan presensi hari ini" (hari ini, sudah presensi) | Klien sudah mencerminkan aturan ini (`submittableToday`, `can_submit_leave_request`); pesan server tampil apa adanya. |

## 4. Pengujian
- **JVM:** 556 tes lolos (`testDebugUnitTest`, dihitung dari XML baru; basis fase sebelumnya 506): `MagangRulesTest` 35, `MagangModelsParseTest` 5, `CoachMarkRulesTest` 6. Uji mutasi pada aturan penting (`can_attend` server-first, `radius_enabled` default) — mutan terbunuh. `:app:compileDebugAndroidTestKotlin` dan `:app:assembleDebug` bersih; terpasang ke Samsung dan Infinix.
- **Device (Samsung SM-A505F, siswa BUDI, dev server, 2026-11-04):** tile Magang → shell; tab Jadwal (117 jadwal nyata; hari ini, hari tanpa jadwal, bulan lain → tanggal 1, Panduan); presensi masuk **dijalankan sungguhan** (2 m dari titik, radius 5 m → "Konfirmasi" → `POST attend` 201 → "Absensi Berhasil!" → kartu menyegar); tab Laporan (2 laporan nyata, filter rentang, pratinjau lampiran, detail); form laporan dengan **kamera** (penghitung, tombol, "Presensi Keluar", `POST leave` multipart 201, "Laporan tersimpan", Keluar + lampiran tampil); form izin (banner terkunci; validasi urutan legacy).
- **Penulisan ke server dev (izin user):** (1) `POST attend` jadwal 2026110425; (2) `POST leave` jadwal 2026110425 (laporan harian + presensi keluar; foto kamera); (3) **izin presensi sekolah** BUDI 4 Nov 2026 (`POST attendance/student/leave-request`, 201, status Menunggu). Dua percobaan `POST internship/leave-request` (jadwal besok dan hari ini) **ditolak server (400)**, tidak ada data tertulis.
- **Metode:** tombol "Laporan Harian" dibuka lewat build sementara (tidak di-commit; dibatalkan; build bersih dipasang ulang ke kedua HP). Gambar uji netral dibuat sendiri dan dihapus dari HP (tidak ada gambar pribadi yang diunggah).

## 5. Belum diverifikasi
Lihat `07e` §7: pengajuan izin magang yang berhasil (`leave-request` 2xx; jalur gagal teruji), Galeri + kompres gambar > batas di device, kapan `can_leave` true, izin pada data nyata, jalur gagal `leave`/`attend`, `AutoTimeGate`, penolakan izin lokasi, tampilan font besar di device (hanya preview).
