# API Contract — Pembayaran, Klaspay, PPOB & Asesmen (AKM)

Sumber: `app/src/main/java/id/diskola/app/api/ApiService.kt` baris 812–1082.
Endpoint yang di-comment (`//`) TIDAK didokumentasikan di sini (dianggap non-aktif / mock lama).

Catatan umum:
- Anotasi `@NullToEmptyString` pada field model berarti backend boleh mengirim `null` dan akan dikonversi ke `""` oleh Moshi adapter kustom — tidak mengubah bentuk JSON.
- Banyak endpoint transaksi memakai `@Body data: Any` (body dinamis, tidak bertipe di kode lama). Untuk endpoint tersebut, skema body TIDAK dikarang — hanya ditandai perlu verifikasi manual ke backend/dokumentasi lain sebelum dipakai di repo baru.
- Response type yang sama dipakai berulang di beberapa endpoint (mis. `InqCheckResponse`, `PdamResponse`) — field JSON konsisten, hanya isi datanya kontekstual mengikuti nama fungsi.

---

## Pembayaran & Transaksi Umum

### `GET payment/transaction/history` — paymentHistory
**Parameter:**
- `@Query("page") page: Int`
- `@Query("pageSize") pageSize: Int`

**Response:** `PaymentHistoryResponse`
```json
{
  "status": "success",
  "message": "Data riwayat transaksi berhasil diambil",
  "data": {
    "transaction": [
      {
        "transaction_id": "TRX20240915000123",
        "price": 150000,
        "priceLabel": "Rp150.000",
        "type": "ppob_pulsa",
        "transaction_status": "SUCCESS",
        "created_at": "2024-09-15T08:30:00.000Z"
      },
      {
        "transaction_id": "SPP20240901000045",
        "price": 500000,
        "priceLabel": "Rp500.000",
        "type": "spp",
        "transaction_status": "PENDING",
        "created_at": "2024-09-01T07:15:00.000Z"
      }
    ]
  }
}
```

### `GET payment/transaction/history/id/{id}` — paymentHistoryDetail
**Parameter:** `@Path("id") id: String?`

**Response:** `InqCheckResponse`
```json
{
  "status": "success",
  "message": "",
  "data": {
    "transaction_detail": {
      "transaction_id": "TRX20240915000123",
      "price": 20000,
      "total_amount": 20500,
      "fee_service": 500,
      "fee_admin": 0,
      "fee_other": 0,
      "cashback": 0,
      "amount": 20000,
      "type": "pulsa",
      "transaction_status": "SUCCESS",
      "created_at": "2024-09-15T08:30:00.000Z",
      "note": "Pembelian pulsa Telkomsel",
      "paid_date": "2024-09-15T08:31:00.000Z",
      "payment_code": "",
      "payment_method": "Klaspay",
      "payment_method_code": "KLASPAY",
      "service_type": "pulsa",
      "product_name": "Telkomsel 20.000",
      "product_id": "TSEL20",
      "pulsa_provider": "TELKOMSEL",
      "pulsa_type": "prabayar",
      "pulsa_name": "Telkomsel 20.000",
      "nama_pelanggan": "",
      "destination": "081234567890",
      "reff": "",
      "reff2": "",
      "periode": "",
      "nominal": 20000,
      "admin": 500,
      "total_bayar": 20500,
      "sn": "1234567890123456",
      "provider": "TELKOMSEL",
      "standawal": "",
      "standakhir": "",
      "message": ""
    }
  }
}
```
> Catatan: `InqCheckResponseDetail` adalah "gabungan" field lintas kategori PPOB (pulsa/listrik/air/internet/bpjs/game/dll) — field yang tidak relevan dengan kategori transaksi biasanya dikirim kosong/`0` oleh backend.

### `GET payment/channel/spp` — sppPaymentChannels
**Parameter:** (tidak ada)

**Response:** `PaymentChannelResponse`
```json
{
  "data": {
    "virtual account": [
      {
        "channel_method_id": 1,
        "channel_method_name": "BCA Virtual Account",
        "channel_method_category": "virtual account",
        "image_url": "https://cdn.diskola.id/bank/bca.png",
        "payment_url": "",
        "payment_url_v2": "",
        "is_active": true,
        "channel_id": 10
      }
    ],
    "e-money": [
      {
        "channel_method_id": 5,
        "channel_method_name": "OVO",
        "channel_method_category": "e-money",
        "image_url": "https://cdn.diskola.id/emoney/ovo.png",
        "payment_url": "",
        "payment_url_v2": "",
        "is_active": true,
        "channel_id": 20
      }
    ]
  }
}
```
> `data` adalah `Map<String, List<VirtualAccountItem>>` — key-nya adalah kategori channel (`virtual account`, `e-money`, `credit card`, `clickpay`, `modern store`, `bank transfer`, `qris`, dst, lihat `KlaspayToolbarProduct`).

---

## Klaspay — SPP & Tagihan

### `GET payment/transaction/history_school` — sppInvoice
**Parameter:** (tidak ada)

**Response:** `KlaspayTagihanSppResult`
```json
{
  "rc": "00",
  "rd": "Success",
  "data": {
    "transaction_invoice": [
      {
        "transaction_id": "SPP20240901000045",
        "price": 500000,
        "type": "spp",
        "transaction_status": "UNPAID",
        "created_date": "2024-09-01T00:00:00.000Z",
        "invoice": "INV/2024/09/0001",
        "invoice_type": "SPP Bulan September",
        "note": "Tagihan SPP September 2024",
        "is_paid": false,
        "paid_date": "",
        "is_inquiry_channel": true
      }
    ]
  }
}
```

