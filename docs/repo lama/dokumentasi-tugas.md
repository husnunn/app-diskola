# Dokumentasi Fitur Tugas (Homework / Assignment)

Dokumentasi ini merangkum **alur UI**, **rules bisnis/UX**, dan **struktur teknis** fitur Tugas di aplikasi Diskola Android Portal untuk **role siswa** dan **role guru**. Sumber utama: kode di `pages/homework/**`, `pages/homework/teacher/**`, entry point `PembelajaranPage`, serta API assignment di `ApiService`.

---

## 1. Ringkasan

| Aspek | Siswa | Guru |
| --- | --- | --- |
| Entry menu | Pembelajaran → **Tugas** | Pembelajaran → **Tugas** |
| Activity awal | `HomeWorkPage` | `HomeworkTeacherPage` |
| Kapabilitas | Lihat backlog / sudah dikerjakan / nilai; baca soal; kumpulkan jawaban (multi-file + link) | Buat/edit/hapus tugas; lihat list tugas; nilai jawaban siswa |
| API prefix | `mobile/app/learning/assignment/students/...` | `mobile/app/learning/assignment/teachers/...` |
| Package | `id.diskola.app.pages.homework` | `id.diskola.app.pages.homework.teacher` |
| Paging | `pageSize = 10` | `pageSize = 10` |

Fitur memakai **XML + ViewBinding/DataBinding**, **Room (MemoryDB)** sebagai cache, dan **Paging**.

### Tipe lokal Room (`HomeworkTable.type`)

| type | Arti | Sumber API siswa |
| --- | --- | --- |
| `0` | Belum dikerjakan (backlog) | `students/backlog` (atau `teachers/backlog` jika `is_teacher`) |
| `1` | Sudah dikerjakan | `students/done` |
| `2` | Sudah dinilai | `students/scored` |

---

## 2. Gate akses (aturan masuk fitur)

Masuk dari halaman **Pembelajaran** (`PembelajaranPage`), menu index **1** ("Tugas").

| Pref key | Fungsi |
| --- | --- |
| `is_having_class` | Jika `false` → menu locked + dialog locked; tidak membuka halaman tugas. |
| `is_student` | `true` → `HomeWorkPage`. `false` → `HomeworkTeacherPage`. |
| `is_teacher` | Mempengaruhi endpoint backlog di ViewModel (`teacherTaskTodo` vs `studentTaskTodo`). |
| `user_id` | Ownership kebab Edit/Hapus di list tugas guru. |
| `teacher` (JSON) | `teacher.id` untuk filter list tugas guru & payload create. |
| `school` (JSON) | `school.id` dikirim saat create/update tugas. |

```
Pembelajaran menu "Tugas"
  ├─ !is_having_class  → dialog locked
  └─ is_having_class
       ├─ is_student = true  → HomeWorkPage
       └─ is_student = false → HomeworkTeacherPage
```

---

## 3. Peta navigasi (UI flow)

### 3.1 Role siswa

```
Pembelajaran
    │
    ▼
HomeWorkPage                          ← shell 3 tab
    ├─ HomeworkBelumPage              ← Belum Dikerjakan (start destination)
    │       │  tap card
    │       ▼
    │   HomeworkDetailPage            ← isFinished=false → boleh kumpulkan
    │       └─ Kirim Tugas sukses → finish (keluar detail)
    │
    ├─ HomeworkSudahPage              ← Sudah Dikerjakan
    │       │  tap card
    │       ▼
    │   HomeworkDetailPage            ← isFinished=true → read-only jawaban
    │
    └─ HomeworkNilaiPage              ← Nilai
            │  tap card
            ▼
        HomeworkNilaiDetailPage       ← bottom sheet skor + pembahasan
```

**Filter siswa:** `HomeworkFilterPage` (bottom sheet) masih ada di kode (`icFilter`), tetapi di `HomeWorkPage.onCreate` dipanggil `toggleFilter(false)` sehingga chip/ikon filter **disembunyikan**. Listener tab juga tidak lagi menampilkan filter.

