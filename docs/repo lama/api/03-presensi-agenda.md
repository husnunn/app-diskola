# API Contract — Presensi & Agenda Mingguan

Sumber: `app/src/main/java/id/diskola/app/api/ApiService.kt` baris 543–811.

> Catatan: rentang baris ini ternyata tidak murni berisi presensi/agenda — juga mencakup ujian (exam), transaksi SPP, perpustakaan (pustaka), policy/about, dan payment gateway Klaspay/Toppers/PPOB. Semua endpoint aktif di rentang tersebut tetap didokumentasikan di bawah, dikelompokkan per sub-fitur, sesuai instruksi untuk mencatat setiap endpoint aktif dalam rentang baris yang diberikan.

Endpoint yang dikomentari (`//`) di source TIDAK didokumentasikan: `mock/payment/wallet` (varian mock dari `klaspayWallet`), `klaspayTopupTrx` versi lama dengan return `KlaspayTopupTrxResponse`, dan `mock/payment/transaction/invoice` (varian mock dari `klaspayInvoice`).

---

## Presensi Guru/Staff (Rekap & Cek)

### `GET mobile/attendance/staff/by-month` — teacherMonthlyAttendance
**Parameter:** `year: Int?` (query), `month: Int?` (query)
**Response:** `AbsensiResponse`
```json
{
  "data": [
    {
      "date": "2026-09-01",
      "month": 9,
      "year": 2026,
      "dateLabel": "Selasa, 1 September 2026",
      "attend_at": "07:12",
      "leave_at": "15:05",
      "is_holiday": false,
      "attend_is_late": false,
      "leave_is_early": false,
      "attend_is_offsite": false,
      "leave_is_offsite": false,
      "attend_status": "hadir",
      "leave_status": "hadir",
      "attend_note": "",
      "leave_note": "",
      "attend_address": "Jl. Pendidikan No. 1, Kota",
      "leave_address": "Jl. Pendidikan No. 1, Kota",
      "attend_photo_url": "https://cdn.diskola.id/attendance/staff/2026-09-01-in.jpg",
      "leave_photo_url": "https://cdn.diskola.id/attendance/staff/2026-09-01-out.jpg",
      "leave_request_status": null,
      "leave_request_type": null
    }
  ]
}
```

### `GET mobile/attendance/staff/by-year` — teacherAnnualAttendance
**Parameter:** `year: Int` (query)
**Response:** `RekapAbsensiResponse`
```json
{
  "data": [
    {
      "date": "2026-09",
      "month": "September",
      "year": "2026",
      "ontime": 18,
      "late": 2,
      "izin": 1,
      "sakit": 0,
      "alpha": 0,
      "order": 9
    }
  ]
}
```

### `GET mobile/attendance/staff/check` — teacherCheckPresensi
**Parameter:** tidak ada
**Response:** `CheckAbsenResponse`
```json
{
  "message": "Anda dapat melakukan presensi masuk",
  "data": {
    "allow_attendance": true,
    "type_attendance": "check-in",
    "attendance_response_text": "Absen masuk dibuka pukul 06:30 - 08:00",
    "attendance_response_text_button": "Absen Masuk",
    "schedule": {
      "start_at": "06:30",
      "end_at": "08:00",
      "source": "jadwal_kerja",
      "name": "Jam Kerja Guru"
    }
  }
}
```

### `GET mobile/attendance/setting/me/today` — attendanceSettingMeToday
**Parameter:** tidak ada
**Response:** `TodaySettingResponse`
```json
{
  "data": {
    "date": "2026-09-09",
    "day": "Rabu",
    "status": "hari_kerja",
    "jam_masuk": "07:00",
    "events": [
      {
        "name": "Upacara Bendera",
        "start_time": "07:00",
        "end_time": "07:30",
        "day_off": false
      }
    ],
    "teaching_today": [
      {
        "subject_schedule_id": 4521,
        "subject_id": 12,
        "subject_name": "Matematika",
        "class_id": 7,
        "class_name": "VII-A",
        "name_of_day": "Rabu",
        "start_at": "08:00",
        "end_at": "09:30",
        "attend_at": "08:02",
        "leave_at": null,
        "session_status": "berlangsung"
      }
    ],
    "roles": ["guru", "wali_kelas"]
  }
}
```