### `POST payment/transaction/spp_trx` — paySpp
**Parameter:** `@Body data: Any`

⚠️ **Body: dinamis (tidak bertipe di kode lama)** — perlu verifikasi manual. Perkiraan field dari konteks (bayar SPP): `transaction_id` / `invoice_id`, `channel_id` atau `payment_method_code`, mungkin `amount`.

**Response:** `Any` — ⚠️ perlu verifikasi manual, tidak ada struktur yang bisa diturunkan dari kode.

### `POST payment/transaction/spp_trx` — paySppChannel
**Parameter:** `@Body data: Any`

⚠️ **Body: dinamis (tidak bertipe di kode lama)** — sama endpoint dengan `paySpp` di atas, kemungkinan varian pemanggilan yang membedakan hanya bentuk parsing response di sisi klien. Perkiraan field: `transaction_id`/`invoice_id`, `channel_id`.

**Response:** `KlaspayBayarResponse`
```json
{
  "status": "success",
  "message": "Transaksi berhasil dibuat",
  "data": {
    "transaction_id": "TRX20240915000200",
    "customer_id": "",
    "customer_phone": "",
    "customer_email": "",
    "bank_name": "BCA",
    "retail_code": "",
    "bank_image": "https://cdn.diskola.id/bank/bca.png",
    "note": "Pembayaran SPP September 2024",
    "admin_fee": 2500,
    "amount": 500000,
    "total_amount": 502500,
    "virtual_account": "39012345678901",
    "payment_code": "",
    "partner_reff2": "",
    "status": "PENDING",
    "created_at": "2024-09-15T09:00:00.000Z",
    "paid_at": "",
    "expired": "2024-09-16T09:00:00.000Z",
    "guidance": [
      {
        "ATM": ["Masukkan kartu ATM", "Pilih menu Transfer", "Masukkan nomor Virtual Account"],
        "MBANGKING": [],
        "IBANKING": [],
        "EDC": [],
        "Kantor": [],
        "ATM Lain": [],
        "Direct": []
      }
    ]
  }
}
```
> Field `guidance` di JSON asli berbentuk objek per kanal (`ATM`, `MBANGKING`, `IBANKING`, `EDC`, `Kantor`, `ATM Lain`, `Direct`), tapi model Kotlin menyatakannya sebagai `List<KlaspayBayarGuidance>` — kemungkinan besar backend hanya mengirim satu objek dan model membungkusnya jadi list satu elemen.

### `POST payment/transaction/bill_trx` — payBill
**Parameter:** `@Body data: Any`

⚠️ **Body: dinamis (tidak bertipe di kode lama)** — perkiraan field (bayar tagihan non-SPP, mis. tagihan sekolah lain): `transaction_id`/`invoice_id`, `channel_id`.

**Response:** `KlaspayBayarResponse` (struktur identik dengan contoh `paySppChannel` di atas).

---

## PPOB — Katalog Produk

### `GET payment/product/list/pulsa/{phone}` — providerPulsa
**Parameter:** `@Path("phone") phone: String`

**Response:** `ListProviderResponse`
```json
{
  "status": "success",
  "message": "",
  "data": {
    "provider": "TELKOMSEL",
    "product": {
      "Pulsa Prabayar": [
        {
          "product_id": "TSEL20",
          "name": "Telkomsel 20.000",
          "price_end": 20500,
          "admin": 500,
          "cashback": 0,
          "provider": "TELKOMSEL",
          "provider_text": "Telkomsel",
          "type": "pulsa",
          "inquiry_code": "TSEL20"
        }
      ],
      "Paket Pulsa": [
        {
          "product_id": "TSELDATA5GB",
          "name": "Telkomsel Data 5GB",
          "price_end": 55000,
          "admin": 500,
          "cashback": 500,
          "provider": "TELKOMSEL",
          "provider_text": "Telkomsel",
          "type": "data",
          "inquiry_code": "TSELDATA5GB"
        }
      ]
    }
  }
}
```
> `data.product` adalah `Map<String, List<PulsaItem>>` — key-nya nama kategori produk (`Pulsa Prabayar`, `Paket Pulsa`, `Paket SMS`, dst).

### `GET payment/product/list/pulsa_pasca/{phone}` — providerPulsaPasca
**Parameter:** `@Path("phone") phone: String`

**Response:** `ListProviderResponse` (struktur sama, kategori contoh `"Pulsa Pascabayar"`).
```json
{
  "status": "success",
  "message": "",
  "data": {
    "provider": "TELKOMSEL",
    "product": {
      "Pulsa Pascabayar": [
        {
          "product_id": "TSELHALO",
          "name": "Kartu Halo",
          "price_end": 0,
          "admin": 2500,
          "cashback": 0,
          "provider": "TELKOMSEL",
          "provider_text": "Telkomsel",
          "type": "pasca",
          "inquiry_code": "TSELHALO"
        }
      ]
    }
  }
}
```

### `GET payment/product/list/pln_prabayar` — listTokenListrik
**Parameter:** (tidak ada)

