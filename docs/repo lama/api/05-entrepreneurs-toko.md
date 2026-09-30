# API Contract — Entrepreneurs / Toko Siswa (belum aktif di produksi)

> **Catatan modul.** Rentang endpoint di bawah ini (baris 1083–1352 `ApiService.kt`) adalah bagian dari modul **Entrepreneurs / Toko Siswa** (`mobile/enterpreneur/*`) — marketplace kewirausahaan siswa (goodies/produk, toko/merchant, transaksi jual-beli, review, cart). Modul ini **SUDAH DIKONFIRMASI TIDAK AKTIF di UI produksi saat ini** (kode masih ada di repo tapi dead code / tidak dipakai flow aplikasi yang berjalan). Tetap didata di sini untuk kelengkapan kontrak API (bagian dari total 355 endpoint) dan sebagai referensi bila modul `entrepreneurs` dibangun ulang di masa depan (lihat `docs/REBUILD_PROMPT.md`, baris fitur `entrepreneurs`: "kewirausahaan siswa: toko sendiri, produk, order, review, income", 47 endpoint).
>
> Model Kotlin sumber ada di dua file berbeda:
> - `app/src/main/java/id/diskola/app/pages/sekolah/store/StoreModels.kt`
> - `app/src/main/java/id/diskola/app/pages/sekolah/store/CartModels.kt`
> - `app/src/main/java/id/diskola/app/pages/entrepreneurs/EntrepreneursModels.kt`
> - `app/src/main/java/id/diskola/app/pages/partisipasi/PartisipasiModels.kt` (endpoint `payment/bill/list` ikut masuk rentang baris ini walau bukan bagian modul entrepreneurs — dicatat di bagian "Lain-lain" di bawah)
>
> ⚠️ **Temuan penting untuk rebuild:** Ada DUA `data class MerchantItem` berbeda dengan nama sama persis di package berbeda:
> - `pages.sekolah.store.MerchantItem` (id, name, avatar, owner, sales_rating, amount_of_reviewer, success_transactions) — dipakai oleh `MerchantResponse`.
> - `pages.entrepreneurs.MerchantItem` (id, buyer_name, status, date, goodies, goodies_count, sub_total, courier_name, courier_fee, total, destination_address) — field-nya justru identik dengan `TransaksiItem`, bukan data merchant. Ini dipakai sebagai tipe field `merchant` pada `TransaksiItem` dan `DetailTransaksiItem`.
> Kemungkinan besar ini bug copy-paste di kode lama (field `merchant` pada transaksi seharusnya berisi data toko, bukan data transaksi lagi). **Perlu verifikasi manual ke response API asli** sebelum dipakai sebagai acuan model baru — jangan salin mentah-mentah struktur `entrepreneurs.MerchantItem`.

---

## Search Produk (Goodies)

### `GET mobile/enterpreneur/search` — searchResult
**Parameter:** `take: Int` (query), `skip: Int` (query), `keyword: String` (query)
**Response:** `SearchResultResponse` (data: List<HomeProductItem>)
```json
{
  "data": [
    {
      "id": 101,
      "name": "Kaos Distro OSIS",
      "description": "Kaos katun combed produksi siswa kelas XII",
      "price": 85000,
      "stock": 12,
      "image": "https://cdn.diskola.id/goodies/kaos-osis.jpg"
    }
  ]
}
```

---

## Merchant Goodie (Profil Toko Publik)

### `GET mobile/enterpreneur/merchants/{sellerId}` — loadMerchantGoodie
**Parameter:** `sellerId: Int` (path)
**Response:** `MerchantResponse`
```json
{
  "data": {
    "id": 12,
    "name": "Toko Kreatif Kelas XII",
    "avatar": "https://cdn.diskola.id/merchants/12/avatar.jpg",
    "owner": {
      "id": 55,
      "name": "Ahmad Fauzan",
      "roles": [ { "id": 3, "name": "Siswa" } ],
      "school": { "id": 4, "name": "SMK Negeri 1 Diskola" },
      "class": {
        "id": 21,
        "name": "XII RPL 1",
        "grade": 12,
        "major": { "id": 4, "name": "SMK Negeri 1 Diskola" }
      }
    },
    "sales_rating": 5,
    "amount_of_reviewer": 34,
    "success_transactions": 120
  }
}
```

