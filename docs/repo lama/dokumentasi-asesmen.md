# Dokumentasi Fitur Asesmen (AKM / Ujian Sekolah)

Dokumentasi ini merangkum **alur UI**, **rules bisnis/UX**, dan **struktur teknis** fitur Asesmen di aplikasi Diskola Android Portal. Sumber utama: `pages/akm/**`, entry `PembelajaranPage`, setelan `AkmSettings`, serta API `ApiService` (akm / exam-school / try-out).

> Modul terkait yang berbagi komponen: **Try Out / Quiz** (`pages/tryout/**`) dan **Ujian legacy** (`pages/ujian/**`). Fokus dokumen ini adalah jalur produk utama **Asesmen sekolah** lewat `AkmPage`.

---

## 1. Ringkasan peran

| Aspek | Siswa | Guru |
| --- | --- | --- |
| Menu Pembelajaran | **Asesmen** → `AkmPage` | Item Asesmen **dihapus** dari grid; diganti **Agenda Mingguan** |
| Badge "segera" | Tidak | Historis: `soon = !is_student` — tidak relevan karena item sudah di-remove |
| Kerjakan / kumpulkan / lihat nilai | Ya (client take-exam penuh) | **Tidak ada UI** di app ini |
| Buat soal / generate password / nilai di app | Tidak | Tidak (pesan UI: minta guru generate password — diasumsikan web/admin) |
| API | `.../akm/...`, `.../student/...`, `.../exam-school/...` | **Tidak ada** endpoint `.../teachers/...` untuk AKM/asesmen |

**Kesimpulan produk:** Asesmen di Android Portal adalah **fitur siswa**. Role guru tidak punya alur create / manage / score di app; yang ada hanya referensi teks ke aksi guru di luar app (mis. generate password).

---

## 2. Gate akses

### 2.1 Pref & menu

| Pref / kondisi | Efek |
| --- | --- |
| `is_having_class = false` | Menu locked + dialog locked. |
| `is_student = true` | Menu **Asesmen** tampil; tap (index 4) → `AkmPage`. |
| `is_student = false` | `removeAt(4)` menghapus Asesmen dari list menu. |
| `is_teacher = true` | Menyisipkan **Agenda Mingguan** di index 4 (bukan Asesmen). |

```
Pembelajaran menu index 4
  ├─ is_student     → AkmPage (Asesmen)
  ├─ is_teacher     → AgendaMingguanPage
  └─ lainnya        → PoinPage (fallback non-siswa non-guru)
```

### 2.2 Entry lain (siswa / device dengan data lokal)

| Entry | Target |
| --- | --- |
| `HomePage` / `AkunPage2` (blok unfinished exam) | `AkmPage` bila ada ujian lokal belum selesai |
| Try Out host | `TryOutPage` → detail lewat `AkmDetailPage` (`examType = TRYOUT`) |
| Notifikasi `COURSE` | Bisa ke modul `ujian` legacy (bukan jalur utama Asesmen) |

Extra penting saat membuka `AkmPage`:

- `isSchoolScope` (default **true** di menu Pembelajaran) — ujian sekolah vs AKM pemerintah.
- `EXAM_TYPE` opsional (`SURVEY` / `ELIGIBLE`) — hanya mengubah judul toolbar; **tidak** ada menu live yang mengirimkannya (kode lama dikomentari).

---

## 3. Tipe ujian & scope

### 3.1 `ExamType` (`AkmEntities.kt`)

| Konstanta | Nilai | Produk |
| --- | --- | --- |
| `TRYOUT` | 1 | Quiz / Try Out |
| `SCHOOL` | 2 | Asesmen sekolah **atau** jadwal AKM gov (tergantung `isSchoolScope` / `gov_schedule`) |

### 3.2 Scope & label UI

