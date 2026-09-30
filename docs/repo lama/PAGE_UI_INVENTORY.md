# Inventaris UI — Diskola Mobile (Android, aplikasi lama)

> **Tujuan dokumen:** memetakan setiap halaman/dialog yang **aktif** di aplikasi lama (elemen UI konkret
> per layar), sebagai bahan untuk redesign & migrasi ke Jetpack Compose. Ini melengkapi
> `docs/REBUILD_PROMPT.md` (yang berisi arsitektur & alur) dengan detail visual per layar.
>
> **Metode:** dibaca langsung dari layout XML (`app/src/main/res/layout/*.xml`) dan
> Activity/Fragment/Compose Kotlin-nya. Halaman yang sudah dikonfirmasi dead code (tidak dipanggil dari
> alur manapun) TIDAK dimasukkan — kecuali ditandai eksplisit "belum aktif di produksi" (kode sudah
> lengkap, tapi entry point-nya sengaja dinonaktifkan).
>
> **Cara baca:** `## Nama Fitur` → `### Nama Halaman` → daftar `Elemen:` singkat (bukan deskripsi
> panjang). Urutan alur antar halaman disebutkan di awal tiap fitur bila relevan.

---

## Auth

Lokasi: `app/src/main/java/id/diskola/app/pages/auth/**` — sudah 100% migrasi ke Jetpack Compose,
di-host oleh `AuthActivity` + `AuthNavHost`.

**Alur utama:** Splash → Onboarding (hanya jika belum pernah) → Login (Masukkan NISN) → Login
(Masukkan Password) → Home. **Alur SSO:** Login (Masukkan NISN) → "Masuk dengan Akun Google" → Login
SSO → Home (atau Aktivasi Klaspay jika `klaspayActive` belum aktif). Jika versi app sudah usang, Splash
langsung memunculkan dialog wajib update yang mengunci alur.

### Splash
Elemen: logo Diskola (animasi fade+scale in), teks "Memuat…", circular progress indicator (saat
status masih Loading), watermark "DISKOLA Mobile v{versi}" di bawah (dari `AuthScaffold` yang dipakai
bersama semua layar auth).

### Onboarding (3 halaman, swipe pager)
Elemen: logo Diskola, ilustrasi per halaman (pembelajaran, kewirausahaan, dompet digital), judul,
deskripsi, page indicator dot (3 titik), button teks "kembali" (halaman ke-2/3), button "berikutnya"
(halaman 1-2), button "selesai" (halaman terakhir → set pref onboard=true → Login).

### Login — Masukkan NISN (LoginFormScreen)
Elemen: header selamat datang (logo, "DISKOLA", tagline "SISTEM INFORMASI AKADEMIK TERPADU", judul
"Selamat Datang"), badge status "Aktif", judul kartu "Login Akun", form input NISN/NIS/NIK (numeric,
contoh format di bawah), form pilih sekolah (buka bottom sheet SchoolPickerSheet), checkbox Syarat &
Ketentuan (link ke dialog kebijakan), button "Masuk" (loading state, disabled sampai semua terisi),
pemisah "atau", button "Masuk dengan Akun Google" (ikon Google), kartu bantuan "NISN/NIK belum
terdaftar? Hubungi operator sekolah Anda" + button "Bantuan".

### Login — Masukkan Password (LoginProcessScreen)
Elemen: logo, button back, judul "MASUK SEBAGAI", avatar, nama pengguna, badge peran, label kelas,
NIS/NIK, form input Password (toggle show/hide), link "Lupa password? Reset", button "Login" (loading
state), kartu bantuan (kontak WhatsApp/telepon operator).

### Login — SSO Google (LoginSsoScreen)
Elemen: logo, button back, judul "MASUK SEBAGAI", avatar & nama akun Google, badge peran, NISN (jika
dikenali), nama kelas (jika ada), nama sekolah (jika sudah terhubung) — atau form pilih sekolah (jika
belum), button "Iya, itu saya" + "Tidak, bukan saya" (jika dikenali), atau button "Daftar" (jika
belum, aktif setelah pilih sekolah).

### Bottom Sheet — Pilih Sekolah (SchoolPickerSheet)
Elemen: judul "Pilih Lembaga Sekolah", button tutup (X), input pencarian "Cari nama sekolah"
(debounce), list sekolah infinite scroll (avatar/inisial, nama, kota, centang jika terpilih), loading
"Memuat daftar sekolah…", empty state "Sekolah tidak ditemukan".

### Dialog — Konfirmasi Lanjut Sebagai Tamu (GuestConfirmDialog)
Muncul saat SSO Google tidak terhubung ke sekolah manapun. Elemen: judul "Konfirmasi", teks
penjelasan, kotak frasa konfirmasi yang harus diketik ulang, form input konfirmasi, button "Batal",
button "Lanjut" (aktif hanya jika input persis sama).

### Dialog — Syarat & Ketentuan (PolicyDialog)
Elemen: judul, konten HTML kebijakan (WebView + loading spinner), button "SAYA PAHAM" (menandai
checkbox terms di layar Login).

### Dialog — Update Wajib (UpdateRequiredDialog)
Tidak bisa ditutup. Elemen: judul "Update Tersedia", teks, button "Update" (buka Play Store).

---

## Home

Lokasi: `app/src/main/java/id/diskola/app/pages/home/**` — `HomePage.kt` adalah shell/container utama
seluruh aplikasi setelah login.

### Home — Shell Utama
Elemen: bottom navigation 3 tab (Pembelajaran, Pembayaran, Akun) dengan avatar profil di tab Akun,
drawer kanan (menu profil), fragment container navigasi utama, dialog "Peringatan" tanggal/waktu
otomatis, dialog konfirmasi Logout, notification permission dialog (Android 13+).

### Home — Drawer/Menu Akun
Elemen: badge peringatan "Email belum terverifikasi", tombol Pengaturan Profil, Kontak & Email, Ubah
Password, Kartu Pelajar (khusus siswa), Kebijakan & Privasi, Tentang Diskola, Perangkat Terhubung,
Logout, label versi aplikasi.

### Home — Dialog Verifikasi Email (Wajib)
Elemen: judul "Masukkan Email", pesan info, form input email + validasi, button "Verifikasi
Sekarang", button "Nanti Saja" (disembunyikan jika wajib), dialog lanjutan "Email sudah digunakan".

### Home — Dialog Konfirmasi Email Terkirim
Elemen: ilustrasi, judul "Verifikasi Email Sekarang", pesan cek email, button "Ok Saya Konfirmasi
Dulu", button "Ubah Email".

