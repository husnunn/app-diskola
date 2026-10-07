package id.diskola.app.ui.screens.presensi

import android.content.Context
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.OffsiteWatermark
import java.io.File
import java.time.LocalDateTime
import kotlin.coroutines.resume
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/** How a capture is taken and stored — Dinas Luar selfie vs. Jurnal KBM scene photo. */
data class CaptureCameraConfig(
    val useBackCamera: Boolean,
    val allowFlip: Boolean,
    val maxSide: Int,
    val maxBytes: Long?,
    val filePrefix: String,
    /** True: address/coordinates lines are left out when unknown (Jurnal). False: always three lines (Dinas Luar). */
    val optionalLines: Boolean,
) {
    companion object {
        /** Dinas Luar (doc `07` §6.3): front camera, no flip, 2560 px. */
        val OffsiteSelfie = CaptureCameraConfig(false, false, 2560, null, "OFFSITE", false)

        /** Jurnal KBM (doc `07` §8.3): back camera + flip, ≤ 1600 px and ≤ 2 MB. */
        val JurnalScene = CaptureCameraConfig(true, true, 1600, 2L * 1024 * 1024, "kbm_capture", true)
    }
}

/**
 * Camera with the live watermark preview (doc `07` §6.3, §8.3), no microphone. The full-resolution capture
 * is stamped and written by [OffsiteWatermark.process]; the caller receives only the finished file.
 */
@Composable
fun CaptureCameraScreen(
    address: String?,
    lat: Double?,
    lng: Double?,
    config: CaptureCameraConfig,
    onClose: () -> Unit,
    onCaptured: (path: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    var processing by remember { mutableStateOf(false) }
    var useBack by remember { mutableStateOf(config.useBackCamera) }

    fun linesAt(time: LocalDateTime): List<String> =
        if (config.optionalLines) OffsiteWatermark.linesOptional(address, lat, lng, time)
        else OffsiteWatermark.lines(address.orEmpty(), lat ?: 0.0, lng ?: 0.0, time)

    // Re-binds when the user flips; falls back to whichever camera the device has.
    LaunchedEffect(useBack) {
        val provider = context.awaitCameraProvider()
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val wanted = if (useBack) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
        val other = if (useBack) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        val selector = if (provider.hasCamera(wanted)) wanted else other
        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
    }

    val overlayLines by produceState(initialValue = linesAt(LocalDateTime.now())) {
        while (true) {
            delay(1_000)
            value = linesAt(LocalDateTime.now())
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        IconButton(onClick = onClose, modifier = Modifier.statusBarsPadding().padding(Spacing.sm).align(Alignment.TopStart)) {
            Icon(Icons.Rounded.Close, contentDescription = "Tutup kamera", tint = Color.White)
        }
        if (config.allowFlip) {
            IconButton(onClick = { useBack = !useBack }, modifier = Modifier.statusBarsPadding().padding(Spacing.sm).align(Alignment.TopEnd)) {
                Icon(Icons.Rounded.FlipCameraAndroid, contentDescription = "Ganti kamera", tint = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.66f)).padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                overlayLines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White) }
            }
            AppButton(
                text = "Ambil Foto",
                onClick = {
                    if (processing) return@AppButton
                    processing = true
                    val dir = File(context.cacheDir, "offsite_photos").apply { mkdirs() }
                    val raw = File(dir, "RAW_${System.currentTimeMillis()}.jpg")
                    imageCapture.takePicture(
                        ImageCapture.OutputFileOptions.Builder(raw).build(),
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                scope.launch {
                                    val stamped = OffsiteWatermark.process(
                                        context, raw, linesAt(LocalDateTime.now()),
                                        maxSide = config.maxSide, maxBytes = config.maxBytes, prefix = config.filePrefix,
                                    )
                                    processing = false
                                    if (stamped != null) onCaptured(stamped.path)
                                    else Toast.makeText(context, "Gagal mengambil foto", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                processing = false
                                Toast.makeText(context, "Gagal mengambil foto", Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                },
                enabled = !processing,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (processing) AppLoadingDialog(message = "Memproses foto")
}

private suspend fun Context.awaitCameraProvider(): ProcessCameraProvider = suspendCancellableCoroutine { cont ->
    val future = ProcessCameraProvider.getInstance(this)
    future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(this))
}
