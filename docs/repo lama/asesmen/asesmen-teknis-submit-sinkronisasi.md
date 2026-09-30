# Asesmen (AKM) — Arsitektur Teknis: Mengerjakan Ujian, Sinkronisasi Soal & Pengumpulan Jawaban

> Dokumen ini fokus pada **mekanisme kerja** (bukan UI) alur: menjawab soal → menyimpan lokal →
> submit ("Kumpulkan") → antre/retry upload → status hasil. Pemetaan UI-ke-field data sudah ada di
> `docs/features/asesmen-akm-pengerjaan-soal.md` — TIDAK diulang di sini kecuali sebagai rujukan
> silang yang perlu untuk memahami mekanisme.
>
> Ditulis untuk jadi referensi migrasi ke repo baru (Hilt + Jetpack Compose + Room).

---

## 1. Ringkasan alur end-to-end

```
Siswa memilih/mengetik jawaban
        │
        ▼
Room (akm_question.answered / answer_essay, akm_answer.selected / selected_id)   ◄── SELALU, online maupun offline
        │
        │  (tidak ada panggilan API di titik ini — lihat §2)
        ▼
Siswa tekan "Kumpulkan"  ──────────────► AkmViewModel.uploadAnswer(akmId)
        │                                        │
        │                              ┌─────────┴─────────┐
        │                        online (isInternetAvailable)   offline
        │                              │                         │
        │                     POST langsung (synchronous)   enqueue WorkManager
        │                     dari coroutine ViewModel      AkmUploader (manual_submit=true)
        │                              │                         │
        │                     sukses → UPLOADED          worker jalan saat ada network
        │                     gagal  → propagate error    → sukses: UPLOADED
        │                                                  → gagal: retry hingga 10x (LINEAR)
        ▼
Selain itu: SETIAP kali jadwal ujian disinkronkan dari server (list/detail), sebuah
WorkManager job unik "exam_uploader_<id>" DIJADWALKAN dengan initialDelay = (date_end - now),
constraint NETWORK CONNECTED — ini jaring pengaman force-submit otomatis saat waktu ujian habis,
independen dari apakah siswa menekan "Kumpulkan" atau tidak.
```

---

## 2. "Online" vs "Offline" mengerjakan soal — TIDAK ADA BEDA logic, cuma beda kapan terkirim

Ini poin paling penting untuk diluruskan karena istilah "ujian online/offline" di kode lama **tidak
merujuk ke dua jalur kode yang berbeda saat mengerjakan soal**.

**Fakta dari kode:** Setiap aksi menjawab soal (`answerEssay`, `answerChoice`, `answerPair` di
`AkmQuestionsPage.kt:841-884`) **HANYA menulis ke Room** lewat `AkmDao`, terlepas dari status
konektivitas. Tidak ada pemeriksaan `isInternetAvailable()`/`isInternetValidated()` di jalur ini, dan
tidak ada panggilan `ApiService` sama sekali saat siswa menjawab per-soal.

```kotlin
// AkmQuestionsPage.kt:841-853 — jawaban esai
private fun answerEssay(answer: String, item: QuestionAnswers) {
    lifecycleScope.launch {
        viewmodel.db.akm()
            .insertQuestion(item.question.copy(answer_essay = answer, answered = true))
        viewmodel.db.akm().setInstAnswered(instructionId)
    }
}

// AkmQuestionsPage.kt:855-874 — pilihan ganda (single/multi correct)
private fun answerChoice(item: AkmAnswerTable) {
    lifecycleScope.launch {
        viewmodel.db.withTransaction {
            viewmodel.db.akm().setAnswered(item.question_id)
            viewmodel.db.akm().setInstAnswered(instructionId)
            if (!multipleCorrects.contains(viewmodel.db.akm().getQuestionType(item.question_id)))
                viewmodel.db.akm().unselectAnswers(item.question_id)
            viewmodel.db.akm().insertAnswer(item)
        }
    }
}

// AkmQuestionsPage.kt:876-884 — pasangan (drag-drop)
private fun answerPair(answers: List<AkmAnswerTable>) {
    lifecycleScope.launch {
        viewmodel.db.withTransaction {
            viewmodel.db.akm().setAnswered(answers.first().question_id)
            viewmodel.db.akm().setInstAnswered(instructionId)
            viewmodel.db.akm().insertAnswers(answers)
        }
    }
}
```

Ketiganya berjalan di `lifecycleScope` (bukan `viewModelScope` — catatan tersendiri: logic ini
menempel di Activity, bukan ViewModel, pelanggaran terhadap aturan layering `docs/rules-global.md`
poin 6 — **di repo baru ini harus pindah ke ViewModel/Repository**).

**Kesimpulan:** "mengerjakan offline" bukan mode terpisah dengan kode berbeda. **Semua pengerjaan
soal SELALU offline-first ke Room** — apakah perangkat online atau tidak sama sekali tidak
memengaruhi bagaimana jawaban disimpan selama proses mengerjakan. Perbedaan online/offline hanya
muncul **satu kali**, yaitu di titik submit (`uploadAnswer`, lihat §3) — itu pun bukan dua jalur kode
yang beda strukturnya, melainkan satu percabangan `if/else` yang menentukan apakah API dipanggil
langsung sekarang atau diserahkan ke WorkManager untuk nanti.

