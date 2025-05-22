package com.example.truckdelivery.ui.screens.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.truckdelivery.data.model.Resource
import com.example.truckdelivery.data.model.User
import com.example.truckdelivery.data.model.UserType
import com.example.truckdelivery.ui.theme.TruckDeliveryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_DisplaysAllElements() {
        // Arrange
        composeTestRule.setContent {
            TruckDeliveryTheme {
                LoginScreen(
                    onNavigateToSignUp = {},
                    onLoginSuccess = {}
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("Truck Delivery").assertExists()
        composeTestRule.onNodeWithText("Email").assertExists()
        composeTestRule.onNodeWithText("Password").assertExists()
        composeTestRule.onNodeWithText("Login").assertExists()
        composeTestRule.onNodeWithText("Don't have an account? Sign Up").assertExists()
    }

    @Test
    fun loginScreen_EmptyFields_LoginButtonDisabled() {
        // Arrange
        composeTestRule.setContent {
            TruckDeliveryTheme {
                LoginScreen(
                    onNavigateToSignUp = {},
                    onLoginSuccess = {}
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("Login")
            .assertIsNotEnabled()
    }

    @Test
    fun loginScreen_ValidInput_LoginButtonEnabled() {
        // Arrange
        composeTestRule.setContent {
            TruckDeliveryTheme {
                LoginScreen(
                    onNavigateToSignUp = {},
                    onLoginSuccess = {}
                )
            }
        }

        // Act
        composeTestRule.onNodeWithText("Email")
            .performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Password")
            .performTextInput("password123")

        // Assert
        composeTestRule.onNodeWithText("Login")
            .assertIsEnabled()
    }

    @Test
    fun loginScreen_ClickSignUp_NavigatesCorrectly() {
        // Arrange
        val onNavigateToSignUp = mock<() -> Unit>()
        
        composeTestRule.setContent {
            TruckDeliveryTheme {
                LoginScreen(
                    onNavigateToSignUp = onNavigateToSignUp,
                    onLoginSuccess = {}
                )
            }
        }

        // Act
        composeTestRule.onNodeWithText("Don't have an account? Sign Up")
            .performClick()

        // Assert
        verify(onNavigateToSignUp).invoke()
    }

    @Test
    fun loginScreen_SuccessfulLogin_NavigatesCorrectly() {
        // Arrange
        val onLoginSuccess = mock<(String) -> Unit>()
        val viewModel = mock<AuthViewModel>()
        
        composeTestRule.setContent {
            TruckDeliveryTheme {
                LoginScreen(
                    onNavigateToSignUp = {},
                    onLoginSuccess = onLoginSuccess,
                    viewModel = viewModel
                )
            }
        }

        // Act
        composeTestRule.onNodeWithText("Email")
            .performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Password")
            .performTextInput("password123")
        composeTestRule.onNodeWithText("Login")
            .performClick()

        // Simulate successful login
        val user = User(
            id = "test-id",
            email = "test@example.com",
            userType = UserType.TRUCK_DRIVER
        )
        viewModel.loginState = Resource.Success(user)

        // Assert
        verify(onLoginSuccess).invoke(UserType.TRUCK_DRIVER.toString())
    }

    @Test
    fun loginScreen_ErrorState_DisplaysErrorMessage() {
        // Arrange
        val viewModel = mock<AuthViewModel>()
        val errorMessage = "Invalid credentials"

        composeTestRule.setContent {
            TruckDeliveryTheme {
                LoginScreen(
                    onNavigateToSignUp = {},
                    onLoginSuccess = {},
                    viewModel = viewModel
                )
            }
        }

        // Act
        viewModel.loginState = Resource.Error(Exception(errorMessage))

        // Assert
        composeTestRule.onNodeWithText(errorMessage).assertExists()
    }

    @Test
    fun loginScreen_LoadingState_DisplaysProgressIndicator() {
        // Arrange
        val viewModel = mock<AuthViewModel>()

        composeTestRule.setContent {
            TruckDeliveryTheme {
                LoginScreen(
                    onNavigateToSignUp = {},
                    onLoginSuccess = {},
                    viewModel = viewModel
                )
            }
        }

        // Act
        viewModel.loginState = Resource.Loading

        // Assert
        composeTestRule.onNodeWithContentDescription("Loading")
            .assertExists()
    }
}