**Response:** `ListPlnResponse`
```json
{
  "status": "success",
  "message": "",
  "data": [
    {
      "product_id": "PLN20",
      "name": "Token Listrik 20.000",
      "price_end": 20500,
      "admin": 500,
      "inquiry_code": "PLN20"
    },
    {
      "product_id": "PLN50",
      "name": "Token Listrik 50.000",
      "price_end": 50500,
      "admin": 500,
      "inquiry_code": "PLN50"
    }
  ]
}
```

### `GET payment/product/list/pln_pascabayar` — listListrikPasca
**Parameter:** (tidak ada)

**Response:** `ListPlnResponse`
```json
{
  "status": "success",
  "message": "",
  "data": [
    {
      "product_id": "PLNPASCA",
      "name": "Tagihan Listrik Pascabayar",
      "price_end": 0,
      "admin": 2500,
      "inquiry_code": "PLNPASCA"
    }
  ]
}
```

### `GET payment/product/list/pdam` — listPdam
**Parameter:** (tidak ada)

**Response:** `PdamResponse`
```json
{
  "status": "success",
  "message": "",
  "data": [
    {
      "product_id": "PDAM01",
      "name": "PDAM Tirta Sejahtera",
      "price": 0,
      "admin": 2500,
      "inquiry_code": "PDAM01"
    }
  ]
}
```

### `GET payment/product/list/internet` — listInternet
**Parameter:** (tidak ada)

**Response:** `PdamResponse`
```json
{
  "status": "success",
  "message": "",
  "data": [
    {
      "product_id": "INDIHOME20",
      "name": "Indihome 20Mbps",
      "price": 300000,
      "admin": 2500,
      "inquiry_code": "INDIHOME20"
    }
  ]
}
```

### `GET payment/product/list/bpjs` — listBpjs
**Parameter:** (tidak ada)

**Response:** `PdamResponse`
```json
{
  "status": "success",
  "message": "",
  "data": [
    {
      "product_id": "BPJSKES",
      "name": "BPJS Kesehatan",
      "price": 0,
      "admin": 2500,
      "inquiry_code": "BPJSKES"
    }
  ]
}
```

### `GET payment/product/list/game` — listGame
**Parameter:** (tidak ada)

**Response:** `ListGameResponse`
```json
{
  "status": "success",
  "message": "",
  "data": {
    "Mobile Legends": {
      "logo": "https://cdn.diskola.id/game/ml.png",
      "products": [
        {
          "product_id": "ML86",
          "name": "86 Diamonds",
          "price_end": 21000,
          "admin": 500,
          "cashback": 0,
          "provider": "MOONTON",
          "provider_text": "Moonton",
          "type": "game"
        }
      ]
    },
    "Free Fire": {
      "logo": "https://cdn.diskola.id/game/ff.png",
      "products": [
        {
          "product_id": "FF70",
          "name": "70 Diamonds",
          "price_end": 15000,
          "admin": 500,
          "cashback": 0,
          "provider": "GARENA",
          "provider_text": "Garena",
          "type": "game"
        }
      ]
    }
  }
}
```
> `data` adalah `Map<String, GameDetail>` — key-nya nama game.

### `GET payment/product/list/game/voucher` — listVoucherGame
**Parameter:** (tidak ada)

**Response:** `ListGameResponse` (struktur sama, contoh kategori voucher: `"Steam Wallet"`, `"Google Play"`).

### `GET payment/product/list/streaming` — listStream
**Parameter:** (tidak ada)

**Response:** `ListGameResponse` (struktur sama, contoh kategori: `"Netflix"`, `"Spotify Premium"`, `"Vidio"`).

---

## PPOB — Transaksi

### `POST payment/transaction/ppob/trx` — buyPulsa
**Parameter:** `@Body data: Any`

⚠️ **Body: dinamis (tidak bertipe di kode lama)**. Perkiraan field dari konteks (beli pulsa langsung tanpa inquiry): `product_id`, `phone`/`destination`, `service_type` ("pulsa").

**Response:** `InqCheckResponse` (struktur sama dengan contoh `paymentHistoryDetail` di atas, `service_type` = `"pulsa"`).

### `POST payment/transaction/ppob/inq` — ppobInquiry
**Parameter:** `@Body data: Any`

⚠️ **Body: dinamis (tidak bertipe di kode lama)**. Perkiraan field: `product_id`, `destination`/`idpel`, `service_type` (mis. `"pdam"`, `"pln_pascabayar"`, `"bpjs"`).

**Response:** `InqResponse`
```json
{
  "status": "success",
  "message": "",
  "data": {
    "transaction_detail": {
      "transaction_id": "INQ20240915000300",
      "transaction_status": "SUCCESS",
      "note": "Inquiry tagihan PDAM berhasil",
      "product_name": "PDAM Tirta Sejahtera"
    }
  }
}
```

### `POST payment/transaction/ppob/inq_check` — ppobInquiryCheck
**Parameter:** `@Body data: Any`

⚠️ **Body: dinamis (tidak bertipe di kode lama)**. Perkiraan field: `transaction_id` (hasil dari `ppobInquiry`), dipakai untuk cek status/detail tagihan sebelum bayar.

