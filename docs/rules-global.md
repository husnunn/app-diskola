# Rules Global — Wajib Dipatuhi Setiap Agent yang Mengerjakan Diskola

> Dokumen ini berisi aturan kerja yang **wajib diikuti** oleh AI agent (Claude Code atau lainnya) saat
> mengerjakan kode Diskola — baik migrasi bertahap di repo ini (`android-portal`) maupun pembangunan
> ulang di repo baru (lihat `docs/REBUILD_PROMPT.md`). Aturan ini lahir dari kesalahan nyata yang
> sudah terjadi di project ini (termasuk yang sudah pernah diperbaiki, seperti fetch duplikat di
> commit "Ship Wave 1: fetch dedupe") — tujuannya supaya kesalahan yang sama **tidak terulang**.
>
> **Cara pakai:** tempel/rujuk file ini di `CLAUDE.md`/`AGENTS.md` repo manapun yang sedang dikerjakan.
> Kalau sebuah aturan di sini bertentangan dengan instruksi eksplisit dari user di sesi itu, instruksi
> user yang menang — tapi tanyakan dulu kalau terasa janggal, jangan diam-diam menyimpang.

---

## 1. Disiplin Pengambilan Data (Fetch Discipline)

### 1.1 Dilarang fetch ulang setiap halaman dibuka, kecuali data itu memang wajib real-time

**Aturan:** Saat sebuah layar dibuka (Activity/Fragment/Composable), JANGAN otomatis memanggil
endpoint jika data yang sama sudah tersedia secara lokal (Room/cache/state ViewModel yang masih
hidup). Fetch on-open hanya boleh untuk data yang **secara sifat wajib real-time**, misalnya:
- `check-account` / status login-sesi
- Saldo wallet (`klaspay balance`) / status aktivasi wallet
- Status transaksi yang sedang pending
- Data yang eksplisit diminta user lewat aksi (submit ujian, konfirmasi pembayaran)

Untuk SEMUA data lain (materi, tugas, jadwal, riwayat, poin, dst) — **muat dari lokal dulu (Room)**,
tampilkan langsung, baru refresh dari server di background KALAU cache sudah kedaluwarsa atau memang
belum pernah dimuat. **Pembaruan data manual harus lewat pull-to-refresh eksplisit** (`SwipeRefreshLayout`
Compose: `pullRefresh`), bukan lewat `LaunchedEffect(Unit)`/`onResume`/`init` yang fetch ulang tanpa
syarat setiap kali layar tampil kembali.

**Kenapa:** ini pola nyata yang menyebabkan bug fetch-ganda di aplikasi lama (lihat
`docs/REBUILD_PROMPT.md` Bagian 7 poin 1) — request yang sama terpicu dari `onCreate` *dan*
`onResume`, atau dari `init` ViewModel *dan* `init` Fragment sekaligus. Fetch berulang tanpa guard
membuang kuota data user, memperlambat layar, dan berisiko race condition antar response.

**Cara verifikasi sebelum bilang selesai:** buka Chucker/network log, buka-tutup-buka lagi layar yang
sama (termasuk lewat back-forward navigasi & rotasi layar), pastikan endpoint yang sama **tidak**
terpanggil lagi kecuali user memang menekan refresh atau cache benar-benar sudah invalid.

### 1.2 Dilarang memanggil endpoint yang sama dua kali dalam satu siklus (deduplikasi request)

**Aturan:** Sebelum menambah pemanggilan API baru, cek apakah data yang sama sudah/akan diambil oleh
proses lain di layar yang sama (mis. ViewModel `init` DAN `LaunchedEffect` composable-nya, atau
parent screen DAN child screen sama-sama fetch data yang sama). Pola aman:
- Satu ViewModel = satu titik pemicu fetch per jenis data (idealnya di `init` atau fungsi `load()`
  eksplisit yang dipanggil sekali).
