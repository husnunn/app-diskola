# API Spec — Absensi / Presensi (Mobile)

Sumber route: `routes/mobile.php` pada group `attendance` (sekitar baris 394–403).  
Registrasi route: `RouteServiceProvider::mapApiMobileRoutes()` → prefix **`/api/mobile`**, middleware **`api`** dan (untuk seluruh isi file ini) **`auth:sanctum`**.

## Base path lengkap

```
/api/mobile/attendance
```

## Auth & header

- Wajib: `Authorization: Bearer <token Sanctum>`
- `Accept: application/json`
- Untuk `POST`: `Content-Type: application/json` (body JSON)

Pengguna harus punya relasi **`student`** atau **`teacher`** sesuai endpoint; beberapa endpoint membedakan perilaku lewat `ClassScheduleResource` (`isset(auth()->user()->student)` vs `teacher`).

---

## Ringkasan alur (siswa)

1. **Jadwal harian** — `GET /schedule?date=YYYY-MM-DD` untuk melihat slot kelas dan status (`toward` / `ongoing` / `passed`), serta `attend_at` / `leave_at` milik siswa login (jika sudah absen).
2. **Detail jadwal** — `GET /schedule/{schedule}` untuk melihat apakah presensi sudah dibuka guru, **`class_password`** (kunci untuk absen masuk), batas telat (`late_limit`), dan ringkasan jumlah siswa.
3. **Absen masuk** — `POST /attend` dengan `school_subject_schedule_id` dan **`password`** yang sama dengan `class_password` dari langkah 2.
4. **Absen keluar** — `POST /leave` setelah selesai mengikuti kelas (tanpa password).
5. **Rekap harian per bulan** — `GET /data/{month}/{year}` untuk kalender / riwayat jam masuk–keluar agregat per tanggal.
6. **Ringkasan tahunan** — `GET /summary/{year}` untuk jumlah hadir tepat waktu vs terlambat per bulan.

Guru membuka sesi dengan **`POST /start`** dan menutup dengan **`POST /end`**; saat start, sistem juga mengirim notifikasi FCM ke siswa (topik + token) agar membuka menu presensi.

---

## Daftar endpoint

| Metode | Path | Controller | Peran utama |
|--------|------|------------|-------------|
| `GET` | `/schedule` | `ClassScheduleController@index` | Guru & siswa — daftar jadwal per hari |
| `GET` | `/schedule/{schedule}` | `ClassScheduleController@show` | Guru & siswa — detail slot + password jika presensi aktif |
| `POST` | `/start` | `SchoolAttendanceController@start` | Guru — mulai presensi |
| `POST` | `/end` | `SchoolAttendanceController@end` | Guru — akhiri presensi |
| `POST` | `/attend` | `SchoolAttendanceController@attend` | Siswa — catat hadir |
| `POST` | `/leave` | `SchoolAttendanceController@leave` | Siswa — catat keluar |
| `GET` | `/data/{month}/{year}` | `SchoolAttendanceController@attendanceData` | Siswa — data per hari dalam bulan |
| `GET` | `/summary/{year}` | `SchoolAttendanceController@attendanceSummary` | Siswa — agregat per bulan dalam tahun |

`{schedule}` adalah **ID** model `SchoolSubjectSchedule` (route model binding).

---

## 1) `GET /schedule`

Daftar jadwal pelajaran untuk **nama hari** yang sesuai dengan query `date` (default: hari ini). Guru melihat kelas yang diajar; siswa melihat jadwal kelasnya.

### Query

| Parameter | Wajib | Keterangan |
|-----------|--------|------------|
| `date` | Tidak | Format `YYYY-MM-DD`. Menentukan hari (SENIN, SELASA, …) yang dipakai filter jadwal. Default: tanggal server hari ini. |

### Proses (ringkas)

1. Konversi `date` → nama hari bahasa Indonesia (`SENIN`, …).
2. Jika user guru: `SchoolSubjectSchedule::getClassListTeacher(...)`.
3. Jika user siswa: `getClassListStudent(...)`.
4. Tiap baris ditransform **`ClassScheduleResource`**.

### Perbedaan siswa vs guru di response

