# Flow Questions — Keputusan yang Perlu Dikonfirmasi ke User

Log pertanyaan/ambiguitas alur yang ditemukan saat migrasi/pengembangan, beserta jawaban user, sesuai
`docs/rules-global.md` §2 ("jangan diam-diam diperbaiki/diasumsikan, tanyakan dulu").

---

## 2026-09-11 — "AKM Pemerintah" di fitur Asesmen

**Konteks:** Saat mengonversi fitur Asesmen dari mock ke API asli, dokumentasi kontrak lama
(`docs/repo lama/api/04-pembayaran-klaspay-ppob-akm.md:558-987`) mencatat dua kelompok endpoint
terpisah dari `ApiService.kt` app lama:
- `mobile/app/learning/akm/schedules` + `akm/scored` (`listAkm`/`listScoreAkm`) — contoh data
  `"AKM Literasi Membaca"` (nama subtes AKM nasional).
- `mobile/app/learning/akm/exam-schedules` + `exam-schedules-scored` (`listUjian`/`listUjianScored`)
  — ujian buatan sekolah.

Agent (tanpa konfirmasi ke user) menyimpulkan kelompok pertama sebagai fitur "AKM Pemerintah" dan
membangun UI dua-jalur (tab/badge/ikon berbeda, field `government: Boolean` di seluruh model &
ViewModel) berdasarkan itu. String "AKM Pemerintah" sendiri **tidak pernah muncul** di dokumentasi
manapun (`docs/repo lama/asesmen/*.md` maupun `docs/repo lama/api/*.md`) — murni istilah karangan
agent.

Saat endpoint `akm/scored` dites ke backend dev project ini, hasilnya 404 (`page not found`).

**Pertanyaan ke user:** apakah fitur "AKM Pemerintah"/AKM nasional memang termasuk scope project ini,
atau cukup "Asesmen Sekolah" saja?

**Jawaban user:** "memangnya ada akm pemerintah?" → pilih **hapus total, fokus Asesmen Sekolah saja**.

**Tindakan:** Seluruh jalur `akm/schedules`, `akm/scored`, `akm/schedules/{id}`,
`akm/student/exam/{id}/download`, `akm/student/exam/{id}/check-password`,
`akm/student/exam/{akm_id}/answer/{student_id}`, `akm/student/exam/{id}/review` dihapus dari
`AsesmenApiService.kt`. Field `government`/`kind` dihapus dari `AkmItem`/`AkmScoreItem`
(`AkmUiModels.kt`), `governmentFlags`/`isGovernment()` dihapus dari `AkmViewModel.kt`. Badge/ikon UI
disederhanakan jadi selalu "Asesmen Sekolah" (`AkmScreen.kt`, `AkmDetailScreen.kt`).

**Kalau nanti backend memang punya AKM nasional:** endpoint-nya masih terdokumentasi lengkap di
`docs/repo lama/api/04-pembayaran-klaspay-ppob-akm.md:558-654` kalau suatu saat perlu ditambahkan
kembali — jangan dikarang ulang dari nol.

---

## 2026-09-30 — Keputusan atas seluruh dokumen migrasi `docs/migrasi/` (auth, cakupan modul, AKM, presensi, keuangan)

**Konteks:** Setelah `docs/migrasi/00`–`10` selesai ditulis (spesifikasi perilaku `android-portal`
v2.1.40 untuk migrasi ke Compose), setiap dokumen mencantumkan daftar "❓ Perlu keputusan user" untuk
bug/perilaku ambigu yang **tidak boleh diperbaiki diam-diam** (`docs/rules-global.md` §2). User
menjawab seluruh daftar tersebut dalam satu sesi. Jawaban di bawah ini adalah **rujukan final** —
dokumen `docs/migrasi/*.md` sudah/akan diperbarui untuk mencerminkannya (baris "**Keputusan
(30-09-2026):**" di tiap dokumen menunjuk balik ke entri ini).

### A. Cakupan modul (`01-peta-fitur-dan-rencana.md` §4)

**Pertanyaan:** modul yang di v2.1.40 tidak terjangkau pengguna (Toko/KWU marketplace, feed sekolah +
buat post/e-book + jelajah, pengumuman, layar Chat, Perpustakaan, Prokes, Perangkat Terhubung/drawer
Home) — ikut dibangun di app baru atau tidak?

**Jawaban user:** **Tidak** — jangan dibangun.

**Tindakan:** Modul-modul di atas **tidak masuk cakupan kerja** sampai ada keputusan baru. Titik
masuk untuk modul ini **tidak dibuat** di app baru (paritas dengan v2.1.40, yang juga tidak
menyediakan titik masuk untuk modul-modul ini). Pengecualian yang **tetap dibangun** karena memang
terjangkau di v2.1.40: `CommentPage`/`ListLikePage`/`HashtagPostPage` (dari notifikasi & profil),
notifikasi chat + balas-dari-notifikasi (`DirectReplyChat`, tanpa layar `ChatPage` baru), Verifikasi
data akun. Lihat `01-peta-fitur-dan-rencana.md` §3–§4 untuk daftar lengkap per status.

### B. Migrasi sesi & database saat update in-place (`01` §5, `10` §1.3/§5.6)

**Pertanyaan:** app baru akan dirilis sebagai *update* `id.diskola.app` di Play Store (menimpa app
lama) — apakah SharedPreferences & Room (`diskola.db`, termasuk jawaban AKM yang belum terunggah)
milik app lama dibaca ulang, atau dihapus bersih?

**Jawaban user:** **Belum tahu** — bagian *database* (Room/`diskola.db`, sinkronisasi AKM) masih
terbuka, perlu diputuskan lagi sebelum fase rilis. (Bagian **sesi login** sudah diputuskan terpisah,
lihat butir C/Q9 di bawah — itu **sudah final**: migrasi, bukan paksa login ulang.)

**Tindakan:** Tandai eksplisit di `01-peta-fitur-dan-rencana.md` §5 dan `10-infrastruktur-api-data-latar.md`
§1.3/§5.6 sebagai **belum diputuskan** — jangan diasumsikan salah satu arah saat implementasi
menyentuh Room/`diskola.db`. Tanyakan lagi mendekati fase rilis.

### C. Auth (`02-auth-login-sesi.md` §14, Q1–Q10) — dijawab berurutan

| # | Temuan (ringkas) | Jawaban | Keputusan final |
|---|---|---|---|
| Q1 | Dialog "lanjutkan sebagai tamu" di login Google mungkin muncul untuk *semua* akun (race condition `postValue`) | **a) Tiru persis** | Replikasi persis logika lama apa adanya (termasuk race condition-nya) — dialog muncul kapan pun perangkat lama memunculkannya. **User belum sempat mengecek di HP kapan persisnya itu terjadi** — ini jadi tugas verifikasi teknis saat implementasi (buka app lama di HP, login Google, amati), bukan lagi pertanyaan terbuka. |
| Q2 | `ssoSchool` bisa berupa objek kosong (bukan null) → tombol "daftar" aktif walau sekolah belum dipilih | **b) Wajib pilih sekolah dulu** | **Deviasi dari kode lama (disengaja):** tombol "daftar" baru aktif setelah `ssoSchool` valid (uuid tidak kosong). |
| Q3 | Pencarian sekolah di form login hanya memfilter cache lokal (param `name` API tak dipakai) | **a) Tiru** | Pertahankan: pencarian sekolah tetap hanya menyaring hasil yang sudah ter-cache secara lokal. |
| Q4 | Gerbang banner FIAM bisa menahan splash tanpa jalan keluar bila banner tidak diklik | **a) Tiru** | Pertahankan perilaku gerbang FIAM apa adanya. |
| Q5 | Jalur SSO tidak menulis `onklas_lite`/`onklas_pro`/`klastime`, tidak me-reset `default_pass` | **a) Tiru** | Pertahankan; jangan ditambah penulisan default yang tidak ada di kode lama. |
| Q6 | Tidak ada penanganan 401 global; `PembayaranPage` melakukan logout untuk **semua** `errorString` | **c) 401 terpusat** | **Deviasi dari kode lama (disengaja):** implementasikan satu penanganan 401 terpusat (interceptor/event bus → alur "Sesi Berakhir" → logout), bukan ditiru per-layar. Logout yang dipicu jalur ini **tetap wajib lewat prosedur logout yang sama** (termasuk pengecekan blokir ujian AKM, lihat butir E/#2) — 401 **tidak boleh** langsung menghapus seluruh database tanpa pengecekan itu (lihat `02a` R10). |
| Q7 | Topik FCM di-subscribe/unsubscribe tersebar & tidak konsisten antar layar | **b) Atur di satu tempat** | **Deviasi dari kode lama (disengaja):** kelola seluruh subscribe/unsubscribe topik FCM dari satu titik berdasarkan status sesi (login/logout), bukan tersebar di `HomePage`/`AkunPage2`/dst. Daftar topik & payload tetap mengikuti dokumentasi (paritas kontrak dengan backend), yang berubah hanya *di mana* kode itu dieksekusi. |
| Q8 | Notifikasi FCM menu `"logout"` membersihkan data di background **tanpa** membawa pengguna ke layar Login | **b) Sekaligus navigasi ke Login** | **Deviasi dari kode lama (disengaja):** setelah data dibersihkan, app juga menavigasi ke layar Login (mis. lewat `Intent` `NEW_TASK\|CLEAR_TASK` ke Activity utama), bukan diam di layar sebelumnya dengan sesi kosong. |
| Q9 | Migrasi sesi (SharedPreferences) app lama → app baru saat update in-place | **a) Migrasi (disarankan)** | **Final:** migrasikan sesi (baca ulang key SharedPreferences lama, format `SharedPreferencesMigration` dengan nama key yang sama) supaya pengguna tetap login setelah update. (Migrasi *database* Room tetap terpisah & belum diputuskan — lihat butir B.) |
| Q10 | Tampilan sistem Credential Manager berbeda dari Smart Lock lama | **ya** | Diterima — tidak bisa dihindari (satu-satunya opsi yang tersedia). |

### D. Home/shell (`03-home-navigasi-akun-notifikasi.md` §14, Q6–Q7)

**Belum diputuskan** — tetap terbuka:
- Q6: Izin notifikasi Android 13+ yang praktis wajib (dialog non-cancelable diulang bila ditolak) — dipertahankan atau boleh ditolak permanen?
- Q7: Dialog wajib "atur tanggal & waktu otomatis" yang menutup aplikasi bila ditolak (`"Jangan Ubah"`) — dipertahankan?

**Tindakan:** jangan diimplementasikan salah satu arah dulu untuk dua poin ini; tanyakan lagi sebelum
menyentuh bagian ini (`HomePage`/gerbang global di dokumen 03 §2.4).

### E. Asesmen/AKM (`06-asesmen-akm-tryout-ujian.md` §23.3, #1–#2)

**Jawaban user:** "jangan logout, berikan nilai default false" — dipetakan ke dua temuan:

1. **`exam_lock_mode` default saat key tidak dikirim server** → **nilai default tetap `false`** (mode
   ketat OFF), sesuai perilaku aktual kode sekarang. Bukan bug yang diperbaiki — dijadikan aturan
   resmi: kalau backend tidak mengirim `exam_lock_mode`, mode ketat **tidak** aktif.
2. **Logout tidak diblokir saat ada ujian berstatus FINISHED yang masih antre unggah offline** →
   **"jangan logout"**: perbaiki (bukan ditiru) — **perluas** aturan blokir logout (dokumen 06 §15,
   sebelumnya hanya untuk ujian yang belum dikumpulkan/diunduh) supaya **juga memblokir** logout
   ketika ada ujian FINISHED yang jawabannya belum terkonfirmasi terunggah ke server. Ini berlaku di
   SEMUA jalur logout (drawer Home, tab Akun, 401 terpusat butir C/Q6, FCM `"logout"` butir C/Q8) —
   satu fungsi logout, satu pengecekan.

### F. Presensi (`07-presensi-magang-prokes-verifikasi.md` §19, Q1 & Q5)

| # | Temuan | Jawaban | Keputusan final |
|---|---|---|---|
| Q1 | Lokasi latar belakang (background location, API≥29) selalu diwajibkan di tab Data Absensi & Magang walau sekolah tidak membatasi radius | **ya** | Pertahankan — tetap selalu wajib, sama seperti kode lama. |
| Q5 | Tidak ada deteksi mock location/root di presensi | **ya** | **Tambahan baru (bukan ada di kode lama):** implementasikan deteksi mock location (dan idealnya deteksi root) sebelum presensi GPS diterima. Ini **mengubah perilaku** dibanding app lama secara sengaja (fitur keamanan tambahan), dampak ke UX perlu didesain (pesan penolakan, dsb.) saat implementasi. |

### G. Keuangan/Klaspay/PPOB (`08-keuangan-pembayaran-klaspay-ppob.md` §18, Q3, Q8, Q10, Q14)