**Deep link / notifikasi**

- `NotifRouter` tipe `"task"` → `HomeWorkPage` (+ extra `id`, `isFinished=false`).
- Notifikasi in-app `"TASK"` → `HomeworkDetailPage` dengan `child_id` (bukan `id` yang dibaca detail) — inkonsisten dengan contract detail yang memakai extra `id`.

### 3.2 Role guru

```
Pembelajaran
    │
    ▼
HomeworkTeacherPage                   ← shell: tab List Tugas | Penilaian + filter + FAB
    │
    ├─ HomeworkTeacherListPage        ← List Tugas (start)
    │       │  filter Kelas & Mapel
    │       │  tap card → CreateHomeworkPage(homeworkId, editable=false)  // mode Detail
    │       │  kebab (milik sendiri) → Edit / Hapus
    │       │  FAB → CreateHomeworkPage()                                 // mode Create
    │       │
    │       └─ CreateHomeworkPage → create/update API → RESULT_OK → refresh
    │
    └─ HomeworkTeacherNilaiPage       ← Penilaian (filter Kelas/Mapel disembunyikan)
            │  tap card grup tugas
            ▼
        HomeworkSubmittedPage         ← daftar jawaban siswa + ringkasan
            │  tombol Nilai
            ▼
        HomeworkScoringPage           ← bottom sheet input skor 0–100
```

---

## 4. Screen-by-screen — Role siswa

### 4.1 `HomeWorkPage` — Shell

| Item | Detail |
| --- | --- |
| Layout | `homework_page.xml` |
| Nav | `@navigation/homework_nav` → start `homeworkBelumPage` |
| Tab | **Belum Dikerjakan** / **Sudah Dikerjakan** / **Nilai** |
| ViewModel | `HomeWorkViewModel` (activity-scoped ke fragment anak) |

**Rules**

1. Tab aktif ditandai stroke + text `colorPrimary`; tab lain gray.
2. Prefetch `fetchHomeworkTodo()` saat `onCreate`.
3. Back → `supportFinishAfterTransition()` (bukan pop NavHost).
4. UI filter (bg/icon/label/spinner) default **GONE**.

### 4.2 `HomeworkBelumPage` — Belum dikerjakan

| Item | Detail |
| --- | --- |
| Data | Room `type = 0` + API backlog |
| UI | Swipe refresh, list card, empty state |
| Tap | `HomeworkDetailPage` + `id` + `isFinished=false` |

**Rules**

1. Swipe refresh reset `prefBelum = -1` lalu fetch ulang.
2. Empty state di-toggle eksplisit saat list kosong/tidak kosong.
3. Pagination: next page jika `count >= 10` dan `hasNextBelum`.

### 4.3 `HomeworkSudahPage` — Sudah dikerjakan

| Item | Detail |
| --- | --- |
| Data | Room `type = 1` + `studentTaskDone` |
| Tap | `HomeworkDetailPage` + `isFinished=true` |

### 4.4 `HomeworkNilaiPage` — Nilai

| Item | Detail |
| --- | --- |
| Data | Room `type = 2` + `studentTaskScored` |
| Tap | `HomeworkNilaiDetailPage(item)` sebagai dialog (bukan activity detail penuh) |

### 4.5 `HomeworkFilterPage` — Filter (tersedia, UI induk disembunyikan)

Bottom sheet dengan:

- Radio **Batas Waktu**: Semua / Belum dikerjakan / Terlewati → `filterTime`
- Checkbox **Mata Pelajaran** (multi) → `filterMapel`
- Menu toolbar reset pilihan

> Saat ini filter **tidak terhubung** ke query list tab (observer filter hanya update UI sheet). List siswa tidak memfilter Room berdasarkan `filterTime`/`filterMapel`.

### 4.6 `HomeworkDetailPage` — Detail & kumpulkan

| Item | Detail |
| --- | --- |
| Layout | `homework_detail_page.xml` |
| Extra | `id` (wajib > 0), `isFinished` (default false) |

#### Elemen UI utama

