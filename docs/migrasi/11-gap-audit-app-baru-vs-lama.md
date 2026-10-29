# 11 — Audit Gap: `Diskola-App-New` (Compose) vs `android-portal` (XML), fokus Pembelajaran

Daftar selisih yang **masih ada** antara aplikasi baru dan aplikasi lama per 2026-10-28, dibuat dengan membaca kode kedua repo
(bukan dari ringkasan dokumen `05`–`08`, yang dipakai hanya sebagai petunjuk dan sebagian sudah basi, lihat §7).
Bagian terbesar membahas fitur **Pembelajaran** (Hub, Materi, Tugas, Agenda Mingguan, Poin). Modul lain dibahas lebih ringkas di §5.

| | |
|---|---|
| Aplikasi lama | `android-portal` 2.1.42 (versionCode 82), HEAD `91c0116c` (2026-10-24) |
| Aplikasi baru | `Diskola-App-New` 2.2.84 (versionCode 83), HEAD `5d0f8cd` + perubahan belum di-commit (alur Bayar SPP, Room v11, `AppDialog`, dll.) dibaca apa adanya |
| Metode | 5 pemeriksaan kode paralel (Hub/Agenda/Poin, Materi, Tugas, Auth/Akun/Notifikasi/Infra, Presensi/Jurnal/AKM/Pembayaran). Tiap klaim diperiksa ke kode di kedua repo, termasuk diff commit lama setelah baseline dokumen |
| Verifikasi tambahan | Klaim P0 berikut saya cek ulang langsung di kode baru: `loadMore()` guru tidak dipanggil layar mana pun; tidak ada `StaffMenuAccess`/`role_names`/`is_staff_limited`; `CheckAccountUserData.id` bertipe `Int` tanpa `@NullToZeroInt`; semua `setContentIntent` di `NotifService` dikomentari; `app_name` = "Education"; `WEB_CLIENT_ID` dikomentari di `local.properties`; `NotifikasiViewModel` memakai `MockNotifications.seed`; tombol di layar Akun `onClick = {}` |
| Batasan | **Tidak ada uji di perangkat, tidak ada build/tes yang dijalankan.** Hal yang bergantung bentuk respons backend atau perilaku runtime ditandai "belum terverifikasi" dan dikumpulkan di §8 |

## 0. Cara baca

- **Dampak:** **P0** = menghalangi rilis atau pengguna terjebak/melihat data palsu · **P1** = pengguna terasa · **P2** = kecil/kosmetik.
- **Jenis:** `BELUM ADA` · `BERBEDA` · `BUG BARU` (cacat yang ditimbulkan app baru) · `DEVIASI DISENGAJA` (keputusan tercatat di `docs/FLOW_QUESTIONS.md`, tidak perlu dikerjakan).
- **Rujukan kode:** `L:` = `android-portal/app/src/main/java/id/diskola/app/`, `Lres:` = `android-portal/app/src/main/res/layout/`, `B:` = `Diskola-App-New/app/src/main/java/id/diskola/app/`.
  Nomor baris mengikuti working tree saat audit dan bisa bergeser.
- **ID gap** dipertahankan dari pemeriksaan per area supaya bisa dilacak: `HUB-`, `AGD-`, `POI-S-`, `POI-G-`, `MAT-`, `TGS-S-` (siswa), `TGS-G-` (guru), `AUT-`, `AKN-`, `NTF-`, `HOM-`, `REL-`, `KOM-`, `PR-`, `JR-`, `AK-`, `PB-`.
- Modul yang **sengaja tidak dibangun** (Marketplace, Feed/post/jelajah, Pengumuman, layar Chat, Perpustakaan, Prokes, Perangkat Terhubung, Try Out, ujian lama, AKM nasional) tidak dihitung sebagai gap (§6).

## 1. Ringkasan eksekutif

### 1.1 Jumlah gap per area

| Area | P0 | P1 | P2 | Catatan |
|---|---|---|---|---|
| Hub Pembelajaran | 2 | 3 | 10 | `HUB-01..15` |
| Agenda Mingguan (guru) | 0 | 1 | 4 | `AGD-01..05` |
| Poin siswa | 0 | 0 | 4 | `POI-S-01..04` (3 di antaranya deviasi disengaja) |
| Poin guru | 0 | 1 | 3 | `POI-G-01..04` |
| Hub/Agenda/Poin, lain-lain | 0 | 0 | 2 | `HUB-X01`, `HUB-X02` |
| Materi (siswa & guru) | 0 (2 kandidat P0) | 9 | 16 | `MAT-01..25`; 7 deviasi disengaja dicatat terpisah |
| Tugas (siswa & guru) | 2 | 8 | 18 | `TGS-S-01..12`, `TGS-G-01..14`, `TGS-X01..02`; 4 deviasi disengaja |
| Auth, Akun, Notifikasi, Rilis, Infra | 8 | 17 | 13 | 38 gap |
| Presensi + Jurnal | 0 | 1 | 13 | hampir paritas; 9 baris deviasi disengaja |
| Asesmen/AKM | 2 | 5 | 12 | 6 baris deviasi disengaja |
| Pembayaran | 3 | 6 | 10 | 8 baris deviasi disengaja |

Beberapa temuan muncul di lebih dari satu area dan hanya dihitung sekali di §1.2: pembatasan staf non-guru (`HUB-01/02/03`, `MAT-06`, `AUT-05/06`), routing notifikasi (`MAT-05`, `TGS-S-12`, `NTF-03`, `PR-08`, `PB-09`), migrasi database (`AK-02` = `REL-02`).

### 1.2 Daftar P0 (tanpa duplikat)

| # | ID | Masalah singkat | Fitur |
|---|---|---|---|
| 1 | `HUB-01` | Pembatasan menu staf non-guru lewat `roles[]` (commit lama `cea243dc`, v2.1.42) **sama sekali tidak ada**. Akun staf (Janitor dkk.) dianggap Tamu: semua tile terkunci termasuk Presensi | Pembelajaran |
| 2 | `HUB-02` | `check-account` dengan `data.id = null` tidak ditangani (`CheckAccountUserData.id: Int` non-null) → gagal parse untuk akun staf; `USER_ID` juga ditimpa 0 | Pembelajaran / Login |
| 3 | `TGS-G-01` | Paging guru mati: `loadMore()` ada tetapi tidak dipanggil. Guru hanya melihat 10 tugas dan 10 grup penilaian teratas | Pembelajaran (Tugas) |
| 4 | `TGS-G-03` | Saat menilai, file jawaban siswa tidak bisa dibuka/diunduh (lama: daftar semua file + Baca/Download; baru: teks satu nama file) | Pembelajaran (Tugas) |
| 5 | `REL-01` | `app_name` = "Education" (lama "Diskola"): update in-place mengganti nama aplikasi di launcher | Rilis |
| 6 | `AUT-01` | Login Google SSO tidak berfungsi: `BuildConfig.WEB_CLIENT_ID` kosong | Auth |
| 7 | `NTF-01` | Daftar & detail notifikasi memakai data karangan (`MockNotifications`, mis. "Tagihan SPP Rp 500.000") | Notifikasi |
| 8 | `NTF-03` | Tap push tidak dirutekan ke layar mana pun (`setContentIntent` dikomentari, tidak ada router) | Notifikasi |
| 9 | `AKN-01` | Pengaturan profil = UI contoh, tombol Simpan nonaktif | Akun |
| 10 | `AKN-03` | Ubah kata sandi = form dengan tombol `onClick = {}` | Akun |
| 11 | `AKN-05` | Kartu pelajar menampilkan **data pribadi palsu** hard-coded (NIS "2410 0032", dst.), juga terlihat oleh tamu | Akun |
| 12 | `REL-02` = `AK-02` | Database lama `diskola.db` tidak dibaca (baru `ebede.db`); jawaban ujian yang belum terkirim hilang saat update in-place. **Keputusan produk masih terbuka** | Rilis / AKM |
| 13 | `AK-01` | Teks `description` + `instruction` soal ("Perintah soal, deskripsi & cerita", dialog "Instruksi Asesmen") tidak pernah tampil; soal berbasis bacaan kehilangan stimulus (bobot bergantung data nyata) | AKM |
| 14 | `PB-01` | Top Up saldo belum ada (hub dan "TOP UP SEKARANG" hanya dialog "dalam pengembangan") | Pembayaran |
| 15 | `PB-02` | Tagihanku belum ada: invoice VA/Retail yang dibuat app baru tidak bisa dilihat/dibayar ulang/dibatalkan | Pembayaran |
| 16 | `PB-03` | Bulan SPP yang sudah punya invoice memunculkan "Cek Tagihanku" yang berujung dialog "dalam pengembangan": bulan itu tidak bisa dibayar sampai invoice kedaluwarsa (jalan buntu) | Pembayaran |

**Kandidat P0** (dinaikkan bila produk menganggap wajib): `MAT-01` (materi baru tidak muncul otomatis), `MAT-02` (tarik-untuk-muat-ulang menghapus cache sebelum jaringan; offline → daftar kosong).
Beberapa P1 punya syarat naik ke P0 bila data nyata mengaktifkannya: `AK-03` (soal bermedia), `PB-07`/`PB-08` (PPOB/Dana Partisipasi dianggap inti), `PR-01` (bila 2.1.42 sudah rilis).

### 1.3 Status fitur Pembelajaran

