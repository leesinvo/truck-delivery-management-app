package com.example.truckdelivery.runner

import android.os.SystemClock
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.example.truckdelivery.annotations.*
import com.example.truckdelivery.util.TestScreenshotHandler
import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Custom test run listener to monitor and log test execution
 */
class TestRunListener : RunListener() {
    private val TAG = "TestRunListener"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val testStartTimes = mutableMapOf<Description, Long>()
    private val testResults = mutableMapOf<String, TestResult>()
    private var suiteStartTime: Long = 0

    override fun testRunStarted(description: Description) {
        suiteStartTime = SystemClock.elapsedRealtime()
        Log.i(TAG, "Test suite started at ${dateFormat.format(Date())}")
        Log.i(TAG, "Running ${description.testCount} tests")
    }

    override fun testRunFinished(result: Result) {
        val duration = SystemClock.elapsedRealtime() - suiteStartTime
        Log.i(TAG, """
            Test suite finished:
            - Duration: ${formatDuration(duration)}
            - Total tests: ${result.runCount}
            - Failed tests: ${result.failureCount}
            - Ignored tests: ${result.ignoreCount}
            - Success rate: ${calculateSuccessRate(result)}%
        """.trimIndent())

        generateTestReport()
    }

    override fun testStarted(description: Description) {
        testStartTimes[description] = SystemClock.elapsedRealtime()
        Log.i(TAG, "Test started: ${description.methodName}")
        logTestAnnotations(description)
    }

    override fun testFinished(description: Description) {
        val startTime = testStartTimes.remove(description) ?: return
        val duration = SystemClock.elapsedRealtime() - startTime

        testResults[description.methodName] = TestResult(
            name = description.methodName,
            className = description.className,
            duration = duration,
            status = "PASSED",
            annotations = getTestAnnotations(description)
        )

        Log.i(TAG, "Test finished: ${description.methodName} (${formatDuration(duration)})")
    }

    override fun testFailure(failure: Failure) {
        val description = failure.description
        val testName = description.methodName

        testResults[testName] = TestResult(
            name = testName,
            className = description.className,
            duration = calculateTestDuration(description),
            status = "FAILED",
            error = failure.exception,
            annotations = getTestAnnotations(description)
        )

        Log.e(TAG, """
            Test failed: $testName
            Error: ${failure.exception}
            Trace: ${failure.trace}
        """.trimIndent())

        // Capture failure screenshot if enabled
        description.getAnnotation(CaptureScreenshots::class.java)?.let { annotation ->
            if (annotation.captureOnFailure) {
                captureFailureScreenshot(description)
            }
        }
    }

    override fun testAssumptionFailure(failure: Failure) {
        val description = failure.description
        val testName = description.methodName

        testResults[testName] = TestResult(
            name = testName,
            className = description.className,
            duration = calculateTestDuration(description),
            status = "SKIPPED",
            error = failure.exception,
            annotations = getTestAnnotations(description)
        )

        Log.w(TAG, "Test assumption failed: $testName (${failure.message})")
    }

    override fun testIgnored(description: Description) {
        testResults[description.methodName] = TestResult(
            name = description.methodName,
            className = description.className,
            duration = 0,
            status = "IGNORED",
            annotations = getTestAnnotations(description)
        )

        Log.i(TAG, "Test ignored: ${description.methodName}")
    }

    private fun logTestAnnotations(description: Description) {
        val annotations = getTestAnnotations(description)
        if (annotations.isNotEmpty()) {
            Log.d(TAG, "Test annotations for ${description.methodName}:")
            annotations.forEach { annotation ->
                Log.d(TAG, "- ${annotation.annotationClass.simpleName}")
            }
        }
    }

    private fun getTestAnnotations(description: Description): List<Annotation> {
        return description.annotations.filter { annotation ->
            annotation.annotationClass.java.packageName.startsWith("com.example.truckdelivery.annotations")
        }
    }

    private fun calculateTestDuration(description: Description): Long {
        val startTime = testStartTimes[description] ?: return 0
        return SystemClock.elapsedRealtime() - startTime
    }

    private fun captureFailureScreenshot(description: Description) {
        val activity = InstrumentationRegistry.getInstrumentation()
            .runOnMainSync { TestUtils.Activities.getActivity() }

        activity?.let {
            val screenshotName = "${description.methodName}_failure_${System.currentTimeMillis()}"
            TestScreenshotHandler.captureActivity(it, screenshotName, "Test failure screenshot")
        }
    }

    private fun generateTestReport() {
        val reportFile = File(
            InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
            "test_report.html"
        )

        reportFile.writeText(generateHtmlReport())
        Log.i(TAG, "Test report generated at ${reportFile.absolutePath}")
    }

    private fun generateHtmlReport(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Execution Report</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; }
                    .passed { color: green; }
                    .failed { color: red; }
                    .ignored { color: orange; }
                    table { border-collapse: collapse; width: 100%; }
                    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                    th { background-color: #f2f2f2; }
                </style>
            </head>
            <body>
                <h1>Test Execution Report</h1>
                <h2>Summary</h2>
                <p>
                    Total Tests: ${testResults.size}<br>
                    Passed: ${testResults.count { it.value.status == "PASSED" }}<br>
                    Failed: ${testResults.count { it.value.status == "FAILED" }}<br>
                    Ignored: ${testResults.count { it.value.status == "IGNORED" }}<br>
                    Duration: ${formatDuration(SystemClock.elapsedRealtime() - suiteStartTime)}
                </p>
                <h2>Test Results</h2>
                <table>
                    <tr>
                        <th>Test</th>
                        <th>Status</th>
                        <th>Duration</th>
                        <th>Error</th>
                    </tr>
                    ${generateTestResultsTable()}
                </table>
            </body>
            </html>
        """.trimIndent()
    }

    private fun generateTestResultsTable(): String {
        return testResults.values.joinToString("\n") { result ->
            """
                <tr>
                    <td>${result.name}</td>
                    <td class="${result.status.toLowerCase()}">${result.status}</td>
                    <td>${formatDuration(result.duration)}</td>
                    <td>${result.error?.message ?: ""}</td>
                </tr>
            """.trimIndent()
        }
    }

    private fun calculateSuccessRate(result: Result): Double {
        if (result.runCount == 0) return 0.0
        val successCount = result.runCount - result.failureCount - result.ignoreCount
        return (successCount.toDouble() / result.runCount.toDouble()) * 100
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60)) % 24
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private data class TestResult(
        val name: String,
        val className: String,
        val duration: Long,
        val status: String,
        val error: Throwable? = null,
        val annotations: List<Annotation> = emptyList()
    )
}
