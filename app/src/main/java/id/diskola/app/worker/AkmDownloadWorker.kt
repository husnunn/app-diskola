package id.diskola.app.worker

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import id.diskola.app.R
import id.diskola.app.apiservice.AsesmenApiService
import id.diskola.app.dataclass.ResponData.AkmQuestionData
import id.diskola.app.dataclass.localDb.AkmDownloadStatus
import id.diskola.app.dataclass.localDb.AkmSyncDao
import id.diskola.app.dataclass.localDb.AkmSyncedExam
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.FileUtils
import id.diskola.app.utils.resolveAssetUrl
import timber.log.Timber
import java.io.File

/** Filter with `adb logcat -s AKM-DOWNLOAD` (or grep "AKM-DOWNLOAD") to see every download
 * attempt this worker makes — skips (already cached), successes, and failures/fallbacks — plus the
 * per-exam start/progress/completion summary. Same tag convention as the legacy app's downloader. */
private const val LOG_TAG = "AKM-DOWNLOAD"

/**
 * Ports `AkmDownloader.doWork()` (legacy `android-portal/.../worker/AkmDownloader.kt:28-237`) to
 * this repo's data model: downloads every question/answer image and cacheable media file to
 * `filesDir/akm-exam{examId}/...` (same naming scheme legacy uses — `buildPairAnswer()` in
 * `AkmUiModels.kt` depends on the `a{questionId}_{id}_2.jpg` pattern matching exactly), then persists
 * the whole schedule (now pointing at local paths) to Room via [AkmSyncDao] so it survives process
 * death and doesn't need re-downloading.
 */