### Home — Dialog Ubah Password Default
Elemen: judul "Ubah Password", pesan keamanan akun, form password lama/baru/konfirmasi, button "Buat
Password Baru", button "Nanti Saja", dialog konfirmasi "Ubah Password Sekarang?", dialog sukses
"Password Diperbarui".

### Home — Dialog Buat Username
Elemen: judul "Buat Username", pesan info sosial media, form input username, button "Konfirmasi",
button "Nanti Saja", dialog sukses "Username Berhasil Dibuat".
> Catatan: pemicu dialog ini tidak ditemukan aktif di alur manapun saat ini (kandidat dead code).

---

## Akun

Lokasi: `app/src/main/java/id/diskola/app/pages/akun/**` — beranda akun & sub-halaman inti sudah
Compose (`AkunPage2` meng-host `AkunHomeScreen`; `SettingAkunPage`/`SettingContactPage`/`DevicesPage`
meng-host layar Compose di folder `compose/`); `ProfilePage` (profil publik), `ProfilePicturePage`,
`TermsPage`, `AboutPage` masih XML/legacy.

**Alur:** Tab Akun (Beranda) → Pengaturan akun / Kontak / Ubah password / Kartu pelajar / Syarat &
ketentuan / Tentang Diskola / Keluar. Avatar di beranda membuka Ubah Foto Profil. Terpisah: `ProfilePage`
dibuka dari halaman lain (mis. notifikasi/mention) untuk profil publik.

### Akun — Beranda (AkunHomeScreen)
Elemen: avatar (klik untuk ganti foto), nama tampilan, `@username`, banner peringatan email belum
terverifikasi (kondisional), section "Akun": row "Pengaturan akun" (subtitle Nama/email/telepon), row
"Kontak" (subtitle verifikasi email & nomor), row "Ubah kata sandi" (kondisional, punya NIS/NIK), row
"Kartu pelajar" (kondisional, khusus siswa); section "Tentang": row "Syarat & ketentuan", row "Tentang
Diskola", row "Keluar".

### Dialog — Konfirmasi Logout (LogoutConfirmDialog)
Elemen: judul "Logout", teks konfirmasi, button "Batal", button "Logout" (merah).

### Akun — Pengaturan Akun (SettingAkunScreen)
Elemen: app bar "Pengaturan akun" + action "Simpan" (aktif jika ada perubahan), teks deskripsi, label
"Peran" + daftar peran, form input "Nama" (wajib), form input "Email" (readonly kecuali mode edit,
keterangan status verifikasi), button "Verifikasi email" (saat mode edit), form input "Nomor telepon".
Ubah email memicu Google Sign-In → dialog sukses "Perbarui Email" atau dialog "Email sudah digunakan".

### Akun — Kontak (SettingContactScreen)
Elemen: app bar "Kontak", deskripsi, form input "Email" (toggle Ubah/Selesai + status terverifikasi),
form input "Nomor telepon" (toggle Ubah/Selesai), button "Kirim verifikasi email" (outline), button
"Perbarui kontak".

### Akun — Perangkat (DevicesScreen)
Elemen: app bar "Perangkat" + action refresh & "Keluar dari perangkat lain", deskripsi, list
perangkat/sesi aktif (nama, waktu terakhir dipakai, button "Keluar" per item), loading skeleton, empty
state "Belum ada perangkat" + button "Muat ulang".

### Akun — Ubah Foto Profil (ProfilePicturePage)
Elemen: toolbar dengan back, foto profil besar, menu "Edit" (atau info "hubungi admin" untuk siswa)
dan "Simpan", pilihan sumber Kamera/Galeri, editor crop (rasio 1:1), dialog konfirmasi keluar dengan
perubahan belum disimpan.

### Profil — Header & Tab Post (ProfilePage + ProfilePostPage)
Elemen: toolbar "Profil", avatar (zoom/ganti), `@username`, nama, badge peran, label kelas (siswa), 3
tab ikon (Post/Media/Ebook), swipe-to-refresh, list post/feed (like, opsi hapus untuk pemilik, klik
mention buka profil user lain), dialog konfirmasi hapus post.

### Profil — Tab Media (ProfileMediaPage)
Elemen: grid foto/media (3 kolom) dari feed bertipe gambar.

### Profil — Tab Ebook (ProfileEbookPage)
Elemen: list ebook (cover, placeholder/error cover, tombol "Lihat" membuka PDF).

### Akun — Syarat & Ketentuan (TermsPage)
Elemen: toolbar "Kebijakan & Privasi", WebView konten kebijakan, progress loading, dialog error
"Gagal memuat halaman" + button "Muat ulang".

### Akun — Tentang Diskola (AboutPage)
Elemen: logo app, nama app, chip versi, kartu "Hubungi Kami" (Email, Telepon), section "Ikuti Kami"
(YouTube, Website), kartu "Yang Baru" (bullet changelog).

### Dialog — Setel Email (EmailSettingDialog)
Muncul otomatis jika email belum terverifikasi. Elemen: judul "Masukkan Email", form input email
(readonly, ikon edit memicu Google Sign-In), teks info hubungi admin, button "Verifikasi Sekarang",
button "Nanti Saja".

### Dialog — Konfirmasi Email (EmailConfirmDialog)
Elemen: ilustrasi, judul "Verifikasi Email Sekarang", pesan info kirim email, button "Ok Saya
Konfirmasi Dulu", button "Ubah Email" (kondisional).

### Dialog — Email Sudah Digunakan
Elemen: judul "PEMBERITAHUAN", ilustrasi, pesan error dinamis, button "Tutup".

> **Dead code ditemukan:** `PairingPage` (`pages/akun/PairingPage.kt`, terdaftar di nav graph tapi
> tidak ada trigger aktif) dan `SetUsernameDialog` (`pages/home/dialogs/username/`, satu-satunya
> pemanggilnya di `CreatePostPage.kt` sudah di-comment) — keduanya tidak terpasang ke alur manapun saat
> ini.

---

## Ganti & Reset Password

Lokasi: `pages/changepass/**`, `pages/resetpass/**` (layout XML legacy). **Alur:** ChangePass dibuka
dari beranda akun ("Ubah kata sandi"); ResetPass dibuka dari Login (Masukkan Password) via link
"Reset".

### Ganti Password — Form
Elemen: toolbar + back, label peringatan jangan bagikan password, form input Password Lama/Baru/
Konfirmasi, button "simpan perubahan" (aktif jika lama & baru terisi), overlay loading, dialog error
"Perubahan Gagal".

