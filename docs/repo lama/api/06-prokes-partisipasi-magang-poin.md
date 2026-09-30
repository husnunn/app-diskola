# API Contract — Prokes, Partisipasi, Magang & Poin

Sumber: `app/src/main/java/id/diskola/app/api/ApiService.kt` baris 1352–1591 (endpoint aktif, non-comment).
Model response dicari di `app/src/main/java/id/diskola/app/pages/**`.

Total endpoint aktif yang didokumentasikan pada rentang ini: **54**.

---

## Dana Partisipasi & Klaspay

### `GET payment/bill/history/id/{id}` — listPaymentDanaPartisipasi
**Parameter:** `id: String` (Path)
**Response:** `ListPartisipasiDetailResponse` (`app/src/main/java/id/diskola/app/pages/partisipasi/PartisipasiModels.kt`)
```json
{
  "data": {
    "bill_id": "BILL-00123",
    "bill_name": "Dana Partisipasi Study Tour Kelas XII",
    "parent_bill_id": "",
    "target_nominal": 1500000,
    "paid_nominal": 500000,
    "wallet_id": "WLT-9081",
    "is_active": true,
    "is_expired": false,
    "expired_date": "2026-12-31",
    "created_date": "2026-08-01",
    "updated_date": "2026-09-01",
    "bill_status": "Belum Lunas",
    "list_child": [
      {
        "transaction_id": "TRX-4521",
        "channel": "va_bca",
        "channel_name": "BCA Virtual Account",
        "nominal": 500000,
        "created_date": "2026-09-01",
        "created_date_t": "2026-09-01T10:00:00Z",
        "is_expired": false
      }
    ]
  }
}
```

### `GET dana-partisipasi/school/{schoolId}/student` — getListStudentKlaspay
**Parameter:** `schoolId: String` (Path)
**Response:** `ContactResponse` (`app/src/main/java/id/diskola/app/pages/chat/ChatModels.kt`)
```json
{
  "data_list": [
    {
      "user_id": 145,
      "user_uuid": "usr-uuid-145",
      "foto": "https://.../avatar.png",
      "wallet_id": "WLT-9081",
      "nisn": "0051234567",
      "name": "Andi Wijaya",
      "kelas": "XII IPA 1",
      "jurusan": "IPA"
    }
  ]
}
```

### `GET payment/merchant/id/{merchantId}` — merchantKlaspay
**Parameter:** `merchantId: Int` (Path)
**Response:** `MerchantKlaspayInfoResponse` (`app/src/main/java/id/diskola/app/pages/sekolah/store/StoreModels.kt`)
```json
{
  "rc": "00",
  "rd": "Success",
  "data": {
    "wallet_id": "WLT-2231",
    "merchant_id": 88,
    "chat_id": "CHAT-771"
  }
}
```

---

## E-Commerce (Enterpreneur), Lokasi & Alamat Pengiriman

### `GET mobile/enterpreneur/checkouts/shipping-fee-list` — getShipping
**Parameter:** `products: List<Int>` (Query `product[]`)
**Response:** `ListShipResponse` (`app/src/main/java/id/diskola/app/pages/sekolah/store/StoreModels.kt`)
```json
{
  "data": [
    {
      "id": "SHIP-1",
      "courier_id": 5,
      "courier_name": "JNE",
      "name": "REG",
      "description": "Reguler 2-3 hari",
      "cost": 15000,
      "estimation_day": "2-3",
      "note": "",
      "information": ""
    }
  ]
}
```

### `POST mobile/enterpreneur/checkouts/transaction-create` — buyProduct
**Parameter:** `data: Any` (Body)
**Body:** dinamis (tidak bertipe di kode lama) — tebakan field: `{ "cart_ids": [1,2,3], "shipping_id": "SHIP-1", "payment_method": "va_bca", "address_id": "1" }` ⚠️ perlu verifikasi manual
**Response:** `Any` (tidak bertipe di kode lama) ⚠️ perlu verifikasi manual