### `POST mobile/attendance/check-in` — teacherCheckIn
**Parameter:** `data: Any` (body)
**Body:** dinamis (tidak bertipe di kode lama) — kemungkinan berisi `lat`, `lng`, `address`, `note` seperti pola check-in lain di file ini. ⚠️ perlu verifikasi manual.
**Response:** `Any` (tidak bertipe, kemungkinan hanya `{ "status": true, "message": "..." }`). ⚠️ perlu verifikasi manual.

### `POST mobile/attendance/staff/check-out` — teacherCheckOut
**Parameter:** `data: Any` (body)
**Body:** dinamis — kemungkinan `lat`, `lng`, `address`, `note`. ⚠️ perlu verifikasi manual.
**Response:** `Any`. ⚠️ perlu verifikasi manual.

### `POST mobile/attendance/staff/check-in` — teacherCheckInSchool
**Parameter:** `data: Any` (body)
**Body:** dinamis — kemungkinan `lat`, `lng`, `address`, `note` (presensi masuk di lokasi sekolah, berpasangan dengan `teacherCheckPresensi`/`teacherCheckOut`). ⚠️ perlu verifikasi manual.
**Response:** `Any`. ⚠️ perlu verifikasi manual.

---

## Presensi Siswa

### `GET mobile/attendance/student/check` — studentCheckPresensi
**Parameter:** tidak ada
**Response:** `CheckAbsenResponse` (struktur sama dengan `teacherCheckPresensi`, lihat contoh di atas — untuk konteks siswa `type_attendance` bernilai mis. `"check-in"`/`"check-out"` sesuai jam sekolah siswa).

### `PUT mobile/attendance/student/journal-update` — studentCheckIn
**Parameter:** `data: Any` (body)
**Body:** dinamis (tidak bertipe di kode lama) — nama fungsi mengarah ke update jurnal KBM saat presensi siswa, kemungkinan berisi `subject_schedule_id`, `material`/`topik`, `note`. ⚠️ perlu verifikasi manual.
**Response:** `Any`. ⚠️ perlu verifikasi manual.

### `POST mobile/attendance/student/check-in` — studentCheckInSchool
**Parameter:** `data: Any` (body)
**Body:** dinamis — kemungkinan `lat`, `lng`, `address`, `note` (presensi masuk siswa). ⚠️ perlu verifikasi manual.
**Response:** `Any`. ⚠️ perlu verifikasi manual.

### `POST mobile/attendance/student/check-out` — studentCheckOut
**Parameter:** `data: Any` (body)
**Body:** dinamis — kemungkinan `lat`, `lng`, `address`, `note` (presensi pulang siswa). ⚠️ perlu verifikasi manual.
**Response:** `Any`. ⚠️ perlu verifikasi manual.

---

## Presensi Luar Kantor (Offsite) — Staff

### `GET mobile/attendance/staff/offsite/check` — offsiteCheckPresensi
**Parameter:** tidak ada
**Response:** `OffsiteCheckResponse`
```json
{
  "code": 200,
  "message": "Anda dapat melakukan presensi luar kantor",
  "data": {
    "allow_attendance": true,
    "type_attendance": "check-in",
    "attendance_status": "belum_presensi",
    "attendance_response_text_button": "Absen Masuk Luar Kantor",
    "attendance_response_text": "Silakan lakukan presensi di luar kantor dengan menyertakan lokasi dan foto",
    "require_note": true,
    "require_photo": true,
    "offsite_enabled": true
  }
}
```

### `POST mobile/attendance/staff/offsite/check-in` — offsiteCheckIn (Multipart)
**Parameter:**
- `lat: RequestBody` (part, text) — latitude
- `lng: RequestBody` (part, text) — longitude
- `address: RequestBody` (part, text) — alamat hasil reverse-geocoding
- `note: RequestBody` (part, text) — catatan/keperluan dinas luar
- `photo: MultipartBody.Part?` (part, file, opsional) — foto bukti kehadiran

**Response:** `OffsiteSubmitResponse`
```json
{
  "code": 200,
  "message": "Presensi luar kantor berhasil disimpan",
  "data": {
    "status": "hadir",
    "date": "2026-09-09",
    "time": "07:05",
    "in_time": "07:05",
    "out_time": "",
    "allow_attendance": false,
    "type_attendance": "check-out",
    "attendance_response_text_button": "Absen Pulang Luar Kantor",
    "attendance_response_text": "Anda sudah melakukan presensi masuk"
  }
}
```

