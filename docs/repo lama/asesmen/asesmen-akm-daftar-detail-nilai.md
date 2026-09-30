# Dokumentasi Presisi — Fitur Asesmen (AKM): Jadwal, Detail, Nilai, Pembahasan

> Dokumen ini dibuat khusus untuk tim desain / AI design generator. Setiap klaim
> "elemen X menampilkan data Y" di bawah ini dibuktikan dengan kutipan kode
> binding asli (XML data-binding `@{...}` atau assignment Kotlin
> `binding.viewId.text = ...`), lengkap dengan file dan nomor baris. **Jangan
> menambah elemen atau field yang tidak tercantum di sini.**
>
> Cakupan kode yang dibaca: `app/src/main/java/id/diskola/app/pages/akm/*.kt`,
> `app/src/main/java/id/diskola/app/db/OnKlasDbUtil.kt` (fungsi
> `processAkmResponse`, yang menurunkan field-field tampilan dari respons API),
> dan seluruh layout XML `akm_*.xml` + `checkout_item.xml` / `score_item.xml`
> yang dipakai lewat `RowAdapter`.

## Catatan arsitektur penting (baca sebelum mendesain)

1. **Root data**: field API mentah ada di `ListAkmData` (`AkmModels.kt`). Field
   ini **tidak** dipakai langsung oleh UI — semuanya diproses dulu oleh
   `OnKlasDbUtil.processAkmResponse()` (`app/src/main/java/id/diskola/app/db/OnKlasDbUtil.kt:887-1065`)
   menjadi entity lokal Room `AkmTable` / `AkmExamsTable` (`AkmEntities.kt`).
   Semua layout `akm_item.xml`, `akm_score_item.xml`, `akm_detail_beban_soal_item.xml`,
   dan kode Kotlin di 6 halaman ini membaca dari `AkmTable`/`AkmExamsTable`
   (bukan langsung dari `ListAkmData`). Kolom `name`, `type`, `date_label`,
   `time_label` pada `AkmTable` adalah **hasil komputasi**, bukan field API
   1:1 — rinciannya di tabel per elemen di bawah.
2. **"Peserta", "NIS", "Kelas", "Sekolah"** yang muncul di panel info
   (`AkmDetailPage`, `AkmScoreDetailPage`) **BUKAN** berasal dari respons API
   Asesmen (AKM) sama sekali. Field-field itu dibaca dari `viewmodel.student`
   (`StudentItem`) dan `viewmodel.school` (`SekolahItem`) — data sesi
   login yang disimpan di SharedPreferences (`AkmViewModel.kt:63-78`), dan
   dimuat dari `LoginModels.kt`. Jangan mengira ini bagian dari payload
   asesmen.
3. **`AkmExplanationPage` secara aktual HANYA dipakai untuk jalur Try Out**
   (`isTryout = true`). Untuk asesmen sekolah/AKM biasa, tombol "Lihat
   Pembahasan" di `AkmScoreDetailPage` (status `EXPLAINED`) membuka
   `AkmTakeResumePage` (di luar cakupan 6 halaman ini), **bukan**
   `AkmExplanationPage`. Satu-satunya pemanggil `AkmExplanationPage` di
   seluruh kode adalah `AkmScoreDetailPage.kt:232`, dan hanya dieksekusi
   ketika `isTryout == true`. Detail lengkap ada di bagian halaman terkait.
4. Beberapa elemen memiliki **dua sumber yang saling menimpa** (binding XML
   `@{...}` DAN assignment manual di Kotlin, dengan `executePendingBindings()`
   dipanggil di antara keduanya). Kasus ini ditandai eksplisit di tabel
   dengan keterangan "⚠️ ada dua mekanisme" beserta urutan eksekusi yang
   ditemukan di kode, supaya tim tidak salah asumsi sumber kebenaran.
5. Ditemukan **dead code** (baris yang tidak pernah tereksekusi) di
   `AkmDetailPage.startExamWithPasswordGate` flow — lihat bagian AkmDetailPage.

---

## 1. AkmPage (host — tab "Jadwal" / "Nilai")

**File:** `AkmPage.kt`, layout `akm_page.xml`, nav graph `akm_nav.xml`.

**Trigger/masuk dari:** `HomePage` menekan menu Asesmen/AKM/Survey/Kelas
Eligible → `startActivity(Intent(HomePage, AkmPage::class.java))` dengan
extra `isSchoolScope: Boolean` dan `EXAM_TYPE: String?` (nilai yang
ditemukan di kode: `"SURVEY"`, `"ELIGIBLE"`, atau `null`/lainnya).
`AkmPage` adalah **container**: dua tombol di atas (`btn_ikuti`, `btn_nilai`)
mengganti fragment di dalam `NavHostFragment` (`page_container`,
`app:navGraph="@navigation/akm_nav"`), berisi `AkmListPage` (start
destination) dan `AkmScorePage`.

**Navigasi keluar:** tombol back toolbar / system back → selalu ke
`HomePage` (`AkmPage.kt:57-65`, `105-114`), tidak peduli tab aktif.

### Elemen UI

| Elemen (label visual) | Key data (path) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Judul toolbar | ⚠️ tidak ada key API — dari intent extra `EXAM_TYPE` | String | `AkmPage.kt:46,52-56` | `"Survey"` jika `EXAM_TYPE=="SURVEY"`; `"Kelas Eligible"` jika `=="ELIGIBLE"`; selain itu `"Asesmen"` |
| Tombol kiri (`btn_ikuti`) | ⚠️ tidak ada key API — dari intent extra `isSchoolScope` | String | `AkmPage.kt:70` | `"Jadwal"` jika `isSchoolScope==true`, else `"Ikuti AKM"` |
| Tombol kanan (`btn_nilai`) | ⚠️ tidak ada key API — dari intent extra `EXAM_TYPE` | String | `AkmPage.kt:72-76` | `"Riwayat Survey"` jika `EXAM_TYPE=="SURVEY"`, selain itu `"Nilai"` |
| Warna aktif tombol (stroke/text primary vs abu-abu) | tidak ada key — murni state UI lokal (tab mana yang sedang aktif) | Boolean (state) | `AkmPage.kt:78-102` | berubah saat tombol ditekan, tidak berasal dari API |

### Dialog yang dipicu dari halaman ini

Tidak ada dialog yang dipicu langsung dari `AkmPage`.

---

## 2. AkmListPage (daftar jadwal asesmen — card per item)

**File:** `AkmListPage.kt`, layout `akm_list_page.xml` (variabel `viewmodel:
AkmViewModel`), item `akm_item.xml` (variabel `item: AkmTable`).

**Trigger/masuk dari:** fragment awal (`app:startDestination="@id/akmListPage"`
di `akm_nav.xml`) saat `AkmPage` dibuka, atau lewat tab "Jadwal"/"Ikuti AKM".

**Navigasi keluar:** tap kartu / tombol → `AkmDetailPage` (`AkmListPage.kt:201-214`,
`requestCode=219`) dengan extra `id`, `examType=ExamType.SCHOOL`,
`isSchoolScope`, `isGovSchedule`. Saat kembali dengan
`resultCode=RESULT_OK` dan extra `showScore=true` → pindah ke tab nilai
(`findNavController().navigate(R.id.action_global_akmScorePage)` +
`(requireActivity() as AkmPage).openScoreList()`, `AkmListPage.kt:376-380`).

### ⚠️ Temuan penting: filter tanggal tidak fungsional di mode non-sekolah

`renderDays()` selalu dijalankan (`AkmListPage.kt:76`) sehingga pil tanggal
dan label bulan **selalu tampil**, apa pun scope-nya. Tapi observer yang
benar-benar memicu pemuatan ulang daftar berdasarkan tanggal terpilih
(`observeLocalByDate()`) **hanya didaftarkan jika `viewmodel.isSchoolScope
== true`** (`AkmListPage.kt:87-98`). Untuk mode AKM/Survey
(`isSchoolScope == false`), daftar dibaca dari `viewmodel.listAkm` tanpa
filter tanggal sama sekali (`AkmListPage.kt:100-109`) — memilih tanggal di
pil kalender tidak mengubah apa pun. Jangan mendesain seolah filter
tanggal berfungsi di semua mode.

