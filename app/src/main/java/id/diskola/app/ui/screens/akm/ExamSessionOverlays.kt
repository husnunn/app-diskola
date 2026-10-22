package id.diskola.app.ui.screens.akm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.exam.ExamSessionController
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.AkmSessionRules
import id.diskola.app.utils.AkmSessionRules.BackAction
import id.diskola.app.utils.AkmSessionRules.EndReason
import id.diskola.app.utils.AkmViolationRules

/** The one exam session of the process, provided by `MainActivity` (the screens never construct it). */
val LocalExamSession = staticCompositionLocalOf<ExamSessionController> {
    error("ExamSessionController belum disediakan oleh MainActivity")
}

/**
 * What sits above the whole exam, whichever screen it is on: the penalty dialog (§11), the "Layar Penuh Diperlukan" cover while the window
 * is not the whole screen (§10.1), and the "Mode Terkunci Terlepas" dialog (§9.3). Both are windows of their own, so they also cover — and take
 * the touches of — the top bar, and neither can be dismissed: the only ways out are closing the split screen, "Aktifkan" and, after
 * three failed tries, "Keluar dari ujian".
 */
@Composable
fun ExamSessionOverlays(controller: ExamSessionController = LocalExamSession.current) {
    val state by controller.state.collectAsStateWithLifecycle()
    // Later windows sit on top of earlier ones: the penalty is the base, the lock recovery and the full-screen cover go over it.
    state.penalty?.let { ExamPenaltyDialog(it, controller) }
    if (state.multiWindowBlocked) MultiWindowBlocker()
    val recovery = state.recovery ?: return
    AppDialog(
        onDismiss = {},
        dismissible = false,
        title = AkmSessionRules.RECOVERY_TITLE,
        body = recovery.message,
        primaryButtonText = if (recovery.checking) AkmSessionRules.RECOVERY_CHECKING else AkmSessionRules.RECOVERY_ACTIVATE,
        onPrimaryClick = controller::onRecoveryActivate,
        primaryEnabled = !recovery.checking,
        secondaryButtonText = if (recovery.canEscape && !recovery.checking) AkmSessionRules.RECOVERY_ESCAPE else null,
        onSecondaryClick = controller::onRecoveryEscape,
    )
}

/** Covers the exam completely and swallows every touch (the dialog window is modal and not cancelable) until the window is full again. */
@Composable
private fun MultiWindowBlocker() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = ScreenHorizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Box(
                    modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
                }
                Text(
                    AkmViolationRules.MULTIWINDOW_TITLE,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    AkmViolationRules.MULTIWINDOW_BODY,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * The answers are queued or sent, so the exam is handed in: the session ends here — the screen stays (it shows "Berhasil" or the
 * "waktu habis" alert), the pin and the window flags go. Covers a manual submit, the deadline and an exam found already handed in.
 */
@Composable
fun EndSessionWhenHandedIn(examId: Int, handedIn: Boolean, controller: ExamSessionController = LocalExamSession.current) {
    val state by controller.state.collectAsStateWithLifecycle()
    LaunchedEffect(handedIn, state.examId) {
        if (handedIn && state.examId == examId) controller.end(examId, EndReason.Submit)
    }
}

/** The alert shown when the student tries to leave the exam lobby (doc 06 §12.1): refused in strict mode, a warning otherwise. */
@Composable
fun ExamLeaveDialog(action: BackAction, onDismiss: () -> Unit, onLeave: () -> Unit) {
    when (action) {
        is BackAction.Refuse -> AppDialog(
            onDismiss = onDismiss,
            title = action.title,
            body = action.message,
            primaryButtonText = action.okLabel,
            onPrimaryClick = onDismiss,
        )

        is BackAction.Confirm -> AppDialog(
            onDismiss = onDismiss,
            title = action.title,
            body = action.message,
            primaryButtonText = action.stayLabel,
            onPrimaryClick = onDismiss,
            secondaryButtonText = action.leaveLabel,
            onSecondaryClick = onLeave,
        )
    }
}
