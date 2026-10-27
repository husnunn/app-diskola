package id.diskola.app.dataclass

import id.diskola.app.dataclass.ResponData.Assignment
import id.diskola.app.dataclass.ResponData.CollectHomeworkResponse
import id.diskola.app.dataclass.ResponData.HomeworkCreateResponse
import id.diskola.app.dataclass.ResponData.HomeworkItem
import id.diskola.app.dataclass.ResponData.HomeworkResponse
import id.diskola.app.dataclass.ResponData.TeacherDataScopeResponse
import id.diskola.app.di.module.MoshiModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeworkModelsParseTest {

    private val moshi = MoshiModule.instance()

    private fun scope(json: String) =
        moshi.adapter(TeacherDataScopeResponse::class.java).fromJson(json)!!.data!!

    @Test
    fun teacherDataScope_restrictedFalseOpensFreeMode() {
        val data = scope("""{ "data": { "restricted": false, "label": "Semua data" } }""")
        assertEquals(false, data.restricted)
        assertEquals("Semua data", data.label)
        assertFalse(data.isRestrictedForForm)
    }

    @Test
    fun teacherDataScope_restrictedTrueKeepsAssignedMode() {
        val data = scope("""{ "data": { "restricted": true } }""")
        assertTrue(data.isRestrictedForForm)
    }

    @Test
    fun teacherDataScope_missingOrNullRestrictedFailsClosed() {
        assertNull(scope("""{ "data": { "teacher_master_data_scope": "all" } }""").restricted)
        assertTrue(scope("""{ "data": { "teacher_master_data_scope": "all" } }""").isRestrictedForForm)
        assertTrue(scope("""{ "data": { "restricted": null } }""").isRestrictedForForm)
        assertTrue(scope("""{ "data": {} }""").isRestrictedForForm)
    }

    @Test
    fun teacherDataScope_legacyResolvedStillWorksAlongsideRestricted() {
        val data = scope("""{ "data": { "teacher_master_data_scope": "assigned", "restricted": false } }""")
        assertTrue(data.isAssigned)
        assertFalse(data.isRestrictedForForm)
    }

    @Test
    fun assignment_nullNumbersDecodeAsZero() {
        val json = """
            {
              "id": null,
              "score": null,
              "scored": null,
              "uploaded": null,
              "upload_at": "2026-10-05 14:30:00",
              "student": {"name": "Andi"}
            }
        """.trimIndent()
        val assignment = moshi.adapter(Assignment::class.java).fromJson(json)!!
        assertEquals(0, assignment.id)
        assertEquals(0, assignment.score)
        assertEquals(0, assignment.scored)
        assertEquals(0, assignment.uploaded)
        assertEquals("Andi", assignment.student.name)
    }

    @Test
    fun assignment_realNumbersAreKept() {
        val json = """{ "id": 41, "score": 90, "scored": 1, "uploaded": 1 }"""
        val assignment = moshi.adapter(Assignment::class.java).fromJson(json)!!
        assertEquals(41, assignment.id)
        assertEquals(90, assignment.score)
        assertEquals(1, assignment.scored)
        assertEquals(1, assignment.uploaded)
    }

    @Test
    fun homeworkItem_nullScheduleDecodes() {
        val item = moshi.adapter(HomeworkItem::class.java)
            .fromJson("""{ "id": 5, "title": "Tugas bebas", "schedule": null }""")!!
        assertEquals(5, item.id)
        assertNull(item.schedule)
    }

    @Test
    fun homeworkItem_absentScheduleDecodes() {
        val item = moshi.adapter(HomeworkItem::class.java)
            .fromJson("""{ "id": 5, "title": "Tugas bebas" }""")!!
        assertNull(item.schedule)
    }

    @Test
    fun homeworkItem_scheduleObjectStillDecodes() {
        val json = """
            {
              "id": 6,
              "schedule": {
                "id": 498,
                "day": "senin",
                "time_plot": {"id": 1, "start_at": "08:00", "end_at": "09:30"}
              }
            }
        """.trimIndent()
        val item = moshi.adapter(HomeworkItem::class.java).fromJson(json)!!
        assertEquals(498, item.schedule?.id)
        assertEquals("senin", item.schedule?.day)
        assertEquals("08:00", item.schedule?.time_plot?.start_at)
    }

    @Test
    fun homeworkItem_emptyArrayScheduleDecodesAsNoSchedule() {
        // PHP encodes an empty map as `[]`: a free-mode task must not fail the whole page of tasks.
        val list = moshi.adapter(HomeworkResponse::class.java)
            .fromJson("""{ "data": [ { "id": 1, "schedule": [] }, { "id": 2, "schedule": { "id": 9, "day": "selasa" } } ] }""")!!
        assertNull(list.data[0].schedule)
        assertEquals(9, list.data[1].schedule?.id)
    }

    @Test
    fun assignment_nullDatesDecodeAsEmptyText() {
        val assignment = moshi.adapter(Assignment::class.java)
            .fromJson("""{ "id": 3, "upload_at": null, "scored_at": null }""")!!
        assertEquals("", assignment.upload_at)
        assertEquals("", assignment.scored_at)
    }

    @Test
    fun collectResponse_nullIdDoesNotFailAfterTheServerAcceptedTheSubmission() {
        val response = moshi.adapter(CollectHomeworkResponse::class.java).fromJson("""{ "data": { "id": null } }""")!!
        assertEquals(0, response.data.id)
    }

    @Test
    fun createResponse_withOnlyAMessageStillParses() {
        val response = moshi.adapter(HomeworkCreateResponse::class.java).fromJson("""{ "message": "Berhasil membuat Tugas" }""")!!
        assertNull(response.data.schedule)
    }
}
