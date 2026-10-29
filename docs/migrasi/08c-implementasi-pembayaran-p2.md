# 08c — Implementasi Pembayaran P2 (komponen bayar bersama + Bayar SPP + gate `payment`) dan dialog kustom

Dikerjakan 2026-10-27. Spesifikasi: `08` §2.4–§2.7, §5.6, §9. Gap: `08a-gap-pembayaran.md`. Path relatif `app/src/main/java/id/diskola/app/`.

## 1. Arsitektur
`UI → ViewModel → Repository → Retrofit/Room`.

| Berkas | Tanggung jawab |
|---|---|
| `dataclass/ResponData/SppPaymentModels.kt` (+ tes parse 7) | DTO toleran: `payment/channel/spp` (urutan kategori dari server dipertahankan), `history_school`, `spp_trx` (`SppPayRequest` = body legacy `{transaction_id:"id,id", pin, channel, channel_method_id}`), panduan, detail transaksi. Nominal dibaca `Double?` (angka/string/null). Entity Room `payment_invoice`, `payment_guide`. |
| `database/{LocalDatabase,Migrations}.kt`, `di/module/DbModule.kt`, `dataclass/localDb/PaymentDao.kt`, `app/schemas/.../11.json` | **Room v11, `MIGRATION_10_11` eksplisit** (hanya `CREATE TABLE` dua tabel baru; `fallbackToDestructiveMigration` masih aktif sehingga tanpa migrasi seluruh DB — termasuk jawaban ujian — terhapus). Tes migrasi: DDL = ekspor Room, tabel v10 tidak berubah. |
| `utils/SppPaymentRules.kt` (+ tes 18) | Logika murni: label kategori metode, baris saldo (`channel_id == 1`, "Saldo tidak cukup"), peringatan nominal bank/retail; centang bulan berurutan, total, id dipisah koma, teks galat per kode HTTP; chip status (termasuk typo "TANSAKSI DIBUAT"), label nomor VA/tagihan; tanggal (batas waktu 3 format, ISO UTC → `dd MMMM yyyy, HH:mm`); invoice dari hasil bayar; gabung detail (Q5); cache panduan 24 jam. |
| `repository/SppPaymentRepository.kt` | Tagihan SPP, channel, `spp_trx` (invoice non-saldo disimpan ke Room), panduan (cache per channel, diganti saat basi — Q4; saat offline pakai salinan lama), detail (pakai invoice lokal bila ber-nominal, selain itu server lalu digabung). |
| `viewmodel/SppPaymentViewModel.kt` | Satu instance untuk daftar, "Rincian Pembayaran" dan "Pilih Metode Pembayaran" (scope entry daftar). Muat ulang tagihan + saldo setiap layar daftar tampil lagi dan mengosongkan pilihan (legacy `onResume`). PIN diambil dari `PinResultChannel` saat layar metode tampil lagi, langsung ke `spp_trx`; tidak pernah masuk state/route. |
| `viewmodel/PaymentInvoiceViewModels.kt` | Panduan ("Selesaikan Pembayaran") dan detail ("Detail Pembayaran"). |
| `ui/screens/pembayaran/SppScreens.kt` | Daftar (kartu siswa + saldo, tab Tagihan/Sudah Dibayar, tarik-untuk-segarkan, bar total melekat), rincian, metode (akordion; satu terbuka). |
| `ui/screens/pembayaran/PaymentResultScreens.kt` | Panduan (batas waktu statis, SALIN nomor & total, petunjuk 2 tingkat), detail (chip status, baris legacy, info Alfamart), sukses (+ In-App Review setelah SPP, `play:review-ktx` 2.0.2). |
| `viewmodel/PaymentHubViewModel.kt`, `ui/screens/pembayaran/PembayaranScreen.kt`, `repository/FeatureGateRepository.kt` | Gate `payment` (§5.6): wallet dicek dulu (belum aktif → dialog aktivasi, gate tidak dipanggil), lalu `check-feature-availability?name=payment` hanya untuk Bayar SPP & Dana Partisipasi; loading "Memeriksa ketersediaan fitur…"; tidak tersedia → `FeatureUnavailableSheet` (guru: "Buka Website" ke root portal — Q22 asumsi kerja); gagal = fail-open. |
| `ui/navigation/{Route,MainTabsScreen}.kt` | Rute `Spp(showPaid)`, `SppCheckout`, `SppPaymentMethod`, `PaymentGuide`, `PaymentDetail`, `PaymentDone`; dummy SPP/Checkout lama dihapus. Hasil bayar menggantikan rincian+metode (`popUpTo Spp`), sehingga tutup → daftar yang memuat ulang. |

