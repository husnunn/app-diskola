# API Contract — Pembelajaran, Tugas & Feed Sekolah

Sumber: `app/src/main/java/id/diskola/app/api/ApiService.kt` baris 272–542.
Model response dicari di `app/src/main/java/id/diskola/app/pages/**/*Models.kt`.

Catatan umum:
- Semua endpoint memakai `suspend fun`, tidak ada yang di-comment pada rentang ini.
- Untuk endpoint dengan `@Body data: Any` / `@PartMap data: Map<String, RequestBody>`, kode lama tidak mendefinisikan model request. Field yang dicantumkan diambil dari lokasi pemanggilan nyata di `ApiWrapper.kt` / ViewModel terkait (bukan tebakan murni), namun tetap ditandai ⚠️ karena tidak ada kontrak tipe resmi dari backend.

---

## Feed Sosial Sekolah
(fitur legacy, sudah tidak aktif di UI produksi, tetap didata untuk kelengkapan kontrak API)

### `GET mobile/app/schools/feeds/{feedId}/like` — feedLike
**Parameter:** `feedId: Int` (path), `take: Int` (query), `skip: Int` (query)
**Response:** `FeedLikeResponse`
```json
{
  "data": [
    {
      "id": 501,
      "created_at_label": "2 jam lalu",
      "user": {
        "id": 10,
        "uuid": "b7a1c2e0-1111-4a2b-9c3d-000000000010",
        "name": "Budi Santoso",
        "email": "budi.santoso@murid.diskola.sch.id",
        "user_username": "budi.santoso",
        "nisn_nik": "0031234567",
        "nis_nik": "2025010012",
        "phone": "081234567890",
        "user_avatar_image": "avatars/budi-santoso.jpg",
        "is_verified": true
      }
    }
  ]
}
```

### `POST mobile/app/schools/feed-posts/send-like` — likeFeed
**Parameter:** `Body: dinamis (tidak bertipe di kode lama)` — `Any`
Body sesungguhnya dikirim dari `ApiWrapper.likeFeed()`:
```json
{
  "user_id": 10,
  "feed_id": 501
}
```
⚠️ perlu verifikasi manual (tidak ada model request resmi, field diambil dari kode caller).
**Response:** `Any`
```json
{
  "status": true,
  "message": "Berhasil menyukai postingan"
}
```

### `DELETE mobile/app/schools/feed-posts/{feedId}/unlike` — unlikeFeed
**Parameter:** `feedId: Int` (path)
**Response:** `Any`
```json
{
  "status": true,
  "message": "Berhasil batal menyukai postingan"
}
```

### `POST mobile/app/schools/feed-posts/{feedId}/comment` — commentFeed
**Parameter:** `feedId: Int` (path), `Body: dinamis (tidak bertipe di kode lama)` — `Any`
Body sesungguhnya dikirim dari `ApiWrapper.commentFeed()`:
```json
{
  "feed_comments_body": "Keren sekali kegiatannya!"
}
```
⚠️ perlu verifikasi manual.
**Response:** `Any`
```json
{
  "status": true,
  "message": "Komentar berhasil ditambahkan"
}
```

### `DELETE mobile/app/schools/feeds/{feedId}` — deleteFeed
**Parameter:** `feedId: Int` (path)
**Response:** `Any`
```json
{
  "status": true,
  "message": "Postingan berhasil dihapus"
}
```

### `GET mobile/app/schools/explore/feed` — exploreFeed
**Parameter:** `take: Int` (query), `skip: Int` (query), `params: String = ""` (query, format filter tidak terdokumentasi di kode)
**Response:** `FeedResponse`
```json
{
  "data": [
    {
      "id": 55,
      "row_id": 55,
      "feed_type": "image",
      "created_at": "2025-01-10T08:00:00Z",
      "created_at_label": "10 Jan 2025",
      "feed_title": "Kegiatan Upacara Bendera",
      "feed_body": "Dokumentasi upacara bendera hari Senin di lapangan sekolah.",
      "feed_author": "Admin Sekolah",
      "feed_thumbnail_image": "feeds/thumbnail-upacara.jpg",
      "users": {
        "id": 2,
        "uuid": "a1b2c3d4-2222-4a2b-9c3d-000000000002",
        "name": "Admin Sekolah",
        "email": "admin@diskola.sch.id",
        "user_username": "admin.sekolah",
        "nisn_nik": "",
        "nis_nik": "",
        "phone": "021123456",
        "user_avatar_image": "avatars/admin.jpg",
        "is_verified": true
      },
      "file": {
        "feed_files_name": "upacara.jpg",
        "feed_files_path": "feeds/files/upacara.jpg",
        "feed_files_size": "2048",
        "feed_files_type": "image/jpeg",
        "feed_files_format": "jpg",
        "feed_files_width": "1080",
        "feed_files_height": "1350",
        "feed_files_id": 900
      },
      "count_comments": 3,
      "count_likes": 12,
      "likes": [],
      "comments": [],
      "is_likes": false
    }
  ]
}
```

