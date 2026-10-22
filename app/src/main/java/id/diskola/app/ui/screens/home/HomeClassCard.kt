package id.diskola.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.diskola.app.dataclass.ResponData.CurrentSubjectData
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.components.ExpandableText
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.utils.HubRules
import id.diskola.app.utils.resolveAssetUrl

/**
 * The student's "Kelas Berlangsung" card, straight from `check-account.current_subject` (legacy `pembelajaran_page.xml`):
 * subject icon (school logo when the subject has none), name, the plot as the server sent it, and at most one button —
 * "Check In" or "Isi Jurnal". Names wrap to two lines and are then cut.
 */
@Composable
fun StudentClassCard(
    subject: CurrentSubjectData?,
    schoolLogo: String,
    action: HubRules.ClassAction,
    onCheckIn: () -> Unit,
    onIsiJurnal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    AppCard(modifier = modifier, variant = CardVariant.Elevated) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(MaterialTheme.extendedColors.success, CircleShape))
                Text(
                    "KELAS BERLANGSUNG",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.extendedColors.success,
                    maxLines = 1,
                    modifier = Modifier.padding(start = Spacing.xs),
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(scheme.surfaceContainerLow),
                    contentAlignment = Alignment.Center,
                ) {
                    val icon = subject?.subject_icon_image.orEmpty().ifBlank { schoolLogo }
                    if (icon.isNotBlank()) {
                        AsyncImage(model = resolveAssetUrl(icon), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(32.dp))
                    } else {
                        Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = scheme.primary)
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
                    Text(
                        HubRules.className(subject),
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    HubRules.classTime(subject)?.let { time ->
                        Text(time, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            when (action) {
                HubRules.ClassAction.CHECK_IN -> ClassButton("Check In", ButtonVariant.Filled, onCheckIn)
                HubRules.ClassAction.ISI_JURNAL -> ClassButton("Isi Jurnal", ButtonVariant.Tonal, onIsiJurnal)
                HubRules.ClassAction.NONE -> Unit
            }
        }
    }
}

@Composable
private fun ClassButton(text: String, variant: ButtonVariant, onClick: () -> Unit) {
    Spacer(modifier = Modifier.height(Spacing.md))
    AppButton(text = text, onClick = onClick, variant = variant, modifier = Modifier.fillMaxWidth())
}

/** The enabled "Verifikasi Data" button (guests and accounts without a class). */
@Composable
fun VerifyDataButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppButton(text = HubRules.VERIFY_LABEL, onClick = onClick, modifier = modifier.fillMaxWidth())
}

/**
 * A verification request that is in review or rejected: the server's message in a card (red when rejected). The message can be
 * long, so it shows three lines and expands on tap.
 */
@Composable
fun VerifyBlockedCard(message: String, rejected: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val container = if (rejected) scheme.errorContainer else scheme.surfaceContainerHigh
    val content = if (rejected) scheme.onErrorContainer else scheme.onSurface
    NoticeCard(
        icon = if (rejected) Icons.Rounded.Report else Icons.Rounded.HourglassTop,
        title = HubRules.VERIFY_LABEL,
        message = message,
        container = container,
        content = content,
        modifier = modifier,
    )
}

/** A student who is not in any class: legacy "Pemberitahuan" + "Anda Belum Terdaftar di Kelas Manapun, Silakan Hubungi Admin!!". */
@Composable
fun StudentNoClassCard(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    NoticeCard(
        icon = Icons.Rounded.Report,
        title = HubRules.NOTICE_TITLE,
        message = HubRules.NOT_IN_CLASS_NOTICE,
        container = scheme.surfaceContainerHigh,
        content = scheme.onSurface,
        modifier = modifier,
    )
}

@Composable
private fun NoticeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(container, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
            ExpandableText(message, style = MaterialTheme.typography.bodyMedium, color = content)
        }
    }
}
