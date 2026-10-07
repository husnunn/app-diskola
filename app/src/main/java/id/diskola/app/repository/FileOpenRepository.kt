package id.diskola.app.repository

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import id.diskola.app.apiservice.CommonApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * "Baca"/"Download" for Materi files (also reusable for Tugas/Poin later, doc
 * `05-pembelajaran-materi-tugas.md` §4.3) — a Compose-friendly rebuild of the legacy
 * `IntentUtil.openFile`/`downloadFile` pair. Streams via [CommonApiService.download] (never
 * `.bytes()`, which loads the whole file into memory) and never requests a storage permission —
 * doc §4.3 is explicit that the legacy app doesn't either, and both destinations here
 * (`filesDir`, `getExternalFilesDir`) are app-scoped and permission-free on every supported API
 * level (27+).
 */
class FileOpenRepository @Inject constructor(
    private val commonApiService: CommonApiService,
    @ApplicationContext private val context: Context,
) {
    /** Downloads to the app's private `filesDir` (overwriting any previous copy by the same
     * name, matching legacy) so it can be opened via [openFileIntent]. */
    suspend fun downloadToAppFiles(url: String, suggestedFileName: String): File = withContext(Dispatchers.IO) {
        // `download` takes `url` as a Retrofit `@Url` — a scheme-less value (bad data seen in
        // practice, e.g. a materi file/explanation path saved without "https://") resolves as a
        // RELATIVE path against our own API base URL instead of failing, silently firing a
        // nonsense request at our own backend. Fail fast instead — the caller already catches and
        // shows "Gagal membuka file".
        require(url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
            "Invalid file URL: $url"
        }
        val fileName = Uri.parse(url).lastPathSegment?.takeIf { it.isNotBlank() } ?: suggestedFileName
        val file = File(context.filesDir, fileName)
        val body = commonApiService.download(url)
        body.byteStream().use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        file
    }

    fun mimeTypeFor(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    fun isPdf(fileName: String): Boolean = fileName.substringAfterLast('.', "").lowercase() == "pdf"

    /** `ACTION_VIEW` for any non-PDF file, via this app's own `FileProvider` — PDFs go through
     * `PdfViewerScreen` instead (doc §4.1/§4.6), never through this. */
    fun openFileIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeTypeFor(file.name))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** `DownloadManager` to `getExternalFilesDir(DIRECTORY_DOWNLOADS)` — doc §4.3.
     * @return whether the download was actually enqueued — `false` for a scheme-less `url` (bad
     * data seen in practice), since `DownloadManager.Request` throws `IllegalArgumentException`
     * synchronously for that instead of failing asynchronously like a normal network error. */
    fun enqueueDownload(url: String, fileName: String): Boolean {
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            return false
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(fileName)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }
        manager.enqueue(request)
        return true
    }
}
