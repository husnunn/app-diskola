# 05e — Selisih Implementasi Poin & Agenda Mingguan vs Aplikasi Lama

Status per fitur untuk cakupan yang dikerjakan 2026-10-06: **Poin siswa & guru** (doc `05` §9–10) dan
**Agenda Mingguan guru** (doc `05` §8). Laporan implementasi lengkap: `05f-implementasi-poin-agenda.md`.

Path relatif terhadap `Diskola-App-New/app/src/main/java/id/diskola/app/`.

Legenda: ✅ Selesai & diverifikasi di device · ✅* Selesai (kode benar, belum sempat diuji jalur ini)
· ⛔ Ditunda.

## 1. Poin — Siswa (doc §10)

| Item | Status | Keterangan |
|---|---|---|
| "Poin Saya": tab Pelanggaran/Prestasi dari **satu** panggilan `score-student` | ✅ | `PoinSiswaViewModel` (load di `init`, bukan `LaunchedEffect`) + `PoinSiswaScreen`. Device: tepat 1× `score-student`, ganti tab tanpa fetch ulang, JSON asli cocok dengan model. |
| Rekap semester: cincin skor, terjemahan `Late`/`Not Out`/`Not Attending Class` | ✅ | Dialog "Detail Pelanggaran Semester" diuji dengan data nyata (skor 15, 5 baris rekap). Dialog Prestasi: cincin hijau, nama apa adanya. |
| Daftar kejadian (nama, "Jumlah Poin", "Dibuat oleh", pesan, "Lihat Dokumentasi") | ✅* | Dibangun; akun uji siswa punya rekap tapi `violation_detail`/`achievement_detail` **kosong**, jadi kartu & dialog gambar belum pernah tampil dengan data nyata. Empty state "Data Kosong" teruji. |
| Tombol pemanggilan + dialog "Detail Penanganan Siswa" | ✅* | Dibangun; `notHandled` kosong di akun uji. |
| Cek tanggal/waktu "Otomatis" (keputusan: pertahankan) | ✅* | `AutoTimeGate` bersama (diekstrak dari `AkmScreen`, yang ikut dipakai ulang). Belum dipicu (waktu HP otomatis). |
| Tanpa cache Room untuk poin siswa (deviasi dari legacy) | ✅ | Legacy: PK `(id, poin_type)` bisa menimpa kejadian sama jenis + `deleteAll` lintas tab tiap fetch. Baru: state di ViewModel, urut `date+time` desc, kunci per posisi. Skor null → 0 (legacy menampilkan "null"). |

## 2. Poin — Guru (doc §9)

| Item | Status | Keterangan |
|---|---|---|
| Gerbang akses Cooperative-only ("Akses Ditolak"), gagal API = izinkan | ✅* | `PoinRepository.isCooperativeOnly()`; belum diuji (butuh akun guru). |
| Cari siswa ≥3 karakter, debounce 300 ms, `name` + `nisn` (bila digit), dedupe | ✅* | `PoinGuruViewModel.search`. Scaffold lama mengirim `q` — diperbaiki jadi `name`/`nisn` dari kode lama. |
| Blokir target non-Student / NISN kosong (keputusan user) | ✅* | `PoinRepository.checkTarget()`; tidak pernah memanggil `writeCheckAccount` (tidak menimpa sesi guru). |
| Layar Hasil + 3 aksi, skor di-refresh setelah kirim | ✅* | `PoinHasilScreen`, `PoinGuruViewModel.refreshSelectedScores()`. |
| Form pelanggaran/prestasi: jenis (picker cari+paging), keterangan, foto opsional ≤1 MB | ✅* | `PoinFormScreen`/`PoinFormViewModel`/`PagedPickerSheet`/`PhotoCapture`. Foto yang tak bisa ≤1 MB ditolak dengan pesan (legacy mengirim tanpa foto diam-diam). |
| Form pemanggilan: tanggal (≥ hari ini) + waktu 24 jam | ✅* | `DatePickerDialog` + `TimePicker`; `calling_at` = `yyyy-MM-dd HH:mm:ss`. |
| Alert sukses hanya bila API sukses (keputusan user, ketiga form) | ✅* | Repository melempar exception; VM hanya set `done` setelah sukses. |
| `user_id` (check-account `data.id`) ≠ `score_student_id` (id hasil cari) | ✅* | Dipisah di `PoinSelected`. |

## 3. Agenda Mingguan — Guru (doc §8)

| Item | Status | Keterangan |
|---|---|---|
| Strip tanggal sebulan, label bulan + picker bulan/tahun, ringkasan "x / y sesi" | ✅* | `DateStrip`, `MonthYearPickerDialog`, `AgendaMingguanScreen`. |
| Status chip & routing tap (hari ini, `gate.in`) | ✅* | `AgendaUiStatus` (INFO tak dibuat), `AgendaMingguanViewModel.resolveTap()`. |
| Alert gate: hanya "Tutup" (keputusan user; Presensi guru belum ada) | ✅* | Tombol "Presensi" legacy sengaja tidak dibuat. |
| Cache Room per tanggal; 1× fetch saat buka | ✅* | `AgendaRepository` (tidak pernah fetch sendiri); `init` memuat sekali, ON_RESUME pertama diabaikan. Legacy: 2× saat buka. |
| Lapor masuk/pulang: izin lokasi, GPS, peta, alamat, POST `{agenda_id,lat,lng}` | ✅* | `AgendaCheckScreen`/`LocationProvider`. Deviasi disengaja: submit disabled sampai ada fix GPS; izin fine **atau** coarse cukup; Geocoder API 33 punya `onError`; jam tampil berjalan. |
| Pesan error `errors` → `message` → fallback | ✅* | `ApiException.validationMessages` (baru, diisi `ResponseInterceptor`) dipakai `AgendaRepository`. |
| Kebijakan collapsible dengan teks persis | ✅* | `AgendaPolicySection`, hormati animator scale 0. |
| Detail Agenda read-only | ✅* | `AgendaDetailScreen`. |
| Badge Agenda di Hub (`summary.missing`, "9+") | ✅* | `HomeViewModel.refresh()` → `AgendaRepository.missingToday()`; `CounterBadge(max=9)`. |

**Belum diuji sama sekali di device**: seluruh sisi guru Poin dan seluruh Agenda (HP uji berisi sesi
siswa; akun guru tersedia tapi menukar sesi di HP itu butuh persetujuan pemilik HP). Peta butuh
`MAPS_API_KEY` valid di `local.properties` (key terpasang lewat manifest placeholder; belum dilihat
berfungsi).