### `GET mobile/enterpreneur/location/province` — listProvinces
**Parameter:** (tidak ada)
**Response:** `ProvinceResponse` (`app/src/main/java/id/diskola/app/pages/sekolah/store/CheckoutModels.kt`)
```json
{
  "data": [
    { "id": "31", "name": "DKI Jakarta", "latitude": -6.2, "longitude": 106.8 }
  ]
}
```

### `GET mobile/enterpreneur/location/city` — listCities
**Parameter:** `provinceId: String` (Query `province_id`), `limit: Int` (Query), `offset: Int` (Query)
**Response:** `CityResponse`
```json
{
  "data": [
    { "id": "3171", "name": "Jakarta Selatan", "province_id": "31", "latitude": -6.29, "longitude": 106.82 }
  ]
}
```

### `GET mobile/enterpreneur/location/district` — listDistrict
**Parameter:** `cityId: String` (Query `city_id`), `limit: Int` (Query), `page: Int` (Query)
**Response:** `DistrictResponse`
```json
{
  "data": [
    { "id": "317101", "name": "Kebayoran Baru", "city_id": "3171", "province_id": "31", "latitude": -6.24, "longitude": 106.79 }
  ]
}
```

### `GET mobile/app/accounts/user/shipping-address` — getUserAddress
**Parameter:** (tidak ada)
**Response:** `AddressResponse`
```json
{
  "data": {
    "name": "Budi Santoso",
    "address": "Jl. Melati No. 10",
    "province": { "id": "31", "name": "DKI Jakarta", "latitude": -6.2, "longitude": 106.8 },
    "city": { "id": "3171", "name": "Jakarta Selatan", "province_id": "31", "latitude": -6.29, "longitude": 106.82 },
    "sub_district": { "id": "317101", "name": "Kebayoran Baru", "city_id": "3171", "province_id": "31", "latitude": -6.24, "longitude": 106.79 }
  }
}
```

### `POST mobile/app/accounts/user/shipping-address` — setUserAddress
**Parameter:** `data: Any` (Body)
**Body:** dinamis (tidak bertipe di kode lama) — tebakan field: `{ "name": "Budi Santoso", "address": "Jl. Melati No. 10", "province_id": "31", "city_id": "3171", "sub_district_id": "317101" }` ⚠️ perlu verifikasi manual
**Response:** `Any` (tidak bertipe di kode lama) ⚠️ perlu verifikasi manual

---

## Kartu Pelajar (Student Card)

### `GET mobile/app/accounts/student-card-template` — idCardTemplate
**Parameter:** (tidak ada)
**Response:** `TemplateResponse` (`app/src/main/java/id/diskola/app/pages/studentcard/StudentCardModels.kt`)
```json
{
  "data": {
    "id": 3,
    "theme": "theme_3",
    "sign_file": "https://.../sign.png",
    "back_title": "Kartu Pelajar",
    "back_content": "Jika kartu ini ditemukan, mohon dikembalikan ke sekolah.",
    "instruction": "Simpan kartu ini baik-baik"
  },
  "school": {
    "address": "Jl. Pendidikan No. 1, Jakarta"
  }
}
```

### `POST mobile/app/accounts/user/student-card` — updateCard (Multipart)
**Parameter:** `data: Map<String, RequestBody>` (PartMap, dinamis), `file: MultipartBody.Part?` (Part, opsional — foto kartu)
**Body PartMap:** dinamis (tidak bertipe di kode lama) — tebakan field berdasarkan `TemplateData`: `theme`, `back_title`, `back_content`, `instruction` ⚠️ perlu verifikasi manual
**Response:** `Any` (tidak bertipe di kode lama) ⚠️ perlu verifikasi manual

---

## Prokes / Screening Kesehatan
(fitur legacy, sudah tidak aktif di UI produksi, tetap didata untuk kelengkapan kontrak API)

