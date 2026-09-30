# Dokumentasi Fitur Materi (Theory)

Dokumentasi ini merangkum **alur UI**, **rules bisnis/UX**, dan **struktur teknis** fitur Materi di aplikasi Diskola Android Portal untuk **role siswa** dan **role guru**. Sumber utama: kode di `pages/theory/**`, `pages/theoryteacher/**`, entry point `PembelajaranPage`, serta API `ApiService`.

---

## 1. Ringkasan

| Aspek | Siswa | Guru |
| --- | --- | --- |
| Entry menu | Pembelajaran → **Materi** | Pembelajaran → **Materi** |
| Activity awal | `TheoryPage` | `TheoryTeacherPage` |
| Kapabilitas | Baca daftar mapel → daftar materi → detail (lihat/unduh file, preview link) | Kelola materi sendiri (lihat, filter, tambah, edit, hapus) + lihat daftar per mapel |
| API prefix | `mobile/app/learning/theories/students/...` | `mobile/app/learning/theories/teachers/...` |
| Package | `id.diskola.app.pages.theory` | `id.diskola.app.pages.theoryteacher` (+ reuse screen siswa untuk daftar/detail) |

Fitur memakai **XML + ViewBinding/DataBinding**, **Room (MemoryDB)** sebagai cache lokal, dan **Paging** (`pageSize = 20`).

---

## 2. Gate akses (aturan masuk fitur)

Semua role masuk dari tab/halaman **Pembelajaran** (`PembelajaranPage`).

### 2.1 Pref yang menentukan akses

| Pref key | Fungsi |
| --- | --- |
| `is_having_class` | Jika `false`, ikon menu Materi ditandai **locked**. Tap menampilkan dialog locked (`showDialogLocked`), **tidak** membuka halaman materi. |
| `is_student` | Jika `true` → buka `TheoryPage`. Jika `false` → buka `TheoryTeacherPage`. |
| `is_teacher` | Dipakai ViewModel untuk memilih endpoint guru (`teacherSubject`, `teacherTheory*`, dll.). |
| `user_id` | Dipakai adapter guru untuk menentukan materi “milik saya” (tampil menu kebab edit/hapus). |
| `teacher` (JSON) | Data guru login; `teacher.id` dipakai filter “Materi Saya”. |

### 2.2 Aturan routing entry

```
Pembelajaran menu index 0 ("Materi")
  ├─ !is_having_class  → dialog locked
  └─ is_having_class
       ├─ is_student = true  → TheoryPage
       └─ is_student = false → TheoryTeacherPage
```

> Catatan: routing entry memakai `is_student`, bukan `is_teacher`. User non-siswa (termasuk guru) masuk ke shell materi guru.

---

## 3. Peta navigasi (UI flow)

### 3.1 Role siswa

```
Pembelajaran
    │
    ▼
TheoryPage                    ← daftar mata pelajaran (+ search)
    │  tap mapel
    ▼
MateriPage                    ← daftar materi per subjectId
    │  tap materi
    ▼
MateriDetailPage              ← deskripsi, file, link, pembahasan
    ├─ Lihat file             → download ke filesDir → IntentUtil.openFile
    ├─ Download file          → IntentUtil.downloadFile (folder "materi" / "pembahasan materi")
    └─ (opsional) PdfPage     ← activity pembaca PDF (tersedia di manifest; detail saat ini memakai openFile)
```

**Deep link / notifikasi**

- `NotifRouter` tipe `"theory"` → `MateriDetailPage` dengan extra `id` + `subId`.
- Alur notifikasi in-app (`NotificationPage` / `DetailNotification`) untuk tipe `"THEORY"` mengirim `child_id` — **tidak** sama dengan extra yang dibaca `MateriDetailPage` (`id`, `subId`). Deep link yang andal untuk detail adalah lewat `NotifRouter`.

### 3.2 Role guru

