package id.diskola.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.BuildConfig
import id.diskola.app.repository.FeatureGateRepository
import id.diskola.app.viewmodel.FeatureOpenViewModel

/**
 * What [FeatureOpenViewModel.open] shows: "Memeriksa ketersediaan fitur…" while it asks, and the "Fitur Belum Tersedia" sheet (with the
 * website as the way out) when the feature is switched off. Draw it once in the screen that calls `open`.
 */
@Composable
fun FeatureGateDialogs(viewModel: FeatureOpenViewModel, isTeacher: Boolean) {
    val context = LocalContext.current
    val checking by viewModel.checking.collectAsStateWithLifecycle()
    val unavailable by viewModel.unavailable.collectAsStateWithLifecycle()
    if (checking) AppLoadingDialog(message = "Memeriksa ketersediaan fitur…")
    unavailable?.let { off ->
        FeatureUnavailableSheet(
            isTeacher = isTeacher,
            serverMessage = off.message,
            onOpenWebsite = {
                val path = when (off.name) {
                    FeatureGateRepository.PRESENSI -> "/presensi/presensi-guru-staff/presensi"
                    FeatureGateRepository.JURNAL_KBM -> "/jurnal-kbm/presensi-kelas"
                    else -> ""
                }
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PORTAL_URL + path))) }
                viewModel.dismiss()
            },
            onDismiss = viewModel::dismiss,
        )
    }
}
