package com.example.truckdelivery.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.example.truckdelivery.config.TestConfigReader
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withTimeout

/**
 * Handles network conditions for tests
 */
object TestNetworkHandler {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Available)
    val networkState: Flow<NetworkState> = _networkState

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _networkState.value = NetworkState.Available
        }

        override fun onLost(network: Network) {
            _networkState.value = NetworkState.Unavailable
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val state = when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                    NetworkState.Available
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                    NetworkState.Available
                else -> NetworkState.Unavailable
            }
            _networkState.value = state
        }
    }

    init {
        registerNetworkCallback()
    }

    /**
     * Register network callback
     */
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    /**
     * Unregister network callback
     */
    fun cleanup() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    /**
     * Check if network is available
     */
    fun isNetworkAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }

    /**
     * Simulate network conditions
     */
    suspend fun simulateNetworkCondition(condition: NetworkCondition) {
        when (condition) {
            NetworkCondition.NO_NETWORK -> disableNetwork()
            NetworkCondition.SLOW_NETWORK -> simulateSlowNetwork()
            NetworkCondition.UNSTABLE_NETWORK -> simulateUnstableNetwork()
            NetworkCondition.NORMAL_NETWORK -> enableNetwork()
        }
    }

    /**
     * Enable network
     */
    private suspend fun enableNetwork() {
        withTimeout(TestConfigReader.Timeouts.networkCalls) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                connectivityManager.setAirplaneMode(false)
            }
            waitForNetworkState(NetworkState.Available)
        }
    }

    /**
     * Disable network
     */
    private suspend fun disableNetwork() {
        withTimeout(TestConfigReader.Timeouts.networkCalls) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                connectivityManager.setAirplaneMode(true)
            }
            waitForNetworkState(NetworkState.Unavailable)
        }
    }

    /**
     * Simulate slow network
     */
    private suspend fun simulateSlowNetwork() {
        // Simulate network latency
        val channel = Channel<Unit>()
        channel.receiveAsFlow().collect {
            delay(2000) // Add 2 second delay
            channel.send(Unit)
        }
    }

    /**
     * Simulate unstable network
     */
    private suspend fun simulateUnstableNetwork() {
        repeat(3) {
            disableNetwork()
            delay(1000)
            enableNetwork()
            delay(1000)
        }
    }

    /**
     * Wait for specific network state
     */
    private suspend fun waitForNetworkState(expectedState: NetworkState) {
        val timeout = TestConfigReader.Timeouts.networkCalls
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeout) {
            if (_networkState.value == expectedState) {
                return
            }
            delay(100)
        }

        throw RuntimeException(
            "Network did not change to $expectedState state within $timeout ms"
        )
    }

    /**
     * Network states
     */
    sealed class NetworkState {
        object Available : NetworkState()
        object Unavailable : NetworkState()
    }

    /**
     * Network conditions for testing
     */
    enum class NetworkCondition {
        NO_NETWORK,
        SLOW_NETWORK,
        UNSTABLE_NETWORK,
        NORMAL_NETWORK
    }

    /**
     * Extension function to execute code with specific network condition
     */
    suspend fun <T> withNetworkCondition(
        condition: NetworkCondition,
        block: suspend () -> T
    ): T {
        try {
            simulateNetworkCondition(condition)
            return block()
        } finally {
            simulateNetworkCondition(NetworkCondition.NORMAL_NETWORK)
        }
    }

    /**
     * Extension function to execute code with retry on network failure
     */
    suspend fun <T> withNetworkRetry(
        maxAttempts: Int = 3,
        delayMs: Long = 1000,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxAttempts - 1) {
                    delay(delayMs)
                }
            }
        }
        throw lastException ?: RuntimeException("Network operation failed after $maxAttempts attempts")
    }
}
