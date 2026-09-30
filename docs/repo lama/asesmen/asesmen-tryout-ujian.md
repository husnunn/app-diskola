# Dokumentasi Presisi UI — Try Out & Ujian (keluarga Asesmen)

> Dibuat untuk kebutuhan AI design generator. **Aturan ketat**: setiap klaim "elemen X menampilkan data Y" di bawah ini dibuktikan dengan mengutip baris kode binding asli (`@{...}` di XML data-binding, atau `binding.viewId.text = ...`/`binding.field = ...` di Kotlin). Semua path file di bawah relatif terhadap root modul `app/src/main/`.
>
> Try Out dan sebagian alur Ujian (khususnya kerjakan-soal/detail) **berbagi komponen dengan AKM** (`pages/akm/AkmDetailPage.kt`, `AkmTakeResumePage.kt`, `AkmQuestionsPage.kt`, `AkmScoreDetailPage.kt`). Dokumen ini **tidak mengulang** dokumentasi umum AKM — hanya mendokumentasikan halaman yang murni milik Try Out/Ujian, dan percabangan kondisi yang **khusus berbeda** untuk Try Out di dalam komponen AKM tersebut. Untuk perilaku umum AKM, rujuk `docs/dokumentasi-asesmen.md`.

---

## Ringkasan arsitektur data

- **Try Out** dan **Ujian** adalah dua sistem backend/data yang **berbeda**, walau UI Try Out (jadwal & nilai) memakai komponen visual yang sama dengan AKM (`AkmTable` entity, `AkmDetailPage`, `AkmScoreDetailPage`).
- **Try Out**: API mengembalikan `ListTryoutData` (lihat `pages/tryout/TryoutModels.kt`). Data ini **dikonversi** oleh `OnKlasDbUtil.processTryoutResponse()` (`app/src/main/java/id/diskola/app/db/OnKlasDbUtil.kt:1073-1204`) menjadi baris **`AkmTable`** (`pages/akm/AkmEntities.kt:26-93`) yang disimpan di Room lalu ditampilkan lewat data-binding. Artinya field yang tampil di layar Try Out **bukan** field mentah `ListTryoutData` secara langsung, melainkan hasil transformasi — didokumentasikan di setiap baris tabel di bawah.
- **Ujian** (legacy, `pages/ujian/**`) memakai entity Room sendiri: **`ExamTable`**, **`QuestionTable`**, **`AnswerTable`**, **`MyAnswerTable`** (`pages/ujian/UjianEntities.kt`), diisi dari `TestStudentResponse`/`TestDetailResponse`/`DownloadSoalResponse` (`pages/ujian/UjianModels.kt`) lewat `OnKlasDbUtil.processListUjianStudent/processUjianScoreStudent/processDetailUjian/processSoalUjian` (`OnKlasDbUtil.kt:447-654`).
- Endpoint API sumber (untuk referensi tim BE/FE, bukan bagian dari kontrak UI):
  - Try Out jadwal: `GET mobile/app/learning/try-out/schedules` (`ApiService.kt:985`)
  - Try Out nilai: `GET mobile/app/learning/try-out/scored` (`ApiService.kt:991`)
  - Try Out passing grade (list univ): `GET mobile/app/learning/try-out/passing-grade` (`ApiService.kt:1483`)
  - Try Out pembahasan: `GET mobile/app/learning/try-out/scored/{id}/explanation` (`ApiService.kt:1486`)
  - Ujian jadwal: `GET mobile/app/learning/examinations/students/exams-list` (`ApiService.kt:652`)
  - Ujian nilai: `GET mobile/app/learning/examinations/students/exams-scored` (`ApiService.kt:659`)
  - Ujian detail/download/start/answer/stop: `ApiService.kt:665-693`

---

# BAGIAN A — TRY OUT

## 1. TryOutPage (host, tab Jadwal/Nilai)

**File:** `pages/tryout/TryOutPage.kt`, layout `res/layout/try_out_page.xml`

**Trigger/masuk dari:** Tidak ditemukan pemicu forward (menu/tombol) yang membuka `TryOutPage` secara langsung di source ini — satu-satunya referensi `TryOutPage::class.java` yang ditemukan adalah sebagai **tujuan kembali** (back-navigation) dari `AkmDetailPage` (`pages/akm/AkmDetailPage.kt:77-82, 343-348`) dan `AkmScoreDetailPage` (`pages/akm/AkmScoreDetailPage.kt:82-88, 485-491`) ketika `examType == ExamType.TRYOUT` / `isTryout == true`. ⚠️ **Ambigu**: entry-point awal (mis. dari menu Pembelajaran/Home) tidak ditemukan lewat pencarian kode — kemungkinan lewat rute lain (deep link/notifikasi) yang tidak tercakup pencarian ini. Tim desain sebaiknya konfirmasi ke tim BE/mobile.

**Navigasi keluar:**
- Tombol back toolbar / hardware back → `HomePage` (`TryOutPage.kt:65-70, 140-147`) dengan extra `isSchoolScope=true`.
- Tab "Jadwal" → fragment `TryOutSchedulePage` via `action_global_tryoutschedulepage` (`TryOutPage.kt:76`).
- Tab "Nilai" → fragment `TryOutScorePage` via `action_global_tryoutscorepage` (`TryOutPage.kt:92`).
- Dialog "Peringatan" (jam/zona waktu perangkat tidak otomatis) muncul di `onResume()` (`TryOutPage.kt:107-136`) — lihat baris HARDCODED di bawah.

### Elemen UI

| Elemen (label visual) | Key data (path dari root JSON response) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Judul toolbar "Quiz" | — | ⚠️ HARDCODED / tidak ada key API | `try_out_page.xml:21` (`app:title="Quiz"`) dan diset ulang di `TryOutPage.kt:64` (`binding.toolbar.title = "Quiz"`) | Selalu tampil, statis |
| Tombol "Jadwal" | — | ⚠️ HARDCODED (label statis) | `try_out_page.xml:40` | Warna aktif (primary) saat dipilih, warna abu saat tidak — logic warna di `TryOutPage.kt:76-88`, DIHITUNG dari state klik terakhir, bukan dari API |
| Tombol "Nilai" | — | ⚠️ HARDCODED (label statis) | `try_out_page.xml:62` | Sama seperti di atas, logic di `TryOutPage.kt:91-104` |
| Dialog "Peringatan" jam/zona waktu | — | ⚠️ HARDCODED (semua teks statis: "Peringatan", "Harap atur tanggal dan waktu ponsel ke \"Otomatis\"") | `TryOutPage.kt:109-135` | DIHITUNG dari `DateUtil().isTimeAutomatic()`/`isTimeZoneAutomatic()` (setting sistem Android, bukan API) |

---

## 2. TryOutSchedulePage (daftar jadwal try out)

**File:** `pages/tryout/TryOutSchedulePage.kt`, item layout `res/layout/try_out_item.xml`

**Trigger/masuk dari:** Tab "Jadwal" di `TryOutPage` (lihat di atas).

**Navigasi keluar:** Tap kartu/tombol "Ikuti Try Out" → `AkmDetailPage` dengan extra `id`, `examType = ExamType.TRYOUT`, `isSchoolScope` (`TryOutSchedulePage.kt:114-124`). Jika activity result membawa extra `showScore=true`, otomatis pindah ke tab Nilai (`TryOutSchedulePage.kt:127-135`).

Daftar diambil dari `viewmodel.listTryout()` yang membaca Room table `akm` terfilter `ExamType.TRYOUT` (`TryOutViewModel.kt:80-91`), diisi oleh `fetchTryout()` → `dbUtil.processTryoutResponse(data, true, ExamType.TRYOUT)` (`TryOutViewModel.kt:120-148`). Item dibind sebagai **`AkmTable`**, bukan `ListTryoutData` mentah.

### Elemen UI (per kartu, `try_out_item.xml`)

