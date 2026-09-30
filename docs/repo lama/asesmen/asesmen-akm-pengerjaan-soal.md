# Dokumentasi Presisi — Alur Pengerjaan Soal Asesmen (AKM)

> Dokumen ini dibuat khusus untuk tim desain (dan AI design generator) agar TIDAK menambah field
> data yang tidak ada di API/DB asli. Setiap baris pada tabel "Elemen UI" dibuktikan dari kode
> binding asli (data-binding `@{...}` di XML, atau `binding.viewId... = ...` di Kotlin).
> Path file bersifat relatif terhadap root repo `android-portal/`.
>
> **Arsitektur data yang perlu dipahami dulu:**
> Respons API (`AkmQuestion`, `AkmAnswer`, `AkmQuestionMedia`, dst — didefinisikan di
> `AkmModels.kt` / `AkmEntities.kt`) **tidak pernah dipakai langsung oleh UI**. Saat soal
> diunduh (`AkmDownloader`), setiap objek API dikonversi menjadi baris tabel Room lokal
> (`AkmQuestionTable`, `AkmAnswerTable`, `AkmQuestionMediaTable`, `AkmInstructionTable`,
> `AkmExamsTable`) lewat constructor sekunder di `AkmEntities.kt`. UI (Activity, ViewHolder,
> XML data-binding) HANYA membaca dari tabel Room ini (via `QuestionAnswers`,
> `InstuctionQuestion`, `AkmExamInstruction`, dst). Karena itu kolom "Key data" di bawah selalu
> saya tulis dalam bentuk `<field lokal Room> (dari API: <field response>)` supaya jelas asal
> muasalnya, dan `⚠️ LOKAL` untuk field yang murni hasil hitungan/state aplikasi (bukan dari API).
>
> Field yang murni hasil hitungan aplikasi (COUNT SQL, flag turunan, dsb) ditandai **⚠️ LOKAL**.
> Elemen yang teksnya tidak berasal dari data sama sekali ditandai **⚠️ HARDCODED**.

---

## 1. AkmTakeResumePage (Daftar Mapel & Tombol Kumpulkan)

Sumber: `app/src/main/java/id/diskola/app/pages/akm/AkmTakeResumePage.kt`,
layout `app/src/main/res/layout/akm_take_resume_page.xml`,
`app/src/main/res/layout/akm_take_exam_item.xml` (kartu per mapel/exam),
`app/src/main/res/layout/akm_take_instruction_item.xml` (baris per instruksi/babak).

### Elemen UI

| Elemen (label visual) | Key data (path/asal) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Judul toolbar ("Kerjakan Asesmen" / "Kerjakan Tryout" / "Pembahasan") | ⚠️ HARDCODED — dipilih dari extra Intent `examType`/`isExplanation`, bukan dari API | String literal | `AkmTakeResumePage.kt:636-643` | "Pembahasan" jika `isExplanation`; else "Kerjakan Asesmen" bila `examType==SCHOOL`, "Kerjakan Tryout" bila `TRYOUT` |
| Nama mapel per kartu (mis. "Numerasi") | `AkmExamsTable.type` (dari API: `AkmExams.type`, field JSON `exam.type`/`exams[].type`) — **tapi khusus scope sekolah field ini di-overwrite jadi literal "Asesmen"** | String (data-binding `@{item.type ?? "Asesmen"}`) | Binding: `akm_take_exam_item.xml:33`; override: `AkmTakeResumePage.kt:878` (`binding.item = item.exam.apply { if (isSchoolScope) type = "Asesmen" }`) | Jika `isSchoolScope==true` → SELALU tampil "Asesmen" (⚠️ HARDCODED, menutupi nilai asli API). Jika bukan school scope (tryout) → tampil `type` asli dari API |
| Badge status mapel ("Selesai" / "Belum selesai") | `AkmExamsTable.finished` ⚠️ LOKAL — bukan field API. Diset true oleh app saat semua instruksi mapel itu terjawab penuh (lihat DAO `setExamStatus()` & `AkmTakeResumePage.kt:187-190`), atau dipaksa `true` bila `isExplanation` (`AkmTakeResumePage.kt:877`) | Boolean → teks & warna | `akm_take_exam_item.xml:46-50` | Teks: `item.finished ? "Selesai" : "Belum selesai"`. Warna: hijau (`@color/green`) jika selesai, merah (`@color/red`) jika belum |
| Ikon chevron expand/collapse mapel | ⚠️ LOKAL — `showChild` adalah state UI lokal (kolom `AkmExamsTable.show_child`, di-toggle oleh tap), bukan dari API | Boolean → rotasi ImageView | `akm_take_exam_item.xml:58` | rotasi 0° saat terbuka, 180° saat tertutup. Klik pada nama/status/ikon men-toggle (`AkmTakeResumePage.kt:882-893`) |
| Nomor romawi instruksi (I., II., ...) | ⚠️ LOKAL — dihitung dari posisi index list (`RomanNumber.toRoman(position+1)`), bukan field API | String | `AkmTakeResumePage.kt:924`, binding `akm_take_instruction_item.xml:31` | Selalu berupa angka Romawi sesuai urutan tampil |
| Background kartu instruksi (kotak hijau vs abu) | Turunan `AkmInstructionTable.answered` & `.num_question` (lihat baris di bawah) | Drawable | `akm_take_instruction_item.xml:19` | `border_instruction_finish` bila `answered == num_question`, else `border_instruction` |
| Label status instruksi ("Selesai pengerjaan" / "Belum dikerjakan" / "Proses pengerjaan") | `AkmInstructionTable.answered` ⚠️ LOKAL (hasil `COUNT(*) FROM akm_question WHERE instruction_id=? AND answered=1`, DAO `setInstAnswered`, `AkmDao.kt:141-142`) dibandingkan dengan `AkmInstructionTable.num_question` (⚠️ LOKAL, diisi saat unduh = `inst.questions.size` dari API, `AkmDownloader.kt:78`) | String (data-binding ternary) | `akm_take_instruction_item.xml:44` | "Selesai pengerjaan" jika `answered==num_question`; "Belum dikerjakan" jika `answered==0`; else "Proses pengerjaan". Saat `isExplanation`, teks dipaksa "Lihat Pembahasan" (`AkmTakeResumePage.kt:928`, ⚠️ HARDCODED override) |
| Ikon status instruksi (jam / centang hijau / warning) | Sama seperti baris di atas | Drawable | `akm_take_instruction_item.xml:57` | `ic_check_green` (selesai) / `ic_clock` (belum) / `ic_warning_primary` (proses) |
| Progress "x / y" instruksi | `AkmInstructionTable.answered` / `AkmInstructionTable.num_question` (keduanya ⚠️ LOKAL, lihat di atas) | String | `akm_take_instruction_item.xml:75` | Format `"{answered} / {num_question}"` |
| Panah ">" di baris instruksi | ⚠️ HARDCODED — karakter statis, bukan dari data | String literal | `akm_take_instruction_item.xml:88` | Selalu tampil |
| Tombol "Kumpulkan" | ⚠️ HARDCODED — teks tombol tetap | String literal (XML `android:text`) | `akm_take_resume_page.xml:66` | `enabled` mengikuti `AkmExamsTable.finished` semua mapel (SCHOOL) atau selalu enabled (TRYOUT) — `AkmTakeResumePage.kt:766-768` |
| Tag/pill penalti (mis. "04:59") | Nilai `time`/`data` dari broadcast `CountDownService` — ⚠️ LOKAL, dihitung dari selisih waktu di `SharedPreferences`, bukan dari API respons soal | String (dikirim via `Intent.putExtra("time", ...)`) | `AkmTakeResumePage.kt:437`, service: `CountDownService.kt:76-114` | Hanya terlihat saat sistem penalti aktif (`penalty_applied` setting dari `GET mobile/setting-akm` bernilai true) DAN siswa terdeteksi keluar aplikasi/terhalang overlay |

