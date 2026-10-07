package id.diskola.app.ui.screens.materi

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.ErrorState
import id.diskola.app.ui.theme.Spacing
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val RENDER_WIDTH_PX = 1080

/**
 * A simple internal PDF viewer — `android.graphics.pdf.PdfRenderer` rendering each page into a
 * `LazyColumn`, scrolled like the legacy app's page list but without its custom pinch-zoom engine
 * (decision: simple viewer, doc `05-pembelajaran-materi-tugas.md` §4.1/§4.2/§4.6). The legacy
 * page-jump controls were dead code (listeners commented out) and are dropped rather than ported.
 */
@Composable
fun PdfViewerScreen(
    filePath: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var renderer by remember(filePath) { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember(filePath) { mutableIntStateOf(0) }
    var openFailed by remember(filePath) { mutableStateOf(false) }
    val renderMutex = remember(filePath) { Mutex() }

    DisposableEffect(filePath) {
        var pfd: ParcelFileDescriptor? = null
        try {
            pfd = ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY)
            val opened = PdfRenderer(pfd)
            renderer = opened
            pageCount = opened.pageCount
        } catch (e: Exception) {
            openFailed = true
        }
        onDispose {
            renderer?.close()
            pfd?.close()
        }
    }

    DetailScaffold(title = title.ifBlank { "Baca PDF" }, onBack = onBack, modifier = modifier) { padding ->
        when {
            openFailed -> ErrorState(
                title = "Gagal membuka berkas",
                description = "Berkas PDF tidak dapat dibaca. Coba unduh ulang.",
                modifier = Modifier.padding(padding),
            )
            pageCount == 0 -> Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.padding(Spacing.xl))
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                items(pageCount) { pageIndex ->
                    PdfPage(renderer = renderer, pageIndex = pageIndex, mutex = renderMutex)
                }
            }
        }
    }
}

@Composable
private fun PdfPage(renderer: PdfRenderer?, pageIndex: Int, mutex: Mutex) {
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var aspect by remember(pageIndex) { mutableStateOf(0.75f) }

    LaunchedEffect(pageIndex, renderer) {
        val currentRenderer = renderer ?: return@LaunchedEffect
        val rendered = withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val page = currentRenderer.openPage(pageIndex)
                    val height = (page.height.toFloat() / page.width * RENDER_WIDTH_PX).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(RENDER_WIDTH_PX, height, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val ratio = page.width.toFloat() / page.height
                    page.close()
                    bmp to ratio
                } catch (e: Exception) {
                    null
                }
            }
        }
        if (rendered != null) {
            bitmap = rendered.first
            aspect = rendered.second
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .padding(horizontal = Spacing.xs),
    ) {
        val current = bitmap
        if (current != null) {
            Image(bitmap = current.asImageBitmap(), contentDescription = "Halaman ${pageIndex + 1}", modifier = Modifier.fillMaxWidth())
        } else {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(aspect)) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(Spacing.xl),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
