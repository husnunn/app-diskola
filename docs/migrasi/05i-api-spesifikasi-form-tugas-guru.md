# API Spesifikasi Mobile — Form Tugas Guru dan Teacher Data Scope

Dokumen ini adalah kontrak implementasi form tambah/edit tugas pada aplikasi mobile.
Tujuannya menyamakan alur mobile dengan CMS sekolah tanpa memutus aplikasi lama.

## 1. Aturan Umum

- Semua endpoint memakai `Authorization: Bearer <token>` dan `Accept: application/json`.
- Base URL endpoint assignment: `{EXAM_HOST}/api/mobile`.
- Endpoint scope berada di service presensi: `{PRESENSI_HOST}/api/mobile`.
- Mobile **wajib bercabang berdasarkan `data.restricted`**, bukan hanya `data.scope`.
- Satu request create menghasilkan satu tugas untuk satu kelas. Untuk multi-kelas, kirim
  satu request per kelas dan tampilkan hasil sukses/gagal tiap kelas.
- Jika request scope gagal, gunakan alur jadwal lama (`assigned`) sebagai fallback aman.

## 2. Baca Mode Form

```http
GET {PRESENSI_HOST}/api/mobile/attendance/teacher-data-scope
```

Response `200`:

```json
{
  "data": {
    "scope": "assigned",
    "label": "Cakupan Data Guru (Mapel & Kelas)",
    "restricted": true
  }
}
```

| Field | Tipe | Nilai | Penggunaan mobile |
|---|---|---|---|
| `scope` | string | `all` \| `assigned` | Nilai setting sekolah untuk informasi/debug |
| `restricted` | boolean | `true` \| `false` | Sumber keputusan UI form |

`restricted` hanya `true` untuk user dengan role tunggal Teacher pada sekolah yang
memakai scope `assigned`. Guru dengan role tambahan mendapat `false`; user tanpa data
guru tetap tidak dapat membuat tugas melalui endpoint teacher.

## 3. Endpoint Sumber Pilihan

Semua URL di bagian ini memakai `{EXAM_HOST}`.

### Daftar kelas

```http
GET /api/mobile/app/learning/assignment/teachers/class
```

- `restricted=false`: seluruh kelas sekolah selain kelas ALUMNI.
- `restricted=true`: kelas dari jadwal guru ditambah kelas tempat guru menjadi wali.

Response:

```json
{
  "data": [
    { "id": 21, "name": "VIII A", "grade": 8 }
  ]
}
```

### Daftar mapel bebas

```http
GET /api/mobile/app/learning/assignment/teachers/subject
```

- Dipakai langsung ketika `restricted=false`; pilihan mapel tidak bergantung pada hari
  atau jadwal.
- Ketika dipanggil pada mode assigned, server hanya mengembalikan mapel yang diampu guru.

Response:

```json
{
  "data": [
    {
      "id": 18,
      "name": "Matematika",
      "icon_image": "https://assets.example/mapel.png"
    }
  ]
}
```

Query opsional: `take` dan `skip`.

### Hari dan jadwal untuk mode assigned

```http
GET /api/mobile/app/learning/assignment/teachers/schedule-day
GET /api/mobile/app/learning/assignment/teachers/schedule?school_class_id=21&day=SENIN
```

Pada `restricted=true`, endpoint `schedule` hanya mengembalikan jadwal milik guru login.
Item jadwal berisi `id`, `day`, `class_room`, `time_plot`, `subject`, dan `teacher`.

## 4. Create dan Update Tugas

```http
POST /api/mobile/app/learning/assignment/teachers/create
POST /api/mobile/app/learning/assignment/teachers/update/{assignmentId}
Content-Type: multipart/form-data
```

### Field umum

| Field | Tipe | Wajib create | Keterangan |
|---|---|---:|---|
| `title` | string | ya | Judul tugas |
| `description` | string | ya di UI | Panduan tugas; mobile mengikuti validasi CMS |
| `end_at` | datetime | ya di UI | Format `YYYY-MM-DD HH:mm:ss` |
| `file` | file | tidak | Maksimal 12 MB |
| `link` / `link[]` | string/array | tidak | Tautan pendukung |
| `checked` | boolean/int | tidak | Flag tugas dibaca/dicentang |
| `uploaded` | boolean/int | tidak | Flag siswa wajib mengunggah |
| `downloaded` | boolean/int | tidak | Alias API untuk kolom legacy `downloded` |