```
Pembelajaran
    │
    ▼
TheoryTeacherPage             ← shell: toolbar "Materi" + FAB Tambah
    │
    ├─ [NavHost start] MateriTeacherpage     ← "Materi Saya"
    │       │  filter Kelas / Mapel
    │       │  tap card → MateriDetailPage
    │       │  kebab (milik sendiri) → Edit / Hapus
    │       │       Edit → UploadMateriPage(materiId)
    │       │       Hapus → konfirmasi → DELETE API + hapus lokal
    │       │
    │       └─ FAB / btnAdd → UploadMateriPage (create)
    │
    └─ MapelTeacherPage                      ← "Mata Pelajaran" (ada di nav graph)
            │  tap mapel
            ▼
        MateriPage → MateriDetailPage        ← reuse alur siswa (API tetap cabang guru via is_teacher)
```

**Status UI tab guru (penting):**

Di layout `theoryteacher_page.xml`, container tab **"Materi Saya" / "Mata Pelajaran"** saat ini `android:visibility="gone"`. Artinya:

- Start destination tetap `MateriTeacherpage` (Materi Saya).
- FAB upload tetap aktif.
- Switch ke `MapelTeacherPage` lewat tombol tab **tidak terlihat** di UI saat ini, meski kode navigasi dan fragment masih ada.

---

## 4. Screen-by-screen — Role siswa

### 4.1 `TheoryPage` — Daftar mata pelajaran

| Item | Detail |
| --- | --- |
| Layout | `theory_page.xml` |
| ViewModel | `TheoryViewModel` |
| Elemen UI | Toolbar "Materi", search "Cari mata pelajaran", swipe refresh, list mapel, empty state pencarian |
| Data | Paging dari Room `mapel`, sync API `studentSubject(take, skip)` |
| Card mapel | Nama mapel, nama guru (opsional), tag/label (`MapelAdapter` + `mapel_item.xml`) |

**Rules UI / perilaku**

1. Fetch mapel pertama kali saat `lifecycleScope.launchWhenCreated`.
2. Search debounce **400 ms**; filter lokal Room (`name` / `label` LIKE keyword).
3. Empty state **"Mata pelajaran tidak ditemukan"** hanya muncul jika keyword tidak kosong **dan** hasil kosong. List kosong tanpa search tidak memaksa empty state search.
4. Swipe refresh reset ke page awal (`fetchMapel(0)`).
5. Pagination: load next jika `hasNextMapel` dan `countMapel >= 20`.
6. Tap mapel → `MateriPage` dengan extra:
   - `title` = nama mapel
   - `id` = `mapel.id` (subjectId)

### 4.2 `MateriPage` — Daftar materi per mapel

| Item | Detail |
| --- | --- |
| Layout | `materi_page.xml` |
| Elemen UI | Toolbar (judul = nama mapel), swipe refresh, list card materi, empty state default visible |
| Data | Paging `getMateri(subjectId)`; API `studentTheoryBySubject` |
| Card | Avatar guru, judul, waktu (`created_at_label`), tag mapel, nama guru (`materi_item.xml`) |

**Rules**

1. Extra wajib: `id` (subjectId). Default `0` jika hilang.
2. Saat fetch `start == 0`, cache materi subject tersebut dihapus dulu (`deleteMateriBySubject`) lalu diisi ulang.
3. Empty state layout default **visible**; disembunyikan hanya jika list **tidak kosong**. (Tidak ada cabang eksplisit untuk meng-set empty lagi saat list kosong setelah refresh.)
4. Menu kebab **tidak** diaktifkan untuk siswa (`MateriAdapter` dipanggil tanpa `myId` / `onClickOption` → `isMine = false`).
5. Tap card → `MateriDetailPage` dengan `title`, `id` (theoryId), `subId` (subjectId).

### 4.3 `MateriDetailPage` — Detail materi

| Item | Detail |
| --- | --- |
| Layout | `materi_detail_page.xml` |
| Elemen | Toolbar, deskripsi, card file materi, preview URL, section pembahasan + card file pembahasan |

**Rules**

