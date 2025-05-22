package com.example.truckdelivery.annotations

/**
 * Marks a test that requires network connectivity
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresNetwork(
    val failOnNoNetwork: Boolean = true
)

/**
 * Marks a test that requires location permissions
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresLocation(
    val failOnNoPermission: Boolean = true
)

/**
 * Marks a test that requires notification permissions
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresNotification(
    val failOnNoPermission: Boolean = true
)

/**
 * Marks a test that requires Firebase emulators
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresEmulator(
    val services: Array<EmulatorService> = [EmulatorService.ALL]
)

/**
 * Marks a test that should be run with specific network conditions
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithNetworkCondition(
    val condition: NetworkCondition
)

/**
 * Marks a test that should capture screenshots
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CaptureScreenshots(
    val baseFileName: String = "",
    val captureOnFailure: Boolean = true
)

/**
 * Marks a test that requires authentication
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresAuth(
    val userType: UserType
)

/**
 * Marks a test that should be retried on failure
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RetryOnFailure(
    val maxAttempts: Int = 3,
    val delayMs: Long = 1000
)

/**
 * Marks a test with a specific timeout
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TestTimeout(
    val timeoutMs: Long
)

/**
 * Marks a test that should be run in a specific order
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TestOrder(
    val order: Int
)

/**
 * Marks a test that should clean up test data after execution
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CleanupTestData

/**
 * Marks a test that should be run with mock time
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithMockTime(
    val initialTimeMillis: Long = 0
)

/**
 * Marks a test that requires specific device features
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresDeviceFeature(
    val features: Array<DeviceFeature>
)

/**
 * Enums for annotation values
 */
enum class EmulatorService {
    AUTH,
    FIRESTORE,
    DATABASE,
    STORAGE,
    FUNCTIONS,
    ALL
}

enum class NetworkCondition {
    NORMAL,
    SLOW,
    UNSTABLE,
    NO_NETWORK
}

enum class UserType {
    DRIVER,
    SALES_POINT,
    ADMIN,
    NONE
}

enum class DeviceFeature {
    LOCATION,
    NETWORK,
    CAMERA,
    BLUETOOTH,
    NOTIFICATIONS
}

/**
 * Test category annotations
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class UiTest

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntegrationTest

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PerformanceTest

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RegressionTest

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SmokeTest

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlakySensitive

/**
 * Test size annotations
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SmallTest

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class MediumTest

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LargeTest
