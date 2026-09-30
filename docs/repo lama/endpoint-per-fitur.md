# Daftar Endpoint API per Fitur

Referensi endpoint dari `app/src/main/java/id/diskola/app/api/ApiService.kt`.  
Path relatif terhadap base URL API. Endpoint mock yang dikomentari **tidak** disertakan.

**Legenda**

| Kolom | Arti |
| --- | --- |
| Method | HTTP method |
| Path | Path Retrofit |
| Multipart | `✓` jika body multipart |

---

## Daftar isi

1. [Umum / utilitas](#1-umum--utilitas)
2. [Auth & sesi](#2-auth--sesi)
3. [Akun & profil](#3-akun--profil)
4. [Notifikasi](#4-notifikasi)
5. [Sosmed / feed sekolah](#5-sosmed--feed-sekolah)
6. [Pengumuman](#6-pengumuman)
7. [Materi (Theory)](#7-materi-theory)
8. [Tugas (Assignment)](#8-tugas-assignment)
9. [Presensi & jurnal](#9-presensi--jurnal)
10. [Agenda mingguan (staff)](#10-agenda-mingguan-staff)
11. [Izin / leave request](#11-izin--leave-request)
12. [Asesmen / AKM](#12-asesmen--akm)
13. [Try Out / Quiz](#13-try-out--quiz)
14. [Ujian legacy (Examinations)](#14-ujian-legacy-examinations)
15. [Poin / konseling](#15-poin--konseling)
16. [Magang](#16-magang)
17. [Perpustakaan](#17-perpustakaan)
18. [Prokes](#18-prokes)
19. [Klaspay / pembayaran / SPP](#19-klaspay--pembayaran--spp)
20. [PPOB](#20-ppob)
21. [Dana partisipasi](#21-dana-partisipasi)
22. [Entrepreneur / toko](#22-entrepreneur--toko)
23. [Kartu pelajar & pairing](#23-kartu-pelajar--pairing)
24. [Policy & about](#24-policy--about)

---

## 1. Umum / utilitas

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `{url}` (dynamic `@Url`) | Download file arbitrary (`download`) |
| GET | `{url}` (dynamic `@Url`) | Download sebagai string/any |
| GET | `mobile/app/config/check-android-version` | Cek versi app |

---

## 2. Auth & sesi

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/app/authentication/schools` | Daftar sekolah |
| POST | `mobile/app/authentication/check-account` | Cek akun |
| POST | `mobile/app/authentication/login-sso` | Login SSO (email) |
| GET | `mobile/app/authentication/login-sso/classes` | Kelas SSO |
| POST | `mobile/app/authentication/login-sso/school` | Login SSO sekolah |
| POST | `mobile/app/authentication/login-sso/check-nisn` | Cek NISN |
| POST | `mobile/app/authentication/login-sso/verification` | Verifikasi NISN |
| POST | `mobile/app/authentication/login-account` | Login akun |
| POST | `mobile/app/authentication/requesting-student` | Ajuan siswa |
| POST | `mobile/app/authentication/requesting-teacher` | Ajuan guru |
| POST | `mobile/app/authentication/reset-password` | Reset password |
| GET | `mobile/app/roles` | Daftar role |
| GET | `mobile/gmail/verify/{google_token}` | Verifikasi Google |
| GET | `mobile/email/verify` | Verifikasi email |
| DELETE | `logout` | Logout |
| GET | `sessions` | Daftar sesi device |
| DELETE | `sessions/others` | Logout device lain |
| DELETE | `sessions/{id}` | Logout device tertentu |
| POST | `sosmed/setting/setup-user-fcm` | Update FCM token |

---

## 3. Akun & profil

| Method | Path | Multipart | Keterangan |
| --- | --- | --- | --- |
| GET | `mobile/app/accounts/user/{userId}` | | Profil user |
| GET | `mobile/app/accounts/get-username/{username}` | | Cek username |
| GET | `mobile/app/accounts/search-username` | | Cari username |
| GET | `mobile/app/accounts/user/{userId}/feed-post` | | Post user |
| GET | `mobile/app/accounts/user/{userId}/feed-image` | | Feed image user |
| GET | `mobile/app/accounts/user/{userId}/feed-ebook` | | Feed ebook user |
| POST | `mobile/app/accounts/user/{userUuid}/change-avatar` | ✓ | Ganti avatar |
| PUT | `mobile/app/accounts/user/{userUuid}/change-profile` | | Update profil |
| PUT | `mobile/app/accounts/user/{userUuid}/change-password` | | Ganti password |
| GET | `mobile/app/accounts/user/{userId}/get-layout-about` | | Layout about |
| GET | `mobile/app/accounts/user/shipping-address` | | Alamat kirim |
| POST | `mobile/app/accounts/user/shipping-address` | | Set alamat kirim |

---

## 4. Notifikasi

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/notification` | List notifikasi |
| GET | `mobile/notification/{id}` | Detail |
| POST | `mobile/notification/{id}` | Tandai dibaca |
| GET | `mobile/notification/summary` | Ringkasan unread |

---

## 5. Sosmed / feed sekolah

| Method | Path | Multipart | Keterangan |
| --- | --- | --- | --- |
| POST | `mobile/app/schools/feed-posts` | ✓ | Buat post |
| POST | `mobile/app/schools/feed-ebooks` | ✓ | Buat ebook |
| GET | `mobile/app/schools/feed-posts` | | List post |
| GET | `mobile/app/schools/feed-ebooks` | | List ebook |
| GET | `mobile/app/schools/feeds/{feedId}` | | Detail feed |
| GET | `mobile/app/schools/feeds/{feedId}/comment` | | Komentar |
| GET | `mobile/app/schools/feeds/{feedId}/like` | | Like |
| POST | `mobile/app/schools/feed-posts/send-like` | | Like post |
| DELETE | `mobile/app/schools/feed-posts/{feedId}/unlike` | | Unlike |
| POST | `mobile/app/schools/feed-posts/{feedId}/comment` | | Kirim komentar |
| DELETE | `mobile/app/schools/feeds/{feedId}` | | Hapus feed |
| GET | `mobile/app/schools/explore/feed` | | Explore feed |
| GET | `mobile/app/schools/explore/user` | | Explore user |
| GET | `mobile/app/schools/explore/hastag` | | Explore hashtag |

---

## 6. Pengumuman

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/app/learning/announcements` | List pengumuman |

---

## 7. Materi (Theory)

### Siswa

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/app/learning/theories/students/subjects` | Daftar mapel |
| GET | `mobile/app/learning/theories/students/subjects/{subjectId}/theories` | Daftar materi |
| GET | `mobile/app/learning/theories/students/subjects/{subjectId}/theories/{theoryId}` | Detail materi |

### Guru

| Method | Path | Multipart | Keterangan |
| --- | --- | --- | --- |
| GET | `mobile/app/learning/theories/teachers/subjects` | | Mapel diampu |
| GET | `mobile/teacher/school-subject` | | Mapel (alternatif) |
| GET | `mobile/app/learning/theories/teachers/theories` | | Materi saya (filter subject/class) |
| GET | `mobile/app/learning/theories/teachers/subjects/{subjectId}/theories` | | Materi per mapel |
| GET | `mobile/app/learning/theories/teachers/subjects/{subjectId}/theories/{theoryId}` | | Detail |
| POST | `mobile/app/learning/theories/teachers/theories` | ✓ | Create |
| POST | `mobile/app/learning/theories/teachers/theories/update/{id}` | ✓ | Update |
| DELETE | `mobile/app/learning/theories/teachers/theories/{id}` | | Hapus |
| GET | `mobile/app/learning/theories/teachers/school-majors` | | Opsi jurusan |
| GET | `mobile/teacher/school-grade` | | Opsi jenjang |
| GET | `mobile/teacher/school-class-room` | | Opsi kelas |

---

## 8. Tugas (Assignment)

### Siswa

| Method | Path | Multipart | Keterangan |
| --- | --- | --- | --- |
| GET | `mobile/app/learning/assignment/students/backlog` | | Belum dikerjakan |
| GET | `mobile/app/learning/assignment/students/done` | | Sudah dikerjakan |
| GET | `mobile/app/learning/assignment/students/scored` | | Sudah dinilai |
| POST | `mobile/app/learning/assignment/students/{id}/collect` | ✓ | Kumpulkan jawaban |
| GET | `mobile/app/learning/assignment/students/{subjectAssignmentId}/collect/{studentAssignmentId}` | | Detail jawaban |

### Guru

| Method | Path | Multipart | Keterangan |
| --- | --- | --- | --- |
| GET | `mobile/app/learning/assignment/teachers/backlog` | | List / backlog (juga via query map filter) |
| GET | `mobile/app/learning/assignment/teachers/class` | | Daftar kelas |
| GET | `mobile/app/learning/assignment/teachers/schedule-day` | | Hari jadwal |
| GET | `mobile/app/learning/assignment/teachers/schedule` | | Jadwal mapel (class + day) |
| POST | `mobile/app/learning/assignment/teachers/create` | ✓ | Buat tugas |
| POST | `mobile/app/learning/assignment/teachers/update/{id}` | ✓ | Update tugas |
| DELETE | `mobile/app/learning/assignment/teachers/delete/{id}` | | Hapus tugas |
| GET | `mobile/app/learning/assignment/teachers/scored` | | Grup penilaian |
| GET | `mobile/app/learning/assignment/teachers/scored/{id}` | | Detail jawaban siswa |
| POST | `mobile/app/learning/assignment/teachers/scored/{colledtedId}/student-assignment/{assignmentId}` | | Simpan nilai |

---

## 9. Presensi & jurnal

### Jadwal & absensi kelas

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/attendance/schedule` | Jadwal hari |
| GET | `mobile/attendance/schedule/{id}` | Detail jadwal |
| GET | `mobile/attendance/schedule/{id}/list-student` | List siswa di kelas |
| GET | `mobile/attendance/data/{month}/{year}` | Data absensi bulan |
| GET | `mobile/attendance/summary/{year}` | Ringkasan tahun |
| POST | `mobile/attendance/start` | Mulai kelas |
| POST | `mobile/attendance/attend` | Hadir |
| POST | `mobile/attendance/end` | Akhiri kelas |
| POST | `mobile/attendance/leave` | Keluar/izin konteks kelas |
| POST | `mobile/attendance/check-in/student` | Guru tandai hadir siswa |
| POST | `mobile/attendance/student/learning/qr` | Presensi QR siswa |

### Presensi siswa

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/attendance/student/by-month` | Rekap bulanan |
| GET | `mobile/attendance/student/by-year` | Rekap tahunan |
| GET | `mobile/attendance/student/check` | Status check hari ini |
| POST | `mobile/attendance/student/check-in` | Check-in |
| POST | `mobile/attendance/student/check-out` | Check-out |
| PUT | `mobile/attendance/student/journal-update` | Update jurnal siswa |
| POST | `mobile/attendance/student/journal-student` | Post jurnal siswa |

### Presensi staff / guru

| Method | Path | Multipart | Keterangan |
| --- | --- | --- | --- |
| GET | `mobile/attendance/staff/by-month` | | Rekap bulanan |
| GET | `mobile/attendance/staff/by-year` | | Rekap tahunan |
| GET | `mobile/attendance/staff/check` | | Status check |
| GET | `mobile/attendance/setting/me/today` | | Setting hari ini |
| POST | `mobile/attendance/check-in` | | Check-in generik |
| POST | `mobile/attendance/staff/check-in` | | Check-in staff |
| POST | `mobile/attendance/staff/check-out` | | Check-out staff |
| GET | `mobile/attendance/staff/offsite/check` | | Status dinas luar |
| POST | `mobile/attendance/staff/offsite/check-in` | ✓ | Check-in offsite |
| POST | `mobile/attendance/staff/offsite/check-out` | ✓ | Check-out offsite |

### Jurnal pembelajaran

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/attendance/list-class` | List kelas |
| GET | `mobile/attendance/list-subject` | List mapel |
| GET | `mobile/attendance/list-teacher` | List guru |
| GET | `mobile/attendance/list-plot` | List plot waktu |
| POST | `mobile/attendance/journal` | Post jurnal guru |
| GET | `mobile/attendance/journal/{scheduleId}` | Preview jurnal |
| POST | `mobile/attendance/journal/save` | Simpan jurnal |

---

## 10. Agenda mingguan (staff)

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/attendance/staff/agendas` | Daftar agenda |
| GET | `mobile/attendance/staff/agendas/today` | Agenda hari ini |
| POST | `mobile/attendance/staff/agendas/check` | Check-in agenda |
| POST | `mobile/attendance/staff/agendas/check-out` | Check-out agenda |

---

## 11. Izin / leave request

| Method | Path | Multipart | Role |
| --- | --- | --- | --- |
| GET | `mobile/attendance/staff/leave-request/today` | | Staff hari ini |
| GET | `mobile/attendance/student/leave-request/today` | | Siswa hari ini |
| GET | `mobile/attendance/staff/leave-request` | | List staff |
| GET | `mobile/attendance/student/leave-request` | | List siswa |
| POST | `mobile/attendance/staff/leave-request` | ✓ | Ajukan (staff) |
| POST | `mobile/attendance/student/leave-request` | ✓ | Ajukan (siswa) |

---

## 12. Asesmen / AKM

### Ujian sekolah (Asesmen)

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/app/learning/akm/exam-schedules` | List jadwal |
| GET | `mobile/app/learning/akm/exam-schedules-scored` | List nilai |
| GET | `mobile/app/learning/akm/exam-schedules/{id}` | Detail (`gov_schedule` query) |
| GET | `mobile/app/learning/akm/student/exam-school/{id}/download` | Unduh soal |
| POST | `mobile/app/learning/akm/student/exam-school/{id}/check-password` | Cek password |
| POST | `mobile/app/learning/akm/student/exam-school/{akm_id}/answer/{student_id}` | Upload jawaban |
| GET | `mobile/app/learning/akm/exam-schedules-scored/{id}/explains` | Pembahasan |

### AKM pemerintah

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/app/learning/akm/schedules` | List jadwal |
| GET | `mobile/app/learning/akm/scored` | List nilai |
| GET | `mobile/app/learning/akm/schedules/{id}` | Detail |
| GET | `mobile/app/learning/akm/student/exam/{id}/download` | Unduh soal |
| POST | `mobile/app/learning/akm/student/exam/{akm_id}/answer/{student_id}` | Upload jawaban |
| GET | `mobile/app/learning/akm/student/exam/{id}/review` | Review |

### Setelan

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/setting-akm` | Strict mode, penalti, dll. |

> Tidak ada endpoint guru untuk create/score asesmen di `ApiService`.

---

## 13. Try Out / Quiz

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/app/learning/try-out/schedules` | List jadwal |
| GET | `mobile/app/learning/try-out/schedules/{id}` | Detail |
| GET | `mobile/app/learning/try-out/scored` | List nilai |
| GET | `mobile/app/learning/try-out/scored/{id}` | Review skor |
| GET | `mobile/app/learning/try-out/student/training/{id}/download` | Unduh soal |
| POST | `mobile/app/learning/try-out/student/training/{akm_id}/answer/{student_id}` | Upload jawaban |
| GET | `mobile/app/learning/try-out/scored/{id}/explanation` | Pembahasan |
| GET | `mobile/app/learning/try-out/passing-grade` | List universitas / passing grade |

---

## 14. Ujian legacy (Examinations)

Modul `pages/ujian` — jalur lama, terpisah dari Asesmen AKM.

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/app/learning/examinations/students/exams-list` | List ujian |
| GET | `mobile/app/learning/examinations/students/exams-scored` | List nilai |
| PUT | `mobile/app/learning/examinations/students/exams/{id}/download` | Download soal |
| GET | `mobile/app/learning/examinations/students/exams/{id}/detail` | Detail |
| PUT | `mobile/app/learning/examinations/students/exams/{id}/start` | Mulai |
| POST | `mobile/app/learning/examinations/students/exams/{id}/answer` | Jawaban |
| PUT | `mobile/app/learning/examinations/students/exams/{id}/stop` | Selesai / stop |

---

## 15. Poin / konseling

| Method | Path | Multipart | Keterangan |
| --- | --- | --- | --- |
| GET | `mobile/konseling/list-violation` | | Master pelanggaran |
| GET | `mobile/konseling/list-achievement` | | Master prestasi |
| GET | `mobile/konseling/list-handling` | | Master penanganan |
| GET | `mobile/konseling/score-student` | | Poin siswa (diri sendiri) |
| GET | `mobile/konseling/score` | | Cari skor siswa (NISN/nama) |
| POST | `mobile/konseling/create/violation` | ✓ | Catat pelanggaran |
| POST | `mobile/konseling/create/achievement` | ✓ | Catat prestasi |
| POST | `mobile/konseling/create/calling-student` | | Pemanggilan / handling |

---

## 16. Magang

| Method | Path | Multipart | Keterangan |
| --- | --- | --- | --- |
| GET | `mobile/internship/schedule` | | Jadwal magang |
| POST | `mobile/internship/attend` | | Presensi magang |
| GET | `mobile/internship/attendance` | | Laporan kehadiran |
| POST | `mobile/internship/leave` | ✓ | Laporan harian |
| POST | `mobile/internship/leave-request` | ✓ | Ajuan izin |
| DELETE | `mobile/internship/leave-request/{id}` | | Hapus ajuan izin |

---

## 17. Perpustakaan

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/app/learning/pustaka/students/homepage-banner` | Banner |
| GET | `mobile/app/learning/pustaka/students/homepage-book-newest` | Buku terbaru |
| GET | `mobile/app/learning/pustaka/students/homepage-book-famous` | Buku populer |
| GET | `mobile/app/learning/pustaka/students/search-books` | Cari buku |
| GET | `mobile/app/learning/pustaka/students/books/{id}/available` | Stok |
| GET | `mobile/app/learning/pustaka/students/rent-archives` | Arsip pinjam |
| GET | `mobile/app/learning/pustaka/students/rents` | Pinjam aktif |

---

## 18. Prokes

### Siswa

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/app/learning/health-protocols/student-form-early-detection` | Form screening |
| POST | `mobile/app/learning/health-protocols/save-report` | Kirim laporan |
| GET | `mobile/app/learning/health-protocols/check-report` | Cek laporan |
| GET | `mobile/app/learning/health-protocols/check-vaccinated` | Cek vaksin |
| POST | `mobile/app/learning/health-protocols/save-vaccinated` | Simpan vaksin |

### Guru

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/app/learning/health-protocols/check-vaccinated-teacher` | Cek vaksin guru |
| POST | `mobile/app/learning/health-protocols/save-vaccinated-teacher` | Simpan vaksin guru |
| GET | `mobile/app/learning/health-protocols/list-school-class-teacher` | List kelas |
| GET | `mobile/app/learning/health-protocols/list-school-class-student/{idClass}` | List siswa |
| GET | `mobile/app/learning/health-protocols/teacher-form-early-detection` | Form guru |
| POST | `mobile/app/learning/health-protocols/save-history-report/{studentId}` | Screening siswa |
| GET | `mobile/app/learning/health-protocols/check-screening-student` | Status screening |
| GET | `mobile/app/learning/health-protocols/check-history-report/{studentId}` | Riwayat siswa |

---

## 19. Klaspay / pembayaran / SPP

### Aktivasi & wallet

| Method | Path | Keterangan |
| --- | --- | --- |
| POST | `mobile/app/payment/check` | Cek status pembayaran |
| POST | `mobile/app/payment/activate` | Aktivasi |
| GET | `payment/wallet` | Info wallet |
| POST | `payment/toppers/register` | Daftar toppers |
| POST | `payment/toppers/unregister` | Unregister toppers |
| POST | `payment/reset-pin` | Reset PIN |
| POST | `payment/reset-pin/setpin` | Set PIN baru |
| GET | `payment/user/campaign/point` | Poin kampanye |
| GET | `payment/merchant/id/{merchantId}` | Info merchant |

### Topup & transfer

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `payment/channel/topup` | Channel topup |
| POST | `payment/transaction/topup_inq` | Inquiry topup |
| POST | `payment/transaction/topup_trx` | Transaksi topup |
| GET | `payment/channel/guide/{channelMethodName}` | Panduan channel |
| POST | `payment/transaction/transfer_inq` | Inquiry transfer |
| POST | `payment/transaction/transfer_trx` | Transaksi transfer |

### Invoice sekolah / SPP (legacy + klaspay)

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `transaction/school-invoice/unpaid` | Belum bayar |
| GET | `transaction/school-invoice/paid` | Sudah bayar |
| GET | `transaction/school-invoice/process` | Proses |
| POST | `transaction/school-invoice/pay` | Bayar |
| GET | `transaction/payment-service` | Service pembayaran |
| GET | `payment/transaction/invoice` | Invoice |
| POST | `payment/transaction/spp_cancel_invoice` | Batal invoice SPP |
| POST | `payment/transaction/transaction_spp` | Transaksi SPP |
| GET | `payment/channel/spp` | Channel SPP |
| GET | `payment/transaction/history_school` | Riwayat sekolah |
| POST | `payment/transaction/spp_trx` | SPP trx |
| POST | `payment/transaction/bill_trx` | Bill trx |

### Riwayat

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `payment/transaction/history` | Riwayat transaksi |
| GET | `payment/transaction/history/id/{id}` | Detail riwayat |

---

## 20. PPOB

### Katalog produk

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `payment/product/list/pulsa/{phone}` | Pulsa prabayar |
| GET | `payment/product/list/pulsa_pasca/{phone}` | Pulsa pascabayar |
| GET | `payment/product/list/pln_prabayar` | PLN prabayar |
| GET | `payment/product/list/pln_pascabayar` | PLN pascabayar |
| GET | `payment/product/list/pdam` | PDAM |
| GET | `payment/product/list/internet` | Internet |
| GET | `payment/product/list/bpjs` | BPJS |
| GET | `payment/product/list/game` | Game |
| GET | `payment/product/list/game/voucher` | Voucher game |
| GET | `payment/product/list/streaming` | Streaming |

### Transaksi PPOB

| Method | Path | Keterangan |
| --- | --- | --- |
| POST | `payment/transaction/ppob/inq` | Inquiry |
| POST | `payment/transaction/ppob/inq_check` | Cek inquiry |
| POST | `payment/transaction/ppob/inq_pay` | Bayar inquiry |
| POST | `payment/transaction/ppob/trx` | Transaksi |

---

## 21. Dana partisipasi

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `payment/bill/list` | List tagihan |
| GET | `payment/bill/history/id/{id}` | Detail / history |
| GET | `dana-partisipasi/school/{schoolId}/student` | List siswa (kontak) |

---

## 22. Entrepreneur / toko

### Katalog & pencarian

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/enterpreneur/homepage` | Home |
| GET | `mobile/enterpreneur/category` | Kategori |
| GET | `mobile/enterpreneur/category/{categoryId}/detail` | Detail kategori |
| GET | `mobile/enterpreneur/goodies/categories/{categoryId}{categorySubId}` | Subkategori goods |
| GET | `mobile/enterpreneur/card` | Card |
| GET | `mobile/enterpreneur/goodies/{goodieId}/detail` | Detail produk |
| GET | `mobile/enterpreneur/goodies/{goodieId}/review` | Review produk |
| GET | `mobile/enterpreneur/goodies/{goodieId}/listreview` | List review |
| GET | `mobile/enterpreneur/filter/count` | Count filter |
| GET | `mobile/enterpreneur/filter/list` | List filter |
| GET | `mobile/enterpreneur/search-merchants` | Cari merchant |
| GET | `mobile/enterpreneur/search-goodies-suggestion` | Saran pencarian |
| GET | `mobile/enterpreneur/search` | Search |

### Merchant publik

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/enterpreneur/merchants/{sellerId}` | Profil merchant |
| GET | `mobile/enterpreneur/merchants/{sellerId}/goodies-all` | Semua produk |
| GET | `mobile/enterpreneur/merchants/{sellerId}/goodies-best-seller` | Best seller |
| GET | `mobile/enterpreneur/merchants/{sellerId}/summary` | Ringkasan |

### Akun merchant (seller)

| Method | Path | Multipart | Keterangan |
| --- | --- | --- | --- |
| GET | `mobile/enterpreneur/merchants/account/profile` | | Profil |
| POST | `mobile/enterpreneur/merchants` | | Buat merchant |
| PUT | `mobile/enterpreneur/merchants/account/profiles` | | Update profil |
| POST | `mobile/enterpreneur/merchants/account/profiles/image` | ✓ | Foto profil |
| GET | `mobile/enterpreneur/merchants/account/profile/goodies-all` | | Produk saya |
| GET | `mobile/enterpreneur/merchants/account/profile/goodies-best-seller` | | Best seller saya |
| GET | `mobile/enterpreneur/merchants/account/profile/summary` | | Summary |
| GET | `mobile/enterpreneur/merchants/account/profile/summary-purchase` | | Summary purchase |
| GET | `mobile/enterpreneur/merchants/account/goodies/{goodieId}` | | View produk |
| POST | `mobile/enterpreneur/merchants/account/goodies/create` | ✓ | Buat produk |
| POST | `mobile/enterpreneur/merchants/account/goodies/create/{goodieId}/image` | ✓ | Tambah gambar |
| POST | `mobile/enterpreneur/merchants/account/goodies/update/{goodieId}/image/{imageId}` | ✓ | Update gambar |
| DELETE | `mobile/enterpreneur/merchants/account/goodies/delete/{goodieId}/image/{imageId}` | | Hapus gambar |
| PUT | `mobile/enterpreneur/merchants/account/goodies/publish/{goodieId}` | | Publish |
| PUT | `mobile/enterpreneur/merchants/account/goodies/update/{goodieId}` | | Update produk |
| DELETE | `mobile/enterpreneur/merchants/account/goodies/delete/{goodieId}` | | Hapus produk |

### Transaksi seller & buyer

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/enterpreneur/merchants/account/transactions/incoming` | Incoming |
| GET | `mobile/enterpreneur/merchants/account/transactions/processed` | Processed |
| GET | `mobile/enterpreneur/merchants/account/transactions/completed` | Completed |
| GET | `mobile/enterpreneur/merchants/account/transactions/{transaksiId}` | Detail seller |
| POST | `mobile/enterpreneur/merchants/account/transactions/{TransaksiId}/accept` | Accept |
| POST | `mobile/enterpreneur/merchants/account/transactions/{TransaksiId}/reject` | Reject |
| GET | `mobile/enterpreneur/transactions/{TransaksiId}/cancel` | Cancel buyer |
| GET | `mobile/enterpreneur/transactions/purchases-done` | Pembelian selesai |
| GET | `mobile/enterpreneur/transactions/purchases-processed` | Pembelian proses |
| GET | `mobile/enterpreneur/transactions/purchases/{transaksiId}` | Detail pembelian |
| GET | `mobile/enterpreneur/merchants/account/reviews` | Review seller |
| GET | `mobile/enterpreneur/reviews` | Review buyer |
| GET | `mobile/enterpreneur/reviews/transactions/{transaksiId}` | Detail review buyer |
| GET | `mobile/enterpreneur/merchants/account/reviews/transactions/{transaksiId}` | Detail review seller |
| POST | `mobile/enterpreneur/reviews/{goodyReviewId}` | Post review buyer |
| POST | `mobile/enterpreneur/merchants/account/reviews/{goodyReviewId}` | Post review seller |
| GET | `mobile/enterpreneur/trackings/{transaksiId}` | Tracking |
| GET | `mobile/enterpreneur/transactions/awb/{transaksiId}` | Input resi |

> Ada satu method `acceptTransaksiBuyer` yang memakai **absolute URL** `https://dev.api.diskola.id/api/...` — beda dari path relatif lain.

### Cart & checkout

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/enterpreneur/carts` | Load cart |
| POST | `mobile/enterpreneur/carts` | Tambah cart |
| PUT | `mobile/enterpreneur/carts/goodies/{goods}` | Update cart |
| DELETE | `mobile/enterpreneur/carts/goodies/{goods}` | Hapus item |
| GET | `mobile/enterpreneur/checkouts/shipping-fee-list` | Ongkir |
| POST | `mobile/enterpreneur/checkouts/transaction-create` | Checkout |

### Lokasi

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/enterpreneur/location/province` | Provinsi |
| GET | `mobile/enterpreneur/location/city` | Kota |
| GET | `mobile/enterpreneur/location/district` | Kecamatan |

---

## 23. Kartu pelajar & pairing

| Method | Path | Multipart | Keterangan |
| --- | --- | --- | --- |
| GET | `mobile/app/accounts/student-card-template` | | Template kartu |
| POST | `mobile/app/accounts/user/student-card` | ✓ | Update kartu |
| GET | `mobile/app/accounts/parent/pair-lists` | | List pairing orang tua |
| POST | `mobile/app/accounts/parent/accept-pair/{id}` | | Terima pairing |

---

## 24. Policy & about

| Method | Path | Keterangan |
| --- | --- | --- |
| GET | `mobile/app/policy` | Kebijakan / terms |

---

## Catatan

1. Sumber tunggal: `ApiService.kt`. Beberapa fitur memakai endpoint yang sama lintas role (mis. `mobile/teacher/school-class-room` dipakai materi & tugas).
2. Paging umum sering memakai query `take` + `skip`.
3. Download konten file (materi/tugas/soal) sering lewat `download(@Url)` ke URL absolut dari response, bukan path khusus.
4. Endpoint guru untuk **Asesmen/AKM tidak ada** di client ini.
5. Duplikat deklarasi Retrofit (mis. `teachers/backlog`, `spp_trx`, `exams/{id}/stop`) tetap tercantum sekali per path unik.

---

*Dihasilkan dari kode project android-portal. Sinkronkan ulang jika `ApiService` berubah.*