| Fitur | Kesimpulan |
|---|---|
| Hub Pembelajaran | Sebagian besar paritas (grid, locked, dialog, kartu kelas, banner presensi, lonceng/badge, SessionGuard, status verifikasi). Gap utama: **staf non-guru (P0)**, tombol "Verifikasi Data" dan tile Magang masih dialog "dalam pengembangan" |
| Materi siswa | Alur utama berfungsi dan sebagian besar paritas. Gap: pola cache/refresh (materi baru tidak muncul, refresh offline mengosongkan daftar), error tidak terlihat, notifikasi tidak membuka materi |
| Materi guru | Daftar, filter, upload, edit, hapus ada. Gap: pesan error server tertelan, banner error mudah tak terlihat, mapel per baris hilang, edit link/file belum pasti |
| Tugas siswa | Alur utama ada. Gap: kartu "Jawaban terkumpul" kemungkinan kosong (bentuk data belum cocok), daftar basi, error kirim mudah tak terlihat |
| Tugas guru | Form/kontrak baru (`d3aa201c`) **paritas penuh**. Gap: **paging mati (P0)**, **file jawaban tak bisa dibuka (P0)**, filter hanya menyaring cache |
| Agenda Mingguan | Paritas kuat. Gap: data hari ini basi (cache dipakai tanpa fetch), tombol "Presensi" melewati gate fitur |
| Poin siswa | Paritas kuat; selisih hanya P2 |
| Poin guru | Paritas kuat; gap: foto kamera kemungkinan gagal karena izin `CAMERA` tidak diminta |
| Perpustakaan | Tidak terjangkau di app lama dan tidak dibangun di app baru: konsisten dengan keputusan, bukan gap |

## 2. Perubahan app lama setelah baseline dokumen migrasi

Dokumen migrasi mengikuti 2.1.40 (AKM: 2.1.41, `e0a12b62`). Commit sesudahnya dan statusnya di app baru:

| Commit lama | Isi | Status di app baru |
|---|---|---|
| `e0a12b62` (10-19) | Soal rumus LaTeX, tombol Berikutnya → Selesai, header `X-App-*` | **Ada** dan terhubung (`B:ui/components/Assessment.kt`, `B:utils/MathParser.kt`, `B:apiservice/RequestInterceptor.kt`). Rumus belum teruji di perangkat |
| `d3aa201c` (10-21) | Kontrak form tugas guru (`teacher-data-scope`), `ApiErrorParser`, gate `payment`, parsing toleran | **Ada** (Tugas: logika murni identik dengan lama). Kurang: guard fallback gate `payment` di layar tujuan (`HUB-13`, `PB-10`) |
| `cea243dc` (10-23, v2.1.42) | `StaffMenuAccess` (`roles[]`), `data.id` null, jangan timpa `user_id`, pref `role_names`/`is_staff_limited` | **Tidak ada sama sekali** → `HUB-01`, `HUB-02`, `HUB-03` |
| `7a757cec` (10-23) | `AppDialog` kustom menggantikan ±60 `AlertDialog` | Padanan Compose ada (`B:ui/components/AppDialog.kt`, belum di-commit). Pemanggil Materi tidak memberi `tone` (`MAT-15/16`, perbaikan satu baris) |
| `699f18f1` (10-23) | Riwayat absen channel (`attend_via`/`leave_via`) di Data Absensi, MemoryDB v48 | **Tidak ada** → `PR-01` |
| `91c0116c` (10-24) | Spasi dialog | Tidak relevan |

## 3. Fitur Pembelajaran

### 3.1 Hub Pembelajaran

Rujukan lama: `L:pages/pembelajaran/PembelajaranPage.kt`, `StaffMenuAccess.kt`. Rujukan baru: `B:ui/screens/home/`, `B:viewmodel/HomeViewModel.kt`, `B:utils/HubRules.kt`, `B:utils/session/SessionStore.kt`.

**P0 / P1**

| ID | Sub-fitur | Lama | Baru | Jenis | Dampak |
|---|---|---|---|---|---|
| `HUB-01` | Staf non-guru lewat `roles[]` (`cea243dc`) | `StaffMenuAccess` FULL hanya bila `roles[]` memuat "Teacher"; staf terbatas hanya Presensi, menu lain terkunci dengan pesan `pembelajaran_role_locked_message`; tanpa popup tamu, `title_class` = email, banner absensi tetap dimuat (`L:pages/pembelajaran/StaffMenuAccess.kt:8-57`, `PembelajaranPage.kt:122,167-172,228-236,260-277`, `SosmedViewModel.kt:306-326`) | Tidak ada. `B:ui/screens/home/HomeMenuGrid.kt:42-71` hanya bercabang siswa/guru/lainnya; `SessionStore.kt:61` `isGuest = !isStudent && !isTeacher`; nama role tidak disimpan. Staf dianggap Tamu: popup tamu, tombol "Verifikasi Data", semua tile terkunci **termasuk Presensi** | BELUM ADA | **P0** |
| `HUB-02` | `check-account` dengan `data.id = null` | `@NullToZeroInt val id` dan `takeIf { it > 0 }` sebelum menulis `user_id` (`L:pages/login/LoginModels.kt:162`, `LoginViewModel.kt:108`) | `B:dataclass/ResponData/ListSekolah.kt:56-57` dan `LoginResponse.kt:118` bertipe `Int` non-null; adapter `@NullToZeroInt` sudah ada (`B:di/module/MoshiModule.kt:80-92`) tetapi tidak dipasang; `SessionStore.kt:78,172` selalu menimpa `USER_ID` dengan `data?.id ?: 0`. `"id": null` → `JsonDataException` → check-account (login langkah 1 dan muat Hub) gagal | BELUM ADA | **P0** (bila akun staf ada di produksi) |
| `HUB-03` | Akun `is_teacher=true` tanpa role "Teacher" | Dibatasi ke Presensi (`PembelajaranPage.kt:230-236,272-277`) | Semua 6 tile terbuka (`HomeMenuGrid.kt:56-63`) | BELUM ADA | P1. Catatan: FLOW_QUESTIONS 23-10 memutuskan "tidak menebak dari nama role" pada hari yang sama commit lama ini dibuat; **keputusan produk perlu disatukan** (`AUT-06`) |
| `HUB-04` | "Verifikasi Data" → `VerificationPage` | Tombol "Sudah"/"Belum" membuka `VerificationPage` (`PembelajaranPage.kt:391-409`) | Kedua tombol hanya dialog "Fitur dalam pengembangan" (`B:ui/screens/home/HomeScreen.kt:237-254`); tidak ada route | BELUM ADA | P1. Tamu/siswa tanpa kelas tidak punya jalan keluar dari status terkunci |
| `HUB-05` | Tile Magang (siswa) | → `MagangPage` (`PembelajaranPage.kt:716-725`) | `implemented=false` → dialog "dalam pengembangan" (`HomeMenuGrid.kt:54`, `HomeScreen.kt:128`). Magang **tidak** termasuk daftar yang diputuskan tidak dibangun | BELUM ADA | P1 |

**P2** (singkat)

