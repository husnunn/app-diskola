# Plan: perbaikan hasil test diskola-v2

Status: **plan, belum dikerjakan.** Dokumen ini hanya berisi rencana; tidak ada kode yang diubah.
Dibuat: 2026-10-27.

## Ringkasan

| # | Catatan test | Akar masalah | Ukuran |
|---|---|---|---|
| 1 | Matikan dark mode | Tema Compose, latar gradien, dan tema XML semuanya mengikuti pengaturan sistem HP | Kecil |
| 2 | Manfaatkan autofocus | Hanya `PasswordScreen` yang punya autofocus; layar isian lain belum, dan `AppTextField` tidak punya parameter autofocus | Sedang |
| 3 | Layar isi password dan akun: gambar belum sesuai bingkai | Avatar `AsyncImage` tidak di-clip ke lingkaran dan tidak memakai `ContentScale.Crop` | Kecil |

Semua jalur file di bawah relatif terhadap `app/src/main/java/id/diskola/app/` kecuali disebut lain.

---

## 1. Matikan dark mode

### Temuan

- `DiskolaTheme` memakai `isSystemInDarkTheme()` sebagai nilai bawaan `darkTheme` (`ui/theme/Theme.kt:14`). Dipanggil dari `ui/MainActivity.kt:55` tanpa argumen, jadi mengikuti sistem.
- `AmbientGradientBackground` memanggil `isSystemInDarkTheme()` sendiri (`ui/components/Background.kt:31`). Latar gradien bisa gelap walau tema sudah dipaksa terang.
- `App.kt:60-71` membaca preferensi `theme_mode` (bawaan `system`) lalu memanggil `AppCompatDelegate.setDefaultNightMode`. Tidak ada layar yang mengisi `theme_mode` (hanya `SessionKeys.THEME_MODE` dan `App.kt`), sehingga hasilnya selalu "ikuti sistem".
- `MainActivity` turunan `ComponentActivity`, bukan `AppCompatActivity`. `AppCompatDelegate` kemungkinan besar tidak memengaruhi `isSystemInDarkTheme()` di Compose. (Dugaan; perlu dibuktikan saat pengerjaan.)
- Ada `res/values-night/themes.xml` dan `res/values-night/colors.xml`. Ini dipakai window sebelum Compose tampil, jadi HP bermode gelap bisa kedip gelap saat app dibuka walau Compose sudah terang.
- Preview bermode gelap (`UI_MODE_NIGHT_YES`) ada di beberapa file (`HomePreviews.kt`, `AkmPrimingScreen.kt`, `ExamPenaltyDialog.kt`, `AkmWatermark.kt`).

### Rencana

1. Satu sumber kebenaran: paksa tema terang di `DiskolaTheme` (nilai bawaan `darkTheme` tidak lagi mengikuti sistem).
2. `AmbientGradientBackground` memakai flag yang sama, bukan memanggil `isSystemInDarkTheme()` sendiri.
3. `App.kt`: set `MODE_NIGHT_NO` secara tetap dan hentikan pembacaan `theme_mode`.
4. Tema XML: hapus `values-night/themes.xml` (dan `values-night/colors.xml` bila tidak dipakai di tempat lain) supaya window awal selalu terang. Ini menghilangkan kedip gelap saat cold start.
5. Skema warna gelap (`DiskolaDarkColorScheme`, `DarkExtendedColors`) boleh dibiarkan sebagai kode tidak aktif kalau dark mode mungkin dihidupkan lagi; kalau tidak, hapus. **Perlu keputusan** (lihat pertanyaan terbuka).
6. Preview bermode gelap: hapus atau biarkan, sesuai keputusan di langkah 5.

### Verifikasi

- Nyalakan mode gelap di pengaturan HP, buka app dari kondisi mati total: tidak ada layar gelap atau kedip di awal, dan semua layar tetap terang.
- Cek bar status dan bar navigasi: ikon tetap gelap di atas latar terang.
- Layar yang memakai `AmbientGradientBackground` (Login, Password, SSO) tampil dengan gradien terang.

