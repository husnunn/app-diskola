package id.app.education.dataclass.mock

/** Lifecycle of one assessment on this device, from "not synced" through to "scored". */
enum class AkmStatus(val label: String) {
    BelumSinkron("Belum sinkron"),
    MengunduhSoal("Mengunduh soal"),
    SiapDikerjakan("Siap dikerjakan"),
    SudahDikumpulkan("Sudah dikumpulkan"),
    MenungguPenilaian("Menunggu penilaian"),
    SudahDinilai("Sudah dinilai"),
}

data class AkmItem(
    val id: Int,
    val name: String,
    /** "Asesmen Sekolah" or "AKM Pemerintah". */
    val kind: String,
    val endLabel: String,
    /** Calendar day [endLabel] falls on — the date strip filters on this. */
    val endDay: Int,
    val questionCount: Int,
    val needsPassword: Boolean,
    /** Government AKM rather than a school-authored assessment; changes icon and CTA wording. */
    val government: Boolean,
    /** Whether the proctor has opened the session yet. */
    val started: Boolean,
)

data class AkmScoreItem(
    val id: Int,
    val name: String,
    val dateLabel: String,
    val score: Int,
    val status: AkmStatus,
    val hasExplanation: Boolean,
)

data class AkmInstruction(
    val id: Int,
    val name: String,
    val total: Int,
    /** null = answered count comes from the live question player rather than a fixed number. */
    val answered: Int?,
)

data class AkmExam(
    val id: Int,
    val name: String,
    val questionCount: Int,
    val instructions: List<AkmInstruction>,
)

enum class QuestionType(val label: String) {
    PilihanGanda("Pilihan ganda"),
    BenarSalah("Benar / salah"),
    MultiJawaban("Pilihan ganda multi jawaban"),
    EsaiSingkat("Esai singkat"),
}

data class AkmQuestion(
    val id: Int,
    val type: QuestionType,
    val text: String,
    val options: List<String> = emptyList(),
)

/** One question's review row on the Pembahasan screen. */
data class AkmExplanation(
    val number: Int,
    val question: String,
    val yourAnswer: String,
    val key: String,
    val rationale: String,
    val correct: Boolean,
    val hasImage: Boolean = false,
)

data class AkmSubtestScore(val label: String, val score: Int)

/** All mock data for Asesmen/AKM — no backend endpoint is wired for this feature yet. */
object MockAkm {

    val items = listOf(
        AkmItem(1, "Asesmen Tengah Semester — Informatika", "Asesmen Sekolah", "8 Sep 2026 · 09.30", 8, 40, needsPassword = true, government = false, started = true),
        AkmItem(2, "Asesmen Harian — Matematika", "Asesmen Sekolah", "8 Sep 2026 · 10.15", 8, 20, needsPassword = false, government = false, started = true),
        AkmItem(3, "AKM Literasi Membaca", "AKM Pemerintah", "8 Sep 2026 · 11.30", 8, 30, needsPassword = false, government = true, started = false),
        AkmItem(4, "AKM Numerasi", "AKM Pemerintah", "8 Sep 2026 · 13.00", 8, 30, needsPassword = false, government = true, started = true),
        AkmItem(5, "Asesmen Tengah Semester — Projek IPAS", "Asesmen Sekolah", "7 Sep 2026 · 09.30", 7, 35, needsPassword = false, government = false, started = true),
    )

    /** Per-assessment starting status, keyed by [AkmItem.id]. */
    val initialStatus: Map<Int, AkmStatus> = mapOf(
        1 to AkmStatus.BelumSinkron,
        2 to AkmStatus.SiapDikerjakan,
        3 to AkmStatus.SiapDikerjakan,
        4 to AkmStatus.SudahDikumpulkan,
        5 to AkmStatus.SudahDinilai,
    )