**Catatan tambahan (bukan tabel):**
- Halaman ini memuat dialog "Ujian Sedang Berlangsung" (mode kunci aktif, tidak bisa keluar) dan dialog konfirmasi keluar/kumpulkan — semua teksnya **⚠️ HARDCODED** (`AkmTakeResumePage.kt:580-614`), tidak berasal dari field API apa pun.

---

## 2. AkmQuestionsPage (Chrome Umum Halaman Kerjakan Soal)

Sumber: `app/src/main/java/id/diskola/app/pages/akm/AkmQuestionsPage.kt`,
layout `app/src/main/res/layout/akm_questions_page.xml`.

### Elemen UI

| Elemen (label visual) | Key data | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Judul toolbar ("Kerjakan Asesmen" / "Pembahasan Soal") | ⚠️ HARDCODED, dari extra `isExplanation` | String literal | `AkmQuestionsPage.kt:270` | — |
| `page_label` ("1 / -", lalu "{pos+1} / {jumlah}") | Posisi RecyclerView (⚠️ LOKAL) / `AkmInstructionTable.num_question` (⚠️ LOKAL, lihat §1) | String | `AkmQuestionsPage.kt:318,332,347` | Diklik → membuka Dialog Pilih Nomor Soal (§5) |
| Tag status soal ("Sudah terjawab" / "Belum terjawab" / "Lihat Pembahasan" / "Belum dibahas") | Non-explanation: `AkmQuestionTable.answered` — set true saat siswa menjawab (⚠️ LOKAL, bukan field API asli, field ini murni state lokal per soal). Explanation: `AkmQuestionTable.explanation_file_path.isNotEmpty()` (path pembahasan yang diunduh) | String + drawable | `AkmQuestionsPage.kt:751-778` (fungsi `setTag`) | "Sudah terjawab" (badge biru `tag_primary`) / "Belum terjawab" (abu `tag_gray`) mode normal; "Lihat Pembahasan" (hijau `oval_green`) / "Belum dibahas" (hitam `oval_black`) mode pembahasan |
| Kartu instruksi — label "Perintah soal, deskripsi & cerita" | ⚠️ HARDCODED | String literal | `akm_questions_page.xml:102` | Selalu tampil |
| Kartu instruksi — aksi "Baca petunjuk" | ⚠️ HARDCODED | String literal | `akm_questions_page.xml:115` | Klik membuka dialog instruksi berisi `AkmInstructionTable.instruction` + `.description` (API asli: `AkmInstruction.instruction`, `AkmInstruction.description`) — lihat `AkmQuestionsPage.kt:291-293` (`Html.fromHtml(instruction)` + `Html.fromHtml(description)`) |
| RecyclerView soal (`rv_questions`) | Daftar `QuestionAnswers` (gabungan `AkmQuestionTable` + `AkmAnswerTable[]` + `AkmQuestionMediaTable[]`) | List | `AkmQuestionsPage.kt:295-393` | Snap-scroll horizontal 1 soal per halaman; ViewHolder dipilih oleh `QuestionAdapter` berdasar `question.type` (lihat §3 masing-masing tipe) |
| Tombol "Berikutnya" | ⚠️ HARDCODED | String literal | `akm_questions_page.xml:153` | `GONE` bila hanya 1 soal (`AkmQuestionsPage.kt:328`); `GONE`/disabled saat sudah di soal terakhir (`AkmQuestionsPage.kt:386-388`) |
| Pill penalti ("Terdeteksi membuka aplikasi lain\nProses ujian terjeda 5 menit") | ⚠️ HARDCODED teks; nilai timer dari `CountDownService` broadcast (⚠️ LOKAL) | String literal + dinamis | `akm_questions_page.xml:174`, `AkmQuestionsPage.kt:223-249` | Tampil saat penalti aktif |
| Menu toolbar "Kumpulkan" (`menu_ujian`) | — | MenuItem | `AkmQuestionsPage.kt:786-791` | Terlihat hanya bila `allowFinish` (semua soal instruksi ini terjawab, SCHOOL) DAN tidak sedang penalti DAN tidak ada breach lockdown |

---

## 3. Tipe Soal (per ViewHolder)

Field pembeda tipe soal ditentukan di **`AkmQuestionTable` constructor** (`AkmEntities.kt:252-271`),
memetakan field API `AkmQuestion.answerType` (String) + bentuk `AkmQuestion.answers[]` menjadi
`AkmQuestionTable.type` (Int, konstanta di `AkmAnswerType`, `AkmEntities.kt:224-235`). ViewHolder
lalu dipilih oleh `QuestionAdapter.onCreateViewHolder` (`QuestionAdapter.kt:57-105`) berdasarkan
nilai Int ini.

| `answerType` API | Kondisi tambahan pada `answers[]` | → `AkmQuestionTable.type` (Int) | → ViewHolder |
|---|---|---|---|
| `"MULTIPLE CHOICE"` | — | `0` ANSWER_MULTIPLE_CHOICE_SINGLE_CORRECT | `QuestionMultipleVh` (kartu jawaban standar, `akm_answer_choice.xml`) |
| `"STATEMENT"` | `answers[0].showFalse == true` | `5` ANSWER_MULTIPLE_CHOICE_MULTIPLE_CORRECT_TABLE | `QuestionTableVh` |
| `"STATEMENT"` | `answers.size == 1` | `3` ANSWER_MULTIPLE_CHOICE_TRUE_FALSE | `QuestionTrueFalseVh` |
| `"STATEMENT"` | selain dua kondisi di atas (banyak opsi, tanpa showFalse) | `4` ANSWER_MULTIPLE_CHOICE_MULTIPLE_CORRECT | `QuestionMultipleVh` (varian checkbox, `akm_answer_choice_check.xml`) |
| `"ESSAY"` | — | `2` ANSWER_ESSAY | `QuestionEssayVh` |
| `"PAIR"` | `answers[0].firstStatement` kosong/null | `7` ANSWER_PAIRING_IMAGE | `QuestionPairImageVh` |
| `"PAIR"` | `answers[0].firstStatement` terisi | `6` ANSWER_PAIRING | `QuestionPairVh` |
| `"SHORT_ESSAY_NUM"` | — | `8` ANSWER_ESSAY_NUM | `QuestionEssayVh` (input numerik) |
| `"SHORT_ESSAY_WORD"` | — | `9` ANSWER_ESSAY_WORD | `QuestionEssayVh` (input tanpa spasi) |
| lainnya/tidak dikenal | — | `2` ANSWER_ESSAY (fallback) | `QuestionEssayVh` |

> ⚠️ **Temuan penting:** konstanta `AkmAnswerType.ANSWER_MULTIPLE_CHOICE_SINGLE_CORRECT_IMAGE` (`1`)
> ada di kode (`AkmEntities.kt:226`) dan punya ViewHolder jawaban sendiri (`AnswerChoiceImageVh`,
> layout `akm_answer_choice_image.xml`, dipetakan di `QuestionMultipleVh.kt:35`), **tetapi
> constructor `AkmQuestionTable` di atas TIDAK PERNAH menghasilkan nilai `1`** — semua
> `"MULTIPLE CHOICE"` selalu jadi tipe `0`. Jadi secara praktik, layout `akm_answer_choice_image.xml`
> (kartu jawaban tanpa teks, gambar dipusatkan) **tidak pernah ter-render** dalam alur saat ini.
> Pilihan ganda bergambar sesungguhnya memakai layout standar `akm_answer_choice.xml`, yang
> memang sudah mendukung gambar opsional (`choice.file_path`) di sisi kanan teks. **Desainer
> jangan mendesain kartu jawaban "gambar saja tanpa label huruf" untuk pilihan ganda** kecuali
> tim engineering mengonfirmasi jalur ini diaktifkan kembali.