### `GET mobile/app/learning/health-protocols/student-form-early-detection` — getformProkesStudent
**Parameter:** (tidak ada)
**Response:** `ResponseEarlyDetectionProkes` (`app/src/main/java/id/diskola/app/pages/prokes/ProkesModels.kt`)
```json
{
  "message": "success",
  "data": {
    "setting_globals_lable": "Deteksi Dini Kesehatan",
    "setting_globals_name": "health_early_detection",
    "setting_globals_value": {
      "feel_indication": [
        { "key": "fever", "text": "Demam" },
        { "key": "cold", "text": "Pilek" }
      ],
      "history_of_illness": [
        { "key": "asthma", "text": "Asma" }
      ],
      "way_of_travel": [
        { "key": "walk", "text": "Jalan Kaki" }
      ],
      "public_transportion_choice": [
        { "key": "bus", "text": "Bus" }
      ]
    }
  }
}
```

### `POST mobile/app/learning/health-protocols/save-report` — sendProksesStudent
**Parameter:** `data: Any` (Body)
**Body:** dinamis (tidak bertipe di kode lama) — tebakan field berdasarkan `cekStudentItem`: `{ "way_of_travel": "walk", "public_transportion_choice": ["bus"], "temperature": "36.5", "feel_indication": ["none"], "history_of_illness": "none" }` ⚠️ perlu verifikasi manual
**Response:** `ResponseCheckReport`
```json
{
  "error": "",
  "message": "success",
  "data": {
    "date_input": "2026-09-01",
    "time_input": "07:30",
    "target_type": "student",
    "target_id": 145,
    "check": {
      "student": {
        "way_of_travel": { "key": "walk", "text": "Jalan Kaki" },
        "already_vaccinated": true,
        "public_transportion_choice": [
          { "key": "bus", "text": "Bus" }
        ],
        "temperature": "36.5",
        "feel_indication": [
          { "key": "none", "text": "Tidak ada keluhan" }
        ],
        "history_of_illness": { "key": "none", "text": "Tidak ada riwayat" }
      },
      "surveyor": "Siti Aminah",
      "vaccinated": true,
      "staff": {
        "email": "guru@sekolah.sch.id",
        "user_id": 12,
        "user_name": "Siti Aminah"
      }
    },
    "updated_at": "2026-09-01T07:30:00Z",
    "created_at": "2026-09-01T07:30:00Z",
    "id": 981,
    "vaccinated": true
  }
}
```

### `GET mobile/app/learning/health-protocols/check-report` — cekProkesStudent
**Parameter:** (tidak ada)
**Response:** `ResponseCheckReport` (struktur sama seperti `sendProksesStudent` di atas)

### `GET mobile/app/learning/health-protocols/check-vaccinated` — cekVaksinasi
**Parameter:** (tidak ada)
**Response:** `ResponseCheckReport` (struktur sama seperti di atas; field yang relevan `vaccinated`)

### `POST mobile/app/learning/health-protocols/save-vaccinated` — saveVaksinasi
**Parameter:** `data: Any` (Body)
**Body:** dinamis (tidak bertipe di kode lama) — tebakan field: `{ "vaccinated": true }` ⚠️ perlu verifikasi manual
**Response:** `ResponseCheckReport` (struktur sama seperti di atas)

### `GET mobile/app/learning/health-protocols/check-vaccinated-teacher` — cekVaksinasiTeacher
**Parameter:** (tidak ada)
**Response:** `ResponseCheckReport` (struktur sama seperti di atas, `target_type: "teacher"`)

### `POST mobile/app/learning/health-protocols/save-vaccinated-teacher` — saveVaksinasiTeacher
**Parameter:** `data: Any` (Body)
**Body:** dinamis (tidak bertipe di kode lama) — tebakan field: `{ "vaccinated": true }` ⚠️ perlu verifikasi manual
**Response:** `ResponseCheckReport` (struktur sama seperti di atas)

### `GET mobile/app/learning/health-protocols/list-school-class-teacher` — listClass
**Parameter:** (tidak ada)
**Response:** `ListClassResponse`
```json
{
  "data": [
    {
      "id": 7,
      "name": "XII IPA 1",
      "grade": 12,
      "major": { "id": 1, "name": "IPA" },
      "total_student": "32",
      "total_student_screening": "20",
      "total_student_remaining": "12"
    }
  ]
}
```

