# Asesmen AKM — Arsitektur Teknis: Melihat Nilai Setelah Mengumpulkan Ujian & Sinkronisasi Pembahasan

> Dokumen ini fokus pada **mekanisme kerja** (state machine, worker, fetch discipline, retry), bukan
> pemetaan UI-ke-data-key. Untuk key data per elemen UI lihat `docs/features/asesmen-akm-daftar-detail-nilai.md`
> dan `docs/api/04-pembayaran-klaspay-ppob-akm.md`. Semua kutipan memakai format `file:baris`.

---

## 1. Ringkasan alur

```
[AkmUploader sukses] --> status lokal = AKM_STATUS_UPLOADED (4)
                                │
                                │  (tidak ada sinyal push/callback dari server)
                                ▼
        [pengguna membuka AkmScorePage / AkmScoreDetailPage, atau pull-to-refresh]
                                │
                                ▼
     GET akm/scored | exam-schedules-scored  (listScoreAkm / listUjianScored)
                                │
                    isAssessed == true?  ── tidak ──▶ status tetap UPLOADED/FINISHED,
                                │                      nilai belum ada, ulangi nanti
                                │ ya
                                ▼
        processAkmResponse menulis status = AKM_STATUS_SCORED (5) + exam_score ke Room
                                │
                                ▼
     Flow Room (`AkmDao.get(id)`) otomatis memancarkan skor baru ke Compose/DataBinding
                                │
                                ▼
        [jika has_explain == true] tombol "Sinkronisasi Pembahasan" muncul
                                │
                    downloadPembahasanSoal() -> AkmExplanationDownloader (worker)
                                │
                    status = AKM_STATUS_EXPLAINED (6) -> tombol jadi "Lihat Pembahasan"
```

**Poin kunci:** tidak ada *push notification*, *WebSocket*, maupun endpoint status-polling khusus.
Perubahan status SCORED murni ditemukan lewat **re-fetch endpoint list/scored** yang sama yang dipakai
untuk memuat daftar nilai — bukan lewat observer `WorkInfo` dari upload, dan bukan lewat field di
response upload.

---

## 2. Dari submit sampai nilai muncul

### 2.1 Apa yang terjadi persis setelah `AkmUploader` sukses

`AkmUploader.doWork()` mengunggah jawaban lalu **hanya mengubah status lokal**, tidak pernah membaca
nilai dari response upload:

```kotlin
// app/src/main/java/id/diskola/app/worker/AkmUploader.kt:176-182
if (resultUpload) {
    akmDao.insert(akm.schedule.copy(status = AkmStatus.AKM_STATUS_UPLOADED))
} else {
    akmDao.insert(akm.schedule.copy(status = AkmStatus.AKM_STATUS_FINISHED))
}
```

Ketiga endpoint upload (`uploadJawabanAkm`, `uploadJawabanUjianSchool`, `uploadJawabanTryOut` —
`ApiService.kt:949-974`) semuanya bertipe kembalian `Any` — **tidak ada model bertipe untuk body
response-nya**, dan kode tidak pernah membaca isi body itu (lihat §3 tabel anti-pattern
`docs/rules-global.md` poin 5, ini praktik lama yang sebaiknya tidak diulang di repo baru). Jadi:

- Nilai **tidak pernah** tersedia langsung di response upload.
- Tidak ada logic apa pun di `AkmUploader`/`AkmViewModel.uploadAnswer()` yang menunggu atau mem-parsing
  skor pasca-upload.

### 2.2 Apa yang memicu status berubah jadi SCORED

Satu-satunya tempat status di-set ke `AkmStatus.AKM_STATUS_SCORED` (5) ada di
`OnKlasDbUtil.processAkmResponse()`:

```kotlin
// app/src/main/java/id/diskola/app/db/OnKlasDbUtil.kt:924-932
val akmTable = AkmTable(
    it,
    when {
        it.isAssessed -> AkmStatus.AKM_STATUS_SCORED
        it.isDone -> AkmStatus.AKM_STATUS_FINISHED
        it.isQueued -> AkmStatus.AKM_STATUS_UPLOADED
        ids.contains(it.id) -> idStatus.first { ids -> ids.id == it.id }.status
        else -> 0
    },
    ...
```

`it.isAssessed` adalah field `ListAkmData.isAssessed` (`AkmModels.kt:46`) yang **hanya dikirim backend
lewat response `listAkm`/`listScoreAkm`/`listUjian`/`listUjianScored`/`detailAkm`/`detailUjianSekolah`**
— dipanggil dari `AkmViewModel.fetchAkm/loadAkmScored/fetchUjianSchool/loadUjianSchoolScored/fetchDetailAkm`.
`processAkmResponse` dipanggil dari titik-titik itu saja (`AkmViewModel.kt:187,210,245,380,415,443`).

