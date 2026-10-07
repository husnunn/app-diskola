# 02a — Selisih Implementasi Auth Compose vs Aplikasi Lama

> **Update 30-09-2026:** Fase "login inti + logout" (lihat `02b-implementasi-auth-login-logout.md`
> untuk laporan lengkap per fase) sudah dikerjakan dan diuji langsung di HP fisik (Infinix, Android
> 15) memakai backend `dev.api.diskola.id`. Dokumen ini sekarang **dipertahankan sebagai riwayat**:
> kolom **Status** di tiap tabel menunjukkan kondisi terkini, kolom **Sekarang** yang asli (per
> 29-09-2026, sebelum perbaikan) **tidak diubah** supaya analisis bug asli tetap terbaca. Untuk apa
> yang masih terbuka, lihat §7 di bawah (sudah diperbarui) dan `docs/FLOW_QUESTIONS.md`.

Hasil membandingkan kode auth yang ada di `Diskola-App-New` (per 29-09-2026, kondisi sebelum
perbaikan) dengan spesifikasi `02-auth-login-sesi.md`. Tujuan aslinya: daftar kerja agar alur login
kembali identik dengan `android-portal`.

Path di bawah relatif terhadap `Diskola-App-New/app/src/main/java/id/diskola/app/`.

Legenda tingkat (kondisi sebelum perbaikan): 🔴 alur/fungsi rusak atau hilang (wajib) · 🟠 aturan/logika
berbeda (wajib, kecuali diputuskan lain) · 🔵 teks/tampilan berbeda (disengaja mengikuti desain baru
di `ref/design_handoff_auth_fase1/`).

Legenda status (kondisi sekarang): ✅ Selesai & diverifikasi (kode + minimal satu jalur diuji di
device fisik) · ✅* Selesai (kode benar, tapi jalur ini belum sempat diuji langsung di device) ·
🟡 Sebagian (ada perbaikan nyata, tapi belum menutup seluruh spesifikasi) · ⛔ Belum dikerjakan
(sengaja, tercatat sebagai pekerjaan fase lain) · — Tidak diubah (disengaja, sesuai keputusan desain).

> Catatan sejarah: komentar lama di `viewmodel/AuthViewModel.kt:184` dan `ui/screens/auth/GoogleSignIn.kt`
> menyebut kode ini di-port dari `LoginPasswordActivity`/`LoginActivity` — itu **bukan** kelas dari
> `android-portal`. `AuthViewModel.kt` sudah **dihapus total**, digantikan `LoginViewModel`/
> `SsoViewModel`/`ResetPasswordViewModel`/`SplashViewModel`/`SessionRefreshViewModel` (lihat 02b §1).

## 1. Splash & router
| # | Tingkat | Status | Sekarang (29-09-2026) | Seharusnya (lama) |
|---|---|---|---|---|
| S1 | 🔴 | ✅ Selesai | `Route.Onboarding` tidak pernah dituju: Splash hanya ke `Login`/`Main` (`ui/navigation/AppNavHost.kt:37-49`), pref `onboard` tidak dibaca | `onboard == false` → Onboarding → Login; `selesai` set `onboard=true` (§2.4, §3) |
| S2 | 🔴 | ✅ Selesai | Hasil `checkVersion()` dibuang (timeout 2 dtk), `UPDATE_REQUIRED` diarahkan ke Login (`SplashScreen.kt:121`, `AppNavHost.kt:40`) | Versi server lebih besar → dialog wajib "Update Tersedia" → Play Store → tutup app (§2.2) |
| S3 | 🔴 | ✅ Selesai | Status login dibaca dari file prefs **`user_session`** (`is_logged_in` + `token`) (`SplashScreen.kt:123-135`) | Pref `logged_in` di file `id.diskola.app`. Pengguna yang update dari app lama akan **ter-logout** (❓ Q9 di dokumen 02) |
| S4 | 🔴 | ⛔ Belum | Tidak ada penanganan extra FCM `page`/`goto` saat app dibuka dari notifikasi | Buka layar tujuan notifikasi langsung bila sudah login (§2.1, §2.4) — bagian dari routing notifikasi dokumen 03, di luar cakupan fase ini |
| S5 | 🟠 | 🟡 Sebagian | Tidak membuat 3 notification channel, tidak menjadwalkan worker `upload_log`, tidak ada gerbang banner FIAM | §2.1, §2.3 (FIAM: ❓ Q4) — 3 channel **sudah** dibuat (`SplashViewModel.createNotificationChannels`); worker `upload_log` dan gerbang FIAM **masih belum** (dicatat eksplisit di `FLOW_QUESTIONS.md`, bukan diam-diam dilewati) |
| S6 | 🔵 | — Tetap | Splash beranimasi + teks "Memuat…" + "DISKOLA MOBILE V…" | Splash statis putih + logo — dipertahankan sesuai desain Fase 1, bukan bug |

