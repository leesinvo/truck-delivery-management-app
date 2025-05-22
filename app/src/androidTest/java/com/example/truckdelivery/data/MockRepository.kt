package com.example.truckdelivery.data

import com.example.truckdelivery.config.TestConfigReader
import com.example.truckdelivery.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Mock repository for testing that simulates Firebase operations
 */
class MockRepository {
    private val users = ConcurrentHashMap<String, User>()
    private val deliveryRequests = ConcurrentHashMap<String, DeliveryRequest>()
    private val truckLocations = ConcurrentHashMap<String, Location>()
    private val notifications = ConcurrentHashMap<String, MockDataProvider.Notification>()

    private val deliveryRequestsFlow = MutableStateFlow<Map<String, DeliveryRequest>>(emptyMap())
    private val truckLocationsFlow = MutableStateFlow<Map<String, Location>>(emptyMap())
    private val notificationsFlow = MutableStateFlow<Map<String, MockDataProvider.Notification>>(emptyMap())

    /**
     * User operations
     */
    suspend fun createUser(user: User): Result<User> = simulateNetworkCall {
        users[user.id] = user
        Result.success(user)
    }

    suspend fun getUser(userId: String): Result<User> = simulateNetworkCall {
        users[userId]?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("User not found"))
    }

    suspend fun updateUser(user: User): Result<User> = simulateNetworkCall {
        if (users.containsKey(user.id)) {
            users[user.id] = user
            Result.success(user)
        } else {
            Result.failure(NoSuchElementException("User not found"))
        }
    }

    suspend fun deleteUser(userId: String): Result<Unit> = simulateNetworkCall {
        if (users.remove(userId) != null) {
            Result.success(Unit)
        } else {
            Result.failure(NoSuchElementException("User not found"))
        }
    }

    /**
     * Delivery request operations
     */
    suspend fun createDeliveryRequest(request: DeliveryRequest): Result<DeliveryRequest> = simulateNetworkCall {
        deliveryRequests[request.id] = request
        updateDeliveryRequestsFlow()
        Result.success(request)
    }

    suspend fun getDeliveryRequest(requestId: String): Result<DeliveryRequest> = simulateNetworkCall {
        deliveryRequests[requestId]?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Delivery request not found"))
    }

    suspend fun updateDeliveryRequest(request: DeliveryRequest): Result<DeliveryRequest> = simulateNetworkCall {
        if (deliveryRequests.containsKey(request.id)) {
            deliveryRequests[request.id] = request
            updateDeliveryRequestsFlow()
            Result.success(request)
        } else {
            Result.failure(NoSuchElementException("Delivery request not found"))
        }
    }

    fun observeDeliveryRequests(): Flow<List<DeliveryRequest>> {
        return deliveryRequestsFlow.map { it.values.toList() }
    }

    fun observeDeliveryRequestsByStatus(status: RequestStatus): Flow<List<DeliveryRequest>> {
        return deliveryRequestsFlow.map { map ->
            map.values.filter { it.status == status }
        }
    }

    /**
     * Location operations
     */
    suspend fun updateDriverLocation(driverId: String, location: Location): Result<Unit> = simulateNetworkCall {
        truckLocations[driverId] = location
        updateTruckLocationsFlow()
        Result.success(Unit)
    }

    suspend fun getDriverLocation(driverId: String): Result<Location> = simulateNetworkCall {
        truckLocations[driverId]?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Driver location not found"))
    }

    fun observeDriverLocations(): Flow<List<Location>> {
        return truckLocationsFlow.map { it.values.toList() }
    }

    /**
     * Notification operations
     */
    suspend fun createNotification(notification: MockDataProvider.Notification): Result<Unit> = simulateNetworkCall {
        notifications[notification.id] = notification
        updateNotificationsFlow()
        Result.success(Unit)
    }

    fun observeNotifications(userId: String): Flow<List<MockDataProvider.Notification>> {
        return notificationsFlow.map { map ->
            map.values.filter { it.userId == userId }
        }
    }

    /**
     * Test data management
     */
    suspend fun seedTestData(data: MockDataProvider.TestDataBatch) = simulateNetworkCall {
        data.users.forEach { users[it.id] = it }
        data.requests.forEach { deliveryRequests[it.id] = it }
        data.notifications.forEach { notifications[it.id] = it }
        updateAllFlows()
        Result.success(Unit)
    }

    fun reset() {
        users.clear()
        deliveryRequests.clear()
        truckLocations.clear()
        notifications.clear()
        updateAllFlows()
    }

    private fun updateAllFlows() {
        updateDeliveryRequestsFlow()
        updateTruckLocationsFlow()
        updateNotificationsFlow()
    }

    private fun updateDeliveryRequestsFlow() {
        deliveryRequestsFlow.value = deliveryRequests.toMap()
    }

    private fun updateTruckLocationsFlow() {
        truckLocationsFlow.value = truckLocations.toMap()
    }

    private fun updateNotificationsFlow() {
        notificationsFlow.value = notifications.toMap()
    }

    /**
     * Network simulation
     */
    private suspend fun <T> simulateNetworkCall(block: suspend () -> Result<T>): Result<T> {
        simulateNetworkDelay()
        return try {
            block()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun simulateNetworkDelay() {
        // Simulate network latency between 100ms and 500ms
        kotlinx.coroutines.delay(Random.nextLong(100, 500))
    }

    private fun simulateNetworkError(): Boolean {
        // 5% chance of network error
        return Random.nextInt(100) < 5
    }

    companion object {
        private const val TAG = "MockRepository"
    }
}