**Tidak ada autosave online selama mengerjakan.** Tidak ditemukan pemanggilan endpoint apa pun (grep
`api\.` di `AkmQuestionsPage.kt`) yang mengirim jawaban parsial ke server sebelum tombol "Kumpulkan"
ditekan. Satu-satunya baris terkait koneksi selama mengerjakan adalah pengecekan retry video offline
(`AkmQuestionsPage.kt:573`, `AkmQuestionMediaBinder.retryOfflineVideos()`) — itu untuk **media soal**,
bukan jawaban.

**Implikasi untuk repo baru:** jangan desain dua code path terpisah "OnlineExamMode" vs
"OfflineExamMode". Cukup satu jalur: setiap event jawab soal → `Repository.saveAnswerLocally(...)` →
tulis Room (source of truth tunggal) → Compose mengamati Room via Flow. Titik cabang online/offline
hanya ada di `Repository.submitAnswers()` / lapisan sinkronisasi, bukan di alur menjawab.

---

## 3. Alur submit ("Kumpulkan")

### 3.1 Trigger dan validasi sebelum kirim

Tombol "Kumpulkan" ada di `AkmTakeResumePage.kt` (`btnAction.setOnClickListener`,
`AkmTakeResumePage.kt:661-761`). Sebelum submit dieksekusi:

1. `enforceLockdownAtCheckpoint("kumpulkan")` — cek mode lockdown/screen-pinning aktif, bukan
   validasi jawaban.
2. **Validasi kelengkapan jawaban BERBEDA per jenis ujian, dan untuk SCHOOL sebenarnya TIDAK ADA**:
   - `ExamType.SCHOOL` (`AkmTakeResumePage.kt:665-687`): langsung panggil
     `viewmodel.uploadAnswer(akmId)` tanpa mengecek apakah semua soal sudah terjawab sama sekali.
     Tidak ada dialog konfirmasi "masih ada soal belum dijawab" untuk jalur ini.
   - `ExamType.TRYOUT` (`AkmTakeResumePage.kt:688-757`): untuk setiap instruksi, cek
     `answerData.answered < answerData.num_question`. Kalau ada yang belum lengkap → dialog
     peringatan "Terdapat soal yang belum terselesaikan..." dengan opsi lanjut mengerjakan atau tetap
     kumpulkan. Kalau semua lengkap → dialog info "Semua soal telah terjawab..." lalu kumpulkan.
     **Catatan:** validasi ini hanya gerbang **dialog konfirmasi UI**, bukan gerbang keras — siswa
     tetap bisa memilih "Yakin dan kumpulkan" meski belum semua terjawab.

Ini bug/inkonsistensi nyata di kode lama yang perlu diputuskan sadar di repo baru: apakah validasi
"semua terjawab" mau diseragamkan untuk semua jenis ujian atau memang sengaja beda per jenis.

### 3.2 Pembangunan payload dari Room

Payload dibangun oleh `AkmAnswerPayload.build()` (`AkmAnswerPayload.kt:25-46`), dipakai BERSAMA oleh
jalur langsung (`AkmViewModel.uploadAnswer`) maupun jalur worker (`AkmUploader`) — ini disengaja
(lihat komentar `AkmAnswerPayload.kt:6-13`) untuk mencegah dua jalur mengirim payload yang berbeda.

Poin teknis penting:
- **Hanya soal yang `answered == true` yang dikirim** — `.filter { it.answered }` di
  `AkmAnswerPayload.kt:31`. Soal yang belum dijawab sama sekali TIDAK muncul di payload (bukan
  dikirim sebagai kosong).
- Bentuk `answer` per `type_label` — sudah didokumentasikan detail di
  `asesmen-akm-pengerjaan-soal.md`, poin krusial untuk migrasi: normalisasi `PAIR` bergambar.
  `selected_id` hasil drag-drop siswa bisa menyimpang dari gambar yang benar-benar tampil (root
  cause tidak dijelaskan di kode, kemungkinan reuse Bitmap/View recycling). Sumber kebenaran yang
  dipakai untuk menimpa `selected_id` adalah **nama file** `second_file_path`, dicocokkan dengan
  regex `a\d+_(\d+)_2\.jpg$` (`AkmAnswerPayload.kt:17,79-115`). Kalau ID hasil ekstraksi nama file
  berbeda dari `selected_id` tersimpan, nama file yang menang — dan dicatat log peringatan
  (`Timber.tag(logTag).w(...)`, `AkmAnswerPayload.kt:96-100`).
- Struktur payload akhir adalah `List<Any>` (bukan data class bertipe kuat!) — tiap elemen
  `mutableMapOf("instruction_id"..., "question_id"..., "answerType"..., "answer"...)`
  (`AkmAnswerPayload.kt:33-40`). **Rekomendasi repo baru:** ganti jadi sealed class/data class
  bertipe (`AnswerPayloadItem` dengan `sealed class AnswerValue { MultipleChoice(id); Statement(...);
  Pair(...); Essay(text) }`) agar serialisasi aman dari refactor dan tidak bergantung pada urutan Map.

### 3.3 Kirim langsung vs lewat worker — persis di `uploadAnswer()`

`AkmViewModel.uploadAnswer(akmId)` (`AkmViewModel.kt:629-697`) adalah titik keputusan tunggal:

```kotlin
// AkmViewModel.kt:636 & 681-684
if (utils.isInternetAvailable()) {
    // ... build payload, POST langsung (synchronous, dalam coroutine IO), lalu update status
} else {
    // AKM-UPLOAD: "Offline, antri worker"
    enqueueWorkRequest(akmId)   // AkmViewModel.kt:699-733
}
```