@HiltWorker
class AkmDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val asesmenApiService: AsesmenApiService,
    private val fileUtils: FileUtils,
    private val akmSyncDao: AkmSyncDao,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val akmId = inputData.getInt("id", 0)
        if (akmId < 1) return Result.failure()

        Timber.tag(LOG_TAG).i("Mulai sinkronisasi: akmId=%d", akmId)

        akmSyncDao.upsert(
            (akmSyncDao.get(akmId) ?: AkmSyncedExam(id = akmId)).copy(
                downloadStatus = AkmDownloadStatus.DOWNLOADING,
                errorMessage = "",
            )
        )

        return try {
            val schedule = asesmenApiService.downloadExamSchool(akmId).data
                ?: throw IllegalStateException("Respons server kosong")

            val rawExams = schedule.exams ?: listOfNotNull(schedule.exam)
            val totalQuestions = rawExams.sumOf { exam -> exam.instructions.sumOf { it.questions.size } }
            Timber.tag(LOG_TAG).i("akmId=%d: %d soal akan diproses", akmId, totalQuestions)
            akmSyncDao.upsert(akmSyncDao.get(akmId)!!.copy(totalQuestions = totalQuestions, downloadProgress = 0))
            showProgress(akmId, totalQuestions, 0)

            var progress = 0
            var failedAssets = 0
            val processedExams = rawExams.map { exam ->
                val processedInstructions = exam.instructions.map { inst ->
                    val processedQuestions = inst.questions.map { question ->
                        val before = failedAssets
                        val processed = downloadQuestionAssets(exam.id, question) { failedAssets++ }
                        if (failedAssets > before) {
                            Timber.tag(LOG_TAG).w("akmId=%d: soal id=%d punya %d aset gagal diunduh", akmId, question.id, failedAssets - before)
                        }
                        progress++
                        akmSyncDao.upsert(akmSyncDao.get(akmId)!!.copy(downloadProgress = progress))
                        showProgress(akmId, totalQuestions, progress)
                        processed
                    }
                    inst.copy(questions = processedQuestions)
                }
                exam.copy(instructions = processedInstructions)
            }

            NotificationManagerCompat.from(applicationContext).cancel(notificationId(akmId))
            akmSyncDao.upsert(
                akmSyncDao.get(akmId)!!.copy(
                    downloadStatus = AkmDownloadStatus.DOWNLOADED,
                    schedule = schedule.copy(exams = processedExams, exam = null),
                )
            )
            Timber.tag(LOG_TAG).i(
                "akmId=%d selesai: %d soal diproses, %d aset gagal diunduh (fallback ke remote/kosong)",
                akmId, totalQuestions, failedAssets
            )
            Result.success()
        } catch (e: Exception) {
            Timber.tag(LOG_TAG).e(e, "akmId=%d: sinkronisasi gagal total", akmId)
            val message = AppErrorHandler.getMessage(e)
            akmSyncDao.upsert(
                (akmSyncDao.get(akmId) ?: AkmSyncedExam(id = akmId)).copy(
                    downloadStatus = AkmDownloadStatus.FAILED,
                    errorMessage = message,
                )
            )
            showDownloadFailed(akmId, message)
            Result.failure(workDataOf("message" to message))
        }
    }

    /** Downloads one question's image, its media, and its answers' images — port of the per-question
     * body of `AkmDownloader.kt:88-217`, including the Menjodohkan pairing shuffle at `:138-155`.
     * [onAssetFailed] is called once per image/media that couldn't be downloaded, so the caller can
     * summarize how many assets fell back per question. */
    private suspend fun downloadQuestionAssets(examId: Int, questionIn: AkmQuestionData, onAssetFailed: () -> Unit): AkmQuestionData {
        val folder = "akm-exam$examId"
        var question = questionIn

        val isPairImageQuestion = question.answerType == "PAIR" &&
            question.answers.firstOrNull()?.firstStatement.isNullOrEmpty()

        if (question.image.isNotBlank()) {
            question = question.copy(image = downloadImageOrFallback(folder, "q${question.id}.jpg", question.image, onAssetFailed))
        }

        question = question.copy(
            media = question.media.map { media ->
                val remoteUrl = media.url
                val mediaType = media.type.lowercase()
                val canCache = remoteUrl.isNotBlank() &&
                    (mediaType == "audio" || (mediaType == "video" && !isYoutubeUrl(remoteUrl)))
                val localPath = if (canCache) {
                    val fallbackExt = if (mediaType == "video") "mp4" else "mp3"
                    val ext = remoteUrl.substringBefore('?').substringAfterLast('.', fallbackExt)
                        .lowercase().takeIf { it.length in 2..5 } ?: fallbackExt
                    downloadRawOrEmpty(folder, "q${question.id}_m${media.id}.$ext", remoteUrl, onAssetFailed)
                } else {
                    if (remoteUrl.isNotBlank()) {
                        Timber.tag(LOG_TAG).v("Media dilewati (tidak di-cache): soalId=%d mediaId=%d type=%s url=%s", question.id, media.id, media.type, remoteUrl)
                    }
                    ""
                }
                media.copy(localPath = localPath)
            }
        )

        var answers = question.answers
        if (question.answerType == "PAIR") {
            val originalSecondStatementById = answers.associate { it.id to it.secondStatement }
            val originalSecondFilePathById = answers.associate { it.id to it.secondFilePath }
            val shuffledIds = answers.map { it.id }.shuffled()
            answers = answers.mapIndexed { index, answer ->
                val selectedId = shuffledIds[index]
                answer.copy(
                    secondStatement = originalSecondStatementById[selectedId].orEmpty(),
                    secondFilePath = originalSecondFilePathById[selectedId].orEmpty(),
                    selected_id = selectedId,
                )
            }
        }

        val processedAnswers = answers.map { answerIn ->
            var answer = answerIn
            if (answer.filePath.isNotBlank()) {
                answer = answer.copy(filePath = downloadImageOrFallback(folder, "a${question.id}_${answer.id}.jpg", answer.filePath, onAssetFailed))
            }
            if (answer.firstFilePath.isNotBlank()) {
                answer = answer.copy(firstFilePath = downloadImageOrFallback(folder, "a${question.id}_${answer.id}_1.jpg", answer.firstFilePath, onAssetFailed))
            }
            if (answer.secondFilePath.isNotBlank()) {
                val ownerId = if (isPairImageQuestion && answer.selected_id > 0) answer.selected_id else answer.id
                answer = answer.copy(secondFilePath = downloadImageOrFallback(folder, "a${question.id}_${ownerId}_2.jpg", answer.secondFilePath, onAssetFailed))
            }
            answer
        }

        return question.copy(answers = processedAnswers)
    }

    /** Falls back to the (now-resolved) remote URL on failure (Coil can still show it online) —
     * mirrors `AkmDownloader.downloadImage()` (`AkmDownloader.kt:273-287`). [rawUrl] is resolved via
     * [resolveAssetUrl] first — the API may send a path relative to the separate assets host, not
     * `API_URL` (`local.properties`' `ASSETS_URL_*`, previously never wired into `BuildConfig`). */
    private suspend fun downloadImageOrFallback(folder: String, fileName: String, rawUrl: String, onFailed: () -> Unit): String {
        val url = resolveAssetUrl(rawUrl)
        if (!url.startsWith("http")) {
            Timber.tag(LOG_TAG).w("URL gambar tidak valid setelah resolveAssetUrl, dilewati: %s <- %s", fileName, rawUrl)
            onFailed()
            return url
        }
        val file = prepareFile(folder, fileName)
        if (file.length() > 0) {
            Timber.tag(LOG_TAG).v("Gambar sudah ada, skip unduh: %s", fileName)
            return file.absolutePath
        }
        val ok = fileUtils.downloadRawFile(url, file)
        return if (ok && file.length() > 0) {
            Timber.tag(LOG_TAG).d("Berhasil unduh gambar: %s (%d bytes) <- %s", fileName, file.length(), url)
            file.absolutePath
        } else {
            runCatching { file.delete() }
            Timber.tag(LOG_TAG).w("Gagal unduh gambar, pakai URL remote: %s <- %s", fileName, url)
            onFailed()
            url
        }
    }

    /** Falls back to empty on failure — mirrors `AkmDownloader.downloadFile()` (`AkmDownloader.kt:249-264`). */
    private suspend fun downloadRawOrEmpty(folder: String, fileName: String, rawUrl: String, onFailed: () -> Unit): String {
        val url = resolveAssetUrl(rawUrl)
        if (!url.startsWith("http")) {
            Timber.tag(LOG_TAG).w("URL media tidak valid setelah resolveAssetUrl, dilewati: %s <- %s", fileName, rawUrl)
            onFailed()
            return ""
        }
        val file = prepareFile(folder, fileName)
        if (file.length() > 0) {
            Timber.tag(LOG_TAG).v("Media sudah ada, skip unduh: %s", fileName)
            return file.absolutePath
        }
        val ok = fileUtils.downloadRawFile(url, file)
        return if (ok && file.length() > 0) {
            Timber.tag(LOG_TAG).d("Berhasil unduh media: %s (%d bytes) <- %s", fileName, file.length(), url)
            file.absolutePath
        } else {
            runCatching { file.delete() }
            Timber.tag(LOG_TAG).w("Gagal unduh media, tidak ada fallback: %s <- %s", fileName, url)
            onFailed()
            ""
        }
    }

    /** Skips a file already downloaded (non-empty) — makes re-sync an incremental resume, not a
     * full re-download, matching `AkmDownloader.kt:249-264,289-295`. */
    private fun prepareFile(folderName: String, fileName: String): File {
        val folder = File(applicationContext.filesDir, folderName).apply { if (!exists()) mkdirs() }
        return File(folder, fileName).apply { if (!exists()) createNewFile() }
    }

    private fun isYoutubeUrl(url: String): Boolean = runCatching {
        val host = Uri.parse(url).host.orEmpty().lowercase()
        host.contains("youtube.com") || host.contains("youtu.be")
    }.getOrDefault(false)

    private fun notificationId(akmId: Int): Int = "akm_download_$akmId".hashCode()

    private fun ensureChannel(): String {
        val channelId = "${applicationContext.getString(R.string.app_name)}.silent"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(channelId) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(channelId, "Sinkronisasi Soal", NotificationManager.IMPORTANCE_MIN)
                )
            }
        }
        return channelId
    }

    @SuppressLint("MissingPermission")
    private fun notifyIfAllowed(id: Int, notification: android.app.Notification) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (allowed) NotificationManagerCompat.from(applicationContext).notify(id, notification)
    }

    private fun showProgress(akmId: Int, total: Int, progress: Int) {
        val channelId = ensureChannel()
        val content = "Proses sinkronisasi soal ($progress/$total)"
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sinkronisasi soal AKM")
            .setContentText(content)
            .setProgress(total.coerceAtLeast(1), progress, false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        notifyIfAllowed(notificationId(akmId), notification)
    }

    private fun showDownloadFailed(akmId: Int, message: String) {
        val channelId = ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Gagal mengunduh soal")
            .setContentText("Sinkronisasi soal gagal: $message")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        notifyIfAllowed(notificationId(akmId), notification)
    }
}
