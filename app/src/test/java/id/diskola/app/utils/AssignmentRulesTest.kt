package id.diskola.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class AssignmentRulesTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val dueAt = 1_791_210_600_000L // 2026-10-05 14:30:00 UTC

    private val freeInput = AssignmentInput(
        restricted = false,
        classId = 7,
        subjectId = 12,
        title = "Latihan Bab 1",
        description = "Kerjakan halaman 10",
        dueAt = dueAt,
    )

    private val assignedInput = AssignmentInput(
        restricted = true,
        classId = 7,
        day = "senin",
        scheduleId = 498,
        title = "Latihan Bab 1",
        description = "Kerjakan halaman 10",
        dueAt = dueAt,
    )

    @Test
    fun fields_freeMode_sendsClassAndSubjectWithoutSchedule() {
        val fields = AssignmentPayload.fields(freeInput, utc)

        assertEquals(
            setOf(
                "title", "description", "school_class_id", "school_subject_id",
                "end_at", "checked", "downloaded", "uploaded",
            ),
            fields.keys,
        )
        assertEquals("7", fields["school_class_id"])
        assertEquals("12", fields["school_subject_id"])
        assertFalse(fields.containsKey("school_subject_schedule_id"))
    }

    @Test
    fun fields_assignedMode_sendsOnlySchedule() {
        val fields = AssignmentPayload.fields(assignedInput, utc)

        assertEquals(
            setOf(
                "title", "description", "school_subject_schedule_id",
                "end_at", "checked", "downloaded", "uploaded",
            ),
            fields.keys,
        )
        assertEquals("498", fields["school_subject_schedule_id"])
        assertFalse(fields.containsKey("school_class_id"))
        assertFalse(fields.containsKey("school_subject_id"))
    }

    @Test
    fun fields_unknownScope_failsClosedToAssignedShape() {
        val fields = AssignmentPayload.fields(assignedInput.copy(restricted = null), utc)
        assertTrue(fields.containsKey("school_subject_schedule_id"))
        assertFalse(fields.containsKey("school_class_id"))
    }

    @Test
    fun fields_neverSendsLegacyKeysOrNullStrings() {
        val keys = AssignmentPayload.fields(freeInput, utc).keys +
                AssignmentPayload.fields(assignedInput, utc).keys
        listOf(
            "teacher_id", "school_id", "grade", "downloded",
            "school_classes_id", "school_subject_schedules_id",
        ).forEach { assertFalse("legacy key $it", it in keys) }

        val values = AssignmentPayload.fields(freeInput.copy(link = ""), utc).values +
                AssignmentPayload.fields(assignedInput.copy(link = ""), utc).values
        assertFalse(values.any { it == "null" })
    }

    @Test
    fun fields_endAtUsesWireFormatAndFlagsAreCanonical() {
        val fields = AssignmentPayload.fields(
            freeInput.copy(needDownload = true, needUpload = false), utc
        )
        assertEquals("2026-10-05 14:30:00", fields["end_at"])
        assertEquals("1", fields["checked"])
        assertEquals("1", fields["downloaded"])
        assertEquals("0", fields["uploaded"])
    }

    @Test
    fun fields_linkOnlyWhenNotBlank() {
        assertFalse(AssignmentPayload.fields(freeInput.copy(link = ""), utc).containsKey("link"))
        assertFalse(AssignmentPayload.fields(freeInput.copy(link = "   "), utc).containsKey("link"))
        assertEquals(
            "https://contoh.id/soal",
            AssignmentPayload.fields(freeInput.copy(link = " https://contoh.id/soal "), utc)["link"],
        )
    }

    @Test
    fun validate_validInputsHaveNoErrors() {
        assertTrue(AssignmentPayload.validate(freeInput).isEmpty())
        assertTrue(AssignmentPayload.validate(assignedInput).isEmpty())
    }

    @Test
    fun validate_unknownScopeBlocksSubmit() {
        val errors = AssignmentPayload.validate(freeInput.copy(restricted = null))
        assertEquals(setOf("scope"), errors.keys)
    }

    @Test
    fun validate_freeModeRequiresClassAndSubjectButNotDayOrSchedule() {
        val errors = AssignmentPayload.validate(freeInput.copy(classId = 0, subjectId = 0))
        assertEquals(setOf("school_class_id", "school_subject_id"), errors.keys)
    }

    @Test
    fun validate_assignedModeRequiresDayAndSchedule() {
        val errors = AssignmentPayload.validate(
            assignedInput.copy(classId = 0, day = "", scheduleId = 0)
        )
        assertEquals(setOf("school_class_id", "day", "school_subject_schedule_id"), errors.keys)
    }

    @Test
    fun validate_requiresTitleDescriptionAndDueDate() {
        val errors = AssignmentPayload.validate(
            freeInput.copy(title = " ", description = "", dueAt = null)
        )
        assertEquals(setOf("title", "description", "end_at"), errors.keys)
    }

    @Test
    fun validate_uploadSoalNeedsFileOrLink() {
        assertEquals(
            setOf("file"),
            AssignmentPayload.validate(freeInput.copy(uploadFile = true)).keys,
        )
        assertTrue(AssignmentPayload.validate(freeInput.copy(uploadFile = true, hasFile = true)).isEmpty())
        assertTrue(
            AssignmentPayload.validate(freeInput.copy(uploadFile = true, link = "https://a.id")).isEmpty()
        )
    }

    @Test
    fun validate_fileLimitIsTwelveMegabytes() {
        val limit = 12L * 1024 * 1024
        assertEquals(limit, AssignmentPayload.MAX_FILE_BYTES)

        val atLimit = freeInput.copy(uploadFile = true, hasFile = true, fileSizeBytes = limit)
        assertTrue(AssignmentPayload.validate(atLimit).isEmpty())

        val tooBig = atLimit.copy(fileSizeBytes = limit + 1)
        assertEquals("Ukuran file maksimal 12 MB", AssignmentPayload.validate(tooBig)["file"])
    }

    @Test
    fun formatWire_usesFixedPatternRegardlessOfLocale() {
        assertEquals("2026-10-05 14:30:00", AssignmentDates.formatWire(dueAt, utc))
        assertEquals(
            "2026-10-05 21:30:00",
            AssignmentDates.formatWire(dueAt, TimeZone.getTimeZone("Asia/Jakarta")),
        )
    }

    @Test
    fun formatDisplay_keepsLegacyDisplayPattern() {
        assertEquals("05-10-2026 14:30:00", AssignmentDates.formatDisplay(dueAt, utc))
    }

    private fun parse(label: String?): String? =
        AssignmentDates.parseEndAtLabel(label, utc)?.let { AssignmentDates.formatWire(it, utc) }

    @Test
    fun parseEndAtLabel_acceptsCommaAndNoComma() {
        assertEquals("2026-10-05 14:30:00", parse("5 Okt 2026 14:30"))
        assertEquals("2026-10-05 14:30:00", parse("05 Okt 2026, 14:30"))
        assertEquals("2026-10-05 08:05:00", parse("  5 Okt 2026 08.05  "))
        assertEquals("2026-10-05 14:30:45", parse("5 Okt 2026 14:30:45"))
    }

    @Test
    fun parseEndAtLabel_acceptsIndonesianAndEnglishMonths() {
        assertEquals("2026-05-17 09:00:00", parse("17 Mei 2026 09:00"))
        assertEquals("2026-05-17 09:00:00", parse("17 May 2026 09:00"))
        assertEquals("2026-08-17 09:00:00", parse("17 Agu 2026 09:00"))
        assertEquals("2026-08-17 09:00:00", parse("17 Aug 2026 09:00"))
        assertEquals("2026-08-17 09:00:00", parse("17 Agustus 2026, 09:00"))
        assertEquals("2026-12-01 23:59:00", parse("1 Des 2026 23:59"))
        assertEquals("2026-12-01 23:59:00", parse("1 December 2026 23:59"))
        assertEquals("2026-10-05 14:30:00", parse("5 oktober 2026 14:30"))
    }

    @Test
    fun parseEndAtLabel_rejectsGarbageAndInvalidDates() {
        assertNull(parse(null))
        assertNull(parse(""))
        assertNull(parse("besok pagi"))
        assertNull(parse("5 Xyz 2026 14:30"))
        assertNull(parse("5 Okt 2026"))
        assertNull(parse("31 Feb 2026 10:00"))
        assertNull(parse("29 Feb 2027 10:00"))
        assertNull(parse("5 Okt 2026 24:00"))
        assertNull(parse("5 Okt 2026 14:61"))
    }

    @Test
    fun parseEndAtLabel_acceptsLeapDay() {
        assertEquals("2028-02-29 10:00:00", parse("29 Feb 2028 10:00"))
    }

    @Test
    fun parseEndAtLabel_roundTripsWithWireFormat() {
        val millis = AssignmentDates.parseEndAtLabel("5 Okt 2026 14:30", utc)!!
        assertEquals(dueAt, millis)
    }
}