### Elemen UI — header tanggal (hanya efektif saat `isSchoolScope=true`)

| Elemen (label visual) | Key data (path) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Label bulan-tahun (`date_label`, mis. "Januari 2026") | DIHITUNG, bukan field API — dari `viewmodel.calUjian` (state kalender lokal) diformat `SimpleDateFormat("MMMM yyyy")` | String | `AkmListPage.kt:42,90,341` | selalu tampil |
| Pil tanggal (`date_recycler` item, angka hari + nama hari) | DIHITUNG — daftar semua tanggal dalam bulan yang sedang ditampilkan (`1..last`), diformat `dd`/`EEE` | String | `AkmListPage.kt:321-338` | tanggal yang sudah lewat (`isPast`) ditampilkan pudar (`alpha=0.3f`) & tidak bisa ditekan |

### Elemen UI — kartu jadwal (`akm_item.xml`, var `item: AkmTable`)

| Elemen (label visual) | Key data (path) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Ikon mapel (`image`) | tidak ada key langsung — dari `item.gov_schedule` (Boolean, turunan `ListAkmData.gov_schedule`) | Boolean → drawable | `AkmListPage.kt:190-193` | `ic_school_login` jika `gov_schedule==true`, else `ic_mapel_akm` |
| Nama asesmen (`mapel`) | `item.name` — **DIHITUNG**: hasil `exams.distinctBy{name}.joinToString(", "){it.name}` dari daftar `ListAkmData.exams[].name` / `ListAkmData.exam.name` | String | binding `akm_item.xml:38`; komputasi di `OnKlasDbUtil.kt:906-934` | selalu tampil, max 2 baris |
| ID ujian (`exam_id_label`) | `item.id` (= `ListAkmData.id`) | Int → String, DIHITUNG string `"ID : " + id` | `akm_item.xml:53` | selalu tampil |
| Jenis tes (`teacher`) | `item.type` — **DIHITUNG**: `exams.filter{numberOfQuestions>0}.joinToString(", "){it.type}` dari `ListAkmData.exams[].type` | String | binding `akm_item.xml:68`; komputasi `OnKlasDbUtil.kt:935-936` | selalu tampil |
| Label statis "Tanggal" | ⚠️ HARDCODED | — | `akm_item.xml:89` | selalu tampil |
| Tanggal (`time_plot`) | `item.date_label` — **DIHITUNG**: `SimpleDateFormat("dd MMMM yyyy").format(dateStart)`, dan `dateStart` sendiri = parse `"${ListAkmData.date} ${ListAkmData.startAt}"` | String | binding `akm_item.xml:102`; komputasi `OnKlasDbUtil.kt:918-943` | selalu tampil |
| Label statis "Pukul" | ⚠️ HARDCODED | — | `akm_item.xml:124` | selalu tampil |
| Jam (`attend`) | `item.time_label` — **DIHITUNG**: string gabungan `"${ListAkmData.startAt} - ${ListAkmData.endAt}"` | String | binding `akm_item.xml:137`; komputasi `OnKlasDbUtil.kt:943` | selalu tampil |
| Info status (`label_info`) | tidak ada key langsung — dari `item.status` (Int enum lokal `AkmStatus`, diturunkan dari `ListAkmData.isAssessed/isDone/isQueued`) | Int → String, DIHITUNG | `akm_item.xml:163,168` | teks `"sedang mendownload soal"` hanya jika `status==DOWNLOADING`; selain itu `"ujian telah selesai"` (dipakai juga untuk status FINISHED/UPLOADED/SCORED/dsb — teks generik, tidak berubah per status). **Disembunyikan** jika `status` NEW atau DOWNLOADED |
| Tombol aksi (`btn_attend`) | ⚠️ ada dua mekanisme yang bertabrakan — lihat catatan di bawah tabel | String | `akm_item.xml:182,186`; `AkmListPage.kt:189` | lihat catatan |
| Pesan kosong (`empty_string`, "Belum terdapat ujian") | ⚠️ HARDCODED | — | `akm_list_page.xml:112` | tampil jika daftar kosong |

**Catatan `btn_attend`:** XML mem-binding
`text='@{item.status == AkmStatus.AKM_STATUS_DOWNLOADED ? "Ikuti Ujian" :
"Download Soal"}'` (`akm_item.xml:182`). Tapi kode Kotlin di
`Viewholder.bind()` (`AkmListPage.kt:187-199`) melakukan
`binding.item = item` → `binding.btnAttend.text = if
(viewmodel.isSchoolScope) "Ikuti Ujian" else "Ikuti AKM"` → baru
`binding.executePendingBindings()` di baris terakhir. Karena
`executePendingBindings()` dipanggil **setelah** assignment manual, hasil
akhir yang benar-benar tampil mengikuti ekspresi XML (berbasis
`item.status`), bukan baris Kotlin di atasnya — baris Kotlin tersebut
efektif tertimpa. Jangan mendesain teks tombol berdasarkan
`viewmodel.isSchoolScope`; teks yang benar-benar tampil hanya bergantung
pada `item.status == DOWNLOADED` (⇒ "Ikuti Ujian") atau tidak (⇒
"Download Soal"), dan tombol hanya terlihat saat `status` NEW atau
DOWNLOADED.

### Dialog yang dipicu dari halaman ini

**Dialog "Peringatan" (jam otomatis)** — dipicu di `onResume()` bila
`DateUtil().isTimeAutomatic()==false` atau `isTimeZoneAutomatic()==false`
(`AkmListPage.kt:127-154`), pakai layout `PresensiIzinLokasiDialogBinding`.

| Elemen | Key data | Tipe | Sumber | Kondisi |
|---|---|---|---|---|
| Judul: "Peringatan" | ⚠️ HARDCODED | — | `AkmListPage.kt:135` | — |
| Pesan: "Harap atur tanggal dan waktu ponsel ke \"Otomatis\"" | ⚠️ HARDCODED | — | `AkmListPage.kt:136` | — |
| Tombol "Buka Pengaturan" | ⚠️ HARDCODED, buka `Settings.ACTION_DATE_SETTINGS` | — | `AkmListPage.kt:141-147` | — |
| Tombol "Jangan Ubah" | ⚠️ HARDCODED, menutup activity | — | `AkmListPage.kt:148-153` | — |

Tidak ada data API yang ditampilkan; ini murni pemeriksaan setelan jam
perangkat.

**Dialog `MonthYearPickerDialog`** — dipicu tap `date_label`
(`AkmListPage.kt:77-85`); murni widget pemilih bulan/tahun, tidak
menampilkan data API.

---

## 3. AkmDetailPage (detail sebelum mulai)

**File:** `AkmDetailPage.kt`, layout `akm_detail_page.xml`. **Catatan
teknis:** file layout ini **tidak** berakar `<layout>` (tidak ada blok
`<data>` / ekspresi `@{...}` sama sekali di file ini) — jadi
`AkmDetailPageBinding` adalah ViewBinding biasa, dan **semua teks
ditentukan lewat kode Kotlin**, bukan lewat binding XML.

**Trigger/masuk dari:** `AkmListPage` tap kartu (`requestCode=219`) dengan
extra `id`, `examType`, `isSchoolScope`, `isGovSchedule`.

**Navigasi keluar:**
- Toolbar back / system back → `AkmPage` (jika `examType==SCHOOL`),
  `TryOutPage` (jika `TRYOUT`), atau `HomePage` (lainnya) —
  `AkmDetailPage.kt:75-97,340-362`.