### `GET mobile/enterpreneur/merchants/{sellerId}/goodies-all` — productMerchantGoodie
**Parameter:** `sellerId: Int` (path), `take: Int` (query), `skip: Int` (query)
**Response:** `MerchantProductResponse`
```json
{
  "data": [
    {
      "id": 101,
      "name": "Kaos Distro OSIS",
      "description": "Kaos katun combed produksi siswa kelas XII",
      "price": 85000,
      "stock": 12,
      "image": "https://cdn.diskola.id/goodies/kaos-osis.jpg",
      "sold": 45,
      "rating": "4.8",
      "status": "aktif",
      "published": 1
    }
  ]
}
```

### `GET mobile/enterpreneur/merchants/{sellerId}/goodies-best-seller` — productBestSellerMerchantGoodie
**Parameter:** `sellerId: Int` (path), `take: Int` (query), `skip: Int` (query)
**Response:** `MerchantProductResponse` (struktur sama dengan endpoint di atas)

### `GET mobile/enterpreneur/merchants/{sellerId}/summary` — summaryMerchantGoodie
**Parameter:** `sellerId: Int` (path)
**Response:** `MerchantSummary`
```json
{
  "data": {
    "product": 8,
    "incoming_order": 3,
    "incoming_amount": 255000,
    "review": 34,
    "history_order": 120
  }
}
```

---

## Merchant User (Toko Saya)

### `GET mobile/enterpreneur/merchants/account/profile` — loadMerchantUser
**Parameter:** (tidak ada)
**Response:** `MerchantResponse` (struktur sama dengan `loadMerchantGoodie` di atas)

### `POST mobile/enterpreneur/merchants` — createMerchantUser
**Parameter:** `name: String` (query)
**Response:** `MerchantResponse`

### `PUT mobile/enterpreneur/merchants/account/profiles` — editMerchantUser
**Parameter:** `name: String` (query)
**Response:** `MerchantResponse`

### `POST mobile/enterpreneur/merchants/account/profiles/image` — editImgMerchantUser
**Anotasi:** `@Multipart`
**Parameter:** `file: MultipartBody.Part?` (@Part, form field diperkirakan bernama `file`)
**Response:** `MerchantResponse`

### `GET mobile/enterpreneur/merchants/account/profile/goodies-all` — productMerchantUser
**Parameter:** `take: Int` (query), `skip: Int` (query)
**Response:** `MerchantProductResponse` (struktur sama seperti di atas)

### `GET mobile/enterpreneur/merchants/account/profile/goodies-best-seller` — productBestSellerMerchantUser
**Parameter:** `take: Int` (query), `skip: Int` (query)
**Response:** `MerchantProductResponse`

### `GET mobile/enterpreneur/merchants/account/profile/summary` — summaryMerchant
**Parameter:** (tidak ada)
**Response:** `MerchantSummary` (struktur sama seperti `summaryMerchantGoodie`)

### `GET mobile/enterpreneur/merchants/account/profile/summary-purchase` — summaryMerchantPembelian
**Parameter:** (tidak ada)
**Response:** `MerchantPembelianSummary`
```json
{
  "data": {
    "purchase": 7,
    "reviewable_order": 2
  }
}
```

---

## Transaksi — Toko Saya (Seller)

