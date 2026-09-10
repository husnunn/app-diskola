# Spesifikasi API — Mobile Student (Sosmed / Pembelajaran)

Sumber route: `routes/mobile.php`, group `student` baris 405–425.

## Konteks umum

| Item | Nilai |
|------|--------|
| **Base path** | `/api/mobile/student` |
| **Prefix global** | `api/mobile` (didefinisikan di `RouteServiceProvider`) |
| **Middleware** | `api`, `auth:sanctum` |
| **Autentikasi** | Header `Authorization: Bearer {token}` (Laravel Sanctum) |

### Query opsional (endpoint daftar)

Banyak endpoint GET mendukung pola berikut:

| Parameter | Default | Keterangan |
|-----------|---------|------------|
| `skip` | `0` | Offset baris |
| `take` | `1000` | Limit jumlah baris |
| `filter` | — | Struktur array/JSON; diproses `SQLHelper::parseFilter` menjadi klausa `WHERE` mentah |
| `sort` | per endpoint | Struktur array; diproses `SQLHelper::parseOrder` |

### Format respons (Laravel API Resource)

- Koleksi: biasanya `{ "data": [ ... ] }`.
- Satu resource: biasanya `{ "data": { ... } }`.

### Content-Type

- JSON: `application/json`
- Unggah file: `multipart/form-data`

---

## 1. Daftar mata pelajaran sekolah

**`GET /api/mobile/student/school-subject`**

| | |
|--|--|
| **Controller** | `App\Http\Controllers\Sosmed\SchoolSubjectController@index` |
| **Resource** | `SchoolSubjectResource` (koleksi) |

### Request

- **Path / body:** tidak ada.
- **Query (opsional):** `skip`, `take`, `filter`, `sort`.
- **Sort default:** `school_subjects.created_at DESC`.
- **Filter server:** hanya mapel dengan `school_id` = `auth()->user()->school->id`.

### Response `200` — item dalam `data[]`

| Field | Tipe | Keterangan |
|-------|------|------------|
| `id` | integer | ID `school_subjects` |
| `name` | string | Nama mapel |
| `icon_image` | string | URL/icon |
| `message_label` | string | `"Baru"` jika dibuat &lt; 1 jam dari sekarang, selain itu `""` |
| `teacher` | object | Saat ini selalu `{}` |

---

## 2. Daftar materi (teori) per mata pelajaran

**`GET /api/mobile/student/school-subject/{ids}/theory`**

| | |
|--|--|
| **Controller** | `App\Http\Controllers\Sosmed\SchoolTheoryController@index` |
| **Resource** | `SchoolTheoryResource` (koleksi) |

### Request

- **`{ids}`:** ID **`school_subjects`** (satu mapel).
- **Query (opsional):** `skip`, `take`, `filter`, `sort`.
- **Sort default:** `school_theories.name ASC`.
- **Filter server:** join `school_subjects`; `school_subjects.id = {ids}` dan `school_subjects.school_id = auth()->user()->school_id`.

### Response `200` — item dalam `data[]`

| Field | Tipe | Keterangan |
|-------|------|------------|
| `id` | integer | ID teori |
| `name` | string | Judul materi |
| `file_name` | string | Boleh kosong |
| `file_path` | string | URL publik (`getFileName()` / AssetHelper) |
| `file_format`, `file_type`, `file_size` | string | Metadata lampiran |
| `created_at_label` | string | Format `d M Y H:i` |
| `message_label` | string | `"Baru"` jika &lt; 1 jam |
| `subject` | object | `SchoolSubjectResource` |
| `school` | object | `SchoolResource` (UUID sekolah, wilayah, kontak, gambar, dll.) |
| `teacher` | object | `TeacherResource` (`id`, `name`, `nip`, `user` → `UserResource`) |

---

## 3. Materi terbaru per mata pelajaran

**`GET /api/mobile/student/school-subject/{ids}/theory-lastest`**

> Catatan: ejaan di route adalah `theory-lastest` (bukan `latest`).

| | |
|--|--|
| **Controller** | `App\Http\Controllers\Sosmed\SchoolTheoryController@indexLastest` |
| **Resource** | `SchoolTheoryResource` (koleksi) |

### Request

- Sama dengan **§2** untuk `{ids}`, query, dan pembatasan sekolah.
- **Sort default:** `school_theories.created_at DESC` (perbedaan utama dari §2).

