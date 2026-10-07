# 05f — Implementasi Fase Pembelajaran: Poin + Agenda Mingguan

Dikerjakan 2026-10-06 (Fase 4 sisa, peta `01` §6). Spesifikasi: `05` §8–10. Status per item:
`05e-gap-poin-agenda.md`. Path relatif `app/src/main/java/id/diskola/app/`.

## 1. Arsitektur
Pola Tugas/Materi: `UI → ViewModel → Repository → Retrofit/Room`, satu ViewModel per layar.

| Berkas | Tanggung jawab |
|---|---|
| `apiservice/KonselingApiService.kt` (diketik ulang) | Param dari kode lama: list `take/skip/name`, search `nisn`/`name`, create multipart (part foto `violation_image`/`achievement_image`), calling-student JSON. Scaffold lama salah (`q`). |
| `dataclass/ResponData/PoinModels.kt` | DTO + entity `PoinItemTable` (master jenis, PK `(id, poin_type)`) + `PoinFormMode`. |
| `repository/PoinRepository.kt` | Master list (paging triplet), cari siswa, `checkTarget`, `isCooperativeOnly`, submit ×3 (melempar bila gagal), `getMyScore`. |
| `viewmodel/Poin{Siswa,Guru,Form}ViewModel.kt` | Siswa; alur guru (akses+cari+target terpilih, scope back-stack `PoinGuru`); satu VM form untuk 3 mode. |
| `ui/screens/poin/` | `PoinSiswaScreen`, `PoinCariScreen`/`PoinHasilScreen`, `PoinFormScreen`. |
| `apiservice/AgendaApiService.kt` + `AgendaModels.kt` | Diketik; `gate`/`window`/`summary`/`agendas` nullable-with-default. |
| `dataclass/localDb/{Poin,Agenda}Dao.kt` | Room v5→**6**: `poin_item`, `staff_agenda_day`, `staff_agenda_item`. |
| `repository/AgendaRepository.kt` | `readLocal`/`fetchDay` terpisah (pemanggil yang memutuskan fetch), `checkIn/Out`, `missingToday`. |
| `viewmodel/Agenda{Mingguan,Check,Detail}ViewModel.kt` | List; lapor (lokasi, submit); detail. |
| `utils/location/LocationProvider.kt` | Fused GPS + Geocoder. |
| `utils/PhotoCapture.kt` | Target kamera (FileProvider `cache-path`) + kompres EXIF-rotate ≤1 MB. |
| `ui/components/` | `AutoTimeGate` (diekstrak dari `AkmScreen`), `DateStrip`, `MonthYearPickerDialog`, `PagedPickerSheet`, `FormParts` (diangkat dari `UploadTugasScreen`). |
| `ui/screens/agenda/` | list, check (peta `maps-compose`), detail, `AgendaPolicySection`. |
| Hub | Tile Poin (3 peran) & Agenda aktif; badge Agenda (`CounterBadge(max=9)`). |
| Build | `maps-compose 6.4.1`; `MAPS_API_KEY` → `manifestPlaceholders` dari `local.properties`; izin lokasi. |
| `ResponseInterceptor.kt` | `ApiException.validationMessages` (pesan `errors` per field, distinct). |

## 2. Keputusan (user) — dicatat di `FLOW_QUESTIONS.md`
Cakupan Poin+Agenda sekaligus; alert sukses hanya bila API sukses; blokir target non-siswa/NISN kosong;
`AutoTimeGate` dipertahankan; alert gate hanya "Tutup"; pasang peta.

## 3. Temuan teknis
| # | Temuan | Penanganan |
|---|---|---|
| 1 | Scaffold search memakai `@Query("q")` | Diganti `name`/`nisn` (kode lama `ApiService.kt:1543`). |
| 2 | `check-account` dipanggil untuk siswa lain; pemanggil lain menulis sesi dari hasilnya | `PoinRepository` memakai `AuthRepository.checkAccount` langsung, tidak memanggil `writeCheckAccount`. |
| 3 | Skor list master = String, skor `score-student` = Int | Model terpisah; `@NullToEmptyString`. |
| 4 | Nama part foto: doc `api/06` "file" vs kode lama `violation_image`/`achievement_image` | Pakai kode lama; **belum diverifikasi ke server** (butuh tes kirim foto). |
| 5 | Respons `score-student` asli (akun siswa uji): rekap berisi tetapi `*_detail` kosong | Model/UI cocok; header+rekap tampil, daftar "Data Kosong". |

## 4. Pengujian di device (Samsung 1080×2340, build debug, `adb install -r -d`)
HP berisi app lama (`versionCode 79`) sehingga build baru ditimpa dengan `-d` atas persetujuan user;
sesi siswa `RIZKY` (SMK Demo Surabaya) tetap terbaca. Terverifikasi: Poin siswa (1× `score-student` 200,
tab Pelanggaran/Prestasi, dialog rekap dengan terjemahan nama, empty state, tanpa fetch saat ganti tab).
**Belum**: Poin guru, Agenda (butuh sesi guru), foto, peta, badge — lihat `05e`.