### Ganti Password — Sukses
Elemen: ilustrasi sukses, teks konfirmasi, button "ok, kembali".

### Reset Password — Form Email
Elemen: toolbar + back, label info link pemulihan, judul kartu "email pemulihan", form input email,
button "kirim sekarang" (aktif jika email > 3 karakter).

### Reset Password — Terkirim
Elemen: ilustrasi, judul "link pemulihan terkirim", info email tujuan, button "kirim ulang" (disabled
dengan hitung mundur 3 menit).

---

## Verifikasi & Pengajuan Data

Lokasi: `pages/verification/**` (layout XML, nav graph `verification_nav.xml`), dibuka dari Pembelajaran
via dialog "Apakah NISN/NIS/NIK anda telah terdaftar disekolah anda?" — **"Sudah"** → alur Verifikasi;
**"Belum"** → alur Pengajuan (Approval).

**Alur Verifikasi:** Cek Data (NISN) → Pilih Kelas & Kode Sekolah → Home.
**Alur Pengajuan:** Form Data (tab Siswa/Guru) → Pilih Kelas & Kode Sekolah (tab Siswa) atau langsung
submit (tab Guru) → Home.

### Verifikasi — Cek Data (NISN/NIS/NIK)
Elemen: judul "VERIFIKASI DATA", avatar profil, nama, badge role, input NISN/NIS/NIK (numeric), label
info hubungi admin, button "Cek Data", dialog loading "Sedang mencari data pengguna...", dialog gagal
"Verifikasi Gagal".

### Verifikasi — Pilih Kelas & Kode Sekolah
Elemen: tombol back, judul "VERIFIKASI DATA", avatar + nama + role + kelas (kondisional), field
"Pilih Kelas" (bottom sheet), input "Kode Sekolah" (caps), button "Verifikasi", dialog loading "Proses
login akun", dialog gagal, notifikasi lokal "Ganti Password" (jika masih password default).

### Pengajuan Data — Form (tab Siswa/Guru)
Elemen: judul "PENGAJUAN DATA", avatar + nama + role, TabLayout 2 tab. **Tab Siswa:** input Nama,
dropdown Jenis Kelamin, input Tanggal Lahir (date picker), input Kota Kelahiran, input Alamat, button
"Berikutnya". **Tab Guru:** input Nama, input NISN/NIS/NIK, dropdown Jenis Kelamin, input No. Telp,
field "Pilih Level Pengguna" (bottom sheet), input Kode Sekolah, label info, button "Submit
Pengajuan"; dialog loading, dialog gagal "Pengajuan Data Gagal".

### Pengajuan Data — Pilih Kelas (lanjutan tab Siswa)
Elemen: tombol back, field "Pilih Kelas" (bottom sheet), button "Submit Pengajuan", dialog loading,
dialog gagal, notifikasi lokal "Ganti Password".

### Bottom Sheet — Pilih Kelas / Pilih Level Pengguna
Elemen: toolbar dengan tombol close, input pencarian (debounce), RecyclerView daftar (kelas / role),
progress + label loading.

---

## Kartu Pelajar

Lokasi: `pages/studentcard/**` (layout XML).

### Kartu Pelajar (StudentCardPage)
Elemen: toolbar "Kartu Pelajar", foto profil siswa (bulat), nama siswa, kartu "Data Pribadi": NIS,
NISN, Nama Lengkap, Tempat & Tanggal Lahir, Jenis Kelamin, Angkatan, Alamat.
> Catatan: halaman ini murni tampilan data pribadi read-only — model tema kartu visual
> (`themes`/`ThemeData` di `StudentCardModels.kt`) ada di kode tapi **tidak dipakai** di mana pun,
> jadi tidak ada tampilan kartu visual/QR/barcode aktif saat ini.

---

## Pembelajaran

Lokasi: `pages/pembelajaran/**` — landing tab "Beranda", halaman paling sering dilihat user.

