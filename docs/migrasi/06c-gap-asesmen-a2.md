# 06c — Gap Asesmen A2 (keamanan ujian) — app baru vs android-portal 2.1.41

Audit 2026-10-16, atas `06-asesmen-akm-tryout-ujian.md` §5.4–§5.6, §9–§13, §23.3. Sumber kebenaran kode lama: `git show origin/development:<path>` (`05ec898b`) — checkout `android-portal` di disk basi (cabang `akm-violation` @ `9a182d31`, tanpa `AkmMultiWindowGuard`). Path lama relatif `app/src/main/java/id/diskola/app/`.
Cakupan tetap: **siswa, ujian sekolah** (Try Out/ujian lama/AKM gov tidak dibangun). Penyelesaian per sub-fase: lihat `06d-implementasi-asesmen-a2.md`.

## 1. Yang belum ada di app baru sebelum A2

| Bagian lama | Rujukan | Status sebelum A2 | Sub-fase |
|---|---|---|---|
| Layar "Sebelum Memulai Ujian" (priming) | `AkmExamPrimingPage` | tak ada; "Mulai" langsung ke Resume | A2a |
| Gerbang mulai: password → "Pemberitahuan !!!" (teks ketat/tidak) → cek online (ketat) → priming → sesi | `AkmDetailPage.kt:430-495` | password + ketentuan tanpa baris kunci/sanksi, tanpa cek online | A2a |
| Lewati password hanya bila `password_checked` **dan** penanda sesi ≠ 1 | `AkmDetailPage:469-495` | dialog password selalu muncul | A2a |
| Mode ketat: `startLockTask`, dialog "Mode Terkunci Terlepas", checkpoint, panah back disembunyikan | `ExamLockdown.kt`, `AkmTakeResumePage` | tak ada | A2a |
| `FLAG_SECURE`, keep-screen-on, `setHideOverlayWindows` | `AkmTakeResumePage:238-324,841` | tak ada | A2a |
| Back: alert "Ujian Sedang Berlangsung" (ketat) / "Peringatan" (tidak ketat); Questions: back mati | `handleBackNavigation:767-824` | back langsung keluar | A2a |
| Penanda sesi `exam_session_active_{id}`, `not_submitted`, pemulihan proses | `AkmSettings`, `ProcessRestoreState` | tak ada | A2a/A2b |
| Setelan server (`exam_lock_mode`, `penalty_applied`, `penalty_times`), TTL 12 jam, `force` sebelum mulai | `SettingAkmCache.kt` | hanya `absence_setting` (Presensi) | A2a |
| Deteksi pelanggaran: `app_background`, `floating_window`, `split_screen`, `screenshot`, `connection_loss`, `time_tampering` | `AkmViolationDetector`, `AkmMultiWindowGuard` | tak ada | A2b |
| Antrean pelanggaran (Room, Mutex, chunk 50, 400/429/500), layanan latar `dataSync`, worker 15 menit, kirim saat submit | `AkmViolationQueue/ReportingService/Uploader` | tak ada | A2b |
| Blocker "Layar Penuh Diperlukan" | `view_akm_multiwindow_block.xml` | tak ada | A2b |
| Penalti: dialog "Penalti Ujian", pill sisa waktu, tombol tersembunyi, durasi `penalty_times` | `AkmPenaltyDialog`, `CountDownService` | pill tiruan (`ExamModeHeader.penaltyLabel`) tanpa logika | A2c |
| Laporkan Masalah (dari dalam dialog penalti), antrean `akm_problem_report` | `AkmProblemReportQueue` | tak ada | A2c |
| Pembahasan (unduh ulang paket, mode `isExplanation`) | `AkmExplanationDownloader` | layar kartu `file_path` ada; tombol tersembunyi (backend belum mengirim `has_explanation`) | ditunda |

## 2. Perilaku yang dipastikan oleh user (2026-10-19)
**Penalti dimatikan di server, pelanggaran tetap dikirim saat siswa keluar aplikasi** (repo lama). Itu sudah menjadi aturan dokumen (§10.5, §11.2 diagram): pelanggaran **selalu dicatat dan dikirim**; `penalty_applied` hanya menentukan apakah dialog/penalti ditampilkan. Server dev (`dev.api.diskola.id`, 2026-10-16) mengirim `penalty_applied:false`, `exam_lock_mode:false` — jadi `app_background` tetap harus terkirim walau mode ketat dan penalti mati (A2b: `AkmViolationTracker`, tes "…even when the penalty is off").