1. Extra wajib: `id` > 0 **dan** `subId` > 0.
2. Jika salah satu hilang/0 → dialog non-cancelable **"Materi Tidak tersedia"** → tombol Tutup → finish.
3. Loading "menampilkan detail materi" saat observe detail.
4. Sumber data: Room `getDetailMateri(id)` dulu; jika null → fetch API (`studentTheoryDetail` / `teacherTheoryDetail` tergantung `is_teacher`).
5. Deskripsi kosong → label + teks deskripsi **disembunyikan**.
6. Card file materi tampil hanya jika `file_name` **dan** `file_path` tidak kosong.
7. **Lihat**: download bytes via `apiService.download(url)` ke `filesDir`, lalu `IntentUtil.openFile`.
8. **Download**: `IntentUtil.downloadFile(..., type = "materi")`.
9. Section pembahasan tampil jika `explanation_file_path` tidak kosong (field lokal dari `file_edited` response).
10. Link preview tampil jika ada entri di tabel `materi_link`; memakai URL pertama.
11. Screen ini **dibagikan** siswa & guru (read-only; tidak ada tombol edit di detail).

### 4.4 `PdfPage` — Pembaca PDF (pendukung)

Activity terdaftar di manifest. Membuka file lokal atau download dari URL, menampilkan via `PdfViewer`. Saat ini alur detail materi utama memakai `openFile` (viewer eksternal/sistem), bukan navigasi otomatis ke `PdfPage`.

---

## 5. Screen-by-screen — Role guru

### 5.1 `TheoryTeacherPage` — Shell

| Item | Detail |
| --- | --- |
| Layout | `theoryteacher_page.xml` |
| Nav | `@navigation/theory_teacher_nav` → start `materiTeacherpage` |
| Elemen | Toolbar "Materi", FAB `btn_add`, (tab Materi Saya / Mata Pelajaran — **hidden**) |

**Rules**

1. FAB → `UploadMateriPage` tanpa `materiId` (= mode create).
2. Back → `supportFinishAfterTransition()`.
3. Kode switch tab masih ada di `TheoryTeacherPage` (ubah stroke/warna + navigate global action), tetapi container tab di XML `visibility=gone`.

### 5.2 `MateriTeacherpage` — Materi Saya

| Item | Detail |
| --- | --- |
| Layout | `materi_teacher_page.xml` |
| ViewModel | `TheoryViewModel` (activity-scoped) |
| Filter | Dropdown **Kelas** + **Mata Pelajaran** (default "Semua") |
| List | Materi milik `teacherId` dari pref, order `id desc` |

**Rules filter**

1. Opsi Kelas: prepend `"Semua"` + list classroom (urut grade lalu nama). Display: `"{grade} - {name}"` jika grade > 0.
2. Opsi Mapel: prepend `"Semua"` + list mapel lokal; jika kosong fetch `teacherSubjectTeach()`.
3. Pilih Kelas (bukan Semua) → `listTeacherMateri(subjectId = null, classId = id)`.
4. Pilih Mapel (bukan Semua) → `listTeacherMateri(subjectId = id, classId = null)`.
5. Filter kelas & mapel saat ini **saling mengganti observer** (tidak menggabungkan kedua filter sekaligus di UI listener; kombinasi subject+class tersedia di ViewModel tapi tidak di-wire dari kedua dropdown bersamaan).
6. Swipe refresh: reset paging flag + `fetchMateriTeacher(0)`.
7. Empty state ditoggle eksplisit (beda dengan `MateriPage` siswa).

**Rules aksi card**

1. Tap card → `MateriDetailPage` (sama seperti siswa).
2. Menu kebab visible **hanya** jika `pref.user_id == teacher.sosmed_user_id` (`isMine`).
3. Opsi kebab:
   - **Edit Materi** → `UploadMateriPage` + extra `materiId`; setelah `RESULT_OK` refresh list.
   - **Hapus Materi** → konfirmasi "Anda yakin akan menghapus materi?" → ProgressDialog → `DELETE .../theories/{id}` → hapus row Room jika sukses.