### `POST mobile/attendance/staff/offsite/check-out` — offsiteCheckOut (Multipart)
**Parameter:** sama seperti `offsiteCheckIn` (`lat`, `lng`, `address`, `note`, `photo?`)
**Response:** `OffsiteSubmitResponse` (struktur sama; `out_time` terisi, `type_attendance` biasanya menjadi `"complete"`).

---

## Izin / Cuti (Leave Request)

### `GET mobile/attendance/staff/leave-request/today` — staffLeaveRequestToday
**Parameter:** tidak ada
**Response:** `LeaveRequestResponse`
```json
{
  "data": {
    "uuid": "b3e1c9a0-1234-4c56-9abc-1234567890ab",
    "date": "2026-09-09",
    "status": "sakit",
    "note": "Demam, istirahat di rumah",
    "file_url": "https://cdn.diskola.id/leave-request/surat-dokter-b3e1c9a0.pdf",
    "approval_status": "pending",
    "rejection_note": null,
    "reviewed_at": null
  }
}
```

### `GET mobile/attendance/student/leave-request/today` — studentLeaveRequestToday
**Parameter:** tidak ada
**Response:** `LeaveRequestResponse` (struktur identik dengan `staffLeaveRequestToday`, `status` bernilai `"izin"`/`"sakit"` untuk siswa).

### `GET mobile/attendance/staff/leave-request` — staffLeaveRequestHistory
**Parameter:** `page: Int = 1` (query), `approvalStatus: String? = null` (query, key `approval_status`)
**Response:** `LeaveRequestListResponse`
```json
{
  "data": [
    {
      "uuid": "b3e1c9a0-1234-4c56-9abc-1234567890ab",
      "date": "2026-09-05",
      "status": "izin",
      "note": "Keperluan keluarga",
      "file_url": "",
      "approval_status": "approved",
      "rejection_note": null,
      "reviewed_at": "2026-09-05 08:30:00"
    }
  ],
  "meta": {
    "total": 4,
    "current_page": 1,
    "last_page": 1
  }
}
```

### `GET mobile/attendance/student/leave-request` — studentLeaveRequestHistory
**Parameter:** `page: Int = 1` (query), `approvalStatus: String? = null` (query, key `approval_status`)
**Response:** `LeaveRequestListResponse` (struktur identik dengan `staffLeaveRequestHistory`).

### `POST mobile/attendance/staff/leave-request` — staffSubmitLeaveRequest (Multipart)
**Parameter:**
- `status: RequestBody` (part, text) — `"izin"` / `"sakit"`
- `note: RequestBody` (part, text) — alasan
- `file: MultipartBody.Part` (part, file, wajib) — lampiran surat/bukti

**Response:** `LeaveRequestSubmitResponse`
```json
{
  "message": "Pengajuan izin berhasil dikirim",
  "data": {
    "uuid": "c4f2d0b1-2345-4d67-8bcd-2345678901bc",
    "date": "2026-09-09",
    "status": "izin",
    "note": "Acara keluarga",
    "file_url": "https://cdn.diskola.id/leave-request/lampiran-c4f2d0b1.jpg",
    "approval_status": "pending",
    "rejection_note": null,
    "reviewed_at": null
  }
}
```

### `POST mobile/attendance/student/leave-request` — studentSubmitLeaveRequest (Multipart)
**Parameter:** sama seperti `staffSubmitLeaveRequest` (`status`, `note`, `file`)
**Response:** `LeaveRequestSubmitResponse` (struktur identik).

---

## Agenda Mingguan Staf

### `GET mobile/attendance/staff/agendas` — staffAgendas
**Parameter:** `date: String? = null` (query, format `YYYY-MM-DD`)
**Response:** `StaffAgendasResponse`
```json
{
  "message": "",
  "data": {
    "agenda_enabled": true,
    "workgroup_id": 3,
    "date": "2026-09-09",
    "day": 3,
    "agenda_source": "jadwal_mingguan",
    "gate": {
      "in": {
        "time": "07:02",
        "status": "Hadir",
        "via": "gps"
      },
      "out": null
    },
    "agendas": [
      {
        "agenda_id": 101,
        "name": "Rapat Koordinasi Kurikulum",
        "kind": "meeting",
        "window": {
          "start_at": "09:00",
          "end_at": "10:30"
        },
        "require_check": true,
        "sort_order": 1,
        "note": null,
        "policy": {
          "checkout_required": true,
          "early_leave_enabled": false,
          "late_enabled": true
        },
        "event": {
          "id": 5501,
          "time": "09:05",
          "status": "Terlambat",
          "via": "gps",
          "checkout_time": null,
          "checkout_status": null
        }
      }
    ],
    "summary": {
      "required_sessions": 3,
      "checked": 1,
      "late": 1,
      "missing": 2
    }
  }
}
```

