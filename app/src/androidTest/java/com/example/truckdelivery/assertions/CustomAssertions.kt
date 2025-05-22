package com.example.truckdelivery.assertions

import android.view.View
import androidx.compose.ui.test.*
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.matcher.ViewMatchers
import com.example.truckdelivery.config.TestConfigReader
import com.example.truckdelivery.data.model.*
import com.example.truckdelivery.util.TestUtils
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.junit.Assert

/**
 * Custom assertions for UI testing
 */
object CustomAssertions {

    /**
     * Location assertions
     */
    object Location {
        fun assertLocationWithinRadius(
            actualLocation: com.example.truckdelivery.data.model.Location,
            expectedCenter: com.example.truckdelivery.data.model.Location,
            radiusKm: Double
        ) {
            val distance = TestUtils.Location.calculateDistance(actualLocation, expectedCenter)
            assertThat(distance).isLessThan(radiusKm)
        }

        fun assertValidCoordinates(location: com.example.truckdelivery.data.model.Location) {
            assertThat(location.latitude).isIn(Range.closed(-90.0, 90.0))
            assertThat(location.longitude).isIn(Range.closed(-180.0, 180.0))
        }
    }

    /**
     * Delivery request assertions
     */
    object DeliveryRequest {
        fun assertValidRequest(request: com.example.truckdelivery.data.model.DeliveryRequest) {
            assertThat(request.id).isNotEmpty()
            assertThat(request.userId).isNotEmpty()
            assertThat(request.productType).isNotEmpty()
            assertThat(request.quantity).isGreaterThan(0)
            assertValidCoordinates(request.location)
            assertValidStatus(request.status)
        }

        fun assertValidStatus(status: RequestStatus) {
            assertThat(status).isIn(RequestStatus.values())
        }

        fun assertValidStatusTransition(
            currentStatus: RequestStatus,
            newStatus: RequestStatus
        ) {
            val validTransitions = when (currentStatus) {
                RequestStatus.PENDING -> setOf(RequestStatus.ACCEPTED, RequestStatus.CANCELLED)
                RequestStatus.ACCEPTED -> setOf(RequestStatus.IN_PROGRESS, RequestStatus.CANCELLED)
                RequestStatus.IN_PROGRESS -> setOf(RequestStatus.COMPLETED, RequestStatus.CANCELLED)
                RequestStatus.COMPLETED -> emptySet()
                RequestStatus.CANCELLED -> emptySet()
            }
            assertThat(newStatus).isIn(validTransitions)
        }
    }

    /**
     * User assertions
     */
    object User {
        fun assertValidUser(user: User) {
            assertThat(user.id).isNotEmpty()
            assertThat(user.email).matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
            assertThat(user.userType).isIn(UserType.values())
        }

        fun assertValidUserType(userType: UserType) {
            assertThat(userType).isIn(UserType.values())
        }
    }

    /**
     * Compose UI assertions
     */
    object ComposeUI {
        fun SemanticsNodeInteraction.assertIsDisplayedWithTimeout(
            timeoutMs: Long = TestConfigReader.Timeouts.animations
        ) {
            runBlocking {
                TestUtils.UI.waitForCondition(timeoutMs) {
                    try {
                        assertIsDisplayed()
                        true
                    } catch (e: AssertionError) {
                        false
                    }
                }
            }
        }

        fun SemanticsNodeInteraction.assertTextEquals(
            expected: String,
            timeoutMs: Long = TestConfigReader.Timeouts.animations
        ) {
            runBlocking {
                TestUtils.UI.waitForCondition(timeoutMs) {
                    try {
                        assertTextEquals(expected)
                        true
                    } catch (e: AssertionError) {
                        false
                    }
                }
            }
        }

        fun SemanticsNodeInteraction.assertHasClickAction(
            timeoutMs: Long = TestConfigReader.Timeouts.animations
        ) {
            runBlocking {
                TestUtils.UI.waitForCondition(timeoutMs) {
                    try {
                        assertHasClickAction()
                        true
                    } catch (e: AssertionError) {
                        false
                    }
                }
            }
        }

        fun SemanticsNodeInteraction.assertIsEnabled(
            timeoutMs: Long = TestConfigReader.Timeouts.animations
        ) {
            runBlocking {
                TestUtils.UI.waitForCondition(timeoutMs) {
                    try {
                        assertIsEnabled()
                        true
                    } catch (e: AssertionError) {
                        false
                    }
                }
            }
        }
    }

    /**
     * Network assertions
     */
    object Network {
        fun assertNetworkAvailable() {
            assertThat(TestUtils.Network.isNetworkAvailable()).isTrue()
        }

        fun assertValidResponse(response: Any?) {
            assertThat(response).isNotNull()
        }
    }

    /**
     * Time assertions
     */
    object Time {
        fun assertValidTimestamp(timestamp: Long) {
            val now = System.currentTimeMillis()
            assertThat(timestamp).isIn(Range.closed(0L, now))
        }

        fun assertValidDuration(startTime: Long, endTime: Long) {
            assertThat(endTime).isGreaterThan(startTime)
        }
    }

    /**
     * Custom view assertions
     */
    object View {
        fun isVisible(): ViewAssertion {
            return ViewAssertion { view, noViewFoundException ->
                if (noViewFoundException != null) {
                    throw noViewFoundException
                }
                assertThat(view.visibility).isEqualTo(android.view.View.VISIBLE)
            }
        }

        fun isGone(): ViewAssertion {
            return ViewAssertion { view, noViewFoundException ->
                if (noViewFoundException != null) {
                    throw noViewFoundException
                }
                assertThat(view.visibility).isEqualTo(android.view.View.GONE)
            }
        }

        fun hasText(expected: String): ViewAssertion {
            return ViewAssertion { view, noViewFoundException ->
                if (noViewFoundException != null) {
                    throw noViewFoundException
                }
                val text = when (view) {
                    is android.widget.TextView -> view.text.toString()
                    is android.widget.EditText -> view.text.toString()
                    else -> throw AssertionError("View is not a text view")
                }
                assertThat(text).isEqualTo(expected)
            }
        }
    }

    /**
     * Extension functions for assertions
     */
    fun assertWithTimeout(
        timeoutMs: Long = TestConfigReader.Timeouts.animations,
        assertion: () -> Unit
    ) {
        runBlocking {
            TestUtils.UI.waitForCondition(timeoutMs) {
                try {
                    assertion()
                    true
                } catch (e: AssertionError) {
                    false
                }
            }
        }
    }

    fun <T> assertEventually(
        timeoutMs: Long = TestConfigReader.Timeouts.animations,
        condition: () -> T,
        matcher: Matcher<T>
    ) {
        runBlocking {
            TestUtils.UI.waitForCondition(timeoutMs) {
                try {
                    ViewMatchers.assertThat(condition(), matcher)
                    true
                } catch (e: AssertionError) {
                    false
                }
            }
        }
    }
}