**Response:** `InqCheckResponse`
```json
{
  "status": "success",
  "message": "",
  "data": {
    "transaction_detail": {
      "transaction_id": "INQ20240915000300",
      "price": 150000,
      "total_amount": 152500,
      "fee_service": 2500,
      "fee_admin": 0,
      "fee_other": 0,
      "cashback": 0,
      "amount": 150000,
      "type": "pdam",
      "transaction_status": "PENDING_PAYMENT",
      "created_at": "2024-09-15T08:00:00.000Z",
      "note": "",
      "paid_date": "",
      "payment_code": "",
      "payment_method": "",
      "payment_method_code": "",
      "service_type": "pdam",
      "product_name": "PDAM Tirta Sejahtera",
      "product_id": "PDAM01",
      "pulsa_provider": "",
      "pulsa_type": "",
      "pulsa_name": "",
      "nama_pelanggan": "Andi Saputra",
      "destination": "1234567890",
      "reff": "",
      "reff2": "",
      "periode": "202409",
      "nominal": 150000,
      "admin": 2500,
      "total_bayar": 152500,
      "sn": "",
      "provider": "PDAM",
      "standawal": "1200",
      "standakhir": "1250",
      "message": ""
    }
  }
}
```

### `POST payment/transaction/ppob/inq_pay` — ppobPay
**Parameter:** `@Body data: Any`

⚠️ **Body: dinamis (tidak bertipe di kode lama)**. Perkiraan field: `transaction_id` (dari inquiry_check), konfirmasi bayar pakai saldo Klaspay.

**Response:** `InqCheckResponse` (struktur sama dengan `ppobInquiryCheck`, `transaction_status` biasanya `"SUCCESS"` setelah dibayar).

---

## Asesmen / AKM

### `GET mobile/app/learning/akm/schedules` — listAkm
**Parameter:**
- `@Query("take") take: Int`
- `@Query("skip") skip: Int`

**Response:** `ListAkmResponse`
```json
{
  "data": [
    {
      "id": 12,
      "level": 1,
      "isActive": 1,
      "password": "",
      "date": "2024-09-20",
      "startAt": "2024-09-20T07:00:00.000Z",
      "endAt": "2024-09-20T09:00:00.000Z",
      "status": "scheduled",
      "show_score": true,
      "requires_password": false,
      "password_checked": false,
      "has_explain": false,
      "isDone": false,
      "isAssessed": false,
      "isQueued": false,
      "gov_schedule": false,
      "participant": {
        "uuid": "b3f1a2c4-56de-789f-0123-456789abcdef",
        "name": "Andi Saputra",
        "nisn_nik": "0051234567",
        "school": "SMA Negeri 1 Diskola"
      },
      "exam": {
        "id": 5,
        "name": "AKM Literasi Membaca",
        "type": "akm",
        "grade": 8,
        "author": { "id": 3, "nip": "198501012010011001", "name": "Budi Guru" },
        "numberOfQuestions": 40,
        "instructions": [],
        "score": 0.0,
        "assessment_category": "AKM",
        "assessment_period": "Ganjil 2024/2025"
      },
      "exams": [],
      "score": [],
      "exam_score": -1.0,
      "type": "akm"
    }
  ]
}
```
> `exam` bisa berupa objek tunggal atau `exams` berupa list, tergantung jenis jadwal (AKM biasa vs ujian sekolah dengan banyak mapel).

### `GET mobile/app/learning/akm/scored` — listScoreAkm
**Parameter:** `take: Int`, `skip: Int` (query, sama seperti `listAkm`)

**Response:** `ListAkmResponse` (struktur sama, dengan `isDone=true`, `isAssessed=true`, dan `score` terisi):
```json
{
  "data": [
    {
      "id": 12,
      "level": 1,
      "isActive": 0,
      "password": "",
      "date": "2024-09-20",
      "startAt": "2024-09-20T07:00:00.000Z",
      "endAt": "2024-09-20T09:00:00.000Z",
      "status": "finished",
      "show_score": true,
      "requires_password": false,
      "password_checked": true,
      "has_explain": true,
      "isDone": true,
      "isAssessed": true,
      "isQueued": false,
      "gov_schedule": false,
      "participant": {
        "uuid": "b3f1a2c4-56de-789f-0123-456789abcdef",
        "name": "Andi Saputra",
        "nisn_nik": "0051234567",
        "school": "SMA Negeri 1 Diskola"
      },
      "exam": { "id": 5, "name": "AKM Literasi Membaca", "type": "akm", "grade": 8, "author": null, "numberOfQuestions": 40, "instructions": [], "score": 82.5, "assessment_category": "AKM", "assessment_period": "Ganjil 2024/2025" },
      "exams": [],
      "score": [
        { "id": 5, "name": "AKM Literasi Membaca", "type": "akm", "scored": 82.5 }
      ],
      "exam_score": 82.5,
      "type": "akm"
    }
  ]
}
```

### `GET mobile/app/learning/akm/schedules/{id}` — detailAkm
**Parameter:** `@Path("id") id: Int`

