# Dokumentasi Fitur Asesmen — Alur, UI, dan Key Data (untuk AI Design Generator)

> **Baca dokumen ini dulu sebelum membuka 3 file detail di bawah.** Fitur "Asesmen" di aplikasi
> Diskola sebenarnya adalah GABUNGAN tiga sistem berbeda yang berbagi sebagian besar komponen UI:
> **AKM** (asesmen sekolah harian), **Try Out** (simulasi tes masuk PTN), dan **Ujian** (sistem lama,
> kini legacy). Dokumen ini menjelaskan bagaimana ketiganya nyambung, notasi yang dipakai di semua
> file detail, dan daftar peringatan paling kritis — supaya AI design generator **tidak mengarang
> key/elemen yang tidak ada di kode asli**.

## Dokumen detail (sumber kebenaran per elemen)

Setiap elemen UI di tiga file ini sudah diverifikasi langsung ke kode binding asli (bukan tebakan),
dengan kutipan file:baris:

1. **[asesmen-akm-daftar-detail-nilai.md](asesmen-akm-daftar-detail-nilai.md)** — `AkmPage` (host),
   `AkmListPage` (daftar jadwal), `AkmDetailPage` (detail sebelum mulai + dialog password/konfirmasi),
   `AkmScorePage` (daftar nilai), `AkmScoreDetailPage` (detail nilai + ring skor), `AkmExplanationPage`
   (pembahasan — ternyata hanya dipakai jalur Try Out, lihat Peringatan #3 di bawah).
2. **[asesmen-akm-pengerjaan-soal.md](asesmen-akm-pengerjaan-soal.md)** — `AkmTakeResumePage` (daftar
   mapel & tombol kumpulkan), `AkmQuestionsPage` (chrome kerjakan soal), **6 tipe soal** (pilihan
   ganda, esai, benar/salah, tabel pernyataan, menjodohkan teks, menjodohkan gambar) masing-masing
   didokumentasikan terpisah karena field beda-beda, media soal (audio/video), dialog pilih nomor
   soal, dialog instruksi, dialog penalti/lockdown, dan payload jawaban yang dikirim ke server.
3. **[asesmen-tryout-ujian.md](asesmen-tryout-ujian.md)** — `TryOutPage`/`TryOutSchedulePage`/
   `TryOutScorePage`/`TryOutPassingGradePage`/`ListUnivPage` (Try Out), dan `UjianPage`/`ListUjianPage`/
   `PrepareUjianPage`/`TakeUjianPage`/`NilaiujianPage`/`UjianDetailPage` (Ujian legacy), plus
   percabangan kondisi khusus Try Out di dalam `AkmDetailPage`/`AkmScoreDetailPage`.

---

## Notasi yang dipakai konsisten di ketiga file (WAJIB dipahami)

| Notasi | Arti | Konsekuensi untuk desain |
|---|---|---|
| **Key data biasa** (mis. `data.schedule.name`) | Field ini benar-benar dibaca dari response API/entity lokal, dibuktikan dengan kutipan kode | Boleh dianggap sebagai data dinamis nyata |
| **⚠️ HARDCODED** | Teks/ikon ini adalah string atau drawable literal di kode — TIDAK berasal dari data apa pun | Jangan mendesain seolah ini bisa berubah per-item/per-user; kalau desain butuh variasi di sini, itu berarti FITUR BARU yang belum ada di backend |
| **⚠️ LOKAL** | Nilai ini murni state aplikasi/perangkat (hasil hitungan lokal, timer, state UI, file di device) — bukan dari API | Boleh didesain sebagai elemen dinamis, tapi jangan menganggapnya sebagai field yang bisa diminta ke backend |
| **DIHITUNG** | Teks ini hasil transformasi dari satu/lebih field API mentah (gabungan string, format tanggal, dsb.) — field mentahnya disebutkan di sebelahnya | Desain boleh mengikuti hasil akhirnya, tapi kalau butuh field mentah terpisah, cek dulu apakah field itu memang ada |
| Tabel "Kamus Field" di akhir tiap file | Daftar SEMUA field yang tersedia dari tiap data class API — **satu-satunya rujukan** field apa saja yang boleh dipakai | Jangan menambah field yang tidak ada di tabel ini |

---

## Peta alur & arsitektur data (ringkas)

```
HomePage / Pembelajaran ──menu "Asesmen"──▶ AkmPage (host, tab Jadwal/Nilai)
                                              │
                         ┌────────────────────┼────────────────────┐
                         ▼                    ▼                    │
                   AkmListPage           AkmScorePage               │
                   (jadwal)              (riwayat nilai)            │
                         │                    │                     │
                         ▼                    ▼                     │
                   AkmDetailPage ◀──────── (tap "Lihat Ujian") ─────┘
                   (detail + password gate)
                         │ status DOWNLOADED & waktunya
                         ▼
                   AkmTakeResumePage (daftar mapel/instruksi)
                         │ tap instruksi
                         ▼
                   AkmQuestionsPage (kerjakan soal, per tipe soal)
                         │ Kumpulkan
                         ▼
                   kembali ke AkmListPage → tab Nilai (AkmScorePage)
                         │ tap "Lihat Ujian" (status SCORED)
                         ▼
                   AkmScoreDetailPage (ring skor)
                         │ status EXPLAINED → tombol "Lihat Pembahasan"
                         ▼
                   AkmTakeResumePage (mode isExplanation=true) — BUKAN AkmExplanationPage
```

**Try Out** memakai `AkmDetailPage`/`AkmTakeResumePage`/`AkmQuestionsPage`/`AkmScoreDetailPage` yang
SAMA (lewat parameter `examType=TRYOUT`/`isTryout=true`), hanya beda judul toolbar & beberapa tombol
tambahan ("Cek Passing Grade"). Satu-satunya halaman unik Try Out: `TryOutPage` (host),
`TryOutSchedulePage`, `TryOutScorePage`, `TryOutPassingGradePage`+`Form`, `ListUnivPage`. **Khusus
Try Out**, tombol "Lihat Pembahasan" mengarah ke `AkmExplanationPage` (bukan `AkmTakeResumePage`).

**Ujian** adalah sistem terpisah total (entity Room sendiri: `ExamTable`/`QuestionTable`/
`AnswerTable`), dengan halaman sendiri: `UjianPage` → `ListUjianPage` → `PrepareUjianPage` →
`TakeUjianPage` → kembali ke `NilaiujianPage` → `UjianDetailPage`. **Fitur ini legacy** — entry
menu utamanya sudah dimatikan, hanya dijangkau lewat notifikasi tugas/ujian atau auto-submit sistem.

---

## 10 Peringatan Paling Kritis (baca sebelum mendesain apa pun)

Ini adalah temuan yang paling mungkin membuat desain baru salah kalau tidak dibaca — dikumpulkan dari
ketiga riset:

1. **Field API mentah (`ListAkmData`, dsb) TIDAK pernah dipakai langsung oleh UI.** Semua diproses
   dulu jadi entity Room (`AkmTable`, `ExamTable`, dst) oleh `OnKlasDbUtil.kt`. Kolom seperti `name`,
   `type`, `date_label` di entity itu adalah HASIL KOMPUTASI, bukan field API 1:1 — rinciannya ada di
   tabel tiap file.
2. **"Peserta", "NIS", "Kelas", "Sekolah" di panel info AKM BUKAN dari response API Asesmen** —
   diambil dari data sesi login (`StudentItem`/`SekolahItem`) yang tersimpan lokal. Jangan
   menganggapnya bagian dari payload asesmen saat merancang API baru.
3. **`AkmExplanationPage` (pembahasan) hanya benar-benar dipakai untuk jalur Try Out.** Untuk AKM/
   Ujian Sekolah biasa, tombol "Lihat Pembahasan" membuka `AkmTakeResumePage` (mode pembahasan),
   BUKAN `AkmExplanationPage` — meski nama class-nya mengandung "Akm".
4. **Beberapa elemen dirender tapi TIDAK PERNAH terlihat pengguna** karena `visibility="gone"`
   permanen tanpa kode yang mengubahnya: badge "Nilai : n" di kartu pembahasan Try Out, label kota di
   kartu universitas, teks hasil kalkulasi inline di form Passing Grade (hasil sebenarnya muncul lewat
   dialog popup terpisah). **Jangan jadikan elemen-elemen ini acuan desain.**
5. **3 layout jawaban soal tidak pernah dipakai (dead code)**: `akm_answer_choice_image.xml`,
   `akm_answer_pair_1.xml`/`_2.xml`, `akm_answer_pair_image_2.xml`. Jangan jadikan acuan gaya kartu
   jawaban "gambar saja tanpa label huruf" kecuali dikonfirmasi ulang ke tim engineering.
6. **Catatan warna di dialog pilih nomor soal SALAH** — teks bilang "terjawab = hijau", tapi warna
   yang benar-benar dipakai adalah BIRU. Pakai warna biru/abu asli, bukan teks catatannya.
7. **Sistem penalti & lockdown (screen-pinning) dikontrol 2 saklar server terpisah**:
   `penalty_applied` (nyala/mati seluruh sistem penalti) dan `exam_lock_mode` (nyala/mati mode kunci
   layar ketat). Kalau salah satu `false`, seluruh dialog/pill terkait di §7 file kedua tidak pernah
   muncul — jangan desain seolah selalu aktif.
8. **Judul toolbar `TakeUjianPage` (Ujian legacy) selalu literal "Asesmen"** — nama ujian sesungguhnya
   tampil sebagai subjudul kecil, dan subjudul ini KOSONG saat mode "Lihat Jawaban".
9. **Nilai Try Out ditampilkan desimal apa adanya**, sedangkan nilai AKM/Ujian sekolah dibulatkan —
   meski keduanya memakai komponen ring skor yang sama persis (`AkmScoreDetailPage`).
10. **`UjianPage` (Ujian legacy) dan `TryOutPage`** entry point-nya tidak lagi/tidak ditemukan lewat
    menu utama — Ujian hanya lewat notifikasi, dan entry awal Try Out tidak ditemukan sama sekali
    dalam pencarian kode (perlu konfirmasi manual ke tim sebelum mendesain "cara pertama kali sampai
    ke fitur ini").

---

## Cara pakai untuk brief ke Claude Design

1. Tentukan dulu halaman mana yang mau didesain — buka file detail sesuai (AKM daftar/detail/nilai,
   AKM pengerjaan soal, atau Try Out/Ujian).
2. Salin tabel "Elemen UI" halaman tersebut apa adanya ke prompt/context desain — jangan diringkas
   sampai kehilangan kolom "Key data" dan tanda ⚠️, karena itu yang mencegah AI menambah field baru.
3. Sebelum finalisasi, cocokkan lagi ke bagian "10 Peringatan Paling Kritis" di atas — pastikan tidak
   ada satupun yang dilanggar oleh hasil desain.
4. Kalau desain butuh data yang ternyata tidak ada di "Kamus Field" manapun, catat sebagai
   **kebutuhan field API baru** — bukan diasumsikan sudah tersedia.