### `GET mobile/app/learning/health-protocols/list-school-class-student/{idClass}` — listStudent
**Parameter:** `idClass: Int` (Path)
**Response:** `ListStudentResponse`
```json
{
  "data": [
    {
      "id": 145,
      "classId": 7,
      "name": "Andi Wijaya",
      "majorName": "IPA",
      "nisn_nik": "0051234567",
      "nisn": "0051234567",
      "nis": "2101234",
      "user_avatar_image": "https://.../avatar.png",
      "already_screening": false
    }
  ]
}
```

### `GET mobile/app/learning/health-protocols/teacher-form-early-detection` — teacherFormEarlyDetetion
**Parameter:** (tidak ada)
**Response:** `ResponseEarlyDetectionProkes` (struktur sama seperti `getformProkesStudent`)

### `POST mobile/app/learning/health-protocols/save-history-report/{studentId}` — saveScreening
**Parameter:** `studentId: Int` (Path), `data: Any` (Body)
**Body:** dinamis (tidak bertipe di kode lama) — tebakan field berdasarkan `cekStudentItem`/`ScreningResponse`: `{ "temperature": "36.5", "feel_indication": ["fever"], "history_of_illness": "none" }` ⚠️ perlu verifikasi manual
**Response:** `SaveScreeningResp`
```json
{ "indication_of_covid": false }
```

### `GET mobile/app/learning/health-protocols/check-screening-student` — screeningCheck
**Parameter:** (tidak ada)
**Response:** `ScreeningCheckResponse`
```json
{
  "message": "success",
  "data": { "total_student": 32, "total_data": 20 }
}
```

### `GET mobile/app/learning/health-protocols/check-history-report/{studentId}` — historyReportStudent
**Parameter:** `studentId: Int` (Path)
**Response:** `ResponseCheckReport` (struktur sama seperti `sendProksesStudent`)

---

## Pairing Akun

### `GET mobile/app/accounts/parent/pair-lists` — pairingList
**Parameter:** (tidak ada)
**Response:** `listPairingResponse` (`app/src/main/java/id/diskola/app/pages/akun/PairingModel.kt`)
```json
{
  "data": [
    {
      "id": 4,
      "status": "pending",
      "user": {
        "id": 20,
        "uuid": "usr-uuid-20",
        "name": "Ani Lestari",
        "email": "ani@gmail.com",
        "nisn_nik": "",
        "nis_nik": "",
        "phone": "081234567890",
        "user_avatar_image": "https://.../avatar_ani.png",
        "user_username": "ani.lestari"
      },
      "student": {
        "id": 145,
        "nisn": "0051234567",
        "nis": "2101234",
        "name": "Andi Wijaya",
        "student_class": { "id": 7, "name": "XII IPA 1", "grade": 12 },
        "user": {
          "id": 145,
          "uuid": "usr-uuid-145",
          "name": "Andi Wijaya",
          "user_avatar_image": "https://.../avatar.png"
        }
      },
      "student_uuid": "std-uuid-9",
      "otp": "482913"
    }
  ]
}
```

### `POST mobile/app/accounts/parent/accept-pair/{id}` — acceptPairing
**Parameter:** `id: Int` (Path)
**Response:** `acceptPairingResponse`
```json
{
  "data": {
    "id": 4,
    "status": "accepted",
    "user": {
      "id": 20,
      "uuid": "usr-uuid-20",
      "name": "Ani Lestari",
      "email": "ani@gmail.com"
    },
    "student": {
      "id": 145,
      "nisn": "0051234567",
      "nis": "2101234",
      "name": "Andi Wijaya",
      "student_class": { "id": 7, "name": "XII IPA 1", "grade": 12 }
    },
    "student_uuid": "std-uuid-9",
    "otp": "482913"
  }
}
```

---

## Magang

