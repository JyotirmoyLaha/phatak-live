package com.phataklive

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.phataklive.ui.MainScreen
import com.phataklive.ui.PhatakTheme

/**
 * Permission flow: the app explains why it needs location (onboarding screen) before the system dialog,
 * asks only for location (precise or approximate, both work), tells "denied" from "denied permanently"
 * and then offers Settings, and re-checks when the user comes back from Settings.
 */
class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private var askedThisSession = false

    private val askLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        val granted = res[Manifest.permission.ACCESS_FINE_LOCATION] == true || res[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        vm.onLocationAccess(
            when {
                granted -> LocationAccess.GRANTED
                // after a refusal Android only shows the dialog again while a rationale is allowed
                !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> LocationAccess.BLOCKED
                else -> LocationAccess.DENIED
            },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PhatakTheme {
                MainScreen(
                    vm,
                    onRequestLocation = ::requestLocation,
                    onOpenAppSettings = {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
                    },
                    onOpenLocationSettings = { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // also catches a permission granted in Settings or GPS switched on while the app was in the background
        if (hasLocation()) vm.onLocationAccess(LocationAccess.GRANTED)
        vm.onForeground(true)
    }

    override fun onPause() {
        vm.onForeground(false)
        super.onPause()
    }

    private fun hasLocation() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun requestLocation() {
        askedThisSession = true
        askLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
}
