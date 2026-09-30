# Prompt: Bangun Ulang Diskola Mobile (Android) — Alur 100% Sama, Kode Bersih

> **Cara pakai:** buat repo kosong, buka Claude Code di situ, lalu paste seluruh isi dokumen ini
> sebagai pesan pertama. Tambahkan di akhir: "Mulai dari Fase 0 dan Fase 1 dulu, jangan lanjut
> sebelum saya review." Simpan juga file ini di repo baru sebagai `docs/BRIEF.md` supaya jadi
> rujukan tetap.

---

## 1. Konteks produk

Diskola adalah aplikasi portal sekolah (SIAKAD) untuk **siswa dan guru** di Indonesia. Satu aplikasi
menggabungkan akademik, presensi, pembayaran sekolah, dompet digital, perpustakaan, dan marketplace
kewirausahaan sekolah.

Aplikasi versi lama sudah berjalan di produksi (`id.diskola.app`, versionName 2.1.37, 75 rilis) dan
akan **dibangun ulang dari nol** di repo ini. Backend, API, dan seluruh alur pengguna **tidak berubah
sama sekali** — yang diganti hanya implementasi Android-nya.

**Skala yang harus ditampung:**
- ~355 endpoint REST
- 101 tabel Room (2 database: cache in-memory + persistent)
- 6 tab navigasi utama, ~25 modul fitur
- 2 peran utama: siswa (`is_student`) dan guru (`is_teacher`), plus flag langganan/fitur:
  `onklas_pro` (premium), `has_store` (punya toko), `klaspayActive` (dompet aktif)

**Target teknis:** minSdk 27, targetSdk 36, compileSdk 35, Kotlin, **Jetpack Compose 100%**
(tidak ada XML layout, tidak ada Fragment).

---

## 2. Dua aturan yang tidak boleh dilanggar

1. **Alur pengguna wajib 100% identik** dengan aplikasi lama — urutan layar, kondisi percabangan,
   nama menu, teks tombol, aturan validasi, dan perilaku error harus sama persis. Kalau ada alur
   yang terasa aneh, **jangan diperbaiki diam-diam** — tulis catatannya di `docs/FLOW_QUESTIONS.md`
   dan tanyakan ke saya.
2. **Arsitekturnya harus benar-benar baru dan bersih.** Aplikasi lama adalah 161 Activity, 113
   Fragment, 612 layout XML, dan satu Dagger component raksasa. Itu yang sedang kita tinggalkan.
   Jangan pernah "port" struktur lama apa adanya.

---

## 3. Arsitektur target

**Modularisasi (Gradle multi-module):**
```
:app                        // hanya Application, MainActivity, navigation host, DI root
:core:designsystem          // token warna/tipografi/spacing/shape/motion + komponen dasar
:core:ui                    // komponen komposit lintas fitur (dialog, empty/error state, list row)
:core:network               // Retrofit, interceptor, error mapping, DTO dasar
:core:database              // Room, DAO, entity
:core:datastore             // preferences (pengganti SharedPreferences lama)
:core:common                // Result wrapper, dispatcher, extension, util
:core:model                 // domain model murni (tanpa anotasi Retrofit/Room)
:feature:auth               // splash, onboarding, login, SSO, reset password
:feature:sekolah            // ... satu module per fitur, lihat Bagian 5
```

**Aturan lapisan:**
- `UI (Composable, stateless)` → `ViewModel (StateFlow<UiState>)` → `Repository (interface di
  domain, implementasi di data)` → `RemoteDataSource` / `LocalDataSource`
- Composable **tidak boleh** menyentuh Repository/ApiService langsung.
- Semua state layar berupa **satu** `data class XxxUiState` yang di-expose lewat
  `StateFlow`, dikonsumsi dengan `collectAsStateWithLifecycle()`. **Jangan pakai LiveData.**
- Semua panggilan jaringan mengembalikan `Result<T>` (sealed: `Success`/`Error`/`Loading`), tidak
  pernah melempar exception mentah ke UI.