### `GET mobile/internship/schedule` — magangSchedule
**Parameter:** (tidak ada)
**Response:** `MagangResponse` (`app/src/main/java/id/diskola/app/pages/magang/MagangModel.kt`)
```json
{
  "data": [
    {
      "id": 12,
      "company": {
        "id": 3,
        "name": "PT Teknologi Nusantara",
        "address": "Jl. Sudirman No. 45, Jakarta",
        "latitude": "-6.2088",
        "longitude": "106.8456",
        "radius": "50"
      },
      "schedule": {
        "date": "2026-09-09",
        "start_time": "08:00",
        "end_time": "16:00"
      },
      "attendance": {
        "status": "not_yet",
        "can_attend": true,
        "can_leave": false,
        "can_submit_leave_request": true,
        "leave_request": null,
        "radius_enabled": true
      }
    }
  ]
}
```

### `POST mobile/internship/attend` — attendMagang
**Parameter:** `data: Any` (Body)
**Body:** dinamis (tidak bertipe di kode lama) — tebakan field: `{ "schedule_id": 12, "latitude": -6.2088, "longitude": 106.8456 }` ⚠️ perlu verifikasi manual
**Response:** (tidak ada body / `Unit`)

### `GET mobile/internship/attendance` — magangReport
**Parameter:** (tidak ada)
**Response:** `MagangReportResp`
```json
{
  "data": [
    {
      "record_type": "attendance",
      "id": 55,
      "date": "2026-09-08",
      "status": "hadir",
      "note": "",
      "can_delete": false,
      "check_in_at": "08:02",
      "check_in_status": "on_time",
      "check_out_at": "16:05",
      "check_out_status": "on_time",
      "file": "",
      "daily_report": "Membantu tim support IT menangani tiket pelanggan",
      "location": { "latitide": -6.2088, "latitude": -6.2088, "longitude": 106.8456 },
      "company": {
        "name": "PT Teknologi Nusantara",
        "address": "Jl. Sudirman No. 45, Jakarta",
        "latitude": "-6.2088",
        "longitude": "106.8456",
        "radius": "50"
      }
    }
  ]
}
```

### `POST mobile/internship/leave` — reportMagang (Multipart)
**Parameter:** `id: RequestBody` (Part `id`), `daily_report: RequestBody` (Part `daily_report`), `file: MultipartBody.Part?` (Part, opsional — bukti/lampiran laporan harian)
**Response:** (tidak ada body / `Unit`)

### `POST mobile/internship/leave-request` — magangSubmitLeaveRequest (Multipart)
**Parameter:** `id: RequestBody` (Part `id`), `status: RequestBody` (Part `status` — mis. izin/sakit), `note: RequestBody` (Part `note`), `file: MultipartBody.Part` (Part, wajib — surat izin/sakit)
**Response:** `id.diskola.app.pages.magang.MagangLeaveRequestSubmitResponse`
```json
{
  "data": {
    "record_type": "leave_request",
    "id": 9,
    "date": "2026-09-09",
    "status": "pending",
    "note": "Sakit demam",
    "file": "https://.../surat_izin.pdf",
    "can_delete": true,
    "company": {
      "name": "PT Teknologi Nusantara",
      "address": "Jl. Sudirman No. 45, Jakarta",
      "latitude": "-6.2088",
      "longitude": "106.8456",
      "radius": "50"
    }
  }
}
```

### `DELETE mobile/internship/leave-request/{id}` — magangDeleteLeaveRequest
**Parameter:** `id: Long` (Path)
**Response:** (tidak ada body / `Unit`)

---

## Try Out

### `GET mobile/app/learning/try-out/passing-grade` — listUniversity
**Parameter:** (tidak ada)
**Response:** `UniversityResponse` (`app/src/main/java/id/diskola/app/pages/tryout/TryoutModels.kt`)
```json
{
  "data": [
    { "id": 1, "name": "Universitas Indonesia", "saintek": 650.5, "soshum": 620.3 }
  ]
}
```

