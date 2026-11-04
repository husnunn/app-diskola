package id.diskola.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing

/** A picture on a full screen: pinch to zoom, drag when zoomed, "Tutup" top right. [model] is a URL or a local file. */
@Composable
fun ImagePreviewDialog(model: Any, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f))) {
            AsyncImage(
                model = model,
                contentDescription = "Gambar",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale == 1f) {
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        }
                    }
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.sm)) {
                Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = Color.White)
            }
        }
    }
}

/**
 * A stored proof picture on a detail screen: the whole picture in a bordered frame, tap → [onClick] (the full-screen preview). While it
 * loads there is a spinner; if it cannot be loaded (offline, a link that expired) the frame says so and offers "Coba lagi", which asks for
 * the picture again, instead of staying blank. The picture never leaves the app.
 */
@Composable
fun ProofImage(
    model: Any,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "File bukti",
) {
    var attempt by remember { mutableIntStateOf(0) }
    val scheme = MaterialTheme.colorScheme
    // A tinted frame with a height cap: a small picture or a tall photo sits centred in it instead of in white space or filling the screen.
    val frame = modifier
        .fillMaxWidth()
        .heightIn(max = 320.dp)
        .clip(DiskolaExtraShapes.card)
        .background(scheme.surfaceContainerLow)
        .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
    // A new key rebuilds the request, so "Coba lagi" really fetches again.
    key(attempt) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = frame,
            loading = {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = scheme.primary)
                }
            },
            error = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text("Gambar tidak dapat dimuat", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    AppButton(text = "Coba lagi", onClick = { attempt++ }, variant = ButtonVariant.Outlined)
                }
            },
            success = { SubcomposeAsyncImageContent(modifier = Modifier.clickable(onClick = onClick)) },
        )
    }
}
