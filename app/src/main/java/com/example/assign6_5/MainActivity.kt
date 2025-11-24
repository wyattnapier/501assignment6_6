package com.example.assign6_5

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.google.maps.android.compose.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition

import com.example.assign6_5.ui.theme.MapDemoTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.android.gms.location.*
import android.location.Geocoder
import androidx.compose.runtime.mutableStateListOf

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val lastKnownLocation = mutableStateOf<Location?>(null)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // Update Jetpack Compose state, not setContent()
                    lastKnownLocation.value = location
                }
            }
        }

        setContent {
            MapDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Start permissions + location updates
                    RequestPermissions(
                        fusedLocationClient,
                        locationRequest,
                        locationCallback
                    )

                    // Only show the map when we have a location
                    lastKnownLocation.value?.let { loc ->
                        MainScreen(loc)
                    }
                }
            }
        }

    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

@Composable
fun RequestPermissions(
    fusedLocationClient: FusedLocationProviderClient,
    locationRequest: LocationRequest,
    locationCallback: LocationCallback
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasLocationPermission = isGranted
        if (isGranted) {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            } catch (e: SecurityException) {
                Log.e("Location", "Security Exception: ${e.message}")
            }
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            } catch (e: SecurityException) {
                Log.e("Location", "Security Exception: ${e.message}")
            }
        }
    }
}

@Composable
fun MainScreen(location: Location) {
    val context = LocalContext.current

    // Holds the human-readable address for current location
    var addressText by remember { mutableStateOf("Loading address...") }

    // Reverse geocode user's current location
    LaunchedEffect(location) {
        val geocoder = Geocoder(context)
        try {
            val results = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!results.isNullOrEmpty()) {
                addressText = results[0].getAddressLine(0)
            } else {
                addressText = "Address unavailable"
            }
        } catch (e: Exception) {
            addressText = "Geocoder error"
        }
    }

    val customMarkers = remember { mutableStateListOf<LatLng>() }

    val target = LatLng(location.latitude, location.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(target, 14f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapType = MapType.SATELLITE),
        uiSettings = MapUiSettings(zoomControlsEnabled = false),

        onMapClick = { latLng ->
            customMarkers.add(latLng)
        }
    ) {
        // Default marker for user's current location
        Marker(
            state = MarkerState(position = target),
            title = "Current Location",
            snippet = addressText
        )

        // Draw all custom markers placed by user
        customMarkers.forEach { latLng ->
            Marker(
                state = MarkerState(latLng),
                title = "Custom Marker",
                snippet = "${latLng.latitude}, ${latLng.longitude}"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapPreview() {
    val testLocation: Location = Location("").apply {
        latitude = 42.3555
        longitude = -71.0565
    }

    MapDemoTheme {
        MainScreen(testLocation)
    }
}