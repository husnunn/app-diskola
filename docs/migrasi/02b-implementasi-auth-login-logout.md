# 02b — Implementasi Fase Auth: Login Inti + Logout

Laporan implementasi untuk fase "menyamakan alur login `Diskola-App-New` dengan `android-portal`"
(login inti + logout), dikerjakan 30-09-2026. Melengkapi `02-auth-login-sesi.md` (spesifikasi
perilaku target) dan `02a-gap-auth-diskola-app-new.md` (status selisih sebelum/sesudah per item).
Dokumen ini menjawab: **apa yang dibangun, di berkas mana, dan apa yang sudah terbukti bekerja**.

Path kode relatif terhadap `Diskola-App-New/app/src/main/java/id/diskola/app/`, kecuali disebutkan lain.

## Daftar isi
1. [Cakupan & keputusan awal](#1-cakupan--keputusan-awal)
2. [Arsitektur baru](#2-arsitektur-baru)
3. [Pekerjaan per fase](#3-pekerjaan-per-fase)
4. [Bug ditemukan & diperbaiki selama implementasi](#4-bug-ditemukan--diperbaiki-selama-implementasi)
5. [Pengujian di device fisik](#5-pengujian-di-device-fisik)
6. [Di luar cakupan fase ini](#6-di-luar-cakupan-fase-ini)
7. [Cara verifikasi lanjutan](#7-cara-verifikasi-lanjutan)

---

## 1. Cakupan & keputusan awal

Disepakati dengan user sebelum mulai (lihat `docs/FLOW_QUESTIONS.md` entri "2026-09-30 —
Implementasi fase auth"):

- **Cakupan:** login inti (splash → onboarding → login NISN/password → SSO Google → aktivasi
  Klaspay → reset password) **+ logout terpusat**. Dialog keamanan pasca-login penuh, validasi sesi
  berkala (`current-user`), dan routing notifikasi **tidak** termasuk — itu bagian dokumen 03
  (fase Home).
- **Teks UI:** tampilan/komponen tetap dari desain Fase 1 (`ref/design_handoff_auth_fase1/`), tapi
  semua **copy** (judul, hint, label tombol, pesan error, isi dialog) diganti persis ke teks
  `android-portal`. Elemen dekoratif desain baru yang tidak ada di app lama (header "Selamat
  Datang", badge "Aktif", footer "Bantuan") dihapus.
- **3 deviasi disengaja dari app lama** (bukan bug, sudah dikonfirmasi ke user):
  1. SSO: siswa/guru dengan `data.school == null` tetap melihat pemilih sekolah + tombol "daftar"
     (sama seperti kode lama), tapi "daftar" baru aktif setelah sekolah benar-benar dipilih
     (sejalan dengan keputusan Q2 di dokumen 02).
  2. Dialog konfirmasi tamu: auto-kapital keyboard dimatikan (`KeyboardCapitalization.None`) supaya
     tidak menyulitkan pencocokan frasa case-sensitive — frasanya sendiri tidak berubah.
  3. Blokir logout karena ujian AKM memakai tabel `akm_synced_exam` (status unduhan) sebagai
     **proksi sementara**, karena tabel jawaban ujian offline yang sesungguhnya belum ada di app
     baru — akan disesuaikan lagi begitu database AKM diputuskan (`FLOW_QUESTIONS.md` bagian B).

## 2. Arsitektur baru

Sebelumnya seluruh logic auth ada di satu `viewmodel/AuthViewModel.kt` (282 baris, dihapus total)
yang membaca-tulis prefs secara langsung dan tersebar. Sekarang dipecah mengikuti pola
`UI → ViewModel → Repository → RemoteDataSource` (`docs/ANDROID_DESIGN_PATTERN.md` §1) dengan satu
titik kebenaran untuk sesi:

| Berkas baru | Tanggung jawab |
|---|---|
| `utils/session/SessionKeys.kt` | Konstanta semua key SharedPreferences auth (tabel §12 dokumen 02) |
| `utils/session/SessionStore.kt` | Satu-satunya pembaca/penulis prefs sesi — `writeCheckAccount`, `writeLoginAccount`, `writeSsoCheckEmail`, `writeSsoSchool`, `markKlaspayActivated`, plus getter (`isLoggedIn`, `isOnboarded`, `user`, `school`, `student`, `teacher`, `roleLabel`, dst.) |
| `utils/session/SessionModels.kt` / `SessionMappers.kt` | Bentuk JSON yang disimpan (`SessionUser`/`SessionSchool`/`SessionStudent`/`SessionTeacher`) + mapper dari tiap bentuk response API |
| `utils/session/FcmTopicManager.kt` | Satu titik subscribe/unsubscribe semua topik FCM sesi (keputusan Q7) |
| `utils/session/SessionManager.kt` | Satu fungsi `logout()`: cek blokir ujian AKM dulu → hapus DB lokal → unsubscribe topik → `DELETE logout` → bersihkan prefs (commit) → navigasi ke Login |
| `utils/session/SessionEvents.kt` | `SharedFlow` event `Unauthorized` (401) untuk penanganan terpusat (keputusan Q6) |
| `utils/session/AuthError.kt` (`AuthErrorMapper`) | Memetakan `ApiException` → `InvalidPassword` / `DeviceConflict` / `Message`, dipakai layar Password & SSO |
| `utils/DeviceIdProvider.kt` | `Settings.Secure.ANDROID_ID` untuk `device_id` (terpisah dari fingerprint AKM di `DeviceUtil`) |
| `repository/AuthRepository.kt` | Panggilan API auth + suntik `device_id` otomatis |
| `repository/SchoolRepository.kt` + `dataclass/localDb/SchoolEntity.kt` | Cache Room `school` untuk pemilih sekolah lokal-only (Q3) |
| `viewmodel/SplashViewModel.kt` | Router splash: notification channel, cek versi, `onboard`/`logged_in` |
| `viewmodel/OnboardingViewModel.kt` | Tandai `onboard=true` |
| `viewmodel/LoginViewModel.kt` | Menggantikan `AuthViewModel` — form NISN, pemilih sekolah, check-account, login-account |
| `viewmodel/SsoViewModel.kt` | `login-sso` / `login-sso/school`, gerbang tamu |
| `viewmodel/ResetPasswordViewModel.kt` | `reset-password` + countdown kirim ulang |
| `viewmodel/KlaspayActivationViewModel.kt` | Aktivasi PIN Klaspay 2 langkah |
| `viewmodel/SessionRefreshViewModel.kt` | Pengganti kecil `AuthViewModel.refreshProfile` untuk Home (penuh baru dikerjakan di dokumen 03) |

Layar (`ui/screens/auth/`): `SplashScreen`, `OnboardingScreen`, `LoginScreen`, `SchoolPickerSheet`,
`PasswordScreen`, `SsoScreen`, `ResetPasswordScreen` (+`ResetPasswordSentScreen`),
`KlaspayActivationScreen`, `AuthDialogs.kt` (Terms/GuestConfirm/UpdateRequired/DeviceConflict).
Navigasi: `Route.AuthGraph` (graph baru) membungkus seluruh sub-alur Auth di `ui/navigation/
AppNavHost.kt`, sehingga `LoginScreen`/`PasswordScreen`/dialog S&K berbagi **satu** instance
`LoginViewModel` (mekanisme `hiltViewModel(navController.getBackStackEntry(Route.AuthGraph))`),
persis seperti Activity-scoped `LoginViewModel` di app lama menyatukan `LoginForm`/`LoginProcess`.

## 3. Pekerjaan per fase

### Fase 1 — Fondasi data & jaringan
- `apiservice/ResponseInterceptor.kt`: `ApiException` ditambah `errorCode` (dari `error_code` body)
  dan `retryAfterSeconds` (header `Retry-After`). Penanganan 401 diubah total: **tidak lagi**
  memanggil logout langsung dari interceptor (yang berjalan di thread OkHttp, bukan Activity) —
  sekarang hanya `sessionEvents.emit(SessionEvent.Unauthorized)` bila sesi sedang login, dan
  `AppNavHost` yang menampilkan dialog + memanggil `SessionManager.logout()`.
- Model Moshi disesuaikan ke bentuk `android-portal`: `LoginAccountRequest` +`device_id`;
  `ProductSchoolData`/`CredentialData` diberi nilai default (bukan nullable) sesuai fallback lama;
  `CheckAccountApiResponse`/`CheckAccountUserData` +`using_default_password`; model SSO baru
  (`LoginSsoModels.kt`): `LoginSsoRequest/ApiResponse`, `LoginSsoSchoolRequest/ApiResponse`,
  `ResetPasswordRequest`, `SetupUserFcmRequest`.
- `AuthApiService.kt`: endpoint `login-sso`, `login-sso/school`, `reset-password`,
  `updateFcmToken`, `currentUser` diketikkan (bukan lagi `Map<String, Any>`); `getSchools` diubah
  jadi `skip/take/name` (name selalu kosong) sesuai kontrak lama, bukan parameter `q` bikinan
  sendiri.
- Room: tabel `school` baru (`SchoolEntity`/`SchoolDao`, `LocalDatabase` naik ke versi 3 —
  `fallbackToDestructiveMigration()` aman untuk build dev).

### Fase 2 — Splash & Onboarding
- `SplashViewModel` membuat 3 notification channel (`app_name`, `fcm_default_channel_id`,
  `<app_name>.silent`), memakai hasil `check-android-version` (bukan dibuang di balik timeout),
  dan membaca `onboard`/`logged_in` dari `SessionStore` (bukan file `user_session` yang sudah
  pensiun).
- `App.kt`: alur in-app update Play Core (`checkForUpdate()`) dihapus — app lama tidak pernah
  benar-benar menampilkannya (lihat 02a S2), dan sekarang ada dialog "Update Tersedia" sendiri yang
  eksplisit mengikuti spesifikasi, supaya tidak ada dua mekanisme update yang saling tumpang tindih.
- `OnboardingScreen`: 3 slide dengan judul/isi persis app lama ("pembelajaran", "kewirausahaan",
  "dompet digital"); `selesai` memanggil `SessionStore.setOnboarded()`.
- `MainActivity`/`AndroidManifest.xml`: `android:screenOrientation="portrait"` dikunci di level
  manifest (paritas dengan `BasePage` app lama yang memaksa portrait semua layar).

### Fase 3 — Login NISN + pemilih sekolah
- `LoginScreen`: teks diganti total ke app lama ("LOGIN AKUN", "Ketikkan NISN/NIS/NIK", kalimat
  S&K panjang, tombol "masuk"); aturan aktif tombol jadi `nisn.isNotBlank()` (bukan panjang ≥ 4).
- `SchoolPickerSheet` + `SchoolRepository`: pemilih sekolah sepenuhnya dirombak dari pencarian
  server ke **cache Room lokal** — `ensureFirstPage()`/`loadNextPage()`/`refresh()` meniru
  `BoundaryCallback` Paging 2 milik app lama (20/halaman, refresh selalu ke halaman 0). Pencarian
  hanya menyaring cache yang sudah ter-load (keputusan Q3, tiru bug/perilaku lama).
- `LoginViewModel.checkAccount()`: sukses menulis efek samping lengkap lewat
  `SessionStore.writeCheckAccount` (`default_pass`, `user_id`, `is_active`, `user`/`school` JSON);
  gagal menampilkan `AppBottomSheet` "Login Gagal" dengan pesan asli dari backend (fallback
  "Terjadi kesalahan saat login. Silakan coba lagi." — bukan pesan generik `AppErrorHandler`).

### Fase 4 — Layar Password
- `PasswordScreen` dirombak untuk membaca profil dari `LoginViewModel.checkAccountResult` (data
  asli check-account), bukan lagi lewat argumen `Route.Auth.Password` yang panjang.
- `login-account` sekarang mengirim `device_id` (`DeviceIdProvider`); sukses memanggil
  `Firebase.analytics.setUserId`, `setup-user-fcm` (error diabaikan), dan
  `FcmTopicManager.subscribeForSession(...)`.
- Penanganan error lewat `AuthErrorMapper`: 400 "Detail login tidak bisa digunakan" (cocok
  sebagian, tanpa membedakan huruf besar/kecil, titik akhir diabaikan) → sheet "Password yang anda
  masukkan salah"; `error_code == "DEVICE_CONFLICT"` → `DeviceConflictDialog` ("Perangkat Lain
  Terdeteksi" + catatan admin); selain itu → sheet "Login Gagal" dengan pesan backend.
  Password default → notifikasi lokal "Ganti Password" (teks kamu/Anda sesuai peran) lalu dialog.
- Kontak "Hubungi Kami" diarahkan ke WhatsApp `6287887219649` dengan fallback panggilan telepon.

### Fase 5 — Google SSO + Aktivasi Klaspay
- `SsoViewModel.checkEmail()` benar-benar memanggil `login-sso` (sebelumnya tidak dipanggil sama
  sekali). Gerbang dialog tamu (`isGuestGate`) memakai kondisi `data == null || data.school ==
  null` — hasil analisis kode lama yang menyimpulkan race `postValue` di app lama justru selalu
  jatuh ke kondisi ini secara konsisten (bukan acak seperti dugaan awal), jadi ditiru langsung
  sebagai kondisi eksplisit, bukan meniru race-nya secara literal.
- `SsoScreen` menampilkan data asli (peran, kelas, sekolah) dan memanggil `login-sso/school`
  lewat `confirmSchool()`; tombol "daftar" baru aktif setelah sekolah dipilih (deviasi disengaja,
  lihat §1).
- `KlaspayActivationScreen` (PIN 6 digit → konfirmasi PIN, memakai ulang komponen `PinDots`/
  `NumericKeypad` yang sudah ada) + `KlaspayApiService.activateKlaspay` diketikkan dan didaftarkan
  ke `ApiModule` (sebelumnya tidak pernah di-provide, jadi tidak bisa dipanggil sama sekali).

### Fase 6 — Reset password
- `ResetPasswordScreen` (form email) + `ResetPasswordSentScreen` (countdown 3 menit, "kirim ulang"
  **tidak** nonaktif di percobaan pertama — baru nonaktif setelah user menekannya sekali, sesuai
  koreksi hasil membaca kode lama langsung, bukan dokumen lama yang keliru soal ini).

### Fase 7 — Logout terpusat
- `SessionManager.logout()` jadi **satu-satunya** prosedur logout, dipakai oleh: tombol Logout di
  Akun (`AkunViewModel`), event 401 terpusat (`AppNavHost`), dan FCM `"logout"` (`NotifService`).
  Urutan: cek `hasPendingExam()` (blokir bila ada) → `clearAllTables()` → unsubscribe topik FCM →
  `DELETE logout` (selagi token masih ada) → bersihkan prefs lalu tulis ulang `url_api`+`onboard`
  (pakai `commit()`, memperbaiki bug lama yang membuat dua blok `edit{}` saling menimpa sehingga
  `onboard` ikut hilang) → batalkan notifikasi & WorkManager → navigasi ke Login.
- `AndroidManifest.xml`: `NotifService` **didaftarkan** (sebelumnya tidak ada sama sekali, jadi FCM
  data message termasuk `"logout"` tidak pernah sampai ke aplikasi).
- `AkunScreen`: teks dialog logout diganti ke "Anda yakin akan keluar dari aplikasi?"; sign-out
  Google Sign-In lama (`GoogleSignIn.getClient(...).signOut()`, deprecated) diganti
  `clearGoogleCredentialState()` lewat Credential Manager.
- `utils/IntentUtil.kt` dirampingkan: kedua fungsi `logOut()` lama dihapus total (kode kamera/file
  picker/WhatsApp yang tidak berkaitan tetap ada, sesuai prinsip satu tanggung jawab jelas).

## 4. Bug ditemukan & diperbaiki selama implementasi

Ditemukan lewat pengujian di device sungguhan, bukan asumsi — dicatat supaya tidak terulang:

| Bug | Penyebab | Perbaikan |
|---|---|---|
| `login-account` gagal dengan `Login Gagal` / "Unable to create converter for class ... LoginAccountApiResponse" | `UserRoles` dan `SekolahItem` (`dataclass/ResponData/LoginResponse.kt`) tidak diberi `@JsonClass(generateAdapter = true)`. Selama field `student`/`teacher` belum ada di `LoginAccountUserData`, dua kelas ini tidak pernah ikut ter-serialize sehingga lubangnya tidak ketahuan — begitu field itu ditambahkan (untuk P7/P9), Moshi (yang di app ini **tidak** memakai `KotlinJsonAdapterFactory` reflection sebagai fallback, lihat `di/module/MoshiModule.kt`) gagal membuat adapter untuk seluruh pohon respons | Tambah `@JsonClass(generateAdapter = true)` ke `UserRoles` dan `SekolahItem` |

Ditemukan lewat pembacaan kode (bukan device, tapi berdampak ke kebenaran fitur — lihat detail di
`02a`): logout lama menghapus `onboard`/`url_api` karena dua blok `pref.edit(true){...}` yang
saling tumpang tindih; topik FCM `ebede-notification-user-*` yang bukan milik Diskola; `NotifService`
tidak terdaftar di manifest; `LoginAccountRequest` tanpa `device_id`; dan seterusnya — semua sudah
masuk daftar 02a dengan status ✅.

## 5. Pengujian di device fisik

**Perangkat:** Infinix (Android 15, API 35), terhubung lewat `adb` — model debug (`id.diskola.app.debug`)
di-build dan di-install langsung (`./gradlew :app:assembleDebug`, `adb install -r`), diuji terhadap
backend `dev.api.diskola.id`. **Akun uji:** NISN `202005`, sekolah "SMK DEMO SURABAYA" (disediakan
user; password tidak dicatat di dokumen ini).

**Jalur yang lolos uji end-to-end** (screenshot + log jaringan, `adb logcat` tag `API_REQUEST`):
1. Install bersih → onboarding 3 slide tampil dengan teks lama → "selesai" → Login.
2. NISN diisi → pemilih sekolah menampilkan data asli dari cache Room (bukan kosong sampai
   mengetik) → cari "surabaya" → cache lokal menyaring ke "SMK DEMO SURABAYA" (di antara hasil
   lain) → dipilih → S&K dicentang → tombol "masuk" aktif.
3. `check-account` → **1x panggilan per tap** (diverifikasi lewat `logcat`, tidak ada fetch ganda,
   sesuai `docs/rules-global.md` §1.2) → layar Password menampilkan "dev tts 5" / STUDENT / "TES
   TTS" / NISN 202005 — semuanya dari response asli, bukan hasil tebakan.
4. `login-account` (dengan `device_id`) → sukses → dialog "Ganti Password" (akun uji memang masih
   password default) → "Mengerti" → Home menampilkan nama sekolah "SMK DEMO SURABAYA" dan nama
   pengguna dari sesi yang baru ditulis.
5. Tab Akun menampilkan profil benar ("dev tts 5" · Siswa) → "Keluar" → dialog "Anda yakin akan
   keluar dari aplikasi?" → "Logout" → log jaringan menunjukkan `DELETE /api/logout` → 200 →
   kembali ke Login (bukan onboarding, karena `onboard=true` tidak ikut terhapus).

**Belum diuji di device** (kode sudah ada, lihat checklist §16 dokumen 02 dan tabel di 02a untuk
daftar lengkap per item): jalur error (password salah, sekolah tidak terdaftar, konflik
perangkat), Google SSO, aktivasi Klaspay, reset password, dialog Update Tersedia, blokir logout
karena ujian AKM.

## 6. Di luar cakupan fase ini

Lihat §7 di `02a-gap-auth-diskola-app-new.md` untuk daftar prioritas sisa pekerjaan. Ringkas:
gerbang FIAM & extra FCM `page`/`goto` (splash), validasi sesi berkala + dialog keamanan penuh +
deep link verifikasi email (semua bagian dokumen 03/fase Home), dialog "Perbaikan Sistem" 503/504,
dan verifikasi field `product_school` (`onklas_*` vs `diskola_*`) dengan response asli.

## 7. Cara verifikasi lanjutan

Untuk siapa pun yang melanjutkan jalur yang belum diuji (§5):
1. `./gradlew :app:assembleDebug` lalu `adb install -r app/build/outputs/apk/debug/app-debug.apk`
   ke device yang sudah tersambung (`adb devices`).
2. Uji berdampingan dengan `id.diskola.app` (app lama, versi release) yang sudah terpasang di
   device yang sama, kalau ada — cocokkan tiap baris checklist §16 dokumen 02 langsung ke perangkat,
   bukan dari ingatan (`docs/migrasi/00-README.md` §"Cara pakai").
3. Pantau `adb logcat | grep API_REQUEST` untuk memastikan tidak ada fetch ganda saat
   buka-tutup-buka layar yang sama (`docs/rules-global.md` §1.1–1.2).
4. Setelah satu baris checklist terverifikasi, centang di §16 dokumen 02 dan perbarui tabel status
   di 02a — jangan biarkan kedua dokumen itu basi lagi seperti sebelum fase ini dikerjakan.
