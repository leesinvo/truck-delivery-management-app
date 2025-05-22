package com.example.truckdelivery.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FirebaseRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val realtimeDb: FirebaseDatabase = FirebaseDatabase.getInstance()

    // Authentication functions
    suspend fun signUp(email: String, password: String, userType: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                // Store additional user info in Firestore
                firestore.collection("users").document(user.uid)
                    .set(mapOf(
                        "email" to email,
                        "userType" to userType,
                        "createdAt" to System.currentTimeMillis()
                    )).await()
            }
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // Delivery Request functions
    suspend fun createDeliveryRequest(
        productType: String,
        quantity: Int,
        location: Map<String, Double>
    ): Result<String> {
        return try {
            val currentUser = auth.currentUser?.uid ?: throw IllegalStateException("No user logged in")
            
            val request = hashMapOf(
                "userId" to currentUser,
                "productType" to productType,
                "quantity" to quantity,
                "location" to location,
                "status" to "PENDING",
                "createdAt" to System.currentTimeMillis()
            )

            val documentRef = firestore.collection("deliveryRequests")
                .add(request)
                .await()

            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Real-time location tracking
    fun updateTruckLocation(latitude: Double, longitude: Double) {
        val currentUser = auth.currentUser?.uid ?: return
        val locationRef = realtimeDb.getReference("truckLocations").child(currentUser)
        locationRef.setValue(mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to System.currentTimeMillis()
        ))
    }

    fun observeNearbyTrucks(radius: Double): Flow<List<Map<String, Any>>> = flow {
        // Implement geohashing or simple radius calculation
        try {
            realtimeDb.getReference("truckLocations")
                .get()
                .await()
                .children
                .mapNotNull { snapshot ->
                    snapshot.value as? Map<String, Any>
                }
                .let { emit(it) }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    // Delivery status updates
    suspend fun updateDeliveryStatus(requestId: String, status: String): Result<Unit> {
        return try {
            firestore.collection("deliveryRequests")
                .document(requestId)
                .update("status", status)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // FCM Token management
    suspend fun updateFCMToken(token: String) {
        val currentUser = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users")
                .document(currentUser)
                .update("fcmToken", token)
                .await()
        } catch (e: Exception) {
            // Handle token update failure
        }
    }

    companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_DELIVERY_REQUESTS = "deliveryRequests"
        const val COLLECTION_TRUCK_LOCATIONS = "truckLocations"
    }
}