1. Info status (sudah dikerjakan / terlambat).
2. Card progress **Syarat pengumpulan** (checklist baca soal / upload jawaban) — hanya siswa + belum finished.
3. Deskripsi tugas.
4. Section **Soal** (file + preview link guru) bila ada file/path atau link teacher.
5. Badge **WAJIB DIBACA** bila `downloded > 0`.
6. Section jawaban terkumpul (multi-file + link siswa) bila sudah ada.
7. Section **Kumpulkan jawaban** (multi-file picker + input link) bila `allowUpload`.
8. Section pembahasan (`explanation_file_path` dari `file_edited`) bila ada.
9. Sticky bar + tombol **Kirim Tugas**.

#### Rules `allowUpload` (form pengumpulan tampil)

```
uploaded == 1
AND isFinished == false
AND is_student == true
```

Artinya: guru menandai tugas wajib dikumpulkan (`uploaded=1`), siswa membuka dari tab Belum, dan user adalah siswa.

#### Rules syarat pengumpulan (instruction list)

| Flag tugas | Syarat UI | Cara memenuhi |
| --- | --- | --- |
| `downloded > 0` (`needRead`) | "Silahkan baca/download soal terlebih dahulu" | Tap **Baca tugas** / **Download** file soal, **atau** tap preview link guru |
| `uploaded > 0` (`needUpload`) | "Lampirkan file jawaban atau tempel link..." | Pilih ≥1 file **atau** preview link jawaban sukses |

- Progress card tampil hanya jika siswa, belum finished, dan (`needRead` || `needUpload`).
- Tombol **Kirim Tugas** enabled hanya jika semua instruction sudah cleared; teks status: "Semua syarat terpenuhi ✓".
- Jika masih ada syarat → tombol disabled; tap tetap bisa memunculkan `prettyAlert` "Belum bisa mengirim" berisi bullet syarat tersisa.
- Jika `!is_student` → progress card disembunyikan.

#### Rules lampiran jawaban siswa (upload)

| Aturan | Nilai |
| --- | --- |
| Extensi diizinkan | `jpeg`, `jpg`, `png`, `pdf`, `doc`, `docx`, `xls`, `xlsx`, `ppt`, `pptx` |
| Ukuran maks | **12 MB per file** |
| Multipart | `file[]` (multi), part map: `checked=1`, `uploaded=1|0`, `link` |
| Endpoint | `POST .../students/{id}/collect` |
| Setelah sukses | Simpan file jawaban lokal, clear selection, **hapus** row homework lokal (keluar dari backlog), `finish()` |

Link preview debounce **1 detik**. Gagal preview → toast `"gagal menampilkan preview url"`.

#### Rules tampilan lain

1. Extra `id` hilang/0 → alert "Halaman tidak tersedia" → finish.
2. `isFinished=true` → info "Kamu sudah mengerjakan tugas" (warna primary); form upload tidak aktif.
3. `is_overdue=true` (dan belum finished) → info merah "Waktu pengumpulan tugas terlambat" (tetap bisa kumpulkan jika `allowUpload`).
4. Section soal visible jika ada `file_path` **atau** link guru (`src = SRC_TEACHER`).
5. Jawaban siswa: cache Room `homework_student_file` dulu; jika kosong dan ada `student_assignment_id` → fetch `students/{subjectAssignmentId}/collect/{studentAssignmentId}`.

### 4.7 `HomeworkNilaiDetailPage` — Detail nilai (dialog)

| Item | Detail |
| --- | --- |
| UI | Toolbar, skor, mapel/guru, card pembahasan |
| Pembahasan | Tampil jika `explanation_file_path` tidak kosong; Lihat / Download ("pembahasan tugas") |

---

## 5. Screen-by-screen — Role guru

### 5.1 `HomeworkTeacherPage` — Shell

| Item | Detail |
| --- | --- |
| Layout | `homework_teacher_page.xml` |
| Nav | `@navigation/homework_teacher_nav` → start List Tugas |
| Tab | **List Tugas** / **Penilaian** |
| Filter | Dropdown **Kelas** + **Mata Pelajaran** (default item pertama = "Semua") |
| FAB | Buat tugas → `CreateHomeworkPage` |

