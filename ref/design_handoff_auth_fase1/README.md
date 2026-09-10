# Handoff: Diskola Mobile — Fase 0 (Design System) + Fase 1 (Auth)

Target repo: `husnunn/app-diskola` (branch `main`, saat handoff ini dibuat masih kosong).
Target teknis: Kotlin, **Jetpack Compose 100%**, minSdk 27 / targetSdk 36, Material 3, Hilt,
Navigation Compose type-safe, Gradle multi-module — sesuai `docs/BRIEF.md`.

## Overview

Paket ini berisi desain untuk dua fase pertama rebuild Diskola:

- **Fase 0** — modul `:core:designsystem`: token warna (light + dark), tipografi Lato, skala
  spacing/shape/motion, dan komponen dasar (Button, TextField, Card, ListRow, Chip/Badge,
  AppDialog, BottomSheet, LoadingState, EmptyState, ErrorState, BannerError).
- **Fase 1** — modul `:feature:auth`: Splash → Onboarding (3 halaman) → Login (NISN) →
  Login (Password) → Home; jalur Google SSO; bottom sheet Pilih Sekolah; dialog Syarat & Ketentuan,
  Konfirmasi Tamu, dan Update Wajib.

## About the Design Files

File di `design/` adalah **referensi desain yang dibuat sebagai HTML** (Design Component /
`.dc.html` + React), bukan kode produksi untuk disalin. File-file itu prototipe: menunjukkan
tampilan dan perilaku yang diinginkan.

Tugasnya adalah **membangun ulang desain ini di lingkungan target** — Jetpack Compose + Material 3
di repo `app-diskola` — memakai pola dan library yang ditetapkan di `docs/BRIEF.md` (Hilt,
Navigation Compose, `StateFlow<UiState>`, `Result<T>`, Room, DataStore). Jangan mem-port HTML/CSS-nya.

Cara membuka referensi: `design/*.dc.html` dirender di alat desain (butuh runtime `support.js`
milik alat tersebut), jadi untuk membaca nilai persisnya buka file sebagai teks — semua style
inline dan eksplisit, dan setiap nilai warna/ukuran ada di dokumen ini juga.

## Fidelity

**High-fidelity.** Warna, tipografi, spacing, radius, durasi animasi, dan seluruh copy sudah final
dan diambil dari `docs/BRIEF.md` §4 serta `docs/BACKGROUND.md`. Recreate se-presisi mungkin.

Dua hal yang **masih placeholder** dan perlu aset nyata sebelum rilis:
1. Ilustrasi 3 halaman Onboarding (kotak putus-putus 252×196 dengan ikon).
2. Logo "G" pada tombol Google Sign-In (kotak putus-putus 20×20 berisi huruf G) — ganti dengan
   aset resmi Google branding.
3. Ikon di mockup memakai **Material Symbols Rounded**; di Compose pakai `androidx.compose.material.icons`
   atau `material-symbols` yang setara. Nama ikon dicantumkan per komponen di bawah.

---

## Design Tokens

### Light scheme (Material 3)

```
primary #006A60   onPrimary #FFFFFF   primaryContainer #005048   onPrimaryContainer #95E7DA
secondary #007A6E onSecondary #FFFFFF secondaryContainer #CAE5E0 onSecondaryContainer #4E6763
tertiary #2D4960  onTertiary #FFFFFF  tertiaryContainer #456179  onTertiaryContainer #BEDCF8
error #BA1A1A     onError #FFFFFF     errorContainer #FFDAD6     onErrorContainer #93000A
background/surface #F4FAF8            onBackground/onSurface #151D1C
surfaceVariant #DBE4E2                onSurfaceVariant #3E4947
outline #6E7977   outlineVariant #BEC9C6
surfaceDim #D3DCD9  surfaceBright #F4FAF8
surfaceContainerLowest #FFFFFF  Low #EDF6F3  Default #E7F0ED  High #E1EAE7  Highest #DBE4E2
inverseSurface #2A3231  inverseOnSurface #EAF3F0  inversePrimary #84D5C8
```

### Dark scheme

```
primary #92D3C7   onPrimary #003731   primaryContainer #005047   onPrimaryContainer #74C3B5
secondary #43DDBD onSecondary #00382D secondaryContainer #00BE9F onSecondaryContainer #00463A
tertiary #86D5C7  onTertiary #00201C  tertiaryContainer #005047  onTertiaryContainer #74C3B5
error #FFB4AB     onError #690005     errorContainer #93000A     onErrorContainer #FFDAD6
background/surface #0A1513            onBackground/onSurface #D9E5E1
surfaceVariant #2C3735                onSurfaceVariant #BFC9C5
outline #899390   outlineVariant #3F4946
surfaceDim #0A1513  surfaceBright #303B39
surfaceContainerLowest #06100E  Low #131E1C  Default #172220  High #212C2A  Highest #2C3735
inverseSurface #D9E5E1  inverseOnSurface #273330  inversePrimary #26695F
```

### Extended colors (di luar M3)

```
success  light #008779  dark #43DDBD
warning  light #FF891C  dark #FFB86A
ambientGlowPrimary    light #9FF2E4  dark #005047
ambientGlowSecondary  light #CDE8E3  dark #00BE9F
ambientGlowTertiary   light #CCE5FF  dark #005047
brandGradientDark #014D48   brandGradientMid #0F7A6E   brandGradientLight #12A78E
```

