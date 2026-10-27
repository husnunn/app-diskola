# 05c — Selisih Implementasi Tugas vs Aplikasi Lama

Status per fitur untuk cakupan yang dikerjakan 2026-10-02: **Tugas Siswa** (doc `05` §5) dan
**Tugas Guru** (doc `05` §6), dibangun mengikuti persis pola arsitektur Materi (`05a`/`05b`).
Laporan implementasi lengkap ada di `05d-implementasi-tugas.md`.

Path relatif terhadap `Diskola-App-New/app/src/main/java/id/diskola/app/`.

Legenda status: ✅ Selesai & diverifikasi di device fisik · ✅* Selesai (kode benar, belum sempat
diuji jalur ini secara langsung) · ⛔ Ditunda/di luar cakupan.

## 1. Tugas — Siswa (doc §5)

| Item | Status | Keterangan |
|---|---|---|
| 3 tab Belum/Sudah/Nilai, paging 10/halaman | ✅ Selesai | `TugasSiswaViewModel` (3 state paging independen) + `TugasSiswaScreen`. Diverifikasi di device: 3 endpoint terpanggil tepat sekali per buka layar (`backlog`/`done`/`scored`), tab berganti tanpa fetch ulang, empty state benar per tab ("Belum terdapat tugas" / "Belum terdapat rekap tugas"). Akun uji tidak punya tugas sama sekali, jadi daftar isi (badge merah/abu, dsb.) **belum teruji dengan data nyata**. |
| Banner error saat fetch gagal (keputusan: tambahkan, app lama diam) | ✅* Selesai | `launchWithHandling(showError=true)` dipakai di tiap tab, `BannerError` tampil di `TugasSiswaScreen`. Belum sempat memaksa kegagalan jaringan untuk memverifikasi tampilannya. |
| Status murni dari endpoint terakhir (bukan dihitung dari tanggal) | ✅* Selesai | `HomeworkTable.type` (0/1/2) ditulis sesuai endpoint yang memuatnya, bukan dihitung ulang di klien — sama seperti doc §5.4. Hanya baris dengan `school.uuid` cocok sesi berjalan yang disimpan. |
| Detail tugas: Room-first lalu "ensure" list relevan bila belum ada (keputusan user, bukan layar kosong) | ✅* Selesai | `TugasDetailViewModel.load()` — beda dari Materi (yang punya endpoint detail-by-id asli): Tugas tidak punya endpoint detail bersih, jadi fallback-nya memanggil `ensureBacklog/Done/ScoredFirstPage()` (siswa) atau `ensureTeacherOwnFirstPage()` (guru) dulu, baru cek Room lagi. Dialog "Tugas Tidak Tersedia" kalau tetap tidak ketemu. **Belum diuji** — tidak ada tugas sama sekali di akun uji untuk memicu jalur ini. |
| Kartu "Syarat pengumpulan", seksi Soal/Jawaban Terkumpul/Kumpulkan Jawaban/Pembahasan | ✅* Selesai | Semua seksi kondisional dibangun di `TugasDetailScreen` sesuai doc §5.5. **Belum diuji dengan data nyata** — akun uji tidak punya tugas untuk dibuka. |
| Syarat baca/upload reaktif (keputusan: perbaiki dari one-way latch app lama) | ✅* Selesai | `TugasDetailViewModel.uploadDone` dihitung ulang tiap kali file/link berubah (bukan flag permanen); menghapus semua file & mengosongkan link mengembalikan status ke "belum". **Belum diuji di device** (butuh tugas nyata dengan `uploaded>0`). |
| Syarat baca diperketat (keputusan: hanya unduhan sukses yang menghitung, bukan "dialog dibatalkan tapi file sudah ada") | ✅* Selesai | `readDone` di-set true hanya di jalur sukses `openFile()` — tidak ada kondisi khusus untuk dialog dibatalkan. Berlaku otomatis dari pola dialog-gated-download yang sama dengan Materi. |
| Validasi upload jawaban: ekstensi & 12 MB/file, pesan persis | ✅* Selesai | `TugasDetailViewModel.addSelectedFile()` menolak dengan pesan persis doc ("Format file tidak didukung: …", "Ukuran maksimal 12 MB per file: …"). Multi-file (`file[]`) — `TugasApiService.collectAssignment` diubah dari satu file jadi `List<MultipartBody.Part>` (scaffold lama cuma dukung 1 file). **Belum diuji** — perlu backend Multipart `file[]` diverifikasi menerima bentuk ini. |
| Kirim sukses → baris hilang dari tab Belum | ✅* Selesai | `TugasRepository.collectAssignment()` memanggil `homeworkDao.deleteById()` setelah API sukses. Belum diuji end-to-end (butuh tugas nyata). |
| Dialog Nilai (skor + pembahasan) | ✅* Selesai | `TugasNilaiDetailSheet` — `AppBottomSheet` dengan skor, `information_label`, kartu pembahasan. Belum diuji (tidak ada tugas ternilai di akun uji). |