| `isSchoolScope` | `examType` | Toolbar / tombol | API utama |
| --- | --- | --- | --- |
| `true` (default menu) | `SCHOOL` | "Asesmen", "Ikuti Ujian", "Mulai Asesmen" | `exam-schedules`, `exam-school/...` |
| `false` | `SCHOOL` | "AKM", "Ikuti AKM", "Mulai AKM" | `akm/schedules`, `akm/student/exam/...` |
| — | `TRYOUT` | "Try Out" / Quiz | `try-out/...` |

`gov_schedule` pada item mempengaruhi ikon list dan query `gov_schedule` saat detail/download.

---

## 4. Status siklus ujian (`AkmStatus`)

| Status | Nilai | Arti UI |
| --- | --- | --- |
| `AKM_STATUS_NEW` | 0 | Belum sinkron soal → tombol **Sinkronisasi Soal** |
| `AKM_STATUS_DOWNLOADING` | 1 | Progress unduh soal |
| `AKM_STATUS_DOWNLOADED` | 2 | Siap mulai (jika waktu ≥ `date_start`) |
| `AKM_STATUS_FINISHED` | 3 | Sudah dikumpulkan lokal / menunggu upload |
| `AKM_STATUS_UPLOADED` | 4 | Jawaban terunggah; menunggu penilaian |
| `AKM_STATUS_SCORED` | 5 | Nilai tersedia → **Lihat Nilai** |
| `AKM_STATUS_EXPLAINED` | 6 | Pembahasan siap (setelah download explain) |

---

## 5. Peta navigasi — Role siswa

```
Pembelajaran → AkmPage                         ← shell tab Jadwal | Nilai
    │
    ├─ AkmListPage (start)                     ← jadwal (strip tanggal jika school scope)
    │       │  tap card
    │       ▼
    │   AkmDetailPage                          ← sinkron / mulai / lihat nilai
    │       │  password gate (opsional)
    │       │  dialog ketentuan + strict mode
    │       ▼
    │   AkmTakeResumePage                      ← daftar mapel/instruksi + Kumpulkan
    │       │  tap instruksi
    │       ▼
    │   AkmQuestionsPage                       ← kerjakan soal (lockdown/penalti)
    │       │
    │       └─ (selesai semua) → kembali resume → Kumpulkan → upload
    │
    └─ AkmScorePage                            ← riwayat nilai + Upload Ulang
            │  tap
            ▼
        AkmScoreDetailPage                     ← skor, unduh pembahasan
            └─ AkmExplanationPage              ← pembahasan soal
```

Nav graph: `res/navigation/akm_nav.xml` (List ↔ Score).

Worker pendukung: `AkmDownloader`, `AkmUploader`, `AkmExplanationDownloader`; service: `CountDownService`.

---

## 6. Screen-by-screen — Siswa

### 6.1 `AkmPage` — Shell

| Item | Detail |
| --- | --- |
| Layout | `akm_page.xml` |
| Tab | **Jadwal** (`btnIkuti`) / **Nilai** (`btnNilai`) |
| Judul | Default "Asesmen"; `SURVEY` → "Survey"; `ELIGIBLE` → "Kelas Eligible" |
| Back | Ke `HomePage` (bukan finish ke Pembelajaran saja) |

**Rules**

1. `isSchoolScope` diset ke ViewModel di init.
2. Jika school scope dan lokal kosong → `loadUjianSchool()` sekali.
3. Daftar nilai / AKM non-school mengandalkan **Paging boundary callback** (fetch saat lokal kosong), bukan prefetch ganda.
4. Saat init, ujian lokal unfinished (`status < UPLOADED`) diantre ke `AkmUploader` dengan delay sampai `date_end`.

### 6.2 `AkmListPage` — Jadwal

#### Mode school scope (`isSchoolScope = true`)

| Elemen | Rules |
| --- | --- |
| Strip tanggal | Hari dalam bulan; tanggal **masa lalu** disabled (alpha 0.3) |
| Label bulan | Tap → `MonthYearPickerDialog` |
| Data list | Room lokal per tanggal (`listUjianSchoolLocal`); ganti tanggal **tidak** auto-hit API |
| Swipe refresh | `loadUjianSchool()` network |
| Tombol card | "Ikuti Ujian" |

