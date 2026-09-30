# Dokumentasi Teknis — Mekanisme Download Soal & Penyimpanan Lokal (AKM)

> Dokumen ini adalah rujukan **mekanisme/alur teknis**, bukan pemetaan
> UI-to-data-key (itu sudah ada di `docs/features/asesmen-akm-*.md`). Tujuannya
> adalah menjadi bahan reimplementasi fitur Download Soal AKM di repo baru
> (Hilt + Jetpack Compose + Room). Setiap klaim disertai file:baris dari kode
> lama sebagai bukti.
>
> File yang dibaca menyeluruh: `worker/AkmDownloader.kt`,
> `pages/akm/AkmDao.kt`, `pages/akm/AkmEntities.kt`, `pages/akm/AkmSettings.kt`,
> `pages/akm/SettingAkmCache.kt`, `pages/akm/AkmModels.kt`,
> `pages/akm/AkmViewModel.kt` (fungsi `downloadSoal`, `refreshPenaltySetting`,
> `checkPasswordUjianSchool`), `pages/akm/AkmDetailPage.kt` &
> `AkmScoreDetailPage.kt` (pemicu & observer worker), `utils/FileUtils.kt`,
> `db/MemoryDB.kt`, dan `pages/akm/AkmQuestionMediaBinder.kt` (konsumsi
> `local_path`, bukan bagian dari proses download tapi menjelaskan *mengapa*
> field itu diisi begitu).

---

## 1. Kapan & bagaimana download dipicu

### 1.1 Kondisi pemicu

Download hanya ditawarkan ke user ketika status lokal jadwal (`AkmTable.status`,
lihat `AkmStatus` di `AkmEntities.kt:8-16`) bernilai `AKM_STATUS_NEW` (0).
Tombol "Sinkronisasi Soal" hanya muncul pada cabang `when` ini:

```kotlin
// AkmDetailPage.kt:192-219
AkmStatus.AKM_STATUS_NEW -> {
    binding.btnAction.apply {
        visibility = View.VISIBLE
        text = "Sinkronisasi Soal"
        setOnClickListener {
            ...
            val workId = viewmodel.downloadSoal(akmId)
            WorkManager.getInstance(applicationContext)
                .getWorkInfoByIdLiveData(workId)
                .observe(this@AkmDetailPage) { workInfo: WorkInfo? -> ... }
        }
    }
}
```

Pola yang **identik** dipakai di `AkmScoreDetailPage.kt` (baris 239-263, 280-296,
302-323) untuk keperluan mengunduh **pembahasan/soal AKM juga** ketika status
`AKM_STATUS_NEW`, `AKM_STATUS_DOWNLOADED` (dengan `hasExplain`), atau
`AKM_STATUS_SCORED` — halaman nilai memakai worker `AkmDownloader` yang sama
untuk kasus tertentu (bukan hanya `AkmExplanationDownloader`).

Catatan penting: di dalam `AkmDownloader.doWork()` sendiri, pemeriksaan status
`AKM_STATUS_NEW` **dikomentari** (baris 39-40: `//if (akm.schedule.status !=
AkmStatus.AKM_STATUS_NEW) return Result.success()`). Artinya guard status
sesungguhnya hanya ada di sisi UI (tombol hanya tampil saat status NEW); worker
itu sendiri tidak menolak permintaan re-download pada status lain — ini
perlu diperhatikan saat reimplementasi karena kalau worker dipanggil ulang
secara tidak sengaja pada status DOWNLOADED, ia tetap akan mengunduh ulang
seluruh soal.

### 1.2 Pembuatan WorkRequest

`AkmViewModel.downloadSoal()`:

```kotlin
// AkmViewModel.kt:510-537
fun downloadSoal(akmId: Int): UUID {
    val workRequest = OneTimeWorkRequestBuilder<AkmDownloader>()
        .setInputData(workDataOf("id" to akmId))
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .setBackoffCriteria(
            BackoffPolicy.LINEAR,
            WorkRequest.MIN_BACKOFF_MILLIS,
            TimeUnit.MILLISECONDS
        )
        .addTag("akm_downloader_$akmId")
        .build()
    val workId = workRequest.id

    WorkManager.getInstance(context)
        .enqueueUniqueWork("akm_downloader_$akmId", ExistingWorkPolicy.KEEP, workRequest)

    return workId
}
```

Poin desain:
- **Input data**: hanya satu key, `"id"` (Int) — id jadwal AKM (`akm.id` /
  `AkmTable.id`, sama dengan id dari API).
- **Constraint**: `NetworkType.CONNECTED` saja — tidak ada `setRequiresCharging`,
  `setRequiresStorageNotLow`, dll. WorkManager sendiri akan menunda eksekusi
  worker sampai perangkat online (tapi tidak menjamin unmetered/WiFi).
- **Backoff**: `LINEAR` dengan `WorkRequest.MIN_BACKOFF_MILLIS` (~10 detik) —
  **namun lihat §4**: backoff ini pada praktiknya tidak pernah terpakai
  karena worker tidak pernah mengembalikan `Result.retry()`.
- **Unique work**: key `"akm_downloader_$akmId"`, policy `KEEP` — bila worker
  untuk `akmId` yang sama sudah antre/berjalan, permintaan baru **diabaikan**
  (tidak menggantikan yang lama). Ini mencegah dobel-klik tombol menghasilkan
  dua worker paralel yang menulis ke Room secara bersamaan.
- Nilai kembalian `workId` (UUID) dipakai UI untuk `observe`
  `getWorkInfoByIdLiveData(workId)` guna mendeteksi kegagalan (lihat §4).

Worker yang sama (`AkmDownloader`) dipakai untuk 3 kombinasi endpoint (AKM
reguler / Ujian Sekolah / Try Out) — dibedakan lewat data yang sudah tersimpan
di Room (`akm.schedule.exam_type`, `is_school_scope`), bukan lewat parameter
worker terpisah. Lihat §2.1.

---

## 2. Alur worker step-by-step (`AkmDownloader.doWork()`)

Referensi dependency worker (didapat lewat service-locator manual, **bukan**
Hilt/Dagger-Android injection langsung ke constructor):

```kotlin
// AkmDownloader.kt:19-26
class AkmDownloader(private val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    private val db by lazy { component.memoryDB }
    private val api by lazy { component.apiService }
    private val pref by lazy { component.preference }
    private val fileUtils by lazy { component.fileUtils }
    private val anyAdapter by lazy { component.moshi.adapter(Any::class.java) }
    ...
}
```
`component` adalah top-level accessor ke `AppComponent` Dagger
(`di/AppComponent.kt:172-181`) — worker konstruktornya adalah yang standar
tanpa-argumen dari WorkManager (dibuat via reflection oleh
`WorkerFactory` default), sehingga dependency diambil manual lewat `lazy`,
bukan lewat `@AssistedInject` seperti pola `HiltWorker`.

### 2.1 Ambil entity lokal & tentukan endpoint

