package com.example.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.telephony.TelephonyManager
import com.example.data.model.LiveConnectionState
import com.example.data.model.NetworkMedium
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LiveConnectionMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private val monitorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _connectionState = MutableStateFlow(LiveConnectionState())
    val connectionState: StateFlow<LiveConnectionState> = _connectionState.asStateFlow()

    private var lastRxBytes = TrafficStats.getTotalRxBytes()
    private var lastTxBytes = TrafficStats.getTotalTxBytes()
    private var lastTimestamp = System.currentTimeMillis()

    init {
        registerNetworkCallback()
        startSpeedSamplingLoop()
    }

    private fun registerNetworkCallback() {
        if (connectivityManager == null) return

        try {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    updateCapabilities(network)
                }

                override fun onLost(network: Network) {
                    _connectionState.value = _connectionState.value.copy(
                        isConnected = false,
                        medium = NetworkMedium.NONE,
                        networkName = "Disconnected",
                        downlinkSpeedKbps = 0,
                        uplinkSpeedKbps = 0
                    )
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    parseCapabilities(capabilities)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(callback)
            }
        } catch (_: Exception) {
            // Fallback for restricted environments
        }

        // Initial inspect
        val activeNetwork = connectivityManager?.activeNetwork
        if (activeNetwork != null) {
            updateCapabilities(activeNetwork)
        }
    }

    private fun updateCapabilities(network: Network) {
        val caps = connectivityManager?.getNetworkCapabilities(network)
        if (caps != null) {
            parseCapabilities(caps)
        }
    }

    private fun parseCapabilities(caps: NetworkCapabilities) {
        val isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        val medium: NetworkMedium
        val name: String

        when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                medium = NetworkMedium.WIFI
                name = "Wi-Fi (Active)"
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                medium = NetworkMedium.CELLULAR
                val carrier = try {
                    telephonyManager?.networkOperatorName?.takeIf { it.isNotBlank() } ?: "Cellular 4G/5G"
                } catch (_: Exception) {
                    "Cellular 4G/5G"
                }
                name = carrier
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                medium = NetworkMedium.ETHERNET
                name = "Ethernet"
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> {
                medium = NetworkMedium.VPN
                name = "VPN Tunnel"
            }
            else -> {
                medium = NetworkMedium.NONE
                name = if (hasInternet) "Connected" else "No Internet"
            }
        }

        _connectionState.value = _connectionState.value.copy(
            isConnected = hasInternet,
            medium = medium,
            isMetered = isMetered,
            networkName = name,
            downlinkSpeedKbps = caps.linkDownstreamBandwidthKbps,
            uplinkSpeedKbps = caps.linkUpstreamBandwidthKbps
        )
    }

    private fun startSpeedSamplingLoop() {
        monitorScope.launch {
            while (isActive) {
                delay(1000L)
                val now = System.currentTimeMillis()
                val currentRx = TrafficStats.getTotalRxBytes()
                val currentTx = TrafficStats.getTotalTxBytes()

                val timeDeltaSec = (now - lastTimestamp) / 1000.0

                if (timeDeltaSec > 0.0 && lastRxBytes >= 0L && lastTxBytes >= 0L) {
                    val rxDelta = (currentRx - lastRxBytes).coerceAtLeast(0L)
                    val txDelta = (currentTx - lastTxBytes).coerceAtLeast(0L)

                    val rxSpeed = (rxDelta / timeDeltaSec).toLong()
                    val txSpeed = (txDelta / timeDeltaSec).toLong()

                    _connectionState.value = _connectionState.value.copy(
                        rxBytesPerSec = rxSpeed,
                        txBytesPerSec = txSpeed
                    )
                }

                lastRxBytes = currentRx
                lastTxBytes = currentTx
                lastTimestamp = now
            }
        }
    }
}