### Response `200`

- Struktur sama **§2**.

---

## 4. Detail satu materi

**`GET /api/mobile/student/school-subject/{ids}/theory/{id}`**

| | |
|--|--|
| **Controller** | `App\Http\Controllers\Sosmed\SchoolTheoryController@show` |
| **Resource** | `SchoolTheoryResource` (tunggal) |

### Request

- **`{ids}`:** ID mapel sekolah (ada di URL).
- **`{id}`:** ID **`school_theories`** (implementasi mengambil `SchoolTheory::find($id)`).

### Response `200`

- Satu objek `SchoolTheoryResource` — field sama **§2**.

---

## 5. Tugas — semua belum dikumpulkan (todo)

**`GET /api/mobile/student/assignment-todo`**

| | |
|--|--|
| **Controller** | `App\Http\Controllers\Sosmed\StudentAssignmentController@indexAllTodo` |
| **Resource** | `SchoolSubjectAssigmentTodoResource` (koleksi) |

### Request

- **Query (opsional):** `skip`, `take`, `filter`, `sort`.
- **Sort default:** `school_subject_assignments.created_at DESC`.
- **Logika server:** dari user login: `student`, lalu `school_class_id` dari `schoolClassStudent`. Assignment yang:
    - tidak punya relasi `studentAssignments`,
    - `grade` = grade siswa,
    - `school_classes_id` = kelas siswa.

### Response `200` — item dalam `data[]`

| Field | Keterangan |
|-------|------------|
| `id`, `title`, `description` | Data tugas |
| `checked`, `downloded`, `uploaded` | Angka/flag (ejaan `downloded` mengikuti kode) |
| `file_name`, `file_path`, `file_format`, `file_type`, `file_size` | Lampiran tugas guru |
| `upload_at_label`, `created_at_label`, `end_at_label` | String terformat |
| `message_label` | `"Baru"` jika &lt; 1 jam |
| `information_label` | String HTML (tenggat lewat / akan berakhir) |
| `is_overdue` | boolean |
| `subject`, `teacher`, `school`, `theories`, `class` | Resource bersarang |

---

## 6. Tugas — sudah dikumpulkan, belum dinilai

**`GET /api/mobile/student/assignment-done`**

| | |
|--|--|
| **Controller** | `App\Http\Controllers\Sosmed\StudentAssignmentController@indexAllDone` |
| **Resource** | `StudentAssignmentDoneResource` (koleksi) |

### Request

- **Query (opsional):** `skip`, `take`, `filter`, `sort`.
- **Sort default:** `student_assignments.created_at DESC`.
- **Filter:** `upload_at` NOT NULL, `scored_at` NULL, `student_id` = siswa login.

### Response `200`

- Per item: gabungan field dari `school_subject_assignment` + **`file_student_*`** (berkas siswa), `message_label` (HTML), `information_label`, nested `subject`, `teacher`, `school`, `theories`, `class`.

> Catatan implementasi: di resource ada akses `chool_theories` (typo); pastikan relasi Eloquent di model assignment konsisten agar field `theories` tidak error.

---

## 7. Tugas — sudah dinilai

**`GET /api/mobile/student/assignment-scored`**

| | |
|--|--|
| **Controller** | `App\Http\Controllers\Sosmed\StudentAssignmentController@indexAllScored` |
| **Resource** | `StudentAssignmentScoredResource` (koleksi) |

### Request

- **Query (opsional):** sama pola §6.
- **Sort default:** `student_assignments.created_at DESC`.
- **Filter:** `upload_at` NOT NULL, `scored_at` NOT NULL, `student_id` = siswa login.

### Response `200`

- Mirip §6, ditambah **`score`**, `information_label` memakai `scored_at`.

---

## 8. Daftar tugas per mata pelajaran

**`GET /api/mobile/student/school-assignment/{id}`**

| | |
|--|--|
| **Controller** | `App\Http\Controllers\Sosmed\StudentAssignmentController@index` |
| **Resource** | `SchoolSubjectAssigmentResource` (koleksi) |

### Request

- **`{id}`:** **`school_subject_id`**.
- **Query (opsional):** `skip`, `take`, `filter`, `sort`.
- **Sort default:** `school_subject_assignments.created_at DESC`.
- **Filter:** `school_subject_id = {id}`.