```kotlin
// AkmDownloader.kt:28-54
val id = inputData.getInt("id", 0)
if (id < 1) return Result.success()
val akm = db.akm().getSingle(id) ?: return Result.failure()
val numQuestion = akm.exams.sumOf { it.num_question }

db.akm().insert(akm.schedule.apply { status = AkmStatus.AKM_STATUS_DOWNLOADING })
showProgress(akm, numQuestion, 0)

var downloadProgress = 0
val data = (
    if (akm.schedule.exam_type == ExamType.SCHOOL) {
        if (akm.schedule.is_school_scope)
            api.downloadSoalUjianSchool(id, if (akm.schedule.gov_schedule) 1 else 0)
        else
            api.downloadSoalAkm(id)
    } else {
        api.downloadSoalUjianTryout(id)
    }).data
db.akm().insert(akm.schedule.apply { student_exam_id = data.studentExam.id })
```

- `db.akm().getSingle(id)` adalah query `@Transaction` yang mengembalikan
  `AkmSchedule` (jadwal + daftar exam relasinya) — lihat §5.
- Status segera diubah ke `AKM_STATUS_DOWNLOADING` **sebelum** request
  jaringan dimulai, supaya UI (yang mengobservasi `db.akm().get(id)` sebagai
  `Flow`) langsung pindah ke progress bar.
- **Endpoint yang dipakai** (dikonfirmasi di `ApiService.kt`):

  | Kondisi | Endpoint | Method |
  |---|---|---|
  | `exam_type == SCHOOL && is_school_scope` | `mobile/app/learning/akm/student/exam-school/{id}/download?gov_schedule={0/1}` | `GET` (`ApiService.kt:926-930`) |
  | `exam_type == SCHOOL && !is_school_scope` | `mobile/app/learning/akm/student/exam/{id}/download` | `GET` (`ApiService.kt:919-920`) |
  | `exam_type == TRYOUT` (lainnya) | `mobile/app/learning/try-out/student/training/{id}/download` | `GET` (`ApiService.kt:945-946`) |

  Ketiganya mengembalikan bentuk response yang sama: `AkmDownloadResponse` →
  `data: AkmDownloadData { id, exams: List<AkmExams>, studentExam: StudentExam }`
  (`AkmModels.kt:114-141`). Field `exams` dianotasi `@ObjectToList` — artinya
  backend kadang mengirim objek (bukan array) untuk daftar exam, dan ada
  Moshi adapter kustom (`di/modules/ObjectToList` — di luar cakupan baca kali
  ini) yang menormalkannya jadi List.
- `studentExam.id` disalin ke `AkmTable.student_exam_id` **segera setelah**
  response diterima, sebelum soal-soal di-insert — ini adalah id sesi ujian
  siswa di server yang dipakai nanti untuk submit jawaban.

### 2.2 Mapping per-exam → `AkmExamsTable`

```kotlin
// AkmDownloader.kt:57-73
data.exams.forEach { exam ->
    if (exam.numberOfQuestions > 0 && exam.instructions.any { it.questions.isNotEmpty() }) {
        val existingExam = akm.exams.firstOrNull { it.id == exam.id }
        val mapped = AkmExamsTable(id, exam)
        db.akm().insertExam(
            mapped.copy(
                assessment_category = mapped.assessment_category.ifBlank { existingExam?.assessment_category.orEmpty() },
                assessment_period = mapped.assessment_period.ifBlank { existingExam?.assessment_period.orEmpty() },
                finished = existingExam?.finished ?: mapped.finished,
                show_child = existingExam?.show_child ?: mapped.show_child,
            )
        )
    }
    ...
}
```

Constructor sekunder `AkmExamsTable(schedule_id, exam: AkmExams)`
(`AkmEntities.kt:154-167`) memetakan field API → kolom Room:

| Kolom Room (`AkmExamsTable`) | Sumber API (`AkmExams`) | Transformasi |
|---|---|---|
| `local_id` | – | `0`, auto-generate PK Room |
| `id` | `exam.id` | langsung |
| `schedule_id` | (parameter `schedule_id`, bukan dari body) | id `AkmTable` induk, dioper manual dari `AkmDownloader` |
| `name` | `exam.name` | langsung |
| `type` | `exam.type` | langsung |
| `grade` | `exam.grade` (`Any?`) | `exam.grade.toString().toIntOrNull() ?: 0` — API kadang kirim grade sebagai String/Int campuran |
| `author_nip` | `exam.author?.nip` | `.orEmpty()` |
| `author_name` | `exam.author?.name` | `.orEmpty()` |
| `num_question` | `exam.numberOfQuestions` | langsung (nama field beda: camelCase API → snake_case Room) |
| `score` | `exam.score` | langsung |
| `assessment_category` / `assessment_period` | `exam.assessment_category` / `assessment_period` | langsung, lalu di-*merge* dengan baris lama (lihat di bawah) |
| `finished`, `show_child` | – | default `false` dari constructor, **ditimpa** oleh nilai lama (lihat di bawah) |

**Merge dengan baris lama** (bukan bagian dari constructor, tapi dilakukan
eksplisit di `AkmDownloader` lewat `.copy()`): karena `insertExam` memakai
`OnConflictStrategy.REPLACE` pada key unique `(id, schedule_id)`
(`AkmEntities.kt:136`), insert baru akan **menimpa total** baris lama kalau
tidak di-merge manual. Field yang sengaja dipertahankan dari baris lama bila
respons baru kosong/default: `assessment_category`, `assessment_period`
(hanya kalau field baru blank), serta `finished` dan `show_child` (state UI
lokal — apakah exam ini sudah dikerjakan / expand-collapse di layar — yang
tidak pernah dikirim server sama sekali, jadi wajib dipertahankan dari
`existingExam` atau hilang setiap kali sinkronisasi ulang).

Filter penting: exam yang `numberOfQuestions <= 0` atau semua instruksinya
tidak punya soal (`instructions.any { it.questions.isNotEmpty() }` bernilai
`false`) **tidak di-insert ke `akm_exams` sama sekali** — namun instruksi &
soal di dalamnya tetap diproses di loop berikutnya (lihat catatan di §2.3,
loop `exam.instructions.forEach` ada di luar blok `if` ini).

### 2.3 Mapping instruksi → `AkmInstructionTable`

```kotlin
// AkmDownloader.kt:75-79
exam.instructions.forEach { inst ->
    if (inst.questions.isNotEmpty())
        db.akm().insertInstruction(AkmInstructionTable(exam.id, inst).apply {
            num_question = inst.questions.size
        })
    ...
}
```

Constructor `AkmInstructionTable(examId, data: AkmInstruction)`
(`AkmEntities.kt:208-216`):

| Kolom Room | Sumber API | Catatan |
|---|---|---|
| `id` | `data.id` | PK, **bukan** auto-generate — id instruksi dari server dipakai langsung sebagai PK lokal |
| `exam_id` | parameter `examId` | FK logis ke `AkmExamsTable.id` (tanpa `@ForeignKey` eksplisit di Room, lihat §5) |
| `instruction` | `data.instruction` | langsung |
| `description` | `data.description` | langsung |
| `sequence` | `data.sequence` | langsung |
| `random` | `data.isRandom` | langsung |
| `num_question` | – | default `0` dari constructor, **ditimpa manual** setelah construct: `.apply { num_question = inst.questions.size }` |

