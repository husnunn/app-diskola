# Plan: loading skeleton untuk muat pertama konten

Status: **plan, belum dikerjakan.** Dokumen ini hanya berisi rencana; tidak ada kode yang diubah.
Dibuat: 2026-10-30.

## Keputusan

Pakai **skeleton** (dengan animasi shimmer) untuk muat pertama layar yang isinya daftar, kartu, atau detail.
Spinner dan `AppLoadingDialog` tetap dipakai untuk **aksi** (login, kirim, unggah, simpan). Pull-to-refresh tetap
untuk refresh daftar yang sudah tampil.

Semua jalur file di bawah relatif terhadap `app/src/main/java/id/diskola/app/` kecuali disebut lain.

## Kondisi sekarang

Ada tiga jenis loading:

1. **`LoadingState`** (`ui/components/States.kt:36-81`). Baris abu-abu statis (kotak ikon 44 dp dan dua garis
   teks, baris kedua dst. beropacity 0,7) **ditambah** baris spinner 18 dp dan teks "Memuat…". Tidak ada animasi
   pada baris abu-abu. Parameter: `rows` (bawaan 3).
2. **`AppLoadingDialog`** (`ui/components/AppLoadingDialog.kt`) untuk aksi: login/cari pengguna, kirim jawaban,
   unggah tugas, simpan jurnal, aktivasi Klaspay, dst.
3. **Pull-to-refresh** (`PullToRefreshBox`) di Poin dan Presensi, dan spinner kecil di Notifikasi.

`LoadingState` dipakai di:

| Modul | Layar |
|---|---|
| AKM | `AkmScreen`, `AkmDetailScreen`, `AkmScoreScreens`, `AkmResumeScreen`, `AkmQuestionScreen` |
| Tugas | `TugasDetailScreen` |
| Jurnal | `JurnalScreen`, `JurnalDetailScreen` |
| Materi | `MateriDetailScreen` |
| Pembayaran | `SppScreens` (`rows = 4` dan `rows = 5`) |

**Catatan penting:** bentuk `LoadingState` sekarang memang sesuai spesifikasi handoff desain
(`ref/design_handoff_auth_fase1/README.md:461`: "skeleton row … + baris spinner 18dp/2.5dp + label 12sp"), dan
`docs/ANDROID_DESIGN_PATTERN.md:895` menyebutnya "skeleton + spinner". Jadi perubahan ini **menyimpang dari spesifikasi
yang ada**; spesifikasinya perlu ikut diperbarui (lihat langkah 8).

## Alasan

- Pengguna langsung melihat bentuk layar yang akan muncul, jadi terasa lebih cepat dan layar tidak melompat saat
  data datang.
- Baris abu-abu statis + spinner memberi dua tanda sekaligus; salah satu cukup.
- Tanpa animasi, baris abu-abu terlihat seperti layar macet.

## Aturan kapan memakai apa

| Situasi | Pilihan |
|---|---|
| Muat pertama daftar/kartu/detail, belum ada data | Skeleton |
| Aksi (login, kirim, unggah, simpan) | `AppLoadingDialog` atau spinner di tombol (tidak berubah) |
| Refresh daftar yang sudah tampil | Pull-to-refresh (tidak berubah) |
| Data sudah ada di cache Room | Tampilkan data lama; **tanpa** skeleton |
| Loading melewati batas waktu | `ErrorState` dengan tombol "Coba Lagi" |
| Login, Splash, layar isian | Tidak perlu skeleton |

Skeleton hanya muncul pada kondisi "belum pernah termuat dan sedang memuat" (pola yang sudah dipakai, misalnya
`!loaded && loading` di `JurnalScreen.kt:120` dan `!bills.loaded && bills.loading` di `SppScreens.kt:221`).

## Rancangan

### A. Primitif

- `SkeletonBlock`: kotak berwarna `MaterialTheme.colorScheme.surfaceContainerHigh`, bentuk dari token
  (`DiskolaExtraShapes.iconBox` untuk ikon, `DiskolaExtraShapes.chipBadge` untuk garis teks), ukuran dari parameter.
  Tidak ada warna, radius, atau ukuran hardcode baru.
- Shimmer: **satu** transisi tak-hingga yang dipakai bersama oleh semua kotak di satu layar (supaya kilau
  sinkron dan murah), digambar lewat `drawWithContent`/`graphicsLayer` agar tidak memicu recomposition per frame.
- Siklus shimmer perlu token durasi. `ui/theme/Motion.kt` baru punya `FAST` 150, `BASE` 250, `SLOW` 400 ms, jadi
  tambahkan satu konstanta (usulan sekitar 1200 ms) di sana, bukan angka di komponen.

### B. Varian bentuk

Satu bentuk generik tidak cukup: bentuk skeleton harus mirip isi aslinya. Usulan awal:

| Varian | Dipakai untuk |
|---|---|
| `ListRow` (kotak ikon + dua garis; bentuk yang ada sekarang) | Daftar tugas, jurnal, materi, notifikasi |
| `Card` (blok judul + nominal + garis status) | Tagihan SPP |
| `Detail` (judul besar + beberapa paragraf + blok lampiran) | Detail tugas, jurnal, materi |
| `HomeHeader` / kartu kelas | Home (tahap berikutnya, desain terpisah) |

API `LoadingState(modifier, rows)` **dipertahankan** dan ditambah parameter `variant` (bawaan `ListRow`), sehingga
10+ pemakai yang ada tidak wajib berubah untuk tahap pertama.

