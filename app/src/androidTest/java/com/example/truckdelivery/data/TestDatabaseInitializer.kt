package com.example.truckdelivery.data

import com.example.truckdelivery.config.TestConfigReader
import com.example.truckdelivery.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.*

/**
 * Initializes test data in Firebase emulators
 */
object TestDatabaseInitializer {
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Initialize all test data
     */
    suspend fun initializeTestData() {
        withTimeout(TestConfigReader.Timeouts.databaseOperations) {
            clearTestData()
            initializeUsers()
            initializeDeliveryRequests()
            initializeTruckLocations()
        }
    }

    /**
     * Clear all test data
     */
    suspend fun clearTestData() {
        withTimeout(TestConfigReader.Timeouts.databaseOperations) {
            val collections = listOf(
                TestConfigReader.Collections.users,
                TestConfigReader.Collections.deliveryRequests,
                TestConfigReader.Collections.truckLocations,
                TestConfigReader.Collections.notifications
            )

            collections.forEach { collection ->
                val documents = firestore.collection(collection)
                    .get()
                    .await()
                    .documents

                documents.forEach { doc ->
                    doc.reference.delete().await()
                }
            }
        }
    }

    /**
     * Initialize test users
     */
    private suspend fun initializeUsers() {
        val users = listOf(
            createTestUser(TestConfigReader.TestUsers.driver),
            createTestUser(TestConfigReader.TestUsers.salesPoint),
            createTestUser(TestConfigReader.TestUsers.admin)
        )

        users.forEach { user ->
            firestore.collection(TestConfigReader.Collections.users)
                .document(user.id)
                .set(user.toMap())
                .await()
        }
    }

    /**
     * Initialize test delivery requests
     */
    private suspend fun initializeDeliveryRequests() {
        val requests = listOf(
            createPendingRequest(),
            createAcceptedRequest(),
            createInProgressRequest(),
            createCompletedRequest()
        )

        requests.forEach { request ->
            firestore.collection(TestConfigReader.Collections.deliveryRequests)
                .document(request.id)
                .set(request.toMap())
                .await()
        }
    }

    /**
     * Initialize test truck locations
     */
    private suspend fun initializeTruckLocations() {
        val locations = TestConfigReader.LocationService.mockLocations.mapIndexed { index, location ->
            TruckLocation(
                truckId = "test_truck_$index",
                location = location,
                lastUpdated = System.currentTimeMillis(),
                status = TruckStatus.AVAILABLE
            )
        }

        locations.forEach { truckLocation ->
            firestore.collection(TestConfigReader.Collections.truckLocations)
                .document(truckLocation.truckId)
                .set(truckLocation.toMap())
                .await()
        }
    }

    /**
     * Helper functions to create test data
     */
    private fun createTestUser(testUser: TestConfigReader.TestUsers.TestUser): User {
        return User(
            id = UUID.randomUUID().toString(),
            email = testUser.email,
            userType = testUser.userType,
            name = "Test ${testUser.userType.name.lowercase().capitalize()}",
            phoneNumber = "+1${(1000000000..9999999999).random()}"
        )
    }

    private fun createPendingRequest(): DeliveryRequest {
        return DeliveryRequest(
            id = "test_pending_${UUID.randomUUID()}",
            userId = TestConfigReader.TestUsers.salesPoint.email,
            productType = "Electronics",
            quantity = 5,
            location = TestConfigReader.LocationService.mockLocations.first(),
            status = RequestStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun createAcceptedRequest(): DeliveryRequest {
        return DeliveryRequest(
            id = "test_accepted_${UUID.randomUUID()}",
            userId = TestConfigReader.TestUsers.salesPoint.email,
            productType = "Groceries",
            quantity = 10,
            location = TestConfigReader.LocationService.mockLocations[1],
            status = RequestStatus.ACCEPTED,
            acceptedBy = TestConfigReader.TestUsers.driver.email,
            createdAt = System.currentTimeMillis() - 3600000 // 1 hour ago
        )
    }

    private fun createInProgressRequest(): DeliveryRequest {
        return DeliveryRequest(
            id = "test_in_progress_${UUID.randomUUID()}",
            userId = TestConfigReader.TestUsers.salesPoint.email,
            productType = "Furniture",
            quantity = 2,
            location = TestConfigReader.LocationService.mockLocations[2],
            status = RequestStatus.IN_PROGRESS,
            acceptedBy = TestConfigReader.TestUsers.driver.email,
            createdAt = System.currentTimeMillis() - 7200000 // 2 hours ago
        )
    }

    private fun createCompletedRequest(): DeliveryRequest {
        return DeliveryRequest(
            id = "test_completed_${UUID.randomUUID()}",
            userId = TestConfigReader.TestUsers.salesPoint.email,
            productType = "Books",
            quantity = 20,
            location = TestConfigReader.LocationService.mockLocations.last(),
            status = RequestStatus.COMPLETED,
            acceptedBy = TestConfigReader.TestUsers.driver.email,
            createdAt = System.currentTimeMillis() - 86400000, // 1 day ago
            completedAt = System.currentTimeMillis()
        )
    }

    /**
     * Extension functions to convert models to maps
     */
    private fun User.toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "email" to email,
        "userType" to userType.toString(),
        "name" to (name ?: ""),
        "phoneNumber" to (phoneNumber ?: "")
    )

    private fun DeliveryRequest.toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "userId" to userId,
        "productType" to productType,
        "quantity" to quantity,
        "location" to mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude
        ),
        "status" to status.toString(),
        "createdAt" to createdAt
    ).plus(acceptedBy?.let { mapOf("acceptedBy" to it) } ?: emptyMap())
     .plus(completedAt?.let { mapOf("completedAt" to it) } ?: emptyMap())

    private fun TruckLocation.toMap(): Map<String, Any> = mapOf(
        "truckId" to truckId,
        "location" to mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude
        ),
        "lastUpdated" to lastUpdated,
        "status" to status.toString()
    )
}