- Kalau ada kemungkinan fetch dipicu dua sumber (mis. observer LiveData/Flow *dan* pemanggilan
  manual), pasang guard `if (isLoading) return` — **dan set flag ini SEBELUM `launch{}` dimulai**,
  bukan di dalam coroutine setelah baris pertama (lihat 1.3, ini bug klasik race condition).
- Untuk request paralel yang secara sengaja perlu dipanggil bareng (mis. gabungan beberapa kartu di
  dashboard), gunakan `coroutineScope { awaitAll(...) }` atau `combine()` Flow, JANGAN memicu masing-
  masing lewat efek terpisah yang bisa retrigger independen satu sama lain.

**Cara verifikasi:** grep semua pemanggil satu endpoint (`grep -rn "namaFungsiApi("`) sebelum
menambah pemanggilan baru — kalau ternyata sudah dipanggil di tempat lain untuk konteks yang sama,
reuse hasil itu (lewat state/Flow yang di-share), jangan panggil lagi.

### 1.3 Guard race condition: set flag SEBELUM `launch`, bukan di dalamnya

```kotlin
// ❌ SALAH — flag baru di-set setelah coroutine mulai jalan, jendela race tetap terbuka
fun load() {
    viewModelScope.launch {
        if (isLoading) return@launch
        isLoading = true
        ...
    }
}

// ✅ BENAR — flag di-set di thread pemanggil, sebelum launch, menutup jendela race sepenuhnya
fun load() {
    if (isLoading) return
    isLoading = true
    viewModelScope.launch {
        try { ... } finally { isLoading = false }
    }
}
```

---

## 2. Alur di Repo Baru Harus Mirip Repo Lama — Kecuali Diminta Berubah

**Aturan:** Urutan layar, kondisi percabangan, nama menu, teks tombol, aturan validasi, dan perilaku
error di repo baru **wajib identik** dengan aplikasi lama, kecuali user secara eksplisit meminta
perubahan perilaku. Ini termasuk:
- Urutan navigasi (mis. Splash → Onboarding → Login NISN → Password → Home, lihat
  `docs/REBUILD_PROMPT.md` Bagian 5).
- Kondisi kapan sebuah tombol/menu muncul atau terkunci (mis. gating `klaspayActive`,
  `is_student`/`is_teacher`, status asesmen — lihat `docs/features/ASESMEN.md` dan
  `docs/PAGE_UI_INVENTORY.md`).
- Pesan dan teks yang user-facing (label tombol, pesan error, judul dialog) — jangan
  diparafrase/"dirapikan" tanpa diminta, karena bisa mengubah makna atau familiar-tidaknya bagi user
  lama.

**Kalau menemukan alur lama yang terasa aneh/buggy** (misalnya kontradiksi kondisi
`enabled`/`visibility` yang sudah dicatat di `docs/features/asesmen-tryout-ujian.md` bagian
NilaiujianPage, atau bug `isSchoolScope` yang dicatat di `asesmen-akm-daftar-detail-nilai.md`):
**JANGAN diam-diam "diperbaiki"**. Tulis catatannya di `docs/FLOW_QUESTIONS.md` (buat kalau belum ada)
dan tanyakan ke user dulu sebelum mengambil keputusan sepihak untuk mengubah perilaku.

**Sebelum mengasumsikan bentuk UI atau field data**, cek dulu dokumentasi yang sudah ada di repo ini:
- `docs/PAGE_UI_INVENTORY.md` — elemen UI tiap halaman aktif
- `docs/api/` — kontrak request/response tiap endpoint (355 endpoint)
- `docs/features/` — dokumentasi mendalam per fitur (key data persis, termasuk mana yang hardcoded/
  lokal/dihitung)
- `docs/BACKGROUND_STYLE_GUIDE.md` — bahasa desain background

Jangan menebak/mengarang elemen atau field yang tidak tercantum di dokumen-dokumen itu. Kalau
dokumentasi untuk area yang sedang dikerjakan belum ada, buat riset serupa dulu (baca kode binding
asli, bukan menebak dari nama variabel) sebelum menulis desain/kode baru.

---