Instruksi tanpa soal (`inst.questions.isEmpty()`) tidak di-insert ke
`akm_instruction`, tapi acakan/downloadnya di bawah tetap jalan untuk semua
`exam.instructions` tanpa syarat itu (soal kosong → loop `questions.forEach`
otomatis tidak melakukan apa-apa).

### 2.4 Pengacakan soal & jawaban (bukan penyimpanan, tapi terjadi *sebelum* insert)

```kotlin
// AkmDownloader.kt:81-86
val questions = if (!inst.isRandom) inst.questions else inst.questions.shuffled()
```

Bila `AkmInstruction.isRandom == true`, urutan soal diacak **satu kali di
sisi client saat download**, lalu urutan hasil acakan itulah yang disimpan ke
Room (tabel tidak punya kolom `sequence` untuk soal — urutan tampil = urutan
insert / urutan baca `id` — lihat DAO `instQuestion` yang tidak ber-`ORDER BY`
eksplisit selain relasi Room default). Artinya **acakan bersifat permanen**
untuk siswa itu sampai data lokal dihapus/diunduh ulang — bukan diacak ulang
setiap kali soal dibuka.

Jawaban punya pengacakan serupa di §2.6, dan untuk tipe `PAIR` ada pengacakan
tambahan yang lebih kompleks (§2.7).

### 2.5 Mapping soal → `AkmQuestionTable`, unduh gambar soal

```kotlin
// AkmDownloader.kt:88-109
questions.forEach { question ->
    val isPairImageQuestion = question.answerType == "PAIR" &&
        question.answers.firstOrNull()?.firstStatement.isNullOrEmpty()
    if (question.image.isNotEmpty()) {
        if (!question.image.startsWith("http")) {
            question.image = BuildConfig.ASSETS_URL.trimEnd('/') + "/" + question.image.trimStart('/')
        }
        question.image = downloadImage("akm-exam${exam.id}", "q${question.id}.jpg", question.image)
    }
    db.akm().insertQuestion(AkmQuestionTable(inst.id, question))
    ...
}
```

Constructor `AkmQuestionTable(instruction_id, data: AkmQuestion)`
(`AkmEntities.kt:252-271`):

| Kolom Room | Sumber API | Transformasi |
|---|---|---|
| `id` | `data.id` | PK langsung dari server |
| `instruction_id` | parameter | FK logis |
| `question` | `data.question` | langsung (HTML mentah dari server) |
| `type` (Int, lihat `AkmAnswerType`) | **diturunkan** dari `data.answerType` (String) + heuristik pada `data.answers` | Lihat tabel derivasi di bawah |
| `type_label` | `data.answerType` | disimpan mentah juga (String asli sebelum konversi ke Int) |
| `file_path` | `data.image` (**sudah** hasil `downloadImage()` di atas — path lokal atau URL remote fallback) | lihat §2.9 |
| `score`, `answered`, `answer_essay`, `explanation_file_path`, `exam_instruction_id` | – | default (`0`/`false`/`""`), diisi belakangan oleh alur lain (skoring, jawab-soal, worker pembahasan) di luar cakupan download |

Derivasi `type` (Int) dari `answerType` (String) + bentuk data jawaban:

| `data.answerType` | Kondisi tambahan | `AkmAnswerType` hasil |
|---|---|---|
| `"MULTIPLE CHOICE"` | – | `ANSWER_MULTIPLE_CHOICE_SINGLE_CORRECT` (0) |
| `"STATEMENT"` | `answers[0].showFalse == true` | `ANSWER_MULTIPLE_CHOICE_MULTIPLE_CORRECT_TABLE` (5) |
| `"STATEMENT"` | `answers.size == 1` | `ANSWER_MULTIPLE_CHOICE_TRUE_FALSE` (3) |
| `"STATEMENT"` | lainnya | `ANSWER_MULTIPLE_CHOICE_MULTIPLE_CORRECT` (4) |
| `"ESSAY"` | – | `ANSWER_ESSAY` (2) |
| `"PAIR"` | `answers[0].firstStatement` kosong/null | `ANSWER_PAIRING_IMAGE` (7) |
| `"PAIR"` | lainnya | `ANSWER_PAIRING` (6) |
| `"SHORT_ESSAY_NUM"` | – | `ANSWER_ESSAY_NUM` (8) |
| `"SHORT_ESSAY_WORD"` | – | `ANSWER_ESSAY_WORD` (9) |
| lainnya | – | fallback `ANSWER_ESSAY` (2) |

Gambar soal: URL relatif (`!startsWith("http")`) diprefiks dengan
`BuildConfig.ASSETS_URL`, lalu diunduh lewat `downloadImage()` (§2.9) ke
folder `akm-exam{examId}/q{questionId}.jpg` di `filesDir` internal app. Nama
file dibentuk dari `exam.id` + `question.id` — **bukan** hash konten, jadi
unduhan ulang untuk soal yang sama akan menimpa file dengan nama yang sama
(atau di-skip kalau file lama sudah > 0 byte, lihat §2.9).

### 2.6 Mapping media (audio/video) → `AkmQuestionMediaTable`

```kotlin
// AkmDownloader.kt:111-135
question.media.sortedWith(compareBy({ it.sequence }, { it.id })).forEach { media ->
    var remoteUrl = media.url
    if (remoteUrl.isNotEmpty() && !remoteUrl.startsWith("http")) {
        remoteUrl = BuildConfig.ASSETS_URL.trimEnd('/') + "/" + remoteUrl.trimStart('/')
    }
    var localPath = ""
    val mediaType = media.type.lowercase()
    val canCacheFile = remoteUrl.isNotEmpty() &&
        (mediaType == "audio" ||
            (mediaType == "video" && AkmQuestionMediaBinder.youtubeId(remoteUrl) == null))
    if (canCacheFile) {
        val fallback = if (mediaType == "video") "mp4" else "mp3"
        val ext = remoteUrl.substringBefore('?').substringAfterLast('.', fallback)
            .lowercase().takeIf { it.length in 2..5 } ?: fallback
        localPath = downloadFile("akm-exam${exam.id}", "q${question.id}_m${media.id}.$ext", remoteUrl)
    }
    db.akm().insertMedia(AkmQuestionMediaTable(question.id, media.copy(url = remoteUrl), localPath))
}
```

Ini menjawab langsung poin tugas #2: **ya, `AkmDownloader` sendiri yang
mengunduh berkas media** (audio/video) ke penyimpanan lokal — bukan
`AkmQuestionMediaBinder`. `AkmQuestionMediaBinder` (dipakai saat soal
ditampilkan) hanya **membaca** `local_path` yang sudah diisi worker ini untuk
memutuskan mau memutar dari file lokal atau streaming dari `url` remote
(`AkmQuestionMediaBinder.kt:258-277`, `isCached()`/`playSource()`).

Aturan pengunduhan media:
- **Audio** selalu dicoba diunduh (`mediaType == "audio"`).
- **Video** hanya diunduh kalau **bukan** video YouTube
  (`AkmQuestionMediaBinder.youtubeId(remoteUrl) == null`) — video YouTube
  selalu diputar via WebView/embed player online, tidak pernah di-cache
  sebagai file lokal.