### `GET mobile/app/schools/explore/user` — exploreUser
**Parameter:** `take: Int` (query), `skip: Int` (query), `params: String = ""` (query)
**Response:** `FeedUserResponse`
```json
{
  "data": [
    {
      "id": 10,
      "uuid": "b7a1c2e0-1111-4a2b-9c3d-000000000010",
      "name": "Budi Santoso",
      "email": "budi.santoso@murid.diskola.sch.id",
      "user_username": "budi.santoso",
      "nisn_nik": "0031234567",
      "nis_nik": "2025010012",
      "phone": "081234567890",
      "user_avatar_image": "avatars/budi-santoso.jpg",
      "is_verified": true
    }
  ]
}
```

### `GET mobile/app/schools/explore/hastag` — exploreHashtag
**Parameter:** `take: Int` (query), `skip: Int` (query), `params: String = ""` (query)
**Response:** `HashtagResponse`
```json
{
  "data": [
    { "name": "#osis", "total": 25 },
    { "name": "#upacara", "total": 14 }
  ]
}
```

---

## Pengumuman (Announcement)

### `GET mobile/app/learning/announcements` — announcement
**Parameter:** `take: Int` (query), `skip: Int` (query)
**Response:** `AnnouncementResponse`
```json
{
  "data": [
    {
      "id": 1,
      "title": "Libur Semester Ganjil",
      "body": "Libur semester ganjil dimulai tanggal 20 Desember 2025 sampai 5 Januari 2026.",
      "image": "announcements/libur-semester.jpg",
      "created": "2025-12-01T09:00:00Z",
      "created_at_label": "1 Des 2025"
    }
  ]
}
```

---

## Materi Pembelajaran (Theories)

### `GET mobile/app/learning/theories/students/subjects` — studentSubject
**Parameter:** `take: Int` (query), `skip: Int` (query)
**Response:** `MapelResponse`
```json
{
  "data": [
    {
      "id": 1,
      "name": "Matematika",
      "icon_image": "icons/matematika.png",
      "message_label": "",
      "teacher": {
        "id": 5,
        "name": "Siti Aminah",
        "nip": "198501012010012001",
        "nik": "3201010101850001",
        "address": "Jl. Merdeka No. 1, Jakarta",
        "user": {
          "id": 20,
          "uuid": "c1d2e3f4-3333-4a2b-9c3d-000000000020",
          "name": "Siti Aminah",
          "email": "siti.aminah@guru.diskola.sch.id",
          "user_username": "siti.aminah",
          "nisn_nik": "",
          "nis_nik": "",
          "phone": "081298765432",
          "user_avatar_image": "avatars/siti-aminah.jpg",
          "is_verified": true
        }
      }
    }
  ]
}
```

### `GET mobile/app/learning/theories/students/subjects/{subjectId}/theories` — studentTheoryBySubject
**Parameter:** `subjectId: Int` (path), `take: Int` (query), `skip: Int` (query)
**Response:** `MateriResponse`
```json
{
  "data": [
    {
      "id": 10,
      "name": "Aljabar Linear",
      "description": "Materi pengantar tentang persamaan dan sistem persamaan linear.",
      "message_label": "",
      "uri": { "link": ["https://youtube.com/watch?v=aljabar-linear"] },
      "file_path": "materi/aljabar-linear.pdf",
      "file_name": "aljabar-linear.pdf",
      "file_format": "pdf",
      "file_type": "application/pdf",
      "file_size": "2048",
      "created_at_label": "5 Jan 2025",
      "subject": {
        "id": 1,
        "name": "Matematika",
        "icon_image": "icons/matematika.png",
        "message_label": "",
        "teacher": { "id": 5, "name": "Siti Aminah", "nip": "198501012010012001", "nik": "3201010101850001", "address": "Jl. Merdeka No. 1, Jakarta", "user": { "id": 20, "uuid": "c1d2e3f4-3333-4a2b-9c3d-000000000020", "name": "Siti Aminah", "email": "siti.aminah@guru.diskola.sch.id", "user_username": "siti.aminah", "nisn_nik": "", "nis_nik": "", "phone": "081298765432", "user_avatar_image": "avatars/siti-aminah.jpg", "is_verified": true } }
      },
      "teacher": { "id": 5, "name": "Siti Aminah", "nip": "198501012010012001", "nik": "3201010101850001", "address": "Jl. Merdeka No. 1, Jakarta", "user": { "id": 20, "uuid": "c1d2e3f4-3333-4a2b-9c3d-000000000020", "name": "Siti Aminah", "email": "siti.aminah@guru.diskola.sch.id", "user_username": "siti.aminah", "nisn_nik": "", "nis_nik": "", "phone": "081298765432", "user_avatar_image": "avatars/siti-aminah.jpg", "is_verified": true } },
      "school": { "row_id": 1, "uuid": "sch-uuid-0001", "id": "1", "name": "SMA Negeri 1 Diskola", "image": "schools/logo.png" },
      "grade": 10,
      "school_class": null,
      "school_major": null,
      "file_edited": ""
    }
  ]
}
```
Catatan: `MateriItem.school_class` dan `school_major` bertipe `Any?` di kode lama (kadang berupa objek `{ "id": ... }`, kadang `null`) — ⚠️ perlu verifikasi manual skema aslinya ke backend.

