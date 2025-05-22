package com.example.truckdelivery.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.example.truckdelivery.config.TestConfigReader
import com.example.truckdelivery.data.model.Location
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeoutException

/**
 * Utility functions for testing
 */
object TestUtils {

    /**
     * UI Automation helpers
     */
    object UI {
        /**
         * Wait for a specific condition with timeout
         */
        suspend fun waitForCondition(
            timeoutMs: Long = TestConfigReader.Timeouts.animations,
            condition: suspend () -> Boolean
        ) {
            withTimeout(timeoutMs) {
                while (!condition()) {
                    delay(100)
                }
            }
        }

        /**
         * Wait for view with specific tag to be displayed
         */
        fun ComposeTestRule.waitForViewWithTag(
            tag: String,
            timeoutMs: Long = TestConfigReader.Timeouts.animations
        ) {
            waitForCondition(timeoutMs) {
                onNodeWithTag(tag).isDisplayed()
            }
        }

        /**
         * Wait for text to be displayed
         */
        fun ComposeTestRule.waitForText(
            text: String,
            timeoutMs: Long = TestConfigReader.Timeouts.animations
        ) {
            waitForCondition(timeoutMs) {
                onNodeWithText(text).isDisplayed()
            }
        }

        /**
         * Perform action with retry
         */
        suspend fun <T> retryAction(
            maxAttempts: Int = 3,
            delayMs: Long = 1000,
            action: suspend () -> T
        ): T {
            var lastException: Exception? = null
            repeat(maxAttempts) { attempt ->
                try {
                    return action()
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < maxAttempts - 1) {
                        delay(delayMs)
                    }
                }
            }
            throw lastException ?: RuntimeException("Action failed after $maxAttempts attempts")
        }
    }

    /**
     * Activity helpers
     */
    object Activities {
        /**
         * Launch activity with custom intent
         */
        inline fun <reified T : Activity> launchActivity(
            intentSetup: (Intent) -> Unit = {}
        ): ActivityScenario<T> {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val intent = Intent(context, T::class.java).apply {
                intentSetup(this)
            }
            return ActivityScenario.launch(intent)
        }

        /**
         * Get current activity
         */
        fun getActivity(): Activity? {
            var activity: Activity? = null
            Espresso.onIdle {
                val resumedActivities = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                activity = resumedActivities.firstOrNull()
            }
            return activity
        }
    }

    /**
     * Location helpers
     */
    object Location {
        /**
         * Calculate distance between two locations in kilometers
         */
        fun calculateDistance(location1: Location, location2: Location): Double {
            val earthRadius = 6371.0 // kilometers
            
            val lat1 = Math.toRadians(location1.latitude)
            val lat2 = Math.toRadians(location2.latitude)
            val dLat = lat2 - lat1
            val dLon = Math.toRadians(location2.longitude - location1.longitude)
            
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(lat1) * Math.cos(lat2) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)
            
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            
            return earthRadius * c
        }

        /**
         * Check if location is within radius
         */
        fun isLocationWithinRadius(
            location: Location,
            center: Location,
            radiusKm: Double
        ): Boolean {
            return calculateDistance(location, center) <= radiusKm
        }
    }

    /**
     * Device helpers
     */
    object Device {
        private val uiDevice: UiDevice by lazy {
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        }

        /**
         * Press back button
         */
        fun pressBack() {
            uiDevice.pressBack()
        }

        /**
         * Press home button
         */
        fun pressHome() {
            uiDevice.pressHome()
        }

        /**
         * Wait for idle
         */
        fun waitForIdle(timeoutMs: Long = 5000) {
            uiDevice.waitForIdle(timeoutMs)
        }

        /**
         * Take screenshot
         */
        fun takeScreenshot(filename: String): Boolean {
            return uiDevice.takeScreenshot(
                TestScreenshotHandler.getScreenshotsDirectory().resolve("$filename.png")
            )
        }
    }

    /**
     * Idling resource helpers
     */
    object Idling {
        /**
         * Register idling resource
         */
        fun registerIdlingResource(idlingResource: IdlingResource) {
            IdlingRegistry.getInstance().register(idlingResource)
        }

        /**
         * Unregister idling resource
         */
        fun unregisterIdlingResource(idlingResource: IdlingResource) {
            IdlingRegistry.getInstance().unregister(idlingResource)
        }

        /**
         * Create counting idling resource
         */
        fun createCountingIdlingResource(name: String): CountingIdlingResource {
            return CountingIdlingResource(name)
        }
    }

    /**
     * Time helpers
     */
    object Time {
        /**
         * Get current time in milliseconds
         */
        fun getCurrentTimeMillis(): Long {
            return SystemClock.elapsedRealtime()
        }

        /**
         * Wait for specified duration
         */
        suspend fun wait(durationMs: Long) {
            delay(durationMs)
        }

        /**
         * Execute with timeout
         */
        suspend fun <T> withTimeout(
            timeoutMs: Long,
            block: suspend () -> T
        ): T {
            return try {
                kotlinx.coroutines.withTimeout(timeoutMs) {
                    block()
                }
            } catch (e: TimeoutException) {
                throw TimeoutException("Operation timed out after $timeoutMs ms")
            }
        }
    }
}

/**
 * Extension function to run test with cleanup
 */
inline fun <T> runWithCleanup(
    setup: () -> Unit = {},
    cleanup: () -> Unit,
    block: () -> T
): T {
    setup()
    try {
        return block()
    } finally {
        cleanup()
    }
}

/**
 * Extension function to run test with timeout
 */
suspend fun <T> runWithTimeout(
    timeoutMs: Long = TestConfigReader.Timeouts.networkCalls,
    block: suspend () -> T
): T = TestUtils.Time.withTimeout(timeoutMs, block)

/**
 * Extension function to retry test action
 */
suspend fun <T> retryTestAction(
    maxAttempts: Int = 3,
    delayMs: Long = 1000,
    block: suspend () -> T
): T = TestUtils.UI.retryAction(maxAttempts, delayMs, block)