**Response:** `DetailAkmResponse` — sama seperti satu elemen `ListAkmData` di atas, dibungkus tunggal:
```json
{
  "data": {
    "id": 12,
    "level": 1,
    "isActive": 1,
    "password": "",
    "date": "2024-09-20",
    "startAt": "2024-09-20T07:00:00.000Z",
    "endAt": "2024-09-20T09:00:00.000Z",
    "status": "scheduled",
    "show_score": true,
    "requires_password": false,
    "password_checked": false,
    "has_explain": false,
    "isDone": false,
    "isAssessed": false,
    "isQueued": false,
    "gov_schedule": false,
    "participant": { "uuid": "b3f1a2c4-56de-789f-0123-456789abcdef", "name": "Andi Saputra", "nisn_nik": "0051234567", "school": "SMA Negeri 1 Diskola" },
    "exam": { "id": 5, "name": "AKM Literasi Membaca", "type": "akm", "grade": 8, "author": { "id": 3, "nip": "198501012010011001", "name": "Budi Guru" }, "numberOfQuestions": 40, "instructions": [], "score": 0.0, "assessment_category": "AKM", "assessment_period": "Ganjil 2024/2025" },
    "exams": [],
    "score": [],
    "exam_score": -1.0,
    "type": "akm"
  }
}
```

### `GET mobile/app/learning/akm/exam-schedules/{id}` — detailUjianSekolah
**Parameter:**
- `@Path("id") id: Int`
- `@Query("gov_schedule") isGovSchedule: Int`

**Response:** `DetailAkmResponse` (struktur sama seperti `detailAkm`, dengan `gov_schedule: true` dan biasanya `exams` berisi beberapa mapel ujian sekolah, contoh):
```json
{
  "data": {
    "id": 88,
    "level": 3,
    "isActive": 1,
    "password": "UJS2024",
    "date": "2024-11-01",
    "startAt": "2024-11-01T07:00:00.000Z",
    "endAt": "2024-11-01T09:00:00.000Z",
    "status": "scheduled",
    "show_score": false,
    "requires_password": true,
    "password_checked": false,
    "has_explain": false,
    "isDone": false,
    "isAssessed": false,
    "isQueued": false,
    "gov_schedule": true,
    "participant": { "uuid": "b3f1a2c4-56de-789f-0123-456789abcdef", "name": "Andi Saputra", "nisn_nik": "0051234567", "school": "SMA Negeri 1 Diskola" },
    "exam": null,
    "exams": [
      { "id": 20, "name": "Matematika", "type": "ujian_sekolah", "grade": 12, "author": null, "numberOfQuestions": 40, "instructions": [], "score": 0.0, "assessment_category": "US", "assessment_period": "Genap 2024/2025" },
      { "id": 21, "name": "Bahasa Indonesia", "type": "ujian_sekolah", "grade": 12, "author": null, "numberOfQuestions": 40, "instructions": [], "score": 0.0, "assessment_category": "US", "assessment_period": "Genap 2024/2025" }
    ],
    "score": [],
    "exam_score": -1.0,
    "type": "ujian_sekolah"
  }
}
```

### `GET mobile/app/learning/try-out/schedules/{id}` — detailTryOut
**Parameter:** `@Path("id") id: Int`

**Response:** `DetailAkmResponse` (struktur sama, `type: "try_out"`, contoh nama mapel `"Tryout SNBT Saintek"`).

### `GET mobile/app/learning/akm/student/exam/{id}/review` — reviewAkm
**Parameter:** `@Path("id") id: Int`

**Response:** `Any` — ⚠️ perlu verifikasi manual, tidak ada struktur bertipe di kode. Kemungkinan besar berisi daftar soal + jawaban siswa + kunci jawaban untuk direview (mirip `AkmDownloadResponse` + jawaban terisi), tapi tidak bisa dipastikan tanpa sampel respons asli.

### `GET mobile/app/learning/try-out/scored/{id}` — reviewTryout
**Parameter:** `@Path("id") id: Int`

**Response:** `Any` — ⚠️ perlu verifikasi manual (analog dengan `reviewAkm`, untuk try out).

### `GET mobile/app/learning/akm/student/exam/{id}/download` — downloadSoalAkm
**Parameter:** `@Path("id") id: Int`

**Response:** `AkmDownloadResponse`
```json
{
  "data": {
    "id": 12,
    "exams": [
      {
        "id": 5,
        "name": "AKM Literasi Membaca",
        "type": "akm",
        "grade": 8,
        "author": { "id": 3, "nip": "198501012010011001", "name": "Budi Guru" },
        "numberOfQuestions": 2,
        "instructions": [
          {
            "id": 1,
            "instruction": "Bacalah teks berikut dengan saksama",
            "description": "Bagian Literasi Membaca",
            "sequence": 1,
            "isRandom": false,
            "questions": [
              {
                "id": 101,
                "question": "Apa gagasan utama paragraf pertama?",
                "image": "",
                "answerType": "single_choice",
                "answers": [
                  { "id": 1001, "answer": "Perubahan iklim global", "filePath": "", "isTrue": true, "showFalse": false, "firstStatement": "", "firstFilePath": "", "secondStatement": "", "secondFilePath": "", "selected_id": 0 },
                  { "id": 1002, "answer": "Krisis energi", "filePath": "", "isTrue": false, "showFalse": false, "firstStatement": "", "firstFilePath": "", "secondStatement": "", "secondFilePath": "", "selected_id": 0 }
                ],
                "media": [
                  { "id": 1, "type": "image", "url": "https://cdn.diskola.id/akm/soal101.png", "sequence": 1 }
                ],
                "exam_instruction_id": 1
              }
            ]
          }
        ],
        "score": 0.0,
        "assessment_category": "AKM",
        "assessment_period": "Ganjil 2024/2025"
      }
    ],
    "studentExam": { "id": 200, "student_id": 55 }
  }
}
```
> Field `exams` bertanda `@ObjectToList` — kemungkinan backend kadang mengirim objek tunggal (bukan array) untuk exam non-ujian-sekolah, dan adapter kustom `ObjectToList` menormalkannya jadi `List<AkmExams>` berisi satu elemen.