Jadi mekanismenya adalah **pull murni**: tidak ada mekanisme di aplikasi yang tahu kapan guru selesai
menilai. Kapan `isAssessed` menjadi `true` sepenuhnya ditentukan backend — bisa langsung (soal pilihan
ganda dinilai otomatis oleh server saat submit) atau tertunda tak tentu (soal esai menunggu guru
menilai manual). Dari sisi client, kedua kasus terlihat identik: status tetap `UPLOADED`/`FINISHED`
sampai suatu saat re-fetch endpoint scored mengembalikan `isAssessed=true`.

**Tidak ada endpoint polling status upload/nilai yang terpisah.** Endpoint yang tersedia untuk AKM di
`ApiService.kt` (baris 888-1001) hanyalah endpoint list/detail/download/upload/explain biasa — tidak
ada `.../status` atau `.../check-scored`. Mekanisme "polling" yang ada di aplikasi ini adalah
pemanggilan ulang `listScoreAkm`/`listUjianScored` yang sama, dipicu **aksi/pembukaan halaman**, bukan
timer berkala (lihat §4).

### 2.3 `exam_score` vs `score[]` — sumber nilai yang benar

```kotlin
// app/src/main/java/id/diskola/app/db/OnKlasDbUtil.kt:1026-1057
val carriesScore =
    it.score.isNotEmpty() || (it.exam_score != null && it.exam_score > -1)

if (it.score.isNotEmpty())
    it.score.forEach { score -> exams.firstOrNull { it.id == score.id }?.score = score.scored }
else if (it.exam_score != null && it.exam_score > -1) {
    exams.onEach { exam -> exam.score = it.exam_score }
}
...
// guard: kalau response TIDAK membawa nilai, pertahankan skor lama, jangan timpa jadi 0
val merged = if (carriesScore) examTables else {
    val stored = memoryDB.akm().getExamsAsync(it.id).associateBy { e -> e.id }
    examTables.map { table ->
        val previous = stored[table.id]?.score ?: 0.0
        if (table.score == 0.0 && previous > 0.0) table.copy(score = previous) else table
    }
}
```

Endpoint jadwal biasa (`exam-schedules/{id}`, `schedules/{id}`) **selalu** mengirim `exam_score = -1`
dan `score = []` — komentar di kode (`AkmScoreDetailPage.kt:403-405`) menegaskan **hanya endpoint
`scored`/`exam-schedules-scored` yang membawa nilai asli**. Guard `merged` di atas mencegah nilai lama
tertimpa jadi 0 kalau layar lain (yang tidak membawa skor) memuat ulang schedule yang sama.

---

## 3. `has_explain` — verifikasi field, BUKAN dari `mobile/setting-akm`

Instruksi awal tugas ini menduga `has_explain` berasal dari `penaltyResponse` (`GET mobile/setting-akm`).
**Setelah verifikasi langsung ke kode, ini keliru** — perlu dikoreksi eksplisit:

- `penaltyResponse` (`AkmModels.kt:197-218`, response dari `settingAkmpenalty()` /
  `GET mobile/setting-akm`) hanya berisi `penalty_times`, `penalty_applied`, `absence_setting`,
  `exam_lock_mode`. **Tidak ada field `has_explain` di sana.**
- `has_explain` sebenarnya adalah field `ListAkmData.has_explain: Boolean?` (`AkmModels.kt:43`),
  dikirim oleh endpoint **per-jadwal** (`listAkm`, `listScoreAkm`, `detailAkm`, `detailUjianSekolah`,
  `listUjian`, `listUjianScored`) — bukan endpoint setelan global.

```kotlin
// app/src/main/java/id/diskola/app/pages/akm/AkmModels.kt:35-43
/**
 * Menandai bahwa server memang menyediakan pembahasan untuk jadwal ini.
 * Default-nya tidak tersedia...
 */
val has_explain: Boolean? = null,
```

### 3.1 Kapan jadi `true`, per-jadwal atau global?

**Per-jadwal (per `akmId`)**, bukan global/per-akun. Nilainya disimpan di tabel key-value
`akm_settings` dengan key unik per id, bukan sebagai kolom `AkmTable` (supaya tidak perlu menaikkan
versi Room — lihat komentar `AkmSettings.kt:37-45`):

```kotlin
// app/src/main/java/id/diskola/app/pages/akm/AkmSettings.kt:46
fun keyHasExplain(akmId: Int): String = "has_explain_$akmId"
```

Ditulis di `OnKlasDbUtil.processAkmResponse()` setiap kali response list/detail AKM diproses:

```kotlin
// app/src/main/java/id/diskola/app/db/OnKlasDbUtil.kt:963-968
memoryDB.akm().insertSetting(
    AkmSettingTable(
        key = AkmSettings.keyHasExplain(it.id),
        value = if (it.has_explain == true) 1 else 0
    )
)
```

Dibaca lewat `AkmSettings.hasExplain(dao, akmId)` (`AkmSettings.kt:56-64`) — sentinel `-1` (belum
pernah tersimpan) dan `0` (server bilang tidak tersedia) **sama-sama** dianggap `false`; hanya nilai
tersimpan `1` yang membuat tombol tampil. Jadi flag ini "aman secara default" (tombol tersembunyi
sampai server eksplisit bilang "ada").

