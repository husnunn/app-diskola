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

**Implementasi:** lihat `SessionStore`/`SessionManager`/`FcmTopicManager` (`app/src/main/java/id/diskola/app/utils/session/`), `LoginViewModel`/`SsoViewModel`/`ResetPasswordViewModel`/`KlaspayActivationViewModel`/`SplashViewModel` (`viewmodel/`), dan layar-layar di `ui/screens/auth/`.

**Belum dikerjakan di fase ini (jangan diasumsikan selesai):**
- Gerbang banner Firebase In-App Messaging di splash (doc 02 §2.3, Q4) — `SplashViewModel` langsung lanjut ke router setelah cek versi, tanpa menunggu FIAM.
- Extra FCM `page`/`goto` saat app dibuka dari notifikasi (doc 02 §2.1 langkah 1, gap S4) — bagian dari routing notifikasi di dokumen 03.
- Dialog keamanan pasca-login (email belum terverifikasi, `EmailConfirmDialog`), validasi sesi berkala (`current-user`, cek `is_active`) — dokumen 02 §8/§9, dikerjakan bersama fase Home (dokumen 03).
- `versionCode`/keystore rilis, migrasi database Room `diskola.db` (bagian B di atas, masih terbuka).

---