- **Cek konektivitas pakai `isInternetAvailable()`** (deteksi kemampuan jaringan saja, BUKAN
  `isInternetValidated()` yang lebih ketat termasuk captive-portal check — lihat §4). Artinya: kalau
  perangkat tersambung ke WiFi captive portal yang belum login, `uploadAnswer()` akan MENGIRA online
  dan mencoba POST langsung — yang kemungkinan besar gagal (timeout/error jaringan), lalu masuk ke
  blok `catch` di `AkmViewModel.kt:686-695` sebagai kegagalan biasa (`errorString`/`uploadStatus =
  false`), **bukan otomatis lari ke `enqueueWorkRequest`**. Siswa harus menekan "Coba lagi" secara
  manual dari dialog di `AkmTakeResumePage.kt:678-684` (tombol ini memanggil ulang
  `viewmodel.uploadAnswer(akmId)` dari awal, yang akan mengulang pengecekan online/offline).
- **Jalur online (langsung, synchronous)**: semuanya di dalam satu `db.withTransaction { ... }`
  (`AkmViewModel.kt:639-677`):
  1. `updateStatusAkm(akmId, AKM_STATUS_FINISHED)`
  2. Build payload via `AkmAnswerPayload.build()`
  3. Panggil endpoint sesuai `exam_type` (`uploadJawabanTryOut` / `uploadJawabanUjianSchool` /
     `uploadJawabanAkm`) — **panggilan API terjadi DI DALAM transaksi Room**, jadi kalau API gagal
     (exception), seluruh transaksi (termasuk perubahan status FINISHED) di-roll back oleh Room.
  4. Kalau sukses: reset flag penalti, `updateStatusAkm(akmId, AKM_STATUS_UPLOADED)`,
     `delay(1000)`, lalu `WorkManager.cancelUniqueWork("exam_uploader_$akmId")` — membatalkan job
     force-submit terjadwal karena sudah tidak perlu lagi.
  - **Catatan desain untuk repo baru:** memanggil API network di dalam `db.withTransaction` adalah
    anti-pattern (transaksi Room seharusnya cepat & lokal saja, memanggil network di dalamnya
    menahan lock database selama request berlangsung). Jangan direplikasi — pisahkan: POST dulu di
    luar transaksi, baru transaksi pendek untuk update status berdasarkan hasil POST.
- **Jalur offline (`enqueueWorkRequest`, `AkmViewModel.kt:699-733`)**: TIDAK memanggil API sama
  sekali. Hanya: bangun `OneTimeWorkRequestBuilder<AkmUploader>` dengan `manual_submit=true`,
  `enqueueUniqueWork(..., ExistingWorkPolicy.REPLACE, ...)`, lalu update status ke
  `AKM_STATUS_FINISHED`, dan **langsung** `uploadStatus.postValue(true)` — artinya dari sudut pandang
  UI, "berhasil diantre" dianggap setara "berhasil" (dialog sukses `onUploadSuccess()` tampil sama
  persis walau upload sebenarnya belum terjadi). Ini penting untuk didesain ulang secara sadar di
  repo baru: state submit sebaiknya membedakan `Queued` vs `Uploaded` secara eksplisit di UI, bukan
  disamakan sebagai satu `Success`.

---

## 4. Deteksi online/offline & validasi captive portal

Dua fungsi berbeda di `Utils.kt`, dipakai untuk tujuan berbeda dan **tidak boleh disamakan**:

| Fungsi | Cek | Dipakai di mana | Catatan |
|---|---|---|---|
| `Utils.isInternetAvailable()` (`Utils.kt:19-24`) | `NET_CAPABILITY_INTERNET` saja | `AkmViewModel.uploadAnswer` (`AkmViewModel.kt:636`); juga gerbang seluruh panggilan API lewat `RequestInterceptor` (disebut di komentar `Utils.kt:32-33`) | Longgar — bisa `true` meski jaringan sebenarnya captive portal yang belum login |
| `Utils.isInternetValidated()` (`Utils.kt:35-41`) | `NET_CAPABILITY_INTERNET` **DAN** `NET_CAPABILITY_VALIDATED` | `AkmDetailPage.kt:578` (refresh setting sebelum mulai ujian), `AkmQuestionsPage.kt:573,657` (retry video/media) | Ketat — sengaja dipisah karena mengetatkan `isInternetAvailable` akan memicu sentinel offline (responseCode 0) di banyak tempat lain (`Utils.kt:32-33`) |

`uploadAnswer()` (titik submit) memakai versi **longgar** (`isInternetAvailable`), bukan versi ketat.
Ini konsisten dengan temuan §3.3: submit saat captive-portal-belum-login akan dicoba sebagai "online"
dan gagal di request API, bukan terdeteksi lebih awal sebagai "offline".