| Elemen (label visual) | Key data (path dari root JSON response) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Icon mapel | — | ⚠️ HARDCODED, `@drawable/ic_mapel_akm` statis | `try_out_item.xml:34` | Selalu sama, tidak dinamis dari data |
| Nama try out (`mapel`) | `data[].exams[].name` (digabung `distinctBy name` + `joinToString(", ")`) → `AkmTable.name` | String | Mapping: `OnKlasDbUtil.kt:1101-1111`. Binding: `try_out_item.xml:46` (`android:text="@{item.name}"`) | Gabungan nama semua exam dalam 1 jadwal try out, dipisah koma |
| Sub-teks jenis ujian (`teacher`, id XML `teacher`) | `data[].exams[].type` (hanya exam dengan `numberOfQuestions > 0`, digabung koma) → `AkmTable.type` | String | Mapping: `OnKlasDbUtil.kt:1112-1113`. Binding: `try_out_item.xml:65` | — |
| Badge kanan atas (`type`, teks) | `data[].type` → `AkmTable.exam_template` | String | Mapping: `OnKlasDbUtil.kt:1123` (`exam_template = it.type`). Binding: `try_out_item.xml:82` | Teks = raw field `type` dari API |
| Badge kanan atas (warna background) | DIHITUNG dari `AkmTable.exam_template == "AKM"` | Boolean-driven drawable | `TryOutSchedulePage.kt:103-106` | `tag_dark_blue` jika `exam_template == "AKM"`, selain itu `tag_purple` |
| "Tanggal" (label statis) | — | ⚠️ HARDCODED | `try_out_item.xml:106` | — |
| Tanggal (`time_plot`) | `data[].date` + `data[].startAt` → diparse `SimpleDateFormat` → `AkmTable.date_label` | String (format tanggal Indonesia) | Mapping: `OnKlasDbUtil.kt:1114-1120` (`akmDateLabelFormat.format(dateStart)`). Binding: `try_out_item.xml:120` | — |
| "Pukul" (label statis) | — | ⚠️ HARDCODED | `try_out_item.xml:145` | — |
| Jam (`attend`) | `"${data[].startAt} - ${data[].endAt}"` → `AkmTable.time_label` | String | Mapping: `OnKlasDbUtil.kt:1120`. Binding: `try_out_item.xml:159` | — |
| Label info bawah (`label_info`) | DIHITUNG dari `AkmTable.status` (bukan field API langsung) | String, computed | `try_out_item.xml:187` (`item.status == AkmStatus.AKM_STATUS_DOWNLOADING ? "sedang mendownload soal" : "ujian telah selesai"`) | Tampil hanya jika status **bukan** `DOWNLOADED`/`NEW` (`try_out_item.xml:192`) |
| Tombol "Ikuti Ujian" / "Download Soal" | DIHITUNG dari `AkmTable.status` | String, computed | `try_out_item.xml:208` | Teks "Ikuti Ujian" jika `status == DOWNLOADED`, selain itu "Download Soal". Tombol hanya tampil jika `status == DOWNLOADED` atau `status == NEW` (`try_out_item.xml:212`). **Catatan:** label default di XML item ini "Ikuti Ujian", namun teks tombol di-override lewat kode `TryOutSchedulePage.kt:107` menjadi **"Ikuti Try Out"** (karena `item.exam_type == ExamType.TRYOUT`) |

`AkmTable.status` sendiri adalah field **hasil komputasi** saat mapping (`OnKlasDbUtil.kt:1101-1109`): `AKM_STATUS_SCORED` jika `data[].isAssessed == true`, `AKM_STATUS_FINISHED` jika `data[].isDone == true`, atau nilai status lokal tersimpan sebelumnya (download/upload), default `0` (NEW).

---

## 3. TryOutScorePage (daftar nilai try out)

**File:** `pages/tryout/TryOutScorePage.kt`, item layout `res/layout/try_out_scored_item.xml`

**Trigger/masuk dari:** Tab "Nilai" di `TryOutPage`.

**Navigasi keluar:** Tap kartu → `AkmScoreDetailPage` dengan extra `id`, `isTryout=true`, `isSchoolScope` (`TryOutScorePage.kt:135-145`).

Data dari `viewmodel.listtryoutScore` → Room `akm` (status > `AKM_STATUS_FINISHED`, `ExamType.TRYOUT`), diisi oleh `loadTryoutScored()` → endpoint `listTryOutScored` (`TryOutViewModel.kt:151-205`).

### Elemen UI (per kartu, `try_out_scored_item.xml`)

| Elemen (label visual) | Key data (path dari root JSON response) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Icon mapel | — | ⚠️ HARDCODED, `@drawable/ic_mapel_akm` | `try_out_scored_item.xml:35` | Statis |
| Nama (`mapel`) | Sama seperti TryOutSchedulePage → `AkmTable.name` | String | `try_out_scored_item.xml:47` | — |
| Jenis (`teacher`) | `AkmTable.type` | String | `try_out_scored_item.xml:66` | — |
| Badge (`type`) teks + warna | `AkmTable.exam_template`; warna DIHITUNG (`tag_dark_blue` jika `"AKM"`, else `tag_purple`) | String | Binding teks: `try_out_scored_item.xml:83`. Warna: `TryOutScorePage.kt:126-129` | — |
| Tanggal (`time_plot`) | `AkmTable.date_label` | String | `try_out_scored_item.xml:120` | — |
| Jam (`attend`) | `AkmTable.time_label` | String | `try_out_scored_item.xml:159` | — |
| Label info (`label_info`) | DIHITUNG: `!item.score_status.empty ? item.score_status : (...)` | String, computed | `try_out_scored_item.xml:187` | Untuk data try out, `score_status` selalu **string kosong** karena `AkmTable.tryoutToTabel()` mengeset `score_status = ""` secara hardcode (`AkmEntities.kt:120`). Jadi teks yang benar-benar muncul untuk item try out adalah fallback: `"Proses upload jawaban"` (jika `status == FINISHED`) atau `"Menunggu penilaian"` (default). Tersembunyi jika `status == SCORED` (`try_out_scored_item.xml:192`) |
| Tombol "Lihat Nilai" | — | ⚠️ HARDCODED (label statis) | `try_out_scored_item.xml:209` | Tampil hanya jika `item.show_score == true` **dan** `status == AKM_STATUS_SCORED` (`try_out_scored_item.xml:214`). `show_score` ← `data[].show_score` API (`OnKlasDbUtil.kt:1121`) |

---

## 4. TryOutPassingGradePage / TryOutPassingGradeForm (cek passing grade)

**File host:** `pages/tryout/TryOutPassingGradePage.kt` (activity kosong, hanya nav-host, layout `try_out_passing_grade_page.xml`)
**File form:** `pages/tryout/TryOutPassingGradeForm.kt`, layout `res/layout/try_out_passing_grade_form.xml`

**Trigger/masuk dari:** Tombol "Cek Passing Grade" (`btn_action_pg`) di `AkmScoreDetailPage`, **hanya tampil jika `isTryout == true`** (`akm_score_detail_page.xml:278-286`, teks "Cek Passing Grade" HARDCODED; kondisi visibility di `AkmScoreDetailPage.kt:116-121`). Intent membawa extra `tryOutScored` = `data.exams.first().score.toString()` (`AkmScoreDetailPage.kt:124`, nilai exam try out pertama — lihat detail di Bagian F).

**Navigasi keluar:** Tombol back toolbar / back-press → `requireActivity().finishAfterTransition()` (`TryOutPassingGradeForm.kt:47-61`).

### Elemen UI

