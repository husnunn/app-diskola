# Dokumentasi Teknis — Logika Penalty & Mode Exam-Lock (AKM)

> Bagian dari seri dokumentasi arsitektur teknis fitur Asesmen, dibuat untuk membangun ulang repo
> baru (Hilt + Jetpack Compose + Room). Ini BUKAN dokumentasi UI (lihat `docs/features/asesmen-akm-*.md`
> untuk itu) — fokus di sini adalah MEKANISME KERJA sistem penalti (deteksi kecurangan) dan sistem
> screen-lock ("mode ketat"), berdasarkan pembacaan penuh `ExamLockdown.kt`, `CountDownService.kt`,
> dan seluruh pemakainya di `AkmTakeResumePage.kt` + `AkmQuestionsPage.kt`.
>
> **Catatan penting:** kedua Activity ini mengimplementasikan mekanisme yang KONSEPNYA identik tapi
> KODENYA diduplikasi penuh (bukan satu komponen bersama) — setiap penjelasan di bawah berlaku untuk
> keduanya kecuali disebutkan beda.

---

## 1. Dua sistem yang saling terpisah tapi saling memicu

Ada **dua mekanisme independen** yang sering disalahpahami sebagai satu:

| Sistem | Tujuan | Sumber saklar server | File utama |
|---|---|---|---|
| **Sistem Penalti** | Menghukum siswa yang keluar aplikasi/pindah app/kena overlay saat ujian, dengan menahan pengerjaan selama N menit | `penalty_applied` (Boolean) + `penalty_times` (Int, detik) dari `GET mobile/setting-akm` | `CountDownService.kt`, fungsi `PenaltyFromOverlay()`/`onRestart()`/`onWindowFocusChanged()` di kedua Activity |
| **Mode Exam-Lock ("mode ketat")** | Mengunci layar (Screen Pinning) supaya siswa tidak bisa membuka app lain sama sekali selama ujian | `exam_lock_mode` (Boolean?, default aman = aktif) dari `GET mobile/setting-akm` | `utils/ExamLockdown.kt` |

**Keduanya saling memicu satu sama lain**: kalau mode exam-lock aktif dan siswa berhasil lepas dari
screen pinning (`ExamLockdown.hasActiveBreach()==true`), itu **juga** dicatat sebagai pelanggaran yang
ditahan lewat `enforceLockdownAtCheckpoint()` — tapi INI PENTING: pada kode saat ini,
**pelanggaran lockdown TIDAK otomatis memicu sistem Penalti** (tidak memanggil
`PenaltyFromOverlay()`). Yang terjadi hanya dialog "Mode Terkunci Terlepas" (lihat §3.4) yang menahan
lewat `return false` di `enforceLockdownAtCheckpoint`, TANPA mencatat penalti/menjalankan
`CountDownService`. Sebaliknya, pelanggaran overlay/keluar-app (Penalti) **bisa terjadi independen**
dari status exam-lock — bahkan di sekolah yang `exam_lock_mode`-nya mati, sistem Penalti tetap
berjalan normal selama `penalty_applied==true`.

---

## 2. Sistem Penalti

### 2.1 Saklar & durasi

Dibaca sekali per `onResume`/`onRestart` (bukan real-time observer) dari tabel key-value
`akm_settings` (via `AkmSettings`, walau kedua Activity ini membaca langsung lewat
`viewmodel.db.akm().getSettingValue("penalty_applied")`, bukan lewat fungsi `AkmSettings.isPenaltyApplied()`
— ada duplikasi logika sentinel `-1`/`0`/`1` yang sama persis ditulis ulang di banyak tempat, bukan
dipanggil dari satu sumber):

```kotlin
val penaltyon = viewmodel.db.akm().getSettingValue("penalty_applied")
penaltyEnabled = if (penaltyon == -1) true else penaltyon == 1   // default aman = aktif
```

Durasi penalti (`penalty_times`, detik) dibaca terpisah lewat `viewmodel.db.akm().getPenaltyMinutes()`
(nama fungsi menyesatkan — isinya detik, bukan menit) di `startPenaltyMinutes()`:

```kotlin
val minutes = viewmodel.db.akm().getPenaltyMinutes()
val minutes = if (times > 0) times else 300   // default 300 detik (5 menit) bila server 0/tak terkirim
```

### 2.2 Titik pemicu (kapan penalti dijatuhkan)

