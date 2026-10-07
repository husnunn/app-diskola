# 07a — Selisih Implementasi Presensi (harian) vs Aplikasi Lama

Status per fitur untuk cakupan 2026-10-07: **Presensi harian** (doc `07` §4–6, §10, §14.1) — Data Absensi,
Rekap, Masuk/Pulang sekolah, Dinas Luar (guru), Izin/Sakit. **Jurnal KBM dan Magang bukan bagian fase ini.**
Laporan implementasi: `07b-implementasi-presensi.md`. Path relatif `Diskola-App-New/app/src/main/java/id/diskola/app/`.

Legenda: ✅ selesai & diuji di device · ✅* selesai (kode benar, jalur ini belum bisa diuji) · ⛔ ditunda.

## 1. Shell, Data Absensi, Rekap

| Item | Status | Keterangan |
|---|---|---|
| Layar lama "Presensi Siswa" diganti | ✅ | Layar lama memakai endpoint kelas-sesi (`start/attend/leave/data/summary`) yang di app lama kode mati. `AbsensiScreen`/`AbsensiViewModel`/model & endpoint mati dihapus. |
| Feature gate `check-feature-availability` (fail-open) | ✅ | Diuji: tepat 1× saat tile dibuka (legacy 2×: tile + onCreate). Sheet "Fitur Belum Tersedia" (siswa/guru + "Buka Website") ✅* (server uji menjawab available). |
| Data Absensi: cache Room → 1× fetch `by-month`, bulan sebelum/berikut, picker bulan | ✅ | Diuji: 1× `staff/by-month`, JSON asli cocok, daftar berurut tanggal dan terbuka di baris hari ini. |
| Warna merah (terlambat / pulang awal / kosong), "Libur", "Sakit"/"Izin" bila izin disetujui, ikon detail dinas luar + sheet foto | ✅* | Dibangun; akun uji belum punya data hadir/libur/offsite sehingga hanya "-" merah yang terlihat. |
| Tombol "Lakukan Presensi" (disabled bila izin hari ini pending/approved) | ✅ | Diuji: guru dengan `offsite_enabled` tapi `allow=false` mendapat alert pesan server ("School Calendar Event is not available for today") — perilaku persis legacy §4.2. |
| Chooser "Pilih Jenis Presensi" (guru, `offsite_enabled && allow`) | ✅* | Belum terpicu (server uji `allow=false`). |
| Rekap: cache-first, filter tahun dinonaktifkan saat memuat + cooldown 5 s, T merah >0 | ✅ | Diuji dengan data asli (`month` = nama bulan String, `year` = Int). |
| Dialog "Atur tanggal & waktu Otomatis" (`AutoTimeGate`) | ✅* | Komponen bersama; tidak terpicu (waktu HP otomatis). |
| "Panduan" (3 langkah) | ✅* | Dibuat sebagai bottom sheet statis (legacy: coach-mark berlapis). |
| Refresh setelah kembali dari layar anak: 1× | ✅ | ON_RESUME pertama diabaikan (legacy: 2× saat buka). |

## 2. Masuk / Pulang sekolah (doc §5)

| Item | Status | Keterangan |
|---|---|---|
| `check` (siswa/guru) + peta + lingkaran radius + jam berdetak + info jadwal | ✅* | Tidak bisa dibuka dengan akun uji guru hari ini (server menolak: tak ada agenda kalender) dan akun siswa tidak aktif di HP. |
| Aturan radius dari `absence_setting` (TTL 12 jam, key hilang/error → **dibatasi**), jarak `<=` radius | ✅* | Logika murni teruji unit (`PresensiRulesTest`, 20 tes). Deviasi: legacy memperlakukan key hilang sebagai tidak dibatasi. |
| Izin lokasi: **presisi (FINE) wajib**, tanpa background location & tanpa geofence | ✅* | Keputusan user (mengubah keputusan 30-09 §19#1). "Approximate saja" dianggap ditolak. |
| Dialog "Turn on location" — satu kali, tanpa loop | ✅* | Legacy mengulang dialog setelah "batal"; sekarang tombol "Aktifkan lokasi". |
| **Deteksi lokasi palsu** (`Location.isMock`/`isFromMockProvider`) | ✅* | Fitur baru (keputusan 30-09). Submit & status tombol menolak dengan "Lokasi palsu terdeteksi, presensi tidak bisa diproses." Root tidak dideteksi (keputusan user). Belum diuji di device. |
| Body `{lat,lng}` String; dialog konfirmasi & sukses (teks persis) | ✅* | Gagal menampilkan alasan server dan tombol tetap ada (legacy menelan alasan & menyembunyikan tombol). |
| Alert koordinat sekolah 0 | ✅* | Hanya bila radius dibatasi (legacy: juga saat `absence_setting=false`). |
| Avatar marker | ⛔ | Marker default; unduhan avatar tanpa try/catch legacy (crash) tidak ditiru. |
| Perbaikan `SessionStore.writeCheckAccount` | ✅ | Fallback `SchoolItem` tanpa koordinat sebelumnya menimpa koordinat sekolah jadi 0.0/"50". |

## 3. Dinas Luar (guru) + selfie (doc §6)

| Item | Status | Keterangan |
|---|---|---|
| `offsite/check` → chooser / alert blokir (pesan "Workgroup" dipetakan) | ✅ | Alert blokir teruji. |
| Form: alamat (auto-geocode), keterangan 10–500 (dihitung setelah trim), foto wajib bila `require_photo` | ✅* | Aturan murni teruji unit. |
| Kamera depan CameraX + watermark live + `OffsiteWatermark` (3 baris, JPEG q90, tidak mirror, ≤2560 px) | ✅* | Belum diuji di device. Tanpa izin mikrofon. |
| Foto basi bila alamat diubah setelah foto | ✅* | Diminta ambil ulang (legacy: foto & teks watermark bisa tidak cocok). |
| Submit: fix baru, mock-guard, `code==0` = gagal, 403 menonaktifkan | ✅* | Key `code` hilang dianggap sukses bila `data.status` ada — **belum diverifikasi ke server**. |

## 4. Izin / sakit (doc §10)

| Item | Status | Keterangan |
|---|---|---|
| Daftar: filter status, Room-first, load-more, pull-to-refresh | ✅ | Diuji dengan data asli (5+ pengajuan "Ditolak"). Sinkron `today` + `page=1` tepat 1× tiap. |
| Detail (alasan penolakan, file bukti, "Direview") | ✅ | Diuji. Buka file hanya untuk URL http(s). |
| Form: radio tanpa preselect, PDF/JPG/PNG ≤2 MB, keterangan 10–500, urutan validasi file→jenis→keterangan | ✅* | Belum dikirim ke server (membuat data nyata). MIME tak dikenal ditolak (legacy melabeli jpeg). |
| Aturan "boleh ajukan" memakai data segar | ✅ | `by-month` + `today` disegarkan dulu (legacy hanya memakai cache Absensi). |

## 5. Integrasi
- Alert gate Agenda Mingguan ("Silakan presensi masuk…") kini punya tombol "Presensi" → layar Presensi baru. ✅*
- Tombol "Hadiri" pada kartu Kelas Berlangsung (Hub): sejak fase Jurnal membuka alur Jurnal (`07c`). Banner absensi Hub (`setting/me/today`) tetap ditunda.
