# 07e — Selisih Implementasi Magang vs Aplikasi Lama

Cakupan 2026-11-04: **Magang** (doc `07` §11, §14.4, §16, §19#8–12) — shell Jadwal/Laporan, presensi masuk (peta + radius perusahaan), laporan harian +
presensi keluar, daftar laporan dengan filter tanggal, dan izin magang (daftar, ajukan, detail). Hanya siswa. Laporan implementasi:
`07f-implementasi-magang.md` (ditulis di akhir fase). Path relatif `Diskola-App-New/app/src/main/java/id/diskola/app/`. Kode legacy:
`android-portal/.../pages/magang/` dan `res/layout/magang_*.xml`.

Legenda: ✅ selesai & teruji (JVM/compile) · ✅* kode selesai, **belum diuji di device** · ⏳ direncanakan/dalam pengerjaan · ⛔ tidak dibangun.

## 1. Fondasi (tahap S1)

| Item | Status | Keterangan |
|---|---|---|
| `MagangApiService` bertipe (`schedule`, `attend`, `attendance`, `dailyReport`, `leaveRequest`, `deleteLeaveRequest`) + terdaftar di Hilt | ✅ | Sebelumnya `Map<String, Any>` dan **tidak terdaftar** di `ApiModule`. Aksi tulis mengembalikan `Unit` (legacy tidak membaca isinya; mengambil ulang daftar). Body `attend` = `{id, lat, lng}` bertipe Double (Presensi memakai String). |
| DTO `MagangModels.kt` (jadwal, perusahaan, state presensi, izin, laporan, lokasi `latitide`) | ✅ | Semua lenient (`@NullToEmptyString`); koordinat dan radius perusahaan datang sebagai String (angka juga terbaca). Bentuk dari kode legacy dan tampilan app lama untuk akun dev, **bukan** kontrak resmi (sebagian contoh JSON di `docs/repo lama/api/06` karangan). |
| `MagangRepository` tanpa cache Room | ✅ | Legacy menghapus tabel jadwal sebelum tiap fetch (offline = kosong + toast mentah) dan laporan/izin saling menimpa; menambah tabel Room butuh `MIGRATION_11_12` (tanpa itu `fallbackToDestructiveMigration` menghapus jawaban AKM yang belum terkirim). Offline = `ErrorState` + coba lagi. |
| `MagangRules` + `MagangRulesTest` + `MagangModelsParseTest` | ✅ | Tanggal/jam, penyaringan jadwal/laporan/izin, gabungan izin, validasi laporan dan izin, banner izin, label, ekstraksi URL gambar, pesan error. |

## 2. Shell, Jadwal (doc §11.1–11.2) — tahap S2, diuji di Samsung (siswa BUDI) 2026-11-04

| Item | Status | Keterangan |
|---|---|---|
| Tile "Magang" Hub (siswa, terkunci bila `!is_having_class`, tanpa feature gate) → `Route.Magang` | ✅ | Tile terbuka ke shell; `MenuTileData.implemented = true`. |
| Tab "Jadwal"/"Laporan", strip tanggal bulan berjalan, label bulan → `MonthYearPickerDialog`, ganti bulan → tanggal 1 | ✅ | Terbuka pada hari ini (seperti legacy). Label bulan diberi panah kecil sebagai tanda bisa diketuk. Isi tab Laporan masih placeholder (S4). |
| Kartu jadwal: perusahaan, tanggal, "Pukul", status (`status`) bila tanpa tombol, "Laporan Harian" (`can_leave`), "Masuk" (`can_attend`) | ✅* | Teramati di device: kartu dengan "Masuk" (`can_attend`) dan kartu berstatus teks ("Belum waktunya presensi masuk", "Waktu presensi masuk sudah lewat"). "Laporan Harian" (`can_leave`) belum pernah true di data uji. Jam `HH:mm` (legacy `HH:mm:ss` mentah). Dua tombol sekaligus disusun atas-bawah (legacy menumpuknya). **Tombol belum tersambung ke layarnya (S3/S4).** |
| Skeleton, "Jadwal Tidak Tersedia", error + "Coba Lagi", pull-to-refresh | ✅* | "Jadwal Tidak Tersedia" dan error state teramati di device; skeleton dan pull-to-refresh belum diamati. |
| `AutoTimeGate` tiap resume di tab Jadwal | ✅* | Komponen bersama; tidak terpicu karena jam ponsel uji otomatis. |
| Menu "Panduan" (4 langkah) dan "Tambah Izin" | ✅* | Panduan = bottom sheet statis (legacy: overlay bertahap; pref `onboarding_magang_filter_done` hanya ditulis, tidak pernah dibaca) — teramati di device. "Tambah Izin" ⏳ S5. |
| `company.id` > `Int.MAX_VALUE` (2149267629) pada data dev | ✅ | DTO `Long?`; tes regresi `schedule_companyIdPastIntRangeAndRadiusAsNumber…`. |

## 3. Presensi masuk (doc §11.3) — tahap S3, diuji di Samsung (siswa BUDI) 2026-11-04

| Item | Status | Keterangan |
|---|---|---|
| Peta perusahaan + lingkaran radius bila `radius_enabled`; kartu status; "Konfirmasi" → dialog konfirmasi → `POST attend` → "Absensi Berhasil!" | ✅ | Diuji penuh di device: 2 m dari PT AMEERA MEKKAH (radius 5 m) → kartu hijau, "Konfirmasi", dialog "Lakukan Absensi", `POST attend` `{id, lat, lng}` (Double) → **201**, dialog "Absensi Berhasil!". Dialog konfirmasi tambahan tidak ada di legacy (lihat FLOW_QUESTIONS). |
| Izin lokasi presisi (FINE) saja, ditolak → alert + keluar | ✅* | Kode mengikuti layar Masuk Sekolah; jalur ditolak belum dijalankan di device. |
| Lokasi palsu ditolak | ✅ | Aturan diuji JVM (`attendStatus_waitsForAFixAndRefusesAMockOne`); tidak dicoba di device. |
| Tombol menunggu posisi (radius mati pun) | ✅ | Legacy menampilkan tombol sebelum ada posisi lalu gagal dengan "Gagal mendapatkan lokasi Anda". |
| Perusahaan tanpa koordinat + radius aktif → pesan, bukan "di luar area" | ✅ | Legacy mengukur ke 0,0. Diuji JVM. |
| Server yang menentukan: tanpa `can_attend` tampil teks `status`-nya, tanpa tombol | ✅ | Diuji JVM. |
| Setelah sukses: tutup dan segarkan shell | ✅ | Teramati: kartu hari ini berubah menjadi "Belum waktunya presensi pulang" tanpa tombol. Legacy membuka ulang `MagangPage` (`CLEAR_TOP`). |
| Tanpa unduh avatar untuk marker | ✅ | Marker pengguna biru seperti layar Masuk Sekolah. |
| Tanpa geofence, tanpa `ACCESS_BACKGROUND_LOCATION` | ✅ | Keputusan 2026-10-07 menggantikan Q1. |

## 4. Laporan harian + riwayat (doc §11.4–11.5) — tahap S4

| Item | Status | Keterangan |
|---|---|---|
| Form: lampiran gambar (Kamera/Galeri), catatan ≥ 100 karakter + penghitung (hijau/merah), "Submit Laporan", alert "Presensi Keluar", `POST leave` multipart `{id, daily_report, file}` | ✅ | Diuji di Samsung 2026-11-04: Kamera → foto → "Gambar terlampir"; catatan 31 karakter → tombol nonaktif; 134 karakter → "134 / min. 100 karakter" dan tombol aktif; "Submit Laporan" → "Presensi Keluar" → "Konfirmasi" → `POST leave` multipart (±416 KB) → **201** → dialog "Laporan tersimpan"; tab Laporan lalu menampilkan Keluar 10:58, lampiran, teks dan "Lihat Selengkapnya". **Catatan metode:** tombol "Laporan Harian" tidak tampil di build bersih karena `can_leave=false`; pengujian memakai build sementara (tidak di-commit) yang memaksa tombol tampil. Server **menerima** laporan walau `can_leave=false` — gerbang jendela pulang hanya ada di klien (`can_leave`). Galeri (picker dokumen Samsung tidak menerima ketukan terprogram) belum dicoba di device. |
| Foto > 5 MB **dikompres**; pesan batas "Foto lebih dari 5 MB" | ✅* | Legacy menolak dengan pesan "8MB". Foto kamera dikompres dan terkirim (±416 KB). Gambar > 5 MB (PNG derau 11,9 MB) tidak berhasil dipilih lewat picker Samsung, jadi jalur kompres besar belum teramati; fungsinya memakai `PhotoCapture.compress` yang sama dengan Poin. Gambar yang tak bisa masuk batas / tak terbaca → "Foto tidak dapat diproses atau lebih dari 5 MB." |
| "Laporan tersimpan" hanya bila sukses | ✅ | Dialog sukses muncul setelah server menerima (201); gagal = banner dan form tetap (legacy menampilkannya walau gagal). Jalur gagal belum teramati karena server menerima. |
| Mode baca dari daftar laporan (lampiran ketuk → penuh) | ✅ | Pratinjau lampiran (pinch-zoom, "Tutup") dan layar detail dari "Lihat Selengkapnya" (laporan 134 karakter) teramati di device. |
| Tab Laporan: "Semua"/"Pilih Tanggal" (rentang), kartu laporan, "Lihat Selengkapnya" bila > 121 karakter | ✅ | Diuji di Samsung: 2 laporan nyata terurut terbaru dulu; filter rentang 1–7 Nov menyisakan satu laporan; "Semua" mengembalikan. Waktu ditampilkan "04 Nov 2026, 10:13" (legacy: string mentah). |
| Bentuk respons `attendance` | ✅ | `id` angka, tanpa `record_type`, `check_in_status`/`check_out_status` "ontime", `file` URL penuh, `daily_report` bisa `null`, `company` tanpa `id`, `location` `{latitude, longitude}`. |

## 5. Izin magang (doc §11.6) — tahap S5

| Item | Status | Keterangan |
|---|---|---|
| Menu "Tambah Izin" di shell → daftar (jenis Semua/Sakit/Izin, navigasi bulan, "Ajukan Izin") | ✅ | Diuji di Samsung: daftar kosong "Belum ada Pengajuan Izin" (akun BUDI belum punya izin), tombol "Ajukan Izin". Digabung dari `attendance` (record `leave_request`) dan `schedule` (`leave_request` pada hari itu); item `attendance` menang. Tidak ada izin di data uji, jadi tampilan baris izin hanya diuji lewat preview. |
| Ajukan: banner, pemilih jadwal bila > 1, jenis, bukti gambar ≤ 2 MB, keterangan 10–5000, `POST leave-request` multipart | ✅* | Banner dan kunci form teramati di device: setelah presensi masuk server menurunkan `can_submit_leave_request` → "Tidak ada jadwal magang yang dapat diajukan izin hari ini." dan "Kirim" nonaktif. Dengan build sementara yang membuka kunci: "Kirim" kosong → "File bukti wajib diunggah", tanpa jenis → "Jenis pengajuan wajib dipilih", keterangan pendek → "Keterangan minimal 10 karakter" (urutan legacy, tiap pesan di bawah kolomnya). **Server menolak** pengiriman uji (tidak ada data tertulis): jadwal besok → 400 `{"error":"Izin/sakit hanya dapat diajukan pada hari ini"}`; jadwal hari ini yang sudah presensi → 400 `{"error":"Anda sudah melakukan presensi hari ini"}`. Jadi aturan "hanya hari ini, sebelum presensi" ditegakkan server dan klien mencerminkannya. **Jalur sukses `leave-request` belum teramati** (butuh siswa yang belum presensi hari itu). Pesan gagal tampil di atas tombol "Kirim" (bukan di atas form yang bisa tergulir). |
| Detail Pengajuan; "Hapus Pengajuan" tetap tersembunyi | ✅* | Seperti legacy; belum ada izin di data uji untuk dibuka. Bukti ditampilkan sebagai gambar (ketuk → pratinjau penuh) alih-alih membuka aplikasi lain. |

## 6. Dihapus / tidak dibangun
- Geofence, `GeofenceBroadcastReceiver`, notifikasi "GeofenceChannel2" (kanalnya tak pernah dibuat, tujuan notifikasinya bukan Activity), izin `ACCESS_BACKGROUND_LOCATION`.
- Cache Room (`magang_schedule`, `magang_company`, `magang_report`, `magang_leave_request`).
- Pengunduhan avatar untuk marker; kode mati legacy (`sp_filter`, `getCompany(scheduleId)`, `geocoder`, `determineLat/Lon`, `menu_save`).

## 7. Belum diverifikasi (jangan diasumsikan selesai)
1. **Pengajuan izin berhasil** (`POST leave-request` → 2xx), daftar dan detail dengan izin nyata: server menolak sesudah presensi masuk dan untuk hari selain hari ini (jalur gagal teruji). Perlu siswa dengan jadwal hari ini yang belum presensi (mis. BUDI besok, atau JAZA bila ada jadwal hari ini).
2. **Galeri** untuk lampiran laporan/izin dan **kompres gambar > 5 MB / > 2 MB** di device (picker dokumen Samsung tidak menerima ketukan terprogram; kamera sudah teruji).
3. Kapan `can_leave` bernilai true (server menerima laporan walau false, jadi jendela pulang hanya klien) dan kosakata `status` selain yang teramati ("Presensi Masuk", "Belum waktunya presensi masuk", "Waktu presensi masuk sudah lewat", "Belum waktunya presensi pulang", "Presensi hari ini selesai").
4. Apakah server menegakkan jarak/jam/ukuran file untuk `attend` (presensi dari luar radius tidak dicoba: app menyembunyikan tombolnya) dan isi pesan 4xx-nya; jalur gagal `leave` (server menerima).
5. Izin pada daftar/detail dengan data nyata (belum ada izin di akun uji); `AutoTimeGate` (jam ponsel uji otomatis) dan penolakan izin lokasi.
