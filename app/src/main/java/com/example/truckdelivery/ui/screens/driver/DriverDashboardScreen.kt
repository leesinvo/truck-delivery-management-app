package com.example.truckdelivery.ui.screens.driver

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.truckdelivery.data.model.DeliveryRequest
import com.example.truckdelivery.data.model.Resource
import com.example.truckdelivery.ui.components.GoogleMapView
import com.example.truckdelivery.ui.components.RequestCard
import com.google.android.gms.maps.model.LatLng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDashboardScreen(
    onLogout: () -> Unit,
    viewModel: DriverViewModel = viewModel()
) {
    var showRequestDetails by remember { mutableStateOf<DeliveryRequest?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val activeRequests by viewModel.activeRequests.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val selectedRequest by viewModel.selectedRequest.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startLocationTracking()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Driver Dashboard") },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Text("⚙") // Replace with proper icon
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Map View
            GoogleMapView(
                modifier = Modifier.fillMaxSize(),
                currentLocation = currentLocation,
                deliveryRequests = activeRequests.takeIf { it is Resource.Success }?.let {
                    (it as Resource.Success).data
                } ?: emptyList(),
                selectedRequest = selectedRequest,
                onMarkerClick = { request ->
                    viewModel.selectRequest(request)
                    showRequestDetails = request
                }
            )

            // Active Requests Panel
            if (activeRequests is Resource.Success && (activeRequests as Resource.Success).data.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    tonalElevation = 8.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Active Requests",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        (activeRequests as Resource.Success).data.take(3).forEach { request ->
                            RequestCard(
                                request = request,
                                onClick = {
                                    viewModel.selectRequest(request)
                                    showRequestDetails = request
                                },
                                onAccept = { viewModel.acceptRequest(request.id) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Request Details Bottom Sheet
            showRequestDetails?.let { request ->
                AlertDialog(
                    onDismissRequest = { showRequestDetails = null },
                    title = { Text("Request Details") },
                    text = {
                        Column {
                            Text("Product: ${request.productType}")
                            Text("Quantity: ${request.quantity}")
                            Text("Status: ${request.status}")
                            if (request.status == "PENDING") {
                                Button(
                                    onClick = {
                                        viewModel.acceptRequest(request.id)
                                        showRequestDetails = null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                ) {
                                    Text("Accept Request")
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showRequestDetails = null }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Logout Dialog
            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = { Text("Logout") },
                    text = { Text("Are you sure you want to logout?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showLogoutDialog = false
                                onLogout()
                            }
                        ) {
                            Text("Logout")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Loading Indicator
            if (activeRequests is Resource.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                )
            }
        }
    }
}
