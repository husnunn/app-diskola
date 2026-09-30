# Dokumentasi Migrasi Diskola: `android-portal` (XML) → `Diskola-App-New` (Jetpack Compose)

Kumpulan dokumen ini adalah **spesifikasi perilaku** aplikasi Android lama `android-portal`
(`id.diskola.app` v2.1.40, versionCode 79), disusun langsung dari kode sumbernya per 29-09-2026.
Tujuannya satu: membangun ulang aplikasi dengan teknologi baru (Jetpack Compose, Material 3, Hilt,
Navigation Compose, Coroutines/Flow, DataStore, Coil, Credential Manager) **tanpa mengubah alur login
maupun fitur yang dipakai pengguna sekarang**.

> Dokumen di `docs/repo lama/` dibuat untuk v2.1.37 dan sebagian sudah basi. Jika berbeda, **yang
> berlaku adalah dokumen di folder ini**. Setiap dokumen punya bagian "Selisih dengan dokumen lama".

## Daftar dokumen

| # | Dokumen | Cakupan |
|---|---|---|
| 00 | `00-README.md` | Indeks, konvensi, cara pakai |
| 01 | `01-peta-fitur-dan-rencana.md` | Inventaris seluruh modul, peran & syarat akses, urutan migrasi, status di app baru |
| 02 | `02-auth-login-sesi.md` | Splash/router, onboarding, login NISN, Google SSO, reset password, dialog keamanan, validasi sesi, logout |
| 02a | `02a-gap-auth-diskola-app-new.md` | Selisih implementasi auth Compose yang sudah ada vs aplikasi lama + urutan perbaikan |
| 03 | `03-home-navigasi-akun-notifikasi.md` | Shell Home (tab, drawer, tile menu + syarat tampil), Akun & pengaturan, notifikasi FCM & routing, deep link, perilaku global BasePage |
| 04 | `04-sosial-feed-chat-pengumuman.md` | Feed sekolah, buat post/ebook, komentar, like, jelajah/hashtag, chat (Socket.IO), pengumuman |
| 05 | `05-pembelajaran-materi-tugas.md` | Hub pembelajaran, materi (siswa/guru), tugas/PR (siswa/guru), perpustakaan, agenda mingguan, poin |
| 06 | `06-asesmen-akm-tryout-ujian.md` | Asesmen/AKM (offline, timer, lockdown, pelanggaran, penalty, laporan masalah), try out, ujian |
| 07 | `07-presensi-magang-prokes-verifikasi.md` | Presensi (QR, GPS/geofence, selfie, offsite, jurnal), izin, magang, prokes/screening, verifikasi akun |
| 08 | `08-keuangan-pembayaran-klaspay-ppob.md` | Pembayaran & SPP, PIN, QR, Klaspay (aktivasi, top up, riwayat, tagihan, promo), PPOB, dana partisipasi |
| 09 | `09-marketplace-entrepreneurs-toko.md` | Toko sekolah (pembeli) & kewirausahaan (penjual): produk, keranjang, checkout, pesanan, ulasan, pendapatan |
| 10 | `10-infrastruktur-api-data-latar.md` | Build & dependency (lama → baru), manifest & permission, indeks endpoint, interceptor, DI, Room, semua key SharedPreferences, worker/service, utilitas, tema |

## Konvensi
- **Rujukan kode** ditulis relatif terhadap root `android-portal`, mis.
  `app/src/main/java/id/diskola/app/pages/login/LoginViewModel.kt:263`. Sebagian dokumen memakai
  singkatan `@/` = `app/src/main/java/id/diskola/app/` (dijelaskan di awal dokumen).
- **Teks UI dalam tanda kutip** disalin persis dari kode/`strings.xml`, termasuk salah ketik aslinya.
  Tombol MaterialButton lama tampil huruf kapital (mis. `"masuk"` tampil **MASUK**).
- **❓ Perlu keputusan user**: perilaku aneh/bug di aplikasi lama yang *mengubah pengalaman pengguna*
  bila diperbaiki. Jangan diperbaiki diam-diam; putuskan dulu, lalu catat jawabannya di
  `docs/FLOW_QUESTIONS.md` (sesuai `docs/rules-global.md` §2).
- **Checklist paritas** di akhir tiap dokumen dipakai sebagai kriteria "selesai" per fitur: cocokkan
  langsung dengan aplikasi lama di perangkat, bukan dari ingatan.

## Cara pakai saat mengerjakan satu fitur
1. Baca dokumen fitur tersebut dan bagian terkait di `10-infrastruktur-api-data-latar.md`
   (endpoint, tabel Room, key pref).
2. Selesaikan semua "❓" yang relevan dengan user, catat di `docs/FLOW_QUESTIONS.md`.
3. Implementasikan mengikuti `docs/ANDROID_DESIGN_PATTERN.md` dan `docs/rules-global.md`
   (terutama disiplin fetch: tidak ada fetch ganda saat layar dibuka).
4. Centang checklist paritas dengan membandingkan langsung ke aplikasi lama.

## Batasan dokumen
- Dibuat dari pembacaan kode statis. Perilaku yang hanya bisa dipastikan saat aplikasi berjalan
  ditandai "(belum terverifikasi)".
- Nilai rahasia (URL API, client ID, API key Maps, keystore) sengaja tidak ditulis; hanya **nama
  key** `local.properties`-nya.
