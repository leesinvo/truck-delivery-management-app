package com.example.truckdelivery.di

import android.content.Context
import com.example.truckdelivery.data.MockRepository
import com.example.truckdelivery.data.repository.FirebaseRepository
import com.example.truckdelivery.service.MockFirebaseMessagingService
import com.example.truckdelivery.service.MockLocationService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TestAppModule {

    private val testDispatcher = TestCoroutineDispatcher()

    @Provides
    @Singleton
    fun provideRepository(): FirebaseRepository {
        return MockRepository()
    }

    @Provides
    @Singleton
    fun provideFusedLocationClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return MockFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return object : FirebaseAuth() {
            override fun signInWithEmailAndPassword(
                email: String,
                password: String
            ): com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult> {
                return if (password == "test123") {
                    MockTask.successful(MockAuthResult())
                } else {
                    MockTask.failed(Exception("Invalid credentials"))
                }
            }

            override fun createUserWithEmailAndPassword(
                email: String,
                password: String
            ): com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult> {
                return MockTask.successful(MockAuthResult())
            }

            override fun signOut() {
                // No-op for tests
            }
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance().apply {
            useEmulator("10.0.2.2", 8080)
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging {
        return object : FirebaseMessaging() {
            override fun getToken(): com.google.android.gms.tasks.Task<String> {
                return MockTask.successful("test-fcm-token")
            }
        }
    }

    @Provides
    @Named("IO")
    fun provideIODispatcher(): CoroutineDispatcher = testDispatcher

    @Provides
    @Named("Default")
    fun provideDefaultDispatcher(): CoroutineDispatcher = testDispatcher

    @Provides
    @Named("Main")
    fun provideMainDispatcher(): CoroutineDispatcher = testDispatcher

    @Provides
    @Singleton
    fun provideLocationService(
        @ApplicationContext context: Context
    ): MockLocationService {
        return MockLocationService()
    }

    @Provides
    @Singleton
    fun provideMessagingService(
        @ApplicationContext context: Context
    ): MockFirebaseMessagingService {
        return MockFirebaseMessagingService()
    }
}

/**
 * Mock implementations for Firebase classes
 */
private class MockAuthResult : com.google.firebase.auth.AuthResult {
    override fun getUser(): com.google.firebase.auth.FirebaseUser {
        return object : com.google.firebase.auth.FirebaseUser() {
            override fun getUid(): String = "test-uid"
            override fun getEmail(): String = "test@example.com"
            // Implement other required methods
        }
    }

    override fun getAdditionalUserInfo(): com.google.firebase.auth.AdditionalUserInfo? = null
    override fun getCredential(): com.google.firebase.auth.AuthCredential? = null
}

private class MockFusedLocationProviderClient(context: Context) : FusedLocationProviderClient(context) {
    private val mockService = MockLocationService()

    override fun getLastLocation(): com.google.android.gms.tasks.Task<android.location.Location> {
        return MockTask.successful(mockService.currentLocation.value)
    }

    override fun requestLocationUpdates(
        request: com.google.android.gms.location.LocationRequest,
        callback: com.google.android.gms.location.LocationCallback,
        looper: android.os.Looper
    ): com.google.android.gms.tasks.Task<Void> {
        return MockTask.successful(null)
    }
}

/**
 * Mock Task implementation
 */
private class MockTask<T> private constructor(private val result: T?) : 
    com.google.android.gms.tasks.Task<T>() {
    
    private var isComplete = true
    private var exception: Exception? = null

    override fun isComplete(): Boolean = isComplete
    override fun isCanceled(): Boolean = false
    override fun isSuccessful(): Boolean = exception == null
    override fun getResult(): T? = result
    override fun getException(): Exception? = exception

    companion object {
        fun <T> successful(result: T?): com.google.android.gms.tasks.Task<T> = MockTask(result)
        fun <T> failed(exception: Exception): com.google.android.gms.tasks.Task<T> {
            return MockTask<T>(null).apply {
                this.exception = exception
            }
        }
    }
}