## 2. Tugas — Guru (doc §6)

| Item | Status | Keterangan |
|---|---|---|
| "List Tugas": filter Kelas/Mapel AND, 1x fetch (keputusan: perbaiki dari bug 3x-fetch app lama) | ✅ Selesai, **diuji di device** | `TugasGuruViewModel` — pola `combine()`+`flatMapLatest` sama persis `MateriGuruViewModel`. Akun guru (NISN `20202620`) tersedia belakangan di fase ini: 1x fetch dikonfirmasi lewat `adb logcat`, dan terungkap+diperbaiki bug nyata `teacher_id=0` (lihat `FLOW_QUESTIONS.md` entri 2026-10-02 "Bug nyata ditemukan & diperbaiki"). Filter Kelas/Mapel sendiri (irisan AND dengan data sungguhan) belum sempat dicoba. |
| Filter "Semua" kirim literal `0` (keputusan: tiru app lama persis, bukan dihilangkan) | ✅* Selesai | `TugasRepository.fetchTeacherOwnPage()` selalu mengirim `school_classes_id`/`school_subject_id` termasuk nilai `0` untuk "Semua" — **belum diverifikasi** apakah backend benar-benar menerima `0` sebagai "semua" (doc sendiri menandai ini belum terverifikasi di app lama). |
| Kebab Edit/Hapus + konfirmasi hapus | ✅* Selesai | `TugasGuruScreen`'s `AppBottomSheet`/`AppDialog`, teks persis ("Anda yakin akan menghapus tugas?"). Belum diuji. |
| "Penilaian" tab, empty state | ✅* Selesai | `TugasPenilaianViewModel`/`PenilaianTab`. Belum diuji. |
| Form Buat/Edit/Detail (3 mode), cascade Kelas→Hari→Mapel | ✅* Selesai | `UploadTugasViewModel`/`UploadTugasScreen`. Cascade fetch jadwal (`fetchSchedule`) dibungkus `launchWithHandling` — **perbaikan wajib** vs app lama yang crash saat jaringan gagal di titik ini (tidak ada try/catch sama sekali di kode lama). Mode Detail (`editable=false`) menonaktifkan semua field. Belum diuji. |
| Edit mode: tanggal default ke sekarang (keputusan: bukan mengurai label server) | ✅* Selesai | `UploadTugasScreen`'s `LaunchedEffect(existing)` sengaja **tidak** mengisi `endAt` dari `end_at_label` — tetap `LocalDateTime.now()`, guru wajib pilih ulang. Belum diuji. |
| Validasi "Kelas/Hari/Mapel wajib dipilih", tombol aktif sesuai aturan | ✅* Selesai | Pesan inline persis doc, tombol `canSubmit` sesuai rumus §6.4. Belum diuji. |
| "Tugas Terkumpul": panel Detail collapsible, filter lokal Semua/Belum/Sudah | ✅* Selesai | `TugasTerkumpulViewModel`/`Screen`. Selalu network (tanpa cache Room, sesuai sifat data yang berubah tiap nilai disimpan). Belum diuji. |
| "Lihat Jawaban" terhubung (keputusan: app lama listener-nya kosong, sekarang dihubungkan ke Detail Tugas mode guru read-only) | ✅* Selesai | Tombol memanggil `onOpenTugasReadonly` → `Route.TugasDetail(isTeacher=true)`. Belum diuji. |
| Beri/Ubah Nilai: input 0–100, info peserta, file jawaban | ✅* Selesai | `TugasScoringViewModel`/`Screen` — skor diambil dari `Assignment` yang sudah dimuat `TugasTerkumpulViewModel` (tidak fetch ulang). Belum diuji. |

## 3. Catatan teknis penting (perlu diverifikasi begitu ada data/akun nyata)