### `GET mobile/attendance/staff/agendas/today` — staffAgendasToday
**Parameter:** tidak ada
**Response:** `StaffAgendasResponse` (struktur identik dengan `staffAgendas`, otomatis untuk tanggal hari ini).

### `POST mobile/attendance/staff/agendas/check` — staffAgendaCheck
**Parameter:** `body: StaffAgendaCheckBody`
```json
{
  "agenda_id": 101,
  "lat": -6.914744,
  "lng": 107.60981
}
```
**Response:** `StaffAgendaCheckResponse`
```json
{
  "message": "Presensi agenda berhasil dicatat",
  "data": {
    "id": 5501,
    "agenda_id": 101,
    "date": "2026-09-09",
    "time": "09:05",
    "status": "Terlambat",
    "via": "gps",
    "checkout_time": null,
    "checkout_status": null
  }
}
```

### `POST mobile/attendance/staff/agendas/check-out` — staffAgendaCheckOut
**Parameter:** `body: StaffAgendaCheckBody` (sama seperti `staffAgendaCheck`: `agenda_id`, `lat`, `lng`)
**Response:** `StaffAgendaCheckResponse` (`checkout_time` dan `checkout_status` terisi, misalnya `"10:31"` dan `"Tepat Waktu"`).

---

## Ujian / Exam (Examinations)

> Di luar cakupan presensi/agenda, namun berada di rentang baris 543–811 sehingga tetap dicatat.

### `GET mobile/app/learning/examinations/students/exams-list` — listExam
**Parameter:** `date: String` (query), `take: Int` (query), `skip: Int` (query)
**Response:** `TestStudentResponse`
```json
{
  "data": [
    {
      "id": 88,
      "message": "Ujian dapat diunduh mulai pukul 07:00",
      "ready_to_download": true,
      "ready_to_start": false,
      "ready_to_end": false,
      "layout": {
        "password": "",
        "date": "2026-09-10",
        "date_human": "10 September 2026",
        "start_at": "08:00",
        "end_at": "09:30",
        "subject": "Matematika",
        "icon_image": "https://cdn.diskola.id/subject/matematika.png",
        "teacher": "Budi Santoso, S.Pd",
        "score": 0
      }
    }
  ]
}
```

### `GET mobile/app/learning/examinations/students/exams-scored` — examScored
**Parameter:** `take: Int` (query), `skip: Int` (query)
**Response:** `TestStudentResponse` (struktur identik, `score` pada `layout` terisi nilai akhir).

### `PUT mobile/app/learning/examinations/students/exams/{id}/download` — downloadExam
**Parameter:** `id: String` (path), `data: Any` (body)
**Body:** dinamis — kemungkinan `password` ujian. ⚠️ perlu verifikasi manual.
**Response:** `DownloadSoalResponse`
```json
{
  "data": [
    {
      "id": 501,
      "layout": {
        "question": "Berapa hasil dari 12 x 8?",
        "image": "",
        "choices": [
          { "id": 1, "answer": "96", "is_true": true, "file_path": "", "answered": false },
          { "id": 2, "answer": "86", "is_true": false, "file_path": "", "answered": false }
        ],
        "essay_answer": "",
        "is_essay_answer_true": ""
      }
    }
  ]
}
```

### `GET mobile/app/learning/examinations/students/exams/{id}/detail` — detailExam
**Parameter:** `id: String` (path), `showCorrect: Int = 1` (query, key `is_show_correct`)
**Response:** `TestDetailResponse`
```json
{
  "data": {
    "id": 88,
    "start_at": "08:00",
    "end_at": "09:30",
    "template": {
      "id": 12,
      "name": "UTS Matematika Ganjil",
      "subject": {
        "id": 3,
        "name": "Matematika",
        "icon_image": "https://cdn.diskola.id/subject/matematika.png",
        "message_label": "",
        "teacher": { "id": 9, "name": "Budi Santoso, S.Pd", "nip": "", "nik": "", "address": "" }
      },
      "teacher": { "id": 9, "name": "Budi Santoso, S.Pd", "nip": "", "nik": "", "address": "" },
      "questions": [
        {
          "id": 501,
          "layout": {
            "question": "Berapa hasil dari 12 x 8?",
            "image": "",
            "choices": [
              { "id": 1, "answer": "96", "is_true": true, "file_path": "", "answered": true }
            ],
            "essay_answer": "",
            "is_essay_answer_true": ""
          }
        }
      ]
    }
  }
}
```

