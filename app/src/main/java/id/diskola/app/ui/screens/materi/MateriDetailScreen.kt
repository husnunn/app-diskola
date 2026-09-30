package id.diskola.app.ui.screens.materi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.SectionLabel
import id.diskola.app.ui.navigation.Route
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing

/**
 * A single material. Every block below the hero is conditional — a material may carry only a
 * description, only a file, only a link, or any combination.
 */
@Composable
fun MateriDetailScreen(
    route: Route.MateriDetail,
    onBack: () -> Unit,
    onOpenFile: () -> Unit,
    onDownloadFile: () -> Unit,
    onOpenLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    DetailScaffold(title = "Detail Materi", onBack = onBack, modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding)
                .padding(bottom = Spacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(
                    route.title.ifBlank { "Tanpa judul" },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = scheme.onSurface,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(scheme.surfaceContainerLow, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            route.teacherInitials.ifBlank { initialsOf(route.teacher) },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = scheme.primary,
                        )
                    }
                    Column(modifier = Modifier.padding(start = Spacing.md)) {
                        Text(
                            route.teacher.ifBlank { "-" },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface,
                        )
                        val meta = listOf(route.date, route.target).filter { it.isNotBlank() }
                        if (meta.isNotEmpty()) {
                            Text(
                                meta.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (route.description.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SectionLabel("DESKRIPSI")
                    Text(
                        route.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            if (route.fileName.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SectionLabel("FILE MATERI")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
                            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
                            .padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(44.dp).background(scheme.errorContainer, DiskolaExtraShapes.iconBox),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = scheme.onErrorContainer)
                            }
                            Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
                                Text(
                                    route.fileName,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.onSurface,
                                )
                                if (route.fileSize.isNotBlank()) {
                                    Text(route.fileSize, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            AppButton(
                                text = "Lihat",
                                onClick = onOpenFile,
                                leadingIcon = { Icon(Icons.Rounded.Visibility, contentDescription = null) },
                                modifier = Modifier.weight(1f),
                            )
                            AppButton(
                                text = "Unduh",
                                onClick = onDownloadFile,
                                variant = ButtonVariant.Outlined,
                                leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            if (route.link.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SectionLabel("TAUTAN TERKAIT")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
                            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
                            .clickable(onClick = onOpenLink),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(104.dp)
                                .background(scheme.surfaceContainerLow),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.Image, contentDescription = null, tint = scheme.onSurfaceVariant)
                                Text("Pratinjau tautan", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                route.link,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = scheme.primary)
                        }
                    }
                }
            }
        }
    }
}
