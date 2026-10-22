# 06d — Implementasi Asesmen A2 (keamanan ujian)

Dikerjakan 2026-10-16 → 19. Spesifikasi: `06` §5.4–§5.6, §9–§13. Gap: `06c-gap-asesmen-a2.md`. Path relatif `app/src/main/java/id/diskola/app/`.
Rencana: `A2a` sesi + kunci layar + priming + gerbang, `A2b` deteksi pelanggaran + antrean + layanan latar, `A2c` penalti + Laporkan Masalah, `A2d` pembahasan (ditunda).
Status: **A2a, A2b dan A2c selesai (kode + tes JVM)**; A2d ditunda. Belum teruji penuh di perangkat — lihat bagian 5.

## 1. A2a — sesi, kunci layar, priming, gerbang mulai

| Berkas | Tanggung jawab |
|---|---|
| `utils/AkmLockdownRules.kt` (+ `AkmLockdownRulesTest`, 26) | Mesin status pin murni (jam dan status-pin disuntik): `requestLock`/`ensureLocked`/`retryLock`/`forceRelock`, `onFocusChanged`, `evaluate` (event-driven), `hasActiveBreach`, `escapeAvailable`, jeda keluar sah, `release`, watchdog 6 jam. Angka legacy dipertahankan (2 percobaan sebelum pin pertama, prompt 20 dtk, keluar sah 10 dtk, 3×300 ms retry "not in foreground"). |
| `utils/AkmSessionRules.kt` (+ `AkmSessionRulesTest`, 15) | Urutan gerbang, `skipPassword`, `mayStart` (mode ketat butuh internet tervalidasi), `EndReason` (hanya Submit/TimeUp/AlreadyUploaded membersihkan penanda), `isUnclosed`, semua teks persis legacy: ketentuan ketat/tidak, "Butuh Koneksi Internet", priming, alert back, "Mode Terkunci Terlepas". |
| `utils/AkmRules.kt` (+ tes) | `isStrict`, `isPenaltyApplied`, `penaltySeconds` (≤0 ⇒ 300), `lockModeRaw`/`penaltyAppliedRaw`, `settingsStale` (12 jam, usia negatif basi). `AkmSettingData.penalty_applied` → `Boolean?`. |
| `utils/AkmExamState.kt` | Prefs `akm_exam_state` (`commit()`), **dikecualikan dari backup** (`backup_rules.xml`, `data_extraction_rules.xml`): `exam_session_active_{id}`, `password_checked_{id}`; dibersihkan saat logout. |
| `repository/AkmSettingsRepository.kt` | `GET mobile/setting-akm`, TTL 12 jam, `refresh(force)` dengan batas 4 dtk; gagal ⇒ nilai lama; −1 (belum pernah) ⇒ aktif. Presensi tetap memakai salinannya sendiri. |
| `exam/ExamSessionController.kt` | Singleton sesi: `begin`/`end`/`abort`, penanda sesi, `FLAG_SECURE` + `filterTouchesWhenObscured`, keep-screen-on + `setHideOverlayWindows` (API 31+) saat resume, pin via `startLockTask` (coroutine, retry 3×), `checkpoint`, dialog pemulihan, `shouldBlockTouch`, `onStaleExamScreen`. |
| `ui/MainActivity.kt` | `attach/detach`, `onResume/onPause/onWindowFocusChanged/dispatchTouchEvent` diteruskan ke controller; izin notifikasi tidak diminta saat sesi aktif; `LocalExamSession`. |
| `ui/screens/akm/AkmPrimingScreen.kt`, `viewmodel/AkmPrimingViewModel.kt`, `Route.Akm.Priming` | "Sebelum Memulai Ujian": 5 baris pelanggaran, 2 informasi, catatan kaki, "Semua catatan ini akan dikirim ke pihak sekolah.", callout kunci hanya bila ketat, banner "Dibatalkan" (keputusan user 2026-10-16), tombol "Mulai Ujian" ⇒ `begin()` ⇒ Resume (`popUpTo Priming`). `@Preview` terang + gelap. |
| `ui/screens/akm/AkmDetailScreen.kt`, `AkmStartDialogs.kt`, `viewmodel/AkmListViewModel.kt` | Gerbang: password (dilewati bila sah) → ketentuan (setelan di-`force`-refresh dulu) → cek online (ketat) → Priming. Tombol "Menyiapkan…" selama refresh. |
| `ui/screens/akm/AkmResumeScreen.kt`, `AkmQuestionScreen.kt`, `ExamSessionOverlays.kt` | Back: ketat ⇒ "Ujian Sedang Berlangsung" ("Mengerti"), tidak ketat ⇒ "Peringatan" ("Tetap mengerjakan"/"Ya, keluar" ⇒ `abort`); panah back tidak digambar saat ketat; Soal: back keras tidak melakukan apa-apa (legacy); checkpoint di Kumpulkan/buka instruksi/pilih-berikutnya-sebelumnya; sesi berakhir otomatis begitu jawaban dikumpulkan/antre (`EndSessionWhenHandedIn`). |
| `ui/navigation/MainTabsScreen.kt` | Route Priming; `ExamScreenGuard` (layar ujian tanpa sesi hidup ⇒ password diminta ulang + kembali ke Detail); event "Keluar dari ujian"; `ExamSessionOverlays`. |
| `repository/AkmRepository.kt` | Penanda sesi dibersihkan saat jawaban diserahkan (`submit` offline/online, `queueNow`, `markSent`, rekonsiliasi). |

