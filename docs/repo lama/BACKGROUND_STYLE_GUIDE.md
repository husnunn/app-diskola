# Panduan Background — Diskola Mobile

> **Kenapa dokumen ini ada:** saat membuat mockup/redesign layar (mis. lewat Claude Design),
> background yang dihasilkan cenderung flat/polos (satu warna solid). Padahal aplikasi Diskola
> **tidak pernah memakai flat color penuh layar** untuk layar hero/landing/status — selalu ada
> lapisan gradient + motif dekoratif. Dokumen ini menjelaskan motif itu persis (warna, bentuk,
> resep CSS/Compose) supaya bisa direplikasi, bukan diganti warna polos.

---

## 1. Akar masalah

Tiga sumber background di codebase, dan hanya satu yang benar-benar flat:

| Sumber | Contoh | Sifat |
|---|---|---|
| **Surface M3 standar** | Layar list/form biasa (Homework, Poin, Notifikasi list, dst) | **Memang flat** — `colorScheme.background`/`surface` polos itu benar untuk konten kerja/baca. Ini BUKAN yang dikeluhkan. |
| **Bitmap dekoratif legacy** | `img_bg_bayar.png` (header Pembelajaran/Pembayaran/Notifikasi), `bgsuccespage.png` (layar sukses pembayaran) | Gambar PNG besar dengan gradient + motif (lihat Bagian 2). Kalau mockup baru cuma niru "ada warna teal di atas" tanpa motifnya, hasilnya kelihatan polos dibanding aslinya. |
| **Gradient vektor baru (Compose, Wave 1)** | `AuthScaffold.kt` — dipakai di Splash/Onboarding/Login | Ini pengganti resmi bitmap lama, **sudah didesain ulang supaya tidak polos**: tonal wash + brand arc + blob cahaya blur. Ini rujukan pola yang benar untuk diteruskan ke fitur lain, BUKAN dihilangkan jadi warna solid. |

Jadi kalau hasil mockup terasa polos, kemungkinan besar prompt/referensi yang dipakai cuma menyebut
"background teal" tanpa menyebut motif stepped-arc / blob cahaya / ilustrasi selebrasi di bawah ini —
akibatnya alat generate cuma isi flat fill.

---

## 2. Tiga bahasa background yang dipakai Diskola

### Pola A — Ambient Gradient Wash (dipakai di alur Auth, ini pola PALING BARU & paling disarankan ditiru ke fitur lain)

Sumber: `app/src/main/java/id/diskola/app/pages/auth/components/AuthScaffold.kt`. Dipakai di Splash,
Onboarding, Login (NISN/Password/SSO). Terdiri dari 3 lapis, ditumpuk:

1. **Base tonal wash** — gradient vertikal lembut 3-stop: `surfaceContainerLow → surface →
   surfaceContainerLow` (bukan satu warna rata, ada sedikit "nafas" gelap-terang-gelap dari atas ke
   bawah).
2. **Brand arc** — bentuk lengkung organik (bekas motif staircase/wave lama, sekarang di-vector-kan)
   digambar dari kiri-atas ke kanan-bawah, mengisi ±34% tinggi layar teratas, diisi gradient
   `primary alpha 0.16 → primary alpha 0.04` + outline tipis `primary alpha 0.12`.
3. **Ambient glow blobs** — 2-3 lingkaran besar (240–380dp), di-blur 40-100dp, radial gradient dari
   warna solid ke transparan, diletakkan di sudut-sudut layar (bukan di tengah) supaya konten tetap
   terbaca. Warna & posisi beda light vs dark:

**Light mode:**
- Blob 1: top-end, offset (+64, -80)dp, size 288dp, blur 40dp, warna `primary alpha 0.12`
- Blob 2: top-start, offset (-80, +112)dp, size 240dp, blur 64dp, warna `ambientGlowPrimary
  (#9FF2E4) alpha 0.25`
- Blob 3: bottom-end, offset (0, +64)dp, size 320×240dp, blur 64dp, warna `primary alpha 0.05`

**Dark mode** (tanpa brand arc, langsung base surface + 3 blob lebih besar & lebih terang):
- Blob 1: top-start, offset (-80, -96)dp, size 320dp, blur 90dp, warna `ambientGlowPrimary
  (#005047) alpha 0.4`
- Blob 2: center-end, offset (+96, 0)dp, size 280dp, blur 90dp, warna `ambientGlowSecondary
  (#00BE9F) alpha 0.25`
- Blob 3: bottom-center, offset (0, +80)dp, size 380×260dp, blur 100dp, warna
  `ambientGlowTertiary (#005047) alpha 0.3`

