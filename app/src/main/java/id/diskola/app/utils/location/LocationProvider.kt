package id.diskola.app.utils.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * One-shot GPS fix + reverse geocoding for Agenda Mingguan's check-in/out. The caller must already
 * hold a location permission (fine *or* coarse — legacy demanded both, which treated a user who
 * granted only "approximate" as denied).
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(context) }

    /** A fresh high-accuracy fix, falling back to the last known one; null if neither is available. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Location? {
        val token = CancellationTokenSource()
        return try {
            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).awaitOrNull()
                ?: fused.lastLocation.awaitOrNull()
        } catch (e: SecurityException) {
            Timber.e(e)
            null
        } finally {
            token.cancel()
        }
    }

    /** A brand-new fix (no cached position allowed) — what a submit must send, never a stale one. */
    @SuppressLint("MissingPermission")
    suspend fun freshLocation(): Location? {
        val token = CancellationTokenSource()
        return try {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(0)
                .build()
            fused.getCurrentLocation(request, token.token).awaitOrNull()
        } catch (e: SecurityException) {
            Timber.e(e)
            null
        } finally {
            token.cancel()
        }
    }

    /** Continuous high-accuracy fixes (Masuk sekolah's live map). Collecting starts them, cancelling stops them. */
    @SuppressLint("MissingPermission")
    fun locationUpdates(intervalMs: Long = 10_000L): Flow<Location> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        try {
            fused.requestLocationUpdates(request, callback, android.os.Looper.getMainLooper())
        } catch (e: SecurityException) {
            Timber.e(e)
            close()
        }
        awaitClose { fused.removeLocationUpdates(callback) }
    }

    fun isLocationEnabled(): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    /**
     * Null when the device's location settings already satisfy a high-accuracy request; otherwise the
     * system "turn on location" dialog as an [IntentSender] to launch (null too when it can't be fixed
     * in-app, in which case [isLocationEnabled] tells the caller what to show).
     */
    suspend fun settingsResolution(): IntentSender? {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L).build()
        val settings = LocationSettingsRequest.Builder().addLocationRequest(request).build()
        return suspendCancellableCoroutine { cont ->
            LocationServices.getSettingsClient(context).checkLocationSettings(settings)
                .addOnSuccessListener { if (cont.isActive) cont.resume(null) }
                .addOnFailureListener { e ->
                    if (cont.isActive) cont.resume((e as? ResolvableApiException)?.resolution?.intentSender)
                }
        }
    }

    /** First address line in id-ID, or null when the geocoder has nothing or fails. */
    suspend fun addressOf(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale("id", "ID"))
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            cont.resume(addresses.firstOrNull()?.getAddressLine(0))
                        }

                        // Without this the coroutine never resumes and the UI sticks on "Mencari alamat…".
                        override fun onError(errorMessage: String?) {
                            cont.resume(null)
                        }
                    })
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()?.getAddressLine(0)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }
}

private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { if (cont.isActive) cont.resume(it) }
    addOnFailureListener { if (cont.isActive) cont.resume(null) }
    addOnCanceledListener { if (cont.isActive) cont.resume(null) }
}

/** Straight-line distance in meters (what the radius check compares against). */
fun distanceMeters(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): Float {
    val out = FloatArray(1)
    Location.distanceBetween(fromLat, fromLng, toLat, toLng, out)
    return out[0]
}