- Ekstensi file ditentukan dari ekstensi asli URL (dibersihkan dari query
  string), fallback `mp4` untuk video dan `mp3` untuk audio bila ekstensi
  tidak valid (panjang di luar 2–5 karakter).
- Nama file: `q{questionId}_m{mediaId}.{ext}` di folder `akm-exam{examId}`.
- `AkmQuestionMediaTable` constructor (`AkmEntities.kt:341-348`) memetakan
  `id`, `type`, `url` (yang sudah dinormalisasi jadi absolute URL — **catatan**:
  field API `url` ditimpa dengan `remoteUrl` lewat `media.copy(url = remoteUrl)`
  sebelum masuk constructor, supaya kolom `url` di Room selalu absolute meski
  gagal di-cache), `sequence` — langsung dari `AkmQuestionMedia` API model.
  Kolom `local_path` diisi terpisah lewat parameter constructor kedua
  (`localPath: String = ""`), **bukan** bagian dari model API sama sekali.
- Bila unduhan gagal (`canCacheFile == false` atau `downloadFile()`
  mengembalikan string kosong), `local_path` disimpan **kosong**
  (`""`) — bukan URL remote. Ini beda perlakuan dengan gambar (lihat §2.9),
  karena `AkmQuestionMediaBinder.playSource()` sudah punya fallback eksplisit
  ke `media.url` ketika `local_path` kosong/tidak valid.

### 2.7 Mapping jawaban → `AkmAnswerTable`, pengacakan PAIR

Untuk tipe `PAIR`, dilakukan pengacakan pasangan **sebelum** insert jawaban:

```kotlin
// AkmDownloader.kt:138-155
if (question.answerType == "PAIR") {
    val originalSecondStatementById = question.answers.associate { it.id to it.secondStatement }
    val originalSecondFilePathById = question.answers.associate { it.id to it.secondFilePath }
    val randIds = question.answers.map { it.id }.shuffled()
    val newList = mutableListOf<AkmAnswer>()
    question.answers.forEachIndexed { index, akmAnswer ->
        val selectedId = randIds[index]
        newList.add(akmAnswer.copy(
            secondStatement = originalSecondStatementById[selectedId].orEmpty(),
            secondFilePath = originalSecondFilePathById[selectedId].orEmpty(),
            selected_id = selectedId
        ))
    }
    question.answers = newList
}
```

Mekanismenya: sisi kiri (`firstStatement`/`firstFilePath`) tetap pada urutan
asal per baris jawaban, tapi sisi kanan (`secondStatement`/`secondFilePath`)
"ditukar" mengikuti `selected_id` yang diacak dari id jawaban lain di soal
yang sama. `selected_id` inilah yang menjadi kunci pencocokan benar/salah
nanti saat siswa menjodohkan — bukan urutan tampil.

Lalu urutan jawaban (per soal) diacak lagi kalau `inst.isRandom`:

```kotlin
// AkmDownloader.kt:157-161
val answers = if (!inst.isRandom) question.answers else question.answers.shuffled()
```

Pembersihan tag: `<p>` di awal `answer`/`firstStatement`/`secondStatement`
dihapus (`replaceFirst`, hanya kemunculan pertama) sebelum disimpan
(baris 164-166) — kemungkinan artefak editor rich-text di CMS backend.

Unduhan gambar jawaban (3 slot independen: `filePath`, `firstFilePath`,
`secondFilePath`) memakai pola yang sama seperti gambar soal
(`downloadImage`, §2.9), dengan nama file:
- `a{questionId}_{answerId}.jpg` untuk `filePath`
- `a{questionId}_{answerId}_1.jpg` untuk `firstFilePath`
- `a{questionId}_{secondFileOwnerId}_2.jpg` untuk `secondFilePath`, dengan
  `secondFileOwnerId` = `selected_id` (bukan `id` sendiri!) khusus untuk
  `isPairImageQuestion` — supaya nama file gambar sisi-kanan konsisten dengan
  jawaban yang sudah ditukar posisinya di atas, bukan dengan id barisnya
  sendiri (baris 194-207).

Constructor `AkmAnswerTable(questionId, sequence, data: AkmAnswer)`
(`AkmEntities.kt:303-317`):

| Kolom Room | Sumber API | Catatan |
|---|---|---|
| `local_id` | – | `0`, auto-generate PK |
| `id` | `data.id` | id jawaban dari server (bukan PK — lihat §5, unique index gabungan) |
| `question_id` | parameter | FK logis |
| `sequence` | parameter `sequence` | **index posisi setelah shuffle**, bukan `id` — inilah urutan tampil final |
| `answer` | `data.answer` | sudah dibersihkan `<p>` |
| `file_path` | `data.filePath` | hasil `downloadImage()` |
| `is_true` | `data.isTrue` | langsung |
| `show_false` | `data.showFalse` | langsung |
| `first_statement` | `data.firstStatement` | sudah dibersihkan `<p>` |
| `first_file_path` | `data.firstFilePath` | hasil `downloadImage()` |
| `second_statement` | `data.secondStatement` | hasil swap PAIR (§ di atas) + dibersihkan `<p>` |
| `second_file_path` | `data.secondFilePath` | hasil swap PAIR + `downloadImage()` |
| `selected_id` | `data.selected_id` | hasil pengacakan PAIR (0 untuk tipe non-PAIR) |
| `selected` | – | default `false`, diisi belakangan saat siswa memilih jawaban (di luar cakupan download) |

Progress dan penyimpanan progress terjadi **per soal** (bukan per jawaban),
tepat setelah semua jawaban satu soal selesai diproses:

```kotlin
// AkmDownloader.kt:219-222
downloadProgress++
showProgress(akm, numQuestion, downloadProgress)
db.akm().insert(akm.schedule.apply { download_progress = downloadProgress })
```

### 2.8 Transaksi Room: **per-item insert, bukan satu transaksi besar**

Ini poin penting untuk reimplementasi: seluruh proses di atas **tidak**
dibungkus `@Transaction` / `db.runInTransaction {}` sama sekali. Setiap
`db.akm().insert...()` adalah operasi Room mandiri (masing-masing DAO method
hanya `@Insert`, tanpa `@Transaction` tambahan di titik pemanggilan). Artinya:

- Bila proses terhenti di tengah (exception, proses di-kill), Room akan
  menyimpan **sebagian** data yang sudah sempat di-insert sebelum titik
  kegagalan — tidak ada rollback otomatis.
- Ini disengaja secara implisit oleh desain progress-per-soal: `status` dan
  `download_progress` di `AkmTable` sendiri ditulis berulang kali di tengah
  proses (bukan cuma di akhir), justru supaya UI bisa menampilkan progress
  granular. Konsekuensinya, kalau reimplementasi ingin all-or-nothing per
  unduhan, perlu keputusan desain baru (lihat §7 rekomendasi).
