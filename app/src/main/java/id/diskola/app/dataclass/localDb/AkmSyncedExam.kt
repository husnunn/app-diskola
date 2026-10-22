package id.diskola.app.dataclass.localDb

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import id.diskola.app.dataclass.ResponData.AkmScheduleData

/**
 * Local download states for one AKM exam's `AkmDownloadWorker` run. Whether the answers reached the server is **not** kept here:
 * that is `akm_submission` ([AkmSubmissionEntity]), so a re-download can never touch it.
 */
object AkmDownloadStatus {
    const val NONE = "NONE"
    const val DOWNLOADING = "DOWNLOADING"
    const val DOWNLOADED = "DOWNLOADED"
    const val FAILED = "FAILED"
}

/**
 * Durable cache for one synced AKM exam — replaces the in-memory-only `rawSchedules` map that used
 * to lose everything on process death. [schedule] is the downloaded [AkmScheduleData] with every
 * question/answer/media URL already rewritten to a local file path by `AkmDownloadWorker`, and with
 * `isRandom` questions/options already shuffled (once, at download), so the stored order is the order shown.
 */
@Entity(tableName = "akm_synced_exam")
data class AkmSyncedExam(
    @PrimaryKey val id: Int,
    val downloadStatus: String = AkmDownloadStatus.NONE,
    val downloadProgress: Int = 0,
    val totalQuestions: Int = 0,
    val errorMessage: String = "",
    val schedule: AkmScheduleData? = null,
    /** The exam session id the server created at download (`studentExam.id`); `> 0` means the exam was started on this device. */
    @ColumnInfo(defaultValue = "0") val studentExamId: Int = 0,
    /** `date`+`endAt` as epoch millis, null when the server's value could not be read. The deadline worker is scheduled from it. */
    val endAtMillis: Long? = null,
)
