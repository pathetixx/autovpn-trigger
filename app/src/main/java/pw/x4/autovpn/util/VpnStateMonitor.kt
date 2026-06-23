package pw.x4.autovpn.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.util.Collections

/**
 * Событийное отслеживание VPN: подписка на сети с TRANSPORT_VPN. Точнее и дешевле
 * периодического опроса — состояние обновляется в момент появления/исчезновения тоннеля.
 */
class VpnStateMonitor(context: Context) {

    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val vpnNetworks = Collections.synchronizedSet(mutableSetOf<Network>())

    @Volatile
    var isActive: Boolean = false
        private set

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            vpnNetworks.add(network)
            isActive = vpnNetworks.isNotEmpty()
        }

        override fun onLost(network: Network) {
            vpnNetworks.remove(network)
            isActive = vpnNetworks.isNotEmpty()
        }
    }

    fun start() {
        // ВАЖНО: по умолчанию NetworkRequest несёт NET_CAPABILITY_NOT_VPN — его надо
        // снять, иначе VPN-сети как раз отфильтруются. registerNetworkCallback также
        // отдаёт onAvailable для уже поднятых подходящих сетей — начальное состояние ок.
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()
        runCatching { connectivityManager?.registerNetworkCallback(request, callback) }
    }

    fun stop() {
        runCatching { connectivityManager?.unregisterNetworkCallback(callback) }
        vpnNetworks.clear()
        isActive = false
    }
}