### Risiko

- Layar atau komponen yang diam-diam bergantung pada `isSystemInDarkTheme()` di tempat lain. Saat ini hanya dua pemanggil non-preview yang ditemukan (`Theme.kt`, `Background.kt`).
- Pengguna yang butuh mode gelap kehilangan pilihan. Keputusan produk, bukan teknis.

---

## 2. Manfaatkan autofocus

### Temuan

- Autofocus hanya ada di `ui/screens/auth/PasswordScreen.kt:88-101`: `FocusRequester`, lalu `keyboardController.show()`, tunggu 200 ms, `show()` lagi. Cara ini manual dan diulang kalau ditiru di layar lain.
- Layar isian yang belum punya fokus otomatis (jumlah `AppTextField`, `focusRequester` = 0 di semuanya):
  - `ui/screens/auth/LoginScreen.kt` (2 field: NISN/NIS/NIK dan pilih sekolah)
  - `ui/screens/auth/ResetPasswordScreen.kt` (1 field)
  - `ui/screens/auth/SsoScreen.kt` (1 field)
  - `ui/screens/akun/AkunSubScreens.kt` (Ganti Kata Sandi, 3 field)
  - `ui/screens/pembayaran/SetPinScreen.kt` (2 field)
- `ui/components/TextField.kt` (`AppTextField`) punya `imeAction` dan `onImeAction` (hanya untuk Done), tetapi tidak punya parameter autofocus.
- Form panjang (jurnal, poin, tugas, presensi izin) punya banyak field dan sebagian memakai bottom bar melekat.

### Rencana

1. **Satu mekanisme di `AppTextField`**: tambah parameter `autoFocus` (bawaan `false`). Di dalamnya `FocusRequester` internal + efek yang meminta fokus sekali saat pertama tampil, lalu memunculkan keyboard. Logika `delay(200)` dari `PasswordScreen` dipindah ke sini supaya tidak diulang.
2. **Pasang di field pertama layar isian tunggal**: Login (NISN), Reset Password, SSO, Ganti Kata Sandi (kata sandi lama), Set PIN. `PasswordScreen` diganti memakai parameter baru, bukan kode manualnya.
3. **Alur keyboard**: `ImeAction.Next` di field non-terakhir (fokus pindah otomatis ke field berikutnya), `ImeAction.Done` di field terakhir yang memicu aksi utama bila form valid. `AppTextField` saat ini hanya menangani `onDone`; perlu dicek apakah perilaku default Compose untuk `Next` sudah cukup.
4. **Jangan diterapkan** pada form panjang (jurnal, poin, tugas, presensi izin): keyboard yang langsung muncul menutupi isi form dan bottom bar.
5. Login punya field kedua berupa pemilih sekolah (bukan input ketik). Fokus otomatis hanya di NISN; pemilih sekolah tetap dibuka dengan ketukan.

### Verifikasi

- Buka tiap layar di daftar: kursor langsung ada di field pertama dan keyboard muncul tanpa ketukan.
- Putar layar dan kembali dari layar lain: tidak muncul keyboard berulang atau fokus lompat.
- Tombol aksi di bawah tidak tertutup keyboard (perilaku `bringIntoView` di `AppTextField` tetap bekerja).
- Hardware keyboard dan TalkBack: urutan fokus masuk akal.

### Risiko

- Layar kecil: keyboard yang langsung terbuka bisa menutupi teks bantuan atau tombol.
- Rebuild/recomposition yang membuat fokus diminta berulang; efek harus diikat ke kunci yang stabil, sekali per tampil.
- Tombol "simpan perubahan" di Ganti Kata Sandi masih `onClick = {}` (`ui/screens/akun/AkunSubScreens.kt:184`). Autofocus di sana baru terasa penuh setelah layarnya benar-benar tersambung ke API.

---

## 3. Gambar tidak sesuai bingkai (layar isi password dan akun)

### Temuan