| Elemen (label visual) | Key data (path dari root JSON response) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Toolbar "Cek Passing Grade" | — | ⚠️ HARDCODED | `try_out_passing_grade_form.xml:38` | — |
| Judul "MASUKKAN DATA UNIVERSITAS" | — | ⚠️ HARDCODED | `try_out_passing_grade_form.xml:52` | — |
| Field "Pilih Universitas" (`tv_univ_name`) | `viewmodel.schoolList.name` (`UniversityItem.name`, hasil pilihan di `ListUnivPage`) | String | `try_out_passing_grade_form.xml:94` (`android:text="@{viewmodel.schoolList.name}"`) | Hint "Pilih Universitas" (hardcoded) saat belum dipilih. Tap membuka `ListUnivPage` bottom sheet (`TryOutPassingGradeForm.kt:68-72`) |
| Icon chevron kanan field universitas | — | ⚠️ HARDCODED, ikon statis | `try_out_passing_grade_form.xml:113` | — |
| Radio "Saintek" / "Soshum" | — | ⚠️ HARDCODED (label statis); nilai terpilih DIHITUNG jadi `viewmodel.major = "saintek"/"soshum"` | `try_out_passing_grade_form.xml:127-140`; logic `TryOutPassingGradeForm.kt:74-87` | Default tercentang: "Saintek" (`android:checkedButton="@id/rb_saintek"`, `try_out_passing_grade_form.xml:122`, dan `viewmodel.major.postValue("saintek")` di `TryOutPassingGradeForm.kt:74`) |
| Tombol "cek nilai" (`btn_calculate`) | — | ⚠️ HARDCODED (label statis, huruf kecil di XML) | `try_out_passing_grade_form.xml:168` | Memicu `viewmodel.checkPassingGrade()` |
| Text hasil inline (`tv_result_pg`) | `viewmodel.errorString` | String | `try_out_passing_grade_form.xml:151` (`android:text="@{viewmodel.errorString}"`) | ⚠️ **Elemen ini SECARA EFEKTIF TIDAK PERNAH TERLIHAT**: visibility default XML = `gone` (`try_out_passing_grade_form.xml:155`), dan setelah tombol "cek nilai" ditekan, kode secara eksplisit mengeset `visibility = View.INVISIBLE` (`TryOutPassingGradeForm.kt:93-99`). Jangan desain UI mengandalkan area ini untuk menampilkan hasil |
| **Hasil kalkulasi (sesungguhnya)** — dialog popup | `viewmodel.errorString` (pesan hasil dari `checkPassingGrade()`) | String, ditampilkan via `AlertDialog` custom (`PrettyAlertDialogBinding`) | Observer: `TryOutPassingGradeForm.kt:88` (`viewmodel.errorString.observe(..., this::prettyAlert)`). Dialog builder: `TryOutPassingGradeForm.kt:115-158` | DIHITUNG, bukan field langsung. Judul dialog = **"Hasil Kalkulasi"** (hardcoded default param, `TryOutPassingGradeForm.kt:117`). Isi pesan salah satu dari 5 string hardcoded di `TryOutViewModel.checkPassingGrade()` (`TryOutViewModel.kt:392-432`), tergantung: universitas belum dipilih → `"Pilih universitas terlebih dahulu"`; jurusan belum dipilih → `"Pilih penjurusan dari universitas anda terlebih dahulu"`; nilai passing grade universitas untuk jurusan itu (`university.saintek` atau `university.soshum`, dari `GET try-out/passing-grade`) `< 0` → `"Mohon maaf, nilai anda dibawah batas normal, harap melakukan tes lagi"`; nilai siswa (`tryOutScored`, dari extra intent) `>=` passing grade univ → `"Selamat, Anda berpeluang untuk masuk ke universitas ini"`; jika `<` → `"Mohon maaf, Peluang anda kecil untuk masuk ke universitas ini"` |

---

## 5. ListUnivPage (bottom sheet pilih universitas)

**File:** `pages/tryout/ListUnivPage.kt`, layout `res/layout/list_univ_page.xml`, item `res/layout/list_sekolah_item.xml`

**Trigger/masuk dari:** Tap field "Pilih Universitas" di `TryOutPassingGradeForm` (`TryOutPassingGradeForm.kt:68-72`).

**Navigasi keluar:** Tap item → callback `onItemClick` mengembalikan `UniversityItem` terpilih ke form, lalu `dismiss()` (`ListUnivPage.kt:136-141`). Tombol back toolbar → `dismiss()` (`ListUnivPage.kt:44`).

Data dari `viewmodel.listUniversity(search)` → Room `university` (`TryOutViewModel.kt:207-221`), diisi dari `GET try-out/passing-grade` → `UniversityResponse.data: List<UniversityItem>` (`TryoutModels.kt:12-25`).

### Elemen UI

| Elemen (label visual) | Key data (path dari root JSON response) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Toolbar "Pilih Universitas" | — | ⚠️ HARDCODED | `list_univ_page.xml:18` | — |
| Search box (hint "Cari Universitas") | Input pengguna, memicu `initData(search)` dengan debounce 500ms (`ListUnivPage.kt:53-56`) | — | `list_univ_page.xml:47` | — |
| Nama universitas (`text`) | `data[].name` → `UniversityItem.name` | String | `ListUnivPage.kt:137` (`binding.text.text = item.name`) | — |
| Kota (`text_city`) | — | ⚠️ **Elemen ada di layout tapi TIDAK PERNAH diisi kode** — tidak ada `binding.textCity` di `ListUnivPage.kt` | `list_sekolah_item.xml:27-35` | Default `visibility="gone"` di XML dan tidak pernah di-set `VISIBLE`/diisi teks oleh kode manapun. **Jangan desain field kota di kartu universitas** — datanya tidak ada di `UniversityItem` (hanya `id`, `name`, `saintek`, `soshum` — `TryoutModels.kt:20-25`) |
| Progress "sedang mengambil data" | — | ⚠️ HARDCODED | `list_univ_page.xml:98` | Tampil saat `firstLoad` sedang memuat (`ListUnivPage.kt:66-96`) |

---

## 6. Field/kondisi KHUSUS Try Out di AkmDetailPage & AkmScoreDetailPage

Halaman detail (`AkmDetailPage`) dan detail nilai (`AkmScoreDetailPage`) dipakai bersama oleh AKM, Ujian Sekolah, dan Try Out. Bagian ini **hanya** mencantumkan percabangan yang aktif saat `examType == ExamType.TRYOUT` (di `AkmDetailPage`) atau `isTryout == true` (di `AkmScoreDetailPage`). Field umum lain (rv_info, rv_types, dsb.) identik dengan AKM — lihat `docs/dokumentasi-asesmen.md`.

### 6.1 AkmDetailPage (saat `examType == ExamType.TRYOUT`)

| Elemen | Kondisi Try Out | Sumber (file:baris) |
|---|---|---|
| Judul toolbar & `title` | `"Try Out"` (vs `"Detail Asesmen"` untuk SCHOOL, `"Tes AKM"` untuk lainnya) | `AkmDetailPage.kt:73-74` |
| Navigasi back toolbar & hardware back | → `TryOutPage` (extra `isSchoolScope=true`) | `AkmDetailPage.kt:76-82`, `342-348` |
| Label jumlah soal (`beban_label`) | `"Jumlah Soal"` (vs `"Jumlah soal: "` untuk SCHOOL, `"Beban soal:"` untuk lainnya) | `AkmDetailPage.kt:107` |
| Tombol aksi awal (`btn_action`) teks default | `"Mulai Try Out"` | `AkmDetailPage.kt:108` |
| Info berakhir (`info`) | `"Try Out berakhir: <tanggal>"` (vs `"Asesmen berakhir: ..."` / `"AKM berakhir: ..."`), tanggal dari `data.schedule.date_end` (`AkmTable.date_end`) | `AkmDetailPage.kt:169` |
| Tombol saat status `AKM_STATUS_DOWNLOADED` | Teks `"Mulai Try Out"` | `AkmDetailPage.kt:239` |

### 6.2 AkmScoreDetailPage (saat `isTryout == true`, extra intent `"isTryout"`, `AkmScoreDetailPage.kt:45`)

