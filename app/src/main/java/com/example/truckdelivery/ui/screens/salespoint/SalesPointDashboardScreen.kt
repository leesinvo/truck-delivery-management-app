package com.example.truckdelivery.ui.screens.salespoint

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.truckdelivery.data.model.Resource
import com.example.truckdelivery.ui.components.GoogleMapView
import com.example.truckdelivery.ui.components.RequestCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesPointDashboardScreen(
    onLogout: () -> Unit,
    viewModel: SalesPointViewModel = viewModel()
) {
    var showRequestDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val activeRequest by viewModel.activeRequest.collectAsState()
    val nearbyTrucks by viewModel.nearbyTrucks.collectAsState()
    val requestState by viewModel.requestState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startObservingNearbyTrucks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Point Dashboard") },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Text("⚙") // Replace with proper icon
                    }
                }
            )
        },
        floatingActionButton = {
            if (activeRequest !is Resource.Success) {
                FloatingActionButton(
                    onClick = { showRequestDialog = true }
                ) {
                    Text("+")  // Replace with proper icon
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Map showing nearby trucks
            GoogleMapView(
                modifier = Modifier.fillMaxSize(),
                currentLocation = viewModel.getCurrentLocation(),
                deliveryRequests = listOf(),  // Empty since this is sales point view
                onMarkerClick = { }
            )

            // Active Request Card (if any)
            (activeRequest as? Resource.Success)?.data?.let { request ->
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Active Request",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        RequestCard(
                            request = request,
                            onClick = { /* Show details */ },
                            onAccept = { /* Not applicable for sales point */ }
                        )
                        Button(
                            onClick = { viewModel.cancelRequest(request.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Cancel Request")
                        }
                    }
                }
            }

            // New Request Dialog
            if (showRequestDialog) {
                NewRequestDialog(
                    onDismiss = { showRequestDialog = false },
                    onSubmit = { productType, quantity ->
                        viewModel.createDeliveryRequest(productType, quantity)
                        showRequestDialog = false
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

            // Loading indicator
            if (requestState is Resource.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                )
            }

            // Error snackbar
            (requestState as? Resource.Error)?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(error.exception.message ?: "An error occurred")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewRequestDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, Int) -> Unit
) {
    var productType by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Delivery Request") },
        text = {
            Column {
                OutlinedTextField(
                    value = productType,
                    onValueChange = { 
                        productType = it
                        showError = false
                    },
                    label = { Text("Product Type") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { 
                        quantity = it.filter { char -> char.isDigit() }
                        showError = false
                    },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (showError) {
                    Text(
                        "Please fill all fields",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (productType.isBlank() || quantity.isBlank()) {
                        showError = true
                        return@TextButton
                    }
                    onSubmit(productType, quantity.toInt())
                }
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