- Avatar ditampilkan dengan `AsyncImage` yang diberi `.background(..., CircleShape)` dan `.border(..., CircleShape)`, tetapi **tanpa `.clip(CircleShape)` dan tanpa `contentScale = ContentScale.Crop`**. Di Compose, `background` dan `border` tidak memotong konten. Foto yang tidak persegi tampil utuh (`ContentScale.Fit` bawaan) dan sudutnya keluar dari lingkaran.
- Lokasi yang terkena:
  - `ui/screens/auth/PasswordScreen.kt:148-155` (layar isi password)
  - `ui/screens/akun/AkunScreen.kt:104-108` (layar akun)
  - `ui/screens/auth/SsoScreen.kt:130-134` (pola sama; tidak ada di catatan test, tetapi akan tampak sama)
- Layar lain sudah benar dan bisa jadi acuan: `ui/screens/home/HomeHeader.kt`, `ui/screens/presensi/PresensiMasukScreen.kt:315` (memakai `ContentScale.Crop`).
- Jika URL gambar gagal dimuat, lingkaran kosong tanpa cadangan (huruf awal nama hanya dipakai saat `avatarUrl` kosong, bukan saat gagal muat).

### Rencana

1. Buat satu komponen bersama `AvatarImage` di `ui/components/`: ukuran dari parameter, bentuk lingkaran (`shape.full`), clip lingkaran, `ContentScale.Crop`, border dan warna latar dari `MaterialTheme.colorScheme` (tanpa hex hardcode).
2. Cadangan: saat gambar gagal atau masih memuat, tampilkan huruf awal nama (atau ikon orang) dengan warna dari tema.
3. Ganti tiga pemakaian di atas dengan komponen ini; ukuran tetap 84 dp (Password, SSO) dan 88 dp (Akun) lewat parameter.
4. Tambahkan `@Preview` untuk: foto persegi, foto lebar, foto tinggi, dan tanpa foto.

### Verifikasi

- Pakai akun dengan foto tidak persegi (lebar dan tinggi): foto memenuhi lingkaran tanpa sudut keluar bingkai.
- Matikan jaringan atau pakai URL rusak: huruf awal tampil, bukan lingkaran kosong.
- Border tetap rapi dan sejajar dengan tepi lingkaran.

### Risiko

- `Crop` memotong sisi foto; wajah yang tidak di tengah bisa terpotong. Bisa disesuaikan dengan `alignment` jika perlu.

---

## Urutan pengerjaan

1. Item 3 (avatar): kecil, terlihat langsung, tanpa keputusan produk.
2. Item 1 (dark mode): kecil, setelah keputusan "hapus atau dimatikan".
3. Item 2 (autofocus): paling banyak layar; kerjakan mekanismenya dulu, lalu pasang per layar.

Pisahkan menjadi tiga commit supaya mudah ditinjau dan dibatalkan satu per satu.

## Checklist kualitas

- Tidak ada warna, ukuran, atau durasi hardcode baru di luar tema/token (`MaterialTheme.colorScheme`, `Spacing`, bentuk dari tema).
- Komponen baru (`AvatarImage`, parameter `autoFocus`) punya `@Preview`.
- Semua elemen interaktif tetap berukuran sentuh minimal 48 dp.
- Tes unit/UI yang sudah ada tetap lulus; layar yang disentuh dicoba manual di HP uji.

## Pertanyaan terbuka

1. Dark mode: **dimatikan saja** (kode skema gelap dibiarkan) atau **dihapus total** (skema gelap, `values-night`, preview gelap)?
2. Autofocus: apakah daftar layar isian tunggal di atas sudah sesuai maksudmu, atau ada layar lain yang kamu maksud dari hasil test?
3. Login: fokus otomatis di NISN sudah cukup, atau pemilih sekolah juga perlu dibuka otomatis setelah NISN diisi?
4. Ganti Kata Sandi di Akun masih belum tersambung ke API. Mau ikut dikerjakan, atau di luar lingkup perbaikan ini?
