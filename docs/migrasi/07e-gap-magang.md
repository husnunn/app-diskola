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

## 2. Shell, Jadwal (doc §11.1–11.2)

| Item | Status | Keterangan |
|---|---|---|
| Tile "Magang" Hub (siswa, terkunci bila `!is_having_class`, tanpa feature gate) → `Route.Magang` | ⏳ | Sekarang jatuh ke "Fitur dalam pengembangan". |
| Tab "Jadwal"/"Laporan", strip tanggal bulan berjalan, label bulan → `MonthYearPickerDialog`, ganti bulan → tanggal 1 | ⏳ | |
| Kartu jadwal: perusahaan, tanggal, "Pukul", status (`att_status`) bila tanpa tombol, "Laporan Harian" (`can_leave`), "Masuk" (`can_attend`) | ⏳ | Jam ditampilkan `HH:mm` (legacy memperlihatkan `HH:mm:ss` mentah). Dua tombol sekaligus ditampilkan berdampingan (legacy menumpuknya). |
| Skeleton, "Jadwal Tidak Tersedia", error + "Coba Lagi", pull-to-refresh | ⏳ | |
| `AutoTimeGate` tiap resume di tab Jadwal | ⏳ | |
| Menu "Panduan" (4 langkah) dan "Tambah Izin" | ⏳ | Panduan = bottom sheet statis (legacy: overlay bertahap; pref `onboarding_magang_filter_done` hanya ditulis, tidak pernah dibaca). |

## 3. Presensi masuk (doc §11.3)

| Item | Status | Keterangan |
|---|---|---|
| Peta perusahaan + lingkaran radius bila `radius_enabled`; kartu status; "Konfirmasi" → `POST attend` → "Absensi Berhasil!" | ⏳ | |
| Izin lokasi presisi (FINE) saja, ditolak → alert + keluar | ⏳ | **Tanpa geofence dan tanpa izin lokasi latar** (keputusan 2026-10-07 menggantikan Q1); legacy memintanya selalu. |
| Lokasi palsu ditolak | ⏳ | Fitur baru (Q5). |
| Setelah sukses: tutup dan segarkan shell | ⏳ | Legacy membuka ulang `MagangPage` dengan `CLEAR_TOP`. |
| Tanpa unduh avatar untuk marker | ⏳ | Legacy mengunduh tanpa try/catch (bisa crash). |

## 4. Laporan harian + riwayat (doc §11.4–11.5)

| Item | Status | Keterangan |
|---|---|---|
| Form: lampiran gambar (Kamera/Galeri), catatan ≥ 100 karakter + penghitung, "Submit Laporan", alert "Presensi Keluar", `POST leave` | ⏳ | Foto > 5 MB **dikompres** (legacy menolak dengan pesan "8MB"); pesan batas yang benar "Foto lebih dari 5 MB". |
| "Laporan tersimpan" hanya bila sukses | ⏳ | Legacy memunculkannya walau gagal dan mengabaikan hasil kirim. |
| Mode baca dari daftar laporan (lampiran ketuk → penuh) | ⏳ | |
| Tab Laporan: "Semua"/"Pilih Tanggal" (rentang), kartu laporan, "Lihat Selengkapnya" bila > 121 karakter | ⏳ | |

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
1. Bentuk asli respons `schedule`/`attendance` (nama field, `null` vs `""`, format `date`/`check_in_at`, tipe koordinat/radius) — dimodelkan dari legacy, lenient.
2. Apa yang membuat `can_attend` / `can_leave` / `can_submit_leave_request` bernilai true, dan kosakata `att_status`.
3. Apakah server menegakkan jarak/jam/ukuran file, dan isi pesan 4xx-nya.
4. Seluruh jalur bertanda ⏳/✅* di device (akun uji hanya punya jadwal 4 Juni 2026; tidak ada jadwal hari ini).
