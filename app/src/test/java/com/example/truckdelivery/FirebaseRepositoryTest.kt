package com.example.truckdelivery

import com.example.truckdelivery.data.model.Location
import com.example.truckdelivery.data.model.UserType
import com.example.truckdelivery.data.repository.FirebaseRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FirebaseRepositoryTest {

    @Mock
    private lateinit var firebaseAuth: FirebaseAuth

    @Mock
    private lateinit var firestore: FirebaseFirestore

    @Mock
    private lateinit var authResult: AuthResult

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    @Mock
    private lateinit var authResultTask: Task<AuthResult>

    private lateinit var repository: FirebaseRepository

    @Before
    fun setup() {
        repository = FirebaseRepository()
        // Setup mock behavior here
    }

    @Test
    fun `test sign up success`() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val userType = UserType.TRUCK_DRIVER

        `when`(firebaseAuth.createUserWithEmailAndPassword(email, password))
            .thenReturn(authResultTask)
        `when`(authResultTask.isSuccessful).thenReturn(true)
        `when`(authResultTask.result).thenReturn(authResult)
        `when`(authResult.user).thenReturn(firebaseUser)
        `when`(firebaseUser.uid).thenReturn("test-uid")

        // Act
        val result = repository.signUp(email, password, userType.toString())

        // Assert
        assertTrue(result.isSuccess)
        verify(firebaseAuth).createUserWithEmailAndPassword(email, password)
    }

    @Test
    fun `test update truck location`() = runTest {
        // Arrange
        val latitude = 37.7749
        val longitude = -122.4194

        // Act
        repository.updateTruckLocation(latitude, longitude)

        // Assert
        // Verify that the correct data was written to Firebase
        // This would require mocking the Firebase Realtime Database reference
    }

    @Test
    fun `test create delivery request`() = runTest {
        // Arrange
        val productType = "Electronics"
        val quantity = 5
        val location = mapOf(
            "latitude" to 37.7749,
            "longitude" to -122.4194
        )

        // Act
        val result = repository.createDeliveryRequest(productType, quantity, location)

        // Assert
        assertTrue(result.isSuccess)
        // Verify that the correct data was written to Firestore
    }

    @Test
    fun `test get nearby trucks`() = runTest {
        // Arrange
        val currentLocation = Location(37.7749, -122.4194)
        val radiusKm = 5.0

        // Act
        repository.observeNearbyTrucks(radiusKm).collect { trucks ->
            // Assert
            assertTrue(trucks.isNotEmpty())
            // Verify that trucks are within the specified radius
            trucks.forEach { truck ->
                val distance = calculateDistance(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    truck.location.latitude,
                    truck.location.longitude
                )
                assertTrue(distance <= radiusKm)
            }
        }
    }

    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val r = 6371 // Earth's radius in kilometers

        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)

        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return r * c
    }
}