- Unduhan file media/gambar (operasi I/O ke `filesDir`, bukan ke Room) jelas
  tidak bisa masuk transaksi Room sama sekali — jadi kombinasi "gambar sudah
  ke-download tapi baris Room belum ke-insert" (atau sebaliknya) mungkin
  terjadi bila proses mati persis di antara kedua langkah itu.

### 2.9 Detail unduhan file (gambar vs media)

Dua fungsi helper dengan perilaku fallback yang **sengaja berbeda**:

```kotlin
// AkmDownloader.kt:249-287
private suspend fun downloadFile(folderName: String, fileName: String, url: String): String {
    val file = prepareFile(folderName, fileName)
    if (file.length() > 0) return file.absolutePath   // sudah lengkap dari percobaan sebelumnya
    fileUtils.downloadFile(url, file)
    return if (file.length() > 0) file.absolutePath
    else { file.delete(); "" }                          // gagal -> string kosong
}

private suspend fun downloadImage(folderName: String, fileName: String, url: String): String {
    val file = prepareFile(folderName, fileName)
    if (file.length() > 0) return file.absolutePath
    fileUtils.downloadImage(url, file)
    return if (file.length() > 0) file.absolutePath
    else { file.delete(); url }                         // gagal -> URL remote asli
}
```

- **Resume/skip idempoten**: kalau file di `filesDir` untuk nama itu sudah
  ada dan ukurannya `> 0`, unduhan **dilewati sepenuhnya** dan path lokal
  lama langsung dipakai. ini bukan resume byte-range HTTP, melainkan
  "skip kalau file lengkap" — sinkronisasi ulang jadi jauh lebih cepat kalau
  sebagian besar aset sudah pernah terunduh sebelumnya (mis. retry setelah
  network putus di tengah).
- **Gagal unduh gambar** → dikembalikan **URL remote asli** (bukan string
  kosong), supaya Glide (image loader yang dipakai UI, di luar cakupan file
  ini) tetap bisa menampilkan gambar dari jaringan saat perangkat online.
  Mengembalikan path file 0-byte akan gagal baik online maupun offline.
- **Gagal unduh media (audio/video)** → dikembalikan **string kosong**,
  bukan URL. Ini berbeda karena `AkmQuestionMediaBinder` punya logika
  fallback eksplisit sendiri (`playSource()`) yang mengecek `isCached()`
  dulu sebelum memutuskan pakai `local_path` atau `url` — jadi kosong berarti
  "belum di-cache, pemutar akan pakai `media.url` langsung".
- `fileUtils.downloadFile`/`downloadImage` (`utils/FileUtils.kt:101-118`)
  memanggil `apiService.download(uri)` (Retrofit `@Streaming @GET` full-URL,
  didefinisikan di `ApiService.kt`, di luar file yang diminta dibaca kali
  ini) dan menulis byte array penuh ke file — **bukan** streaming
  chunk-by-chunk ke disk, sehingga seluruh isi file ditahan di memori
  sebelum ditulis. Untuk file besar (video) ini berisiko `OutOfMemoryError`
  — poin yang layak diperbaiki di reimplementasi (streaming langsung ke
  `OutputStream`).
- Folder penyimpanan: `context.filesDir/akm-exam{examId}/...` — internal
  storage privat app (bukan `getExternalFilesDir`), otomatis terhapus saat
  app di-uninstall, tidak butuh runtime permission.

---

## 3. Bagaimana progress dilaporkan ke UI

Progress **tidak** dilaporkan lewat `setProgressAsync()` / `WorkInfo.progress`
milik WorkManager. Sumber kebenaran progress adalah **kolom Room**
(`AkmTable.download_progress` dan `AkmTable.status`), yang di-*update*
berulang kali sepanjang `doWork()` (§2.7) dan dibaca UI lewat query reaktif:

```kotlin
// AkmDao.kt:101-103
@Transaction
@Query("select * from akm where id = :id")
fun get(id: Int): Flow<AkmSchedule>
```

```kotlin
// AkmViewModel.kt:451
fun detailAkm(akmId: Int) = db.akm().get(akmId)
```

`AkmDetailPage` men-`collectLatest` Flow ini (baris 161 dst.) dan setiap kali
baris `akm` berubah (termasuk setiap increment `download_progress`), `when
(data.schedule.status)` dievaluasi ulang: pada status `AKM_STATUS_DOWNLOADING`
progress bar dan label teks diperbarui langsung dari `data.schedule
.download_progress` (`AkmDetailPage.kt:223-232`, pola identik di
`AkmScoreDetailPage.kt:267-278`).

`WorkInfo` (via `getWorkInfoByIdLiveData(workId)`) **hanya** dipakai untuk satu
hal: mendeteksi **kegagalan** worker (`WorkInfo.State.FAILED`) dan membaca
`outputData.getString("message")` untuk ditampilkan sebagai alert (§4). Tidak
ada penggunaan `WorkInfo.State.SUCCEEDED`/`RUNNING`/progress dari WorkManager
sama sekali — status "berhasil" dideteksi cukup dari Room (`status` berubah
jadi `AKM_STATUS_DOWNLOADED`, lewat Flow yang sama).

Notifikasi sistem (`NotificationCompat`) juga ditampilkan paralel
(`showProgress()`, `AkmDownloader.kt:297-331`) sebagai progress notification
(`setProgress(numQuestion, progress, false)`) — berguna kalau app di
background/worker jalan meski activity tidak difokuskan, tapi ini kanal
terpisah dari mekanisme observe Flow di atas.

---

## 4. Penanganan error, retry, partial data

```kotlin
// AkmDownloader.kt:36, 231-236
return try {
    ... // seluruh proses §2
    Result.success()
} catch (e: Exception) {
    Timber.e(e)
    showDownloadFail(akm, e.message.orEmpty())
    db.akm().insert(akm.schedule.apply { status = AkmStatus.AKM_STATUS_NEW })
    Result.failure(workDataOf("message" to e.message))
}
```

Poin kunci:

1. **Tidak ada retry otomatis WorkManager yang efektif.** `BackoffCriteria`
   di-set saat build request (§1.2), tapi backoff WorkManager hanya berlaku
   ketika worker mengembalikan `Result.retry()`. Worker ini **selalu**
   mengembalikan `Result.failure()` pada exception apa pun (network putus,
   parsing error, dll) — jadi WorkManager menandai pekerjaan **gagal
   permanen**, tidak dijadwalkan ulang secara otomatis sama sekali. Konfigurasi
   backoff yang ada praktis dead code.
2. **Data partial dibiarkan, tidak dibersihkan.** Baris `akm_exams`,
   `akm_instruction`, `akm_question`, `akm_answer`, `akm_question_media`, dan
   file yang sudah sempat terunduh sebelum exception **tidak** dihapus/
   di-rollback (konsisten dengan §2.8 — tidak ada transaksi). Yang dilakukan
   hanya mengembalikan `AkmTable.status` ke `AKM_STATUS_NEW` — sehingga saat
   user menekan tombol "Sinkronisasi Soal" lagi, seluruh proses `doWork()`
   berjalan ulang dari awal (fetch API lagi, lalu insert ulang tiap baris
   dengan `OnConflictStrategy.REPLACE`, dan file yang sudah lengkap otomatis
   di-skip berkat pengecekan `file.length() > 0` di §2.9). Efeknya: retry
   manual berperilaku seperti resume parsial untuk file, tapi selalu re-fetch
   penuh untuk data JSON/Room.
