# 08a — Gap Pembayaran / Klaspay (app baru vs app lama)

Audit 2026-10-14 (3 agen Explore + pembacaan langsung `android-portal/.../pages/{pembayaran,klaspay}`). Spesifikasi perilaku: `08-keuangan-pembayaran-klaspay-ppob.md`.
Path relatif `app/src/main/java/id/diskola/app/`. Status: ✅ nyata (backend asli) · 🟡 sebagian · 🧪 data dummy · ❌ belum ada.

## 1. Ringkasan
Sebelum P1 seluruh tab Pembayaran dan 9 layar `Route.PembayaranSub.*` memakai data dummy (`dataclass/mock/PembayaranModels.kt` → `MockPembayaran`;
`PembayaranViewModel` tanpa repository: saldo Rp 1.250.000, "5 tagihan menunggu", kontak Ahmad Fauzan dst.). Yang nyata hanya aktivasi Klaspay (`payment/activate`)
dan badge lonceng. `KlaspayApiService` (29 endpoint), `PpobApiService`, `DanaPartisipasiApiService` masih `Map<String,Any>` (melanggar `rules-global` §3.5);
tak ada tabel Room pembayaran; tak ada unit test. Perkiraan ±85–90 % pekerjaan tersisa (≈40 endpoint, ≈35 layar legacy).

## 2. Status per bagian doc 08 (sebelum → sesudah P1)
| Bagian | Sebelum | Sesudah P1 | Sub-fase |
|---|---|---|---|
| Hub: saldo, ID wallet, gerbang aktivasi, menu per peran | 🧪 | ✅ | P1 |
| Aktivasi 2 langkah PIN + dialog sukses + alert SSO | 🟡 (tanpa toolbar/back/dialog sukses/KONFIRMASI) | ✅ | P1 |
| Pra-cek aktivasi (email terverifikasi → password default → `payment/check`) | ❌ | ✅ (dialog keamanan bersama) | P1 |
| ConfirmPin, Lupa pin, SetPin, deep link reset PIN | ❌ | ✅ komponen + tes; pemanggil ConfirmPin datang di P2 | P1 |
| Q14 (log body/PIN di release) | ❌ | ✅ | P1 |
| Komponen bayar (PaymentType/Guide/Detail/Success) + SPP | 🧪 | belum | P2 |
| Top Up + Transfer Toppers + Riwayat | 🧪 | belum | P3 |
| Tagihanku + QR Pay + My QR | ❌/🧪 | belum | P4 |
| PPOB (6 produk) | ❌ | belum | P5 |
| Dana Partisipasi | ❌ | belum | P6 |
| Daftar notifikasi & router `PAYMENT/*` | 🧪 | belum | fase Notifikasi |
| Feature gate `payment` (menu Bayar SPP & Dana Partisipasi, notifikasi `PAYMENT/SPP`, push `new-spp`/`spp`; doc 08 §5.6) | ❌ | belum | ikut P2 (SPP) & P6 (Dana Partisipasi) |

Baris gate `payment` ditambahkan 22-10-2026 dari perubahan di `android-portal` (21-10-2026). Layar SPP dan Dana Partisipasi di app baru memang belum ada,
jadi gate dipasang bersamaan dengan layarnya. Menurut `07b`, app baru sudah punya `FeatureGateRepository` (fail-open) dari fase Presensi, sehingga cukup
menambah key `payment`. Kode app baru tidak diperiksa ulang pada pembaruan ini.

## 3. Selisih UI app baru vs lama yang disengaja
| Item | App lama | App baru | Alasan |
|---|---|---|---|
| Input PIN | 6 kotak `EditText` + keyboard sistem (`AdaptivePinView`) | `PinDots` + `NumericKeypad` (komponen yang sudah ada) | PIN tak melewati keyboard pihak ketiga; tempel (paste) tak didukung |
| Format "Rp" | `"Rp "` vs `"Rp"` beda per layar | `Rp 150.000` seragam (`PaymentRules.formatRupiah`) | doc 08 §2.1 mencatatnya tak konsisten |
| Info wallet | top sheet | bottom sheet "ID Wallet" | pola komponen app baru |
| Hub | Transfer tersembunyi ("SOON"), tanpa "Transaksi Terakhir"/Toko/Perpus | sama seperti lama (elemen dummy dihapus) | keputusan user 2026-10-14 |
| Muat ulang | `NetworkCallback` + fragment-scoped init + `check-account` di `onStart` | tarik-untuk-segarkan + `ErrorState`; `check-account` tidak diulang (sudah di hub Pembelajaran) | disiplin fetch `rules-global` |
