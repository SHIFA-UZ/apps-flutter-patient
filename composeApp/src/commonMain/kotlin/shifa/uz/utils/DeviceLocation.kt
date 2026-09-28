package shifa.uz.utils

import androidx.compose.runtime.Composable
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sqrt

/** One location fix, or why there's none. */
sealed interface DeviceLocation {
    data class Found(val latitude: Double, val longitude: Double) : DeviceLocation {
        /** 01 `location_utils`: Haversine, in km. */
        fun distanceKm(
            latitude: Double,
            longitude: Double,
        ): Double {
            val p = 0.017453292519943295 // π / 180
            val a =
                0.5 - cos((latitude - this.latitude) * p) / 2 +
                    cos(this.latitude * p) * cos(latitude * p) * (1 - cos((longitude - this.longitude) * p)) / 2
            return EARTH_DIAMETER_KM * asin(sqrt(a))
        }
    }

    data object Disabled : DeviceLocation

    data object Denied : DeviceLocation

    data object DeniedForever : DeviceLocation

    data object Failed : DeviceLocation
}

private const val EARTH_DIAMETER_KM = 12742

/** 07 §2.1 Near me: asks for the permission when needed, then one medium-accuracy fix. */
@Composable
expect fun rememberLocationRequest(onResult: (DeviceLocation) -> Unit): () -> Unit