- **Tidak ada satu pun tugas di akun uji** (NISN 202005) selama sesi ini — ketiga endpoint siswa
  (`backlog`/`done`/`scored`) terverifikasi 200 OK tapi selalu `{"data":[]}`. Ini berarti **seluruh
  bentuk JSON `HomeworkItem`/`HomeworkCollected`/`Assignment` belum pernah divalidasi terhadap
  response asli** — berbeda dengan fase Materi yang sempat menemukan & memperbaiki ketidakcocokan
  field nyata. Risiko terbesar: tipe field `count_assignment_collected_*` (`String` di scaffold
  lama — dipertahankan apa adanya, belum dikonfirmasi benar) dan apakah `student_assignment_id`
  benar-benar dikirim oleh backend di body list (dipakai untuk fallback fetch jawaban).
- `TugasApiService.collectAssignment` diubah dari `@Part file: MultipartBody.Part?` (satu file)
  menjadi `@Part files: List<MultipartBody.Part>` — **perubahan kontrak wajib** karena submit
  jawaban butuh multi-file (`file[]`), tapi belum ada kesempatan memverifikasi bentuk `file[]` ini
  diterima backend (dev server tidak dicoba karena tidak ada tugas untuk dikirim jawabannya).
- Komponen baru yang diekstrak jadi reusable: `ui/components/SimpleDropdown.kt` (sebelumnya privat
  di `UploadMateriScreen.kt`, sekarang dipakai juga oleh `UploadTugasScreen.kt`).

## 4. Kontrak backend baru (2026-10-22) — diterapkan di app baru 2026-10-22, form guru mode bebas diuji di perangkat (sisanya belum)

Backend mengubah API form tugas guru (cabang `teacher-data-scope`, payload kanonik); app lama (`android-portal` `d3aa201c`; iOS juga) sudah disesuaikan dan app baru
**kini mengikutinya** (kode: `utils/AssignmentRules.kt`, `utils/AssignmentErrors.kt`, `apiservice/ApiErrorParser.kt`, `viewmodel/UploadTugasViewModel.kt`,
`ui/screens/tugas/UploadTugasScreen.kt`, `repository/TugasRepository.kt`; tes: `AssignmentRulesTest` (+`AssignmentFilterTest`), `AssignmentErrorsTest`,
`ApiErrorParserTest`, `HomeworkModelsParseTest`, `AkmMigrationTest`). Rincian perilaku: `05-pembelajaran-materi-tugas.md` §6.4–§6.5; keputusan: `FLOW_QUESTIONS.md`
(2026-10-22, "Tugas guru di app baru"). Logika murni diport dari app lama; sumber kode lama yang benar: `DISKOLA MOBILE/android-portal` (klon di `proyek diskola/` basi).

Legenda: ✅ dikerjakan (hanya diuji JVM/build) · ⏸ ditunda · ❓→✅ diputuskan user 2026-10-22 ("ikuti app lama").

