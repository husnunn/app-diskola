package id.diskola.app.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import id.diskola.app.utils.DateUtil

/**
 * Blocking "set your clock to Otomatis" dialog, shared by Asesmen and Poin siswa. Shows only while
 * [enabled] and the device date/time or time zone is manual, clears itself once the user switches
 * back to automatic (re-checked on every ON_RESUME, e.g. returning from Settings), and "Jangan Ubah"
 * calls [onCancel] so the host screen can leave.
 */
@Composable
fun AutoTimeGate(
    title: String,
    body: String,
    onCancel: () -> Unit,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    var visible by rememberSaveable { mutableStateOf(enabled && !isTimeAutomatic(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && visible && isTimeAutomatic(context)) {
                visible = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (visible) {
        AppDialog(
            onDismiss = { visible = false },
            title = title,
            body = body,
            primaryButtonText = "Buka Pengaturan",
            onPrimaryClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_DATE_SETTINGS)) }
            },
            secondaryButtonText = "Jangan Ubah",
            onSecondaryClick = {
                visible = false
                onCancel()
            },
            dismissible = false,
        )
    }
}

private fun isTimeAutomatic(context: android.content.Context): Boolean {
    val dateUtil = DateUtil()
    return dateUtil.isTimeAutomatic(context) && dateUtil.isTimeZoneAutomatic(context)
}
