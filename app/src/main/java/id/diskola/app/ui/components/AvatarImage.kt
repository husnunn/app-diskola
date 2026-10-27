package id.diskola.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.Spacing

/** The person icon shown when there is no name to take an initial from, relative to the avatar's [AvatarImage] size. */
private const val ICON_SIZE_RATIO = 0.45f

/**
 * A round profile photo. The photo is clipped to the circle and cropped to fill it (a landscape or portrait picture
 * is never shown letterboxed with its corners outside the frame). While the photo is loading, or when [imageUrl] is
 * blank or fails to load, the first letter of [name] is shown instead (a person icon when [name] is blank too).
 */
@Composable
fun AvatarImage(
    imageUrl: String?,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    borderWidth: Dp = 2.dp,
    /** Style of the initial shown as the fallback; pick one that suits [size]. */
    initialStyle: TextStyle = MaterialTheme.typography.titleLarge,
) {
    AvatarFrame(size = size, borderWidth = borderWidth, modifier = modifier) {
        if (imageUrl.isNullOrBlank()) {
            AvatarFallback(name = name, size = size, initialStyle = initialStyle)
        } else {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { AvatarFallback(name = name, size = size, initialStyle = initialStyle) },
                error = { AvatarFallback(name = name, size = size, initialStyle = initialStyle) },
            )
        }
    }
}

/** The circle every [AvatarImage] state is drawn in: clips its content and keeps the border above it, flush with the edge. */
@Composable
private fun AvatarFrame(
    size: Dp,
    borderWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .clip(DiskolaExtraShapes.avatar)
            .background(scheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        content()
        // Drawn last: the photo fills the whole circle and would otherwise cover a border set on the frame itself.
        Box(modifier = Modifier.fillMaxSize().border(borderWidth, scheme.outlineVariant, DiskolaExtraShapes.avatar))
    }
}

@Composable
private fun AvatarFallback(name: String, size: Dp, initialStyle: TextStyle) {
    val initial = name.trim().firstOrNull()?.uppercase().orEmpty()
    if (initial.isNotEmpty()) {
        Text(initial, style = initialStyle, color = MaterialTheme.colorScheme.primary)
    } else {
        Icon(
            Icons.Rounded.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size * ICON_SIZE_RATIO),
        )
    }
}

// ---- Previews ----

/** A stand-in photo of a given shape (diagonal gradient plus a cross) so Preview can show how a non-square picture is cropped. */
private class PreviewPhotoPainter(width: Float, height: Float, private val from: Color, private val to: Color, private val cross: Color) : Painter() {
    override val intrinsicSize: Size = Size(width, height)

    override fun DrawScope.onDraw() {
        drawRect(Brush.linearGradient(listOf(from, to), start = Offset.Zero, end = Offset(this.size.width, this.size.height)))
        drawLine(cross, Offset(0f, 0f), Offset(this.size.width, this.size.height), strokeWidth = 6f)
        drawLine(cross, Offset(this.size.width, 0f), Offset(0f, this.size.height), strokeWidth = 6f)
    }
}

@Composable
private fun PreviewPhoto(width: Float, height: Float) {
    val scheme = MaterialTheme.colorScheme
    AvatarFrame(size = 84.dp, borderWidth = 2.dp) {
        Image(
            painter = PreviewPhotoPainter(width, height, scheme.primary, scheme.tertiary, scheme.surface),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AvatarImagePhotoShapesPreview() {
    DiskolaTheme {
        Row(modifier = Modifier.padding(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            PreviewPhoto(width = 200f, height = 200f)
            PreviewPhoto(width = 400f, height = 200f)
            PreviewPhoto(width = 200f, height = 400f)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AvatarImageNoPhotoPreview() {
    DiskolaTheme {
        Row(modifier = Modifier.padding(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            AvatarImage(imageUrl = null, name = "Budi Santoso", size = 84.dp)
            AvatarImage(imageUrl = "", name = "", size = 84.dp)
        }
    }
}