## 3. Anti-Pattern Teknis yang Sudah Terbukti Bermasalah (jangan diulang)

Ringkasan dari audit arsitektur & kode yang sudah dilakukan terhadap aplikasi lama — daftar lengkap
dengan bukti file:baris ada di riwayat sesi/PR terkait, di sini cukup daftar aturan larangannya:

| # | Larangan | Kenapa |
|---|---|---|
| 1 | `GlobalScope.launch` di Service/Worker/Socket | Lepas dari lifecycle komponen, job bisa lanjut jalan setelah komponen berhenti. Pakai `viewModelScope`/`lifecycleScope`/scope milik Worker sendiri. |
| 2 | Bitmap besar (>500KB) sebagai background full-screen | Ditemukan bitmap 8.8MB didekode di tiap layar. Pakai vector/Canvas gradient theme-aware (lihat `docs/BACKGROUND_STYLE_GUIDE.md` Pola A sebagai contoh yang benar). |
| 3 | Dua Room `@Database` menunjuk ke file fisik yang sama | Ditemukan `MemoryDB`(v42) & `PersistentDB`(v6) berbagi file `diskola.db` — risiko `fallbackToDestructiveMigration` menghapus data. Satu file = satu `@Database`. |
| 4 | Dua library serialization JSON sekaligus (Moshi + Gson) | Pilih satu (repo baru: `kotlinx.serialization`, lihat REBUILD_PROMPT Bagian 6). |
| 5 | `@Body data: Any` / `Map<String, Any>` (request tanpa tipe) | Bikin kontrak API tidak eksplisit dan rawan salah kirim field. Selalu definisikan data class request. |
| 6 | Business logic (network call, kalkulasi, aturan blokir) di Activity/Fragment/Composable | Harus di ViewModel/Repository. UI hanya render state & teruskan event. |
| 7 | Satu Dagger/DI component raksasa yang mendaftar puluhan ViewModel manual | Pakai Hilt (`@HiltViewModel`, auto-discovery) di repo baru. |
| 8 | God ViewModel/Util yang menangani banyak domain sekaligus (ditemukan `PresensiViewModel` 1235 baris, `IntentUtil` 901 baris) | Pecah per tanggung jawab sebelum/selagi migrasi, jangan porting apa adanya. |
| 9 | Nama class yang sama dipakai untuk bentuk data berbeda di package berbeda (ditemukan `MerchantItem`, `UserResponse`) | Rawan tertukar sumber data. Beri nama unik & jelas domainnya. |
| 10 | Elemen UI yang teksnya statis tapi terlihat seperti data dinamis (banyak ditemukan di area Asesmen — lihat `docs/features/ASESMEN.md` notasi ⚠️ HARDCODED) | Kalau memang perlu jadi dinamis, itu artinya field API baru harus diminta ke backend — jangan berpura-pura sudah ada. |
| 11 | Chrome/background dibangun ulang di tiap layar (bukan dibungkus sekali di luar NavHost) | Ikuti pola `AuthScaffold` (dibungkus sekali di luar `NavHost`, layar hanya isi konten) — hemat render & konsisten. |

---

## 4. Checklist Sebelum Agent Bilang "Selesai"

Sebelum melaporkan sebuah task selesai, pastikan:

- [ ] Tidak ada endpoint yang terpanggil berulang tanpa perlu (cek via network log/Chucker, sesuai §1.1–1.2).
- [ ] Alur & teks yang dibuat cocok dengan dokumentasi (`docs/PAGE_UI_INVENTORY.md`/`docs/api/`/
      `docs/features/`), bukan hasil tebakan.
- [ ] Tidak ada `GlobalScope`, business logic di UI, atau request tanpa tipe (`Body: Any`) yang baru
      ditambahkan (§3).
- [ ] Kalau ada penyimpangan sengaja dari alur lama, sudah dicatat di `docs/FLOW_QUESTIONS.md` dan
      dikonfirmasi ke user — bukan diputuskan sepihak.