### 3.1 Pilihan Ganda — `QuestionMultipleVh`

Sumber: `QuestionMultipleVh.kt`, layout soal `akm_question_multiple_item.xml`, layout item jawaban
`akm_answer_choice.xml` (tipe `0`, standar/dengan gambar opsional) dan `akm_answer_choice_check.xml`
(tipe `4`, multi-jawaban/checkbox). Semua tipe dari §3 di atas kecuali TRUE_FALSE, TABLE, PAIR,
ESSAY jatuh ke ViewHolder ini (`QuestionAdapter.kt:96-105`).

#### Elemen UI — bagian soal

| Elemen | Key data | Tipe | Sumber (file:baris) | Kondisi |
|---|---|---|---|---|
| Gambar soal (`image`) | `AkmQuestionTable.file_path` (dari API: `AkmQuestion.image`) via custom attr `imageFitUrlRounded` | ImageView URL | `akm_question_multiple_item.xml:28` | `GONE` bila `file_path` kosong |
| Teks soal (`question`) | `AkmQuestionTable.question` (dari API: `AkmQuestion.question`, HTML) | TextView (di-render lewat `HtmlMathRenderer.setTextWithMath`, mendukung MathJax) | `QuestionMultipleVh.kt:141-153`; visibility XML `akm_question_multiple_item.xml:42` | `GONE` bila teks kosong |
| Media soal (`question_media` include) | `QuestionAnswers.media` (list `AkmQuestionMediaTable`, dari API `AkmQuestion.media[]`) | Include layout | `akm_question_multiple_item.xml:48-54`, binder di `QuestionMultipleVh.kt:151` | Lihat §4 |
| RecyclerView jawaban (`rv_answer`) | `QuestionAnswers.answers` (list `AkmAnswerTable`, dari API `AkmQuestion.answers[]`) | RecyclerView | `akm_question_multiple_item.xml:56-64` | — |

#### Elemen UI — item jawaban standar (`akm_answer_choice.xml`, tipe 0)

| Elemen | Key data | Tipe | Sumber (file:baris) | Kondisi |
|---|---|---|---|---|
| Label huruf (A, B, C, ...) | ⚠️ LOKAL — dihasilkan dari index posisi (`listAlphabet[position]`), bukan field API | String | `QuestionMultipleVh.kt:27,45` | — |
| Background bulat label & teks (`oval_gray`/`oval_primary`/`oval_green`) | `AkmAnswerTable.selected` (⚠️ LOKAL, state pilihan siswa) dan `.is_true` (dari API: `AkmAnswer.isTrue` — **hanya dipakai saat `isExplanation`**, dikirim lewat parameter `scored`) | Boolean → drawable | `akm_answer_choice.xml:28` | Mode normal: primer bila dipilih, abu bila tidak. Mode pembahasan (`scored=true`): hijau bila `is_true`, primer bila dipilih tapi salah, abu lainnya |
| Gambar opsi (`image`) | `AkmAnswerTable.file_path` (dari API: `AkmAnswer.filePath`) | ImageView URL | `akm_answer_choice.xml:47` | `GONE` bila kosong. Klik gambar memicu `onImageClick` (preview gambar, `QuestionMultipleVh.kt:47,57`) |
| Teks jawaban (`text_answer`) | `AkmAnswerTable.answer` (dari API: `AkmAnswer.answer`, HTML via `Html.fromHtml`) | TextView | `akm_answer_choice.xml:63` | `GONE` bila kosong. Warna putih bila dipilih/benar, hitam lainnya |
| Klik kartu jawaban | — | onClick | `QuestionMultipleVh.kt:48-50` | Memanggil `onAnswer(item.copy(selected=true))` → tersimpan sebagai jawaban tunggal (opsi lama otomatis di-unselect, lihat §Payload) |

#### Elemen UI — item jawaban checkbox (`akm_answer_choice_check.xml`, tipe 4 — multi-jawaban benar)

| Elemen | Key data | Tipe | Sumber (file:baris) | Kondisi |
|---|---|---|---|---|
| CheckBox (`label`) | `AkmAnswerTable.selected` (⚠️ LOKAL) | Boolean → checked state | `akm_answer_choice_check.xml:32` | Centang mengubah jawaban langsung (`QuestionMultipleVh.kt:67-69`), TIDAK meng-unselect opsi lain (lihat `AkmAnswerType.ANSWER_MULTIPLE_CHOICE_MULTIPLE_CORRECT` dalam daftar `multipleCorrects` di `AkmQuestionsPage.kt:861-865`) |
| Teks jawaban (`text_answer`) | `AkmAnswerTable.answer` (dari API `AkmAnswer.answer`) | TextView (HTML) | `akm_answer_choice_check.xml:45` | Warna hijau bila `scored && is_true`, hitam lainnya |
| Gambar opsi (`image`) | `AkmAnswerTable.file_path` (dari API `AkmAnswer.filePath`) | ImageView URL | `akm_answer_choice_check.xml:55` | `GONE` bila kosong |

---

### 3.2 Esai — `QuestionEssayVh`

Berlaku untuk 3 sub-tipe (`ANSWER_ESSAY`, `ANSWER_ESSAY_NUM`, `ANSWER_ESSAY_WORD`), semua memakai
layout yang sama `akm_question_essay_item.xml`, dibedakan hanya lewat `inputType` field isian.

| Elemen | Key data | Tipe | Sumber (file:baris) | Kondisi |
|---|---|---|---|---|
| Gambar soal (`image`) | `AkmQuestionTable.file_path` (dari API `AkmQuestion.image`) | ImageView URL | `akm_question_essay_item.xml:29` | `GONE` bila kosong |
| Teks soal (`question`) | `AkmQuestionTable.question` (dari API `AkmQuestion.question`) | TextView + MathJax | `QuestionEssayVh.kt:54-67` | `GONE` bila kosong |
| Media soal | `QuestionAnswers.media` (dari API `AkmQuestion.media[]`) | Include | `akm_question_essay_item.xml:53-59` | Lihat §4 |
| Field isian jawaban (`input_essay`) | `AkmQuestionTable.answer_essay` (⚠️ LOKAL — field ini murni jawaban siswa tersimpan lokal; TIDAK ADA field ini di response API soal, hanya dikirim balik saat submit) | TextInputEditText | Binding `akm_question_essay_item.xml:85` (`android:text="@{item.question.answer_essay}"`) | Hint "Ketikkan jawabanmu . . ." (⚠️ HARDCODED). `inputType` NUMBER untuk `ANSWER_ESSAY_NUM`, MULTI_LINE untuk lainnya (`QuestionEssayVh.kt:96-100`). Untuk `ANSWER_ESSAY_WORD`, spasi otomatis dihapus saat mengetik (`QuestionEssayVh.kt:109-114`) |
| Counter karakter TextInputLayout | `isCounterEnabled` | Boolean | `QuestionEssayVh.kt:94` | Aktif HANYA untuk `answerType==ANSWER_ESSAY` biasa (bukan NUM/WORD) |

---

### 3.3 Benar/Salah — `QuestionTrueFalseVh`

Layout `akm_question_true_false_item.xml`. Dipetakan saat `answerType=="STATEMENT"` dan
`answers.size==1` (satu pernyataan saja, dijawab Benar atau Salah).