/** The teacher's own task list query (`AssignmentFilter`): "Semua" is not sent and the indices stay in sequence. */
class AssignmentFilterTest {

    private fun query(classId: Int? = null, subjectId: Int? = null, skip: Int = 0) =
        AssignmentFilter.teacherListQuery(teacherId = 7, classId = classId, subjectId = subjectId, take = 10, skip = skip)

    @Test
    fun `with no class and no subject only the teacher is filtered`() {
        assertEquals(
            mapOf("take" to "10", "skip" to "0", "filter[0][0]" to "teacher_id", "filter[0][1]" to "=", "filter[0][2]" to "7"),
            query(),
        )
    }

    @Test
    fun `Semua is never sent, neither as null nor as zero`() {
        assertEquals(query(), query(classId = 0, subjectId = 0))
        assertFalse(query(classId = 0).keys.any { it.contains("school_classes_id") || it.startsWith("filter[2]") })
    }

    @Test
    fun `a class alone is the second filter`() {
        val q = query(classId = 12)
        assertEquals("and", q["filter[1]"])
        assertEquals("school_classes_id", q["filter[2][0]"])
        assertEquals("12", q["filter[2][2]"])
        assertNull(q["filter[3]"])
    }

    @Test
    fun `a subject alone takes the next free index, not a fixed one`() {
        val q = query(subjectId = 5)
        assertEquals("and", q["filter[1]"])
        assertEquals("school_subject_id", q["filter[2][0]"])
        assertEquals("5", q["filter[2][2]"])
        assertNull(q["filter[4][0]"])
    }

    @Test
    fun `class and subject together follow each other`() {
        val q = query(classId = 12, subjectId = 5, skip = 20)
        assertEquals("20", q["skip"])
        assertEquals("school_classes_id", q["filter[2][0]"])
        assertEquals("and", q["filter[3]"])
        assertEquals("school_subject_id", q["filter[4][0]"])
        assertEquals("5", q["filter[4][2]"])
    }
}