### Dialog kustom (permintaan user 2026-10-27)
`ui/components/AppDialog.kt` ditulis ulang mengikuti `widgets/AppDialog.kt` app lama: kartu dengan **badge nada mengambang** di tepi atas, chip tag, slot detail, daftar opsi, tombol primer (merah untuk nada galat) + sekunder tonal, animasi masuk. Nada dari parameter atau ditebak dari judul (`utils/AppDialogTone.kt`, tes 5 — sama dengan `fromTitle` lama). API lama tetap, jadi ±86 pemanggil langsung ikut. Token nada baru di `ExtendedColors` (terang/gelap). `AlertDialog` bawaan terakhir (Akun: Logout, S&K) diganti; pemilih tanggal/jam Tugas & Poin memakai `AppDatePickerDialog`/`AppTimePickerDialog` (kartu, bentuk, tombol yang sama). `ButtonVariant.Danger` ditambahkan.

## 2. Keputusan user (2026-10-27)
Q16 → **pesan server** (bukan "PIN Tidak Sesuai" untuk semua galat) · Q4 → **cache panduan diganti per channel** · Q5 → **mapping detail diperbaiki** · dialog default → **dialog kustom reusable seperti app lama** (lihat `FLOW_QUESTIONS.md`).

## 3. Deviasi lain (default saya)
| # | App lama | App baru | Alasan |
|---|---|---|---|
| 1 | Bayar dari saldo: status non-sukses tetap ke halaman sukses | Status yang mengandung `fail/error/reject` → dialog gagal dengan pesan server | Semangat Q8 ("gagal ya tampilkan gagal") |
| 2 | Tombol "Top Up Saldo" di baris saldo tidak melakukan apa pun | Tidak ditampilkan; baris abu-abu + "Saldo tidak cukup" | Tombol mati membingungkan |
| 3 | "Cek Tagihanku" membuka Tagihanku | Dialog "Fitur dalam pengembangan" | Tagihanku dibangun di P4 |
| 4 | Batas waktu tak terbaca → "Invalid date"/"Invalid time" | Teks mentah `expired_at` | Lebih berguna bagi pengguna |
| 5 | Format "Rp" berbeda per layar | `Rp 150.000` seragam (`PaymentRules.formatRupiah`) | Keputusan P1 |
| 6 | Urutan baris rincian diurutkan nama bulan Inggris (praktis = urutan asli) | Urutan asli (bulan tertua dulu) | Hasil tampil sama |

## 4. Pengujian
- JVM: **451 tes lolos** (`testDebugUnitTest`, dihitung dari XML baru): +`SppPaymentRulesTest` 18, `SppPaymentModelsParseTest` 7, `AppDialogToneTest` 5, migrasi 10→11 3. `compileDebugKotlin`, `assembleDebug`, `installDebug` bersih.
- **Perangkat (Infinix X1102, Android 15, akun siswa, 2026-10-27):** dialog baru tampil benar (Logout, Aktivasi Wallet); tab Pembayaran → "Bayar Spp" dengan wallet belum aktif → dialog "Aktivasi Wallet Lebih Dahulu", **tanpa** request gate/SPP (sesuai §5.6).
- **Gate `payment` di perangkat (2026-10-27, wallet akun uji sudah aktif):** server dev menjawab `{"available":false,"message":"maintenance"}`. "Bayar Spp" → bottom sheet "Fitur Belum Tersedia di Aplikasi". Awalnya "Dana Partisipasi" (belum dibangun, P6) menampilkan dialog "Fitur dalam pengembangan" tanpa bertanya gate — tidak konsisten dengan app lama, yang menjaga keduanya dengan gate yang sama. Diperbaiki: untuk kedua menu gate ditanya lebih dulu; "Fitur dalam pengembangan" hanya bila server bilang tersedia tetapi layarnya belum ada. Terbukti: keduanya kini menampilkan sheet maintenance yang sama.
- **Belum teruji di perangkat:** seluruh alur SPP dengan wallet aktif (daftar tagihan nyata, rincian, metode, PIN, `spp_trx`, panduan, detail, sukses + In-App Review), migrasi Room 10→11 pada DB nyata. Butuh akun siswa ber-wallet aktif dan tagihan SPP; **pembayaran sungguhan tidak dilakukan oleh agen** (transaksi keuangan) — uji sampai layar PIN, pembayarannya oleh pengguna.