| Elemen | Key data | Tipe | Sumber (file:baris) | Kondisi |
|---|---|---|---|---|
| Gambar soal | `AkmQuestionTable.file_path` (API `AkmQuestion.image`) | ImageView URL | `akm_question_true_false_item.xml:37` | `GONE` bila kosong |
| Teks soal | `AkmQuestionTable.question` (API `AkmQuestion.question`) | TextView + MathJax | `QuestionTrueFalseVh.kt:48-56` | `GONE` bila kosong |
| Media soal | `QuestionAnswers.media` | Include | `akm_question_true_false_item.xml:58-64` | Lihat §4 |
| Tombol "Benar" (`tv_true`) | ⚠️ HARDCODED teks. Background/warna dari `AkmAnswerTable.selected` (⚠️ LOKAL) — `answers.first()` (API asal: `AkmQuestion.answers[0]`, umumnya `isTrue=true` sebagai kunci jawaban) | String literal + Boolean drawable | `akm_question_true_false_item.xml:76,73` | Primer bila `answered && answer.selected`, abu lainnya. Klik → `onAnswer(answer.copy(selected=true))` |
| Tombol "Salah" (`tv_false`) | ⚠️ HARDCODED teks. Warna dari `!AkmAnswerTable.selected` | String literal + Boolean drawable | `akm_question_true_false_item.xml:94,91` | Primer bila `answered && !answer.selected`, abu lainnya. Klik → `onAnswer(answer.copy(selected=false))` |

> Catatan: hanya ADA SATU baris `AkmAnswerTable` per soal tipe ini (`item.answers.first()`,
> `QuestionTrueFalseVh.kt:82`). Field `selected=true` berarti siswa memilih "Benar", `selected=false`
> berarti "Salah" — bukan berarti "belum dijawab". Status "sudah dijawab" dicek terpisah lewat
> `AkmQuestionTable.answered` (⚠️ LOKAL, parameter `answered` di binding).

---

### 3.4 Tabel Pernyataan — `QuestionTableVh`

Layout `akm_question_statement_table_item.xml` (soal) + `akm_answer_statement_table.xml` (baris
tabel). Dipetakan saat `answerType=="STATEMENT"` dan `answers[0].showFalse==true` (banyak
pernyataan, masing-masing dijawab Benar/Salah via radio button per baris).

| Elemen | Key data | Tipe | Sumber (file:baris) | Kondisi |
|---|---|---|---|---|
| Gambar soal | `AkmQuestionTable.file_path` (API `AkmQuestion.image`) | ImageView URL | `akm_question_statement_table_item.xml:28` | `GONE` bila kosong |
| Teks soal | `AkmQuestionTable.question` (API `AkmQuestion.question`) | TextView + MathJax | `QuestionTableVh.kt:101-107` | `GONE` bila kosong |
| Media soal | `QuestionAnswers.media` | Include | `akm_question_statement_table_item.xml:48-54` | Lihat §4 |
| Header kolom "Pernyataan" / "Benar" / "Salah" | ⚠️ HARDCODED — 3 label kolom tabel statis | String literal | `akm_question_statement_table_item.xml:66,94,115` | Selalu tampil sama untuk semua soal tipe ini |
| RecyclerView baris pernyataan (`rv_answer`) | `QuestionAnswers.answers` (list `AkmAnswerTable`, API `AkmQuestion.answers[]`) | RecyclerView | `akm_question_statement_table_item.xml:122-129` | Satu baris per pernyataan |
| Gambar per baris pernyataan (`image`) | `AkmAnswerTable.file_path` (API `AkmAnswer.filePath`) | ImageView URL | `akm_answer_statement_table.xml:47` | `GONE` bila kosong |
| Teks pernyataan (`statement`) | `AkmAnswerTable.answer` (API `AkmAnswer.answer`, HTML) | TextView | `akm_answer_statement_table.xml:65` | — |
| RadioButton "Benar" (`radio_true`) | `AkmAnswerTable.selected` (⚠️ LOKAL) | Boolean checked | `akm_answer_statement_table.xml:106` | `checked = answered && selected`. Klik → `onAnswer(item.copy(selected=true))` (`QuestionTableVh.kt:37-40`) |
| RadioButton "Salah" (`radio_false`) | `!AkmAnswerTable.selected` | Boolean checked | `akm_answer_statement_table.xml:118` | `checked = answered && !selected`. Klik → `onAnswer(item.copy(selected=false))` |

> Field `AkmAnswer.isTrue` (API, kunci jawaban benar) TIDAK dipakai untuk styling saat mode
> pengerjaan biasa di layout ini (tidak ada binding warna hijau/merah berbasis `is_true`
> di `akm_answer_statement_table.xml`) — hanya dipakai di halaman pembahasan/skor terpisah bila
> ada. **Desainer jangan menambahkan indikator benar/salah visual di tabel pengerjaan.**

---

### 3.5 Menjodohkan / Pasangan (teks) — `QuestionPairVh`

Layout soal `akm_question_pair_item.xml`, layout tiap baris pasangan **yang benar-benar dipakai**
adalah `akm_answer_pair_item.xml` (di-inflate lewat `AkmAnswerPairItemBinding`,
`QuestionPairVh.kt:23,262-265`). Dipetakan saat `answerType=="PAIR"` dan
`answers[0].firstStatement` terisi.

> ⚠️ File `akm_answer_pair_1.xml` dan `akm_answer_pair_2.xml` ada di direktori layout tapi
> **tidak pernah di-inflate oleh kode manapun** (hanya dirujuk sebagai `tools:listitem` preview di
> `akm_question_pair_item.xml:68`) — anggap sebagai layout basi/tidak dipakai, jangan dijadikan
> acuan desain.

| Elemen | Key data | Tipe | Sumber (file:baris) | Kondisi |
|---|---|---|---|---|
| Gambar soal | `AkmQuestionTable.file_path` (API `AkmQuestion.image`) | ImageView URL | `akm_question_pair_item.xml:30` | `GONE` bila kosong |
| Teks soal | `AkmQuestionTable.question` (API `AkmQuestion.question`) | TextView + MathJax | `QuestionPairVh.kt:209-215` | `GONE` bila kosong |
| Media soal | `QuestionAnswers.media` | Include | `akm_question_pair_item.xml:52-58` | Lihat §4 |
| RecyclerView pasangan (`rv_first`) | `QuestionAnswers.answers` (list `AkmAnswerTable`, API `AkmQuestion.answers[]`) | RecyclerView | `akm_question_pair_item.xml:60-68` | Satu kartu per pasangan pernyataan |
| Label huruf kiri (`label`, A/B/C) | ⚠️ LOKAL — dari index posisi | String | `QuestionPairVh.kt:29,57` | — |
| Pernyataan sisi kiri/tetap (`answer_label`) | `AkmAnswerTable.first_statement` (dari API: `AkmAnswer.firstStatement`) | TextView (HTML) | `akm_answer_pair_item.xml:62` | Posisinya tidak berubah (jangkar) |
| Label angka drag (`label_2`) | ⚠️ LOKAL — `"${position+1}"` | String | `QuestionPairVh.kt:58` | `visibility="gone"` di XML (`akm_answer_pair_item.xml:100`) — **tidak pernah terlihat pengguna meski datanya di-bind** |
| Pernyataan sisi kanan/dapat digeser (`drag_area` isinya) | `AkmAnswerTable.second_statement` (dari API: `AkmAnswer.secondStatement`, tapi diacak posisinya via drag & drop antar kartu) | TextView (HTML) | `akm_answer_pair_item.xml:109` | Long-press pada `drag_area` memulai drag (`QuestionPairVh.kt:137-143`); drop menukar `selected_id` & `second_statement` antar 2 kartu (`QuestionPairVh.kt:97-107`) |

---

### 3.6 Pasangan Gambar — `QuestionPairImageVh`

Layout soal `akm_question_pair_image_item.xml`, baris pasangan `akm_answer_pair_image_1.xml`
(di-inflate via `AkmAnswerPairImage1Binding`, `QuestionPairImageVh.kt:23,250-253`). Dipetakan saat
`answerType=="PAIR"` dan `answers[0].firstStatement` KOSONG (soal menjodohkan berbasis gambar).

> ⚠️ File `akm_answer_pair_image_2.xml` ada tapi **tidak dirujuk/di-inflate di manapun** dalam
> kode Kotlin maupun XML lain — layout basi, jangan dijadikan acuan.

