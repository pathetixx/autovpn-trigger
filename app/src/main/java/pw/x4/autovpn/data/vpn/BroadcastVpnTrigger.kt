package pw.x4.autovpn.data.vpn

import android.content.Context
import android.content.Intent

/**
 * Тихий коннект без UI: шлём таргетированный broadcast, имитируя нажатие виджета VPN.
 * Для Happ это action "com.happproxy.action.widget.click" — он работает как ТУМБЛЕР
 * (инвертирует состояние), поэтому отправлять его должен только тот, кто уже сверил
 * фактическое состояние VPN (см. VpnStateChecker), иначе можно выключить рабочий VPN.
 */
class BroadcastVpnTrigger(private val context: Context) {

    /**
     * @param vpnPackage целевой пакет (напр. com.happproxy)
     * @param action     broadcast-экшен тумблера
     * @return true, если broadcast отправлен системе
     */
    fun sendToggle(vpnPackage: String, action: String): Boolean = runCatching {
        // setPackage обязателен: с Android 8 неявные broadcast'ы по статическим receiver'ам
        // не доставляются. Явная адресация на пакет обходит это ограничение.
        val intent = Intent(action).setPackage(vpnPackage)
        context.sendBroadcast(intent)
        true
    }.getOrDefault(false)
}