- **Guru**: `attend_at` / `leave_at` diambil dari **`school_attendances`** (waktu mulai/akhir sesi oleh guru).
- **Siswa**: `attend_at` / `leave_at` diambil dari **`school_attendance_details`** baris milik `auth()->user()->student->id` untuk jadwal & tanggal tersebut.

### Response 200

Bungkus Laravel resource collection (biasanya):

```json
{
  "data": [
    {
      "date": "2026-04-21",
      "subject_schedule_id": 123,
      "name_of_day": "SENIN",
      "school_name": "SMA Contoh",
      "class_name": "XII IPA 1",
      "teacher_id": 45,
      "teacher_name": "Nama Guru",
      "teacher_nip": "1234567890",
      "subject_id": 10,
      "subject_name": "Matematika",
      "subject_icon_image": "https://...",
      "attend_at": "08:05:12",
      "leave_at": "09:45:00",
      "time_plot_start_at": "08:00",
      "time_plot_end_at": "10:00",
      "status": "ongoing",
      "late_at": "2026-04-21 08:15:00"
    }
  ]
}
```

### Field penting

| Field | Arti |
|-------|------|
| `subject_schedule_id` | ID jadwal mapel; dipakai di `POST /attend`, `POST /leave`, dan path `GET /schedule/{schedule}`. |
| `status` | `toward` (belum mulai / tanggal mendatang), `ongoing` (dalam rentang jam plot), `passed` (lewat jam atau tanggal lampau). |
| `late_at` | Dari header presensi hari itu (kosong jika guru belum `POST /start`). |
| `attend_at` / `leave_at` | Untuk siswa: jam absen masuk/keluar sendiri; string waktu `H:i:s` atau `""`. |

**Catatan implementasi:** pada `ClassScheduleResource`, untuk peran siswa field `leave_at` di daftar jadwal diambil dari objek presensi sesi (`school_attendances`), bukan dari baris detail siswa; `attend_at` mengikuti `school_attendance_details`. Untuk jam keluar per siswa yang konsisten dengan database detail, gunakan `GET /data/{month}/{year}` atau periksa alur di client setelah `POST /leave`.

---

## 2) `GET /schedule/{schedule}`

Detail satu slot jadwal: rentang waktu efektif (termasuk **rantai plot berurutan** mapel yang sama), dan jika presensi sudah ada, metadata sesi + **`class_password`**.

### Query

| Parameter | Wajib | Keterangan |
|-----------|--------|------------|
| `date` | Tidak | `YYYY-MM-DD` untuk mencari `SchoolAttendance` pada tanggal itu. Default hari ini. |

### Proses (ringkas)

1. Ambil `SchoolAttendance` untuk `schedule->id` dan `whereDate(created_at, $date)`.
2. Hitung deretan jadwal berurutan (plot berikutnya yang menyambung) untuk `plot_end_at` agregat.
3. Response **`ClassScheduleDetailResource`**.

### Response 200 — presensi sudah berjalan (`attendance` ada)

```json
{
  "data": {
    "attendance_id": 500,
    "subject_schedule_id": 123,
    "class_password": "diskola2023",
    "late_limit": 10,
    "teacher_name": "Nama Guru",
    "school_name": "SMA Contoh",
    "school_major_name": "IPA",
    "subject_name": "Matematika",
    "subject_icon_image": "https://...",
    "class_name": "XII IPA 1",
    "grade": 12,
    "name_of_day": "SENIN",
    "attend_at": "08:00:00",
    "leave_at": "",
    "plot_start_at": "08:00",
    "plot_end_at": "12:00",
    "total_attended_student": 35,
    "total_student": 40
  }
}
```

- `class_password` — string yang harus dikirim siswa sebagai `password` pada **`POST /attend`**.
- `late_limit` — menit antara `created_at` presensi dan `late_at` (batas “tepat waktu”).
- `total_attended_student` — di kode, dihitung dari jumlah detail dengan **`is_late` tidak null** (bukan sekadar `attend_at` terisi); ini mencerminkan “sudah tercatat kehadiran” (tepat waktu atau terlambat).
- `attend_at` / `leave_at` pada resource ini untuk **level sesi** (waktu buka/tutup kelas oleh guru), bukan jam per-siswa.

### Response 200 — presensi belum dibuka (`attendance` kosong)