#### Mode AKM (`isSchoolScope = false`)

| Elemen | Rules |
| --- | --- |
| Strip tanggal | Tidak dipakai (list paging `listAkm`) |
| Tombol card | "Ikuti AKM" |
| Swipe refresh | `loadAkm()` |

#### Rules bersama

1. **Wajib waktu otomatis**: jika `auto time` / timezone off → dialog non-cancelable "Harap atur tanggal dan waktu ponsel ke Otomatis" → Buka Pengaturan / Jangan Ubah (finish).
2. Tap card → `AkmDetailPage` + `id`, `examType=SCHOOL`, `isSchoolScope`, `isGovSchedule`.
3. Empty state: "Belum terdapat ujian" (teks layout).
4. Setelah detail `RESULT_OK` + `showScore` → navigasi ke tab Nilai.

### 6.3 `AkmDetailPage` — Detail & mulai

| Extra | Arti |
| --- | --- |
| `id` | ID jadwal asesmen |
| `examType` | `SCHOOL` / `TRYOUT` |
| `isSchoolScope` | Cabang API & label |
| `isGovSchedule` | Query `gov_schedule` |

#### Elemen UI

- Nama, jenis, tenggat berakhir.
- Notice password (jika `requires_password` untuk jadwal school).
- Info peserta: ID ujian, nama, NIS/NISN, kelas, kategori, periode, sekolah.
- List beban/jumlah soal per sub-ujian.
- Progress sinkronisasi soal.
- Tombol aksi dinamis.

#### Rules tombol per status

| Status | Tombol / UI |
| --- | --- |
| `NEW` | **Sinkronisasi Soal** → WorkManager download; gagal → alert + tombol kembali |
| `DOWNLOADING` | Progress `download_progress / total_questions` |
| `DOWNLOADED` + sekarang ≥ `date_start` | **Mulai Asesmen / AKM / Try Out** → password gate |
| `DOWNLOADED` + belum mulai | Label "Asesmen belum dimulai" |
| `FINISHED` / `UPLOADED` | Label "Asesmen telah dikumpulkan"; alert waktu habis (kondisional) |
| `SCORED` | **Lihat Nilai** → `AkmScoreDetailPage` |

#### Rules password (school + school scope saja)

1. `requiresPassword` dari `akm_settings` key `requires_password_{id}`.
2. Jika wajib dan belum `password_checked_{id}` → dialog **Password Ujian**.
3. Password kosong → error "Password ujian wajib diisi".
4. API `POST .../exam-school/{id}/check-password` (+ device fingerprint).
5. Error khusus:
   - sudah digunakan → minta **guru** generate password baru
   - perangkat lain → pakai HP yang sama / minta guru generate ulang
   - terlalu banyak percobaan → tunggu
6. Setelah lolos (atau tidak wajib) → dialog ketentuan mulai.

#### Rules dialog mulai + mode ketat (`exam_lock_mode`)

Sebelum start, `refreshPenaltySetting(force=true)`.

Jika **strict mode aktif** (default aktif jika setting belum ada):

- Syarat UI: layar dikunci; wajib online saat **mulai**.
- Offline saat start → `prettyAlertt` "Butuh Koneksi Internet" (tidak start).

Jika strict mati:

- Teks: pengerjaan offline diperbolehkan; tidak mengunci layar.

Ketentuan umum selalu disebut: sanksi keluar/pindah app, internet stabil saat upload, larangan angkat telepon.

Setelah OK → `startExamIntent()` ke `AkmTakeResumePage` (izin FGS opsional di jalur permission terpisah).

### 6.4 `AkmTakeResumePage` — Daftar instruksi & kumpulkan

| Elemen | Detail |
| --- | --- |
| List | Mapel/exam expand → instruksi soal (progres answered/num_question, status Belum/Proses/Selesai) |
| Tombol | **Kumpulkan** — enabled jika semua exam `finished` (semua instruksi terjawab penuh) |
| Mode kunci | `ExamLockdown` jika strict mode |
| Penalti | `CountDownService` + dialog jika `penalty_applied` |

