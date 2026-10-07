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

