package shifa.uz.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

private val PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

// "fused" is LocationManager.FUSED_PROVIDER (API 31); getProviders only lists the ones we may use.
private val PROVIDERS = listOf("fused", LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)

@Composable
actual fun rememberLocationRequest(onResult: (DeviceLocation) -> Unit): () -> Unit {
    val context = LocalContext.current
    val onLocated by rememberUpdatedState(onResult)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            when {
                granted.values.any { it } -> locate(context, onLocated)
                // No rationale after a denial: "don't ask again".
                context.activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        PERMISSIONS[1],
                    )
                } == false ->
                    onLocated(DeviceLocation.DeniedForever)
                else -> onLocated(DeviceLocation.Denied)
            }
        }
    return {
        val granted =
            PERMISSIONS.any {
                ContextCompat.checkSelfPermission(
                    context,
                    it,
                ) == PackageManager.PERMISSION_GRANTED
            }
        if (granted) locate(context, onLocated) else launcher.launch(PERMISSIONS)
    }
}

@SuppressLint("MissingPermission") // granted before this runs
private fun locate(
    context: Context,
    onResult: (DeviceLocation) -> Unit,
) {
    val manager = context.getSystemService(LocationManager::class.java)
    if (manager == null || !LocationManagerCompat.isLocationEnabled(manager)) return onResult(DeviceLocation.Disabled)
    val enabled = manager.getProviders(true)
    val provider = PROVIDERS.firstOrNull { it in enabled } ?: return onResult(DeviceLocation.Disabled)
    // Times out on its own (about 30 s) with null; then the freshest last known fix, if any.
    LocationManagerCompat.getCurrentLocation(
        manager,
        provider,
        null as CancellationSignal?,
        ContextCompat.getMainExecutor(context),
    ) { current ->
        val location = current ?: enabled.mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
        onResult(location?.let { DeviceLocation.Found(it.latitude, it.longitude) } ?: DeviceLocation.Failed)
    }
}

private val Context.activity: Activity?
    get() = this as? Activity ?: (this as? ContextWrapper)?.baseContext?.activity