**Rules**

1. Instruksi selesai (`answered == num_question`) → exam ditandai `finished` di Room.
2. School/AKM: Kumpulkan hanya jika semua selesai (tombol disabled selama belum).
3. Try Out: boleh kumpulkan meski ada soal belum terjawab → konfirmasi "Terdapat soal yang belum terselesaikan".
4. Upload → `uploadAnswer(akmId)` → WorkManager / API sesuai `exam_type`.
5. Sukses upload → lepas lockdown, navigasi keluar.
6. Back saat lockdown aktif → **ditolak**; pesan selesaikan lewat Kumpulkan / tunggu waktu habis.
7. Status `FINISHED` / `UPLOADED` saat resume → auto redirect / release lock.
8. Overlay / loss focus (jika penalti aktif) dapat memicu penalti timer.

### 6.5 `AkmQuestionsPage` — Kerjakan soal

| Elemen | Detail |
| --- | --- |
| Navigasi | Label nomor → grid `QuestionSelectDialog` |
| Swipe | Kartu soal horizontal (snap) |
| Menu | Selesai (kembali ke resume) |
| Media | Audio / video / gambar; offline → ajakan aktifkan data |
| Penalti badge | Timer bila terdeteksi keluar/app lain |

#### Tipe soal (`AkmAnswerType`)

| Kode | Jenis UI |
| --- | --- |
| 0 | Pilihan ganda 1 jawaban |
| 1 | Pilihan ganda gambar |
| 2 | Esai panjang |
| 3 | Benar / salah |
| 4 | Pilihan ganda multi jawaban |
| 5 | Tabel pernyataan |
| 6 | Menjodohkan teks |
| 7 | Menjodohkan gambar |
| 8 | Esai singkat angka |
| 9 | Esai singkat kata (tidak boleh spasi) |

Jawaban disimpan ke Room lokal; upload kemudian (mendukung offline setelah start jika strict hanya menahan **awal** ujian).

**Rules**

1. Waktu habis (`status = FINISHED`) → alert kumpulkan + release lockdown → `AkmPage`.
2. Sudah `UPLOADED` → keluar ke `AkmPage`.
3. Whitelist keluar ke Settings jaringan agar tidak kena penalti palsu.
4. Mode kunci lepas → dialog recovery "Aktifkan" ulang.

### 6.6 `AkmInstructionPage`

Dialog/halaman instruksi HTML sebelum/saat mengerjakan (petunjuk per instruksi).

### 6.7 `AkmScorePage` — Riwayat nilai

| Elemen | Rules |
| --- | --- |
| Strip tanggal | Disembunyikan |
| Empty | "Belum terdapat nilai" |
| Data | School: `listUjianSchoolScored`; AKM: `listScoreAkm` |
| **Upload Ulang** | Tampil jika status `FINISHED`/`UPLOADED` (atau gagal terakhir), kecuali skor "tidak mengerjakan" / "sesi terlewat" |
| Cooldown | 1 menit antar upload ulang (`akm_upload_cooldown_{id}`) |
| Tanpa data lokal | Alert: tidak bisa upload ulang di perangkat ini |

Tap item → detail nilai / lanjut ke hasil.

### 6.8 `AkmScoreDetailPage` — Hasil

| Item | Detail |
| --- | --- |
| Judul | "Hasil Asesmen" / "Hasil AKM" / "Hasil Quiz" |
| Skor | Ditahan sampai refresh server selesai (hindari flash nilai lama) |
| List | Skor per subtes / diagram (tergantung template) |
| Try Out | Tombol cek passing grade → `TryOutPassingGradePage` |
| Pembahasan | Jika `AkmSettings.hasExplain(id)` → unduh explain → `AkmExplanationPage` |

### 6.9 `AkmExplanationPage` — Pembahasan

List nomor instruksi + card: soal, jawaban siswa, benar/salah, kunci, teks pembahasan, gambar full-screen.

---

