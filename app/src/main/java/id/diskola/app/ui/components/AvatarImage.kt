package id.diskola.app.ui.components

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.Spacing

/** The person icon shown when there is no name to take an initial from, relative to the avatar's [AvatarImage] size. */
private const val ICON_SIZE_RATIO = 0.45f

/** Space kept on each side of a logo, relative to the avatar's size: what is left (70%) fits in the square inside the circle. */
private const val LOGO_INSET_RATIO = 0.15f

/**
 * A round profile picture, clipped to the circle. A photo is cropped to fill it (a landscape or portrait picture is
 * never shown letterboxed with its corners outside the frame). A logo is shown whole, inside the circle: a user without
 * a photo gets the school's default picture, the Diskola logo, which cropping would cut at its tips. While the picture
 * is loading, or when [imageUrl] is blank or fails to load, the first letter of [name] is shown instead (a person icon
 * when [name] is blank too).
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
            val context = LocalContext.current
            // A software bitmap, so its corners can be read to tell a logo from a photo.
            val request = remember(imageUrl) { ImageRequest.Builder(context).data(imageUrl).allowHardware(false).build() }
            SubcomposeAsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { AvatarFallback(name = name, size = size, initialStyle = initialStyle) },
                error = { AvatarFallback(name = name, size = size, initialStyle = initialStyle) },
                success = { state ->
                    val logo = remember(state.result.drawable) { state.result.drawable.isLogo() }
                    AvatarPicture(painter = painter, logo = logo, size = size)
                },
            )
        }
    }
}

/** How a loaded picture sits in the circle: a photo is cropped to fill it, a logo is shown whole within it. */
@Composable
private fun AvatarPicture(painter: Painter, logo: Boolean, size: Dp) {
    Image(
        painter = painter,
        contentDescription = null,
        contentScale = if (logo) ContentScale.Fit else ContentScale.Crop,
        modifier = Modifier.fillMaxSize().then(if (logo) Modifier.padding(size * LOGO_INSET_RATIO) else Modifier),
    )
}

/** A logo rather than a photo: a picture with see-through corners (a photo, even a PNG one, is opaque edge to edge). */
private fun Drawable.isLogo(): Boolean {
    val bitmap = (this as? BitmapDrawable)?.bitmap ?: return false
    if (!bitmap.hasAlpha()) return false
    val right = bitmap.width - 1
    val bottom = bitmap.height - 1
    return runCatching {
        listOf(0 to 0, right to 0, 0 to bottom, right to bottom).any { (x, y) -> bitmap.getPixel(x, y) ushr 24 < 0xFF }
    }.getOrDefault(false)
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

/**
 * Centred in a box of its own: Coil's loading and error slots hand their content the whole circle as its minimum size,
 * which would put a bare Text in the top-left corner, outside the circle.
 */
@Composable
private fun AvatarFallback(name: String, size: Dp, initialStyle: TextStyle) {
    val initial = name.trim().firstOrNull()?.uppercase().orEmpty()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
}

// ---- Previews ----

/**
 * A stand-in picture of a given shape so Preview can show how it is fitted: a photo (diagonal gradient plus a cross,
 * edge to edge) or a logo (a diamond touching every edge, see-through corners, like the Diskola logo).
 */
private class PreviewPhotoPainter(
    width: Float,
    height: Float,
    private val logo: Boolean,
    private val from: Color,
    private val to: Color,
    private val cross: Color,
) : Painter() {
    override val intrinsicSize: Size = Size(width, height)

    override fun DrawScope.onDraw() {
        val brush = Brush.linearGradient(listOf(from, to), start = Offset.Zero, end = Offset(this.size.width, this.size.height))
        if (logo) {
            val diamond = Path().apply {
                moveTo(this@onDraw.size.width / 2, 0f)
                lineTo(this@onDraw.size.width, this@onDraw.size.height / 2)
                lineTo(this@onDraw.size.width / 2, this@onDraw.size.height)
                lineTo(0f, this@onDraw.size.height / 2)
                close()
            }
            drawPath(diamond, brush)
        } else {
            drawRect(brush)
            drawLine(cross, Offset(0f, 0f), Offset(this.size.width, this.size.height), strokeWidth = 6f)
            drawLine(cross, Offset(this.size.width, 0f), Offset(0f, this.size.height), strokeWidth = 6f)
        }
    }
}

@Composable
private fun PreviewPhoto(width: Float, height: Float, logo: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    AvatarFrame(size = 84.dp, borderWidth = 2.dp) {
        AvatarPicture(
            painter = PreviewPhotoPainter(width, height, logo, scheme.primary, scheme.tertiary, scheme.surface),
            logo = logo,
            size = 84.dp,
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
            PreviewPhoto(width = 526f, height = 474f, logo = true)
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
