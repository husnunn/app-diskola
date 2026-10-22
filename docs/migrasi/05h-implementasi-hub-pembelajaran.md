# 05h — Implementasi: Samakan Hub Pembelajaran dengan `PembelajaranPage` Lama

Dikerjakan 2026-10-12 atas permintaan user ("samakan data yang tampil, endpoint yang dipanggil… perbedaan hanya di UI; pastikan teks panjang tertangani").
Status per item: `05g-gap-hub-pembelajaran.md`. Path relatif `app/src/main/java/id/diskola/app/`.

## 1. Keputusan (user) — `FLOW_QUESTIONS.md`
Lonceng = badge + penguncian Klaspay seperti lama; kartu kelas = persis lama (`current_subject`, tanpa `schedule`); frekuensi muat = seperti lama + pengaman.

## 2. Arsitektur
| Berkas | Tanggung jawab |
|---|---|
| `dataclass/ResponData/HubModels.kt` | DTO lenient: `CurrentSubjectData`, `UserProgressData`, `CurrentUserResponse`, `KlaspayWalletResponse`, `NotificationSummaryResponse`, `TodaySettingResponse`. `CheckAccountApiResponse` +`current_subject`, `userProgress`; `SchoolDetail` +`city_name` |
| `apiservice/{Auth,Klaspay,Notifikasi,Presensi}ApiService.kt` | placeholder `Map` diganti endpoint bertipe (`currentUser`, `getWallet`, `getNotificationSummary(wallet_id)`, `settingToday`); `NotifikasiApiService` kini disediakan di `ApiModule` |
| `repository/HubNotificationRepository.kt`, `AuthRepository.currentUser`, `PresensiRepository.settingToday` | wallet→summary (hanya Klaspay aktif, gagal wallet = tidak berubah, gagal summary = 0); session guard; setelan hari ini |
| `utils/HubRules.kt` (+ `HubRulesTest`, 21 tes) | `verifyState`, `classAction/className/classTime`, `classLine`, `bellBadge`, `deviceMismatch`, `attendanceBanner` (+ `workHours/eventName/sourceLabel`) — teks legacy persis |
| `viewmodel/HomeViewModel.kt` | satu `StateFlow<HomeUiState>`; `onResumed()` (dilewati bila <30 dtk setelah muat sukses) dan `refresh()` (selalu); flag `inFlight` di-set sebelum `launch`; urutan check-account → paralel (session guard, lonceng, lencana agenda guru, banner non-siswa) |
| `ui/screens/home/Home{Screen,Header,ClassCard,AttendanceBanner,MenuGrid,Previews}.kt` | layar + dialog / header / kartu siswa + kartu verifikasi / banner / grid / pratinjau teks panjang |
| `ui/components/ExpandableText.kt`, `AppDialog.kt`, `FeatureUnavailableSheet.kt`; `ui/screens/jurnal/JurnalMethodSheet.kt` | teks panjang (ketuk untuk selengkapnya, dialog/sheet bisa di-scroll); sheet "Pilih Metode Presensi" dipakai Hub dan layar Jurnal |
| `ui/navigation/{Route,AppNavHost,MainTabsScreen}.kt` | `Route.Jurnal` tanpa argumen pintasan (replay `initialAction` dihapus); `Route.Auth.KlaspayActivation(returnToMain)`; callback `onOpenKlaspayActivation` |

## 3. Temuan teknis
| # | Temuan | Penanganan |
|---|---|---|
| 1 | Legacy Home **tidak** memanggil `schedule?date=`; kartu kelas = `check-account.current_subject` | Panggilan dihapus dari Home |
| 2 | Guru di legacy tidak melihat kartu kelas, tetapi banner presensi | Banner `AttendanceBannerCard` |
| 3 | `userProgress` & `current_subject` adalah **saudara** `data` di respons check-account (bukan di dalamnya) | Dimodelkan di `CheckAccountApiResponse` |
| 4 | `has_student_class` legacy = `student_class.id > 0` = `is_having_class` untuk siswa | Satu sumber (`isHavingClass`) |
| 5 | Banner legacy gagal-total tampil "Jadwal absensi belum diatur" (menyesatkan) | Banner dibiarkan bila `setting/me/today` gagal |
| 6 | Hero tinggi tetap 196 dp + overlap 56 dp menimpa teks pada font besar | `heightIn(min)` + padding bawah = `HeroOverlap` |

## 4. Pengujian
- JVM: `HubRulesTest` 21, `HubModelsParseTest` 9 (Moshi: `current_subject`/`userProgress`/null/teks 8 rb karakter/wallet string-atau-angka/today setting) + 51 tes lama = **81 lolos**.
- `./gradlew :app:compileDebugKotlin`, `:app:assembleDebug` bersih.
- **Device (2026-10-12, sesi siswa):** log tab Pembelajaran = check-account, current-user, payment/wallet, notification/summary, **tanpa** `schedule`; header/kartu/tile/bell tampil sesuai (lihat `05g`). Sisi guru, "Check In"/"Isi Jurnal",
  verifikasi in_review/rejected, badge > 0 dan "Perangkat Lain Terdeteksi" belum teruji. Daftar cek semula: log tab Pembelajaran (check-account, current-user, setting/me/today, staff/check, staff/agendas/today; wallet+summary bila Klaspay aktif;
  **tanpa** `schedule`), buka-tutup <30 dtk tidak memuat ulang, refresh sekali, header/banner/lonceng/tile, font scale 1,3 dan 2,0. Sisi siswa (kartu kelas, "Check In"/"Isi Jurnal", verifikasi in_review/rejected) dan
  "Perangkat Lain Terdeteksi" butuh akun siswa / perangkat kedua.
