package com.example.truckdelivery.ui.screens.salespoint

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.truckdelivery.data.model.DeliveryRequest
import com.example.truckdelivery.data.model.Location
import com.example.truckdelivery.data.model.Resource
import com.example.truckdelivery.data.model.TruckLocation
import com.example.truckdelivery.data.repository.FirebaseRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SalesPointViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FirebaseRepository()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _activeRequest = MutableStateFlow<Resource<DeliveryRequest>>(Resource.Loading)
    val activeRequest: StateFlow<Resource<DeliveryRequest>> = _activeRequest.asStateFlow()

    private val _nearbyTrucks = MutableStateFlow<Resource<List<TruckLocation>>>(Resource.Loading)
    val nearbyTrucks: StateFlow<Resource<List<TruckLocation>>> = _nearbyTrucks.asStateFlow()

    private val _requestState = MutableStateFlow<Resource<Unit>>(Resource.Success(Unit))
    val requestState: StateFlow<Resource<Unit>> = _requestState.asStateFlow()

    private var currentLocation: Location? = null

    init {
        observeActiveRequest()
        updateCurrentLocation()
    }

    private fun observeActiveRequest() {
        viewModelScope.launch {
            try {
                repository.observeActiveRequest().collect { request ->
                    _activeRequest.value = Resource.Success(request)
                }
            } catch (e: Exception) {
                _activeRequest.value = Resource.Error(e)
            }
        }
    }

    fun startObservingNearbyTrucks() {
        viewModelScope.launch {
            try {
                repository.observeNearbyTrucks(NEARBY_RADIUS_KM).collect { trucks ->
                    _nearbyTrucks.value = Resource.Success(trucks)
                }
            } catch (e: Exception) {
                _nearbyTrucks.value = Resource.Error(e)
            }
        }
    }

    private fun updateCurrentLocation() {
        viewModelScope.launch {
            try {
                val location = fusedLocationClient.lastLocation.await()
                location?.let {
                    currentLocation = Location(
                        latitude = it.latitude,
                        longitude = it.longitude
                    )
                }
            } catch (e: Exception) {
                // Handle location error
            }
        }
    }

    fun getCurrentLocation(): Location {
        return currentLocation ?: Location() // Return default location if not available
    }

    fun createDeliveryRequest(productType: String, quantity: Int) {
        viewModelScope.launch {
            _requestState.value = Resource.Loading
            try {
                val location = currentLocation ?: throw IllegalStateException("Location not available")
                repository.createDeliveryRequest(
                    productType = productType,
                    quantity = quantity,
                    location = mapOf(
                        "latitude" to location.latitude,
                        "longitude" to location.longitude
                    )
                ).fold(
                    onSuccess = {
                        _requestState.value = Resource.Success(Unit)
                        // Request created successfully, it will be picked up by observeActiveRequest
                    },
                    onFailure = { exception ->
                        _requestState.value = Resource.Error(exception)
                    }
                )
            } catch (e: Exception) {
                _requestState.value = Resource.Error(e)
            }
        }
    }

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            _requestState.value = Resource.Loading
            try {
                repository.updateDeliveryStatus(requestId, "CANCELLED").fold(
                    onSuccess = {
                        _requestState.value = Resource.Success(Unit)
                        // Request cancelled successfully, it will be picked up by observeActiveRequest
                    },
                    onFailure = { exception ->
                        _requestState.value = Resource.Error(exception)
                    }
                )
            } catch (e: Exception) {
                _requestState.value = Resource.Error(e)
            }
        }
    }

    companion object {
        private const val NEARBY_RADIUS_KM = 10.0 // 10km radius for nearby trucks
    }
}