- DI pakai **Hilt** (`@HiltViewModel`, `@Module @InstallIn`). Jangan pakai Dagger component manual.
- Navigasi pakai **Navigation Compose dengan route type-safe** (`@Serializable` route object),
  satu `NavHost` per graph fitur, satu Activity untuk seluruh aplikasi.
- Data fetching: setiap layar memuat datanya **satu kali** lewat `init` ViewModel atau
  `LaunchedEffect(Unit)`; refresh manual lewat aksi eksplisit (pull-to-refresh / tombol).
  **Dilarang** memicu fetch dari `onResume`-equivalent tanpa alasan yang ditulis di komentar.
- Caching: Room sebagai single source of truth untuk data yang perlu offline
  (jadwal, tugas, materi, ujian). Layar membaca dari Room, network hanya mengisi Room.

**Testing minimum:** unit test untuk setiap ViewModel (state transition + error path) dan setiap
Repository (mapping + cache policy). Tidak perlu UI test dulu.

---

## 4. Design system (pakai nilai ini persis)

Font: **Lato** (satu keluarga, hierarki lewat weight — jangan tambah font lain).
Skala spacing: `xs 4, sm 8, md 12, lg 16, xl 20, xxl 24, xxxl 32, huge 40, massive 48`,
padding horizontal layar tetap **20dp** di seluruh aplikasi.
Shape: `small 8dp, medium 14dp, large 24dp, extraLarge 28dp`.
Motion: `fast 150ms, base 250ms, slow 400ms`; easing masuk `CubicBezier(0.05, 0.7, 0.1, 1)`,
easing keluar `CubicBezier(0.3, 0, 0.8, 0.15)`.

**Light scheme (Material 3):**
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

**Dark scheme:**
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

**Warna tambahan (extended, di luar M3):** `success` light `#008779` / dark `#43DDBD`,
`warning` light `#FF891C` / dark `#FFB86A`.

**Aturan pemakaian:**
- Tidak ada hex hardcode di luar module `:core:designsystem`.
- `primaryContainer` di palet ini **gelap** di kedua mode — jangan dipakai sebagai latar
  badge/ikon di mode terang; untuk itu pakai `surfaceContainerLow` atau `primary.copy(alpha=0.1f)`.
- Dark mode wajib diuji terpisah, bukan asumsi inversi.
- Semua target sentuh ≥ 48dp; setiap komponen interaktif punya state pressed + disabled.
- Setiap layar berdata wajib punya state **loading / empty / error** yang didesain, masing-masing
  dengan satu aksi pemulihan.

---

## 5. Peta fitur & alur

Bangun per-fitur dengan urutan ini (kecil → besar), satu Gradle module per fitur:

| # | Module | Isi utama | Skala lama (file) |
|---|---|---|---|
| 1 | `auth` | splash → onboarding → login NISN → password → home; Google SSO; reset password | 12 + 8 |
| 2 | `akun` | profil, ganti password, kartu pelajar, kontak, verifikasi email, logout | 22 |
| 3 | `notification` | daftar & detail notifikasi, ringkasan badge | 7 |
| 4 | `agenda_mingguan` | agenda harian staf, check-in/out per agenda | 8 |
| 5 | `homework` | daftar tugas, pengumpulan, lampiran, penilaian | 20 |
| 6 | `partisipasi` | penilaian partisipasi siswa | 10 |
| 7 | `poin` | poin pelanggaran/prestasi | 14 |
| 8 | `presensi` | presensi harian, izin, rekap, geolokasi | 30 |
| 9 | `prokes` | screening kesehatan (siswa & guru) | 20 |
| 10 | `perpus` | katalog buku, peminjaman | 12 |
| 11 | `akm` + `ujian` + `tryout` | asesmen, ujian offline-first, unggah jawaban | 26 + 12 + 9 |
| 12 | `theory` / `pembelajaran` | materi, jurnal kelas, feed pembelajaran | 9 + 6 |
| 13 | `klaspay` | dompet digital: aktivasi, saldo, riwayat, top-up | 29 |
| 14 | `pembayaran` | SPP & tagihan sekolah, detail pembayaran | 30 |
| 15 | `ppob` | pulsa, token listrik, tagihan pihak ketiga | 44 |
| 16 | `sekolah` + `store` | profil sekolah, feed sosial, toko sekolah, keranjang, checkout | 57 |
| 17 | `entrepreneurs` | kewirausahaan siswa: toko sendiri, produk, order, review, income | 47 |
| 18 | `magang` | program magang siswa | 15 |
| 19 | `jelajah` | eksplorasi konten/feed | 8 |
| 20 | `chat` + `createpost` + `announcement` | pesan, buat postingan, pengumuman | 6 + 6 + 5 |