### `GET mobile/enterpreneur/merchants/account/transactions/incoming` — IncomingOrder
**Parameter:** `take: Int` (query), `skip: Int` (query), `date: String` (query)
**Response:** `TransaksiResponse`
```json
{
  "data": [
    {
      "id": 501,
      "buyer_name": "Siti Nurhaliza",
      "status": "menunggu_konfirmasi",
      "date": "2026-09-01 10:23:00",
      "goodies": [
        {
          "id": 1,
          "goody_id": 101,
          "goody_image": "https://cdn.diskola.id/goodies/kaos-osis.jpg",
          "goody_name": "Kaos Distro OSIS",
          "goody_price": 85000,
          "goody_quantity": 2,
          "rating": null,
          "comment": "",
          "review_user": null,
          "review_merchant": null
        }
      ],
      "goodies_count": 2,
      "sub_total": 170000,
      "courier_name": "JNE Reguler",
      "courier_fee": 15000,
      "total": 185000,
      "merchant": null,
      "destination_address": "Jl. Pendidikan No. 10, Bandung"
    }
  ]
}
```
> ⚠️ Field `merchant` bertipe `pages.entrepreneurs.MerchantItem` (lihat catatan bug di atas) — kemungkinan besar seharusnya berisi info toko (id/name/avatar), bukan field transaksi. Perlu verifikasi ke response API asli.

### `GET mobile/enterpreneur/merchants/account/transactions/processed` — ProcessedTransaksi
**Parameter:** `take: Int` (query), `skip: Int` (query), `date: String` (query)
**Response:** `TransaksiResponse` (struktur sama dengan `IncomingOrder`)

### `GET mobile/enterpreneur/merchants/account/transactions/completed` — CompletedTransaksi
**Parameter:** `take: Int` (query), `skip: Int` (query), `date: String` (query)
**Response:** `TransaksiResponse` (struktur sama, biasanya `status: "selesai"`)

### `GET mobile/enterpreneur/merchants/account/reviews` — ReviewTransaksiSeller
**Parameter:** `take: Int` (query), `skip: Int` (query), `date: String` (query)
**Response:** `TransaksiResponse` (list transaksi yang sudah/bisa direview oleh seller, struktur sama)

---

## Review — Buyer

### `GET mobile/enterpreneur/reviews` — ReviewTransaksiBuyer
**Parameter:** `take: Int` (query), `skip: Int` (query), `date: String` (query)
**Response:** `TransaksiResponse` (struktur sama, dari sisi pembeli)

### `GET mobile/enterpreneur/reviews/transactions/{transaksiId}` — detailReviewBuyerTransaksi
**Parameter:** `transaksiId: Int` (path)
**Response:** `DetailTransaksiResponse` (lihat contoh lengkap di `detailTransaksi` bawah)

### `GET mobile/enterpreneur/merchants/account/reviews/transactions/{transaksiId}` — detailReviewSellerTransaksi
**Parameter:** `transaksiId: Int` (path)
**Response:** `DetailTransaksiResponse`

### `POST mobile/enterpreneur/reviews/{goodyReviewId}` — postReviewBuyer
**Parameter:** `goodyReviewId: Int` (path), `rating: Int` (query), `comment: String` (query)
**Response:** `Any` — tidak bertipe di kode lama. Perkiraan respons generik:
```json
{
  "status": true,
  "message": "Review berhasil dikirim"
}
```
⚠️ perlu verifikasi manual.

### `POST mobile/enterpreneur/merchants/account/reviews/{goodyReviewId}` — postReviewSeller
**Parameter:** `goodyReviewId: Int` (path), `rating: Int` (query), `comment: String` (query)
**Response:** `Any` — sama seperti `postReviewBuyer`, ⚠️ perlu verifikasi manual.

---

## Aksi Transaksi Masuk (Seller)

### `POST mobile/enterpreneur/merchants/account/transactions/{TransaksiId}/accept` — acceptTransaction
**Parameter:** `TransaksiId: Int` (path)
**Response:** `AcceptRejectResponse`
```json
{
  "data": {
    "id": 501,
    "transaction_date": "2026-09-01 10:23:00",
    "buyer_name": "Siti Nurhaliza",
    "buyer_school": "SMK Negeri 1 Diskola",
    "buyer_type": "siswa",
    "goodies": [
      {
        "id": 1,
        "goody_id": 101,
        "goody_image": "https://cdn.diskola.id/goodies/kaos-osis.jpg",
        "goody_name": "Kaos Distro OSIS",
        "goody_price": 85000,
        "goody_quantity": 2,
        "rating": null,
        "comment": "",
        "review_user": null,
        "review_merchant": null
      }
    ],
    "origin_province": "Jawa Barat",
    "destination_sub_district": "Coblong",
    "destination_city": "Bandung",
    "destination_province": "Jawa Barat",
    "destination_address": "Jl. Pendidikan No. 10, Bandung",
    "goodies_count": 2,
    "sub_total": 170000,
    "courier_name": "JNE Reguler",
    "courier_fee": 15000,
    "total": 185000,
    "rejected_reason": "",
    "status": "diproses"
  }
}
```