| Elemen | Key data | Tipe | Sumber (file:baris) | Kondisi |
|---|---|---|---|---|
| Gambar soal | `AkmQuestionTable.file_path` (API `AkmQuestion.image`) | ImageView URL | `akm_question_pair_image_item.xml:29` | `GONE` bila kosong |
| Teks soal | `AkmQuestionTable.question` (API `AkmQuestion.question`) | TextView + MathJax | `QuestionPairImageVh.kt:197-203` | `GONE` bila kosong |
| Media soal | `QuestionAnswers.media` | Include | `akm_question_pair_image_item.xml:50-56` | Lihat §4 |
| RecyclerView pasangan (`rv_first`) | `QuestionAnswers.answers` (API `AkmQuestion.answers[]`) | RecyclerView | `akm_question_pair_image_item.xml:58-65` | Satu kartu per pasangan gambar |
| Label huruf (`label`, A/B/C) | ⚠️ LOKAL — dari index posisi | String | `QuestionPairImageVh.kt:29,57` | — |
| Gambar kiri/tetap (`image`) | `AkmAnswerTable.first_file_path` (dari API: `AkmAnswer.firstFilePath`) | ImageView URL | `akm_answer_pair_image_1.xml:60` | Klik → preview gambar (`onImageClick`, `QuestionPairImageVh.kt:59`) |
| Gambar kanan/dapat digeser (`drag_area`) | `AkmAnswerTable.second_file_path` (dari API: `AkmAnswer.secondFilePath`) | ImageView URL | `akm_answer_pair_image_1.xml:73` | Long-press memulai drag; drop menukar `selected_id` & `second_file_path` antar kartu (`QuestionPairImageVh.kt:99-109`). Klik (tanpa drag) → preview gambar |

> **Kekhususan payload untuk tipe ini:** karena `selected_id` bisa menyimpang dari gambar yang
> sungguh tampil setelah drag berkali-kali, `AkmAnswerPayload.normalizedPairAnswers()`
> (`AkmAnswerPayload.kt:79-115`) menormalkan ulang `selected_id` dari pola nama berkas
> `second_file_path` (regex `a{soal}_{idGambar}_2.jpg`) sebelum dikirim ke server — lihat §Payload.

---

## 4. Media Soal — `AkmQuestionMediaBinder`

Sumber: `app/src/main/java/id/diskola/app/pages/akm/AkmQuestionMediaBinder.kt`, layout wadah
`akm_question_media.xml` (kosong, di-`include` di semua item soal), item audio
`akm_question_media_audio.xml`, item video/YouTube `akm_question_media_video.xml`.

Data sumber untuk semua baris di bawah: `AkmQuestionMediaTable` (kolom `id`, `type`, `url`,
`local_path`, `sequence`) — field `type`, `url`, `sequence` berasal langsung dari API
`AkmQuestionMedia.type` / `.url` / `.sequence`; `local_path` murni ⚠️ LOKAL (hasil unduhan cache
oleh `AkmDownloader`, tidak ada di API).

### Elemen UI

| Elemen | Key data | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Wadah media (`question_media`) | `QuestionAnswers.media` (list) | ViewGroup | `AkmQuestionMediaBinder.kt:88-122` | `GONE` total bila `media` kosong |
| Baris audio (`akm_question_media_audio.xml`) | `AkmQuestionMediaTable.type` (API `AkmQuestionMedia.type`) `== "audio"` | Layout row | `AkmQuestionMediaBinder.kt:99-115` | Satu baris per item media bertipe audio |
| Tombol play/pause audio (`btn_play`) | State pemutaran ⚠️ LOKAL (`MediaPlayer.isPlaying`) | ImageButton icon | `AkmQuestionMediaBinder.kt:105,168-251` | Icon play ↔ pause. Sumber file: `AkmQuestionMediaTable.local_path` bila sudah di-cache, else `.url` langsung (`playSource()`, `AkmQuestionMediaBinder.kt:276-277`) |
| SeekBar audio (`seek_audio`) | Posisi/`duration` MediaPlayer ⚠️ LOKAL | Progress | `AkmQuestionMediaBinder.kt:71-74,106-114` | Update tiap 250ms saat main |
| Durasi audio (`tv_duration`, "00:00") | Waktu berjalan/total ⚠️ LOKAL, format `mm:ss` | TextView | `AkmQuestionMediaBinder.kt:72,785-793` | — |
| Baris video/YouTube (`akm_question_media_video.xml`) | `AkmQuestionMediaTable.type` (API) `!= "audio"` | Layout row | `AkmQuestionMediaBinder.kt:116-121` | — |
| Player video (WebView utk YouTube atau VideoView utk mp4) | `AkmQuestionMediaTable.url` (API) diperiksa apakah ID YouTube (`youtubeId()`, `AkmQuestionMediaBinder.kt:770-783`); atau `.local_path` bila sudah ter-cache (mp4 sendiri) | WebView/VideoView | `AkmQuestionMediaBinder.kt:448-574` | Video YouTube TIDAK PERNAH di-cache offline (by design, lihat komentar `requiresNetwork`, `:264-274`) |
| Placeholder offline video — ikon (`img_video_offline`) | ⚠️ HARDCODED (drawable statis `ic_reload`) | ImageView | `akm_question_media_video.xml:69-74` | Tampil hanya saat video butuh jaringan (`requiresNetwork(media)==true`) DAN perangkat offline (`isOnline()==false`), lihat `AkmQuestionMediaBinder.kt:384-386,404-426` |
| Placeholder offline video — teks "Video ini memerlukan koneksi internet" | String resource `R.string.akm_media_video_offline` (⚠️ HARDCODED, statis) | TextView | `akm_question_media_video.xml:76-84`, string di `values/strings.xml:175` | Sama seperti di atas |
| Tombol "Coba lagi" (`btn_video_retry`) | ⚠️ HARDCODED (`R.string.akm_media_retry`) | Button | `akm_question_media_video.xml:86-92` | Klik → `retryVideo()`; menampilkan toast "Masih belum ada koneksi internet" (`R.string.akm_media_still_offline`) bila tetap offline |
| Tombol "Aktifkan Data" (`btn_video_enable_data`) | ⚠️ HARDCODED (`R.string.akm_media_enable_data`) | Button | `akm_question_media_video.xml:94-100` | Klik → `onEnableDataRequested?.invoke(media)` → membuka Setelan Jaringan Android (lihat `AkmQuestionsPage.kt:675-695`), TANPA penalti (keluar-sementara yang sah) |
| Dialog "Butuh Koneksi Internet" (saat siswa scroll ke soal bervideo & offline) | String resource `R.string.akm_video_need_data_title` / `_message` / `_later` (⚠️ HARDCODED) | AlertDialog | `AkmQuestionsPage.kt:653-669`, strings di `strings.xml:179-181` | Muncul otomatis sekali per media id (`promptedMediaIds`) saat siswa berhenti di soal yang mediannya `requiresNetwork==true` dan koneksi belum tervalidasi |
| Tombol play overlay video (`btn_play_overlay`) | ⚠️ HARDCODED icon statis | ImageButton | `akm_question_media_video.xml:112-125` | Hilang saat video sedang main |
| SeekBar & durasi video (`seek_video`, `tv_video_duration`) | Progres pemutaran ⚠️ LOKAL (dari `VideoView`/YouTube JS bridge `onTime`) | Progress + teks `mm:ss / mm:ss` | `AkmQuestionMediaBinder.kt:648-696,785-793` | — |

---

## 5. Dialog Pilih Nomor Soal — `QuestionSelectDialog`

Sumber: `app/src/main/java/id/diskola/app/pages/akm/QuestionSelectDialog.kt`, layout dialog
`app/src/main/res/layout/select_question_dialog.xml`, item grid
`app/src/main/res/layout/akm_question_select_item.xml`.