**Resep CSS (untuk mockup HTML/Claude Design), light mode:**
```css
.screen-bg {
  position: relative;
  background: linear-gradient(180deg, #EDF6F3 0%, #F4FAF8 50%, #EDF6F3 100%);
  overflow: hidden;
}
.screen-bg::before { /* brand arc, ganti dgn SVG path kalau butuh presisi lengkung */
  content: "";
  position: absolute; inset: 0 0 66% 0;
  background: linear-gradient(180deg, rgba(0,106,96,0.16), rgba(0,106,96,0.04));
  clip-path: polygon(0 0, 100% 0, 100% 26%, 50% 41%, 0 22%);
}
.glow { position: absolute; border-radius: 50%; filter: blur(40px); pointer-events: none; }
.glow--1 { top: -80px; right: -64px; width: 288px; height: 288px;
  background: radial-gradient(circle, rgba(0,106,96,0.12), transparent 70%); }
.glow--2 { top: 112px; left: -80px; width: 240px; height: 240px; filter: blur(64px);
  background: radial-gradient(circle, rgba(159,242,228,0.25), transparent 70%); }
.glow--3 { bottom: -64px; right: 0; width: 320px; height: 240px; filter: blur(64px);
  background: radial-gradient(circle, rgba(0,106,96,0.05), transparent 70%); }
```

**Kapan dipakai:** layar auth (sudah jadi), dan disarankan jadi pola default untuk **semua layar
landing/hero baru** yang dulunya pakai bitmap besar (lihat Pola B) — supaya konsisten & theme-aware
tanpa perlu aset gambar.

---

### Pola B — Hero Curved Banner (header bergradasi bentuk tangga)

Sumber: `img_bg_bayar.png` (teal, ukuran asli ~1080×1920, dipakai `scaleType="fitXY"` setinggi 80–140dp
saja di bagian atas layar, bukan full-screen).

**Bentuk:** siluet "anak tangga" (staircase) menurun dari kanan-atas ke kiri-bawah — 4 blok kotak
bertingkat membentuk garis zig-zag horizontal, diisi gradient vertikal dari teal gelap (atas) ke teal
lebih terang/cerah (bawah blok tangga tertinggi di kanan).

**Warna gradient (sampling dari file asli):** `#014D48` (paling gelap, kiri-atas) → `#0F7A6E` →
`#12A78E` (paling terang, blok tangga kanan-atas) — arah gradient vertikal per-blok, bukan satu
gradient global.

**Resep CSS:**
```css
.hero-banner {
  height: 120px; /* atau sesuai dimen layar: pembelajaran ~140sdp, notifikasi ~80sdp */
  background: linear-gradient(180deg, #014D48 0%, #0F7A6E 60%, #12A78E 100%);
  clip-path: polygon(
    0% 55%, 12% 55%, 12% 78%, 30% 78%, 30% 45%,
    48% 45%, 48% 62%, 66% 62%, 66% 33%, 84% 33%,
    84% 0%, 100% 0%, 100% 100%, 0% 100%
  ); /* siluet tangga naik dari kiri ke kanan */
}
```

**Dipakai di:** header **Pembelajaran (Beranda)**, **Pembayaran (Wallet Dashboard)**, **Notifikasi
(daftar)** — konten (logo sekolah, avatar, judul, saldo) ditumpuk di atas banner ini, bukan di atas
warna solid.

**Rekomendasi migrasi:** ganti bitmap ini dengan bentuk vektor (SVG path/Canvas, sama seperti pola A)
supaya theme-aware (auto dark mode) dan tidak membawa file gambar besar — tapi **pertahankan motif
staircase-nya**, jangan diganti jadi persegi panjang warna rata.

---

### Pola C — Full-Bleed Celebration Illustration (layar status sukses)

Sumber: `bgsuccespage.png` (7.5MB, full-screen background layar sukses — Pembayaran Berhasil,
Partisipasi Berhasil, QR Pembayaran Berhasil).

**Komposisi berlapis** (dari atas ke bawah/depan ke belakang):
1. Base gradient diagonal teal gelap (`#014D48` kiri-atas) → teal medium-terang (`#0E8377`)
   memenuhi seluruh layar.
2. Pola garis "circuit board" tipis (jaringan garis+titik menyerupai jalur PCB) di sepertiga
   atas-kanan, opacity rendah (~15-20%), warna teal muda.
3. Glyph besar translucent — bentuk kubus isometrik/kristal (brand mark pencapaian), diletakkan di
   kanan-atas-tengah, opacity ~25-30%, tidak menutupi area teks utama.