`ConnectionLiveData.kt` adalah versi **observable** dari cek ketat (`isInternetValidated`-equivalent):
mendaftarkan `ConnectivityManager.NetworkCallback` selama ada observer aktif (`onActive`/`onInactive`,
lifecycle-aware, tidak bocor), memancarkan `Boolean` tiap kali `onAvailable`/`onLost`/
`onCapabilitiesChanged` terjadi, dengan syarat kedua capability (`INTERNET` + `VALIDATED`)
(`ConnectionLiveData.kt:51-62`). **Tidak dipakai di alur upload jawaban** saat ini — hanya utility
yang tersedia. **Rekomendasi repo baru:** pertimbangkan memakai varian StateFlow dari pola ini
sebagai satu sumber kebenaran konektivitas yang dipakai KONSISTEN di semua titik (submit, retry
media, dsb.) — bukan bercampur `isInternetAvailable()` di satu tempat dan `isInternetValidated()` di
tempat lain seperti kode lama.

---

## 5. Retry/antrian upload lewat `AkmUploader` (WorkManager)

`AkmUploader` (`app/src/main/java/id/diskola/app/worker/AkmUploader.kt`) adalah `CoroutineWorker`
yang dijadwalkan dari 4 tempat berbeda dengan konfigurasi retry **identik**:
1. `AkmViewModel.enqueueWorkRequest` — saat submit manual sambil offline (`manual_submit=true`).
2. `AkmViewModel.startUjian` — saat ujian dimulai, `initialDelay = timeLeft` sampai `date_end`
   (`manual_submit=false`, ini jaring pengaman force-submit).
3. `OnKlasDbUtil.processAkmResponse` (`OnKlasDbUtil.kt:982-1013`) — setiap kali jadwal SCHOOL
   disinkronkan dari server dan status < UPLOADED, dijadwalkan ulang dengan `initialDelay` baru
   (menjamin jadwal force-submit selalu akurat walau app di-refresh berkali-kali).
4. `OnKlasDbUtil.processTryoutResponse` — sama, untuk jalur Tryout (`OnKlasDbUtil.kt:1133-...`; di
   awal fungsi ada `WorkManager.cancelAllWorkByTag("exam_uploader")` untuk membersihkan job lama
   sebelum re-schedule).

Konfigurasi WorkManager yang dipakai di ke-4 tempat (identik):
```kotlin
Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
.setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
.addTag("exam_uploader_$akmId")
// enqueueUniqueWork("exam_uploader_$akmId", ExistingWorkPolicy.REPLACE, workRequest)
```
- **Constraint jaringan**: worker TIDAK akan dijalankan sistem sampai perangkat `NetworkType.CONNECTED`
  — ini mekanisme utama "antre sampai online" (bukan polling manual).
- **Backoff**: `BackoffPolicy.LINEAR` dengan delay awal `WorkRequest.MIN_BACKOFF_MILLIS` (konstanta
  AndroidX WorkManager, saat ini 10 detik). LINEAR berarti delay retry ke-n ≈ `n × MIN_BACKOFF_MILLIS`
  (bukan eksponensial) — retry 1 ≈10 dtk, retry 2 ≈20 dtk, dst., dikelola otomatis oleh WorkManager
  saat worker mengembalikan `Result.retry()`.
- **Unique work + REPLACE**: nama unik `"exam_uploader_$akmId"` per ujian, kebijakan
  `ExistingWorkPolicy.REPLACE` — penjadwalan baru (mis. jadwal ter-refresh, atau siswa submit
  manual) selalu MENGGANTI job lama untuk `akmId` yang sama, bukan menumpuk job duplikat.

### 5.1 Batas retry & alasan berhenti — di dalam `doWork()`

```kotlin
// AkmUploader.kt:243
const val MAX_UPLOAD_ATTEMPTS = 10

// AkmUploader.kt:130-140 — kegagalan hasil upload API (bukan exception)
if (!resultUpload && runAttemptCount < MAX_UPLOAD_ATTEMPTS) {
    akmDao.insert(akm.schedule.copy(status = AkmStatus.AKM_STATUS_FINISHED))
    return Result.retry()
}

// AkmUploader.kt:185-190 — exception tak tertangani selama doWork()
} catch (e: Exception) {
    akmDao.insert(akm.schedule.copy(status = AkmStatus.AKM_STATUS_FINISHED))
    if (runAttemptCount < MAX_UPLOAD_ATTEMPTS) Result.retry() else Result.failure()
}
```
- `runAttemptCount` adalah properti bawaan `ListenableWorker` (jumlah percobaan yang sudah dilakukan
  WorkManager untuk instance kerja unik ini, termasuk retry akibat backoff). Setelah 10 percobaan,
  worker berhenti retry (`Result.failure()` pada exception tak tertangani; pada kegagalan hasil upload
  biasa malah **jatuh ke bawah dan lanjut ke notifikasi "gagal" + `Result.success()` implisit** —
  lihat §5.2, ini penting: dua jalur kegagalan berakhir dengan `Result` WorkManager yang berbeda).
- **Sebelum percobaan ke-1 dieksekusi**, ada gerbang force-submit-grace (`AkmUploader.kt:57-68`):
  kalau `manual_submit=false` (ini job otomatis, bukan submit manual) DAN sisa waktu ke `date_end`
  masih > `FORCE_SUBMIT_GRACE_MS` (5 detik), worker **tidak melakukan apa pun** — hanya
  menjadwalkan ulang dirinya sendiri via `rescheduleForceSubmit()` dengan delay baru, lalu
  `Result.success()` (bukan retry) untuk instance job saat ini. Ini jaring pengaman kalau sistem
  menjalankan worker terlalu cepat dari jadwal (mis. karena Doze/App Standby exemption momentary).
