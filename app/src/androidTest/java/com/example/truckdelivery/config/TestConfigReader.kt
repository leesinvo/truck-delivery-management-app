package com.example.truckdelivery.config

import android.content.Context
import com.example.truckdelivery.data.model.Location
import com.example.truckdelivery.data.model.UserType
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Reads and provides access to test configuration values
 */
object TestConfigReader {
    private lateinit var config: JsonObject

    /**
     * Initialize the config reader with the application context
     */
    fun initialize(context: Context) {
        val inputStream = context.assets.open("test_config.json")
        val reader = BufferedReader(InputStreamReader(inputStream))
        val jsonString = reader.readText()
        config = Json.parseToJsonElement(jsonString).jsonObject["test_environment"]?.jsonObject
            ?: throw IllegalStateException("Invalid test configuration file")
    }

    /**
     * Firebase emulator configuration
     */
    object Firebase {
        val isEmulatorEnabled: Boolean
            get() = config["firebase_emulator"]?.jsonObject?.get("enabled")?.jsonPrimitive?.boolean ?: true

        val emulatorHost: String
            get() = config["firebase_emulator"]?.jsonObject?.get("host")?.jsonPrimitive?.content
                ?: "10.0.2.2"

        object Ports {
            val auth: Int
                get() = getPort("auth")
            val firestore: Int
                get() = getPort("firestore")
            val database: Int
                get() = getPort("database")
            val functions: Int
                get() = getPort("functions")
            val storage: Int
                get() = getPort("storage")

            private fun getPort(service: String): Int {
                return config["firebase_emulator"]?.jsonObject
                    ?.get("ports")?.jsonObject
                    ?.get(service)?.jsonPrimitive?.int ?: 0
            }
        }
    }

    /**
     * Location service configuration
     */
    object LocationService {
        val updateIntervalMs: Long
            get() = config["location_service"]?.jsonObject
                ?.get("update_interval_ms")?.jsonPrimitive?.long ?: 1000L

        val fastestIntervalMs: Long
            get() = config["location_service"]?.jsonObject
                ?.get("fastest_interval_ms")?.jsonPrimitive?.long ?: 500L

        val maxWaitTimeMs: Long
            get() = config["location_service"]?.jsonObject
                ?.get("max_wait_time_ms")?.jsonPrimitive?.long ?: 5000L

        val defaultAccuracy: String
            get() = config["location_service"]?.jsonObject
                ?.get("default_accuracy")?.jsonPrimitive?.content ?: "high"

        val mockLocations: List<Location>
            get() {
                return config["location_service"]?.jsonObject
                    ?.get("mock_locations")?.jsonArray
                    ?.map { location ->
                        val obj = location.jsonObject
                        Location(
                            latitude = obj["latitude"]?.jsonPrimitive?.double ?: 0.0,
                            longitude = obj["longitude"]?.jsonPrimitive?.double ?: 0.0
                        )
                    } ?: emptyList()
            }
    }

    /**
     * Test user configuration
     */
    object TestUsers {
        data class TestUser(
            val email: String,
            val password: String,
            val userType: UserType
        )

        val driver: TestUser
            get() = getTestUser("driver")

        val salesPoint: TestUser
            get() = getTestUser("sales_point")

        val admin: TestUser
            get() = getTestUser("admin")

        private fun getTestUser(type: String): TestUser {
            val userObj = config["test_users"]?.jsonObject?.get(type)?.jsonObject
                ?: throw IllegalStateException("Test user $type not found in config")

            return TestUser(
                email = userObj["email"]?.jsonPrimitive?.content
                    ?: throw IllegalStateException("Email not found for test user $type"),
                password = userObj["password"]?.jsonPrimitive?.content
                    ?: throw IllegalStateException("Password not found for test user $type"),
                userType = UserType.valueOf(
                    userObj["user_type"]?.jsonPrimitive?.content
                        ?: throw IllegalStateException("User type not found for test user $type")
                )
            )
        }
    }

    /**
     * Test timeout configuration
     */
    object Timeouts {
        val networkCalls: Long
            get() = getTimeout("network_calls")
        val locationUpdates: Long
            get() = getTimeout("location_updates")
        val animations: Long
            get() = getTimeout("animations")
        val databaseOperations: Long
            get() = getTimeout("database_operations")
        val authOperations: Long
            get() = getTimeout("auth_operations")

        private fun getTimeout(key: String): Long {
            return config["test_timeouts"]?.jsonObject
                ?.get(key)?.jsonPrimitive?.long ?: 5000L
        }
    }

    /**
     * Test collection names
     */
    object Collections {
        val users: String
            get() = getCollectionName("users")
        val deliveryRequests: String
            get() = getCollectionName("delivery_requests")
        val truckLocations: String
            get() = getCollectionName("truck_locations")
        val notifications: String
            get() = getCollectionName("notifications")

        private fun getCollectionName(key: String): String {
            return config["test_collections"]?.jsonObject
                ?.get(key)?.jsonPrimitive?.content
                ?: "${key}_test"
        }
    }

    /**
     * Notification configuration
     */
    object Notifications {
        data class NotificationChannel(
            val id: String,
            val name: String,
            val description: String,
            val importance: String
        )

        val channels: Map<String, NotificationChannel>
            get() {
                return config["notifications"]?.jsonObject
                    ?.get("channels")?.jsonObject
                    ?.mapValues { (_, value) ->
                        val channelObj = value.jsonObject
                        NotificationChannel(
                            id = channelObj["id"]?.jsonPrimitive?.content ?: "",
                            name = channelObj["name"]?.jsonPrimitive?.content ?: "",
                            description = channelObj["description"]?.jsonPrimitive?.content ?: "",
                            importance = channelObj["importance"]?.jsonPrimitive?.content ?: "default"
                        )
                    } ?: emptyMap()
            }

        val notificationTypes: Map<String, String>
            get() = config["notifications"]?.jsonObject
                ?.get("types")?.jsonObject
                ?.mapValues { it.value.jsonPrimitive.content }
                ?: emptyMap()
    }
}