**Siapa yang set:** backend, lewat field `has_explain` di response — bukan sesuatu yang dihitung
client. Client hanya cermin dari nilai terakhir yang diterima.

**Kapan dicek ulang:** setiap kali `processAkmResponse` dipanggil untuk id itu (yaitu setiap kali
listAkm/listScoreAkm/listUjian/listUjianScored/detailAkm/detailUjianSekolah untuk jadwal itu di-fetch
ulang) — bukan di-cache dengan TTL seperti `SettingAkmCache` (§ berikutnya). Karena hanya dipanggil
saat halaman dibuka/pull-to-refresh (fetch discipline §4), nilai `has_explain` di Room bisa saja
tertinggal dari server sampai fetch berikutnya terjadi.

---

## 4. Kapan nilai di-refresh dari server (fetch discipline)

### 4.1 `AkmScorePage` (daftar nilai) — TIDAK fetch-on-open tanpa syarat

```kotlin
// app/src/main/java/id/diskola/app/pages/akm/AkmScorePage.kt:75-80
// Sebelumnya di sini ada pemanggilan loadUjianSchoolScored() tanpa syarat...
// Pengambilan awal kini ditangani boundary callback Paging, yang hanya menembak
// saat data lokal benar-benar kosong. Selebihnya lewat swipe refresh.
```

Konkretnya:
- **Boundary callback Paging** (`AkmViewModel.kt:122-141` untuk `listScoreAkm`, `:282-301` untuk
  `listUjianSchoolScored`) hanya memicu `loadAkmScored()`/`loadUjianSchoolScored()` ketika Paging
  **kehabisan data lokal** (list kosong atau perlu halaman berikutnya) — bukan setiap kali fragment
  `onCreate`/`onResume`.
- **Pull-to-refresh eksplisit** (`binding.swipeRefresh.setOnRefreshListener`, `AkmScorePage.kt:94-100`
  dan `:113-119`) me-reset guard `isLoading*`/`hasMore*` lalu memanggil ulang.
- **Setelah kembali dari `AkmScoreDetailPage`** (`onActivityResult` request code 219,
  `AkmScorePage.kt:273-290`) — memaksa reload daftar, karena detail page bisa saja baru menyinkronkan
  nilai yang belum tercermin di daftar.

Pola ini **patuh** pada `docs/rules-global.md` §1.1 (dilarang fetch-on-open kecuali wajib real-time):
daftar nilai dimuat dari Room dulu, fetch hanya terjadi saat cache kosong atau permintaan eksplisit
pengguna.

### 4.2 `AkmScoreDetailPage` — fetch-on-open YANG DISENGAJA, dengan alasan eksplisit di kode

Berbeda dengan daftar, halaman **detail** nilai justru fetch setiap kali dibuka:

```kotlin
// app/src/main/java/id/diskola/app/pages/akm/AkmScoreDetailPage.kt:127-135
// Ambil nilai terbaru begitu halaman dibuka. Sejak daftar AKM tidak lagi menembak
// server setiap kali dibuka, nilai di Room bisa tertinggal dari yang sudah dinilai
// guru. Endpoint detail mengembalikan `exam_score` pada payload yang sama...
// Panggilan ini dipicu aksi siswa (menekan "Lihat Nilai"), bukan pembukaan layar
// sembarangan, sehingga volumenya tetap rendah.
lifecycleScope.launchWhenCreated { refreshScoreFromServer() }
```

```kotlin
// app/src/main/java/id/diskola/app/pages/akm/AkmScoreDetailPage.kt:399-419
private suspend fun fetchScored() {
    if (isTryout) return
    viewmodel.isSchoolScope = isSchoolScope
    // Paksa lewati penjaga "sedang memuat / sudah habis" supaya pembukaan halaman ini
    // selalu mengambil nilai terbaru.
    if (isSchoolScope) {
        viewmodel.isLoadingUjianSchoolScored = false
        viewmodel.hasMoreUjianSchoolScored = true
        viewmodel.loadUjianSchoolScored()
    } else {
        viewmodel.isLoadingScored = false
        viewmodel.hasMoreScored = true
        viewmodel.loadAkmScored()
    }
}
```

**Ini adalah pengecualian yang sah terhadap larangan fetch-on-open di `docs/rules-global.md` §1.1**,
dan kode sudah mendokumentasikan alasannya sendiri: karena §4.1 sengaja menghilangkan fetch-on-open di
daftar (untuk hemat kuota), satu-satunya titik yang *masih* menyegarkan nilai adalah detail page — dan
volumenya rendah karena hanya terpicu saat siswa benar-benar membuka satu hasil ujian (aksi eksplisit
"Lihat Nilai" dari daftar), bukan setiap render ulang layar. Ini **konsisten** dengan kategori
pengecualian resmi di aturan tersebut ("Data yang eksplisit diminta user lewat aksi").