- Tombol aksi (`btn_action`) berubah total tergantung `data.schedule.status`:
  - `AKM_STATUS_NEW`: teks **"Sinkronisasi Soal"**, tetap di halaman ini,
    memicu `viewmodel.downloadSoal(akmId)` (worker download) —
    `AkmDetailPage.kt:192-221`.
  - `AKM_STATUS_DOWNLOADING`: tombol disembunyikan, progress bar tampil —
    `AkmDetailPage.kt:223-232`.
  - `AKM_STATUS_DOWNLOADED` **dan** `Date() > date_start`: teks
    **"Mulai Asesmen"/"Mulai Try Out"/"Mulai AKM"** (tergantung
    `examType`) → `startExamWithPasswordGate()` → (lihat dialog di bawah)
    → `AkmTakeResumePage` (`requestCode=129`) — `AkmDetailPage.kt:235-260`.
  - `AKM_STATUS_DOWNLOADED` dan belum waktunya: tombol disembunyikan,
    label **"Asesmen belum dimulai"** (HARDCODED) — `AkmDetailPage.kt:256-259`.
  - `FINISHED`/`UPLOADED`: label **"Asesmen telah dikumpulkan"**
    (HARDCODED); jika `!isTimeUp` (dihitung dari jam saat ini vs jam akhir
    ujian) muncul alert **"Waktu habis, Asesmen telah dikumpulkan"**
    (HARDCODED) yang menutup halaman ke `AkmPage` — `AkmDetailPage.kt:262-284`.
  - `AKM_STATUS_SCORED`: teks **"Lihat Nilai"** → `AkmScoreDetailPage`
    (extra `id`, `examType`, **tanpa** `isSchoolScope`!) —
    `AkmDetailPage.kt:286-301` (lihat catatan potensi bug di bagian
    `AkmScoreDetailPage`).
- Selesai mengerjakan (`onActivityResult(129, finished=true)`) →
  `setResult(OK, showScore=true)` + `finish()`, kembali ke `AkmListPage`
  yang lalu pindah ke tab Nilai — `AkmDetailPage.kt:307-315`.

### Elemen UI

| Elemen (label visual) | Key data (path) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Judul toolbar | ⚠️ HARDCODED per `examType`: `"Try Out"` / `"Detail Asesmen"` / `"Tes AKM"` | String | `AkmDetailPage.kt:73-74` | selalu tampil |
| Ikon mapel (`image`) | ⚠️ HARDCODED, selalu `ic_mapel_akm` — **tidak** berubah walau `gov_schedule` true (berbeda dari `AkmListPage`) | — | `akm_detail_page.xml:42`; tidak ada override di `AkmDetailPage.kt` | selalu sama |
| Nama asesmen (`akm_name`) | `data.schedule.name` (= `AkmTable.name`, field DIHITUNG — sama seperti di `AkmListPage`) | String | `AkmDetailPage.kt:166` | selalu tampil |
| Jenis tes (`akm_type`) | `data.schedule.type` (= `AkmTable.type`, DIHITUNG) | String | `AkmDetailPage.kt:167` | selalu tampil |
| Info berakhir (`info`) | **DIHITUNG**, string gabungan: `"{label examType} berakhir: " + dateFormat("dd MMMM yyyy, HH:mm").format(data.schedule.date_end)`; `date_end` sendiri DIHITUNG dari `ListAkmData.date + endAt` | String | `AkmDetailPage.kt:168-169` | label depan HARDCODED per `examType`: "Asesmen"/"Try Out"/"AKM" |
| Kartu peringatan password (`password_notice`) | tidak ada key langsung — dari `requiresPassword` = `examType==SCHOOL && isSchoolScope && AkmSettings.requiresPassword(akmId)` (setting lokal, sumber asli `ListAkmData.requires_password`) | Boolean, DIHITUNG | `AkmDetailPage.kt:163-165,170` | tampil hanya jika kondisi di atas true |
| — judul kartu password: "Ujian ini memakai password" | ⚠️ HARDCODED | — | `akm_detail_page.xml:126` | mengikuti visibility kartu |
| — pesan kartu password | ⚠️ HARDCODED: "Silakan meminta password ke pengawas ujian sebelum mulai mengerjakan asesmen." | — | `akm_detail_page.xml:141` | mengikuti visibility kartu |
| Baris info "ID Ujian" | `data.schedule.id` (= `ListAkmData.id`) | Int→String | `AkmDetailPage.kt:174` | selalu tampil |
| Baris info "Peserta" | ⚠️ **BUKAN dari API AKM** — `viewmodel.student.name` (sesi login lokal, `StudentItem`) | String | `AkmDetailPage.kt:175`; model `LoginModels.kt` (~L224) | selalu tampil |
| Baris info "NIS" | ⚠️ **BUKAN dari API AKM** — `viewmodel.student.nis` bila tidak kosong, else `viewmodel.student.nisn` | String | `AkmDetailPage.kt:176` | selalu tampil |
| Baris info "Kelas" | ⚠️ **BUKAN dari API AKM** — `viewmodel.student.student_class.class_room.name` | String | `AkmDetailPage.kt:177` | selalu tampil |
| Baris info "Kategori" | `data.schedule.assessment_category` — **DIHITUNG**: exam pertama di `exams[]` yang `assessment_category` tidak kosong, fallback ke nilai lokal tersimpan sebelumnya | String, `"-"` bila kosong | `AkmDetailPage.kt:178`; komputasi `OnKlasDbUtil.kt:948-949` | selalu tampil |
| Baris info "Periode" | `data.schedule.assessment_period` — DIHITUNG sama seperti Kategori | String, `"-"` bila kosong | `AkmDetailPage.kt:179`; komputasi `OnKlasDbUtil.kt:950-951` | selalu tampil |
| Baris info "Sekolah" | ⚠️ **BUKAN dari API AKM** — `viewmodel.school.name` (`SekolahItem`, sesi login lokal) | String | `AkmDetailPage.kt:180`; model `LoginModels.kt:27` | selalu tampil |
| Label "Beban soal:" | ⚠️ HARDCODED per `examType`: "Jumlah Soal" / "Jumlah soal: " / "Beban soal:" | — | `AkmDetailPage.kt:107` | selalu tampil |
| Baris beban soal (`rv_types` item) | `"- " + item.type + " (" + item.num_question + " Soal)"` — `item.type`/`num_question` = `AkmExamsTable.type`/`num_question` (turunan `AkmExams.type` & `numberOfQuestions`) | String, DIHITUNG | binding `akm_detail_beban_soal_item.xml:15`; sumber `AkmDetailPage.kt:184` (filter `num_question > 0`) | satu baris per exam dengan `num_question > 0` |
| Progress bar & label sinkronisasi | `data.schedule.download_progress` (field **lokal**, di-update worker `AkmDownloader`, bukan field API) / `numQuestions` = `data.exams.sumBy{num_question}` | Int, sebagian lokal sebagian DIHITUNG | `AkmDetailPage.kt:203,224-231` | tampil hanya saat status NEW (setelah ditekan) atau DOWNLOADING |
| Tombol aksi (`btn_action`) | lihat rincian status di bagian "Navigasi keluar" di atas | String, DIHITUNG dari `status` | `AkmDetailPage.kt:190-302` | teks & aksi berbeda total per status |

### Dialog yang dipicu dari halaman ini

**1. Dialog "Password Ujian"** (`akm_check_password_dialog.xml`,
`AkmCheckPasswordDialogBinding`) — dipicu `startExamWithPasswordGate()`
saat `requiresPassword==true && passwordChecked==false`
(`AkmDetailPage.kt:459-477,479-531`).

| Elemen | Key data | Tipe | Sumber | Kondisi |
|---|---|---|---|---|
| Judul dialog: "Password Ujian" | ⚠️ HARDCODED | — | `AkmDetailPage.kt:484` | — |
| Kartu info judul: "Asesmen memakai password" | ⚠️ HARDCODED | — | `akm_check_password_dialog.xml:44` | — |
| Kartu info pesan | ⚠️ HARDCODED: "Silakan meminta password ke pengawas ujian sebelum mulai mengerjakan asesmen." | — | `akm_check_password_dialog.xml:59` | — |
| Label input: "Masukkan password dari pengawas" | ⚠️ HARDCODED | — | `akm_check_password_dialog.xml:75` | — |
| Hint input: "Contoh: K7MP2X" / helper "Password akan terlihat saat diketik." | ⚠️ HARDCODED | — | `akm_check_password_dialog.xml:88,97` | — |
| Error di bawah input | pesan **dari respons server** (`ApiException.message`, hasil parsing key `"message"` di body error) bila mengandung kata "Password ujian"; else `"Password ujian tidak sesuai"` (HARDCODED) bila `checked==false` tanpa exception | String, dari API | `AkmDetailPage.kt:486-528`; parsing pesan `AkmDetailPage.kt:486-508` (`resolveHttpErrorMessage`) | muncul saat validasi password gagal |
| Tombol "Lanjut" | memanggil `viewmodel.checkPasswordUjianSchool(akmId, password)` → `POST` cek password (`ExamPasswordCheckResponse.data.checked`) | — | `AkmDetailPage.kt:493-530`; API model `AkmModels.kt:120-129` | wajib isi password, jika kosong error "Password ujian wajib diisi" (HARDCODED) |
| Tombol "Batal" | ⚠️ HARDCODED, menutup dialog | — | `AkmDetailPage.kt:487` | — |

