package com.example.truckdelivery

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.truckdelivery.data.repository.FirebaseRepository

class TruckDeliveryApp : Application() {
    private val applicationScope = CoroutineScope(Dispatchers.Default)
    private lateinit var repository: FirebaseRepository

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        repository = FirebaseRepository()

        // Create notification channels
        createNotificationChannels()

        // Initialize FCM
        initializeFCM()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delivery requests channel
            val deliveryChannel = NotificationChannel(
                CHANNEL_DELIVERY_REQUESTS,
                "Delivery Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new delivery requests"
                enableLights(true)
                enableVibration(true)
            }

            // Location tracking channel
            val locationChannel = NotificationChannel(
                CHANNEL_LOCATION_TRACKING,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background location tracking notification"
            }

            // General notifications channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            }

            // Register the channels
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(
                listOf(deliveryChannel, locationChannel, generalChannel)
            )
        }
    }

    private fun initializeFCM() {
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Update FCM token in Firestore
                applicationScope.launch {
                    task.result?.let { token ->
                        repository.updateFCMToken(token)
                    }
                }
            }
        }

        // Set up FCM auto-init
        Firebase.messaging.isAutoInitEnabled = true
    }

    companion object {
        const val CHANNEL_DELIVERY_REQUESTS = "delivery_requests"
        const val CHANNEL_LOCATION_TRACKING = "location_tracking"
        const val CHANNEL_GENERAL = "general_notifications"
    }
}
