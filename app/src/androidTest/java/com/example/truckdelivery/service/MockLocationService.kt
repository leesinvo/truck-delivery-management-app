package com.example.truckdelivery.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import com.example.truckdelivery.data.MockDataProvider
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

/**
 * Mock location service for testing that simulates location updates
 */
class MockLocationService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var locationUpdateJob: Job? = null

    private val _currentLocation = MutableStateFlow(
        MockDataProvider.Locations.SAN_FRANCISCO.toAndroidLocation()
    )
    val currentLocation: StateFlow<Location> = _currentLocation

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        serviceScope.cancel()
    }

    private fun startLocationUpdates() {
        locationUpdateJob = serviceScope.launch {
            var angle = 0.0
            while (isActive) {
                // Simulate circular movement
                val newLocation = simulateMovement(angle)
                _currentLocation.value = newLocation
                angle = (angle + 10) % 360 // Increment angle by 10 degrees
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopLocationUpdates() {
        locationUpdateJob?.cancel()
        locationUpdateJob = null
    }

    private fun simulateMovement(angle: Double): Location {
        val radiusKm = 0.5 // 500 meters radius
        val baseLocation = MockDataProvider.Locations.SAN_FRANCISCO
        
        // Convert angle to radians
        val angleRad = Math.toRadians(angle)
        
        // Calculate offset
        val latOffset = (radiusKm / EARTH_RADIUS_KM) * cos(angleRad)
        val lonOffset = (radiusKm / EARTH_RADIUS_KM) * sin(angleRad) / 
                       cos(Math.toRadians(baseLocation.latitude))
        
        return com.example.truckdelivery.data.model.Location(
            latitude = baseLocation.latitude + latOffset,
            longitude = baseLocation.longitude + lonOffset
        ).toAndroidLocation()
    }

    companion object {
        private const val UPDATE_INTERVAL_MS = 1000L // 1 second
        private const val EARTH_RADIUS_KM = 6371.0

        fun startMockLocationUpdates(context: Context) {
            val intent = Intent(context, MockLocationService::class.java)
            context.startService(intent)
        }

        fun stopMockLocationUpdates(context: Context) {
            val intent = Intent(context, MockLocationService::class.java)
            context.stopService(intent)
        }
    }
}

/**
 * Mock location client for testing
 */
class MockFusedLocationClient(private val context: Context) : FusedLocationProviderClient(context) {
    private val mockService = MockLocationService()
    private val callbacks = mutableListOf<LocationCallback>()

    override fun requestLocationUpdates(
        request: LocationRequest,
        callback: LocationCallback,
        looper: Looper
    ): Task<Void> {
        callbacks.add(callback)
        
        // Start sending mock location updates
        CoroutineScope(Dispatchers.Main).launch {
            mockService.currentLocation.collect { location ->
                callback.onLocationResult(
                    LocationResult.create(listOf(location))
                )
            }
        }

        return MockTask.successful(null)
    }

    override fun removeLocationUpdates(callback: LocationCallback): Task<Void> {
        callbacks.remove(callback)
        return MockTask.successful(null)
    }

    override fun getLastLocation(): Task<Location> {
        return MockTask.successful(mockService.currentLocation.value)
    }

    fun cleanup() {
        callbacks.clear()
    }
}

/**
 * Mock Task implementation
 */
class MockTask<T> private constructor(private val result: T?) : Task<T>() {
    private var isComplete = true
    private var exception: Exception? = null

    override fun isComplete(): Boolean = isComplete
    override fun isCanceled(): Boolean = false
    override fun isSuccessful(): Boolean = exception == null
    override fun getResult(): T? = result
    override fun getException(): Exception? = exception

    companion object {
        fun <T> successful(result: T?): Task<T> = MockTask(result)
        fun <T> failed(exception: Exception): Task<T> {
            return MockTask<T>(null).apply {
                this.exception = exception
            }
        }
    }
}

/**
 * Extension functions
 */
fun com.example.truckdelivery.data.model.Location.toAndroidLocation(): Location {
    return Location("mock_provider").apply {
        latitude = this@toAndroidLocation.latitude
        longitude = this@toAndroidLocation.longitude
        time = System.currentTimeMillis()
        accuracy = 10f
        speed = 5f
        bearing = 0f
    }
}

/**
 * Mock location request builder for testing
 */
class MockLocationRequestBuilder {
    private var interval: Long = 1000
    private var priority: Int = Priority.PRIORITY_HIGH_ACCURACY

    fun setInterval(interval: Long): MockLocationRequestBuilder {
        this.interval = interval
        return this
    }

    fun setPriority(priority: Int): MockLocationRequestBuilder {
        this.priority = priority
        return this
    }

    fun build(): LocationRequest {
        return LocationRequest.Builder(interval)
            .setPriority(priority)
            .build()
    }
}
