package pw.x4.autovpn.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Надёжная проверка «поднят ли VPN прямо сейчас» через ConnectivityManager.
 *
 * Тумблер Happ инвертирует состояние, поэтому ПЕРЕД отправкой broadcast нужно точно
 * знать текущее состояние — иначе случайно выключим уже работающий VPN (или наоборот).
 *
 * Считаем VPN активным, если ЛЮБАЯ сеть имеет транспорт TRANSPORT_VPN (не привязываемся
 * к activeNetwork — при split-tunnel дефолтной может быть не VPN).
 *
 * Требует разрешения ACCESS_NETWORK_STATE.
 */
object VpnStateChecker {

    fun isVpnActive(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        @Suppress("DEPRECATION") // allNetworks ок для разового опроса; callback здесь избыточен
        return cm.allNetworks.any { network ->
            cm.getNetworkCapabilities(network)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }
    }
}
