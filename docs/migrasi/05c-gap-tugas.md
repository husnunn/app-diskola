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
