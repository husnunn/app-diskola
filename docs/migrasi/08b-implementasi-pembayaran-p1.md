# 08b — Implementasi Pembayaran P1 (fondasi, hub, aktivasi, PIN)

Dikerjakan 2026-10-14. Spesifikasi: `08` §1–§5, `02` §8.2 (dialog keamanan). Gap: `08a-gap-pembayaran.md`. Path relatif `app/src/main/java/id/diskola/app/`.

## 1. Arsitektur
`UI → ViewModel → Repository → Retrofit` (tanpa Room di P1).

| Berkas | Tanggung jawab |
|---|---|
| `utils/LogRedactor.kt` (+ tes 9) | Menyamarkan `pin`, `password`, `old/new_password`, `reset_token`, `token`… (JSON string/angka & form) dan header `Authorization`/`Cookie`. Dipakai logger HTTP dan `LogFileInterceptor`. |
| `di/module/ApiModule.kt`, `App.kt` | Log body HTTP hanya `BuildConfig.DEBUG`; Timber `DebugTree` hanya debug (Q14). Chucker sudah no-op di release. |
| `dataclass/ResponData/KlaspayModels.kt` (+ tes 8) | DTO lenient: wallet (`balance` Double: terima angka/string/null), reset-pin, set-pin, `change-profile`, `change-password`. |
| `apiservice/KlaspayApiService.kt`, `AkunApiService.kt`, `AuthApiService.kt` | Diketik: `payment/check`, `activate`, `wallet`, `reset-pin`, `reset-pin/setpin`, `GET payment/reset-pin/token/{token}`, `change-profile`, `change-password`, `mobile/email/verify`. `AkunApiService` kini punya provider Hilt. Endpoint lain tetap placeholder untuk P2+. |
| `utils/session/SessionKeys/SessionStore.kt` | Key legacy `isToppers`, `toppersStatus`, `klaspay_id`; `writeWallet` (setelah respons — memperbaiki race legacy), `markEmailVerifying`, `markPasswordChanged`, `writeUserContact`. |
| `repository/KlaspayRepository.kt` | `wallet()`, `preflightCheck()` (kirim ulang verifikasi bila error `email`), `activate()`, `resetPin()`, `setNewPin()`, `validateResetToken()`. |
| `repository/AccountSecurityRepository.kt` | `changeEmail`, `requestEmailVerification`, `changePassword` + sinkron flag sesi. |
| `utils/PaymentRules.kt` (+ tes 17), `utils/SecurityDialogRules.kt` (+ tes 6) | Logika murni: menu hub per peran, langkah aktivasi, PIN, reset PIN, Rupiah, validasi email/password dengan teks legacy persis (termasuk typo). |
| `viewmodel/PaymentHubViewModel.kt` | `payment/wallet` hanya bila wallet aktif; on resume + refresh; `inFlight` sebelum `launch`; throttle 5 dtk. |
| `viewmodel/ActivationGateViewModel.kt` + `ui/components/{ActivationGate,SecurityDialogs}.kt` | Satu mesin status untuk: dialog "Aktivasi Wallet Lebih Dahulu" → dialog email → dialog ganti password → `payment/check` → buka layar PIN. Satu instance di `MainTabsScreen`, dipakai hub dan lonceng Home. |
| `ui/screens/pembayaran/PembayaranScreen.kt`, `ui/components/WalletHeader.kt` | Hub nyata; menu belum jadi → "Fitur dalam pengembangan" (tak ada layar dummy yang terjangkau). |
| `ui/screens/auth/KlaspayActivationScreen.kt`, `viewmodel/KlaspayActivationViewModel.kt` | Toolbar "Buat PIN Wallet", KONFIRMASI hanya aktif 6 digit / PIN sama, label "Pin yang Anda masukkan tidak sama", dialog sukses (2 tombol), back bertahap; PIN tak disimpan di state yang tersimpan. |
| `ui/screens/pembayaran/{ConfirmPinScreen,SetPinScreen}.kt`, `viewmodel/{ConfirmPinViewModel,SetPinViewModel}.kt`, `utils/{PinResultChannel,EmailApp}.kt`, `ui/components/PinEntry.kt` | "Konfirmasi Pembayaran" + "Lupa pin?" + "Atur Pin Baru"; PIN kembali ke pemanggil lewat `PinResultChannel` (memori, sekali baca). |
| `Route.kt`, `AppNavHost.kt`, `MainTabsScreen.kt`, `AndroidManifest.xml` | `Route.PembayaranSub.ConfirmPin`, `Route.SetPin`, `Route.SetPinLink` (+ `navDeepLink` prod/dev) dan intent-filter `https://(dev.)api.diskola.id/api/payment/reset-pin/token`. |

## 2. Keputusan (user) — `FLOW_QUESTIONS.md` 2026-10-14
Hub ikuti app lama · aktivasi seragam lewat pra-cek (Q19) · SSO "TOP UP SEKARANG" tiru lama (Q2, dibangun di P3).

## 3. Temuan teknis
| # | Temuan | Penanganan |
|---|---|---|
| 1 | PIN tercetak di Logcat (BODY di semua build) | `LogRedactor` + level BODY hanya debug (Q14). |
| 2 | `klaspay_id` ditulis sebelum respons wallet tiba (race legacy) | `writeWallet` setelah respons. |
| 3 | Dialog email "Nanti Saja" memicu ulang tombol aktivasi → dialog muncul lagi tanpa akhir | `PaymentRules.afterEmailStep`: lanjut ke password bila default, selain itu berhenti. |
| 4 | Deep link legacy mengambil URL host apa pun dengan kredensial pengguna | Validasi token lewat base API build ini (`GET payment/reset-pin/token/{token}`), bukan host dari tautan. |
| 5 | PIN lewat route/`SavedStateHandle` bisa tersimpan di saved-state bundle | `PinResultChannel` di memori, sekali baca. |
| 6 | Layar dummy `PembayaranSub.*` berbagi `PembayaranViewModel` bermock | Tetap ada sebagai referensi UI, tak terjangkau dari hub; diganti per sub-fase, dihapus di P6. |

## 4. Pengujian
- JVM: 121 tes lolos (`testDebugUnitTest`): 81 lama + `LogRedactorTest` 9, `PaymentRulesTest` 17, `SecurityDialogRulesTest` 6, `KlaspayModelsParseTest` 8.
- `:app:compileDebugKotlin` dan `:app:assembleDebug` bersih.
- **Device: belum diuji** — HP uji tidak terhubung saat P1 selesai. Perlu diverifikasi: `payment/wallet` tepat sekali per masuk tab/refresh; saldo/nama/ID wallet = respons asli; info wallet + Salin; menu siswa/guru (Game tersembunyi untuk guru); tile belum jadi → dialog; "Pinjam Buku" → dialog; teks panjang; status bar/nav bar.
- Tidak teruji tanpa akun/aksi khusus: alur wallet belum aktif (butuh akun tanpa Klaspay); "Lupa pin" mengirim email nyata & dibatasi 1× per 3 hari; deep link `autoVerify` (butuh APK rilis + assetlinks server); sisi guru.