> Catatan: ada juga file layout `question_select_item.xml` di direktori yang sama, memakai
> variabel tipe `id.diskola.app.pages.ujian.QuestionTable` (bukan `akm`) — ini milik fitur lain
> (Ujian, bukan AKM) dan **tidak dipakai oleh `QuestionSelectDialog`**. `QuestionSelectDialog`
> memakai `AkmQuestionSelectItemBinding`, yaitu `akm_question_select_item.xml`.

### Elemen UI

| Elemen | Key data | Tipe | Sumber (file:baris) | Kondisi |
|---|---|---|---|---|
| Judul dialog "Halaman soal" | ⚠️ HARDCODED | String literal | `select_question_dialog.xml:22` | Statis |
| Grid nomor soal (`rv_question_numbers`) | List `AkmQuestionTable` (satu per soal dalam instruksi ini), 5 kolom (`GridLayoutManager` span=5, `QuestionSelectDialog.kt:30-33`) | GridLayoutManager | `QuestionSelectDialog.kt:31-45` | — |
| Angka nomor soal (`tvNumber`) | ⚠️ LOKAL — `"${position+1}"` dari index posisi dalam list, BUKAN dari field API manapun | String | `QuestionSelectDialog.kt:87`, binding `akm_question_select_item.xml:25` | — |
| Warna & background angka (biru terisi vs abu kosong) | `AkmQuestionTable.answered` (⚠️ LOKAL) — pada mode `isExplanation`, di-override jadi `explanation_file_path.isNotEmpty()` (`QuestionSelectDialog.kt:89-90`) | Boolean → warna teks & background | `akm_question_select_item.xml:28-29` | Teks putih + `fill_blue_radius6` bila `answered==true`; teks hitam + `fill_form_ppob` bila `false` |
| Teks catatan bawah "note: soal yang sudah dijawab akan berwarna hijau, jika belum terjawab berwarna putih" | ⚠️ HARDCODED | String literal | `select_question_dialog.xml:49` | **Tidak akurat terhadap implementasi aktual** — warna yang benar-benar dipakai adalah BIRU (`fill_blue_radius6`) untuk terjawab, bukan hijau; abu/putih (`fill_form_ppob`) untuk belum. Desainer harus pakai warna biru/abu asli ini, bukan teks catatan tersebut |
| Klik item nomor | — | onClick | `QuestionSelectDialog.kt:94-100` | Menutup dialog & memindah RecyclerView utama ke posisi soal terpilih |

---

## 6. Dialog/Halaman Instruksi — `AkmInstructionPage` + Kartu Instruksi Inline

Ada DUA jalur instruksi yang tampak serupa tapi berbeda kelas:
1. **Halaman penuh** `AkmInstructionPage` (`app/src/main/java/id/diskola/app/pages/akm/AkmInstructionPage.kt`, layout `akm_instruction_page.xml`) — dibuka dari daftar instruksi di `AkmTakeResumePage` (`AkmTakeResumePage.kt:944-954`, sebelum masuk ke soal).
2. **Dialog ringkas** yang dibuka dari tombol "Baca petunjuk" di dalam `AkmQuestionsPage` itu sendiri (`AkmQuestionsPage.kt:291-293, 397-407`) — hanya `MaterialAlertDialogBuilder` sederhana, bukan Activity terpisah.

### 6.1 Elemen UI — `AkmInstructionPage` (halaman penuh)

| Elemen | Key data | Tipe | Sumber (file:baris) | Kondisi |
|---|---|---|---|---|
| Toolbar title "Perintah Soal" | ⚠️ HARDCODED (XML `app:title`) | String literal | `akm_instruction_page.xml:21` | Statis |
| Judul konten (mis. "I. PERINTAH SOAL") | ⚠️ LOKAL — `RomanNumber.toRoman(number)` dari extra Intent `number` (posisi instruksi) + literal " PERINTAH SOAL" | String | `AkmInstructionPage.kt:40` | Format `"{romawi}. PERINTAH SOAL"` |
| Isi instruksi (`instruction`) | `AkmInstructionTable.instruction` (dari API: `AkmInstruction.instruction`), diteruskan sebagai extra Intent String, dirender `Html.fromHtml` | TextView (HTML) | `AkmInstructionPage.kt:41` | — |
| Label "DESKRIPSI & CERITA" | ⚠️ HARDCODED | String literal | `akm_instruction_page.xml:63` | Statis |
| Isi deskripsi (`desc`) | `AkmInstructionTable.description` (dari API: `AkmInstruction.description`), `Html.fromHtml` | TextView (HTML) | `AkmInstructionPage.kt:42` | — |
| Tombol aksi ("Kerjakan Soal" / "Lihat Pembahasan") | ⚠️ HARDCODED, dipilih dari extra `isExplanation` | String literal | `AkmInstructionPage.kt:44` | Klik → buka `AkmQuestionsPage` (bila bukan mode `action_back`) |

### 6.2 Elemen UI — Dialog instruksi inline di `AkmQuestionsPage`

| Elemen | Key data | Tipe | Sumber (file:baris) | Kondisi |
|---|---|---|---|---|
| Judul dialog "Instruksi Asesmen" | ⚠️ HARDCODED | String literal | `AkmQuestionsPage.kt:292` | Statis |
| Isi dialog | `instruction` + `description` (extra Intent, asal dari `AkmInstructionTable.instruction`/`.description`, API `AkmInstruction.instruction`/`.description`) digabung `"${Html.fromHtml(instruction)}\n\n${Html.fromHtml(description)}"` | AlertDialog message | `AkmQuestionsPage.kt:293` | Muncul saat kartu "Baca petunjuk" ditekan |

---

## 7. Dialog Mode Terkunci Terlepas / Penalti

Ada 2 dialog berbeda yang keduanya identik strukturnya di `AkmTakeResumePage` dan
`AkmQuestionsPage` (kode diduplikasi di kedua file, bukan komponen bersama).

### 7.1 Dialog "Mode Terkunci Terlepas" (lock-task/screen-pinning lepas)

Sumber: `AkmTakeResumePage.kt:311-339` (fungsi `showLockdownRecoveryDialog`) dan identik di
`AkmQuestionsPage.kt:188-216`.

| Elemen | Key data | Tipe | Sumber (file:baris) | Kondisi tampil |
|---|---|---|---|---|
| Judul "Mode Terkunci Terlepas" | ⚠️ HARDCODED | String literal | `AkmTakeResumePage.kt:315` / `AkmQuestionsPage.kt:192` | Muncul saat `ExamLockdown.hasActiveBreach()==true` DAN penalti tidak sedang aktif |
| Pesan "Mode terkunci ujian tidak aktif. Aktifkan kembali penyematan layar untuk melanjutkan pengerjaan." | ⚠️ HARDCODED | String literal | `AkmTakeResumePage.kt:316` / `AkmQuestionsPage.kt:193` | Dialog `setCancelable(false)` — tidak bisa ditutup tanpa aksi |
| Tombol "Aktifkan" | ⚠️ HARDCODED | String literal | `AkmTakeResumePage.kt:318` / `AkmQuestionsPage.kt:195` | Klik memicu `ExamLockdown.retryLock()`, teks berubah jadi "Memeriksa..." (⚠️ HARDCODED) sambil menunggu verifikasi (`VERIFY_DELAY_MS`) |
| Pesan retry "Penyematan layar belum aktif. Tekan Aktifkan, lalu konfirmasi dialog sistem jika muncul." | ⚠️ HARDCODED | String literal | `AkmTakeResumePage.kt:330` / `AkmQuestionsPage.kt:207` | Muncul bila verifikasi lock gagal setelah klik "Aktifkan" |

### 7.2 Dialog/Pill Penalti (deteksi keluar aplikasi / overlay)

