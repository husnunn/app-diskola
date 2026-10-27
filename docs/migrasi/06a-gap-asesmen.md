# 06a — Gap Asesmen/AKM (ujian sekolah, siswa) app baru vs app lama

Audit 2026-10-14/15 (3 agen Explore + doc `06` + kode legacy `android-portal/.../pages/akm`); disesuaikan 2026-10-16 dengan doc `06` yang diperbarui ke **android-portal 2.1.41**
(`versionCode 80`, commit `05ec898b`: label/stempel jadwal dibatalkan §4.4, perilaku input esai §5.7). Lingkup: ujian sekolah saja (`exam-schedules*`), siswa saja;
Try Out dan ujian lama **tidak dibangun**; tak ada jalur AKM nasional (keputusan user, lihat `FLOW_QUESTIONS.md`). Path relatif `app/src/main/java/id/diskola/app/`.
Status: ✅ selesai di A1 · ⏭ A2 (lockdown & pelacakan) · ⛔ tidak dibangun · 🆕 fitur app lama yang muncul **setelah** audit ini (belum diaudit di app baru).

> **Diperbarui 2026-10-22:** doc `06` kini mengikuti **android-portal `e0a12b62`** (19-10-2026). Tiga baris 🆕 di tabel bawah mencatat apa yang baru di app lama sejak baseline `05ec898b`; status-nya sengaja "belum diaudit" karena laporan A1/A2 (`06b`, `06d`) lebih dulu dari perubahan itu.

## 1. Status per bagian doc 06