**Rules**

1. Tab List Tugas → tampilkan filter Kelas/Mapel.
2. Tab Penilaian → **sembunyikan** filter.
3. Perubahan `classId` / `mapel` di ViewModel memicu refresh list guru.
4. Setelah create `RESULT_OK` → `fetchHomeworkTodo()` (backlog teacher).

### 5.2 `HomeworkTeacherListPage` — List tugas dibuat

| Item | Detail |
| --- | --- |
| Data | Homework milik `teacherId`, filter opsional subject/class |
| API | `teacherAssignment` dengan query filter `teacher_id`, opsional `school_classes_id`, `school_subject_id` |
| Empty | "Belum terdapat tugas / Silahkan buat tugas untuk siswa" |

**Rules aksi**

1. Tap card → `CreateHomeworkPage` dengan `homeworkId` + **`editable=false`** (mode Detail Tugas, form read-only).
2. Kebab visible hanya jika `pref.user_id == teacher.sosmed_user_id` (`isMine`).
3. **Edit Tugas** → `CreateHomeworkPage(homeworkId)` (`editable` default true).
4. **Hapus Tugas** → konfirmasi → `DELETE .../teachers/delete/{id}` + hapus lokal.
5. Setelah edit `RESULT_OK` → refresh list.

### 5.3 `HomeworkTeacherNilaiPage` — Penilaian (grup tugas terkumpul)

| Item | Detail |
| --- | --- |
| Data | `homework_collected` dari `assignmentCollected` (`teachers/scored`) |
| Empty | "Belum terdapat penilaian / Belum ada tugas yang dikumpulkan siswa" |
| Tap | `HomeworkSubmittedPage` + `id` = collected group id |

Card menampilkan ringkasan: judul, waktu, jumlah terkumpul / dinilai (`count_assignment_*`).

### 5.4 `CreateHomeworkPage` — Buat / Edit / Detail

| Item | Detail |
| --- | --- |
| Layout | `create_homework_page.xml` |
| ViewModel | `CreateHomeworkVm` |
| Extra | `homeworkId` (0 = create), `editable` (default true) |

#### Mode

| Kondisi | Judul toolbar | Perilaku |
| --- | --- | --- |
| `homeworkId == 0` | Upload/Create | Form aktif, Posting Tugas |
| `homeworkId > 0` + `editable=true` | Edit Tugas | Form aktif, update API |
| `homeworkId > 0` + `editable=false` | Detail Tugas | Form disabled; menu opsi → Edit / Hapus |

#### Elemen form & urutan dependensi

1. **Kelas** (wajib) → enable hari.
2. **Hari Mata Pelajaran** (wajib, dari `assignmentDay`) → fetch schedule.
3. **Mata Pelajaran / jadwal** (wajib): dropdown schedule `"[start - end] subject"` dari `assignmentScheduleClass(classId, day)` → set `scheduleId` + `mapel`.
4. **Batas pengumpulan** — DatePicker + TimePicker; format `dd-MM-yyyy HH:mm:ss`; default sekarang; `minDate` = hari ini.
5. **Judul** — max **500**, counter `n/500`.
6. **Deskripsi** — opsional.
7. Checkbox **Upload Soal** (`uploadFile`) — jika dicentang, tampil zona file + URL link.
8. Checkbox **Siswa wajib membaca materi** (`needDownload` / field API `downloded`) — ditampilkan saat ada file atau link.
9. Checkbox **Siswa wajib mengumpulkan jawaban** (`needUpload` / field API `uploaded`).
10. Tombol **Posting Tugas**.

> Di XML, `check_student_download` default `visibility=gone`; dijalankan visible dari kode saat file/link terisi.

#### Rules validasi `allowCreate` (enable Posting)

Wajib terpenuhi:

1. Judul tidak kosong  
2. `mapel > 0`  
3. `classId > 0`  
4. `classDay` tidak kosong  
5. `scheduleId > 0`  