Sumber: `AkmTakeResumePage.kt` (banyak lokasi: `init()` ~L404, `onRestart()` ~L372-402,
`PenaltyFromOverlay()` ~L237-285) dan duplikatnya di `AkmQuestionsPage.kt` (`onCreate` ~L287-289,
`onRestart` ~L471-509, `PenaltyFromOverlay` ~L697-740). Timer aktual dihitung oleh
`CountDownService.kt`.

| Elemen | Key data | Tipe | Sumber (file:baris) | Kondisi tampil |
|---|---|---|---|---|
| Judul dialog "Penalti" / "Penalti Ujian" | ⚠️ HARDCODED | String literal | `AkmTakeResumePage.kt:806` (resume-page: "Penalti Ujian"), `AkmQuestionsPage.kt:288` (questions-page: "Penalti") | Muncul saat `state_penalty_{akmId}` true di `SharedPreferences` DAN `penalty_applied` setting (dari `GET mobile/setting-akm`, field respons `penalty_applied`) bernilai true |
| Pesan "Terdeteksi keluar aplikasi ujian. Mohon menunggu untuk dapat melanjutkan ujian" (variasi kalimat sedikit beda antar 2 halaman) | ⚠️ HARDCODED | String literal | `AkmTakeResumePage.kt:807`, `AkmQuestionsPage.kt:289` | — |
| Judul dialog dinamis berisi sisa waktu (di-`setTitle(data)`) | Nilai `data`/`time` dari broadcast `CountDownService` — ⚠️ LOKAL, format `"{menit}:{detik}"` dihitung dari selisih `data_{akmId}` (waktu mulai penalti tersimpan di SharedPreferences) terhadap durasi `akm_settings.penalty_times` (API: field `penalty_times` dari respons `GET mobile/setting-akm`, disimpan lokal via `AkmSettingTable`) | String | `CountDownService.kt:76-114` (`twoDatesBetweenTime`), diterima di `AkmTakeResumePage.kt:438` / `AkmQuestionsPage.kt:225` | Diperbarui tiap 1 detik (`NOTIFY_INTERVAL=1000ms`, `CountDownService.kt:133`) |
| Toolbar judul saat penalti ("Penalti Ujian" / "  Penalti") | ⚠️ HARDCODED | String literal | `AkmTakeResumePage.kt:451`, `AkmQuestionsPage.kt:241` | Menggantikan judul normal selama pill/timer penalti aktif (`data != ""`) |
| Tombol "Kumpulkan"/"Berikutnya" tersembunyi | — | Visibility | `AkmTakeResumePage.kt:441,450`, `AkmQuestionsPage.kt:228,240` | `INVISIBLE` selama penalti berjalan |

> **Sumber saklar aktif/nonaktifnya seluruh sistem penalti & lockdown:**
> - `penalty_applied` (Boolean, API `GET mobile/setting-akm` → `penaltyResponse.penalty_applied`, `AkmModels.kt:201`) — bila `false`, seluruh pill/dialog penalti di §7.2 di-skip total.
> - `exam_lock_mode` (Boolean?, API respons sama → `penaltyResponse.exam_lock_mode`, `AkmModels.kt:217`, default `true`/aman bila null) — mengontrol apakah screen-pinning (§7.1) diaktifkan sama sekali (`AkmSettings.isStrictMode`, `AkmSettings.kt:26-35`; dipanggil di `AkmTakeResumePage.kt:792-799`).
> - `penalty_times` (Int, API sama → `penaltyResponse.penalty_times`) — durasi penalti dalam detik, dipakai `startPenaltyMinutes()` di kedua halaman.

---

## Payload Jawaban (dikirim ke server)

Sumber tunggal: `app/src/main/java/id/diskola/app/pages/akm/AkmAnswerPayload.kt` (dipakai baik
oleh alur worker offline `AkmUploader` maupun alur upload langsung di `AkmViewModel.uploadAnswer`).
Ini adalah **request body**, bukan field response — jangan disamakan dengan tabel-tabel di atas.

Payload berupa `List<Map<String, Any>>`, HANYA berisi soal yang `AkmQuestionTable.answered==true`
(`AkmAnswerPayload.kt:32`). Struktur tiap item:

| Field payload | Tipe | Asal nilai | Sumber (file:baris) |
|---|---|---|---|
| `instruction_id` | Int | `AkmInstructionTable.id` (=API `AkmInstruction.id`) | `AkmAnswerPayload.kt:35` |
| `question_id` | Int | `AkmQuestionTable.id` (=API `AkmQuestion.id`) | `AkmAnswerPayload.kt:36` |
| `answerType` | String | `AkmQuestionTable.type_label` (=API `AkmQuestion.answerType` asli, disimpan apa adanya: `"MULTIPLE CHOICE"`, `"STATEMENT"`, `"PAIR"`, `"ESSAY"`, dst) | `AkmAnswerPayload.kt:37` |
| `answer` | Any (bentuk tergantung `type_label`, lihat di bawah) | `buildAnswer()` | `AkmAnswerPayload.kt:38,48-72` |

Bentuk `answer` per `type_label`:

| `type_label` | Bentuk `answer` yang dikirim | Detail |
|---|---|---|
| `"MULTIPLE CHOICE"` | `Int` — id jawaban terpilih, atau `0` bila tidak ada | `akmDao.getSelectedAnswers(question.id).firstOrNull()?.id ?: 0` (`AkmAnswerPayload.kt:53-54`). `type_label` = `AkmQuestion.answerType` API mentah, jadi hanya soal yang di API-nya benar-benar `answerType=="MULTIPLE CHOICE"` yang memakai bentuk Int tunggal ini |
| `"STATEMENT"` | `List<Map>` — satu entri per baris jawaban: `{"id": <Int>, "isTrue": <0/1>, "answered": <0/1>}` | `id`=`AkmAnswerTable.id`; `isTrue`=`AkmAnswerTable.is_true` (API `AkmAnswer.isTrue`, kunci jawaban asli — **dikirim balik ke server**, bukan hanya `selected`); `answered`=`AkmAnswerTable.selected` (pilihan siswa). Berlaku untuk 3 tipe UI sekaligus, karena semuanya berasal dari `answerType` API `"STATEMENT"`: Benar/Salah (§3.3), Tabel Pernyataan (§3.4), DAN varian checkbox pilihan-ganda-multi (§3.1, tipe `4`) — bentuknya di payload SAMA meski tampilannya beda (`AkmAnswerPayload.kt:56-62`) |
| `"PAIR"` | `List<Map>` — `{"id": <Int>, "answered": <Int>}` | `id`=`AkmAnswerTable.id`; `answered`=`AkmAnswerTable.selected_id` (id pasangan yang sedang ditempatkan siswa setelah drag-drop). Untuk soal **pasangan gambar** (`type==ANSWER_PAIRING_IMAGE`), `selected_id` DINORMALISASI ulang dari pola nama file `second_file_path` (regex `a\d+_(\d+)_2\.jpg$`) sebelum dikirim — lihat `normalizedPairAnswers()` (`AkmAnswerPayload.kt:64-69,79-115`) |
| lainnya (termasuk `"ESSAY"`, `"SHORT_ESSAY_NUM"`, `"SHORT_ESSAY_WORD"`) | `String` | `AkmQuestionTable.answer_essay` (jawaban esai/isian siswa) | `AkmAnswerPayload.kt:71` |

---

## Kamus Field — Gabungan Semua Data Class Terkait

### Response API — soal & instruksi (`AkmModels.kt`)