### `POST mobile/enterpreneur/merchants/account/transactions/{TransaksiId}/reject` — rejectTransaction
**Parameter:** `TransaksiId: Int` (path)
**Response:** `AcceptRejectResponse` (struktur sama, biasanya `status: "ditolak"` dan `rejected_reason` terisi)

---

## Aksi Transaksi (Buyer)

### `GET https://dev.api.diskola.id/api/mobile/enterpreneur/transactions/{TransaksiId}/accept` — acceptTransaksiBuyer
**Parameter:** `TransaksiId: Int` (path)
**Response:** `AcceptCancleBuyerResponse`
> ⚠️ URL endpoint ini **hardcoded absolute** ke host dev (`https://dev.api.diskola.id`), bukan path relatif seperti endpoint lain — kemungkinan bug/lupa dihapus dari base URL saat development. Perlu diperbaiki jadi path relatif (`mobile/enterpreneur/transactions/{TransaksiId}/accept`) saat rebuild.
```json
{
  "data": {
    "id": 501,
    "user_id": 77,
    "enterpreneur_merchant_id": 12,
    "total": 185000,
    "status": "diterima_pembeli"
  }
}
```

### `GET mobile/enterpreneur/transactions/{TransaksiId}/cancel` — cancleTransactionBUyer
**Parameter:** `TransaksiId: Int` (path)
**Response:** `AcceptCancleBuyerResponse` (struktur sama, `status: "dibatalkan"`)

---

## Histori Transaksi Pembelian (Buyer)

### `GET mobile/enterpreneur/transactions/purchases-done` — ListPurchasesDone
**Parameter:** `take: Int` (query), `skip: Int` (query), `date: String` (query)
**Response:** `TransaksiResponse` (struktur sama dengan `IncomingOrder`, status `selesai`)

### `GET mobile/enterpreneur/transactions/purchases-processed` — ListPurchasesProcess
**Parameter:** `take: Int` (query), `skip: Int` (query), `date: String` (query)
**Response:** `TransaksiResponse` (status dalam proses, misal `dikirim`)

### `GET mobile/enterpreneur/transactions/purchases/{transaksiId}` — detailTransaksiPembelian
**Parameter:** `transaksiId: Int` (path)
**Response:** `DetailTransaksiResponse` (lihat contoh lengkap di bawah)

---

## Detail Transaksi

### `GET mobile/enterpreneur/merchants/account/transactions/{transaksiId}` — detailTransaksi
**Parameter:** `transaksiId: Int` (path)
**Response:** `DetailTransaksiResponse`
```json
{
  "data": {
    "id": 501,
    "transaction_date": "2026-09-01 10:23:00",
    "date": "2026-09-01",
    "buyer_name": "Siti Nurhaliza",
    "buyer_school": "SMK Negeri 1 Diskola",
    "buyer_type": "siswa",
    "goodies": [
      {
        "id": 1,
        "goody_id": 101,
        "goody_image": "https://cdn.diskola.id/goodies/kaos-osis.jpg",
        "goody_name": "Kaos Distro OSIS",
        "goody_price": 85000,
        "goody_quantity": 2,
        "rating": 5,
        "comment": "Bagus dan cepat sampai",
        "review_user": {
          "id": 9,
          "user": {
            "id": 77,
            "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            "name": "Siti Nurhaliza",
            "user_username": "siti.nurhaliza",
            "email": "siti.nurhaliza@example.com",
            "nis_nik": 2210001,
            "phone": 6281234567890,
            "user_avatar_image": "https://cdn.diskola.id/users/77/avatar.jpg",
            "avatar": "https://cdn.diskola.id/users/77/avatar.jpg"
          },
          "rating": 5,
          "comment": "Bagus dan cepat sampai",
          "date": "2026-09-05"
        },
        "review_merchant": null
      }
    ],
    "origin_sub_district": "Coblong",
    "origin_city": "Bandung",
    "origin_province": "Jawa Barat",
    "destination_sub_district": "Cicendo",
    "destination_city": "Bandung",
    "destination_province": "Jawa Barat",
    "destination_address": "Jl. Pendidikan No. 10, Bandung",
    "goodies_count": 2,
    "sub_total": 170000,
    "courier_name": "JNE Reguler",
    "courier_fee": 15000,
    "shipping_name": "JNE Reguler",
    "shipping_fee": 15000,
    "total": 185000,
    "status": "selesai",
    "merchant": null
  }
}
```
> ⚠️ Field `merchant` bertipe `pages.entrepreneurs.MerchantItem` — lihat catatan bug duplikasi nama class di bagian atas dokumen. Perlu verifikasi manual struktur asli dari API.