Penting: `fetchScored()` memanggil ulang `loadUjianSchoolScored()`/`loadAkmScored()` — **endpoint LIST
yang sama**, bukan endpoint detail per-id. Ini konsisten dengan §2.3: hanya endpoint scored yang
membawa nilai asli, jadi meski yang ingin disegarkan cuma satu jadwal, yang dipanggil tetap daftar
scored (di-`filterNotNull()` lalu di-`processAkmResponse` untuk seluruh halaman pertama).

### 4.3 Race nilai lama vs nilai baru — `scoreReady` guard

`AkmScoreDetailPage` membaca dari `Flow` Room (`viewmodel.detailAkm(akmId)`, sumber
`AkmDao.get(id): Flow<AkmSchedule>`) yang memancarkan nilai tersimpan **seketika**, sebelum
`refreshScoreFromServer()` selesai. Tanpa penahan, siswa akan melihat nilai lama berkedip lalu berubah.
Flag `scoreReady` + `refreshTick` (`AkmScoreDetailPage.kt:55-59, 158-166, 387-397`) menahan tampilan ke
"memuat" sampai response server (sukses **atau gagal**) selesai, baru menggambar nilai final:

```kotlin
// app/src/main/java/id/diskola/app/pages/akm/AkmScoreDetailPage.kt:387-397
private suspend fun refreshScoreFromServer() {
    try {
        fetchScored()
    } finally {
        scoreReady = true
        refreshTick.value = refreshTick.value + 1
    }
}
```

`finally` memastikan penahan lepas walau offline — nilai lama di Room tetap ditampilkan daripada
selamanya menampilkan "…".

### 4.4 Tidak ada observer `WorkInfo` yang memicu refetch nilai otomatis

Perlu diluruskan satu asumsi lagi dari instruksi tugas: **tidak ditemukan** kode yang mengamati
`WorkInfo` dari `AkmUploader` untuk otomatis memicu refetch nilai begitu upload sukses. Observer
`WorkInfo` yang ada di `AkmScoreDetailPage` (baris 251-262, 283-294, 315-326) semuanya mengamati worker
**download soal/pembahasan** (`AkmDownloader`/`AkmExplanationDownloader`) untuk menangani kegagalan
unduhan — bukan `AkmUploader`, dan bukan untuk refresh nilai. Setelah upload, satu-satunya jalur
menuju nilai baru adalah §4.1/§4.2 (pembukaan halaman/pull-to-refresh), bukan reaksi otomatis terhadap
selesainya worker upload.

---

## 5. Worker `AkmExplanationDownloader` — detail penuh

File: `app/src/main/java/id/diskola/app/worker/AkmExplanationDownloader.kt` (158 baris).

### 5.1 Trigger

Dienqueue oleh `AkmViewModel.downloadPembahasanSoal(akmId)`:

```kotlin
// app/src/main/java/id/diskola/app/pages/akm/AkmViewModel.kt:539-566
fun downloadPembahasanSoal(akmId: Int): UUID {
    val workRequest = OneTimeWorkRequestBuilder<AkmExplanationDownloader>()
        .setInputData(workDataOf("id" to akmId))
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
        .addTag("akm_explanation_downloader_$akmId")
        .build()
    WorkManager.getInstance(context)
        .enqueueUniqueWork("akm_explanation_downloader_$akmId", ExistingWorkPolicy.KEEP, workRequest)
    return workRequest.id
}
```

Dipanggil dari `AkmScoreDetailPage` pada dua titik state (`AkmScoreDetailPage.kt:280-295` status
`AKM_STATUS_DOWNLOADED` — otomatis dipicu begitu status ini tercapai **jika** `hasExplain` true, dan
`:302-329` tombol "Sinkronisasi Pembahasan" pada status `AKM_STATUS_SCORED`, yang menjalankan
`downloadSoal()` (unduh soal) dulu — worker pembahasan baru jalan setelah soal (`AkmDownloader`)
selesai dan status berubah jadi `DOWNLOADED`). `ExistingWorkPolicy.KEEP` berarti kalau worker dengan
tag yang sama sudah antre/berjalan, permintaan baru diabaikan (tidak dobel).

Prasyarat sebelum tombol ini bahkan muncul: `data.schedule.show_score && hasExplain` harus `true`
(§3). Jadi worker ini **tidak pernah** dipicu untuk jadwal yang server-nya belum menyatakan
`has_explain = true`.

### 5.2 Endpoint yang dipanggil

```kotlin
// app/src/main/java/id/diskola/app/worker/AkmExplanationDownloader.kt:43
val data = api.downloadExamSchoolExplanation(id, if (akm.schedule.gov_schedule) 1 else 0).data
```

```kotlin
// app/src/main/java/id/diskola/app/api/ApiService.kt:939-943
@GET("mobile/app/learning/akm/exam-schedules-scored/{id}/explains")
suspend fun downloadExamSchoolExplanation(
    @Path("id") id: Int,
    @Query("gov_schedule") isGovSchedule: Int
): AkmExplanationResponse
```

