# API Contract — Index

Dokumentasi kontrak seluruh endpoint di `app/src/main/java/id/diskola/app/api/ApiService.kt` (aplikasi
lama), dipecah per kelompok fitur. Tujuannya: di **repo baru**, bikin data class/model Kotlin
(request + response) langsung dari sini tanpa perlu baca ulang `ApiService.kt` dan puluhan file
`*Models.kt` satu-satu.

Tiap file berisi, per endpoint: method HTTP + path, parameter (`@Query`/`@Path`/`@Body`/`@Part`), nama
class response, dan **contoh JSON response** yang disusun dari field data class aslinya (bukan
karangan).

## Daftar file

| File | Cakupan | Jumlah endpoint |
|---|---|---|
| [01-auth-akun.md](01-auth-akun.md) | Config app, autentikasi (login/SSO/reset password), sesi/logout, FCM, akun/profil, feed sosial sekolah (legacy) | 43 |
| [02-pembelajaran-tugas-feed.md](02-pembelajaran-tugas-feed.md) | Feed sosial (legacy), pengumuman, materi pembelajaran (theories), tugas (homework), awal presensi | 53 |
| [03-presensi-agenda.md](03-presensi-agenda.md) | Presensi guru/siswa/offsite, izin/cuti, agenda mingguan staf, ujian, SPP, pustaka, policy, awal Klaspay | 63 |
| [04-pembayaran-klaspay-ppob-akm.md](04-pembayaran-klaspay-ppob-akm.md) | Pembayaran umum, Klaspay tagihan/topup, katalog & transaksi PPOB, Asesmen (AKM), awal Entrepreneurs | 53 |
| [05-entrepreneurs-toko.md](05-entrepreneurs-toko.md) | Modul Entrepreneurs/Toko Siswa — **belum aktif di produksi**, didata untuk rencana rebuild | 45 |
| [06-prokes-partisipasi-magang-poin.md](06-prokes-partisipasi-magang-poin.md) | Dana partisipasi, kartu pelajar, prokes (legacy), pairing akun, magang, try out, poin/konseling, presensi QR | 54 |

**Total: 311 endpoint** terdokumentasi dari total ~355 anotasi HTTP di `ApiService.kt` (selisihnya adalah
baris yang sudah di-comment/dead code di source lama, sudah sengaja dilewati oleh tiap riset).

## Cara pakai untuk bikin model di repo baru

1. Buka file sesuai fitur yang sedang dikerjakan (urutan disarankan ikuti `docs/REBUILD_PROMPT.md`
   Bagian 5 — auth dulu, baru fitur lain kecil→besar).
2. Untuk tiap endpoint, contoh JSON di bawahnya langsung bisa ditempel ke generator model (quicktype,
   plugin Android Studio "JSON to Kotlin Class", atau bikin manual) untuk hasilkan `data class`.
3. Endpoint yang ditandai **"Body: dinamis (tidak bertipe di kode lama)"** + ⚠️ — skema request-nya
   TIDAK diambil dari data class (karena di kode lama memang dikirim sebagai `Map<String, Any>`/`Any`
   mentah). Tebakan field di bawahnya hanya estimasi dari nama fungsi/pemanggil — **wajib diverifikasi**
   ke backend/Postman collection asli sebelum dipakai sebagai kontrak final. Total ada sekitar 89
   endpoint seperti ini tersebar di 6 file.

## Anomali & catatan yang perlu ditindaklanjuti manual

Ditemukan oleh riset saat menyisir kode — bukan gap dokumentasi, tapi hal yang sebaiknya diperbaiki
atau dikonfirmasi ulang ke backend saat membangun ulang di repo baru:

- **Path endpoint duplikat** (dua fungsi Kotlin beda nama, path API sama persis) — kemungkinan salah
  satu adalah sisa refactor yang belum dibersihkan:
  - `teacherSubject` vs `teacherSubjectTeach` → sama-sama `mobile/app/learning/theories/teachers/subjects`
  - `teacherTaskTodo` vs `teacherAssignment` → sama-sama `mobile/app/learning/assignment/teachers/backlog`
- **Nama class response `MerchantItem`/`UserResponse`/`SessionResponse` dipakai di lebih dari satu
  package** dengan field yang tidak selalu identik — riset sudah memakai versi yang benar-benar
  di-import di `ApiService.kt`, tapi saat bikin model baru pastikan tidak tertukar sumbernya.
- **`inputResi` (modul Entrepreneurs) pakai method `GET`** padahal secara semantik ini operasi
  tulis (input nomor resi pengiriman) — cek ulang ke backend, kemungkinan seharusnya `POST`/`PUT`.
- **`InputResiResponse` dan `UpadateProductData`** (modul Entrepreneurs) tidak dibungkus pola
  `{status, data}` seperti response lain — field langsung di root JSON.
- **`acceptTransaksiBuyer`** (modul Entrepreneurs) memakai base URL absolut ter-hardcode ke host
  development, bukan `baseUrl` Retrofit standar — perlu diperbaiki saat migrasi, bukan diikuti.
- Beberapa endpoint tidak mengembalikan JSON sama sekali: `logout`/`logoutOthers`/`logoutDevice`
  (tidak ada response body), `download` (mengembalikan file mentah/`ResponseBody`, bukan JSON), dan 2
  endpoint legacy (`updateFcmAsync`, `endExamAsync`) masih pakai `Call<T>` callback lama alih-alih
  `suspend fun`.

Tidak ada endpoint yang tipe response-nya gagal ditemukan definisinya — keenam riset melaporkan semua
class response berhasil ditelusuri sampai ke `data class` aslinya.
