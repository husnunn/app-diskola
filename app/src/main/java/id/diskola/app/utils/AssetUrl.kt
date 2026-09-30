package id.diskola.app.utils

import id.diskola.app.BuildConfig
import java.io.File

/**
 * Normalizes a possibly-relative asset path (question/answer image, media url) from the API into
 * an absolute URL on the separate assets CDN host. Confirmed real: `local.properties` defines
 * `ASSETS_URL_DEV`/`ASSETS_URL_PROD` distinct from `API_URL_*` (now wired into `BuildConfig` in
 * `app/build.gradle.kts`), and the legacy app always does exactly this before ever fetching an
 * asset (`AkmDownloader.kt`: `if (!question.image.startsWith("http")) question.image =
 * BuildConfig.ASSETS_URL + "/" + question.image`) — this repo's `AkmUiModels.toAkmQuestion()`
 * skipped that step entirely, so a relative `image`/`filePath` never resolved to anything Coil
 * could fetch and silently rendered blank.
 *
 * Already-absolute URLs and local file paths (from `AkmDownloadWorker`) pass through untouched —
 * re-resolving a local path here would corrupt it. A relative *server* path could coincidentally
 * also start with `/` (root-relative), so the local-path check is `File(...).exists()` rather than
 * a bare prefix test — a worker-downloaded file always exists by construction, whereas a
 * server-relative path essentially never coincides with a real absolute path on this device.
 */
fun resolveAssetUrl(pathOrUrl: String): String {
    if (pathOrUrl.isBlank() || pathOrUrl.startsWith("http")) return pathOrUrl
    if (pathOrUrl.startsWith("/") && runCatching { File(pathOrUrl).exists() }.getOrDefault(false)) return pathOrUrl
    return BuildConfig.ASSETS_URL.trimEnd('/') + "/" + pathOrUrl.trimStart('/')
}
