**# API Spec `theories/teachers`

Sumber route: `routes/mobile.php` pada group `app/learning/theories/teachers` (baris 134-149).  
Semua endpoint di bawah ini berada di dalam middleware `auth:sanctum`.

## Base Path

`/app/learning/theories/teachers`

## Auth & Header

- Wajib: `Authorization: Bearer <token>`
- `Accept: application/json`
- Untuk upload file: `Content-Type: multipart/form-data`

---

## Endpoint List

1. `GET /school-majors`
2. `GET /subjects`
3. `GET /subjects/{id}/theories`
4. `GET /subjects/{id}/theories/{ids}`
5. `GET /theories`
6. `POST /theories`
7. `DELETE /theories/{id}`
8. `POST /theories/update/{id}`

---

## 1) GET `/school-majors`

Ambil jurusan berdasarkan `school_id` user login.

### Query

- `skip` (optional, default `0`)
- `take` (optional, default `1000`)

### Proses request

1. Ambil user dari token.
2. Query `school_majors` dengan filter `school_id = auth()->user()->school_id`.
3. Urutkan `name ASC`.
4. Terapkan `skip/take`.
5. Transform dengan `SchoolMajorResource`.

### Response 200

```json
{
  "data": [
    { "id": 101, "name": "Multimedia" }
  ]
}
```

---

## 2) GET `/subjects`

Ambil daftar mata pelajaran di sekolah user login.

### Query

- `skip` (optional, default `0`)
- `take` (optional, default `1000`)

### Proses request

1. Query `school_subjects` dengan `school_id = auth()->user()->school_id`.
2. Urutkan `school_subjects.created_at DESC`.
3. Terapkan `skip/take`.
4. Transform via `SchoolSubjectResource`.

### Response 200

```json
{
  "data": [
    {
      "id": 1,
      "name": "Konsep Jaringan",
      "icon_image": "https://.../default.png",
      "school": null
    }
  ]
}
```

Catatan: `school` memakai `whenLoaded`, sehingga umumnya `null` jika relasi tidak di-load.

---

## 3) GET `/subjects/{id}/theories`

Ambil materi berdasarkan mapel tertentu.

### Path param

- `id` (required): `school_subject_id`

### Query

- `skip` (optional, default `0`)
- `take` (optional, default `1000`)

### Proses request

1. Query `school_theories`.
2. Filter `activated = 1`.
3. Filter `school_subject_id = {id}`.
4. Urutkan `school_theories.created_at DESC`.
5. Terapkan `skip/take`.
6. Transform dengan `SchoolTheoryResource`.

### Response 200 (contoh)

```json
{
  "data": [
    {
      "id": 6,
      "name": "Judul Materi",
      "description": "--deskripsi--",
      "uri": { "link": ["https://example.com"] },
      "file_name": "materi.pdf",
      "file_path": "https://.../theories/...pdf",
      "file_type": "application/pdf",
      "file_size": "14425",
      "file_edited": "",
      "is_explained": false,
      "explanations": {},
      "is_explanation": false,
      "grade": 10,
      "subject": { "id": 1, "name": "Konsep Jaringan", "icon_image": "https://.../default.png", "school": null },
      "school_class": "",
      "school_major": "",
      "teacher": { "id": 1, "name": "Nama Guru", "nip": "12345" },
      "created_at_label": "25 Oct 2020 02:11"
    }
  ]
}
```

Catatan: endpoint ini tidak memfilter `teacher_id` login, hanya berdasarkan mapel dan `activated`.

---

## 4) GET `/subjects/{id}/theories/{ids}`

Ambil detail materi tertentu di mapel tertentu.

### Path param

- `id` (required): `school_subject_id`
- `ids` (required): `school_theories.id`

### Proses request

1. Query `school_theories` dengan:
    - `id = {ids}`
    - `school_subject_id = {id}`
2. Ambil satu data (`first()`).
3. Return `SchoolTheoryResource`.

### Response 200

```json
{
  "data": {
    "id": 2,
    "name": "Materi #2"
  }
}
```

---

## 5) GET `/theories`

Ambil daftar materi milik guru login, dengan filter opsional.

### Query

- `school_class` (optional, numeric) -> filter `school_classes_id`
- `school_subject` (optional, numeric) -> filter `school_subject_id`
- `skip` (optional, default `0`)
- `take` (optional, default `1000`)

