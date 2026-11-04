package id.diskola.app.ui.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Says [message] once as a toast and then calls [onShown] so the owner clears it: for a notice that is not an error ("proses download …
 * akan dimulai sesaat lagi"), which legacy toasted and which must not look like a failure (the red error banner did).
 */
@Composable
fun InfoToastEffect(message: String?, onShown: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onShown()
        }
    }
}
