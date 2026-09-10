package id.app.education.ui.screens.materi

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import id.app.education.ui.components.AppButton
import id.app.education.ui.components.AppCard
import id.app.education.ui.components.ButtonVariant
import id.app.education.ui.navigation.Route
import id.app.education.ui.theme.DiskolaExtraShapes
import id.app.education.ui.theme.Spacing

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun MateriDetailScreen(
    route: Route.MateriDetail,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cleanDescription = route.description.trim().replace("--deskripsi--", "").trim()
    val validLinks = route.links.filter { it.isNotBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Materi") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null) }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            AppCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = route.subjectIcon.ifBlank { null },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(40.dp).clip(DiskolaExtraShapes.iconBox),
                        )
                        Text(
                            text = route.subjectName.ifBlank { "-" }.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier
                                .padding(start = Spacing.md)
                                .clip(DiskolaExtraShapes.chipBadge)
                                .background(MaterialTheme.colorScheme.secondary)
                                .padding(horizontal = Spacing.md, vertical = 4.dp),
                        )
                    }
                    Spacer(modifier = Modifier.size(Spacing.md))
                    Text(route.title.ifBlank { "Tanpa Judul" }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
                    LabelValueRow("Guru", route.teacher.ifBlank { "-" })
                    Spacer(modifier = Modifier.size(Spacing.xs))
                    LabelValueRow("Tanggal", route.date.ifBlank { "-" })
                }
            }

            if (cleanDescription.isNotBlank()) {
                AppCard {
                    Column {
                        Text("Deskripsi", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                        Text(cleanDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            if (route.filePath.isNotBlank()) {
                AppCard {
                    Column {
                        Text("Lampiran", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    route.fileName.ifBlank { "Lihat File" },
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                                val sizeKb = route.fileSize.toLongOrNull()
                                val sizeLabel = when {
                                    sizeKb == null -> ""
                                    sizeKb >= 1024 -> "%.1f MB".format(sizeKb / 1024.0)
                                    else -> "$sizeKb KB"
                                }
                                Text(
                                    listOf(route.fileType, sizeLabel).filter { it.isNotBlank() }.joinToString(" • "),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            AppButton(
                                text = "Buka",
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(route.filePath))) },
                                variant = ButtonVariant.Outlined,
                                modifier = Modifier.width(96.dp),
                            )
                        }
                    }
                }
            }

            if (validLinks.isNotEmpty()) {
                AppCard {
                    Column {
                        Text("Tautan", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                        validLinks.forEach { link ->
                            Text(
                                text = link,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(vertical = Spacing.xs)
                                    .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
