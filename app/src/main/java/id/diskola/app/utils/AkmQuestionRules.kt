package id.diskola.app.utils

/** The role of the right-hand button under a question (doc 06 §5.6, android-portal `NextButtonMode.kt`). */
enum class NextButtonMode {
    /** "Berikutnya": go to the next question. */
    NEXT,

    /** "Selesai": on the last question, back to the exam lobby where "Kumpulkan" is. */
    FINISH,

    /** Not usable and not visible, but still in its place so the layout never shifts. */
    HIDDEN,
}

/** Pure rules of the question screen; no Android, every case has a JVM test. */
object AkmQuestionRules {

    /**
     * The role of the bottom button for the question at [position] of [questionCount]. "Selesai" on the last question has the same condition
     * as finishing from the header ([allowFinish] — every question of this instruction answered — and no penalty or lock breach), so the two
     * always agree. The explanation mode never shows "Selesai". (The old screen disabled "Berikutnya" on the last question instead.)
     */
    fun nextButtonMode(
        position: Int,
        questionCount: Int,
        penaltyActive: Boolean,
        isExplanation: Boolean,
        allowFinish: Boolean,
        hasBreach: Boolean,
    ): NextButtonMode = when {
        penaltyActive || questionCount == 0 -> NextButtonMode.HIDDEN
        position < questionCount - 1 -> NextButtonMode.NEXT
        isExplanation -> NextButtonMode.HIDDEN
        allowFinish && !hasBreach -> NextButtonMode.FINISH
        else -> NextButtonMode.HIDDEN
    }
}