| Elemen | Kondisi Try Out | Sumber (file:baris) |
|---|---|---|
| Judul toolbar | `"Hasil Quiz"` (vs `"Hasil Asesmen"` / `"Hasil AKM"`) | `AkmScoreDetailPage.kt:80` |
| Navigasi back toolbar & hardware back | → `TryOutPage` (extra `isSchoolScope=true`) | `AkmScoreDetailPage.kt:82-88`, `485-491` |
| Tombol "Cek Passing Grade" (`btn_action_pg`) | Hanya **VISIBLE** jika `isTryout==true` (selain itu `INVISIBLE`); klik → `TryOutPassingGradePage` dengan extra `tryOutScored` | `AkmScoreDetailPage.kt:116-125` |
| Nilai utama (`score_ujian`) | Untuk try out (bukan template "AKM"): teks = `data.exams.first().score.toString()` (nilai **desimal apa adanya**, tidak dibulatkan — beda dengan jalur `isSchoolScope` biasa yang memakai `.toInt()`) | `AkmScoreDetailPage.kt:174-180` |
| Tombol pembahasan (`btn_action`, label "Lihat Pembahasan" — lihat AKM) | Untuk try out: **selalu VISIBLE** (`if (isTryout) View.VISIBLE else {...}` — tidak bergantung `show_score`/`hasExplain` seperti jalur AKM biasa); klik → `AkmExplanationPage` dengan extra `isTryout=true`, `scheduleId` | `AkmScoreDetailPage.kt:229-236` |
| Refresh nilai dari server (`fetchScored()`) | Untuk try out: **di-skip** (`if (isTryout) return`) — nilai try out TIDAK diambil ulang lewat jalur `fetchDetailAkm`/`loadUjianSchoolScored`; jalur nilainya sendiri (via `processTryoutResponse` saat load list) | `AkmScoreDetailPage.kt:399-401` (komentar kode: *"Tryout punya jalur nilainya sendiri; fetchDetailAkm hanya menangani ExamType.SCHOOL"*) |

`AkmExplanationPage` (halaman pembahasan) berbagi komponen dengan AKM dan **di luar cakupan** dokumen ini.

---

# BAGIAN B — UJIAN

## 7. UjianPage (host, tab Ikuti/Nilai)

**File:** `pages/ujian/UjianPage.kt`, layout `res/layout/ujian_page.xml`

**Trigger/masuk dari:** Menu Pembelajaran index-4 versi lama **sudah tidak aktif** — di `pages/pembelajaran/PembelajaranPage.kt:670-676` baris `UjianPage::class.java` sudah **dikomentari** dan digantikan `AkmPage::class.java` (fitur "Asesmen" sekarang, lihat `docs/dokumentasi-asesmen.md`). Entry point yang **masih aktif** ditemukan di:
- `pages/notification/NotificationPage.kt:264-274` dan `pages/notification/DetailNotification.kt:155` — notifikasi dengan `page.parent == "COURSE"`, `page.child == "ASSIGNMENT"`, dan `child_id` kosong → buka `UjianPage` (jika `child_id` ada, langsung ke `UjianDetailPage`).
- `worker/ExamEndWorker.kt:49` dan `services/ExamStopService.kt:63` — notifikasi/deep-link setelah ujian otomatis diakhiri sistem (timer habis / app ditinggalkan), mengarah ke `UjianPage` (atau `Loginpage` jika sesi habis).

Kesimpulan: `UjianPage` kini **fitur legacy** yang hanya dijangkau lewat notifikasi terkait tugas/ujian ("ASSIGNMENT"), bukan lagi lewat menu utama.

**Navigasi keluar:**
- Back toolbar / hardware back → `finish()` (`UjianPage.kt:33, 63-65`).
- Tab "Ikuti Ujian" → fragment `ListUjianPage` via `action_global_listUjianPage` (`UjianPage.kt:36-39`).
- Tab "Nilai" → fragment `NilaiujianPage` via `action_global_nilaiujianPage` (`UjianPage.kt:41-44`).

### Elemen UI

| Elemen (label visual) | Key data | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Toolbar "Asesmen" | — | ⚠️ HARDCODED | `ujian_page.xml:21` | Judul toolbar TIDAK diubah di Kotlin (beda dengan try out yang override "Quiz") — tetap "Asesmen" |
| Tombol "Ikuti Ujian" | — | ⚠️ HARDCODED (label statis) | `ujian_page.xml:40` | Warna DIHITUNG dari state klik terakhir (`UjianPage.kt:51-61`) |
| Tombol "Nilai" | — | ⚠️ HARDCODED | `ujian_page.xml:62` | idem |

---

## 8. ListUjianPage (daftar ujian)

**File:** `pages/ujian/ListUjianPage.kt`, layout `res/layout/list_ujian_page.xml`, item `res/layout/ujian_item.xml`

**Trigger/masuk dari:** Tab "Ikuti Ujian" di `UjianPage`.

**Navigasi keluar:** Tombol "Mulai Ujian" (`btn_attend`) → `PrepareUjianPage` dengan extra `id` (`item.id.toString()`) dan `name` (`item.mapelName`) (`ListUjianPage.kt:147-154`), request code `4783`.

Data list: `viewmodel.listUjianStudent()` → Room `exam` terfilter tanggal terpilih (`UjianViewModel.kt:87-104`), diisi `fetchUjian()` → `GET exams-list` → `processListUjianStudent()` (`OnKlasDbUtil.kt:447-482`).

### Elemen UI

| Elemen (label visual) | Key data (path dari root JSON response) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Header tanggal (`date_label`) | `viewmodel.calKelas` (state kalender lokal, BUKAN dari API — hanya tanggal navigasi UI, di-set awal ke `Calendar.getInstance()`) | String, computed via `PresensiBindConverter.calendarToDateFormat` | `list_ujian_page.xml:20-32` (`android:text="@{PresensiBindConverter.calendarToDateFormat(viewmodel.calKelas)}"`) | Tanggal ini dipakai sebagai parameter `date` query ke `GET exams-list` (`UjianViewModel.kt:116`), jadi menentukan ISI list, bukan sekadar label |
| Panah kiri/kanan (`prev`/`next`) | — | ⚠️ HARDCODED (ikon statis `ic_lower_than`) | `list_ujian_page.xml:34-57` | Klik memanggil `viewmodel.substractCalendarKelas()`/`addCalendarKelas()` (mundur/maju 1 hari) — `UjianViewModel.kt:81-85` |
| Icon mapel (`image`) | `data[].layout.icon_image` → `ExamTable.mapelIcon` | Image URL | Mapping: `OnKlasDbUtil.kt:453`. Binding: `ujian_item.xml:24` (custom attr `imageUrl`) | — |
| Nama mapel (`mapel`) | `data[].layout.subject` → `ExamTable.mapelName` | String | Mapping: `OnKlasDbUtil.kt:454`. Binding: `ujian_item.xml:34` | — |
| Nama guru (`teacher`) | `data[].layout.teacher` → `ExamTable.teacherName` | String | Mapping: `OnKlasDbUtil.kt:455`. Binding: `ujian_item.xml:49` | — |
| "Tanggal" (label statis) | — | ⚠️ HARDCODED | `ujian_item.xml:70` | — |
| Tanggal (`time_plot`) | `data[].layout.date_human` → `ExamTable.dateLabel` | String | Mapping: `OnKlasDbUtil.kt:459`. Binding: `ujian_item.xml:83` | — |
| "Pukul" (label statis) | — | ⚠️ HARDCODED | `ujian_item.xml:105` | — |
| Jam (`attend`) | `"${data[].layout.start_at} - ${data[].layout.end_at}"` → `ExamTable.time` | String | Mapping: `OnKlasDbUtil.kt:462`. Binding: `ujian_item.xml:118` | — |
| Label info (`label_info`) | DIHITUNG/langsung: `data[].message` **atau** `"Selesai"` jika sudah pernah diselesaikan secara lokal → `ExamTable.message` | String | Mapping: `OnKlasDbUtil.kt:475` (`if (isFinished) "Selesai" else it.message`). Binding: `ujian_item.xml:144` | Tersembunyi jika `status == 1` (Persiapan) (`ujian_item.xml:149`) |
| Tombol "Mulai Ujian" | — | ⚠️ HARDCODED (label statis) | `ujian_item.xml:165` | Tampil hanya jika `status == 1` (Preparation) atau `status == 2` (Started) (`ujian_item.xml:169`). `status` DIHITUNG (bukan field API langsung): `3` jika sudah selesai lokal; `0` jika `!ready_to_download && !ready_to_start`; `1` jika `ready_to_download`; `2` jika `ready_to_start`; else `3` — sumber field mentah: `data[].ready_to_download`, `data[].ready_to_start` (`OnKlasDbUtil.kt:463-467`) |

