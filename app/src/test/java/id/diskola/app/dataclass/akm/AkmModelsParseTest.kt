package id.diskola.app.dataclass.akm

import id.diskola.app.dataclass.ResponData.AkmScheduleData
import id.diskola.app.dataclass.ResponData.AkmScheduleResponse
import id.diskola.app.dataclass.ResponData.AkmSettingResponse
import id.diskola.app.di.module.MoshiModule
import id.diskola.app.utils.AkmRules
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AkmModelsParseTest {

    private val moshi = MoshiModule.instance()
    private lateinit var previousZone: TimeZone

    @Before
    fun fixZone() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Jakarta"))
    }

    @After
    fun restoreZone() = TimeZone.setDefault(previousZone)

    private inline fun <reified T> parse(json: String): T = moshi.adapter(T::class.java).fromJson(json)!!

    // ---- settings ----

    @Test
    fun `a missing exam_lock_mode means strict mode is off`() {
        assertEquals(false, parse<AkmSettingResponse>("""{"data":{"penalty_times":5}}""").data!!.exam_lock_mode)
        assertEquals(true, parse<AkmSettingResponse>("""{"data":{"exam_lock_mode":true}}""").data!!.exam_lock_mode)
    }

    @Test
    fun `an explicit null stays null and the lockdown reads it as strict, like legacy`() {
        val lockMode = parse<AkmSettingResponse>("""{"data":{"exam_lock_mode":null}}""").data!!.exam_lock_mode
        assertNull(lockMode)
        assertTrue(AkmRules.isStrict(AkmRules.lockModeRaw(lockMode)))
        assertFalse(AkmRules.isStrict(AkmRules.lockModeRaw(parse<AkmSettingResponse>("""{"data":{}}""").data!!.exam_lock_mode)))
    }

    @Test
    fun `penalty_applied is off when the key is missing and on for an explicit null, like legacy`() {
        val missing = parse<AkmSettingResponse>("""{"data":{"penalty_times":60}}""").data!!
        assertEquals(false, missing.penalty_applied)
        assertFalse(AkmRules.isPenaltyApplied(AkmRules.penaltyAppliedRaw(missing.penalty_applied)))
        assertEquals(60, AkmRules.penaltySeconds(missing.penalty_times))

        val nulled = parse<AkmSettingResponse>("""{"data":{"penalty_applied":null}}""").data!!
        assertNull(nulled.penalty_applied)
        assertTrue(AkmRules.isPenaltyApplied(AkmRules.penaltyAppliedRaw(nulled.penalty_applied)))

        assertEquals(true, parse<AkmSettingResponse>("""{"data":{"penalty_applied":true}}""").data!!.penalty_applied)
    }

    // ---- explanation flag ----

    @Test
    fun `either spelling of the explanation flag is read and the newer wins`() {
        assertEquals(true, parse<AkmScheduleData>("""{"id":1,"has_explain":true}""").hasExplanation)
        assertEquals(true, parse<AkmScheduleData>("""{"id":1,"has_explanation":true}""").hasExplanation)
        assertEquals(false, parse<AkmScheduleData>("""{"id":1,"has_explain":true,"has_explanation":false}""").hasExplanation)
        assertNull(parse<AkmScheduleData>("""{"id":1}""").hasExplanation)
    }

    // ---- lists ----

    @Test
    fun `a single exam object where a list is expected is read as a one element list`() {
        val response = parse<AkmScheduleResponse>("""{"data":[{"id":1,"exams":{"id":5,"name":"Matematika"}}]}""")
        assertEquals(listOf("Matematika"), response.data.single().exams!!.map { it.name })
    }

    @Test
    fun `a schedule with exams survives being stored and read back`() {
        val original = parse<AkmScheduleData>(
            """{"id":9,"date":"2026-04-14","startAt":"08:00","endAt":"10:00","requires_password":true,
               "exams":[{"id":5,"name":"Matematika","numberOfQuestions":2,
                 "instructions":[{"id":7,"isRandom":true,"questions":[{"id":1,"answerType":"PAIR",
                   "answers":[{"id":11,"firstStatement":"A","secondStatement":"1","selected_id":12}]}]}]}]}""",
        )
        val adapter = moshi.adapter(AkmScheduleData::class.java)
        val back = adapter.fromJson(adapter.toJson(original))!!
        assertEquals(original, back)
        assertEquals(12, back.exams!!.single().instructions.single().questions.single().answers.single().selected_id)
    }

    @Test
    fun `null text fields become empty strings`() {
        val data = parse<AkmScheduleData>("""{"id":1,"date":null,"startAt":null,"endAt":null,"status":null}""")
        assertEquals("", data.date)
        assertEquals("", data.status)
    }

    // ---- dates ----

    @Test
    fun `start and end are read in the device zone and an unreadable end stays unknown`() {
        val item = parse<AkmScheduleData>("""{"id":1,"date":"2026-04-14","startAt":"08:00","endAt":"10:00"}""").toAkmItem()
        assertNotNull(item.startAtMillis)
        assertEquals(2 * 3_600_000L, item.endAtMillis!! - item.startAtMillis!!)
        assertEquals(14, item.endDay)
        val unknown = parse<AkmScheduleData>("""{"id":2,"date":"","startAt":"","endAt":""}""").toAkmItem()
        assertNull(unknown.endAtMillis)
        assertFalse(unknown.hasEnded(Long.MAX_VALUE))
        assertTrue(unknown.hasStarted(0))
    }

    // ---- question types ----

    private fun type(answerType: String, vararg showFalse: Boolean): QuestionType {
        val answers = showFalse.joinToString(",") { """{"id":1,"showFalse":$it}""" }
        return parse<id.diskola.app.dataclass.ResponData.AkmQuestionData>("""{"id":1,"answerType":"$answerType","answers":[$answers]}""").toQuestionType()
    }

    @Test
    fun `a one row statement with the table flag is a table, not benar salah`() {
        assertEquals(QuestionType.TabelPernyataan, type("STATEMENT", true))
        assertEquals(QuestionType.BenarSalah, type("STATEMENT", false))
        assertEquals(QuestionType.MultiJawaban, type("STATEMENT", false, false))
        assertEquals(QuestionType.TabelPernyataan, type("STATEMENT", true, true))
    }

    @Test
    fun `unknown answer types fall back to an essay so the exam can still be finished`() {
        assertEquals(QuestionType.EsaiSingkat, type("SOMETHING_NEW"))
        assertEquals(QuestionType.EsaiSingkat, type("ESSAY"))
        assertEquals(QuestionType.EsaiSingkat, type("SHORT_ESSAY_NUM"))
        assertEquals(QuestionType.PilihanGanda, type("MULTIPLE CHOICE"))
        assertEquals(QuestionType.Menjodohkan, type("PAIR"))
    }

    // ---- "Dibatalkan" watermark (doc 06 §4.4) ----

    @Test
    fun `watermark keys are optional, nullable and survive storage`() {
        val none = parse<AkmScheduleData>("""{"id":1}""")
        assertNull(none.watermark_bool)
        assertEquals("", none.watermark_label)
        val cancelled = parse<AkmScheduleData>("""{"id":1,"watermark_bool":true,"watermark_label":"DIBATALKAN"}""")
        assertEquals(true, cancelled.watermark_bool)
        val nulls = parse<AkmScheduleData>("""{"id":1,"watermark_bool":null,"watermark_label":null}""")
        assertNull(nulls.watermark_bool)
        assertEquals("", nulls.watermark_label)
        val adapter = moshi.adapter(AkmScheduleData::class.java)
        assertEquals(cancelled, adapter.fromJson(adapter.toJson(cancelled)))
    }

    @Test
    fun `a cancelled schedule maps to the mark on its card and a plain one does not`() {
        val cancelled = parse<AkmScheduleData>("""{"id":1,"watermark_bool":true}""").toAkmItem()
        assertEquals(id.diskola.app.utils.AkmRules.AkmWatermark(true, "DIBATALKAN"), cancelled.watermark)
        val custom = parse<AkmScheduleData>("""{"id":1,"watermark_bool":true,"watermark_label":"DIGANTI"}""").toAkmItem()
        assertEquals("DIGANTI", custom.watermark.label)
        assertFalse(parse<AkmScheduleData>("""{"id":1}""").toAkmItem().watermark.enabled)
        assertFalse(parse<AkmScheduleData>("""{"id":1,"watermark_bool":false,"watermark_label":"X"}""").toAkmItem().watermark.enabled)
    }

    @Test
    fun `a score row without the key keeps the mark known from the list`() {
        val known = id.diskola.app.utils.AkmRules.AkmWatermark(true, "DIBATALKAN")
        val withoutKey = parse<AkmScheduleData>("""{"id":1,"exam_score":80}""")
        assertTrue(withoutKey.toAkmScoreItem(AkmStatus.SudahDinilai, known).watermark.enabled)
        assertFalse(withoutKey.toAkmScoreItem(AkmStatus.SudahDinilai, null).watermark.enabled)
        val restored = parse<AkmScheduleData>("""{"id":1,"watermark_bool":false}""")
        assertFalse(restored.toAkmScoreItem(AkmStatus.SudahDinilai, known).watermark.enabled)
    }
}