| Bagian | Sebelum A1 | Sesudah A1 | Sub-fase |
|---|---|---|---|
| §5.2 Daftar jadwal | API saja; offline = kosong; error ditelan | ✅ cache terakhir di Room, ujian yang masih punya jawaban belum terkirim selalu ikut (bagian "Perlu dikumpulkan"), banner pesan bila gagal | A1 |
| §5.3 Detail & gerbang password | status SUBMITTED tak dikenal, spinner tanpa akhir bila ujian tak di daftar, label "dikumpulkan" salah untuk Tidak Mengerjakan/Sesi Terlewat | ✅ cabang status benar, `EmptyState` "Asesmen tidak ditemukan"; ketentuan memuat butir yang benar-benar berlaku | A1 |
| §5.3 pesan error password per kebijakan backend (tabel 4 baris), cek online saat mulai, `exam_session_active` | sebagian/tidak ada | ⏭ | A2 |
| §5.4 Layar "Sebelum Memulai Ujian" | tidak ada | ⏭ (isinya daftar pelacakan pelanggaran) | A2 |
| §5.5 Resume + Kumpulkan | pop sebelum POST selesai; kegagalan tak terlihat; waktu habis ditampilkan walau gagal | ✅ alur "Sedang memproses jawaban" → "Berhasil"/"Gagal" (teks legacy), navigasi sesudah OK, dialog waktu habis satu tombol | A1 |
| §5.6 Pemutar soal 9 tipe | jawaban hanya di memori; urutan acak di memori | ✅ jawaban ke Room tiap interaksi (esai debounce 200 ms + flush), urutan acak disimpan sekali saat unduh, argumen route memulihkan layar setelah proses mati | A1 |
| §4.3 pemetaan tipe | STATEMENT 1 baris salah jadi B/S; tipe tak dikenal tak bisa dijawab | ✅ `showFalse` dulu; tipe tak dikenal → esai (fallback legacy) | A1 |
| §6 Unduh paket | worker mereset baris DOWNLOADED, mengacak ulang, menelan `CancellationException` | ✅ tak mengunduh ulang yang sudah ada; acak sekali; cancel dilempar ulang; jadwal deadline dibuat saat unduh selesai | A1 |
| §7.1 Autosave offline-first | tidak ada | ✅ `akm_answer` | A1 |
| §7.3 Submit manual (online/offline) | online saja, tanpa antrean | ✅ online langsung; offline → antre `AkmSubmitWorker` | A1 |
| §7.4 Auto-submit di batas waktu | hanya coroutine di `viewModelScope` | ✅ `akm_deadline_{id}` (KEEP) + `akm_submit_{id}`, klasifikasi gagal, retry ≤ 10, backoff linear 10 dtk | A1 |
| §7.5 Upload ulang + cooldown 60 dtk | tanpa cooldown, payload di memori | ✅ dari Room, cooldown per ujian, teks legacy | A1 |
| §15 Blokir logout | DOWNLOADED saja; FCM/401 yang terblokir diabaikan; event tak pernah dikirim | ✅ satu predikat (`AkmRules.isPending`), logout tertunda dijalankan otomatis setelah terkirim, dialog penjelasan | A1 |
| §8 Timer | auto-submit hanya bila layar ujian terbuka | ✅ deadline worker tak bergantung layar; layar menampilkan dialog bila waktu habis | A1 |
| §9 Lockdown/FLAG_SECURE/keep-screen-on | tidak ada (UI berpura-pura: chip "Mode ketat", ikon kunci, "Layar dikunci…") | pura-pura **dihapus**; fitur sebenarnya ⏭ | A2 |
| §10–§11 Pelanggaran & penalti | tidak ada ("00:24" palsu) | palsu **dihapus**; fitur ⏭ | A2 |
| §13 Laporkan masalah | tidak ada | ⏭ | A2 |
| §4.4 Label/stempel "Dibatalkan" (`watermark_bool`/`watermark_label`, 2.1.41) | tidak ada | ✅ label di kartu daftar; baris "Status" + stempel di Detail dan Hasil; watermark diagonal di halaman soal; merge "null = pertahankan nilai lokal" lewat cache; **banner juga di Resume** (deviasi, keputusan user 2026-10-16); layar priming ⏭ ikut menandai | A1 (priming: A2) |
| §5.7 Input esai (2.1.41): enter dibuang/tempelan diratakan, tombol "Selesai", kursor diikuti, min 8 baris | satu baris default, Enter menyisipkan baris baru, tanpa auto-scroll kursor | ✅ `AkmRules.sanitizeEssayInput`, `ImeAction.Done` (esai & satu kata; angka tetap keyboard angka), `AppTextField(followCursor, minLines = 8)` | A1 |
| 🆕 §5.8 Rumus LaTeX di teks soal, pilihan jawaban, pernyataan (`e0a12b62`, 19-10-2026) | — (fitur baru di app lama; versi lama menampilkan rumus kosong) | ✅ 2026-10-22 — `MathParser` + `HtmlMathRenderer` ditulis ulang (`AndroidGraphics2D`, ukuran piksel, cache, warna dioper, gagal parse → teks sumber, tanpa `LinkMovementMethod`), lebar dirender ulang setelah layout, aturan R8; salinan lama (rumus tampil kosong) diganti. **Belum teruji di perangkat** (`HtmlMathRendererTest` instrumented hanya dikompilasi) | `06d` §6 |
| 🆕 §5.6 Tombol bawah "Berikutnya" → "Selesai" di soal terakhir (`e0a12b62`) | — | ✅ 2026-10-22 — `AkmQuestionRules.nextButtonMode` (7 tes) + tombol kanan selalu di komposisi (`alpha(0f)` + nonaktif saat tersembunyi); "Selesai" ⇒ kembali ke ringkasan; `hasBreach` = proxy `recovery != null`; menu header "Selesai" tidak dibuat (panah setara). **Belum teruji di perangkat** | `06d` §6 |
| 🆕 §16 Header `X-App-Platform: android` + `X-App-Version` di semua request (`e0a12b62`) | — | ✅ 2026-10-22 — `RequestInterceptor.withAppHeaders` (6 tes JVM); nilai = `versionName` (kini `1.0.0`, lihat risiko di `06d` §6). **Belum dilihat di log perangkat** | `06d` §6 |
| §5.3 Label baris detail "Peserta" | "Nama" | ✅ ("Peserta"); Hasil kini punya tabel info yang sama (doc §14 "info seperti detail") | A1 |
| §14 Pembahasan (`AkmExplanationDownloader`) | daftar kartu per soal | tetap; ⏭ penyempurnaan | A2 |
| §19–§20 Try Out, ujian lama | scaffolding kosong | ⛔ | — |

## 2. Bug legacy yang sengaja tidak ditiru
Menghapus ujian yang sedang dikerjakan saat refresh daftar (dan salah kolom `delete … where id` pada `akm_exams`); penjadwalan ulang deadline yang membatalkan unggahan manual yang sedang jalan
(satu nama unik + REPLACE); menulis ulang baris penuh dari state basi (menurunkan SCORED → UPLOADED); POST + `delay(1 s)` di dalam transaksi Room; worker yang memposting ulang ujian berstatus UPLOADED
(`> UPLOADED` vs `>=`); semua error di-retry 10× termasuk 4xx permanen; dialog waktu habis dengan tombol "Batal" yang tetap bisa mengedit; `date_end` gagal parse → "sekarang" lalu kirim paksa;
perbandingan jam HH:mm di detail (salah lintas hari); `fallbackToDestructiveMigration` menghapus jawaban belum terkirim; notifikasi "\"Jawaban berhasil terkirim\"" dengan tanda kutip literal.
