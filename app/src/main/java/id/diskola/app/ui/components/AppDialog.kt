package id.diskola.app.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.Motion
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.utils.AppDialogTone

/** Optional summary card under the message (a schedule, a status, a short recap) — legacy `AppDialogDetail`. */
data class AppDialogDetail(
    val title: String,
    val status: String? = null,
    val note: String? = null,
    val icon: ImageVector = Icons.Rounded.Event,
)

private val BadgeSize = 56.dp
private val BadgeIconSize = 28.dp
private val DialogMaxWidth = 400.dp
private val OptionMinHeight = 52.dp

/**
 * The app's one dialog, after legacy `widgets/AppDialog.kt` (`dialog_app_confirm.xml`): a card with a tone badge floating over its top edge,
 * then tag → title → message → detail → options → your [content], and the buttons (primary on top, secondary under it). Empty slots are
 * left out. The tone comes from [tone] or, when null, from the title like legacy (`AppDialogTone.fromTitle`): "Berhasil…" success,
 * "Gagal…" error (red primary button), "Perhatian…" warning, anything else info. [icon] replaces the tone's badge icon.
 *
 * A long title or message (a server text, say) scrolls inside the card; the buttons stay in view. Pass `dismissible = false` for a dialog
 * that only its buttons may close. A blank [primaryButtonText] hides the primary button (an options-only dialog).
 */
@Composable
fun AppDialog(
    onDismiss: () -> Unit,
    title: String,
    body: String,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    dismissible: Boolean = true,
    primaryLoading: Boolean = false,
    primaryEnabled: Boolean = true,
    tone: AppDialogTone? = null,
    tag: String? = null,
    detail: AppDialogDetail? = null,
    options: List<String> = emptyList(),
    onOptionClick: ((Int) -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = false,
        ),
    ) {
        val appear = remember { Animatable(0f) }
        LaunchedEffect(Unit) { appear.animateTo(1f, tween(Motion.BASE, easing = Motion.EaseIn)) }
        Box(
            modifier = Modifier
                .padding(horizontal = Spacing.lg)
                .graphicsLayer {
                    alpha = appear.value
                    val scale = 0.92f + 0.08f * appear.value
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center,
        ) {
            AppDialogCard(
                title = title,
                body = body,
                primaryButtonText = primaryButtonText,
                onPrimaryClick = onPrimaryClick,
                modifier = modifier,
                icon = icon,
                secondaryButtonText = secondaryButtonText,
                onSecondaryClick = onSecondaryClick,
                primaryLoading = primaryLoading,
                primaryEnabled = primaryEnabled,
                tone = tone ?: AppDialogTone.fromTitle(title),
                tag = tag,
                detail = detail,
                options = options,
                onOptionClick = onOptionClick,
                content = content,
            )
        }
    }
}

