package id.diskola.app.utils

import id.diskola.app.dataclass.ResponData.Assignment
import id.diskola.app.dataclass.ResponData.AssignmentAnswerFile

/**
 * What the teacher's scoring screen shows of a student's answer (legacy `Assignment.resolvedAnswerFiles` and `StudentAssignmentFile`).
 * A response may carry the files as a list, or only as the single `file_*` fields of the submission; either way each file is read with the
 * same two spellings of its fields.
 */
object AnswerFileRules {

    /** The `files` list when the server sent one, else the submission's own single file (nothing when it has no path). */
    fun filesOf(assignment: Assignment): List<AssignmentAnswerFile> {
        if (assignment.files.isNotEmpty()) return assignment.files
        if (assignment.file_path.isBlank()) return emptyList()
        return listOf(
            AssignmentAnswerFile(
                file_name = assignment.file_name,
                file_path = assignment.file_path,
                file_format = assignment.file_format,
                file_type = assignment.file_type,
                file_size = assignment.file_size,
            ),
        )
    }

    /** The links the student attached; the server sends `[null]` for none, which is not a link. */
    fun linksOf(assignment: Assignment): List<String> =
        assignment.uri_student?.link.orEmpty().filter { !it.isNullOrBlank() }

    /** The "Jawaban terkumpul" section exists only when there is a file or a link to show (an empty heading is not shown). */
    fun hasAnswer(files: List<AssignmentAnswerFile>, links: List<String>): Boolean = files.isNotEmpty() || links.isNotEmpty()

    fun name(file: AssignmentAnswerFile): String = file.file_name.ifEmpty { file.student_assignment_files_name }

    fun size(file: AssignmentAnswerFile): String = file.file_size.ifEmpty { file.student_assignment_files_size }

    fun format(file: AssignmentAnswerFile): String =
        file.file_format.ifEmpty { file.student_assignment_files_format }.ifEmpty { name(file).substringAfterLast('.', "") }.lowercase()

    /** Where to fetch the file: backslashes dropped, a path without a host put under the assets host; blank when the file has no path. */
    fun url(file: AssignmentAnswerFile): String {
        val raw = file.file_path.ifEmpty { file.student_assignment_files_path }.replace("\\", "")
        return if (raw.isBlank()) "" else resolveAssetUrl(raw)
    }
}