### `GET mobile/setting-akm` — settingAkmpenalty
**Parameter:** (tidak ada)

**Response:** `ResponsePenaltyTimes`
```json
{
  "data": {
    "penalty_times": 3,
    "penalty_applied": false,
    "absence_setting": true,
    "exam_lock_mode": true
  }
}
```

### `GET mobile/app/learning/akm/student/exam-school/{id}/download` — downloadSoalUjianSchool
**Parameter:**
- `@Path("id") id: Int`
- `@Query("gov_schedule") isGovSchedule: Int`

**Response:** `AkmDownloadResponse` (struktur sama dengan `downloadSoalAkm`, biasanya `exams` berisi lebih dari satu mapel ujian sekolah).

### `POST mobile/app/learning/akm/student/exam-school/{id}/check-password` — checkPasswordUjianSchool
**Parameter:**
- `@Path("id") id: Int`
- `@Header("X-Device-Fingerprint") deviceFingerprint: String`
- `@Body data: Any`

⚠️ **Body: dinamis (tidak bertipe di kode lama)**. Perkiraan field: `{ "password": "UJS2024" }`.

**Response:** `ExamPasswordCheckResponse`
```json
{
  "data": {
    "checked": true,
    "requires_password": true
  }
}
```

### `GET mobile/app/learning/akm/exam-schedules-scored/{id}/explains` — downloadExamSchoolExplanation
**Parameter:**
- `@Path("id") id: Int`
- `@Query("gov_schedule") isGovSchedule: Int`

**Response:** `AkmExplanationResponse`
```json
{
  "data": [
    {
      "id": 1,
      "file_path": "https://cdn.diskola.id/akm/pembahasan-101.pdf",
      "created_at": "2024-11-02T10:00:00.000Z",
      "updated_at": "2024-11-02T10:00:00.000Z",
      "question": {
        "id": 101,
        "question": "Apa gagasan utama paragraf pertama?",
        "image": "",
        "answerType": "single_choice",
        "answers": [],
        "media": [],
        "exam_instruction_id": 1
      }
    }
  ]
}
```

### `GET mobile/app/learning/try-out/student/training/{id}/download` — downloadSoalUjianTryout
**Parameter:** `@Path("id") id: Int`

**Response:** `AkmDownloadResponse` (struktur sama dengan `downloadSoalAkm`, contoh nama `"Tryout SNBT Saintek"`, `type: "try_out"`).

### `POST mobile/app/learning/akm/student/exam/{akm_id}/answer/{student_id}` — uploadJawabanAkm
**Anotasi tambahan:** `@LogFile`

**Parameter:**
- `@Path("akm_id") akm_id: Int`
- `@Path("student_id") student_id: Int?`
- `@Body data: Any`

⚠️ **Body: dinamis (tidak bertipe di kode lama)**. Perkiraan field dari konteks (submit jawaban AKM): daftar jawaban per soal, mis. `{ "answers": [ { "question_id": 101, "answer_id": 1001 } ], "elapsed_time": 1200 }`.

**Response:** `Any` — ⚠️ perlu verifikasi manual (kemungkinan hanya `{ "status": "success" }` atau kosong).

### `POST mobile/app/learning/akm/student/exam-school/{akm_id}/answer/{student_id}` — uploadJawabanUjianSchool
**Anotasi tambahan:** `@LogFile`

**Parameter:**
- `@Path("akm_id") akm_id: Int`
- `@Path("student_id") student_id: Int?`
- `@Header("X-Device-Fingerprint") deviceFingerprint: String`
- `@Query("gov_schedule") isGovSchedule: Int`
- `@Body data: Any`

⚠️ **Body: dinamis (tidak bertipe di kode lama)** — analog `uploadJawabanAkm`, untuk ujian sekolah.

**Response:** `Any` — ⚠️ perlu verifikasi manual.

### `POST mobile/app/learning/try-out/student/training/{akm_id}/answer/{student_id}` — uploadJawabanTryOut
**Anotasi tambahan:** `@LogFile`

**Parameter:**
- `@Path("akm_id") akm_id: Int`
- `@Path("student_id") student_id: Int?`
- `@Body data: Any`

⚠️ **Body: dinamis (tidak bertipe di kode lama)** — analog `uploadJawabanAkm`, untuk try out.

**Response:** `Any` — ⚠️ perlu verifikasi manual.

### `GET mobile/app/learning/akm/exam-schedules` — listUjian
**Parameter:**
- `@Query("date") date: String`
- `@Query("take") take: Int`
- `@Query("skip") skip: Int`

**Response:** `ListAkmResponse` (struktur sama dengan `listAkm`, `gov_schedule: true`, `type: "ujian_sekolah"`).

### `GET mobile/app/learning/try-out/schedules` — listTryOutSchedule
**Parameter:**
- `@Query("take") take: Int`
- `@Query("skip") skip: Int`