Ada **4 titik pemicu independen**, semuanya bermuara ke fungsi `PenaltyFromOverlay()` (nama fungsi
warisan lama, sebenarnya dipakai untuk SEMUA jenis pelanggaran, bukan cuma overlay):

1. **Overlay window** (`dispatchTouchEvent`) — event sentuh datang dengan flag
   `MotionEvent.FLAG_WINDOW_IS_OBSCURED`/`FLAG_WINDOW_IS_PARTIALLY_OBSCURED` (Android API 29+ untuk
   partial), artinya ada window lain (overlay dari app lain) menutupi sebagian/seluruh layar ujian.
2. **Keluar & kembali ke aplikasi** (`onRestart()`) — Activity di-restart setelah sempat berhenti
   (siswa menekan Home/Recents lalu kembali). **Tidak menunggu konfirmasi apa pun** — begitu
   `onRestart` terpanggil dan tidak masuk daftar pengecualian, penalti langsung dijatuhkan.
3. **Kehilangan fokus jendela di Android 15+** (`onWindowFocusChanged`, hanya `SDK_INT >= 35`) —
   fallback karena flag obscured MotionEvent dilaporkan tidak selalu terpicu di Android 15 ke atas.
4. Lockdown breach — **saat ini TIDAK memicu penalti** (lihat catatan §1). Hanya menahan aksi lewat
   dialog terpisah.

### 2.3 Pengecualian (kapan TIDAK boleh menjatuhkan penalti)

Semua titik pemicu di atas dijaga guard yang sama (ditulis ulang di kedua Activity, urutan
pengecekan sedikit beda tapi substansinya sama) — penalti **tidak** dijatuhkan bila salah satu true:

- `isExplanation` — sedang di mode lihat pembahasan, bukan mengerjakan.
- `isPenaltyActive()` — sudah dalam masa penalti berjalan (`state_penalty_$akmId` atau
  `state_penalty_pending_$akmId` true di SharedPreferences).
- `isDialogShown` / `isModalVisible` — dialog lain (termasuk dialog penalti itu sendiri) sedang
  tampil.
- `navigatingInternally` — Activity sedang berpindah ke layar lain di dalam alur ujian sendiri
  (bukan keluar aplikasi).
- `whitelistedExit` (hanya `AkmQuestionsPage`) — siswa sengaja dikirim ke Setelan Jaringan lewat
  tombol "Aktifkan Data" pada video offline (lihat `docs/features/asesmen-akm-pengerjaan-soal.md` §4).
- `ExamLockdown.promptPending` — dialog sistem "Sematkan layar?" kemungkinan sedang tampil (window
  sistem ini juga terbaca sebagai overlay/kehilangan fokus, jadi harus dikecualikan).
- `ExamLockdown.isIntentionalExit()` — masih dalam jendela 10 detik setelah aplikasi sendiri yang
  melepas pin untuk keperluan sah.
- Dialog alert/loading milik BasePage (`alertDialog?.isShowing`/`loadingDialog?.isShowing`) sedang
  tampil.

### 2.4 Yang terjadi saat penalti dijatuhkan (`PenaltyFromOverlay()`)

1. Baca ulang `penalty_applied` dari Room (bisa jadi berbeda dari nilai yang dibaca saat
   `onResume`, karena guard ini dijalankan async lewat `lifecycleScope.launch`) — kalau ternyata
   `false`, batal (tidak ada penalti meski trigger sempat lolos guard lain).
2. Tampilkan pill/badge `binding.tagPenalty` (`View.VISIBLE`).
3. Catat waktu mulai penalti: `SharedPreferences["data_$akmId"] = "HH:mm:ss"` (format lokal, bukan
   epoch millis — lihat catatan risiko di §2.6).
4. Panggil `startPenaltyMinutes()` — baca durasi (`penalty_times` dari server, default 300 detik),
   simpan ke `SharedPreferences["hours_$akmId"]`, lalu `startService(CountDownService)` dengan extra
   `akm_id`.
5. Tampilkan `MaterialAlertDialogBuilder` (judul **"Penalti"** di `AkmQuestionsPage`, **"Penalti
   Ujian"** di `AkmTakeResumePage`, pesan "Terdeteksi keluar aplikasi (ujian). Mohon menunggu untuk
   dapat melanjutkan ujian") — dialog ini **tidak punya tombol** (murni informatif, hilang sendiri
   saat broadcast "selesai" diterima, bukan lewat dismiss manual).