/** The card itself, without the dialog window — what [AppDialog] shows, and what the previews draw. */
@Composable
internal fun AppDialogCard(
    title: String,
    body: String,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    tone: AppDialogTone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    primaryLoading: Boolean = false,
    primaryEnabled: Boolean = true,
    tag: String? = null,
    detail: AppDialogDetail? = null,
    options: List<String> = emptyList(),
    onOptionClick: ((Int) -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val colors = toneColors(tone)
    val maxCardHeight = (LocalConfiguration.current.screenHeightDp * 0.85f).dp
    val hasPrimary = primaryButtonText.isNotBlank()
    val hasSecondary = secondaryButtonText != null && onSecondaryClick != null

    Box(modifier = modifier.widthIn(max = DialogMaxWidth).fillMaxWidth()) {
        Surface(
            modifier = Modifier.padding(top = BadgeSize / 2).fillMaxWidth(),
            shape = DiskolaExtraShapes.dialog,
            color = MaterialTheme.extendedColors.dialogSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = maxCardHeight)
                    .padding(start = Spacing.xxl, end = Spacing.xxl, top = BadgeSize / 2 + Spacing.lg, bottom = Spacing.xxl),
            ) {
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (!tag.isNullOrBlank()) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.onContainer,
                            modifier = Modifier
                                .padding(bottom = Spacing.sm)
                                .background(colors.container, CircleShape)
                                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        )
                    }
                    if (title.isNotBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().semantics { heading() },
                        )
                    }
                    if (body.isNotBlank()) {
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                        )
                    }
                    detail?.let { DetailSlot(it) }
                    if (options.isNotEmpty()) OptionList(options, onOptionClick)
                    if (content != null) {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg)) { content() }
                    }
                }
                if (hasPrimary || hasSecondary) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xxl),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        if (hasPrimary) {
                            AppButton(
                                text = primaryButtonText,
                                onClick = onPrimaryClick,
                                loading = primaryLoading,
                                enabled = primaryEnabled,
                                variant = if (tone == AppDialogTone.ERROR) ButtonVariant.Danger else ButtonVariant.Filled,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (hasSecondary) {
                            AppButton(
                                text = secondaryButtonText.orEmpty(),
                                onClick = { onSecondaryClick?.invoke() },
                                variant = ButtonVariant.Tonal,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
        // The badge overlaps the card's top edge; the card's top margin leaves it the room.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(BadgeSize)
                .shadow(8.dp, CircleShape)
                .background(colors.container, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon ?: colors.icon,
                contentDescription = null,
                tint = colors.onContainer,
                modifier = Modifier.size(BadgeIconSize),
            )
        }
    }
}

@Composable
private fun DetailSlot(detail: AppDialogDetail) {
    Column(
        modifier = Modifier
            .padding(top = Spacing.lg)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, DiskolaExtraShapes.iconBox)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, DiskolaExtraShapes.iconBox)
            .padding(Spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(detail.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(
                text = detail.title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(start = Spacing.sm),
            )
            if (!detail.status.isNullOrBlank()) {
                Text(
                    text = detail.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = Spacing.sm)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                )
            }
        }
        if (!detail.note.isNullOrBlank()) {
            Text(
                text = detail.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}

@Composable
private fun OptionList(options: List<String>, onOptionClick: ((Int) -> Unit)?) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg)) {
        options.forEachIndexed { index, label ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = OptionMinHeight)
                    .clickable(enabled = onOptionClick != null) { onOptionClick?.invoke(index) }
                    .padding(horizontal = Spacing.xs),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

private data class ToneColors(val container: Color, val onContainer: Color, val icon: ImageVector)

@Composable
private fun toneColors(tone: AppDialogTone): ToneColors {
    val extended = MaterialTheme.extendedColors
    val scheme = MaterialTheme.colorScheme
    return when (tone) {
        AppDialogTone.INFO -> ToneColors(extended.infoContainer, extended.onInfoContainer, Icons.Rounded.Info)
        AppDialogTone.SUCCESS -> ToneColors(extended.successContainer, extended.onSuccessContainer, Icons.Rounded.CheckCircle)
        AppDialogTone.WARNING -> ToneColors(extended.warningContainer, extended.onWarningContainer, Icons.Rounded.Warning)
        AppDialogTone.ERROR -> ToneColors(scheme.errorContainer, scheme.onErrorContainer, Icons.Rounded.Error)
    }
}

@Preview(name = "Warning — light", showBackground = true)
@Composable
private fun AppDialogWarningPreview() {
    DiskolaTheme {
        Box(Modifier.padding(Spacing.lg)) {
            AppDialogCard(
                title = "Perhatian",
                body = "Masih terdapat ujian yang belum diselesaikan, silahkan kumpulkan ujian terlebih dahulu agar nilai ujian terproses.",
                primaryButtonText = "Kumpulkan Sekarang",
                onPrimaryClick = {},
                secondaryButtonText = "Nanti Saja",
                onSecondaryClick = {},
                tone = AppDialogTone.fromTitle("Perhatian"),
                tag = "Asesmen",
            )
        }
    }
}

@Preview(name = "Error — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppDialogErrorDarkPreview() {
    DiskolaTheme {
        Box(Modifier.padding(Spacing.lg)) {
            AppDialogCard(
                title = "Gagal menyimpan tugas",
                body = "Tugas telah dikerjakan",
                primaryButtonText = "Mengerti",
                onPrimaryClick = {},
                tone = AppDialogTone.ERROR,
            )
        }
    }
}

@Preview(name = "Success + detail — light", showBackground = true)
@Composable
private fun AppDialogSuccessPreview() {
    DiskolaTheme {
        Box(Modifier.padding(Spacing.lg)) {
            AppDialogCard(
                title = "Berhasil Dibatalkan",
                body = "Tagihan berhasil dibatalkan",
                primaryButtonText = "Oke, Terima Kasih",
                onPrimaryClick = {},
                tone = AppDialogTone.SUCCESS,
                detail = AppDialogDetail(title = "SPP Oktober 2026", status = "Dibatalkan", note = "Saldo tidak terpotong."),
            )
        }
    }
}

@Preview(name = "Options — dark, font 1.5x", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 1.5f)
@Composable
private fun AppDialogOptionsPreview() {
    DiskolaTheme {
        Box(Modifier.padding(Spacing.lg)) {
            AppDialogCard(
                title = "Pilih Sumber Foto",
                body = "",
                primaryButtonText = "",
                onPrimaryClick = {},
                tone = AppDialogTone.INFO,
                options = listOf("Ambil dari kamera", "Pilih dari galeri"),
                onOptionClick = {},
                secondaryButtonText = "Batal",
                onSecondaryClick = {},
            )
        }
    }
}