### 5.3 `MapelTeacherPage` — Mata pelajaran diampu

Mirip `TheoryPage` siswa, tanpa search:

1. Fetch `teacherSubject` (karena `isTeacher`).
2. Tap mapel → `MateriPage` (daftar materi subject; API cabang `teacherTheoryBySubject`).

### 5.4 `UploadMateriPage` — Create / Edit

| Item | Detail |
| --- | --- |
| Layout | `upload_materi_page.xml` |
| ViewModel | `UploadMateriViewmodel` |
| Mode | Create: tanpa `materiId` / `materiId = 0`. Edit: `materiId > 0` → `setMateri(id)` isi form dari Room |

#### Elemen form

| Field | Batas / UI | Binding |
| --- | --- | --- |
| Judul Materi | max **500** karakter, counter `n/500` | `materiTitle` |
| Deskripsi | max **5000** karakter, counter `n/5000` | `materiDesc` |
| Mata Pelajaran | dropdown wajib | `subjectId` |
| Ditampilkan ke | radio **Jenjang** / **Jurusan** / **Kelas** (saling exclusive) | `showSelected` |
| File Materi | hint "File dalam format pdf"; picker mendukung PDF & image | `materiFile` |
| Url Link | debounce preview 1 detik | `materiLink` |
| Tombol | "Posting materi" (`btnPost`) | enabled via `allowUpload` |

#### Rules target audience ("Ditampilkan ke")

Hanya **satu** mode aktif:

| Mode | Radio | Field aktif | Payload multipart |
| --- | --- | --- | --- |
| Jenjang (default) | `grade` | `inputGrade` | `grade` |
| Jurusan | `major` | `inputJurusan` | `school_major_id` |
| Kelas | `class` | `inputKelas` | `school_classes_id` |

Saat ganti mode: field lain di-disable + background gray; ID field non-aktif di-reset ke `0`.

Sumber opsi:

- Mapel: `teacherSubjectTeach`
- Grade: `gradeTeach` / `mobile/teacher/school-grade`
- Kelas: `teacherClassRoom` / `mobile/teacher/school-class-room`
- Jurusan: `teacherMajor` / `.../teachers/school-majors`

#### Rules validasi tombol Posting (`allowUpload = true` jika)

1. Judul **tidak kosong**, **dan**
2. `subjectId > 0`, **dan**
3. Ada **file** (`materiFile` tidak kosong) **atau** ada **link** (`materiLink` tidak kosong).

Deskripsi **tidak** wajib. Target jenjang/jurusan/kelas **tidak** masuk kondisi `allowUpload` (hanya dikirim sesuai radio terpilih).

#### Rules file & upload

1. Ukuran file maksimal **12 MB**. Lebih dari itu → error toast `"ukuran file melebihi batas maksimal (12Mb)"`, upload dibatalkan.
2. Multipart field:
   - Text: `name`, `description`, `link`, `school_subject_id`, + salah satu target (`grade` / `school_major_id` / `school_classes_id`)
   - File part name: `"file"` (opsional jika hanya link)
3. Create → `POST .../teachers/theories`
4. Update → `POST .../teachers/theories/update/{id}`
5. Sukses → `setResult(RESULT_OK)` + finish.
6. Edit mode: isi ulang judul, deskripsi, link, subject, grade/class/major, info file; radio dipilih dari prioritas `major_id > 0` → major, else `class_id > 0` → class, else grade.
7. Preview link gagal → toast `"gagal menampilkan preview url"`; flag `linkAttached` di-set, tapi enable tombol tetap mengikuti aturan file **atau** teks link (bukan wajib sukses preview).

---

## 6. Perbandingan rules siswa vs guru