## 2. Form login (NISN + sekolah)
| # | Tingkat | Status | Sekarang (29-09-2026) | Seharusnya |
|---|---|---|---|---|
| L1 | 🟠 | ✅ Selesai | Tombol aktif jika `nisn.length >= 4` (`ui/screens/auth/LoginScreen.kt:75`) | Cukup tidak blank (§4.1) |
| L2 | 🟠 | ✅ Selesai | Pemilih sekolah: pencarian server (`q`, `name`, `take=10`), daftar **kosong sampai user mengetik**, tanpa paging/pull-to-refresh (`viewmodel/AuthViewModel.kt:57-73`, `apiservice/AuthApiService.kt:27-33`) | Daftar langsung tampil (20/halaman, infinite scroll, pull-to-refresh), pencarian lokal debounce 500 ms (§4.2, ❓ Q3) — diuji di device: daftar sekolah tampil otomatis dari cache Room, pencarian "surabaya" berhasil menyaring cache lokal |
| L3 | 🟠 | ✅ Selesai | Error check-account tampil sebagai `BannerError` inline | Bottom sheet "Login Gagal" + tombol "Coba Lagi" (§4.1) |
| L4 | 🟠 | ✅ Selesai | Efek samping check-account tidak disimpan: `default_pass`, `user_id`, `is_active`, pref `user`/`school` | §4.1 "Efek samping" (dipakai reset password & dialog ganti password) — diverifikasi di device (Home menampilkan nama sekolah dari sesi yang ditulis check-account) |
| L5 | 🔵 | ✅ Selesai | Header "Selamat Datang", "SISTEM INFORMASI AKADEMIK TERPADU", badge "Aktif"; label "NISN / NIS / NIK" + contoh; teks S&K pendek; footer "Bantuan" (onClick kosong) | Judul "LOGIN AKUN", hint "Ketikkan NISN/NIS/NIK", kalimat S&K panjang (§4.1); tidak ada footer bantuan — semua diganti ke teks lama, footer/badge dihapus |
| L6 | 🔵 | ✅ Selesai | Judul pemilih sekolah "Pilih Lembaga Sekolah", placeholder "Cari nama sekolah" | "Pilih Sekolah" / "Cari sekolah" |