### Response `200` — item dalam `data[]`

| Field | Keterangan |
|-------|------------|
| `id`, `title`, `description`, `checked`, `downloded`, `uploaded` | Tugas |
| `file_*` | Lampiran guru |
| `upload_at_label`, `created_at_label`, `end_at_label` | Label tanggal |
| `message_label`, `information_label`, `is_overdue` | Status / tenggat |
| `subject`, `teacher`, `school`, `theories`, `class` | Nested resources |

---

## 9. Mulai / catat pengumpulan tugas

**`POST /api/mobile/student/school-assignment/{id}/collect`**

| | |
|--|--|
| **Controller** | `App\Http\Controllers\Sosmed\StudentAssignmentController@store` |
| **Resource** | `StudentAssignmentResource` |

### Request

- **`{id}`:** **`school_subject_assignment_id`**.
- **Body (JSON atau form):**

| Field | Validasi | Keterangan |
|-------|----------|------------|
| `checked` | `numeric` | Opsional |
| `uploaded` | `numeric` | Opsional |

Server membuat `StudentAssignment` dengan `student_id` (login), `school_subject_assignment_id`, `upload_at` = waktu sekarang.

### Response

| HTTP | Body |
|------|------|
| `200` | `StudentAssignmentResource`: `id`, `upload_at`, `score`, `scored`, `scored_at`, `checked`, `uploaded`, metadata file, `student`, `subject_assignment` |
| `400` | `{ "message": "<pesan exception>" }` |
| `422` | Error validasi Laravel |

---

## 10. Unggah berkas pengumpulan tugas

**`POST /api/mobile/student/school-assignment/{id}/collect/{ids}/file`**

| | |
|--|--|
| **Controller** | `App\Http\Controllers\Sosmed\StudentAssignmentController@collectFile` |
| **Resource** | `StudentAssignmentResource` |

### Request

- **`{id}`:** ada di URL; tidak dipakai di logika update (yang dipakai hanya `{ids}`).
- **`{id}` path segment:** `school-assignment/{id}` — tetap wajib mengikuti route.
- **`{ids}`:** ID **`student_assignments`**.
- **Body:** `multipart/form-data`, field **`file`** (wajib): `max:5120` (KB), `mimes:jpeg,png,pdf`.

File disimpan ke disk `s3` di path `assignment-collect-student/...`, metadata di-update pada record `StudentAssignment`.

### Response

| HTTP | Body |
|------|------|
| `200` | `StudentAssignmentResource` |
| `400` | `{ "message": "..." }` |
| `404` | Jika `StudentAssignment` dengan `{ids}` tidak ada (`firstOrFail`) |
| `422` | Validasi file gagal |

---

## 11. Daftar ujian sekolah (tab “Ikuti Ujian”)

**`GET /api/mobile/student/school-examination`**

| | |
|--|--|
| **Controller** | `App\Http\Controllers\Mobile\Student\SchoolExaminationController@index` |
| **Resource** | `SchoolExaminationAllResource` (koleksi) |

### Request

- Tidak ada query khusus; data: `SchoolExamination::get()` (semua baris model sesuai query default).

### Response `200` — item dalam `data[]`

Hanya field berikut yang **aktif** di resource (field lain dikomentari di kode):

| Field | Tipe | Keterangan |
|-------|------|------------|
| `id` | integer | |
| `title` | string | |
| `ready_to_download` | boolean | Saat ini selalu `false` |

---

## Ringkasan status HTTP

| Kode | Kapan |
|------|--------|
| `401` | Tidak terautentikasi / token tidak valid |
| `404` | Resource tidak ditemukan (mis. `collectFile`) |
| `422` | Validasi gagal |
| `400` | Exception bisnis / DB pada beberapa `POST` |

---

## Referensi file kode

- Route: `routes/mobile.php` (sekitar baris 405–425).
- Controller: `app/Http/Controllers/Sosmed/SchoolSubjectController.php`, `SchoolTheoryController.php`, `StudentAssignmentController.php`, `app/Http/Controllers/Mobile/Student/SchoolExaminationController.php`.
- Resource: `app/Http/Resources/Sosmed/*`, `app/Http/Resources/Mobile/Student/SchoolExaminationAllResource.php`.