```json
{
  "data": {
    "attendance_id": 0,
    "subject_schedule_id": 123,
    "class_password": "",
    "late_limit": "",
    "teacher_name": "Nama Guru",
    "school_name": "SMA Contoh",
    "school_major_name": "IPA",
    "subject_name": "Matematika",
    "subject_icon_image": "https://...",
    "class_name": "XII IPA 1",
    "grade": 12,
    "name_of_day": "SENIN",
    "attend_at": "",
    "leave_at": "",
    "plot_start_at": "08:00",
    "plot_end_at": "10:00",
    "total_attended_student": "",
    "total_student": ""
  }
}
```

---

## 3) `POST /start` (guru)

Membuka presensi untuk jadwal mapel; membuat baris **`school_attendances`** + **`school_attendance_details`** per siswa di kelas; menjaga **jadwal berurutan** (beberapa `school_subject_schedule_id` dalam satu rangkaian); mengirim FCM.

### Body (JSON)

| Field | Wajib | Tipe | Keterangan |
|-------|--------|------|------------|
| `school_subject_schedule_id` | Ya | number | ID jadwal; harus milik `teacher_id` guru login. |
| `password` | Ya | string | Kode kelas yang akan divalidasi siswa saat absen. |
| `late_limit` | Ya | number | Menit dari **sekarang** untuk set `late_at` (`now + late_limit`). |

### Response 200

Objek tunggal **`SchoolAttendanceResource`** (biasanya dibungkus `data`):

| Field | Keterangan |
|-------|------------|
| `attendance_id` | ID `school_attendances`. |
| `school_subject_schedule_id` | Jadwal utama. |
| `late_limit` | Menit antara `created_at` dan `late_at`. |
| `start_at` | `created_at` sesi (datetime string). |
| `end_at` | Kosong sampai `POST /end`. |
| `teacher_name`, `teacher_nip`, `school_name`, `school_major_name`, `subject_name`, `class_name`, `grade` | Snapshot teks di baris presensi. |
| `student_attendances` | Hanya terisi jika relasi `schoolAttendanceDetail` di-load; pada endpoint `start` response standar **biasanya tidak** menyertakan koleksi ini. |

### Error yang umum

- Validasi gagal → **422** (`school_subject_schedule_id`, `password`, `late_limit`).
- Jadwal tidak ditemukan / bukan milik guru → bisa **404** atau error query tergantung data.

---

## 4) `POST /end` (guru)

Menutup presensi untuk **semua** `school_attendances` hari ini yang `school_subject_schedule_id`-nya termasuk dalam **`subject_schedule_id_squence`** (satu rangkaian plot).

### Body (JSON)

| Field | Wajib | Tipe |
|-------|--------|------|
| `school_subject_schedule_id` | Ya | number |

### Response 200

Sama seperti **`SchoolAttendanceResource`** untuk baris presensi yang dipakai lookup; `end_at` terisi waktu sekarang untuk semua sesi terkait yang `end_at`-nya masih null.

---

## 5) `POST /attend` (siswa)

Mencatat **jam masuk** (`attend_at`) pada **semua** baris `school_attendance_details` milik siswa login yang terhubung ke rangkaian presensi hari ini (beberapa `school_attendance_id` jika jadwal beruntun), jika `attend_at` masih null.

### Body (JSON)

| Field | Wajib | Keterangan |
|-------|--------|------------|
| `school_subject_schedule_id` | Ya | ID jadwal (salah satu dari rangkaian). |
| `password` | Ya | Harus **sama persis** dengan `password` pada `school_attendances` untuk presensi hari itu. |

### Logika bisnis

1. Cari `SchoolAttendance` hari ini untuk `school_subject_schedule_id`.
2. Kumpulkan semua ID presensi dalam `subject_schedule_id_squence` untuk tanggal yang sama.
3. Jika password salah → **422** dengan pesan validasi custom: `credential: ["password kelas salah."]`.
4. `is_late` = `true` jika waktu sekarang **>** `late_at` presensi; selain itu `false`.
5. Update detail: `attend_at` = now, `is_late` sesuai poin 4, hanya baris `attend_at` masih null.

### Response 200

**`SchoolAttendanceDetailResource`** mem-forward array sebagai resource; isi umumnya:

```json
{
  "data": {
    "school_subject_schedule_id": 123,
    "student_id": 456,
    "attend_at": "2026-04-21T08:04:17.000000Z"
  }
}
```

