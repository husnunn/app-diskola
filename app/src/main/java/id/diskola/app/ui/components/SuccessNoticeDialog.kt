package id.diskola.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.AppDialogTone
import id.diskola.app.utils.ContentNotices
import id.diskola.app.utils.SuccessNotice

/**
 * The "it worked" dialog after a teacher saved or deleted something: [AppDialog] in the success tone with one "Ok" button. The tone is
 * passed, not read from the title, because these titles start with the noun ("Tugas Berhasil Dihapus"), which `AppDialogTone.fromTitle`
 * would call info. Tapping outside or pressing back counts as "Ok", so a teacher is never stuck behind it; what [onConfirm] does (close the
 * form, clear the notice) is the same either way.
 */
@Composable
fun SuccessNoticeDialog(notice: SuccessNotice, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    AppDialog(
        onDismiss = onConfirm,
        title = notice.title,
        body = notice.body,
        primaryButtonText = "Ok",
        onPrimaryClick = onConfirm,
        modifier = modifier,
        tone = AppDialogTone.SUCCESS,
    )
}

@Preview(name = "Tugas diposting — light", showBackground = true)
@Composable
private fun SuccessNoticeLightPreview() {
    DiskolaTheme {
        Box(Modifier.padding(Spacing.lg)) {
            val notice = ContentNotices.tugasSaved(isEdit = false)
            AppDialogCard(title = notice.title, body = notice.body, primaryButtonText = "Ok", onPrimaryClick = {}, tone = AppDialogTone.SUCCESS)
        }
    }
}

@Preview(name = "Materi dihapus — dark, font 1.5x", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 1.5f)
@Composable
private fun SuccessNoticeDarkLargeFontPreview() {
    DiskolaTheme {
        Box(Modifier.padding(Spacing.lg)) {
            val notice = ContentNotices.materiDeleted()
            AppDialogCard(title = notice.title, body = notice.body, primaryButtonText = "Ok", onPrimaryClick = {}, tone = AppDialogTone.SUCCESS)
        }
    }
}