### `PUT mobile/app/learning/examinations/students/exams/{id}/start` — startExam
**Parameter:** `id: String` (path), `data: Any` (body)
**Body:** dinamis — kemungkinan kosong/`{}` atau `password`. ⚠️ perlu verifikasi manual.
**Response:** `Any`. ⚠️ perlu verifikasi manual.

### `POST mobile/app/learning/examinations/students/exams/{id}/answer` — answerExam
**Parameter:** `id: String` (path), `data: Any` (body)
**Body:** dinamis — kemungkinan `question_id`, `answer_id`/`essay_answer`. ⚠️ perlu verifikasi manual.
**Response:** `Any`. ⚠️ perlu verifikasi manual.

### `PUT mobile/app/learning/examinations/students/exams/{id}/stop` — endExam
**Parameter:** `id: String` (path)
**Response:** `Any`. ⚠️ perlu verifikasi manual.

### `PUT mobile/app/learning/examinations/students/exams/{id}/stop` — endExamAsync
**Parameter:** `id: String` (path)
**Response:** `Call<Any>` — varian non-suspend (callback) dari endpoint yang sama dengan `endExam`. ⚠️ perlu verifikasi manual.

---

## Transaksi / Tagihan Sekolah (SPP)

### `GET transaction/school-invoice/unpaid` — unpaidInvoice
**Parameter:** `page: Int` (query), `limit: Int` (query)
**Response:** `SppResponse`
```json
{
  "data": [
    {
      "id": 205,
      "month": "September 2026",
      "issued_at": "2026-09-01",
      "school_id": 1,
      "product_school_id": 12,
      "student_id": 5001,
      "discount_amount": 0,
      "total_fee": 350000,
      "paid_at": "",
      "date": 1725148800000,
      "transaction_id": "",
      "is_inquiry_channel": false
    }
  ],
  "links": {
    "first": "https://api.diskola.id/transaction/school-invoice/unpaid?page=1",
    "last": "https://api.diskola.id/transaction/school-invoice/unpaid?page=1",
    "prev": "",
    "next": ""
  }
}
```

### `GET transaction/school-invoice/paid` — paidInvoice
**Parameter:** `page: Int` (query), `limit: Int` (query)
**Response:** `SppResponse` (struktur identik, `paid_at` dan `transaction_id` terisi).

### `GET transaction/school-invoice/process` — processInvoice
**Parameter:** `page: Int` (query), `limit: Int` (query)
**Response:** `SppProcessListResponse`
```json
{
  "data": [
    {
      "id": 77,
      "transaction_type": "spp",
      "reff_id": "SPP-2026090901",
      "payment_method": "Virtual Account BCA",
      "payment_code": "70012345678901",
      "payment_status_url": "https://api.diskola.id/payment/status/70012345678901",
      "expired_at": "2026-09-10 23:59:00",
      "paid_at": "",
      "is_paid": false,
      "is_expired": false,
      "total": 350000,
      "school_fee_invoice": []
    }
  ],
  "links": {
    "first": "https://api.diskola.id/transaction/school-invoice/process?page=1",
    "last": "https://api.diskola.id/transaction/school-invoice/process?page=1",
    "prev": "",
    "next": ""
  }
}
```

### `POST transaction/school-invoice/pay` — payInvoice
**Parameter:** `data: Any` (body), timeout khusus 5 menit (`@Timeout(5, TimeUnit.MINUTES)`)
**Body:** dinamis — kemungkinan `invoice_ids: List<Int>`, `payment_method`/`payment_code`. ⚠️ perlu verifikasi manual.
**Response:** `CheckoutResponse`
```json
{
  "rc": "00",
  "rd": "Success",
  "request_time": "2026-09-09T10:00:00Z",
  "data": {
    "payment_code": "70012345678901",
    "payment_method": "Virtual Account BCA",
    "payment_method_code": "VA_BCA",
    "expired": "2026-09-10 23:59:00",
    "total_amount": "350000",
    "transaction_type": "spp",
    "spi_status_url": "https://api.diskola.id/payment/status/70012345678901"
  }
}
```

