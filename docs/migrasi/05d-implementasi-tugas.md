# 05d — Implementasi Fase Pembelajaran: Tugas

Laporan implementasi untuk fase "Tugas" (Siswa + Guru sekaligus), dikerjakan 2026-10-02.
Melengkapi `05-pembelajaran-materi-tugas.md` §5–6 (spesifikasi perilaku target) dan
`05c-gap-tugas.md` (status selisih per item). Dokumen ini menjawab: **apa yang dibangun, di
berkas mana, dan apa yang sudah terbukti bekerja**.

Path kode relatif terhadap `Diskola-App-New/app/src/main/java/id/diskola/app/`, kecuali disebutkan
lain.

## Daftar isi
1. [Cakupan & keputusan awal](#1-cakupan--keputusan-awal)
2. [Arsitektur baru](#2-arsitektur-baru)
3. [Keputusan disepakati dengan user](#3-keputusan-disepakati-dengan-user)
4. [Perbaikan teknis wajib (bukan bug legacy, temuan implementasi)](#4-perbaikan-teknis-wajib-bukan-bug-legacy-temuan-implementasi)
5. [Pengujian di device fisik](#5-pengujian-di-device-fisik)
6. [Di luar cakupan fase ini](#6-di-luar-cakupan-fase-ini)
7. [Cara verifikasi lanjutan](#7-cara-verifikasi-lanjutan)

---

## 1. Cakupan & keputusan awal

Disepakati dengan user sebelum mulai:

- **Fase ini:** lanjutan langsung dari Hub+Materi (`05a`/`05b`), mengikuti urutan yang didokumentasikan
  di `01-peta-fitur-dan-rencana.md` §6 dan keputusan eksplisit di `FLOW_QUESTIONS.md` yang
  memisahkan Tugas sebagai fase tersendiri setelah Materi.
- **Cakupan:** Tugas Siswa (3 tab, kumpulkan jawaban) **+** Tugas Guru (List Tugas, buat/edit/hapus,
  Penilaian, beri nilai) sekaligus dalam satu rencana — bukan siswa dulu baru guru terpisah.
  Perpustakaan, Agenda Mingguan (isi fitur), Poin tetap fase terpisah.
- **Pola arsitektur:** direplikasi persis dari Materi (`05a`/`05b`) — Repository tunggal, satu
  ViewModel per layar (`docs/rules-global.md` §5.2), DAO dengan pola `ensureFirstPage`/`loadMore`/
  `refresh`, reuse komponen desain yang sudah ada (`DetailScaffold`, `AppDialog`, `AppBottomSheet`,
  `BannerError`, `PullToRefreshBox`, `LinkPreviewCard`, `FileOpenRepository`, `PdfViewerScreen`).

## 2. Arsitektur baru

| Berkas baru | Tanggung jawab |
|---|---|
| `dataclass/ResponData/HomeworkModels.kt` (reshape in place) | Model API Tugas bertipe (sebelumnya `Map<String,Any>` di scaffold `TugasApiService`): `HomeworkItem`, `HomeworkResponse`, `HomeworkCollected`, `Assignment`/`AssignmentData`/`AssignmentResponse`, `AssignmentDay`/`AssignmentSchedule`/`AssignmentTimePlot` (yang sudah ada dipakai langsung, tidak diduplikasi), `TugasStudentItem`/`TugasStudentUser` (baru, shape sederhana khusus Tugas Terkumpul), `AssignmentFileItem`/`AssignmentAnswerDetailResponse` (baru). Entity Room baru: `HomeworkTable` (denormalized, pola `MateriTable`), `HomeworkAnswerFileTable`. `TeacherTable` (dead, hanya dipakai `HomeworkItemTable` yang sekarang dihapus) ikut dibersihkan. |
| `apiservice/TugasApiService.kt` | Return type semua method diketikkan; `collectAssignment` diubah ke multi-file (`List<MultipartBody.Part>`) — lihat §4. |
| `dataclass/localDb/HomeworkDao.kt` | DAO Tugas — per-type (siswa), per-teacher AND-filter (guru), per-collected (Penilaian), per-answer-file. |
| `repository/TugasRepository.kt` | Satu repository: tab siswa (`ensure/loadMore/refreshBacklog/Done/Scored`), detail (Room-only + answer-file fallback fetch), submit, List Tugas guru (AND filter, literal-0 "Semua"), Penilaian/Tugas Terkumpul/Scoring, referensi jadwal (Hari/Mapel cascade). `PAGE_SIZE=10` (beda dari Materi yang 20). Kelas/Mapel dropdown form tetap pakai `MateriRepository` yang sudah ada (tidak diduplikasi). |
| `viewmodel/TugasSiswaViewModel.kt` | 3 tab independen (Belum/Sudah/Nilai), masing-masing paging+loading+error sendiri. |
| `viewmodel/TugasDetailViewModel.kt` | Detail + alur kirim jawaban (file multi-pilih, link debounce, syarat reaktif, validasi, submit). |
| `viewmodel/TugasNilaiDetailViewModel.kt` | Dialog skor — load-by-id + aksi baca/unduh pembahasan, terpisah dari `TugasDetailViewModel` (tidak ada logic syarat/submit). |
| `viewmodel/TugasGuruViewModel.kt` | "List Tugas" — filter AND, delete. |
| `viewmodel/TugasPenilaianViewModel.kt` | Tab "Penilaian" — paging tanpa filter. |
| `viewmodel/TugasTerkumpulViewModel.kt` | Detail per-tugas (selalu network) + filter lokal Semua/Belum/Sudah Dinilai. |
| `viewmodel/TugasScoringViewModel.kt` | Input skor + simpan — menerima `Assignment` yang sudah dimuat `TugasTerkumpulViewModel` (tidak fetch ulang). |
| `viewmodel/UploadTugasViewModel.kt` | Form 3-mode (create/edit/detail) — cascade Kelas→Hari→Mapel, tanggal/jam, file+link, 2 checkbox. |
| `ui/screens/tugas/` (9 file baru) | `TugasSiswaScreen`, `TugasListItem`, `TugasDetailScreen`, `TugasNilaiDetailSheet`, `TugasGuruScreen`, `TugasTerkumpulScreen`, `TugasScoringScreen`, `UploadTugasScreen`. |
| `ui/components/SimpleDropdown.kt` (baru, diekstrak) | Dropdown sederhana yang sebelumnya privat di `UploadMateriScreen.kt` — sekarang dipakai juga oleh `UploadTugasScreen.kt`. |
| `ui/navigation/Route.kt` | `TugasSiswa`, `TugasGuru`, `TugasDetail(tugasId,type,isTeacher)`, `UploadTugas(isEdit,tugasId,editable)`, `TugasTerkumpul(collectedId)`, `TugasScoring(collectedId,assignmentId)`. |
| `database/LocalDatabase.kt` | Room versi 4→5, entity `HomeworkTable`/`HomeworkAnswerFileTable`/`HomeworkCollected` ditambahkan. |

`HomeScreen.kt`'s tile "Tugas" (sebelumnya jatuh ke dialog generik "Fitur dalam pengembangan")
sekarang `implemented=true` di ketiga cabang peran, routing ke `Route.TugasGuru`/`Route.TugasSiswa`
mengikuti pola percabangan `isTeacher` yang sama dengan tile Materi.

## 3. Keputusan disepakati dengan user

Dicatat lengkap di `docs/FLOW_QUESTIONS.md` (entri "2026-10-02 — Fase Tugas"). Ringkas:
- Detail tugas belum ter-cache → panggil ulang list relevan dulu (mirip `ensureFirstPage` Materi),
  baru dialog "tidak tersedia" kalau tetap tidak ketemu.
- Syarat kirim tugas (baca/upload) dibuat reaktif, bukan one-way latch.
- Form edit tugas (guru): tanggal batas pengumpulan default ke sekarang, bukan mengurai label server.
- Banner error ditambahkan ke semua layar list Tugas.
- Syarat "baca soal" diperketat — hanya unduhan sukses yang menghitung (user memilih opsi lebih
  ketat daripada rekomendasi awal).
- Filter guru "Semua" mengirim literal `0` — **tiru app lama persis** (user memilih opsi ini,
  bukan rekomendasi "hilangkan parameter").
- Fetch ganda List Tugas guru diperbaiki jadi 1x.
- Tombol "Lihat Jawaban" yang mati di app lama dihubungkan ke Detail Tugas mode guru read-only.

## 4. Perbaikan teknis wajib (bukan bug legacy, temuan implementasi)

| # | Temuan | Perbaikan |
|---|---|---|
| 1 | `TugasApiService.collectAssignment` (scaffold sebelumnya) hanya menerima **satu** file (`@Part file: MultipartBody.Part?`), padahal spesifikasi jelas minta multi-file jawaban (`file[]`, doc §5.5/§5.6) | Diubah ke `@Part files: List<MultipartBody.Part>`. **Belum diverifikasi ke backend asli** (tidak ada tugas untuk dikirim jawabannya di akun uji) — lihat `05c` §3. |
| 2 | Form buat/edit tugas (`CreateHomeworkVm` lama) mengambil `listDay` tanpa `try/catch` sama sekali — kegagalan jaringan = crash aplikasi | `UploadTugasViewModel.loadReferenceData()`/`onDaySelected()` dibungkus `launchWithHandling`/try-catch, menampilkan error alih-alih crash. |
| 3 | Konflik nama file lama `TeacherTable` di `MapelResponse.kt` hanya hidup untuk `HomeworkItemTable`'s `@Relation` (dead scaffold yang sekarang dihapus) | `TeacherTable` dan `HomeworkItemTable`/`HomeworkLink` (tabel Room `@Relation`-based yang tidak dipakai) dihapus, digantikan kolom denormalized di `HomeworkTable` (pola `MateriTable`). |
| 4 | `school_class` di `TugasApiService.teacherAssignmentSchedule` dikirim dengan nama query `class` — backend sungguhan menolak dengan 422 (dilaporkan user lewat logcat, akun guru nyata pertama kali dites fase ini) | Diganti `@Query("school_class_id")`. Diverifikasi di device: 200 OK dengan jadwal nyata. |
| 5 | Fetch ganda di `TugasGuruScreen`/`MateriGuruScreen`: `LaunchedEffect(Unit)` di screen + `onTugasChanged()`/`onMateriChanged()` di nav layer keduanya memanggil fetch pertama pada recomposition yang sama saat kembali dari form upload | Logic fetch pertama dipindah ke `init {}` masing-masing ViewModel (berjalan 1x per backstack entry, bukan per komposisi); `LaunchedEffect(Unit)` yang redundan dihapus dari kedua screen. Diverifikasi: sebelum 2x panggilan identik, sesudah 1x. |
| 6 | **Bug nyata, bukan sekadar risiko dokumentasi:** `SessionStore.writeLoginAccount()` menulis `TEACHER_JSON` dari `data?.teacher?.id`, tapi response asli `login-account` untuk login guru **tidak pernah menyertakan field `data.teacher` sama sekali** (dikonfirmasi lewat `adb logcat` saat re-login akun guru nyata) — akibatnya setiap login guru baru menulis `teacher.id = 0`, dan screen manapun yang bergantung pada `sessionStore.teacher?.id` (List Tugas, Materi Saya guru, dll.) memfilter dengan `teacher_id=0` sampai sesuatu lain menimpanya. `data.teacher` yang benar (`{id, name, nip}`) ternyata hanya ada di response `check-account` — yang sebelumnya tidak pernah ditulis ke `TEACHER_JSON` oleh `writeCheckAccount()` sama sekali | Tambah penulisan `TEACHER_JSON` di `writeCheckAccount()` (mengikuti pola penulisan `STUDENT_JSON` yang sudah ada di fungsi yang sama), bersumber dari `response.data?.teacher`. `writeLoginAccount()`'s cabang guru dipertahankan sebagai mapping defensif (untuk kalau backend suatu saat menambah field ini di login-account), tapi diberi guard `data?.teacher != null` supaya tidak diam-diam menimpa nilai yang benar dengan `0`. Field `SessionTeacher.nik`/`TeacherData.id`-only juga dibetulkan: field asli backend adalah `nip` bukan `nik`, dan `TeacherData` diperluas dengan `name`/`nip` (sebelumnya hanya `id`). Diverifikasi end-to-end di device: setelah fix, `teacher` di shared_prefs menjadi `{"id":10804,"nip":"20202620","name":"Guru DEV"}` segera setelah Home memanggil `check-account`, dan `GET .../teachers/backlog` membawa `teacher_id=10804` yang benar — List Tugas guru yang sebelumnya kosong (karena memfilter `teacher_id=0`) sekarang menampilkan seluruh tugas nyata milik guru tsb. |

## 5. Pengujian di device fisik

**Perangkat:** Infinix (Android 15), `adb install -r`, akun siswa NISN `202005` (SMK Demo
Surabaya) — sama seperti fase Materi. Akun guru NISN `20202620` (SMK Demo Surabaya) didapat
belakangan dalam fase ini (bukan di sesi implementasi awal) — lihat §5a untuk apa yang sudah
diverifikasi dengannya.

### 5a. Guru — diverifikasi di device (ditambahkan belakangan, akun NISN `20202620`)

- Login fresh (logout → masuk ulang) via NISN+password, role terdeteksi `Teacher` dengan benar.
- List Tugas: setelah perbaikan teknis #6 (§4), tab ini memuat seluruh tugas nyata milik guru
  (sebelumnya kosong/salah karena `teacher_id=0`) — dikonfirmasi `GET .../teachers/backlog`
  membawa `teacher_id=10804` yang benar, 1x panggilan saja (tidak ada fetch ganda, konsisten
  dengan perbaikan #5).
- Buat tugas baru ("Tes", mapel Antropologi) lewat form cascade Kelas→Hari→Mapel → muncul di List
  Tugas setelah fix teacher_id (sebelumnya hilang meski create sukses 200 OK, lihat #6).
- `teacherAssignmentSchedule` (perbaikan #4) — 200 OK dengan jadwal nyata, tidak lagi 422.
- Dropdown Kelas/Hari/Mapel di form (restyle `SimpleDropdown` meniru pola "Pilih Sekolah" login)
  — bottom sheet terbuka, cascade terisi benar, pemilihan berfungsi.

**Belum diuji sama sekali meski akun guru sudah tersedia** (di luar waktu sesi ini): filter
Kelas/Mapel List Tugas dengan data sungguhan, Edit/Hapus tugas, tab Penilaian, Tugas Terkumpul,
Beri Nilai, dan seluruh alur kumpulkan-jawaban sisi siswa (akun siswa uji masih belum punya tugas
yang ditugaskan kepadanya).

**Terverifikasi di device:**
1. Tile "Tugas" di Hub terbuka (tidak lagi dialog "Fitur dalam pengembangan"), masuk ke
   `TugasSiswaScreen` dengan 3 tab.
2. `adb logcat` tag `API_REQUEST`: tepat 3 panggilan (`backlog`/`done`/`scored`, masing-masing
   `take=10&skip=0`) saat layar dibuka — **tidak ada fetch ganda**, konsisten dengan
   `rules-global.md` §1.
3. Ganti tab Belum→Sudah→Nilai: tidak memicu fetch ulang (data sudah di-cache per tab).
4. Empty state benar per tab: "Belum terdapat tugas" (Belum), "Belum terdapat rekap tugas"
   (Sudah/Nilai).
5. Tidak ada crash (`adb logcat` grep `FATAL`/`AndroidRuntime` kosong) di seluruh alur yang
   terjangkau (akun ini tidak punya tugas sama sekali, jadi detail/submit tidak bisa dipicu).

**Tidak bisa diuji sama sekali di sesi ini** (akun uji tidak punya satu pun tugas, dan tidak ada
akun guru) — **kode ada, status verifikasi kosong, bukan "sebagian teruji" seperti fase Materi**:
- Seluruh isi Detail Tugas siswa (kartu syarat, seksi Soal/Jawaban/Kumpulkan Jawaban/Pembahasan,
  validasi file, submit, penghapusan baris setelah kirim).
- Dialog Nilai.
- Seluruh sisi guru (List Tugas, filter, buat/edit/hapus, Penilaian, Tugas Terkumpul, Beri Nilai).
- Bentuk JSON asli `HomeworkItem`/`HomeworkCollected`/`Assignment` — belum pernah terlihat sama
  sekali dari backend nyata (lihat `05c` §3 untuk risiko spesifik: tipe field skor terkumpul,
  `student_assignment_id`, kontrak multipart `file[]`).

## 6. Di luar cakupan fase ini

- Perpustakaan (sudah final: tidak dibangun, tidak ada titik masuk di app lama), Agenda Mingguan
  (isi fitur, bukan cuma badge/tile), Poin — fase terpisah.
- `HomeworkFilterPage` (filter tersembunyi/dead di app lama) — tidak dimigrasi, konsisten dengan
  keputusan serupa di fase Materi untuk UI mati.
- Notifikasi/deep-link ke Tugas (FCM `task`/`penilaian`, kategori `COURSE/ASSIGNMENT` vs
  `COURSE/EXAM` yang tertukar di app lama) — belum ada `NotifRouter` equivalent di app baru sama
  sekali (dikonfirmasi saat fase auth), jadi di luar cakupan sampai infrastruktur notifikasi dibangun.
- `AssignmentDay.lable` (typo asli backend, dipertahankan apa adanya — bukan "label") — tidak
  "diperbaiki" karena ini field JSON dari server, bukan salah ketik di kode kita.

## 7. Cara verifikasi lanjutan

Begitu ada akun guru dan/atau tugas nyata di akun siswa:
1. **Siswa:** minta guru membuat 1 tugas untuk kelas siswa uji → cek muncul di tab "Belum
   Dikerjakan" dengan badge benar (abu-abu "Berakhir pada" / merah "Terlambat dari" sesuai
   `is_overdue`) → buka detail → baca soal (download+buka, PDF lewat `PdfViewerScreen`) → lampirkan
   file jawaban (coba file >12MB dan ekstensi tidak didukung untuk memastikan pesan validasi persis)
   → tempel link → "Kirim Tugas" → pastikan hilang dari tab Belum dan muncul di tab Sudah.
2. **Guru:** buka "List Tugas" → filter Kelas/Mapel (pastikan hasil irisan AND, bukan salah satu
   saja, dan hanya 1x fetch per ganti filter via `adb logcat`) → buat tugas baru (cascade
   Kelas→Hari→Mapel, pastikan Mapel disabled sampai Hari dipilih) → edit tugas yang sudah ada
   (pastikan tanggal default ke sekarang, bukan label lama) → hapus salah satu (konfirmasi dialog).
3. **Guru, setelah siswa mengumpulkan jawaban:** buka "Penilaian" → pastikan tugas yang sudah
   dikumpulkan muncul dengan warna hijau/merah sesuai status skor → "Lihat" → Tugas Terkumpul →
   filter Semua/Belum/Sudah Dinilai → "Lihat Jawaban" (pastikan membuka Detail Tugas read-only,
   bukan tombol mati seperti app lama) → "Beri Nilai" → masukkan skor 0-100 → "simpan" → pastikan
   kembali ke Tugas Terkumpul dengan skor ter-update tanpa perlu refresh manual.
4. **Verifikasi kontrak API sebelum menganggap final:** capture response asli ketiga endpoint tab
   siswa dan `teachers/scored/{id}` lewat Chucker, bandingkan field-by-field dengan
   `HomeworkItem`/`AssignmentData` di `HomeworkModels.kt` — khususnya tipe
   `count_assignment_collected_*` dan keberadaan `student_assignment_id`.