| Item | Status | Keterangan |
|---|---|---|
| Baca `GET mobile/attendance/teacher-data-scope`; cabang `data.restricted` (gagal/hilang → mode assigned) | ✅ | `TeacherDataScope.restricted/label`, `isRestrictedForForm = restricted != false`; `TugasRepository.fetchTeacherScope()` tanpa cache (cache scope Presensi/Jurnal gagal ⇒ ALL, sebaliknya dari yang dibutuhkan); gagal ⇒ mode jadwal + banner |
| Mode bebas: Kelas + Mapel dari `GET .../teachers/subject?take&skip` (tanpa Hari) | ✅ | `TugasApiService.teacherAssignmentSubject`; paging 100, berhenti bila < 100 atau tanpa id baru, dedupe, urut nama |
| Kelas form dari `assignment/teachers/class` | ✅ | **Tidak ditulis ke Room** (`classroom` dipakai bersama Materi dan layar filter); sebelumnya form memakai `mobile/teacher/school-class-room` dan menimpa tabel itu |
| Payload kanonik (assigned `school_subject_schedule_id`; bebas `school_class_id` + `school_subject_id`; `title,description,end_at,checked,downloaded,uploaded`; `link` hanya bila tidak kosong); kunci lama dibuang | ✅ | `AssignmentPayload.fields`; `restricted == null` ⇒ bentuk assigned; `link`/file/`downloaded` hanya bila "Upload Soal" aktif (dulu dikirim walau tidak dicentang) |
| `end_at` `yyyy-MM-dd HH:mm:ss` | ✅ | `AssignmentDates.formatWire` (Locale.US); sebelumnya `dd-MM-yyyy HH:mm:ss` |
| Penanganan error: 422 per field + draft utuh; 404 saat update; 401; 403 "Akses diblokir" + `trace_id`; 413 | ✅ | `ApiException.fieldErrors/traceId` (diisi `ApiErrorParser` di interceptor); `AssignmentErrors.classify/placeFieldErrors`; 422 ⇒ pesan di bawah field (+ dialog "Periksa kembali isian" untuk `file.N` dkk), 404 ⇒ hapus baris + tutup + segarkan, 403 ⇒ dialog + ID, 413 ⇒ "Ukuran file terlalu besar. Maksimal 12 MB", 401 ⇒ penanganan sesi pusat |
| Prefill edit: mapel dari `subject_id`; Hari + jadwal; baris lokal hilang ⇒ tutup form | ✅ | Kolom baru `homework.schedule_day` (**Room v10, `MIGRATION_9_10` eksplisit** — tanpa migrasi, `fallbackToDestructiveMigration` menghapus seluruh DB termasuk jawaban ujian yang belum terkirim); baris hilang ⇒ toast + tutup (dulu membuka form kosong "Edit Tugas") |
| Prefill edit: `end_at` dari `end_at_label` | ❓→✅ Q18 | `AssignmentDates.parseEndAtLabel`; gagal ⇒ kosong + toast "Waktu berakhir tidak terbaca, silakan pilih ulang". **Membalik keputusan 2026-10-02 #3** (alasan lama — label relatif — tidak berlaku: label nyata berupa tanggal absolut) |
| Filter "Semua": tidak kirim bila 0; indeks `filter[n]` berurutan | ❓→✅ Q19 | `AssignmentFilter.teacherListQuery` + 5 tes. **Membalik keputusan 2026-10-02 #6** |
| Deskripsi wajib; tanpa default tanggal; Posting tidak menunggu preview link; file soal ≤ 12 MB di klien | ❓→✅ Q16, Q17 | `AssignmentPayload.validate`; tombol Posting nonaktif sampai valid; DatePicker hanya hari ini dan sesudahnya |
| Refresh daftar guru setelah create | ✅ (sudah ada) | `tugasChanged` ⇒ `refresh()`; tidak berubah |
| Sisi siswa: `link` kosong tidak dikirim; 422 per file (`file.N` → "File ke-N: …"); 403 "Akses diblokir" + trace id; 400 teks server | ✅ | `TugasDetailViewModel.submit`; tombol "Kirim Tugas" kini ber-`loading` (dulu selalu `false`, bisa kirim dua kali); link spasi-saja tidak dihitung sebagai jawaban |
| Penilaian: id pengumpulan/penugasan ≤ 0 tidak memanggil API; parsing toleran `id`/`score`/`scored`/`uploaded` null, `schedule` null atau `[]`, `data.id` collect null | ✅ | `@NullToZeroInt`, `HomeworkItem.schedule` nullable + adapter `[]` ⇒ null, `TugasScoringViewModel` guard; `LazyColumn` Tugas Terkumpul memakai kunci cadangan agar id 0 ganda tidak crash |
| Multi-kelas (satu request per kelas, hasil per kelas) | ⏸ ditunda (❓ Q20) | Keputusan user: satu kelas dulu |

