package com.example.truckdelivery.util

import android.app.Activity
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.ScreenCapture
import androidx.test.runner.screenshot.Screenshot
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handles screenshot capture and management for tests
 */
object TestScreenshotHandler {
    private const val TAG = "TestScreenshotHandler"
    private const val SCREENSHOTS_DIR = "test_screenshots"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    /**
     * Capture screenshot of the entire activity
     */
    fun captureActivity(
        activity: Activity,
        name: String,
        description: String? = null
    ): File? {
        return try {
            val screenshotName = generateScreenshotName(name)
            val screenCapture = Screenshot.capture(activity)
            screenCapture.name = screenshotName
            description?.let { screenCapture.setDescription(it) }
            processScreenCapture(screenCapture)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture activity screenshot: ${e.message}")
            null
        }
    }

    /**
     * Capture screenshot of a specific view
     */
    fun captureView(
        view: View,
        name: String,
        description: String? = null
    ): File? {
        return try {
            val screenshotName = generateScreenshotName(name)
            val screenCapture = Screenshot.capture(view)
            screenCapture.name = screenshotName
            description?.let { screenCapture.setDescription(it) }
            processScreenCapture(screenCapture)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture view screenshot: ${e.message}")
            null
        }
    }

    /**
     * Capture screenshot of a Compose UI element
     */
    fun captureComposeNode(
        node: SemanticsNodeInteraction,
        name: String,
        description: String? = null
    ): File? {
        return try {
            val screenshotName = generateScreenshotName(name)
            val bitmap = node.captureToImage().asAndroidBitmap()
            saveBitmap(bitmap, screenshotName, description)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture Compose node screenshot: ${e.message}")
            null
        }
    }

    /**
     * Save bitmap as screenshot
     */
    private fun saveBitmap(
        bitmap: Bitmap,
        name: String,
        description: String? = null
    ): File? {
        return try {
            val screenCapture = ScreenCapture()
            screenCapture.name = name
            screenCapture.bitmap = bitmap
            description?.let { screenCapture.setDescription(it) }
            processScreenCapture(screenCapture)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bitmap screenshot: ${e.message}")
            null
        }
    }

    /**
     * Process and save screen capture
     */
    private fun processScreenCapture(screenCapture: ScreenCapture): File? {
        return try {
            val processor = createScreenCaptureProcessor()
            processor.process(screenCapture)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process screen capture: ${e.message}")
            null
        }
    }

    /**
     * Create screen capture processor
     */
    private fun createScreenCaptureProcessor(): BasicScreenCaptureProcessor {
        return object : BasicScreenCaptureProcessor() {
            init {
                mDefaultScreenshotPath = getScreenshotsDirectory()
            }
        }
    }

    /**
     * Get screenshots directory
     */
    private fun getScreenshotsDirectory(): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val screenshotsDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            SCREENSHOTS_DIR
        )
        
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdirs()
        }
        
        return screenshotsDir
    }

    /**
     * Generate screenshot name with timestamp
     */
    private fun generateScreenshotName(baseName: String): String {
        val timestamp = dateFormat.format(Date())
        return "${baseName}_$timestamp"
    }

    /**
     * Clean up old screenshots
     */
    fun cleanupOldScreenshots(maxAgeHours: Int = 24) {
        try {
            val screenshotsDir = getScreenshotsDirectory()
            val cutoffTime = System.currentTimeMillis() - (maxAgeHours * 60 * 60 * 1000)

            screenshotsDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old screenshots: ${e.message}")
        }
    }

    /**
     * Compare two screenshots for similarity
     */
    fun compareScreenshots(
        screenshot1: File,
        screenshot2: File,
        similarityThreshold: Double = 0.95
    ): Boolean {
        return try {
            val bitmap1 = android.graphics.BitmapFactory.decodeFile(screenshot1.absolutePath)
            val bitmap2 = android.graphics.BitmapFactory.decodeFile(screenshot2.absolutePath)

            if (bitmap1.width != bitmap2.width || bitmap1.height != bitmap2.height) {
                return false
            }

            var matchingPixels = 0
            val totalPixels = bitmap1.width * bitmap1.height

            for (x in 0 until bitmap1.width) {
                for (y in 0 until bitmap1.height) {
                    if (bitmap1.getPixel(x, y) == bitmap2.getPixel(x, y)) {
                        matchingPixels++
                    }
                }
            }

            val similarity = matchingPixels.toDouble() / totalPixels
            similarity >= similarityThreshold
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compare screenshots: ${e.message}")
            false
        }
    }

    /**
     * Get all screenshots in the directory
     */
    fun getAllScreenshots(): List<File> {
        return try {
            getScreenshotsDirectory().listFiles()?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get screenshots: ${e.message}")
            emptyList()
        }
    }

    /**
     * Delete all screenshots
     */
    fun deleteAllScreenshots() {
        try {
            getScreenshotsDirectory().deleteRecursively()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete screenshots: ${e.message}")
        }
    }
}