---

## Tracking & Resi Pengiriman

### `GET mobile/enterpreneur/trackings/{transaksiId}` — trackingDetail
**Parameter:** `transaksiId: Int` (path)
**Response:** `TrackingDetailResponse`
```json
{
  "data": [
    {
      "code": "OTW",
      "description": "Paket sedang dalam perjalanan menuju kota tujuan",
      "datetime": "2026-09-02 08:00:00"
    },
    {
      "code": "DELIVERED",
      "description": "Paket telah diterima",
      "datetime": "2026-09-04 14:30:00"
    }
  ]
}
```

### `GET mobile/enterpreneur/transactions/awb/{transaksiId}` — inputResi
**Parameter:** `transaksiId: Int` (path), `waybill: String` (query), `courier: String` (query)
**Response:** `InputResiResponse`
> Catatan: response ini TIDAK memakai wrapper `data` seperti kebanyakan endpoint lain — field langsung di top-level.
```json
{
  "error": "false",
  "message": "Nomor resi berhasil disimpan",
  "waybill": "JNE1234567890",
  "courier": "JNE"
}
```
> ⚠️ Method HTTP `GET` untuk aksi "input resi" (yang secara semantik adalah operasi tulis/update data) terlihat tidak lazim — kemungkinan seharusnya `POST`/`PUT`. Perlu verifikasi manual ke backend.

---

## Produk Toko (CRUD Goodie)

### `GET mobile/enterpreneur/merchants/account/goodies/{goodieId}` — viewGood
**Parameter:** `goodieId: Int` (path)
**Response:** `MyProductResponse`
```json
{
  "data": {
    "id": 101,
    "name": "Kaos Distro OSIS",
    "description": "Kaos katun combed produksi siswa kelas XII",
    "status": "aktif",
    "price": 85000,
    "stock": 12,
    "published": 1,
    "category": { "id": 3, "name": "Fashion" },
    "sub_category": { "id": 7, "name": "Kaos" },
    "image": [
      { "id": 1, "sequence": 1, "image": "https://cdn.diskola.id/goodies/kaos-osis-1.jpg" },
      { "id": 2, "sequence": 2, "image": "https://cdn.diskola.id/goodies/kaos-osis-2.jpg" }
    ]
  }
}
```

### `POST mobile/enterpreneur/merchants/account/goodies/create` — createGood
**Anotasi:** `@Multipart`
**Parameter:**
- `data: Map<String, RequestBody>` (@PartMap) — Body: dinamis (tidak bertipe di kode lama). Tebakan field berdasarkan `CreateProductData`/`UpadateProductData`: `name`, `description`, `price`, `stock`, `enterpreneur_category_id`/`category_id`, `enterpreneur_sub_category_id`/`sub_category_id`. ⚠️ perlu verifikasi manual.
- `files: List<MultipartBody.Part>?` (@Part) — file gambar produk, multiple.

