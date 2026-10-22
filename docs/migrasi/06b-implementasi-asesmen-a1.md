# 06b — Implementasi Asesmen A1 ("data aman")

Dikerjakan 2026-10-15. Spesifikasi: `06` §5–§8, §15, §17. Gap: `06a-gap-asesmen.md`. Path relatif `app/src/main/java/id/diskola/app/`.
Prinsip: **semua yang penting ada di Room** — proses mati, layar ditutup, atau jaringan putus tidak boleh menghilangkan jawaban.

## 1. Arsitektur
`UI → ViewModel → AkmRepository → Retrofit / Room / WorkManager`.

| Berkas | Tanggung jawab |
|---|---|
| `utils/AkmRules.kt` (+ `AkmRulesTest`, 26 tes) | Murni: predikat blokir logout `isPending`, presedensi status (`serverStatus`/`localStatus`/`effectiveStatus`), klasifikasi gagal (`Retry`/`AuthExpired`/`Permanent`), `deadlineAction`, `parseDateTime` (null bila tak terbaca), `isTimeUp`, cooldown, `canReupload`, `cleanEssay`, `allAnswered`. |
| `dataclass/akm/AkmAnswerCodec.kt` (+ tes 20) | `AkmPlayerAnswer` **berbasis id jawaban** ⇄ JSON Room; `AkmPayload.build` (payload §7.2); susunan Menjodohkan (`arranged`/`swapped`/`toArrangement`). |
| `dataclass/akm/AkmShuffle.kt` (+ tes 5) | Acak sekali saat unduh (soal/opsi bila `isRandom`; sisi kanan PAIR selalu). |
| `dataclass/localDb/` `AkmAnswerStore`, `AkmSubmissionStore`, `AkmScheduleCache`, `AkmSyncedExam` (+`studentExamId`, `endAtMillis`), `AkmSyncDao` | Tabel `akm_answer`, `akm_submission` (QUEUED/SENT/REJECTED), `akm_schedule_cache`; kueri ringan `observeStates` (tanpa memuat paket); `countPending` = cermin `AkmRules.isPending`. |
| `database/Migrations.kt` (+ `AkmMigrationTest`) | `Migration(7,8)` eksplisit (hanya menambah; data unduhan aman; baris lama `SUBMITTED` dipindah ke `akm_submission`). Skema diekspor ke `app/schemas/…/8.json`; tes membandingkan DDL migrasi dengan DDL Room. |
| `repository/AkmRepository.kt` | Daftar (server → cache; gagal → cache + ujian pending), rekonsiliasi dengan server, jawaban (`saveAnswer`, `saveAnswerDetached`), `submit`/`queueNow`/`reupload`, `post` (satu-satunya pemanggil `…/answer/{student_exam_id}`), cooldown, `hasPending`, `scheduleDeadlines`, `syncedExam` (menyalin sesi dari paket lama). |
| `worker/AkmSubmitWorker.kt`, `AkmWork.kt`, `AkmNotifications.kt` | Dua unique work per ujian (`akm_submit_{id}`, `akm_deadline_{id}`, KEEP); retry ≤ 10, 401 tak menghabiskan percobaan; setelah terkirim menjalankan logout tertunda. |
| `worker/AkmDownloadWorker.kt` | Tak mengunduh ulang paket yang sudah ada; acak sekali; menyimpan `studentExamId`/`endAtMillis`; menjadwalkan deadline; `CancellationException` dilempar ulang. |
| `viewmodel/AkmListViewModel`, `AkmResumeViewModel`, `AkmQuestionViewModel`, `AkmExamSupport` | `AkmViewModel` (506 baris) dipecah. Resume/Question mengambil argumen dari route (`SavedStateHandle.toRoute`) dan membaca Room. |
| `utils/session/SessionManager.kt`, `SessionEvents.kt`, `ui/navigation/AppNavHost.kt`, `App.kt` | Satu `hasPendingExam()`; `logoutInBackground` yang terblokir → `deferLogout` + event + dialog; 401 yang terblokir tidak lagi mati; `scheduleDeadlines()` saat start. |
| `di/module/MoshiModule.kt` | `ObjectToListAdapter.toJson` kini menulis daftar (sebelumnya kosong → JSON tak valid saat paket di-cache). |

## 2. Keputusan (user) — `FLOW_QUESTIONS.md` 2026-10-15
Blokir logout **mutlak**; logout FCM/401 yang terblokir **ditunda lalu dijalankan otomatis**; Try Out/ujian lama tidak dibangun; Asesmen dipecah A1/A2.

## 3. Temuan teknis
| # | Temuan | Penanganan |
|---|---|---|
| 1 | `ObjectToListAdapter.toJson` kosong → menyimpan jadwal (yang ber-`exams`) ke cache melempar | Diperbaiki + tes round trip. |
| 2 | `isDone` hanya berarti jendela ujian ditutup; menutup antrean lokal karenanya akan menghilangkan jawaban | `serverAlreadyHasAnswers` hanya `isQueued`/dinilai; `isDone` + jawaban belum terkirim tampil "Proses upload jawaban". |
| 3 | Indeks pilihan terhadap urutan tampil tidak stabil | Jawaban berbasis id; urutan acak disimpan sekali di paket. |
| 4 | Worker memanggil `markSent` yang membatalkan unique work miliknya sendiri | `markSent(examId, runningWork)` hanya membatalkan pekerjaan *lain*. |
| 5 | Baris unduhan lama tak punya `studentExamId`/`endAtMillis` (hanya di blob paket) | `syncedExam()`/`repairSessions()` menyalinnya sekali; `hasPending()` memanggilnya dulu. |
| 6 | `exam_lock_mode` default `true` bertentangan dengan keputusan 30-09 | Default `false`; nilai `null` eksplisit dibiarkan untuk A2 menafsirkan. |
| 7 | `gradlew -q … | grep` menyembunyikan kegagalan tes dan hasil XML basi | Tes selalu dijalankan tanpa `-q` dan dihitung dari XML yang baru. |