### `GET mobile/app/learning/try-out/scored/{id}/explanation` — getTryoutExplanation
**Parameter:** `id: Int` (Path)
**Response:** `ListTryoutExplanationResponse`
```json
{
  "data": {
    "list_instruction": [
      {
        "schedule_id": 10,
        "instruction_id": 1,
        "instruction_number": 1,
        "instruction_text": "Bacalah teks berikut dengan saksama",
        "instruction_description": "Petunjuk umum pengerjaan soal"
      }
    ],
    "student_answer": [
      {
        "schedule_id": 10,
        "instruction_id": 1,
        "instruction_text": "Bacalah teks berikut dengan saksama",
        "instruction_description": "Petunjuk umum pengerjaan soal",
        "answer_id": 101,
        "answer_text": "B. Fotosintesis",
        "answer_isTrue": true,
        "answer_answered": 1,
        "answer_filePath": "",
        "question": "Proses pembuatan makanan pada tumbuhan disebut...",
        "question_filePath": "",
        "question_answerkey": "B",
        "question_answerkey_filePath": "",
        "explanation": "Fotosintesis adalah proses tumbuhan mengolah makanan menggunakan cahaya matahari.",
        "explanation_image": ""
      }
    ]
  }
}
```

---

## Poin / Konseling

### `GET mobile/konseling/list-violation` — listViolation
**Parameter:** `take: Int` (Query), `skip: Int` (Query), `search: String = ""` (Query `name`)
**Response:** `ListViolationResponse` (`app/src/main/java/id/diskola/app/pages/poin/PoinModels.kt`)
```json
{
  "data": [
    { "violation_id": 1, "violation_name": "Terlambat masuk sekolah", "violation_score": "5" }
  ]
}
```

### `GET mobile/konseling/list-achievement` — listAchievement
**Parameter:** `take: Int` (Query), `skip: Int` (Query), `search: String = ""` (Query `name`)
**Response:** `ListAchievementResponse`
```json
{
  "data": [
    { "achievement_id": 1, "achievement_name": "Juara 1 Lomba Matematika", "achievement_score": "10" }
  ]
}
```

### `GET mobile/konseling/list-handling` — listHandling
**Parameter:** `take: Int` (Query), `skip: Int` (Query), `search: String = ""` (Query `name`)
**Response:** `ListHandlingResponse`
```json
{
  "data": [
    { "id": 3, "name": "Pemanggilan Orang Tua" }
  ]
}
```

### `GET mobile/konseling/score-student` — getScoredPoinStudent
**Parameter:** (tidak ada)
**Response:** `StudentPoinListResponse`
```json
{
  "data": [
    {
      "violation": {
        "violation_recap": {
          "violation_score": 15,
          "recap": [
            { "name_of_violation": "Terlambat masuk sekolah", "number_of_violations": 3, "number_of_scores": 15 }
          ]
        },
        "violation_detail": [
          {
            "violation_id": 1,
            "violation_name": "Terlambat masuk sekolah",
            "violation_score": 5,
            "violation_image": "",
            "violation_message": "Terlambat 15 menit",
            "date": "2026-09-01",
            "time": "07:15",
            "creator_name": "Budi Guru",
            "creator_nip": "198501012010011001"
          }
        ]
      },
      "achievement": {
        "achievement_recap": {
          "achievement_score": 10,
          "recap": [
            { "name_of_achievement": "Juara 1 Lomba Matematika", "number_of_achievements": 1, "number_of_scores": 10 }
          ]
        },
        "achievement_detail": [
          {
            "achievement_id": 1,
            "achievement_name": "Juara 1 Lomba Matematika",
            "achievement_score": 10,
            "achievement_image": "",
            "achievement_message": "Mewakili sekolah di tingkat provinsi",
            "date": "2026-08-20",
            "time": "10:00",
            "creator_name": "Siti Guru",
            "creator_nip": "198702022011012002"
          }
        ]
      },
      "notHandled": [
        {
          "id": 8,
          "score_student_id": 145,
          "violation_score": 15,
          "school_handling_id": 3,
          "calling_at": "2026-09-05",
          "calling_name": "Pemanggilan Orang Tua",
          "calling_message": "Mohon hadir ke sekolah",
          "handling_at": "",
          "handling_message": "",
          "handled": 0
        }
      ]
    }
  ]
}
```