(`attend_at` mengikuti serialisasi datetime Laravel/JSON.)

### Error

- **404** jika tidak ada presensi hari ini untuk jadwal tersebut (`firstOrFail` di `getAttendanceId`).
- **422** validasi field atau password salah.

---

## 6) `POST /leave` (siswa)

Mencatat **jam keluar** (`leave_at`) pada detail milik siswa untuk semua `school_attendance_id` dalam rangkaian yang sama (hanya jika `leave_at` masih null).

### Body (JSON)

| Field | Wajib |
|-------|--------|
| `school_subject_schedule_id` | Ya |

### Response 200

```json
{
  "data": {
    "school_subject_schedule_id": 123,
    "student_id": 456,
    "leave_at": "2026-04-21T09:55:00.000000Z"
  }
}
```

---

## 7) `GET /data/{month}/{year}` (siswa)

Rekap **per tanggal** dalam bulan: jam masuk pertama dan jam keluar terakhir dari **semua** slot absensi siswa pada tanggal itu (hanya hari dengan `attend_at` tidak null ikut query agregat).

### Path parameter

| Parameter | Contoh | Keterangan |
|-----------|--------|------------|
| `month` | `4` | Bulan 1–12 |
| `year` | `2026` | Tahun empat digit |

### Response 200

Koleksi **`SchoolAttendanceDataResource`** — satu item per hari di bulan tersebut:

```json
{
  "data": [
    {
      "date": "2026-04-01",
      "attend_at": "08:01:05",
      "leave_at": "02:30:00"
    }
  ]
}
```

Catatan implementasi: format jam di resource memakai `Carbon::format('h:i:s')` (**jam 12-jam**). Jika jam malam perlu ditampilkan konsisten dengan 24 jam, pertimbangkan penyesuaian di resource.

---

## 8) `GET /summary/{year}` (siswa)

Agregat jumlah rekaman **`school_attendance_details`** per bulan dalam tahun: **`ontime`** (`is_late` = false) dan **`late`** (`is_late` = true).

### Path parameter

- `year` — misalnya `2026`.

### Response 200

Koleksi **`SchoolAttendanceSummaryResource`**:

```json
{
  "data": [
    {
      "month": "Januari",
      "year": 2026,
      "ontime": 20,
      "late": 1
    }
  ]
}
```

Nama bulan dalam bahasa Indonesia (`Januari` … `Desember`).

---

## Model data (konsep)

| Entitas | Fungsi |
|---------|--------|
| `school_attendances` | Satu sesi presensi per jadwal per hari: password, `late_at`, snapshot nama sekolah/mapel/kelas, `end_at`, `subject_schedule_id_squence` (CSV id jadwal berurutan). |
| `school_attendance_details` | Per siswa per sesi: `attend_at`, `leave_at`, `is_late` (null belum absen masuk; setelah absen masuk berisi boolean). |

---

## Notifikasi (guru `POST /start`)

Firebase Cloud Messaging mengirim payload `data` antara lain:

- `title`, `body`
- `page.menu` = `presensi`
- `page.school_subject_schedule_id`, `school_name`, `class_name`, `teacher_name`, `subject_name`, `class_start_at`, `class_end_at`

Topik: `attendance-{school_class_id}`; sekaligus ke token FCM siswa di kelas.

---

## Referensi file kode

| Bagian | File |
|--------|------|
| Route | `routes/mobile.php` |
| Prefix API | `app/Providers/RouteServiceProvider.php` (`mapApiMobileRoutes`) |
| Jadwal | `app/Http/Controllers/Mobile/Teacher/ClassScheduleController.php` |
| Presensi | `app/Http/Controllers/Mobile/Teacher/SchoolAttendanceController.php` |
| Transform response | `app/Http/Resources/School/ClassScheduleResource.php`, `ClassScheduleDetailResource.php`, `SchoolAttendanceResource.php`, `SchoolAttendanceDetailResource.php`, `SchoolAttendanceDataResource.php`, `SchoolAttendanceSummaryResource.php` |

Dokumen ini menggambarkan perilaku sesuai kode pada saat penulisan; jika ada perubahan route atau resource, sesuaikan dengan `routes/mobile.php` dan resource terkait.
