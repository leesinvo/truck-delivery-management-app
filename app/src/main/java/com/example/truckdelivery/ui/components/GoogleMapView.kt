package com.example.truckdelivery.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.truckdelivery.data.model.DeliveryRequest
import com.example.truckdelivery.data.model.Location
import com.example.truckdelivery.ui.theme.DeliveryRouteColor
import com.example.truckdelivery.ui.theme.ShopMarkerColor
import com.example.truckdelivery.ui.theme.TruckMarkerColor
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.google.maps.android.ktx.addMarker
import com.google.maps.android.ktx.addPolyline
import kotlinx.coroutines.launch

@Composable
fun GoogleMapView(
    modifier: Modifier = Modifier,
    currentLocation: Location,
    deliveryRequests: List<DeliveryRequest>,
    selectedRequest: DeliveryRequest? = null,
    onMarkerClick: (DeliveryRequest) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var currentLocationMarker by remember { mutableStateOf<Marker?>(null) }
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }
    val requestMarkers = remember { mutableMapOf<String, Marker>() }

    // Lifecycle handling
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> throw IllegalStateException()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Map setup
    AndroidView(
        factory = { mapView },
        modifier = modifier
    ) { view ->
        coroutineScope.launch {
            if (googleMap == null) {
                view.getMapAsync { map ->
                    googleMap = map
                    map.apply {
                        uiSettings.apply {
                            isZoomControlsEnabled = true
                            isMyLocationButtonEnabled = true
                            isMapToolbarEnabled = true
                        }
                        
                        // Set initial camera position
                        moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(currentLocation.latitude, currentLocation.longitude),
                                15f
                            )
                        )

                        // Handle marker clicks
                        setOnMarkerClickListener { marker ->
                            val request = deliveryRequests.find { 
                                it.id == marker.tag as? String 
                            }
                            request?.let { onMarkerClick(it) }
                            true
                        }
                    }
                }
            }
        }
    }

    // Update markers and route when data changes
    LaunchedEffect(googleMap, currentLocation, deliveryRequests, selectedRequest) {
        googleMap?.let { map ->
            // Update truck location marker
            currentLocationMarker?.remove()
            currentLocationMarker = map.addMarker {
                position(LatLng(currentLocation.latitude, currentLocation.longitude))
                icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                title("Current Location")
            }

            // Update delivery request markers
            requestMarkers.values.forEach { it.remove() }
            requestMarkers.clear()
            
            deliveryRequests.forEach { request ->
                val marker = map.addMarker {
                    position(LatLng(request.location.latitude, request.location.longitude))
                    icon(BitmapDescriptorFactory.defaultMarker(
                        when (request) {
                            selectedRequest -> BitmapDescriptorFactory.HUE_GREEN
                            else -> BitmapDescriptorFactory.HUE_RED
                        }
                    ))
                    title(request.productType)
                    snippet("Quantity: ${request.quantity}")
                }
                marker?.tag = request.id
                marker?.let { requestMarkers[request.id] = it }
            }

            // Draw route to selected request
            routePolyline?.remove()
            selectedRequest?.let { request ->
                // In a real app, you would use the Directions API to get the actual route
                routePolyline = map.addPolyline {
                    add(
                        LatLng(currentLocation.latitude, currentLocation.longitude),
                        LatLng(request.location.latitude, request.location.longitude)
                    )
                    color(DeliveryRouteColor.toArgb())
                    width(5f)
                }

                // Move camera to show both points
                val bounds = LatLngBounds.builder()
                    .include(LatLng(currentLocation.latitude, currentLocation.longitude))
                    .include(LatLng(request.location.latitude, request.location.longitude))
                    .build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            }
        }
    }
}