### `GET transaction/payment-service` — paymentServices
**Parameter:** tidak ada
**Response:** `PaymentTypeResponse`
```json
{
  "data": [
    { "id": "va_bca", "name": "Virtual Account BCA", "payment_code": "VA_BCA" },
    { "id": "qris", "name": "QRIS", "payment_code": "QRIS" }
  ]
}
```

---

## Perpustakaan (Pustaka)

### `GET mobile/app/learning/pustaka/students/homepage-banner` — perpusBanner
**Parameter:** tidak ada
**Response:** `PerpusBannerResponse`
```json
{
  "data": [
    {
      "content_id": 1,
      "image": "https://cdn.diskola.id/pustaka/banner1.jpg",
      "book_subject": "Fiksi",
      "book_category": "Novel"
    }
  ]
}
```

### `GET mobile/app/learning/pustaka/students/homepage-book-newest` — bookNewest
**Parameter:** tidak ada
**Response:** `BookResponse`
```json
{
  "data": [
    {
      "book": {
        "book_id": 301,
        "title": "Laskar Pelangi",
        "description": "Novel tentang perjuangan anak-anak Belitung",
        "isbn": "9789793062792",
        "number_of_page": "534",
        "language": "Indonesia",
        "author": "Andrea Hirata",
        "publisher": "Bentang Pustaka",
        "published_at": "2005-09-01",
        "cover_image_url": "https://cdn.diskola.id/pustaka/laskar-pelangi.jpg"
      },
      "subject": { "id": 1, "name": "Fiksi" },
      "category": { "id": 2, "name": "Novel", "book_subject_id": 1 }
    }
  ]
}
```

### `GET mobile/app/learning/pustaka/students/homepage-book-famous` — bookBest
**Parameter:** tidak ada
**Response:** `BookResponse` (struktur identik dengan `bookNewest`).

### `GET mobile/app/learning/pustaka/students/search-books` — searchBook
**Parameter:** `name: String` (query)
**Response:** `BookResponse` (struktur identik).

### `GET mobile/app/learning/pustaka/students/books/{id}/available` — bookStock
**Parameter:** `id: Int` (path)
**Response:** `BookStockRespones`
```json
{
  "data": {
    "tab": {
      "stock": 5,
      "available": 3
    }
  }
}
```

### `GET mobile/app/learning/pustaka/students/rent-archives` — rentHistory
**Parameter:** `take: Int` (query), `skip: Int` (query)
**Response:** `BookRentResponse`
```json
{
  "data": [
    {
      "school_book_rent_id": 900,
      "start_at": "2026-08-01",
      "retur_at": "2026-08-15",
      "status": "selesai",
      "code": "PJM-000900",
      "title": "Laskar Pelangi",
      "author": "Andrea Hirata",
      "publisher": "Bentang Pustaka",
      "cover_image_url": "https://cdn.diskola.id/pustaka/laskar-pelangi.jpg",
      "is_late": false
    }
  ],
  "total_rent": 1,
  "total_rent_late": 0
}
```

### `GET mobile/app/learning/pustaka/students/rents` — rentOngoing
**Parameter:** `take: Int` (query), `skip: Int` (query)
**Response:** `BookRentResponse` (struktur identik, berisi peminjaman yang masih berjalan).

---

## Policy / About

### `GET mobile/app/policy` — policy
**Parameter:** tidak ada
**Response:** `PolicyResponse`
```json
{
  "data": {
    "content": "<h1>Kebijakan Privasi</h1><p>...</p>"
  }
}
```

### `GET mobile/app/accounts/user/{userId}/get-layout-about` — about
**Parameter:** `userId: Int` (path)
**Response:** `PolicyResponse` (struktur identik, `content` berisi HTML halaman "Tentang").

---

## Klaspay — Aktivasi & Wallet

### `POST mobile/app/payment/check` — klaspayCheck
**Parameter:** tidak ada
**Response:** `KlaspayCheckResponse`
```json
{
  "data": {
    "email": true,
    "password": false
  }
}
```