## 7. Role guru — rules (apa yang berlaku di app)

| Rules | Status di Android Portal |
| --- | --- |
| Membuka menu Asesmen | **Tidak** — item dihapus untuk non-siswa |
| Membuat / menjadwalkan asesmen | **Tidak ada UI** |
| Generate password ujian | **Tidak ada UI**; siswa diarahkan "Minta guru generate password baru" (luar app) |
| Menilai jawaban di app | **Tidak**; nilai datang dari server (`SCORED`); komentar kode: guru menilai di server |
| Melihat nilai siswa di app | **Tidak** sebagai fitur guru |
| Agenda Mingguan di slot menu yang sama | Ya (pengganti slot Asesmen) |

Implikasi dokumentasi/QA: skenario "login guru → Asesmen" harus diharapkan **tidak** membuka `AkmPage`. Verifikasi create/score asesmen dilakukan di **kanal admin/web**, bukan di app ini.

---

## 8. Setelan server (`GET mobile/setting-akm` → `akm_settings`)

| Key | Default jika belum ada | Efek |
| --- | --- | --- |
| `exam_lock_mode` | **Aktif** (`raw != 0`) | Strict: pin layar + wajib online saat mulai |
| `penalty_applied` | **Aktif** | Penalti saat keluar/overlay |
| `absence_setting` | Aktif | Dipakai modul lain (presensi) |
| `requires_password_{akmId}` | false kecuali `== 1` | Dialog password |
| `password_checked_{akmId}` | false kecuali `== 1` | Skip dialog password |
| `has_explain_{akmId}` | false kecuali `== 1` | Tombol pembahasan |
| Durasi penalti (detik) | Dari response setting / default lokal | `CountDownService` |

Sentinel DAO `-1` = "belum tersimpan", **bukan** false — mapping ada di `AkmSettings`.

---

## 9. Perbandingan siswa vs guru (Asesmen)

| Capability | Siswa | Guru (app) |
| --- | --- | --- |
| Lihat jadwal | Ya | Tidak |
| Sinkron & kerjakan soal | Ya | Tidak |
| Password gate | Ya (konsumen) | Generate di luar app |
| Lockdown / penalti | Ya | N/A |
| Kumpulkan & upload | Ya | N/A |
| Lihat nilai sendiri | Ya | N/A |
| Upload ulang jawaban | Ya (dengan syarat) | N/A |
| Lihat pembahasan | Ya (jika `has_explain`) | N/A |
| Create / edit / hapus jadwal | Tidak | Tidak di app |

---

## 10. API reference (siswa)

### Asesmen sekolah

| Method | Path | Dipakai untuk |
| --- | --- | --- |
| GET | `mobile/app/learning/akm/exam-schedules` | List jadwal |
| GET | `.../exam-schedules-scored` | List nilai |
| GET | `.../exam-schedules/{id}?gov_schedule=` | Detail |
| GET | `.../akm/student/exam-school/{id}/download` | Unduh soal |
| POST | `.../exam-school/{id}/check-password` | Cek password |
| POST | `.../exam-school/{akm_id}/answer/{student_id}` | Upload jawaban |
| GET | `.../exam-schedules-scored/{id}/explains` | Pembahasan |

### AKM pemerintah (`isSchoolScope = false`)

| Method | Path |
| --- | --- |
| GET | `.../akm/schedules`, `.../akm/scored` |
| GET | `.../akm/schedules/{id}` |
| GET | `.../akm/student/exam/{id}/download` |
| POST | `.../akm/student/exam/{akm_id}/answer/{student_id}` |
| GET | `.../akm/student/exam/{id}/review` |

### Setelan & try-out

| Method | Path |
| --- | --- |
| GET | `mobile/setting-akm` |
| GET/POST | `mobile/app/learning/try-out/...` (jadwal, answer, scored, passing-grade) |

Tidak ada path `.../akm/teachers/...` atau `.../examinations/teachers/...` di `ApiService` untuk fitur ini.

---

## 11. Model & komponen kunci

