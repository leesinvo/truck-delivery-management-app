package com.example.truckdelivery

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.StrictMode
import androidx.test.runner.AndroidJUnitRunner
import com.example.truckdelivery.config.TestConfigReader
import com.example.truckdelivery.data.TestDatabaseInitializer
import com.example.truckdelivery.util.TestNetworkHandler
import com.example.truckdelivery.util.TestPermissionHandler
import com.example.truckdelivery.util.TestScreenshotHandler
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking

/**
 * Custom test runner for TruckDelivery app instrumented tests
 */
class TruckDeliveryTestRunner : AndroidJUnitRunner() {

    override fun onCreate(arguments: Bundle) {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
        super.onCreate(arguments)
    }

    @Throws(
        ClassNotFoundException::class,
        IllegalAccessException::class,
        InstantiationException::class
    )
    override fun newApplication(
        classLoader: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(
            classLoader,
            HiltTestApplication::class.java.name,
            context
        )
    }

    override fun onStart() {
        runBlocking {
            setupTestEnvironment()
        }
        super.onStart()
    }

    override fun finish(resultCode: Int, results: Bundle) {
        runBlocking {
            cleanupTestEnvironment()
        }
        super.finish(resultCode, results)
    }

    private suspend fun setupTestEnvironment() {
        try {
            // Initialize test configuration
            TestConfigReader.initialize(targetContext)

            // Setup Firebase emulators
            setupFirebaseEmulators()

            // Initialize test database
            TestDatabaseInitializer.initializeTestData()

            // Grant required permissions
            TestPermissionHandler.grantRequiredPermissions()

            // Setup network conditions
            TestNetworkHandler.simulateNetworkCondition(
                com.example.truckdelivery.annotations.NetworkCondition.NORMAL
            )

            // Clean up old screenshots
            TestScreenshotHandler.cleanupOldScreenshots()

        } catch (e: Exception) {
            throw RuntimeException("Failed to setup test environment", e)
        }
    }

    private suspend fun cleanupTestEnvironment() {
        try {
            // Clear test data
            TestDatabaseInitializer.clearTestData()

            // Reset network conditions
            TestNetworkHandler.simulateNetworkCondition(
                com.example.truckdelivery.annotations.NetworkCondition.NORMAL
            )

            // Revoke permissions
            TestPermissionHandler.revokeAllPermissions()

        } catch (e: Exception) {
            throw RuntimeException("Failed to cleanup test environment", e)
        }
    }

    private fun setupFirebaseEmulators() {
        if (TestConfigReader.Firebase.isEmulatorEnabled) {
            val host = TestConfigReader.Firebase.emulatorHost
            
            // Setup Auth emulator
            com.google.firebase.auth.FirebaseAuth.getInstance()
                .useEmulator(host, TestConfigReader.Firebase.Ports.auth)

            // Setup Firestore emulator
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .useEmulator(host, TestConfigReader.Firebase.Ports.firestore)

            // Setup Storage emulator
            com.google.firebase.storage.FirebaseStorage.getInstance()
                .useEmulator(host, TestConfigReader.Firebase.Ports.storage)

            // Setup Functions emulator
            com.google.firebase.functions.FirebaseFunctions.getInstance()
                .useEmulator(host, TestConfigReader.Firebase.Ports.functions)

            // Setup Database emulator
            com.google.firebase.database.FirebaseDatabase.getInstance()
                .useEmulator(host, TestConfigReader.Firebase.Ports.database)
        }
    }

    companion object {
        init {
            // Enable debug logging for tests
            System.setProperty("firebase.debug.log", "true")
        }
    }
}