**Diverifikasi di perangkat (2026-10-22; Samsung SM-A505F Android 11, server `dev.api.diskola.id`, akun guru uji SMK DEMO SURABAYA):**
- `teacher-data-scope` → `{"scope":"all","restricted":false}` ⇒ form mode bebas; kelas dari `teachers/class`, mapel dari `teachers/subject?take=100&skip=0`, semuanya 200.
- Form: batas waktu kosong di awal, tanggal sebelum hari ini tidak bisa dipilih (hari ini bisa), deskripsi wajib, "Posting Tugas" nonaktif sampai semua terisi.
- Create: `POST .../teachers/create` multipart dengan kunci `title, description, school_class_id, school_subject_id, end_at (2026-10-26 13:24:00), checked=1, downloaded=0, uploaded=0` — tanpa `link` dan tanpa kunci lama — ⇒ 200 `{"message":"Berhasil membuat Tugas"}`; daftar dimuat ulang dan tugas baru (id 87) tampil. Server mengembalikan `"schedule":{}` untuk tugas mode bebas dan daftar tetap terbaca.
- Edit: kelas, mapel, judul, deskripsi, dan `end_at` dari `end_at_label` ("26 Oct 2026, 13:24" → 26-10-2026 13:24) terisi benar; `POST .../teachers/update/87` ⇒ 200 `{"message":"Berhasil update Tugas"}`; buka lagi, batas waktu baru terisi; `link` lama ikut terkirim ulang. Tautan bukan-URL memunculkan banner "gagal menampilkan preview url" yang tidak menghalangi Posting.
- Header `X-App-Platform: android` dan `X-App-Version: 1.0.0` ada di semua request (log `API_REQUEST`); migrasi Room 9→10 jalan pada DB nyata (versi 10, kolom `schedule_day` ada; tabel ujian masih kosong sehingga **pelestarian data belum terbukti**).
- **Simpan nilai** (Penilaian → Tugas Terkumpul → Beri Nilai): sebelumnya selalu gagal dengan "Gagal menyimpan nilai" karena `saveAssignmentScore` ditolak Retrofit sebelum request dibuat (wildcard pada `@Body Map<String, Any>`; perbaikan `@JvmSuppressWildcards`, lihat `FLOW_QUESTIONS.md` 2026-10-22). Setelah perbaikan: `POST .../teachers/scored/74/student-assignment/2701` body `{"score":80}` ⇒ 200, daftar menampilkan 80 dan tombol "Ubah Nilai". Nilai 80 untuk siswa RIZKY (tugas "UPLOAD SOAL WAJIB MEMBACA MATERI") kini tersimpan di server dev dari uji ini.
- **Hapus** (`DELETE .../teachers/delete/87`) ⇒ **400 `{"message":"Tugas telah dikerjakan"}`** pada tugas yang baru dibuat, baik dengan batas waktu lampau maupun masa depan — aturan/perilaku backend dev, bukan form. App sebelumnya menutupi alasannya dengan kalimat tetap "Gagal menghapus tugas. Silakan coba lagi."; sekarang pesan server ditampilkan (sama dengan app lama yang memakai `e.message`) — terbukti di perangkat. Tugas uji `UJI-FORM-GURU-BARU` (id 87) **masih ada di server dev** dan tidak bisa dihapus lewat app. Terulang dengan akun guru kedua di Infinix (lihat di bawah): `delete/88` ⇒ 400 yang sama.

**Diverifikasi di Infinix X1102 (tablet 1200×1920, Android 15, tema terang), akun guru kedua "Johan" — 2026-10-23:** scope `{"scope":"assigned","restricted":false}` ⇒ juga form mode bebas (jadi mode assigned tetap belum teruji; nilai `scope` tidak dipakai, hanya `restricted`). Buat tugas dengan mapel bernama panjang ("Dasar-Dasar Program Keahlian Pengembangan Perangkat Lunak dan Gim" — muat satu baris), link `https://example.com` (pratinjau "Example Domain" tampil), "Siswa wajib membaca materi" dan "Siswa wajib mengumpulkan jawaban" dicentang ⇒ multipart `title, description, school_class_id=4969, school_subject_id=34, end_at=2026-10-26 08:34:00, checked=1, downloaded=1, uploaded=1, link=https://example.com` ⇒ 200; server menyimpan `downloded:1`, `uploaded:1`, `uri.link:["https://example.com"]` (id 88). Edit: link dan ketiga centang terisi kembali. Simpan nilai (`Ubah Nilai`, nilai sama 99 untuk satu siswa; tidak ada perubahan data) ⇒ `POST .../scored/54/student-assignment/2297` `{"score":99}` ⇒ 200. Hapus tugas 88 ⇒ 400 "Tugas telah dikerjakan" (sama dengan akun pertama), banner hilang sendiri ±5 dtk. Tugas uji `UJI-INFINIX-GURU` (id 88) juga tertinggal di server dev.

**Akses guru (2026-10-23):** `AssignmentAccess.canManage` (akun `is_teacher` **dengan** record guru, `teacher.id > 0`) menjaga layar Tugas guru, tab Penilaian, dan form; akun tanpa data guru melihat "Tugas guru tidak tersedia" dan tidak memicu request teacher. Akun Infinix ber-role "Janitor" **punya** data guru (`rule.is_teacher:true`, `teacher.id` terisi) sehingga diizinkan backend — bukan celah app. Jalur tanpa data guru hanya diuji JVM (`AssignmentAccessTest`), belum di perangkat.

**Belum diverifikasi:** mode assigned (akun uji berada di mode bebas); 422/403/404/413 — server dev menerima `end_at` lampau dan `link` bukan-URL (200), jadi 422 tak bisa dipicu dari UI, hanya terbukti lewat tes JVM; 401; unggah file soal; cara menghapus file/link lama saat edit; sisi siswa (rumus, "Selesai", kirim tugas); apakah backend masih menerima payload lama (tidak diuji — payload lama diganti seluruhnya).

Catatan: pola yang berhasil di app lama dan ditiru — logika murni (payload + validasi + format/parse tanggal + pemetaan error + penempatan pesan 422) dipisah dari UI sehingga diuji JVM,
selaras `rules-global.md` §5.2.