---

## 9. PrepareUjianPage (mulai ujian — password, unduh soal, petunjuk)

**File:** `pages/ujian/PrepareUjianPage.kt`, layout `res/layout/prepare_ujian_page.xml`

**Trigger/masuk dari:** Tombol "Mulai Ujian" di `ListUjianPage` (extra `id`, `name`).

**Navigasi keluar:**
- Sukses `viewmodel.startExam(id, password)` → `TakeUjianPage` (meneruskan seluruh extras intent) lalu `finish()` (`PrepareUjianPage.kt:62-79`).
- Toolbar back → `finish()`, diblok dengan toast `"Sedang proses mendownload ujian"` jika `isDownloadingSoal == true` (`PrepareUjianPage.kt:38-44, 104-109`).
- Jika intent tidak membawa extra `id` → dialog "Perhatian" / "Ujian tidak valid" lalu `finish()` (`PrepareUjianPage.kt:92-100`, semua teks HARDCODED).

Soal otomatis diunduh saat halaman dibuka: `viewmodel.downloadSoal(id)` (`PrepareUjianPage.kt:88-90`) → `GET exams/{id}/download` → `processSoalUjian()` (`OnKlasDbUtil.kt:589-654`).

### Elemen UI

| Elemen (label visual) | Key data | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Toolbar "Mulai Ujian" | — | ⚠️ HARDCODED | `prepare_ujian_page.xml:32` | — |
| Progress "mendownload soal ujian" + spinner | `viewmodel.isDownloadingSoal` (state lokal, bukan field API) | Boolean-driven visibility | `prepare_ujian_page.xml:52-78` | Tampil selama proses download (`downloadSoal()`) |
| Input password (`in_password`) | Input pengguna, dikirim sebagai `password` ke `startExam()` (bukan ditampilkan dari API) | — | `prepare_ujian_page.xml:127-140` | Tampil hanya jika `!isDownloadingSoal && successDownloadSoal` (`prepare_ujian_page.xml:117`) |
| Tombol "Mulai Ujian" (`btn_start`) | — | ⚠️ HARDCODED (label statis) | `prepare_ujian_page.xml:154` | Enabled hanya jika field password terisi (`inPassword.text.length() > 0`, `prepare_ujian_page.xml:151`). Visible jika `!isDownloadingSoal && successDownloadSoal` |
| Tombol "Download Soal" (`btn_download`) | — | ⚠️ HARDCODED | `prepare_ujian_page.xml:174` | Visible jika `!isDownloadingSoal && !successDownloadSoal` (gagal/belum download) |
| Judul "Petunjuk Mengerjakan Ujian" | — | ⚠️ HARDCODED | `prepare_ujian_page.xml:199` | — |
| Daftar petunjuk (`rv_rules`) | — | ⚠️ HARDCODED — 4 kalimat statis di-hardcode langsung di kode, BUKAN dari API | `PrepareUjianPage.kt:53-60` (list literal: "Dilarang menutup/meninggalkan aplikasi...", "Ketika proses ujian sedang berlangsung...", "Kerjakan soal yang menurut Anda lebih mudah...", "Selamat mengerjakan, semoga berhasil!") | Selalu 4 item yang sama untuk semua ujian |

---

## 10. TakeUjianPage (kerjakan ujian)

**File:** `pages/ujian/TakeUjianPage.kt`, layout `res/layout/take_ujian_page.xml`, item soal `res/layout/question_item.xml` (pilihan ganda) / `res/layout/question_essay_item.xml` (esai), pilihan `res/layout/choice_item.xml`, dialog navigasi soal `res/layout/select_question_dialog.xml` + item `res/layout/question_select_item.xml`

**Trigger/masuk dari:**
- `PrepareUjianPage` setelah `startExam()` sukses (mode kerjakan, `scored=false` default) (`PrepareUjianPage.kt:70-77`).
- `UjianDetailPage` tombol "Lihat Jawaban", dengan extra `scored=true` (mode review, hanya setelah waktu ujian berakhir) (`UjianDetailPage.kt:62-83`).

**Navigasi keluar:**
- Mode kerjakan: tombol "Selesai" (`btn_finish`) → dialog konfirmasi → `endExam()` → `setResult(RESULT_OK)` + `finish()` (`TakeUjianPage.kt:133-143, 254-263`).
- Timer habis otomatis → `endExam(false)` lalu dialog "Ujian berakhir" → `finish()` (`TakeUjianPage.kt:147-202`).
- Mode review (`scored=true`): back button aktif normal (`onBackPressed` hanya jalan jika `scored`, `TakeUjianPage.kt:250-252`); toolbar back → `finish()` (`TakeUjianPage.kt:56-61`, hanya tampil tombol back saat `scored`).

### Elemen UI — Toolbar & navigasi soal

| Elemen (label visual) | Key data | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Judul toolbar (`title_label`) | — | ⚠️ HARDCODED, selalu **"Asesmen"** | `take_ujian_page.xml:69` | Tidak pernah di-override di Kotlin — perhatikan ini BUKAN nama ujian |
| Subjudul toolbar (`subtitle_label`) | Extra intent `"name"` → `binding.ujianTitle` | String | Set di `TakeUjianPage.kt:65` (`binding.ujianTitle = intent.getStringExtra("name")`). Binding: `take_ujian_page.xml:85` | Tersembunyi jika `ujian_title` null/kosong (`take_ujian_page.xml:86`). ⚠️ Saat masuk dari `UjianDetailPage` (mode review), intent **tidak membawa extra `"name"`** (`UjianDetailPage.kt:73-78`) sehingga subjudul ini **kosong/tersembunyi** di mode review |
| Timer (`timer_label`) | DIHITUNG dari `ExamTable.date` + `ExamTable.endAt` (Room lokal) dikurangi waktu sekarang | String `HH:MM:SS`, computed | Hitung mundur: `TakeUjianPage.kt:147-196` (`CountDownTimer`, method `formatTime`) | Hanya berjalan jika `!scored`. Tersembunyi total jika `scored==true` (`take_ujian_page.xml:102`) |
| Tombol "Selesai" (`btn_finish`) | — | ⚠️ HARDCODED (label statis) | `take_ujian_page.xml:116` | Tersembunyi jika `scored==true` |
| Label halaman (`page_label`, mis. "3/50") | DIHITUNG: `currentPage` (posisi scroll pager, state lokal) `+ "/" + total_page` (`listSoal.size`, jumlah soal terunduh) | String, computed | `take_ujian_page.xml:157` (`current_page + "/" + total_page`); `currentPage` diupdate di `TakeUjianPage.kt:86-87` | Tap membuka dialog navigasi nomor soal (`TakeUjianPage.kt:110-113`) |
| Tag status soal (`tag`) | DIHITUNG dari kombinasi `scored`, `is_correct` (`QuestionTable.is_correct`), `answered` (`QuestionTable.answered`) | String + drawable, computed | `take_ujian_page.xml:170, 175` | Mode review: `"Jawaban benar"`/`"Jawaban salah"` (hijau/merah). Mode kerjakan: `"Soal terjawab"`/`"Belum terjawab"` (primary/abu) |

### Elemen UI — Kartu soal pilihan ganda (`question_item.xml`)

