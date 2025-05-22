package com.example.truckdelivery.data

import com.example.truckdelivery.config.TestConfigReader
import com.example.truckdelivery.data.model.*
import java.util.*
import kotlin.random.Random

/**
 * Provides mock data for testing
 */
object MockDataProvider {

    /**
     * User data generation
     */
    object Users {
        fun getTestDriver() = TestConfigReader.TestUsers.driver
        fun getTestSalesPoint() = TestConfigReader.TestUsers.salesPoint
        fun getTestAdmin() = TestConfigReader.TestUsers.admin

        fun createMockUser(
            userType: UserType = UserType.TRUCK_DRIVER,
            email: String = generateEmail(),
            id: String = UUID.randomUUID().toString()
        ): User {
            return User(
                id = id,
                email = email,
                userType = userType
            )
        }

        private fun generateEmail(): String {
            val timestamp = System.currentTimeMillis()
            return "test.user.$timestamp@example.com"
        }
    }

    /**
     * Location data generation
     */
    object Locations {
        fun getMockLocations() = TestConfigReader.LocationService.mockLocations

        fun generateRandomLocation(): Location {
            val bounds = TestConfigReader.TestData.locationBounds
            return Location(
                latitude = Random.nextDouble(
                    bounds.latitude.min,
                    bounds.latitude.max
                ),
                longitude = Random.nextDouble(
                    bounds.longitude.min,
                    bounds.longitude.max
                )
            )
        }

        fun generateLocationNearby(center: Location, radiusKm: Double = 1.0): Location {
            // Convert radius from kilometers to degrees (approximate)
            val radiusLat = radiusKm / 111.0 // 1 degree latitude = ~111 km
            val radiusLng = radiusKm / (111.0 * Math.cos(Math.toRadians(center.latitude)))

            return Location(
                latitude = center.latitude + Random.nextDouble(-radiusLat, radiusLat),
                longitude = center.longitude + Random.nextDouble(-radiusLng, radiusLng)
            )
        }
    }

    /**
     * Delivery request data generation
     */
    object DeliveryRequests {
        fun createMockRequest(
            id: String = UUID.randomUUID().toString(),
            userId: String = UUID.randomUUID().toString(),
            status: RequestStatus = RequestStatus.PENDING,
            location: Location = Locations.generateRandomLocation()
        ): DeliveryRequest {
            val testData = TestConfigReader.TestData.deliveryRequests
            return DeliveryRequest(
                id = id,
                userId = userId,
                productType = testData.productTypes.random(),
                quantity = Random.nextInt(
                    testData.quantityRange.min.toInt(),
                    testData.quantityRange.max.toInt()
                ),
                location = location,
                status = status,
                createdAt = System.currentTimeMillis(),
                acceptedBy = if (status == RequestStatus.PENDING) null else UUID.randomUUID().toString()
            )
        }

        fun createMockRequests(count: Int): List<DeliveryRequest> {
            return List(count) { createMockRequest() }
        }

        fun getValidStatusTransitions(currentStatus: RequestStatus): List<RequestStatus> {
            return TestConfigReader.TestData.deliveryRequests.statusTransitions[currentStatus.name]
                ?.map { RequestStatus.valueOf(it) }
                ?: emptyList()
        }
    }

    /**
     * Notification data generation
     */
    object Notifications {
        fun createMockNotification(
            type: String = getRandomNotificationType(),
            userId: String = UUID.randomUUID().toString()
        ): Notification {
            return Notification(
                id = UUID.randomUUID().toString(),
                userId = userId,
                type = type,
                title = getNotificationTitle(type),
                message = "Test notification message",
                timestamp = System.currentTimeMillis(),
                data = mapOf(
                    "requestId" to UUID.randomUUID().toString(),
                    "status" to RequestStatus.values().random().name
                )
            )
        }

        private fun getRandomNotificationType(): String {
            return TestConfigReader.Notifications.types.keys.random()
        }

        private fun getNotificationTitle(type: String): String {
            return TestConfigReader.Notifications.types[type] ?: "Test Notification"
        }
    }

    /**
     * Test data batch generation
     */
    object TestData {
        fun generateTestDataBatch(
            userCount: Int = 5,
            requestsPerUser: Int = 3,
            notificationsPerUser: Int = 2
        ): TestDataBatch {
            val users = List(userCount) { Users.createMockUser() }
            val requests = users.flatMap { user ->
                List(requestsPerUser) { DeliveryRequests.createMockRequest(userId = user.id) }
            }
            val notifications = users.flatMap { user ->
                List(notificationsPerUser) { Notifications.createMockNotification(userId = user.id) }
            }

            return TestDataBatch(
                users = users,
                requests = requests,
                notifications = notifications
            )
        }
    }

    data class TestDataBatch(
        val users: List<User>,
        val requests: List<DeliveryRequest>,
        val notifications: List<Notification>
    )

    data class Notification(
        val id: String,
        val userId: String,
        val type: String,
        val title: String,
        val message: String,
        val timestamp: Long,
        val data: Map<String, String>
    )
}
