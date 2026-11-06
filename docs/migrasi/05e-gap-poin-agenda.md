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
| Strip tanggal sebulan, label bulan + picker bulan/tahun, ringkasan "x / y sesi" | ✅ | `DateStrip`, `MonthYearPickerDialog`, `AgendaMingguanScreen`. **uji device 2026-11-06: strip, label bulan, picker, ringkasan "0 / 7 sesi".** |
| Status chip & routing tap (hari ini, `gate.in`) | ✅* | `AgendaUiStatus` (INFO tak dibuat), `AgendaMingguanViewModel.resolveTap()`. **uji device 2026-11-06: chip "Belum"; ketuk "Absen Hadir" saat `gate.in` null membuka dialog gate; status lain belum bisa dicapai.** |
| Alert gate: tombol "Presensi" dan "Tutup" (seperti legacy; koreksi 2026-11-04 — catatan lama "hanya Tutup" basi sejak tombol Presensi dibuat) | ✅ | "Presensi" membuka layar Presensi langsung; legacy melewati pemeriksaan ketersediaan fitur dulu (belum ditiru, lihat FLOW_QUESTIONS 2026-11-04 audit). **uji device 2026-11-06: "Presensi" memanggil `check-feature-availability` lalu membuka Presensi.** |
| Cache Room per tanggal; 1× fetch saat buka | ✅ | `AgendaRepository` (tidak pernah fetch sendiri); `init` memuat sekali, ON_RESUME pertama diabaikan. Legacy: 2× saat buka. **uji device 2026-11-06: 1 fetch saat buka, 1 per tanggal baru, 0 untuk tanggal yang sudah dibuka.** |
| Lapor masuk/pulang: izin lokasi, GPS, peta, alamat, POST `{agenda_id,lat,lng}` | ✅* | `AgendaCheckScreen`/`LocationProvider`. Deviasi disengaja: submit disabled sampai ada fix GPS; izin fine **atau** coarse cukup; Geocoder API 33 punya `onError`; jam tampil berjalan. **uji device 2026-11-06: belum teruji, layar butuh `gate.in` terisi.** |
| Pesan error `errors` → `message` → fallback | ✅* | `ApiException.validationMessages` (baru, diisi `ResponseInterceptor`) dipakai `AgendaRepository`. |
| Kebijakan collapsible dengan teks persis | ✅ | `AgendaPolicySection`, hormati animator scale 0. **uji device 2026-11-06: tiga varian teks.** |
| Detail Agenda read-only | ✅ | `AgendaDetailScreen`. **uji device 2026-11-06.** |
| Badge Agenda di Hub (`summary.missing`, "9+") | ✅ | `HomeViewModel.refresh()` → `AgendaRepository.missingToday()`; `CounterBadge(max=9)`. **uji device 2026-11-06: 7 = `summary.missing`; sempat tampil nilai lama selama refresh.** |

**Pembaruan 2026-11-05 — Poin diuji dua peran (Infinix guru "Guru DEV", Samsung siswa HUSNI, server dev):**
- **Teruji di device:** cari siswa (nama; hasil dari `konseling/score?name=`), layar Hasil (skor 0/0 lalu angka naik sesudah kirim), picker jenis (3 form), validasi form kosong ("Mohon isi keterangan terlebih dahulu", tanpa request; pesan dan urutan sama dengan legacy), ketiga kirim (`create/achievement` 200, `create/violation` 200, `create/calling-student` 201, body `score_student_id`, `school_handling_id`, `calling_at` "yyyy-MM-dd HH:mm:ss"), dialog sukses sesuai legacy, **tutup seluruh alur ke Hub** sesudah "Baik".
- **Sisi siswa dengan data nyata:** Prestasi (cincin 2, kartu "Jumlah Poin/Dibuat oleh/keterangan", dialog "Detail Prestasi Semester"), Pelanggaran (cincin merah 12, kartu, dialog "Detail Pelanggaran Semester" dengan terjemahan label), tombol pemanggilan dan "Detail Penanganan Siswa".
- **Belum teruji:** lampiran foto di form, gerbang "Akses Ditolak" untuk peran Cooperative-only, blokir target non-siswa, `AutoTimeGate`, "Lihat Dokumentasi" (butuh kejadian berfoto).
- Dirapikan sesudahnya (teruji di Samsung): kartu kejadian siswa punya baris "Tanggal" ("05 Nov 2026, 10:10"), jadwal pemanggilan tampil "06 Nov 2026, 10:10" (legacy: teks mentah server `2026-11-06 10:10:00`, dan `date || time` di dialog foto, kini juga diformat). Bentuk nyata dari server: `date` "2026-11-05", `time` "10:10:08", `calling_at` "2026-11-06 10:10:00"; nilai yang tak terbaca ditampilkan apa adanya. `PoinDateRules` (6 tes).
- **Agenda:** daftar dan alert gate "Presensi" sudah dibuka di Infinix sesi guru (2026-11-04, lewat pemeriksaan ketersediaan fitur); lapor masuk/pulang, detail, dan peta belum diuji. Peta butuh `MAPS_API_KEY` valid di `local.properties` (key terpasang lewat manifest placeholder; belum dilihat berfungsi).