**2. Dialog error pemeriksaan password** (`showPasswordCheckError`,
`AkmDetailPage.kt:533-562`) — judul & pesan ditentukan dengan mencocokkan
substring pada pesan error API (`safeMessage`):

| Kondisi pencocokan pesan error API | Judul (HARDCODED) | Pesan (HARDCODED) |
|---|---|---|
| mengandung "sudah digunakan" | "Password Sudah Digunakan" | "Password ujian sudah digunakan. Minta guru generate password baru." |
| mengandung "perangkat lain" | "Perangkat Berbeda" | "Password sudah dipakai di perangkat lain. Gunakan HP yang dipakai saat cek password, atau minta guru generate password baru." |
| mengandung "Terlalu banyak" | "Terlalu Banyak Percobaan" | "Terlalu banyak percobaan akses ujian. Tunggu beberapa saat sebelum mencoba lagi." |
| lainnya | (tanpa judul khusus) | isi `safeMessage` langsung dari server (fallback HARDCODED bila kosong: "Password ujian belum bisa diperiksa. Silahkan ulangi beberapa saat lagi") |

**3. Dialog "Butuh Koneksi Internet"** — dipicu bila mode ketat
(`AkmSettings.isStrictMode`, dari setting server `exam_lock_mode`) aktif
dan perangkat offline (`AkmDetailPage.kt:576-594`). Semua teks
**HARDCODED**: judul "Butuh Koneksi Internet", pesan "Ujian ini harus
dimulai dalam keadaan terhubung internet...", tombol "Mengerti".

**4. Dialog konfirmasi mulai ujian ("Pemberitahuan !!!")** —
`showStartExamDialog()` (`AkmDetailPage.kt:596-639`).

| Elemen | Key data | Tipe | Sumber | Kondisi |
|---|---|---|---|---|
| Judul: "Pemberitahuan !!!" | ⚠️ HARDCODED | — | `AkmDetailPage.kt:627` | — |
| Isi pesan (aturan ujian) | **DIHITUNG**: pemilihan blok kalimat mana yang dipakai bergantung pada `strict` (dari `AkmSettings.isStrictMode`, sumber server `exam_lock_mode`); kalimat itu sendiri HARDCODED | String, gabungan kondisional | `AkmDetailPage.kt:605-624` | 2 varian teks tergantung mode ketat aktif/tidak |
| Tombol "OK" | ⚠️ HARDCODED; `onClick` → `startExamIfAllowed(strict)` | — | `AkmDetailPage.kt:631-635` | — |

### ⚠️ Dead code ditemukan

Di `setOnClickListener` tombol "Mulai Asesmen" (`AkmDetailPage.kt:240-254`):
```kotlin
setOnClickListener {
    startExamWithPasswordGate()
    return@setOnClickListener
    // requestPermissionAndStartExam()
    startActivityForResult(...)
}
```
Baris `startActivityForResult(...)` setelah `return@setOnClickListener`
**tidak pernah tereksekusi**. Akibatnya fungsi-fungsi
`requestPermissionAndStartExam()`, `showPermissionRationaleDialog()`,
`showSettingsDialog()`, dan `startCountdownService()`
(`AkmDetailPage.kt:364-431`) **tidak pernah dipanggil** dalam alur
aplikasi saat ini. Jangan mendesain dialog permission
Foreground-Service — dialog itu ada di kode tapi mati (unreachable).

---

## 4. AkmScorePage (daftar nilai/riwayat)

**File:** `AkmScorePage.kt`. Menggunakan **layout yang sama** dengan
`AkmListPage`, yaitu `akm_list_page.xml` (di-inflate lewat
`AkmListPageBinding`), dengan `date_label`, `date_recycler`, `prev`, `next`
disembunyikan secara eksplisit (`AkmScorePage.kt:56-60`). Item daftar
pakai `akm_score_item.xml` (var `item: AkmTable`).

**Trigger/masuk dari:** tab "Nilai"/"Riwayat Survey" di `AkmPage`, atau
redirect otomatis dari `AkmListPage` setelah selesai ujian
(`showScore=true`).

**Navigasi keluar:** tombol "Lihat Ujian" (`btn_attend`) → tampil hanya
saat `status` SCORED/EXPLAINED — `AkmScoreDetailPage`
(`requestCode=219`) dengan extra `id`, `isSchoolScope` —
`AkmScorePage.kt:261-270`.

### Elemen UI (`akm_score_item.xml`, var `item: AkmTable`)

| Elemen (label visual) | Key data (path) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Ikon mapel (`image`) | ⚠️ HARDCODED, selalu `ic_mapel_akm` (tidak ada logika `gov_schedule` di item ini, berbeda dari `akm_item.xml`) | — | `akm_score_item.xml:35` | selalu sama |
| Tombol upload ulang (`btn_upload`) | ⚠️ ada dua mekanisme (lihat catatan di bawah) | Boolean, DIHITUNG | `akm_score_item.xml:46`; `AkmScorePage.kt:184-192` | lihat catatan |
| Nama asesmen (`mapel`) | `item.name` (DIHITUNG, sama seperti `AkmListPage`) | String | `akm_score_item.xml:56` | selalu tampil |
| ID ujian (`exam_id_label`) | `item.id` | Int→String | `akm_score_item.xml:71` | selalu tampil |
| Jenis tes (`teacher`) | `item.type` (DIHITUNG) | String | `akm_score_item.xml:86` | selalu tampil |
| Tanggal (`time_plot`) | `item.date_label` (DIHITUNG) | String | `akm_score_item.xml:120` | selalu tampil |
| Jam (`attend`) | `item.time_label` (DIHITUNG) | String | `akm_score_item.xml:155` | selalu tampil |
| Info status (`label_info`) | ⚠️ ada dua mekanisme (lihat catatan) | String | `akm_score_item.xml:181`; `AkmScorePage.kt:194-209` | lihat catatan |
| Tombol "Lihat Ujian" (`btn_attend`) | tidak ada key langsung — visible bila `item.status` SCORED/EXPLAINED, atau dipaksa tampil bila upload-ulang barusan sukses (`wasSuccess`, state sesi, bukan API) | Boolean | `akm_score_item.xml:216`; `AkmScorePage.kt:205-209` | — |

