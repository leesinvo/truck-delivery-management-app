package com.example.truckdelivery.ui.screens.driver

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.truckdelivery.data.model.DeliveryRequest
import com.example.truckdelivery.data.model.Location
import com.example.truckdelivery.data.model.Resource
import com.example.truckdelivery.data.repository.FirebaseRepository
import com.example.truckdelivery.service.LocationTrackingService
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DriverViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FirebaseRepository()

    private val _activeRequests = MutableStateFlow<Resource<List<DeliveryRequest>>>(Resource.Loading)
    val activeRequests: StateFlow<Resource<List<DeliveryRequest>>> = _activeRequests.asStateFlow()

    private val _currentLocation = MutableStateFlow(Location())
    val currentLocation: StateFlow<Location> = _currentLocation.asStateFlow()

    private val _selectedRequest = MutableStateFlow<DeliveryRequest?>(null)
    val selectedRequest: StateFlow<DeliveryRequest?> = _selectedRequest.asStateFlow()

    private val _deliveryStatus = MutableStateFlow<Resource<Unit>>(Resource.Success(Unit))
    val deliveryStatus: StateFlow<Resource<Unit>> = _deliveryStatus.asStateFlow()

    init {
        observeActiveRequests()
        observeCurrentLocation()
    }

    private fun observeActiveRequests() {
        viewModelScope.launch {
            try {
                repository.observePendingRequests().collect { requests ->
                    _activeRequests.value = Resource.Success(requests)
                }
            } catch (e: Exception) {
                _activeRequests.value = Resource.Error(e)
            }
        }
    }

    private fun observeCurrentLocation() {
        viewModelScope.launch {
            repository.observeCurrentTruckLocation().collect { location ->
                _currentLocation.value = location
            }
        }
    }

    fun startLocationTracking() {
        LocationTrackingService.startService(getApplication())
    }

    fun stopLocationTracking() {
        LocationTrackingService.stopService(getApplication())
    }

    fun selectRequest(request: DeliveryRequest?) {
        _selectedRequest.value = request
    }

    fun acceptRequest(requestId: String) {
        viewModelScope.launch {
            _deliveryStatus.value = Resource.Loading
            try {
                repository.acceptDeliveryRequest(requestId).fold(
                    onSuccess = {
                        _deliveryStatus.value = Resource.Success(Unit)
                        // Refresh active requests
                        observeActiveRequests()
                    },
                    onFailure = { exception ->
                        _deliveryStatus.value = Resource.Error(exception)
                    }
                )
            } catch (e: Exception) {
                _deliveryStatus.value = Resource.Error(e)
            }
        }
    }

    fun completeDelivery(requestId: String) {
        viewModelScope.launch {
            _deliveryStatus.value = Resource.Loading
            try {
                repository.completeDelivery(requestId).fold(
                    onSuccess = {
                        _deliveryStatus.value = Resource.Success(Unit)
                        // Clear selected request and refresh active requests
                        _selectedRequest.value = null
                        observeActiveRequests()
                    },
                    onFailure = { exception ->
                        _deliveryStatus.value = Resource.Error(exception)
                    }
                )
            } catch (e: Exception) {
                _deliveryStatus.value = Resource.Error(e)
            }
        }
    }

    fun updateLocation(location: Location) {
        viewModelScope.launch {
            try {
                repository.updateTruckLocation(location.latitude, location.longitude)
            } catch (e: Exception) {
                // Handle location update error
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationTracking()
    }
}