**Plus** aturan file:

- Jika `uploadFile != true` → OK tanpa file/link.  
- Jika `uploadFile == true` → harus ada **file** atau **link**.

Error inline (setelah touch / saat post):

- "Kelas wajib dipilih"
- "Hari Mata Pelajaran wajib dipilih"
- "Mata Pelajaran wajib dipilih"

Ganti kelas mereset hari, schedule, mapel. Ganti hari mereset schedule & mapel.

#### Payload create/update (multipart)

| Field | Sumber |
| --- | --- |
| `title`, `description` | form |
| `teacher_id`, `school_id` | pref |
| `school_subject_id` | mapel dari schedule |
| `school_classes_id` | kelas |
| `school_subject_schedules_id` | scheduleId |
| `grade` | grade dari ClassRoom terpilih |
| `end_at` | expired |
| `checked` | selalu `"1"` |
| `downloded` | `"1"`/`"0"` dari needDownload |
| `uploaded` | `"1"`/`"0"` dari needUpload |
| `link` | URL soal |
| `file` | part opsional |

- Create → `POST .../teachers/create`  
- Update → `POST .../teachers/update/{id}`

### 5.5 `HomeworkSubmittedPage` — Jawaban terkumpul

| Item | Detail |
| --- | --- |
| API | `GET .../teachers/scored/{id}` → `AssignmentResponse` |
| UI | Expand/collapse info (terkumpul, kelas, mapel, upload, berakhir); filter lokal Semua / Belum Dinilai / Sudah Dinilai; list siswa |

**Rules**

1. Filter lokal: `scored == 0` Belum Dinilai, `scored == 1` Sudah Dinilai.
2. Tombol **Nilai** → `HomeworkScoringPage(collectId, assignment)`.
3. "Lihat" detail tugas di panel info → `HomeworkDetailPage` dengan `id` grup (bukan student assignment id) — perilaku tergantung data Room/local.
4. Response null → alert "Halaman tidak tersedia".

### 5.6 `HomeworkScoringPage` — Beri nilai (bottom sheet)

| Item | Detail |
| --- | --- |
| UI | Info peserta (nama, NIS, kelas, sekolah), list file jawaban + preview link, input skor, Simpan |
| Skor | Integer **0–100** saja (`InputFilter`); tombol Simpan enabled jika skor tidak kosong |
| API | `POST .../teachers/scored/{collectedId}/student-assignment/{assignmentId}` body `{ "score": n }` |
| Sukses | callback refresh list + dismiss |

---

## 6. Perbandingan rules siswa vs guru

| Rules | Siswa | Guru |
| --- | --- | --- |
| Masuk fitur | `is_having_class` + `is_student` | `is_having_class` + non-siswa |
| Tab utama | Belum / Sudah / Nilai | List Tugas / Penilaian |
| Buat tugas | Tidak | Ya (FAB) |
| Edit/hapus tugas | Tidak | Ya, hanya milik sendiri |
| Kumpulkan jawaban | Ya (jika `uploaded==1`) | Tidak |
| Syarat baca soal | Ya jika `downloded>0` | Mengatur flag saat create |
| Nilai tugas | Lihat skor + pembahasan | Input skor 0–100 per siswa |
| Filter kelas/mapel | UI filter sheet (hidden / belum efektif) | Aktif di tab List Tugas |
| Multi-file jawaban | Ya (`file[]`) | Lihat multi-file saat scoring |
| Max ukuran file | 12 MB / file (upload jawaban) | Tidak di-hardcode di create VM (beda dengan materi) |

---

## 7. Model data inti