6. `isModalVisible = true` selama dialog tampil (mencegah loop pemicu penalti berulang & membiarkan
   sentuhan pada dialog itu sendiri lewat `dispatchTouchEvent`).

### 2.5 `CountDownService` — mesin hitung mundur

Bukan Foreground Service (`Service` biasa, `START_STICKY`, tanpa `startForeground()`/notifikasi) —
berjalan selama proses aplikasi hidup dan Activity ujian ada di foreground.

- Dipicu ulang tiap `startService()` (bisa dipanggil berkali-kali dari titik pemicu berbeda; setiap
  kali membatalkan `Timer` lama (`mTimer?.cancel()`) dan membuat `Timer` baru yang berjalan tiap
  **1000ms** (`NOTIFY_INTERVAL`).
- Tiap tick: hitung selisih waktu SEKARANG (`HH:mm:ss` saat ini) dikurangi waktu MULAI penalti
  (`data_$akmId`), lalu sisa = `durasi_dikonfigurasi - selisih`.
  - Sisa > 0 → broadcast `time="mm:ss"` (lokal, action `id.diskola.app.pages.akm.countdownservice`,
    dibatasi ke package sendiri lewat `setPackage()`), set `state_penalty_$akmId=true`.
  - Sisa habis → broadcast `time=""`, set `state_penalty_$akmId=false`, `mTimer.cancel()`,
    `stopSelf()`.
- Kedua Activity mendaftarkan `BroadcastReceiver` untuk action ini selama `onResume`–`onPause`
  (`RECEIVER_NOT_EXPORTED` di Android 13+ demi keamanan — broadcast lokal ini bisa membatalkan
  penalti kalau bocor ke app lain). Saat menerima `data==""` → kembalikan semua UI ke kondisi normal
  (tombol Kumpulkan/Berikutnya muncul lagi, list soal terlihat lagi, toolbar judul & tombol back
  dikembalikan). Saat `data!=""` → sebaliknya, kunci semua itu dan tampilkan angka mundur di
  `tagPenalty`.

### 2.6 Risiko/keterbatasan yang perlu diperbaiki di repo baru

- **Format waktu mulai penalti disimpan sebagai string `HH:mm:ss` lokal**, bukan epoch millis
  (`System.currentTimeMillis()`). Ini rentan salah hitung kalau device melewati pergantian hari
  (23:59:59 → 00:00:01) selama masa penalti, dan tidak portable untuk time zone berbeda. **Di repo
  baru, simpan sebagai epoch millis UTC.**
- **`CountDownService` bukan Foreground Service** dan pakai `java.util.Timer` (bukan
  `WorkManager`/`Coroutine`) — berisiko dibekukan sistem di Android versi baru saat app pindah ke
  background sebentar. Ada draf implementasi Foreground Service dengan notifikasi yang SUDAH DITULIS
  tapi dikomentari total di bagian bawah file yang sama (`CountDownService.kt:137-275`) — draf ini
  memakai `endTime` epoch (`System.currentTimeMillis() + duration`) yang jauh lebih benar, tapi tidak
  pernah diaktifkan. **Jadikan draf itu sebagai titik awal, bukan implementasi aktif yang sekarang.**
- **Kalau proses aplikasi mati saat penalti berjalan** (mis. di-kill sistem), `CountDownService` ikut
  mati dan broadcast berhenti — tapi `SharedPreferences["state_penalty_$akmId"]` tetap `true`
  (stale, tidak pernah di-set `false` lagi karena tidak ada yang menjalankan tick berikutnya). Saat
  app dibuka lagi, `isPenaltyActive()` akan tetap membaca `true` dari flag basi ini tanpa ada service
  yang menghitung mundurnya — berpotensi membuat siswa TERKUNCI PERMANEN di state "sedang dipenalti"
  tanpa hitung mundur yang jalan. **Ini bug nyata untuk diperbaiki, bukan perilaku yang harus
  ditiru** — di repo baru, `onResume`/init awal harus mengevaluasi ulang sisa waktu dari epoch
  tersimpan dan me-restart timer/worker bila memang masih tersisa, atau membersihkan flag basi bila
  waktunya sudah lewat.
- **`penalty_applied`/`penalty_times` dibaca dari `PreferenceManager.getDefaultSharedPreferences`**
  untuk sebagian nilai (`mpref`/`mEditor` di kedua halaman ini), BUKAN dari jalur `PreferenceModule`
  resmi aplikasi — konsisten dengan temuan audit arsitektur sebelumnya bahwa modul AKM punya sumber
  SharedPreferences terpisah dari sisa aplikasi. **Di repo baru, satu sumber DataStore/Preferences
  saja untuk seluruh app.**

---

## 3. Mode Exam-Lock ("mode ketat") — `ExamLockdown`

Screen Pinning (`Activity.startLockTask()`) **tanpa Device Owner** — artinya ini adalah App Pinning
biasa (fitur bawaan Android untuk semua app), bukan Kiosk Mode tingkat perangkat. Konsekuensinya
disadari penuh oleh kode (lihat komentar kelas): panggilan telepon masuk, alarm, dan gestur
lepas-pin (tahan tombol Back+Overview) tetap bisa menembus keluar dari mode terkunci ini.

### 3.1 Kenapa didesain sebagai singleton in-memory (bukan Room/SharedPreferences)

Dua alasan eksplisit di kode:
1. **Lock task adalah properti TASK, bukan Activity** — sekali `startLockTask()` berhasil,
   perpindahan `AkmTakeResumePage` ⇄ `AkmQuestionsPage` (satu task yang sama) tetap terkunci tanpa
   panggilan tambahan. Menyimpan status per-Activity akan salah model.
2. **Sengaja tidak dipersistensi** — kalau proses mati lalu hidup lagi, yang benar adalah membaca
   ulang `ActivityManager.getLockTaskModeState()` (sumber kebenaran OS), bukan mempercayai flag lama
   yang bisa membuat aplikasi salah kira dirinya terkunci padahal tidak (atau sebaliknya).

**Rekomendasi untuk repo baru:** pertahankan pola ini (state proses-wide, bukan Room) — TAPI bungkus
sebagai singleton yang di-inject Hilt (`@Singleton class ExamLockdownManager @Inject constructor()`)
supaya bisa di-mock saat testing, bukan `object` Kotlin polos seperti sekarang.

### 3.2 Gerbang aktif/nonaktif tunggal

Seluruh mekanisme dikendalikan SATU flag: `exam_lock_mode` (Boolean?, dari `GET mobile/setting-akm`,
**default aman = true/aktif** kalau field ini null atau setting belum pernah berhasil diambil —
lihat `AkmSettings.isStrictMode()`). `ExamLockdown.arm(examId)` **hanya dipanggil kalau
`isStrictMode()==true`**, dipicu tepat sebelum masuk ke `AkmQuestionsPage`/saat `AkmTakeResumePage`
dibuka pertama kali untuk sesi itu. Kalau `false`, `arm()` tidak pernah jalan → `ExamLockdown.desired`
tetap `false` selamanya untuk sesi itu → SEMUA fungsi lain (`ensureLocked`, `consumeBreach`,
`hasActiveBreach`, `watchdogCheck`) langsung `return` di baris pertama tanpa efek apa pun. Desain
gerbang tunggal ini bagus untuk ditiru — satu flag menonaktifkan seluruh subsistem dengan bersih.

### 3.3 Siklus hidup satu sesi lockdown

```
arm(examId)                     — set desired=true, reset semua counter/state
   │
   ▼
requestLock(activity)           — panggil startLockTask() (posted ke next frame),
   │                               set promptPendingSinceMs (supaya dialog sistem "Sematkan
   │                               layar?" tidak salah kebaca sebagai overlay/breach)
   ▼
[async, hasil beda per OEM] ────┬── berhasil langsung terkunci
                                 └── tampil dialog sistem "Sematkan layar?" dulu
   │
   ▼
evaluateAfterFocusRegained()    — dipanggil dari onWindowFocusChanged(true), baca
   │                               ActivityManager.lockTaskModeState SEBENARNYA (bukan
   │                               percaya begitu saja tidak ada exception dari startLockTask)
   ├── pinned    → everPinned=true
   └── tidak     → declinedByUser=true (percobaan berikutnya dibatasi MAX_ATTEMPTS=2,
                    KECUALI sudah pernah berhasil terkunci sebelumnya di sesi ini)
   │
   ▼ (di setiap checkpoint: onResume, onWindowFocusChanged, tap layar, pindah soal, buka menu)
ensureLocked(activity)          — no-op kalau: !desired, declinedByUser, promptPending,
   │                               isIntentionalExit(), atau sudah pinned. Selain itu →
   │                               requestLock() lagi (retry otomatis, TANPA UI)
   ▼
consumeBreach()/hasActiveBreach() — deteksi lock task LEPAS tanpa sepengetahuan kita
   │                                 (siswa paksa lepas-pin, atau interupsi sistem/panggilan
   │                                 masuk). hasActiveBreach() dipakai untuk MENAHAN aksi
   │                                 (ganti soal, kumpulkan); consumeBreach() didebounce 3
   │                                 detik untuk LOG/dialog supaya tidak spam.
   ▼
release(activity, reason)       — WAJIB dipanggil di SETIAP jalur keluar (waktu habis, sudah
                                    ter-upload, halaman resume di-destroy, submit, batal) —
                                    tanpa ini siswa mendarat di daftar ujian dalam keadaan
                                    perangkat masih terkunci.
```

### 3.4 UI yang terhubung ke mekanisme ini (rujuk kembali ke dokumen UI untuk detail elemen)

- **Dialog "Ujian Akan Dikunci"** (`AkmTakeResumePage.showLockdownExplainer()`) — tampil SEKALI di
  awal sesi (`lockdownPromptShown` flag), sebelum lock task pertama kali diminta.
- **Dialog "Mode Terkunci Terlepas"** (`showLockdownRecoveryDialog()`, ada di KEDUA Activity secara
  terpisah) — tampil saat `enforceLockdownAtCheckpoint()` mendeteksi breach DAN tidak sedang
  penalti/dialog lain. Tombol "Aktifkan" → `retryLock()` → tunggu `VERIFY_DELAY_MS` (1200ms) →
  `verifyRequestedLock()` → sukses: tutup dialog; gagal: ubah pesan & aktifkan tombol lagi (siswa
  bisa coba berkali-kali tanpa batas dari dialog ini — `MAX_ATTEMPTS` hanya membatasi percobaan
  OTOMATIS lewat `ensureLocked`/`requestLock`, bukan retry manual lewat dialog).
- **Dialog "Butuh Koneksi Internet"** (`AkmDetailPage`) — gerbang terpisah SEBELUM ujian dimulai:
  kalau `isStrictMode()==true` dan `!Utils.isInternetValidated()`, ujian tidak boleh dimulai sama
  sekali. Ini murni pemeriksaan satu kali di awal, TIDAK memblokir lagi di tengah pengerjaan meski
  koneksi putus (disengaja — lihat komentar di `AkmDetailPage.kt:568-571`: menghindari sinyal
  naik-turun berulang kali menghentikan siswa).
- **Watchdog** (`watchdogCheck`, dipanggil tiap `onResume`) — pengaman terakhir: sesi yang "armed"
  lebih dari 6 jam (`MAX_SESSION_MS`) dianggap sesi terlupakan/error, kunci dilepas paksa.

### 3.5 Rekomendasi arsitektur untuk repo baru (Hilt + Compose)

- Pertahankan **satu manager singleton** untuk lock-task state (alasan §3.1 tetap valid), tapi jadikan
  `@Singleton` yang di-inject, dengan API yang sama persis secara konsep (`arm`/`ensureLocked`/
  `hasActiveBreach`/`release`) supaya gampang di-unit-test dengan fake `ActivityManager`.
  Expose state-nya sebagai `StateFlow<LockdownState>` (sealed: `Inactive`/`Armed`/`Pinned`/
  `Breached`/`Declined`) alih-alih variabel `var` mutable biasa, supaya Compose UI (banner/blocker)
  bisa `collectAsStateWithLifecycle()` reaktif alih-alih dipanggil imperatif dari tiap lifecycle
  callback seperti sekarang.
- Jangan gabungkan lagi logic sistem Penalti dan Exam-Lock jadi kode yang saling menembak imperatif
  di dalam Activity seperti sekarang — pertimbangkan keduanya sebagai use case terpisah yang
  masing-masing men-subscribe ke sinyal yang sama (lifecycle events, window-focus events) lewat satu
  titik observasi di ViewModel, bukan dua Activity yang duplikat penuh.
- Ganti representasi waktu penalti ke epoch millis (lihat §2.6), dan pindahkan `CountDownService` ke
  `CoroutineWorker`/`Foreground Service` yang benar (draf yang sudah ada di file yang sama, tinggal
  dirapikan) supaya tahan dibekukan sistem.
- Pertimbangkan eksplisit: apakah pelanggaran lockdown SEHARUSNYA memicu penalti juga (saat ini
  tidak, lihat §1) — ini keputusan produk, bukan bug teknis, tapi perlu diputuskan sadar untuk repo
  baru, bukan diwariskan diam-diam.
