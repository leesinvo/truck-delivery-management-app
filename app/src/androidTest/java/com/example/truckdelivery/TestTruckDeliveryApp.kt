package com.example.truckdelivery

import android.app.Application
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.example.truckdelivery.config.TestConfigReader
import com.example.truckdelivery.data.MockRepository
import com.example.truckdelivery.data.TestDatabaseInitializer
import com.example.truckdelivery.service.MockFirebaseMessagingService
import com.example.truckdelivery.service.MockLocationService
import com.example.truckdelivery.util.TestDispatcherProvider
import com.example.truckdelivery.util.TestNetworkHandler
import com.example.truckdelivery.util.TestPermissionHandler
import com.example.truckdelivery.util.TestScreenshotHandler
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.testing.CustomTestApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
@CustomTestApplication(Application::class)
class TestTruckDeliveryApp : Application() {

    private val testScope = CoroutineScope(SupervisorJob() + TestDispatcherProvider().main)
    lateinit var repository: MockRepository
        private set
    lateinit var locationClient: FusedLocationProviderClient
        private set

    override fun onCreate() {
        super.onCreate()
        runBlocking {
            setupTestEnvironment()
            setupDependencies()
            setupFirebase()
            setupNotificationChannels()
        }
    }

    private suspend fun setupTestEnvironment() {
        // Initialize test configuration
        TestConfigReader.initialize(this)

        // Initialize test data
        TestDatabaseInitializer.initializeTestData()

        // Grant required permissions
        TestPermissionHandler.grantRequiredPermissions()

        // Setup network conditions
        TestNetworkHandler.simulateNetworkCondition(
            com.example.truckdelivery.annotations.NetworkCondition.NORMAL
        )

        // Clean up old screenshots
        TestScreenshotHandler.cleanupOldScreenshots()
    }

    private fun setupDependencies() {
        // Initialize repository
        repository = MockRepository()

        // Initialize location client
        locationClient = MockFusedLocationProviderClient(this)

        // Start mock location service
        MockLocationService.startMockLocationUpdates(this)
    }

    private fun setupFirebase() {
        if (TestConfigReader.Firebase.isEmulatorEnabled) {
            val host = TestConfigReader.Firebase.emulatorHost

            // Configure Firebase emulators
            com.google.firebase.auth.FirebaseAuth.getInstance()
                .useEmulator(host, TestConfigReader.Firebase.Ports.auth)

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .useEmulator(host, TestConfigReader.Firebase.Ports.firestore)

            com.google.firebase.database.FirebaseDatabase.getInstance()
                .useEmulator(host, TestConfigReader.Firebase.Ports.database)

            com.google.firebase.storage.FirebaseStorage.getInstance()
                .useEmulator(host, TestConfigReader.Firebase.Ports.storage)

            com.google.firebase.functions.FirebaseFunctions.getInstance()
                .useEmulator(host, TestConfigReader.Firebase.Ports.functions)
        }
    }

    private fun setupNotificationChannels() {
        // Create test notification channels
        createNotificationChannel(
            "test_delivery_channel",
            "Test Delivery Notifications",
            "Notifications for delivery updates in test environment"
        )
        
        createNotificationChannel(
            "test_location_channel",
            "Test Location Updates",
            "Notifications for location updates in test environment"
        )
    }

    private fun createNotificationChannel(
        channelId: String,
        channelName: String,
        description: String
    ) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                channelName,
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = description
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) 
                as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onTerminate() {
        runBlocking {
            // Clear test data
            TestDatabaseInitializer.clearTestData()

            // Reset network conditions
            TestNetworkHandler.simulateNetworkCondition(
                com.example.truckdelivery.annotations.NetworkCondition.NORMAL
            )

            // Revoke permissions
            TestPermissionHandler.revokeAllPermissions()

            // Stop mock services
            MockLocationService.stopMockLocationUpdates(this@TestTruckDeliveryApp)
            repository.reset()
            MockFirebaseMessagingService().resetState()

            // Clear notifications
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) 
                as android.app.NotificationManager
            notificationManager.cancelAll()
        }
        super.onTerminate()
    }

    companion object {
        fun getInstance(context: Context): TestTruckDeliveryApp {
            return context.applicationContext as TestTruckDeliveryApp
        }
    }
}

/**
 * Custom test runner that uses our TestTruckDeliveryApp
 */
class TestAppJUnitRunner : AndroidJUnitRunner() {
    override fun newApplication(
        classLoader: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(
            classLoader,
            TestTruckDeliveryApp::class.java.name,
            context
        )
    }
}

/**
 * Extension function to get mock repository
 */
fun Context.getTestRepository(): MockRepository {
    return (applicationContext as TestTruckDeliveryApp).repository
}

/**
 * Extension function to get mock location client
 */
fun Context.getTestLocationClient(): FusedLocationProviderClient {
    return (applicationContext as TestTruckDeliveryApp).locationClient
}

/**
 * Mock FusedLocationProviderClient for testing
 */
class MockFusedLocationProviderClient(context: Context) : FusedLocationProviderClient(context) {
    private val mockService = MockLocationService()

    override fun getLastLocation(): com.google.android.gms.tasks.Task<android.location.Location> {
        return MockTask.successful(mockService.currentLocation.value)
    }

    override fun requestLocationUpdates(
        request: com.google.android.gms.location.LocationRequest,
        callback: com.google.android.gms.location.LocationCallback,
        looper: android.os.Looper
    ): com.google.android.gms.tasks.Task<Void> {
        // Implementation moved to MockLocationService
        return MockTask.successful(null)
    }
}

/**
 * Mock Task implementation
 */
class MockTask<T> private constructor(private val result: T?) : com.google.android.gms.tasks.Task<T>() {
    private var isComplete = true
    private var exception: Exception? = null

    override fun isComplete(): Boolean = isComplete
    override fun isCanceled(): Boolean = false
    override fun isSuccessful(): Boolean = exception == null
    override fun getResult(): T? = result
    override fun getException(): Exception? = exception

    companion object {
        fun <T> successful(result: T?): com.google.android.gms.tasks.Task<T> = MockTask(result)
        fun <T> failed(exception: Exception): com.google.android.gms.tasks.Task<T> {
            return MockTask<T>(null).apply {
                this.exception = exception
            }
        }
    }
}