Ini mengonfirmasi endpoint yang disinggung dokumen lain (`exam-schedules-scored/{id}/explains`) memang
persis endpoint yang dipakai worker ini — **bukan** `akm/scored` (yang untuk daftar nilai) dan bukan
endpoint terpisah untuk AKM non-sekolah (tidak ada varian `.../akm/exam-schedules-scored/.../explains`
untuk `exam_type != SCHOOL`; worker ini hanya dipakai untuk jadwal ujian sekolah/AKM biasa lewat jalur
yang sama — tryout punya halaman pembahasannya sendiri, `AkmExplanationPage`, di luar cakupan file ini).

### 5.3 Format data pembahasan — **per-soal, bukan satu file gabungan**

```kotlin
// app/src/main/java/id/diskola/app/pages/akm/AkmModels.kt:104-112
data class AkmExplanationResponse(val data: List<AkmExplanationData> = emptyList())

data class AkmExplanationData(
    val id: Int = 0,
    @NullToEmptyString val file_path: String = "",
    @NullToEmptyString val created_at: String = "",
    @NullToEmptyString val updated_at: String = "",
    val question: AkmQuestion = AkmQuestion()
)
```

Response adalah **array**, satu elemen per soal (`question.id` + `question.exam_instruction_id`
mengidentifikasi soal yang mana), masing-masing membawa `file_path`-nya sendiri (biasanya URL PDF/
gambar pembahasan soal itu — lihat contoh di `docs/api/04-pembayaran-klaspay-ppob-akm.md:832-858`).
**Tidak ada file gabungan** untuk seluruh ujian; setiap soal punya pembahasannya sendiri, dan
`file_path` di response **langsung dipakai sebagai URL** — worker ini **tidak mendownload file media
ke penyimpanan lokal** (berbeda dari `AkmDownloader` yang mengunduh gambar soal/jawaban ke
`filesDir`). `file_path` hasil API disimpan **apa adanya** (bisa berupa URL remote) ke Room:

```kotlin
// app/src/main/java/id/diskola/app/worker/AkmExplanationDownloader.kt:46-53
data.forEach { explanationData ->
    if (explanationData.file_path.isNotEmpty()) {
        db.akm().updateQuestionExplanation(
            explanationData.question.id,
            explanationData.question.exam_instruction_id,
            explanationData.file_path
        )
    }
    downloadProgress++
    showProgress(akm, numQuestion, downloadProgress)
    db.akm().insert(akm.schedule.apply { download_progress = downloadProgress })
}
```

### 5.4 Mapping ke Room — kolom yang diisi, dan yang TIDAK diisi

Ada method `downloadImage()` di file worker ini (`AkmExplanationDownloader.kt:74-86`) yang **tidak
pernah dipanggil** di `doWork()` — kode mati yang tersisa dari copy-paste `AkmDownloader`.

Kolom yang benar-benar terisi di `AkmQuestionTable` (`AkmEntities.kt:238-249`):

| Kolom | Diisi worker ini? | Keterangan |
|---|---|---|
| `explanation_file_path` | **Ya** | Satu-satunya kolom yang diubah, lewat query UPDATE (bukan INSERT ulang seluruh baris) |
| `file_path` (soal, bukan pembahasan) | Tidak | Sudah diisi `AkmDownloader` sebelumnya |
| `score`, `answered`, `answer_essay` | Tidak | Di luar tanggung jawab worker ini |

Query update-nya spesifik dua kolom kunci (`id` dan `instruction_id`), bukan `id` saja:

```kotlin
// app/src/main/java/id/diskola/app/pages/akm/AkmDao.kt:22-23
@Query("update akm_question set explanation_file_path = :explanationFilePath where id = :questionId and instruction_id = :instId")
suspend fun updateQuestionExplanation(questionId: Int, instId: Int, explanationFilePath: String)
```

Perhatikan: parameter kedua yang dikirim worker adalah `explanationData.question.exam_instruction_id`
(§5.3), sedangkan kolom yang dicocokkan DAO adalah `instruction_id` — **bukan** `exam_instruction_id`
(yang merupakan kolom lain, dipakai untuk keperluan berbeda di `AkmQuestionTable`). Ini valid *hanya
selama* `exam_instruction_id` dari response API kebetulan berisi nilai yang sama dengan
`instruction_id` lokal (id instruksi/wacana) — sebuah asumsi implisit yang tidak divalidasi di kode.
**Poin untuk didalami sebelum porting 1:1 ke repo baru** (lihat §8).

### 5.5 Perbandingan progres/state dengan `AkmDownloader`

Sama persis dengan pola `AkmDownloader` (soal, `AkmDownloader.kt`):