### `GET mobile/konseling/score` — searchScoredPoinStudent
**Parameter:** `nisn: String?` (Query, opsional), `name: String?` (Query, opsional)
**Response:** `SearchPoinStudentResponse`
```json
{
  "data": [
    {
      "id": 145,
      "name": "Andi Wijaya",
      "user_avatar_image": "https://.../avatar.png",
      "role": "student",
      "student_class": "XII IPA 1",
      "nisn": "0051234567",
      "violation_score": 15,
      "achievement_score": 10
    }
  ]
}
```

### `POST mobile/konseling/create/violation` — postStudentViolation (Multipart)
**Parameter:** `user_id: RequestBody` (Part `user_id`), `school_violation_id: RequestBody` (Part `school_violation_id`), `violation_message: RequestBody` (Part `violation_message`), `file: MultipartBody.Part?` (Part, opsional — foto bukti)
**Response:** (tidak ada body / `Unit`)

### `POST mobile/konseling/create/achievement` — postStudentAchievement (Multipart)
**Parameter:** `user_id: RequestBody` (Part `user_id`), `school_achievement_id: RequestBody` (Part `school_achievement_id`), `achievement_message: RequestBody` (Part `achievement_message`), `file: MultipartBody.Part?` (Part, opsional — foto bukti)
**Response:** (tidak ada body / `Unit`)

### `POST mobile/konseling/create/calling-student` — postStudentHandling
**Parameter:** `data: Any` (Body)
**Body:** dinamis (tidak bertipe di kode lama) — tebakan field berdasarkan `StudentPoinHandlingItem`: `{ "score_student_id": 145, "school_handling_id": 3, "calling_message": "Mohon hadir ke sekolah", "calling_at": "2026-09-05" }` ⚠️ perlu verifikasi manual
**Response:** (tidak ada body / `Unit`)

---

## Presensi & Jurnal (termasuk QR)

### `GET mobile/attendance/list-class` — listClass
**Parameter:** `take: Int` (Query), `skip: Int` (Query), `search: String = ""` (Query `name`)
**Response:** `ListClassItemResponse` (`app/src/main/java/id/diskola/app/pages/presensi/PresensiModel.kt`)
```json
{ "data": [ { "id": 7, "name": "XII IPA 1", "grade": 12 } ] }
```

### `GET mobile/attendance/list-subject` — listSubject
**Parameter:** `take: Int` (Query), `skip: Int` (Query), `search: String = ""` (Query `name`)
**Response:** `ListSubjectItemResponse`
```json
{ "data": [ { "id": 3, "name": "Matematika" } ] }
```

### `GET mobile/attendance/list-teacher` — listTeacher
**Parameter:** `take: Int` (Query), `skip: Int` (Query), `search: String = ""` (Query `name`)
**Response:** `ListTeacherItemResponse`
```json
{
  "data": [
    { "id": 12, "name": "Budi Santoso", "nip": "198501012010011001", "user": { "id": "usr-uuid-12" } }
  ]
}
```

### `GET mobile/attendance/list-plot` — listPlot
**Parameter:** (tidak ada)
**Response:** `ListPlotItemResponse`
```json
{
  "data": [
    { "time_plot_id": 1, "time_plot_day": "Senin", "time_plot_start_at": "07:00", "time_plot_end_at": "07:45" }
  ]
}
```

### `POST mobile/attendance/journal` — postJournalTeacher
**Parameter:** `data: Any` (Body)
**Body:** dinamis (tidak bertipe di kode lama) — tebakan field: `{ "school_subject_schedule_id": 88, "learning_objective": "Memahami konsep integral" }` ⚠️ perlu verifikasi manual
**Response:** `JournalCreateResponse`
```json
{
  "data": [
    {
      "attendance_id": 501,
      "plot_start_at": "07:00",
      "plot_end_at": "07:45",
      "learning_objective": "Memahami konsep integral",
      "status": "berlangsung",
      "subject_name": "Matematika",
      "class_name": "XII IPA 1"
    }
  ]
}
```

