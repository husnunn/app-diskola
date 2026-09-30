# 02a — Selisih Implementasi Auth Compose Saat Ini vs Aplikasi Lama

Hasil membandingkan kode auth yang **sudah ada** di `Diskola-App-New` (per 29-09-2026, termasuk
perubahan yang belum di-commit) dengan spesifikasi `02-auth-login-sesi.md`. Tujuannya daftar kerja
agar alur login kembali identik dengan `android-portal`.

Path di bawah relatif terhadap `Diskola-App-New/app/src/main/java/id/diskola/app/`.

Legenda: 🔴 alur/fungsi rusak atau hilang (wajib) · 🟠 aturan/logika berbeda (wajib, kecuali
diputuskan lain) · 🔵 teks/tampilan berbeda (cek dulu: bisa jadi disengaja mengikuti desain baru di
`ref/design_handoff_auth_fase1/`).

> Catatan penting: komentar di `viewmodel/AuthViewModel.kt:184` dan `ui/screens/auth/GoogleSignIn.kt`
> menyebut kode ini di-port dari `LoginPasswordActivity`/`LoginActivity` — itu **bukan** kelas dari
> `android-portal` (di sana namanya `LoginForm`/`LoginProcess`/`LoginSso`). Topik logout
> `ebede-notification-user-*` (`utils/IntentUtil.kt:538`) juga bukan milik Diskola. Kemungkinan besar
> sebagian logika auth berasal dari proyek lain, jadi perlu diselaraskan ulang ke `android-portal`.

## 1. Splash & router
| # | Tingkat | Sekarang | Seharusnya (lama) |
|---|---|---|---|
| S1 | 🔴 | `Route.Onboarding` tidak pernah dituju: Splash hanya ke `Login`/`Main` (`ui/navigation/AppNavHost.kt:37-49`), pref `onboard` tidak dibaca | `onboard == false` → Onboarding → Login; `selesai` set `onboard=true` (§2.4, §3) |
| S2 | 🔴 | Hasil `checkVersion()` dibuang (timeout 2 dtk), `UPDATE_REQUIRED` diarahkan ke Login (`SplashScreen.kt:121`, `AppNavHost.kt:40`) | Versi server lebih besar → dialog wajib "Update Tersedia" → Play Store → tutup app (§2.2) |
| S3 | 🔴 | Status login dibaca dari file prefs **`user_session`** (`is_logged_in` + `token`) (`SplashScreen.kt:123-135`) | Pref `logged_in` di file `id.diskola.app`. Pengguna yang update dari app lama akan **ter-logout** (❓ Q9 di dokumen 02) |
| S4 | 🔴 | Tidak ada penanganan extra FCM `page`/`goto` saat app dibuka dari notifikasi | Buka layar tujuan notifikasi langsung bila sudah login (§2.1, §2.4) |
| S5 | 🟠 | Tidak membuat 3 notification channel, tidak menjadwalkan worker `upload_log`, tidak ada gerbang banner FIAM | §2.1, §2.3 (FIAM: ❓ Q4) |
| S6 | 🔵 | Splash beranimasi + teks "Memuat…" + "DISKOLA MOBILE V…" | Splash statis putih + logo |

## 2. Form login (NISN + sekolah)
| # | Tingkat | Sekarang | Seharusnya |
|---|---|---|---|
| L1 | 🟠 | Tombol aktif jika `nisn.length >= 4` (`ui/screens/auth/LoginScreen.kt:75`) | Cukup tidak blank (§4.1) |
| L2 | 🟠 | Pemilih sekolah: pencarian server (`q`, `name`, `take=10`), daftar **kosong sampai user mengetik**, tanpa paging/pull-to-refresh (`viewmodel/AuthViewModel.kt:57-73`, `apiservice/AuthApiService.kt:27-33`) | Daftar langsung tampil (20/halaman, infinite scroll, pull-to-refresh), pencarian lokal debounce 500 ms (§4.2, ❓ Q3) |
| L3 | 🟠 | Error check-account tampil sebagai `BannerError` inline | Bottom sheet "Login Gagal" + tombol "Coba Lagi" (§4.1) |
| L4 | 🟠 | Efek samping check-account tidak disimpan: `default_pass`, `user_id`, `is_active`, pref `user`/`school` | §4.1 "Efek samping" (dipakai reset password & dialog ganti password) |
| L5 | 🔵 | Header "Selamat Datang", "SISTEM INFORMASI AKADEMIK TERPADU", badge "Aktif"; label "NISN / NIS / NIK" + contoh; teks S&K pendek "Saya menyetujui Syarat & Ketentuan penggunaan Diskola."; footer "NISN/NIK belum terdaftar? …" + tombol "Bantuan" (onClick kosong) | Judul "LOGIN AKUN", hint "Ketikkan NISN/NIS/NIK", kalimat S&K panjang (§4.1); tidak ada footer bantuan |
| L6 | 🔵 | Judul pemilih sekolah "Pilih Lembaga Sekolah", placeholder "Cari nama sekolah" | "Pilih Sekolah" / "Cari sekolah" |