- [ ] Build/lint relevan sudah dijalankan (`./gradlew :app:assembleDebug`, `detekt`/`ktlint` bila ada).
- [ ] State loading/empty/error layar sudah ada (bukan hanya jalur sukses).
- [ ] Kode baru sudah dicek kompatibilitas `minSdk 27` (§5.1) — bukan cuma jalan di emulator/HP terbaru.
- [ ] Tidak ada file/fungsi yang menangani banyak hal tak berkaitan sekaligus (§5.2 — pecah dulu).

---

## 5. Kompatibilitas Android 8.1 (API 27) & Struktur Kode Bersih

> Ditambahkan 30-09-2026 atas instruksi eksplisit user, berlaku untuk seluruh pekerjaan migrasi di
> `Diskola-App-New` mulai saat ini.

### 5.1 `minSdk 27` wajib benar-benar berfungsi, bukan cuma lolos compile

**Aturan:** `minSdk = 27` (Android 8.1) sudah dipasang di `app/build.gradle.kts` dan **wajib tetap
didukung penuh secara fungsional** — bukan hanya lolos compile/lint minSdk. Kejadian nyata yang sudah
terjadi di project ini: sebuah perubahan berjalan mulus di Android versi baru tetapi **gagal/tidak
berfungsi di Android 8.1**. Jangan sampai terulang. Sebelum menulis kode yang menyentuh API level
tertentu (permission runtime, Credential Manager, predictive back, splash screen API, kamera/lokasi,
notification channel, dsb.):

- Cek level API minimum fitur/kelas tersebut (dokumentasi Android resmi / anotasi `@RequiresApi` /
  lint Android Studio) — **jangan asumsikan tersedia di API 27** hanya karena tersedia atau lazim
  dipakai di API terbaru.
- Kalau API itu baru tersedia di level lebih tinggi, bungkus dengan
  `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.xxx)` dan sediakan **jalur fallback yang benar-
  benar berfungsi** di API 27 — bukan cabang else yang diam-diam no-op atau mengembalikan hasil kosong.
- Cek juga `minSdk` setiap library baru yang ditambahkan (Credential Manager, CameraX, Accompanist,
  dst.) supaya tidak menaikkan `minSdk` project tanpa disadari.
- Sebelum melaporkan sebuah fitur selesai, verifikasi (emulator API 27, atau minimal baca changelog
  API 28–36 untuk setiap API yang dipakai) — jangan hanya mengandalkan perangkat/emulator versi baru
  yang biasa dipakai sehari-hari.

### 5.2 Tidak boleh ada "spaghetti code" — struktur wajib rapi & mudah dikembangkan

**Aturan:** Ikuti pemisahan lapisan yang sudah digariskan (`docs/repo lama/REBUILD_PROMPT.md` Bagian
3): `UI (Composable stateless)` → `ViewModel (StateFlow<UiState>)` → `Repository` →
`RemoteDataSource`/`LocalDataSource`. Konkret:

- Satu file/class = satu tanggung jawab jelas (lihat §3 poin 8: God ViewModel/Util dilarang).
- Jangan menaruh logika bisnis, pemanggilan API, atau parsing di Composable — itu semua milik
  ViewModel/Repository (§3 poin 6).
- Nama yang jelas & konsisten; hindari nama sama untuk bentuk data yang berbeda (§3 poin 9).
- Kalau logika yang sama dibutuhkan di ≥2 tempat, ekstrak ke fungsi/komponen bersama
  (`:core:common`/`:core:ui`/`:core:designsystem`) — jangan disalin-tempel.
- Sebelum menambah kode baru, grep dulu apakah komponen/util yang setara sudah ada — jangan membuat
  implementasi paralel untuk hal yang sama.

**Cara verifikasi:** sebelum melapor selesai, cek ulang: kalau satu file/fungsi mulai menangani banyak
hal yang tidak berkaitan, atau state yang sama tersebar di banyak tempat tanpa satu sumber kebenaran
(`StateFlow`/`UiState` tunggal per layar), pecah dulu sebelum lanjut.
