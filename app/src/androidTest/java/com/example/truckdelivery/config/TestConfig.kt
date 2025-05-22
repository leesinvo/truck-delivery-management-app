package com.example.truckdelivery.config

import com.example.truckdelivery.data.model.DeliveryRequest
import com.example.truckdelivery.data.model.Location
import com.example.truckdelivery.data.model.RequestStatus
import com.example.truckdelivery.data.model.User
import com.example.truckdelivery.data.model.UserType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Configuration and mock data for tests
 */
object TestConfig {
    // Test timeouts
    const val DEFAULT_TIMEOUT = 5000L // 5 seconds
    const val NETWORK_TIMEOUT = 10000L // 10 seconds
    const val LOCATION_TIMEOUT = 3000L // 3 seconds

    // Test Firebase paths
    const val TEST_USERS_COLLECTION = "users_test"
    const val TEST_REQUESTS_COLLECTION = "delivery_requests_test"
    const val TEST_LOCATIONS_COLLECTION = "truck_locations_test"

    // Test user credentials
    object TestCredentials {
        const val DRIVER_EMAIL = "test.driver@example.com"
        const val SALES_EMAIL = "test.sales@example.com"
        const val TEST_PASSWORD = "Test123!"
        const val INVALID_PASSWORD = "wrong_password"
    }

    // Test locations
    object TestLocations {
        val SAN_FRANCISCO = Location(
            latitude = 37.7749,
            longitude = -122.4194
        )
        val NEW_YORK = Location(
            latitude = 40.7128,
            longitude = -74.0060
        )
        val LONDON = Location(
            latitude = 51.5074,
            longitude = -0.1278
        )
    }

    // Mock users
    object MockUsers {
        val DRIVER = User(
            id = "test_driver_id",
            email = TestCredentials.DRIVER_EMAIL,
            userType = UserType.TRUCK_DRIVER
        )

        val SALES_POINT = User(
            id = "test_sales_id",
            email = TestCredentials.SALES_EMAIL,
            userType = UserType.SALES_POINT
        )
    }

    // Mock delivery requests
    object MockRequests {
        val PENDING_REQUEST = DeliveryRequest(
            id = "test_pending_request",
            userId = MockUsers.SALES_POINT.id,
            productType = "Electronics",
            quantity = 5,
            location = TestLocations.SAN_FRANCISCO,
            status = RequestStatus.PENDING
        )

        val IN_PROGRESS_REQUEST = DeliveryRequest(
            id = "test_in_progress_request",
            userId = MockUsers.SALES_POINT.id,
            productType = "Groceries",
            quantity = 10,
            location = TestLocations.NEW_YORK,
            status = RequestStatus.IN_PROGRESS,
            acceptedBy = MockUsers.DRIVER.id
        )
    }

    /**
     * Test environment setup and cleanup
     */
    object TestSetup {
        suspend fun setupTestEnvironment() {
            cleanupTestData()
            createTestUsers()
            createTestDeliveryRequests()
        }

        suspend fun cleanupTestData() {
            val firestore = FirebaseFirestore.getInstance()
            
            // Clean up test collections
            listOf(
                TEST_USERS_COLLECTION,
                TEST_REQUESTS_COLLECTION,
                TEST_LOCATIONS_COLLECTION
            ).forEach { collection ->
                try {
                    firestore.collection(collection)
                        .get()
                        .await()
                        .documents
                        .forEach { doc ->
                            doc.reference.delete().await()
                        }
                } catch (e: Exception) {
                    println("Error cleaning up collection $collection: ${e.message}")
                }
            }

            // Sign out any existing user
            FirebaseAuth.getInstance().signOut()
        }

        private suspend fun createTestUsers() {
            val firestore = FirebaseFirestore.getInstance()
            
            // Create test driver
            createTestUser(MockUsers.DRIVER, TestCredentials.DRIVER_EMAIL)
            
            // Create test sales point
            createTestUser(MockUsers.SALES_POINT, TestCredentials.SALES_EMAIL)
        }

        private suspend fun createTestUser(user: User, email: String) {
            try {
                // Create auth user
                val authResult = FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, TestCredentials.TEST_PASSWORD)
                    .await()

                // Store user data in Firestore
                FirebaseFirestore.getInstance()
                    .collection(TEST_USERS_COLLECTION)
                    .document(authResult.user?.uid ?: throw Exception("User ID not found"))
                    .set(user)
                    .await()
            } catch (e: Exception) {
                println("Error creating test user ${user.email}: ${e.message}")
            }
        }

        private suspend fun createTestDeliveryRequests() {
            val firestore = FirebaseFirestore.getInstance()
            
            try {
                // Create pending request
                firestore.collection(TEST_REQUESTS_COLLECTION)
                    .document(MockRequests.PENDING_REQUEST.id)
                    .set(MockRequests.PENDING_REQUEST)
                    .await()

                // Create in-progress request
                firestore.collection(TEST_REQUESTS_COLLECTION)
                    .document(MockRequests.IN_PROGRESS_REQUEST.id)
                    .set(MockRequests.IN_PROGRESS_REQUEST)
                    .await()
            } catch (e: Exception) {
                println("Error creating test delivery requests: ${e.message}")
            }
        }
    }
}