| Aspek | `AkmDownloader` (soal) | `AkmExplanationDownloader` (pembahasan) |
|---|---|---|
| Status awal | `AKM_STATUS_NEW`→`AKM_STATUS_DOWNLOADING` (1) | `status apa pun`→`AKM_STATUS_DOWNLOADING` (1) — **menimpa status yang sama dengan worker soal** |
| Status sukses | `AKM_STATUS_DOWNLOADED` (2) | `AKM_STATUS_EXPLAINED` (6) |
| Status gagal | kembali ke `AKM_STATUS_NEW` (0) | kembali ke `AKM_STATUS_NEW` (0) — **regresi ganjil**: pembahasan gagal mengembalikkan status ujian ke seolah-olah belum pernah dikerjakan sama sekali, bukan ke `AKM_STATUS_SCORED` (5) tempat ia semula |
| Progress notification | per-soal, `download_progress` dihitung `numQuestion = akm.exams.sumOf { it.num_question }` | identik, notifikasi berbeda teks ("pembahasan" vs "soal") |
| Unduh file media | Ya (gambar/audio/video soal & jawaban ke `filesDir`) | **Tidak** — hanya menyalin URL `file_path` apa adanya |
| Constraint | `NetworkType.CONNECTED` | sama |
| `ExistingWorkPolicy` saat enqueue | `KEEP` | `KEEP` |

Poin "status gagal kembali ke `AKM_STATUS_NEW`" pada §baris di atas patut dicatat sebagai potensi bug
alur lama (§2, `docs/rules-global.md`): kegagalan sinkronisasi pembahasan membuat jadwal yang sudah
`SCORED` terlihat kembali seperti belum dikerjakan — tombol aksi di `AkmScoreDetailPage` untuk status
`AKM_STATUS_NEW` (`:239-265`) akan muncul lagi ("Sinkronisasi Pembahasan" dengan alur unduh **soal**
dari awal), bukan kembali menampilkan nilai yang sudah ada. Ini **tidak diperbaiki** dalam riset ini —
sesuai `docs/rules-global.md` §2, dicatat di sini untuk dikonfirmasi ke user sebelum direplikasi/
diperbaiki di repo baru.

### 5.6 Error handling

```kotlin
// app/src/main/java/id/diskola/app/worker/AkmExplanationDownloader.kt:66-71
} catch (e: Exception) {
    Timber.e(e)
    showDownloadFail(akm, e.message.orEmpty())
    db.akm().insert(akm.schedule.apply { status = AkmStatus.AKM_STATUS_NEW })
    Result.failure(workDataOf("message" to e.message))
}
```

- **Tidak ada retry otomatis oleh WorkManager** — `Result.failure()` (bukan `Result.retry()`), padahal
  `setBackoffCriteria` terpasang di builder (baris konfigurasi tidak berefek karena tidak pernah
  `Result.retry()`-kan). Bandingkan dengan `AkmUploader` yang eksplisit `Result.retry()` sampai
  `MAX_UPLOAD_ATTEMPTS`. Jadi kegagalan sinkronisasi pembahasan **tidak** dicoba ulang oleh sistem;
  perlu aksi manual pengguna (lihat §6).
- Notifikasi kegagalan (`showDownloadFail`) memakai `NotificationCompat.PRIORITY_MIN` — nyaris tidak
  terlihat pengguna kecuali membuka notification shade.
  `WorkInfo.State.FAILED` diobservasi balik oleh `AkmScoreDetailPage` (baris 283-294) yang menampilkan
  `alert()` dialog dengan pesan dari `workInfo.outputData.getString("message")` — jalur ini **hanya
  aktif selama halaman detail masih terbuka** saat worker gagal; kalau pengguna sudah menutup halaman,
  satu-satunya jejak kegagalan adalah notifikasi prioritas rendah + status yang kembali ke `NEW`.

---

## 6. Retry nilai yang gagal dimuat

Tidak ada mekanisme retry otomatis (auto-retry berkala) untuk fetch nilai yang gagal karena network
error. Yang ada:

1. **Silent fallback ke Room** — `fetchScored()`/`refreshScoreFromServer()` di `AkmScoreDetailPage`
   membungkus kegagalan dalam `try { fetchScored() } finally { scoreReady = true; ... }` (§4.3).
   Kalau fetch gagal (mis. offline), tidak ada pesan error ke pengguna sama sekali di halaman detail —
   nilai lama dari Room tetap ditampilkan seolah itu final, tanpa indikator "gagal menyegarkan".
2. **`loadAkmScored`/`loadUjianSchoolScored` set `errorString`** (`AkmViewModel.kt:250-258, 420-428`)
   bila exception terjadi — tapi khusus jalur ini `errorString.postValue` **dikomentari** untuk
   `loadAkmScored` (baris 252, memakai pesan generik "Belum terdapat data" di baris berikutnya, bukan
   error asli) sementara `loadUjianSchoolScored` (baris 422) langsung memposting `e.message` asli.
   Tidak konsisten antar dua jalur (AKM murni vs ujian sekolah) — poin lain untuk dikonfirmasi user
   sebelum disamakan di repo baru.
