package com.example.assign6_6

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

import com.example.assign6_6.ui.theme.MapDemoTheme
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.android.gms.location.*
import android.location.Geocoder
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.Polygon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

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
                    RequestPermissions(
                        fusedLocationClient,
                        locationRequest,
                        locationCallback
                    )

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

    var addressText by remember { mutableStateOf("Loading address...") }

    // Polyline customization states
    var polylineColor by remember { mutableStateOf(Color.Red) }
    var polylineWidth by remember { mutableStateOf(8f) }

    // Polygon customization states
    var polygonFillColor by remember { mutableStateOf(Color.Green.copy(alpha = 0.3f)) }
    var polygonStrokeColor by remember { mutableStateOf(Color.Green) }
    var polygonStrokeWidth by remember { mutableStateOf(4f) }

    // Dialog states for click information
    var showPolylineDialog by remember { mutableStateOf(false) }
    var showPolygonDialog by remember { mutableStateOf(false) }

    val freedomTrail = listOf(
        LatLng(42.35505, -71.06563), // 1. Boston Common (Start)
        LatLng(42.35817, -71.06370), // 2. Massachusetts State House
        LatLng(42.35694, -71.06216), // 3. Park Street Church
        LatLng(42.35736, -71.06164), // 4. Granary Burying Ground
        LatLng(42.35755, -71.06278), // 5. King's Chapel + Burying Ground
        LatLng(42.35845, -71.06048), // 6. Benjamin Franklin Statue / First Public School
        LatLng(42.35895, -71.05768), // 7. Old Corner Bookstore
        LatLng(42.35898, -71.05684), // 8. Old South Meeting House
        LatLng(42.35876, -71.05369), // 9. Old State House
        LatLng(42.36000, -71.05509), // 10. Boston Massacre Site
        LatLng(42.36098, -71.05479), // 11. Faneuil Hall
        LatLng(42.36147, -71.05272), // 12. Paul Revere House
        LatLng(42.36368, -71.05370), // 13. Old North Church
        LatLng(42.36638, -71.05436), // 14. Copp's Hill Burying Ground
        LatLng(42.37086, -71.06352), // 15. USS Constitution / Charlestown Navy Yard
        LatLng(42.37630, -71.06167)  // 16. Bunker Hill Monument (End)
    )

    val freedomTrailNames: List<String> = listOf(
        "Boston Common",
        "Massachusetts State House",
        "Park Street Church",
        "Granary Burying Ground",
        "King's Chapel & Burying Ground",
        "First Public School / Benjamin Franklin Statue",
        "Old Corner Bookstore",
        "Old South Meeting House",
        "Old State House",
        "Boston Massacre Site",
        "Faneuil Hall",
        "Paul Revere House",
        "Old North Church",
        "Copp's Hill Burying Ground",
        "USS Constitution",
        "Bunker Hill Monument"
    )

    // Boston Public Garden polygon (park area of interest)
    val bostonPublicGarden = listOf(
        LatLng(42.35494, -71.07152),
        LatLng(42.35694, -71.07152),
        LatLng(42.35694, -71.06852),
        LatLng(42.35494, -71.06852)
    )

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

    Column {
        // Polyline Width Control
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Trail Width: ${polylineWidth.toInt()}dp")
            Slider(
                value = polylineWidth,
                onValueChange = { polylineWidth = it },
                valueRange = 2f..20f,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
        }

        // Polyline Color Control
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text("Trail Color:")
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            DropdownMenuColorSelector(selectedColor = polylineColor) {
                polylineColor = it
            }
        }

        // Polygon Stroke Width Control
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Park Border: ${polygonStrokeWidth.toInt()}dp")
            Slider(
                value = polygonStrokeWidth,
                onValueChange = { polygonStrokeWidth = it },
                valueRange = 2f..15f,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
        }

        // Polygon Color Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text("Park Fill:")
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            DropdownMenuColorSelector(selectedColor = polygonFillColor.copy(alpha = 1f)) {
                polygonFillColor = it.copy(alpha = 0.3f)
            }

            Spacer(modifier = Modifier.padding(horizontal = 8.dp))

            Text("Border:")
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            DropdownMenuColorSelector(selectedColor = polygonStrokeColor) {
                polygonStrokeColor = it
            }
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
            // Current location marker
            Marker(
                state = MarkerState(position = target),
                title = "Current Location",
                snippet = addressText
            )

            // Freedom Trail polyline with click listener
            Polyline(
                points = freedomTrail,
                color = polylineColor,
                width = polylineWidth,
                clickable = true,
                onClick = {
                    showPolylineDialog = true
                }
            )

            // Boston Public Garden polygon with click listener
            Polygon(
                points = bostonPublicGarden,
                fillColor = polygonFillColor,
                strokeColor = polygonStrokeColor,
                strokeWidth = polygonStrokeWidth,
                clickable = true,
                onClick = {
                    showPolygonDialog = true
                }
            )
        }
    }

    // Dialog for Freedom Trail information
    if (showPolylineDialog) {
        AlertDialog(
            onDismissRequest = { showPolylineDialog = false },
            title = { Text("Freedom Trail") },
            text = {
                Column {
                    Text("The Freedom Trail is a 2.5-mile-long path through Boston that passes by 16 significant historic sites.")
                    Spacer(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Total Stops: ${freedomTrailNames.size}")
                    Spacer(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Trail Length: ~2.5 miles")
                    Spacer(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Established: 1951")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPolylineDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog for Boston Public Garden information
    if (showPolygonDialog) {
        AlertDialog(
            onDismissRequest = { showPolygonDialog = false },
            title = { Text("Boston Public Garden") },
            text = {
                Column {
                    Text("The Boston Public Garden is a large public park in the heart of Boston, Massachusetts.")
                    Spacer(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Area: 24 acres")
                    Spacer(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Established: 1837")
                    Spacer(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Notable Features: Swan Boats, lagoon, Victorian-era statues, and meticulously maintained flower beds.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPolygonDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun DropdownMenuColorSelector(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Magenta, Color.Cyan, Color.Yellow)
    val colorNames = listOf("Red", "Blue", "Green", "Magenta", "Cyan", "Yellow")
    val selectedColorName = colorNames[colors.indexOf(selectedColor.copy(alpha = 1f))]

    Box {
        Button(onClick = { expanded = true }) {
            Text(text = selectedColorName)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            colors.forEachIndexed { index, color ->
                DropdownMenuItem(
                    text = { Text(colorNames[index]) },
                    onClick = {
                        onColorSelected(color)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun distanceBetween(a: LatLng, b: LatLng): Double {
    val results = FloatArray(1)
    android.location.Location.distanceBetween(
        a.latitude, a.longitude,
        b.latitude, b.longitude,
        results
    )
    return results[0].toDouble()
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