# API Contract — Auth & Akun

Sumber: `app/src/main/java/id/diskola/app/api/ApiService.kt` baris 1–271 (interface `ApiService`).
Base URL tidak didefinisikan di file ini (lihat konfigurasi Retrofit/OkHttp di modul DI).

Catatan umum:
- Semua response yang mengikuti pola `data class XxxResponse(val data: ... )` dibungkus di top-level dengan key `"data"`. Tidak semua response memiliki field `"status"`/`"message"` di top-level — kode lama sering hanya bergantung pada HTTP status code + shape `data`, jadi field ekstra seperti `status`/`message` di contoh JSON di bawah ini adalah TEBAKAN dan perlu diverifikasi ke backend nyata jika dibutuhkan.
- Field bertipe `@NullToEmptyString` berarti backend boleh mengirim `null` tapi Moshi akan mengonversinya jadi string kosong di app lama — di model baru boleh dibuat `String?` atau `String` dengan default "".
- Endpoint dengan `@Body data: Any` / `Map<String, Any>` artinya app lama TIDAK punya request model bertipe; field yang dikirim tidak bisa dipastikan hanya dari `ApiService.kt`.

---

## Utility / Download Generik

### `GET {url}` — download
**Header:** `Accept: */*`
**Parameter:**
- `@Url url: String` — URL lengkap (bisa beda host dari base URL), dipakai untuk download file umum (misal file materi/soal).

**Response:** `okhttp3.ResponseBody` (raw bytes, bukan JSON). Tidak perlu data class.

---

### `GET {url}` — downloadString
**Parameter:**
- `@Url url: String`

