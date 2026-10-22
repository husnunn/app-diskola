# 05g — Selisih Hub Pembelajaran (Home) Baru vs `PembelajaranPage` Lama

Cakupan 2026-10-12: menyamakan tab **Pembelajaran** (`ui/screens/home/`) dengan `PembelajaranPage` aplikasi lama dalam hal **data
yang tampil, endpoint yang dipanggil, dan aturan tampil/sembunyi**; UI boleh berbeda sedikit. Termasuk penanganan teks/respons
panjang. Sumber kebenaran: kode lama `android-portal/.../pages/pembelajaran/PembelajaranPage.kt`, `PembelajaranVm.kt`,
`pages/presensi/PresensiViewModel.kt` (banner), `pages/SessionGuard.kt`, `pages/sekolah/SosmedViewModel.kt` (`checkUser`) dan layout XML-nya
(dibaca langsung, bukan ringkasan). Laporan implementasi: `05h-implementasi-hub-pembelajaran.md`. Spesifikasi perilaku: `03` §3.
Path relatif `Diskola-App-New/app/src/main/java/id/diskola/app/`.

Legenda: ✅ kode selesai + teruji JVM (`HubRulesTest`, `HubModelsParseTest`) · ✅* kode selesai, **belum diuji di device** (HP uji tidak
terhubung saat fase ini selesai; akunnya guru) · ⛔ di luar cakupan / ditunda.

## 1. Endpoint yang dipanggil

Legacy `loadData()` jalan di tiap `onResume` dan ikon refresh. Urutan baru mengikuti legacy; panggilan **`schedule?date=` dihapus dari Home** (legacy tidak
memanggilnya — kartu kelas datang dari `check-account.current_subject`).

| Endpoint | Siapa | Lama | Baru |
|---|---|---|---|
| `POST …/authentication/check-account` | semua | ✅ | ✅ (sudah ada); kini juga memetakan `current_subject`, `userProgress`, `school.city_name` |
| `GET …/authentication/current-user` (SessionGuard) | semua | ✅ | ✅* baru: dialog "Perangkat Lain Terdeteksi"; 401 sudah ditangani terpusat (`SessionEvents`) |
| `GET payment/wallet` → `GET mobile/notification/summary?wallet_id=` | Klaspay aktif | ✅ | ✅* baru: badge lonceng ("99+" dari 99) |
| `GET mobile/attendance/setting/me/today` + `…/{role}/check` | non-siswa dengan konten tampil | ✅ | ✅* baru: banner presensi |
| `GET mobile/attendance/staff/agendas/today` | guru | ✅ | ✅ (sudah ada) |
| `GET mobile/app/check-feature-availability?name=` | tap Presensi / Jurnal / CTA banner | ✅ | ✅ (sudah ada) |
| `GET mobile/attendance/schedule?date=` | — | **tidak ada di Home lama** | **dihapus dari Home** (tetap dipakai layar Jurnal) |

## 2. Data & tampilan

