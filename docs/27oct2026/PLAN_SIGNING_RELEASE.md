# Plan: signing release di `app/build.gradle.kts`

Status: **plan, belum dikerjakan.** Tidak ada file proyek yang diubah oleh dokumen ini.
Dibuat: 2026-10-27.

## Latar belakang

App lama (`android-portal`) menandatangani build release lewat `signingConfigs` yang membaca keystore dari
`local.properties`. App baru belum punya `signingConfig` untuk release di `app/build.gradle.kts`, jadi build release
harus ditandatangani manual lewat wizard *Generate Signed Bundle / APK*. Tujuan plan ini: build release bisa
ditandatangani otomatis dengan kunci yang benar, supaya hasilnya bisa diunggah sebagai update ke Play Store.

## Temuan

1. **Play App Signing sudah aktif.**
   - App rilis (`id.diskola.app`) di salah satu HP uji terpasang dari Play Store (versi 82 / 2.1.42) dan
     ditandatangani Google.
   - Yang harus cocok saat unggah adalah **upload key**, bukan kunci yang dipakai pengguna.
   - Versi yang live sudah 82, jadi AAB baru butuh `versionCode` minimal 83.

2. **Ada dua keystore di app lama, dan hanya satu yang bisa dibuka dengan kredensial yang ada.**

   | | Alias | Pemilik sertifikat | Dibuat | SHA-256 |
   |---|---|---|---|---|
   | `keystore.jks` di root | `sicendol` | Luthfirrohman, PT. Era Baru Digitama | 12 Sep 2023 | `76:44:84:D7:EE:A5:73:A3:59:51:D6:EA:E5:E3:CD:6C:D2:AF:23:94:20:E0:0C:65:9C:6D:25:B7:F7:C9:9F:E8` |
   | `app/keystore.jks` (app lama) | `onklas` | Agus Wardianzah, PT. Onklas Prima Indonesia | 8 Jul 2020 | `71:F9:65:14:93:06:ED:17:F5:24:E9:E1:C3:2E:CA:42:6F:82:4B:E2:9C:E1:85:2F:59:3F:FD:B7:F6:77:B7:86` |

   - `keystore.jks` di root repo baru identik (hash file sama) dengan `keystore.jks` di root app lama.
   - Password di `local.properties` membuka keystore root (`sicendol`). Keystore `app/keystore.jks` (`onklas`)
     tidak bisa dibuka dengan password itu.
   - Di `app/build.gradle` lama, `file("keystore.jks")` resolve ke folder `app/`, sehingga tidak bisa dipastikan
     keystore mana yang dipakai untuk rilis terakhir.
   - APK `app/debug/app-debug.apk` yang dibuat lewat wizard bertanda tangan `76:44:84:D7…`.

3. **Kredensial sudah lengkap.** `local.properties` repo baru sudah memuat `signing_keyAlias`,
   `signing_keyPassword`, `signing_storeFile`, dan `signing_storePassword`. Yang belum ada hanya sambungannya di Gradle.

4. **Riwayat git app lama.** Di remote `diskola-id/android-portal` ada commit yang menyentuh `keystore.jks`,
   `app/keystore.jks`, atau `local.properties` (misalnya `8ad5d8ba` "First release to console"). Isinya belum
   diperiksa. Repo baru bersih dari riwayat keystore.

## Langkah

### 0. Verifikasi upload key (dikerjakan manual di Play Console, sebelum kode)

- Buka *Test and release > App integrity > App signing* dan lihat **Upload key certificate SHA-256**.
- Kalau `76:44:84:D7…`: lanjut ke langkah 1. Tidak ada yang perlu diganti.
- Kalau `71:F9:65:14…`: butuh password asli `app/keystore.jks` yang tidak ada di `local.properties`.
  Jika password itu hilang, upload key harus di-reset lewat Play Console.

### 1. Ubah `app/build.gradle.kts`

- Tambah `signingConfigs` bernama `release`, memakai objek `properties` yang sudah membaca `local.properties`
  di file itu.
- Resolve path keystore dengan `rootProject.file(...)`, karena `keystore.jks` ada di root repo, bukan di `app/`.
- Pasang hanya di `buildTypes.release`, tidak di `defaultConfig`. Build debug tetap memakai kunci Android Debug
  bawaan, sama dengan yang terpasang di HP uji.
- Beri pengaman: signing release aktif hanya jika keempat key ada dan file keystore ditemukan. Jika tidak, build
  tetap jalan dengan peringatan, sehingga debug, mesin lain, dan CI tanpa secret tidak gagal sync.

### 2. Naikkan `versionCode` ke 83 atau lebih

Saat ini `versionCode = 1` dan `versionName = "1.0.0"`. Pisahkan dari perubahan signing supaya mudah ditinjau.
Tanpa ini Play Store menolak AAB-nya.

### 3. Verifikasi

- Jalankan `bundleRelease` dan `assembleRelease`.
- Cek sidik jari APK dengan `apksigner verify --print-certs`. Harus sama dengan upload key di Play Console.
- Cek AAB dengan `keytool -printcert -jarfile <file>.aab`.
- Jalankan `assembleDebug` lagi. Sidik jarinya harus tetap `87:41:51:A7:75:E1:CD:8F:3A:B4:4F:DA:63:F3:A8:DF:14:60:B4:92:E1:D4:65:BA:41:A8:F8:31:20:82:95:B9`
  (kunci debug), supaya install ke HP tidak bentrok.
- Uji build release di HP, karena `isMinifyEnabled = true`. Termasuk jalur update dari 2.1.42 ke build baru
  (cek login, data lokal Room, layar utama).

### 4. Unggah bertahap

Internal testing dulu, lalu staged rollout (misalnya 5–10%) sebelum rilis penuh.

### 5. Rapikan

- Catat di docs key apa saja yang wajib ada di `local.properties` untuk build release.
- Simpan cadangan keystore dan passwordnya di tempat aman (misalnya password manager).
- `.gitignore` sudah menutup `*.jks`, `local.properties`, dan `keystore.properties`, jadi tidak perlu diubah.

## Risiko

- `local.properties` tidak ikut git, jadi tiap mesin yang membuild release harus punya sendiri.
- Jika keystore yang dipakai bukan upload key yang terdaftar, Play Store menolak unggahan. Karena itu langkah 0
  dikerjakan lebih dulu.
- Jika riwayat git app lama ternyata memuat keystore dan repo itu bisa diakses banyak orang, anggap kuncinya
  terekspos dan pertimbangkan reset upload key.
- Password signing jangan pernah dicetak ke log build atau ditulis di dokumen ini.

## Di luar lingkup plan ini

- Kompatibilitas data pengguna yang update dari 2.1.42 (nama dan skema database Room, SharedPreferences, sesi login).
- Aturan R8/ProGuard untuk build release (`app/proguard-rules.pro` sedang berubah di working tree).
- Sidik jari Play App Signing di Firebase (Google Sign-In) dan pembatasan Maps API key.
