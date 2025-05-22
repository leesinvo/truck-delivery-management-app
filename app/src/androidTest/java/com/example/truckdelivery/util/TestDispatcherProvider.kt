package com.example.truckdelivery.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Provides coroutine dispatchers for testing
 */
class TestDispatcherProvider {
    private val testDispatcher = TestCoroutineDispatcher()

    val main: CoroutineDispatcher = testDispatcher
    val io: CoroutineDispatcher = testDispatcher
    val default: CoroutineDispatcher = testDispatcher
    val unconfined: CoroutineDispatcher = testDispatcher

    /**
     * Cleanup test dispatchers
     */
    fun cleanupTestCoroutines() {
        testDispatcher.cleanupTestCoroutines()
    }
}

/**
 * JUnit rule to manage coroutine dispatchers in tests
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestCoroutineRule(
    private val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
) : TestWatcher(), TestCoroutineScope by TestCoroutineScope(testDispatcher) {

    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        cleanupTestCoroutines()
        Dispatchers.resetMain()
    }
}

/**
 * Extension functions for test coroutines
 */
@OptIn(ExperimentalCoroutinesApi::class)
object TestCoroutineExtensions {
    /**
     * Runs a test with a test dispatcher
     */
    suspend fun runTest(
        block: suspend TestCoroutineScope.() -> Unit
    ) {
        val testDispatcher = TestCoroutineDispatcher()
        val testScope = TestCoroutineScope(testDispatcher)
        
        try {
            Dispatchers.setMain(testDispatcher)
            testScope.block()
        } finally {
            testScope.cleanupTestCoroutines()
            Dispatchers.resetMain()
        }
    }

    /**
     * Pauses all coroutines
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun TestCoroutineDispatcher.pauseDispatcher() {
        pauseDispatcher()
    }

    /**
     * Resumes all coroutines
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun TestCoroutineDispatcher.resumeDispatcher() {
        resumeDispatcher()
    }

    /**
     * Advances time by the specified amount
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun TestCoroutineDispatcher.advanceTimeBy(delayTimeMillis: Long) {
        advanceTimeBy(delayTimeMillis)
    }

    /**
     * Runs all pending coroutines until idle
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun TestCoroutineDispatcher.runCurrent() {
        runCurrent()
    }
}

/**
 * Test scope for managing coroutines in tests
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestCoroutineManager {
    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)

    fun beforeTest() {
        Dispatchers.setMain(testDispatcher)
    }

    fun afterTest() {
        testScope.cleanupTestCoroutines()
        Dispatchers.resetMain()
    }

    /**
     * Executes a block with test coroutine context
     */
    suspend fun <T> withTestContext(block: suspend TestCoroutineScope.() -> T): T {
        return testScope.block()
    }

    /**
     * Advances virtual time by the specified amount
     */
    fun advanceTimeBy(delayTimeMillis: Long) {
        testDispatcher.advanceTimeBy(delayTimeMillis)
    }

    /**
     * Runs until there are no more pending coroutines
     */
    fun advanceUntilIdle() {
        testDispatcher.advanceUntilIdle()
    }

    /**
     * Pauses the execution of coroutines
     */
    fun pauseDispatcher() {
        testDispatcher.pauseDispatcher()
    }

    /**
     * Resumes the execution of coroutines
     */
    fun resumeDispatcher() {
        testDispatcher.resumeDispatcher()
    }

    /**
     * Runs all currently executable coroutines
     */
    fun runCurrent() {
        testDispatcher.runCurrent()
    }
}
