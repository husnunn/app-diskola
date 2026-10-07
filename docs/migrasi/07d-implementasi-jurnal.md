# 07d — Implementasi Fase Jurnal KBM

Dikerjakan 2026-10-07. Spesifikasi: `07` §7–9, §14.2–14.3, §16.3. Status per item: `07c-gap-jurnal.md`.
Path relatif `app/src/main/java/id/diskola/app/`.

## 1. Arsitektur
Pola Presensi/Poin/Agenda: `UI → ViewModel → Repository → Retrofit` (tanpa Room untuk Jurnal).

| Berkas | Tanggung jawab |
|---|---|
| `apiservice/JurnalApiService.kt` | Typed: `schedule`, `teacher-data-scope`, `journal-capture-scope`, `list-plot`, `list-class/subject/teacher` (`take/skip/name`), multipart `createJournal` & `createStudentJournal`, `check-in` (guru), `journal-update` (siswa), `learning/qr`, `schedule/{id}`, `list-student`, `journal/{id}`, `journal/save`. |
| `dataclass/ResponData/JurnalModels.kt` | DTO lenient (semua field nullable/default; `late_at: String?`, `is_present: Boolean?`). Model/entity jadwal legacy dihapus. |
| `utils/JurnalRules.kt` (+ `JurnalRulesTest`, 20 tes) | Logika murni: baris sintetis & kunci siswa per jam, visibilitas tombol, opsi metode, `parseQr`, validasi form, urutan chip, `journalErrorMessage`, payload 307, aturan edit detail. |
| `repository/JurnalRepository.kt` | Seluruh akses data; `translate{}` mengubah `ApiException` menjadi pesan §14.3 atau `JournalExistsException` (307). Scope & `required` disimpan di `PreferenceClass` (`teacher_master_data_scope`, `journal_capture_required`). |
| `utils/QrScanner.kt` | Google code scanner (`play-services-code-scanner:16.1.0`), QR saja. |
| `viewmodel/JurnalViewModel.kt` | Daftar + "Mulai kelas" (lokasi best-effort 4 s, tolak lokasi palsu) + QR; `busy` untuk dialog blokir. |
| `viewmodel/VerifikasiJurnalViewModel.kt` | Gerbang radius + lokasi + lokasi palsu (`PresensiRepository.isRadiusRestricted`), kirim `journal-update`. |
| `viewmodel/JurnalFormViewModel.kt` | Form guru/siswa, chip, tiga `PagedPicker`, foto, `prepareCapture`, submit + hasil (`Created`/`Exists`). |
| `viewmodel/JurnalDetailViewModel.kt` | Muat guru/non-guru, edit & `dirty`, simpan/buang. |
| `ui/screens/jurnal/` | `JurnalScreen`, `VerifikasiJurnalScreen`, `JurnalFormScreen`, `JurnalDetailScreen`, `JurnalParts` (radio 48 dp). |
| `ui/screens/presensi/CaptureCameraScreen.kt` | Kamera bersama (Dinas Luar + KBM) dengan `CaptureCameraConfig`. |
| Lainnya | `OffsiteWatermark` diparametrisasi (`maxSide`, `maxBytes`, baris opsional); `AppTextField(minLines)`; route `Jurnal/VerifikasiJurnal/JurnalForm/JurnalCapture/JurnalDetail` (lat/lng **String**, bukan Double); `HomeViewModel`/`HomeScreen` memakai `JurnalRepository`. |

## 2. Keputusan (user) — `FLOW_QUESTIONS.md`
Jurnal lengkap; Google code scanner; kunci jam siswa dipertahankan dengan indeks diperbaiki.

## 3. Temuan teknis
| # | Temuan | Penanganan |
|---|---|---|
| 1 | Model jadwal legacy mengikuti dokumen backend lama, bentuk salah (respons asli bersarang `data[]{…school_attendances[]}`) | Diganti DTO lenient baru; model lama dihapus (nama kelas ganda juga memicu error KSP). |
| 2 | Kunci jam siswa legacy bergeser pada daftar yang sudah di-flatten | Dihitung per jam. |
| 3 | `class_id` di pref login bisa basi | `SessionStore.student.classRoomId`. |
| 4 | Chip tak berurutan menghapus semua pilihan; daftar jam gagal → crash | Tolak tanpa menghapus; form tetap terbuka. |
| 5 | Argumen route `Double` rawan | `String`. |
| 6 | Picker legacy memakai tabel Room `class_journal_item` + boundary | Paging server langsung, state di memori, respons basi dibatalkan dengan penghitung generasi. |

## 4. Pengujian
- JVM: `JurnalRulesTest` 20 tes + `PresensiRulesTest` 20 tes lolos (`testDebugUnitTest`).
- `./gradlew :app:compileDebugKotlin` dan `:app:assembleDebug` bersih; APK terpasang ke HP uji (`adb install -r -d`).
- **Device: belum diuji.** Layar HP terkunci (keyguard) saat fase selesai, sehingga daftar jadwal, picker, dialog metode, kartu Hub per peran, dan layar lain belum dilihat/diverifikasi. Aksi pembuat data nyata (Mulai kelas, kirim jurnal, simpan detail, QR, journal-update) **tidak** dijalankan.
- Yang perlu diuji ulang di device begitu HP dibuka: `schedule` 1× saat buka (tanpa fetch ganda), bentuk respons asli, picker kelas/mapel/guru (paging + cari), dialog metode, kartu Hub siswa vs guru, tampilan Detail.