    val scores = listOf(
        AkmScoreItem(5, "Asesmen Tengah Semester — Projek IPAS", "7 Sep 2026", 88, AkmStatus.SudahDinilai, hasExplanation = true),
        AkmScoreItem(6, "Asesmen Harian — Informatika", "5 Sep 2026", 92, AkmStatus.SudahDinilai, hasExplanation = true),
        AkmScoreItem(7, "Asesmen Harian — PPKn", "2 Sep 2026", 0, AkmStatus.MenungguPenilaian, hasExplanation = false),
        AkmScoreItem(8, "AKM Literasi Membaca", "28 Agu 2026", 0, AkmStatus.SudahDikumpulkan, hasExplanation = false),
    )

    val exams = listOf(
        AkmExam(
            id = 1,
            name = "Informatika Paket A",
            questionCount = 25,
            instructions = listOf(
                AkmInstruction(1, "Instruksi 1 — Struktur Data", total = 5, answered = 5),
                AkmInstruction(2, "Instruksi 2 — Algoritma Pengurutan", total = 4, answered = null),
            ),
        ),
        AkmExam(
            id = 2,
            name = "Literasi Digital",
            questionCount = 15,
            instructions = listOf(
                AkmInstruction(3, "Instruksi 1 — Teks Informasi", total = 12, answered = 12),
            ),
        ),
    )

    val questions = listOf(
        AkmQuestion(
            id = 1,
            type = QuestionType.PilihanGanda,
            text = "Kompleksitas waktu terburuk pencarian linear pada array berisi n elemen adalah …",
            options = listOf("O(1)", "O(log n)", "O(n)", "O(n²)"),
        ),
        AkmQuestion(
            id = 3,
            type = QuestionType.MultiJawaban,
            text = "Pilih struktur data yang menyimpan elemen secara berurutan. (boleh lebih dari satu)",
            options = listOf("Array", "ArrayList", "HashMap", "LinkedList"),
        ),
        AkmQuestion(
            id = 2,
            type = QuestionType.BenarSalah,
            text = "Bubble sort selalu lebih cepat daripada selection sort pada data yang hampir terurut.",
            options = listOf("Benar", "Salah"),
        ),
        AkmQuestion(
            id = 4,
            type = QuestionType.EsaiSingkat,
            text = "Sebutkan nama algoritma pengurutan yang membandingkan pasangan elemen bersebelahan.",
        ),
    )

    val subtestScores = listOf(
        AkmSubtestScore("Struktur Data", 92),
        AkmSubtestScore("Algoritma Pengurutan", 84),
        AkmSubtestScore("Literasi Digital", 88),
    )

    val explanations = listOf(
        AkmExplanation(
            number = 1,
            question = "Kompleksitas waktu terburuk pencarian linear pada array berisi n elemen adalah …",
            yourAnswer = "C · O(n)",
            key = "C · O(n)",
            rationale = "Pencarian linear memeriksa elemen satu per satu. Pada kasus terburuk elemen yang dicari berada di posisi terakhir atau tidak ada, sehingga perlu n perbandingan — kompleksitasnya O(n).",
            correct = true,
        ),
        AkmExplanation(
            number = 2,
            question = "Bubble sort selalu lebih cepat daripada selection sort pada data yang hampir terurut.",
            yourAnswer = "Benar",
            key = "Salah",
            rationale = "Pernyataan terlalu umum. Bubble sort dengan optimasi berhenti dini memang cepat pada data hampir terurut, tetapi tanpa optimasi jumlah perbandingannya tetap sama dengan selection sort.",
            correct = false,
            hasImage = true,
        ),
    )

    /** Participant block on the detail screen — a real build reads this from the session. */
    val participantRows = listOf(
        "ID Ujian" to "ASM-2026-0091",
        "Nama" to "M. D. Muzakki",
        "NISN" to "0051234567",
        "Kelas" to "10 · Informatika",
        "Sekolah" to "SMK DEMO",
    )

    /** Days shown in the schedule strip, with their day-of-week labels. */
    val scheduleDays = listOf(
        4 to "Jum", 5 to "Sab", 6 to "Min", 7 to "Sen", 8 to "Sel",
        9 to "Rab", 10 to "Kam", 11 to "Jum", 12 to "Sab", 13 to "Min",
    )

    const val MONTH_LABEL = "September 2026"
    const val TODAY = 8
    const val EXAM_PASSWORD = "123456"
    const val PENALTY_LABEL = "00:24"
}