3. **Retry manual = pull-to-refresh** di `AkmScorePage` (`SwipeRefreshLayout`, §4.1) — ini satu-satunya
   tombol/aksi eksplisit yang tersedia untuk pengguna mencoba ulang fetch daftar nilai.
4. **Tidak ada tombol "Retry" khusus** di `AkmScoreDetailPage` untuk kegagalan `fetchScored()` —
   halaman itu tidak menampilkan UI error state untuk kegagalan refresh nilai sama sekali (§4.3
   sengaja menahan render sampai `finally`, tapi tidak membedakan hasil sukses vs gagal secara visual).
   Menutup dan membuka ulang halaman adalah satu-satunya cara memicu `fetchScored()` lagi.
5. **AkmUploader** (bukan fetch nilai, tapi terkait) punya retry otomatis WorkManager
   (`Result.retry()` sampai `MAX_UPLOAD_ATTEMPTS = 10`, `AkmUploader.kt:130-140, 189`) — pola retry
   *ini* ada, tapi untuk **upload jawaban**, bukan untuk *membaca* nilai.

---

## 7. Rekomendasi untuk repo baru (Hilt + Compose + Room)

### 7.1 State nilai ke Compose

```kotlin
sealed interface ScoreUiState {
    data object Loading : ScoreUiState
    data class Scored(
        val examScore: Double,
        val perSubjectScores: List<SubjectScore>,
        val hasExplanation: Boolean,
        val explanationStatus: ExplanationSyncStatus, // NotStarted/Syncing(progress)/Synced/Failed
    ) : ScoreUiState
    data object PendingReview : ScoreUiState   // sudah diunggah, isAssessed masih false
    data class Error(val message: String, val cachedScore: Scored?) : ScoreUiState // tetap bawa cache lama
}
```

- Bedakan eksplisit **`PendingReview`** dari **`Loading`** — ini yang tidak ada di implementasi lama
  (§6 poin 1): pengguna lama tidak pernah tahu apakah nilai "sedang dimuat" atau "memang belum
  dinilai guru". State ini dipetakan dari `status == UPLOADED/FINISHED` + belum ada `exam_score` valid.
- `Error` tetap membawa `cachedScore` (dari Room) supaya UI bisa menampilkan nilai lama + banner
  "gagal menyegarkan, coba lagi" — bukan diam-diam menyembunyikan kegagalan seperti versi lama (§6
  poin 1), dan bukan pula menghapus nilai lama saat refresh gagal.
- Expose lewat `StateFlow<ScoreUiState>` per `scheduleId`, hasil `combine` dari Flow Room (nilai
  tersimpan, selalu tersedia) dengan sinyal hasil network call (event, bukan berlanjut) — pola yang
  meniru guard `scoreReady` (§4.3) tapi dibuat eksplisit sebagai state, bukan flag boolean tersembunyi.

### 7.2 Perlu polling berkala?

**Rekomendasi: tidak perlu WorkManager periodic murni untuk polling nilai**, tapi PERLU salah satu
dari retry-on-resume/retry-on-open yang eksplisit — ini pengecualian yang sah terhadap larangan
fetch-on-open di `docs/rules-global.md` §1.1, dengan alasan yang sama seperti kode lama sudah
tuliskan sendiri (§4.2):

- Nilai esai bisa berubah **kapan saja** dari sisi guru, tanpa sinyal apa pun ke client (tidak ada
  push/WebSocket dari backend). Ini termasuk kategori "status transaksi yang sedang pending" di daftar
  pengecualian resmi §1.1 — status kelulusan penilaian adalah status pending yang statusnya
  ditentukan pihak lain (guru), sama seperti status pembayaran ditentukan payment gateway.
- **Kenapa BUKAN WorkManager periodic:** granularitas minimum `PeriodicWorkRequest` adalah 15 menit,
  dan Android membatasi ketat proses background — polling berkala untuk data yang perubahannya jarang
  (satu kali per submission, ditentukan jadwal guru menilai, bisa berjam-jam/berhari) memboroskan
  baterai & kuota untuk ribuan siswa yang mayoritas hasil pollingnya "belum berubah". Ini persis pola
  yang `docs/rules-global.md` §1 minta dihindari (fetch tanpa perlu).
- **Pola yang direkomendasikan: retry-on-resume dengan TTL pendek, dibatasi HANYA pada state
  `PendingReview`.** Konkretnya:
  1. Saat membuka layar detail nilai untuk jadwal berstatus `PendingReview`
     (uploaded tapi belum `isAssessed`), fetch sekali (persis pola lama §4.2).
  2. Kalau hasil fetch masih `PendingReview`, JANGAN pasang timer di background. Cukup fetch lagi
     saat: (a) layar detail itu dibuka lagi (`onResume`/navigasi balik), (b) pengguna menekan
     pull-to-refresh eksplisit di layar itu, (c) opsional: satu retry ringan dengan `TTL` singkat
     (mis. 60 detik) HANYA selama layar itu masih di foreground (`LaunchedEffect` dengan `while
     (isActive)` + `delay`, dibatalkan begitu Composable di-dispose) — mirip
     `AkmScorePage.onResume()`'s `countdownJob` yang membatalkan diri di `onPause()`
     (`AkmScorePage.kt:292-308`), bukan `WorkManager` yang tetap hidup di luar lifecycle layar.
  3. Begitu status jadi `Scored`, hentikan seluruh mekanisme retry untuk jadwal itu — tidak ada alasan
     mem-polling data yang sudah final.
