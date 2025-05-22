package com.example.truckdelivery.rules

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.example.truckdelivery.annotations.*
import com.example.truckdelivery.config.TestConfigReader
import com.example.truckdelivery.data.TestDatabaseInitializer
import com.example.truckdelivery.util.*
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.TimeoutException

/**
 * Base test rule that handles common test setup and cleanup
 */
abstract class BaseTestRule : TestRule {
    protected val TAG = this.javaClass.simpleName

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                before(description)
                try {
                    base.evaluate()
                } catch (t: Throwable) {
                    onError(t, description)
                    throw t
                } finally {
                    after(description)
                }
            }
        }
    }

    protected open fun before(description: Description) {
        Log.d(TAG, "Starting test: ${description.methodName}")
    }

    protected open fun after(description: Description) {
        Log.d(TAG, "Finished test: ${description.methodName}")
    }

    protected open fun onError(error: Throwable, description: Description) {
        Log.e(TAG, "Error in test ${description.methodName}: ${error.message}")
    }
}

/**
 * Rule to handle network requirements
 */
class NetworkRule : BaseTestRule() {
    override fun before(description: Description) {
        super.before(description)
        description.getAnnotation(RequiresNetwork::class.java)?.let { annotation ->
            if (!TestNetworkHandler.isNetworkAvailable() && annotation.failOnNoNetwork) {
                throw IllegalStateException("Test requires network connection")
            }
        }

        description.getAnnotation(WithNetworkCondition::class.java)?.let { annotation ->
            runBlocking {
                TestNetworkHandler.simulateNetworkCondition(annotation.condition)
            }
        }
    }

    override fun after(description: Description) {
        runBlocking {
            TestNetworkHandler.simulateNetworkCondition(NetworkCondition.NORMAL)
        }
        super.after(description)
    }
}

/**
 * Rule to handle permissions
 */
class PermissionRule : BaseTestRule() {
    override fun before(description: Description) {
        super.before(description)
        runBlocking {
            handleLocationPermission(description)
            handleNotificationPermission(description)
        }
    }

    private suspend fun handleLocationPermission(description: Description) {
        description.getAnnotation(RequiresLocation::class.java)?.let { annotation ->
            if (!TestPermissionHandler.isPermissionGranted(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                if (annotation.failOnNoPermission) {
                    throw IllegalStateException("Test requires location permission")
                } else {
                    TestPermissionHandler.grantRequiredPermissions()
                }
            }
        }
    }

    private suspend fun handleNotificationPermission(description: Description) {
        description.getAnnotation(RequiresNotification::class.java)?.let { annotation ->
            if (!TestPermissionHandler.isPermissionGranted(android.Manifest.permission.POST_NOTIFICATIONS)) {
                if (annotation.failOnNoPermission) {
                    throw IllegalStateException("Test requires notification permission")
                } else {
                    TestPermissionHandler.grantRequiredPermissions()
                }
            }
        }
    }
}

/**
 * Rule to handle Firebase emulators
 */
class EmulatorRule : BaseTestRule() {
    override fun before(description: Description) {
        super.before(description)
        description.getAnnotation(RequiresEmulator::class.java)?.let { annotation ->
            if (!TestConfigReader.Firebase.isEmulatorEnabled) {
                throw IllegalStateException("Test requires Firebase emulators")
            }
        }
    }
}

/**
 * Rule to handle screenshots
 */
class ScreenshotRule : BaseTestRule() {
    private var testStartTime: Long = 0

    override fun before(description: Description) {
        super.before(description)
        testStartTime = System.currentTimeMillis()
    }

    override fun onError(error: Throwable, description: Description) {
        super.onError(error, description)
        description.getAnnotation(CaptureScreenshots::class.java)?.let { annotation ->
            if (annotation.captureOnFailure) {
                captureFailureScreenshot(description)
            }
        }
    }

    private fun captureFailureScreenshot(description: Description) {
        val fileName = "${description.methodName}_failure_${System.currentTimeMillis()}"
        TestScreenshotHandler.captureActivity(
            TestUtils.Activities.getActivity()!!,
            fileName,
            "Test failure screenshot"
        )
    }
}

/**
 * Rule to handle test retries
 */
class RetryRule : BaseTestRule() {
    override fun apply(base: Statement, description: Description): Statement {
        return description.getAnnotation(RetryOnFailure::class.java)?.let { annotation ->
            object : Statement() {
                override fun evaluate() {
                    var lastError: Throwable? = null
                    repeat(annotation.maxAttempts) { attempt ->
                        try {
                            base.evaluate()
                            return
                        } catch (t: Throwable) {
                            lastError = t
                            Log.w(TAG, "Test failed attempt ${attempt + 1}/${annotation.maxAttempts}")
                            Thread.sleep(annotation.delayMs)
                        }
                    }
                    throw lastError!!
                }
            }
        } ?: base
    }
}

/**
 * Rule to handle test timeouts
 */
class TimeoutRule : BaseTestRule() {
    override fun apply(base: Statement, description: Description): Statement {
        return description.getAnnotation(TestTimeout::class.java)?.let { annotation ->
            object : Statement() {
                override fun evaluate() {
                    runBlocking {
                        try {
                            kotlinx.coroutines.withTimeout(annotation.timeoutMs) {
                                base.evaluate()
                            }
                        } catch (e: TimeoutException) {
                            throw TimeoutException(
                                "Test ${description.methodName} timed out after ${annotation.timeoutMs}ms"
                            )
                        }
                    }
                }
            }
        } ?: base
    }
}

/**
 * Rule to handle test data cleanup
 */
class CleanupRule : BaseTestRule() {
    override fun after(description: Description) {
        if (description.getAnnotation(CleanupTestData::class.java) != null) {
            runBlocking {
                TestDatabaseInitializer.clearTestData()
            }
        }
        super.after(description)
    }
}

/**
 * Rule to handle authentication
 */
class AuthRule : BaseTestRule() {
    override fun before(description: Description) {
        super.before(description)
        description.getAnnotation(RequiresAuth::class.java)?.let { annotation ->
            runBlocking {
                when (annotation.userType) {
                    UserType.DRIVER -> loginAsDriver()
                    UserType.SALES_POINT -> loginAsSalesPoint()
                    UserType.ADMIN -> loginAsAdmin()
                    UserType.NONE -> logout()
                }
            }
        }
    }

    private suspend fun loginAsDriver() {
        val user = TestConfigReader.TestUsers.driver
        // Implement login logic
    }

    private suspend fun loginAsSalesPoint() {
        val user = TestConfigReader.TestUsers.salesPoint
        // Implement login logic
    }

    private suspend fun loginAsAdmin() {
        val user = TestConfigReader.TestUsers.admin
        // Implement login logic
    }

    private suspend fun logout() {
        // Implement logout logic
    }
}

/**
 * Composite rule that combines all test rules
 */
class CompositeTestRule : BaseTestRule() {
    private val rules = listOf(
        NetworkRule(),
        PermissionRule(),
        EmulatorRule(),
        ScreenshotRule(),
        RetryRule(),
        TimeoutRule(),
        CleanupRule(),
        AuthRule()
    )

    override fun apply(base: Statement, description: Description): Statement {
        var statement = base
        rules.reversed().forEach { rule ->
            statement = rule.apply(statement, description)
        }
        return statement
    }
}