| Field API | Tipe | Deskripsi | Dipakai di UI? |
|---|---|---|---|
| `AkmInstruction.id` | Int | ID instruksi/babak | Ya → `AkmInstructionTable.id`, dipakai payload `instruction_id` |
| `AkmInstruction.instruction` | String | Perintah/instruksi soal (HTML) | Ya → §6 (halaman & dialog instruksi) |
| `AkmInstruction.description` | String | Deskripsi/cerita pengantar (HTML) | Ya → §6 |
| `AkmInstruction.sequence` | Int | Urutan instruksi | Tidak langsung ditampilkan (urutan render pakai posisi list) |
| `AkmInstruction.isRandom` | Boolean | Soal diacak atau tidak | Tidak ditemukan pemakaian di UI pengerjaan |
| `AkmInstruction.questions` | List\<AkmQuestion\> | Daftar soal dalam instruksi ini | Ya → sumber `AkmQuestionTable` per soal |
| `AkmQuestion.id` | Int | ID soal | Ya → payload `question_id` |
| `AkmQuestion.question` | String | Teks soal (HTML, bisa memuat formula `data-value="..."` utk MathJax) | Ya → semua §3.x "Teks soal" |
| `AkmQuestion.image` | String | URL gambar soal | Ya → `AkmQuestionTable.file_path`, ImageView "image" di semua tipe soal |
| `AkmQuestion.answerType` | String | Penentu tipe soal (`MULTIPLE CHOICE`/`STATEMENT`/`ESSAY`/`PAIR`/`SHORT_ESSAY_NUM`/`SHORT_ESSAY_WORD`) | Ya → §3 (pemetaan `AkmQuestionTable.type`), juga tersimpan mentah di `type_label` utk payload |
| `AkmQuestion.answers` | List\<AkmAnswer\> | Daftar opsi/pernyataan/pasangan jawaban | Ya → `AkmAnswerTable[]` |
| `AkmQuestion.media` | List\<AkmQuestionMedia\> | Daftar media (audio/video) soal | Ya → §4 |
| `AkmQuestion.exam_instruction_id` | Int | (var, di-set di runtime) | Tidak dipakai langsung di UI pengerjaan |
| `AkmAnswer.id` | Int | ID opsi/pernyataan/pasangan | Ya → payload (`getSelectedAnswers`, id dalam list STATEMENT/PAIR) |
| `AkmAnswer.answer` | String | Teks jawaban/pernyataan (HTML) | Ya → §3.1 (pilihan ganda), §3.4 (tabel pernyataan) |
| `AkmAnswer.filePath` | String | URL gambar jawaban (opsi pilihan ganda / baris tabel) | Ya → §3.1, §3.4 |
| `AkmAnswer.isTrue` | Boolean | Kunci jawaban benar (server-side) | Ya, tapi TERBATAS: dipakai untuk styling saat `isExplanation`/`scored=true` di §3.1; DIKIRIM BALIK ke server di payload STATEMENT (field `isTrue`); TIDAK dipakai untuk styling di §3.4 (tabel) |
| `AkmAnswer.showFalse` | Boolean | Penentu apakah soal STATEMENT ini bertipe "tabel banyak pernyataan" | Ya → §3 (logika pemilihan tipe), tidak ditampilkan langsung |
| `AkmAnswer.firstStatement` | String | Pernyataan sisi kiri/tetap (soal Pair teks) | Ya → §3.5 |
| `AkmAnswer.firstFilePath` | String | Gambar sisi kiri/tetap (soal Pair gambar) | Ya → §3.6 |
| `AkmAnswer.secondStatement` | String | Pernyataan sisi kanan/dapat digeser (soal Pair teks) | Ya → §3.5 |
| `AkmAnswer.secondFilePath` | String | Gambar sisi kanan/dapat digeser (soal Pair gambar) | Ya → §3.6, juga sumber normalisasi `selected_id` di payload |
| `AkmAnswer.selected_id` | Int | (var, dipakai runtime utk urutan pasangan) | Sebagian — nilai awal dari API biasanya menandai pasangan default sebelum diacak siswa |
| `AkmQuestionMedia.id` | Int | ID media | Ya → key dedup dialog "butuh data" (`promptedMediaIds`) |
| `AkmQuestionMedia.type` | String | `"audio"` atau lainnya (video) | Ya → §4, penentu audio vs video row |
| `AkmQuestionMedia.url` | String | URL media (bisa link YouTube) | Ya → §4, sumber pemutaran |
| `AkmQuestionMedia.sequence` | Int | Urutan media | Menentukan urutan render list (tidak ditampilkan sbg teks) |

### Room lokal — turunan/state aplikasi (`AkmEntities.kt`)

| Field lokal | Tipe | Asal | Murni state lokal? |
|---|---|---|---|
| `AkmQuestionTable.type` | Int (`AkmAnswerType.*`) | Hasil pemetaan dari `answerType`+`answers` API | Turunan, bukan field API langsung |
| `AkmQuestionTable.type_label` | String | = `AkmQuestion.answerType` API mentah | Salinan API |
| `AkmQuestionTable.answered` | Boolean | ⚠️ LOKAL — di-set `true` oleh app saat siswa menjawab (`setAnswered()`/`answerEssay()`) | Ya, murni lokal |
| `AkmQuestionTable.answer_essay` | String | ⚠️ LOKAL — jawaban esai siswa | Ya, murni lokal |
| `AkmQuestionTable.explanation_file_path` | String | Diisi worker pembahasan (`AkmExplanationDownloader`), bukan dari `AkmQuestion` biasa | Semi-lokal (dari API pembahasan terpisah, `AkmExplanationData.file_path`) |
| `AkmInstructionTable.answered` | Int | ⚠️ LOKAL — `COUNT` soal terjawab (SQL, `setInstAnswered`) | Ya, murni lokal |
| `AkmInstructionTable.num_question` | Int | Diisi saat unduh = `questions.size` dari API | Turunan (hitungan array API saat unduh), bukan field API literal |
| `AkmExamsTable.finished` | Boolean | ⚠️ LOKAL — turunan status semua instruksi selesai | Ya, murni lokal |
| `AkmExamsTable.show_child` | Boolean | ⚠️ LOKAL — state expand/collapse UI | Ya, murni lokal |
| `AkmAnswerTable.selected` | Boolean | ⚠️ LOKAL — pilihan siswa saat ini | Ya, murni lokal |
| `AkmAnswerTable.selected_id` | Int | Awal dari API `AkmAnswer.selected_id`, diperbarui terus oleh drag-drop siswa (Pair) | Campuran (nilai awal API, diubah lokal) |
| `AkmQuestionMediaTable.local_path` | String | ⚠️ LOKAL — path cache hasil unduhan `AkmDownloader` | Ya, murni lokal |

### Setelan terkait penalti/lockdown (`penaltyResponse`, API `GET mobile/setting-akm`)

| Field API | Tipe | Dipakai untuk |
|---|---|---|
| `penalty_times` | Int | Durasi penalti (detik) di §7.2 |
| `penalty_applied` | Boolean | Saklar aktif/nonaktif seluruh sistem penalti (§7.2) |
| `absence_setting` | Boolean | Tidak ditemukan pemakaian di alur pengerjaan soal ini (kemungkinan dipakai fitur presensi lain) |
| `exam_lock_mode` | Boolean? (default true) | Saklar aktif/nonaktif screen-pinning & dialog "Mode Terkunci Terlepas" (§7.1) |

---

## Ringkasan Elemen ⚠️ HARDCODED (tidak boleh dianggap punya sumber API)

Semua label statis berikut TIDAK BOLEH digambar ulang seolah berasal dari data — mereka string
literal tetap di XML/Kotlin: judul-judul toolbar per mode, teks "Kumpulkan"/"Berikutnya"/"Baca
petunjuk"/"Perintah soal, deskripsi & cerita", label kolom tabel pernyataan ("Pernyataan"/"Benar"/
"Salah"), teks tombol "Benar"/"Salah" true-false, seluruh teks dialog penalti & lockdown, seluruh
string resource `akm_media_*`/`akm_video_need_data_*`, judul "Halaman soal" dan catatan warna di
dialog pilih nomor (yang bahkan tidak akurat — lihat §5), serta label huruf A/B/C dan nomor urut
1/2/3 yang semuanya dihasilkan dari index posisi list, bukan field data.