## 3. Layar password
| # | Tingkat | Sekarang | Seharusnya |
|---|---|---|---|
| P1 | 🔴 | `LoginAccountRequest` hanya `{uuid, password}` — **tanpa `device_id`** (`dataclass/ResponData/LoginResponse.kt:12-15`) | `{uuid, password, device_id: ANDROID_ID}`; backend lama menolak dengan 422 bila `device_id` kosong (§4.3) |
| P2 | 🔴 | Kontak "Hubungi Kami" berisi nomor contoh "WhatsApp operator · 0812-3456-7890" dan "Telepon sekolah · (0321) 123456" (`ui/screens/auth/PasswordScreen.kt:156-163`) | "NISN/NIK terdaftar bukan milik Anda? **Hubungi Kami**" → WhatsApp `6287887219649`, fallback telepon (§4.3) |
| P3 | 🔴 | Error login diganti pesan generik "Cek Password atau Akun Anda" (`AuthViewModel.kt:171`); tidak ada dialog konflik perangkat | 400 "Detail login tidak bisa digunakan" → "Password yang anda masukkan salah"; `DEVICE_CONFLICT` → dialog "Perangkat Lain Terdeteksi"; selain itu pesan backend (§4.3) |
| P4 | 🟠 | Tombol aktif jika `password.length >= 4` (`PasswordScreen.kt:147`) | Password tidak kosong |
| P5 | 🟠 | Password default → dialog "Ganti Password" / "Mengerti" lalu masuk Main (`PasswordScreen.kt:228-236`) | Notifikasi lokal "Ganti Password" + dialog `ChangePasswordDialog` di Home yang wajib bila Klaspay aktif (§4.3, §8.2) |
| P6 | 🟠 | Setelah login tidak memanggil `setup-user-fcm` dan `Firebase.analytics.setUserId` | §4.3 langkah 3 |
| P7 | 🟠 | `saveSession` (`AuthViewModel.kt:189-281`): peran ditebak dari nama role ("teacher"/"guru"/"student"/"siswa") dan siswa diprioritaskan; tidak menulis `isSso`, `is_having_class`, `student`/`teacher` JSON, `class_id`, `school` JSON, `school_uuid`, `user` JSON di prefs `id.diskola.app` | Peran dari `rule.is_student`/`rule.is_teacher`; tulis semua key di tabel §4.3 |
| P8 | 🟠 | Produk sekolah dibaca dari field `diskola_lite`/`diskola_pro` (`LoginResponse.kt:165`) | Field JSON di app lama `onklas_lite`/`onklas_pro` — kalau backend mengirim `onklas_*`, nilai sekarang selalu `false` (menu PRO bisa hilang). Verifikasi dengan response asli |
| P9 | 🟠 | Label peran "PENGGUNA"/"Guru Mapel", identitas "NISN {nis_nik}", data dikirim lewat argumen route | `role_label` = `roles.first().name`, kelas `class_room.name`, nomor `nis_nik` (§4.3) |
| P10 | 🔵 | Label "Password", "Lupa password? …", tombol "Login" | Hint "Masukkan Password", "Lupa password? **Reset**", tombol "LOGIN" |

## 4. Login Google (SSO)
| # | Tingkat | Sekarang | Seharusnya |
|---|---|---|---|
| G1 | 🔴 | Credential Manager sudah dipakai (baik), tetapi `login-sso` **tidak dipanggil**; `SsoScreen` menampilkan "-" untuk semua detail, `linked` selalu `false` | `POST login-sso {email, name, image, device_id}` → isi layar dari response (§5.2–5.3) |
| G2 | 🔴 | Dialog tamu muncul **setelah** menekan "Iya, itu saya"/"Daftar", lalu langsung ke Main **tanpa login ke backend** (`AppNavHost.kt:133-160`) | Dialog tamu muncul **sebelum** layar SSO hanya bila akun belum punya sekolah; "iya, itu saya"/"daftar" → `POST login-sso/school` → Home atau Aktivasi Klaspay (§5.2–5.3, ❓ Q1) |
| G3 | 🔴 | Frasa konfirmasi "SAYA MENGERTI" (`ui/screens/auth/AuthDialogs.kt:52`) | `lanjutkan sebagai tamu dulu bukan siswa/guru` (case-sensitive), teks dialog §5.2 |
| G4 | 🔴 | Tidak ada alur Aktivasi Klaspay setelah SSO | §6 |
| G5 | 🟠 | Foto profil Google tidak diambil (`GoogleAccountInfo` hanya `idToken`, `displayName`, `email`) | Kirim `image` = `profilePictureUri` |