| Rules | Siswa | Guru |
| --- | --- | --- |
| Masuk fitur | `is_having_class` + `is_student` | `is_having_class` + non-siswa |
| Lihat daftar mapel | Ya (`TheoryPage` + search) | Ya via `MapelTeacherPage` (tab saat ini hidden) |
| Lihat daftar materi per mapel | Ya | Ya (reuse `MateriPage`) |
| Lihat detail / unduh | Ya | Ya |
| Search mapel | Ya (debounce 400 ms) | Tidak di Materi Saya; tidak di MapelTeacherPage |
| Filter kelas/mapel | Tidak | Ya di Materi Saya |
| Upload materi | Tidak | Ya (FAB) |
| Edit materi | Tidak | Ya, hanya materi milik sendiri |
| Hapus materi | Tidak | Ya, hanya milik sendiri + konfirmasi |
| Endpoint list/detail | `.../students/...` | `.../teachers/...` |
| Ownership kebab | N/A | `user_id` == `teacher.sosmed_user_id` |

---

## 7. Model data inti

### 7.1 Response / entity penting

- `MapelItem` / `MapelTable` — mata pelajaran (+ icon, label, teacher).
- `MateriItem` / `MateriTable` — materi (nama, deskripsi, file_*, subject, teacher, grade, class, major, `file_edited` → `explanation_file_path`).
- `MateriLink` / `MateriLinkTable` — daftar URL terkait materi.
- `MateriMapelTeacher` — join untuk card list (materi + mapel + teacher + user avatar).
- `MateriWithLink` — detail (materi + list link).
- `GradeTable`, `MajorItem`, `ClassRoomTable` — opsi target upload guru.

### 7.2 Mapping `school_class` / `school_major`

Pada `MateriTable.fromMateriItem`, `school_class` dan `school_major` bertipe `Any?` dan diparse sebagai `Map` untuk mengambil `id`. Gagal parse → `0`.

---

## 8. API reference (Materi)

### Siswa

| Method | Path | Dipakai untuk |
| --- | --- | --- |
| GET | `mobile/app/learning/theories/students/subjects` | Daftar mapel |
| GET | `.../students/subjects/{subjectId}/theories` | Daftar materi mapel |
| GET | `.../students/subjects/{subjectId}/theories/{theoryId}` | Detail materi |

Query paging umum: `take`, `skip` (ukuran page app = 20).

### Guru

| Method | Path | Dipakai untuk |
| --- | --- | --- |
| GET | `.../teachers/subjects` | Daftar mapel (paging / teach) |
| GET | `.../teachers/theories` | Materi Saya (`school_subject`, `school_class` opsional) |
| GET | `.../teachers/subjects/{subjectId}/theories` | Materi per mapel |
| GET | `.../teachers/subjects/{subjectId}/theories/{theoryId}` | Detail |
| POST multipart | `.../teachers/theories` | Create |
| POST multipart | `.../teachers/theories/update/{id}` | Update |
| DELETE | `.../teachers/theories/{id}` | Hapus |
| GET | `.../teachers/school-majors` | Opsi jurusan |
| GET | `mobile/teacher/school-grade` | Opsi jenjang |
| GET | `mobile/teacher/school-class-room` | Opsi kelas |

Download file konten memakai endpoint download generik `apiService.download(url)`.

---

## 9. Cache & paging (aturan teknis UI)

1. List mapel/materi ditampilkan dari **Room** via `toLiveData(pageSize = 20)` + `PagedListBoundaryCallback`.
2. Boundary callback memicu fetch network saat list kosong / scroll ke akhir (jika `hasNext*`).
3. Guard `prevStart == start` mencegah double-fetch page yang sama.
4. Guru fetch Materi Saya dengan `start == 0` memanggil `clearMateri()` (hapus **semua** cache materi) sebelum insert ulang.
5. Siswa/guru fetch by subject dengan `start == 0` hanya `deleteMateriBySubject(subjectId)`.
6. Error network dipost ke `errorString` → toast di page.

---

## 10. Daftar file terkait

### Siswa / shared