- Untuk daftar nilai (bukan detail satu jadwal), **pertahankan pola lama**: tanpa fetch-on-open,
  murni cache-first + pull-to-refresh + boundary-callback saat cache kosong (§4.1) — daftar tidak
  butuh real-time karena pengguna akan membuka detail satu-per-satu untuk melihat progres penilaian.

### 7.3 Repository method yang disarankan

```kotlin
interface AkmRepository {
    /** Cache-first: emit nilai tersimpan Room segera, lalu 1x refresh jaringan jika [refresh] true. */
    fun getScore(scheduleId: Int, refresh: Boolean = false): Flow<ScoreUiState>

    /** Dipanggil eksplisit oleh pull-to-refresh atau retry-on-resume; tidak auto-invoked oleh init{}. */
    suspend fun refreshScore(scheduleId: Int): Result<Unit>

    /** Cermin AkmSettings.hasExplain — baca dari Room, tidak fetch jaringan sendiri. */
    fun hasExplanation(scheduleId: Int): Flow<Boolean>

    /** Enqueue worker sinkronisasi pembahasan; KEEP policy spt lama agar tidak dobel. */
    fun syncExplanation(scheduleId: Int): Flow<ExplanationSyncStatus> // Idle/Syncing(progress)/Done/Failed(msg)

    /** Retry manual saat syncExplanation gagal — replace, bukan keep, supaya percobaan baru tidak diblok oleh Result.failure() lama. */
    suspend fun retrySyncExplanation(scheduleId: Int): Result<Unit>
}
```

Catatan desain berdasarkan temuan §5-6:

- **`syncExplanation` harus punya retry policy yang jelas** (unlike `AkmExplanationDownloader` lama
  yang `Result.failure()` tanpa retry otomatis, §5.6). Kalau tetap pakai WorkManager, pertimbangkan
  `Result.retry()` dengan backoff seperti `AkmUploader`, atau minimal expose state `Failed` yang jelas
  ke UI supaya ada tombol "Coba Lagi" eksplisit — bukan notifikasi prioritas rendah yang mudah
  terlewat.
- **Jangan mereplikasi regresi status "gagal → kembali ke NEW"** (§5.5) tanpa konfirmasi user — di
  repo baru, kegagalan sync pembahasan sebaiknya membuat state terpisah (`ExplanationSyncStatus.Failed`)
  yang tidak menyentuh status keseluruhan jadwal (`Scored` tetap `Scored`), supaya nilai yang sudah ada
  tidak "hilang" dari UI hanya karena unduhan pembahasan gagal.
- **Format pembahasan per-soal** (§5.3): entity Room pembahasan sebaiknya tetap 1 baris per
  `(question_id, instruction_id)` seperti `AkmQuestionTable.explanation_file_path`, bukan digabung
  jadi satu dokumen — mengikuti bentuk asli response API (`AkmExplanationData` per soal).
- Perhatikan potensi mismatch `exam_instruction_id` vs `instruction_id` (§5.4) — sebelum porting query
  update itu apa adanya ke repo baru, verifikasi ke backend/tim API apakah kedua field itu memang
  selalu sama, atau field yang benar untuk dicocokkan adalah `instruction_id` murni.

---

## 8. Hal yang perlu dikonfirmasi ke user (bukan diputuskan sepihak)

Sesuai `docs/rules-global.md` §2, poin-poin berikut adalah alur lama yang terlihat janggal namun
**tidak diubah** dalam riset ini:

1. `AkmExplanationDownloader` gagal → status kembali ke `AKM_STATUS_NEW` (0), bukan `AKM_STATUS_SCORED`
   (5) — berpotensi menyembunyikan nilai yang sudah ada (§5.5).
2. `loadAkmScored` menelan pesan error asli dan selalu menampilkan "Belum terdapat data" (§6 poin 2),
   sedangkan `loadUjianSchoolScored` menampilkan `e.message` asli — dua jalur nilai yang mestinya
   paralel berperilaku beda.
3. Query `updateQuestionExplanation` mencocokkan kolom `instruction_id` dengan nilai yang dikirim dari
   `exam_instruction_id` di response (§5.4) — perlu verifikasi apakah ini asumsi yang valid atau
   celah laten.
4. Tidak ada state UI eksplisit untuk kegagalan `fetchScored()` di `AkmScoreDetailPage` (§6 poin 4) —
   apakah ini perilaku yang diinginkan dipertahankan (silent fallback ke cache) atau harus ditambah
   indikator error di repo baru.