## 5. Reset password, sesi, logout
| # | Tingkat | Sekarang | Seharusnya |
|---|---|---|---|
| R1 | 🔴 | Reset password berupa placeholder teks "Reset Password — belum diimplementasikan" (`AppNavHost.kt:163-166`) | Form email → `POST reset-password {user_id, email}` → layar terkirim + countdown 3 menit (§7) |
| R2 | 🔴 | `DELETE logout` di-comment (`utils/IntentUtil.kt` fungsi `logOut`) | Dipanggil bila sebelumnya login (§10.2) |
| R3 | 🔴 | Tidak ada blokir logout saat ada ujian AKM belum dikumpulkan | Alert "Perhatian" → buka daftar ujian (§10.1) |
| R4 | 🔴 | Topik FCM salah: unsubscribe `ebede-notification-user-*`; subscribe topik saat login (`diskola-notification-user-*`, `school-leave-request-*`, dst.) belum ada | Daftar topik §8.4 / §10.2 |
| R5 | 🟠 | Teks konfirmasi logout "Kamu akan keluar dari akun ini. Data offline … akan dihapus dari perangkat." | "Anda yakin akan keluar dari aplikasi?" (§10.1) |
| R6 | 🔴 | Belum ada validasi sesi berkala: `current-user` (konflik perangkat / 401), `is_active == false`, sekolah tidak terdaftar. `refreshProfile` hanya memperbarui profil | §9 |
| R7 | 🔴 | Belum ada dialog keamanan pasca-login (email belum terverifikasi → verifikasi, password default → ganti password); `markEmailVerificationSent()` masih mock (`viewmodel/AkunViewModel.kt:45-47`) | §8.2–8.3 |
| R8 | 🟠 | Belum ada deep link `verify-email` & reset PIN | §8.3 |
| R9 | 🟠 | Dialog 503/504 "Perbaikan Sistem" di-comment (`apiservice/ResponseInterceptor.kt:185`); `ApiException` tidak membawa `error_code` sehingga `DEVICE_CONFLICT` tidak bisa dideteksi | §9 interceptor; tambah `errorCode`, `retryAfterSeconds` |
| R10 | 🔴 | **Setiap** respons 401 langsung memicu logout otomatis + hapus seluruh database lokal (`ResponseInterceptor.kt:46-67`) | **✅ Diputuskan (30-09-2026, 02 Q6):** 401 tetap ditangani **terpusat** (bukan diteruskan ke tiap layar seperti app lama) — arah project baru sudah benar. Yang **wajib diperbaiki**: fungsi logout yang dipicu 401 ini harus lebih dulu memanggil pengecekan blokir ujian AKM (unfinished **atau** FINISHED-belum-terunggah, dokumen 06 §15/§23.3#2) sebelum `clearAllTables()` — saat ini tidak ada pengecekan itu sama sekali, sehingga jawaban AKM offline pasti terhapus pada 401 pertama yang tak terduga |

## 6. Yang sudah sesuai / lebih baik
- Credential Manager untuk Google Sign-In (pengganti Smart Lock + `GoogleSignIn`), sesuai rekomendasi §13.
- Dialog S&K memuat HTML `mobile/app/policy` di WebView dengan tombol "SAYA PAHAM".
- `RequestInterceptor` hanya mengirim `Authorization` bila token ada, dan pesan tanpa internet sama persis.
- Onboarding: 3 halaman dengan tombol "kembali"/"berikutnya"/"selesai" sudah ada (teks isi berbeda, 🔵).
- File prefs utama tetap `context.packageName` (`di/module/PreferenceModule.kt:17`), sehingga migrasi
  dari app lama cukup menyelaraskan key (bukan memindah file) — asal status login tidak lagi dibaca
  dari `user_session` (S3).

## 7. Urutan perbaikan yang disarankan

> Semua ❓ di dokumen ini sudah dijawab 30-09-2026 — lihat `docs/FLOW_QUESTIONS.md` entri "2026-09-30
> — Keputusan atas seluruh dokumen migrasi" bagian C, dan tabel §14 di `02-auth-login-sesi.md`.

1. P1, P3 (device_id & error mapping) → login bisa dipakai di backend produksi.
2. S1–S4, R2–R4, R10 (router, logout, topik FCM satu titik per Q7, logout otomatis 401 per Q6) →
   siklus login-logout benar **dan** jawaban AKM offline tidak ikut terhapus (fungsi logout wajib
   memanggil pengecekan blokir ujian AKM dari 06 §15 sebelum `clearAllTables()`, dipakai oleh SEMUA
   pemicu logout termasuk 401 terpusat dan FCM `"logout"` per Q8).
3. G1–G4 (SSO lengkap + aktivasi Klaspay — termasuk validasi wajib pilih sekolah per Q2, dan dialog
   tamu yang meniru persis kondisi kode lama per Q1), R1 (reset password).
4. R6–R7 (validasi sesi & dialog keamanan).
5. Sesi tetap login setelah update in-place (migrasi SharedPreferences per Q9 — **final**); migrasi
   database Room/AKM **masih terbuka**, jangan diasumsikan (`docs/FLOW_QUESTIONS.md` bagian B).
6. Sisanya (🟠 lalu 🔵 sesuai keputusan desain).