### `GET mobile/app/learning/theories/students/subjects/{subjectId}/theories/{theoryId}` — studentTheoryDetail
**Parameter:** `subjectId: Int` (path), `theoryId: Int` (path)
**Response:** `DetailMateriResponse`
```json
{
  "data": {
    "id": 10,
    "name": "Aljabar Linear",
    "description": "Materi pengantar tentang persamaan dan sistem persamaan linear.",
    "message_label": "",
    "uri": { "link": ["https://youtube.com/watch?v=aljabar-linear"] },
    "file_path": "materi/aljabar-linear.pdf",
    "file_name": "aljabar-linear.pdf",
    "file_format": "pdf",
    "file_type": "application/pdf",
    "file_size": "2048",
    "created_at_label": "5 Jan 2025",
    "subject": { "id": 1, "name": "Matematika", "icon_image": "icons/matematika.png", "message_label": "", "teacher": { "id": 5, "name": "Siti Aminah", "nip": "198501012010012001", "nik": "3201010101850001", "address": "Jl. Merdeka No. 1, Jakarta", "user": { "id": 20, "uuid": "c1d2e3f4-3333-4a2b-9c3d-000000000020", "name": "Siti Aminah", "email": "siti.aminah@guru.diskola.sch.id", "user_username": "siti.aminah", "nisn_nik": "", "nis_nik": "", "phone": "081298765432", "user_avatar_image": "avatars/siti-aminah.jpg", "is_verified": true } } },
    "teacher": { "id": 5, "name": "Siti Aminah", "nip": "198501012010012001", "nik": "3201010101850001", "address": "Jl. Merdeka No. 1, Jakarta", "user": { "id": 20, "uuid": "c1d2e3f4-3333-4a2b-9c3d-000000000020", "name": "Siti Aminah", "email": "siti.aminah@guru.diskola.sch.id", "user_username": "siti.aminah", "nisn_nik": "", "nis_nik": "", "phone": "081298765432", "user_avatar_image": "avatars/siti-aminah.jpg", "is_verified": true } },
    "school": { "row_id": 1, "uuid": "sch-uuid-0001", "id": "1", "name": "SMA Negeri 1 Diskola", "image": "schools/logo.png" },
    "grade": 10,
    "school_class": null,
    "school_major": null,
    "file_edited": ""
  }
}
```

### `GET mobile/teacher/school-class-room` — teacherClassRoom
**Parameter:** tidak ada
**Response:** `ClassRoomResponse`
```json
{
  "data": [
    { "id": 1, "name": "X IPA 1", "grade": 10, "majorId": 2 }
  ]
}
```

### `GET mobile/app/learning/theories/teachers/subjects` — teacherSubject
**Parameter:** `take: Int` (query), `skip: Int` (query)
**Response:** `MapelResponse` (struktur sama dengan `studentSubject` di atas)

### `GET mobile/app/learning/theories/teachers/subjects` — teacherSubjectTeach
**Parameter:** tidak ada
**Response:** `MapelResponse`
Catatan: path sama persis dengan `teacherSubject` di atas, hanya beda nama fungsi Kotlin dan tanpa parameter paging (kemungkinan sisa kode lama/duplikat) — ⚠️ perlu verifikasi mana yang benar-benar dipakai di backend.

### `GET mobile/teacher/school-subject` — teacherSubjectTeach2
**Parameter:** tidak ada
**Response:** `MapelResponse`

### `GET mobile/app/learning/theories/teachers/school-majors` — teacherMajor
**Parameter:** `take: Int = 1000` (query)
**Response:** `MajorResponse`
```json
{
  "data": [
    { "id": 1, "name": "IPA" },
    { "id": 2, "name": "IPS" }
  ]
}
```

### `GET mobile/app/learning/theories/teachers/theories` — teacherTheory
**Parameter:** `take: Int` (query), `skip: Int` (query), `school_subject: Int?` (query, nullable), `school_class: Int?` (query, nullable)
**Response:** `MateriResponse` (struktur sama dengan `studentTheoryBySubject`)

### `GET mobile/app/learning/theories/teachers/subjects/{subjectId}/theories` — teacherTheoryBySubject
**Parameter:** `subjectId: Int` (path), `take: Int` (query), `skip: Int` (query)
**Response:** `MateriResponse`

### `GET mobile/app/learning/theories/teachers/subjects/{subjectId}/theories/{theoryId}` — teacherTheoryDetail
**Parameter:** `subjectId: Int` (path), `theoryId: Int` (path)
**Response:** `DetailMateriResponse`

### `POST mobile/app/learning/theories/teachers/theories` (multipart) — createTheory
**Parameter:** `data: Map<String, RequestBody>` (PartMap, dinamis), `file: MultipartBody.Part?` (part, nullable)
Field `data` sesungguhnya dikirim dari `UploadMateriViewmodel.createMateri()`:
```
name: string
description: string
link: string
school_subject_id: string (angka sebagai string)
grade: string (opsional, hanya jika target = "grade")
school_classes_id: string (opsional, hanya jika target = "class")
school_major_id: string (opsional, hanya jika target = "major")
```
⚠️ perlu verifikasi manual — semua value dikirim sebagai text/plain `RequestBody`, tidak ada model request resmi.
**Response:** `Any`
```json
{
  "status": true,
  "message": "Materi berhasil ditambahkan"
}
```