## 3. Bug/anti-pola kode lama yang **tidak** ditiru (§23.3 + audit)

| # | Temuan | Tindakan di app baru |
|---|---|---|
| 3 | `forceRelock` no-op bila belum pernah ter-pin + dialog tak bisa ditutup ⇒ siswa terjebak | `AkmLockdownRules.forceRelock` bekerja sebelum pin pertama; setelah 3 "Aktifkan" gagal muncul "Keluar dari ujian" |
| — | Verifikasi pin 1,8 detik tetap (mengabaikan jendela prompt 20 dtk; pin terlambat tak pernah dikenali) | berbasis event (fokus, status pin, 20 dtk) + pemantau yang menutup dialog saat pin akhirnya aktif |
| — | `arm()` dipanggil ulang tiap halaman ujian dibuat (mereset `everPinned`, jam 6 jam) | satu sesi = satu `arm` |
| 7 | Teks siswa + checkbox laporan tidak dikirim | tetap tidak dikirim (kontrak backend belum ada); disimpan di Room — A2c |
| 8 | Backoff 500 tak pernah jalan (500 = `HttpException`) | `AkmViolationRules.classify` membaca `ApiException` **dan** `HttpException`; 403/5xx jeda 30 dtk |
| — | Baris dikirim dengan `studentExamId=0` lalu dibuang oleh 400 | baris menunggu id sesi yang sebenarnya |
| — | `clear()` antrean di luar Mutex | jeda dan flush satu Mutex |
| — | Layanan latar `START_STICKY` tanpa extras = notifikasi tanpa pemilik; `startForeground` tanpa try/catch; tanpa `onTimeout` (API 35) | `START_NOT_STICKY`, extras kosong ⇒ `stopSelf`, guard, `onTimeout`; dimulai hanya dari UI foreground |
| — | Receiver jam: baseline diambil saat konstruksi; `deviceTimeBefore` = jam event sebelumnya | baseline saat sesi mulai; `deviceTimeBefore` = jam yang *seharusnya* |
| — | `connection_loss` terikat lifecycle Activity (outage saat di latar hilang) | dipantau sepanjang sesi |
| 10 | Penalti dihitung dari `HH:mm:ss` (salah lewat tengah malam), reset tiap halaman dibuat ulang | epoch akhir tersimpan — A2c |
| 11 | `startActivity` berantai, instance basi | satu Activity + nav graph; `ExamScreenGuard` untuk layar tanpa sesi |
| 14 | `onBackPressed()` usang (predictive back di Android 16) | `BackHandler` Compose |
| 20 | Worker laporan periodik abadi; antrean berhenti setelah "Ya, keluar" | worker pelanggaran membatalkan diri hanya bila antrean kosong **dan** sesi tak aktif — A2b/A2c |
| 17/12/19 | Handler jawaban hidup di mode pembahasan; unduh ulang paket mereset SCORED→NEW | pembahasan ditunda; layar kartu yang ada dipertahankan |

## 4. Keputusan yang diambil di rencana A2 (ubah bila tidak setuju)
Satu Activity + `ExamSessionController` singleton (bukan `ExamActivity`); penalti tidak ditahan dialog milik app (`isModalVisible` dibuang); judul tetap "Kerjakan Asesmen"; payload laporan = legacy persis; pembahasan ditunda; screenshot hanya API 34+; rotasi bukan `app_background`; prefs `akm_exam_state` dikecualikan dari backup. Detail + alasan: `FLOW_QUESTIONS.md` 2026-10-16 (A2).

## 5. Celah yang diketahui dan belum ditutup
- **Media soal membuka aplikasi lain** (`QuestionMediaList`, `ACTION_VIEW`): di app lama audio/video diputar di dalam halaman. Selama sesi aktif, memutar media = keluar aplikasi ⇒ tercatat `app_background` (dan, mode ketat, pin menahannya). Perlu pemutar dalam-app (Media3) — belum dibangun; ujian uji tidak memuat media.
- `penalty_applied` yang **hilang** dari JSON dibaca *mati* (default DTO `false`, persis legacy); `null` eksplisit dibaca *aktif*. Server dev selalu mengirimnya.