| # | Temuan | Jawaban | Keputusan final |
|---|---|---|---|
| Q3 | Tombol "SALIN" di panduan pembayaran menyalin `amount` (tanpa biaya admin), padahal yang ditampilkan `total_amount` | **tidak** (jangan tiru bug) | **Perbaiki:** salin `total_amount` (nominal + biaya admin), bukan `amount`. |
| Q8 | `transfer_trx` mengirim `nominal=0`; halaman sukses tetap tampil walau status transaksi bukan sukses | **jangan (tiru); "gagal ya gagal"** | **Perbaiki:** kirim `nominal` transfer yang sebenarnya, dan **tampilkan halaman gagal** bila status transaksi memang gagal — jangan menampilkan halaman sukses untuk transaksi yang gagal. |
| Q10 | Notifikasi `PAYMENT/SPP`+`child_id` membuka DialogFragment sebagai Activity → crash; `PAYMENT/BILL`+`child_id` membuka layar kosong | **(belum dijawab eksplisit)** | **Asumsi kerja (perlu konfirmasi ulang):** karena ini crash/layar-kosong (bukan soal preferensi UX) dan pola jawaban lain di grup ini konsisten "perbaiki, jangan tiru bug", diasumsikan sementara **opsi (a) — perbaiki**: arahkan ke `SppPaymentPage` / `PaymentDetail(childId)` yang benar. **Tandai sebagai asumsi, bukan keputusan final** — konfirmasi ulang ke user sebelum menutup item ini. |
| Q14 | PIN & body HTTP (termasuk header `Authorization`) ikut tercatat di log build **release**, sebagian ter-upload ke Firebase Storage (`10` §9.2 #10) | **ya** (perbaiki) | **Perbaiki:** matikan logging body/BODY level HTTP dan Timber `DebugTree` di build release; jangan sertakan data sensitif (PIN, token) di file log yang di-upload. Ini murni perbaikan keamanan, tidak mengubah UX yang terlihat pengguna. |

### H. Aturan kerja tambahan (berlaku untuk semua fitur mulai sekarang)

User menambahkan dua aturan kerja permanen, sudah dituliskan sebagai **`docs/rules-global.md` §5**:
1. Semua kode wajib benar-benar berfungsi di **Android 8.1 / API 27** (`minSdk` project), tidak cukup
   hanya lolos compile atau teruji di Android versi baru — sudah pernah ada kasus nyata gagal di
   Android lama walau berhasil di versi baru.
2. Struktur kode **tidak boleh "spaghetti"** — pemisahan lapisan UI/ViewModel/Repository harus rapi
   dan konsisten, mudah dikembangkan lebih lanjut (detail lihat `rules-global.md` §5.2).

---

## 2026-09-30 — Implementasi fase auth (login inti + logout): keputusan tambahan

**Konteks:** Menyamakan alur login `Diskola-App-New` dengan `android-portal` (dokumen 02, gap 02a).
Beberapa titik butuh keputusan yang belum eksplisit tercatat di dokumen 02/02a — dijawab user
sebelum implementasi, dicatat di sini sesuai `rules-global.md` §2.

| # | Temuan | Jawaban | Keputusan final |
|---|---|---|---|
| 1 | SSO: siswa/guru dengan `data.school == null` (bukan hanya tamu) tetap melihat tombol "iya, itu saya" tanpa pemilih sekolah di kode lama, lalu mengirim `school_id` kosong ke `login-sso/school` | **Wajib pilih sekolah dulu** | **Deviasi disengaja:** kondisi tampil pemilih sekolah + "daftar" tetap sama seperti kode lama (`school.name` kosong), tapi tombol "daftar" baru aktif setelah sekolah dipilih (sejalan dengan keputusan Q2 di dokumen 02 — `SsoScreen.kt`). Peran (siswa/guru/tamu) yang ditulis ke pref **tidak berubah** karena keputusan ini — tetap mengikuti `is_student`/`is_teacher` dari `login-sso`, bukan dipaksa jadi tamu. |
| 2 | Dialog konfirmasi tamu: keyboard Android otomatis membuat huruf pertama kapital, padahal frasa `lanjutkan sebagai tamu dulu bukan siswa/guru` dicocokkan case-sensitive | **Matikan auto-kapital** | **Deviasi kecil disengaja:** input field dialog tamu diberi `KeyboardCapitalization.None`. Pencocokan tetap case-sensitive dan frasanya sama persis — hanya keyboard yang tidak lagi mengubah huruf pertama. (Catatan implementasi: `AppTextField`/`KeyboardOptions` di app baru sebenarnya sudah default `None` sejak awal, jadi field lain tidak terpengaruh; parameter `capitalization` ditambahkan eksplisit agar niatnya tercatat di kode.) |
| 3 | Blokir logout karena ujian AKM belum dikumpulkan (doc 02 §10.1, diperluas oleh keputusan bagian E di atas): jawaban ujian offline belum ada tabelnya di app baru, yang ada baru `akm_synced_exam` (status unduhan) | **Pakai `akm_synced_exam`** | **Keputusan sementara sampai fase AKM (5) selesai:** logout diblokir bila ada baris `akm_synced_exam` berstatus `DOWNLOADED` yang belum ditandai `SUBMITTED`. `AkmViewModel.submitExam`/`reuploadAnswer` sukses → tandai `SUBMITTED`. Ini bukan pengganti penuh query lama (`status < 3` di tabel jawaban ujian) — begitu database jawaban AKM offline ada (masih terbuka, lihat bagian B), pengecekan ini harus disesuaikan lagi. |

**Implementasi:** lihat `SessionStore`/`SessionManager`/`FcmTopicManager` (`app/src/main/java/id/diskola/app/utils/session/`), `LoginViewModel`/`SsoViewModel`/`ResetPasswordViewModel`/`KlaspayActivationViewModel`/`SplashViewModel` (`viewmodel/`), dan layar-layar di `ui/screens/auth/`. Laporan lengkap per fase kerja + hasil uji di device fisik: `docs/migrasi/02b-implementasi-auth-login-logout.md`.

**Belum dikerjakan di fase ini (jangan diasumsikan selesai):**
- Gerbang banner Firebase In-App Messaging di splash (doc 02 §2.3, Q4) — `SplashViewModel` langsung lanjut ke router setelah cek versi, tanpa menunggu FIAM.
- Extra FCM `page`/`goto` saat app dibuka dari notifikasi (doc 02 §2.1 langkah 1, gap S4) — bagian dari routing notifikasi di dokumen 03.
- Dialog keamanan pasca-login (email belum terverifikasi, `EmailConfirmDialog`), validasi sesi berkala (`current-user`, cek `is_active`) — dokumen 02 §8/§9, dikerjakan bersama fase Home (dokumen 03).
- `versionCode`/keystore rilis, migrasi database Room `diskola.db` (bagian B di atas, masih terbuka).

---

## 2026-09-30 — Fase Pembelajaran: Hub + Materi: keputusan tambahan

**Konteks:** Lanjutan dari fase auth — membangun Hub Pembelajaran (doc 05 §1) dan mengaudit/memperbaiki
Materi siswa & guru (doc 05 §2–4). Beberapa titik butuh keputusan sebelum implementasi, dijawab
user, dicatat di sini sesuai `rules-global.md` §2.

| # | Temuan | Jawaban | Keputusan final |
|---|---|---|---|
| 1 | Cakupan fase "halaman pembelajaran" terlalu luas untuk satu sesi (Hub + Materi + Tugas + Perpustakaan + Agenda Mingguan + Poin) | **Hub + audit Materi dulu** | Cakupan dipersempit ke Hub Pembelajaran (menu grid) + Materi siswa & guru (termasuk viewer PDF, link preview). Tugas/Perpustakaan/Agenda Mingguan (isi fitur)/Poin tetap fase terpisah, sesuai `01-peta-fitur-dan-rencana.md` §6. |
| 2 | Materi siswa sedang memakai endpoint tak terdokumentasi (`mobile/student/school-subject...`) padahal endpoint sesuai dokumen (`mobile/app/learning/theories/students/subjects...`) sudah ada di kode sebagai placeholder tak terpakai | **Ganti ke endpoint sesuai dokumen, verifikasi di device** | Endpoint diganti total (bukan dipertahankan sebagai fallback), diverifikasi di device fisik terhadap `dev.api.diskola.id` — lihat `05b` §5. |
| 3 | Kedalaman pengerjaan cache/paging Materi, viewer PDF, dan link preview — bisa jalan pintas (skip cache, delegasikan PDF ke app eksternal, biarkan link preview placeholder) atau dibangun penuh | **Bangun penuh** (cache+paging Room 20/halaman, viewer PDF internal sederhana tanpa pinch-zoom, link preview sederhana tanpa dependensi baru) | Ketiganya dibangun penuh mengikuti pola yang sudah terbukti di fase auth (`SchoolRepository`). Detail arsitektur di `05b` §2. |
| 4 | Empat bug perilaku di Materi legacy: pull-to-refresh yang tidak benar-benar reload; "Materi Saya" guru tidak auto-refresh setelah create/edit; filter Kelas/Mapel guru saling eksklusif (bukan AND) dan tidak saling reset; form upload mengirim `grade="null"` diam-diam bila target tak dipilih | **Perbaiki semua (bukan tiru)** | Keempatnya diperbaiki, bukan direplikasi. Detail per bug di `05a` §2–3 dan `05b` §1. |
| 5 | Kontrol navigasi lompat-halaman di viewer PDF legacy sudah mati (listener dikomentari, tombol/label tidak berfungsi) | **Sembunyikan saja** | Kontrol itu tidak dibangun ulang maupun ditiru sebagai dekorasi mati — `PdfViewerScreen` hanya scroll biasa, tanpa kontrol halaman. |
| 6 | Badge notifikasi Hub (rantai `payment/wallet`→`notification/summary`) dan badge Agenda Mingguan guru (`attendance/staff/agendas/today`) ternyata butuh endpoint yang **tidak ada sama sekali** di app baru (dikonfirmasi lewat pencarian seluruh repo, bukan cuma belum di-type) — beda dengan Materi yang endpointnya sudah ada sebagai placeholder | **Bangun yang berisiko rendah dulu; badge ditunda** | Hub dibangun untuk bagian yang datanya sudah tersedia/berisiko rendah (grid menu, locked gating, dialog, popup tamu, kartu "Kelas Berlangsung" nyata via endpoint yang sudah ada, cek akun nonaktif). Badge notifikasi & Agenda Mingguan ditunda ke fase lanjutan — endpoin harus dibangun dari nol dan diverifikasi kontraknya dulu, dan badge Agenda Mingguan juga butuh akun guru untuk diuji (tidak tersedia sesi ini). |

**Bug tambahan ditemukan saat implementasi (bukan hasil pertanyaan ❓, langsung diperbaiki karena
bukan ambigu — lihat `05b` §3 untuk detail):** `SessionStore.writeCheckAccount()` tidak pernah
menulis `is_student`/`is_teacher`/`is_having_class`/`roles`, hanya `writeLoginAccount()` (bug fase
auth yang baru kelihatan sekarang karena Hub-lah pemakai pertama `is_having_class` yang terlihat
pengguna); dan fetch ganda `check-account` di `HomeScreen` akibat `LaunchedEffect(Unit)` dan
observer `ON_RESUME` sama-sama terpicu saat pertama tampil.

**Implementasi:** lihat `repository/MateriRepository.kt`, `repository/FileOpenRepository.kt`,
`repository/LinkPreviewRepository.kt`, `viewmodel/HomeViewModel.kt`,
`viewmodel/Materi*ViewModel.kt`, `ui/screens/home/HomeScreen.kt`, `ui/screens/materi/`. Laporan
lengkap: `docs/migrasi/05b-implementasi-hub-materi.md`. Status per item: `docs/migrasi/05a-gap-pembelajaran-hub-materi.md`.

**Belum dikerjakan di fase ini (jangan diasumsikan selesai):**
- Badge notifikasi Hub dan badge Agenda Mingguan guru (endpoint belum ada sama sekali).
- Mesin status persetujuan penuh (`APPROVED`/`CLEAR`/`IN_REVIEW`/`REJECTED`) untuk tombol verifikasi
  — dipakai versi sederhana (`isGuest || !isHavingClass`).
- `VerificationPage` sungguhan — dokumen 07.
- Seluruh alur Materi Guru, popup tamu, dialog akun nonaktif, link preview — kode ada tapi belum
  diuji di device (tidak ada akun guru/tamu tersedia sesi ini).
- Tugas, Perpustakaan, Agenda Mingguan (isi fitur), Poin — fase terpisah.

---

## 2026-10-02 — Fase Tugas (Siswa + Guru): keputusan tambahan

**Konteks:** Lanjutan setelah Hub+Materi — membangun modul Tugas (doc `05` §5-6) sekaligus sisi
siswa dan guru, mengikuti persis pola arsitektur Materi. Beberapa titik butuh keputusan sebelum/
selama implementasi, dijawab user, dicatat di sini sesuai `rules-global.md` §2.

| # | Temuan | Jawaban | Keputusan final |
|---|---|---|---|
| 1 | Layar Detail Tugas app lama memuat data **hanya** dari cache Room — tugas yang belum pernah ter-load (mis. dibuka dari notifikasi sebelum list dibuka) tampil **kosong tanpa pesan**, dan tidak ada endpoint detail-by-id yang bersih untuk memperbaikinya sepenuhnya | **Panggil ulang daftar tugas dengan filter seperti Materi** | Saat `TugasDetailScreen` dibuka dan id tidak ada di Room, `TugasDetailViewModel` memanggil `ensureBacklog/Done/ScoredFirstPage()` (siswa) atau `ensureTeacherOwnFirstPage()` (guru) yang relevan dulu — mirip `ensureFirstPage` Materi — baru cek Room lagi. Masih tidak ketemu → dialog "Tugas Tidak Tersedia", bukan layar kosong. |
| 2 | Syarat "baca soal"/"upload jawaban" app lama sekali terpenuhi **tidak pernah** kembali ke status belum, walau siswa menghapus semua file jawaban/mengosongkan link | **Perbaiki, jadi reaktif** | `uploadDone`/`readDone` dihitung ulang dari status file/link saat ini, bukan flag permanen. Menghapus semua file & mengosongkan link mengembalikan status ke "belum" (termasuk menghapus cache `homework_answer_file` yang sudah pernah dikirim). |
| 3 | Form edit tugas (guru) app lama mengisi ulang "Batas pengumpulan" dengan **label tampilan server** (mis. "2 hari lagi"), bukan tanggal/jam asli yang bisa diproses ulang — berisiko mengirim string salah format kalau guru tidak mengubah tanggal | **Default ke sekarang, wajib pilih ulang** | Mode edit **selalu** men-default `endAt` ke `LocalDateTime.now()`, tidak pernah mencoba mengurai `end_at_label`. Guru wajib memilih ulang tanggal/jam yang benar sebelum menyimpan. |
| 4 | Daftar Tugas (3 tab siswa, List Tugas & Penilaian guru) app lama **tidak** menampilkan pesan apa pun kalau fetch gagal karena jaringan (beda dari layar Detail yang sudah menampilkan error) | **Tambahkan banner error** | Semua ViewModel list Tugas memakai `launchWithHandling(showError=true)`, `BannerError` tampil di semua layar list — deviasi disengaja dari app lama demi UX yang lebih jelas. |
| 5 | Syarat "baca soal" app lama tetap terpenuhi walau dialog izin akses file dibatalkan, asalkan file sempat terunduh sebelumnya | **Perketat** (bukan rekomendasi "pertahankan") | `readDone` di-set true **hanya** di jalur sukses `openFile()` (download benar-benar selesai) — tidak ada kondisi "dialog dibatalkan tapi tetap dihitung". Ini otomatis didapat dari pola dialog-gated-download yang sama dengan Materi, tidak perlu logic tambahan. |
| 6 | Filter guru "Semua" app lama mengirim literal `0` ke server untuk `school_classes_id`/`school_subject_id` (belum terverifikasi backend menanganinya dengan benar) | **Tiru app lama, kirim `0`** (bukan rekomendasi "hilangkan parameter") | `TugasRepository.fetchTeacherOwnPage()` selalu mengirim kedua filter termasuk nilai `0` untuk "Semua" — persis app lama. Belum diverifikasi ke backend asli (tidak ada akun guru sesi ini). |
| 7 | List Tugas guru app lama fetch 3x setiap kali layar dibuka (bug: observer filter ikut terpicu bersamaan dengan fetch awal) | **Perbaiki jadi 1x fetch** | `TugasGuruViewModel` memanggil `ensureFirstPage()` sekali per perubahan filter, bukan 3x saat buka layar — konsisten `rules-global.md` §1. |
| 8 | Tombol "Lihat Jawaban" di "Tugas Terkumpul" (guru) app lama **tidak terhubung ke apa pun** (listener kosong, tombol terlihat tapi tidak berfungsi) | **Hubungkan ke detail tugas** | Tombol membuka Detail Tugas mode guru (read-only) untuk siswa yang bersangkutan — fungsinya jadi nyata, bukan tombol mati yang direplikasi. |

**Temuan teknis tambahan saat implementasi (bukan ❓, langsung diperbaiki karena bukan ambigu):**
`TugasApiService.collectAssignment` (scaffold sebelumnya) hanya mendukung satu file jawaban,
padahal spesifikasi jelas minta multi-file (`file[]`) — diubah ke `List<MultipartBody.Part>`;
form buat/edit tugas app lama (`CreateHomeworkVm.init`) mengambil daftar hari tanpa `try/catch`
sama sekali (kegagalan jaringan = crash) — dibungkus `launchWithHandling` di app baru.

**Implementasi:** lihat `repository/TugasRepository.kt`, `viewmodel/Tugas*ViewModel.kt`,
`viewmodel/UploadTugasViewModel.kt`, `ui/screens/tugas/`. Laporan lengkap:
`docs/migrasi/05d-implementasi-tugas.md`. Status per item: `docs/migrasi/05c-gap-tugas.md`.

**Belum dikerjakan/diuji di fase ini (jangan diasumsikan selesai):**
- **Tidak ada satu pun tugas di akun uji** — ketiga endpoint siswa terverifikasi 200 OK tapi selalu
  kosong, jadi seluruh bentuk JSON `HomeworkItem`/`HomeworkCollected`/`Assignment` belum pernah
  divalidasi terhadap response asli (beda dari fase Materi yang sempat menemukan & memperbaiki
  ketidakcocokan field nyata — di sini risiko itu belum bisa dicek sama sekali).
- Seluruh sisi guru (List Tugas, buat/edit/hapus, Penilaian, Tugas Terkumpul, Beri Nilai) — tidak
  ada akun guru tersedia sesi ini.
- Kontrak multipart `file[]` untuk kirim jawaban — belum pernah dicoba ke backend asli.
- Notifikasi/deep-link ke Tugas (FCM `task`/`penilaian`) — belum ada `NotifRouter` equivalent di
  app baru sama sekali, di luar cakupan sampai infrastruktur notifikasi dibangun.
- Perpustakaan, Agenda Mingguan (isi fitur), Poin — fase terpisah.

---

## 2026-10-02 — Bug nyata ditemukan & diperbaiki: `teacher_id` selalu 0 setelah login guru

**Konteks:** Akun guru nyata (NISN `20202620`, SMK Demo Surabaya) baru tersedia belakangan di fase
Tugas ini (lihat entri di atas, yang masih menulis "tidak ada akun guru tersedia sesi ini"). Dengan
akun ini, ditemukan bug nyata (bukan gap verifikasi) yang sebelumnya tidak mungkin terdeteksi.

**Gejala:** Guru membuat tugas baru lewat form → create sukses (200 OK) → tugas tidak muncul di
"List Tugas" meski GET backlog juga sukses (200 OK, 22KB body). `teacherAssignmentSchedule` juga
sempat 422 karena query param salah (`class` → `school_class_id`, sudah diperbaiki, lihat `05d` §4
poin 4).

**Root cause (dikonfirmasi lewat `adb logcat` + re-login terkontrol, bukan dugaan):**
`SessionStore.writeLoginAccount()` menulis `TEACHER_JSON` dari `data?.teacher?.id`, tapi response
asli `POST login-account` untuk login guru **tidak pernah menyertakan field `data.teacher` sama
sekali** — field itu hanya ada di response `POST check-account` (`data.teacher = {id, name, nip}`).
Akibatnya setiap login guru baru menulis `teacher.id = 0` ke shared_prefs, dan `TugasGuruViewModel`/
`MateriGuruViewModel` (yang membaca `sessionStore.teacher?.id`) memfilter request ke server dengan
`teacher_id=0` — kosong atau salah, bergantung bagaimana backend menginterpretasikan `teacher_id=0`.

**Perbaikan:** `SessionStore.writeCheckAccount()` sekarang juga menulis `TEACHER_JSON`, bersumber
dari `response.data?.teacher` (field yang memang selalu ada di response ini) — mengikuti pola
penulisan `STUDENT_JSON` yang sudah ada di fungsi yang sama. `writeLoginAccount()`'s cabang guru
dipertahankan sebagai mapping defensif (untuk kalau backend suatu saat menambah field ini di
login-account) tapi diberi guard `data?.teacher != null` supaya tidak lagi diam-diam menimpa
dengan `0`. Field dibetulkan dari `nik` → `nip` (nama asli field backend) di `SessionTeacher` dan
`TeacherData` (sebelumnya hanya punya `id`, sekarang juga `name`/`nip`).

**Verifikasi di device:** setelah fix, `teacher` di shared_prefs menjadi
`{"id":10804,"nip":"20202620","name":"Guru DEV"}` segera setelah Home memanggil `check-account`
(dipanggil ulang setiap Home landing/resume, lihat doc comment `writeCheckAccount`), dan
`GET .../teachers/backlog` membawa `teacher_id=10804` — List Tugas guru yang sebelumnya kosong
sekarang menampilkan seluruh tugas nyata milik guru tsb.

**Dampak ke dokumentasi lain:** `05d-implementasi-tugas.md` §4 (poin 6) dan §5a diperbarui dengan
detail lengkap + hasil verifikasi. Ini juga retroaktif menjelaskan kenapa `MateriGuruViewModel`
(fase sebelumnya) tidak pernah sempat diuji benar-benar dengan `teacher_id` yang valid.

---

## 2026-10-06 — Fase Poin (siswa + guru) + Agenda Mingguan (guru): keputusan

**Konteks:** Sisa Fase 4 peta migrasi. Dua modul dikerjakan sekaligus atas pilihan user. Detail:
`docs/migrasi/05e-gap-poin-agenda.md`, `05f-implementasi-poin-agenda.md`.

| # | Temuan | Jawaban | Keputusan final |
|---|---|---|---|
| 1 | Cakupan fase | **Poin + Agenda Mingguan sekaligus** | Keduanya dalam satu rencana. |
| 2 | Legacy menampilkan alert sukses pelanggaran/prestasi walau API gagal; pemanggilan gagal tanpa pesan (doc `05` §13 #12) | **Perbaiki: sukses hanya bila API sukses** | Repository melempar exception, VM hanya mengumumkan sukses setelah sukses; gagal = banner + form tetap terbuka, ketiga form. |
| 3 | Legacy: siswa non-"Student" tetap diproses (hanya toast); NISN kosong membuat `check-account` tak terdefinisi | **Blokir dengan pesan** | `PoinRepository.checkTarget()` menolak role bukan Student atau NISN kosong sebelum ke layar Hasil. |
| 4 | Legacy memblokir Poin siswa bila waktu ponsel tidak "Otomatis" | **Pertahankan** | Komponen bersama `AutoTimeGate` (dipakai juga Asesmen). |
| 5 | Check-in agenda butuh `gate.in` (presensi masuk guru), Presensi guru belum ada di app baru | **Hanya tombol "Tutup"** | Tombol "Presensi" pada alert tidak dibuat sampai Presensi guru dibangun. |
| 6 | Peta Google di layar lapor | **Pasang peta** | `maps-compose`, `MAPS_API_KEY` dari `local.properties` lewat manifest placeholder, izin lokasi. |

**Deviasi disengaja lain (tanpa ❓, dicatat):** Poin siswa tanpa cache Room (PK legacy menimpa kejadian
sama jenis + hapus lintas tab); foto >1 MB setelah kompres ditolak (legacy mengirim tanpa foto diam-diam);
`gate`/`window`/`summary` null-safe (legacy crash); submit lapor agenda disabled sampai ada fix GPS dan
izin fine *atau* coarse diterima (legacy mewajibkan keduanya & mengizinkan submit tanpa lokasi);
Geocoder API 33+ punya `onError`; jam lapor berjalan; first-load Agenda 1× (legacy 2×); back Poin = pop
biasa (legacy membuka HomePage baru); pesan error `errors` ditambahkan ke `ApiException.validationMessages`.

**Belum diverifikasi (jangan diasumsikan selesai):** nama part foto di server (`violation_image` vs
`file`), seluruh sisi guru Poin dan Agenda di device, peta (butuh key valid), JSON asli `staff/agendas`.

---

## 2026-10-07 — Fase Presensi (harian, siswa + guru): keputusan

**Konteks:** Fase 3 peta migrasi (doc `07` §4–6, §10). Jurnal KBM dan Magang fase terpisah. Detail:
`docs/migrasi/07a-gap-presensi.md`, `07b-implementasi-presensi.md`.

| # | Temuan | Jawaban | Keputusan final |
|---|---|---|---|
| 1 | Cakupan | **Presensi lengkap** | Data Absensi + Rekap + Masuk/Pulang + Dinas Luar (guru) + Izin. |
| 2 | Kamera selfie Dinas Luar | **CameraX** | Kamera depan, watermark live, tanpa izin mikrofon (legacy: otaliastudios CameraView). |
| 3 | Deteksi lokasi palsu (keputusan 30-09 §19#5) | **Mock location saja** | `Location.isMock`/`isFromMockProvider` menolak submit Masuk/Pulang & Dinas Luar; root tidak dideteksi. |
| 4 | Geofence legacy tak pernah berfungsi; keputusan 30-09 §19#1 mewajibkan background location | **Buang geofence & syarat background** | Lokasi presisi (FINE) cukup saat layar terbuka; `ACCESS_BACKGROUND_LOCATION` tidak dideklarasikan. **Mengubah keputusan 30-09 §19#1.** |

**Deviasi lain (default saya, belum ditanyakan):** avatar marker tidak diunduh (crash legacy hilang); alert
"koordinat sekolah 0" hanya bila radius dibatasi (doc §19#2); `absence_setting` key hilang/error → dibatasi;
dialog setelan lokasi tidak berulang setelah batal (doc §19#7); alasan gagal submit ditampilkan dan tombol
tetap ada; "Nanti Saja" Dinas Luar menutup dialog (doc §19#6); catatan dihitung setelah trim; foto Dinas Luar
yang tidak cocok dengan alamat diminta ulang; MIME bukti izin tak dikenal ditolak; "boleh ajukan izin" memakai data
segar; refresh Data Absensi 1× setelah kembali; tombol "Hadiri" Hub → dialog dalam pengembangan sampai Jurnal;
`SessionStore.writeCheckAccount` tidak lagi menimpa koordinat sekolah.

**Belum diverifikasi (jangan diasumsikan selesai):** Masuk/Pulang (server uji menolak: "School Calendar Event is
not available for today"), Dinas Luar + kamera + watermark, kirim Izin, mock location, sisi siswa, makna key
`code` pada respons offsite (legacy: 0 = gagal; key hilang diperlakukan sukses bila `data.status` ada).

---

## 2026-10-07 — Fase Jurnal KBM (siswa + guru): keputusan

**Konteks:** Fase berikutnya setelah Presensi (doc `07` §7–9, §14.2–14.3). Detail: `docs/migrasi/07c-gap-jurnal.md`,
`07d-implementasi-jurnal.md`.

| # | Temuan | Jawaban | Keputusan final |
|---|---|---|---|
| 1 | Cakupan | **Jurnal lengkap** | Daftar + Hub, Verifikasi Jurnal & Scan QR (siswa), Mulai kelas + form Isi Jurnal (guru/siswa), foto suasana KBM, Detail Jurnal. |
| 2 | Pemindai QR | **Google code scanner** | `play-services-code-scanner` (QR saja, tanpa izin kamera/preview sendiri); legacy memakai zxing + `PortraitCaptureActivity`. |
| 3 | Kunci jam siswa (doc §19#13) | **Pertahankan aturan, perbaiki indeksnya** | Dihitung per jam pelajaran, bukan indeks daftar yang bergeser (legacy salah saat satu jam punya >1 kelas). |

**Deviasi lain (default saya, belum ditanyakan):** jadwal tanpa cache Room (legacy dikunci `plot_start_at`, kosong
offline); picker kelas/mapel/guru dipaging dari server dengan state di memori (tidak memakai tabel Room `classroom`/`mapel`);
dialog "Pilih Metode Presensi" memakai aturan `JurnalPage` (manual bila belum ada status; QR hanya bila belum hadir dan
di dalam jam plot) dan berupa bottom sheet; tombol "Hadiri"/"Isi Jurnal" di kartu Hub hanya untuk siswa; chip jam guru
yang ditolak tidak lagi menghapus semua pilihan; "Mulai kelas" disembunyikan sebelum jamnya mulai; badge status sesi
juga tampil untuk guru; foto KBM wajib divalidasi saat kirim (tombol tetap aktif, pesan persis legacy); mock location
menolak Verifikasi Jurnal dan Mulai kelas, sedangkan lat/lng form siswa dihilangkan (bukan diblokir) bila lokasi palsu;
QR tak valid kini diberi pesan; sukses Verifikasi Jurnal menampilkan dialog "Presensi Berhasil"; `school_class_id`
siswa dari `SessionStore.student.classRoomId`; crash legacy (gagal memuat daftar jam, avatar tanpa try/catch) tidak
ditiru; "Penugasan" tidak lagi hilang di detail guru; detail non-guru menampilkan izin/sakit apa adanya; daftar
disegarkan setelah "Mulai kelas" (legacy lupa); sukses "Mulai kelas" membuka detail seperti legacy.

**Belum diverifikasi (jangan diasumsikan selesai):** bentuk asli respons `schedule` (siswa/guru), makna `is_present`
dan domain `status`, perilaku HTTP 307 di OkHttp (apakah redirect diikuti sebelum interceptor), apakah server
menegakkan "dalam jam plot" untuk QR, pemindaian QR fisik, serta seluruh layar Jurnal di device (HP uji terkunci saat
fase selesai) — kode dan aturan murni lolos kompilasi/unit test saja.

## 2026-10-12 — Penyamaan Hub Pembelajaran dengan `PembelajaranPage` lama: keputusan

**Konteks:** permintaan user sebelum fase berikutnya — Home baru sama dengan lama dalam data tampil, endpoint, dan aturan tampil;
UI boleh sedikit berbeda; teks/respons panjang tertangani. Detail: `docs/migrasi/05g-gap-hub-pembelajaran.md`, `05h-implementasi-hub-pembelajaran.md`.

| # | Temuan | Jawaban | Keputusan final |
|---|---|---|---|
| 1 | Lonceng lama: badge angka (`payment/wallet` → `notification/summary`) dan dikunci bila Klaspay belum aktif | **Badge + penguncian seperti lama** | Badge "99+" dari 99; Klaspay belum aktif → toast "Silahkan aktivasi wallet terlebih dahulu untuk mengunakan fitur notifikasi" + layar aktivasi Klaspay (`returnToMain`). Daftar Notifikasi masih data contoh. |
| 2 | Kartu "Kelas Berlangsung" lama TIDAK memanggil `schedule?date=`; datanya `check-account.current_subject` | **Persis lama** | `schedule` dihapus dari Home; kosong → "Tidak terdapat jadwal". Layar Jurnal tetap memakai `schedule`. |
| 3 | Legacy memuat ulang semua data di tiap `onResume` dan refresh, tanpa pengaman; aturan proyek melarang fetch ulang tanpa syarat | **Seperti lama + pengaman** | Muat saat kembali ke halaman dan saat refresh; tidak tumpang-tindih; dilewati bila <30 dtk setelah muat sukses (refresh manual selalu jalan). |

**Deviasi lain (default saya, belum ditanyakan):** sapaan "Assalamualaikum," dipertahankan (hanya gaya); baris kota dan placeholder "Carousel banner · 3 slide" dibuang (tak ada di legacy);
label tombol kartu "Check In" (legacy) menggantikan "Hadiri"; "Check In" memakai sheet hanya "Verifikasi Jurnal" (QR di hub lama mati — layar Jurnal tetap punya QR); `current_subject.id` null → tombol disembunyikan (legacy meloloskan);
banner presensi dibiarkan (tidak diganti "Jadwal absensi belum diatur") bila `setting/me/today` gagal; kartu verifikasi menampilkan pesan server dengan "Selengkapnya" (legacy: teks tombol tanpa batas);
dialog akun nonaktif menghormati logout yang tertahan ujian; lonceng dimuat setelah check-account (legacy membaca `klaspayActive` basi); CTA banner satu warna; toast gagal check-account memakai pesan ramah, bukan `localizedMessage`;
`AppDialog`/`FeatureUnavailableSheet` bisa di-scroll (berlaku ke semua dialog aplikasi); `Route.Jurnal` tidak lagi menerima argumen pintasan dari Hub.

**Diuji di device (2026-10-12, sesi siswa "RIZKY", kelas DEVELOPER MOBILE, Klaspay aktif):** urutan endpoint saat buka tab = `check-account` → `current-user` → `payment/wallet` → `notification/summary?wallet_id=` dan **tanpa** `schedule`;
header (logo sekolah, avatar → logo, nama, baris kelas), kartu "Kelas Berlangsung" ("Tidak terdapat jadwal", ikon = logo sekolah), 7 tile siswa, bell → Notifikasi. Bentuk respons asli: `current_subject` = `{}` (objek kosong, bukan null),
`userProgress` = null, `school` tanpa `city_name`, `wallet.user_id` = string ("US…"), `summary.count_unread` = 0 (badge tersembunyi), `current-user` `single_device_login_enabled=true` + `device_id` ada (tidak ada konflik di perangkat ini).
**Belum diverifikasi (jangan diasumsikan selesai):** sisi guru setelah perubahan ini (banner presensi, lencana agenda), tombol "Check In"/"Isi Jurnal" dengan `current_subject` terisi, status verifikasi in_review/rejected, badge lonceng > 0,
`setting/me/today` dan `staff/check` di app baru, dialog "Perangkat Lain Terdeteksi", `VerificationPage` (tombol "Sudah"/"Belum" masih ke dialog "dalam pengembangan"). Item `HomePage` host (dialog jam "Otomatis",
dialog keamanan email/password, dialog gagal wallet, FCM topic) di luar cakupan.

## 2026-10-12 — UI tidak menembus status bar & bar navigasi: keputusan

**Konteks:** permintaan user "pastikan ui tidak menembus status bar android & bottom nav android". Audit: layar di dalam `MainTabsScreen` aman; layar di luar shell
(splash, onboarding, login, password, SSO, aktivasi Klaspay, reset password) tidak punya penanganan inset sama sekali; keyboard tidak ditangani di mana pun.
Pedoman: `docs/ANDROID_DESIGN_PATTERN.md` §11 "Window Insets".

| # | Temuan | Jawaban | Keputusan final |
|---|---|---|---|
| 1 | Layar auth menggambar logo/tombol di bawah bar sistem; gradien latarnya penuh layar | **Seluruh layar dibatasi seperti layar lain** | `SystemBarsFrame` (`safeDrawingPadding()` + latar `background`): gradien ikut berhenti di batas bar, strip polos di atas/bawah. |
| 2 | Tombol bawah/kolom isian tertutup keyboard di form (Jurnal, Upload Materi/Tugas, Poin, Izin, Transfer, …) | **Ikut dibereskan** | `windowSoftInputMode="adjustResize"` + `imePadding()` di satu titik shell (setelah `consumeWindowInsets`); auth lewat `safeDrawingPadding()`. |

**Deviasi lain (default saya, belum ditanyakan):** `AppBottomSheet` kini membuka setinggi isinya (`skipPartiallyExpanded = true`) — ditemukan saat uji di device: sheet "Pilih Sekolah" terbuka setengah layar dan baris
terakhir daftar tergambar di bawah bar navigasi; `Scaffold` shell memakai `systemBars ∪ displayCutout` (cutout muncul bila kunci portrait diabaikan di layar besar Android 16); sheet kisi soal AKM dibatasi 50% tinggi layar;
tema XML memakai bar transparan (kilatan hijau saat cold start hilang); `NavHost` luar diberi latar opak agar crossfade tidak menembus; seam warna tipis antara `bottomBar` sticky dan strip bar navigasi
serta kamera/QR/header ujian yang "terbingkai" strip dibiarkan (kosmetik, konsisten dengan keputusan 1).

**Diuji di device (HP 1080×2340, navigasi 3-tombol, mode terang):**
- Layar Login (sesi berakhir): logo mulai di bawah status bar, isi berhenti di atas bar navigasi; dengan keyboard numerik terbuka isi berhenti di tepi atas keyboard dan tetap bisa di-scroll.
- Sheet "Pilih Sekolah": sebelum perbaikan baris terakhir menembus bar navigasi (sheet terbuka setengah layar); sesudahnya terbuka setinggi isi dan seluruh daftar di atas bar.
- Sesi siswa, di dalam shell: Home, Notifikasi, Akun, Ubah kata sandi, Presensi (bar "Lakukan Presensi"), Pengajuan Izin (bar "Ajukan Izin") rapi di antara status bar dan bar navigasi;
  **Tambah Izin dengan keyboard terbuka** — bar "Kirim" naik tepat di atas keyboard dan kolom keterangan ter-scroll ke area terlihat.
- **Kolom isian di atas bar sticky** (Transfer Saldo → "Catatan (opsional)"): sebelum perbaikan kolom tinggal terlihat setengah di balik bar (tepi bawah kolom 37 px di bawah area scroll). `AppTextField` kini memanggil
  `BringIntoViewRequester` setelah tinggi keyboard (`WindowInsets.ime`) stabil 150 ms (batas tunggu 1,2 s bila tak ada keyboard) — penundaan tetap 300 ms gagal karena keyboard masih bergeser saat dipanggil. Sesudahnya: kolom [915–1041] penuh di dalam area scroll (berakhir di 1046).
- Ubah kata sandi dengan keyboard terbuka (kolom terbawah fokus): kolom penuh terlihat, area scroll menyusut ke atas keyboard; tombol "simpan perubahan" ada di dalam scroll sehingga dijangkau dengan menggulir.
- Splash: latar & logo di antara kedua bar, versi aplikasi di atas bar navigasi.
- Tab Pembelajaran/Akun: `NavigationBar` tidak berubah.
**Belum diverifikasi (jangan diasumsikan selesai):** Onboarding, Password, SSO, aktivasi Klaspay (akun uji sudah Klaspay-aktif), reset password — butuh logout/akun lain; form berkeyboard lain di dalam shell (JurnalForm, UploadMateri/Tugas, PoinForm,
PresensiOffsite — memakai mekanisme `imePadding` + `AppTextField` yang sama dengan Izin/Transfer); sheet kisi AKM ≥40 soal (butuh memulai ujian, membuat data); navigasi gesture, mode gelap, font 1,3× (mengubah pengaturan sistem HP, tidak dilakukan).


## 2026-10-14 — Pembayaran / Klaspay fase 6, sub-fase P1: keputusan

**Konteks:** user memilih "Pembayaran / Klaspay" sebagai fase berikutnya (roadmap `01` fase 6). Audit: seluruh tab Pembayaran dan 9 layar sub berdata dummy (`08a-gap-pembayaran.md`).
Dikerjakan bertahap P1–P6; P1 = fondasi, hub nyata, aktivasi, PIN (`08b-implementasi-pembayaran-p1.md`). Try Out & ujian lama (Asesmen) **tidak dibangun** (keputusan user, jawaban pada sesi yang sama);
Asesmen nanti dua sub-fase (A1 data aman, A2 lockdown/pelanggaran/penalti/laporan masalah).

| # | Temuan | Jawaban | Keputusan final |
|---|---|---|---|
| 1 | Hub dummy memuat Transfer, "Transaksi Terakhir", Toko, Perpus, Lainnya, "5 tagihan menunggu" — tak ada padanan di app lama | **Ikuti app lama** | Hub: Top Up, QR Pay, Riwayat; kartu Tagihanku; menu Bayar Spp (siswa)/Dana Partisipasi/Pinjam Buku (dialog "Fitur belum tersedia"); 6 PPOB (Game tersembunyi untuk guru). Elemen dummy dihapus; Transfer tetap ada di dalam Top Up untuk Toppers (P3). |
| 2 | doc 08 Q19: dialog "Aktivasi Sekarang" membuka PIN tanpa cek email/password default, sedangkan tombol hub mengecek | **Samakan dengan tombol hub** | Semua jalan masuk aktivasi (tombol hub, dialog, lonceng Home) lewat: email terverifikasi → password default → `payment/check`. Jalur SSO tidak melewati pra-cek. |
| 3 | doc 08 Q2: SSO + "TOP UP SEKARANG" tidak membuka Home | **Tiru app lama** | Top Up terbuka sendirian setelah aktivasi SSO (dibangun di P3). Sampai P3 tombol itu menampilkan "Fitur dalam pengembangan" lalu berperilaku seperti "NANTI SAJA". |
| 4 | doc 08 Q14: PIN & body HTTP tercetak di Logcat release | (final 2026-09-30) | **Selesai di P1:** `LogRedactor`; level BODY & Timber `DebugTree` hanya `BuildConfig.DEBUG`; header `Authorization`/`Cookie` disamarkan; `LogFileInterceptor` ikut menyamarkan. |

**Deviasi lain (default saya, belum ditanyakan):**
- Input PIN tetap memakai `PinDots` + `NumericKeypad` yang sudah ada (legacy: 6 kotak + keyboard sistem); tempel tidak didukung; PIN tak masuk saved-state, route argument, atau log (`PinResultChannel`).
- Hub tidak memanggil `check-account` lagi (sudah dilakukan hub Pembelajaran); muat ulang = on resume (throttle 5 dtk) + tarik-untuk-segarkan, tanpa `NetworkCallback`.
- Format "Rp" diseragamkan menjadi `Rp 150.000` (legacy campur `"Rp "`/`"Rp"`).
- Dialog "Nanti Saja" pada dialog email tidak lagi memicu ulang tombol aktivasi tanpa akhir (legacy); setelah dialog email: ke dialog password bila password masih default, selain itu berhenti.
- Error `payment/check` selain `email`/`password` ditampilkan sebagai toast (legacy diam).
- Pemicu otomatis dialog keamanan email/password di Home (listener pref) **belum** dibangun; dialog dipakai di alur pembayaran saja. Penyelesaian verifikasi email via deep link `portal.diskola.id/verify-email` ikut fase Notifikasi
  (flag `is_email_verified` pulih sendiri lewat `check-account` di Home).
- Deep link reset PIN memvalidasi token lewat base API build ini, bukan host dari tautan (legacy memanggil URL tautan apa adanya dengan kredensial pengguna).
- Layar `Route.PembayaranSub.*` lama (berdata dummy) tidak terjangkau lagi dari hub; dipertahankan sebagai referensi UI dan diganti/dihapus per sub-fase, sisanya di P6 bersama `MockPembayaran`.
- Info wallet berupa bottom sheet (legacy: top sheet).

**Belum diverifikasi (jangan diasumsikan selesai):** semua perilaku di device — HP uji tidak terhubung saat P1 selesai (lihat `08b` §4); alur wallet belum aktif (butuh akun tanpa Klaspay); "Lupa pin" (email nyata, batas 1× per 3 hari);
deep link `autoVerify` (butuh APK rilis + assetlinks server); sisi guru; `ConfirmPin` baru punya pemanggil di P2.

## 2026-10-15 — Asesmen/AKM fase 5, sub-fase A1 "data aman": keputusan

**Konteks:** user menunda Pembayaran (berhenti di P1) dan memilih Asesmen/AKM. Audit (`06a-gap-asesmen.md`): jawaban hanya di memori, tak ada auto-submit/antrean offline, risiko buntu logout, banyak data palsu
(penalti "00:24", chip "Mode ketat", tombol "Simulasi offline"). Dikerjakan A1 = data aman (`06b-implementasi-asesmen-a1.md`); A2 = lockdown, pelacakan pelanggaran, penalti, laporan masalah, layar priming, pembahasan.

| # | Temuan | Jawaban | Keputusan final |
|---|---|---|---|
| 1 | Try Out & ujian lama (doc 06 §19–§20) | **Tidak dibangun** | Hanya jalur ujian sekolah; scaffolding `TryOutApiService`/`UjianLegacyApiService` dibiarkan tak dipakai. |
| 2 | Pemecahan cakupan Asesmen | **Dua sub-fase** | A1 data aman; A2 lockdown/pelanggaran/penalti/laporan/priming/pembahasan. |
| 3 | Blokir logout bisa buntu (token mati / jawaban tak pernah terkirim) | **Blokir mutlak** | Persis keputusan 30-09: logout diblokir selama jawaban belum terkirim, tanpa pengecualian 401/24 jam. **Risiko sisa:** token tidak berlaku + jawaban tertunda = tak bisa logout/login ulang; mitigasi: worker terus mencoba; tindak lanjut bila terjadi = "login ulang tanpa menghapus tabel ujian". |
| 4 | Push FCM `"logout"` / 401 saat jawaban belum terkirim (dulu diabaikan diam-diam) | **Tunda lalu jalankan otomatis** | `SessionManager.deferLogout()` + `LogoutBlockedByPendingExam` + dialog penjelasan; `AkmSubmitWorker` menjalankan logout tertunda begitu tidak ada ujian pending. |

**Deviasi lain (default saya, belum ditanyakan):**
- Migrasi Room eksplisit `7→8` untuk tabel ujian (konvensi proyek: destruktif); `fallbackToDestructiveMigration` tetap untuk jalur lain. Skema diekspor ke `app/schemas`.
- Submission `REJECTED` (server menolak permanen, mis. 422) **tidak** memblokir logout dan tampil sebagai kegagalan — klarifikasi atas "mutlak": yang diblokir hanya yang masih mungkin terkirim.
- Esai kosong dan Pilihan ganda multi-jawaban yang semua centangnya dilepas **tetap terhitung terjawab** (perilaku legacy §7.1/§22 #5; app baru sempat menghapusnya, yang memblokir tombol Kumpulkan).
- Tipe soal tak dikenal dirender sebagai esai (fallback legacy) agar ujian tetap bisa diselesaikan.
- Ujian terunduh yang tak pernah dibuka tetap dikirim (kosong) di batas waktu — perilaku legacy; `date_end` yang tak terbaca **tidak** dianggap "sekarang" (tak ada auto-submit atas tebakan).
- Kegagalan POST online saat menekan "Kumpulkan": dialog "Gagal"/"Coba lagi" dan ujian tetap bisa diedit (legacy mengembalikan transaksi); deadline worker tetap mengirim di batas waktu.
- Upload ulang saat offline = gagal ("Gagal mengirim jawaban. Coba lagi"); legacy mengantrekan lalu tetap menampilkan "Jawaban berhasil dikirim".
- Teks ketentuan A1 hanya memuat butir yang benar-benar berlaku (offline boleh, koneksi stabil saat mengunggah, tidak boleh mengangkat telepon); butir kunci layar/sanksi/pelacakan kembali bersama teks legacy persis di A2. Chip "Mode ketat", ikon kunci, subtitle "Layar dikunci…", "00:24" dan tombol "Simulasi mulai saat offline" dihapus.
- `exam_lock_mode` default `false` (keputusan 30-09); nilai `null` eksplisit dibiarkan `null` — legacy menafsirkannya sebagai strict (doc 06 §9.1), A2 yang memutuskan.
- Fingerprint ujian tetap UUID `DeviceUtil` (legacy 2.1.40 memakai `ANDROID_ID`, doc 06 §12.2) — **belum diverifikasi ke backend**.
- Sumber waktu tetap jam perangkat (seperti legacy); `AutoTimeGate` tetap hanya peringatan di daftar.

**Belum diverifikasi (jangan diasumsikan selesai):** seluruh jalur unduh → jawab → kumpulkan, antrean offline, auto-submit di batas waktu, proses mati saat mengerjakan, FCM `"logout"` dan 401 tertahan, upload ulang + cooldown (butuh ujian uji yang bisa dimulai, izin, dan untuk offline mengubah pengaturan sistem HP).

## 2026-10-16 — Asesmen/AKM: penyesuaian dengan dokumen 06 v2.1.41

**Konteks:** doc `06` diperbarui ke android-portal 2.1.41 (`05ec898b`): label/stempel jadwal dibatalkan (§4.4) dan perilaku input esai (§5.7). Diterapkan ke A1 (`06b` §5).

| # | Temuan | Jawaban | Keputusan final |
|---|---|---|---|
| 1 | doc 06 §4.4: layar "Sebelum Memulai Ujian" dan ringkasan ujian (Resume) di app lama **tidak** menandai jadwal berwatermark ("catat, jangan diperbaiki diam-diam") | **Tampilkan juga di layar Resume** | Banner informatif di `AkmResumeScreen` (tetap tidak menghalangi mengerjakan/mengumpulkan). Layar priming dibangun di A2 dan ikut menandai. Deviasi dari app lama. |

**Deviasi lain (default saya, belum ditanyakan):**
- Tanda "dibatalkan" disimpan di JSON cache jadwal (bukan kolom Room seperti legacy `akm.watermark_bool/label`); aturan "key tidak ada → pertahankan nilai lokal" dijalankan saat menyimpan cache (`AkmRepository.store`), sehingga berlaku juga setelah ujian pindah dari daftar ke nilai dan setelah restart.
- Hasil (Score Detail) kini menampilkan tabel info peserta yang sama dengan Detail (termasuk baris "Status") — doc 06 §14 "info seperti detail"; sebelumnya hanya cincin skor + subtes.
- Label baris "Nama" → "Peserta" (doc 06 §5.3).
- Kursor "diikuti" memakai posisi akhir teks (`AppTextField` hanya melihat string, bukan seleksi) — kasus mengetik biasa; mengedit di tengah teks tidak menggulir ke kursor.
- Esai: tombol "Selesai" menutup keyboard dan melepas fokus (legacy: `IME_ACTION_DONE`); `minLines = 8` dipakai untuk semua tipe esai (layout legacy), termasuk isian angka dan satu kata.

**Belum diverifikasi (jangan diasumsikan selesai):** tampilan label/stempel/watermark/banner di device (butuh jadwal yang ditandai server — belum diketahui apakah backend ini sudah mengirim `watermark_bool`), perilaku keyboard esai di HP (Done pada field multi-baris, enter dibuang, scroll kursor), dan semua yang masih tercatat belum teruji di entri 2026-10-15. HP uji tidak terhubung saat penyesuaian ini selesai.

## 2026-10-16 — Asesmen/AKM fase 5, sub-fase A2 "keamanan ujian" (A2a sesi + kunci layar, A2b pelanggaran, A2c penalti + laporan)

**Konteks:** `06c-gap-asesmen-a2.md`, `06d-implementasi-asesmen-a2.md`. Sumber kebenaran kode lama: `origin/development` (`05ec898b`); checkout lokal basi. Rencana disetujui user 2026-10-16 ("lanjut ke A2"); keputusan di bawah adalah **default yang saya ambil di rencana** (user menyetujui rencana, belum menjawab tiap butir satu per satu).

**Ditegaskan user (2026-10-19):** di repo lama penalti sudah dinonaktifkan (server) **tetapi pelanggaran tetap dikirim** saat siswa keluar aplikasi. Itu sudah aturan dokumen (§10.5, §11.2) dan menjadi tes (`AkmViolationTrackerTest`): pelanggaran **selalu dicatat dan dikirim**, penalti hanya menentukan dialog.

| # | Temuan | Keputusan | Alasan |
|---|---|---|---|
| 1 | Arsitektur: banyak Activity (lama) vs satu Activity | Satu Activity + `ExamSessionController` singleton (bukan `ExamActivity`) | Memakai ulang nav/Hilt/tema; masalah "instance halaman basi" (§23.3 #11) hilang; manifest per-layar diganti deteksi runtime (targetSdk 36 mengabaikan batas itu di layar ≥ 600 dp) |
| 2 | §23.3 #3 `forceRelock` no-op bila belum pernah ter-pin ⇒ siswa terjebak | Diperbaiki; verifikasi pin berbasis event bukan 1,8 dtk; setelah 3 "Aktifkan" gagal muncul "Keluar dari ujian" (abort: penanda tetap, penalti bila aktif) | Tanpa jalan keluar, perangkat yang menolak pin mengunci siswa selamanya. **Deviasi** |
| 3 | Penalti ditahan bila ada dialog dalam-app (`isModalVisible`) — bisa dieksploitasi | Pelanggaran selalu dicatat; penalti **tidak** ditahan dialog milik app; tetap ditahan prompt pin sistem dan keluar sah (10 dtk). **Deviasi** (A2c) | Dialog app tidak boleh jadi tameng |
| 4 | Judul setelah penalti: Resume "Kerjakan Ujian", Soal "Kerjakan Asesmen" | Tetap "Kerjakan Asesmen" (A2c) | Inkonsistensi legacy |
| 5 | §23.3 #7 teks siswa + checkbox laporan tidak dikirim | Payload legacy persis; teks/checkbox/violations disimpan di Room tapi tidak dikirim (A2c) | Kontrak backend belum ada; `logcat` diberi `LogRedactor` |
| 6 | §23.3 #10 penalti dari `HH:mm:ss` | Simpan epoch akhir, sisa dibatasi ≤ durasi (A2c) | Aman lewat tengah malam / jam mundur |
| 7 | §23.3 #20 worker laporan periodik abadi; antrean berhenti setelah "Ya, keluar" | Worker pelanggaran membatalkan diri hanya bila antrean kosong **dan** sesi tak aktif; worker laporan one-time (A2c) | Pelanggaran sesi yang ditinggalkan tetap terkirim |
| 8 | §23.3 #12/#17 pembahasan: unduh ulang paket mereset SCORED→NEW | **Ditunda** (backend belum mengirim `has_explanation`; tombol tersembunyi); layar kartu `file_path` dipertahankan | Tidak meniru perilaku yang menghilangkan nilai |
| 9 | Deteksi screenshot di bawah API 34 (fallback `MediaStore`) | Hanya API 34+ (`ScreenCaptureCallback`) | Izin baca media dicabut di manifest lama ⇒ fallback tak pernah jalan; `FLAG_SECURE` sudah menghitamkan gambar |
| 10 | Rotasi/resize membuat ulang Activity | Bukan `app_background` (`isChangingConfigurations`) | Tanpa itu satu rotasi = satu pelanggaran palsu |
| 11 | Auto-backup bisa memulihkan penanda sesi ke perangkat lain | Prefs `akm_exam_state` dikecualikan dari backup | Penanda yang "bangkit" = `not_submitted` palsu |

**Deviasi lain (default saya):**
- `penalty_applied` yang **hilang** dari JSON ⇒ *mati* (default DTO `false`, persis legacy); `null` eksplisit ⇒ *aktif*; belum pernah diterima (−1) ⇒ aktif. (Rencana awal menulis "hilang ⇒ aktif" — itu salah terhadap legacy; kode mengikuti legacy.) `exam_lock_mode`: hilang ⇒ mati (keputusan 2026-09-30), `null` eksplisit ⇒ ketat (legacy).
- Mode tidak ketat: tetap menampilkan alert "Peringatan" saat Back, dan mencatat pelanggaran (legacy: pelanggaran tidak bergantung pada mode ketat).
- `connection_loss` dan jam dipantau **sepanjang sesi**, bukan hanya saat layar resume; baseline jam diambil saat sesi mulai dan `deviceTimeBefore` memakai jam yang seharusnya (legacy: jam event sebelumnya).
- Baris pelanggaran tidak pernah dikirim dengan `studentExamId = 0` (legacy: 400 lalu dibuang); antrean menunggu id sesi.
- Backoff 500/403 benar-benar berjalan (legacy: tidak, karena `HttpException`).
- FGS `dataSync`: `START_NOT_STICKY`, dimulai hanya dari UI foreground, `onTimeout` (API 35), kanal senyap yang sudah ada (`AkmNotifications`), tanpa tergantung `POST_NOTIFICATIONS`.
- Mode ketat: dialog pemulihan menutup sendiri begitu pin aktif (pemantau 500 ms) — perangkat uji menampilkan prompt pin **tanpa** perubahan fokus jendela, sehingga "prompt belum dijawab" tak dapat dibedakan dari "tak ada prompt".
- Penalti (A2c): dialog juga menulis **"Sisa waktu m:ss"** (legacy hanya menampilkannya di pill header, yang tertutup dialog — tidak ada angka di dialog legacy). Penalti disimpan sebagai epoch akhir dan **dilanjutkan** (bukan diulang penuh) bila sesi dibuka lagi saat masih berjalan; hilang saat jawaban diserahkan.
- "Ya, keluar" dengan penalti aktif tidak menyimpan `state_penalty` terpisah seperti legacy: penanda sesi yang tersisa ⇒ `not_submitted` di sesi berikut ⇒ penalti lewat jalur pelanggaran yang sama (hasilnya sama; sebabnya kini tertulis "keluar dari ujian tanpa mengumpulkan jawaban").
- Laporan masalah: pekerja pengirim satu kali unik (bukan periodik abadi); teks siswa, checkbox, dan daftar pelanggaran disimpan di `akm_problem_report` tetapi tidak dikirim (kontrak backend belum ada, sama seperti legacy).
- Hardware back di layar Soal tidak melakukan apa pun saat sesi berjalan (legacy `shouldDisableBackButton`); panah di header kembali ke Resume.

**Belum diverifikasi (jangan diasumsikan selesai):** semua pelanggaran nyata di perangkat, pengiriman nyata ke endpoint `violations`, migrasi Room 8→9 pada DB nyata, pin sampai berhasil, FGS pada API 34/35. Server dev: `exam_lock_mode:false`, `penalty_applied:false`, dan ujian uji berstatus "Tidak Mengerjakan" — alur ketat hanya bisa diuji dengan memaksa setelan secara lokal.
**Celah diketahui:** media soal (audio/video) membuka aplikasi lain ⇒ tercatat sebagai keluar aplikasi; perlu pemutar dalam-app (Media3), belum dibangun.

## 2026-10-21 — Feature gate `payment` (menu Bayar SPP & Dana Partisipasi) di aplikasi lama

**Konteks:** backend menambah key `payment` pada `GET mobile/app/check-feature-availability?name=` (sebelumnya hanya `jurnal-kbm` dan `presensi`). Permintaan user:
bila `payment` maintenance, blokir menu SPP dan Dana Partisipasi, **disesuaikan seperti yang sudah ada di presensi dan jurnal-kbm**. Diterapkan di `android-portal` dan `ios-portal`.
Spesifikasi: `08-keuangan-pembayaran-klaspay-ppob.md` §5.6; rujukan terkait di `03` §7.4/§8.5/§10.6, `10` §6.6, `01`, `08a`.

| # | Temuan | Jawaban | Keputusan final |
|---|---|---|---|
| 1 | Key `payment` maintenance | **Blokir Bayar SPP & Dana Partisipasi, perilaku sama dengan presensi/jurnal-kbm** | Cek sebelum layar dibuka; fail-open bila jaringan/format gagal; bottom sheet (Android) / popup (iOS) "Fitur Belum Tersedia"; non-guru tanpa tombol website, guru dengan "Buka Website". |

**Deviasi lain (default saya, belum ditanyakan):**
- Yang dijaga hanya SPP dan Dana Partisipasi. Top Up, QR Pay, Riwayat, Tagihanku, dan PPOB **tidak** ikut diblokir (sesuai permintaan, tidak diperluas).
- Pintu masuk yang ikut dijaga di Android: tiga tombol hub, notifikasi `PAYMENT/SPP` (daftar & detail), dan push `new-spp`/`spp` lewat `guardFeatureOnCreate` di `SppPaymentPage`/`PartisipasiPage`.
  Notifikasi `PAYMENT/BILL` Android **tidak** dijaga (tujuannya Klaspay Tagihan); di iOS `BILL` dijaga karena tujuannya Dana Partisipasi.
- `"Buka Website"` untuk guru mengarah ke **root portal** (tidak ada path web pembayaran di kode) — dicatat sebagai ❓ Q22 di doc 08 §18.
- `SppPaymentPage` dan `PartisipasiPage` memuat saldo/daftar hanya setelah guard lolos (`contentReady`), agar tidak ada request payment di belakang bottom sheet maintenance; perilaku pertama masuk tetap sama.
- Gate terpanggil dua kali dari menu hub (menu + `onCreate`), sama seperti Presensi/Jurnal; tidak dioptimalkan di aplikasi lama.
- Notifikasi `SPP` tanpa id di daftar tetap membuka `HomeWorkPage` tanpa gate (perilaku lama, ❓ Q2 doc 03).

**Belum diverifikasi (jangan diasumsikan selesai):** perilaku di device (menu, notifikasi, push `new-spp`/`spp`, akun siswa vs guru); respons backend sebenarnya untuk `name=payment` (bentuk `data.available`/`data.message` diasumsikan sama dengan `presensi`);
perilaku bila key `payment` belum ada di server prod (dugaan: fail-open lewat error → tetap terbuka). Yang sudah dipastikan hanya build: `:app:compileDebugKotlin` (Android) dan `xcodebuild` generic iOS tanpa signing.

## 2026-10-19 — Asesmen/AKM (app lama, `e0a12b62`): rumus LaTeX, tombol bawah "Selesai", header `X-App-*`

**Konteks:** perubahan di app lama `android-portal` (commit `e0a12b62`, 19-10-2026) yang kini tercatat di `migrasi/06-asesmen-akm-tryout-ujian.md` §5.6, §5.8, §16, §23.4 dan `migrasi/10-infrastruktur-api-data-latar.md` §3.2. Kontrak backend untuk rumus ada di `android-portal/docs/api/formula-matematika-contract.md`. iOS (`ios-portal`) dikerjakan paralel atas permintaan user ("lakukan hal yang sama di ios-portal"; rumus LaTeX + header) dan memakai kontrak yang sama.

| # | Temuan | Jawaban user | Keputusan final |
|---|---|---|---|
| 1 | Di soal terakhir, tombol bawah harus berubah dari "Berikutnya" menjadi "Selesai" seperti di header. Bila masih ada soal belum terjawab: tombol tetap aktif membuka ringkasan, atau ikut header (tersembunyi sampai semua terjawab)? | **"Ikut header"** | `FINISH` hanya bila `allowFinish && !penalti && !breach` (syarat sama dengan menu "Selesai"); selain itu tombol tersembunyi. Pilihan "selalu aktif" **ditolak** (doc 06 §5.6) |
| 2 | Pembahasan: tetap PDF (Opsi A) atau ditambah field teks `question.explanation` berformat HTML+LaTeX (Opsi B)? | **Opsi A: tetap PDF** | Tidak ada kode/field baru untuk pembahasan; rumus di PDF otomatis tampil. Opsi B tetap tercatat sebagai pertanyaan terbuka di kontrak |
| 3 | Halaman pembahasan Tryout lama (`AkmExplanationPage`, `Html.fromHtml` + teks polos) ikut diberi rumus? | **Lewati** | Tidak disentuh; hanya alur AKM sekolah |
| 4 | Header `X-App-Platform` (`android`/`ios`) dan `X-App-Version` harus dikirim pada request download soal | Instruksi user (tanpa merinci cakupan) | Dikirim pada **semua** request lewat interceptor — lihat default di bawah |

**Deviasi/default lain (default saya, belum ditanyakan):**
- Header `X-App-*` dikirim di **semua** endpoint, bukan hanya download soal (satu tempat: `RequestInterceptor` di Android, `ApiTarget.headers` di iOS). Nilai versi = `versionName` / `CFBundleShortVersionString` (bukan `versionCode`). Bila backend menolak header tak dikenal di endpoint tertentu, batasi ke kasus download.
- Tersembunyi memakai **INVISIBLE**, bukan GONE, supaya tinggi daftar soal tidak berubah; "Selesai" tampil terisi (`colorPrimary`) dan "Berikutnya" outline, tombol minimal 48dp (doc 06 §5.6).
- App **ketat**: hanya rumus berpembatas (`\( \)`, `\[ \]`, `$$ $$`) yang digambar. Tawaran heuristik "LaTeX telanjang" (`\frac{a}{b}` tanpa pembatas) **belum diputuskan user** dan **tidak dibangun**; backend yang harus mengirim pembatas (sudah diperbaiki untuk jadwal 312881).
- Rumus tidak valid ditampilkan sebagai **teks sumber** (bukan kosong); warna rumus dioper eksplisit (putih saat opsi PG terpilih); `LinkMovementMethod` hanya untuk teks soal, tidak untuk baris jawaban.
- Soal benar/salah satu-jawaban: layar tak menampilkan `answers[0].answer`; kontrak meminta pernyataan + rumus ada di `question` (perilaku lama dipertahankan, bukan diubah).
- Perubahan `ApiException` (`fieldErrors`, `traceId`, commit `d3aa201c`) berada di luar AKM dan **belum** dicerminkan di doc 06 §16 / doc 10 §3.3.

**Belum diverifikasi (jangan diasumsikan selesai):**
- Rumus di **pilihan jawaban nyata** dari backend pada tampilan Android setelah backend memperbaiki pembatas (uji Infinix 19-10-2026 masih memakai data lama tanpa pembatas; yang terbukti hanya tes instrumented dan teks soal berumus di device).
- Tombol bawah di device untuk: swipe ke soal terakhir, mode pembahasan, penalti, soal tunggal (hanya `NextButtonModeTest`, logika murni). Yang terbukti di Infinix: tersembunyi → "Selesai" aktif setelah semua terjawab → membuka ringkasan; tanpa tombol hantu; aman dengan keyboard esai terbuka.
- Laporan user "force close saat klik soal berumus berikutnya" (19-10-2026): **tidak tereproduksi/tertangkap** — log konsol iPhone berakhir normal (exit code 0), logcat Infinix kosong, tidak ada crash log app di iPhone. Penyebab belum diketahui; tetap terbuka.


---

## 2026-10-22 — Tugas Guru: kontrak backend baru (`teacher-data-scope`) + perbaikan app lama (Android & iOS)

**Konteks:** tim backend mengubah API form tugas guru (spek: form tugas guru + `teacher-data-scope`; 4 endpoint assignment) agar sama dengan CMS sekolah.
Perubahan dikerjakan di app **lama** (`android-portal`, `ios-portal`) dan diuji di Infinix X1102 + iPhone 13 terhadap `dev.api.diskola.id`. App baru
(`Diskola-App-New`) tidak ada di disk — statusnya hanya dari dokumen. Dokumen yang diperbarui: `05` (§1.4, §1.8, §5.5, §6, §12–§15), `10` (§3.3, §3.7, indeks
endpoint), `05c` (§4 baru), `01`, `07` (satu baris). `repo lama/` sengaja **tidak** diubah (basi); selisihnya dicatat di `05` §14. Pertanyaan produk yang belum
dijawab = `05` §13 Q16–Q20.

| # | Temuan | Jawaban | Keputusan final |
|---|---|---|---|
| 1 | Spek: satu request create = satu tugas untuk **satu kelas**; multi-kelas = satu request per kelas dengan hasil sukses/gagal per kelas | **Satu kelas dulu** (user) | App lama (Android & iOS) memilih satu kelas; payload sudah per kelas sehingga multi-kelas bisa menyusul (Q20) |
| 2 | Dokumen spek kedua menyentuh sisi siswa & penilaian (`file[]`, 400/403/422, `data.id` null) | **Ikut dikerjakan** (user) | Android: `link` kosong tak dikirim, pesan 422 per file, 403 "Akses diblokir" + trace id, guard id ≤ 0 saat menilai; iOS: `file[]` selalu + penanganan 400/403/422/401 + guard id |
| 3 | iOS tidak punya form edit tugas (hanya buat); spek mencakup `update/{id}` | **Tambah edit di iOS** (user) | `AddTaskViewController(assignment:)`; dibuka dari ⋮ pada kartu tugas guru → action sheet "Edit Tugas" / "Lihat File" / "Batal". Hapus tugas di iOS tidak diminta |
| 4 | Saat dev masih memakai validasi lama (2026-10-20) create canonical ditolak 422 (minta `school_classes_id`, `school_subject_id`, `school_subject_schedules_id`, `grade`): kirim field lama berdampingan dengan canonical agar jalan di server lama? | **Tidak dijawab** — dev kemudian di-deploy | Tidak diterapkan; app lama hanya mengirim field canonical (spek §5 mengizinkan alias hanya untuk kompatibilitas) |

**Deviasi lain (default saya, belum ditanyakan):**
- **Bug alur lama diperbaiki** (dicatat sesuai `rules-global.md` §2): (a) daftar guru **tidak refresh setelah create** — handler hasil memanggil `fetchHomeworkTodo()` (bukan query list guru, ber-guard `prefBelum`), kini `prefTeacher = -1` + `fetchHomeworkTeacher()`; (b) **crash `PembelajaranPage.loadData()`** (`IllegalStateException` saat pindah tab cepat: coroutine melanjutkan ke `viewLifecycleOwner` setelah view hancur) — guard `if (view == null)`; (c) edit pada baris Room yang hilang membuat tugas **baru** (`homeworkId` tetap 0) — kini tutup form + toast; (d) `assignmentDay()` tanpa try/catch.
- Form guru: **deskripsi wajib** (dulu opsional; Q16); **batas waktu tanpa default** dan **Posting tidak menunggu preview link** (Q17); file soal ≤ 12 MB di klien; kelas/mapel yang bergantung scope tidak ditulis ke Room/singleton `listClass`/`listSchedule`/`listSubject`.
- Mode ditentukan **scope aktif**, bukan bentuk tugas yang diedit; mapel prefill dari `subjectId`.
- **Prefill `end_at` diurai dari `end_at_label`** (app baru: default sekarang, keputusan 2026-10-02 #3 — Q18) dan **filter "Semua" tidak dikirim bila 0**, indeks `filter[n]` berurutan (app baru: kirim `0`, keputusan #6 — Q19). Dua hal ini sengaja **tidak** diubah di app baru; hanya dibuka sebagai pertanyaan.
- Parsing JSON Android toleran: `HomeworkItem.schedule` nullable; `Assignment.id/score/scored/uploaded` memakai `@NullToZeroInt`.
- iOS (belum ada dokumen iOS di `docs-new`): satu request multipart (dulu dua: create lalu upload file → risiko tugas ganda saat retry); kolom link dibaca (satu baris → `link`, banyak → `link[]`); `validateJson` membawa `errors`/`trace_id`; `SessionGuard.handleUnauthorized()` untuk 401; daftar guru dimuat ulang setelah create/edit (`onCompleted` → `refreshAll()`); kolom Mata Pelajaran mode bebas berhalaman 50.

**Fakta terverifikasi (dev, 2026-10-19..21):**
- `teacher-data-scope` → `{"data":{"scope":"assigned"|"all","label":"Cakupan Data Guru (Mapel & Kelas)","restricted":bool}}`. Teramati `assigned/true` (guru tunggal), `all/false`, dan `assigned/false` (guru ber-role tambahan) — jadi **`restricted`, bukan `scope`**, yang menentukan form.
- `teachers/class`: `restricted=true` → 1 kelas (jadwal + wali); `restricted=false` → banyak kelas. `schedule-day` → `[{"key":"SENIN","lable":"Senin"},…]` (kapital + typo `lable`). `teachers/subject?take=100&skip=0` → 200 `{"data":[{id,name,icon_image}]}`.
- Create/update: assigned & bebas → 200 `{"message":"Berhasil membuat Tugas"}` / `{"message":"Berhasil update Tugas"}`; 422 berbentuk `{"message","errors":{field:[…]},"error","trace_id"}`; draft tetap utuh di app.
- `end_at_label` = tanggal absolut; tugas yang dibuat `2026-10-24 13:17:00` terisi kembali tepat saat Edit.
- Linimasa: sebelum backend baru aktif di dev (2026-10-20) dev menolak payload canonical (422 field lama) dan `teachers/subject` 404 (juga `api.diskola.id` saat dicek); setelah itu 200. Di iPhone, kolom Mata Pelajaran sempat "Error, Tap to retry" karena 404 itu.
- Daftar guru memakai Room `ORDER BY id DESC` sehingga tugas baru tampil di atas setelah fetch.

**Belum diverifikasi (jangan diasumsikan selesai):** 404 saat update tugas yang sudah dihapus, 401 (token dicabut), 413, alur siswa (akun uji = guru; `link` kosong & error 422/403 siswa tidak teruji); tampilan form iOS di perangkat (UI Automation iPhone tidak aktif — hanya log jaringan mode bebas yang tertangkap; build & `build-for-testing` lulus, tes unit iOS belum dijalankan); `downloaded=1` (selalu `0` pada uji, belum terbukti tersimpan sebagai wajib baca); apakah `end_at` boleh dihilangkan saat update dan cara menghapus file/link lama (belum ditanyakan ke backend); `schedule: []` (array kosong) pada tugas mode bebas dapat menggagalkan parsing seluruh daftar Android (hanya `null`/`{}` tertangani; belum teramati). **Temuan terpisah, belum diselidiki:** tab Pembayaran Android menampilkan dialog "Sekolah anda sudah tidak terdaftar / anda akan dikeluarkan" (tombol OKE → logout) bila `school.uuid` tersimpan lokal kosong (`PembayaranPage.kt:443-446`); muncul saat uji pindah tab cepat di Infinix, belum jelas apakah muncul tiap kali tab dibuka.

## 2026-10-22 — Tindak lanjut pembaruan dokumentasi di app baru: header `X-App-*`, rumus LaTeX, tombol "Selesai", dan Tugas guru (kontrak `teacher-data-scope`)

**Konteks:** pengecekan `docs/` (user: "cek di @docs ada pembaruan dokumentasi versi terbaru diskola") menemukan dua entri di atas (2026-10-19 AKM `e0a12b62`, 2026-10-22 Tugas guru) belum tercermin di app baru. Dikerjakan nomor 1 dan 2; gate `payment` (nomor 3) ditunda ke P2/P6.
Sumber kode lama yang benar: `DISKOLA MOBILE/android-portal` (`development` @ `d3aa201c`); klon di `proyek diskola/` tidak punya kedua commit. Laporan: `05c` §4, `06d` §6.

| # | Temuan | Jawaban user | Keputusan final |
|---|---|---|---|
| 1 | Edit Tugas: isi ulang batas waktu dari `end_at_label`? Filter "Semua" tidak dikirim bila 0? (membalik keputusan 2026-10-02 #3 dan #6) | **Ikuti app lama** | Prefill dari label (gagal ⇒ kosong + toast, wajib pilih ulang); filter "Semua" tidak dikirim, indeks `filter[n]` berurutan. Alasan keputusan lama (label relatif) tidak berlaku: di data nyata label berupa tanggal absolut |
| 2 | Form buat tugas: deskripsi wajib, batas waktu tanpa default, Posting tidak menunggu preview link, file soal ≤ 12 MB di klien (Q16/Q17, belum dikonfirmasi backend) | **Ikuti app lama** | Diterapkan semua; multi-kelas (Q20) ditunda — satu kelas per request |

**Deviasi lain (default saya, belum ditanyakan):**
- Kelas form dimuat dari `assignment/teachers/class` **tanpa menulis Room** (sebelumnya `mobile/teacher/school-class-room` dan menimpa tabel `classroom` yang dipakai Materi dan layar filter).
- `homework.schedule_day` kolom baru (Room v10) agar prefill Hari mode assigned lengkap; **`MIGRATION_9_10` eksplisit** karena `fallbackToDestructiveMigration` masih aktif dan versi tanpa migrasi menghapus seluruh DB, termasuk jawaban ujian yang belum terkirim.
- Mode ditentukan **scope aktif** (`restricted`), bukan bentuk tugas yang diedit; scope tidak di-cache (cache Presensi/Jurnal gagal ⇒ ALL, sebaliknya dari kebutuhan form); gagal baca scope ⇒ mode jadwal + banner.
- `link`, file, dan "wajib membaca" hanya dikirim bila "Upload Soal" aktif (app lama mengirim `downloaded` walau dimatikan — tidak ditiru).
- `"schedule": []` dibaca "tanpa jadwal" (PHP menulis map kosong sebagai `[]`); `Assignment.id/score/scored/uploaded` dan `CollectHomework.id` `null` ⇒ 0; `upload_at`/`scored_at` `null` ⇒ kosong; `LazyColumn` Tugas Terkumpul memakai kunci cadangan agar id 0 ganda tidak crash.
- Sisi siswa: pesan error server ditampilkan apa adanya (sebelumnya satu kalimat "Gagal mengirim tugas" menutupi 400/422/403), tombol Kirim ber-`loading`, link spasi-saja tidak dihitung jawaban; penilaian: id ≤ 0 tidak memanggil API.
- DatePicker memakai UTC untuk tanggal terpilih (kode lama memakai zona lokal ⇒ bisa mundur sehari pukul 00.00–07.00 WIB).
- "Selesai" (tombol bawah soal terakhir): menu header "Selesai" tidak dibuat (panah sudah setara); `hasBreach` memakai proxy `recovery != null`, klik tetap lewat `checkpoint("menu_kumpulkan")`.
- `X-App-Version` = `BuildConfig.VERSION_NAME` apa adanya (`1.0.0`); lihat risiko versi di `06d` §6.

- **Siapa yang boleh membuat tugas (2026-10-23, ditegur user: akun Infinix ber-role "Janitor" bisa membuat tugas).** Bukti dari `check-account` akun itu: `roles:[{name:"Janitor"}]` tunggal, **`rule.is_teacher:true`**, `rule_label:"Janitor"`, dan **`data.teacher` terisi (id + NIP)**; scope `{"scope":"assigned","restricted":false}`; `create` dibalas 200. Jadi menurut backend akun itu **memiliki data guru** (record `teacher` dipakai juga untuk karyawan) dan spesifikasi tidak melarang role bernama apa pun — yang ditutup hanya "user tanpa data guru" (endpoint teacher membalas 400). App mengikuti `rule.is_teacher` (keputusan `02a` P7: tidak menebak dari nama role). Yang sebelumnya **belum ditangani di app**: akun `is_teacher` tanpa record guru (`teacher.id` 0) tetap dibawa ke daftar `teacher_id=0` dan form yang hanya gagal saat Posting. Kini `AssignmentAccess.canManage(isTeacher, isStudent, teacherId)` (tes `AssignmentAccessTest`, 6): layar Tugas guru menampilkan "Tugas guru tidak tersedia — akun ini tidak memiliki data guru…" tanpa tab/FAB dan **tanpa satu pun request teacher** (`TugasGuruViewModel`, `TugasPenilaianViewModel`), dan form menutup sendiri dengan toast bila dibuka tanpa data guru (`UploadTugasViewModel.start`). Jalur ini **belum teruji di perangkat** (tidak ada akun tanpa data guru); akun Infinix (punya data guru) tetap masuk. Bila produk ingin memblokir role tertentu (mis. Janitor), itu harus dari backend (`is_teacher:false`/403) atau daftar role yang diberikan — app tidak menebak dari nama role.
- `BannerError` (komponen bersama, dipakai ±30 layar) kini hilang sendiri: 5–12 dtk menurut panjang teks (`BannerRules.autoDismissMillis`, 3 dtk + 60 ms/karakter), diperpanjang bila sistem meminta waktu baca lebih lama (`LocalAccessibilityManager`, mis. TalkBack); tombol X tetap menutup seketika dan pesan baru memulai ulang hitungan. Sebelumnya hanya X yang menutup (app lama memakai `Toast`, yang hilang sendiri) sehingga pesan lama menempel dan mendorong isi layar ke bawah (di form Edit Tugas ±210 px). Diuji di perangkat pada banner "Tugas telah dikerjakan": hilang ±5 dtk.
- **Bug nyata, ditemukan saat user melaporkan "Gagal menyimpan nilai" (2026-10-22): 41 metode API tidak pernah bisa terkirim.** Kotlin menulis `@Body Map<String, Any>` sebagai `Map<String, ? extends Object>` (`Any` tidak final) dan Retrofit menolaknya sebelum request dibuat ("Parameter type must not include a type variable or wildcard"). Terbukti di perangkat pada `TugasApiService.saveAssignmentScore` (tidak ada request di log; pesan itu tampil setelah pesan tetap diganti pesan asli). Tes refleksi `ApiServiceSignatureTest` menyapu semua `*ApiService` dan menemukan 41 metode, termasuk `AsesmenApiService.submitExamSchoolAnswer`, `reportExamViolations`, `reportExamProblem` (jawaban/pelanggaran/laporan ujian), `AuthApiService.refreshToken/checkNisn/verifyNisn/requestingStudent/requestingTeacher`, seluruh `KlaspayApiService`/`PpobApiService`/`EntrepreneurApiService`/`ProkesApiService`/`SosmedApiService` yang berbody Map, `MagangApiService.internshipAttend`, `AkunApiService.setShippingAddress`, `TryOutApiService`, `UjianLegacyApiService`. Perbaikan: `@JvmSuppressWildcards` pada 12 interface; tesnya kini hijau dan menjaga metode baru. Terbukti di perangkat hanya untuk penilaian tugas (`POST .../scored/74/student-assignment/2701` body `{"score":80}` ⇒ 200, daftar menampilkan 80 dan "Ubah Nilai"); sisanya **belum dijalankan di perangkat** — alur ujian, refresh token, dan Pembayaran/Magang/Prokes/Sosmed tetap perlu diuji satu per satu. Fitur Asesmen A1/A2 yang dicatat selesai (kirim jawaban, pelanggaran, laporan masalah) memakai metode yang terdampak, sehingga jalur-jalur itu tidak pernah sampai ke server sebelum perbaikan ini.
- Hapus tugas guru: pesan error server ditampilkan apa adanya (ditemukan saat uji perangkat: backend dev membalas 400 "Tugas telah dikerjakan" dan app menampilkan kalimat tetap "Gagal menghapus tugas…" yang menyembunyikan alasannya; app lama menampilkan `e.message`).

**Diverifikasi di perangkat (Samsung SM-A505F, server dev, akun guru uji mode bebas, 2026-10-22):** header `X-App-*` di log `API_REQUEST`; scope `restricted:false` ⇒ form mode bebas; create (payload kanonik ⇒ 200, tugas tampil di daftar), edit (prefill kelas/mapel/`end_at` dari label ⇒ update 200), batas waktu kosong di awal dan tanggal lampau tak bisa dipilih; migrasi Room 9→10 jalan pada DB nyata (tabel ujian masih kosong ⇒ pelestarian data belum terbukti); pesan hapus dari server tampil. Rincian: `05c` §4.
**Temuan backend dev:** `teachers/delete/87` ⇒ 400 "Tugas telah dikerjakan" pada tugas yang baru dibuat (tugas uji `UJI-FORM-GURU-BARU` id 87 tertinggal di server dev); `teachers/update` menerima `end_at` lampau dan `link` bukan-URL (200) — perlu ditanyakan ke backend apakah itu disengaja.

**Belum diverifikasi (jangan diasumsikan selesai):** rumus tergambar dan "Selesai" di layar (butuh akun siswa), form mode assigned, 422/403/404/413 di server (hanya tes JVM; server dev tidak bisa dipancing menolak), unggah file soal, `downloaded=1`, apakah backend masih menerima payload lama (tidak diuji; payload lama diganti seluruhnya). Tes JVM (405 lulus), build, `compileDebugAndroidTestKotlin` dan `minifyReleaseWithR8` dijalankan sebelum uji perangkat.

## 2026-10-27 — Pembayaran P2 (komponen bayar + Bayar SPP + gate `payment`) dan dialog kustom

**Konteks:** user: "lanjut ke gap selanjutnya" → Pembayaran P2 (dijeda setelah P1). Laporan: `docs/migrasi/08c-implementasi-pembayaran-p2.md`.

| # | Temuan | Jawaban user | Keputusan final |
|---|---|---|---|
| 1 | Q16 (doc 08 §18): bayar SPP/partisipasi gagal → toast "PIN Tidak Sesuai" untuk semua galat | **Pesan server** | Dialog "Pembayaran Gagal" berisi pesan server (galat jaringan → teks koneksi) |
| 2 | Q4: cache petunjuk pembayaran terduplikasi setelah 24 jam | **Ganti cache per channel** | Satu baris per channel (`payment_guide`), refetch setelah 24 jam menimpa; offline pakai salinan lama |
| 3 | Q5: detail menimpa `amount=total_amount`, `expired_at=""`, `cancelable=false` | **Perbaiki mapping** | `PaymentInvoiceRules.mergeDetail` mempertahankan amount, batas waktu, logo, partner reff, cancelable |
| 4 | (tambahan dari jawaban Q16) dialog yang masih default | **"gunakan dialog custom seperti yang ada di repo lama agar bisa reusable dan tampilan tidak kaku, gunakan skills mobile-ui-promax"** | `AppDialog` ditulis ulang mengikuti `widgets/AppDialog.kt` lama (badge nada, tag, detail, opsi, tombol primer/sekunder, nada dari judul); `AlertDialog` bawaan dan pemilih tanggal/jam ikut diganti |

**Deviasi lain (default saya, belum ditanyakan):** status bayar saldo yang gagal tidak lagi berakhir di halaman sukses (semangat Q8); tombol mati "Top Up Saldo" di baris saldo dihilangkan; "Cek Tagihanku" → "Fitur dalam pengembangan" sampai P4; batas waktu tak terbaca → teks mentah (bukan "Invalid date"). Rincian `08c` §3.

**Belum diverifikasi:** alur SPP dengan wallet aktif sampai server (akun uji Infinix belum mengaktifkan wallet; agen tidak mengaktifkan wallet dan tidak melakukan pembayaran sungguhan), gate `available=false`, migrasi Room 10→11 pada DB nyata.

## 2026-10-29 — Dialog error/peringatan alur login: bawah atau tengah?

**Konteks:** di halaman Password, "Login Gagal" muncul sebagai bottom sheet sedangkan "Perangkat Lain Terdeteksi" sebagai dialog tengah (warisan `ErrorUiModel` app lama, doc `02` §4.3). User: "harusnya semua sama, agar selaras — jangan error ada yang di bawah, ada juga yang di tengah."

| # | Pertanyaan | Jawaban | Keputusan final |
|---|---|---|---|
| 1 | Seragamkan ke mana: (a) semua dialog tengah, dibedakan nada, atau (b) error di bawah (bottom sheet) dan peringatan di tengah? | **(a) Semua di tengah, beda nada** | Error ("Login Gagal") = `AppDialog` ERROR (merah); peringatan ("Perangkat Lain Terdeteksi", "Ganti Password") = `AppDialog` WARNING (kuning). Berlaku di Login, Password, dan SSO. Deviasi dari app lama; `FeatureUnavailableSheet`, Toast aktivasi Klaspay, dan banner reset password tidak diubah. |

## 2026-10-30 — Hub Pembelajaran: `check-account` juga untuk akun tanpa NISN/NIK (login SSO/Google)

**Konteks:** user: di halaman Pembelajaran `check-account` harus dipanggil bersama `current-user`, karena sebagian besar data sesi diambil dari endpoint itu agar sesuai dan realtime.

**Temuan (bukti log Samsung SM-A505F, 08:44–08:47, setelah `login-sso` → `login-sso/school` → `payment/activate`):** empat kali hub dimuat, tiap kali hanya `current-user` + `payment/wallet` + `notification/summary`; **nol** `check-account`. Penyebab: `HomeViewModel.refreshProfile()` keluar diam-diam bila NISN kosong, dan akun Google ini memang tanpa NISN/NIK (`login-sso`: `"nisn": null`; `login-sso/school`: `"nisn_nik": null`) serta tanpa `student` di sesi. Sesi SSO jadi parsial selamanya (`user.uuid` kosong, `schoolId` 0, tanpa data kelas, `userProgress`/`current_subject` tidak pernah dimuat).

**Legacy:** `ApiWrapper.checkAccount(nisn, schoolId)` membuang `nisn_nik` bila kosong dan tetap memanggil dengan `{school_id}` saja (server mengenali user dari bearer token); docs `03` mencatat body `{nisn_nik?, school_id}`. App baru menambahkan penjaga `nisn.isBlank()` yang tidak ada di legacy.

**Keputusan (mengembalikan perilaku legacy, bukan pilihan produk baru):** `CheckAccountRequest.nisn_nik` opsional (Moshi tidak menulis `null`, body = `{"school_id":"…"}`); `HubRules.checkAccountNisn` memilih NIS siswa, lalu NIS/NIK akun, lalu `null`; guard NISN dibuang, guard `school.uuid` tetap tetapi dicatat (`Timber.w`). Urutan hub tetap seperti legacy: `check-account` dulu, lalu `current-user`, bell, agenda, banner paralel.

**Belum diverifikasi (jangan diasumsikan selesai):** respons server dev untuk `check-account` **tanpa** `nisn_nik` pada akun SSO belum pernah diamati di perangkat; bila ditolak (mis. 422) app menampilkan toast pesan server dan memakai sesi lama, jadi data parsial tetap dan perlu keputusan lain dari backend. Hanya tes JVM yang dijalankan (468 lulus).

## 2026-10-30 — Loading skeleton menggantikan baris abu-abu statis + spinner (tahap 1: komponen + layar P1)

**Konteks:** plan `docs/PLAN_SKELETON_LOADING.md`. `LoadingState` (baris abu-abu statis + spinner 18dp + "Memuat…") memberi dua tanda sekaligus dan tanpa animasi sehingga terlihat macet. Diganti skeleton ber-shimmer. **Menyimpang dari spesifikasi handoff** (`ref/design_handoff_auth_fase1/README.md`, `ANDROID_DESIGN_PATTERN.md`), disetujui user ("lanjutkan sesuai plan"); kedua dokumen diperbarui.

| # | Pertanyaan | Jawaban user | Keputusan final |
|---|---|---|---|
| 1 | Lingkup tahap pertama | **Komponen + P1** | `Skeleton.kt` + `LoadingState` baru; layar Jurnal, Tugas detail, Materi detail, SPP memilih varian. P2 (AKM) dan P3 tidak disunting |
| 2 | Batas waktu sebelum `ErrorState` | **30 dtk, ikut timeout jaringan** | Tanpa timer UI sendiri. Semua titik P1 sudah punya jalur keluar saat gagal (ErrorState/`notFound`/`invalid`/`loaded=true`/dialog) |
| 3 | Layar P3 | **Daftar saja** (Tugas siswa/guru, Materi, Poin siswa, Presensi) | Dikerjakan di tahap berikutnya; Home dan Notifikasi tidak |
| 4 | Branch & commit | **Branch sekarang, commit otomatis tiap tahap** | Commit per tahap, stage lewat path eksplisit |

**Penyimpangan dari plan sumber (default saya):** (a) tanpa "tampil minimal 400 ms" — menahan skeleton setelah data siap menambah latensi dan memaksa tiap layar memakai tiga keadaan; kedipan dicegah oleh tunda 200 ms + fade-in 150 ms di dalam `LoadingState`; (b) tanpa fase batas waktu (jawaban #2).

**Yang ikut berubah tanpa disunting:** `LoadingState` dipakai bersama, jadi layar AKM/P2 (`AkmScreen`, `AkmDetailScreen`, `AkmScoreScreens`, `AkmResumeScreen`, `AkmQuestionScreen`) otomatis memakai tampilan `ListRow` baru (tanpa spinner, dengan shimmer).

**Diverifikasi di Infinix X1102 (Android 15, akun siswa RIZKY), 2026-10-30:** karena server dev menjawab ±300–600 ms, skeleton **tidak pernah tampil** pada jaringan normal (Jurnal: layar menunggu cek gate, lalu daftar datang sebelum tunda 200 ms) — itu perilaku yang diinginkan, tetapi artinya tampilannya tak terlihat tanpa bantuan. Untuk melihatnya dipakai **salinan sementara di luar repo** dengan interceptor jeda 3,5 dtk (plus jeda 5 dtk di `TugasDetailViewModel.load` dan `JurnalScreen` dipaksa `Card`), direkam dengan `screenrecord` dan diurai `ffmpeg`; salinan dihapus dan build asli dipasang ulang. Hasil: varian `ListRow` (layar Jurnal, daftar Asesmen), `Card` dan `Detail` (Detail Tugas) tampil sesuai desain; kilau shimmer berpindah posisi antar-frame; baris kedua `ListRow` lebih redup; tanpa spinner/teks; layar Asesmen tetap utuh (tab "Jadwal/Nilai" ada, skeleton di bawahnya). **Reduce-motion:** `animator_duration_scale = 0` ⇒ piksel dua frame berjarak 0,5 dtk identik dan batang polos; kontrol dengan animasi normal ⇒ piksel berbeda (nilai pengaturan dipulihkan ke semula, `null`).

**Belum diverifikasi:** layar SPP (gate `payment` dev sedang `maintenance`; hanya kompilasi, dan varian `Card`/`ListRow` yang sama sudah terlihat di layar lain), skeleton `AkmQuestionScreen` di alur ujian (butuh memulai ujian sungguhan — tidak dilakukan), skeleton Detail Jurnal/Materi (jalur data berbeda; varian `Detail` yang sama terlihat di Detail Tugas), font 1,5× dan TalkBack (skeleton tanpa teks; hanya dibaca "Memuat"), `LoadingStateTest` (dikompilasi, tidak dijalankan). **Catatan desain:** kartu Jurnal asli berbingkai dengan chip waktu sehingga skeleton `ListRow` lebih datar dari isinya (varian `Card` lebih dekat); warna dasar `surfaceContainerHigh` di atas latar `surface` kontrasnya lembut — sama dengan baris statis sebelumnya.

## 2026-10-30 — Loading skeleton, tahap 3: layar daftar (Tugas, Materi, Poin siswa, Presensi)

**Konteks:** lanjutan entri di atas; lingkup P3 = **daftar saja** (jawaban user), tanpa Home/Notifikasi. Tiga varian baru di `Skeleton.kt`: `ListCard` (ikon + dua bar, kartu berbingkai — daftar mapel/materi), `TextCard` (judul, dua baris, pil + tanggal — kartu tugas dan poin), `Rows` (baris tabel — Presensi). `LoadingState` mendapat parameter `padding` (default tetap `Spacing.lg`): pemanggil mengoper padding isinya sendiri supaya tepi skeleton sejajar dengan kartu asli. Layar P1 ikut disejajarkan (sebelumnya skeleton menjorok 36 dp vs kartu 20 dp).

| Layar | Varian | Catatan |
|---|---|---|
| Tugas siswa (3 tab) | `TextCard` ×4 | tab tetap tampil, skeleton di bawahnya |
| Tugas guru (daftar + penilaian) | `TextCard` ×4 | |
| Materi: Mapel / daftar materi / guru | `ListCard` ×5 / ×4 / ×4 | Mapel sebelumnya hanya teks "Loading..." |
| Poin siswa | `TextCard` ×3 | `isRefreshing = loading && events.isNotEmpty()` — spinner tarik-segarkan hanya saat sudah ada isi |
| Presensi (Data Absensi / Rekap) | `Rows` ×7 / ×6 | sama: spinner tarik-segarkan hanya saat sudah ada isi |

Pola kondisi: `kosong && !loading → EmptyState`, `kosong → LoadingState`, selain itu `PullToRefreshBox`. Tidak disentuh: Poin guru (spinner "Memeriksa akses…" itu cek hak akses, bukan muat data), Tugas terkumpul (tanpa state loading), Home, Notifikasi.

**Diverifikasi di Infinix X1102 (akun siswa RIZKY), 2026-10-30,** dengan cara yang sama seperti tahap 1 (salinan sementara di luar repo: jeda 3,5 dtk pada semua request + DB Room terpisah yang kosong; salinan, berkas DB sementara, dan penanda sementara sudah dihapus, build asli terpasang ulang): Tugas siswa tab pertama (`TextCard`), Mapel (`ListCard`, tepi sejajar dengan kartu asli, daftar muncul tanpa kedip), Poin siswa (`TextCard` di bawah chip Pelanggaran/Prestasi), Presensi Data Absensi (`Rows` di bawah header tabel, tombol "Lakukan Presensi" tetap di bawah). Seluruhnya tanpa spinner/teks "Memuat".

**Kedip EmptyState sebelum daftar (ada sebelum tahap ini; diperbaiki setelahnya, 2026-10-30):** di Tugas siswa muncul "Belum terdapat tugas" ±0,5 dtk setelah skeleton. Akar masalahnya satu: layar tidak bisa membedakan "belum dijawab Room" dari "dijawab, memang kosong". Ada tiga jendela: (1) sebelum Room menjawab, `stateIn(…, emptyList())` kosong dan flag loading belum menyala; (2) flag loading dilepas begitu `insertAll` selesai, padahal `Flow` Room baru menyampaikan baris itu sesudahnya; (3) `MateriViewModel.subjects` memakai `debounce(400)` pada nilai pertama, jadi tiap membuka Mapel dengan cache daftarnya "belum diketahui" ±400 ms (jendela ini kesimpulan dari kode, belum diukur sebelum diperbaiki).

**Perbaikan:** kelas `ListLoad` (`viewmodel/ListLoad.kt`) menyatukan daftar Room dan flag loading-nya; ViewModel Tugas siswa (3 daftar), Tugas guru, Penilaian, Materi (Mapel dan materi per mapel) dan Materi guru memakainya, layar tidak diubah. `loading` tetap menyala selama (a) Room belum menjawab sekali, (b) ada `fetch` berjalan (dihitung, jadi dua fetch yang tumpang-tindih tidak saling mematikan), dan (c) sesudah `fetch` sampai daftar menyusul isi Room — dicek dengan satu pembacaan Room baru; bila Room memang kosong, tidak ada yang ditunggu; bila daftar tak kunjung menampilkan baris itu, berhenti menunggu setelah 1,5 dtk. Pencarian Mapel: nilai kosong (awal dan saat kotak dihapus) tidak lagi menunggu 400 ms, mengetik tetap menunggu. `MateriViewModel.materiOfSubject` memakai `emptyFlow()` (bukan `flowOf(emptyList())`) saat belum ada mapel, supaya "belum ada mapel" tidak terbaca "kosong". `PresensiViewModel`: flag loading dinyalakan sebelum cache dibaca (Rekap hanya bila belum ada baris, agar spinner tarik-segarkan tidak berkedip), dan tiap muat membawa nomor generasi sehingga `finally` job lama yang dibatalkan tidak mematikan flag job penggantinya (percobaan pertama tanpa nomor itu sempat dibatalkan karena balapan ini). Efek samping: `MapelGuruScreen` (tidak diubah) memakai `isRefreshing = loading`, jadi spinner tarik-segarkan menyala sebentar sampai Room menjawab.

**Diverifikasi:** 7 tes `ListLoadTest` (JVM, coroutines asli) — termasuk "Room tertinggal dari penulisan" dan "memang kosong tidak menunggu"; dua tes catch-up gagal bila logikanya dihapus (dicoba, lalu dikembalikan). Di Infinix dengan salinan sementara (jeda 3,5 dtk + DB kosong; salinan, DB, dan penanda dihapus, build asli terpasang ulang): Tugas siswa muat pertama = skeleton ±4 dtk lalu daftar, nol frame EmptyState (sebelumnya ±5 frame pada 10 fps); Presensi = skeleton tanpa "Data kehadiran tidak tersedia" (tinggal area kosong ±0,5 dtk karena tunda 200 ms skeleton + dialog cek fitur menutup); Mapel muat pertama dan buka-ulang dengan cache = nol frame EmptyState, daftar muncul begitu layar terlihat.

**Diuji dengan akun guru (Infinix, "Guru DEV · Guru Mapel", build asli, jaringan nyata, 2026-10-30):** Tugas guru (skeleton `TextCard` ±1,5 dtk di bawah filter Kelas/Mapel lalu daftar, tombol Tambah tetap ada), tab Penilaian (daftar dan tombol Lihat; spinner tarik-segarkan sesaat saat fetch ulang berjalan di atas isi cache), Materi guru (skeleton `ListCard` singkat lalu daftar), filter Mata Pelajaran = IPS (hasil memang kosong → "Belum terdapat materi" langsung, tidak macet di skeleton) lalu dikembalikan ke Semua, Mapel guru (daftar langsung, tanpa spinner), Poin guru (layar input data siswa), Presensi Data Absensi dan Rekap. Temuan dari uji ini: tab **Rekap** masih menampilkan satu frame "Data rekap tidak tersedia" sebelum skeleton, sebab `ensureRekap()` dipanggil dari `LaunchedEffect` sesudah frame pertama tab sedangkan `_rekapLoading` awalnya `false`; kini awalnya `true` (rekam ulang: area kosong ±0,4 dtk karena tunda 200 ms skeleton, lalu baris rekap, nol frame EmptyState).

**Belum diverifikasi:** Tugas siswa tab "Sudah Dikerjakan"/"Nilai", skeleton Penilaian saat muat pertama (di uji guru daftarnya sudah ada di cache), filter Kelas dan pull-to-refresh di layar guru, alur tambah/ubah/hapus tugas dan materi sesudah perubahan ini, Poin siswa (tidak berubah: `loading` sudah menyala sebelum frame pertama), font 1,5×, TalkBack. 480 tes JVM lulus, `compileDebugAndroidTestKotlin` lulus (`LoadingStateTest` mengiterasi semua varian, tidak dijalankan).

## 2026-10-30 — Layar Masuk/Pulang Sekolah: tampilan baru (visual saja) dan peta yang bisa digeser

**Konteks:** permintaan user: UI halaman melakukan presensi (peta + tombol Masuk/Pulang, `PresensiMasukScreen`) "terlalu simple". Jawaban user: **visual saja** — data, kondisi tombol, dialog, dan alur sama dengan app lama; tidak ada tombol atau langkah baru. Pertanyaan susulan user: peta terasa berat digeser, tidak seperti app lama → dua perbaikan di bawah.

**Tampilan:** hero gradien brand (idiom `HomeHeader`, bawah membulat 28 dp) berisi avatar, nama, pil "Presensi Masuk/Pulang", tanggal, dan jam langsung (`HH:mm` besar, `:ss` kecil); kartu peta menimpa hero tanpa `offset` negatif (hero melapor tinggi lebih pendek sebesar `HeroOverlap`, jadi sentuhan di peta utuh dan tidak ada ruang kosong di bawah halaman); pil jarak di peta ("364 m dari sekolah"); tombol "Lokasi saya" 48 dp berelevasi; radius pakai warna `primary` (bukan merah hardcode), marker pengguna biru, marker sekolah tetap merah. Satu **kartu status** (ikon + judul + keterangan, warna per nada) menggantikan teks satu baris; loading = skeleton `SkeletonBlock` di kartu yang sama. Jadwal jadi dua ubin "Mulai"/"Pulang". Tombol bawah diberi ikon Login/Logout dan garis pemisah atas. Dialog "Absensi Waktu Berhasil" kini bernada sukses eksplisit (sebelumnya terbaca "info" dari judulnya). Peta tidak lagi dirender ulang tiap detik: jam dibaca sebagai `State` hanya oleh hero.

**Teks baru** (selebihnya teks lama dipertahankan): judul status saat di dalam radius "Anda berada di wilayah absensi" (kata dari toast geofence legacy, `07` §5.5); keterangan "{jarak} dari titik sekolah · radius {radius}"; "Lokasi Anda sudah ditemukan" (di dalam radius tak bisa dinilai: radius ≤ 0 atau sekolah tanpa koordinat); pil "Presensi Masuk/Pulang"; "dari sekolah" di pil jarak; "Jadwal absen" tanpa "dari …" bila sumbernya kosong (sebelumnya "Jadwal absen dari " menggantung). Lokasi perangkat mati kini bernada peringatan (sebelumnya merah/error).

**Logika murni** (`PresensiRules`, +17 tes): `proximity` (tes selaras: "di luar radius" di kartu ⇔ `MasukStatus.OutsideRadius`, supaya kartu tidak pernah bilang "di luar" saat tombol terbuka), `formatDistance` ("85 m", "1,2 km", bulat tanpa desimal: radius 9999 m tampil "10 km"; sempat tampil "10,0 km" di Samsung), `masukCard` (nada + teks tiap `MasukStatus`), `scheduleSourceLabel`. `PresensiMasukViewModel` mendapat `distance` (aditif; `status` tidak berubah).

**Peta berat digeser — dua penyebab, keduanya sudah ada di versi Compose sebelum perubahan ini** (app lama: peta di `ConstraintLayout` tanpa scroll, dan kamera dipindah ke pengguna **sekali** saja lewat `myLocAttached`; fix berikutnya hanya menggeser marker):
1. Peta ada di dalam kolom `verticalScroll`, yang mengambil semua seretan vertikal: di Samsung seretan horizontal menggeser peta, vertikal tidak sama sekali. Perbaikan: selama ada jari di peta, scroll halaman dimatikan (`reportTouch` di Initial pass, tidak mengonsumsi apa pun).
2. `LaunchedEffect(user)` memanggil `camera.animate` di **setiap** fix lokasi (tiap beberapa detik), menarik kamera kembali ke pengguna. Perbaikan: kamera ke pengguna sekali pada fix pertama (sebelum itu ke sekolah); sesudahnya hanya marker yang bergerak; "Lokasi saya" tetap mengembalikan kamera. Animasi diluncurkan di scope layar supaya tidak terpotong saat efeknya dimulai ulang. Berlaku juga untuk peta Dinas Luar (komponen dipakai bersama). Seret vertikal di Dinas Luar diperbaiki dengan cara yang sama (`reportTouch` dipakai bersama, form `verticalScroll(enabled = !mapTouched)`); hanya dikompilasi, tidak dijalankan.

**Kartu status, teks satu baris:** `Row` kartu rata atas dengan kotak ikon 40 dp, sedangkan satu baris teks (mis. pesan server "belum waktunya pulang") hanya ±20 dp, jadi teks menempel di atas dan separuh bawah kosong. Kolom teks kini setinggi minimal kotak ikon dan isinya dipusatkan; teks panjang, atau teks + tombol "Aktifkan lokasi", tetap tumbuh dari atas. Diukur di Samsung: pusat teks dan pusat kotak ikon sama (y 1464 px).

**Kontras** (putih di atas gradien hero, dihitung): nama 8,6:1, tanggal 5,5:1, jam 4,9:1, `:ss` 4,0:1 (teks besar, cukup 3:1). Pil dengan latar putih 18% hanya 4,3:1 (perlu 4,5:1 untuk 12 sp) → latar 12% (4,9:1).

**Diverifikasi di Samsung SM-A505F (Android 11, akun siswa, build asli, 2026-10-30):** layar Masuk dalam keadaan "di dalam radius" (jarak, kartu hijau, jadwal, tombol "Masuk Terlambat" dari server, ikon, pil, marker biru; target sentuh 48 dp); geser vertikal peta berhasil; kamera tetap di area yang digeser saat fix baru masuk (pil 365 → 366 m) dan kembali ke pengguna lewat "Lokasi saya". 497 tes JVM lulus (480 + 17), `compileDebugAndroidTestKotlin` lulus.

**Belum diverifikasi (jangan diasumsikan selesai):** keadaan selain "di dalam radius" — di luar radius, lokasi mati, menunggu lokasi, loading, pesan server, gagal muat, jadwal belum diatur, dan font 1,5× — hanya lewat `@Preview` (kompilasi; tidak dirender di sini; app berjalan light-only sehingga varian gelap hanya penjaga token). Tombol Masuk/Pulang dan "Ya, Absen Sekarang" **tidak ditekan** (menulis data nyata ke server dev), jadi dialog sukses dengan nada baru belum terlihat. Sisi guru dan layar Dinas Luar tidak dijalankan (hanya guru/staf yang bisa membukanya): perbaikan kamera dan seret vertikal di sana hanya dikompilasi; tampilan layar Dinas Luar sendiri tidak diubah. Alur izin lokasi tidak diuji ulang (izin sudah diberikan di perangkat).

## 2026-10-31 — Isi Jurnal: watermark foto kehilangan alamat dan koordinat saat "Ambil Ulang Foto"

**Laporan user:** di Isi Jurnal (siswa), foto pertama berstempel lengkap (alamat, lat/lng, waktu), tetapi sesudah "Ambil Ulang Foto" stempelnya hanya tanggal dan jam; minta dicek juga di role guru.

**Yang pasti dari kode:** stempel Jurnal memakai `OffsiteWatermark.linesOptional`, yang membuang baris alamat dan koordinat bila tidak ada (baris waktu selalu ada) — jadi "hanya tanggal dan jam" berarti `prepareCapture` menghasilkan `CaptureSpot(null, null, null)`, yaitu **tidak ada fix lokasi**. Argumen rute `JurnalCapture` (alamat, lat, lng sebagai string) dan jalur "ambil" vs "ambil ulang" sama persis, jadi bukan di situ. Form Isi Jurnal dipakai kedua peran (`JurnalFormViewModel.isTeacher`), jadi guru memakai kode yang sama.

**Dugaan penyebab (dari kode, belum direproduksi di device):** `bestEffortLocation()` meminta fix **baru** berakurasi tinggi (`freshLocation()`, `maxUpdateAge = 0`) dalam batas 4 detik; bila lewat, seluruh blok dibatalkan (fallback `currentLocation()` tidak sempat jalan) dan hasilnya null. Di dalam ruangan 4 detik sering tidak cukup. Legacy (`JournalLocationHelper`) memakai `lastLocation` lebih dulu (instan), baru `getCurrentLocation`. Mengapa tepatnya di "ambil ulang" belum terjelaskan: GPS tidak bisa dipantau dari sini.

**Perubahan:** urutan untuk stempel: fix baru ≤ 4 dtk → bila gagal, posisi terakhir yang diketahui perangkat (`LocationProvider.lastKnownLocation`) asal umurnya ≤ 10 menit (`JurnalRules.isUsableLastKnown`; lebih tua dianggap tidak ada, supaya tidak salah tempat) → bila tidak ada, tetap hanya waktu. Alamat: bila geocoder tidak menjawab dalam 4 dtk, dipakai alamat yang diingat dari foto sebelumnya selama posisinya ≤ 100 m (`JurnalRules.sameSpot`). Tes JVM +3 (batas umur, "tempat yang sama", "tempat lain"). Yang dikirim ke server saat submit (`bestEffortLocation`) **tidak diubah**: tetap fix baru atau tidak sama sekali.

**Belum diverifikasi:** di device — Samsung tidak terhubung ke adb saat dikerjakan (hanya Infinix), jadi ambil foto lalu ambil ulang belum dicoba; tidak diketahui apakah laporan itu memang karena batas 4 dtk. Role guru: kodenya sama, tetapi tidak ada sesi guru di perangkat. Bila `lastLocation` juga kosong (GPS mati, atau belum ada aplikasi yang memakai lokasi), stempel tetap hanya waktu.

**Catatan di luar permintaan (tidak diubah):** aturan "fix baru ≤ 4 dtk atau tanpa koordinat" yang sama dipakai saat submit jurnal siswa/guru dan "Mulai kelas" (`JurnalViewModel`), jadi di dalam ruangan koordinat yang dikirim bisa kosong; legacy memakai `lastLocation`. Layar kamera juga tidak memperbarui stempel bila fix baru datang sesudah kamera terbuka.

## 2026-10-31 — Jurnal: uji berdampingan dengan app lama, penyebab siswa tak bisa isi jurnal, dan penyelarasan

**Konteks:** permintaan user "tes semua fitur jurnal dan perbaiki jika ada masalah", lalu "fitur jurnal di app baru dan lama masih berbeda" (yang terasa: alur dan tombol, data dan validasi). Target yang disepakati: **perilaku dan data sama dengan app lama; tampilan Compose yang lebih modern bukan bug.** Pembanding: kode dan layout `android-portal` (commit `92d7feba`) dan app lama v2.1.42 yang terpasang di Infinix (siswa kelas 10 UMUM). App baru: Samsung SM-A505F (siswa kelas DEVELOPER MOBILE, id 4969) dan Infinix (guru uji). Hari uji: Sabtu. Endpoint dan field kedua app **identik**; bedanya di perilaku.

**Kenapa siswa tidak bisa isi jurnal (terbukti, bukan pengaturan endpoint):** `student/journal-student` ditolak `422` dengan `errors.school_time_plot_id = "Kombinasi plot, mapel, dan kelas tidak ada di jadwal guru."` Server menerima jurnal siswa hanya bila kombinasi (jam, mapel, kelas) ada di jadwal seorang guru; kelas DEVELOPER MOBILE tidak punya jadwal guru di hari Sabtu (semua jam sintetis "Kosong"). Pembanding: app lama dengan siswa kelas 10 UMUM **berhasil** untuk jam 1 (mapel Bahasa Indonesia + guru) dan daftarnya berubah persis seperti spek (jam 1 jadi sesi nyata "Anda Hadir Dikelas Ini", jam 2 "Isi Jurnal", jam 3 "Kelas belum dimulai"). Tidak ada endpoint "bebas/dibatasi" untuk siswa: `teacher-data-scope` hanya untuk guru (siswa menerima bentuk `assigned/restricted:false` yang sama dan tidak dipakai selain memilih teks pesan). Pesan server memang sudah ditampilkan app baru, tetapi di banner paling atas area scroll sehingga tak terlihat saat halaman tergulir (lihat perbaikan 7).

**Bukti guru (Guru DEV, `restricted:false`):** `list-class` mengembalikan 19 kelas (tidak dibatasi) tetapi banner "Hanya menampilkan kelas/mapel/jam dari jadwal Anda" tampil karena dibaca dari `scope == assigned`. Jurnal guru untuk kelas DEVELOPER MOBILE berhasil (jam 2 dan jam 4); jam 1 ditolak 400 "Guru yang sama tidak bisa menggunakan plot waktu yang sama pada hari yang sama." (guru itu sudah punya sesi di jam 1); mengirim ulang jam yang sama juga 400, **bukan** 307, jadi jalur 307 tidak teruji. `journal/save` dengan `session_status = "Penugasan"` ditolak 422 "Isian session status yang dipilih tidak valid."; dengan "Terlaksana" sukses.

**Diperbaiki (selaras dengan legacy kecuali dinyatakan lain):**
1. **Verifikasi gagal:** dialog "Pilih status pembelajaran" tetap terbuka (legacy), alasan kegagalan ditulis di dalam dialog; sebelumnya dialog menutup dan error di banner tersembunyi di balik dialog.
2. **Mulai kelas:** tanpa dialog konfirmasi "Ya, Mulai" (legacy tidak punya; terkonfirmasi di device), dialog sukses bisa ditutup dengan ketuk di luar tanpa memaksa ke Detail, "Ok" tetap ke Detail. Spinner "memproses" dipertahankan (mencegah ketuk ganda; legacy tanpa).
3. **Detail guru: status sesi dua pilihan** (Terlaksana, Tidak Terlaksana); "Penugasan" yang dikirim server dibaca "belum dipilih" (legacy). Alasan di atas: server menolak "Penugasan". (Form siswa tetap tiga pilihan, itu status kelas, bukan status sesi.)
4. **Detail:** tolak `school_subject_schedule_id ≤ 0` dengan "ID jadwal mata pelajaran tidak tersedia" (legacy; sebelumnya memanggil `journal/0`); foto bisa diketuk untuk dilihat penuh (legacy; **tanpa cubit-zoom**, komponen itu belum ada di app); **pull-to-refresh** dengan "Buang perubahan?" bila ada perubahan (terverifikasi); gagal muat ulang menampilkan banner, bukan dialog blokir; "Menyimpan jurnal ..." kini khusus saat menyimpan (sebelumnya menyala juga saat memuat).
5. **Pemilih (mapel, guru, kelas):** server mengabaikan `name` (`name=Matematika` dan `name=Mukhlish` mengembalikan daftar yang sama), sedangkan legacy menyaring dari cache lokal; kini baris yang sudah dimuat disaring di sisi app (judul atau NIP, tanpa peduli huruf besar) dan pemilihan memakai id, bukan indeks. Kegagalan muat tampil di dalam sheet ("Data tidak ditemukan" diganti pesan), tidak lagi di balik sheet. Urutan pilihan tetap urutan server (legacy: id menurun dari cache); cache offline legacy tidak ditiru.
6. **`captureRequired` dan cakupan guru** diisi dari nilai terakhir sejak bingkai pertama (legacy); sebelumnya `false` sampai jaringan menjawab sehingga "Proses" cepat melewati foto wajib dan tanda `*` telat. Cakupan kini disimpan sebagai `restricted` (kunci pref baru `jurnal_teacher_scope_restricted`).
7. **Pesan error terlihat:** form menggulir ke atas saat error baru muncul (banner ada di dalam area scroll, "Proses" di bar tetap). Untuk siswa ditambah satu baris penjelasan: "Pilih mata pelajaran dan guru sesuai jadwal kelas Anda pada jam ini." (teks baru, menjawab keluhan "tidak ada keterangan"; bukan perubahan perilaku).
8. **Banner guru "Hanya menampilkan … dari jadwal Anda"** kini mengikuti `restricted` (disetujui user; legacy memakai `scope == assigned`). Terbukti di device: `assigned/restricted:false` dengan 19 kelas tidak dibatasi; setelah perubahan banner tidak tampil untuk Guru DEV.
9. **Foto dan lokasi:** izin lokasi diminta sekali per kunjungan bila kamera sudah diberi tetapi lokasi belum (legacy memintanya tiap "Ambil Foto"); urutan lokasi untuk stempel, kirim jurnal siswa, dan Mulai kelas: fix baru ≤ 4 dtk, lalu posisi terakhir yang diketahui bila ≤ 10 menit (`JurnalRules.isUsableLastKnown`; legacy `lastLocation` tanpa batas umur); posisi mock tidak dipakai untuk stempel; alamat yang diingat dipakai bila geocoder lambat dan posisi ≤ 100 m dari foto sebelumnya; ukuran foto: sebelumnya `inSampleSize` pangkat dua membuat foto 4000 px menjadi 1000 px (batas 1600/2560), kini di-decode lalu diskalakan tepat ke batas; berkas stempel lama dihapus saat ambil ulang, hapus, dan sesudah terkirim; hint "Foto suasana kelas saat KBM berlangsung" ditambah di kamera Jurnal (legacy).
10. **Pesan 401/403/5xx** pada kirim jurnal lewat pesan umum aplikasi, bukan "Unauthorized" mentah.
11. **307 tanpa id yang bisa dibuka:** pesan server dan form tetap terbuka (legacy), bukan dialog "Membuka detail" lalu kembali ke daftar. Tidak teruji (server tidak mengirim 307 pada percobaan).

**Sengaja tidak diubah:** tampilan Compose (kartu, chip siswa satu vs semua jam, badge status guru, header Detail, bottom sheet pemilih, tombol "Isi Jurnal" bernada Tonal); tombol Hub melewati gate (legacy sama); "memulai kelas" di Verifikasi (legacy sama); `late_at == ""` tanpa tombol (legacy sama; server mengirim `null`, terbukti); kunci jam siswa (hanya jam kosong pertama yang bisa diisi, legacy sama); yang sudah terdokumentasi sebagai keputusan (tanpa cache offline, pesan QR tidak valid, deteksi mock, dialog "Presensi Berhasil" sesudah Verifikasi, tanpa izin lokasi saat radius tak dibatasi, kartu Hub hanya siswa).

**Diverifikasi di device (31 Okt, build dengan semua perbaikan; 506 tes JVM lulus, +6; uji mutasi: sampleSize dan matchesQuery dirusak → 3 tes gagal, lalu dikembalikan):** form siswa (hint baru, pencarian pemilih menyaring, error di atas), watermark Samsung (foto pertama dan ambil ulang sama-sama lengkap; kegagalan aslinya **tidak bisa diulang** di sini, jadi perbaikan lokasi belum terbukti menyembuhkannya), form guru tanpa banner, Mulai kelas tanpa konfirmasi (+ tutup di luar), Detail dua pilihan status, pull-to-refresh dengan "Buang perubahan?", Simpan sukses dengan "Terlaksana". Data uji yang tertinggal di server dev: jurnal siswa app lama (jam 1, kelas 10 UMUM); sesi guru jam 2 dan jam 4 untuk DEVELOPER MOBILE (attendance 858 dan 859, keduanya dimulai; 859 disimpan "Terlaksana"); gagal/ditolak: jurnal siswa baru, jam 1 dan duplikat guru, simpan "Penugasan".

**Belum teruji (jangan diasumsikan selesai):** Verifikasi/Masuk Kelas siswa, QR fisik, dan jalur "Hadiri kelas" (jam 1 kelas siswa tidak bisa diisi siapa pun sehingga semua jam sesudahnya terkunci; `current_subject` tetap `{}` walau sesi dibuat dan dimulai, jadi kartu Hub "Check In/Isi Jurnal" dan nilai bawaan `attendance_id` (legacy 0, baru null) tidak teramati); jalur 307; pull-to-refresh tanpa perubahan; foto Detail (belum ada foto di sesi uji); font 1,5× dan TalkBack; sisi guru app lama (hanya dari kode). **Catatan data:** server menampilkan semua siswa kelas sebagai "hadir / Absen mandiri" di Detail sesi baru walau "Kehadiran" server H 0; legacy memakai pemetaan yang sama, tidak diubah.

## 2026-11-02 — Menu E-Raport di Hub Pembelajaran (siswa dan guru), app baru dan app lama

**Konteks:** permintaan user "tambahkan menu E-Raport di menu pembelajaran role guru dan siswa, di repo baru dan repo lama, tapi ketika diklik 'Fitur dalam pengembangan'". Fiturnya belum ada di kedua app; yang diminta hanya tile dan dialog.

**Diputuskan:**
1. **Posisi paling akhir di kedua app** (siswa sesudah Magang, guru sesudah Poin). App lama merutekan klik menurut posisi indeks; menyisipkan di tengah akan menggeser Agenda Mingguan, Poin, dan Magang.
2. **App lama (`android-portal`, `PembelajaranPage.kt`):** item `E-Raport` ditambahkan hanya bila `is_student || is_teacher`, sesudah penyisipan Agenda Mingguan. Posisi guru 6 bentrok dengan cabang `6 -> MagangPage`, jadi klik dicegat lebih dulu lewat `eRaportPosition` (diisi dari label saat daftar dibangun) sebelum `when (pos)`. Ikon baru `ic_eraport` (vector, ilustrasi datar di kotak mint 150 × 150, setara PNG ikon menu lain; dicek lewat render SVG, bukan di device).
3. **App baru:** `MenuKey.E_RAPORT`, ikon `grading`, `implemented = false`; klik jatuh ke cabang `else` yang sudah ada dan membuka `comingSoonDialog` ("Fitur dalam pengembangan"). Tes `HomeMenuGridTest` (4 tes) mengunci urutan, posisi terakhir, dan tamu tanpa E-Raport.
4. **Tanpa kelas:** tile terkunci seperti menu lain dan klik membuka dialog terkunci, bukan "dalam pengembangan" (selaras dengan ikon gembok). Akun guru yang bukan role "Teacher" (`StaffMenuAccess.PRESENSI_ONLY`) mendapat tile terkunci dengan pesan "hanya untuk akun dengan peran Guru". Tamu dan staf terbatas tidak mendapat E-Raport.

**Sengaja tidak dilakukan:** label "SOON" di app lama (kolom `soon` ada tetapi tidak dipakai menu lain; tidak diminta); `docs-new` (terakhir diubah 24 Okt dan bukan repo git, dokumen yang aktif adalah `docs/` ini).

**Diverifikasi:** tes JVM app baru `HomeMenuGridTest` 4 dari 4 lulus; kedua app compile (`compileDebugKotlin` app lama exit 0, app baru exit 0).

**Belum teruji (jangan diasumsikan selesai):** tampilan tile dan ikon di device (Hub 3 kolom: siswa 8 tile, baris terakhir berisi 2; guru 7 tile, baris terakhir berisi 1), dialog saat diklik di kedua app, dan perilaku akun guru dengan role non-Teacher di app lama.

## 2026-11-04 — Fase Magang (siswa): keputusan

**Konteks:** permintaan "lanjutkan ke fitur magang". Magang adalah satu-satunya bagian modul `07` yang belum dikerjakan (doc `07` §11, §14.4). Kondisi awal: hanya `MagangApiService` (semua `Map<String, Any>`, **tidak terdaftar** di Hilt, tanpa konsumen) dan tile di Home yang jatuh ke "Fitur dalam pengembangan". Cakupan (jawaban user): **seluruh Magang dalam satu fase**, boleh menulis data uji ke server dev, PR menunggu fase selesai. Detail: `docs/migrasi/07e-gap-magang.md`.

| # | Temuan | Keputusan |
|---|---|---|
| 1 | Legacy menyimpan jadwal/laporan/izin di Room tetapi **menghapus tabel jadwal sebelum tiap fetch**, dan `fetchReport` menimpa tabel izin dengan item laporan saja sedangkan sinkron izin menggabungkan dua sumber | **Tanpa Room (network-only)**; satu `MagangViewModel` memegang hasil fetch terakhir. Juga menghindari `MIGRATION_11_12` (tanpanya `fallbackToDestructiveMigration` menghapus jawaban AKM yang belum terkirim). Offline = `ErrorState` + coba lagi (legacy: daftar kosong + toast mentah) |
| 2 | Doc `07` §19#1 / Q1: "lokasi latar selalu wajib di presensi/magang, pertahankan" vs keputusan 2026-10-07: geofence dan lokasi latar dibuang | **Tanpa geofence dan tanpa `ACCESS_BACKGROUND_LOCATION`**; hanya izin presisi (FINE) saat layar presensi terbuka, ditolak → alert + keluar. Teks usang di `07:610,615,854` dan `10-infra` dinyatakan digantikan |
| 3 | Q5: deteksi lokasi palsu untuk presensi | Presensi masuk Magang menolak posisi mock (`MockLocationGuard`) |
| 4 | Legacy: "Laporan tersimpan" muncul walau `POST leave` gagal; hasil kirim diabaikan | Toast hanya bila sukses; gagal = banner dan form tetap (prinsip yang sama dengan form Poin/Izin: sukses hanya bila API sukses) |
| 5 | Legacy menolak foto laporan > 5 MiB dengan pesan "Foto lebih dari 8MB" | Pesan benar "Foto lebih dari 5 MB"; foto lebih besar **dikompres** (maks 5 MB) alih-alih ditolak, karena foto kamera modern sering > 5 MB. Bukti izin: dikompres ke ≤ 2 MB, hanya gambar |
| 6 | Legacy menampilkan "Laporan Harian" dan "Masuk" di tempat yang sama (bertumpuk bila dua flag true) | Kedua tombol tampil, disusun atas-bawah (laporan lebih dulu); berdampingan terlalu sempit pada font besar |
| 7 | Legacy mengunduh avatar untuk marker tanpa try/catch (loading macet / crash bila gagal) | Marker pengguna biru bawaan seperti layar Masuk |
| 8 | Setelah presensi masuk sukses legacy membuka ulang `MagangPage` (`CLEAR_TOP`) | Tutup layar presensi, kembali ke shell dan segarkan |
| 9 | "Panduan": overlay 4 langkah, pref `onboarding_magang_filter_done` ditulis tapi tidak pernah dibaca; "Hapus Pengajuan" selalu tersembunyi | ~~Panduan = bottom sheet statis (seperti Presensi)~~ **direvisi 2026-11-04 atas pertanyaan user: Panduan = tur spotlight 4 langkah seperti legacy** (`ui/components/CoachMarks.kt`, reusable); hapus tetap tersembunyi. Panduan Presensi disamakan juga (3 langkah legacy: Data Absensi, Rekap Absensi, Tambah Izin) |
| 10 | Format yang tidak pasti: tanggal `schedule.date`, `check_in_at`, jam `HH:mm:ss`, koordinat/radius sebagai String | Parsing toleran (`MagangRules.dateOf` membaca 10 karakter pertama; jam dipotong ke `HH:mm`; koordinat tak terbaca = 0.0 seperti legacy) |

**Diverifikasi di app lama (Infinix, siswa uji, 2026-11-04, hanya melihat):** Magang membuka tab Jadwal pada hari ini ("Jadwal Tidak Tersedia"), strip tanggal bulan berjalan dengan label "November 2026" (ketuk → pemilih bulan/tahun roda dengan "BATAL"/"PILIH"), menu "Panduan" dan "Tambah Izin"; ganti ke Juni 2026 → satu kartu "PT. TESTING MAGANG / 04 Jun 2026 / Pukul 15:00:00 : 19:15:00 / Laporan Harian"; tab Laporan → chip "Semua"/"Pilih Tanggal" dan satu laporan (masuk `2026-06-04 14:50:00`, keluar "-", lampiran "-", laporan "-"); izin: "Belum ada Pengajuan Izin", form menampilkan "Tidak ada jadwal magang yang dapat diajukan izin hari ini.". **Akibat bagi pengujian:** akun uji tidak punya jadwal hari ini, jadi presensi masuk dan izin tidak bisa dijalankan; hanya "Laporan Harian" untuk 4 Juni 2026 yang tampak bisa.

**Diverifikasi di app baru (S2, Samsung SM-A505F, siswa BUDI kelas DEVELOPER MOBILE, 2026-11-04):** akun ini punya **117 jadwal** (1 Jun–31 Des 2026) dan **satu jadwal hari ini** — PT AMEERA MEKKAH, 08:00–16:00, `can_attend=true`, `can_submit_leave_request=true`, `radius_enabled=true`, radius 5 m — jadi presensi masuk dan izin bisa dijalankan di akun ini (bukan JAZA). Bentuk respons `schedule` yang sebenarnya: `id` angka (mis. 2026110425), `company` = `{id, name, address, latitude: "string", longitude: "string", radius: angka}`, `schedule` = `{date: "yyyy-MM-dd", start_time/end_time: "HH:mm:ss"}`, `attendance` = `{status, can_attend, can_leave, can_submit_leave_request, leave_request: null, radius_enabled}`. Kosakata `status`: "Presensi Masuk" (bila `can_attend`), "Belum waktunya presensi masuk", "Waktu presensi masuk sudah lewat". **Temuan: `company.id` bisa 2149267629 (> `Int.MAX_VALUE`)** — DTO awal memakai `Int?` sehingga seluruh daftar gagal dibaca dan layar menampilkan teks Moshi mentah; diubah ke `Long?` dengan tes regresi dari baris nyata. Teks error mentah itu sendiri adalah kelemahan `AppErrorHandler` (cabang `else` meneruskan `throwable.message`); dicatat sebagai tugas terpisah. Perilaku legacy yang dipertahankan: layar terbuka pada **hari ini** (legacy: `selectedDate` awal = hari ini, `keepSelectedDay=true`) dan memilih bulan melompat ke tanggal 1.

**S3 — Presensi masuk (Samsung, BUDI, 2026-11-04):** Samsung ternyata berada 2 m dari titik PT AMEERA MEKKAH (radius 5 m), jadi alur lengkap dijalankan. **Penulisan ke server dev (sesuai izin user):** satu `POST mobile/internship/attend` untuk jadwal 2026110425 (presensi masuk BUDI, 4 Nov 2026 ±10:13) lewat tombol "Konfirmasi" → "Ya, Absen Sekarang"; respons 201; sesudahnya `attendance` = `{status: "Belum waktunya presensi pulang", can_attend: false, can_leave: false, can_submit_leave_request: false}`. Keputusan tambahan (default saya): **(11)** dialog konfirmasi "Lakukan Absensi" sebelum kirim — legacy langsung mengirim; waktu tercatat saat tap, jadi salah tap tidak bisa dibatalkan (sama dengan Masuk Sekolah di app baru); **(12)** tombol "Konfirmasi" menunggu posisi, juga saat radius mati (legacy menampilkannya dulu, lalu "Gagal mendapatkan lokasi Anda" saat ditekan); **(13)** perusahaan tanpa koordinat dengan radius aktif menampilkan "Titik lokasi tempat magang belum diatur…" alih-alih "di luar area" (legacy mengukur ke 0,0).

**S4 — Laporan (Samsung, BUDI, 2026-11-04):** tab Laporan membaca `GET attendance` bersama jadwal (satu `refresh`, hasil masing-masing: jadwal gagal tidak menyembunyikan laporan). Respons nyata: 2 laporan (hari ini tanpa keluar/lampiran/teks; 5 Jun 2026 lengkap dengan lampiran `https://dev.assets.diskola.id/internship-attendance/…jpg`), tanpa `record_type`. Keputusan tambahan: **(14)** waktu laporan diformat "04 Nov 2026, 10:13" (legacy menampilkan `yyyy-MM-dd HH:mm:ss` mentah); **(15)** `Pilih Tanggal` memakai pemilih rentang Material dengan "Pilih" yang menunggu dua ujung (legacy: `MaterialDatePicker` rentang); **(16)** meminta izin CAMERA saat memilih "Kamera" — legacy memakai Dexter dan pesan "Izin kamera dibutuhkan" bila ditolak (dipertahankan). **Penulisan ke server dev #2 (sesuai izin user):** satu `POST mobile/internship/leave` multipart (`id=2026110425`, `daily_report` 134 karakter, `file` foto kamera ±400 KB) untuk BUDI, 4 Nov 2026 ±10:58, lewat "Submit Laporan" → "Konfirmasi"; respons **201**, dialog "Laporan tersimpan"; `GET attendance` lalu memuat Keluar 10:58 + lampiran + teks, dan kartu jadwal menjadi "Presensi hari ini selesai". **Metode:** `can_leave` masih false, jadi tombol "Laporan Harian" dibuka dengan build uji sementara (tidak di-commit, sudah dibatalkan dan build bersih dipasang ulang); **temuan: server menerima laporan walau `can_leave=false`** — jendela pulang hanya ditegakkan klien. Gambar uji netral dibuat sendiri (derau) dan dihapus dari HP; picker dokumen Samsung tidak menerima ketukan terprogram, jadi Galeri belum teruji (kamera teruji).

**S5 — Izin (Samsung, BUDI, 2026-11-04):** daftar, form dan detail mengikuti legacy (jenis Sakit/Izin, bukti gambar, keterangan 10–5000, banner, "Hapus Pengajuan" tersembunyi). Teramati: setelah presensi masuk, `can_submit_leave_request` turun ke false dan form terkunci dengan banner legacy — izin hanya bisa diajukan **sebelum** presensi hari itu. Keputusan tambahan: **(17)** gambar bukti > 2 MB dikompres (legacy: ditolak "Ukuran file maksimal 2 MB"); **(18)** kesalahan tiap kolom tampil di bawah kolomnya (legacy: toast untuk jenis/keterangan, teks merah untuk file); **(19)** bukti di layar detail tampil sebagai gambar dengan pratinjau penuh (legacy: `ACTION_VIEW` ke aplikasi lain); **(20, 2026-11-04, atas pertanyaan user)** hal yang sama berlaku di detail **izin presensi sekolah**: gambar bukti tampil di halaman (legacy hanya nama file), PDF tetap membuka aplikasi lain; komponen bersama `ProofImage` + `ImagePreviewDialog` di `ui/components/ImagePreview.kt`. Catatan: mengetes presensi masuk memakai slot izin hari ini untuk BUDI, jadi pengiriman izin tidak bisa dijalankan di akun ini pada 4 Nov 2026.

**Uji izin lanjutan (permintaan user, 2026-11-04, Samsung, BUDI; build uji sementara tidak di-commit):** *Izin presensi sekolah:* form terbuka, validasi keterangan, kirim → `POST attendance/student/leave-request` 201 (Izin, "abc uji coba izin presensi dari aplikasi baru"), daftar "Menunggu", gerbang memblokir izin kedua, detail benar — satu izin uji tertinggal di dev (tidak ada fitur hapus). *Izin magang:* validasi per kolom teruji; dua kiriman uji **ditolak server 400**: jadwal besok → "Izin/sakit hanya dapat diajukan pada hari ini", jadwal hari ini → "Anda sudah melakukan presensi hari ini" (tidak ada data tertulis). **Temuan UI:** `BannerError` di atas kolom tergulir tak terlihat dan hilang sendiri; pada form laporan/izin Magang banner dipindah ke bar bawah. Bukti uji dimuat dari cache app karena picker dokumen Samsung tidak bisa dikendalikan adb.
