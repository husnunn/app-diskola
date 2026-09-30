package id.diskola.app.dataclass.localDb

import androidx.room.Entity
import androidx.room.PrimaryKey
import id.diskola.app.dataclass.ResponData.AkmScheduleData

/** Local download states for one AKM exam's `AkmDownloadWorker` run. */
object AkmDownloadStatus {
    const val NONE = "NONE"
    const val DOWNLOADING = "DOWNLOADING"
    const val DOWNLOADED = "DOWNLOADED"
    const val FAILED = "FAILED"

    /** Set once `submitExamSchoolAnswer` succeeds — a [DOWNLOADED] row that never reaches this
     * state is what [id.diskola.app.utils.session.SessionManager.hasPendingExam] blocks logout for
     * (doc `06-asesmen-akm-tryout-ujian.md` §15/§23.3#2). */
    const val SUBMITTED = "SUBMITTED"
}

/**
 * Durable cache for one synced AKM exam — replaces the in-memory-only `rawSchedules` map that used
 * to lose everything on process death. [schedule] is the downloaded [AkmScheduleData] with every
 * question/answer/media URL already rewritten to a local file path by `AkmDownloadWorker`.
 */
@Entity(tableName = "akm_synced_exam")
data class AkmSyncedExam(
    @PrimaryKey val id: Int,
    val downloadStatus: String = AkmDownloadStatus.NONE,
    val downloadProgress: Int = 0,
    val totalQuestions: Int = 0,
    val errorMessage: String = "",
    val schedule: AkmScheduleData? = null,
)