### Pembelajaran — Beranda
Elemen: header logo & nama sekolah, tombol notifikasi (badge counter), tombol refresh, avatar & nama
pengguna, banner kelas berlangsung (nama kelas, waktu, tombol "Check In"/"Hadiri", tombol "Isi
Jurnal"), banner absensi kantor (status, jam kerja, info agenda, tombol check-in/out), banner
verifikasi data (untuk guest), grid menu 3 kolom (Materi, Tugas, Presensi, Jurnal, Agenda
Mingguan/Asesmen, Poin, Magang — sebagian terkunci/badge "segera"), carousel banner iklan/edutainment,
dialog pilih metode presensi (Verifikasi Jurnal/Scan QR), dialog "Fitur ini terkunci", dialog "Fitur
dalam pengembangan", dialog hasil presensi, popup info khusus role Guest.

---

## Presensi

Lokasi: `pages/presensi/**` (30 file) — fitur harian dengan cakupan terbesar.

### Presensi — Shell Utama
Elemen: toolbar dengan action "Tambah Izin" dan "Panduan" (onboarding guide), 3 tab (Kelas, Data
Absensi, Rekap Absensi), dialog peringatan tanggal/waktu ponsel tidak otomatis.

### Presensi — Tab Kelas (Jadwal Hari Ini)
Elemen: navigasi tanggal (prev/next), swipe refresh, list card jadwal per mapel (foto, nama mapel,
guru, jam, status hadir), tombol "Hadiri kelas"/"Mulai kelas", tombol "Isi Jurnal", dialog pilih
metode presensi manual/QR, bottom sheet form password kelas (+ hitung mundur), bottom sheet pilih
toleransi keterlambatan (5/10/15 menit), dialog sukses absensi.

### Presensi — Tab Data Absensi
Elemen: navigasi bulan, tabel header (Tanggal/Jam masuk/Jam keluar), swipe refresh, list riwayat
harian, tombol "Lakukan Presensi", dialog pilih tipe absensi Sekolah/Luar Sekolah (guru), dialog izin
lokasi latar belakang, bottom sheet detail absen luar lokasi (status, waktu, alamat, catatan, foto),
dialog preview foto.

### Presensi — Tab Rekap Absensi
Elemen: navigasi tahun, tabel rekap bulanan (Bulan/H/T/I/S), swipe refresh, list rekap per bulan.

### Presensi — Absen Masuk (Onsite)
Elemen: nama pengguna, tanggal & jam real-time, status jarak/lokasi, tombol aksi presensi dinamis,
dialog konfirmasi "Lakukan Absensi", dialog jam sekolah belum diatur, dialog izin lokasi, dialog
sukses.

### Presensi — Absen Luar Lokasi (Offsite)
Elemen: nama pengguna, tombol ambil foto, status/error label, tombol aksi dinamis, dialog konfirmasi,
dialog sukses.

### Presensi — Kamera Absen Offsite
Elemen: preview kamera, overlay watermark (alamat, koordinat, waktu), tombol tutup, tombol jepret.

### Presensi — Detail Jurnal Kelas (Guru)
Elemen: nama mapel & guru, dialog edit "Tujuan Pembelajaran", radio "Status Pembelajaran"
(Terlaksana/Tidak), tabel ringkasan kehadiran (H/I/S/A), list nama siswa + status per baris,
notifikasi kelas belum dimulai (read-only).

### Presensi — Isi Jurnal Guru / Siswa
Elemen (guru): toolbar "Detail Jurnal", picker plot waktu, picker kelas (bottom sheet cari), picker
mapel (bottom sheet cari), form tujuan pembelajaran, status proses/simpan. Elemen (siswa): plot waktu,
mapel, nama guru, radio status pembelajaran, field penugasan.

### Presensi — Data Jurnal (List Harian)
Elemen: navigasi tanggal, swipe refresh, list card kelas per hari (foto, mapel, guru, jam, tombol
Detail/Mulai kelas/Isi Jurnal), empty state "Data Jurnal Kosong".

### Presensi — Daftar & Ajukan Izin
Elemen (daftar): tab filter (Semua/Pending/Disetujui/Ditolak), list riwayat, empty state, tombol
"Ajukan Izin". Elemen (form): radio jenis (Sakit/Izin), upload surat/file pendukung, input
keterangan, tombol "Kirim". Elemen (detail): icon status berwarna, jenis & tanggal, keterangan, alasan
penolakan (jika ditolak), link file bukti, tanggal direview.

---

## Agenda Mingguan

Lokasi: `pages/agenda_mingguan/**` — agenda staf mingguan, khusus guru.

### Agenda Mingguan — Daftar Agenda
Elemen: toolbar, month-year picker, strip tanggal, label tanggal & ringkasan progress sesi
("x/y sesi"), list agenda per hari (icon tipe, judul, catatan, status: Belum/Belum pulang/Hadir/
Terlambat/Info, jam mulai-selesai), empty state, dialog popup info/error saat load gagal.

### Agenda Mingguan — Lapor Masuk/Pulang
Elemen: toolbar dinamis, card ringkasan (tipe, nama, waktu mulai), panel kebijakan absensi
(collapsible), label waktu lapor, peta lokasi saat ini, info alamat & koordinat, tombol "Lapor
masuk"/"Lapor pulang", dialog sukses/gagal.

### Agenda Mingguan — Detail Agenda
Elemen: toolbar "Detail Agenda", tipe & status, nama & durasi jadwal, timeline check-in/check-out
(waktu & catatan), ID agenda, status footer, info "Via", catatan agenda, info gate masuk/pulang.

---

## Theory (Materi Siswa)

Lokasi: `pages/theory/**`.

### Theory — Daftar Mata Pelajaran
Elemen: toolbar, search bar mapel, swipe refresh, list card mapel (foto, nama, guru, tag), empty
state "Mata pelajaran tidak ditemukan".

### Theory — Daftar Materi per Mapel
Elemen: toolbar, swipe refresh, list card materi (foto, judul, tanggal, mapel, guru), empty state
"Belum terdapat materi".

### Theory — Detail Materi
Elemen: toolbar, deskripsi, card file materi (nama file, tombol Lihat/Download), preview link, card
pembahasan/explanation, dialog "Materi Tidak tersedia".

### Theory — Pembaca PDF
Elemen: toolbar, viewer PDF (page-by-page), progress loading, kontrol halaman + label nomor.

---

## Theory Teacher (Materi Guru)

Lokasi: `pages/theoryteacher/**`.

### TheoryTeacher — Shell Materi Guru
Elemen: toolbar, 2 tab ("Materi Saya", "Mata Pelajaran"), FAB "Tambah" (upload materi baru).

### TheoryTeacher — Daftar Mata Pelajaran / Materi Saya
Elemen (mapel): swipe refresh, list card mapel diampu. Elemen (materi saya): filter dropdown Kelas &
Mata Pelajaran, swipe refresh, list card materi (menu kebab), empty state, dialog pilihan Edit/Hapus,
dialog konfirmasi hapus, progress dialog.

### TheoryTeacher — Upload/Edit Materi
Elemen: toolbar, input judul (counter), input deskripsi (counter), picker mapel/jenjang/jurusan/
kelas, upload file PDF, input URL link, preview link, tombol "Posting materi".

---

## Homework (Tugas Siswa)

Lokasi: `pages/homework/**`.

### Homework — Shell Tugas Siswa
Elemen: toolbar, 3 tab (Belum Dikerjakan, Sudah Dikerjakan, Nilai), chip filter (bottom sheet).

### Homework — Belum Dikerjakan / Sudah Dikerjakan / Nilai
Elemen: swipe refresh, list card tugas (tag status, foto, judul, waktu, info mapel/guru, atau skor
untuk tab Nilai), empty state per tab.

### Homework — Filter Tugas (Bottom Sheet)
Elemen: toolbar, radio "Batas Waktu", checkbox "Mata Pelajaran", tombol "Terapkan".

### Homework — Detail & Kumpulkan Tugas
Elemen: toolbar, info batas waktu, card progress "Syarat pengumpulan" (checklist baca soal/upload
jawaban), deskripsi tugas, section Soal (badge WAJIB DIBACA, file materi, preview link), section
jawaban terkumpul, section "Kumpulkan jawaban" (upload multi-file, input link opsional), section
Pembahasan (setelah dinilai), sticky bottom bar + tombol "Kirim Tugas".

### Homework — Detail Nilai
Elemen: toolbar, foto & nama mapel, nama guru, skor, tanggal dinilai, card pembahasan (file +
Download, tombol "baca materi").

---

## Homework Teacher (Tugas Guru)

### HomeworkTeacher — Shell
Elemen: toolbar, 2 tab ("List Tugas", "Penilaian"), filter dropdown Kelas & Mata Pelajaran, FAB
"Buat Tugas".

### HomeworkTeacher — Daftar Tugas Dibuat / Penilaian
Elemen (list): swipe refresh, list card tugas (menu opsi, judul, batas waktu, mapel, waktu upload),
dialog Edit/Hapus + konfirmasi. Elemen (penilaian): list card per grup tugas (judul, waktu, ringkasan
status pengumpulan, tombol Lihat).

### HomeworkTeacher — Buat/Edit Tugas
Elemen: toolbar, picker Kelas, dropdown Hari Mata Pelajaran, picker Mata Pelajaran, date/time picker
batas waktu, input judul (counter), input deskripsi, checkbox upload soal PDF, input URL link,
checkbox wajib baca materi/wajib mengumpulkan, tombol "Posting Tugas".

### HomeworkTeacher — Tugas Terkumpul & Beri Nilai
Elemen (terkumpul): toolbar, filter Kelas, tombol expand info tugas, list card jawaban siswa (foto,
nama, tag status, skor, tombol "Lihat Jawaban"). Elemen (beri nilai, bottom sheet): foto & nama mapel,
input skor, tanggal dinilai, section jawaban terkumpul + preview link, tombol "simpan", progress
dialog.

---

## Asesmen (AKM)

Lokasi: `pages/akm/**` (26 file).

### Daftar Asesmen — Host & Jadwal
Elemen: toolbar judul dinamis ("Asesmen"/"Survey"/"Kelas Eligible"), tab "Jadwal"/"Nilai". Tab Jadwal:
strip tanggal (date-picker bulan-tahun), swipe refresh, list card asesmen (ikon mapel, nama, tombol
"Ikuti AKM"/"Ikuti Ujian"), empty state "Belum terdapat ujian".

### Detail Asesmen
Elemen: toolbar, ikon+nama+jenis, info tenggat waktu, notice "ujian memakai password" (kondisional),
list info peserta, list "Beban soal", progress unduh soal, tombol aksi dinamis ("Sinkronisasi Soal" →
"Mulai Asesmen"/"AKM"/"Try Out" → "Lihat Nilai").

### Dialog Password Ujian & Konfirmasi Mulai
Elemen (password): judul, kartu peringatan kunci password, input password, tombol "Lanjut"/"Batal",
pesan error kontekstual. Elemen (konfirmasi mulai): judul "Pemberitahuan!!!", ilustrasi, daftar
ketentuan (mode kunci layar, wajib online, sanksi keluar, larangan telepon), tombol "OK".

### Daftar Mata Pelajaran / Kumpulkan
Elemen: toolbar, badge timer penalti, list mapel expand/collapse berisi instruksi soal (status Belum/
Proses/Selesai, progres X/Y), tombol "Kumpulkan" (aktif jika semua terjawab), dialog konfirmasi keluar/
penalti, dialog "Ujian Sedang Berlangsung".

### Kerjakan Soal
Elemen: toolbar + menu "Selesai", label nomor soal (buka grid navigasi), tag status jawaban, kartu
"Baca petunjuk", swipe soal per kartu (pilihan ganda, esai, benar/salah, tabel pernyataan, menjodohkan,
pasangan gambar), media soal (audio/video/gambar + "Aktifkan Data" bila offline), tombol "Berikutnya",
badge timer penalti "terdeteksi membuka aplikasi lain".

### Dialog Pilih Nomor Soal / Instruksi / Mode Terkunci
Elemen: grid nomor soal berwarna status; dialog instruksi (HTML); dialog "Mode Terkunci Terlepas" +
tombol "Aktifkan"; dialog "Penalti" dengan timer mundur.

### Nilai/Riwayat & Detail Nilai
Elemen (riwayat): swipe refresh, list card nilai (status upload, tombol "Upload Ulang" + cooldown),
empty state. Elemen (detail): toolbar, ring skor total, list skor per subtes, tombol "Cek Passing
Grade" (tryout), list info peserta & beban soal, progress unduh pembahasan, tombol aksi dinamis.

### Pembahasan Soal
Elemen: toolbar, list horizontal nomor instruksi/mapel, teks instruksi, list card pembahasan (gambar
soal, jawaban siswa, indikator benar/salah, jawaban kunci, teks pembahasan, viewer gambar full-screen).

---

## Try Out

Lokasi: `pages/tryout/**`.

### Try Out — Host, Jadwal, Nilai
Elemen: tab "Jadwal"/"Nilai" (host `TryOutPage`); Jadwal: swipe refresh, list card try out (badge
jenis, nama mapel, tombol "Ikuti Try Out") — tap membuka Detail Asesmen bertipe TRYOUT (share komponen
dengan AKM); Nilai: swipe refresh, list card nilai, tombol lihat skor & pembahasan.

### Cek Passing Grade
Elemen: toolbar, field "Pilih Universitas" (bottom sheet pencarian), radio jurusan Saintek/Soshum,
tombol "Cek Nilai", teks hasil kalkulasi, dialog hasil (sukses/gagal + ilustrasi).

### Pilih Universitas (Bottom Sheet)
Elemen: toolbar tutup, input pencarian (debounce), list nama universitas (infinite scroll), loading.

---

## Ujian

Lokasi: `pages/ujian/**` — folder terpisah dari akm, dipakai untuk auto-submit/notifikasi.

### Ujian — Host & Daftar
Elemen: tab "Ikuti"/"Nilai"; daftar: swipe refresh, list card ujian per mapel + tombol "Ikuti".

### Mulai Ujian
Elemen: toolbar "Mulai Ujian", progres unduh soal, field password (setelah unduh sukses), tombol
"Download Soal" → "Mulai Ujian", list "Petunjuk Mengerjakan Ujian".

### Kerjakan Ujian
Elemen: toolbar (judul mapel + subtitle), timer countdown, tombol "Selesai", label nomor soal (grid),
tag status jawaban, swipe soal (pilihan ganda teks/gambar, atau esai), tombol Sebelumnya/Selanjutnya,
dialog konfirmasi "Akhiri Ujian", dialog sistem "Ujian berakhir", auto-submit via alarm/service saat
app ditinggalkan.

### Nilai & Detail Ujian
Elemen (nilai): swipe refresh, list card skor/status, tombol "Kirim Ulang" untuk jawaban gagal
terkirim. Elemen (detail): toolbar, ring skor total (atau "Menunggu dinilai"), list info, tombol
"Lihat Jawaban" (setelah waktu berakhir), tombol "Laporkan Kesalahan" + dialog konfirmasi.

---

## Klaspay

Lokasi: `pages/klaspay/**` (29 file) — dompet digital sekolah.

### Aktivasi Wallet (3 langkah)
Elemen (password): label "Masukkan Password Akun", input password (toggle show/hide), button
"Konfirmasi". Elemen (buat PIN): icon shield, judul "Buat 6 Digit PIN Keamanan", keypad PIN custom 6
digit (dipakai 2x: input awal & konfirmasi ulang). Elemen (sukses): icon sukses, judul "Aktivasi
Wallet Berhasil!", button "Top Up Sekarang"/"Nanti Saja".

### Top Up — Pilih Metode & Input Nominal
Elemen (metode): saldo wallet, list metode (VA per bank, "Toppers"/transfer antar wallet, scan QR
toppers), dialog S&K Toppers. Elemen (nominal): input manual (min Rp 20.000), 4 tombol nominal cepat,
button "Konfirmasi", dialog konfirmasi (rincian nama/level/NISN/sekolah/nominal + warning akun tamu).

### Checkout Transfer (Toppers/QR)
Elemen: saldo wallet, icon & nama produk, rincian item, rincian biaya, total bayar, button "Bayar
Sekarang".

### Riwayat Pembayaran — List & Detail
Elemen (list): filter tab, ringkasan saldo masuk/keluar, filter rentang tanggal, list transaksi
(icon, ID, status, tanggal, nominal), ilustrasi kosong. Elemen (detail): jenis, ID transaksi,
keperluan, tanggal, status badge, harga, diskon, total.

### Tagihanku — List & Detail
Elemen (list): tab Menunggu/Sudah Dibayar, list tagihan, ilustrasi kosong. Elemen (detail topup): jenis
pembayaran, ID transaksi, tanggal, status, ID Wallet, jumlah, biaya admin/layanan, total.

### Selesaikan Pembayaran — Petunjuk & Rincian
Elemen: countdown batas waktu, nama bank/channel + logo, nomor VA + "Salin", total + "Salin",
accordion "Petunjuk Pembayaran" per channel.

### Dialog — Wajib Aktivasi / Info ID Wallet / Cashback
Elemen (wajib aktivasi): ilustrasi warning, judul, button "Aktivasi Sekarang"/"Nanti Saja". Elemen
(info ID wallet, top-sheet): nama pemilik, ID Wallet + "Salin". Elemen (cashback/promo): banner gambar
promo klik-untuk-detail.

### Promo — List & Detail
Elemen: banner promo, judul, tanggal, ringkasan; detail: konten HTML S&K promo.

---

## Pembayaran

Lokasi: `pages/pembayaran/**` (30 file, termasuk subfolder `spp/`, `qrscan/`).

### Beranda Pembayaran (Wallet Dashboard)
Elemen: header saldo wallet + button "Aktivasi Wallet" (jika belum aktif), shortcut icon Top Up/
Transfer/QR Pay/Riwayat, banner "Tagihanku", grid menu pembayaran (SPP, PPOB, dst), section
"Pembelian" (grid kedua), dialog info wallet.

### Pilih Metode / Checkout / Konfirmasi PIN (generik)
Elemen (pilih metode): list metode pembayaran. Elemen (checkout): metode terpilih, list rincian item,
button "Bayar Sekarang". Elemen (PIN): pesan "Masukkan pin", keypad PIN custom, link "Lupa pin?",
button "Konfirmasi"/"Bayar Sekarang".

### Atur PIN Baru
Elemen: banner info keamanan, input PIN baru + ketik ulang, button "Simpan Perubahan", dialog sukses.

### Detail Pembayaran (Status) & Redirect Web
Elemen (status): badge status, total bayar, card rincian, button "Cetak Struk"/"Batalkan"; varian
guide: countdown expired, VA/kode bayar + salin, accordion petunjuk. Elemen (redirect): WebView
gateway + progress loading.

### Pembayaran Berhasil
Elemen: judul "Transaksi Berhasil", badge lunas/pending, ringkasan item, info saldo terpotong
(kondisional), button "Detail Pembelian"/"OK, Kembali".

### QR Scan Pembayaran
Elemen: kamera scanner QR, card nama kantin/merchant, form keterangan & nominal, form biaya admin
(readonly) & total, form input PIN, button "Konfirmasi Pembayaran", link "Lupa PIN?", button "Scan
Ulang". Dialog kode QR milik siswa (untuk dipindai kasir). Halaman sukses QR: icon sukses, card detail
transaksi, button "OK, Kembali".

### SPP — Tagihan, Sudah Dibayar, Riwayat
Elemen (tagihan): tab Tagihan/Sudah Dibayar, card profil siswa, list tagihan per bulan (checkbox
multi-select, badge "Segera Lunasi"), ringkasan checkout bawah layar. Elemen (sudah dibayar): navigasi
tahun, list SPP lunas. Elemen (riwayat): list riwayat + detail rincian item & tombol "Bayar Kembali".

---

## PPOB

Lokasi: `pages/ppob/**` (44 file). **Pola umum semua produk:** form nomor pelanggan/HP → pilih produk/
nominal (grid kartu harga) → checkout (saldo wallet + rincian biaya + total) → konfirmasi PIN →
halaman sukses. Checkout & detail status memakai layout bersama antar produk.

### Pulsa & Paket Data
Elemen: tab Prabayar/Pascabayar, form nomor HP + auto-deteksi operator + pilih dari kontak; Prabayar:
grid produk pulsa & paket data; Pascabayar: form cek tagihan.

### Listrik
Elemen: tab Token/Tagihan, form nomor meter/ID pelanggan; Token: grid denominasi + total + "Selanjutnya";
Tagihan: info tenggat + verifikasi + total; Detail: ID pelanggan, nama, bulan/tahun, jumlah tagihan,
biaya admin/layanan, total, button "Bayar Tagihan".

### Air PDAM
Elemen: form pilih wilayah (dropdown ke halaman pencarian PDAM), form nomor meter/ID pelanggan,
button "Cek Tagihan".

### Internet & TV Kabel
Elemen: form pilih layanan (dropdown provider), form nomor pelanggan, button "Cek Tagihan".

### Game (Topup & Voucher)
Elemen: tab Topup/Voucher, grid pilihan game; Topup: form Email/ID Player + Server ID (khusus Mobile
Legends), list voucher/diamond; Voucher: list voucher tanpa form nomor.

### BPJS Kesehatan
Elemen: form nomor VA Keluarga, form pilih periode "Bayar Hingga" (opsional, maks 12 bulan), catatan
info, button "Cek Tagihan".

### Checkout & Detail Pembayaran PPOB
Elemen (checkout): saldo wallet, icon & nama produk, rincian produk & biaya, cashback (kondisional),
total, button "Bayar Sekarang". Elemen (detail status): badge "Menunggu Pembayaran", total, card
rincian, cashback, button "Cetak Struk".

---

## Partisipasi

Lokasi: `pages/partisipasi/**`.

### Dana Partisipasi — List
Elemen: tab "Partisipasi"/"Telah Berakhir", card profil siswa (opsional), saldo wallet, list kegiatan
(card progress), ilustrasi kosong, overlay loading.

### Dana Partisipasi — Detail Kegiatan
Elemen: nama kegiatan, biaya partisipasi (target), progress bar terkumpul, batas akhir, accordion
"Detail pembayaran" (riwayat), saldo wallet, button "Berpartisipasi" (via wallet), button "Pilih
Metode Pembayaran" (via VA/minimarket).

### Input Nominal, Checkout, Detail Status, Riwayat, Sukses
Elemen (nominal): input manual, error minimal, ringkasan kegiatan, button "Konfirmasi Pembayaran".
Elemen (checkout): saldo wallet, jenis & metode pembayaran, rincian harga, total, "Bayar Sekarang".
Elemen (detail status): total bayar, card rincian, "Cetak Struk". Elemen (riwayat): card profil, nama
kegiatan, total riwayat, list riwayat pembayaran. Elemen (sukses): judul "Pembayaran Berhasil", tipe +
tanggal, info saldo terpotong, button "Detail Pembayaran"/"OK, Kembali".

---

## Poin

Lokasi: `pages/poin/**` (14 file).

### Poin Sekolah (Guru/Staf) — Cari Siswa & Ringkasan
Elemen (cari): toolbar "Poin Siswa", field pencarian live-search (≥3 karakter), list hasil (avatar,
nama, kelas, NISN), empty state, dialog progres. Elemen (ringkasan): foto profil siswa, nama, badge
peran, kelas, NISN, ringkasan skor Pelanggaran (merah)/Prestasi (hijau), tombol "Pelanggaran",
"Prestasi", "Pemanggilan Siswa".

### Tambah Poin Pelanggaran / Prestasi / Pemanggilan Siswa
Elemen (pelanggaran/prestasi): field "Pilih Jenis" (bottom sheet pencarian), textarea "Keterangan",
lampiran foto opsional (Kamera/Galeri, preview, crop), button "Kirim", dialog sukses. Elemen
(pemanggilan): field "Pilih Jenis Pemanggilan", textarea keterangan, date & time picker jadwal, button
"Kirim", dialog sukses.

### Poin Saya (Siswa) — Home, Pelanggaran, Prestasi
Elemen (home): tab "Pelanggaran"/"Prestasi", dialog peringatan jam/zona waktu perangkat. Elemen (list
pelanggaran/prestasi): pull-to-refresh, kartu ringkasan bulan berjalan, tombol "Detail (semester)" →
dialog rekap per jenis (jumlah kejadian, total poin, skor ring), banner "Pemanggilan Siswa" → dialog
riwayat, list poin (nama, tanggal/waktu, tombol lihat dokumen/bukti → dialog detail dengan foto), empty
state.

---

## Magang

Lokasi: `pages/magang/**` (15 file).

### Magang — Shell (Jadwal & Laporan)
Elemen: toolbar, tab "Jadwal"/"Laporan", filter tanggal horizontal + label bulan (khusus tab Jadwal),
menu ikon "Tambah Izin" & panduan onboarding.

### Magang — Daftar Jadwal
Elemen: pull-to-refresh, progress bar, empty state, list jadwal (card: nama perusahaan, tanggal, jam
masuk-selesai, status kehadiran, tombol "Masuk"/absen → dialog peta lokasi, tombol laporan harian).

### Magang — Absen Lokasi (dialog fullscreen)
Elemen: toolbar "Presensi Magang", info jadwal, jam berjalan, peta Google Maps + marker + lingkaran
radius geofence + marker lokasi pengguna, tombol "lokasi saya", status teks di dalam/luar area, tombol
"Konfirmasi" (aktif hanya di dalam radius), dialog sukses.

### Magang — Tulis/Lihat Laporan Harian
Elemen: toolbar "Laporan Magang", area lampiran foto (Add Image, format max 5MB, dialog Kamera/
Galeri), preview gambar (mode lihat), textarea catatan (counter min 100 karakter), tombol "Submit
Laporan", dialog konfirmasi "Presensi Keluar".

### Magang — Daftar Laporan
Elemen: pull-to-refresh, chip filter Semua/Pilih Tanggal, list laporan (card: nama perusahaan, jam
masuk-keluar, thumbnail lampiran, cuplikan isi, tombol "Lihat Selengkapnya").

### Magang — Pengajuan Izin (Daftar, Ajukan, Detail)
Elemen (daftar): navigasi bulan, filter chip Semua/Sakit/Izin, list pengajuan (card: ikon jenis,
judul, keterangan, perusahaan), FAB "Ajukan Izin". Elemen (ajukan): banner status, dropdown pilih
jadwal (jika >1), radio jenis Sakit/Izin, upload surat, textarea keterangan, button "Kirim". Elemen
(detail): ikon jenis, label jenis & tanggal, baris perusahaan/keterangan/file bukti (kondisional).

---

## Notifikasi

Lokasi: `pages/notification/**`.

### Daftar Notifikasi
Elemen: toolbar "Notifikasi", filter "Semua"/"Belum Dibaca", pull-to-refresh, infinite scroll, list
notifikasi (card: ikon per tipe, judul, deskripsi, status baca/belum, tombol aksi kondisional "Lihat
Detail"/"Coba Sekarang"), empty state.

### Detail Notifikasi
Elemen: toolbar, ikon besar sesuai tipe, judul, isi pesan, image slider banner (jika ada lampiran),
tombol aksi kondisional yang mengarah ke berbagai modul (materi, tugas, ujian, presensi, poin, jurnal,
riwayat pembayaran/SPP/tagihan, topup, PPOB, profil, ubah password, kartu pelajar, kebijakan).

---

## Chat

Lokasi: `pages/chat/**`.

### Daftar Percakapan
Elemen: toolbar back, list percakapan (foto, nama, pesan terakhir, waktu, status terkirim/dibaca,
badge unread "99+"), empty state, FAB "percakapan baru" (disembunyikan untuk tipe "kwu").

### Pilih Kontak Chat Baru
Elemen: toolbar back, field pencarian kontak (debounce), list kontak (nama, kelas/jurusan).

### Halaman Obrolan (ChatPage)
> **Status: tidak aktif.** Seluruh isi `onCreate` di `ChatPage.kt` dikomentari — dibuka dari daftar
> percakapan tapi tidak menampilkan UI obrolan apa pun saat ini. Elemen yang dirancang di layout (tidak
> berfungsi): app bar, bubble chat masuk/keluar, bubble kartu produk, pemisah tanggal, info sistem,
> input teks, tombol emoji, tombol kirim.

---

## Komentar & Like

Lokasi: `pages/comment/**`, `pages/listlike/**` — dijangkau lewat deep link notifikasi dari sisa modul
feed sosial lama.

### Komentar Postingan
Elemen: toolbar back, preview post (gambar lampiran → ImageViewPage, caption dengan mention/hashtag
aktif, waktu, ringkasan "X Suka - Y Komentar"), list komentar (foto, nama, isi, waktu, menu opsi "..."),
kolom balasan bawah, dialog "Post Tidak tersedia" (jika feed_id tidak valid).

### Lihat Gambar (ImageViewPage)
> **Status: tidak aktif.** Seluruh isi `onCreate` dikomentari — dibuka dari CommentPage tapi tidak
> menampilkan apa pun. Elemen yang dirancang (tidak berfungsi): app bar judul dinamis, gambar
> full-screen, menu unduh (kondisional).

### Daftar yang Menyukai
Elemen: toolbar back, list pengguna yang menyukai (foto, nama → buka profil), dialog "Post Tidak
tersedia".

---

## Perpustakaan (belum aktif di produksi)

Lokasi: `pages/perpus/**`. Seluruh modul ini sudah lengkap secara kode (UI, ViewModel, DAO) tapi
**tidak aktif** — kedua entry point (menu "Pinjam Buku" di Pembayaran, dan referensi di Pembelajaran)
sudah di-comment dan diganti dialog "Fitur belum tersedia untuk sekolah ini".

### KlasPustaka — Beranda
Elemen: toolbar "KlasPustaka" back, search bar, carousel banner + dots, RecyclerView section buku
bertingkat, menu opsi ke riwayat peminjaman.

### KlasPustaka — Detail Buku, Deskripsi, Info
Elemen (detail): toolbar "INFORMASI BUKU", cover, judul, penulis, label stok, genre, 2 tab Deskripsi/
Detail Informasi, dialog "Buku tidak ditemukan". Elemen (deskripsi): teks deskripsi. Elemen (info):
list baris (tanggal terbit, jumlah halaman, ISBN, bahasa, penerbit).

### KlasPustaka — Pencarian, Riwayat Peminjaman
Elemen (pencarian): toolbar close + search bar, swipe refresh, list hasil. Elemen (riwayat): toolbar
"RIWAYAT PEMINJAMAN", 2 tab Berlangsung/Berakhir, 2 kartu ringkasan (total pinjaman, terlambat), list
peminjaman per tab.

---

## Komponen UI Bersama (errorui)

Lokasi: `pages/errorui/**` — bukan halaman, tapi UI-kit kecil reusable untuk error/empty state, dipicu
lewat `BasePage.showError(model, ...)` yang merutekan sesuai tipe tampilan.

- **BannerErrorView** — banner merah di atas konten, bisa ditutup. Elemen: icon warning, teks pesan,
  tombol close.
- **EmptyStateView** — state kosong/gagal-fetch untuk halaman list. Elemen: ilustrasi/icon, judul,
  deskripsi, tombol primary opsional ("Coba Lagi").
- **ErrorBottomSheet** — bottom sheet modal (langsung expanded). Elemen: ilustrasi, judul, deskripsi,
  tombol primary + secondary opsional.
- **SnackbarError** — snackbar ringan non-blocking. Elemen: teks pesan, action button opsional.
- **DialogErrorMapping / ToastErrorMapping** — memetakan error ke dialog `prettyAlert` standar atau ke
  Toast singkat (bukan komponen visual baru).
- **InlineErrorExtensions** — error inline pada form: `TextInputLayout`/`EditText` dengan border merah
  + **InlineErrorView** (icon warning kecil + teks di bawah field).

---

## Dialog Standar (BasePage / BaseFragment)

Kumpulan dialog umum tersedia dari base class, dipakai di ratusan halaman lain — referensi wajib untuk
padanan versi Compose nanti.

- **loading() / updateLoading() / dismissloading()** — `ProgressDialog` standar state memproses
  (default: "Mohon tunggu" / "Sedang memproses ...").
- **toast(msg)** — Toast singkat.
- **alert(...)** — `MaterialAlertDialogBuilder` standar (title, message, positive + neutral button).
- **prettyAlert(...)** — dialog kustom paling sering dipakai di seluruh app untuk feedback sukses/
  gagal. Elemen: ilustrasi opsional (sukses/gagal/custom), judul, konten, tombol OK, tombol Abort
  opsional.
- **prettyAlertt** — varian sama, layout khusus konteks asesmen.
- **alertSelect(...) / alertSelectNew(...)** — dialog pilihan single-select / dialog custom dengan
  judul+konten+tombol OK/Abort.
- Khusus `BaseFragment`:
  - **unsubscribeAlert(...)** — dialog unsubscribe dengan spinner alasan, input teks tambahan, tombol
    Kirim (enable setelah teks diisi) & Batal.
  - **confirmationAlert(...)** — dialog konfirmasi dengan ilustrasi, judul, pesan (mendukung link
    clickable ke WhatsApp), checkbox persetujuan (tombol Confirm aktif hanya jika dicentang).

---

## Catatan Dead Code / Legacy (ditemukan saat riset)

- `PairingPage` (`pages/akun/PairingPage.kt`) dan `SetUsernameDialog` (`pages/home/dialogs/username/`)
  — tidak ada trigger aktif di kode saat ini.
- `ChatPage.kt` dan `ImageViewPage.kt` — isi `onCreate` seluruhnya dikomentari, halaman terbuka tapi
  kosong/tidak berfungsi meski masih dipanggil dari halaman lain.
- `StudentCardPage` — hanya menampilkan data pribadi read-only; model tema kartu visual sudah ada di
  kode tapi tidak dipakai.
- `pages/onboard/` (legacy XML, `OnBoardPage.kt`) — **masih dirujuk aktif** dari `BasePage.goNext2()`,
  `DeepLinkPage.kt`, `login/Loginpage.kt`, berdampingan dengan `pages/auth/onboard/OnboardScreen.kt`
  (Compose baru). Migrasi tampaknya belum 100% memutus jalur lama ini — perlu ditelusuri lebih lanjut
  jalur mana yang benar-benar aktif dieksekusi sebelum menghapus salah satunya.
- `SekolahPage`, `jelajah`, `createpost`, `entrepreneurs`, `prokes`, `announcement` — sudah dikonfirmasi
  mati total (lihat riset fitur aktif sebelumnya), sengaja tidak dimasukkan dokumen ini.