**Response:** `CreateProductResponse`
```json
{
  "data": {
    "id": 102,
    "name": "Gantungan Kunci Akrilik",
    "description": "Gantungan kunci custom desain siswa",
    "price": "15000",
    "stock": "50",
    "category": { "id": 5, "name": "Aksesoris", "icon": "ic_accessory", "is_selected": false },
    "sub_category": {
      "id": 9,
      "name": "Gantungan Kunci",
      "data": [ { "id": 5, "name": "Aksesoris", "icon": "ic_accessory", "is_selected": false } ]
    }
  }
}
```

### `POST mobile/enterpreneur/merchants/account/goodies/create/{goodieId}/image` — addImageGood
**Anotasi:** `@Multipart`
**Parameter:** `goodieId: Int` (path), `file: MultipartBody.Part` (@Part)
**Response:** `Any` — tidak bertipe di kode lama. Perkiraan generik `{ "status": true, "message": "..." }`. ⚠️ perlu verifikasi manual.

### `POST mobile/enterpreneur/merchants/account/goodies/update/{goodieId}/image/{imageId}` — updateImageGood
**Anotasi:** `@Multipart`
**Parameter:** `goodieId: Int` (path), `imageId: Int` (path), `file: MultipartBody.Part` (@Part)
**Response:** `Any` — ⚠️ perlu verifikasi manual, kemungkinan generik status/message.

### `PUT mobile/enterpreneur/merchants/account/goodies/publish/{goodieId}` — publishGood
**Parameter:** `goodieId: Int` (path), `data: Any` (@Body) — Body: dinamis (tidak bertipe di kode lama). Tebakan: `{ "published": 1 }` atau `{ "is_published": true }`. ⚠️ perlu verifikasi manual.
**Response:** `Any` — ⚠️ perlu verifikasi manual.

### `DELETE mobile/enterpreneur/merchants/account/goodies/delete/{goodieId}/image/{imageId}` — deleteImageGood
**Parameter:** `goodieId: Int` (path), `imageId: Int` (path)
**Response:** `Any` — ⚠️ perlu verifikasi manual.

### `PUT mobile/enterpreneur/merchants/account/goodies/update/{goodieId}` — updateGood
**Parameter:** `goodieId: Int` (path), `data: Any` (@Body) — Body: dinamis (tidak bertipe di kode lama). Tebakan field berdasarkan tipe return `UpadateProductData`: `name`, `description`, `price`, `stock`, `enterpreneur_category_id`, `enterpreneur_sub_category_id`. ⚠️ perlu verifikasi manual.
**Response:** `UpadateProductData`
> Catatan: response ini TIDAK memakai wrapper `data` (berbeda dari `CreateProductResponse`) — field langsung di top-level.
```json
{
  "id": 102,
  "name": "Gantungan Kunci Akrilik Custom",
  "description": "Gantungan kunci custom desain siswa, update deskripsi",
  "price": 18000,
  "stock": 40,
  "enterpreneur_category_id": 5,
  "enterpreneur_sub_category_id": 9
}
```

### `DELETE mobile/enterpreneur/merchants/account/goodies/delete/{goodieId}` — deleteGood
**Parameter:** `goodieId: Int` (path)
**Response:** *(tidak bertipe / `Unit`)* — fungsi tidak mendeklarasikan return type di kode lama, kemungkinan hanya mengandalkan HTTP status code tanpa body. ⚠️ perlu verifikasi manual.

---

## Cart (Keranjang Belanja)

### `GET mobile/enterpreneur/carts` — loadCart
**Parameter:** (tidak ada)
**Response:** `CartResponse`
```json
{
  "data": [
    {
      "merchant": {
        "id": 12,
        "name": "Toko Kreatif Kelas XII",
        "avatar": "https://cdn.diskola.id/merchants/12/avatar.jpg"
      },
      "goodies": [
        {
          "goodie": {
            "id": 101,
            "name": "Kaos Distro OSIS",
            "price": 85000,
            "stock": 12,
            "image": { "image": "https://cdn.diskola.id/goodies/kaos-osis.jpg" }
          },
          "quantity": 2
        }
      ]
    }
  ]
}
```

