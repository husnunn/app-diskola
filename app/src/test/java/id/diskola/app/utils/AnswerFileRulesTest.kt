package id.diskola.app.utils

import id.diskola.app.BuildConfig
import id.diskola.app.dataclass.ResponData.Assignment
import id.diskola.app.dataclass.ResponData.AssignmentAnswerFile
import id.diskola.app.dataclass.ResponData.AssignmentFileItem
import id.diskola.app.dataclass.ResponData.HomeworkAnswerFileTable
import id.diskola.app.dataclass.ResponData.MateriLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerFileRulesTest {

    private val listed = AssignmentAnswerFile(file_name = "jawaban-1.pdf", file_path = "https://cdn.example/a/jawaban-1.pdf", file_size = "2048")

    @Test
    fun filesOf_prefersTheListOverTheSingleFields() {
        val a = Assignment(files = listOf(listed), file_name = "lain.pdf", file_path = "https://cdn.example/lain.pdf")
        assertEquals(listOf(listed), AnswerFileRules.filesOf(a))
    }

    @Test
    fun filesOf_fallsBackToTheSubmissionsOwnFileWhenThereIsNoList() {
        val a = Assignment(file_name = "tugas.docx", file_path = "https://cdn.example/tugas.docx", file_format = "docx", file_size = "100")
        val files = AnswerFileRules.filesOf(a)
        assertEquals(1, files.size)
        assertEquals("tugas.docx", AnswerFileRules.name(files.single()))
        assertEquals("100", AnswerFileRules.size(files.single()))
    }

    @Test
    fun filesOf_isEmptyWhenThereIsNothingToOpen() {
        assertTrue(AnswerFileRules.filesOf(Assignment()).isEmpty())
        assertTrue(AnswerFileRules.filesOf(Assignment(file_name = "tanpa-path.pdf")).isEmpty())
    }

    @Test
    fun linksOf_dropsTheNullTheServerSendsForNoLink() {
        // Dev sends `"uri_student":{"link":[null]}` for a submission without a link; that read as "has an answer" and drew an empty heading.
        val none = Assignment(uri_student = MateriLink(link = listOf("")))
        assertTrue(AnswerFileRules.linksOf(none).isEmpty())
        assertTrue(AnswerFileRules.linksOf(Assignment(uri_student = null)).isEmpty())
        val one = Assignment(uri_student = MateriLink(link = listOf("", "https://youtu.be/x")))
        assertEquals(listOf("https://youtu.be/x"), AnswerFileRules.linksOf(one))
    }

    @Test
    fun hasAnswer_needsAFileOrALink() {
        assertFalse(AnswerFileRules.hasAnswer(emptyList(), emptyList()))
        assertTrue(AnswerFileRules.hasAnswer(listOf(listed), emptyList()))
        assertTrue(AnswerFileRules.hasAnswer(emptyList(), listOf("https://youtu.be/x")))
    }

    @Test
    fun fieldsAreReadUnderEitherSpelling() {
        val alt = AssignmentAnswerFile(
            student_assignment_files_name = "jawab.pdf",
            student_assignment_files_path = "https://cdn.example/b/jawab.pdf",
            student_assignment_files_size = "5",
        )
        assertEquals("jawab.pdf", AnswerFileRules.name(alt))
        assertEquals("5", AnswerFileRules.size(alt))
        assertEquals("https://cdn.example/b/jawab.pdf", AnswerFileRules.url(alt))
        assertEquals("pdf", AnswerFileRules.format(alt))
        // The first spelling wins when both are filled.
        assertEquals("jawaban-1.pdf", AnswerFileRules.name(listed.copy(student_assignment_files_name = "x.pdf")))
    }

    @Test
    fun url_dropsBackslashesAndPutsAHostlessPathUnderTheAssetsHost() {
        assertEquals("https://cdn.example/a/b.pdf", AnswerFileRules.url(AssignmentAnswerFile(file_path = "https://cdn.example/a/b\\.pdf")))
        val relative = AnswerFileRules.url(AssignmentAnswerFile(file_path = "assignment/x/jawaban.pdf"))
        assertEquals(BuildConfig.ASSETS_URL.trimEnd('/') + "/assignment/x/jawaban.pdf", relative)
        assertEquals("", AnswerFileRules.url(AssignmentAnswerFile()))
    }

    @Test
    fun resolvePath_givesTheStudentsOwnAnswerFileTheSameUrlTheTeacherViewUses() {
        assertEquals(BuildConfig.ASSETS_URL.trimEnd('/') + "/assignment/x/jawaban.pdf", AnswerFileRules.resolvePath("assignment\\/x\\/jawaban.pdf"))
        assertEquals("https://cdn.example/a/b.pdf", AnswerFileRules.resolvePath("https://cdn.example/a/b.pdf"))
        assertEquals("", AnswerFileRules.resolvePath(""))
    }

    @Test
    fun homeworkAnswerFileTable_keepsAResolvedPathSoOpenAndDownloadNeedNoHostGuessing() {
        val row = HomeworkAnswerFileTable.from(7, AssignmentFileItem(file_name = "j.pdf", file_path = "assignment/x/j.pdf"))
        assertEquals(BuildConfig.ASSETS_URL.trimEnd('/') + "/assignment/x/j.pdf", row.file_path)
        assertEquals(7, row.homework_id)
    }
}