### `POST mobile/app/learning/theories/teachers/theories/update/{id}` (multipart) — updateTheory
**Parameter:** `id: Int` (path), `data: Map<String, RequestBody>` (PartMap, dinamis — field sama dengan `createTheory`), `file: MultipartBody.Part?` (part, nullable)
**Response:** `Any`
```json
{
  "status": true,
  "message": "Materi berhasil diperbarui"
}
```

### `DELETE mobile/app/learning/theories/teachers/theories/{id}` — deleteTheory
**Parameter:** `id: Long` (path)
**Response:** `Any`
```json
{
  "status": true,
  "message": "Materi berhasil dihapus"
}
```

### `GET mobile/teacher/school-grade` — gradeTeach
**Parameter:** tidak ada
**Response:** `GradeResponse`
```json
{
  "data": [
    { "id": 10, "name": "Kelas X" },
    { "id": 11, "name": "Kelas XI" }
  ]
}
```

---

## Tugas (Assignment / Homework)

### `GET mobile/app/learning/assignment/students/backlog` — studentTaskTodo
**Parameter:** `take: Int` (query), `skip: Int` (query)
**Response:** `HomeworkResponse`
```json
{
  "data": [
    {
      "id": 200,
      "title": "Tugas Aljabar Linear Bab 1",
      "description": "Kerjakan soal halaman 20-25.",
      "is_overdue": false,
      "checked": 1,
      "downloded": 1,
      "uploaded": 1,
      "information_label": "Belum dikumpulkan",
      "file_name": "tugas-aljabar.pdf",
      "file_path": "assignments/tugas-aljabar.pdf",
      "file_format": "pdf",
      "file_type": "application/pdf",
      "file_size": "1024",
      "message_label": "",
      "end_at_label": "20 Jan 2025, 23:59",
      "upload_at_label": "",
      "file_student_name": "",
      "file_student_path": "",
      "file_student_format": "",
      "file_student_type": "",
      "file_student_size": "",
      "files": [],
      "score": 0,
      "school": { "row_id": 1, "uuid": "sch-uuid-0001", "id": "1", "name": "SMA Negeri 1 Diskola", "image": "schools/logo.png" },
      "subject": { "id": 1, "name": "Matematika", "icon_image": "icons/matematika.png", "message_label": "", "teacher": { "id": 5, "name": "Siti Aminah", "nip": "198501012010012001", "nik": "3201010101850001", "address": "Jl. Merdeka No. 1, Jakarta", "user": { "id": 20, "uuid": "c1d2e3f4-3333-4a2b-9c3d-000000000020", "name": "Siti Aminah", "email": "siti.aminah@guru.diskola.sch.id", "user_username": "siti.aminah", "nisn_nik": "", "nis_nik": "", "phone": "081298765432", "user_avatar_image": "avatars/siti-aminah.jpg", "is_verified": true } } },
      "teacher": { "id": 5, "name": "Siti Aminah", "nip": "198501012010012001", "nik": "3201010101850001", "address": "Jl. Merdeka No. 1, Jakarta", "user": { "id": 20, "uuid": "c1d2e3f4-3333-4a2b-9c3d-000000000020", "name": "Siti Aminah", "email": "siti.aminah@guru.diskola.sch.id", "user_username": "siti.aminah", "nisn_nik": "", "nis_nik": "", "phone": "081298765432", "user_avatar_image": "avatars/siti-aminah.jpg", "is_verified": true } },
      "class": { "id": 1, "name": "X IPA 1", "grade": 10, "majorId": 2 },
      "uri": null,
      "uri_student": null,
      "schedule": {
        "id": 1,
        "day": "senin",
        "class_room": { "id": 1, "name": "X IPA 1", "grade": 10, "majorId": 2 },
        "time_plot": { "id": 1, "day": "senin", "start_at": "07:00", "end_at": "08:30" },
        "subject": { "id": 1, "name": "Matematika", "icon_image": "icons/matematika.png", "message_label": "", "teacher": { "id": 5, "name": "Siti Aminah", "nip": "198501012010012001", "nik": "3201010101850001", "address": "Jl. Merdeka No. 1, Jakarta", "user": { "id": 20, "uuid": "c1d2e3f4-3333-4a2b-9c3d-000000000020", "name": "Siti Aminah", "email": "siti.aminah@guru.diskola.sch.id", "user_username": "siti.aminah", "nisn_nik": "", "nis_nik": "", "phone": "081298765432", "user_avatar_image": "avatars/siti-aminah.jpg", "is_verified": true } } }
      },
      "file_edited": ""
    }
  ]
}
```
Catatan: field JSON `class` di-map ke properti Kotlin `classRoom` lewat `@Json(name = "class")`.

### `GET mobile/app/learning/assignment/teachers/backlog` — teacherTaskTodo
**Parameter:** `take: Int` (query), `skip: Int` (query)
**Response:** `HomeworkResponse` (struktur sama dengan `studentTaskTodo`)

### `GET mobile/app/learning/assignment/students/done` — studentTaskDone
**Parameter:** `take: Int` (query), `skip: Int` (query)
**Response:** `HomeworkResponse`

### `GET mobile/app/learning/assignment/students/scored` — studentTaskScored
**Parameter:** `take: Int` (query), `skip: Int` (query)
**Response:** `HomeworkResponse` (item biasanya berisi `score` > 0 dan `upload_at_label` terisi)

