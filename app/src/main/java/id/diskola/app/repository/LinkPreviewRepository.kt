package id.diskola.app.repository

import id.diskola.app.apiservice.CommonApiService
import id.diskola.app.utils.isImageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class LinkPreviewData(
    val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
)

/**
 * A dependency-free stand-in for the legacy `LinkPreview`'s Jsoup scraping (doc
 * `05-pembelajaran-materi-tugas.md` §4.5) — same idea (`og:title`/`og:image`/description meta
 * tags, `<title>` fallback), done with regex over the raw HTML instead of pulling in a new HTML
 * parser dependency for what's a handful of meta tags.
 */
private const val MAX_HTML_BYTES = 256L * 1024

class LinkPreviewRepository @Inject constructor(
    private val commonApiService: CommonApiService,
) {
    suspend fun fetch(url: String): LinkPreviewData? = withContext(Dispatchers.IO) {
        // `downloadAsString` takes `url` as a Retrofit `@Url` — a scheme-less value (bad data
        // seen in practice, e.g. a materi `link` field saved as "dev.portal.diskola.id" without
        // "https://") resolves as a RELATIVE path against our own API base URL instead of failing,
        // silently firing a nonsense request at our own backend. Guard against that explicitly.
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            return@withContext null
        }
        // A link that is itself a picture is its own preview: nothing to scrape, and downloading it as text (seen: 2 MB) is wasted.
        if (isImageFile(url)) return@withContext LinkPreviewData(url = url, title = null, description = null, imageUrl = url)
        try {
            val html = commonApiService.downloadAsString(url).use { body ->
                // Meta tags sit at the top of a page; never read more than this much of whatever the link serves.
                val source = body.source()
                source.request(MAX_HTML_BYTES)
                source.buffer.snapshot().utf8()
            }
            LinkPreviewData(
                url = url,
                title = metaContent(html, "og:title") ?: tagContent(html, "title"),
                description = metaContent(html, "description") ?: metaContent(html, "og:description"),
                imageUrl = metaContent(html, "og:image"),
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun metaContent(html: String, name: String): String? {
        val escaped = Regex.escape(name)
        val forward = Regex(
            """<meta[^>]+(?:property|name)\s*=\s*["']$escaped["'][^>]+content\s*=\s*["']([^"']*)["']""",
            RegexOption.IGNORE_CASE,
        ).find(html)?.groupValues?.get(1)
        if (!forward.isNullOrBlank()) return forward.trim()
        return Regex(
            """<meta[^>]+content\s*=\s*["']([^"']*)["'][^>]+(?:property|name)\s*=\s*["']$escaped["']""",
            RegexOption.IGNORE_CASE,
        ).find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun tagContent(html: String, tag: String): String? =
        Regex("""<$tag[^>]*>([^<]*)</$tag>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
}
