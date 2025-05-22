package com.example.truckdelivery.util

import android.Manifest
import android.app.Instrumentation
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import com.example.truckdelivery.config.TestConfigReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/**
 * Handles runtime permissions for tests
 */
object TestPermissionHandler {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val uiDevice: UiDevice = UiDevice.getInstance(instrumentation)

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE
    )

    /**
     * Grant all required permissions
     */
    suspend fun grantRequiredPermissions() {
        withTimeout(TestConfigReader.Timeouts.authOperations) {
            requiredPermissions.forEach { permission ->
                if (!isPermissionGranted(permission)) {
                    grantPermission(permission)
                }
            }
        }
    }

    /**
     * Revoke all permissions
     */
    suspend fun revokeAllPermissions() {
        withTimeout(TestConfigReader.Timeouts.authOperations) {
            requiredPermissions.forEach { permission ->
                if (isPermissionGranted(permission)) {
                    revokePermission(permission)
                }
            }
        }
    }

    /**
     * Check if a specific permission is granted
     */
    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == 
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * Grant a specific permission
     */
    private suspend fun grantPermission(permission: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permission == Manifest.permission.POST_NOTIFICATIONS &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                grantNotificationPermission()
            } else {
                val command = "pm grant ${context.packageName} $permission"
                uiDevice.executeShellCommand(command)
                waitForPermissionState(permission, true)
            }
        }
    }

    /**
     * Revoke a specific permission
     */
    private suspend fun revokePermission(permission: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permission == Manifest.permission.POST_NOTIFICATIONS &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                revokeNotificationPermission()
            } else {
                val command = "pm revoke ${context.packageName} $permission"
                uiDevice.executeShellCommand(command)
                waitForPermissionState(permission, false)
            }
        }
    }

    /**
     * Grant notification permission (Android 13+)
     */
    private suspend fun grantNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Click "Allow" on notification permission dialog
            val allowButton = uiDevice.findObject(UiSelector().text("Allow"))
            if (allowButton.exists()) {
                allowButton.click()
                waitForPermissionState(Manifest.permission.POST_NOTIFICATIONS, true)
            }
        }
    }

    /**
     * Revoke notification permission (Android 13+)
     */
    private suspend fun revokeNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Navigate to app notification settings and disable notifications
            val command = "cmd notification set-notification-policy ${context.packageName} blocked"
            uiDevice.executeShellCommand(command)
            waitForPermissionState(Manifest.permission.POST_NOTIFICATIONS, false)
        }
    }

    /**
     * Wait for permission state to change
     */
    private suspend fun waitForPermissionState(permission: String, expectedState: Boolean) {
        val timeout = TestConfigReader.Timeouts.authOperations
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeout) {
            if (isPermissionGranted(permission) == expectedState) {
                return
            }
            delay(100)
        }

        throw RuntimeException(
            "Permission $permission did not change to $expectedState state within $timeout ms"
        )
    }

    /**
     * Handle permission dialogs
     */
    suspend fun handlePermissionDialog(action: PermissionDialogAction) {
        withTimeout(TestConfigReader.Timeouts.authOperations) {
            when (action) {
                PermissionDialogAction.ALLOW -> clickAllowButton()
                PermissionDialogAction.DENY -> clickDenyButton()
                PermissionDialogAction.DENY_AND_DONT_ASK_AGAIN -> clickDenyAndDontAskAgainButton()
            }
        }
    }

    private suspend fun clickAllowButton() {
        clickDialogButton("Allow", "OK", "Accept")
    }

    private suspend fun clickDenyButton() {
        clickDialogButton("Deny", "NO", "Cancel")
    }

    private suspend fun clickDenyAndDontAskAgainButton() {
        val checkbox = findDialogElement("Don't ask again", "Never ask again")
        checkbox?.click()
        clickDenyButton()
    }

    private suspend fun clickDialogButton(vararg buttonTexts: String) {
        buttonTexts.forEach { text ->
            val button = findDialogElement(text)
            if (button?.exists() == true) {
                button.click()
                return
            }
        }
        throw RuntimeException("No matching dialog button found")
    }

    private fun findDialogElement(vararg texts: String): UiObject? {
        texts.forEach { text ->
            val element = uiDevice.findObject(UiSelector().text(text))
            if (element.exists()) {
                return element
            }
        }
        return null
    }

    /**
     * Permission dialog actions
     */
    enum class PermissionDialogAction {
        ALLOW,
        DENY,
        DENY_AND_DONT_ASK_AGAIN
    }
}