### `POST mobile/app/learning/assignment/students/{id}/collect` (multipart) — answerHomework
**Parameter:** `id: Int` (path), `data: Map<String, RequestBody>` (PartMap, dinamis), `files: List<MultipartBody.Part>?` (part list, nullable — nama field form `file[]`)
Field `data` sesungguhnya dari `HomeWorkViewModel.uploadHomework()`:
```
checked: string ("1")
uploaded: string ("1" atau "0")
link: string (link tugas opsional)
```
⚠️ perlu verifikasi manual.
**Response:** `CollectHomeworkResponse`
```json
{
  "data": {
    "id": 300,
    "upload_at": "2025-01-15T10:00:00Z",
    "score": null,
    "scored": 0,
    "scored_at": "",
    "checked": 1,
    "uploaded": 1,
    "uri_student": null,
    "file_name": "jawaban-budi.pdf",
    "file_path": "assignments/answers/jawaban-budi.pdf",
    "file_format": "pdf",
    "file_type": "application/pdf",
    "file_size": "512",
    "files": [
      {
        "id": 900,
        "file_name": "jawaban-budi.pdf",
        "file_path": "assignments/answers/jawaban-budi.pdf",
        "file_type": "application/pdf",
        "file_format": "pdf",
        "file_size": "512",
        "student_assignment_id": 300,
        "student_assignment_files_name": "",
        "student_assignment_files_path": "",
        "student_assignment_files_type": "",
        "student_assignment_files_format": "",
        "student_assignment_files_size": ""
      }
    ]
  }
}
```

### `GET mobile/app/learning/assignment/students/{subjectAssignmentId}/collect/{studentAssignmentId}` — studentAssignmentCollectDetail
**Parameter:** `subjectAssignmentId: Int` (path), `studentAssignmentId: Int` (path)
**Response:** `StudentCollectDetailResponse`
```json
{
  "data": {
    "id": 200,
    "title": "Tugas Aljabar Linear Bab 1",
    "description": "Kerjakan soal halaman 20-25.",
    "is_overdue": false,
    "checked": 1,
    "downloded": 1,
    "uploaded": 1,
    "information_label": "Sudah dikumpulkan",
    "file_name": "tugas-aljabar.pdf",
    "file_path": "assignments/tugas-aljabar.pdf",
    "file_format": "pdf",
    "file_type": "application/pdf",
    "file_size": "1024",
    "message_label": "",
    "end_at_label": "20 Jan 2025, 23:59",
    "upload_at_label": "15 Jan 2025, 10:00",
    "file_student_name": "jawaban-budi.pdf",
    "file_student_path": "assignments/answers/jawaban-budi.pdf",
    "file_student_format": "pdf",
    "file_student_type": "application/pdf",
    "file_student_size": "512",
    "files": [],
    "score": 0,
    "school": { "row_id": 1, "uuid": "sch-uuid-0001", "id": "1", "name": "SMA Negeri 1 Diskola", "image": "schools/logo.png" },
    "subject": { "id": 1, "name": "Matematika", "icon_image": "icons/matematika.png", "message_label": "", "teacher": { "id": 5, "name": "Siti Aminah", "nip": "198501012010012001", "nik": "3201010101850001", "address": "Jl. Merdeka No. 1, Jakarta", "user": { "id": 20, "uuid": "c1d2e3f4-3333-4a2b-9c3d-000000000020", "name": "Siti Aminah", "email": "siti.aminah@guru.diskola.sch.id", "user_username": "siti.aminah", "nisn_nik": "", "nis_nik": "", "phone": "081298765432", "user_avatar_image": "avatars/siti-aminah.jpg", "is_verified": true } } },
    "teacher": { "id": 5, "name": "Siti Aminah", "nip": "198501012010012001", "nik": "3201010101850001", "address": "Jl. Merdeka No. 1, Jakarta", "user": { "id": 20, "uuid": "c1d2e3f4-3333-4a2b-9c3d-000000000020", "name": "Siti Aminah", "email": "siti.aminah@guru.diskola.sch.id", "user_username": "siti.aminah", "nisn_nik": "", "nis_nik": "", "phone": "081298765432", "user_avatar_image": "avatars/siti-aminah.jpg", "is_verified": true } },
    "class": { "id": 1, "name": "X IPA 1", "grade": 10, "majorId": 2 },
    "uri": null,
    "uri_student": null,
    "schedule": { "id": 1, "day": "senin", "class_room": { "id": 1, "name": "X IPA 1", "grade": 10, "majorId": 2 }, "time_plot": { "id": 1, "day": "senin", "start_at": "07:00", "end_at": "08:30" }, "subject": { "id": 1, "name": "Matematika", "icon_image": "icons/matematika.png", "message_label": "", "teacher": { "id": 5, "name": "Siti Aminah", "nip": "198501012010012001", "nik": "3201010101850001", "address": "Jl. Merdeka No. 1, Jakarta", "user": { "id": 20, "uuid": "c1d2e3f4-3333-4a2b-9c3d-000000000020", "name": "Siti Aminah", "email": "siti.aminah@guru.diskola.sch.id", "user_username": "siti.aminah", "nisn_nik": "", "nis_nik": "", "phone": "081298765432", "user_avatar_image": "avatars/siti-aminah.jpg", "is_verified": true } } } },
    "file_edited": ""
  },
  "student_assignment": {
    "id": 300,
    "upload_at": "2025-01-15T10:00:00Z",
    "score": null,
    "scored": 0,
    "scored_at": "",
    "checked": 1,
    "uploaded": 1,
    "uri_student": null,
    "file_name": "jawaban-budi.pdf",
    "file_path": "assignments/answers/jawaban-budi.pdf",
    "file_format": "pdf",
    "file_type": "application/pdf",
    "file_size": "512",
    "files": []
  }
}
```