| Item | Status | Keterangan |
|---|---|---|
| Kartu "Kelas Berlangsung" siswa dari `current_subject` (ikon → logo sekolah → ikon buku; nama mapel / "Tidak terdapat jadwal"; jam mentah `start - end`, disembunyikan bila start kosong) | ✅* | Aturan murni `HubRules.classAction/className/classTime`. |
| Tombol kartu: "Check In" (`attendance_id≠null` & status kosong), "Isi Jurnal" (`attendance_id==null`), tidak ada bila ada status / `id` 0 | ✅* | Label sama dengan legacy (sebelumnya "Hadiri"). `id` null → tombol disembunyikan (legacy meloloskannya — deviasi). |
| "Check In" → sheet "Pilih Metode Presensi" **hanya** "Verifikasi Jurnal" → `VerifikasiJurnal`; "Isi Jurnal" → `JurnalForm(plotId = current_subject.id)` | ✅* | Tombol "Scan QR" di sheet legacy mati (selalu tersembunyi); tidak dibangun di Hub. Layar Jurnal tetap punya QR. |
| Guru/staf: banner presensi (NO_AGENDA / HOLIDAY / OUTSIDE_HOURS / CHECK_IN / CHECK_OUT / COMPLETE, jam kerja, "Agenda · …", sumber, CTA "Presensi" → gate `presensi`) menggantikan kartu kelas | ✅ (aturan) / ✅* (UI) | Teks persis legacy. Banner muncul di tempat kartu konten non-siswa (legacy `layoutCurrentClass` visible). Warna CTA oranye untuk pulang tidak dibawa (CTA satu warna). |
| Status verifikasi: tabel `userProgress.status` × role × `is_having_class` (approved / "" / in_review / rejected) | ✅ (aturan) / ✅* (UI) | "Verifikasi Data" aktif; nonaktif (kartu abu/merah) berisi pesan server; siswa tanpa kelas → "Pemberitahuan" + "Anda Belum Terdaftar di Kelas Manapun,\nSilakan Hubungi Admin!!". Tombol "Sudah"/"Belum" masih ke dialog "dalam pengembangan" (`VerificationPage` belum dibangun). |
| Lonceng: badge angka; tap → Notifikasi bila Klaspay aktif, selain itu toast "Silahkan aktivasi wallet terlebih dahulu untuk mengunakan fitur notifikasi" + layar aktivasi Klaspay | ✅* | Layar Notifikasi masih data contoh (fase notifikasi). `Route.Auth.KlaspayActivation(returnToMain)` agar selesai kembali ke Home. |
| Header: logo sekolah, nama sekolah, kartu user (avatar → logo sekolah → inisial, nama, baris `title_class`: email guru / nama kelas siswa / "Belum memiliki kelas"), ikon refresh (spinner saat memuat) | ✅* | Sapaan "Assalamualaikum," dipertahankan (gaya). **Dihapus:** baris kota (tak ada di legacy) dan placeholder "Carousel banner · 3 slide" (legacy menyembunyikan carousel selamanya). |
| Menu per peran (urutan, lock = `!is_having_class`, badge agenda "9+") | ✅ | Sudah sama sejak `05b`; kini label 2 baris + ellipsis, tinggi minimum (tidak tetap), ikon kunci di kiri-atas seperti legacy. |
| Dialog: tamu, akun nonaktif, terkunci, dalam pengembangan, verifikasi | ✅* | Akun nonaktif kini menghormati `BlockedByPendingExam` (sebelumnya logout diam-diam); popup tamu bertahan saat rotasi. |

## 3. Teks / respons panjang

| Elemen | Perlakuan |
|---|---|
| Nama sekolah, nama user, baris kelas/email | 1 baris + ellipsis; di antara logo/aksi sehingga tidak mendorong tata letak |
| Nama mapel kartu kelas | 2 baris + ellipsis |
| Pesan verifikasi (`userProgress.message`), info banner, nama agenda | 3 (agenda 2) baris + ellipsis; ketuk → seluruh isi ("Selengkapnya"/"Ringkas") — `ExpandableText` |
| Label menu | 2 baris + ellipsis, tile tumbuh sesuai font |
| Header | `heightIn(min)` + padding bawah = `HeroOverlap`, kartu konten tidak menimpa teks pada font besar |
| `AppDialog` (semua dialog aplikasi) | judul + isi + konten bisa di-scroll (maks 80% tinggi layar), tombol tetap terlihat |
| `FeatureUnavailableSheet` | pesan server (guru) bisa di-scroll (maks 220 dp) |

Verifikasi: `@Preview` terang/gelap/font 2× di `HomePreviews.kt` (header, kartu kelas, verifikasi, banner, tile). Model: `HubModelsParseTest` memuat pesan >8.000 karakter.

## 4. Bug aplikasi baru yang ikut diperbaiki
Data hero basi (`remember` tak terikat hasil check-account) → satu `StateFlow<HomeUiState>`; kota sekolah terhapus tiap check-account (`SchoolDetail` tanpa `city_name`);
`confirmInactiveLogout` mengabaikan `BlockedByPendingExam`; dua sumber `isTeacher`; popup tamu muncul lagi saat rotasi; `NotifikasiApiService` tidak pernah disediakan di DI.

## 5. Di luar cakupan / belum
⛔ Item `HomePage` (host, bukan fragment): dialog jam "Otomatis" tiap resume, dialog keamanan (email / password default), dialog gagal wallet Klaspay, FCM topic. ⛔ `VerificationPage`, `MagangPage`, daftar Notifikasi nyata, halaman Klaspay selain aktivasi.
**Diuji di device 2026-10-12 (sesi siswa, Klaspay aktif):** urutan endpoint (check-account → current-user → wallet → summary, tanpa `schedule`), header, kartu kelas kosong, tile, bell. Respons asli: `current_subject` = `{}`, `userProgress` = null,
`school` tanpa `city_name`, `wallet.user_id` string, `count_unread` = 0.
**Belum diverifikasi (jangan diasumsikan selesai):** sisi guru (banner presensi, lencana agenda) setelah perubahan; "Check In"/"Isi Jurnal" dengan `current_subject` terisi; verifikasi in_review/rejected; badge lonceng > 0; respons asli
`setting/me/today` dan `staff/check`; dialog "Perangkat Lain Terdeteksi" (butuh dua perangkat).