**Navigasi utama (bottom navigation, 6 tab, urutan persis):**
`Sekolah` · `Jelajah` · `Pembelajaran` · `Toko` · `Pembayaran` · `Akun`

**Alur auth (harus persis seperti ini):**
1. **Splash** — cek versi aplikasi (`check-android-version`). Jika ada versi lebih baru → dialog
   "Update Tersedia" yang **tidak bisa ditutup**, tombol Update membuka Play Store lalu menutup app.
2. Belum pernah onboarding → **Onboarding** 3 halaman (pembelajaran, kewirausahaan, dompet digital),
   tombol kembali/berikutnya/selesai. Setelah selesai, flag `onboard` disimpan permanen.
3. Sudah login → langsung ke **Home**. Jika ada payload notifikasi (`page`), route langsung ke
   halaman tujuan notifikasi dan lewati Home.
4. Belum login → **Login**: input NISN/NIS/NIK + pilih sekolah (bottom sheet dengan pencarian,
   paginasi, logo sekolah dari API) + centang Syarat & Ketentuan (dialog berisi konten HTML dari
   endpoint `policy`). Tombol Masuk aktif hanya jika ketiganya terisi.
5. Masuk → `check-account` → **halaman Password** menampilkan nama, avatar, peran, dan kelas
   pengguna; input password; ada "Lupa password → Reset" dan "Hubungi Kami".
6. Login sukses → jika masih memakai password default, munculkan notifikasi ganti password →
   masuk ke Home.
7. **Jalur Google SSO:** tombol "Masuk dengan Akun Google" → Credentials API/Google Sign-In →
   `login-sso`. Jika akun belum terhubung sekolah → dialog konfirmasi tamu (pengguna harus
   mengetik frasa persis untuk lanjut) → halaman SSO untuk memilih sekolah → daftar/masuk.
   Setelah sukses: `klaspayActive` true → Home, selain itu → halaman Aktivasi Klaspay.
8. **Logout** (dari Akun): konfirmasi → cek ada ujian yang belum dikumpulkan (blokir + arahkan ke
   ujian bila ada) → sign-out Google → hapus sesi & database → kembali ke layar Login.

---

## 6. Kontrak API

- Base URL dibedakan per build type (dev/prod) lewat `buildConfigField`.
- Konvensi path: `mobile/app/...` (auth, akun, config), `mobile/...` (fitur), plus beberapa
  endpoint legacy tanpa prefix (`logout`, `sessions`).
- Contoh endpoint auth: `GET mobile/app/config/check-android-version`,
  `GET mobile/app/authentication/schools`, `POST mobile/app/authentication/check-account`,
  `POST mobile/app/authentication/login-account`, `POST mobile/app/authentication/login-sso`,
  `POST mobile/app/authentication/reset-password`, `DELETE logout`, `GET sessions`.
- Serialization: **satu** library saja (pilih kotlinx.serialization; aplikasi lama memakai Moshi
  dan Gson sekaligus — jangan diulang).
- Interceptor: auth token, logging (debug saja), error mapping ke `Result.Error` dengan pesan siap
  tampil.
- Push notification: FCM, dengan router yang memetakan `page` payload → destinasi navigasi.
  Definisikan pemetaan ini di satu file, bukan tersebar.

Sumber kebenaran lengkap 355 endpoint ada di repo lama pada
`app/src/main/java/id/diskola/app/api/ApiService.kt`. Saat mengerjakan sebuah fitur, salin
kontraknya dari sana per-fitur, jangan sekaligus.

---

