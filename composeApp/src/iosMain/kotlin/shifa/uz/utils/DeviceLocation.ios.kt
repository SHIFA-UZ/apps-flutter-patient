package shifa.uz.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSError
import platform.darwin.NSObject

@Composable
actual fun rememberLocationRequest(onResult: (DeviceLocation) -> Unit): () -> Unit {
    val onLocated by rememberUpdatedState(onResult)
    val request = remember { LocationRequest { onLocated(it) } }
    return request::start
}

// Remembered, because CLLocationManager holds its delegate weakly.
private class LocationRequest(
    private val onResult: (DeviceLocation) -> Unit,
) : NSObject(),
    CLLocationManagerDelegateProtocol {
    private val manager = CLLocationManager()
    private var waiting = false

    init {
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        manager.delegate = this
    }

    fun start() {
        waiting = true
        next(manager)
    }

    // Services switched off also read as denied.
    private fun next(manager: CLLocationManager) {
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
            kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> finish(DeviceLocation.DeniedForever)
            else -> manager.requestLocation()
        }
    }

    private fun finish(location: DeviceLocation) {
        if (!waiting) return
        waiting = false
        onResult(location)
    }

    // Also called once when the delegate is set, before any request.
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        if (waiting) next(manager)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>,
    ) {
        val fix =
            (didUpdateLocations.lastOrNull() as? CLLocation)?.coordinate?.useContents {
                DeviceLocation.Found(latitude, longitude)
            }
        finish(fix ?: DeviceLocation.Failed)
    }

    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError,
    ) {
        finish(DeviceLocation.Failed)
    }
}