## 4. Pengujian
- JVM: 186 tes lolos (`testDebugUnitTest`), termasuk baru: `AkmRulesTest` 26, `AkmAnswerCodecTest` 20, `AkmShuffleTest` 5, `AkmModelsParseTest` 9, `AkmMigrationTest` 5, dan 55 tes Pembayaran P1 (`LogRedactorTest`, `PaymentRulesTest`, `SecurityDialogRulesTest`, `KlaspayModelsParseTest`).
- `:app:assembleDebug` bersih; APK terpasang ke HP uji (Samsung SM-A505F, akun siswa "RIZKY", mode gelap).
- **Diuji di device (2026-10-15):** update dari build lama membuka database tanpa crash — `user_version=8`, kolom baru ada, `akm_schedule_cache` terisi (1 LIST + 66 SCORED); daftar Asesmen memanggil `exam-schedules` dan `exam-schedules-scored` sekali; kartu "TEST SOAL ASESMEN" berstatus "Sesi Terlewat" (kata server); detail menampilkan bar "Sesi Terlewat" (bukan "dikumpulkan"); tab Nilai memuat 66+ baris tanpa tombol Upload Ulang untuk "Tidak Mengerjakan/Sesi Terlewat". Sekaligus tab Pembayaran P1: satu `payment/wallet`, saldo/ID wallet nyata, info wallet, menu per peran.
- **Belum diuji (butuh ujian uji yang bisa dimulai + izin, karena unduh membuat `student_exam` di server dan kumpulkan menjadi nilai sungguhan):** unduh → jawab → kumpulkan; antrean offline + auto-submit; proses mati saat mengerjakan; FCM `"logout"`/401 tertahan; upload ulang + cooldown. Uji offline membutuhkan mengubah pengaturan sistem HP (mode pesawat) — minta izin dulu.

## 5. Penyesuaian dengan dokumen 06 v2.1.41 (2026-10-16)
Dokumen 06 diperbarui ke android-portal **2.1.41** (commit `05ec898b`). Dua perubahan legacy baru ikut diterapkan ke A1:

| Perubahan legacy | Penerapan |
|---|---|
| Label/stempel jadwal dibatalkan (§4.4; `watermark_bool: Boolean?`, `watermark_label`) — murni visual | `AkmScheduleData` + dua field (nullable); `AkmRules.AkmWatermark`/`watermark()` (non-null ikut server, `false` memulihkan, `null` mempertahankan nilai lokal, `true` + label kosong → "DIBATALKAN"); `AkmRepository.store()` menyalin tanda yang sudah dikenal ke jadwal tanpa key (daftar → nilai, setelah restart; tanpa migrasi Room) dan `watermarkOf(examId)`; komponen `ui/components/AkmWatermark.kt` (`WatermarkLabel`, `AkmStamp`/`StampedIf`, `AkmWatermarkOverlay`, `@Preview` terang+gelap); kartu daftar, Detail (baris "Status" + stempel), Hasil (`AkmParticipantCard` + stempel), halaman soal (overlay diagonal tanpa menelan sentuhan). |
| Input esai (§5.7) | `AkmRules.sanitizeEssayInput` (enter tunggal dibuang, tempelan diratakan dengan spasi), `AkmRules.usesDoneAction` (esai & satu kata "Selesai"; angka keyboard angka), `AkmRules.ESSAY_MIN_LINES = 8`; `AppTextField` + `imeAction`, `onImeAction`, `followCursor` (rect kursor lewat `onTextLayout` → `BringIntoViewRequester`); `AkmQuestionViewModel.setEssay` menyaring sebelum `cleanEssay`. |
| Label baris detail "Peserta" | `akmParticipantRows` (ID Ujian, Status, Peserta, NIS, Kelas, Kategori, Periode, Sekolah). |

**Deviasi (keputusan user):** `AkmResumeScreen` menampilkan banner "Jadwal ini ditandai …" untuk jadwal berwatermark, padahal app lama tidak menandainya di layar itu (celah yang dicatat doc 06 §4.4). Layar "Sebelum Memulai Ujian" (A2) akan ikut menandai.

Pengujian: 197 tes JVM lulus (`AkmRulesTest` 34, `AkmModelsParseTest` 12 dst.), `:app:assembleDebug` bersih. **Device: belum diuji** — HP uji tidak terhubung saat penyesuaian ini selesai; tampilan label/stempel/watermark/banner butuh jadwal yang ditandai server, dan esai (min 8 baris, "Selesai", enter dibuang, scroll kursor) butuh ujian uji yang bisa dimulai.