## 7. Anti-pattern dari kode lama yang HARUS dihindari

Ini semua masalah nyata yang sudah ditemukan di repo lama. Jangan diulang:

1. **Fetch ganda saat layar dibuka** — memicu request yang sama dari `onCreate` *dan* `onResume`,
   atau dari `init` ViewModel *dan* `init` Fragment *dan* `onViewCreated` sekaligus. Satu layar =
   satu pemicu muat awal.
2. **Guard race condition** — pola `if (loading) return` yang flag-nya baru di-set di dalam
   coroutine. Set flag **sebelum** `launch`.
3. **Aset gambar raksasa** — background PNG 1049×2096 di `drawable-nodpi` yang jadi ~8.8MB saat
   di-decode dan digambar di setiap layar. Gunakan vector/Canvas untuk dekorasi, WebP untuk foto,
   dan bucket densitas yang benar.
4. **Mencampur API system bar lama dan baru** — `enableEdgeToEdge()` bersama
   `window.decorView.systemUiVisibility`. Pakai `WindowInsetsControllerCompat` saja.
5. **`finishAffinity()` + Activity tujuan di task yang sama** — menutup seluruh aplikasi alih-alih
   pindah layar. Kalau memang perlu, luncurkan tujuan dengan `NEW_TASK or CLEAR_TASK`.
6. **Chrome yang diduplikasi per layar** — setiap layar membangun sendiri background/scaffold yang
   "seharusnya sama". Pasang sekali membungkus NavHost, layar hanya mengisi konten.
7. **Komponen bawaan tanpa identitas** — `AlertDialog`/`MaterialAlertDialogBuilder` polos untuk
   semua hal. Sediakan `AppDialog` sendiri di `:core:ui` dan pakai bottom sheet untuk pilihan
   kontekstual.
8. **Logika bisnis di UI** — pemanggilan API, mapping, dan aturan blokir langsung di Activity/
   Fragment. Semua itu milik ViewModel/Repository.
9. **Satu Dagger component raksasa** yang menginject puluhan ViewModel satu per satu.
10. **Dua library serialization, dua sistem binding** (dataBinding + viewBinding aktif bersamaan).
11. **Nilai hardcode tersebar** — warna hex, dp acak (10/13/15/18 dalam satu layar), durasi animasi.

---

## 8. Definition of done per fitur

Sebuah fitur dianggap selesai jika:
- Alurnya identik dengan aplikasi lama (dibandingkan langsung, bukan dari ingatan).
- UI 100% Compose, memakai token dari `:core:designsystem`, tanpa hex/dp liar.
- Light dan dark mode dua-duanya benar; kontras teks body ≥ 4.5:1.
- Ada state loading, empty, dan error dengan aksi pemulihan.
- Layout tidak rusak di layar kecil (320dp), tablet, dan saat font di-scale 1.5×
  (batasi lebar konten form ~440dp dan center-kan di layar lebar).
- ViewModel punya unit test untuk jalur sukses dan gagal.
- Tidak ada request duplikat saat layar dibuka (verifikasi lewat log/Chucker).
- `./gradlew :app:assembleDebug` dan `detekt`/`ktlint` bersih.

---

## 9. Urutan pengerjaan yang saya minta

- **Fase 0 — Fondasi:** setup Gradle multi-module, Hilt, Retrofit, Room, DataStore, Navigation
  Compose, dan `:core:designsystem` lengkap (token + komponen dasar: Button, TextField, Card,
  Dialog, BottomSheet, ListRow, EmptyState, ErrorState, LoadingState) beserta `@Preview` light+dark.
- **Fase 1 — Auth:** seluruh alur Bagian 5 poin 1–8, sebagai bukti bahwa arsitekturnya jalan.
- **Fase 2 dan seterusnya:** fitur sesuai urutan tabel Bagian 5, satu fitur per iterasi.

Sebelum menulis kode di setiap fase, tulis dulu rencana singkat (file yang akan dibuat + alasan
keputusan arsitektur), tunggu saya setujui, baru eksekusi. Jangan mengerjakan lebih dari satu fase
tanpa review.