3. **User diberi tahu lewat dua kanal**:
   - Notifikasi sistem "Gagal mendownload soal ..." dengan pesan
     `e.message` (`showDownloadFail()`, `AkmDownloader.kt:333-367`), berisi
     `PendingIntent` yang membuka kembali `AkmDetailPage` untuk jadwal terkait.
   - Bila activity masih terbuka, observer `WorkInfo` di UI menangkap
     `WorkInfo.State.FAILED`, membaca `outputData.getString("message")`, lalu
     memanggil `alert(msg = message, abortLabel = "")` dan mengembalikan
     tombol "Sinkronisasi Soal" ke `VISIBLE` (`AkmDetailPage.kt:210-217`,
     `AkmScoreDetailPage.kt` pola serupa) — karena `status` sudah balik ke
     `AKM_STATUS_NEW` di Room, Flow `get(id)` otomatis memicu `when` untuk
     menampilkan ulang tombol itu (mekanisme reaktif yang sama dengan §3).
4. **Retry oleh user** = menekan tombol yang sama lagi → `downloadSoal()`
   dipanggil ulang. Karena `enqueueUniqueWork(..., ExistingWorkPolicy.KEEP,
   ...)` (§1.2), retry hanya efektif kalau worker sebelumnya benar-benar
   sudah selesai (gagal/sukses) — kalau masih `ENQUEUED`/`RUNNING`, permintaan
   baru diabaikan diam-diam (tidak ada pesan ke user bahwa permintaan
   diabaikan).
5. Bila `id < 1` pada input data, worker langsung `Result.success()` tanpa
   melakukan apa pun (guard pertahanan, `AkmDownloader.kt:31`). Bila entity
   `akm` tidak ditemukan di Room (`getSingle(id) == null`), worker
   `Result.failure()` tanpa pesan/tanpa notifikasi gagal (baris 34) — kasus
   ini seharusnya tidak pernah terjadi dari alur UI normal karena
   `AkmDetailPage` sudah memuat data yang sama sebelum tombol ditampilkan.

---

## 5. Skema Room lengkap (entity AKM)

```
AkmTable (tabel "akm")
 PK: id (Int, = id jadwal dari server)
 Index: id (unique), is_active, is_school_scope
 kolom kunci: status, download_progress, student_exam_id, exam_type,
              is_school_scope, gov_schedule, password
   │
   │ 1—N  (relasi logis: schedule_id == AkmTable.id; TANPA @ForeignKey Room,
   │       dihubungkan lewat @Relation di data class gabungan AkmSchedule)
   ▼
AkmExamsTable (tabel "akm_exams")
 PK: local_id (Int, autoGenerate)
 Kolom lain: id (id exam dari server, BUKAN PK — bisa duplikat local_id
             kalau di-insert ulang tanpa conflict match)
 Index: (id, schedule_id) unique  <- ini kunci konflik REPLACE yang sesungguhnya
   │
   │ 1—N  (relasi logis: exam_id == AkmExamsTable.id)
   ▼
AkmInstructionTable (tabel "akm_instruction")
 PK: id (Int, = id instruksi dari server — dipakai langsung sbg PK lokal)
 Kolom lain: exam_id (FK logis), num_question, answered
 Tanpa index tambahan selain PK
   │
   │ 1—N  (relasi logis: instruction_id == AkmInstructionTable.id)
   ▼
AkmQuestionTable (tabel "akm_question")
 PK: id (Int, = id soal dari server)
 Kolom lain: instruction_id (FK logis), type, file_path, answered,
             answer_essay, explanation_file_path, exam_instruction_id
 Tanpa index tambahan selain PK
   │
   ├── 1—N (relasi logis: question_id == AkmQuestionTable.id)
   │   ▼
   │  AkmAnswerTable (tabel "akm_answer")
   │   PK: local_id (Int, autoGenerate)
   │   Kolom lain: id (id jawaban dari server, bukan PK), sequence (posisi
   │               tampil final setelah shuffle), selected_id, selected
   │   Index: (id, question_id) unique <- kunci konflik REPLACE
   │
   └── 1—N (relasi logis: question_id == AkmQuestionTable.id)
       ▼
      AkmQuestionMediaTable (tabel "akm_question_media")
       PK: id (Int, = id media dari server)
       Kolom lain: question_id (FK logis), type, url, local_path, sequence
       Index: question_id (non-unique)

AkmSettingTable (tabel "akm_settings")  — TIDAK berelasi ke AkmTable
 PK: key (String)
 Kolom lain: value (Int)
 Generic key-value store, lihat §6.
```

Catatan penting soal integritas referensial: **tidak ada satupun
`@ForeignKey` Room** yang dideklarasikan di antara tabel-tabel di atas — semua
relasi (`AkmSchedule`, `AkmExamInstruction`, `InstuctionQuestion`,
`QuestionAnswers`) dibangun lewat anotasi `@Relation` (Room POJO gabungan,
query terpisah + join di memori oleh Room), bukan constraint DB-level. Room
tidak akan menolak insert anak yatim (mis. `akm_question` dengan
`instruction_id` yang tidak ada di `akm_instruction`), dan **tidak ada
`onDelete = CASCADE`** — menghapus baris `AkmTable` (`deleteListAkm`/`delete`)
**tidak otomatis menghapus** baris turunannya di `akm_exams`,
`akm_instruction`, `akm_question`, `akm_answer`, `akm_question_media` (perlu
dicek terpisah apakah ada pembersihan manual di tempat lain — di luar
cakupan file yang dibaca kali ini, kemungkinan berupa kebocoran data yatim
piatu bila jadwal dihapus).

PK strategy campuran juga perlu dicatat: `AkmInstructionTable`,
`AkmQuestionTable`, `AkmQuestionMediaTable` memakai **id server langsung**
sebagai PK (server dianggap sumber id unik global), sedangkan
`AkmExamsTable` dan `AkmAnswerTable` memakai **`local_id` autoGenerate**
dengan `id` server sebagai kolom biasa + unique index gabungan
`(id, <parent_fk>)`. Perbedaan ini kemungkinan karena id jawaban dari server
**bisa duplikat lintas soal berbeda** (butuh index gabungan untuk unik), dan
id exam juga bisa duplikat lintas `schedule_id` berbeda (mis. template exam
yang sama dipakai beberapa jadwal) — sedangkan id instruksi, soal, dan media
diasumsikan unik global di seluruh sistem.

---

## 6. Bagaimana data lokal dibaca ulang setelah download

Semua konsumsi data setelah download memakai `AkmDao`, dua gaya akses:

**Reaktif (Flow), untuk layar yang perlu auto-update saat data berubah**
(termasuk saat progress download berjalan):
- `get(id): Flow<AkmSchedule>` (`AkmDao.kt:101-103`) — dipakai `AkmDetailPage`
  & `AkmScoreDetailPage` untuk seluruh siklus status (NEW → DOWNLOADING →
  DOWNLOADED → ... ).
