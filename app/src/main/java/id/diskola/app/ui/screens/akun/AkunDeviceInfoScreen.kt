package id.diskola.app.ui.screens.akun

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.DeviceInfoGroup
import id.diskola.app.utils.DeviceInfoRow
import id.diskola.app.utils.DeviceInfoRules
import id.diskola.app.viewmodel.DeviceInfoViewModel
import kotlinx.coroutines.delay

/** How long the copy button says "Tersalin" before it offers to copy again. */
private const val COPIED_FEEDBACK_MILLIS = 2_000L

/**
 * "Tentang Perangkat": the app version, Android version and device identity of this phone, so a user's bug report can be traced to
 * the build and device it happened on. "Salin informasi" copies the same rows as plain text, ready to paste into a message.
 */
@Composable
fun AkunDeviceInfoScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeviceInfoViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FEEDBACK_MILLIS)
            copied = false
        }
    }

    DeviceInfoContent(
        groups = viewModel.groups,
        copied = copied,
        onCopy = { copied = copyToClipboard(context, viewModel.plainText) },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun DeviceInfoContent(
    groups: List<DeviceInfoGroup>,
    copied: Boolean,
    onCopy: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    DetailScaffold(
        title = "Tentang Perangkat",
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(scheme.surfaceContainerLowest)
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
            ) {
                AppButton(
                    text = if (copied) "Tersalin" else "Salin informasi",
                    onClick = onCopy,
                    leadingIcon = { Icon(if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy, contentDescription = null) },
                    // Below Android 13 the system shows no "copied" notice, so the button itself says it, and TalkBack reads it out.
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        },
    ) { padding ->
        // Selectable, so a single value (say the device id) can be copied on its own.
        SelectionContainer {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenHorizontalPadding)
                    .padding(top = Spacing.md, bottom = Spacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                Text(
                    "Informasi ini membantu tim Diskola menelusuri kendala. Salin lalu kirim saat kamu melaporkan masalah.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                groups.forEach { DeviceInfoSection(it) }
            }
        }
    }
}

@Composable
private fun DeviceInfoSection(group: DeviceInfoGroup, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            group.title,
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onSurface,
            modifier = Modifier.padding(start = Spacing.xs).semantics { heading() },
        )
        AppCard(variant = CardVariant.Elevated, contentPadding = PaddingValues(0.dp)) {
            Column {
                group.rows.forEachIndexed { index, row ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg), color = scheme.outlineVariant)
                    DeviceInfoLine(row)
                }
            }
        }
    }
}

/** Label on the left, value on the right: a whole group fits one screenshot, which is how most users send a report. */
@Composable
private fun DeviceInfoLine(row: DeviceInfoRow, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val missing = row.value == DeviceInfoRules.NOT_AVAILABLE
    val base = MaterialTheme.typography.bodyMedium
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Text(row.label, style = base, color = scheme.onSurfaceVariant, modifier = Modifier.weight(2f))
        Text(
            row.value,
            // An id in the body font is easy to misread (0 and O, l and 1) when someone types it out from a screenshot.
            style = base.copy(
                fontWeight = if (missing) FontWeight.Normal else FontWeight.SemiBold,
                fontFamily = if (row.monospace && !missing) FontFamily.Monospace else base.fontFamily,
            ),
            color = if (missing) scheme.onSurfaceVariant else scheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(3f),
        )
    }
}

private fun copyToClipboard(context: Context, text: String): Boolean = try {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Informasi perangkat Diskola", text))
    true
} catch (_: Exception) {
    false
}