### `POST mobile/app/payment/activate` — klaspayActivate
**Parameter:** `data: Any` (body)
**Body:** dinamis — kemungkinan `email`, `password`, `password_confirmation`. ⚠️ perlu verifikasi manual.
**Response:** tidak ada return type (`Unit`) — hanya HTTP status yang diperiksa oleh caller.

### `GET payment/wallet` — klaspayWallet
**Parameter:** tidak ada
**Response:** `KlaspayWalletResponse`
```json
{
  "status": "success",
  "message": "",
  "data": {
    "wallet_id": "WL-500123",
    "user_id": "5001",
    "name": "Muhammad Husnun Niam",
    "email": "muhammadhusnunniam20@gmail.com",
    "school_id": 1,
    "is_toppers": true,
    "toppers_status": "active",
    "balance": 150000,
    "last_update": "2026-09-09 09:00:00"
  }
}
```

### `POST payment/reset-pin` — resetPinKlaspay
**Parameter:** `data: Any` (body)
**Body:** dinamis — kemungkinan `email`. ⚠️ perlu verifikasi manual.
**Response:** `ResetPinResponse`
```json
{
  "status": "success",
  "message": "Kode reset PIN telah dikirim ke email Anda",
  "reset_token": "a1b2c3d4e5",
  "email_verified": true,
  "mail_verified": true
}
```

### `POST payment/reset-pin/setpin` — setPinKlaspay
**Parameter:** `data: Any` (body)
**Body:** dinamis — kemungkinan `reset_token`, `new_pin`, `new_pin_confirmation`. ⚠️ perlu verifikasi manual.
**Response:** `ResetPinResponse` (struktur identik dengan `resetPinKlaspay`).

---

## Klaspay — Topup & Toppers (Transfer)

### `POST payment/toppers/register` — toppersActivate
**Parameter:** `data: Any` (body)
**Body:** dinamis — kemungkinan berisi `pin` aktivasi Toppers. ⚠️ perlu verifikasi manual.
**Response:** `Any`. ⚠️ perlu verifikasi manual.

### `POST payment/toppers/unregister` — toppersDeactivate
**Parameter:** `data: Any` (body)
**Body:** dinamis — kemungkinan `pin` konfirmasi. ⚠️ perlu verifikasi manual.
**Response:** `Any`. ⚠️ perlu verifikasi manual.

### `POST payment/transaction/transfer_inq` — toppersTopupInq
**Parameter:** `data: Any` (body)
**Body:** dinamis — kemungkinan `destination_user` (NISN/username tujuan), `nominal`. ⚠️ perlu verifikasi manual.
**Response:** `ToppersInqResponse`
```json
{
  "status": "success",
  "message": "",
  "data": {
    "transaction_id": "TRX-20260909-001",
    "destination_user": "5002",
    "destination_name": "Siti Aminah",
    "nominal": 50000,
    "admin_fee": 0,
    "school": "SMA Diskola 1"
  }
}
```

### `POST payment/transaction/transfer_trx` — toppersTopupTrx
**Parameter:** `data: Any` (body)
**Body:** dinamis — kemungkinan `transaction_id` (dari inquiry) dan `pin`. ⚠️ perlu verifikasi manual.
**Response:** `ToppersInqResponse` (struktur identik dengan `toppersTopupInq`, dikembalikan setelah transfer dieksekusi).

### `GET payment/user/campaign/point` — paymentCampaign
**Parameter:** tidak ada
**Response:** `PaymentCampaignResponse`
```json
{
  "status": "success",
  "message": "",
  "data": {
    "id": 1,
    "ppob_points": 120,
    "canteen_points": 40,
    "user_id": "5001",
    "campaign_id": "CMP-01",
    "description": "Poin loyalitas kantin & PPOB",
    "campaign": {
      "id": "CMP-01",
      "name": "Promo Kemerdekaan",
      "description": "Kumpulkan poin selama bulan Agustus",
      "start_date": "2026-08-01",
      "end_date": "2026-08-31",
      "status": "berakhir"
    }
  }
}
```

### `GET payment/channel/topup` — paymentChannel
**Parameter:** tidak ada
**Response:** `PaymentChannelResponse`
```json
{
  "data": {
    "virtual account": [
      {
        "channel_method_id": 11,
        "channel_method_name": "Virtual Account BCA",
        "channel_method_category": "virtual account",
        "image_url": "https://cdn.diskola.id/payment/bca.png",
        "payment_url": "",
        "payment_url_v2": "",
        "is_active": true,
        "channel_id": 1
      }
    ]
  }
}
```

