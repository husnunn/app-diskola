package id.diskola.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.components.ExpandableText
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.HubRules

/**
 * What teachers and staff see where students see their class: today's attendance status (legacy `pembelajaran_attendance_banner.xml`
 * + `PresensiViewModel.resolveAttendanceBannerState`). While the two requests are out a quiet placeholder keeps the height, so
 * the menu below does not jump. Event names, sources and the info text can be long: they are bounded and expand on tap.
 */
@Composable
fun AttendanceBannerCard(
    banner: HubRules.AttendanceBanner?,
    loading: Boolean,
    onPresensi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (banner == null) {
        if (loading) BannerPlaceholder(modifier)
        return
    }
    val scheme = MaterialTheme.colorScheme
    AppCard(modifier = modifier, variant = CardVariant.Elevated) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(40.dp).background(scheme.surfaceContainerLow, DiskolaExtraShapes.iconBox),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(bannerIcon(banner.state), contentDescription = null, tint = scheme.primary, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        banner.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (banner.workHours.isNotBlank()) {
                        Text(banner.workHours, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (banner.eventName.isNotBlank()) {
                        ExpandableText("Agenda · ${banner.eventName}", MaterialTheme.typography.labelMedium, scheme.onSurfaceVariant, collapsedLines = 2)
                    }
                    if (banner.source.isNotBlank()) {
                        Text(banner.source, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (banner.info.isNotBlank()) {
                ExpandableText(
                    banner.info,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(scheme.surfaceContainerLow, DiskolaExtraShapes.card)
                        .padding(Spacing.md),
                )
            }
            if (banner.showCta) {
                AppButton(text = HubRules.CTA_PRESENSI, onClick = onPresensi, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BannerPlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, DiskolaExtraShapes.card),
    )
}

private fun bannerIcon(state: HubRules.BannerState) = when (state) {
    HubRules.BannerState.CHECK_IN -> Icons.AutoMirrored.Rounded.Login
    HubRules.BannerState.CHECK_OUT -> Icons.AutoMirrored.Rounded.Logout
    HubRules.BannerState.OUTSIDE_HOURS -> Icons.Rounded.Schedule
    HubRules.BannerState.COMPLETE -> Icons.Rounded.TaskAlt
    HubRules.BannerState.NO_AGENDA, HubRules.BannerState.HOLIDAY -> Icons.Rounded.EventBusy
}