## 2. A2b — deteksi pelanggaran, antrean, layanan latar

| Berkas | Tanggung jawab |
|---|---|
| `utils/AkmViolationRules.kt` (+ tes 25) | Jenis, ambang (5 dtk koneksi/jam/sentuhan), `triggersPenalty`, deskripsi + pesan penalti, aturan penahan, payload (`detectedAt` ISO offset, metadata angka tetap angka), `chunks` 50, `classify` (400 buang, 429 `Retry-After`/60, 403/5xx 30 dtk; `ApiException` **dan** `HttpException`), `isOemFloatingConfig`, `isWindowShrunk` (toleransi 4 %). |
| `utils/AkmViolationTracker.kt` (+ tes 19) | Memori pengamat, tanpa tipe Android: `app_background` (rotasi bukan keluar; jeda sistem tidak dihitung), `floating_window` (debounce 5 dtk), `split_screen` (1 baris per episode), `connection_loss` (≥ 5 dtk; mulai offline tak dihitung), `time_tampering` (`deviceTimeBefore` = jam yang seharusnya). |
| `exam/ExamViolationDetector.kt` | Sambungan Android: `NetworkCallback` (VALIDATED, main handler), receiver `ACTION_TIME_CHANGED` (`RECEIVER_NOT_EXPORTED`), `ScreenCaptureCallback` API 34 (objek bersarang agar API lama tak memuat tipenya), `ExamWindowCheck` (multi-window/PiP/OEM/shrunk API 30+). |
| `dataclass/localDb/AkmViolationStore.kt`, `AkmProblemReportStore.kt`, `database/Migrations.kt` | Room **v9**: `akm_violation`, `akm_problem_report` (+ indeks). `MIGRATION_8_9` terdaftar di `DbModule`; `9.json` diekspor; `AkmMigrationTest` (9 tes) membandingkan DDL migrasi dengan DDL Room untuk tabel **dan indeks** dan memastikan tabel v8 tak berubah. |
| `repository/AkmViolationQueue.kt` | `record` (selalu tersimpan lebih dulu), `flush` (Mutex tunggal, cek `isInternetAvailable` seperti legacy, jeda per ujian, baris tak dikirim dengan id sesi 0), `flushAll`, `clearPause`. |
| `exam/AkmViolationReportingService.kt` | FGS `dataSync`, flush 30–60 dtk acak + saat jaringan kembali; `START_NOT_STICKY`; extras kosong ⇒ `stopSelf`; `onTimeout`; kanal senyap yang sudah ada. |
| `worker/AkmViolationUploadWorker.kt`, `AkmWork` | Periodik 15 mnt (`exam_violation_uploader_{id}`, KEEP, CONNECTED, ≤ 10 percobaan); membatalkan diri bila antrean kosong dan sesi tak aktif; dijadwalkan ulang di `App.onCreate` bila ada baris. |
| `exam/ExamSessionController.kt` | Memasang semua di atas: `begin` ⇒ detektor, layanan, worker, `not_submitted` bila penanda lama masih menyala; `end` ⇒ berhenti + satu flush; `ExamViolation` dipancarkan untuk lapisan penalti (A2c); blocker "Layar Penuh Diperlukan" (`Dialog` penuh, tak bisa ditutup) di `ExamSessionOverlays`. |
| `repository/AkmRepository.kt`, `ui/MainActivity.kt` | Flush pelanggaran setelah jawaban terkirim (ditunggu di worker, latar saat submit manual); `dispatchTouchEvent` meneruskan sentuhan tertutup. |