| ID | Selisih | Rujukan |
|---|---|---|
| `HUB-06` | `check-account` Hub tidak menyegarkan `klaspayActive`/`is_verified`/`is_email_verified`/`is_email_verifying` (lama menyegarkan tiap muat). Lonceng membaca flag basi sampai login ulang | L:`SosmedViewModel.kt:281-285,368` · B:`SessionStore.kt:73-149` |
| `HUB-07` | Hub tidak dimuat ulang bila kembali < 30 dtk; tidak ada muat-paksa setelah Isi Jurnal/Verifikasi Jurnal/Presensi (DEVIASI DISENGAJA, FLOW 10-12 #3) | B:`HomeViewModel.kt:112-125,288` |
| `HUB-08` | Badge lonceng "99+" mulai 99 (lama mulai 100). Doc 05g salah menulis legacy "dari 99" | L:`PembelajaranPage.kt:89` · B:`HubRules.kt:92-96` |
| `HUB-09` | Teks `title_class` Tamu: "Belum memiliki kelas" (lama "Tidak memiliki kelas") | B:`HubRules.kt:21,89` |
| `HUB-10` | Banner presensi: bila `setting/me/today` gagal, area kosong tanpa pesan (lama "Jadwal absensi belum diatur"/sembunyi). DEVIASI DISENGAJA | B:`HomeViewModel.kt:202-210` |
| `HUB-11` | `current_subject.attendance_id` default `null` (lama `0`): beda hanya bila server menghilangkan key; bentuk terisi belum pernah dilihat | B:`dataclass/ResponData/HubModels.kt:13-25` |
| `HUB-12` | Fallback pesan gate guru: "belum tersedia di aplikasi…" (lama "sedang diperbarui…"); sheet baru bisa ditutup | B:`ui/components/FeatureUnavailableSheet.kt:34-37` |
| `HUB-13` | Tidak ada guard gate di layar Presensi/Jurnal/SPP/Partisipasi (lama `guardFeatureOnCreate`); jalur Agenda → Presensi melewati gate. Naik P1 begitu routing notifikasi dibangun | L:`feature/FeatureGate.kt:84-120` |
| `HUB-14` | `check-account` Hub memakai `student.nis` dulu (lama `user.nis_nik`); belum terverifikasi untuk sekolah dengan NIS ≠ NISN | B:`HomeViewModel.kt:160-161` |
| `HUB-15` | Routing tile memakai `rememberIsTeacher()` (sumber peran kedua; juga menganggap `roles` berisi "teacher"/"guru"). Klaim "dua sumber isTeacher diperbaiki" tidak benar | B:`MainTabsScreen.kt:239`, `materi/RoleDetection.kt:13-17` |
| `HUB-X01` | Peta di layar lapor Agenda butuh `MAPS_API_KEY`; build tanpa key → peta kosong tanpa error build | B:`app/build.gradle.kts:58` |
| `HUB-X02` | `AutoTimeGate` sudah dipasang di Asesmen/Jurnal/Presensi/Poin; gate jam di HomePage (Q7) belum diputuskan | B:`ui/components/AutoTimeGate.kt` |

### 3.2 Materi (siswa & guru)

Rujukan lama: `L:pages/theory/`, `L:pages/theoryteacher/`, `L:utils/IntentUtil.kt`, `L:utils/LinkPreview.kt`. Rujukan baru: `B:ui/screens/materi/`, `B:viewmodel/Materi*`, `B:repository/MateriRepository.kt`, `B:repository/FileOpenRepository.kt`.

**P1**

| ID | Sub-fitur | Lama | Baru | Jenis |
|---|---|---|---|---|
| `MAT-01` (kandidat P0) | Muat ulang otomatis saat layar dibuka | Halaman 0 selalu diambil saat buka (`L:pages/theory/TheoryPage.kt:30-32`, `MateriPage.kt:81`, `TheoryViewModel.kt:189-209`) | Hanya mengambil bila cache kosong (`B:repository/MateriRepository.kt:36-38,74-76`); cache tidak pernah kedaluwarsa, hanya dibersihkan saat logout. Siswa tidak melihat materi baru dari guru kecuali tarik-untuk-muat-ulang. ("Materi Saya" guru sama dengan lama, bukan gap) | BERBEDA |
| `MAT-02` (kandidat P0) | Tarik-untuk-muat-ulang vs offline | API dipanggil dulu, cache dihapus hanya setelah sukses (`L:TheoryViewModel.kt:195-204`) | Cache dihapus **sebelum** jaringan: `mapelDao.clear()`/`clearBySubject`/`clearByTeacher` (`B:MateriRepository.kt:42-45,59-62,81-84,111-114`). Offline → daftar kosong + "Belum terdapat materi". `mapel.clear()` juga mengosongkan tabel yang dipakai Tugas guru | BUG BARU |
| `MAT-03` | Error jaringan di daftar/refresh/detail | Toast `localizedMessage` (`L:TheoryViewModel.kt:183,207,256,226`) | Hanya `Timber.e` (`B:MateriViewModel.kt:61-63,95-97`, `MateriGuruViewModel.kt:68-129`); gagal detail → dialog "Materi Tidak tersedia" yang menyesatkan (`MateriRepository.kt:137-139`) | BERBEDA |
| `MAT-04` | Tarik-untuk-muat-ulang dari daftar kosong | `SwipeRefreshLayout` membungkus empty state (`Lres:materi_page.xml:24-66`) | Empty state menggantikan `PullToRefreshBox` (`B:MateriListScreen.kt:57-63`, `MateriGuruScreen.kt:119-124`); gagal awal → tersangkut di "Belum terdapat materi" | BERBEDA |
| `MAT-05` | Notifikasi/deep link ke Materi | FCM `theory` membuka `MateriDetailPage` (`L:services/NotifRouter.kt:77-79`) | Tidak ada: `setContentIntent` dikomentari (`B:network/NotifService.kt:107,124,138`); tombol detail notifikasi `TODO`; `Route.MateriDetail` + VM sudah siap tetapi tidak ada pemanggil | BELUM ADA |
| `MAT-06` | Kunci menu Materi untuk staf non-guru | `StaffMenuAccess` | Tidak ada → staf non-guru bisa masuk Materi guru/siswa (`B:HomeMenuGrid.kt:41-71`). Lihat `HUB-01` | BELUM ADA |
| `MAT-07` | Pesan error server saat upload/edit/hapus | `e.localizedMessage` (pesan backend nyata) (`L:theoryteacher/UploadMateriViewmodel.kt:196-199`) | `customMessage` menimpa: selalu "Gagal membuat materi" (juga saat edit), hapus selalu "Gagal menghapus materi…" (`B:UploadMateriViewModel.kt:94-99`, `BaseViewModel.kt:54`); `AppErrorHandler` tidak terpakai di dua jalur ini | BERBEDA |
| `MAT-08` | Letak banner error di form upload | Toast | `BannerError` di dalam kolom scroll, tombol Posting di `bottomBar`, banner hilang sendiri (`B:UploadMateriScreen.kt:170-201`, `ui/components/States.kt:169-176`) → error tak terlihat bila form sudah di-scroll | BUG BARU |
| `MAT-18` | Info baris "Materi Saya" | Tag mapel + nama guru + foto guru (`Lres:materi_item.xml:39-122`) | Inisial guru (bukan foto); "Materi Saya" tanpa mapel/guru, hanya chip target + format + tanggal. `subject_name` tersimpan tetapi tidak dibaca (`B:MateriListItem.kt:65-156`) | BERBEDA |

**P2**

| ID | Selisih |
|---|---|
| `MAT-09` | Edit: `link` hanya dikirim bila tidak kosong → mengosongkan link tidak pasti menghapus di server; tombol X pada file hanya mengosongkan label (file lama tetap). Perlu uji backend (`B:UploadMateriScreen.kt:437,282-286`) |
| `MAT-10` | Dropdown mapel form/filter guru hanya halaman pertama `take=20` tanpa `loadMore` (guru > 20 mapel; `B:MateriRepository.kt:53-68`) |
| `MAT-11` | State form upload memakai `remember` biasa, hilang saat recreate Activity/process death (`B:UploadMateriScreen.kt:91-102`) |
| `MAT-12` | Berkas disalin ke `cacheDir` di thread UI saat klik Posting; nama berkas mentah, salinan tidak dihapus (`B:UploadMateriScreen.kt:178-182,447-456`) |
| `MAT-13` | Tidak ada dialog progres upload/hapus; menutup form saat upload membatalkan coroutine tanpa konfirmasi (`B:UploadMateriScreen.kt:176-186`, `MateriGuruScreen.kt:68-74`) |
| `MAT-14` | Tombol X form juga memicu refresh daftar (`materiChanged=true`) → menghapus cache guru (`B:MainTabsScreen.kt:453-461`); jadi P1 bila offline |
| `MAT-15` | Konfirmasi hapus materi: nada INFO (lama ERROR + ikon peringatan); teks diperpanjang "Materi akan hilang dari daftar siswa" (`B:MateriGuruScreen.kt:201-214`) |
| `MAT-16` | Dialog "Materi Tidak tersedia": nada INFO (lama WARNING) (`B:MateriDetailScreen.kt:208-217`) |
| `MAT-17` | Teks UI: "Unduh"/"Lihat" (lama "Download"/"baca materi"), "Unggah Materi" (lama "Upload Materi"), "Loading..." Inggris, toast info unduh tampil di banner merah, label bagian huruf kapital |
| `MAT-19` | Pencarian mapel: tanpa tombol hapus dan aksi IME search; `loadMoreSubjects` dimatikan saat ada kata kunci; debounce menunda daftar awal 400 ms (`B:MapelScreen.kt:130-157`, `MateriViewModel.kt:39-72`) |
| `MAT-20` | Backslash pada URL file tidak dibersihkan (lama dibersihkan, `L:MateriDetailPage.kt:52-53`); belum terverifikasi apakah backend produksi mengirimnya |
| `MAT-21` | Link preview hanya `og:image` + regex (lama Jsoup + 4 cadangan gambar); situs tanpa `og:image` tampil kartu "Tidak ada gambar pratinjau" (`B:LinkPreviewRepository.kt:34-61`) |
| `MAT-22` | Detail yang diambil dari API: layar kosong tanpa indikator (lama dialog "menampilkan detail materi") |
| `MAT-23` | Penentuan peran Materi: tile ikut `isStudent`, jalur masuk ikut `is_teacher`/`roles` → akun peran ganda berbeda (`B:RoleDetection.kt:12-16`) |
| `MAT-24` | Timeout HTTP connect/read 30 dtk (lama 120 dtk): upload 12 MB/unduh di jaringan lambat berisiko; lintas fitur (`B:di/module/ApiModule.kt:201-203`) |
| `MAT-25` | Filter "Materi Saya": status paging tidak direset saat ganti filter; `teacherId` dibaca sekali saat flow dibangun (`B:MateriGuruViewModel.kt:34-120`) |

**Deviasi disengaja (bukan gap):** pull-to-refresh benar-benar memuat ulang; filter Kelas+Mapel AND; target wajib dipilih; batas 12 MB dicek saat memilih berkas; kontrol lompat-halaman PDF disembunyikan; tanpa pinch-zoom PDF; tab "Mata Pelajaran" guru dapat diakses.
Perbaikan baru yang sebaiknya dicatat di FLOW_QUESTIONS: validasi skema URL, bagikan bila tidak ada aplikasi pembuka, status galat PDF "Gagal membuka berkas", unduhan streaming, nama berkas pembahasan yang benar, dialog "Akses File Diperlukan" **sebelum** unduh.

### 3.3 Tugas / PR (siswa & guru)

Rujukan lama: `L:pages/homework/` (+ `teacher/`). Rujukan baru: `B:ui/screens/tugas/`, `B:viewmodel/Tugas*`, `B:repository/TugasRepository.kt`, `B:utils/AssignmentRules.kt`.

Catatan lingkup: rumus LaTeX dan tombol bawah "Selesai" adalah fitur **AKM**, bukan Tugas; di Tugas lama tidak ada rendering rumus, jadi tidak ada gap rumus untuk Tugas.
Semua isi `d3aa201c` yang menyentuh Tugas (kontrak form `teacher-data-scope`, payload kanonik, `AssignmentErrors`, `ApiErrorParser`, header `X-App-*`, guard id ≤ 0) **sudah ada** dan logikanya identik dengan lama.

**Guru: P0 / P1**

| ID | Sub-fitur | Lama | Baru | Jenis | Dampak |
|---|---|---|---|---|---|
| `TGS-G-01` | Paging "List Tugas" & "Penilaian" | `PagedList` + `boundaryCallback` (`L:HomeWorkViewModel.kt:208-300`) | `loadMore()` ada di `B:TugasGuruViewModel.kt:110-116` dan `TugasPenilaianViewModel.kt:48-54` tetapi **tidak dipanggil** dari `TugasGuruScreen.kt` (`LazyColumn` tanpa `LazyListState`/pemicu; bandingkan `TugasSiswaScreen.kt:99,128,157`). Guru > 10 tugas tidak bisa mengedit/menghapus yang lama; pengumpulan pada tugas lama tak bisa dinilai; "Lihat Tugas" dari Penilaian untuk tugas di luar cache → dialog "Tugas Tidak Tersedia" | BUG BARU | **P0** |
| `TGS-G-03` | File jawaban siswa saat menilai | Dialog nilai menampilkan semua file (`files[]`/`file_*`) dengan Baca/Download + link preview (`L:HomeworkScoringPage.kt:61-191`, `HomeworkAnswerFilesAdapter.kt`) | Hanya teks satu nama file, tanpa tombol; model `Assignment` tidak punya `files[]` (`B:TugasScoringScreen.kt:91-101`, `dataclass/ResponData/HomeworkModels.kt:237-252`). Link jawaban masih bisa dibuka lewat kartu preview | BELUM ADA | **P0** |
| `TGS-G-02` | Filter Kelas/Mapel | Tiap perubahan filter fetch server dengan filter + paging | Fetch hanya bila hitungan lokal untuk kombinasi filter = 0 (`B:TugasGuruViewModel.kt:91-108`, `TugasRepository.kt:122-126`) → bila cache sebagian, filter hanya menyaring cache; bersama `TGS-G-01` hasil tidak lengkap | BUG BARU | P1 |
| `TGS-G-04` | "Lihat Jawaban" di Tugas Terkumpul | Tombol disembunyikan, listener kosong | Tombol tampil tetapi membuka detail **tugas** yang sama untuk semua siswa (id tugas, bukan id siswa), tidak menampilkan jawaban (`B:TugasTerkumpulScreen.kt:127,196`). Label menjanjikan lebih dari yang ada; terkait `TGS-G-03` | DEVIASI DISENGAJA (dengan celah) | P1 |

**Siswa: P1**

| ID | Sub-fitur | Lama | Baru | Jenis |
|---|---|---|---|---|
| `TGS-S-01` | "Jawaban terkumpul" di detail tugas Sudah/Nilai | File dari `files[]`/`file_student_*`; bila kosong GET collect dibaca di `student_assignment.files`; respons kirim `data.files` disimpan (`L:HomeWorkViewModel.kt:353-374,442`, `OnKlasDbUtil.kt:396-413`) | `file_student_*` diparse tetapi tidak disimpan; `files[]` tidak ada di model; GET collect dibaca di `data.files` (bukan `student_assignment.files`); respons kirim tidak disimpan; bersyarat `student_assignment_id > 0` dan `uploaded > 0` (`B:TugasRepository.kt:95-115`, `HomeworkModels.kt:139-171,334-354`). Dugaan kuat: kartu kosong. **Belum terverifikasi terhadap response asli** | BUG BARU |
| `TGS-S-02` | Path file jawaban relatif | `resolvedPath()` menambah `ASSETS_URL` bila bukan http (`L:HomeWorkModels.kt:244-249`) | `downloadToAppFiles` menolak bila bukan http(s) (`B:FileOpenRepository.kt:38`); tidak ada resolver di jalur Tugas | BERBEDA |
| `TGS-S-03` | Kesegaran daftar | Tab Belum selalu fetch halaman 1 saat dibuat; Sudah/Nilai fetch tiap Activity baru | `ensure*FirstPage` hanya bila tabel kosong (`B:TugasRepository.kt:46-72`); tugas baru dari guru tidak muncul sampai pull-to-refresh; setelah kirim jawaban tab Sudah tidak ikut segar | BERBEDA |
| `TGS-S-04` | Empty state & gagal muat awal | Empty state di dalam `SwipeRefreshLayout` | Daftar kosong → hanya `EmptyState` tanpa `PullToRefreshBox`/aksi (`B:TugasSiswaScreen.kt:101-103,130-132,159-161`); gagal awal tampil "Belum terdapat tugas" menyesatkan, tanpa retry. Sama untuk tab guru | BUG BARU |
| `TGS-S-05` | Error kirim jawaban terlihat | Toast / dialog "Akses diblokir" | Banner di dalam kolom scroll, tombol Kirim di `bottomBar`, banner hilang 5–12 dtk (`B:TugasDetailScreen.kt:156-212`) | BERBEDA |
| `TGS-S-12` | Notifikasi → Tugas | Push `task`/`penilaian` membuka daftar Tugas (`L:services/NotifRouter.kt:72,80-82`) | Tidak ada rute (`B:network/NotifService.kt:105-136`, `MainTabsScreen.kt:244-246`). Paritas cukup dengan membuka `Route.TugasSiswa`/`TugasGuru` | BELUM ADA |

**P2 (Tugas)**

| ID | Selisih |
|---|---|
| `TGS-S-06` | Pilihan file jawaban: tanpa dedupe; dua file nama sama dari folder berbeda saling menimpa di `cacheDir`; salin file di thread utama (`B:TugasDetailViewModel.kt:205-212`, `TugasDetailScreen.kt:181,475-483`) |
| `TGS-S-07` | Tombol Download soal tidak menandai "sudah baca" (lama menandai); hanya "Baca" atau klik link soal (`B:TugasDetailScreen.kt:236`). Perlu konfirmasi produk |
| `TGS-S-08` | Dialog "Akses File Diperlukan" sebelum Baca (lama langsung buka). DEVIASI DISENGAJA |
| `TGS-S-09` | Detail selalu menampilkan DESKRIPSI/SOAL/PEMBAHASAN dengan placeholder (lama menyembunyikan yang kosong); status berupa badge |
| `TGS-S-10` | Kartu daftar siswa tanpa avatar guru/centang/tag "Baru" berwarna |
| `TGS-S-11` | Dialog Nilai: sheet tidak mengumpulkan `errorMessage`; gagal buka pembahasan senyap; info sukses unduh diberi warna error (`B:TugasNilaiDetailSheet.kt:48-80`) |
| `TGS-G-05` | Ketuk item guru → `TugasDetailScreen` read-only; Edit/Hapus hanya lewat kebab (lama: form terkunci "Detail Tugas" dengan menu Edit/Hapus). Mode `UploadTugas(editable=false)` tidak pernah dinavigasi = kode mati |
| `TGS-G-06` | Kartu daftar guru tanpa `information_label` (info batas/pengumpulan) dan nama guru |
| `TGS-G-07` | Refresh menghapus cache dulu (`clearByTeacher`/`clearCollected`); menutup form lewat X/Back ikut memicu refresh → memotong daftar ke 10; offline → kosong (`B:TugasRepository.kt:129-213`, `MainTabsScreen.kt:566-575`) |
| `TGS-G-08` | Pemilih file soal hanya `application/pdf` (lama semua tipe dokumen/gambar); hanya nama file tampil (tanpa ukuran); link tanpa batas 5000 (`B:UploadTugasScreen.kt:357,480-485`). Spesifikasi backend `05i` tidak membatasi tipe |
| `TGS-G-09` | Kelas form: gagal → kosong + banner (lama cadangan Room). Sengaja agar tidak menimpa tabel `classroom` bersama |
| `TGS-G-10` | Beri/Ubah Nilai: tanpa header mapel/guru dan baris Sekolah; skor > 100 dipaksa 100 (lama ditolak) |
| `TGS-G-11` | Layar Terkumpul tanpa spinner saat memuat; hapus bisa ditekan berulang (loading tidak dirender) |
| `TGS-G-12` | Peran masuk Tugas: akun `is_student` + `is_teacher` melihat tile siswa tetapi masuk `TugasGuru`, lalu ditolak "Tugas guru tidak tersedia"; staf non-guru masuk `TugasSiswa` (lama: halaman guru) (`B:MainTabsScreen.kt:239-246`) |
| `TGS-G-13` | `AssignmentAccess.canManage` memblokir akun tanpa data guru (lama gagal di server 400). DEVIASI DISENGAJA |
| `TGS-G-14` | Multi-kelas: satu kelas per request di kedua app (Q20 ditunda) |
| `TGS-X01` | Interceptor: 2xx dengan `status != "success"` tidak dilempar sebagai error (lama dilempar); dialog "Perbaikan Sistem" 503/504 tidak ada (`B:apiservice/ResponseInterceptor.kt:107-111`). Belum terverifikasi apakah backend pernah membalas 200 + `status:"error"` |
| `TGS-X02` | Orientasi portrait global (lama Aktivitas Tugas tidak dikunci). Keputusan global |

### 3.4 Agenda Mingguan (guru)

Rujukan lama: `L:pages/agenda_mingguan/`. Rujukan baru: `B:ui/screens/agenda/`, `B:viewmodel/AgendaMingguanViewModel.kt`, `B:repository/AgendaRepository.kt`.

| ID | Selisih | Dampak |
|---|---|---|
| `AGD-01` | **Data hari ini basi.** Lama: `onResume` (termasuk yang pertama) → `loadAgendas(today, forceRefresh = true)` (`L:AgendaMingguanListFragment.kt:155-163`). Baru: `init` → `load(force=false)`; bila Room punya cache hari ini → tanpa fetch, dan ON_RESUME pertama diabaikan (`B:AgendaMingguanViewModel.kt:47-71,93`). Buka Agenda pagi → Presensi → buka lagi: `gate.in` tetap null → dialog "presensi masuk dulu" yang salah | **P1** |
| `AGD-02` | Tombol "Presensi" pada alert gate membuka `Route.Presensi` **tanpa** `check-feature-availability` (lama lewat `launchFeature`); layar lapor baru juga punya tombol "Presensi" (lama hanya "Tutup"). Dokumen 05e/FLOW 10-06 #5 ("hanya Tutup") sudah basi (`B:AgendaMingguanScreen.kt:179-192`, `MainTabsScreen.kt:473`) | P2 |
| `AGD-03` | Kartu item menambah baris "Hadir"/"Pulang" + tombol "Absen Hadir"/"Absen Pulang" (lama "Lapor masuk/pulang" di layar lapor) | P2 |
| `AGD-04` | Gagal muat tanpa cache: `BannerError` + EmptyState (lama dialog "Pemberitahuan" + "Tutup") | P2 |
| `AGD-05` | Lokasi lapor: submit nonaktif sampai ada fix GPS (DEVIASI DISENGAJA), tetapi `LocationProvider` jatuh ke `lastLocation` bila fix segar gagal; tidak ada deteksi mock-location | P2 |

Paritas terbukti: strip tanggal + picker bulan/tahun + ringkasan "x / y sesi", status chip, aturan tap, lapor masuk/pulang (izin, peta, alamat, payload `{agenda_id, lat, lng}`, urutan pesan error), kebijakan collapsible, detail, cache per tanggal, badge Agenda di Hub ("9+").

### 3.5 Poin (siswa & guru)

Rujukan lama: `L:pages/poin/`. Rujukan baru: `B:ui/screens/poin/`, `B:repository/PoinRepository.kt`, `B:viewmodel/Poin*`.

| ID | Selisih | Jenis | Dampak |
|---|---|---|---|
| `POI-G-01` | **Foto dari kamera (Pelanggaran/Prestasi):** `TakePicture()` dipakai tanpa meminta izin `CAMERA`, padahal manifest mendeklarasikannya (lama meminta lewat `IntentUtil.openCameraFragment`). Pada Android 6+ tanpa izin → `SecurityException` tertangkap `runCatching` → pesan menyesatkan "Gagal membuka kamera, ijin untuk menyimpan file tidak diberikan". Baru jalan bila izin sudah diberikan lewat fitur lain (`B:PoinFormScreen.kt:109-111,264-272`). **Belum terverifikasi di perangkat** | BUG BARU (kandidat) | **P1** |
| `POI-G-02` | Setelah sukses kirim kembali ke layar Hasil + skor disegarkan (lama menutup seluruh Poin). `refreshSelectedScores` mencocokkan `it.id == student.id`; bila `id==0` bisa salah baris | DEVIASI DISENGAJA | P2 |
| `POI-G-03` | Judul "Pilih Jenis Pemanggilan" + sub-teks "Poin N" (lama "Pilih Jenis Penanganan") | BERBEDA | P2 |
| `POI-G-04` | Waktu pemanggilan `HH:mm` (detik selalu 00; lama `HH:mm:ss`) | BERBEDA | P2 |
| `POI-S-01` | Label tombol pemanggilan = elemen pertama urutan server (lama yang terbaru, `id desc`); belum terverifikasi urutan `notHandled` | BERBEDA | P2 |
| `POI-S-02` | `score-student` dengan > 1 elemen `data`: hanya `firstOrNull()` (lama `forEach`); belum terverifikasi apakah server pernah mengirim > 1 | BERBEDA | P2 |
| `POI-S-03` | Tanpa cache offline daftar poin | DEVIASI DISENGAJA | P2 |
| `POI-S-04` | Header rekap tampil juga bila daftar kosong (perbaikan) | DEVIASI DISENGAJA | P2 |

Paritas terbukti: tab Pelanggaran/Prestasi dari satu endpoint, rekap semester + terjemahan "Late/Not Out/Not Attending Class", daftar kejadian + dialog dokumentasi, dialog "Detail Penanganan Siswa", cek waktu otomatis; guru: gerbang Cooperative-only, cari siswa (≥3 karakter, debounce 300 ms, `name` + `nisn`), layar Hasil + 3 aksi, tiga form dengan payload dan pesan yang sama.

### 3.6 Perpustakaan

Di app lama tidak terjangkau: rute `PerpusPage` di Hub dikomentari (`L:PembelajaranPage.kt:681-683`); tile "Pinjam Buku" di Pembayaran hanya dialog "Fitur belum tersedia". Di app baru tidak ada layar (hanya `PerpustakaanApiService` placeholder) dan tile "Pinjam Buku" membuka dialog yang sama. **Konsisten dengan keputusan "tidak dibangun", bukan gap.**

## 4. Gap lintas fitur

| Tema | Dampak ke | Gap terkait | Ringkasan |
|---|---|---|---|
| Pembatasan staf non-guru (`cea243dc`) | Hub, Materi, Tugas, Login | `HUB-01/02/03`, `MAT-06`, `TGS-G-12`, `AUT-05/06` | Satu paket perbaikan: `@NullToZeroInt` pada `CheckAccountUserData.id`/`LoginAccountUserData.id`, jangan timpa `USER_ID` bila 0, simpan `roles[].name`, port `StaffMenuAccess` + notice. **Keputusan produk dulu**: FLOW 23-10 menyatakan "tidak menebak dari nama role" (`AUT-06`) |
| Routing notifikasi/push | Materi, Tugas, Presensi, Jurnal, Ujian, Pembayaran | `MAT-05`, `TGS-S-12`, `NTF-02/03`, `PR-08`, `PB-09`, `HUB-13` | Semua `setContentIntent` dikomentari; tidak ada `NotifRouter`; `MainActivity` tidak membaca extra `page`/`goto`; layar notifikasi masih mock. Semua layar tujuan sudah ada di app baru. Saat router dibangun, guard gate fitur (`HUB-13`, `PB-10`) wajib menyusul |
| Pola cache-first tanpa refresh | Materi, Tugas | `MAT-01/02/04/14/25`, `TGS-S-03/04`, `TGS-G-02/07` | `ensure*` hanya fetch bila cache kosong, refresh menghapus cache sebelum jaringan, empty state tanpa pull-to-refresh, kegagalan hanya di-log. Perbaikan seragam di repository + layar (urutan: fetch dulu baru ganti cache; empty state di dalam `PullToRefreshBox`; tampilkan error) |
| Error tidak terlihat | Materi, Tugas | `MAT-03/07/08`, `TGS-S-05`, `TGS-S-11` | `BannerError` di dalam area scroll sementara aksi di `bottomBar`, hilang sendiri 5–12 dtk; pesan server tertimpa `customMessage`. Pindahkan ke luar scroll/snackbar dan pakai `AppErrorHandler` |
| Timeout HTTP | Semua unduh/unggah | `MAT-24`, `AUT-10` | connect/read 30 dtk (lama 120 dtk) |
| Interceptor respons | Semua | `TGS-X01`, `AUT-09` | 2xx dengan `status != "success"` dan dialog "Perbaikan Sistem" 503/504 tidak ada |
| Tidak ada tes otomatis | Materi | — | `app/src/test` dan `androidTest` tidak memuat Materi/Mapel/PDF/LinkPreview |

## 5. Modul lain (non-pembelajaran)

Detail terbaik dibaca per tabel; di sini hanya P0/P1 dan yang perlu keputusan. P2 ada di laporan kerja.

### 5.1 Auth, sesi, rilis, infrastruktur

Paritas atau lebih baik: login NISN, validasi sesi, `is_active`, logout (diperluas ke FINISHED-belum-terunggah), 401 terpusat, header `X-App-*`, logging rilis (tanpa body/PIN/token), `applicationId` `id.diskola.app`, `versionCode` 83 > 82, keystore identik (hash sama), `google-services.json` identik, 16 KB page size.

| ID | Selisih | Dampak |
|---|---|---|
| `REL-01` | `app_name` = "Education" (`B-res:values/strings.xml:2`; lama "Diskola"); dipakai sebagai label launcher dan nama kanal. Ikon `iconeapp.png` bukan mipmap adaptif | **P0** |
| `AUT-01` | Google SSO memakai `BuildConfig.WEB_CLIENT_ID` (`B:ui/screens/auth/GoogleSignIn.kt:23`) yang dibaca dari `local.properties` tetapi barisnya dikomentari → `""` di build rilis; `getCredential` gagal diam-diam. Perbaikan murah: pakai `R.string.default_web_client_id` hasil plugin google-services (sudah ada) | **P0** |
| `REL-02` | Room: baru `ebede.db` v11, `diskola.db` lama tidak dibaca/dihapus; worker lama merujuk kelas yang tidak ada. Keputusan masih "Belum tahu" (`FLOW_QUESTIONS.md` bagian B) | **P0** (keputusan) |
| `AUT-02` | Banner Firebase In-App Messaging tidak tampil (artefak `-display` tidak ada; gerbang splash tidak ada) | P1 |
| `AUT-03` | Dialog keamanan email/password default tidak muncul otomatis di Home/Akun (hanya di gerbang aktivasi Klaspay) | P1 |
| `AUT-04` | `writeCheckAccount` tidak menulis `is_email_verified`/`is_verified`/`is_email_verifying`, dan deep link `portal.diskola.id/verify-email` tidak ada. Klaim FLOW_QUESTIONS "pulih lewat check-account" **salah**; gerbang aktivasi Klaspay terus meminta email sampai login ulang | P1 |
| `AUT-05` | Lihat `HUB-02` (dan `StaffMenuAccess`) | P0/P1 |
| `AUT-06` | Dua keputusan bertabrakan soal akun `is_teacher` non-Teacher (lama membatasi 23-10, baru memutuskan sebaliknya hari yang sama). Perlu keputusan | P1 |
| `REL-03` | Sesi tetap login (file & key sama), tetapi isi JSON `user/school/student/teacher` memakai nama field berbeda (`nis_nik` vs `nisNik`, dst.). Guru/staf hasil migrasi: `nisn` kosong → check-account tidak berjalan; avatar dan koordinat sekolah kosong sampai login ulang. Tidak ada kode/tes migrasi | P1 |
| `REL-05` | `ACCESS_BACKGROUND_LOCATION` dan receiver geofence dibuang. Pemeriksaan Auth menandainya bertentangan dengan keputusan 30-09 F/Q1 ("tetap wajib"), tetapi pemeriksaan Presensi menemukan keputusan 10-07 #4 sudah mengubahnya untuk Presensi (`PR-02`, deviasi disengaja). Sisa pekerjaannya hanya konfirmasi backend, dan keputusan ulang bila Magang dibangun | P2 (dinilai ulang dari P1; hitungan §1.1 memakai angka asli pemeriksaan) |

P2: topik FCM `unlogged` tidak pernah dilepas untuk sesi berjalan (`AUT-07`), topik kelas tidak diperbarui setelah login (`AUT-08`), dialog "Perbaikan Sistem" (`AUT-09`), timeout (`AUT-10`), pengunggah log `upload_log` dihapus tanpa keputusan (`AUT-11`), notifikasi lokal "Ganti Password" tanpa tujuan (`AUT-12`), gerbang izin notifikasi/waktu otomatis belum diputuskan (`AUT-13`), risiko jalur 401/logout (`AUT-14`), dialog wallet Klaspay di Home (`HOM-03`), urutan versi (`REL-04`), worker/service lama tanpa padanan (`REL-06`).

### 5.2 Akun

Hanya **logout** yang nyata. Sisanya layar contoh; beberapa menampilkan data palsu ke pengguna sungguhan.

| ID | Selisih | Dampak |
|---|---|---|
| `AKN-01` | Pengaturan profil (username/telepon/email/pilih email Google): layar contoh, "Simpan" `enabled=false` (`B:ui/screens/akun/AkunSubScreens.kt:54-79`) | **P0** |
| `AKN-03` | Ubah password: form lengkap tetapi tombol `onClick = {}` (`AkunSubScreens.kt:184-189`); selalu terlihat (lama disembunyikan bila tak punya NIS) | **P0** |
| `AKN-05` | Kartu pelajar: data hard-coded NIS "2410 0032", NISN "0051234567", "Jombang, 15 Mei 2009", dst.; baris menu tampil untuk semua non-guru termasuk tamu (`AkunSubScreens.kt:221-265`, `AkunScreen.kt:68,150`) | **P0** |
| `AKN-02` | Kontak & verifikasi email: layar contoh, tombol mati | P1 |
| `AKN-04` | Foto profil (crop + upload): ikon kamera dekoratif | P1 |
| `AKN-06` | Tentang: versi statis "2.2.0", `support@diskola.id`, `(0321) 123456`, tombol YouTube/Website mati, changelog contoh ("mode gelap", padahal app selalu terang); Kebijakan & Privasi pasca-login tidak bisa dibuka (dialog placeholder; syarat Play) | P1 |
| `AKN-07` | Layar "Perangkat · 3 sesi aktif" berisi data palsu (`MockAkun.devices`) padahal diputuskan tidak dibangun dan pintunya di app lama dikomentari | P1 |
| `AKN-08` | Header: username dikarang dari nama depan, peran selalu "Siswa" untuk non-guru (termasuk tamu), banner "Email belum terverifikasi" selalu tampil dan tak bisa diklik, "Terakhir diubah 4 bulan lalu" statis | P1 |

Rekomendasi minimal sebelum rilis: **sembunyikan** layar palsu (`AKN-05`, `AKN-06`, `AKN-07`) dan nonaktifkan baris yang tombolnya mati sampai dibangun.

### 5.3 Notifikasi

| ID | Selisih | Dampak |
|---|---|---|
| `NTF-01` | Daftar & detail: `NotifikasiViewModel` memuat `MockNotifications.seed`; kontrak placeholder `take/skip` tidak cocok dengan backend (`wallet_id/page/limit`, 50/halaman, tandai dibaca) | **P0** |
| `NTF-03` | Tap push → layar tujuan: tidak ada router; `MainActivity` tidak membaca extra `page`/`goto` | **P0** |
| `NTF-02` | Tombol aksi di detail notifikasi `/* TODO */` | P1 |
| `NTF-04` | Push foreground: judul/isi hanya dari `data["title"/"body"]`; payload v2 menaruhnya di blok `notification` → notifikasi kosong (belum terverifikasi dengan payload nyata) | P1 |
| `NTF-06` | Chat dari notifikasi + "Balas" belum ada (bertentangan dengan keputusan A: tetap dibangun) | P1 |
| `NTF-05` | Ikon/kanal: tanpa meta-data FCM; channel id `fcm_default_channel_id` bukan `diskola_fcm` | P2 |

Lonceng + badge ringkasan sudah paritas (kode).

### 5.4 Presensi + Jurnal KBM

Praktis paritas, **tanpa P0**. Yang nyata:

| ID | Selisih | Dampak |
|---|---|---|
| `PR-08` | Notifikasi `ATTENDANCE`/`EXAM`/`JOURNAL` belum dirutekan (lihat §4) | P1 |
| `PR-01` | Riwayat absen channel (ikon + sheet App/Mesin/Face di Data Absensi, `699f18f1`) belum ada; DTO/Room tanpa `*_via`. Naik P1 bila 2.1.42 sudah rilis dan sekolah memakai mesin/face | P2 |
| `PR-07` | Gerbang `presensi`/`jurnal-kbm` hanya di Home (lama juga di `onCreate` layar) | P2 |
| `PR-03` | Panduan layar Presensi = sheet statis (lama coach-mark berlapis) | P2 |

Deviasi disengaja: geofence + izin lokasi latar tidak dibangun (`PR-02`), avatar pada marker peta (`PR-04`), deteksi lokasi palsu (tambahan baru, `PR-05`), `absence_setting` hilang → dibatasi (`PR-06`), pemindai QR Google code scanner (`JR-02`), tanpa cache jadwal Jurnal (`JR-01`, `JR-03`), kumpulan perbaikan bug lama (`PR-09`, `JR-05`). Risiko `JR-04` (HTTP 307 "jurnal sudah ada") mekanismenya identik dengan lama.
Dialog "Batas waktu presensi masuk telah lewat…" (`7a757cec`) tidak ada sebagai string aktif di app lama, jadi bukan gap.

### 5.5 Asesmen/AKM

Inti sudah lengkap: 9 tipe soal, autosave, timer + deadline worker, submit online/antrean offline, blokir logout, lockdown, pelanggaran + antrean, penalti, laporan masalah, label/stempel "Dibatalkan", rumus LaTeX, tombol Selesai, `AppDialog`.

| ID | Selisih | Dampak |
|---|---|---|
| `AK-01` | `description` + `instruction` tidak pernah tampil (`L:pages/akm/AkmQuestionsPage.kt:398,497-512`; baru hanya diparse di `AkmModels.kt:136`) | **P0** (bersyarat data) |
| `AK-02` | Migrasi data AKM in-place (= `REL-02`). Tambahan: nama unik `exam_violation_uploader_{id}` sama dengan lama dan dipakai `KEEP`; job lama yang tersisa bisa menahan job baru (belum terverifikasi) | **P0** (keputusan) |
| `AK-03` | Media audio/video soal dibuka lewat `ACTION_VIEW` ke URL remote (lama pemutar dalam halaman + cache offline). Keluar app = `app_background`; mode ketat tidak bisa memutar; offline gagal; `localPath` hasil unduh tidak dibaca UI | P1 (P0 bila ujian bermedia) |
| `AK-04` | `show_score` diparse tetapi tidak dipakai: sekolah yang menyembunyikan nilai tidak terhormati (nilai bocor) | P1 |
| `AK-05` | Tidak ada pull-to-refresh/refresh-saat-resume di daftar dan detail: status "Sudah dinilai" baru muncul setelah keluar-masuk | P1 |
| `AK-06` | Pemilih bulan hilang; filter tanggal memakai angka hari tanpa bulan → ujian bulan lain bisa tampil di hari yang salah | P1 |
| `AK-08` | `exam-schedules` tanpa parameter `date/take/skip` (lama berpaginasi); belum terverifikasi dampaknya | P1 (belum terverifikasi) |

P2: pesan galat password per kebijakan (`AK-07`), screenshot callback tidak didaftarkan di `begin()` (`AK-09`), gerbang waktu otomatis tidak muncul lagi saat resume (`AK-10`), "beban soal" per subtes (`AK-11`), pembahasan ditunda (`AK-12`), `fallbackToDestructiveMigration` masih aktif (`AK-13`), judul instruksi lobi mentah (`AK-14`).

### 5.6 Pembayaran

Yang berfungsi: hub (saldo, menu per peran), gerbang aktivasi, aktivasi Klaspay 2 langkah PIN, konfirmasi PIN + "Lupa pin" + deep link reset PIN, **Bayar SPP penuh** (daftar tagihan/sudah dibayar, rincian, metode, PIN, `spp_trx`, panduan, detail, sukses + In-App Review), gate `payment`. Seluruh alur Bayar SPP + Room v11 belum di-commit; kompilasi terbaru belum terverifikasi.

| ID | Selisih | Dampak |
|---|---|---|
| `PB-01` | Top Up (VA/Bank/Retail/E-money, nominal ≥ 20.000, `topup_inq`/`topup_trx`, panduan) belum ada | **P0** |
| `PB-02` | Tagihanku (Menunggu/Dibayar/Detail/Bayar ulang/Batalkan dengan PIN) belum ada | **P0** |
| `PB-03` | SPP bulan `is_inquiry_channel`: "Cek Tagihanku" → dialog "dalam pengembangan" (`B:MainTabsScreen.kt:783-802`) → jalan buntu. Bila produk merilis bertahap, minimal sembunyikan/nonaktifkan invoice non-saldo sampai Tagihanku ada | **P0** |
| `PB-04` | Riwayat Klaspay (paging, detail, poin kampanye) | P1 |
| `PB-05` | QR Pay (pindai QR merchant, `qr_pay`) + My QR | P1 |
| `PB-06` | Toppers (daftar/batal/transfer via ID atau QR) | P1 |
| `PB-07` | PPOB: Pulsa pra/pasca, Listrik, Air, Internet, Game, BPJS + checkout/detail (~17 layar aktif lama) | P1 (P0 bila dianggap inti) |
| `PB-08` | Dana Partisipasi (6 layar aktif) | P1 (P0 bila sekolah memakainya) |
| `PB-09` | Notifikasi `PAYMENT/*` + push `new-spp`/`spp` (lihat §4) | P1 |

P2: guard gate di layar tujuan (`PB-10`), dialog "Sekolah tidak terdaftar" di tab Pembayaran (`PB-11`), deviasi SPP (`PB-12..16`: pesan galat bayar, status non-sukses, baris saldo tak cukup, tanggal tak terbaca, tanpa paste PIN), `PinResultChannel.clear()` tak pernah dipanggil (`PB-17`), kode dummy pembayaran tersisa + 7 rute tak terjangkau (`PB-18`), pemicu otomatis dialog keamanan (`PB-19`).
Bukan gap (tersembunyi/mati di app lama): filter riwayat, Riwayat SPP + detail, WebView payment gateway, promo, periode BPJS, detail kegiatan partisipasi, halaman `Klaspay*` lama, ikon Transfer ("SOON"). Tab "Sudah Dibayar" SPP **aktif** di lama dan sudah ada di baru.

### 5.7 Magang, Verifikasi Data, Komentar/penyuka/tagar

| Modul | Status | Catatan |
|---|---|---|
| Magang (13 file lama) | BELUM ADA (`HOM-01`/`HUB-05`) | P1. Tidak termasuk daftar yang diputuskan tidak dibangun; butuh keputusan lokasi latar (`REL-05`) |
| Verifikasi Data akun (9 file lama) | BELUM ADA (`HOM-02`/`HUB-04`) | P1 |
| Komentar post, daftar penyuka, post per tagar | BELUM ADA (`KOM-01`) | P2. Keputusan A: dibangun, tetapi satu-satunya pintu masuk (push `feed-single/feed-detail`) juga belum ada |

## 6. Sengaja tidak dibangun (bukan gap)

Marketplace/Toko & kewirausahaan, Feed/buat post/jelajah, Pengumuman, layar Chat, Perpustakaan, Prokes, Perangkat Terhubung (`DevicesPage`), Try Out, ujian lama, AKM nasional/Survey/Kelas Eligible, Riwayat SPP, WebView payment gateway, promo, profil publik (`ProfilePage` dkk., tersirat dari Feed). Dua pengecualian yang **tidak** konsisten dengan keputusan dan tercatat sebagai gap: layar Perangkat palsu masih terpasang (`AKN-07`) dan balas-chat-dari-notifikasi belum ada (`NTF-06`).

## 7. Klaim dokumen yang basi atau tidak terbukti di kode

| Dokumen | Klaim | Fakta kode |
|---|---|---|
| 05a §2, FLOW 09-30 #4 | Pull-to-refresh Materi "benar-benar reload ✅" | Memanggil API, tetapi menghapus cache dulu (`MAT-02`), tidak bisa dipakai dari empty state (`MAT-04`), kegagalan tak dilaporkan (`MAT-03`) |
| 05a §2 | `MateriDetailViewModel` "siap untuk FCM deep link" | Route dan VM ada, tidak ada pemanggil (`MAT-05`) |
| 05b §2 | Unduh ke `getExternalFilesDir` | Kode memakai `filesDir`; hanya `DownloadManager` ke `getExternalFilesDir` |
| 05a §3 | Auto-refresh setelah create/edit/delete; filter AND | Terhubung, tetapi juga terpicu saat X (`MAT-14`) dan status paging tak direset (`MAT-25`) |
| 05c §2 | "Filter 'Semua' kirim literal 0", "Edit: tanggal default ke sekarang" | Basi: kode sekarang tidak mengirim 0 dan membaca `end_at_label` (FLOW 10-22) |
| 05c §2 | "Form Buat/Edit/Detail (3 mode)" | Mode Detail tidak pernah dinavigasi (kode mati, `TGS-G-05`) |
| 05c, FLOW 10-02 #8 | "Lihat Jawaban" terhubung untuk siswa bersangkutan | Terhubung ke detail tugas yang sama, bukan jawaban siswa (`TGS-G-04`) |
| 05c | "Beri/Ubah Nilai: … file jawaban" | Hanya teks nama file (`TGS-G-03`) |
| 05 §5.5, 05d | Jawaban terkumpul: cache Room + fallback GET collect | Bentuk data tidak cocok dengan lama (`TGS-S-01`) |
| FLOW 10-22 | `X-App-Version` = `1.0.0` | Kode memakai `VERSION_NAME` = 2.2.84 (risiko di 06d §6 sudah teratasi) |
| 05e §3, FLOW 10-06 #5 | "Alert gate hanya Tutup; tombol Presensi sengaja tidak dibuat" | Tombol "Presensi" sudah ada di list dan layar lapor (`AGD-02`) |
| 05e §3 | "1× fetch saat buka (legacy 2×)" | Benar hanya bila cache kosong; cache hari ini ada → 0 fetch (`AGD-01`) |
| 05g, FLOW 10-12 #1 | Lonceng membaca `klaspayActive` yang disegarkan | `writeCheckAccount` tidak menulisnya (`HUB-06`) |
| 05g, `HubRulesTest` | Badge "99+" dari 99 (legacy) | Legacy mulai 100 (`HUB-08`) |
| 05g §4 | "Dua sumber `isTeacher`" diperbaiki | Masih ada sumber kedua di routing (`HUB-15`) |
| 05g §2 | Menu per peran "sama sejak 05b" | Tidak mencakup `cea243dc` (`HUB-01..03`) |
| 02a/02b, FLOW pembayaran P1 | `is_email_verified` pulih sendiri lewat check-account; sesi dimigrasi | Tidak menulis flag itu (`AUT-04`); migrasi hanya di tingkat file/key, isi JSON tidak kompatibel (`REL-03`) |
| 03 §8 (komentar `NotifService`) | Routing notifikasi seolah ada | Seluruh kode `TaskStackBuilder` dikomentari |
| 06c §5 / 06b | Unduhan media AKM tampak mendukung offline | `localPath` tidak dibaca UI (`AK-03`) |
| 07a §5 | Banner absensi Hub "ditunda" | Sudah ada dan terhubung (`HomeAttendanceBanner.kt`) |
| 07b §1 | `PresensiOffsiteCameraScreen` | Berkas tidak ada; padanannya `CaptureCameraScreen.kt` |
| `01` §3 | Magang, Komentar/penyuka/tagar, balas-chat "dibangun" | Belum ada (`HOM-01`, `KOM-01`, `NTF-06`) |
| FLOW 09-30 D/Q7 | Gate jam "Otomatis" jangan diimplementasikan dulu | `AutoTimeGate` sudah di Asesmen/Jurnal/Presensi/Poin; hanya HomePage yang belum |

Kode ada tetapi **tidak terhubung** (scaffolding): `loadMore()` Tugas guru, mode `UploadTugas(editable=false)`, `show_score`, `AkmMedia.localPath`, `ExamSessionController.beginTemporaryExit/endTemporaryExit`, `AkmSettingsRepository.refresh(force=false)`, `AppErrorHandler` di jalur Materi, `AkunViewModel.fetchPolicy/fetchAbout`, `AkunApiService` (kecuali `updateProfile`/`changePassword`), `SecurityDialogs` di luar `ActivationGate`, `PinResultChannel.clear()`, ±20 endpoint `KlaspayApiService` + seluruh `PpobApiService`/`DanaPartisipasiApiService`/`KartuPelajarApiService`/`MagangApiService`/`SosmedApiService`, `libs.app.update.ktx`, `B:utils/IntentUtil.kt` (626 baris tanpa pemanggil luar).

## 8. Hanya bisa dipastikan lewat uji di perangkat/server

**Pembelajaran**
1. Akun staf non-guru (Janitor/Security): login (parse `data.id` null), isi Hub, Presensi; akun `is_teacher=true` tanpa role Teacher (`HUB-01/02/03`).
2. Bentuk asli JSON list tugas siswa: ada tidaknya `files[]`, `file_student_*`, `student_assignment_id`; bentuk GET `students/{id}/collect/{sa}` (`data.files` vs `student_assignment.files`); path file jawaban relatif atau absolut (`TGS-S-01/02`).
3. Guru dengan > 10 tugas dan > 10 grup penilaian; hasil filter vs data server (`TGS-G-01/02`).
4. Server menerima `file[]` dan `link` kosong tak dikirim; perilaku 400/403/404/413/422; respons 200 dengan `status:"error"`; form mode assigned; migrasi Room 9→10 dengan data nyata.
5. Materi: offline + tarik-untuk-muat-ulang, buka ulang setelah guru menambah materi, backend menghapus link bila `link` tak dikirim, guru > 20 mapel, upload multi-MIME dan > 12 MB, berkas virtual Google Drive, link preview nyata (YouTube/Drive/bit.ly/tanpa `og:image`), PDF besar/berpassword/korup, timeout 30 dtk pada jaringan lambat, buka docx/xlsx/jpg di Samsung/Xiaomi/Infinix.
6. Agenda: skenario basi (buka pagi → Presensi → buka lagi), peta (`MAPS_API_KEY`), Geocoder, izin lokasi fine/coarse/ditolak permanen, lapor masuk/pulang, status "Terlambat".
7. Poin guru: foto kamera pada HP yang belum pernah memberi izin kamera, nama part foto di server, picker jenis, `refreshSelectedScores` saat `id==0`, gerbang Cooperative-only; Poin siswa dengan data detail nyata.
8. Hub: kartu "Kelas Berlangsung" dengan `current_subject` terisi, verifikasi `in_review`/`rejected`, badge 99 vs 100, dialog "Perangkat Lain Terdeteksi" (dua perangkat), dialog akun nonaktif, font scale 1,3/2,0, Android 8.1.

**Lainnya**
9. Update in-place dari APK lama: sesi tetap login, nama/avatar/sekolah/kelas, check-account untuk guru dan siswa tanpa `nis`, `ebede.db` dan `diskola.db` berdampingan, worker lama tidak crash; keystore = kunci unggah Play Console; R8 rilis.
10. Google SSO setelah `WEB_CLIENT_ID` diperbaiki; push FCM latar vs foreground (judul/isi kosong?), ikon/kanal, `logout` dari latar; topik `unlogged`; alur 401 nyata dengan/tanpa ujian pending.
11. AKM: lockdown pada OEM berbeda, pelanggaran nyata (`app_background`, split-screen, `floating_window`, screenshot API 34+, `connection_loss`, `time_tampering`, `not_submitted`), pengiriman ke server, FGS `dataSync` API 34/35, rumus LaTeX pada soal nyata, kebijakan password server, paginasi `exam-schedules`.
12. Pembayaran: seluruh alur SPP dengan wallet aktif dan data tagihan nyata (jangan menjalankan pembayaran sungguhan), bentuk respons `payment/channel/spp`/`spp_trx`, lama kedaluwarsa invoice VA/retail (menentukan lamanya `PB-03` menjebak), migrasi Room 10→11, deep link reset PIN (`autoVerify`, butuh APK rilis + assetlinks), gate `payment` untuk guru.
13. Presensi: dengan akun yang agenda kalendernya ada, radius dalam/luar, Dinas Luar penuh, kirim Izin/Sakit, mock location, QR fisik via Google code scanner.

## 9. Keputusan produk terbuka yang menentukan peringkat

1. **Migrasi database in-place** (`REL-02`/`AK-02`): jawaban ujian yang belum terkirim di `diskola.db` boleh hilang atau tidak? (`FLOW_QUESTIONS.md` bagian B, "Belum tahu".)
2. **Pembatasan staf non-guru** (`HUB-03`/`AUT-06`): ikuti commit lama `cea243dc` (hanya Presensi) atau tetap "tidak menebak dari nama role"? Dua keputusan bertabrakan pada 23-10.
3. **Rilis bertahap pembayaran:** bila Top Up/Tagihanku/PPOB/Dana Partisipasi boleh menyusul, `PB-03` tetap blocker minimal (sembunyikan/nonaktifkan invoice non-saldo atau beri jalan keluar).
4. **Status rilis 2.1.42:** sudah beredar di produksi? Menentukan `PR-01` (P1 atau P2) dan seberapa mendesak `HUB-01`.
5. **Magang dan Verifikasi Data:** dibangun sebelum rilis atau ditunda? Tile/tombolnya saat ini hanya dialog "dalam pengembangan".
6. **Lokasi latar belakang** (`REL-05`): konfirmasi bahwa Presensi/Magang tanpa `ACCESS_BACKGROUND_LOCATION` tetap diterima backend.
7. **`TGS-S-07`:** tombol Download soal menandai "sudah baca" atau tidak.
8. **Kode dummy pembayaran** (`PB-18`) dihapus sekarang atau bersama fase PPOB.

## 10. Urutan perbaikan yang disarankan

Mengikuti preferensi kerja "perbaiki per fitur, pelan-pelan, jangan sapu massal".

**Tahap 0 — putuskan dulu (tanpa kode):** §9 butir 1–5.

**Tahap 1 — blocker murah (≈ satu hari tiap butir, risiko kecil)**
1. `REL-01` ganti `app_name` → "Diskola" (dan ikon adaptif).
2. `AUT-01` pakai `R.string.default_web_client_id` untuk SSO.
3. `HUB-02` + `HUB-01` + `HUB-03` satu paket: `@NullToZeroInt` pada `data.id`, jangan timpa `USER_ID`, simpan `roles[].name`, port `StaffMenuAccess` + notice (setelah keputusan §9.2). Dengan ini `MAT-06`, `TGS-G-12`, `AUT-05` ikut selesai.
4. `TGS-G-01` panggil `loadMore()` guru (Tugas + Penilaian).
5. `POI-G-01` minta izin `CAMERA` sebelum `TakePicture`.
6. `AGD-01` paksa refresh hari ini saat Agenda dibuka.
7. Sembunyikan layar palsu (`AKN-05`, `AKN-06`, `AKN-07`, daftar `MockNotifications`) dan tombol mati sampai fiturnya dibangun.

**Tahap 2 — Pembelajaran (per fitur)**
- **Tugas:** `TGS-G-03` (daftar file jawaban + Baca/Download di dialog nilai), `TGS-S-01` (cocokkan bentuk data jawaban terkumpul dengan response asli), `TGS-G-02`, `TGS-S-03/04/05`, `TGS-G-07`.
- **Materi:** pola cache/refresh `MAT-01/02/04/14/25`, error terlihat `MAT-03/07/08`, mapel per baris `MAT-18`, uji edit link/file `MAT-09`.
- **Agenda/Poin/Hub:** `AGD-02` (lewat gate), `HUB-06`, `HUB-08`, sisanya P2 saat menyentuh fitur terkait.
- Pola yang berulang (cache-first tanpa refresh, error di area scroll) sebaiknya diperbaiki **satu kali** di repository + komponen bersama (`PullToRefreshBox` di dalam empty state, `BannerError` di luar scroll), lalu dipakai Materi dan Tugas.

**Tahap 3 — modul besar yang masih kosong**
- **Notifikasi nyata + `NotifRouter`** (membuka `NTF-01/02/03`, `MAT-05`, `TGS-S-12`, `PR-08`, `PB-09`; tambahkan guard gate bersamaan).
- **Akun fungsional** (`AKN-01/02/03/04`, `AUT-03/04`, kebijakan/tentang).
- **AKM:** `AK-01`, `AK-04`, `AK-05`, `AK-06`, lalu `AK-03` (pemutar media dalam halaman + cache offline).
- **Pembayaran:** `PB-02` (Tagihanku, sekaligus menutup `PB-03`), `PB-01` (Top Up), `PB-04` (Riwayat), `PB-05` (QR), `PB-07`/`PB-08` sesuai keputusan produk.
- **Verifikasi Data, Magang, komentar/penyuka/tagar** sesuai keputusan §9.5.

## Lampiran — deviasi disengaja yang terverifikasi di kode (bukan gap)

401 terpusat + logout lewat satu fungsi dengan blokir ujian pending (diperluas ke FINISHED-belum-terunggah); topik FCM satu titik; FCM `logout` menavigasi ke Login; tombol "daftar" SSO menunggu sekolah dipilih; log rilis tanpa body/PIN/token; Hub dimuat ulang paling cepat tiap 30 dtk; Poin: sukses hanya bila API sukses, blokir target bukan Student, tanpa cache Room siswa; Agenda: submit nonaktif sampai ada fix GPS; Materi: refresh benar-benar memuat ulang, filter AND, target wajib, validasi 12 MB saat memilih berkas, PDF tanpa pinch-zoom; Tugas: filter "Semua" tidak mengirim 0, `end_at` diprefill dari label, syarat baca/upload reaktif, banner error saat fetch gagal, akses guru hanya dengan data guru, multi-kelas ditunda; Presensi: tanpa geofence/izin lokasi latar, deteksi lokasi palsu ditambahkan; Jurnal: pemindai QR Google code scanner, tanpa cache jadwal; Pembayaran: SALIN menyalin `total_amount`, pesan galat bayar dari server, cache panduan per channel, ikon Transfer tidak dibangun.
