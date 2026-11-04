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
| Form: lampiran gambar (Kamera/Galeri), catatan ≥ 100 karakter + penghitung (hijau/merah), "Submit Laporan", alert "Presensi Keluar", `POST leave` multipart `{id, daily_report, file}` | ✅* | Kode selesai, kompilasi dan aturan teruji JVM. **Belum dibuka di device**: tombol "Laporan Harian" muncul hanya bila `can_leave`, dan itu belum pernah true di akun uji BUDI (status "Belum waktunya presensi pulang") — lihat §7. Kamera meminta izin CAMERA saat dipakai (manifest sudah mendeklarasikannya, jadi tanpa izin kamera sistem menolak dibuka). |
| Foto > 5 MB **dikompres**; pesan batas "Foto lebih dari 5 MB" | ✅* | Legacy menolak dengan pesan "8MB". Foto yang tak bisa masuk batas / tak terbaca → "Foto tidak dapat diproses atau lebih dari 5 MB." |
| "Laporan tersimpan" hanya bila sukses | ✅* | Dialog sukses muncul setelah server menerima; gagal = banner dan form tetap (legacy menampilkannya walau gagal). |
| Mode baca dari daftar laporan (lampiran ketuk → penuh) | ✅ | Pratinjau lampiran (pinch-zoom, "Tutup") teramati di device; layar detail "Lihat Selengkapnya" belum terlihat karena laporan uji ≤ 121 karakter (diuji lewat aturan JVM). |
| Tab Laporan: "Semua"/"Pilih Tanggal" (rentang), kartu laporan, "Lihat Selengkapnya" bila > 121 karakter | ✅ | Diuji di Samsung: 2 laporan nyata terurut terbaru dulu; filter rentang 1–7 Nov menyisakan satu laporan; "Semua" mengembalikan. Waktu ditampilkan "04 Nov 2026, 10:13" (legacy: string mentah). |
| Bentuk respons `attendance` | ✅ | `id` angka, tanpa `record_type`, `check_in_status`/`check_out_status` "ontime", `file` URL penuh, `daily_report` bisa `null`, `company` tanpa `id`, `location` `{latitude, longitude}`. |

## 5. Izin magang (doc §11.6)

| Item | Status | Keterangan |
|---|---|---|
| Daftar (jenis Semua/Sakit/Izin, navigasi bulan, "Ajukan Izin") | ⏳ | Digabung dari `attendance` (record `leave_request`) dan `schedule`; item `attendance` menang. |
| Ajukan: banner, pemilih jadwal bila > 1, jenis, bukti gambar ≤ 2 MB, keterangan 10–5000, `POST leave-request` | ⏳ | Gambar > 2 MB dikompres. |
| Detail Pengajuan; "Hapus Pengajuan" tetap tersembunyi | ⏳ | Seperti legacy. |

## 6. Dihapus / tidak dibangun
- Geofence, `GeofenceBroadcastReceiver`, notifikasi "GeofenceChannel2" (kanalnya tak pernah dibuat, tujuan notifikasinya bukan Activity), izin `ACCESS_BACKGROUND_LOCATION`.
- Cache Room (`magang_schedule`, `magang_company`, `magang_report`, `magang_leave_request`).
- Pengunduhan avatar untuk marker; kode mati legacy (`sp_filter`, `getCompany(scheduleId)`, `geocoder`, `determineLat/Lon`, `menu_save`).

## 7. Belum diverifikasi (jangan diasumsikan selesai)
1. Bentuk asli respons `attendance` (laporan dan izin: `null` vs `""`, format `check_in_at`, `location`). Respons `schedule` sudah terbukti dari akun BUDI (lihat FLOW_QUESTIONS 2026-11-04); `company.id` melewati `Int`.
2. Apa yang membuat `can_leave` bernilai true: sesudah presensi masuk status jadi "Belum waktunya presensi pulang" dengan `can_leave=false`; kapan jendela pulang terbuka (menurut jam `end_time`?) belum teramati.
3. Apakah server menegakkan jarak/jam/ukuran file, dan isi pesan 4xx-nya (presensi masuk dari luar radius tidak dicoba: app sengaja menyembunyikan tombolnya).
4. Jalur tulis laporan harian dan izin di device (S4/S5). Presensi masuk (`POST attend`) sudah dijalankan sekali di device.
