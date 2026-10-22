package id.diskola.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import id.diskola.app.utils.WebLink

/** Icon for a file by its extension: PDF, picture, or a generic document. */
fun fileIcon(name: String): ImageVector = when (name.substringAfterLast('.', "").lowercase()) {
    "pdf" -> Icons.Rounded.PictureAsPdf
    "jpg", "jpeg", "png", "gif", "webp" -> Icons.Rounded.Image
    else -> Icons.Rounded.InsertDriveFile
}

/** Last path segment of a server file path (a "pembahasan" path is a URL, not a display name). */
fun displayNameOf(path: String): String = path.substringBefore('?').trimEnd('/').substringAfterLast('/').ifBlank { path }

/**
 * Hands a downloaded file to an installed app (the ViewModel's `openFileIntent`). When no app on the device can
 * open that type — common for .xlsx/.docx on a phone without Office — it says so and offers the system share sheet
 * instead of failing silently. [fileName] only improves the wording.
 */
@Composable
fun FileViewerHost(intent: Intent?, fileName: String?, onConsumed: () -> Unit) {
    val context = LocalContext.current
    var unopenable by remember { mutableStateOf<Intent?>(null) }

    LaunchedEffect(intent) {
        val view = intent ?: return@LaunchedEffect
        try {
            context.startActivity(view)
        } catch (e: ActivityNotFoundException) {
            unopenable = view
        }
        onConsumed()
    }

    unopenable?.let { view ->
        AppDialog(
            onDismiss = { unopenable = null },
            title = "Tidak Bisa Membuka Berkas",
            body = "Tidak ada aplikasi di perangkat ini yang bisa membuka ${fileName?.takeIf { it.isNotBlank() } ?: "berkas ini"}. " +
                "Bagikan ke aplikasi lain (mis. Google Drive atau Sheets), atau pasang aplikasi pembuka dokumen.",
            primaryButtonText = "Bagikan / Buka dengan…",
            onPrimaryClick = {
                unopenable = null
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = view.type ?: "*/*"
                    putExtra(Intent.EXTRA_STREAM, view.data)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(Intent.createChooser(send, "Buka dengan"))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "Tidak ada aplikasi yang bisa menerima berkas ini", Toast.LENGTH_LONG).show()
                }
            },
            secondaryButtonText = "Tutup",
            onSecondaryClick = { unopenable = null },
        )
    }
}

/**
 * Opens a teacher/student-entered link outside the app — YouTube and other video links go to the YouTube app or the
 * browser, which is where they play. Returns true once the other app was started; otherwise says why in a toast.
 */
fun openLink(context: Context, rawUrl: String): Boolean {
    val url = WebLink.normalize(rawUrl)
    if (url == null) {
        Toast.makeText(context, "Tautan tidak valid", Toast.LENGTH_SHORT).show()
        return false
    }
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        true
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Tidak ada aplikasi untuk membuka tautan ini", Toast.LENGTH_LONG).show()
        false
    }
}