### Validasi

- `school_class`: `nullable|numeric`
- `school_subject`: `nullable|numeric`

Invalid validasi: `422 Unprocessable Entity` (format default Laravel validation error).

### Proses request

1. Validasi query.
2. Ambil teacher login dari `auth()->user()->teacher->id`.
3. Query `school_theories`:
    - `activated = 1`
    - `teacher_id = teacher login`
4. Terapkan filter opsional `school_class` / `school_subject` jika ada.
5. Urutkan `created_at DESC`.
6. Terapkan `skip/take`.
7. Return collection `SchoolTheoryResource`.

### Response 200

Format item sama seperti endpoint list pada `GET /subjects/{id}/theories`.

---

## 6) POST `/theories`

Buat materi baru (guru).

### Body (multipart/form-data)

- `name` (required)
- `school_subject_id` (required)
- `description` (optional)
- `file` (optional, max `12288` KB / 12 MB)
- `link` (optional, string atau array string)
- field model lain diperbolehkan bila sesuai kolom tabel

### Validasi

- `name`: required
- `school_subject_id`: required
- `file`: max:12288

Jika gagal validasi -> `422`.

### Proses request

1. Validasi input.
2. Cek user punya relasi teacher.
3. Jika ada file:
    - upload ke S3: `theories/{school_uuid}`
    - set metadata `file_name`, `file_path`, `file_type`, `file_format`, `file_size`
4. Jika ada `link`:
    - normalisasi jadi array
    - simpan ke field `uri` (JSON string)
5. Set `teacher_id` dari teacher login.
6. Simpan `SchoolTheory::create($request->except(['file', 'link']))`.

### Response

- `200`

```json
{ "message": "Berhasil membuat Materi" }
```

- `400` (bukan teacher)

```json
{ "message": "Akun anda tidak boleh menambah materi" }
```

- `400` (exception)

```json
{ "error": "..." }
```

---

## 7) DELETE `/theories/{id}`

Soft delete materi (set `activated = 0`) dan hanya boleh oleh pemilik materi.

### Path param

- `id` (required): ID materi

### Proses request

1. Cek user adalah teacher.
2. Ambil materi `findOrFail($id)`.
3. Verifikasi ownership: `theories.teacher_id === teacher login`.
4. Update `activated = 0`.

### Response

- `200`

```json
{ "message": "Berhasil menghapus Materi" }
```

- `400` (forbidden)

```json
{ "message": "Akun anda tidak boleh menghapus materi" }
```

- `400` (exception)

```json
{ "error": "..." }
```

---

## 8) POST `/theories/update/{id}`

Update materi milik guru.

### Path param

- `id` (required): ID materi

### Body (multipart/form-data)

Opsional sesuai field yang ingin diubah, termasuk:

- `name`
- `description`
- `school_subject_id`
- `file` (max 12 MB)
- `link` (string atau array string)

### Validasi

- `file`: `max:12288`

Gagal validasi -> `422`.

### Proses request

1. Cek teacher login.
2. Ambil materi berdasarkan `id`.
3. Cek ownership.
4. Jika upload file baru:
    - hapus file lama di S3 (jika ada)
    - upload file baru ke `theories/{school_uuid}`
    - update metadata file
5. Jika ada `link`, simpan ke `uri` sebagai JSON string.
6. Update data: `$theories->update($request->except(['file', 'link']))`.

### Response

- `200`

```json
{ "message": "Berhasil Merubah Materi" }
```

- `400` (forbidden/exception)

```json
{ "message": "Akun anda tidak boleh merubah materi" }
```

atau

```json
{ "error": "..." }
```

---

## Mapping Field `SchoolTheoryResource`

Field yang umum muncul di endpoint materi:

- `id`, `name`, `description`
- `uri` (hasil `json_decode` dari kolom `uri`)
- `file_name`, `file_path`, `file_type`, `file_size`
- `file_edited`, `is_explained`, `explanations`, `is_explanation`
- `grade`
- `subject` (resource mapel)
- `school_class`, `school_major`
- `teacher`
- `created_at_label` (format `d M Y H:i`)

Catatan implementasi:

- `file_path` diturunkan dari method model (`getFileName()`), jadi nilainya URL/path final hasil helper model, bukan raw kolom DB.
- `is_explanation` ditentukan dari keberadaan relasi `explanations`.**