| Elemen (label visual) | Key data (path dari root JSON response) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Gambar soal (`image`) | Saat download: `data[].layout.image` → `QuestionTable.image` (disimpan sbg path file lokal hasil download). Saat review: `template.questions[].layout.image` | Image (custom attr `imageFitUrl`) | Mapping download: `OnKlasDbUtil.kt:589-618`. Mapping review: `OnKlasDbUtil.kt:525-543` (field `image` diisi `it.layout.image` mentah, bukan file lokal, di jalur `processDetailUjian`). Binding: `question_item.xml:27` | Tersembunyi jika kosong (`question_item.xml:32`) |
| Teks soal (`question`) | `data[].layout.question` → `QuestionTable.question` | String (HTML, di-render `Html.fromHtml`) | Mapping: `OnKlasDbUtil.kt:533, 614`. Binding: `question_item.xml:41` (`Html.fromHtml(item.question)`) | Tersembunyi jika kosong |
| Daftar pilihan (`rv_choice` → `choice_item.xml`) | `data[].layout.choices[]` → `AnswerTable` | List | Mapping: `OnKlasDbUtil.kt:544-558, 620-641` | — |
| Label huruf pilihan (`label`, A/B/C/D...) | — | ⚠️ HARDCODED — huruf dihasilkan dari indeks array (`('A'..'Z').toList()`), BUKAN dari API | `TakeUjianPage.kt:325, 337-341` | — |
| Gambar pilihan (`image` di choice) | `data[].layout.choices[].file_path`/`image` → `AnswerTable.image` | Image | `choice_item.xml:54` | Tersembunyi jika kosong |
| Teks pilihan (`text_answer`) | `data[].layout.choices[].answer` → `AnswerTable.answer` | String (HTML) | `choice_item.xml:71` | Tersembunyi jika kosong |
| Warna bulatan/teks pilihan | DIHITUNG dari kombinasi `scored`, `choice.isCorrect` (`AnswerTable.isCorrect`, dari `data[].layout.choices[].is_true`), `myChoice.answerId == choice.id` (state pilihan siswa) | Drawable/warna, computed | `choice_item.xml:32, 46, 72` | Mode review: hijau jika `isCorrect`, primary jika itu pilihan siswa tapi salah, abu selainnya. Mode kerjakan: primary jika dipilih, abu jika tidak |

### Elemen UI — Kartu soal esai (`question_essay_item.xml`)

| Elemen (label visual) | Key data | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Gambar & teks soal | Sama seperti pilihan ganda | — | `question_essay_item.xml:29-49` | — |
| Input jawaban (`input_answer`) | `myAnswer.answerEssay` → `MyAnswerTable.answerEssay` (mode kerjakan: state ketikan siswa disimpan lokal; mode review: dari `data[].layout.essay_answer` API) | String, editable text | Mapping review: `OnKlasDbUtil.kt:560` (`insertMyAnswer(..., it.layout.essay_answer)`). Binding: `question_essay_item.xml:59` | Editable hanya jika `!scored` (`TakeUjianPage.kt:436`) |

### Elemen UI — Dialog navigasi nomor soal (`select_question_dialog.xml` / `question_select_item.xml`)

| Elemen (label visual) | Key data | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Nomor soal (`textview`) | — | ⚠️ HARDCODED — nomor urut dari posisi list (`adapterPosition + 1`), BUKAN field API | `TakeUjianPage.kt:236` | Warna primary jika `item.answered == true` |
| Icon centang | DIHITUNG dari `QuestionTable.answered` | Icon, computed | `question_select_item.xml:39-49` | Tampil hanya jika soal sudah dijawab |

### Dialog konfirmasi "Akhiri Ujian"

| Elemen | Key data | Sumber (file:baris) |
|---|---|---|
| Judul "Akhiri Ujian" | ⚠️ HARDCODED | `TakeUjianPage.kt:135` |
| Pesan | DIHITUNG: prefix `"Perhatian, masih ada soal yang belum terjawab. "` (⚠️ HARDCODED) muncul HANYA jika `listSoal.any { it.myAnswer == null }` (ada soal `MyAnswerTable` yang belum tercatat), diikuti kalimat tetap `"Anda yakin telah mengerjakan semua soal dan akan mengumpulkan jawaban sekarang?"` (⚠️ HARDCODED) | `TakeUjianPage.kt:136` |
| Tombol "Ya" / "Batal" | ⚠️ HARDCODED | `TakeUjianPage.kt:137, 141` |

---

## 11. NilaiujianPage (daftar nilai ujian)

**File:** `pages/ujian/NilaiujianPage.kt`, layout `res/layout/nilai_ujian_page.xml`, item `res/layout/ujian_scored_item.xml`

**Trigger/masuk dari:** Tab "Nilai" di `UjianPage`.

**Navigasi keluar:** Tap kartu (beberapa area klik: `root`, `image`, `mapel`, `teacher`, `timePlot`, `attend`, `labelInfo`, `score`) → `UjianDetailPage` dengan extra `id` (`NilaiujianPage.kt:161-168, 171-178`).

Data: `viewmodel.listUjianScoredStudent2` (custom `ItemKeyedDataSource`, `UjianViewModel.kt:148-227`) menggabungkan data lokal (`persistDB`, ujian yang sudah selesai tapi jawabannya belum terkirim) dan data server (`memoryDB`, dari `GET exams-scored` → `processUjianScoreStudent()`, `OnKlasDbUtil.kt:484-523`).

### Elemen UI

| Elemen (label visual) | Key data (path dari root JSON response) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Icon mapel, Nama mapel, Nama guru, Tanggal, Jam | Sama seperti `ListUjianPage` (`ExamTable.mapelIcon/mapelName/teacherName/dateLabel/time`) | — | `ujian_scored_item.xml:28-126` | — |
| Tombol "Dapatkan nilai" / "Memproses nilai" (`btn_send`) | DIHITUNG dari `ExamTable.status` (bukan field API langsung — status lokal 3=selesai/menunggu kirim, 4=terkirim) | String, computed | `ujian_scored_item.xml:150` | Enabled hanya jika `status == 3`. Tersembunyi jika `status == 3 || status == 4`... ⚠️ **kondisi tampil kontradiktif dengan enabled** (`ujian_scored_item.xml:144, 154`) — perlu klarifikasi ke dev, kemungkinan bug/legacy: secara literal tombol ini **selalu GONE** untuk status 3 maupun 4 (karena `status==3` masuk kondisi GONE juga), sehingga hanya terlihat pada status lain (mis. 0/1/2, tampil dengan enabled=false karena `status==3` gagal). Loading spinner icon tampil hanya jika `item.status == 4` (`NilaiujianPage.kt:141-147`) |
| Label info (`label_info`) | `data[].message` → `ExamTable.message` | String | Mapping: `OnKlasDbUtil.kt:499`. Binding: `ujian_scored_item.xml:175` | Tersembunyi jika `message` kosong (`ujian_scored_item.xml:180`) |
| Ring nilai (`score`/`score_label`) | `data[].layout.score` → `ExamTable.score` | Integer | Mapping: `OnKlasDbUtil.kt:501`. Binding: `ujian_scored_item.xml:204` (`"" + item.score`) | Tampil (menggantikan `label_info`) hanya jika `message` kosong (`ujian_scored_item.xml:192`) |

---

## 12. UjianDetailPage (detail nilai)

**File:** `pages/ujian/UjianDetailPage.kt`, layout `res/layout/ujian_detail_page.xml`

**Trigger/masuk dari:** Tap kartu di `NilaiujianPage`.

**Navigasi keluar:**
- Toolbar back → `finish()` (`UjianDetailPage.kt:40`).
- Tombol "Lihat Jawaban" (`btn_jawaban`) → `TakeUjianPage` dengan extra `id`, `scored=true` — **hanya jika** waktu sekarang sudah melewati `date + endAt` ujian; jika belum, `toast("Jawaban dapat dilihat setelah jadwal ujian berakhir")` (`UjianDetailPage.kt:62-83`).
- Tombol "Laporkan Kesalahan" (`btn_report`) → dialog konfirmasi → upload file log ke Firebase Storage (`UjianDetailPage.kt:88-114`).