### `GET mobile/app/learning/assignment/teachers/class` — assignmentClass
**Parameter:** tidak ada
**Response:** `ClassRoomResponse` (struktur sama dengan `teacherClassRoom`)

### `GET mobile/app/learning/assignment/teachers/schedule-day` — assignmentDay
**Parameter:** tidak ada
**Response:** `AssignmentDayResp`
```json
{
  "data": [
    { "key": "senin", "lable": "Senin" },
    { "key": "selasa", "lable": "Selasa" }
  ]
}
```

### `GET mobile/app/learning/assignment/teachers/schedule` — assignmentScheduleClass
**Parameter:** `classId: Int` (query `school_class_id`), `day: String` (query `day`)
**Response:** `AssignmentScheduleResp`
```json
{
  "data": [
    {
      "id": 1,
      "day": "senin",
      "class_room": { "id": 1, "name": "X IPA 1", "grade": 10, "majorId": 2 },
      "time_plot": { "id": 1, "day": "senin", "start_at": "07:00", "end_at": "08:30" },
      "subject": { "id": 1, "name": "Matematika", "icon_image": "icons/matematika.png", "message_label": "", "teacher": { "id": 5, "name": "Siti Aminah", "nip": "198501012010012001", "nik": "3201010101850001", "address": "Jl. Merdeka No. 1, Jakarta", "user": { "id": 20, "uuid": "c1d2e3f4-3333-4a2b-9c3d-000000000020", "name": "Siti Aminah", "email": "siti.aminah@guru.diskola.sch.id", "user_username": "siti.aminah", "nisn_nik": "", "nis_nik": "", "phone": "081298765432", "user_avatar_image": "avatars/siti-aminah.jpg", "is_verified": true } } }
    }
  ]
}
```

### `POST mobile/app/learning/assignment/teachers/create` (multipart) — createAssignment
**Parameter:** `data: Map<String, RequestBody>` (PartMap, dinamis), `file: MultipartBody.Part?` (part, nullable)
Field `data` sesungguhnya dari `CreateHomeworkVm.create()`:
```
title: string
description: string
teacher_id: string (angka sebagai string)
school_id: string (angka sebagai string)
school_subject_id: string
school_classes_id: string
school_subject_schedules_id: string
grade: string
end_at: string (tanggal deadline)
checked: string ("1")
downloded: string ("1"/"0")
uploaded: string ("1"/"0")
link: string
```
⚠️ perlu verifikasi manual.
**Response:** `Any`
```json
{
  "status": true,
  "message": "Tugas berhasil dibuat"
}
```

### `POST mobile/app/learning/assignment/teachers/update/{id}` (multipart) — updateAssignment
**Parameter:** `id: Int` (path), `data: Map<String, RequestBody>` (PartMap, dinamis — field sama dengan `createAssignment`), `file: MultipartBody.Part?` (part, nullable)
**Response:** `Any`
```json
{
  "status": true,
  "message": "Tugas berhasil diperbarui"
}
```

### `DELETE mobile/app/learning/assignment/teachers/delete/{id}` — deleteAssignment
**Parameter:** `id: Int` (path)
**Response:** `Any`
```json
{
  "status": true,
  "message": "Tugas berhasil dihapus"
}
```

### `GET mobile/app/learning/assignment/teachers/backlog` — teacherAssignment
**Parameter:** `params: Map<String, Any?>?` (QueryMap, dinamis — dari kode yang di-comment di `ApiWrapper.kt` terlihat pola filter seperti `filter[0][0]=teacher_id`, `filter[0][1]==`, `filter[0][2]=<id>`, dst.) ⚠️ perlu verifikasi manual
**Response:** `HomeworkResponse` (struktur sama dengan `studentTaskTodo`; path sama dengan `teacherTaskTodo`, hanya beda cara passing parameter — kemungkinan duplikat/varian lama)

### `GET mobile/app/learning/assignment/teachers/scored` — assignmentCollected
**Parameter:** `take: Int` (query), `skip: Int` (query)
**Response:** `HomeworkCollectedResponse`
```json
{
  "data": [
    {
      "id": 200,
      "title": "Tugas Aljabar Linear Bab 1",
      "description": "Kerjakan soal halaman 20-25.",
      "upload_at_label": "15 Jan 2025",
      "end_at_label": "20 Jan 2025, 23:59",
      "count_assignment_collected_all": "30",
      "count_assignment_collected_scored": "10",
      "count_assignment_collected": "20",
      "message_label": ""
    }
  ]
}
```