### Spacing

`xs 4 · sm 8 · md 12 · lg 16 · xl 20 · xxl 24 · xxxl 32 · huge 40 · massive 48`
**Padding horizontal layar tetap 20dp di seluruh aplikasi.**

### Shape

`small 8dp · medium 14dp · large 24dp · extraLarge 28dp`

Pemakaian di desain ini: Button 14 · TextField 14 · Card 14 · ikon-kotak dalam ListRow 12 ·
Dialog 24 · BottomSheet 28 (atas saja) · search field pill 24 · chip 8 · badge 8 · avatar 50%.

### Motion

`fast 150ms · base 250ms · slow 400ms`
easing masuk `CubicBezier(0.05, 0.7, 0.1, 1)` · easing keluar `CubicBezier(0.3, 0, 0.8, 0.15)`

Dipakai: press-scale button 0.97–0.98 selama 150ms (fast, easing masuk) · transisi antar layar
fade 250ms · bottom sheet slide-up translateY 28dp → 0 selama 250ms · dialog scale 0.94 → 1 +
fade 250ms · logo splash fade + scale 0.86 → 1 selama 400ms · spinner rotasi 900ms linear infinite.

### Typography — Lato (satu keluarga, hierarki lewat weight)

| Peran | Ukuran / weight / line-height / tracking | Contoh |
|---|---|---|
| display | 28sp / 900 / 1.15 / 0 | judul onboarding, "Selamat Datang" (26sp di kartu login) |
| headline | 22sp / 900 / 1.2 | "DISKOLA" wordmark (tracking 7sp di splash, 6sp di login) |
| title | 20sp / 900 / 1.2 | nama pengguna di layar Password |
| titleSmall | 16–17sp / 900 | "Login Akun", judul dialog & sheet |
| body | 15sp / 400 / 1.55 | isi field, deskripsi onboarding |
| bodySmall | 13sp / 400 / 1.5–1.65 | teks bantuan, isi dialog |
| label | 12sp / 700 | label field ("NISN / NIS / NIK") |
| labelSmall | 11sp / 700–900 | label bottom nav, caption kartu |
| overline | 9–11sp / 900 / tracking 1.6–2.2sp, UPPERCASE | "MASUK SEBAGAI", "SISTEM INFORMASI AKADEMIK TERPADU", "KELAS BERLANGSUNG" |

Ukuran teks terkecil di desain: 9sp hanya untuk overline dekoratif; teks yang harus terbaca ≥11sp.

### Aturan token yang wajib dipatuhi

