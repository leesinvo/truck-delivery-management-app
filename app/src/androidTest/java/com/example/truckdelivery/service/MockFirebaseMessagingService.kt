package com.example.truckdelivery.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.truckdelivery.MainActivity
import com.example.truckdelivery.R
import com.example.truckdelivery.data.MockDataProvider
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * Mock Firebase messaging service for testing notifications
 */
class MockFirebaseMessagingService : FirebaseMessagingService() {

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        private const val CHANNEL_ID = "test_channel"
        private const val CHANNEL_NAME = "Test Channel"
        private val notificationId = AtomicInteger(0)

        // StateFlows for testing
        private val _lastMessage = MutableStateFlow<RemoteMessage?>(null)
        val lastMessage: StateFlow<RemoteMessage?> = _lastMessage

        private val _lastToken = MutableStateFlow<String?>(null)
        val lastToken: StateFlow<String?> = _lastToken

        // Test notification data
        object NotificationTypes {
            const val NEW_REQUEST = "new_request"
            const val REQUEST_ACCEPTED = "request_accepted"
            const val DRIVER_NEARBY = "driver_nearby"
            const val DELIVERY_COMPLETED = "delivery_completed"
        }

        // Helper method to create test notifications
        fun createTestNotification(type: String): RemoteMessage {
            val data = when (type) {
                NotificationTypes.NEW_REQUEST -> mapOf(
                    "type" to NotificationTypes.NEW_REQUEST,
                    "requestId" to MockDataProvider.DeliveryRequests.PENDING_REQUEST.id,
                    "productType" to MockDataProvider.DeliveryRequests.PENDING_REQUEST.productType,
                    "quantity" to MockDataProvider.DeliveryRequests.PENDING_REQUEST.quantity.toString()
                )
                NotificationTypes.REQUEST_ACCEPTED -> mapOf(
                    "type" to NotificationTypes.REQUEST_ACCEPTED,
                    "requestId" to MockDataProvider.DeliveryRequests.ACCEPTED_REQUEST.id,
                    "driverId" to MockDataProvider.Users.DRIVER.id,
                    "driverName" to MockDataProvider.Users.DRIVER.name
                )
                NotificationTypes.DRIVER_NEARBY -> mapOf(
                    "type" to NotificationTypes.DRIVER_NEARBY,
                    "requestId" to MockDataProvider.DeliveryRequests.IN_PROGRESS_REQUEST.id,
                    "estimatedArrival" to "5 minutes"
                )
                NotificationTypes.DELIVERY_COMPLETED -> mapOf(
                    "type" to NotificationTypes.DELIVERY_COMPLETED,
                    "requestId" to MockDataProvider.DeliveryRequests.COMPLETED_REQUEST.id
                )
                else -> mapOf("type" to "unknown")
            }

            return RemoteMessage.Builder("test@firebase.com")
                .setData(data)
                .build()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        _lastMessage.value = message
        
        // Process the message based on type
        when (message.data["type"]) {
            NotificationTypes.NEW_REQUEST -> handleNewRequest(message)
            NotificationTypes.REQUEST_ACCEPTED -> handleRequestAccepted(message)
            NotificationTypes.DRIVER_NEARBY -> handleDriverNearby(message)
            NotificationTypes.DELIVERY_COMPLETED -> handleDeliveryCompleted(message)
        }
    }

    override fun onNewToken(token: String) {
        _lastToken.value = token
    }

    private fun handleNewRequest(message: RemoteMessage) {
        val requestId = message.data["requestId"]
        val productType = message.data["productType"]
        val quantity = message.data["quantity"]

        showNotification(
            title = "New Delivery Request",
            body = "Product: $productType, Quantity: $quantity",
            intent = createMainActivityIntent()
        )
    }

    private fun handleRequestAccepted(message: RemoteMessage) {
        val driverName = message.data["driverName"]
        
        showNotification(
            title = "Request Accepted",
            body = "Driver $driverName has accepted your request",
            intent = createMainActivityIntent()
        )
    }

    private fun handleDriverNearby(message: RemoteMessage) {
        val estimatedArrival = message.data["estimatedArrival"]
        
        showNotification(
            title = "Driver Nearby",
            body = "Your delivery will arrive in approximately $estimatedArrival",
            intent = createMainActivityIntent()
        )
    }

    private fun handleDeliveryCompleted(message: RemoteMessage) {
        showNotification(
            title = "Delivery Completed",
            body = "Your delivery has been completed successfully",
            intent = createMainActivityIntent()
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Test notification channel"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        intent: PendingIntent
    ) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(intent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(notificationId.incrementAndGet(), notification)
    }

    private fun createMainActivityIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Test helper methods
    fun clearNotifications() {
        notificationManager.cancelAll()
    }

    fun resetState() {
        _lastMessage.value = null
        _lastToken.value = null
        clearNotifications()
    }
}