### `POST mobile/app/learning/assignment/teachers/scored/{colledtedId}/student-assignment/{assignmentId}` — scoreAssignment
**Parameter:** `colledtedId: Int` (path), `assignmentId: Int` (path), `Body: dinamis (tidak bertipe di kode lama)` — `Any`
Body sesungguhnya dari `ApiWrapper.scoreAssignment()`:
```json
{
  "score": 85
}
```
⚠️ perlu verifikasi manual.
**Response:** `Any`
```json
{
  "status": true,
  "message": "Nilai berhasil disimpan"
}
```

### `GET mobile/app/learning/assignment/teachers/scored/{id}` — assignmentDetail
**Parameter:** `id: Int` (path)
**Response:** `AssignmentResponse`
```json
{
  "data": {
    "id": 200,
    "upload_at_label": "15 Jan 2025",
    "end_at_label": "20 Jan 2025, 23:59",
    "count_assignment_collected_all": 30,
    "count_assignment_collected_scored": 10,
    "count_assignment_collected": 20,
    "assignments": [
      {
        "id": 300,
        "upload_at": "2025-01-15T10:00:00Z",
        "score": 85,
        "scored": 1,
        "uploaded": 1,
        "scored_at": "2025-01-16T09:00:00Z",
        "file_name": "jawaban-budi.pdf",
        "file_path": "assignments/answers/jawaban-budi.pdf",
        "file_format": "pdf",
        "file_type": "application/pdf",
        "file_size": "512",
        "student": {
          "id": 40,
          "nisn": "0031234567",
          "nis": "2025010012",
          "name": "Budi Santoso"
        },
        "subject_assignment": { "id": 200, "title": "Tugas Aljabar Linear Bab 1" },
        "uri_student": null,
        "files": []
      }
    ],
    "subject": { "id": 1, "name": "Matematika", "icon_image": "icons/matematika.png", "message_label": "", "teacher": { "id": 5, "name": "Siti Aminah", "nip": "198501012010012001", "nik": "3201010101850001", "address": "Jl. Merdeka No. 1, Jakarta", "user": { "id": 20, "uuid": "c1d2e3f4-3333-4a2b-9c3d-000000000020", "name": "Siti Aminah", "email": "siti.aminah@guru.diskola.sch.id", "user_username": "siti.aminah", "nisn_nik": "", "nis_nik": "", "phone": "081298765432", "user_avatar_image": "avatars/siti-aminah.jpg", "is_verified": true } } },
    "class": { "id": 1, "name": "X IPA 1", "grade": 10, "majorId": 2 }
  }
}
```
Catatan: field `student` (`Assignment.student`) memakai model `StudentItem` dari `pages/login/LoginModels.kt` yang punya jauh lebih banyak properti profil siswa (alamat, tinggi/berat badan, dll) — hanya sebagian ditampilkan di contoh di atas.

---

## Presensi
(Baru dimulai di ujung rentang ini; kelanjutan penuh kemungkinan ada di rentang baris berikutnya)

### `GET mobile/attendance/schedule` — schedule
**Parameter:** `date: String` (query), `schoolClassId: Int?` (query `school_class_id`), `schoolSubjectId: Int?` (query `school_subject_id`), `limit: Int?` (query `limit`)
**Response:** `PresensiListResponse`
```json
{
  "data": [
    {
      "id": 1,
      "school_id": 1,
      "name_of_day": "Senin",
      "start_at": "07:00",
      "end_at": "15:00",
      "journals": 3,
      "school_attendances": [
        {
          "attendance_id": 10,
          "plot_start_at": "07:00",
          "plot_end_at": "08:30",
          "late_at": null,
          "end_at": "08:30",
          "teacher_name": "Siti Aminah",
          "teacher_nip": "198501012010012001",
          "school_name": "SMA Negeri 1 Diskola",
          "learning_objective": "Memahami konsep dasar aljabar linear",
          "status": "Terlaksana",
          "is_present": true,
          "school_major_name": "IPA",
          "subject_name": "Matematika",
          "subject_icon_image": "icons/matematika.png",
          "class_name": "X IPA 1",
          "created_at": "2025-01-13T07:00:00Z",
          "present": 28,
          "absent": 2,
          "timeLeft": ""
        }
      ]
    }
  ]
}
```

### `GET mobile/attendance/teacher-data-scope` — teacherDataScope
**Parameter:** tidak ada
**Response:** `TeacherDataScopeResponse`
```json
{
  "data": {
    "teacher_master_data_scope": "assigned",
    "scope": "assigned"
  }
}
```

### `GET mobile/attendance/data/{month}/{year}` — attendance
**Parameter:** `month: Int?` (path), `year: Int?` (path)
**Response:** `AbsensiResponse`
```json
{
  "data": [
    {
      "date": "2025-01-13",
      "month": 1,
      "year": 2025,
      "dateLabel": "13 Jan 2025",
      "attend_at": "06:55",
      "leave_at": "15:10",
      "is_holiday": false,
      "attend_is_late": false,
      "leave_is_early": false,
      "attend_is_offsite": false,
      "leave_is_offsite": false,
      "attend_status": "hadir",
      "leave_status": "pulang",
      "attend_note": "",
      "leave_note": "",
      "attend_address": "Jl. Merdeka No. 1, Jakarta",
      "leave_address": "Jl. Merdeka No. 1, Jakarta",
      "attend_photo_url": "attendance/photos/masuk-13jan.jpg",
      "leave_photo_url": "attendance/photos/pulang-13jan.jpg",
      "leave_request_status": null,
      "leave_request_type": null
    }
  ]
}
```