- Field lain yang membatasi eksekusi: kalau `student_exam_id < 1` atau
  `status > AKM_STATUS_UPLOADED` (sudah SCORED/EXPLAINED), worker langsung `Result.success()` tanpa
  melakukan apa pun (`AkmUploader.kt:55`) — mencegah re-upload untuk ujian yang sudah dinilai.

### 5.2 Alur sesudah percobaan upload (sukses atau gagal, TIDAK exception)

Setelah panggilan API (`uploadJawabanTryOut`/`uploadJawabanUjianSchool`/`uploadJawabanAkm`)
selesai — baik sukses maupun gagal via `try/catch` lokal per jenis ujian (`AkmUploader.kt:82-114`,
bukan exception yang lolos ke `catch` terluar):
1. Reset semua state penalti SharedPreferences untuk `akmId` ini (`state_penalty_*`,
   `state_penalty_pending_*`, `finish_*`, hapus `data_*`/`hours_*`/`penalty_end_*`), dan
   `stopService(CountDownService)` — supaya penalti tidak nyangkut ke ujian berikutnya
   (`AkmUploader.kt:117-124`).
2. Kalau **masih bisa retry** (gagal & `runAttemptCount < MAX_UPLOAD_ATTEMPTS`) → set status
   `FINISHED`, `return Result.retry()` — **notifikasi TIDAK ditampilkan** pada percobaan yang masih
   akan diulang.
3. Kalau **sudah final** (sukses, ATAU gagal tapi attempt sudah habis) → tampilkan
   `NotificationCompat` ("Jawaban berhasil terkirim" / "Jawaban gagal terkirim"), lalu:
   - sukses → `updateStatusAkm(AKM_STATUS_UPLOADED)`
   - gagal final → `updateStatusAkm(AKM_STATUS_FINISHED)` (berhenti di FINISHED selamanya, tidak ada
     transisi otomatis lain — siswa harus pakai tombol "Upload Ulang" di `AkmScorePage`, lihat §7)
   - keduanya diakhiri `Result.success()` — **penting**: kegagalan final (`resultUpload=false`
     setelah attempt habis) tetap mengembalikan `Result.success()` ke WorkManager, bukan
     `Result.failure()`. WorkManager menganggap job ini "selesai" walau secara bisnis gagal — status
     kegagalan hanya tercermin di `AkmStatus.AKM_STATUS_FINISHED` pada tabel Room dan notifikasi,
     bukan di riwayat WorkManager.

---

## 6. Status upload (`AkmStatus`) — siapa mengubah, kapan persis

Enum status (`AkmEntities.kt:8-16`), sebagai `Int` di kolom `akm.status`:

| Konstanta | Nilai | Arti |
|---|---|---|
| `AKM_STATUS_NEW` | 0 | Belum diunduh |
| `AKM_STATUS_DOWNLOADING` | 1 | Sedang mengunduh soal |
| `AKM_STATUS_DOWNLOADED` | 2 | Soal siap dikerjakan |
| `AKM_STATUS_FINISHED` | 3 | Ujian selesai dikerjakan/disubmit LOKAL, TAPI **belum tentu** terkirim ke server — status transisi/gagal juga hinggap di sini |
| `AKM_STATUS_UPLOADED` | 4 | Jawaban terkonfirmasi terkirim ke server |
| `AKM_STATUS_SCORED` | 5 | Server sudah menilai |
| `AKM_STATUS_EXPLAINED` | 6 | Pembahasan tersedia |

**Poin krusial:** `AKM_STATUS_FINISHED` adalah status **ambigu/overloaded** — dipakai untuk 3 kondisi
berbeda yang tidak bisa dibedakan hanya dari nilai status ini:
1. Baru saja mulai proses submit (`AkmViewModel.kt:640`, set FINISHED sebelum payload dibangun).
2. Antre menunggu worker karena offline saat submit manual (`enqueueWorkRequest`, `AkmViewModel.kt:729`).
3. **Gagal upload** — baik retry masih berjalan (`AkmUploader.kt:138`) maupun sudah final gagal
   (`AkmUploader.kt:181`).

`AkmScorePage.kt:177-186` membedakan "antre" vs "gagal" bukan dari `AkmStatus` semata, tapi dari
kombinasi `status == UPLOADED` (dianggap `isQueued`, karena UPLOADED belum tentu SCORED) ATAU
`status == FINISHED` (dianggap `isFailedStatus`) DIGABUNG dengan state UI lokal `lastResult`/
`lastResultId` (hasil upload ulang sesi berjalan, di memori Fragment, hilang saat proses mati). Ini
berarti: **kalau app di-kill sebelum notifikasi terlihat, satu-satunya sumber kebenaran yang bertahan
adalah `AkmStatus` di Room** — dan karena FINISHED overload 3 arti, UI tidak bisa membedakan "masih
diproses worker" vs "gagal permanen" tanpa juga mengecek apakah ada WorkManager job aktif dengan tag
`exam_uploader_$akmId` (kode lama TIDAK melakukan pengecekan ini di `AkmScorePage`).

Siapa yang mengubah status, ringkas:

| Transisi | Dipicu oleh | Lokasi |
|---|---|---|
| apapun → FINISHED (mulai submit, jalur online) | `AkmViewModel.uploadAnswer` (dalam transaksi, sebelum POST) | `AkmViewModel.kt:640` |
| FINISHED → UPLOADED (jalur online, sukses) | `AkmViewModel.uploadAnswer` (dalam transaksi, setelah POST sukses) | `AkmViewModel.kt:672` |
| apapun → FINISHED (jalur offline, sebelum diantre) | `AkmViewModel.enqueueWorkRequest` | `AkmViewModel.kt:729` |
| apapun → FINISHED (worker mulai kerja) | `AkmUploader.doWork` (dalam transaksi bareng build payload) | `AkmUploader.kt:77` |
| FINISHED → FINISHED (retry, gagal tapi masih ada percobaan) | `AkmUploader.doWork` | `AkmUploader.kt:138` |
| FINISHED → UPLOADED (worker sukses) | `AkmUploader.doWork` | `AkmUploader.kt:178` |
| FINISHED → FINISHED (worker gagal final) | `AkmUploader.doWork` | `AkmUploader.kt:181` |
| FINISHED → FINISHED (exception tak tertangani) | `AkmUploader.doWork` catch block | `AkmUploader.kt:187` |
| UPLOADED → SCORED / EXPLAINED | **Ditentukan server**, ditulis ulang ke Room saat `dbUtil.processAkmResponse()` memproses response `listScoreAkm`/`listUjianScored`/`detailAkm` berikutnya (bukan diubah oleh Uploader atau ViewModel) | `OnKlasDbUtil.kt` (lihat `AkmTable.status` dari mapping response, tidak dibahas detail di sini — di luar cakupan submit/sinkron) |

Kesimpulan: **baik `AkmViewModel` (jalur langsung) maupun `AkmUploader` (worker) sama-sama menulis ke
kolom status yang sama** — tidak ada pemisahan "ViewModel hanya baca, Worker hanya tulis". Ini rawan
race condition kalau kedua jalur kebetulan berjalan bersamaan untuk `akmId` yang sama (mis. siswa
menekan "Kumpulkan" tepat saat force-submit worker terjadwal jalan) — kode lama tidak punya mutex
eksplisit untuk mencegah ini selain `ExistingWorkPolicy.REPLACE` pada penjadwalan (yang hanya
mencegah *penjadwalan* dobel, bukan *eksekusi* dobel kalau satu instance worker sudah lanjut jalan).

---

## 7. Sinkronisasi ulang manual — tombol "Upload Ulang" (`AkmScorePage`)

Tombol ini (`AkmScorePage.kt:211-256`, `btnUpload`) **memanggil fungsi yang SAMA PERSIS**
(`viewmodel.uploadAnswer(item.id)`, `AkmScorePage.kt:250`) dengan submit pertama kali — bukan jalur
kode terpisah. Bedanya murni di lapisan UI/guard sebelum memanggilnya:

1. **Gerbang status**: hanya aktif kalau `status == UPLOADED` atau `status == FINISHED`
   (`AkmScorePage.kt:212`) — tidak bisa dipakai untuk ujian yang sudah SCORED (percuma, sudah
   dinilai) atau yang belum pernah disubmit sama sekali (statusnya masih < FINISHED, tombol ini
   tidak tampil, lihat kondisi `showUpload` di `AkmScorePage.kt:186`).
2. **Cooldown 1 menit di SharedPreferences** (`akm_upload_cooldown_$id`, `AkmScorePage.kt:310-320`):
   murni proteksi UI sisi klien (anti-spam-klik), bukan dari server. Kalau dalam cooldown, klik
   menampilkan dialog timer (`showCooldownDialog`) dan **tidak** memanggil `uploadAnswer` sama
   sekali.
3. **Pengecekan data lokal tersedia** (`hasUploadData()`, `AkmScorePage.kt:322-337`): query Room
   `getExamInstructionAsync` → per instruksi `getInstQuestion` → kalau SEMUA instruksi kosong
   (`questions.isEmpty()`) di semua exam, dianggap tidak ada data untuk diupload ulang dan
   ditampilkan pesan "Data jawaban tidak ditemukan di perangkat ini" — mencegah percobaan upload ulang
   yang pasti mengirim payload kosong (skenario umum: app di-reinstall/data dibersihkan setelah submit
   pertama tapi sebelum sukses, Room kosong tapi status masih FINISHED di server).
4. Dialog konfirmasi (`prettyAlert` "Yakin ingin upload ulang...") → baru panggil `uploadAnswer`.
5. Setelah dipanggil: `startCooldown(item.id)`, set `uploadingAkmId` (state di memori Fragment untuk
   menonaktifkan tombol & ubah label ke "Mengupload jawaban...") — **tidak ada observer LiveData** ke
   `viewmodel.uploadStatus` di titik ini (beda dari `AkmTakeResumePage` yang meng-observe). Hasil
   sukses/gagal ditangani lewat variabel `lastResult`/`lastResultId` yang diisi dari tempat lain
   (di luar potongan yang dibaca — kemungkinan callback lain di file yang sama; **tidak
   ditelusuri detail di sini karena di luar cakupan tugas, tapi perlu dicatat sebagai TITIK AMBIGU**:
   mekanisme persis bagaimana `lastResult` terisi tidak terlihat pada baris 150-356 yang dibaca).

**Kesimpulan:** "Upload Ulang" BUKAN mekanisme sinkronisasi berbeda — ia mendaur ulang persis logic
online/offline branching yang sama di §3.3 (kalau device online saat itu → POST langsung; kalau
offline → antre worker lagi). Nilai tambahnya murni di lapisan guard UI (cooldown, cek status, cek
data lokal), bukan di logic pengiriman.