### C. Perilaku waktu

Satu helper untuk semua layar (mis. `rememberLoadingPhase(loading)`) dengan tiga fase:

1. **Tunda ~200 ms** sebelum skeleton muncul, supaya tidak berkedip kalau data datang cepat.
2. **Tampil minimal ~400 ms** begitu muncul, supaya tidak hilang-timbul.
3. **Batas waktu (usulan ~10 detik)**: pindah ke `ErrorState` dengan tombol "Coba Lagi". Jaringan sekolah bisa lambat dan
   skeleton yang terlalu lama terlihat macet.

Angka-angka di atas usulan; ditentukan saat uji di jaringan lambat.

### D. Aksesibilitas

- Spinner dan teks "Memuat…" **dihapus dari tampilan**; maknanya pindah ke semantics (keterangan "Memuat" +
  live region) sehingga TalkBack tetap membacakannya.
- Reduce-motion: kalau animasi sistem dimatikan, shimmer berhenti dan kotak tampil statis. (Compose tidak punya API
  khusus; pilihannya membaca skala animasi sistem. Cara pastinya diputuskan saat pengerjaan.)
- Skeleton bukan elemen interaktif dan tidak boleh bisa difokuskan.

## Layar yang disentuh

| Prioritas | Layar | Catatan |
|---|---|---|
| P1 | Jurnal, Tugas (detail), Materi (detail), SPP | Sudah memakai `LoadingState`; cukup ganti isi dan pilih varian |
| P2 | AKM (`AkmScreen`, `AkmDetailScreen`, `AkmScoreScreens`, `AkmResumeScreen`, `AkmQuestionScreen`) | Sudah memakai `LoadingState`. Layar ujian: pertahankan sederhana, jangan ganggu alur sesi ujian |
| P3 | Home, Notifikasi, Tugas siswa/guru/terkumpul, Materi (daftar), Poin, Presensi | Belum memakai `LoadingState`; perlu dicek satu per satu bagaimana loading-nya sekarang dan skeleton khusus |

Tidak diubah: `AppLoadingDialog` dan semua pemakainya, pull-to-refresh, Login, Splash, layar isian.

## Langkah pengerjaan

1. Tambah konstanta durasi shimmer di `ui/theme/Motion.kt`.
2. Buat `SkeletonBlock` dan mekanisme shimmer bersama; tambahkan `@Preview` (terang; gelap hanya jika dark mode masih ada).
3. Buat varian `ListRow` (setara bentuk sekarang), `Card`, `Detail`.
4. Ubah `LoadingState` memakai primitif baru: hapus spinner+teks dari tampilan, tambah semantics, tambah parameter `variant`.
5. Buat helper fase loading (tunda, tampil minimal, batas waktu) dan pasang di layar P1.
6. Pilih varian yang tepat di layar P1: Jurnal, Tugas detail, Materi detail, SPP.
7. Perluas ke P2 lalu P3. Tiap layar P3 dinilai dulu apakah skeleton memang cocok.
8. Perbarui dokumen: `ref/design_handoff_auth_fase1/README.md` (entri **LoadingState**) dan
   `docs/ANDROID_DESIGN_PATTERN.md` baris sekitar 895, supaya spesifikasi tidak lagi menyebut "skeleton + spinner".

Pisahkan menjadi commit per tahap (komponen, P1, P2, P3) agar mudah ditinjau dan dibatalkan.

## Verifikasi

- Batasi jaringan (throttle) atau pakai jaringan lambat: skeleton muncul setelah jeda singkat, shimmer berjalan, dan
  layar tidak melompat saat data datang.
- Jaringan cepat: tidak ada kedip skeleton.
- Mode pesawat: setelah batas waktu, `ErrorState` muncul dengan "Coba Lagi", dan tombolnya memuat ulang.
- Data ada di cache Room: tidak ada skeleton, data lama langsung tampil lalu diperbarui.
- Reduce-motion/animasi dimatikan: skeleton statis.
- TalkBack: membacakan "Memuat" sekali, tidak memfokuskan kotak skeleton.
- Layar kecil dan font diperbesar: bentuk skeleton tidak patah.
- Tes unit/UI yang sudah ada tetap lulus.

## Risiko

- **Menyimpang dari handoff desain.** Perlu persetujuan pemegang desain, dan dokumen spesifikasi harus diperbarui
  (langkah 8).
- **Skeleton yang bentuknya tidak mirip isi** malah menambah lompatan layout; karena itu varian per layar.
- **Beban animasi** kalau tiap kotak punya transisi sendiri; karena itu satu transisi bersama.
- **Layar ujian (AKM):** perubahan visual di tengah alur ujian harus minimal dan diuji terpisah.
- **Waktu tunda dan batas waktu** yang salah membuat layar terasa lambat atau menampilkan error terlalu cepat; diuji di
  jaringan nyata sekolah.

## Pertanyaan terbuka

1. Apakah penyimpangan dari handoff desain (spinner dan teks "Memuat…" dihapus) disetujui?
2. Batas waktu sebelum `ErrorState`: 10 detik cukup, atau lebih lama untuk jaringan sekolah?
3. Layar P3 mana yang benar-benar dimau pakai skeleton (Home paling kompleks karena bentuknya beragam)?
4. Lingkup tahap pertama: P1 saja, atau langsung P1 + P2?