### `POST payment/transaction/topup_inq` — klaspayTopupInq
**Parameter:** `data: Any` (body)
**Body:** dinamis — kemungkinan `channel_method_id`, `nominal`. ⚠️ perlu verifikasi manual.
**Response:** `KlaspayTopupInqResponse`
```json
{
  "data": {
    "transaction_id_inquiry": "INQ-20260909-001"
  }
}
```

### `POST payment/transaction/topup_trx` — klaspayTopupTrx
**Parameter:** `data: Any` (body)
**Body:** dinamis — kemungkinan `transaction_id_inquiry` dan `pin`. ⚠️ perlu verifikasi manual.
**Response:** `KlaspayBayarResponse`
```json
{
  "status": "success",
  "message": "",
  "data": {
    "transaction_id": "TRX-20260909-002",
    "customer_id": "5001",
    "customer_phone": "081234567890",
    "customer_email": "muhammadhusnunniam20@gmail.com",
    "bank_name": "BCA",
    "retail_code": "",
    "bank_image": "https://cdn.diskola.id/payment/bca.png",
    "note": "Topup Klaspay",
    "admin_fee": 2500,
    "amount": 100000,
    "total_amount": 102500,
    "virtual_account": "7001234567890123",
    "payment_code": "7001234567890123",
    "partner_reff2": "",
    "status": "pending",
    "created_at": "2026-09-09 10:00:00",
    "paid_at": "",
    "expired": "2026-09-10 10:00:00",
    "guidance": []
  }
}
```

### `GET payment/channel/guide/{channelMethodName}` — payGuide
**Parameter:** `channelMethodName: String` (path)
**Response:** `GuidanceResponse`
```json
{
  "status": "success",
  "message": "",
  "data": {
    "ATM": [
      "Masukkan kartu ATM dan PIN",
      "Pilih menu Transfer > Virtual Account",
      "Masukkan nomor virtual account dan konfirmasi"
    ]
  }
}
```

### `GET payment/transaction/invoice` — klaspayInvoice
**Parameter:** tidak ada
**Response:** `KlaspayInvoiceResponse`
```json
{
  "rc": "00",
  "rd": "Success",
  "data": {
    "transaction_invoice": [
      {
        "transaction_id": "TRX-20260909-002",
        "product_id": "TOPUP",
        "transaction_amount": 102500,
        "payment_method": "Virtual Account BCA",
        "transaction_note": "Topup Klaspay",
        "status": "pending",
        "created_at": "2026-09-09 10:00:00",
        "reff_transaction_id": null,
        "transaction_type": "topup",
        "channel_id": 1,
        "response_processed": "",
        "expired_date": "2026-09-10 10:00:00",
        "payment_code": "7001234567890123",
        "cancelable": true,
        "is_paid": false,
        "details": {
          "transaction_id": "TRX-20260909-002",
          "customer_id": "5001",
          "customer_phone": "081234567890",
          "customer_email": "muhammadhusnunniam20@gmail.com",
          "bank_name": "BCA",
          "retail_code": "",
          "bank_image": "https://cdn.diskola.id/payment/bca.png",
          "note": "Topup Klaspay",
          "admin_fee": 2500,
          "amount": 100000,
          "total_amount": 102500,
          "virtual_account": "7001234567890123",
          "payment_code": "7001234567890123",
          "partner_reff2": "",
          "status": "pending",
          "created_at": "2026-09-09 10:00:00",
          "paid_at": "",
          "expired": "2026-09-10 10:00:00",
          "guidance": []
        }
      }
    ]
  }
}
```

### `POST payment/transaction/spp_cancel_invoice` — klaspayCancelInvoice
**Parameter:** `data: Any` (body)
**Body:** dinamis — kemungkinan `transaction_id`. ⚠️ perlu verifikasi manual.
**Response:** `Any`. ⚠️ perlu verifikasi manual.

### `POST payment/transaction/transaction_spp` — klaspaySppInq
**Parameter:** `data: Any` (body)
**Body:** dinamis — kemungkinan `invoice_ids`, `channel_method_id` (inquiry pembayaran SPP via Klaspay). ⚠️ perlu verifikasi manual.
**Response:** `Any`. ⚠️ perlu verifikasi manual.