**Response:** `ListTryoutResponse`
```json
{
  "data": [
    {
      "id": 30,
      "level": 3,
      "isActive": 1,
      "date": "2024-10-05",
      "startAt": "2024-10-05T07:00:00.000Z",
      "endAt": "2024-10-05T09:00:00.000Z",
      "show_score": true,
      "isDone": false,
      "isAssessed": false,
      "exams": [
        {
          "id": 40,
          "name": "Tryout SNBT Saintek",
          "type": "try_out",
          "grade": 12,
          "author": { "id": 3, "nip": "198501012010011001", "name": "Budi Guru" },
          "numberOfQuestions": 30,
          "score": -1.0,
          "schoolType": ["SMA", "MA"],
          "schoolCities": "Jakarta"
        }
      ],
      "score": [],
      "type": "try_out"
    }
  ]
}
```

### `GET mobile/app/learning/try-out/scored` — listTryOutScored
**Parameter:**
- `@Query("take") take: Int`
- `@Query("skip") skip: Int`

**Response:** `ListTryoutResponse` (struktur sama, `isDone: true`, `isAssessed: true`, `score` terisi):
```json
{
  "data": [
    {
      "id": 30,
      "level": 3,
      "isActive": 0,
      "date": "2024-10-05",
      "startAt": "2024-10-05T07:00:00.000Z",
      "endAt": "2024-10-05T09:00:00.000Z",
      "show_score": true,
      "isDone": true,
      "isAssessed": true,
      "exams": [],
      "score": [
        { "id": 40, "name": "Tryout SNBT Saintek", "type": "try_out", "scored": 650.5 }
      ],
      "type": "try_out"
    }
  ]
}
```

### `GET mobile/app/learning/akm/exam-schedules-scored` — listUjianScored
**Parameter:**
- `@Query("take") take: Int`
- `@Query("skip") skip: Int`

**Response:** `ListAkmResponse` (struktur sama dengan `listScoreAkm`, tapi untuk ujian sekolah — `gov_schedule: true`, `type: "ujian_sekolah"`, `score` bisa berisi beberapa mapel).

---

## Entrepreneurs / Toko (Home Store)

### `GET mobile/enterpreneur/homepage` — loadHomepage
**Parameter:** (tidak ada)

**Response:** `HomepageResponse`
```json
{
  "data": {
    "category": [
      { "id": 1, "name": "Makanan", "icon": "https://cdn.diskola.id/category/makanan.png", "is_selected": false }
    ],
    "populer": [
      { "id": 101, "name": "Keripik Singkong", "description": "Keripik singkong renyah homemade", "price": 15000, "stock": 50, "image": "https://cdn.diskola.id/product/101.png" }
    ],
    "newest": [
      { "id": 102, "name": "Es Teh Kemasan", "description": "Es teh manis kemasan botol", "price": 5000, "stock": 100, "image": "https://cdn.diskola.id/product/102.png" }
    ],
    "bestseller": [
      { "id": 103, "name": "Nasi Goreng Spesial", "description": "Nasi goreng dengan telur dan ayam", "price": 20000, "stock": 20, "image": "https://cdn.diskola.id/product/103.png" }
    ],
    "bestprice": [
      { "id": 104, "name": "Air Mineral 600ml", "description": "Air mineral kemasan botol", "price": 3000, "stock": 200, "image": "https://cdn.diskola.id/product/104.png" }
    ],
    "other_horizontal": [],
    "other_vertical": [],
    "recomendation": [
      { "id": 105, "name": "Buku Tulis 38 Lembar", "description": "Buku tulis sekolah", "price": 4000, "stock": 300, "image": "https://cdn.diskola.id/product/105.png" }
    ]
  }
}
```

### `GET mobile/enterpreneur/category` — loadCategory
**Parameter:** (tidak ada)

**Response:** `CategoryResponse`
```json
{
  "data": [
    { "id": 1, "name": "Makanan", "icon": "https://cdn.diskola.id/category/makanan.png", "is_selected": false },
    { "id": 2, "name": "Alat Tulis", "icon": "https://cdn.diskola.id/category/atk.png", "is_selected": false }
  ]
}
```

### `GET mobile/enterpreneur/category/{categoryId}/detail` — loadCategorySub
**Parameter:**
- `@Path("categoryId") categoryId: Int`
- `@Query("take") take: Int`
- `@Query("skip") skip: Int`

**Response:** `CategorySubResponse`
```json
{
  "data": [
    {
      "id": 11,
      "name": "Makanan Ringan",
      "data": [
        { "id": 1, "name": "Makanan", "icon": "https://cdn.diskola.id/category/makanan.png", "is_selected": false }
      ]
    }
  ]
}
```

### `GET mobile/enterpreneur/goodies/categories/{categoryId}{categorySubId}` — loadCategoryProduct
**Parameter:**
- `@Path("categoryId") categoryId: Int`
- `@Path("categorySubId") categorySubId: String` (mis. `"?sub_category_id=9"` — disisipkan mentah ke path, lihat komentar route lama `?take=10&skip=0&sub_category_id=9`)
- `@Query("take") take: Int`
- `@Query("skip") skip: Int`

