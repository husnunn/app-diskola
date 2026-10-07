# 07c — Selisih Implementasi Jurnal KBM vs Aplikasi Lama

Cakupan 2026-10-07: **Jurnal KBM** (doc `07` §7–9, §14.2–14.3, §16.3) — daftar jurnal + Hub, Verifikasi Jurnal &
Scan QR (siswa), Mulai kelas + form Isi Jurnal (guru & siswa), foto suasana KBM, Detail Jurnal. Magang tidak termasuk.
Laporan implementasi: `07d-implementasi-jurnal.md`. Path relatif `Diskola-App-New/app/src/main/java/id/diskola/app/`.

Legenda: ✅ selesai & teruji (JVM/compile) · ✅* kode selesai, **belum diuji di device** (HP uji terkunci layar saat
fase ini selesai; akun uji juga tidak punya data yang membuat jalur ini aktif) · ⛔ ditunda.

## 1. Daftar jurnal + Hub (doc §7.1)

| Item | Status | Keterangan |
|---|---|---|
| `GET mobile/attendance/schedule?date=` → baris per jam + sesi kelas; **tanpa cache Room** | ✅* | Cache legacy dikunci `plot_start_at` dan kosong saat offline. Daftar hanya "hari ini": load 1× di `init`, refresh 1× saat kembali dari layar anak (ON_RESUME pertama diabaikan), pull-to-refresh. |
| Baris sintetis "Jam Pelajaran - n" / "Kosong"; "Kelas belum dimulai" bila jam belum mulai | ✅ | `JurnalRules.buildRows`, teruji unit. |
| Kunci jam siswa (doc §19#13) | ✅ | **Aturan dipertahankan, indeks diperbaiki**: dihitung per jam pelajaran. Legacy menjalankan aturan di atas daftar yang sudah di-flatten sehingga bergeser begitu satu jam punya >1 kelas. |
| Tombol siswa: "Hadiri kelas" / "Isi Jurnal" / label "Anda Hadir Dikelas Ini" | ✅ | Teruji unit (`studentUi`). |
| Tombol guru: "Isi Jurnal" / "Detail" / "Mulai kelas" | ✅ | "Mulai kelas" **disembunyikan sebelum jamnya mulai** (legacy menampilkannya; server menolak). Status sesi non-kosong ikut tampil sebagai badge (legacy hanya "Kosong"). |
| Dialog "Pilih Metode Presensi" | ✅* | Dibuat sebagai bottom sheet. Aturan = `JurnalPage` (manual bila belum ada status; QR hanya bila belum hadir **dan** `mulai <= sekarang < selesai`), bukan aturan Hub yang berbeda. |
| Empty state "Belum ada jurnal hari ini" + "Muat ulang", error state + "Coba Lagi" | ✅* | |
| Tile "Jurnal" Hub (3 peran) → feature gate `jurnal-kbm` → `Route.Jurnal` | ✅* | Tombol "Hadiri"/"Isi Jurnal" kartu "Kelas Berlangsung" **hanya untuk siswa** (legacy; app baru sebelumnya menampilkannya ke semua peran) dan membuka alur baris itu setelah daftar termuat. |
| `AutoTimeGate` ("Atur tanggal & waktu Otomatis") | ✅* | Komponen bersama. |

## 2. Hadir kelas siswa (doc §7.2–7.3)

| Item | Status | Keterangan |
|---|---|---|
| "Masuk Kelas" / Verifikasi Jurnal: peta + radius dari `absence_setting`, tombol "Konfirmasi" hanya bila di dalam radius | ✅* | Gerbang (`VerifikasiGate`): memuat → menunggu lokasi → lokasi palsu → di luar radius → koordinat sekolah 0 → siap. Radius tidak dibatasi → tidak perlu izin lokasi. Izin presisi (FINE) wajib hanya bila radius dibatasi; ditolak → alert + keluar. Tanpa background location & geofence (keputusan fase Presensi). |
| Lokasi palsu | ✅* | Fitur baru: ditolak dengan "Lokasi palsu terdeteksi, presensi tidak bisa diproses.". |
| Dialog "Pilih status pembelajaran" (Terlaksana / Penugasan / Tidak Terlaksana) → `PUT student/journal-update` `{school_attendance_id, status}` | ✅* | Tanpa pilihan → pesan "Pilih status pembelajaran". Setelah sukses tampil dialog "Presensi Berhasil" (legacy langsung menutup). |
| Scan QR: Google code scanner (QR saja, tanpa izin kamera) → `parseQr` (split `_`, ≥12 bagian, indeks 2/4/6/8/10) → `POST student/learning/qr` | ✅* | Parser teruji unit. **Pemindaian fisik belum bisa diuji lewat adb.** QR tak valid sekarang diberi pesan "QR Code tidak valid untuk presensi kelas" (legacy: hanya log). Batal → toast "QR Code dibatalkan". |

## 3. Form Isi Jurnal (doc §8)

| Item | Status | Keterangan |
|---|---|---|
| Chip jam guru dari `list-plot`; pilih berurutan; "pilih secara urut" | ✅ | `toggleChip` teruji unit. **Deviasi:** pilihan ditolak tidak lagi menghapus semua chip (legacy menghapus). Gagal memuat daftar jam tidak crash (legacy crash): form tetap terbuka dengan pesan. |
| Chip jam siswa: satu jam terkunci | ✅* | Hanya jam dari baris yang dibuka (label dikirim lewat route). |
| Picker Kelas/Mapel/Guru: cari (debounce 500 ms), paging 20, server-side | ✅* | `PagedPickerSheet` + `PagedPicker` (state di memori, generasi untuk membatalkan respons basi, dedup per id). **Tidak** memakai tabel Room `class_journal_item`/`classroom`/`mapel` legacy. Label kelas `"<grade> - <name>"`. |
| Hint scope `assigned` | ✅* | "Hanya menampilkan kelas/mapel/jam dari jadwal Anda"; scope dibaca setiap form dibuka (fallback ke nilai tersimpan). |
| Validasi & pesan persis: foto wajib → "Foto suasana KBM wajib diambil"; guru "Data isian tidak lengkap"; siswa "Mohon isi keterangan terlebih dahulu" | ✅ | Tombol "Proses" tetap aktif; validasi saat kirim (spek). Teruji unit. |
| Kirim guru `POST mobile/attendance/journal` (multipart, `school_time_plot_id[]` distinct) | ✅* | Belum dikirim ke server (membuat data nyata). |
| Kirim siswa `POST student/journal-student`: `teacher_id` = **UUID** guru, `school_class_id` dari `SessionStore.student.classRoomId` (bukan pref login yang basi) | ✅* | lat/lng best-effort (timeout 4 s); lokasi palsu → koordinat **tidak dikirim**. |
| HTTP 307 "jurnal sudah ada" → dialog "Jurnal sudah ada" → "Buka" → detail | ✅* | **Risiko terbuka:** OkHttp mungkin mengikuti redirect sebelum interceptor; bila ya, 307 tidak pernah sampai dan yang tampil adalah pesan fallback. Parser payload (`attendance_id`, `subject_schedule_id_squence`/`_sequence`, angka Double) teruji unit. |
| Pesan error 422/400/404 (`journalErrorMessage`), 422 "validasi" + scope assigned → "Kombinasi kelas/mapel/jam tidak ada di jadwal Anda" | ✅ | Teruji unit. |

## 4. Foto suasana KBM (doc §8.3)

| Item | Status | Keterangan |
|---|---|---|
| Scope `journal-capture-scope` dibaca tiap form dibuka; tanda "*" bila wajib | ✅* | Fallback ke nilai tersimpan saat gagal. |
| Kamera CameraX belakang + tombol ganti kamera, watermark live (alamat/koordinat opsional + waktu), hanya kamera (tanpa galeri) | ✅* | `CaptureCameraScreen` dipakai bersama Dinas Luar (`CaptureCameraConfig.OffsiteSelfie` / `JurnalScene`). |
| ≤ 1600 px, JPEG q90 → turun 10 per langkah sampai ≤ 2 MB (minimum q40) | ✅* | `OffsiteWatermark.process(maxSide, maxBytes)`. |
| Lokasi + geocode best-effort (4 s + 4 s) tidak memblokir kamera | ✅* | Izin: CAMERA wajib ("Izin kamera diperlukan untuk mengambil foto"), lokasi opsional. |
| Preview, ketuk untuk melihat penuh, "Hapus"/"Ambil Ulang Foto" | ✅* | |

## 5. Detail Jurnal (doc §9)

| Item | Status | Keterangan |
|---|---|---|
| Guru: `schedule/{id}` + `journal/{scheduleId}?date=` (10 karakter pertama `created_at` bila `yyyy-MM-dd`); non-guru: read-only + fallback `list-student` | ✅* | |
| Info rows Pengajar/Kelas/Jadwal/Kehadiran ("H x · I x · S x · A x" dari server sampai ada edit, lalu hitung lokal) | ✅* | |
| Edit tujuan (dialog), status sesi wajib, H/I/S/A per siswa + sumber ("Absen mandiri" / "Dari izin" / "Ditetapkan guru"), editable bila guru ∧ `is_started` ∧ `scheduleId>0` ∧ `is_overridable` | ✅ (aturan) / ✅* (UI) | `canEditDetail`/`normalizeStudentStatus`/`statusSourceLabel` teruji unit. |
| "Penugasan" | ✅* | **Dipertahankan** (legacy menghapusnya di detail guru dan mengosongkan radio). |
| Non-guru: izin/sakit ditampilkan apa adanya | ✅* | Legacy menyatukan izin/sakit menjadi alpha. |
| Simpan `POST journal/save` (tujuan kosong → field dihilangkan); "Buang perubahan?"; "Jurnal Berhasil Disimpan"; "Jurnal gagal dimuat" + "Coba Lagi" | ✅* | Semua siswa dikirim, termasuk yang tidak bisa di-override (seperti legacy). |
| Setelah "Mulai kelas" sukses → dialog "Absensi Berhasil" → "Ok" → detail; daftar disegarkan | ✅* | Legacy lupa menyegarkan daftar. |

## 6. Dihapus / tidak dibangun
- `AbsensiApiService`, `AttendanceModels.kt`, `ScheduleDetailResponse.kt` (model legacy, bentuk salah), `ui/absensi/` — dihapus; semuanya digantikan `JurnalApiService`/`JurnalModels.kt`.
- Notifikasi/broadcast `SHOW_CONFIRM_BUTTON` legacy, geofence, background location — tidak dibangun.

## 7. Belum diverifikasi (jangan diasumsikan selesai)
1. Bentuk asli respons `schedule` untuk akun siswa/guru, nilai `status`, makna `is_present`, `late_at` (dimodelkan dari legacy, lenient).
2. Perilaku 307 di OkHttp (lihat §3).
3. Apakah server menegakkan "dalam jam plot" untuk QR.
4. Seluruh jalur pada tabel di atas bertanda ✅* di device (HP uji terkunci; jalur pembuat data nyata sengaja tidak dijalankan tanpa izin).
