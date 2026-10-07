# 05a — Selisih Implementasi Hub Pembelajaran & Materi vs Aplikasi Lama

Status per fitur untuk cakupan yang dikerjakan 30-09-2026: **Hub Pembelajaran** (doc `05` §1) dan
**Materi siswa/guru + viewer PDF/link preview** (doc `05` §2–4). Laporan implementasi lengkap ada
di `05b-implementasi-hub-materi.md`. Doc `05` §5–12 (Tugas, Perpustakaan, Agenda Mingguan, Poin)
**tidak disentuh** — itu tetap fase terpisah per `01-peta-fitur-dan-rencana.md` §6.

Path relatif terhadap `Diskola-App-New/app/src/main/java/id/diskola/app/`.

Legenda status: ✅ Selesai & diverifikasi di device fisik · ✅* Selesai (kode benar, belum sempat
diuji jalur ini secara langsung) · 🟡 Sebagian · ⛔ Ditunda (disengaja, alasan dicatat) · — Tidak
diubah/di luar cakupan.

## 1. Hub Pembelajaran (doc §1)

| Item | Status | Keterangan |
|---|---|---|
| Grid menu per-role (§1.5) | ✅ Selesai | `enum MenuKey` + `menuTilesFor(sessionStore)` (`ui/screens/home/HomeScreen.kt`) menggantikan routing berbasis posisi index. Item persis tabel doc: Siswa 7 (Materi/Tugas/Presensi/Jurnal/Asesmen/Poin/Magang), Guru 6 (…/Agenda Mingguan/Poin, tanpa Asesmen), Lainnya 5 (…/Poin, tanpa Asesmen/Agenda Mingguan/Magang). Tile "Perpus" dan tile "Agenda" (harian, terpisah dari "Agenda Mingguan") yang ada di kode sebelumnya **dihapus** — keduanya tidak ada di tabel doc manapun. |
| Locked gating seragam (§1.5) | ✅ Selesai | `locked = !sessionStore.isHavingClass` diterapkan ke **semua** tile (sebelumnya hardcoded per-tile: hanya Magang/Perpus yang locked). Diverifikasi di device: akun uji yang `is_having_class=false` (lihat 05b §3 bug#1) menunjukkan seluruh tile terkunci + tombol verifikasi, lalu setelah bug#1 diperbaiki seluruh tile terbuka. |
| Teks dialog locked/coming-soon (§1.5) | ✅ Selesai | "Fitur ini terkunci" / "Oops, nampaknya anda belum terdaftar di kelas manapun. Hubungi admin sekolah anda untuk mengakses fitur ini" / "Ok Deh"; "Fitur dalam pengembangan" / "Menu ini sedang dalam proses pembangunan, ditunggu updatenya yah…" / "Ok Deh" — persis teks doc, diverifikasi tampil di device. |
| Popup tamu (§1.4 butir 3) | ✅* Selesai | Teks persis ("Anda Masuk sebagai Tamu" / … / "Oke, Terimakasi", typo dipertahankan), sekali per instance layar. **Belum diuji di device** — tidak ada akun tamu tersedia sesi ini. |
| Kartu "Kelas Berlangsung" nyata (§1.3, §1.4) | ✅ Selesai | `HomeViewModel` memanggil `AbsensiApiService.getAttendanceSchedule(date)` (endpoint yang sudah ada, dipakai juga oleh tab Presensi) dan memilih item yang jendela waktunya mencakup waktu sekarang. Kosong → "Tidak terdapat jadwal" (fallback jujur, bukan data contoh). Diverifikasi di device dengan response asli. Field lokasi ruangan ("· Ruang Lab 2" di mockup Fase 1) **dihapus** — `AttendanceScheduleItem` tidak punya field itu sama sekali, jadi itu hiasan mockup, bukan data nyata. |
| Tombol verifikasi (§1.4 aturan tombol) | 🟡 Sebagian | Kondisi tampil disederhanakan ke `isGuest \|\| !isHavingClass` (dua dari lima baris tabel doc). Mesin status persetujuan penuh (`APPROVED`/`CLEAR`/`IN_REVIEW`/`REJECTED`, sumber `sosmedViewModel.approvalProgress`) **ditunda** — butuh endpoint `checkUser()`-setara yang belum ada sama sekali di app baru (dicek eksplisit, lihat 05b §4). Menekan tombol → dialog "VERIFIKASI DATA PENGGUNA" teks asli → "Sudah"/"Belum" keduanya mengarah ke dialog "Fitur dalam pengembangan" (bukan `VerificationPage` sungguhan — itu cakupan doc `07`). |
| Cek akun nonaktif → logout (§1.4) | ✅* Selesai | `HomeViewModel.refreshProfile()` membaca `is_active` dari `check-account` (yang sekarang dipanggil ulang tiap Home tampil) → dialog "Peringatan" / "Akun anda tidak aktif, hubungi admin sekolah untuk aktivasi" / "Baik" → `SessionManager.logout()` → ke Login. **Belum diuji di device** — akun uji yang tersedia selalu `is_active=true`. |
| Badge notifikasi (§1.4 butir 1, §1.6) | ⛔ Ditunda | Rantai `GET payment/wallet` → `GET mobile/notification/summary?wallet_id=` **tidak ada sama sekali** di app baru (diverifikasi lewat pencarian seluruh repo: `KlaspayApiService.getWallet()` dan `NotifikasiApiService.getNotificationSummary()` ada tapi sebagai placeholder `Map<String, Any>` tanpa parameter `wallet_id`, tanpa model bertipe, tanpa pemanggil). Lonceng notifikasi tetap berfungsi membuka `Notifikasi`, hanya badge angkanya yang tidak dibangun. Disepakati dengan user untuk ditunda ke fase lanjutan (risiko: endpoint belum pernah diverifikasi kontraknya terhadap backend nyata). |
| Badge Agenda Mingguan guru (§1.5 akhir) | ⛔ Ditunda | `GET mobile/attendance/staff/agendas/today` **tidak ada sama sekali** di app baru (dicek eksplisit, nol hasil). Tile "Agenda Mingguan" tetap ada di grid guru (locked-gated seperti tile lain) tapi tanpa badge count. Ditunda bersamaan dengan alasan yang sama di atas — juga tidak bisa diverifikasi sesi ini karena tidak ada akun guru untuk device-test. |
| `title_class` (Guru: email; lainnya: nama kelas) (§1.3) | — Di luar cakupan | Baris kartu user (`title_class`) tidak dibangun ulang di rev ini; `HomeHero` menampilkan nama sekolah + nama pengguna saja seperti versi sebelumnya. |
| Banner presensi non-siswa (§1.3) | — Di luar cakupan | `layout_attendance_banner` adalah bagian dokumen Presensi (`07`), tidak disentuh. |

## 2. Materi — Siswa (doc §2)

| Item | Status | Keterangan |
|---|---|---|
| Endpoint subjects (§2.5) | ✅ Selesai | `MateriApiService.studentSubjects()` diganti dari `mobile/student/school-subject...` (tidak sesuai doc, tidak pernah diverifikasi) ke `mobile/app/learning/theories/students/subjects` (sesuai doc). Diverifikasi di device: `200`, 10 mata pelajaran nyata dari `dev.api.diskola.id`. |
| Endpoint materi per subjek (§2.5) | ✅ Selesai | `studentTheories(subjectId)` → `.../students/subjects/{id}/theories`. Diverifikasi di device. |
| Endpoint detail (§2.5) | ✅* Selesai | `studentTheoryDetail(subjectId, theoryId)`. Kode benar (dipakai `MateriDetailViewModel.getDetail()` dengan fallback Room-first), diverifikasi tidak langsung (Room-first berarti jalur ini hanya kepakai untuk id yang belum ter-cache — device test memakai item yang sudah ter-cache dari list). |
| Cache + paging Room 20/halaman (§2.6) | ✅ Selesai | `MapelDao`/`MateriDao` + `MateriRepository.ensure*/loadMore*/refresh*`, pola sama persis `SchoolRepository` fase auth. Diverifikasi di device (scroll memicu `skip=10`, `skip=20` dst., halaman kosong menghentikan paging). |
| Pull-to-refresh benar-benar reload (§2.9 catatan) | ✅ Selesai | Bug lama: `MateriPage`/`MapelPage` legacy punya guard mati yang membuat pull-to-refresh tidak memanggil ulang API. Diperbaiki (keputusan user: perbaiki, bukan tiru) — diverifikasi di device lewat `logcat`: swipe-refresh memicu `GET .../theories?skip=0` baru. |
| Viewer PDF internal (§4.1/§4.2) | ✅ Selesai | `PdfViewerScreen` (Android `PdfRenderer`, render per halaman ke `LazyColumn`, `Mutex` untuk serialize render). Tanpa pinch-zoom, tanpa kontrol lompat-halaman (kontrol itu di app lama sudah mati/listener dikomentari — keputusan user: sembunyikan, bukan dibangun ulang). Diverifikasi end-to-end di device: dialog izin → unduh via `FileOpenRepository` → render 1 halaman PDF nyata (22KB, `dummy.pdf` dari backend dev, halaman kosong — ini konten fixture backend, bukan bug render). |
| Link preview sederhana (§4.5) | ✅* Selesai | `LinkPreviewRepository` (regex `og:title`/`og:image`/`<title>` dari HTML mentah, tanpa dependensi baru seperti Jsoup) + `LinkPreviewCard`. Dibangun dan dipakai `MateriDetailScreen`/`UploadMateriScreen`. **Belum diuji di device** — item materi uji yang tersedia tidak memiliki field `link` terisi. |
| Dialog izin akses file (§2.4) | ✅ Selesai | Teks "Akses File Diperlukan" persis app lama, diverifikasi tampil di device. |
| "Materi Tidak tersedia" untuk id invalid (§2.4/§2.9) | ✅* Selesai | Dialog non-cancelable dibangun di `MateriDetailViewModel`/`MateriDetailScreen`. Belum diuji langsung (butuh memaksa id tidak valid). |

## 3. Materi — Guru (doc §3)

| Item | Status | Keterangan |
|---|---|---|
| Cache + paging "Materi Saya" (§3.6) | ✅ Selesai, **diuji di device** | `MateriGuruViewModel` + `MateriRepository.observeTeacherOwnMateri/ensure*/loadMore*/refresh*`. Akun guru (NISN `20202620`) tersedia belakangan (fase Tugas) — terungkap+diperbaiki bug nyata `teacher_id` selalu `0` setelah login, yang juga mempengaruhi filter "Materi Saya" ini (sama `sessionStore.teacher?.id`); lihat `docs/FLOW_QUESTIONS.md` entri 2026-10-02 "Bug nyata ditemukan & diperbaiki" dan `05d-implementasi-tugas.md` §4 poin 6. Fetch ganda (1x vs 3x) juga diperbaiki, lihat poin di bawah. |
| Filter Kelas/Mapel gabungan AND (§3.8) | ✅* Selesai | Bug lama: filter Kelas dan Mapel di `MateriGuruScreen`/`MapelDao.observeByTeacher()` saling eksklusif dan tidak saling mereset. Diperbaiki lewat `WHERE teacher_id = :teacherId AND (:subjectId IS NULL OR subject_id = :subjectId) AND (:classId IS NULL OR class_id = :classId)` (keputusan user: perbaiki). Belum diuji di device. |
| Auto-refresh setelah create/edit/delete (§3.8) | ✅* Selesai | Bug lama: layar "Materi Saya" tidak pernah refresh otomatis setelah upload/edit tanpa pull-to-refresh manual. Diperbaiki lewat `previousBackStackEntry.savedStateHandle["materiChanged"]` (`ui/navigation/MainTabsScreen.kt`) yang memicu `MateriGuruViewModel.onMateriChanged()` saat kembali dari `UploadMateri`. Belum diuji di device. |
| Target wajib dipilih sebelum submit (§3.4) | ✅* Selesai | Bug lama: form upload mengirim `grade="null"` (string literal) bila radio target tak pernah disentuh. Diperbaiki: tombol submit nonaktif sampai target (Jenjang/Jurusan/Kelas) benar-benar dipilih (`UploadMateriScreen.canSubmit`). Belum diuji di device. |
| Endpoint Kelas form vs filter berbeda (§3.4) | ✅* Selesai | `refreshFormClasses()` (`mobile/teacher/school-class-room`, dropdown form upload) dipisah dari `refreshClasses()` (`mobile/app/learning/assignment/teachers/class`, filter "Materi Saya") — dua endpoint berbeda sesuai doc, berbagi satu tabel cache `classroom`. Belum diuji di device. |
| Validasi ukuran file 12MB (§3.4/§4.4) | ✅* Selesai | Bug lama: file >12MB diam-diam gagal terkirim (`filePart = null` tanpa pesan). Diperbaiki: dicek di titik pilih file (`OpenableColumns.SIZE`, sebelum disalin), pesan "ukuran file melebihi batas maksimal (12Mb)" persis teks lama. Belum diuji di device. |
| MIME allowlist pemilih file (§4.4) | ✅* Selesai | `ActivityResultContracts.OpenDocument()` dengan array MIME (pdf/jpeg/png/image umum/doc/docx/xls/xlsx/ppt/pptx) menggantikan `GetContent("*/*")` tanpa batasan. Belum diuji di device. |

## 4. Ringkasan cakupan pengujian device

Diuji end-to-end di Infinix (Android 15) dengan akun **siswa** (`202005` / SMK Demo Surabaya):
Hub (semua dialog + kartu Kelas Berlangsung nyata), Materi siswa (daftar subjek → daftar materi →
detail → buka PDF internal), pull-to-refresh. **Tidak diuji** karena tidak ada akun guru tersedia
sesi ini: seluruh alur Materi Guru (§3), badge Agenda Mingguan, popup tamu, dialog akun nonaktif,
link preview (materi uji tidak punya field link terisi). Semua item ini bertanda ✅*/⛔ di tabel di
atas dan perlu diverifikasi begitu akun guru/tamu tersedia — lihat 05b §5 untuk detail kredensial
yang dibutuhkan.