| File | Peran |
| --- | --- |
| `pages/theory/TheoryPage.kt` | Daftar + search mapel |
| `pages/theory/MateriPage.kt` | Daftar materi per mapel |
| `pages/theory/MateriDetailPage.kt` | Detail, lihat/unduh |
| `pages/theory/PdfPage.kt` | Viewer PDF |
| `pages/theory/TheoryViewModel.kt` | Fetch, paging, delete, detail |
| `pages/theory/TheoryModels.kt` | Model + Room entities |
| `pages/theory/TheoryDao.kt` | Query Room |
| `pages/theory/MapelAdapter.kt` / `MateriAdapter.kt` | List adapters |
| `res/layout/theory_page.xml`, `materi_page.xml`, `materi_detail_page.xml`, `materi_item.xml`, `mapel_item.xml`, `materi_detail_item.xml` | UI |

### Guru

| File | Peran |
| --- | --- |
| `pages/theoryteacher/TheoryTeacherPage.kt` | Shell + FAB |
| `pages/theoryteacher/MateriTeacherpage.kt` | Materi Saya + filter + edit/hapus |
| `pages/theoryteacher/MapelTeacherPage.kt` | Mapel diampu |
| `pages/theoryteacher/UploadMateriPage.kt` | Form create/edit |
| `pages/theoryteacher/UploadMateriViewmodel.kt` | Validasi + multipart |
| `pages/theoryteacher/UploadMateriBindConverter.kt` | Two-way binding id ↔ nama |
| `res/navigation/theory_teacher_nav.xml` | Nav host guru |
| `res/layout/theoryteacher_page.xml`, `materi_teacher_page.xml`, `upload_materi_page.xml`, `materi_upload_item.xml` | UI |

### Entry & notifikasi

| File | Peran |
| --- | --- |
| `pages/pembelajaran/PembelajaranPage.kt` | Menu Materi + lock + role routing |
| `services/NotifRouter.kt` | Deep link `"theory"` → detail (`id`, `subId`) |
| `db/OnKlasDbUtil.kt` | `processMapelResponse` / `processMateriResponse` |

---

## 11. Checklist QA cepat

### Siswa

- [ ] Tanpa kelas (`is_having_class=false`) → menu locked + dialog.
- [ ] Dengan kelas → daftar mapel load; search debounce; empty search state.
- [ ] Tap mapel → daftar materi; swipe refresh; empty "Belum terdapat materi".
- [ ] Detail: deskripsi optional; file Lihat/Download; link preview; pembahasan jika ada.
- [ ] Extra `id`/`subId` invalid → dialog Materi Tidak tersedia.

### Guru

- [ ] Entry membuka shell Materi + FAB.
- [ ] Materi Saya: filter Kelas/Mapel "Semua" dan spesifik.
- [ ] Kebab hanya di materi milik sendiri; Edit membuka form terisi; Hapus butuh konfirmasi.
- [ ] Upload: tombol disabled sampai judul + mapel + (file atau link); file >12MB ditolak.
- [ ] Radio Jenjang/Jurusan/Kelas exclusive; hanya satu field target terkirim.
- [ ] Create & update sukses menutup form dan merefresh list (setelah edit via Activity Result).

---

## 12. Catatan implementasi saat ini

1. **Tab Materi Saya / Mata Pelajaran** di shell guru disembunyikan di XML; produk efektif guru saat ini berpusat pada **Materi Saya + FAB**.
2. Filter Kelas dan Mapel di Materi Saya tidak di-combine di UI (masing-masing listener mengabaikan filter pasangan).
3. `MateriPage` (siswa/guru per mapel) hanya **menyembunyikan** empty state saat ada data; tidak men-toggle balik ke visible di observer yang sama.
4. Notifikasi in-app `"THEORY"` memakai extra `child_id`, sementara detail membaca `id` + `subId` — bedakan dari deep link `NotifRouter`.
5. Label UI upload menyebut PDF, tetapi picker file mengizinkan PDF dan image.

---

*Dokumen ini mengikuti perilaku kode di branch kerja saat dokumentasi dibuat. Jika ada perubahan kontrak API backend (mis. wajib target kelas, batas ukuran file, atau field baru), sesuaikan bagian rules upload dan API reference.*