- `getExamInstruction(akmId): Flow<List<AkmExamInstruction>>` (`AkmDao.kt:112-114`)
  — daftar exam + instruksi bersarang.
- `instQuestion(instId): Flow<InstuctionQuestion>` (`AkmDao.kt:130-132`) —
  instruksi + daftar soalnya, dipakai saat mengerjakan soal.
- `questionAnswer(questionId): Flow<QuestionAnswers>` (`AkmDao.kt:144-146`) —
  satu soal + jawaban + media, unit terkecil yang di-observe halaman
  pengerjaan soal.

**One-shot (`suspend fun`), untuk pembacaan sekali pakai / kalkulasi
turunan**: `getSingle(id)`, `getExamsAsync`, `getExamInstructionAsync`,
`getInstQuestion`, `getMedia(questionId)`, `getAnswers(questionId)`,
`getSelectedAnswers`, `getStudentExamId`, `getExamIdByInstId`,
`getQuestionType`, `hasUnfinishedUjian`, `getUnfinishedUjian`,
`countAllByScope`, dll. (`AkmDao.kt` — lihat daftar lengkap, banyak di
antaranya query agregat `count`/`update` untuk state UI turunan seperti
status "selesai/belum" per exam: `setExamStatus()`, `setInstAnswered()`).

Query listing jadwal (`listAkm`, `listUjianSchool`, `listNilaiAkm`, dst.)
memakai `DataSource.Factory<Int, AkmTable>` (Paging **2**, bukan Paging 3) —
relevan sebagai catatan migrasi bila repo baru pakai Paging 3 / `PagingSource`.

---

## 7. `AkmSettingTable` — key-value store generic

Lihat isi lengkap `AkmSettings.kt` untuk komentar desain asli (bahasa
Indonesia, sangat eksplisit). Ringkasan mekanismenya:

- **Sumber data**: `GET mobile/setting-akm` (`ApiService.kt:922-923`,
  `settingAkmpenalty()`), response `ResponsePenaltyTimes.data`
  (`penaltyResponse`: `penalty_times`, `penalty_applied`, `absence_setting`,
  `exam_lock_mode`).
- **Cache TTL 12 jam**, dikelola `SettingAkmCache.refresh()`
  (`SettingAkmCache.kt:43-94`) — timestamp fetch terakhir disimpan di
  `SharedPreferences` (bukan Room), key `setting_akm_last_fetch`. Dipanggil
  dengan `force=false` dari alur biasa (presensi, buka detail AKM), dan
  `force=true` (`AkmViewModel.refreshPenaltySetting(true)`) tepat sebelum
  ujian dimulai karena `exam_lock_mode`/`penalty_applied` menentukan perilaku
  ujian yang akan berjalan dan tidak boleh pakai cache basi.
- **Penyimpanan**: setiap field global disimpan sebagai **satu baris**
  `AkmSettingTable(key, value: Int)` — `penalty_times`, `penalty_applied`
  (0/1), `exam_lock_mode` (0/1, key konstan `AkmSettings.KEY_STRICT_MODE =
  "exam_lock_mode"`), `absence_setting` (0/1). Selain field global, ada juga
  **key per-jadwal** (di-generate dinamis dari `akmId`):
  `has_explain_$akmId`, `requires_password_$akmId`, `password_checked_$akmId`
  (`AkmSettings.kt:46-48`) — dua yang terakhir diisi dari response
  `checkPasswordUjianSchool()` (`AkmViewModel.kt:465-477`), bukan dari
  `setting-akm`.
- **Sentinel `-1`**: DAO `getSettingValue(key)` mengembalikan `-1` via SQL
  `ifnull(...)` (`AkmDao.kt:176-177`) bila key belum pernah tersimpan.
  `-1` secara eksplisit dibedakan dari `0`/`1` di setiap fungsi pembaca
  `AkmSettings` (`isStrictMode`, `hasExplain`, dst.) — masing-masing punya
  default aman sendiri (mis. `isStrictMode` default **aktif** `true` demi
  keamanan ujian; `hasExplain` default **`false`** karena lebih aman untuk
  tidak menjanjikan pembahasan yang belum tentu ada).
- **Mengapa key-value generic, bukan kolom baru di `AkmTable`** — kutipan
  komentar asli (`AkmSettings.kt:37-45`):
  > "Menambah kolom berarti menaikkan versi MemoryDB, dan kedua database
  > aplikasi ini dibangun di atas berkas yang sama (`diskola.db`) dengan
  > `fallbackToDestructiveMigration()` — kenaikan versi akan menghapus
  > seluruh data lokal, termasuk jawaban ujian offline yang belum sempat
  > terunggah."

  Konteks ini sudah didokumentasikan lengkap di tempat lain sesuai instruksi
  tugas; di sini cukup dicatat sebagai **alasan** kenapa desainnya
  key-value, bukan diulang detailnya. `MemoryDB` saat ini di `version = 42`
  (`db/MemoryDB.kt:65`) dengan >50 entity lintas fitur berbagi satu file
  database — risiko itu nyata dan relevan untuk keputusan desain di repo
  baru (§8, tidak perlu mengulang pola ini karena repo baru = database
  terpisah).

---

## 8. Rekomendasi untuk repo baru (Hilt + Compose + Room)

### 8.1 Repository interface

```kotlin
interface AkmRepository {
    // Trigger + observe dalam satu Flow: emit status terkini setiap ada perubahan.
    fun downloadQuestions(scheduleId: Int): Flow<DownloadProgress>

    // Query reaktif pengganti AkmDao.get()/getExamInstruction()/instQuestion()/questionAnswer()
    fun observeSchedule(scheduleId: Int): Flow<AkmScheduleWithExams>
    fun observeInstructionQuestions(instructionId: Int): Flow<InstructionWithQuestions>
    fun observeQuestionAnswers(questionId: Int): Flow<QuestionWithAnswersAndMedia>
}

sealed interface DownloadProgress {
    data object Idle : DownloadProgress
    data class Downloading(val current: Int, val total: Int) : DownloadProgress
    data object Success : DownloadProgress
    data class Failed(val message: String, val cause: Throwable? = null) : DownloadProgress
}
```

Perbedaan penting dari kode lama: progress **tidak** ditulis ke kolom entity
domain (`AkmTable.status`/`download_progress`) sebagai satu-satunya sumber
kebenaran UI, karena itu mencampur *state persisten jadwal* dengan *state
transient proses download*. Lebih baik dipisah: entity tetap punya
`status`/`downloadedQuestionCount` untuk keperluan lain (mis. resume setelah
app di-kill di tengah proses, atau menampilkan progress lama saat halaman
dibuka ulang tanpa observe worker aktif), tapi UI Compose yang sedang
aktif mengamati proses berjalan sebaiknya mengamati **`WorkInfo` via
`setProgressAsync()`** (lihat 8.2) sebagai sumber real-time, dan
`observeSchedule()` sebagai sumber "state terakhir yang persisten" untuk
kasus halaman dibuka tanpa proses aktif — kombinasi keduanya menghindari
"progress bar diam" bila Flow Room di-throttle/batch oleh Room di baris
banyak.