| Model | Peran |
| --- | --- |
| `HomeworkItem` / `HomeworkTable` | Tugas (judul, deadline, file soal, flag `downloded`/`uploaded`, skor, schedule, jawaban siswa legacy single-file) |
| `HomeworkItemTable` | Join homework + teacher + user + mapel + links + studentFiles |
| `HomeworkLink` | URL soal (`SRC_TEACHER=0`) atau jawaban (`SRC_STUDENT=1`) |
| `HomeworkStudentFile` / `StudentAssignmentFile` | Multi-file jawaban siswa |
| `HomeworkCollected` | Ringkasan grup tugas untuk tab Penilaian guru |
| `Assignment` / `AssignmentData` | Detail pengumpulan per siswa dalam satu tugas |
| `AssignmentSchedule` / `AssignmentDay` | Jadwal mapel untuk form create |
| `ClassRoomTable` | Kelas tujuan |
| `SelectedHomeworkFile` | File lokal sementara sebelum submit jawaban |
| `CollectHomework` / `CollectHomeworkResponse` | Response setelah siswa collect |

**Catatan field backend**

- `downloded` (typo di API/app) = wajib baca/download soal.  
- `uploaded` = wajib mengumpulkan jawaban.  
- Pembahasan memakai `file_edited` → disimpan sebagai `explanation_file_path` (struktur `explanations` belum dipakai penuh).

---

## 8. API reference (Tugas)

### Siswa

| Method | Path | Dipakai untuk |
| --- | --- | --- |
| GET | `.../assignment/students/backlog` | Belum dikerjakan |
| GET | `.../assignment/students/done` | Sudah dikerjakan |
| GET | `.../assignment/students/scored` | Sudah dinilai |
| POST multipart | `.../assignment/students/{id}/collect` | Kumpulkan jawaban |
| GET | `.../assignment/students/{subjectAssignmentId}/collect/{studentAssignmentId}` | Detail file jawaban |

### Guru

| Method | Path | Dipakai untuk |
| --- | --- | --- |
| GET | `.../assignment/teachers/backlog` | Backlog / list (juga via query map filter) |
| GET | `.../assignment/teachers/class` | Daftar kelas |
| GET | `.../assignment/teachers/schedule-day` | Hari jadwal |
| GET | `.../assignment/teachers/schedule` | Jadwal mapel per kelas+hari |
| POST multipart | `.../assignment/teachers/create` | Buat tugas |
| POST multipart | `.../assignment/teachers/update/{id}` | Update tugas |
| DELETE | `.../assignment/teachers/delete/{id}` | Hapus tugas |
| GET | `.../assignment/teachers/scored` | List grup penilaian |
| GET | `.../assignment/teachers/scored/{id}` | Detail jawaban siswa |
| POST | `.../assignment/teachers/scored/{collectedId}/student-assignment/{assignmentId}` | Simpan nilai |

Query paging umum: `take`, `skip` (app memakai 10).

Cache insert homework hanya untuk item yang `school.uuid` cocok dengan sekolah login (`processHomeworkRespones`).

---

## 9. Cache & paging (aturan teknis)

1. List siswa/guru ditampilkan dari Room via `toLiveData(pageSize = 10)` + boundary callback.
2. Guard `prefBelum` / `prefSudah` / `prefNilai` / `prefTeacher` / `prefDikumpulkan` mencegah double-fetch page yang sama.
3. Setelah siswa berhasil collect, row homework lokal dihapus sehingga hilang dari tab Belum (akan muncul lagi di tab Sudah setelah sync `done`).
4. `student_assignment_id` dipertahankan saat re-insert backlog (dari existing / pref `hw_sa_{homeworkId}`) agar refresh file jawaban tetap bisa.
5. Error → `errorString` → toast.

---

## 10. Daftar file terkait

### Siswa / shared

| File | Peran |
| --- | --- |
| `HomeWorkPage.kt` | Shell 3 tab |
| `HomeworkBelumPage.kt` / `HomeworkSudahPage.kt` / `HomeworkNilaiPage.kt` | List per status |
| `HomeworkDetailPage.kt` | Detail + kumpulkan |
| `HomeworkNilaiDetailPage.kt` | Dialog detail nilai |
| `HomeworkFilterPage.kt` | Bottom sheet filter |
| `HomeWorkViewModel.kt` | Fetch, upload jawaban, scoring, delete |
| `HomeWorkModels.kt` | Model + entities |
| `HomeworkDao.kt` | Query Room |
| `HomeworkAdapter.kt`, `HomeworkAnswerFilesAdapter.kt` | List adapters |
| `res/navigation/homework_nav.xml` | Nav siswa |
| Layouts `homework_*.xml`, `assignment_item.xml` | UI |