---

## 8. Hal yang TIDAK ditemukan / ambigu (perlu keputusan sadar di repo baru)

- **Tidak ada autosave jawaban ke server** selama pengerjaan — murni Room sampai submit. (Dikonfirmasi,
  bukan ambigu — dicatat di sini supaya tidak dianggap terlewat.)
- **Validasi "semua soal terjawab" tidak konsisten** antara SCHOOL (tidak ada validasi) dan TRYOUT
  (ada dialog peringatan, tapi bisa dilewati) — lihat §3.1.
- **`AKM_STATUS_FINISHED` overloaded** (3 arti berbeda: baru mulai submit / antre offline / gagal
  permanen) — lihat §6. UI (`AkmScorePage`) membedakannya sebagian pakai state di memori yang tidak
  bertahan lintas proses.
- **Kegagalan upload final tetap `Result.success()`** di WorkManager (`AkmUploader.kt`, akhir fungsi)
  — histori WorkManager tidak mencerminkan kegagalan bisnis, hanya `AkmStatus` Room dan notifikasi
  sistem yang mencerminkannya.
- **Race condition berpotensi** antara jalur `AkmViewModel.uploadAnswer` (langsung) dan
  `AkmUploader` (worker force-submit) kalau kebetulan berjalan bersamaan untuk `akmId` sama — tidak
  ada mutex/lock eksplisit selain `ExistingWorkPolicy.REPLACE` pada *penjadwalan* job baru.
  `enqueueUniqueWork` tidak menghentikan instance yang **sudah terlanjur berjalan (RUNNING)**.
- **Bagaimana persis `lastResult`/`lastResultId` di `AkmScorePage` terisi setelah upload ulang** tidak
  tertelusuri dari rentang baris yang dibaca (150-356) — kemungkinan ada callback/listener lain di
  bagian atas file yang tidak termasuk cakupan file yang diminta untuk didokumentasikan detail
  (fokus tugas ini adalah `AkmAnswerPayload`, `AkmUploader`, fungsi submit `AkmViewModel`/
  `AkmTakeResumePage`, deteksi konektivitas, dan penyimpanan jawaban — bukan seluruh `AkmScorePage.kt`).
  Kalau dibutuhkan detail penuh alur ini, perlu pembacaan menyeluruh file tersebut secara terpisah.
- **Panggilan API di dalam `db.withTransaction`** pada jalur online `uploadAnswer` (§3.3) adalah
  anti-pattern yang sebaiknya TIDAK direplikasi, bukan ambiguitas tapi catatan desain eksplisit.

---

## 9. Rekomendasi untuk repo baru (Hilt + Compose + Room)

### 9.1 Layering: Repository/UseCase

```kotlin
// domain/repository
interface AkmAnswerRepository {
    // Tulis jawaban ke Room SEGERA, tidak pernah menyentuh network di sini.
    suspend fun saveMultipleChoiceAnswer(questionId: Int, answer: AkmAnswerEntity)
    suspend fun saveEssayAnswer(questionId: Int, text: String)
    suspend fun savePairAnswer(questionId: Int, answers: List<AkmAnswerEntity>)

    // Titik satu-satunya yang boleh memicu network untuk jawaban.
    suspend fun submitAnswers(scheduleId: Int, manual: Boolean): Result<SubmitOutcome>

    // Observasi status submit sebuah ujian (dari Room, reaktif).
    fun observeSubmitStatus(scheduleId: Int): Flow<AkmSubmitStatus>
}

sealed class SubmitOutcome {
    data object Uploaded : SubmitOutcome()
    data object Queued : SubmitOutcome()   // eksplisit beda dari Uploaded — lihat temuan §3.3
}

enum class AkmSubmitStatus { NOT_SUBMITTED, PENDING_UPLOAD, UPLOADED, SCORED, EXPLAINED, UPLOAD_FAILED_RETRYING, UPLOAD_FAILED_FINAL }
```

Catatan desain, langsung menjawab temuan §6 & §8:
- **Pecah `AKM_STATUS_FINISHED` yang overloaded** menjadi status yang tidak ambigu: minimal pisahkan
  `PENDING_UPLOAD` (antre offline) dari `UPLOAD_FAILED_RETRYING` dan `UPLOAD_FAILED_FINAL`. Ini bisa
  berupa kolom Room terpisah (mis. `upload_attempt_count`, `last_upload_error`) di samping status
  utama, bukan menimpa satu kolom `status` untuk 3 arti berbeda.
- **`submitAnswers()` TIDAK memanggil API di dalam transaksi Room** — build payload dalam satu
  `withTransaction` singkat (baca-saja), lalu POST di luar transaksi, lalu transaksi kedua yang
  singkat untuk commit status hasil. Menghindari anti-pattern §3.3.
- Payload dibangun dari data class bertipe (bukan `List<Any>`/`Map` mentah seperti
  `AkmAnswerPayload.kt` lama) — mis. `sealed interface AnswerValue` per `answerType`, supaya
  serialisasi Moshi/kotlinx.serialization aman dari perubahan struktur tanpa compile error tersamar.

### 9.2 Offline-queue via WorkManager `HiltWorker`

Pertahankan pola yang sudah terbukti (constraint `NetworkType.CONNECTED`, backoff, unique work per
`scheduleId`), tapi perbaiki titik lemah yang ditemukan:

```kotlin
@HiltWorker
class SubmitAkmAnswersWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: AkmAnswerRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val scheduleId = inputData.getInt(KEY_SCHEDULE_ID, -1)
        if (scheduleId < 0) return Result.failure()
        return when (val outcome = repository.submitAnswers(scheduleId, manual = true)) {
            is kotlin.Result -> /* map ke Result.success()/retry()/failure() eksplisit,
                                    JANGAN return Result.success() untuk kegagalan bisnis final —
                                    lihat temuan §5.2. Kegagalan final bisnis = Result.failure(),
                                    supaya histori WorkManager & WorkInfo bisa dipakai UI observe. */
        }
    }
    companion object {
        const val MAX_ATTEMPTS = 10   // pertahankan angka yang sudah terbukti dari AkmUploader lama
    }
}
```

- **Konsisten pakai satu sumber kebenaran konektivitas**: adopsi pola `ConnectionLiveData` lama tapi
  sebagai `StateFlow<Boolean>` (via `callbackFlow` + `NetworkCallback`), dan **selalu pakai varian
  VALIDATED** (setara `isInternetValidated`) untuk keputusan submit, bukan varian longgar
  (`isInternetAvailable`) seperti kode lama (temuan §4) — supaya captive portal terdeteksi SEBELUM
  mencoba POST, bukan gagal di tengah jalan.
- **Cegah race antara submit langsung dan force-submit worker** (temuan §6 & §8): sebelum jalur
  langsung mem-POST, cek `WorkManager.getWorkInfosForUniqueWork(tag).get()` — kalau ada instance
  `RUNNING`, jangan POST dobel; tunggu/gabungkan hasilnya. Atau lebih simpel: satukan SEMUA jalur
  submit (manual maupun force-submit terjadwal) untuk selalu lewat WorkManager yang sama (tidak ada
  lagi jalur "langsung dari ViewModel" terpisah dari worker) — deduplikasi otomatis lewat
  `ExistingWorkPolicy.REPLACE`/`KEEP` tergantung kebutuhan (submit manual baru sebaiknya `REPLACE`,
  retry otomatis dari refresh jadwal sebaiknya `KEEP` supaya tidak membatalkan job yang sedang antre).

### 9.3 State submit di Compose

```kotlin
sealed interface SubmitUiState {
    data object Idle : SubmitUiState
    data object Submitting : SubmitUiState
    data class Queued(val reason: String) : SubmitUiState   // eksplisit: offline saat submit
    data object Success : SubmitUiState
    data class Failed(val message: String, val retryable: Boolean) : SubmitUiState
}

class AkmSubmitViewModel @Inject constructor(
    private val repository: AkmAnswerRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SubmitUiState>(SubmitUiState.Idle)
    val uiState: StateFlow<SubmitUiState> = _uiState.asStateFlow()

    fun submit(scheduleId: Int) {
        viewModelScope.launch {
            _uiState.value = SubmitUiState.Submitting
            repository.submitAnswers(scheduleId, manual = true)
                .onSuccess { outcome ->
                    _uiState.value = when (outcome) {
                        SubmitOutcome.Uploaded -> SubmitUiState.Success
                        SubmitOutcome.Queued -> SubmitUiState.Queued("Menunggu koneksi internet")
                    }
                }
                .onFailure { e -> _uiState.value = SubmitUiState.Failed(e.message ?: "Gagal", retryable = true) }
        }
    }
}
```

Composable HANYA mengamati `uiState` dan memanggil `viewModel.submit(scheduleId)` — tidak pernah
memanggil Repository/WorkManager langsung dari Composable (selaras `docs/rules-global.md` poin 6 dan
`docs/REBUILD_PROMPT.md` baris 61-63 soal layering `UI → ViewModel(StateFlow) → Repository`).

### 9.4 Pola offline-first untuk write/submit (bukan cuma GET)

`docs/rules-global.md` §1 mendisiplinkan *fetch* (GET) supaya tidak dipanggil berulang dari
Composable/lifecycle tanpa guard. Prinsip yang sama berlaku untuk *write*/submit:
- **Jangan pernah panggil API submit langsung dari UI thread/Composable** (`LaunchedEffect`, klik
  handler yang langsung `api.uploadX(...)`) — HARUS lewat Repository, dan idealnya Repository sendiri
  tidak POST langsung melainkan mendelegasikan ke WorkManager (§9.2) supaya submit otomatis
  survive process death, rotasi layar, dan app-kill — persis alasan `AkmUploader` lama dibuat, tapi
  tanpa titik lemah yang sudah ditemukan (§6, §8).
- **Room tetap source of truth**: status submit yang ditampilkan di UI SELALU dibaca dari Room
  (via Flow), bukan dari variabel di memori ViewModel/Fragment (masalah nyata `lastResult`/
  `lastResultId` di `AkmScorePage` lama, temuan §8, yang hilang begitu proses mati).
- **Retry policy**: pertahankan `BackoffPolicy.LINEAR` + `NetworkType.CONNECTED` + `MAX_ATTEMPTS=10`
  sebagai baseline yang sudah terbukti di produksi (`AkmUploader.kt:243`), kecuali ada alasan kuat
  untuk beralih ke EXPONENTIAL (mis. kalau endpoint submit diketahui rentan terhadap thundering herd
  saat banyak siswa submit bersamaan di akhir waktu ujian — EXPONENTIAL akan menyebar beban retry
  lebih baik daripada LINEAR).