## 3. Layar password
| # | Tingkat | Status | Sekarang (29-09-2026) | Seharusnya |
|---|---|---|---|---|
| P1 | 🔴 | ✅ Selesai | `LoginAccountRequest` hanya `{uuid, password}` — **tanpa `device_id`** (`dataclass/ResponData/LoginResponse.kt:12-15`) | `{uuid, password, device_id: ANDROID_ID}`; backend lama menolak dengan 422 bila `device_id` kosong (§4.3) — `DeviceIdProvider` ditambahkan, login sukses di device fisik dengan `device_id` terkirim |
| P2 | 🔴 | ✅ Selesai | Kontak "Hubungi Kami" berisi nomor contoh "WhatsApp operator · 0812-3456-7890" dan "Telepon sekolah · (0321) 123456" (`ui/screens/auth/PasswordScreen.kt:156-163`) | "NISN/NIK terdaftar bukan milik Anda? **Hubungi Kami**" → WhatsApp `6287887219649`, fallback telepon (§4.3) |
| P3 | 🔴 | ✅* Selesai | Error login diganti pesan generik "Cek Password atau Akun Anda" (`AuthViewModel.kt:171`); tidak ada dialog konflik perangkat | 400 "Detail login tidak bisa digunakan" → "Password yang anda masukkan salah"; `DEVICE_CONFLICT` → dialog "Perangkat Lain Terdeteksi"; selain itu pesan backend (§4.3) — `AuthErrorMapper`/`DeviceConflictDialog` dibuat; jalur sukses login sudah diuji di device, jalur password-salah/konflik-perangkat belum sempat dipicu langsung |
| P4 | 🟠 | ✅ Selesai | Tombol aktif jika `password.length >= 4` (`PasswordScreen.kt:147`) | Password tidak kosong |
| P5 | 🟠 | 🟡 Sebagian | Password default → dialog "Ganti Password" / "Mengerti" lalu masuk Main (`PasswordScreen.kt:228-236`) | Notifikasi lokal "Ganti Password" + dialog `ChangePasswordDialog` di Home yang wajib bila Klaspay aktif (§4.3, §8.2) — notifikasi lokal **sudah** dikirim (diverifikasi tampil di device); dialog **masih** langsung setelah login, belum dipindah ke sistem prioritas dialog Home (pekerjaan fase Home, dokumen 03 §8.2) |
| P6 | 🟠 | ✅ Selesai | Setelah login tidak memanggil `setup-user-fcm` dan `Firebase.analytics.setUserId` | §4.3 langkah 3 |
| P7 | 🟠 | ✅ Selesai | `saveSession` (`AuthViewModel.kt:189-281`): peran ditebak dari nama role dan siswa diprioritaskan; tidak menulis `isSso`, `is_having_class`, `student`/`teacher` JSON, `class_id`, `school` JSON, `school_uuid`, `user` JSON | Peran dari `rule.is_student`/`rule.is_teacher`; tulis semua key di tabel §4.3 — diverifikasi di device (Akun menampilkan "Siswa", Home menampilkan sekolah & nama sesuai response asli) |
| P8 | 🟠 | 🟡 Belum terverifikasi | Produk sekolah dibaca dari field `diskola_lite`/`diskola_pro` (`LoginResponse.kt:165`) | Field JSON di app lama `onklas_lite`/`onklas_pro` — kalau backend mengirim `onklas_*`, nilai sekarang selalu default. **Belum diverifikasi** dengan response `login-account` asli (response yang tertangkap di log device tidak menyertakan `product_school`) — cek lagi saat menyentuh gating menu PRO |
| P9 | 🟠 | ✅ Selesai | Label peran "PENGGUNA"/"Guru Mapel", identitas "NISN {nis_nik}", data dikirim lewat argumen route | `role_label` = `roles.first().name`, kelas `class_room.name`, nomor `nis_nik` (§4.3) — `Route.Auth.Password` sekarang tanpa argumen, data dibaca dari `LoginViewModel` yang di-share lewat `Route.AuthGraph`; diverifikasi di device ("STUDENT", "TES TTS", "NISN 202005") |
| P10 | 🔵 | 🟡 Sebagian (temuan baru) | Label "Password", "Lupa password? …", tombol "Login" | Hint "Masukkan Password", "Lupa password? **Reset**", tombol "LOGIN" — hint & "Reset" sudah benar, tapi tombol desain baru (`AppButton`) tidak menerapkan `textAllCaps` seperti `MaterialButton` lama, sehingga tampil "login"/"masuk" apa adanya, bukan **LOGIN**/**MASUK**. Ditemukan saat uji device (30-09-2026); belum diperbaiki — perlu keputusan apakah `AppButton` diberi transformasi allCaps atau dibiarkan sesuai gaya desain baru |

## 4. Login Google (SSO)
| # | Tingkat | Status | Sekarang (29-09-2026) | Seharusnya |
|---|---|---|---|---|
| G1 | 🔴 | ✅* Selesai | Credential Manager sudah dipakai, tetapi `login-sso` **tidak dipanggil**; `SsoScreen` menampilkan "-" untuk semua detail, `linked` selalu `false` | `POST login-sso {email, name, image, device_id}` → isi layar dari response (§5.2–5.3) — `SsoViewModel`/`AuthRepository.loginSso` dibuat; belum diuji end-to-end dengan akun Google sungguhan di device (perlu akun Google uji) |
| G2 | 🔴 | ✅* Selesai | Dialog tamu muncul **setelah** menekan "Iya, itu saya"/"Daftar", lalu langsung ke Main **tanpa login ke backend** (`AppNavHost.kt:133-160`) | Dialog tamu muncul **sebelum** layar SSO hanya bila akun belum punya sekolah; "iya, itu saya"/"daftar" → `POST login-sso/school` → Home atau Aktivasi Klaspay (§5.2–5.3, ❓ Q1) — logika gerbang tamu (`isGuestGate` di `SsoViewModel`, sesuai kondisi asli `data==null\|\|school==null`) dan pemanggilan `login-sso/school` sudah ada, belum diuji di device |
| G3 | 🔴 | ✅ Selesai | Frasa konfirmasi "SAYA MENGERTI" (`ui/screens/auth/AuthDialogs.kt:52`) | `lanjutkan sebagai tamu dulu bukan siswa/guru` (case-sensitive), teks dialog §5.2 — frasa diperbaiki, auto-kapital keyboard dimatikan (`KeyboardCapitalization.None`) |
| G4 | 🔴 | ✅* Selesai | Tidak ada alur Aktivasi Klaspay setelah SSO | §6 — `KlaspayActivationScreen` (2 langkah PIN) + `activateKlaspay` bertipe dibuat; belum diuji di device (butuh akun SSO tanpa Klaspay aktif) |
| G5 | 🟠 | ✅ Selesai | Foto profil Google tidak diambil (`GoogleAccountInfo` hanya `idToken`, `displayName`, `email`) | Kirim `image` = `profilePictureUri` — field ditambahkan |

## 5. Reset password, sesi, logout
| # | Tingkat | Status | Sekarang (29-09-2026) | Seharusnya |
|---|---|---|---|---|
| R1 | 🔴 | ✅ Selesai | Reset password berupa placeholder teks "Reset Password — belum diimplementasikan" (`AppNavHost.kt:163-166`) | Form email → `POST reset-password {user_id, email}` → layar terkirim + countdown 3 menit (§7) — `ResetPasswordScreen`/`ResetPasswordSentScreen` dibuat (belum diuji end-to-end di device) |
| R2 | 🔴 | ✅ Selesai | `DELETE logout` di-comment (`utils/IntentUtil.kt` fungsi `logOut`) | Dipanggil bila sebelumnya login (§10.2) — **diverifikasi di device** lewat log jaringan: `DELETE /api/logout` → 200 saat tombol Logout ditekan |
| R3 | 🔴 | 🟡 Sebagian | Tidak ada blokir logout saat ada ujian AKM belum dikumpulkan | Alert "Perhatian" → buka daftar ujian (§10.1) — diblokir memakai tabel `akm_synced_exam` (status `DOWNLOADED` belum `SUBMITTED`) sebagai **proksi sementara**; tabel jawaban ujian AKM offline yang sesungguhnya belum ada (lihat `FLOW_QUESTIONS.md` bagian B) — akan disesuaikan lagi saat fase Asesmen dikerjakan |
| R4 | 🔴 | ✅ Selesai | Topik FCM salah: unsubscribe `ebede-notification-user-*`; subscribe topik saat login belum ada | Daftar topik §8.4 / §10.2 — `FcmTopicManager` satu titik subscribe/unsubscribe, topik palsu dihapus |
| R5 | 🟠 | ✅ Selesai | Teks konfirmasi logout "Kamu akan keluar dari akun ini. Data offline … akan dihapus dari perangkat." | "Anda yakin akan keluar dari aplikasi?" (§10.1) — **diverifikasi di device** (screenshot) |
| R6 | 🔴 | ⛔ Belum | Belum ada validasi sesi berkala: `current-user` (konflik perangkat / 401), `is_active == false`, sekolah tidak terdaftar. `refreshProfile` hanya memperbarui profil | §9 — dipindah ke fase Home (dokumen 03); `SessionRefreshViewModel` sementara hanya menjaga tampilan Home/Akun tetap benar |
| R7 | 🔴 | ⛔ Belum | Belum ada dialog keamanan pasca-login (email belum terverifikasi → verifikasi, password default → ganti password); `markEmailVerificationSent()` masih mock | §8.2–8.3 — dipindah ke fase Home (dokumen 03); lihat P5 untuk apa yang sudah ada (notifikasi lokal ganti password) |
| R8 | 🟠 | ⛔ Belum | Belum ada deep link `verify-email` & reset PIN | §8.3 — dipindah ke fase Home (dokumen 03) |
| R9 | 🟠 | 🟡 Sebagian | Dialog 503/504 "Perbaikan Sistem" di-comment; `ApiException` tidak membawa `error_code` sehingga `DEVICE_CONFLICT` tidak bisa dideteksi | §9 interceptor; tambah `errorCode`, `retryAfterSeconds` — kedua field **sudah** ditambahkan ke `ApiException` (dipakai `AuthErrorMapper` untuk `DEVICE_CONFLICT`); dialog "Perbaikan Sistem" 503/504 **belum** dibuat (prioritas rendah — audit kode lama menemukan dialog itu nyaris tidak pernah tampil di app lama juga, karena dibangun dari context OkHttp yang bukan Activity) |
| R10 | 🔴 | ✅ Selesai | **Setiap** respons 401 langsung memicu logout otomatis + hapus seluruh database lokal (`ResponseInterceptor.kt:46-67`) | 401 ditangani **terpusat** (`SessionEvents` → dialog "Sesi Berakhir" di `AppNavHost` → `SessionManager.logout()`), yang **wajib** mengecek blokir ujian AKM (R3) dulu sebelum `clearAllTables()` — sudah tidak ada lagi logout otomatis tanpa pengecekan |

## 6. Yang sudah sesuai / lebih baik (per 29-09-2026, sebelum perbaikan fase ini)
- Credential Manager untuk Google Sign-In (pengganti Smart Lock + `GoogleSignIn`), sesuai rekomendasi §13.
- Dialog S&K memuat HTML `mobile/app/policy` di WebView dengan tombol "SAYA PAHAM".
- `RequestInterceptor` hanya mengirim `Authorization` bila token ada, dan pesan tanpa internet sama persis.
- Onboarding: 3 halaman dengan tombol "kembali"/"berikutnya"/"selesai" sudah ada (teks isi sekarang
  sudah disamakan ke app lama, lihat §1 S-series di atas).
- File prefs utama tetap `context.packageName` (`di/module/PreferenceModule.kt:17`), sehingga migrasi
  dari app lama cukup menyelaraskan key (bukan memindah file) — ini sudah dimanfaatkan penuh: semua
  key sesi sekarang dibaca/ditulis lewat `utils/session/SessionStore.kt` dengan nama key yang sama
  persis dengan `android-portal` (S3 selesai).

## 7. Sisa pekerjaan (diperbarui 30-09-2026)

Urutan perbaikan asli (P1/P3 → S/R-router/logout → G-series → R6/R7) **sudah selesai dikerjakan**
untuk cakupan "login inti + logout" — lihat `02b-implementasi-auth-login-logout.md` untuk laporan
lengkap per fase dan berkas yang dibuat/diubah. Yang masih terbuka, urut prioritas:

1. **P10** — putuskan apakah tombol `AppButton` perlu `textAllCaps` seperti `MaterialButton` lama,
   atau teks lowercase (`"masuk"`/`"login"`) memang gaya desain baru yang disengaja.
2. **P8** — verifikasi field `product_school` (`onklas_*` vs `diskola_*`) dengan response
   `login-account` asli begitu ada akun uji yang datanya lengkap.
3. **G1–G4** — uji end-to-end jalur SSO Google (akun tanpa sekolah/tamu, akun dengan sekolah,
   aktivasi Klaspay) di device fisik; kodenya sudah ada tapi belum pernah benar-benar dijalankan.
4. **R1** — uji end-to-end reset password (butuh akses ke email penerima link).
5. **S4, R6–R8** — bagian dari fase Home (dokumen 03): routing extra FCM `page`/`goto`, validasi
   sesi berkala (`current-user`), dialog keamanan (email/ganti password) yang benar via `HomeDialog`.
6. **S5** — worker `upload_log` dan gerbang banner FIAM (❓ Q4) — prioritas rendah, boleh menyusul.
7. **R3** — sesuaikan ulang begitu tabel jawaban ujian AKM offline yang sesungguhnya ada (keputusan
   migrasi database di `docs/FLOW_QUESTIONS.md` bagian B masih terbuka).
8. **R9** — dialog "Perbaikan Sistem" 503/504, kalau memang mau dibangun lebih baik dari app lama
   (bukan sekadar ditiru) — prioritas rendah, lihat catatan di baris R9.
