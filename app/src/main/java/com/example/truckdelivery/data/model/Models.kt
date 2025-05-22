package com.example.truckdelivery.data.model

enum class UserType {
    TRUCK_DRIVER,
    SALES_POINT
}

data class User(
    val id: String = "",
    val email: String = "",
    val userType: UserType = UserType.SALES_POINT,
    val fcmToken: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class DeliveryRequest(
    val id: String = "",
    val userId: String = "",
    val productType: String = "",
    val quantity: Int = 0,
    val location: Location = Location(),
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val acceptedBy: String? = null,
    val acceptedAt: Long? = null,
    val completedAt: Long? = null
)

data class Location(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

data class TruckLocation(
    val truckId: String,
    val location: Location,
    val isAvailable: Boolean = true
)

data class DeliveryHistory(
    val requestId: String,
    val salesPointId: String,
    val driverId: String,
    val productType: String,
    val quantity: Int,
    val pickupLocation: Location,
    val deliveryLocation: Location,
    val startTime: Long,
    val completionTime: Long,
    val status: RequestStatus
)

// Sealed class for handling UI states
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val exception: Exception) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}

// Data class for FCM notifications
data class NotificationData(
    val title: String,
    val message: String,
    val type: NotificationType,
    val data: Map<String, String> = emptyMap()
)

enum class NotificationType {
    NEW_REQUEST,
    REQUEST_ACCEPTED,
    DRIVER_NEARBY,
    DELIVERY_COMPLETED
}
