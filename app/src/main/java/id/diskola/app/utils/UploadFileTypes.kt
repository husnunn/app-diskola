package id.diskola.app.utils

/** What a teacher may attach as a learning file (materi, or the question of a tugas): the list legacy's shared file picker offered. */
object UploadFileTypes {
    val DOCUMENT_MIME_TYPES: Array<String> = arrayOf(
        "application/pdf",
        "image/jpeg",
        "image/png",
        "image/*",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    )

    /** The line under "Upload …" that names them. */
    const val HINT = "PDF, gambar, Word, Excel, atau PowerPoint"
}