### `GET mobile/attendance/journal/{scheduleId}` — journalPreview
**Parameter:** `scheduleId: Int` (Path), `date: String?` (Query, opsional)
**Response:** `JournalPreviewResponse`
```json
{
  "data": {
    "attendance_id": 501,
    "school_subject_schedule_id": 88,
    "date": "2026-09-09",
    "subject_name": "Matematika",
    "class_name": "XII IPA 1",
    "learning_objective": "Memahami konsep integral",
    "session_status": "berlangsung",
    "is_started": true,
    "students": [
      {
        "student_id": 145,
        "name": "Andi Wijaya",
        "nisn": "0051234567",
        "status": "hadir",
        "status_source": "manual",
        "attend_at": "07:05",
        "is_late": false,
        "is_overridable": true
      }
    ]
  }
}
```

### `POST mobile/attendance/journal/save` — saveAttendanceJournal
**Parameter:** `data: JournalSaveRequest` (Body — bertipe)
```json
{
  "school_subject_schedule_id": 88,
  "learning_objective": "Memahami konsep integral",
  "session_status": "selesai",
  "students": [
    { "student_id": 145, "status": "hadir" }
  ]
}
```
**Response:** `JournalSaveResponse`
```json
{
  "data": {
    "attendance_id": 501,
    "school_subject_schedule_id": 88,
    "start_at": "07:00",
    "end_at": "07:45",
    "late_limit": 15,
    "teacher_name": "Budi Santoso",
    "teacher_nip": "198501012010011001",
    "school_name": "SMA Diskola 1",
    "school_major_name": "IPA",
    "subject_name": "Matematika",
    "subject_image": "https://.../math.png",
    "class_name": "XII IPA 1",
    "status": "selesai",
    "grade": 12,
    "learning_objective": "Memahami konsep integral",
    "time_plot": "07:00 - 07:45",
    "plot_start_at": "07:00",
    "plot_end_at": "07:45",
    "total_student": "32",
    "student_attendances": [
      {
        "student": { "id": 145, "name": "Andi Wijaya", "nisn": "0051234567", "nis": "2101234" },
        "date": "2026-09-09",
        "attend_at": "07:05",
        "leave_at": "",
        "status": "hadir",
        "status_source": "manual",
        "is_late": false
      }
    ],
    "total_attended_student": 30,
    "status_breakdown": null
  }
}
```

### `POST mobile/attendance/student/journal-student` — postJournalStudent
**Parameter:** `data: Any` (Body)
**Body:** dinamis (tidak bertipe di kode lama) — tebakan field: `{ "school_subject_schedule_id": 88, "status": "hadir" }` ⚠️ perlu verifikasi manual
**Response:** `JournalCreateResponse` (struktur sama seperti `postJournalTeacher`)

### `POST mobile/attendance/student/journal-check-in` — studentJournalCheckIn
**Parameter:** `data: Any` (Body)
**Body:** dinamis (tidak bertipe di kode lama) — tebakan field: `{ "school_subject_schedule_id": 88, "latitude": -6.2088, "longitude": 106.8456 }` ⚠️ perlu verifikasi manual
**Response:** `Any` (tidak bertipe di kode lama) ⚠️ perlu verifikasi manual

### `POST mobile/attendance/student/learning/qr` — postQrAttendance
**Parameter:** `body: Any` (Body)
**Body:** dinamis (tidak bertipe di kode lama) — tebakan field: `{ "qr_code": "QR-88-20260909", "school_subject_schedule_id": 88 }` ⚠️ perlu verifikasi manual
**Response:** `Response<Unit>` (hanya status HTTP, tidak ada body JSON tetap) ⚠️ perlu verifikasi manual

### `POST mobile/attendance/check-in/student` — teacherPresent
**Parameter:** `data: Any` (Body)
**Catatan kode:** komentar di source menyebut endpoint ini hanya toggle `attend_at`; status penuh (izin/sakit/alpha) harus lewat `saveAttendanceJournal`.
**Body:** dinamis (tidak bertipe di kode lama) — tebakan field: `{ "school_subject_schedule_id": 88, "attend_at": "07:05" }` ⚠️ perlu verifikasi manual
**Response:** `Any` (tidak bertipe di kode lama) ⚠️ perlu verifikasi manual