- Tidak ada hex hardcode di luar `:core:designsystem`.
- `primaryContainer` **gelap di kedua mode** — jangan dipakai sebagai latar badge/ikon di light
  mode. Untuk itu pakai `surfaceContainerLow` (#EDF6F3) atau `primary.copy(alpha = 0.1f)`.
  Semua latar ikon di desain ini memakai `surfaceContainerLow`.
- Semua target sentuh ≥48dp (tombol teks tinggi 48, tombol utama 52, row list min-height 48–56,
  icon button 44–48).
- Setiap layar berdata wajib punya state loading / empty / error dengan satu aksi pemulihan.

---

## Background patterns

Dari `docs/BACKGROUND.md`. Jangan mengganti motif ini dengan warna rata, dan jangan menambahkannya
ke layar list/form.

### Pola A — Ambient Gradient Wash (dipakai di SELURUH layar auth)

Chrome ini dipasang **sekali** membungkus NavHost auth (`AuthScaffold`), bukan per layar.

Light mode, 3 lapis bertumpuk:
1. Base tonal wash — gradient vertikal `#EDF6F3 0% → #F4FAF8 50% → #EDF6F3 100%`.
2. Brand arc — lapisan setinggi **34% tinggi layar** di atas, gradient vertikal
   `primary alpha .16 → primary alpha .04`, outline bawah 1dp `primary alpha .12`.
   Bentuk lengkung (di mockup didekati dengan polygon `0 0, 100% 0, 100% 64%, 50% 100%, 0 58%` —
   di Compose gambar dengan `Path` + `quadraticBezierTo` supaya lengkungnya organik).
3. Tiga ambient glow blob (radial gradient warna → transparan di 70%):
   - top-end, offset (+64, −80)dp, 288dp, blur 40dp, `primary` alpha .12
   - top-start, offset (−80, +112)dp, 240dp, blur 64dp, `ambientGlowPrimary #9FF2E4` alpha .25
   - bottom-end, offset (0, +64)dp, 320×240dp, blur 64dp, `primary` alpha .05

Dark mode: **tanpa brand arc**, base surface `#0A1513` + 3 blob lebih besar & terang:
   - top-start, offset (−80, −96)dp, 320dp, blur 90dp, `#005047` alpha .40
   - center-end, offset (+96, 0)dp, 280dp, blur 90dp, `#00BE9F` alpha .25
   - bottom-center, offset (0, +80)dp, 380×260dp, blur 100dp, `#005047` alpha .30

### Pola B — Hero banner (header Home / Pembelajaran)

Blok gradient vertikal `#014D48 0% → #0F7A6E 60% → #12A78E 100%`, tinggi **196dp**, sudut bawah
radius 28dp, membentang edge-to-edge (di belakang status bar).

> **Keputusan desain (revisi dari `docs/BACKGROUND.md`):** motif staircase **tidak dipakai** di
> header Home. Alasannya: siluet tangga membuat sebagian teks putih header berada di luar area
> hijau sehingga menyatu dengan surface terang. Header memakai blok penuh. Motif staircase versi
> vektor tetap didokumentasikan di opsi `1d` mockup bila nanti dipakai untuk header lain.

Kartu "Kelas berlangsung" dan konten di bawahnya berada dalam container `offset y = −56dp`
(menutupi sebagian hijau) dengan z-order di atas banner tapi **di bawah** bottom sheet & dialog.

### Pola C — Full-bleed celebration (layar sukses, Fase 2)

Base gradient diagonal 135° `#014D48 0% → #0E8377 55% → #12A78E 100%`; grid garis tipis
`ambientGlowPrimary` alpha .18 (spasi 26dp) di 36% kanan-atas; glyph logo Diskola besar di
kanan-atas opacity .16; cincin/pita translucent putih alpha .07 di kanan-bawah; wave alas bawah
26% tinggi dengan gradient `rgba(2,60,55,.35) → rgba(2,60,55,.6)` dan radius elips.
Khusus momen selebrasi — bukan untuk form/list.

---

## Screens / Views

Semua layar auth: lebar konten form dibatasi ~440dp dan di-center di layar lebar; padding
horizontal 20dp; edge-to-edge (`WindowInsetsControllerCompat` saja, jangan campur dengan
`systemUiVisibility`).

### 1. Splash

**Purpose:** cek versi aplikasi (`GET mobile/app/config/check-android-version`), lalu route.

**Layout:** kolom center-vertical & center-horizontal, gap 18dp. Watermark di dasar layar.

**Components:**
- Logo mark 108dp (`assets/logo-mark.png`), animasi fade + scale 0.86 → 1, 400ms easing masuk.
- Wordmark sebagai teks: "DISKOLA", 22sp / 900 / tracking 7sp / `onSurface`.
- Baris loading (margin-top 12dp): spinner 18dp, stroke 2.5dp, track `surfaceContainerHighest`,
  indikator `primary`, rotasi 900ms linear infinite + label "Memuat…" 14sp/400/`onSurfaceVariant`.
- Watermark bawah: "DISKOLA MOBILE V2.2.0", 10sp/400/tracking 1sp/`onSurfaceVariant` alpha .75,
  center, padding-bottom 12dp.

**Routing sesudah cek versi:**
1. Ada versi lebih baru → dialog **Update Wajib** (tidak bisa ditutup).
2. Belum pernah onboarding (`onboard` flag di DataStore false) → Onboarding.
3. Sudah login → Home. Jika ada payload notifikasi (`page`), route langsung ke halaman tujuan
   dan **lewati Home**.
4. Belum login → Login.

### 2. Onboarding (3 halaman, swipe pager)

**Layout:** kolom; header logo+wordmark kecil (logo 30dp + "DISKOLA" 13sp/900/tracking 3sp) di
kiri atas; area tengah center dengan gap 26dp; page indicator; baris tombol; padding 8/20/20dp.

**Components per halaman:**
- Ilustrasi: **placeholder** 252×196dp, radius 24dp, border 1.5dp dashed `outlineVariant`,
  fill `surfaceContainerLow`, isi ikon 52dp `primary` + caption "Ilustrasi · {tag}" 11sp/700.
- Judul 25sp/900/line-height 1.25, center.
- Deskripsi 15sp/400/line-height 1.6/`onSurfaceVariant`, center.
- Page indicator: 3 dot tinggi 6dp radius 3dp `primary`; aktif width 22dp opacity 1, non-aktif
  width 6dp opacity 0.28; transisi 250ms easing masuk.
- Tombol: "kembali" (text button, tinggi 52dp, `primary`, hanya di halaman 2 & 3) +
  tombol utama flex-1 tinggi 52dp radius 14dp `primary`/`onPrimary` 15sp/900 —
  label "berikutnya" di halaman 1–2, "selesai" di halaman 3.

**Copy final:**

| # | Tag ilustrasi | Ikon | Judul | Deskripsi |
|---|---|---|---|---|
| 1 | Pembelajaran | `auto_stories` | Belajar tanpa batas ruang kelas | Materi, tugas, dan jurnal kelas dalam satu tempat — semua yang kamu butuhkan untuk mengikuti pelajaran. |
| 2 | Kewirausahaan | `storefront` | Toko sekolah di saku kamu | Kelola produk, pesanan, dan pemasukan unit usaha sekolah langsung dari ponsel. |
| 3 | Dompet digital | `account_balance_wallet` | Bayar sekolah tanpa antre | SPP, tagihan, dan top up saldo Klaspay cukup dari satu dompet digital. |

Setelah "selesai": set flag `onboard = true` permanen (DataStore), lalu ke Login.

### 3. Login — Masukkan NISN

**Layout:** kolom scrollable, padding 12/20/28dp, gap 18dp.

**Components:**
1. **Header** (kolom center, gap 8dp): logo 56dp · "DISKOLA" 20sp/900/tracking 6sp ·
   "SISTEM INFORMASI AKADEMIK TERPADU" 9sp/900/tracking 1.6sp/`onSurfaceVariant` ·
   badge "Aktif" tinggi 26dp radius 13dp, fill `surfaceContainerLow`, dot 6dp `success`,
   label 10sp/900 `success`.
2. **Judul:** "Selamat Datang" 26sp/900 + sub "Masuk untuk melanjutkan ke portal sekolahmu."
   14sp/400/`onSurfaceVariant`.
3. **Kartu "Login Akun"** — `surfaceContainerLowest`, border 1dp `outlineVariant`, radius 14dp,
   padding 20dp, gap 16dp:
   - Judul "Login Akun" 16sp/900.
   - Field **NISN / NIS / NIK**: label 12sp/700/`onSurfaceVariant`; input tinggi 52dp radius 14dp
     border 1.5dp `outlineVariant` (focus: 2dp `primary`), padding-x 16dp, teks 15sp/400,
     placeholder "Contoh: 0051234567", keyboard numeric, **filter non-digit**.
   - Field **Sekolah**: tombol tinggi 52dp radius 14dp border 1.5dp `outlineVariant`, teks kiri
     ("Pilih sekolah" `outline` bila kosong, nama sekolah `onSurface` bila terpilih, ellipsis),
     trailing ikon `expand_more` 22dp `onSurfaceVariant`. Klik → bottom sheet Pilih Sekolah.
   - **Checkbox Syarat & Ketentuan**: row min-height 48dp, ikon `check_box_outline_blank`/
     `check_box` 22dp (`outline` / `primary`), teks 13sp/400/line-height 1.5 —
     "Saya menyetujui **Syarat & Ketentuan** penggunaan Diskola." Frasa "Syarat & Ketentuan"
     `primary` bold dan **clickable terpisah** → dialog Kebijakan.
   - Tombol **"Masuk"**: tinggi 52dp radius 14dp `primary`/`onPrimary` 15sp/900.
     **Enabled hanya jika NISN terisi (≥4 digit di mockup — pakai aturan validasi backend), sekolah
     terpilih, dan checkbox tercentang.** Disabled = opacity 0.45. Press = scale 0.98/150ms.
     Punya loading state (spinner menggantikan label).
   - Divider "atau": garis 1dp `outlineVariant` di kedua sisi, teks 12sp/400/`onSurfaceVariant`.
   - Tombol **"Masuk dengan Akun Google"**: outlined, tinggi 52dp radius 14dp border 1.5dp
     `outline`, teks 14sp/900 `onSurface`, leading logo Google 20dp (**placeholder** di mockup).
4. **Kartu bantuan**: `surfaceContainerLow` radius 14dp padding 16dp, teks 13sp/400 —
   "**NISN/NIK belum terdaftar?** Hubungi operator sekolah Anda untuk pendaftaran akun." +
   tombol outlined "Bantuan" tinggi 44dp radius 14dp, align start.

**Aksi:** "Masuk" → `POST mobile/app/authentication/check-account` → layar Password.

### 4. Bottom Sheet — Pilih Sekolah

**Layout:** menempel bawah, max-height 76% layar, `surfaceContainer` (#E7F0ED / #172220),
radius atas 28dp, animasi slide-up 250ms easing masuk, scrim `rgba(0,0,0,.42)` fade 200ms
(tap scrim = tutup).

**Components:**
- Drag handle 32×4dp radius 2dp `outlineVariant`, center.
- Baris judul: "Pilih Lembaga Sekolah" 17sp/900 + icon button `close` 44dp.
- Search field: tinggi 48dp radius 24dp fill `surfaceContainerLowest`, leading ikon `search` 20dp,
  input 14sp placeholder "Cari nama sekolah", **debounce** (300ms) → `GET mobile/app/authentication/schools`.
- List item (min-height 48dp, padding 12/8dp, radius 14dp, ripple `surfaceContainerLow`):
  avatar inisial 44dp radius 12dp fill `surfaceContainerLow` teks 13sp/900 `primary` ·
  nama 14sp/700 · kota 12sp/400 `onSurfaceVariant` · trailing `check_circle` 22dp `primary`
  hanya pada item terpilih.
- Footer loading (paginasi infinite scroll): spinner 16dp + "Memuat daftar sekolah…" 12sp.
- Empty state: "Sekolah tidak ditemukan".

**Data contoh mockup:** SMK DEMO (Jombang, Jawa Timur) · SMA Demo Nusantara 1 (Surabaya) ·
MTs Mambaul Maarif (Jombang) · SMK Robithotul Hikmah (Kediri) · SMP Demo Harapan (Malang).

### 5. Login — Masukkan Password

**Layout:** kolom scrollable, padding 4/20/28dp, gap 20dp.

**Components:**
- App bar minimal: icon button `arrow_back` 48dp (offset −12dp agar optical align 20dp) +
  logo 30dp di kanan.
- Blok identitas (center, gap 10dp): overline "MASUK SEBAGAI" 10sp/900/tracking 2.2sp ·
  avatar 84dp bulat fill `surfaceContainerLow` border 2dp `outlineVariant`, inisial 26sp/900
  `primary` · nama 20sp/900 line-height 1.3 max-width 280dp · row badge peran
  (tinggi 24dp radius 12dp `secondaryContainer`/`onSecondaryContainer` 10sp/900 tracking .6sp,
  "SISWA"/"GURU") + label kelas 13sp/400 `onSurfaceVariant` · baris identitas 12sp/400.
- Kartu form (`surfaceContainerLowest`, border 1dp, radius 14dp, padding 20dp, gap 14dp):
  field Password tinggi 52dp radius 14dp border 1.5dp, trailing icon button 40dp
  `visibility`/`visibility_off` 20dp untuk toggle show/hide (default hidden) ·
  baris "Lupa password? **Reset**" 13sp (Reset `primary` bold, clickable → alur Reset Password) ·
  tombol "Login" tinggi 52dp `primary`, disabled sampai password ≥4 karakter, loading state.
- Kartu "Hubungi Kami" (`surfaceContainerLow` radius 14dp padding 16dp gap 10dp):
  judul 13sp/900 · row ikon `chat` 18dp `primary` + "WhatsApp operator · 0812-3456-7890" ·
  row ikon `call` + "Telepon sekolah · (0321) 123456".

**Varian peran** (data mockup: Muhammad Dliyaurrahman Muzakki, inisial MD):

| | Badge | Sub-label | Baris identitas |
|---|---|---|---|
| Siswa | SISWA | Kelas 10 · Informatika | NISN 0051234567 |
| Guru | GURU | Guru Mapel · Informatika | NIK 3517041505900001 |

**Aksi:** "Login" → `POST mobile/app/authentication/login-account`. Sukses → jika masih memakai
password default, munculkan notifikasi/dialog ganti password → Home.

### 6. Login — SSO Google

**Layout & app bar** sama dengan layar Password; padding 4/20/28dp, gap 20dp.

**Components:**
- Blok identitas: overline "MASUK SEBAGAI" · avatar 84dp bulat border 1.5dp **dashed**
  `outlineVariant` dengan ikon `person` 38dp (placeholder foto akun Google) · nama 18sp/900 ·
  email 13sp/400 `onSurfaceVariant`.
- Kartu detail (`surfaceContainerLowest`, border 1dp, radius 14dp, padding-x 16dp): 4 baris
  min-height 48dp dengan divider 1dp di antaranya — Peran / NISN / Kelas / Sekolah,
  label kiri 13sp/400 `onSurfaceVariant`, nilai kanan 13sp/700.
  Jika akun **belum terhubung sekolah**, baris Sekolah diganti field "Pilih sekolah"
  (bottom sheet yang sama).
- Tombol: "Iya, itu saya" filled 52dp + "Tidak, bukan saya" text button 48dp.
  Jika belum terhubung: satu tombol "Daftar", aktif setelah sekolah dipilih.

**Alur:** tombol Google di Login → Credentials API / Google Sign-In → `POST mobile/app/authentication/login-sso`.
Jika akun belum terhubung sekolah → **dialog Konfirmasi Tamu** → halaman SSO untuk memilih sekolah
→ daftar/masuk. Setelah sukses: `klaspayActive == true` → Home, selain itu → Aktivasi Klaspay.

### 7. Dialog — Syarat & Ketentuan

Scrim `rgba(0,0,0,.42)`, padding 24dp, kartu `surfaceContainer` radius 24dp, animasi scale
0.94 → 1 + fade 250ms.

- Judul "Syarat & Ketentuan" 17sp/900, padding 20/20/12dp.
- Konten **scrollable** 13sp/400 line-height 1.65 `onSurfaceVariant`, sub-judul 13sp/900
  `onSurface`. Di produksi ini konten HTML dari endpoint `policy` (WebView + loading spinner).
  Bagian di mockup: 1. Penggunaan Akun · 2. Data Akademik · 3. Transaksi Pembayaran ·
  4. Kebijakan Privasi.
- Tombol "SAYA PAHAM" full-width 52dp radius 14dp `primary`, tracking .6sp —
  **menandai checkbox terms di layar Login lalu menutup dialog.**

### 8. Dialog — Konfirmasi Lanjut Sebagai Tamu

Kartu `surfaceContainer` radius 24dp padding 24dp gap 14dp.

- Judul "Konfirmasi" 17sp/900.
- Penjelasan 13sp/400/1.6: "Akun Google ini belum terhubung ke sekolah manapun. Anda akan
  melanjutkan sebagai **tamu** dengan akses terbatas. Ketik frasa berikut untuk melanjutkan:"
- Kotak frasa: padding 12/14dp radius 8dp fill `surfaceContainerHighest`, teks
  "SAYA MENGERTI" 14sp/900 tracking 1sp, center.
- Input konfirmasi tinggi 52dp radius 14dp border 1.5dp (focus 2dp `primary`),
  placeholder "Ketik frasa di atas".
- Baris tombol gap 8dp: "Batal" outlined flex-1 · "Lanjut" filled flex-1,
  **enabled hanya jika input == frasa** (perbandingan trim + uppercase), disabled opacity 0.45.

### 9. Dialog — Update Wajib

**Tidak bisa ditutup** (non-dismissible: tanpa tap-outside, tanpa back). Scrim `rgba(0,0,0,.55)`.
Kartu `surfaceContainer` radius 24dp padding 26/24dp, center, gap 14dp:
ikon bulat 76dp fill `surfaceContainerLow` dengan `system_update` 38dp `primary` ·
judul "Update Tersedia" 18sp/900 · pesan 13sp/400/1.6 · tombol "Update" full-width 52dp
`primary` (buka Play Store, lalu tutup aplikasi).

### 10. Home — Shell + Pembelajaran (tujuan akhir Fase 1)

Bottom navigation **3 tab** sesuai kondisi produksi: `Pembelajaran` · `Pembayaran` · `Akun`
(brief menyebut 6 tab; 3 lainnya belum aktif — dikonfirmasi mengikuti kondisi live).

**Header (Pola B):** blok gradient tinggi 196dp radius bawah 28dp, edge-to-edge di belakang
status bar. Konten di atasnya, padding 12/20dp, gap 16dp:
- Row sekolah: avatar inisial 38dp bulat putih (teks `#014D48` 13sp/900) · nama sekolah 15sp/900
  putih + kota 11sp/400 putih alpha .82 · icon button `refresh` 44dp putih ·
  icon button `notifications` 44dp putih dengan badge counter (min 16dp, radius 8dp, `#BA1A1A`,
  teks putih 10sp/900) di kanan-atas.
- Row pengguna: avatar 48dp bulat fill putih alpha .22 border 1.5dp putih alpha .5, inisial
  16sp/900 putih · "Assalamualaikum," 11sp/400 putih alpha .82 + nama 16sp/900 putih (ellipsis).

**Konten** — container `offset y = −56dp`, padding-x 20dp, padding-bottom 24dp, gap 16dp,
z-order di atas banner dan di bawah sheet/dialog:
1. **Kartu "Kelas berlangsung"** — `surfaceContainerLowest` radius 14dp padding 16dp,
   shadow `0 4dp 16dp rgba(0,0,0,.10)`, gap 12dp:
   dot 8dp `success` + overline "KELAS BERLANGSUNG" 11sp/900 tracking 1sp `success` ·
   row ikon-kotak 46dp radius 12dp `surfaceContainerLow` (ikon `code` 24dp `primary`) +
   "Informatika" 15sp/900 + "07.30 – 09.00 · Ruang Lab 2" 12sp/400 `onSurfaceVariant` ·
   row 2 tombol gap 8dp tinggi 48dp radius 14dp: "Hadiri" (`primary`/`onPrimary`) dan
   "Isi Jurnal" (`secondaryContainer`/`onSecondaryContainer`).
2. **Grid menu 3 kolom**, gap 12dp — judul section "Menu Pembelajaran" 14sp/900.
   Tiap sel: `surfaceContainerLowest` border 1dp `outlineVariant` radius 14dp, padding 14/4dp,
   ikon 26dp `primary` + label 11sp/700 center. Item & ikon:
   Materi `auto_stories` · Tugas `assignment` · Presensi `how_to_reg` · Jurnal `edit_note` ·
   Agenda `event_note` · Asesmen `quiz` · Poin `workspace_premium` · Magang `business_center` ·
   Perpus `menu_book`. (Item terkunci mendapat badge "segera" — lihat inventaris UI.)
3. **Carousel banner** — placeholder tinggi 112dp radius 14dp dashed, 3 slide.

**Bottom navigation:** `surfaceContainer`, border-top 1dp `outlineVariant`, padding 8/8/4dp.
Tab aktif: pill tinggi 32dp padding-x 20dp radius 16dp `secondaryContainer` dengan ikon 22dp
`onSecondaryContainer`, label 11sp/900 `onSurface`. Tab non-aktif: ikon/label 22dp/11sp/400
`onSurfaceVariant`. Tab Akun memakai avatar 24dp, bukan ikon.

---

## Komponen `:core:designsystem` (dari `design/DiskolaKit.dc.html`)

Semua sudah didesain dengan `@Preview` light + dark sebagai target.

**Button** — 4 varian × 3 state:

| Varian | Enabled | Pressed | Disabled |
|---|---|---|---|
| Filled | h52 r14 `primary`/`onPrimary` 14sp/900 | scale .97 (150ms) + `primaryContainer`/`onPrimaryContainer` | `surfaceContainerHighest`/`onSurfaceVariant`, opacity .55 |
| Tonal | h52 r14 `secondaryContainer`/`onSecondaryContainer` | scale .97 + `surfaceContainerHigh` | idem, opacity .55 |
| Outlined | h52 r14 border 1.5dp `outline`, teks `primary` | border `primary` + fill `surfaceContainerLow` | border `outlineVariant`, teks `onSurfaceVariant`, opacity .55 |
| Text | h48 r14 transparan, teks `primary` | fill `surfaceContainerLow` | teks `onSurfaceVariant`, opacity .55 |

**TextField** — h52 r14, 4 state: kosong (border 1.5dp `outlineVariant`, placeholder `outline`) ·
focused (border 2dp `primary`, label `primary`) · error (border 2dp `error`, label `error`,
**InlineErrorView** di bawah field: ikon `error` 16dp + teks 11sp `error`) ·
readonly (fill `surfaceContainer`, teks `onSurfaceVariant`, trailing `check_circle` `success`).

**Card** — elevated (`surfaceContainerLowest`, r14, shadow `0 2dp 8dp rgba(0,0,0,.08)`) ·
outlined (transparan, border 1.5dp `outlineVariant`, r14 — untuk kartu form di atas Pola A) ·
filled (`surfaceContainerLow`, r14 — kartu bantuan/informasi sekunder).

**ListRow** — min-height 56dp, padding 14/16dp, gap 16dp; leading ikon-kotak 44dp r12
`surfaceContainerLow` + ikon 22dp `primary`; title 15sp/700 + subtitle 12sp/400
`onSurfaceVariant`; trailing `chevron_right` 22dp; hover/ripple `surfaceContainerLow`;
divider 1dp `outlineVariant` dengan inset kiri 76dp.

**Chip / Badge** — filter chip aktif (h36 r8 `primary`/`onPrimary` 13sp/700, leading `check` 16dp)
dan non-aktif (border 1.5dp `outlineVariant`) · status badge h28 r8 `surfaceContainerLow` dengan
dot 6dp + label 11sp/900 (`success` Hadir, `warning` Terlambat) · badge error
(`errorContainer`/`onErrorContainer`, "Alpa") · counter badge min 20dp r10 `error` teks putih 11sp/900.

**AppDialog** (pengganti `prettyAlert`) — `surfaceContainer` r24 padding 24dp gap 14dp, center:
slot ilustrasi bulat 72–76dp `surfaceContainerLow` + ikon 34–38dp `primary` · judul 18sp/900 ·
body 14sp/400/1.55 `onSurfaceVariant` · tombol primary full-width 52dp + tombol secondary text 48dp.

**AppBottomSheet** — `surfaceContainer` radius atas 28dp, padding 12/20/20dp, drag handle 32×4dp,
baris judul 16sp/900 + `close` 44dp, list opsi radio min-height 48dp
(`radio_button_checked`/`radio_button_unchecked` 22dp), tombol "Terapkan" 52dp.

**LoadingState** — skeleton row (kotak 44dp r12 + 2 bar 12dp/10dp r6, fill `surfaceContainerHigh`,
baris berikutnya opacity .7) + baris spinner 18dp/2.5dp + label 12sp.

**EmptyState** — ikon bulat 64dp `surfaceContainerLow` + ikon 30dp `onSurfaceVariant` ·
judul 14sp/900 · deskripsi 12sp/400/1.5 · **satu aksi pemulihan** outlined 44dp ("Muat ulang").

**ErrorState** — sama, ikon bulat `errorContainer` + ikon `onErrorContainer` (`wifi_off`),
aksi filled ("Coba Lagi").

**BannerError** — row padding 12/14dp r14 `errorContainer`, ikon `warning` 20dp
`onErrorContainer`, teks 12sp/700/1.4, tombol `close` — dapat ditutup.

---

## Interactions & Behavior

### Navigasi

Satu Activity, Navigation Compose route type-safe (`@Serializable`). Graph `:feature:auth`:
`Splash → Onboarding → Login → Password → Home`, plus `Sso` dan `ResetPassword`.
Chrome Pola A dipasang **sekali** membungkus NavHost auth.

Transisi antar layar: fade 250ms. Back dari Password/SSO kembali ke Login dengan state form
Login **tetap tersimpan** (NISN, sekolah, checkbox).

### Validasi form

| Layar | Aturan enable |
|---|---|
| Login NISN | NISN terisi **dan** sekolah terpilih **dan** terms tercentang |
| Login Password | password terisi (≥ panjang minimum backend) |
| Dialog Tamu | input == "SAYA MENGERTI" (trim, case-insensitive) |
| SSO (belum terhubung) | sekolah terpilih |

Input NISN memfilter karakter non-digit saat diketik.

### Data fetching

Setiap layar memuat datanya **satu kali** lewat `init` ViewModel atau `LaunchedEffect(Unit)`.
Refresh manual hanya lewat aksi eksplisit (tombol refresh / pull-to-refresh).
**Jangan** memicu fetch dari `onResume`-equivalent tanpa alasan yang ditulis di komentar.
Guard anti double-fetch: set flag **sebelum** `launch`, bukan di dalam coroutine.

Search sekolah: debounce 300ms + paginasi infinite scroll.

### Animasi (ringkasan implementasi)

| Elemen | Animasi |
|---|---|
| Logo splash | fade + `scaleIn(0.86f)`, 400ms, easing masuk |
| Transisi layar | fade 250ms |
| Bottom sheet | slide-up translateY 28dp → 0, 250ms easing masuk; scrim fade 200ms |
| Dialog | `scaleIn(0.94f)` + fade, 250ms easing masuk |
| Press tombol | scale 0.97–0.98, 150ms easing masuk |
| Page indicator | width 6dp ↔ 22dp + alpha, 250ms |
| Spinner | rotasi 900ms linear infinite |

---

## State Management

`StateFlow<XxxUiState>` + `collectAsStateWithLifecycle()`; **jangan pakai LiveData**.
Semua panggilan jaringan mengembalikan `Result<T>` (`Success`/`Error`/`Loading`).

```kotlin
data class SplashUiState(
    val loading: Boolean = true,
    val updateRequired: Boolean = false,
    val destination: AuthDestination? = null, // Onboarding | Login | Home(page?)
    val error: UiText? = null,
)

data class OnboardingUiState(val page: Int = 0)   // 0..2

data class LoginUiState(
    val nisn: String = "",
    val school: School? = null,
    val termsAccepted: Boolean = false,
    val submitting: Boolean = false,
    val canSubmit: Boolean = false,                // derived
    val policyDialog: PolicyDialogState? = null,
    val schoolSheet: SchoolSheetState? = null,
    val error: UiText? = null,
)

data class SchoolSheetState(
    val visible: Boolean = false,
    val query: String = "",
    val items: List<School> = emptyList(),
    val loadingMore: Boolean = false,
    val page: Int = 1,
    val endReached: Boolean = false,
    val empty: Boolean = false,
)

data class PasswordUiState(
    val account: Account,                          // nama, avatar, peran, kelas, id
    val password: String = "",
    val passwordVisible: Boolean = false,
    val submitting: Boolean = false,
    val mustChangeDefaultPassword: Boolean = false,
    val error: UiText? = null,
)

data class SsoUiState(
    val googleAccount: GoogleAccount,
    val linked: Boolean,
    val school: School? = null,
    val guestDialog: GuestDialogState? = null,     // phrase input + canContinue
    val submitting: Boolean = false,
    val error: UiText? = null,
)
```

Semua state layar **satu** data class; Composable stateless dan tidak pernah menyentuh
Repository/ApiService.

### Unit test minimum

Setiap ViewModel: transisi state jalur sukses **dan** jalur gagal. Setiap Repository: mapping DTO →
domain model dan cache policy. Khusus Fase 1: routing Splash (4 cabang), aturan `canSubmit` Login,
perbandingan frasa dialog Tamu, dan alur logout (blokir bila ada ujian belum dikumpulkan).

---

## Kontrak API (Fase 1)

Base URL per build type lewat `buildConfigField`. Serialization: **kotlinx.serialization saja**.

```
GET    mobile/app/config/check-android-version
GET    mobile/app/authentication/schools           (search + paginasi)
POST   mobile/app/authentication/check-account
POST   mobile/app/authentication/login-account
POST   mobile/app/authentication/login-sso
POST   mobile/app/authentication/reset-password
GET    policy                                      (konten HTML S&K)
DELETE logout
GET    sessions
```

Interceptor: auth token · logging (debug saja) · error mapping ke `Result.Error` dengan pesan siap
tampil. FCM: router payload `page` → destinasi navigasi, **didefinisikan di satu file**.

---

## Anti-pattern yang harus dihindari (dari `docs/BRIEF.md` §7)

1. Fetch ganda saat layar dibuka — satu layar, satu pemicu muat awal.
2. Guard race condition — set flag sebelum `launch`.
3. Aset gambar raksasa — Pola A/B/C digambar dengan vektor/Canvas, bukan PNG besar.
4. Mencampur API system bar lama & baru — `WindowInsetsControllerCompat` saja.
5. `finishAffinity()` untuk pindah layar.
6. Chrome yang diduplikasi per layar — `AuthScaffold` sekali membungkus NavHost.
7. Komponen bawaan tanpa identitas — pakai `AppDialog` dan bottom sheet sendiri.
8. Logika bisnis di UI.
9. Satu Dagger component raksasa — pakai Hilt.
10. Dua library serialization / dua sistem binding.
11. Nilai hardcode tersebar — semua dp/warna/durasi dari token.

---

## Assets

| File | Asal | Catatan |
|---|---|---|
| `assets/logo-mark.png` | crop dari `husnunn/diskola-rebrand@main` `app/src/main/res/drawable/ic_launcher_playstore.png` | Mark isometrik (mortarboard/kubus). Untuk produksi minta versi vektor/SVG. |
| `assets/logo-wordmark.png` | crop dari file yang sama | Wordmark hitam — **jangan** dipakai di dark mode; di desain ini wordmark dirender sebagai teks Lato 900. |
| Font Lato | Google Fonts | Bundle di `:core:designsystem` sebagai resource font, weight 300/400/700/900. |
| Ikon | Material Symbols Rounded (mockup) | Ganti dengan `androidx.compose.material.icons` / Material Symbols di Compose. Nama ikon dicantumkan per komponen di atas. |
| Ilustrasi onboarding | **belum ada** | 3 ilustrasi (pembelajaran, kewirausahaan, dompet digital), rasio ±252×196. |
| Logo Google | **belum ada** | Pakai aset resmi Google Sign-In branding. |

---

## Files

| File | Isi |
|---|---|
| `design/Diskola Mobile.dc.html` | Papan utama. Opsi `1a` prototipe Auth interaktif (state, alur, semua dialog) · `1b` kit Fase 0 light + dark · `1c` 3 varian tata letak Login · `1d` varian motif background Pola A/B/C · `1e` 3 tafsir komponen dasar. Logic class di bagian `<script data-dc-script>` berisi state machine alur auth — rujukan untuk `LoginUiState` dkk. |
| `design/DiskolaKit.dc.html` | Galeri komponen `:core:designsystem` (token, tipografi, spacing/shape/motion, Button, TextField, Card, ListRow, Chip, Dialog, BottomSheet, Loading/Empty/Error). Semua style memakai CSS variable yang namanya sepadan dengan role M3 — lihat tabel token di atas. |
| `design/android-frame.jsx` | Frame device Android (status bar + gesture nav) untuk konteks mockup. Bukan bagian desain aplikasi. |

### Catatan penting sebelum implementasi

`husnunn/app-diskola@main` masih **kosong** saat handoff ini dibuat, dan repo aplikasi lama
(produksi) tidak tersedia. Artinya seluruh alur, label, dan copy di dokumen ini berasal dari
`docs/BRIEF.md` + inventaris UI + panduan background — **bukan** hasil pembandingan langsung
dengan `ApiService.kt` dan layout XML aplikasi lama.

Sebelum menandai fitur selesai, bandingkan alur dengan aplikasi lama secara langsung (bukan dari
ingatan) sesuai `docs/BRIEF.md` §8, dan tulis setiap ketidaksesuaian di `docs/FLOW_QUESTIONS.md`
alih-alih memperbaikinya diam-diam.