**Response:** `Any` (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada — kemungkinan dipakai untuk mengambil konten teks/JSON generik dari URL dinamis.

---

## App Config & Feature Flags

### `GET mobile/app/config/check-android-version` — checkVersion
**Parameter:** (tidak ada)
**Response:** `CheckVersionResponse`
```json
{
  "data": {
    "setting_globals_lable": "Android Version Apps",
    "setting_globals_name": "android-version",
    "setting_globals_value": "2.1.37"
  }
}
```

---

### `GET mobile/app/check-feature-availability` — checkFeatureAvailability
**Parameter:**
- `@Query name: String` — nama fitur, misal `"jurnal-kbm"` atau `"presensi"` (lihat `FeatureKey`).

**Response:** `FeatureAvailabilityResponse`
```json
{
  "data": {
    "name": "jurnal-kbm",
    "available": true,
    "message": null
  }
}
```
Jika `available: false`, `message` biasanya berisi pesan seperti "Fitur ini sedang diperbarui. Silakan gunakan website terlebih dahulu."

---

## Sekolah (Pencarian Sekolah untuk Login/Registrasi)

### `GET mobile/app/authentication/schools` — listSekolah
**Parameter:**
- `@Query take: Int` — jumlah data per halaman.
- `@Query skip: Int` — offset pagination.
- `@Query name: String = ""` — kata kunci pencarian nama sekolah (opsional).

**Response:** `SekolahResponse`
```json
{
  "data": [
    {
      "id": 101,
      "uuid": "f3a1c2b0-9d4e-4a2a-8b7f-1234567890ab",
      "name": "SMA Negeri 1 Diskola",
      "image": "https://cdn.diskola.id/schools/sman1-diskola.png",
      "address": "Jl. Pendidikan No. 10, Bandung",
      "city_name": "Bandung",
      "coordinate_radius": "50",
      "coordinate_latitude": -6.914744,
      "coordinate_longitude": 107.60981
    }
  ]
}
```
Catatan: field JSON asli adalah `city_name` (dipetakan ke properti Kotlin `city` via `@Json(name = "city_name")`).

---

## Notifikasi

### `GET mobile/notification` — getNotifications
**Parameter:**
- `@Query wallet_id: String` — dipakai sebagai `userId` (nama query param tetap `wallet_id`).
- `@Query page: Int`
- `@Query limit: Int`

**Response:** `NotificationResponse`
```json
{
  "data": [
    {
      "id": 5501,
      "type": "announcement",
      "title": "Pengumuman Libur Sekolah",
      "message": "Sekolah libur pada tanggal 17 Agustus 2026 dalam rangka HUT RI.",
      "image": ["https://cdn.diskola.id/notif/hut-ri.png"],
      "page": {
        "parent": "announcement",
        "child": "detail",
        "child_id": "5501"
      },
      "is_read": false
    }
  ]
}
```

---

### `GET mobile/notification/{id}` — getNotificationDetail
**Parameter:**
- `@Path id: Int` — id notifikasi.

**Response:** `NotificationResponseData`
```json
{
  "data": {
    "id": 5501,
    "type": "announcement",
    "title": "Pengumuman Libur Sekolah",
    "message": "Sekolah libur pada tanggal 17 Agustus 2026 dalam rangka HUT RI.",
    "image": ["https://cdn.diskola.id/notif/hut-ri.png"],
    "page": {
      "parent": "announcement",
      "child": "detail",
      "child_id": "5501"
    },
    "is_read": true
  }
}
```

---

### `POST mobile/notification/{id}` — markNotificationAsRead
**Parameter:**
- `@Path id: Int` — id notifikasi.
- `@Body data: Any` — Body: dinamis (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada. Dari nama fungsi kemungkinan body kosong `{}` atau berisi `{"is_read": true}`.

**Response:** `NotificationResponse` (kemungkinan mengembalikan ulang daftar notifikasi terbaru — sama shape dengan `getNotifications`).

---

### `GET mobile/notification/summary` — getsummaryNotifications
**Parameter:**
- `@Query wallet_id: String` — userId.

**Response:** `summaryNotificationsResponse`
```json
{
  "data": {
    "count_unread": 3
  }
}
```

---

## Autentikasi

### `POST mobile/app/authentication/check-account` — checkAccount
**Parameter:**
- `@Body data: Any` — Body: dinamis (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada. Dari nama fungsi kemungkinan berisi identitas login seperti `{"username": "...", "school_id": 101}` untuk mengecek apakah akun terdaftar sebelum login.

**Response:** `UserResponse`
```json
{
  "data": {
    "id": 2001,
    "uuid": "8b1e0a90-5c3d-4f21-9a11-abcdef123456",
    "using_default_password": "Y",
    "name": "Ahmad Fauzan",
    "email": "ahmad.fauzan@diskola.id",
    "nisn_nik": "0051234567",
    "nis_nik": "20231234",
    "phone": "081234567890",
    "place_of_birth": "Bandung",
    "date_of_birth": "2008-05-14",
    "gender": "L",
    "user_avatar_image": "https://cdn.diskola.id/avatars/2001.png",
    "religion": "Islam",
    "blood_type": "O",
    "address": "Jl. Merdeka No. 5",
    "sub_district_id": "3273010",
    "sub_district": "Sukasari",
    "city_id": "3273",
    "city": "Bandung",
    "province_id": "32",
    "province": "Jawa Barat",
    "user_username": "ahmadfauzan",
    "student": null,
    "teacher": null,
    "school": {
      "id": 101,
      "uuid": "f3a1c2b0-9d4e-4a2a-8b7f-1234567890ab",
      "name": "SMA Negeri 1 Diskola",
      "image": "https://cdn.diskola.id/schools/sman1-diskola.png",
      "address": "Jl. Pendidikan No. 10, Bandung",
      "city_name": "Bandung",
      "coordinate_radius": "50",
      "coordinate_latitude": -6.914744,
      "coordinate_longitude": 107.60981
    },
    "roles": [
      { "id": 3, "name": "student", "guard_name": "api" }
    ]
  },
  "errors": null,
  "error": "",
  "using_default_password": "Y",
  "rule_label": "Siswa",
  "rule": { "is_student": true, "is_teacher": false, "is_librarian": false },
  "schedule": {
    "id": 0,
    "subject": "",
    "subject_image": "",
    "classroom": "",
    "time_plot": "",
    "status": ""
  },
  "current_subject": null,
  "is_klaspay_activated": false,
  "is_email_verified": true,
  "is_verified": true,
  "is_active": true,
  "userProgress": null
}
```
Catatan: field `errors.credential` (`UserResponseError`) berisi `List<String>` pesan error kredensial jika ada, mis. `{"errors": {"credential": ["Username tidak ditemukan"]}}`.

---

### `POST mobile/app/authentication/login-sso` — checkEmail
**Parameter:**
- `@Body data: Any` — Body: dinamis (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada. Dari nama fungsi kemungkinan `{"email": "..."}` untuk login SSO (Google) awal.

**Response:** `UserSsoResponseData`
```json
{
  "data": {
    "id": 2002,
    "username": "siti.nurhaliza",
    "email": "siti.nurhaliza@gmail.com",
    "nisn": "0051234999",
    "image": "https://cdn.diskola.id/avatars/2002.png",
    "is_student": true,
    "is_teacher": false,
    "is_klaspay_activated": false,
    "school": null,
    "current_class": null,
    "credential": { "using_default_password": "Y" }
  },
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": ""
}
```

---

### `GET mobile/app/authentication/login-sso/classes` — getClasses
**Parameter:**
- `@Query take: Int`
- `@Query skip: Int`
- `@Query name: String = ""` — pencarian nama kelas.

**Response:** `SchoolClassResponse`
```json
{
  "data": [
    { "id": 12, "name": "X IPA 1", "grade": 10 }
  ]
}
```

---

### `GET mobile/app/roles` — getRoles
**Parameter:**
- `@Query take: Int`
- `@Query skip: Int`
- `@Query name: String = ""`

**Response:** `UserRoleResponse`
```json
{
  "data": [
    { "id": 3, "name": "student", "guard_name": "api" },
    { "id": 4, "name": "teacher", "guard_name": "api" }
  ]
}
```

---

### `POST mobile/app/authentication/login-account` — login
**Parameter:**
- `@Body data: Any` — Body: dinamis (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada. Dari nama fungsi kemungkinan `{"username": "...", "password": "...", "school_id": 101}`.

**Response:** `LoginResponse`
```json
{
  "data": {
    "id": 2001,
    "uuid": "8b1e0a90-5c3d-4f21-9a11-abcdef123456",
    "using_default_password": "Y",
    "name": "Ahmad Fauzan",
    "email": "ahmad.fauzan@diskola.id",
    "nisn_nik": "0051234567",
    "nis_nik": "20231234",
    "phone": "081234567890",
    "place_of_birth": "Bandung",
    "date_of_birth": "2008-05-14",
    "gender": "L",
    "user_avatar_image": "https://cdn.diskola.id/avatars/2001.png",
    "religion": "Islam",
    "blood_type": "O",
    "address": "Jl. Merdeka No. 5",
    "sub_district_id": "3273010",
    "sub_district": "Sukasari",
    "city_id": "3273",
    "city": "Bandung",
    "province_id": "32",
    "province": "Jawa Barat",
    "user_username": "ahmadfauzan",
    "student": null,
    "teacher": null,
    "school": null,
    "roles": [ { "id": 3, "name": "student", "guard_name": "api" } ]
  },
  "meta": { "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." },
  "credential": { "using_default_password": "Y" },
  "error": "",
  "is_verified": true,
  "rule_label": "Siswa",
  "rule": { "is_student": true, "is_teacher": false, "is_librarian": false },
  "is_klaspay_activated": false,
  "is_email_verified": true,
  "product_school": { "onklas_lite": true, "onklas_pro": false, "klastime": false }
}
```

---

### `POST mobile/app/authentication/login-sso/school` — loginSso
**Parameter:**
- `@Body data: Any` — Body: dinamis (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada. Dari nama fungsi kemungkinan `{"email": "...", "school_id": 101}` — memilih sekolah setelah login SSO.

**Response:** `LoginSsoResponse`
```json
{
  "data": {
    "id": 2002,
    "name": "Siti Nurhaliza",
    "email": "siti.nurhaliza@gmail.com",
    "avatar": "https://cdn.diskola.id/avatars/2002.png",
    "uuid": "1a2b3c4d-5e6f-4a7b-8c9d-0123456789ab",
    "school_id": 101,
    "is_klaspay_activated": 0
  }
}
```

---

### `POST mobile/app/authentication/login-sso/check-nisn` — checkNisn
**Parameter:**
- `@Body data: Any` — Body: dinamis (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada. Dari nama fungsi kemungkinan `{"nisn": "0051234567", "school_id": 101}`.

**Response:** `CheckNisnResponseData`
```json
{
  "data": {
    "id": 2001,
    "name": "Ahmad Fauzan",
    "email": "ahmad.fauzan@diskola.id",
    "nisn_nik": "0051234567",
    "school": "SMA Negeri 1 Diskola",
    "is_student": true,
    "roles": [ { "id": 3, "name": "student", "guard_name": "api" } ],
    "current_class": {
      "id": 12,
      "class_room": { "id": 3, "name": "X IPA 1", "grade": 10, "majorId": 1 },
      "name": "X IPA 1",
      "grade": 10
    }
  }
}
```

---

### `POST mobile/app/authentication/login-sso/verification` — verifNisn
**Parameter:**
- `@Body data: Any` — Body: dinamis (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada. Dari nama fungsi kemungkinan `{"nisn": "...", "otp": "123456"}` untuk verifikasi kode OTP/NISN.

**Response:** `LoginSsoResponse` (sama shape dengan endpoint `loginSso` di atas).

---

### `POST mobile/app/authentication/requesting-student` — submitStudentApproval
**Parameter:**
- `@Body data: Any` — Body: dinamis (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada. Dari nama fungsi kemungkinan berisi data pengajuan akun siswa (nama, nisn, kelas, dsb) untuk direview admin sekolah.

**Response:** `UserSsoResponseData` (sama shape dengan endpoint `checkEmail` di atas).

---

### `POST mobile/app/authentication/requesting-teacher` — submitTeacherApproval
**Parameter:**
- `@Body data: Any` — Body: dinamis (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada. Dari nama fungsi kemungkinan berisi data pengajuan akun guru (nama, nip, dsb) untuk direview admin sekolah.

**Response:** `UserSsoResponseData` (sama shape dengan endpoint `checkEmail` di atas).

---

### `GET mobile/gmail/verify/{google_token}` — verifyEmail
**Parameter:**
- `@Path google_token: String` — token Google yang akan diverifikasi.

**Response:** `Any` (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada.

---

### `POST mobile/app/authentication/reset-password` — resetPass
**Parameter:**
- `@Body data: Any` — Body: dinamis (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada. Dari nama fungsi kemungkinan `{"email": "..."}` atau `{"username": "...", "school_id": 101}` untuk memicu reset password.

**Response:** `Any` (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada.

---

## Sesi & Logout

### `DELETE logout` — logout
**Parameter:** (tidak ada)
**Response:** tidak ada (fungsi `suspend fun logout()` tanpa return type eksplisit / `Unit`). Cukup mengandalkan HTTP status code.

---

### `DELETE sessions/others` — logoutOthers
**Parameter:** (tidak ada)
**Response:** `Unit` (tidak ada body).

---

### `DELETE sessions/{id}` — logoutDevice
**Parameter:**
- `@Path id: Int` — id sesi/device yang akan di-logout.

**Response:** `Unit` (tidak ada body).

---

### `GET sessions` — listSessions
**Parameter:**
- `@Query limit: Int = 10`
- `@Query take: Int = 10`
- `@Query skip: Int = 0`

**Response:** `SessionResponse`
```json
{
  "data": [
    { "id": 1, "name": "Redmi Note 12 - Chrome", "last_used_at": "2026-09-08 21:15:00" },
    { "id": 2, "name": "iPhone 13 - Safari", "last_used_at": "2026-09-01 08:02:11" }
  ]
}
```

---

## FCM (Push Notification Token)

### `POST sosmed/setting/setup-user-fcm` — updateFcm
**Parameter:**
- `@Body data: Any` — Body: dinamis (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada. Dari nama fungsi kemungkinan `{"fcm_token": "...", "user_id": 2001, "device_type": "android"}`.

**Response:** `Any` (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada.

---

### `POST sosmed/setting/setup-user-fcm` — updateFcmAsync
Sama endpoint dengan `updateFcm` di atas, tapi versi non-suspend (callback based).
**Parameter:**
- `@Body data: Any` — sama dengan `updateFcm`.

**Response:** `Call<String>` — String mentah dari body response (bukan JSON object bertipe).

---

## Akun / Profil

### `GET mobile/app/accounts/user/{userId}` — getUser
**Parameter:**
- `@Path userId: Int`

**Response:** `GetUserResp`
```json
{
  "data": {
    "id": 2001,
    "uuid": "8b1e0a90-5c3d-4f21-9a11-abcdef123456",
    "name": "Ahmad Fauzan",
    "email": "ahmad.fauzan@diskola.id",
    "user_username": "ahmadfauzan",
    "nisn_nik": "0051234567",
    "nis_nik": "20231234",
    "phone": "081234567890",
    "user_avatar_image": "https://cdn.diskola.id/avatars/2001.png",
    "is_verified": true
  }
}
```

---

### `GET mobile/app/accounts/get-username/{username}` — getUsername
**Parameter:**
- `@Path username: String?`

**Response:** `FeedUsernameResponse` (shape `data` sama dengan `FeedUser`, lihat contoh di `getUser` di atas).

---

### `GET mobile/app/accounts/user/{userId}/feed-post` — getUserPost
**Parameter:**
- `@Path userId: Int?`
- `@Query take: Int`
- `@Query skip: Int`

**Response:** `FeedResponse`
```json
{
  "data": [
    {
      "id": 9001,
      "row_id": 9001,
      "feed_type": "post",
      "created_at": "2026-08-20T09:00:00Z",
      "created_at_label": "20 Agustus 2026",
      "feed_title": "Kegiatan Upacara Bendera",
      "feed_body": "Upacara bendera memperingati HUT RI berjalan lancar.",
      "feed_author": "Ahmad Fauzan",
      "feed_thumbnail_image": "https://cdn.diskola.id/feed/9001-thumb.png",
      "users": {
        "id": 2001,
        "uuid": "8b1e0a90-5c3d-4f21-9a11-abcdef123456",
        "name": "Ahmad Fauzan",
        "email": "ahmad.fauzan@diskola.id",
        "user_username": "ahmadfauzan",
        "nisn_nik": "0051234567",
        "nis_nik": "20231234",
        "phone": "081234567890",
        "user_avatar_image": "https://cdn.diskola.id/avatars/2001.png",
        "is_verified": true
      },
      "file": {
        "feed_files_name": "upacara.png",
        "feed_files_path": "https://cdn.diskola.id/feed/upacara.png",
        "feed_files_size": "204800",
        "feed_files_type": "image",
        "feed_files_format": "png",
        "feed_files_width": "1080",
        "feed_files_height": "1080",
        "feed_files_id": 501
      },
      "count_comments": 2,
      "count_likes": 10,
      "likes": [],
      "comments": [],
      "is_likes": false
    }
  ]
}
```

---

### `GET mobile/app/accounts/user/{userId}/feed-image` — getUserImage
**Parameter:**
- `@Path userId: Int?`
- `@Query take: Int`
- `@Query skip: Int`

**Response:** `FeedResponse` (shape sama dengan `getUserPost`, difilter hanya feed bertipe gambar).

---

### `GET mobile/app/accounts/user/{userId}/feed-ebook` — getUserEbook
**Parameter:**
- `@Path userId: Int?`
- `@Query take: Int`
- `@Query skip: Int`

**Response:** `FeedResponse` (shape sama dengan `getUserPost`, difilter hanya feed bertipe ebook).

---

### `POST mobile/app/accounts/user/{userUuid}/change-avatar` — uploadProfilePicture
**Multipart request.**
**Parameter:**
- `@Path userUuid: String`
- `@Part file: MultipartBody.Part?` — file gambar avatar.

**Response:** `Any` (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada.

---

### `PUT mobile/app/accounts/user/{userUuid}/change-profile` — updateAccount
**Parameter:**
- `@Path userUuid: String`
- `@Body data: Any` — Body: dinamis (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada. Dari nama fungsi kemungkinan berisi field-field profil seperti `name`, `email`, `phone`, `address`, `gender`, `date_of_birth`, dsb (lihat `UserResponseData` untuk daftar field profil yang ada).

**Response:** `Any` (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada.

---

### `GET mobile/email/verify` — sendEmailVerification
**Parameter:** (tidak ada)
**Response:** `Any` (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada. Kemungkinan hanya memicu pengiriman email verifikasi tanpa body signifikan.

---

### `PUT mobile/app/accounts/user/{userUuid}/change-password` — changePassword
**Parameter:**
- `@Path userUuid: String`
- `@Body data: Any` — Body: dinamis (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada. Dari nama fungsi kemungkinan `{"old_password": "...", "new_password": "...", "new_password_confirmation": "..."}`.

**Response:** `Any` (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada.

---

### `GET mobile/app/accounts/search-username` — searchUsername
**Parameter:**
- `@Query params: String` — kata kunci pencarian username.

**Response:** `FeedUserResponse`
```json
{
  "data": [
    {
      "id": 2003,
      "uuid": "3c4d5e6f-7a8b-4c9d-0e1f-234567890abc",
      "name": "Budi Santoso",
      "email": "budi.santoso@diskola.id",
      "user_username": "budisantoso",
      "nisn_nik": "0051235000",
      "nis_nik": "20231240",
      "phone": "081298765432",
      "user_avatar_image": "https://cdn.diskola.id/avatars/2003.png",
      "is_verified": true
    }
  ]
}
```

---

## Feed Sosial Sekolah (Legacy — sudah tidak aktif di UI, tetap bagian dari kontrak API)

### `POST mobile/app/schools/feed-posts` — createPost
**Multipart request.**
**Parameter:**
- `@PartMap data: Map<String, RequestBody>` — Body: dinamis (tidak bertipe di kode lama, dikirim sebagai multipart form fields). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada. Dari nama fungsi kemungkinan berisi field seperti `feed_title`, `feed_body`.
- `@Part file: MultipartBody.Part?` — file gambar/lampiran postingan.

**Response:** `Any` (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada.

---

### `POST mobile/app/schools/feed-ebooks` — createEbook
**Multipart request.**
**Parameter:**
- `@PartMap data: Map<String, RequestBody>` — Body: dinamis (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada. Dari nama fungsi kemungkinan berisi field seperti `feed_title`, `feed_body`.
- `@Part file: List<MultipartBody.Part>?` — satu atau lebih file ebook.

**Response:** `Any` (tidak bertipe di kode lama). ⚠️ perlu verifikasi manual ke backend/Postman collection kalau ada.

---

### `GET mobile/app/schools/feed-posts` — feeds
**Parameter:**
- `@Query take: Int`
- `@Query skip: Int`

**Response:** `FeedResponse` (shape sama dengan contoh pada `getUserPost` di atas, `feed_type: "post"`).

---

### `GET mobile/app/schools/feed-ebooks` — ebooks
**Parameter:**
- `@Query take: Int`
- `@Query skip: Int`

**Response:** `FeedResponse` (shape sama dengan contoh pada `getUserPost` di atas, `feed_type: "ebook"`).

---

### `GET mobile/app/schools/feeds/{feedId}` — feedDetail
**Parameter:**
- `@Path feedId: Int`

**Response:** `FeedSingleResponse`
```json
{
  "data": {
    "id": 9001,
    "row_id": 9001,
    "feed_type": "post",
    "created_at": "2026-08-20T09:00:00Z",
    "created_at_label": "20 Agustus 2026",
    "feed_title": "Kegiatan Upacara Bendera",
    "feed_body": "Upacara bendera memperingati HUT RI berjalan lancar.",
    "feed_author": "Ahmad Fauzan",
    "feed_thumbnail_image": "https://cdn.diskola.id/feed/9001-thumb.png",
    "users": {
      "id": 2001,
      "uuid": "8b1e0a90-5c3d-4f21-9a11-abcdef123456",
      "name": "Ahmad Fauzan",
      "email": "ahmad.fauzan@diskola.id",
      "user_username": "ahmadfauzan",
      "nisn_nik": "0051234567",
      "nis_nik": "20231234",
      "phone": "081234567890",
      "user_avatar_image": "https://cdn.diskola.id/avatars/2001.png",
      "is_verified": true
    },
    "file": {
      "feed_files_name": "upacara.png",
      "feed_files_path": "https://cdn.diskola.id/feed/upacara.png",
      "feed_files_size": "204800",
      "feed_files_type": "image",
      "feed_files_format": "png",
      "feed_files_width": "1080",
      "feed_files_height": "1080",
      "feed_files_id": 501
    },
    "count_comments": 2,
    "count_likes": 10,
    "likes": [
      {
        "id": 701,
        "created_at_label": "20 Agustus 2026",
        "user": {
          "id": 2003,
          "uuid": "3c4d5e6f-7a8b-4c9d-0e1f-234567890abc",
          "name": "Budi Santoso",
          "email": "budi.santoso@diskola.id",
          "user_username": "budisantoso",
          "nisn_nik": "0051235000",
          "nis_nik": "20231240",
          "phone": "081298765432",
          "user_avatar_image": "https://cdn.diskola.id/avatars/2003.png",
          "is_verified": true
        }
      }
    ],
    "comments": [
      {
        "id": 801,
        "feed_comments_body": "Mantap!",
        "created_at_label": "20 Agustus 2026",
        "user": {
          "id": 2003,
          "uuid": "3c4d5e6f-7a8b-4c9d-0e1f-234567890abc",
          "name": "Budi Santoso",
          "email": "budi.santoso@diskola.id",
          "user_username": "budisantoso",
          "nisn_nik": "0051235000",
          "nis_nik": "20231240",
          "phone": "081298765432",
          "user_avatar_image": "https://cdn.diskola.id/avatars/2003.png",
          "is_verified": true
        }
      }
    ],
    "is_likes": false
  }
}
```

---

### `GET mobile/app/schools/feeds/{feedId}/comment` — feedComment
**Parameter:**
- `@Path feedId: Int`
- `@Query take: Int`
- `@Query skip: Int`

**Response:** `FeedCommentResponse`
```json
{
  "data": [
    {
      "id": 801,
      "feed_comments_body": "Mantap!",
      "created_at_label": "20 Agustus 2026",
      "user": {
        "id": 2003,
        "uuid": "3c4d5e6f-7a8b-4c9d-0e1f-234567890abc",
        "name": "Budi Santoso",
        "email": "budi.santoso@diskola.id",
        "user_username": "budisantoso",
        "nisn_nik": "0051235000",
        "nis_nik": "20231240",
        "phone": "081298765432",
        "user_avatar_image": "https://cdn.diskola.id/avatars/2003.png",
        "is_verified": true
      }
    }
  ]
}
```
