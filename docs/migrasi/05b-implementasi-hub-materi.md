# 05b — Implementasi Fase Pembelajaran: Hub + Materi

Laporan implementasi untuk fase "halaman pembelajaran" (Hub menu grid + audit/perbaikan Materi
siswa & guru), dikerjakan 30-09-2026. Melengkapi `05-pembelajaran-materi-tugas.md` (spesifikasi
perilaku target) dan `05a-gap-pembelajaran-hub-materi.md` (status selisih per item). Dokumen ini
menjawab: **apa yang dibangun, di berkas mana, dan apa yang sudah terbukti bekerja**.

Path kode relatif terhadap `Diskola-App-New/app/src/main/java/id/diskola/app/`, kecuali disebutkan
lain.

## Daftar isi
1. [Cakupan & keputusan awal](#1-cakupan--keputusan-awal)
2. [Arsitektur baru](#2-arsitektur-baru)
3. [Bug ditemukan & diperbaiki selama implementasi](#3-bug-ditemukan--diperbaiki-selama-implementasi)
4. [Keputusan disepakati dengan user](#4-keputusan-disepakati-dengan-user)
5. [Pengujian di device fisik](#5-pengujian-di-device-fisik)
6. [Di luar cakupan fase ini](#6-di-luar-cakupan-fase-ini)
7. [Cara verifikasi lanjutan](#7-cara-verifikasi-lanjutan)

---

## 1. Cakupan & keputusan awal

Disepakati dengan user sebelum mulai:

- **Cakupan:** Hub Pembelajaran (menu grid, doc §1) **+** audit/perbaikan Materi siswa & guru
  (doc §2–4, termasuk viewer PDF dan link preview). **Tidak termasuk:** Tugas, Perpustakaan, Agenda
  Mingguan (isi fiturnya sendiri, bukan cuma tile-nya), Poin — semua itu tetap fase terpisah
  (`01-peta-fitur-dan-rencana.md` §6: Fase 4).
- **Kedalaman pengerjaan** (empat keputusan awal, semua opsi "Disarankan" dipilih user):
  1. Endpoint Materi siswa diganti ke path yang sesuai dokumen (bukan endpoint tak terverifikasi
     yang sedang dipakai), diverifikasi di device fisik.
  2. Cache + paging Room penuh (20/halaman) untuk Materi siswa & guru, bukan jalan pintas tanpa cache.
  3. Viewer PDF internal sederhana (`PdfRenderer` + daftar halaman scroll, tanpa pinch-zoom), bukan
     mendelegasikan ke aplikasi eksternal.
  4. Link preview sederhana (scraping meta tag HTML lewat stack jaringan yang sudah ada, tanpa
     dependensi baru), bukan dibiarkan placeholder.
- **Empat keputusan perbaikan bug** (semua "Perbaiki", bukan "Tiru" — lihat §3 untuk detail tiap bug):
  pull-to-refresh Materi, auto-refresh Materi Guru setelah create/edit, filter Kelas/Mapel digabung
  AND, target wajib dipilih sebelum submit upload.
- **Satu keputusan UI:** kontrol navigasi halaman PDF yang sudah mati di app lama (tombol/label
  tanpa listener) **disembunyikan**, tidak dibangun ulang maupun ditiru sebagai dekorasi mati.
- **Satu keputusan cakupan Hub** (setelah survei kode mengonfirmasi endpoint badge notifikasi &
  Agenda Mingguan tidak ada sama sekali di app baru): bangun bagian berisiko rendah dulu (grid menu,
  locked gating, dialog, popup tamu, kartu Kelas Berlangsung nyata, cek akun nonaktif); badge
  notifikasi (rantai wallet→notification/summary) dan badge Agenda Mingguan **ditunda** ke fase
  lanjutan — lihat `05a` §1 dan §6 di bawah.

## 2. Arsitektur baru

Mengikuti pola `UI → ViewModel → Repository → RemoteDataSource(Retrofit)/LocalDataSource(Room)`
yang sama dengan fase auth, dengan Repository baru untuk domain Materi dan satu ViewModel baru
untuk Home:

| Berkas baru | Tanggung jawab |
|---|---|
| `repository/MateriRepository.kt` | Satu titik data Materi: subjects (siswa & guru), materi per subjek, "Materi Saya" guru (filter AND), detail (Room-first + fallback API), create/update/delete, referensi Kelas/Jurusan/Jenjang. Pola `ensureFirstPage/loadMore/refresh` sama seperti `SchoolRepository` fase auth. |
| `repository/FileOpenRepository.kt` | Unduh file ke `getExternalFilesDir` (tanpa izin runtime, tanpa Dexter — app-scoped dir tidak pernah butuh izin), buka lewat `FileProvider`, deteksi PDF, `DownloadManager.enqueue`. |
| `repository/LinkPreviewRepository.kt` | Scraping `og:title`/`og:image`/`<title>` dari HTML mentah via `commonApiService`, tanpa Jsoup. |
| `dataclass/localDb/MapelDao.kt`, `MateriDao.kt`, `TeacherReferenceDao.kt` | DAO Room untuk cache subjek, materi (dengan filter guru AND), dan referensi Kelas/Jurusan/Jenjang. |
| `viewmodel/MateriViewModel.kt` | Pemilih subjek + daftar materi per subjek (siswa & guru berbagi, dibedakan parameter `isTeacher`) — dipecah dari satu ViewModel lama supaya tidak jadi "God ViewModel" (`rules-global.md` §5.2). |
| `viewmodel/MateriGuruViewModel.kt` | "Materi Saya": filter Kelas/Mapel gabungan AND, delete, sinyal refresh setelah upload. |
| `viewmodel/UploadMateriViewModel.kt` | Form create/edit: data referensi, link preview debounce 1000ms, submit. |
| `viewmodel/MateriDetailViewModel.kt` | Detail materi (Room-first), buka/unduh file, link preview. |
| `viewmodel/HomeViewModel.kt` | Hub: refresh profil (`check-account`) + "Kelas Berlangsung" (`attendance/schedule`) + cek akun nonaktif. Menggantikan `SessionRefreshViewModel` (dihapus total — hanya dipakai `HomeScreen`, digabung ke sini). |

Layar (`ui/screens/materi/`): `MapelScreen`/`MapelGuruScreen` (pemilih subjek), `MateriListScreen`
(materi per subjek, dipakai kedua peran), `MateriGuruScreen` ("Materi Saya"), `MateriDetailScreen`,
`UploadMateriScreen`, `PdfViewerScreen` (baru). `ui/screens/home/HomeScreen.kt` dirombak total.
Route disederhanakan jadi berbasis id (`MateriDetail(materiId, subjectId, isTeacher)`,
`UploadMateri(isEdit, materiId, subjectId)`) menggantikan route lama yang membawa seluruh field
materi lewat argumen navigasi — `MateriDetailViewModel`/`UploadMateriViewModel` yang mengambil data
asli dari Room/API berdasarkan id.

## 3. Bug ditemukan & diperbaiki selama implementasi

Ditemukan lewat pengujian di device sungguhan, bukan asumsi:

| # | Bug | Penyebab | Perbaikan |
|---|---|---|---|
| 1 | Hub menampilkan **semua tile terkunci + tombol verifikasi** untuk akun siswa yang sebenarnya sudah punya kelas (`student_class.class_room` terisi di response `check-account` asli) | `SessionStore.writeCheckAccount()` tidak pernah menulis `IS_STUDENT`/`IS_TEACHER`/`IS_HAVING_CLASS`/`ROLES` — hanya `default_pass`/`user_id`/`is_active`/`school`/`user`. Field-field itu cuma ditulis sekali oleh `writeLoginAccount()` saat login, jadi kalau penempatan kelas terjadi **setelah** login, Hub tidak pernah tahu tanpa re-login. Ini gap lama dari fase auth (scope waktu itu memang belum mencakup Home) yang baru kelihatan sekarang karena Hub-lah yang pertama kali benar-benar memakai `is_having_class` untuk sesuatu yang terlihat pengguna. | `writeCheckAccount()` (`utils/session/SessionStore.kt`) sekarang menghitung `isStudentRole`/`isTeacherRole`/`isHavingClass` dari `response.rule`/`response.data.student.student_class` — persis logika yang sudah ada di `writeLoginAccount()` — dan menulis ulang `IS_STUDENT`/`IS_TEACHER`/`IS_HAVING_CLASS`/`ROLES`/`STUDENT_JSON`. Diverifikasi di device: sebelum perbaikan seluruh tile terkunci, sesudah perbaikan seluruh tile terbuka dan tombol verifikasi hilang, dengan response `check-account` yang sama persis. |
| 2 | **Fetch ganda** `POST check-account` setiap kali Home dibuka (dua panggilan ~300ms terpisah, terlihat di `logcat`) | `HomeScreen` punya dua pemicu independen: `LaunchedEffect(Unit) { refreshAccount() }` **dan** `DisposableEffect` yang memasang `LifecycleEventObserver` untuk `ON_RESUME` — dan `Lifecycle.addObserver` di Android **memutar ulang** event state saat ini (termasuk `ON_RESUME`) ke observer yang baru dipasang, jadi observer itu langsung terpicu sekali lagi tepat setelah `LaunchedEffect(Unit)` jalan. Pola ini sudah ada di kode Home sebelum rev ini (warisan dari draf awal), tapi baru kelihatan dampaknya sekarang karena `check-account` baru sungguh-sungguh dipanggil di sini. | `LaunchedEffect(Unit)` yang terpisah **dihapus**; hanya observer `ON_RESUME` yang dipertahankan (perilaku replay lifecycle itu sendiri sudah cukup untuk memicu load pertama kali). Diverifikasi di device lewat `logcat`: tepat satu `POST check-account` dan satu `GET attendance/schedule` per kali layar dibuka atau ditekan tombol refresh. |
| 3 | Endpoint Materi siswa memakai path tak terdokumentasi `mobile/student/school-subject...` yang tidak pernah diverifikasi terhadap kontrak API | Sisa dari draf awal sebelum dokumen `05` dibaca ulang — endpoint yang benar (`mobile/app/learning/theories/students/subjects...`) sudah ada di kode sebagai method bertipe `Map<String, Any>` yang tidak pernah dipanggil siapa pun | `MateriApiService` dirapikan: method lama dihapus total (bukan dipertahankan sebagai deprecated), method yang benar diberi tipe response yang tepat. Diverifikasi di device: `200`, data mata pelajaran asli. |
| 4 | Konflik nama tabel Room `major` — `MajorTable` (`HomeworkModels.kt`, scaffold Tugas yang belum dipakai) dan `MajorItem` (`MapelResponse.kt`, yang benar-benar dipakai) sama-sama `@Entity(tableName = "major")` | Duplikasi tak disengaja saat scaffold Tugas ditambahkan sebelumnya | `MajorTable` dihapus (dikonfirmasi tidak dipakai di mana pun lewat pencarian repo), `MajorItem` dipertahankan. |

## 4. Keputusan disepakati dengan user

Dicatat di `docs/FLOW_QUESTIONS.md` (entri "2026-09-30 — Fase Pembelajaran: Hub + Materi").
Ringkas (detail lengkap ada di §1 di atas dan `05a`):
- Cakupan: Hub + audit Materi, bukan Tugas/Perpus/Agenda/Poin.
- Endpoint Materi siswa: ganti ke yang sesuai dokumen, verifikasi di device.
- Cache/paging, viewer PDF, link preview: bangun penuh, bukan jalan pintas.
- 4 bug Materi (pull-to-refresh, auto-refresh guru, filter AND, target wajib): perbaiki.
- Kontrol halaman PDF mati: sembunyikan.
- Cakupan Hub: bagian berisiko-rendah dulu; badge notifikasi & Agenda Mingguan ditunda (endpoint
  belum ada sama sekali, tidak bisa diverifikasi tanpa akun guru).

## 5. Pengujian di device fisik

**Perangkat:** Infinix (Android 15, API 35) via `adb`, model debug (`id.diskola.app.debug`),
terhadap backend `dev.api.diskola.id`. **Akun uji:** NISN `202005` (siswa), sekolah "SMK DEMO
SURABAYA" — akun yang sama dari fase auth. **Tidak tersedia sesi ini:** akun guru, akun tamu —
lihat §7 untuk apa yang perlu diverifikasi begitu tersedia.

**Jalur yang lolos uji end-to-end** (screenshot + `adb logcat` tag `API_REQUEST`):
1. Hub: kartu "Kelas Berlangsung" menampilkan "Tidak terdapat jadwal" nyata (bukan data contoh
   "Informatika · 07.30–09.00 · Ruang Lab 2" yang sebelumnya selalu tampil hardcoded), dari response
   asli `GET attendance/schedule` yang tidak punya jadwal cocok jam saat itu.
2. Hub: dialog locked ("Fitur ini terkunci"), coming-soon ("Fitur dalam pengembangan"), dan
   verifikasi ("VERIFIKASI DATA PENGGUNA" → "Sudah"/"Belum") tampil dengan teks persis dokumen.
3. Hub → bug#1 ditemukan (semua tile terkunci padahal akun punya kelas) → diperbaiki → tile terbuka,
   navigasi ke Materi dari tile berhasil.
4. Hub → bug#2 ditemukan (fetch ganda `check-account`) → diperbaiki → tepat 1x per buka layar/tekan
   refresh, diverifikasi lewat `logcat`.
5. Materi siswa: daftar mata pelajaran (10 item asli, termasuk ikon), scroll memicu halaman
   berikutnya, cari-lokal, pull-to-refresh benar-benar memanggil ulang API (perbaikan bug pull-to-
   refresh terverifikasi).
6. Materi siswa → detail materi (`test simart jenjang`, jenjang 10, file `dummy.pdf` 12.95Kb) →
   dialog izin file → unduh (`FileOpenRepository`) → **viewer PDF internal** merender halaman nyata
   dari file yang diunduh (halaman kosong karena isi fixture backend memang kosong, bukan bug
   render — dikonfirmasi file 22KB valid 1-halaman lewat `pdftotext`/header PDF).

**Belum diuji di device** (kode ada, lihat `05a` untuk daftar lengkap per item): seluruh alur
Materi Guru (§3 doc — "Materi Saya", upload/edit, delete, filter gabungan, validasi ukuran file),
popup tamu, dialog akun nonaktif, link preview (materi uji tidak punya field `link` terisi), badge
Agenda Mingguan (tidak dibangun — lihat §6).

## 6. Di luar cakupan fase ini

- Doc `05` §5–12 (Tugas, Perpustakaan, Agenda Mingguan isi fitur, Poin) — fase terpisah.
- Badge notifikasi Hub (`payment/wallet`→`notification/summary`) dan badge Agenda Mingguan guru
  (`attendance/staff/agendas/today`) — endpoint belum ada sama sekali di app baru (dikonfirmasi
  lewat pencarian seluruh repo, bukan cuma belum di-type), butuh dibangun dari nol termasuk model
  Moshi dan pemanggil, dan badge Agenda Mingguan juga butuh akun guru untuk diverifikasi.
- Mesin status persetujuan penuh (`APPROVED`/`CLEAR`/`IN_REVIEW`/`REJECTED`) yang menentukan kapan
  tombol verifikasi aktif/nonaktif/berwarna — butuh endpoint `checkUser()`-setara yang juga belum
  ada. Tombol verifikasi sekarang pakai aturan sederhana (`isGuest || !isHavingClass`).
- `VerificationPage` sungguhan (isi alur verifikasi NISN) — cakupan dokumen `07`. Tombol "Sudah"/
  "Belum" sekarang mengarah ke dialog "Fitur dalam pengembangan" yang sama dipakai tile lain.
- `title_class` di kartu user Hub (email guru / nama kelas siswa) dan banner presensi non-siswa —
  tidak dibangun ulang di rev ini.

## 7. Cara verifikasi lanjutan

Untuk siapa pun yang melanjutkan jalur yang belum diuji (§5), dengan akun guru:
1. Login sebagai guru → Hub → pastikan tile "Agenda Mingguan" muncul (bukan "Asesmen"), lock gating
   konsisten dengan `is_having_class` guru tersebut.
2. Tab Materi (guru) → "Mata Pelajaran" → pilih subjek → "Materi Saya" → coba filter Kelas dan Mapel
   bersamaan, pastikan hasil adalah irisan (AND), bukan salah satu saja.
3. Tombol "+" → isi form upload tanpa memilih target (Jenjang/Jurusan/Kelas) → pastikan tombol
   submit tetap nonaktif; pilih target → aktif. Pilih file >12MB → pastikan pesan "ukuran file
   melebihi batas maksimal (12Mb)" muncul dan file tidak terkirim.
4. Selesaikan submit → kembali ke "Materi Saya" → pastikan daftar ter-refresh otomatis tanpa perlu
   pull-to-refresh manual (perbaikan bug §3 dokumen `05a`).
5. Edit salah satu materi → pastikan semua field ter-prefill (termasuk target/kelas/file lama).
6. Hapus salah satu materi → pastikan hilang dari daftar dan galat (jika ada) tampil di banner,
   bukan diam-diam gagal.

Dengan akun tamu: buka Hub, pastikan popup "Anda Masuk sebagai Tamu" tampil sekali saja per buka
layar (tidak muncul lagi setelah scroll/recompose, tapi muncul lagi jika layar benar-benar dibuka
ulang).

Untuk akun dengan `is_active=false` di backend (butuh diubah manual dari sisi admin/DB): buka Hub,
pastikan dialog "Peringatan" muncul dan menekan "Baik" benar-benar logout ke Login.