### `GET mobile/attendance/summary/{year}` — attendanceSummary
**Parameter:** `year: Int?` (path)
**Response:** `RekapAbsensiResponse`
```json
{
  "data": [
    {
      "date": "2025-01",
      "month": "Januari",
      "year": "2025",
      "ontime": 20,
      "late": 2,
      "izin": 1,
      "sakit": 0,
      "alpha": 0,
      "order": 1
    }
  ]
}
```

### `GET mobile/attendance/schedule/{id}` — attendanceSchedule
**Parameter:** `id: Int` (path)
**Response:** `ScheduleDetailResponse`
```json
{
  "data": {
    "attendance_id": 10,
    "school_subject_schedule_id": 1,
    "start_at": "07:00",
    "end_at": "08:30",
    "late_limit": 15,
    "teacher_name": "Siti Aminah",
    "teacher_nip": "198501012010012001",
    "school_name": "SMA Negeri 1 Diskola",
    "school_major_name": "IPA",
    "subject_name": "Matematika",
    "subject_icon_image": "icons/matematika.png",
    "class_name": "X IPA 1",
    "status": "Terlaksana",
    "grade": 10,
    "learning_objective": "Memahami konsep dasar aljabar linear",
    "time_plot": "07:00 - 08:30",
    "plot_start_at": "07:00",
    "plot_end_at": "08:30",
    "total_student": "30",
    "student_attendances": [
      {
        "student": { "id": 40, "name": "Budi Santoso", "nisn": "0031234567", "nis": "2025010012" },
        "date": "2025-01-13",
        "attend_at": "06:58",
        "leave_at": "",
        "status": "hadir",
        "status_source": "self",
        "is_late": false
      }
    ],
    "total_attended_student": 28,
    "status_breakdown": { "hadir": 28, "izin": 1, "sakit": 0, "alpha": 1 }
  }
}
```

### `GET mobile/attendance/schedule/{id}/list-student` — listAttendanceSchedule
**Parameter:** `id: Int` (path)
**Response:** `ScheduleListAttendance`
```json
{
  "data": [
    {
      "id": 401,
      "school_attendance_id": 10,
      "name": "Budi Santoso",
      "nisn": "0031234567",
      "attend_at": "06:58",
      "is_attend": true
    }
  ]
}
```

### `POST mobile/attendance/start` — startClass
**Parameter:** `Body: dinamis (tidak bertipe di kode lama)` — `Any`
Body sesungguhnya dari `ApiWrapper.startClass()`:
```json
{
  "school_subject_schedule_id": 1,
  "lat": -6.200000,
  "lng": 106.816666
}
```
Catatan: field `password` dan `late_limit` ada di kode namun sedang di-comment (tidak dikirim saat ini). ⚠️ perlu verifikasi manual ke backend.
**Response:** `Any`
```json
{
  "status": true,
  "message": "Kelas berhasil dimulai"
}
```

### `POST mobile/attendance/attend` — attendClass
**Parameter:** `Body: dinamis (tidak bertipe di kode lama)` — `Any`
Body sesungguhnya dari `ApiWrapper.attendClass()`:
```json
{
  "school_subject_schedule_id": 1,
  "lat": -6.200000,
  "lng": 106.816666
}
```
⚠️ perlu verifikasi manual.
**Response:** `Any`
```json
{
  "status": true,
  "message": "Berhasil absen hadir"
}
```

### `POST mobile/attendance/end` — endClass
**Parameter:** `Body: dinamis (tidak bertipe di kode lama)` — `Any`
Body sesungguhnya dari `ApiWrapper.endClass()`:
```json
{
  "school_subject_schedule_id": 1
}
```
⚠️ perlu verifikasi manual.
**Response:** `Any`
```json
{
  "status": true,
  "message": "Kelas berhasil diakhiri"
}
```

### `POST mobile/attendance/leave` — leaveClass
**Parameter:** `Body: dinamis (tidak bertipe di kode lama)` — `Any`
Body sesungguhnya dari `ApiWrapper.leaveClass()`:
```json
{
  "school_subject_schedule_id": 1
}
```
⚠️ perlu verifikasi manual.
**Response:** `Any`
```json
{
  "status": true,
  "message": "Berhasil absen pulang"
}
```

### `GET mobile/attendance/student/by-month` — studentMonthlyAttendance
**Parameter:** `year: Int?` (query), `month: Int?` (query)
**Response:** `AbsensiResponse` (struktur sama dengan `attendance`)

### `GET mobile/attendance/student/by-year` — studentAnnualAttendance
**Parameter:** `year: Int` (query)
**Response:** `RekapAbsensiResponse` (struktur sama dengan `attendanceSummary`)

### `GET mobile/attendance/staff/by-month` — teacherMonthlyAttendance
**Parameter:** `year: Int?` (query), `month: Int?` (query)
**Response:** `AbsensiResponse` (struktur sama dengan `attendance`, dipakai untuk data presensi staf/guru)