4. Bentuk pita/loop melengkung besar translucent di kanan-bawah, opacity rendah, menambah kedalaman.
5. Wave/blob melengkung di bagian bawah layar (mengisi ~20% tinggi dari bawah) dengan sedikit drop
   shadow lembut — motif yang sama dengan "tangga"/"ombak" brand, jadi alasnya tidak terlihat kosong.

**Resep CSS (disederhanakan, cukup untuk mockup — tidak perlu presisi vektor kubus/circuit):**
```css
.success-bg {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(135deg, #014D48 0%, #0E8377 55%, #12A78E 100%);
  overflow: hidden;
}
.success-bg::before { /* wave alas bawah */
  content: "";
  position: absolute; left: -10%; right: -10%; bottom: -6%;
  height: 26%;
  background: radial-gradient(60% 100% at 30% 100%, rgba(255,255,255,0.10), transparent 70%),
              linear-gradient(180deg, rgba(2,60,55,0.35), rgba(2,60,55,0.55));
  border-radius: 50% 50% 0 0 / 100% 100% 0 0;
  filter: blur(2px);
}
.success-bg::after { /* glyph kubus translucent, boleh diganti ikon brand asli */
  content: "";
  position: absolute; top: 5%; right: -8%;
  width: 55%; aspect-ratio: 1;
  background: radial-gradient(circle, rgba(255,255,255,0.12), transparent 65%);
}
```

**Dipakai di:** `success_pay_page.xml`, `partisipasi_success_page.xml`,
`activity_success_pay_page_qr.xml` — semua layar "Transaksi/Pembayaran Berhasil".

**Aturan pemakaian:** pola ini KHUSUS untuk momen selebrasi/status akhir positif. Jangan dipakai untuk
layar form/list biasa — di situ tetap pakai surface flat standar (lihat Bagian 1, baris pertama).

---

## 3. Token warna dasar (dari `ui/theme/Color.kt`, dipakai semua resep di atas)

**Light:**
```
primary #006A60   primaryContainer #005048
background/surface #F4FAF8   surfaceContainerLow #EDF6F3
ambientGlowPrimary #9FF2E4   ambientGlowSecondary #CDE8E3   ambientGlowTertiary #CCE5FF
```

**Dark:**
```
primary #92D3C7   primaryContainer #005047
background/surface #0A1513
ambientGlowPrimary #005047   ambientGlowSecondary #00BE9F   ambientGlowTertiary #005047
```

Palet lengkap M3 (semua role warna) sudah didokumentasikan di `docs/REBUILD_PROMPT.md` Bagian 4 —
rujuk ke sana untuk warna komponen non-background (tombol, teks, dsb).

---

## 4. Aturan pemakaian — kapan pola apa

| Jenis layar | Pola background | Contoh |
|---|---|---|
| Auth (splash/onboarding/login) | **A — Ambient Gradient Wash** | Splash, Login NISN/Password/SSO |
| Landing/dashboard 3 tab utama | **B — Hero Curved Banner** di header saja, sisanya surface flat | Pembelajaran, Pembayaran, Notifikasi |
| Status akhir sukses/selebrasi | **C — Full-Bleed Celebration** | Pembayaran/Partisipasi/QR Berhasil |
| List, form, detail, pengaturan | **Flat surface M3 biasa** (memang benar polos) | Homework, Poin, Presensi, Akun, dst — mayoritas layar app |
| Dialog/bottom sheet | Flat `surfaceContainer` sesuai M3, tanpa motif tambahan | Semua dialog standar (`prettyAlert`, dst) |

**Prinsip:** hanya layar yang sifatnya "pintu masuk" (auth), "kanvas utama" (3 landing tab), atau
"momen emosional" (sukses) yang butuh background berlapis. Layar kerja/transaksional harian sengaja
flat supaya tidak mengganggu keterbacaan konten — itu bukan bug, itu memang desainnya. Jangan
menambahkan motif ke SEMUA layar; cukup ke tiga kategori di atas, sisanya biarkan flat.

---

## 5. Referensi implementasi asli

- Pola A (vektor, rujukan utama): `app/src/main/java/id/diskola/app/pages/auth/components/AuthScaffold.kt`
- Pola B (bitmap legacy, kandidat divektorkan): `app/src/main/res/drawable-nodpi/img_bg_bayar.png`,
  dipakai di `pembelajaran_page.xml`, `pembayaran_page.xml`, `notification_page.xml`
- Pola C (bitmap legacy, kandidat divektorkan): `app/src/main/res/drawable/bgsuccespage.png`, dipakai
  di `success_pay_page.xml`, `partisipasi_success_page.xml`, `activity_success_pay_page_qr.xml`
- Token warna: `app/src/main/java/id/diskola/app/ui/theme/Color.kt`