Manifest: `ACCESS_NETWORK_STATE`, `HIDE_OVERLAY_WINDOWS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `DETECT_SCREEN_CAPTURE`, `<service … foregroundServiceType="dataSync" exported="false">`.

## 3. A2c — penalti dan Laporkan Masalah

| Berkas | Tanggung jawab |
|---|---|
| `utils/AkmPenaltyRules.kt` (+ `AkmPenaltyRulesTest`, 13) | `decide` (`None`/`Start`/`UpdateCause`: penalti mati ⇒ hanya dicatat; sedang berjalan ⇒ hanya mengganti sebab; sistem penyebabnya ⇒ tak ada penalti — **tidak** ditahan dialog milik app), `endAt`, `remainingMillis` (dari **epoch akhir**, dibatasi ≤ durasi, aman lewat tengah malam dan jam mundur), `pillLabel` `m:ss`, teks dialog/footer `v{versi} • Android {rilis} (API {sdk})`. |
| `utils/AkmReportRules.kt` (+ `AkmReportRulesTest`, 11) | Form: maks 500 + counter, min 3 baris, tombol aktif bila teks + checkbox + cooldown 60 dtk habis, label "Tunggu {n} detik lagi"; `buildLog` (§13 langkah 1, maks 200.000); `payload` = empat field legacy persis, dipotong per batas, `logcat` lewat `LogRedactor`. |
| `utils/AkmExamState.kt` | `penalty_end_{id}` + `penalty_total_{id}` (bertahan setelah proses mati), `problem_report_last_submit_at` (cooldown global), `markHandedIn` (penanda sesi + penalti dibersihkan saat jawaban diserahkan). |
| `exam/ExamSessionController.kt` | `applyPenaltyDecision` dari tiap pelanggaran (pengaturan dibaca ulang), `startPenalty` (durasi `penalty_times`, ≤0 ⇒ 300), pengatur waktu 500 ms dari epoch tersimpan, `resumeStoredPenalty` saat sesi dibuka (lanjut, **tidak** diulang), `submitProblemReport`. "Ya, keluar" tidak menyimpan penalti terpisah: penanda sesi yang tersisa ⇒ `not_submitted` pada sesi berikut ⇒ penalti lewat jalur pelanggaran yang sama. |
| `ui/screens/akm/ExamPenaltyDialog.kt`, `ExamSessionOverlays.kt` | Satu dialog non-cancelable "Penalti Ujian" (kartu sebab bila ada, "Kamu tidak dapat melanjutkan ujian sampai waktu penalti habis.", **"Sisa waktu m:ss"** — tambahan, header yang memuat pill tertutup dialog —, tautan "Laporkan Masalah", footer versi); "Laporkan Masalah" menukar isi dialog menjadi form (bukan dialog baru). Toast "Laporan tersimpan di perangkatmu dan akan dikirim otomatis saat tersedia." selalu. `@Preview` terang/gelap. |
| `ui/screens/akm/AkmResumeScreen.kt`, `AkmQuestionScreen.kt` | Selama penalti: judul "Penalti Ujian" (Resume) / "Penalti" (Soal), pill di header, daftar + Kumpulkan / soal + Berikutnya + tombol grid disembunyikan, panah back Resume tidak digambar. Judul setelah penalti tetap "Kerjakan Asesmen". |
| `repository/AkmProblemReportQueue.kt`, `worker/AkmProblemReportWorker.kt`, `AkmWork.enqueueProblemReportUpload` | `file` (simpan ke `akm_problem_report` dengan pelanggaran sesi + info perangkat; teks siswa/checkbox/violations hanya disimpan, **tidak dikirim**), `flush` (Mutex, jeda global, `POST …/error-reports/{student_exam_id}` objek 4 field; sukses/400 ⇒ `isSent`; 429/403/5xx ⇒ jeda; lain ⇒ coba lagi), pekerja satu kali unik (bukan periodik abadi), dijadwalkan ulang di `App.onCreate` bila ada yang belum terkirim. |

## 4. Pengujian (JVM)
`testDebugUnitTest` dari root, tanpa `-q`, dihitung dari XML baru: **316 tes, 0 gagal** setelah A2c (A1: 197; +26 `AkmLockdownRulesTest`, +15 `AkmSessionRulesTest`, +5 `AkmRulesTest`, +1 `AkmModelsParseTest`, +25 `AkmViolationRulesTest`, +19 `AkmViolationTrackerTest`, +4 `AkmMigrationTest`, +13 `AkmPenaltyRulesTest`, +11 `AkmReportRulesTest`). `:app:assembleDebug` bersih.

## 5. Diuji di perangkat dan yang belum
Diuji (Infinix X1102, Android 15, 2026-10-16, akun siswa "HANAFI", dengan patch uji sementara yang **sudah dicabut**): gerbang → "Pemberitahuan !!!" (teks tidak ketat dan ketat), Priming (tanpa/dengan callout kunci), sesi mulai, layar ujian hitam di `screencap` (**`FLAG_SECURE` aktif**), Back ⇒ "Peringatan" ⇒ "Tetap mengerjakan" menutup / "Ya, keluar" kembali ke Detail dan penanda `exam_session_active_312873=true` tetap tertulis; mode ketat (dipaksa lokal): prompt pin sistem muncul < 1 dtk. Temuan: dialog "Mode Terkunci Terlepas" ikut tampil di atas prompt (keputusan "ditolak" jatuh sebelum ada perubahan fokus) dan tidak hilang saat pin berhasil — **diperbaiki** dengan pemantau pin (`watchRecovery`), perbaikan itu belum dicoba ulang di perangkat.

**Belum teruji (perlu perangkat + ujian yang bisa dimulai):**
- Pin sampai berhasil, lepas pin ⇒ dialog + sentuhan ditahan, "Aktifkan" ×3 ⇒ "Keluar dari ujian".
- Penalti nyata (dialog, hitung mundur, lanjut setelah proses mati, hilang saat jawaban diserahkan) dan form Laporkan Masalah (cooldown, antrean, kirim) — server dev mengirim `penalty_applied:false`, jadi penalti hanya bisa dicoba dengan memaksa setelan lokal.
- Semua pelanggaran nyata: keluar ke Home (`app_background`), split screen/jendela mengambang (blocker + 1 baris), overlay (`floating_window`), screenshot (Android 14+), matikan data ≥ 5 dtk, ubah jam, kill dari Recents ⇒ `not_submitted`, `adb shell am kill` ⇒ layar ujian menutup + password diminta ulang.
- Pengiriman nyata ke `…/exam-school/{id}/violations/{studentExam}` (kontrak belum pernah dipanggil dari app baru) dan perilaku FGS (izin notifikasi ditolak, batas waktu API 35).
- Migrasi Room 8→9 pada database nyata (tes JVM membuktikan DDL sama; jalur upgrade sebenarnya belum dijalankan karena perangkat terlepas).
- Ujian uji di server berstatus "Tidak Mengerjakan" (tombol Mulai tak muncul secara normal) dan server dev mengirim `exam_lock_mode:false`, `penalty_applied:false`.