**Catatan `btn_upload` (visibility):** XML: `visibility='@{item.status ==
AkmStatus.AKM_STATUS_UPLOADED ? View.VISIBLE : View.GONE}'`
(`akm_score_item.xml:46`). Kotlin di `Viewholder.bind()`
(`AkmScorePage.kt:172-192`) memanggil `binding.item = item` lalu
`binding.executePendingBindings()` **di awal fungsi** (baris 173-174),
baru kemudian menghitung `showUpload` (kombinasi `status==UPLOADED` atau
`status==FINISHED` atau upload sebelumnya gagal, dikecualikan bila
`item.score_status` (raw field `ListAkmData.status`) mengandung "tidak
mengerjakan"/"sesi terlewat") dan meng-assign
`binding.btnUpload.visibility` secara manual (baris 190-192) **setelah**
`executePendingBindings()`. Karena assignment manual ini terjadi
belakangan dan tidak ada `executePendingBindings()` lagi sesudahnya,
**logika Kotlin `showUpload`-lah yang benar-benar menentukan tampilan
akhir**, bukan ekspresi XML sederhana.

**Catatan `label_info` (teks):** Urutan eksekusi sama seperti di atas.
Nilai XML awal: `!item.score_status.empty ? item.score_status :
(item.status==DOWNLOADING ? "" : (item.status==FINISHED ? "Proses upload
jawaban" : "Menunggu penilaian"))` (`akm_score_item.xml:181`, semua
string selain `score_status` HARDCODED). Kotlin lalu **menimpa** teks ini
bila salah satu kondisi terpenuhi (dieksekusi setelah
`executePendingBindings()` pertama): `"Mengupload jawaban..."` (sedang
proses), `"Gagal mengirim jawaban. Coba lagi"` (upload gagal), atau
`"Upload ulang tersedia dalam {n}s"` (masih cooldown lokal) —
`AkmScorePage.kt:195-202`. Jika tidak ada kondisi Kotlin yang terpenuhi,
teks XML di atas yang tampil (jadi `item.score_status`, field API asli,
adalah sumber default).

### Dialog yang dipicu dari halaman ini

**1. Dialog cooldown "Tunggu"** (`showCooldownDialog`,
`AkmScorePage.kt:339-355`) — dipicu jika tombol upload ditekan saat masih
dalam masa tunggu (cooldown 60 detik **tersimpan lokal** di
SharedPreferences per `akmId`, bukan dari server). Judul "Tunggu"
HARDCODED, pesan `"Upload ulang tersedia dalam {n} detik"` DIHITUNG dari
sisa waktu lokal, diperbarui tiap detik via `CountDownTimer`.

**2. Dialog "Data jawaban tidak ditemukan"** (`AkmScorePage.kt:222-234`) —
dipicu bila `hasUploadData(item.id)` (query Room lokal) mengembalikan
`false`. Semua teks HARDCODED: judul "Informasi", pesan "Data jawaban
tidak ditemukan di perangkat ini. anda tidak bisa melakukan upload ulang
jawaban !!", tombol "Tutup".

**3. Dialog konfirmasi "Upload ulang ujian"** (`AkmScorePage.kt:237-254`) —
semua teks HARDCODED: judul "Upload ulang ujian", pesan "Yakin ingin
upload ulang jawaban ujian ini?", tombol "Ya, Upload"/"Batal". Menekan
"Ya, Upload" memicu cooldown 60 detik lokal dan
`viewmodel.uploadAnswer(item.id)`.

---

## 5. AkmScoreDetailPage (detail nilai + ring skor + beban soal)

**File:** `AkmScoreDetailPage.kt`, layout `akm_score_detail_page.xml`.
**Catatan teknis penting:** file layout ini **tidak berakar `<layout>`**
(tidak ada blok `<data>`), padahal isinya memuat ekspresi
`android:text="@{table.name}"` dan `@{table.type}`
(`akm_score_detail_page.xml:55,74`). Karena tidak ada `<layout>` root,
Data Binding **tidak memprosesnya** — ekspresi `@{...}` ini **mati/tidak
pernah dievaluasi** dan `AkmScoreDetailPageBinding` adalah ViewBinding
biasa. Nilai yang benar-benar tampil ditentukan sepenuhnya oleh assignment
manual di Kotlin (`binding.akmName.text = data.schedule.name`, dst).
Jangan mengira ada variabel binding bernama `table` yang aktif.

**Trigger/masuk dari:**
1. `AkmScorePage` tombol "Lihat Ujian" → extra `id`, `isSchoolScope`
   (`AkmScorePage.kt:261-270`).
2. `AkmDetailPage` tombol "Lihat Nilai" (status SCORED) → extra `id`,
   `examType` **saja, tanpa `isSchoolScope`** (`AkmDetailPage.kt:290-298`).

**⚠️ Potensi bug lintas-halaman:** `isSchoolScope` dibaca dengan default
`true` (`AkmScoreDetailPage.kt:44`,
`intent.getBooleanExtra("isSchoolScope", true)`). Karena pemanggilan dari
`AkmDetailPage` **tidak pernah mengirim extra ini**, setiap kali halaman
dibuka lewat "Lihat Nilai" nilainya **selalu dianggap `true`** — walau
sebenarnya asesmen yang dilihat berasal dari mode AKM non-sekolah
(`isSchoolScope==false`, kondisi ini mungkin terjadi karena
`examType==SCHOOL` tidak selalu berarti tab "Ujian Sekolah"; lihat
`AkmDetailPage`/`AkmListPage`, keduanya membedakan `isSchoolScope` secara
independen dari `examType`). Dampaknya, `fetchScored()`
(`AkmScoreDetailPage.kt:399-419`) bisa memanggil endpoint nilai yang
salah (`loadUjianSchoolScored()` padahal seharusnya `loadAkmScored()`).
Tim desain tidak perlu mendesain ulang untuk ini, tapi perlu tahu risiko
data nilai yang salah/kosong bisa muncul di jalur ini.

**Navigasi keluar:** toolbar back → `TryOutPage` (jika `isTryout`) atau
`AkmPage` (`AkmScoreDetailPage.kt:81-97,483-500`). Tombol `btn_action`
navigasi tergantung status (lihat tabel).

### Elemen UI

| Elemen (label visual) | Key data (path) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Judul toolbar | ⚠️ HARDCODED: "Hasil Quiz"/"Hasil Asesmen"/"Hasil AKM" tergantung `isTryout`/`isSchoolScope` | String | `AkmScoreDetailPage.kt:79-80` | selalu tampil |
| Ikon mapel (`image`) | ⚠️ HARDCODED, selalu `ic_mapel_akm` | — | `akm_score_detail_page.xml:44` | selalu sama |
| Nama asesmen (`akm_name`) | `data.schedule.name` (DIHITUNG, lihat catatan teknis di atas — bukan lewat XML binding `table.name`) | String | `AkmScoreDetailPage.kt:153` | selalu tampil |
| Jenis tes (`akm_type`) | `data.schedule.type` (DIHITUNG) | String | `AkmScoreDetailPage.kt:154` | selalu tampil |
| Ring skor tunggal (`score_ujian`) | `data.exams.first().score` — DIHITUNG dari `AkmExamsTable.score` (turunan `ListAkmData.score[]`/`exam_score`); `.toInt()` bila `isSchoolScope`, apa adanya bila `isTryout` | Double/Int→String | `AkmScoreDetailPage.kt:176-180` | hanya tampil bila `data.schedule.exam_template != "AKM"` **dan** `show_score==true` **dan** `scoreReady==true` (nilai server sudah pernah diambil sejak halaman dibuka) |
| Indikator memuat nilai (`score_loading`, "Sedang memuat nilai…") | string resource `@string/akm_score_loading` (HARDCODED string resource, bukan dari API) | — | `strings.xml:182`; kondisi `AkmScoreDetailPage.kt:158-168` | tampil selama `scoreReady==false` dan `data.exams` tidak kosong |
| Ring skor per sub-tes (`rv_score`, template "AKM") | tiap ring = `data.exams[i].score` (`AkmExamsTable.score`) | Double→String | `AkmScoreDetailPage.kt:184-196` (`ScoreAdapter`, `score_item.xml`) | hanya untuk `data.schedule.exam_template == "AKM"`. ⚠️ **Tidak ada label nama sub-tes** yang tampil di ring — fungsi `addDiagramLabel()` yang seharusnya menambah label sudah dikomentari/kosong (`AkmScoreDetailPage.kt:421-428`), jadi hanya angka nilai tanpa keterangan mata pelajaran per ring |
| Baris info "ID Ujian"/"Peserta"/"NIS"/"Kelas"/"Kategori"/"Periode"/"Sekolah" | sama persis dengan `AkmDetailPage` — lihat tabel di bagian 3 (Peserta/NIS/Kelas/Sekolah dari sesi login lokal, Kategori/Periode DIHITUNG) | — | `AkmScoreDetailPage.kt:210-224` | selalu tampil |
| Baris info tambahan "Level" | `data.schedule.level` (= `ListAkmData.level`, disimpan apa adanya) | Int→String | `AkmScoreDetailPage.kt:220-221` | ⚠️ **hanya disisipkan (index ke-3) bila `!isSchoolScope`** — tidak muncul di `AkmDetailPage` sama sekali |
| Tombol "Cek Passing Grade" (`btn_action_pg`) | tidak ada key API — murni flag `isTryout` | Boolean (bukan API) | `AkmScoreDetailPage.kt:116-121` | `VISIBLE` hanya jika `isTryout`, else `INVISIBLE` (tetap makan tempat) |
| Label "Beban soal:" | ⚠️ HARDCODED: "Jumlah soal: " (isSchoolScope) / "Beban soal:" (lainnya) | — | `AkmScoreDetailPage.kt:114` | selalu tampil |
| Baris beban soal (`rv_types`) | sama seperti `AkmDetailPage`: `"- " + type + " (" + num_question + " Soal)"` | String, DIHITUNG | `AkmScoreDetailPage.kt:199-200`; `akm_detail_beban_soal_item.xml:15` | satu baris per exam `num_question>0` |
| Progress bar & label sinkronisasi pembahasan | `data.schedule.download_progress` (lokal) / `numQuestions` DIHITUNG | Int | `AkmScoreDetailPage.kt:246-247,267-276,310-311` | tampil sesuai status (lihat kolom tombol aksi) |
| Tombol aksi (`btn_action`) | teks & aksi tergantung `data.schedule.status` **dan** `hasExplain` (`AkmSettings.hasExplain`, sumber asli `ListAkmData.has_explain`) **dan** `data.schedule.show_score` | String, DIHITUNG | `AkmScoreDetailPage.kt:237-350` | lihat rincian di bawah |

**Rincian tombol aksi (`btn_action`) per status** (semua label
HARDCODED, tampil hanya jika `show_score && hasExplain`, kecuali
disebutkan lain):
- `NEW`: **"Sinkronisasi Pembahasan"** (label default tombol, dari
  `akm_score_detail_page.xml:365`) → memicu `downloadSoal()`.
- `DOWNLOADING`: tombol disembunyikan, progress tampil.
- `DOWNLOADED`: tidak mengubah tombol; otomatis memicu
  `downloadPembahasanSoal()` di background bila `hasExplain`.
- `FINISHED`/`UPLOADED`: label **"Asesmen telah dikumpulkan"** pada
  `progress_label` (HARDCODED).
- `SCORED`: teks tombol diganti **"Sinkronisasi Pembahasan"** →
  `downloadSoal()`.
- `EXPLAINED`: teks tombol diganti **"Lihat Pembahasan"** →
  **membuka `AkmTakeResumePage`** (extra `isExplanation=true`) — **bukan**
  `AkmExplanationPage` (lihat catatan arsitektur di awal dokumen).
- Khusus `isTryout==true`: tombol selalu `VISIBLE`
  (`AkmScoreDetailPage.kt:229`) dan `onClick`-nya membuka
  `AkmExplanationPage` dengan extra `isTryout=true`, `scheduleId` —
  `AkmScoreDetailPage.kt:230-236`.

### Dialog yang dipicu dari halaman ini

Tidak ada dialog kustom dengan data API. Yang ada hanya:
- `prettyAlert` generik saat exception saat memuat data — judul "Terjadi
  kesalahan", pesan "Gagal menampilkan data" (HARDCODED),
  `AkmScoreDetailPage.kt:365-378`.
- `alert()` saat worker download gagal — pesan diambil dari
  `workInfo.outputData.getString("message")` (DIHITUNG dari output
  worker, yang bisa memuat pesan error dari API saat proses download
  pembahasan gagal), `AkmScoreDetailPage.kt:254-262` dst.

---

## 6. AkmExplanationPage (pembahasan soal)

**File:** `AkmExplanationPage.kt`, layout `akm_explanation_page.xml`.
ViewModel yang dipakai adalah **`TryOutViewModel`** (bukan
`AkmViewModel`), dan model data yang dipakai adalah
**`ListInstruction`**/**`StudentAnswer`** dari
`pages/tryout/TryoutModels.kt` (bukan `AkmQuestion`/`AkmAnswer` di
`AkmModels.kt`). Endpoint sumber: `api.getTryoutExplanation(scheduleId)`
→ `ListTryoutExplanationResponse.data` berisi `list_instruction: List<ListInstruction>`
dan `student_answer: List<StudentAnswer>` (`TryOutViewModel.kt:278-315`).

**Trigger/masuk dari:** ⚠️ **Satu-satunya pemanggil di seluruh kode adalah
`AkmScoreDetailPage.kt:230-236`, dan hanya dieksekusi ketika
`isTryout==true`.** Untuk jalur asesmen sekolah/AKM biasa (yang menjadi
fokus dokumen ini), status `EXPLAINED` di `AkmScoreDetailPage` justru
membuka `AkmTakeResumePage` (di luar cakupan 6 halaman ini), bukan
halaman ini. Dengan kata lain, **`AkmExplanationPage` saat ini adalah
halaman pembahasan Try Out, bukan pembahasan AKM/Ujian Sekolah** — meski
berada di package `akm` dan namanya mengandung "Akm". Ekstra intent yang
diterima: `isTryout: Boolean`, `scheduleId: Int`.

**Navigasi keluar:** toolbar back → `supportFinishAfterTransition()`
(`AkmExplanationPage.kt:173,230-233`), kembali ke halaman pemanggil.
Tap gambar soal/jawaban/kunci/pembahasan → `ImageViewPage` dengan extra
`title="Gambar Ujian"`, `downloadable=false`, data = URL gambar
bersangkutan (`AkmExplanationPage.kt:340-371`).

### Elemen UI — pill instruksi (`rv_instruction`, `akm_explanation_instruction_item.xml`, var `item: ListInstruction`)

| Elemen (label visual) | Key data (path) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Nomor pill (`btn_nilai`) | ⚠️ **DIHITUNG, bukan field langsung** — meski model `ListInstruction` punya field `instruction_number`, adapter menimpanya jadi `position + 1` (nomor urut tampilan) sebelum bind, `text = "Nilai"` di XML adalah placeholder yang selalu ditimpa Kotlin | Int→String | model `TryoutModels.kt:73`; komputasi `AkmExplanationPage.kt:246-258,277` | selalu tampil, satu pill per instruksi |
| Warna latar pill saat dipilih | tidak ada key — state UI lokal (dipulihkan otomatis setelah 1 detik) | — | `AkmExplanationPage.kt:284-311` | berubah biru sesaat lalu kembali putih/abu setelah 1 detik |

### Elemen UI — konten instruksi terpilih

| Elemen (label visual) | Key data (path) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Teks instruksi (`tv_instruction`) | `instruction_text` (raw field `ListInstruction.instruction_text`, instruksi pertama otomatis dipilih saat data dimuat) | String, DIHITUNG string `"Instruksi : " + it` | `AkmExplanationPage.kt:185-187` (⚠️ XML `@{item.instructionText}` di `akm_explanation_page.xml:67` tidak pernah dievaluasi karena variabel `item` tak pernah di-assign — nilai final murni dari observer Kotlin ini) | selalu tampil setelah instruksi dipilih |
| Teks deskripsi (`tv_description`) | `instruction_description` (raw field `ListInstruction.instruction_description`) | String, DIHITUNG `"Deskripsi : " + Html.fromHtml(it)` | `AkmExplanationPage.kt:188-190` (XML `@{item.instructionDesc}` juga mati, sama alasan) | selalu tampil setelah instruksi dipilih |

### Elemen UI — kartu jawaban per soal (`rv_explanation`, `akm_explanation_item.xml`, var `item: StudentAnswer`)

| Elemen (label visual) | Key data (path) | Tipe | Sumber (file:baris) | Kondisi tampil/format |
|---|---|---|---|---|
| Gambar soal (`image`) | `question_filePath` (raw field `StudentAnswer.question_filePath`) via custom binding `imageFitUrlRounded` | String (URL) | `akm_explanation_item.xml:27-28` | tampil hanya bila `question_filePath` tidak kosong |
| Teks soal (`tv_soal`) | `question` — **DIHITUNG**: adapter menimpa jadi `"{n+1}. " + Html.fromHtml(question_asli)` (`n`=posisi di list) sebelum bind | String | model `TryoutModels.kt:97`; komputasi `AkmExplanationPage.kt:398-418`; assignment `AkmExplanationPage.kt:332` | selalu tampil |
| "Nilai : n" (`tv_soal_weight`) | `"Terjawab benar"`/`"Belum terjawab benar"` dari `answer_isTrue` — **DIHITUNG** | Boolean→String | `AkmExplanationPage.kt:374` | ⚠️ **elemen ini di XML di-set `android:visibility="gone"` secara permanen** (`akm_explanation_item.xml:59`) dan **tidak ada kode Kotlin yang mengubahnya** — teks di atas di-assign tapi **tidak pernah benar-benar terlihat** di layar. Jangan mendesain badge ini sebagai elemen yang tampak |
| Label "Jawaban Siswa :" (`tv_title_answer`) | ⚠️ HARDCODED | — | `akm_explanation_item.xml:75` | selalu tampil |
| Latar kotak jawaban (`answer_plot`) | tidak ada key langsung — dari `answer_isTrue` dan `answer_text` kosong/tidak | Boolean, DIHITUNG | `AkmExplanationPage.kt:333,336` | oval merah (`min_oval_red`) bila `answer_isTrue==true` dan terjawab; oval abu (`min_oval_gray`) bila salah atau `answer_text` kosong |
| Teks jawaban siswa (`tv_answer_student`) | `answer_text` (raw field) | String | `AkmExplanationPage.kt:334-338` | `"[Tidak Terjawab]"` (HARDCODED) bila `answer_text` kosong, else `Html.fromHtml(answer_text)` |
| Gambar jawaban siswa (`iv_answer_student`) | `answer_filePath` (raw field) via `imageFitUrlRounded` | String (URL) | `akm_explanation_item.xml:107-108` | tampil bila tidak kosong |
| Latar kotak kunci jawaban (`answer_plot2`) | ⚠️ HARDCODED, selalu `label_green` — tidak berubah sesuai benar/salah | — | `akm_explanation_item.xml:133` | selalu sama |
| Teks kunci jawaban (`tv_answer_true`) | `question_answerkey` (raw field) | String | `AkmExplanationPage.kt:373` | `Html.fromHtml(question_answerkey)` |
| Gambar kunci jawaban (`iv_answer_key`) | `question_answerkey_filePath` (raw field) via `imageFitUrlRounded` | String (URL) | `akm_explanation_item.xml:151-152` | tampil bila tidak kosong |
| Label "Pembahasan :" (`title_explan`) | ⚠️ HARDCODED | — | `akm_explanation_item.xml:168` | selalu tampil |
| Teks pembahasan (`tv_answer_explain_true`) | `explanation` (raw field) | String | `AkmExplanationPage.kt:375` | selalu tampil |
| Gambar pembahasan (`iv_answer_true`) | `explanation_image` (raw field) via `imageFitUrlRounded` | String (URL) | `akm_explanation_item.xml:197-198` | tampil bila tidak kosong |

### Dialog yang dipicu dari halaman ini

**Dialog "Peringatan" (jam otomatis)** — sama persis mekanismenya dengan
`AkmListPage` (`onResume`, cek `DateUtil`), semua teks HARDCODED
(`AkmExplanationPage.kt:197-228`). Bedanya tombol "Jangan Ubah" di sini
menutup activity (`finish()`), bukan `requireActivity().finish()`.

---

## Kamus Field (rujukan tunggal — semua field API/entity terkait area Asesmen)

### `ListAkmData` — root data respons daftar/detail AKM (`AkmModels.kt:22-55`)

| Field | Tipe | Nullable/default | Dipakai di halaman |
|---|---|---|---|
| `id` | Int | default `0` | AkmListPage, AkmDetailPage, AkmScorePage, AkmScoreDetailPage (via `AkmTable.id`) |
| `level` | Int | default `0` | AkmScoreDetailPage ("Level", hanya non-sekolah) |
| `isActive` | Int | default `0` | dipakai internal (`AkmTable.is_active`), tidak ditampilkan langsung |
| `password` | String | `@NullToEmptyString`, default `""` | disimpan di `AkmTable.password`, tidak ditampilkan di 6 halaman ini |
| `date` | String | `@NullToEmptyString`, default `""` | bahan komputasi `date_label`/`time_label` (AkmListPage, AkmScorePage) |
| `startAt` | String | `@NullToEmptyString`, default `""` | bahan komputasi `date_label`/`time_label` |
| `endAt` | String | `@NullToEmptyString`, default `""` | bahan komputasi `time_label` & info "berakhir" di AkmDetailPage |
| `status` | String | `@NullToEmptyString`, default `""` | `AkmTable.score_status`, dipakai `label_info` di AkmScorePage |
| `show_score` | Boolean | default `true` | AkmScoreDetailPage (gate tampil ring skor & tombol pembahasan) |
| `requires_password` | Boolean | default `false` | AkmDetailPage (kartu & dialog password) |
| `password_checked` | Boolean | default `false` | AkmDetailPage (gate dialog password muncul/tidak) |
| `has_explain` | Boolean? | default `null` (null = tidak tersedia) | AkmScoreDetailPage (gate tombol pembahasan) |
| `isDone` | Boolean | default `false` | bahan komputasi status lokal (`AkmStatus`) |
| `isAssessed` | Boolean | default `false` | bahan komputasi status lokal (SCORED) |
| `isQueued` | Boolean | default `false` | bahan komputasi status lokal (UPLOADED) |
| `gov_schedule` | Boolean | default `false` | AkmListPage (pilihan ikon `image`) |
| `participant` | `AkmParticipant` | default kosong | tidak dipakai di 6 halaman ini |
| `exam` | `AkmExams?` | default `AkmExams()` | bahan gabungan daftar `exams` (nama/jenis/beban soal) |
| `exams` | `List<AkmExams>?` | default kosong | bahan gabungan `name`/`type`/beban soal/kategori/periode |
| `score` | `List<AkmScore>` | default kosong | bahan `AkmExamsTable.score` (endpoint scored) |
| `exam_score` | Double? | default `-1.0` | bahan `AkmExamsTable.score` (fallback bila `score[]` kosong) |
| `type` | String | `@NullToEmptyString`, default `""` | `AkmTable.exam_template` (penentu tampilan ring tunggal vs banyak di AkmScoreDetailPage) |

### `AkmParticipant` (`AkmModels.kt:59-64`)
`uuid`, `name`, `nisn_nik`, `school` — semua `String`, default `""`. **Tidak
dipakai** di 6 halaman ini (info peserta yang tampil berasal dari
`StudentItem`/`SekolahItem`, bukan dari objek ini).

### `AkmExams` (`AkmModels.kt:68-79`) — item di dalam `exam`/`exams`

| Field | Tipe | Default | Dipakai di halaman |
|---|---|---|---|
| `id` | Int | `0` | pencocokan skor (`score[].id == exam.id`) |
| `name` | String | `""` | bahan `AkmTable.name` (AkmListPage, AkmDetailPage, AkmScorePage, AkmScoreDetailPage) |
| `type` | String | `""` | bahan `AkmTable.type` & `AkmExamsTable.type` (beban soal) |
| `grade` | Any? | `0` | tidak dipakai di 6 halaman ini |
| `author` | `AkmAuthor?` | kosong | tidak dipakai di 6 halaman ini |
| `numberOfQuestions` | Int | `0` | filter `num_question > 0` untuk baris beban soal; bahan `numQuestions` progress |
| `instructions` | `List<AkmInstruction>` | kosong | dipakai saat mengerjakan soal (di luar cakupan 6 halaman) |
| `score` | Double (var) | `0.0` | `AkmExamsTable.score` — ring nilai (AkmScoreDetailPage) |
| `assessment_category` | String | `""` | `AkmTable.assessment_category` ("Kategori") |
| `assessment_period` | String | `""` | `AkmTable.assessment_period` ("Periode") |

### `AkmScore` (`AkmModels.kt:83-88`)
`id`, `name`, `type` (String, default `""`), `scored` (Double, default
`0.0`) — dipakai untuk mencocokkan nilai ke `exams[].score` lewat
`id == score.id` (`OnKlasDbUtil.kt:1032-1035`); tidak tampil langsung.

### `AkmAuthor` (`AkmModels.kt:92-96`)
`id` (Int), `nip`, `name` (String) — tidak dipakai di 6 halaman ini.

### `ExamPasswordCheckData` / `ExamPasswordCheckResponse` (`AkmModels.kt:120-129`)
`checked: Boolean` (default `false`), `requires_password: Boolean`
(default `false`) — dipakai di dialog password `AkmDetailPage` untuk
menentukan lolos/tidaknya password yang dimasukkan.

### `penaltyResponse` (`AkmModels.kt:199-218`) — `GET mobile/setting-akm`

| Field | Tipe | Default | Dipakai di halaman |
|---|---|---|---|
| `penalty_times` | Int | `0` | tidak dipakai di 6 halaman ini (dipakai saat mengerjakan soal) |
| `penalty_applied` | Boolean | `false` | tidak dipakai di 6 halaman ini |
| `absence_setting` | Boolean | `false` | tidak dipakai di 6 halaman ini |
| `exam_lock_mode` | Boolean? | `false` | AkmDetailPage — menentukan isi dialog konfirmasi mulai ujian & gate offline |

### `AkmInstruction`, `AkmQuestion`, `AkmQuestionMedia`, `AkmAnswer`
(`AkmModels.kt:145-189`) — dipakai untuk mengambil & mengerjakan soal
ujian (halaman `AkmQuestionsPage`, di luar cakupan 6 halaman dokumen ini).
Tidak tampil di AkmPage/AkmListPage/AkmDetailPage/AkmScorePage/
AkmScoreDetailPage/AkmExplanationPage.

### `AkmTable` — entity lokal Room, sumber langsung sebagian besar UI (`AkmEntities.kt:26-50`)

| Field | Tipe | Asal | Dipakai di halaman |
|---|---|---|---|
| `id` | Int | `= ListAkmData.id` | semua |
| `status` | Int (enum `AkmStatus`) | DIHITUNG dari `isAssessed`/`isDone`/`isQueued`/status lokal sebelumnya | AkmListPage, AkmDetailPage, AkmScorePage, AkmScoreDetailPage |
| `is_active` | Boolean | `= ListAkmData.isActive > 0` | tidak ditampilkan langsung |
| `name` | String | DIHITUNG (gabungan nama exam, lihat atas) | AkmListPage, AkmDetailPage, AkmScorePage, AkmScoreDetailPage |
| `type` | String | DIHITUNG (gabungan jenis exam) | sama seperti `name` |
| `level` | Int | `= ListAkmData.level` | AkmScoreDetailPage ("Level") |
| `date_start` | Date | DIHITUNG dari `date`+`startAt` | AkmDetailPage (gate tombol "Mulai") |
| `date_end` | Date | DIHITUNG dari `date`+`endAt` | AkmDetailPage (info "berakhir"), worker upload |
| `date_label` | String | DIHITUNG (format `date_start`) | AkmListPage, AkmScorePage |
| `time_label` | String | DIHITUNG (`startAt - endAt`) | AkmListPage, AkmScorePage |
| `password` | String | `= ListAkmData.password` | tidak ditampilkan di 6 halaman ini |
| `score_status` | String | `= ListAkmData.status` (raw) | AkmScorePage (`label_info` default) |
| `download_progress` | Int | lokal, di-update `AkmDownloader` worker | AkmDetailPage, AkmScoreDetailPage (progress bar) |
| `show_score` | Boolean | `= ListAkmData.show_score` | AkmScoreDetailPage |
| `is_school_scope` | Boolean | dari parameter panggilan (bukan field API) | tidak ditampilkan langsung |
| `exam_type` | Int | dari parameter panggilan (`ExamType.SCHOOL`/`TRYOUT`) | AkmScorePage (`viewmodel.uploadAnswer` routing) |
| `exam_template` | String | `= ListAkmData.type` (raw) | AkmScoreDetailPage (ring tunggal vs banyak) |
| `gov_schedule` | Boolean | `= ListAkmData.gov_schedule` | AkmListPage (ikon) |
| `assessment_category` | String | DIHITUNG (lihat atas) | AkmDetailPage, AkmScoreDetailPage ("Kategori") |
| `assessment_period` | String | DIHITUNG (lihat atas) | AkmDetailPage, AkmScoreDetailPage ("Periode") |

### `AkmExamsTable` (`AkmEntities.kt:137-167`)

| Field | Tipe | Asal | Dipakai di halaman |
|---|---|---|---|
| `id` | Int | `= AkmExams.id` | pencocokan internal |
| `schedule_id` | Int | id `AkmTable` induk | relasi Room |
| `name` | String | `= AkmExams.name` | tidak ditampilkan langsung di sini (dipakai di `AkmTable.name`) |
| `type` | String | `= AkmExams.type` | baris beban soal (AkmDetailPage, AkmScoreDetailPage) |
| `grade` | Int | `= AkmExams.grade` | tidak dipakai |
| `author_nip`/`author_name` | String | `= AkmExams.author` | tidak dipakai |
| `num_question` | Int | `= AkmExams.numberOfQuestions` | baris beban soal, progress sinkronisasi |
| `score` | Double | `= AkmExams.score` (via `AkmScore`/`exam_score`) | ring skor (AkmScoreDetailPage) |
| `assessment_category`/`assessment_period` | String | `= AkmExams.assessment_category/period` | bahan `AkmTable.assessment_category/period` |

### `StudentItem` / `SekolahItem` (`pages/login/LoginModels.kt`) — BUKAN bagian API Asesmen

| Field dipakai | Tipe | Sumber | Dipakai di halaman |
|---|---|---|---|
| `StudentItem.name` | String | sesi login lokal | AkmDetailPage, AkmScoreDetailPage ("Peserta") |
| `StudentItem.nis` | String | sesi login lokal | AkmDetailPage, AkmScoreDetailPage ("NIS", prioritas 1) |
| `StudentItem.nisn` | String | sesi login lokal | AkmDetailPage, AkmScoreDetailPage ("NIS", fallback) |
| `StudentItem.student_class.class_room.name` | String | sesi login lokal | AkmDetailPage, AkmScoreDetailPage ("Kelas") |
| `SekolahItem.name` | String | sesi login lokal | AkmDetailPage, AkmScoreDetailPage ("Sekolah") |

### `ListInstruction` / `StudentAnswer` (`pages/tryout/TryoutModels.kt:70-103`) — dipakai `AkmExplanationPage`

| Field | Tipe | Default | Dipakai di halaman |
|---|---|---|---|
| `ListInstruction.schedule_id` | Int | `0` | relasi Room lokal |
| `ListInstruction.instruction_id` | Int | `0` (PrimaryKey) | identifikasi instruksi terpilih |
| `ListInstruction.instruction_number` | Int | `0` | ⚠️ **ditimpa** jadi `posisi+1` di UI, nilai asli field ini tidak dipakai untuk tampilan pill |
| `ListInstruction.instruction_text` | String | `""` | teks instruksi (`tv_instruction`) |
| `ListInstruction.instruction_description` | String | `""` | teks deskripsi (`tv_description`) |
| `StudentAnswer.schedule_id`/`instruction_id`/`instruction_text`/`instruction_description` | — | — | konteks instruksi induk jawaban |
| `StudentAnswer.answer_id` | Int | `0` (PrimaryKey) | identitas item list |
| `StudentAnswer.answer_text` | String | `""` | teks jawaban siswa |
| `StudentAnswer.answer_isTrue` | Boolean | `false` | warna kotak jawaban, teks "Terjawab benar" (tidak tampil, lihat catatan) |
| `StudentAnswer.answer_answered` | Int | `0` | tidak dipakai langsung di UI |
| `StudentAnswer.answer_filePath` | String | `""` | gambar jawaban siswa |
| `StudentAnswer.question` | String | `""` | teks soal (ditimpa dengan nomor urut, lihat komputasi) |
| `StudentAnswer.question_filePath` | String | `""` | gambar soal |
| `StudentAnswer.question_answerkey` | String | `""` | teks kunci jawaban |
| `StudentAnswer.question_answerkey_filePath` | String | `""` | gambar kunci jawaban |
| `StudentAnswer.explanation` | String | `""` | teks pembahasan |
| `StudentAnswer.explanation_image` | String | `""` | gambar pembahasan |