### `POST mobile/enterpreneur/carts` — addToCart
**Parameter:** `data: Any` (@Body) — Body: dinamis (tidak bertipe di kode lama). Tebakan: `{ "goodie_id": 101, "quantity": 2 }`. ⚠️ perlu verifikasi manual.
**Response:** `Any` — ⚠️ perlu verifikasi manual.

### `PUT mobile/enterpreneur/carts/goodies/{goods}` — updateCart
**Parameter:** `productId: Int` (path, nama param Kotlin `productId` untuk path `{goods}`), `data: Any` (@Body) — Body: dinamis. Tebakan: `{ "quantity": 3 }`. ⚠️ perlu verifikasi manual.
**Response:** `Any` — ⚠️ perlu verifikasi manual.

### `DELETE mobile/enterpreneur/carts/goodies/{goods}` — deleteCart
**Parameter:** `productId: Int` (path, nama param Kotlin `productId` untuk path `{goods}`)
**Response:** `Any` — ⚠️ perlu verifikasi manual.

---

## Lain-lain (di luar modul Entrepreneurs, tapi berada dalam rentang baris yang diriset)

### `GET payment/bill/list` — listDanaPartisipasi
> Endpoint ini bukan bagian modul `entrepreneurs`/toko siswa — ini modul **Dana Partisipasi** (`pages/partisipasi`). Dicatat di sini hanya karena posisinya ada di rentang baris 1083–1352 yang diriset. Dokumentasi lengkap modul ini sebaiknya masuk file kontrak API terpisah untuk modul Partisipasi.
**Parameter:** (tidak ada)
**Response:** `ListPartisipasiResponse`
```json
{
  "data": {
    "list_bill": [
      {
        "bill_id": "BILL-2026-001",
        "bill_name": "Iuran Study Tour Kelas XII",
        "parent_bill_id": "",
        "target_nominal": 1500000,
        "paid_nominal": 750000,
        "wallet_id": "WLT-0012",
        "is_active": true,
        "is_expired": false,
        "expired_date": "2026-12-31",
        "created_date": "2026-08-01",
        "updated_date": "2026-09-01",
        "bill_status": "Belum Lunas",
        "list_child": []
      }
    ]
  }
}
```

> Catatan: ada satu anotasi endpoint lagi (`@GET("payment/bill/history/id/{id}")`) yang baru mulai muncul persis di baris 1352 (batas akhir rentang riset) dan definisi fungsinya terpotong di luar rentang 1083–1352 — **tidak didokumentasikan di file ini** karena berada di luar batas baris yang diminta.

---

## Ringkasan

- Total endpoint aktif yang didokumentasikan dari rentang baris 1083–1352: **45 endpoint**.
- Semua tipe response berhasil ditemukan definisi `data class`-nya di kode (`StoreModels.kt`, `CartModels.kt`, `EntrepreneursModels.kt`, `PartisipasiModels.kt`) — tidak ada response type yang hilang/tidak ditemukan.
- 9 endpoint memiliki return type `Any` (tidak bertipe): `postReviewBuyer`, `postReviewSeller`, `addImageGood`, `updateImageGood`, `publishGood`, `deleteImageGood`, `addToCart`, `updateCart`, `deleteCart` — skema response-nya ditandai ⚠️ perlu verifikasi manual.
- 5 endpoint memiliki `@Body data: Any` (body dinamis, tidak bertipe): `publishGood`, `updateGood`, `addToCart`, `updateCart` — field ditebak dari konteks nama fungsi/response type terkait, ⚠️ perlu verifikasi manual.
- 1 endpoint (`deleteGood`) tidak punya return type sama sekali (Unit).
- Ditemukan bug/anomali penting untuk rebuild: duplikasi nama class `MerchantItem` di dua package berbeda dengan struktur field yang tidak konsisten (lihat catatan di awal dokumen), endpoint `acceptTransaksiBuyer` memakai absolute URL hardcoded ke host dev, `InputResiResponse` dan `UpadateProductData` tidak memakai wrapper `data` seperti response lain, dan `inputResi` memakai method `GET` untuk operasi yang semestinya bersifat tulis/update.