### Payload mode bebas (`restricted=false`)

| Field | Tipe | Wajib | Keterangan |
|---|---|---:|---|
| `school_class_id` | integer | ya | ID dari endpoint `class` |
| `school_subject_id` | integer | ya | ID dari endpoint `subject` |

Jangan mengirim `school_subject_schedule_id`. Server menyimpan relasi jadwal sebagai
`null` dan menentukan `grade` dari kelas.

```bash
curl -X POST "{EXAM_HOST}/api/mobile/app/learning/assignment/teachers/create" \
  -H "Authorization: Bearer $TOKEN" -H "Accept: application/json" \
  -F "title=Latihan Pecahan" \
  -F "description=Kerjakan nomor 1-10" \
  -F "school_class_id=21" \
  -F "school_subject_id=18" \
  -F "end_at=2026-10-30 15:00:00"
```

### Payload mode assigned (`restricted=true`)

| Field | Tipe | Wajib | Keterangan |
|---|---|---:|---|
| `school_subject_schedule_id` | integer | ya | ID dari endpoint `schedule` |

Server menentukan kelas, mapel, dan grade dari jadwal. Nilai `school_class_id`,
`school_subject_id`, atau `grade` yang ikut dikirim tidak menjadi sumber kebenaran.

```bash
curl -X POST "{EXAM_HOST}/api/mobile/app/learning/assignment/teachers/create" \
  -H "Authorization: Bearer $TOKEN" -H "Accept: application/json" \
  -F "title=Latihan Pecahan" \
  -F "description=Kerjakan nomor 1-10" \
  -F "school_subject_schedule_id=44" \
  -F "end_at=2026-10-30 15:00:00"
```

Create dan update yang berhasil mempertahankan response legacy:

```json
{ "message": "Berhasil membuat Tugas" }
```

```json
{ "message": "Berhasil update Tugas" }
```

## 5. Kompatibilitas Payload Lama

Backend menerima nama canonical dan alias lama berikut:

| Canonical | Alias lama |
|---|---|
| `school_class_id` | `school_classes_id` |
| `school_subject_schedule_id` | `school_subject_schedules_id` |

- Mobile baru harus memakai nama canonical.
- Jika canonical dan alias dikirim bersamaan dengan nilai sama, request diterima.
- Jika nilainya berbeda, server mengembalikan `422` pada key canonical.
- Payload assigned lama yang mengirim kelas/mapel/grade tetap diterima, tetapi target
  selalu diturunkan ulang dari jadwal oleh server.

## 6. Error yang Harus Ditangani

| Status | Kondisi | Perilaku mobile |
|---|---|---|
| `401` | Token tidak valid | Arahkan ke login/session refresh |
| `404` | Assignment update bukan milik guru/sekolah atau tidak ditemukan | Tutup form dan refresh daftar |
| `422` | Target tidak lengkap/tidak valid, jadwal guru lain, atau alias konflik | Tampilkan pesan field dari `errors` |
| `400` | Gagal upload/menyimpan atau akun tidak memiliki data guru | Tampilkan `message`/`error` umum |

Contoh `422`:

```json
{
  "message": "The given data was invalid.",
  "errors": {
    "school_subject_schedule_id": [
      "Jadwal mata pelajaran bukan milik guru yang sedang masuk."
    ]
  }
}
```

## 7. State Machine untuk Implementasi Agent Mobile

```text
open form
  -> GET teacher-data-scope
     -> gagal: restricted = true (fallback)
  -> GET assignment/teachers/class
  -> if restricted == false
       -> GET assignment/teachers/subject
       -> tampilkan kelas + mapel tanpa input hari
       -> submit school_class_id + school_subject_id
     else
       -> tampilkan kelas + hari
       -> GET schedule berdasarkan kelas + hari
       -> tampilkan schedule.subject milik guru
       -> submit school_subject_schedule_id
  -> HTTP 200: refresh daftar dan tutup form
  -> HTTP 422: petakan errors ke field canonical dan pertahankan draft
```

Untuk edit, isi nilai awal dari detail tugas. Tugas tanpa schedule masuk alur bebas;
tugas dengan schedule masuk alur assigned. Setelah scope selesai dimuat, aturan scope
aktif tetap menjadi sumber kebenaran saat submit.