Data: `viewmodel.memoryDB.ujian().get(id)` → `ExamTable` (`UjianDetailPage.kt:46-122`).

### Elemen UI

| Elemen (label visual) | Key data (path dari root JSON response) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Toolbar "Detail Ujian" | — | ⚠️ HARDCODED | `ujian_detail_page.xml:32` | — |
| Icon mapel (`mapel_img`) | `ExamTable.mapelIcon` | Image (bulat, custom attr `imageCircleUrl`) | `ujian_detail_page.xml:49` | — |
| Nama mapel (`mapel_name`) | `ExamTable.mapelName` | String | `ujian_detail_page.xml:59` | — |
| Nama guru (`teacher_name`) | `ExamTable.teacherName` | String | `ujian_detail_page.xml:71` | — |
| Ring nilai (`score`) | `ExamTable.score` | Integer | `ujian_detail_page.xml:95` (`"" + item.score`) | Tampil hanya jika `item.message` kosong (`ujian_detail_page.xml:84`) |
| Label info (`label_info`) | `ExamTable.message` | String | `ujian_detail_page.xml:108` | Tampil hanya jika `item.message` **tidak** kosong (kebalikan dari ring nilai) |
| Baris info (`rv_info`) — "Name" | `viewmodel.student.name` (data profil siswa lokal, bukan dari `ExamTable`) | String | `UjianDetailPage.kt:51` | Daftar key-value, render via `RowAdapter` (`pages/pembayaran/RowAdapter`) |
| Baris info — "Kelas" | `viewmodel.student.student_class.class_room.name` | String | `UjianDetailPage.kt:52` | — |
| Baris info — "Tanggal" | `ExamTable.dateLabel` (`it.dateLabel`) | String | `UjianDetailPage.kt:53` | — |
| Baris info — "Waktu" | `ExamTable.time` (`it.time`) | String | `UjianDetailPage.kt:54` | — |
| Baris info — "Sekolah" | `viewmodel.school.name` | String | `UjianDetailPage.kt:55` | — |
| Tombol "Lihat Jawaban" (`btn_jawaban`) | — | ⚠️ HARDCODED (label statis) | `ujian_detail_page.xml:143` | Enabled hanya jika `status==3`; visible hanya jika `message` kosong (`ujian_detail_page.xml:137, 147`). Klik dibatasi lagi secara runtime oleh perbandingan waktu (lihat "Navigasi keluar" di atas) |
| Tombol "Laporkan Kesalahan" (`btn_report`) | Visibility DIHITUNG dari **keberadaan file lokal** `filesDir/"${user_id}_${date}.txt"` — ⚠️ bukan field API sama sekali, murni file device | Boolean-driven | `UjianDetailPage.kt:85-88, 115-116` | Tampil hanya jika file log tersebut ada di penyimpanan device (artinya ujian sempat dijalankan di device ini dan sistem logging membuat file jejak) |
| Dialog "Laporkan Nilai" (judul, pesan, tombol) | — | ⚠️ HARDCODED, semua teks statis | `UjianDetailPage.kt:92-114` | Upload file log ke Firebase Storage path `logs/{logFileName}` |

---

# Kamus Field — Data Class `TryoutModels.kt` & `UjianModels.kt`

> Semua tipe di bawah adalah kontrak JSON asli dari API (via Moshi `@JsonClass`). Kolom "Dipakai di" merujuk ke bagian dokumen ini bila field tersebut benar-benar dirender ke UI (langsung atau via transformasi Room); field yang tidak dipakai UI ditandai "tidak dipakai di halaman yang didokumentasikan".

## `pages/tryout/TryoutModels.kt`

| Data class | Field | Tipe | Dipakai di |
|---|---|---|---|
| `UniversityResponse` | `data: List<UniversityItem>` | List | Root response `GET try-out/passing-grade` |
| `UniversityItem` | `id` | Int | Bagian 4/5 (identitas pilihan) |
| | `name` | String | Bagian 4 (`tv_univ_name`), Bagian 5 (`text` item universitas) |
| | `saintek` | Double | Bagian 4 — dibaca via `TryOutDao.checkPassingGradeSaintek(id)`, dipakai dalam kalkulasi dialog hasil |
| | `soshum` | Double | Bagian 4 — idem, via `checkPassingGradeSoshum(id)` |
| `ListTryoutResponse` | `data: List<ListTryoutData>` | List | Root response `GET try-out/schedules` & `GET try-out/scored` |
| `ListTryoutData` | `id` | Int | Dipetakan ke `AkmTable.id` |
| | `level` | Int? | Tidak dipakai di halaman yang didokumentasikan (dikomentari di `AkmTable.tryoutToTabel`) |
| | `isActive` | Int? | Tidak dipakai (hardcode `is_active = true` di `AkmTable.tryoutToTabel`, `AkmEntities.kt:112`) |
| | `date` | String | Sumber `AkmTable.date_label`/`date_start` (Bagian 2) |
| | `startAt` | String | Sumber `AkmTable.date_start`, `time_label` (Bagian 2) |
| | `endAt` | String | Sumber `AkmTable.date_end`, `time_label` (Bagian 2, Bagian 6.1 info berakhir) |
| | `show_score` | Boolean | Sumber `AkmTable.show_score` → tombol "Lihat Nilai" (Bagian 3) |
| | `isDone` | Boolean | Sumber status `AKM_STATUS_FINISHED` (Bagian 2) |
| | `isAssessed` | Boolean | Sumber status `AKM_STATUS_SCORED` (Bagian 2) |
| | `exams: List<TryoutExams>?` | List | Sumber `AkmTable.name/type` (Bagian 2), `AkmExamsTable` (Bagian 6.2 nilai) |
| | `score: List<TryoutScore>` | List | Dipakai untuk mengisi `exam.score` di `AkmExamsTable` per exam (`OnKlasDbUtil.kt:1179-1182`) — sumber nilai di Bagian 6.2 |
| | `type` | String | Sumber `AkmTable.exam_template` → badge (Bagian 2/3) |
| `ListTryoutExplanationResponse` | `data: ListTryoutExplanationData` | — | Dipakai `AkmExplanationPage` (di luar cakupan dokumen ini) |
| `ListTryoutExplanationData` | `list_instruction`, `student_answer` | List | idem, di luar cakupan |
| `ListInstruction` | `schedule_id`, `instruction_id`, `instruction_number`, `instruction_text`, `instruction_description` | — | Di luar cakupan (halaman pembahasan) |
| `StudentAnswer` | seluruh field (`answer_id`, `answer_text`, `answer_isTrue`, `answer_answered`, `answer_filePath`, `question`, `question_filePath`, `question_answerkey`, `question_answerkey_filePath`, `explanation`, `explanation_image`) | — | Di luar cakupan (halaman pembahasan) |
| `TryoutExams` | `id` | Int | Kunci relasi ke `AkmExamsTable` |
| | `name` | String | Bagian 2 (`AkmTable.name`, digabung) |
| | `type` | String | Bagian 2 (`AkmTable.type`, digabung; hanya exam dgn `numberOfQuestions>0`) |
| | `grade` | Any? | Tidak dipakai di halaman yang didokumentasikan |
| | `author: TryoutAuthor?` | Object | Tidak dipakai di halaman yang didokumentasikan |
| | `numberOfQuestions` | Int | Filter tampil-tidaknya exam di beberapa join, dan di `rvTypes`/`BebanAdapter` (bagian umum AKM) |
| | `score` | Double | Bagian 6.2 (`score_ujian`, nilai try out) |
| | `schoolType`, `schoolCities` | List/String | Tidak dipakai di halaman yang didokumentasikan |
| `TryoutScore` | `id`, `name`, `type`, `scored` | — | `scored` dipetakan ke `exam.score` (lihat `score` di atas) |
| `TryoutAuthor` | `id`, `nip`, `name` | — | Tidak dipakai di halaman yang didokumentasikan |

## `pages/ujian/UjianModels.kt`

