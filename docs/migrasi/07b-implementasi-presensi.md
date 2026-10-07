# 07b — Implementasi Fase Presensi (harian)

Dikerjakan 2026-10-07. Spesifikasi: `07` §4–6, §10, §14.1. Status per item: `07a-gap-presensi.md`.
Path relatif `app/src/main/java/id/diskola/app/`.

## 1. Arsitektur
Pola Tugas/Poin/Agenda: `UI → ViewModel → Repository → Retrofit/Room`.

| Berkas | Tanggung jawab |
|---|---|
| `apiservice/PresensiApiService.kt` | Satu service typed untuk `student`/`staff` via `@Path("role")`: `by-month`, `by-year`, `check`, `check-in/out` (body `Map<String,String>`), offsite (multipart), leave-request, `check-feature-availability`. `IzinApiService` dihapus; placeholder presensi dihapus dari `AbsensiApiService` (service ini kemudian dihapus seluruhnya pada fase Jurnal, `07d`). |
| `dataclass/ResponData/PresensiModels.kt` | DTO lenient + entity `presensi_day`, `presensi_rekap`, `leave_request` (Room v6→7). |
| `repository/PresensiRepository.kt` | Data Absensi/Rekap (Room-first, tidak fetch sendiri), `check`, `checkIn/Out`, `isRadiusRestricted` (TTL 12 jam di prefs), `offsiteAvailability`, `offsiteSubmit`. |
| `repository/IzinRepository.kt`, `FeatureGateRepository.kt` | Izin (`refresh`, `loadPage`, `gate`, `submit`); feature gate fail-open. |
| `utils/PresensiRules.kt` (+ `PresensiRulesTest`, 20 tes) | Keputusan murni: status tombol Masuk, radius, validasi Dinas Luar & Izin, aturan izin. |
| `utils/location/{LocationProvider,MockLocationGuard}.kt` | Fix baru, update 10 s, setelan lokasi, jarak; penolakan lokasi palsu. |
| `utils/OffsiteWatermark.kt` | Watermark 3 baris, JPEG q90, EXIF, ≤2560 px. |
| `viewmodel/Presensi*ViewModel.kt`, `IzinViewModels.kt` | Shell+Absensi+Rekap; Masuk; Dinas Luar; Izin list/add/detail. |
| `ui/screens/presensi/` | `PresensiScreen`, `PresensiMasukScreen`, `PresensiOffsiteScreen`, `PresensiOffsiteCameraScreen` (CameraX), `IzinScreens`. |
| `ui/components/FeatureUnavailableSheet.kt` | Sheet "Fitur Belum Tersedia di Aplikasi". |
| Hub | Tile Presensi → feature gate → `Route.Presensi`; "Hadiri" → alur Jurnal (fase Jurnal, `07d`). |
| Build | CameraX 1.4.2; `CAMERA` + `uses-feature` opsional; `PORTAL_URL` (BuildConfig); **tanpa** `RECORD_AUDIO`/`ACCESS_BACKGROUND_LOCATION`. |

## 2. Keputusan (user) — `FLOW_QUESTIONS.md`
Presensi lengkap; CameraX; mock location saja; buang geofence & background location.

## 3. Temuan teknis
| # | Temuan | Penanganan |
|---|---|---|
| 1 | Layar Absensi lama memakai endpoint kelas-sesi yang mati di app lama | Diganti total. |
| 2 | `SessionStore.writeCheckAccount` fallback menimpa koordinat sekolah jadi 0.0 | `keepingGeoFrom(stored)`. |
| 3 | `AkmSettingData.absence_setting` default `false` → key hilang = tidak dibatasi | Jadi `Boolean?`; null → dibatasi. |
| 4 | Legacy mengulang dialog setelan lokasi setelah batal; avatar tanpa try/catch; foto offsite bisa tidak cocok dengan alamat; catatan dihitung mentah; `leave-request/today` hanya dari cache | Diperbaiki (lihat `07a`). |
| 5 | Data Absensi legacy `order by date`; versi awal memakai DESC (hari ini tersembunyi di bawah) | ASC + buka di baris hari ini. |
| 6 | `leave-request/today` terpanggil 2× saat membuka daftar izin | `refreshGate(includeToday=false)` setelah `refresh()`. |

## 4. Pengujian
- JVM: `PresensiRulesTest` — 20 tes lolos (`testDebugUnitTest`).
- Device (Samsung 1080×2340, sesi guru `Guru DEV`, SMK Demo Surabaya, `adb install -r -d`): feature gate (1×), Data Absensi (1× `staff/by-month`, buka di hari ini), Rekap (data asli), daftar & detail Izin (data asli), alert blokir Dinas Luar. **Tidak ada submit presensi/izin yang dikirim** (membuat data nyata di server dev).
- **Belum diuji**: Masuk/Pulang (server menolak hari ini: "School Calendar Event is not available for today"), Dinas Luar + kamera + watermark, form Izin kirim, mock location, sisi siswa.
