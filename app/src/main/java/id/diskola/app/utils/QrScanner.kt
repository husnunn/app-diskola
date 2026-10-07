package id.diskola.app.utils

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * QR scan through Google Play services' code scanner: one call, QR only, no preview UI or camera
 * permission of our own. Replaces the legacy zxing `IntentIntegrator` + `PortraitCaptureActivity`.
 */
object QrScanner {

    sealed interface Result {
        data class Scanned(val raw: String) : Result
        data object Cancelled : Result
        data class Failed(val message: String) : Result
    }

    suspend fun scan(context: Context): Result {
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        val scanner = GmsBarcodeScanning.getClient(context, options)
        return suspendCancellableCoroutine { cont ->
            scanner.startScan()
                .addOnSuccessListener { if (cont.isActive) cont.resume(Result.Scanned(it.rawValue.orEmpty())) }
                .addOnCanceledListener { if (cont.isActive) cont.resume(Result.Cancelled) }
                .addOnFailureListener { if (cont.isActive) cont.resume(Result.Failed(it.message ?: "Pemindai QR tidak tersedia")) }
        }
    }
}
