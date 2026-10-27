package id.diskola.app.ui.screens.akm

import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.diskola.app.BuildConfig
import id.diskola.app.exam.ExamSessionController
import id.diskola.app.exam.ExamSessionState
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.utils.AkmPenaltyRules
import id.diskola.app.utils.AkmReportRules
import kotlinx.coroutines.delay

/**
 * The penalty dialog (doc 06 §11.2): one window, not cancelable, above whatever screen the exam is on; "Laporkan Masalah" swaps its content
 * for the report form (§13) instead of opening another dialog. It closes by itself when the penalty ends (the controller clears
 * [ExamSessionState.penalty]). A second violation only changes the cause card.
 *
 * Not in legacy: the time left is also written in the dialog — legacy only showed it in the header pill, which this dialog covers.
 */
@Composable
fun ExamPenaltyDialog(penalty: ExamSessionState.PenaltyState, controller: ExamSessionController) {
    val context = LocalContext.current
    var reporting by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("") }
    var confirmed by rememberSaveable { mutableStateOf(false) }
    var cooldown by remember { mutableIntStateOf(controller.problemReportCooldownSeconds()) }

    // The button label counts the cooldown down once a second while the form is open.
    LaunchedEffect(reporting) {
        while (reporting) {
            cooldown = controller.problemReportCooldownSeconds()
            delay(1_000)
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        ExamPenaltyContent(
            cause = penalty.cause,
            remainingLabel = AkmPenaltyRules.pillLabel(penalty.remainingMillis),
            footer = AkmPenaltyRules.footer(BuildConfig.VERSION_NAME, Build.VERSION.RELEASE, Build.VERSION.SDK_INT),
            reporting = reporting,
            message = message,
            confirmed = confirmed,
            cooldownSeconds = cooldown,
            onOpenReport = { reporting = true },
            onMessageChange = { message = AkmReportRules.clampMessage(it) },
            onConfirmedChange = { confirmed = it },
            onCancel = { reporting = false },
            onSend = {
                controller.submitProblemReport(message.trim(), confirmed)
                // Always the same words, like legacy: the report is stored on the phone and goes out when it can.
                Toast.makeText(context, AkmReportRules.SAVED_TOAST, Toast.LENGTH_LONG).show()
                message = ""
                confirmed = false
                reporting = false
            },
        )
    }
}

@Composable
internal fun ExamPenaltyContent(
    cause: String?,
    remainingLabel: String,
    footer: String,
    reporting: Boolean,
    message: String,
    confirmed: Boolean,
    cooldownSeconds: Int,
    onOpenReport: () -> Unit,
    onMessageChange: (String) -> Unit,
    onConfirmedChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A long cause or a small screen scrolls inside the dialog instead of pushing the buttons away.
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.85f).dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, DiskolaExtraShapes.dialog)
            .padding(Spacing.xxl),
    ) {
        Column(
            modifier = Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (reporting) {
                ReportSection(message, confirmed, cooldownSeconds, onMessageChange, onConfirmedChange, onCancel, onSend)
            } else {
                PenaltySection(cause, remainingLabel, footer, onOpenReport)
            }
        }
    }
}

@Composable
private fun PenaltySection(cause: String?, remainingLabel: String, footer: String, onOpenReport: () -> Unit) {
    val extended = MaterialTheme.extendedColors
    Box(
        modifier = Modifier.size(64.dp).background(extended.penaltyContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Timer, contentDescription = null, tint = extended.onPenaltyContainer, modifier = Modifier.size(32.dp))
    }
    Text(
        AkmPenaltyRules.DIALOG_TITLE,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    if (AkmPenaltyRules.showCause(cause)) {
        Text(
            cause.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = extended.onPenaltyContainer,
            modifier = Modifier
                .fillMaxWidth()
                .background(extended.penaltyContainer, DiskolaExtraShapes.card)
                .padding(Spacing.md),
        )
    }
    Text(
        AkmPenaltyRules.DIALOG_MESSAGE,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Text(
        "Sisa waktu $remainingLabel",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
        color = extended.onPenaltyContainer,
    )
    AppButton(text = AkmPenaltyRules.REPORT_LINK, onClick = onOpenReport, variant = ButtonVariant.Text, modifier = Modifier.fillMaxWidth())
    Text(
        footer,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ReportSection(
    message: String,
    confirmed: Boolean,
    cooldownSeconds: Int,
    onMessageChange: (String) -> Unit,
    onConfirmedChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit,
) {
    Text(
        AkmPenaltyRules.REPORT_LINK,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        AkmReportRules.SUBTITLE,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
    AppTextField(
        value = message,
        onValueChange = onMessageChange,
        label = AkmReportRules.LABEL,
        placeholder = AkmReportRules.HINT,
        minLines = AkmReportRules.MIN_LINES,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        AkmReportRules.counter(message),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onConfirmedChange(!confirmed) },
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(checked = confirmed, onCheckedChange = null, modifier = Modifier.padding(top = Spacing.xs))
        Text(
            AkmReportRules.CONFIRM_TEXT,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = Spacing.md, top = Spacing.sm),
        )
    }
    AppButton(
        text = AkmReportRules.sendLabel(cooldownSeconds),
        onClick = onSend,
        enabled = AkmReportRules.canSubmit(message, confirmed, cooldownSeconds),
        modifier = Modifier.fillMaxWidth(),
    )
    AppButton(text = AkmReportRules.CANCEL, onClick = onCancel, variant = ButtonVariant.Outlined, modifier = Modifier.fillMaxWidth())
}

@Preview(showBackground = true)
@Composable
private fun ExamPenaltyPreview() {
    DiskolaTheme {
        ExamPenaltyContent(
            cause = "Terdeteksi keluar dari halaman ujian atau berpindah aplikasi.\n\nPelanggaranmu telah dicatat dan dikirim ke pihak sekolah.",
            remainingLabel = "4:59",
            footer = "v1.0.0 • Android 11 (API 30)",
            reporting = false,
            message = "",
            confirmed = false,
            cooldownSeconds = 0,
            onOpenReport = {},
            onMessageChange = {},
            onConfirmedChange = {},
            onCancel = {},
            onSend = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExamReportDarkPreview() {
    DiskolaTheme(darkTheme = isSystemInDarkTheme()) {
        ExamPenaltyContent(
            cause = null,
            remainingLabel = "0:42",
            footer = "v1.0.0 • Android 11 (API 30)",
            reporting = true,
            message = "Aplikasi berhenti merespons saat saya membuka soal nomor 3.",
            confirmed = true,
            cooldownSeconds = 23,
            onOpenReport = {},
            onMessageChange = {},
            onConfirmedChange = {},
            onCancel = {},
            onSend = {},
        )
    }
}
