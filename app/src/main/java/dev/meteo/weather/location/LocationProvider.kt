package dev.meteo.weather.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Position source that never assumes which backend produced a fix.
 *
 * Attempts, in order:
 * 1. an active fix from the Google Play services fused provider — multi-source fusion, and on
 *    GrapheneOS with sandboxed Play services this is rerouted to the OS implementation;
 * 2. an active fix from the framework [LocationManager], used when Play services are absent;
 * 3. the freshest cached fix from either source, as a placeholder only.
 *
 * Quality and freshness are always read from the [Location] itself (accuracy, monotonic
 * timestamps), never from the name of the provider.
 */
class LocationProvider(private val context: Context) {

    private val manager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val fused: FusedLocationProviderClient? by lazy {
        if (playServicesAvailable()) LocationServices.getFusedLocationProviderClient(context) else null
    }

    fun hasPermission(): Boolean = granted(Manifest.permission.ACCESS_FINE_LOCATION) ||
        granted(Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** An active fix, or the freshest cached one when no provider answers within the timeout. */
    suspend fun currentFix(): LocationFix? {
        if (!hasPermission()) return null
        val active = activeFix(ACTIVE_TIMEOUT_MILLIS)
        if (active != null) return active.toFix()
        return cachedFix()
    }

    /** Freshest cached fix across both backends, or null when nothing has ever been recorded. */
    private suspend fun cachedFix(): LocationFix? {
        if (!hasPermission()) return null
        val fromFused = fused?.let { withTimeoutOrNull(CACHED_TIMEOUT_MILLIS) { it.awaitLastKnown() } }
        val fromPlatform = manager?.freshestLastKnown()
        val best = listOfNotNull(fromFused, fromPlatform).maxByOrNull { it.elapsedRealtimeNanos }
            ?: return null
        return best.toFix()
    }

    private suspend fun activeFix(timeoutMillis: Long): Location? {
        val fromFused = fused?.let { withTimeoutOrNull(timeoutMillis) { it.awaitCurrent() } }
        if (fromFused != null) return fromFused
        return withTimeoutOrNull(timeoutMillis) { manager?.awaitCurrent() }
    }

    @SuppressLint("MissingPermission") // every entry point is guarded by hasPermission()
    private suspend fun FusedLocationProviderClient.awaitCurrent(): Location? =
        suspendCancellableCoroutine { continuation ->
            val token = CancellationTokenSource()
            continuation.invokeOnCancellation { token.cancel() }
            try {
                getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            } catch (_: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun FusedLocationProviderClient.awaitLastKnown(): Location? =
        suspendCancellableCoroutine { continuation ->
            try {
                lastLocation
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            } catch (_: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun LocationManager.awaitCurrent(): Location? {
        val provider = activeProvider() ?: return null
        return suspendCancellableCoroutine { continuation ->
            // Register, take the first fix, unregister: one call that works on every supported API
            // level. The wait is bounded by `withTimeoutOrNull` at the call site.
            val holder = arrayOfNulls<LocationListener>(1)
            val listener = LocationListener { location ->
                holder[0]?.let { runCatching { removeUpdates(it) } }
                if (continuation.isActive) continuation.resume(location)
            }
            holder[0] = listener
            continuation.invokeOnCancellation {
                holder[0]?.let { runCatching { removeUpdates(it) } }
            }
            try {
                requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    /** Freshest cached fix from an enabled framework provider. */
    @SuppressLint("MissingPermission")
    private fun LocationManager.freshestLastKnown(): Location? =
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { provider -> runCatching { isProviderEnabled(provider) }.getOrDefault(false) }
            .mapNotNull { provider -> runCatching { getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.elapsedRealtimeNanos }

    private fun LocationManager.activeProvider(): String? =
        listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { provider -> runCatching { isProviderEnabled(provider) }.getOrDefault(false) }

    private fun Location.toFix(): LocationFix = LocationFix(latitude = latitude, longitude = longitude)

    private fun playServicesAvailable(): Boolean = try {
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    } catch (_: Throwable) {
        false
    }

    private companion object {
        /** Longest wait for an active fix before falling back to a cached one. */
        const val ACTIVE_TIMEOUT_MILLIS = 10_000L
        private const val CACHED_TIMEOUT_MILLIS = 2_000L
    }
}