| File / tipe | Peran |
| --- | --- |
| `AkmTable` / `AkmSchedule` | Jadwal + status + skor |
| `AkmExamsTable` | Sub-ujian / beban soal |
| `AkmInstructionTable` + pertanyaan/jawaban Room | Paket soal offline |
| `AkmViewModel` | List, detail, download, password, upload, setting |
| `AkmSettings` | Saklar strict / penalti / password / explain |
| `ExamLockdown` | Pin layar selama ujian |
| `CountDownService` | Timer penalti |
| `AkmUploader` / `AkmDownloader` | WorkManager |
| `Question*Vh` | Renderer tipe soal |

---

## 12. Daftar file terkait

| Area | Path |
| --- | --- |
| Shell & list | `AkmPage.kt`, `AkmListPage.kt`, `AkmScorePage.kt` |
| Detail & mulai | `AkmDetailPage.kt` |
| Kerjakan | `AkmTakeResumePage.kt`, `AkmQuestionsPage.kt`, `AkmInstructionPage.kt` |
| Nilai & pembahasan | `AkmScoreDetailPage.kt`, `AkmExplanationPage.kt` |
| Model / DAO | `AkmEntities.kt`, `AkmModels.kt`, `AkmDao.kt`, `AkmAnswerPayload.kt` |
| Nav / layout | `res/navigation/akm_nav.xml`, `res/layout/akm_*.xml` |
| Entry | `PembelajaranPage.kt` |
| Try Out (share detail) | `pages/tryout/**` |
| Legacy ujian | `pages/ujian/**` |

---

## 13. Checklist QA

### Siswa

- [ ] Tanpa kelas → menu locked.
- [ ] Waktu HP manual → dialog wajib set otomatis sebelum list.
- [ ] Strip tanggal: hari lalu tidak bisa dipilih; swipe refresh mengisi lokal.
- [ ] Alur status: Sinkron → Mulai (setelah `date_start`) → kerjakan → Kumpulkan → nilai.
- [ ] Password wajib: salah / dipakai / device lain menampilkan pesan yang benar.
- [ ] Strict mode on + offline → tidak bisa mulai; on + online → lockdown.
- [ ] Strict mode off → boleh mulai tanpa pin; penalti mengikuti `penalty_applied`.
- [ ] Kumpulkan school disabled sampai semua instruksi selesai; Try Out boleh partial dengan konfirmasi.
- [ ] Upload ulang: cooldown 1 menit; gagal tanpa data lokal.
- [ ] Setelah `SCORED`, Lihat Nilai tidak flash skor lama.

### Guru

- [ ] Login guru: menu **Asesmen tidak muncul**; ada Agenda Mingguan.
- [ ] Tidak ada deep link produk yang membuka create/score asesmen untuk guru di app.
- [ ] Skenario password: siswa melihat copy yang merujuk aksi guru di luar app.

---

## 14. Catatan implementasi

1. **Guru = no product surface** untuk Asesmen di app; dokumentasi rules guru adalah *absence of feature* + pesan password.
2. Default `isSchoolScope=true` dari Pembelajaran = jalur **ujian sekolah**, bukan AKM nasional.
3. `EXAM_TYPE` SURVEY/ELIGIBLE hanya sisa kode judul; tidak di-wire menu.
4. Setting strict/penalti default **aktif** jika belum pernah sync — lebih aman daripada terbuka.
5. Jawaban offline didukung setelah start; yang diblokir strict hanyalah **awal** ujian saat offline.
6. Kenaikan skema Room dihindari untuk flag `has_explain` (disimpan di `akm_settings`) agar tidak destructive-migration menghapus jawaban lokal.
7. Modul `pages/ujian` adalah jalur lama; notifikasi bisa masih mengarah ke sana — bedakan dari Asesmen `pages/akm`.

---

*Dokumen ini mengikuti perilaku kode di branch kerja saat dokumentasi dibuat. Perubahan kontrak setting-akm, password, atau penilaian server perlu diselaraskan ke bagian rules & API.*