**Response:** `CategoryProductResponse`
```json
{
  "data": [
    {
      "id": 101,
      "name": "Keripik Singkong",
      "description": "Keripik singkong renyah homemade",
      "price": 15000,
      "stock": 50,
      "image": "https://cdn.diskola.id/product/101.png",
      "category": { "id": 1, "name": "Makanan", "icon": "https://cdn.diskola.id/category/makanan.png", "created_at": "2024-01-01T00:00:00.000Z", "updated_at": "2024-01-01T00:00:00.000Z" },
      "sub_category": { "id": 11, "name": "Makanan Ringan", "icon": "", "created_at": "2024-01-01T00:00:00.000Z", "updated_at": "2024-01-01T00:00:00.000Z" }
    }
  ]
}
```

### `GET mobile/enterpreneur/card` — loadGoodieCartDetail
**Parameter:**
- `@Query("take") take: Int`
- `@Query("skip") skip: Int`
- `@Query("filter") filter: String` (nilai contoh: `sekolah_lain`, `terpopuler`, `terbaru`, `terlaris`)

**Response:** `GoodieCardDetailResponse`
```json
{
  "data": [
    {
      "id": 101,
      "name": "Keripik Singkong",
      "price": 15000,
      "rating": "4.8",
      "product_sold": 120,
      "main_image": [
        { "image": "https://cdn.diskola.id/product/101.png" }
      ]
    }
  ]
}
```
> `main_image` bertanda `@ObjectToList` — backend kemungkinan bisa mengirim objek tunggal maupun array, dinormalkan ke list.

### `GET mobile/enterpreneur/goodies/{goodieId}/detail` — loadGoodieDetail
**Parameter:** `@Path("goodieId") goodieId: Int`

**Response:** `GoodieDetailResponse`
```json
{
  "data": {
    "id": 101,
    "name": "Keripik Singkong",
    "description": "Keripik singkong renyah homemade, tanpa bahan pengawet.",
    "price": 15000,
    "stock": 50,
    "weight": 200,
    "rating": "4.8",
    "sold": 120,
    "image": [
      { "id": 1, "sequence": 1, "image_original": "https://cdn.diskola.id/product/101_original.png", "image": "https://cdn.diskola.id/product/101.png" }
    ],
    "merchant": {
      "id": 5,
      "name": "Toko Snack Sekolah",
      "avatar": "https://cdn.diskola.id/merchant/5.png",
      "owner": {
        "id": 55,
        "name": "Andi Saputra",
        "roles": [ { "id": 1, "name": "Siswa" } ],
        "school": { "id": 1, "name": "SMA Negeri 1 Diskola" },
        "class": { "id": 10, "name": "XII IPA 1", "grade": 12, "major": { "id": 1, "name": "IPA" } }
      },
      "sales_rating": 5,
      "amount_of_reviewer": 30,
      "success_transactions": 200
    }
  }
}
```

### `GET mobile/enterpreneur/goodies/{goodieId}/review` — loadGoodieDetailReview
**Parameter:** `@Path("goodieId") goodieId: Int`

**Response:** `DetailReviewResponse`
```json
{
  "data": {
    "average_rating": "4.8",
    "jumlah_reviewer": 30
  }
}
```

### `GET mobile/enterpreneur/goodies/{goodieId}/listreview` — loadGoodieReview
**Parameter:**
- `@Path("goodieId") goodieId: Int`
- `@Query("star") star: String` (nilai: `"all"` atau `"1"`..`"5"`)

**Response:** `ListReviewResponse`
```json
{
  "data": [
    {
      "user": { "name": "Budi Santoso", "avatar": "https://cdn.diskola.id/user/budi.png" },
      "date": "2024-09-10",
      "rating": 5,
      "comment": "Enak dan renyah, pengiriman cepat!"
    }
  ]
}
```

### `GET mobile/enterpreneur/filter/count` — LoadCountProductFilter
**Parameter:** `@Query("filter") filter: String`

**Response:** `CountProductFilterResponse`
```json
{
  "data": "24"
}
```
> `data` bertipe `String` (bukan angka) di model Kotlin.

### `GET mobile/enterpreneur/filter/list` — LoadResultFilterGoodies
**Parameter:**
- `@Query("take") take: Int`
- `@Query("skip") skip: Int`
- `@Query("filter") filter: String`

**Response:** `ResultGooidesFilterResponse`
```json
{
  "data": [
    {
      "id": 101,
      "name": "Keripik Singkong",
      "price": 15000,
      "rating": "4.8",
      "product_sold": 120,
      "main_image": [
        { "image": "https://cdn.diskola.id/product/101.png" }
      ]
    }
  ]
}
```

### `GET mobile/enterpreneur/search-merchants` — suggestMerchant
**Parameter:**
- `@Query("take") take: Int`
- `@Query("skip") skip: Int`
- `@Query("keyword") keyword: String`

**Response:** `SuggestMerchantResponse`
```json
{
  "data": [
    {
      "id": 5,
      "name": "Toko Snack Sekolah",
      "avatar_original": "https://cdn.diskola.id/merchant/5_original.png",
      "avatar": "https://cdn.diskola.id/merchant/5.png",
      "school_name": "SMA Negeri 1 Diskola"
    }
  ]
}
```

### `GET mobile/enterpreneur/search-goodies-suggestion` — suggestProduct
**Parameter:**
- `@Query("take") take: Int`
- `@Query("skip") skip: Int`
- `@Query("keyword") keyword: String`

**Response:** `SuggestProductResponse`
```json
{
  "data": [
    { "id": 101, "name": "Keripik Singkong" },
    { "id": 106, "name": "Keripik Kentang" }
  ]
}
```