| Data class | Field | Tipe | Dipakai di |
|---|---|---|---|
| `TestStudentResponse` | `data: List<TestData>` | List | Root response `GET exams-list` & `GET exams-scored` |
| `TestData` | `id` | Int | → `ExamTable.id` |
| | `message` | String | → `ExamTable.message` (Bagian 8, 11 `label_info`) |
| | `ready_to_download` | Boolean | Bahan hitung `ExamTable.status` (Bagian 8) |
| | `ready_to_start` | Boolean | Bahan hitung `ExamTable.status` (Bagian 8) |
| | `ready_to_end` | Boolean | Tidak dipakai di halaman yang didokumentasikan |
| | `layout: TestLayout` | Object | Lihat di bawah |
| `TestLayout` | `password` | String | Tidak dirender langsung (dipakai validasi start exam, bukan tampilan) — → `ExamTable.password` (tidak ditampilkan di UI manapun yang didokumentasikan) |
| | `date` | String | → `ExamTable.date` (dipakai hitung timer, Bagian 10) |
| | `date_human` | String | → `ExamTable.dateLabel` (Bagian 8, 11, 12) |
| | `start_at` | String | → `ExamTable.startAt`, `time` (Bagian 8, 11, 12) |
| | `end_at` | String | → `ExamTable.endAt`, `time` (Bagian 8, 11, 12); dipakai hitung timer & syarat "Lihat Jawaban" (Bagian 10, 12) |
| | `subject` | String | → `ExamTable.mapelName` & `subject` (Bagian 8, 11, 12) |
| | `icon_image` | String | → `ExamTable.mapelIcon` (Bagian 8, 11, 12) |
| | `teacher` | String | → `ExamTable.teacherName` (Bagian 8, 11, 12) |
| | `score` | Int | → `ExamTable.score` (Bagian 11, 12) |
| `TestDetailResponse` | `data: TestDetailData` | Object | Root response `GET exams/{id}/detail` |
| `TestDetailData` | `id` | Int | → testId di `processDetailUjian` |
| | `start_at`, `end_at` | String | Tidak dipakai langsung (level ini berbeda dari `layout.start_at/end_at`) di halaman yang didokumentasikan |
| | `template: ExamTemplate` | Object | Sumber daftar soal review (`template.questions`) |
| `ExamTemplate` | `id`, `name` | — | Tidak dipakai di halaman yang didokumentasikan |
| | `subject: MapelItem`, `teacher: TeacherItem` | Object | Tidak dipakai di halaman yang didokumentasikan (beda dari `TestLayout.subject/teacher` yang String) |
| | `questions: List<QuestionItem>` | List | Sumber soal mode review (Bagian 10) |
| `QuestionResponse`/`QuestionData`/`QuestionTemplate` | — | — | Tidak ditemukan pemanggilan endpoint yang mengembalikan tipe ini di scope pencarian — kemungkinan legacy/tidak terpakai |
| `DownloadSoalResponse` | `data: List<QuestionItem>` | List | Root response `PUT exams/{id}/download` — sumber soal mode kerjakan (Bagian 10) |
| `QuestionItem` | `id` | Int | → `QuestionTable.id` / `AnswerTable` relasi |
| | `layout: QuestionLayout` | Object | Lihat di bawah |
| `QuestionLayout` | `question` | String? | → `QuestionTable.question` (Bagian 10, teks soal) |
| | `image` | String | → `QuestionTable.image` (Bagian 10, gambar soal) |
| | `choices: List<AnswerItem>` | List | → `AnswerTable` (Bagian 10, pilihan jawaban) |
| | `essay_answer` | String | → `MyAnswerTable.answerEssay` mode review (Bagian 10) |
| | `is_essay_answer_true` | String | Bahan hitung `QuestionTable.is_correct` untuk soal esai (`OnKlasDbUtil.kt:536-539`) — tidak dirender teks langsung, hanya memengaruhi tag "Jawaban benar/salah" |
| `AnswerItem` | `id` | Int | → `AnswerTable.id` |
| | `answer` | String | → `AnswerTable.answer` (Bagian 10, teks pilihan) |
| | `is_true` | Boolean | → `AnswerTable.isCorrect` (Bagian 10, warna pilihan saat review) |
| | `file_path` | String | → `AnswerTable.image` (Bagian 10, gambar pilihan) |
| | `answered` | Boolean | Bahan hitung `QuestionTable.answered`/`is_correct` (`OnKlasDbUtil.kt:536, 540`) |

## Entity Room turunan yang dipakai langsung untuk binding UI (bukan JSON mentah, tapi wajib diketahui desainer)

| Entity | File | Field yang dirender UI |
|---|---|---|
| `AkmTable` | `pages/akm/AkmEntities.kt:26-93` | `name`, `type`, `exam_template`, `date_label`, `time_label`, `status`, `show_score`, `score_status`, `date_end`, `id`, `level`, `assessment_category`, `assessment_period` (2 terakhir dipakai di bagian umum AKM) |
| `AkmExamsTable` | `pages/akm/AkmEntities.kt:137-186` | `score`, `type`, `num_question` (Bagian 6.2, bagian umum AKM `rv_types`) |
| `ExamTable` | `pages/ujian/UjianEntities.kt:8-26` | `mapelIcon`, `mapelName`, `teacherName`, `dateLabel`, `time`, `message`, `score`, `status`, `date`, `endAt` |
| `QuestionTable` | `pages/ujian/UjianEntities.kt:32-40` | `question`, `image`, `answered`, `is_correct` |
| `AnswerTable` | `pages/ujian/UjianEntities.kt:46-53` | `answer`, `image`, `isCorrect`, `id`, `qId` |
| `MyAnswerTable` | `pages/ujian/UjianEntities.kt:61-66` | `answerId`, `answerEssay` |

---

# Ringkasan Peringatan Penting untuk Desainer

1. **Jangan menambahkan field "Kota" di kartu universitas** (`ListUnivPage`) — ada di layout tapi tidak pernah diisi kode; datanya juga tidak tersedia di `UniversityItem`.
2. **Hasil kalkulasi Passing Grade tampil sebagai dialog popup**, bukan teks inline di form — teks `tv_result_pg` di layar form secara efektif selalu tersembunyi.
3. **Judul toolbar `TakeUjianPage` selalu "Asesmen" (hardcoded)** — nama ujian sesungguhnya (kalau ada) muncul sebagai subjudul kecil di bawahnya, dan subjudul ini **kosong** saat membuka halaman dalam mode "Lihat Jawaban" dari `UjianDetailPage`.
4. **Nilai try out ditampilkan apa adanya (desimal, tidak dibulatkan)**, berbeda dari nilai ujian sekolah biasa yang dibulatkan (`.toInt()`) di komponen yang sama (`AkmScoreDetailPage`).
5. **Tombol "Lihat Pembahasan" di halaman nilai selalu tampil untuk Try Out**, tidak bergantung pada flag `show_score`/ketersediaan pembahasan seperti pada AKM biasa.
6. **Tombol "Laporkan Kesalahan" di `UjianDetailPage` murni bergantung pada keberadaan file log lokal di device**, sama sekali bukan dari field API manapun.
7. `UjianPage` adalah **fitur legacy**: entry menu utamanya sudah dikomentari di `PembelajaranPage.kt` dan digantikan `AkmPage` ("Asesmen"); satu-satunya jalan masuk yang masih aktif adalah lewat notifikasi tugas/ujian ("COURSE"/"ASSIGNMENT") atau saat ujian diakhiri otomatis oleh sistem. Entry point awal `TryOutPage` (dari menu mana pengguna pertama kali sampai ke halaman jadwal/nilai try out) **tidak ditemukan** dalam pencarian source untuk dokumen ini — perlu dikonfirmasi terpisah sebelum mendesain alur "cara masuk" ke fitur Try Out.
8. Tombol "Dapatkan nilai" (`ujian_scored_item.xml`) memiliki kondisi `enabled` dan `visibility` yang **tampak kontradiktif** (lihat Bagian 11) — perlu klarifikasi ke tim dev sebelum didesain ulang.