### Guru

| File | Peran |
| --- | --- |
| `HomeworkTeacherPage.kt` | Shell + filter + FAB |
| `HomeworkTeacherListPage.kt` | List tugas + edit/hapus |
| `HomeworkTeacherNilaiPage.kt` | List penilaian |
| `CreateHomeworkPage.kt` / `CreateHomeworkVm.kt` | Form create/edit |
| `CreateHomeworkBindConverter.kt` | Binding id ↔ nama kelas |
| `HomeworkSubmittedPage.kt` | Jawaban terkumpul |
| `HomeworkScoringPage.kt` | Input nilai |
| `res/navigation/homework_teacher_nav.xml` | Nav guru |
| `create_homework_page.xml`, `homework_teacher_*.xml`, `homework_scoring_page.xml`, `homework_submitted_paged.xml` | UI |

### Entry & util

| File | Peran |
| --- | --- |
| `PembelajaranPage.kt` | Menu Tugas + lock + role routing |
| `NotifRouter.kt` | Deep link `"task"` |
| `OnKlasDbUtil.processHomeworkRespones` | Persist list ke Room |
| `ApiWrapper.scoreAssignment` | Body `{score}` |

---

## 11. Checklist QA cepat

### Siswa

- [ ] Tanpa kelas → menu locked.
- [ ] Tab Belum / Sudah / Nilai load + empty state + swipe refresh.
- [ ] Detail tanpa `id` → alert halaman tidak tersedia.
- [ ] Tugas `downloded=1`: Kirim disabled sampai baca/download/tap link soal.
- [ ] Tugas `uploaded=1`: Kirim disabled sampai ada file atau link valid.
- [ ] File format salah / >12MB → toast error, tidak terkirim.
- [ ] Collect sukses → keluar detail; item hilang dari Belum setelah sync.
- [ ] Tab Nilai → dialog skor + pembahasan (jika ada).
- [ ] Overdue menampilkan banner merah tetapi masih bisa submit jika syarat terpenuhi.

### Guru

- [ ] List Tugas: filter Kelas/Mapel "Semua" dan spesifik merefresh list.
- [ ] FAB create: validasi kelas→hari→jadwal→judul; Upload Soal mewajibkan file/link.
- [ ] Checkbox wajib baca / wajib kumpul tersimpan sebagai `downloded` / `uploaded`.
- [ ] Tap card = Detail read-only; kebab Edit/Hapus hanya milik sendiri.
- [ ] Penilaian: buka grup → filter Belum/Sudah Dinilai → input skor 0–100 → refresh.
- [ ] Hapus butuh konfirmasi.

---

## 12. Catatan implementasi saat ini

1. **Filter siswa** (`HomeworkFilterPage`) masih di codebase, tetapi kontrol filter di shell **disembunyikan** dan belum memfilter datasource list.
2. Typo API dipertahankan di app: `downloded` (bukan `downloaded`).
3. Notifikasi in-app tugas memakai `child_id`, sementara detail membaca `id` — deep link andal lewat `NotifRouter` (`task`) atau navigasi dari list.
4. Tap item di List Tugas guru membuka **Detail** (`editable=false`), bukan langsung Edit; Edit lewat kebab atau menu opsi di halaman Detail.
5. Checkbox "Siswa wajib membaca materi" default GONE di XML; muncul dinamis saat ada lampiran soal.
6. Create tugas tidak memvalidasi batas ukuran file di client (beda dengan upload jawaban siswa / upload materi).
7. Setelah siswa collect, item dihapus dari DB lokal; pastikan tab Sudah di-fetch agar status terlihat.

---

*Dokumen ini mengikuti perilaku kode di branch kerja saat dokumentasi dibuat. Jika kontrak API berubah (flag wajib, multi-file create, atau scoring), sesuaikan bagian rules dan API reference.*