### 8.2 `HiltWorker` menggantikan worker manual

```kotlin
@HiltWorker
class AkmDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val api: AkmApi,
    private val akmDao: AkmDao,
    private val mediaDownloader: MediaDownloader, // ganti FileUtils.downloadFile/Image
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val scheduleId = inputData.getInt(KEY_SCHEDULE_ID, -1)
        if (scheduleId < 0) return Result.failure()
        return try {
            // ... mapping sama seperti §2, tapi:
            // - progress dilaporkan lewat setProgressAsync(workDataOf(KEY_CURRENT to i, KEY_TOTAL to n))
            // - insert per-exam/instruction/question/answer dibungkus db.withTransaction { }
            //   per unit yang masuk akal (mis. satu instruksi = satu transaksi), bukan satu
            //   transaksi raksasa untuk seluruh jadwal (supaya progress tetap granular tapi
            //   tidak ada baris "setengah jadi" per instruksi)
            Result.success()
        } catch (e: IOException) {
            Result.retry() // <-- benar-benar pakai retry() supaya BackoffCriteria efektif,
                            //     beda dari kode lama yang selalu failure()
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR to e.message))
        }
    }
}
```

Perlu `HiltWorkerFactory` didaftarkan di `Application` (`Configuration
.Provider`) — ini menggantikan pola `component.memoryDB`/`component
.apiService` (service locator global) dengan constructor injection murni,
sehingga worker jadi unit-test-able tanpa perlu `component` singleton palsu.

Rekomendasi tambahan dari temuan §4: bedakan **retryable** (`IOException`/
timeout jaringan → `Result.retry()`, biarkan `BackoffCriteria` bekerja) vs
**non-retryable** (parsing error, 4xx dari server, password salah →
`Result.failure()` langsung) — kode lama memperlakukan semua exception sama.

### 8.3 Skema Room yang konsisten (tanpa berbagi file database)

Pertahankan bentuk relasi 1-N yang sama (Schedule → Exam → Instruction →
Question → {Answer, Media}), tapi:
- Deklarasikan `@ForeignKey` eksplisit dengan `onDelete = CASCADE` dari
  Question→Instruction→Exam→Schedule, supaya menghapus jadwal otomatis
  membersihkan seluruh soal turunannya (mengatasi celah §5).
  Sertakan `ON DELETE CASCADE` juga untuk `Answer`/`Media`→`Question`.
- Konsisten memilih PK: kalau id server dijamin unik per tabel (kemungkinan
  besar iya untuk instruction/question/media di backend baru), pakai id
  server langsung sebagai PK tanpa `local_id` ganda seperti `AkmExamsTable`/
  `AkmAnswerTable` lama — sederhanakan ke satu strategi PK di seluruh
  entity AKM supaya tidak membingungkan (kode lama memakai dua strategi
  berbeda karena alasan historis, lihat catatan di §5).
- Database AKM berdiri sendiri (`AkmDatabase` miliknya sendiri, atau minimal
  bukan menumpang file `.db` fitur lain) — migrasi Room (`Migration`) bisa
  dipakai bebas tanpa risiko fitur lain, jadi setelan seperti
  `exam_lock_mode`/`penalty_applied` **boleh** jadi kolom biasa di tabel
  `Schedule` atau tabel `ExamSettings` terpisah (1-1 relasi ke Schedule)
  alih-alih key-value generic — desain key-value di kode lama murni mitigasi
  risiko migrasi database bersama, bukan kebutuhan domain.
- Simpan `sequence` hasil shuffle sebagai kolom eksplisit di tabel Question
  juga (bukan cuma di Answer seperti kode lama) supaya urutan tampil soal
  tidak bergantung pada urutan insert row.

### 8.4 State ke Compose UI

```kotlin
@HiltViewModel
class AkmDownloadViewModel @Inject constructor(
    private val repository: AkmRepository,
    private val workManager: WorkManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DownloadUiState>(DownloadUiState.Idle)
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    fun startDownload(scheduleId: Int) {
        viewModelScope.launch {
            repository.downloadQuestions(scheduleId)
                .map { it.toUiState() }
                .collect { _uiState.value = it }
        }
    }
}

sealed interface DownloadUiState {
    data object Idle : DownloadUiState
    data class Downloading(val progress: Int, val total: Int) : DownloadUiState
    data object Success : DownloadUiState
    data class Error(val message: String) : DownloadUiState
}
```

Composable observer:

```kotlin
val state by viewModel.uiState.collectAsStateWithLifecycle()
when (state) {
    is DownloadUiState.Idle -> DownloadButton(onClick = { viewModel.startDownload(scheduleId) })
    is DownloadUiState.Downloading -> LinearProgressIndicator(progress = state.progress / state.total.toFloat())
    is DownloadUiState.Success -> StartExamButton(...)
    is DownloadUiState.Error -> ErrorBanner(state.message, onRetry = { viewModel.startDownload(scheduleId) })
}
```

Ini menggantikan pola lama `WorkManager.getWorkInfoByIdLiveData(workId)
.observe(activity) { ... }` yang digabung manual di Activity — di Compose,
`WorkManager` tetap dipakai sebagai *engine* eksekusi background, tapi
dijembatani ke `Flow`/`StateFlow` (mis. via `workManager
.getWorkInfoByIdFlow(workId)` yang tersedia di WorkManager KTX modern) supaya
lifecycle-aware secara idiomatis lewat `collectAsStateWithLifecycle()`, tanpa
observer manual per Activity seperti `AkmDetailPage`/`AkmScoreDetailPage`
lama yang mendaftarkan `observe()` baru setiap kali tombol ditekan (berisiko
duplikat observer bila `when` dievaluasi ulang beberapa kali oleh Flow yang
sama, meski `LiveData` di Java biasanya idempotent per-lifecycle-owner).

---

## Ambiguitas / hal yang tidak sepenuhnya jelas dari kode yang dibaca

1. `apiService.download(uri)` (dipanggil dari `FileUtils.downloadFile`/
   `downloadImage`) tidak dibaca definisinya secara eksplisit dari
   `ApiService.kt` di sesi ini (di luar daftar file yang diminta) — asumsi
   `@Streaming @GET` full-URL berdasarkan pola penggunaan (menerima URL
   absolut sebagai parameter), tapi anotasi Retrofit persisnya belum
   diverifikasi baris per baris.
2. Tidak ditemukan kode pembersihan (cascade delete manual) untuk baris anak
   (`akm_exams`/`akm_instruction`/`akm_question`/`akm_answer`/
   `akm_question_media`) saat `AkmDao.delete(id)`/`deleteListAkm(id)`
   dipanggil — kemungkinan ada di file lain yang di luar cakupan tugas ini
   (mis. `OnKlasDbUtil.kt`), tidak dikonfirmasi.
3. `di/modules/ObjectToList` (adapter Moshi untuk field `@ObjectToList val
   exams`) tidak dibaca isinya — disebut sebagai konteks saja karena relevan
   dengan parsing response download, bukan bagian dari file yang diminta.
