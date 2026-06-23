package pw.x4.autovpn.data.vpn

import android.content.Context
import android.content.Intent
import android.net.Uri
import pw.x4.autovpn.domain.vpn.VpnIntentConfig
import pw.x4.autovpn.domain.vpn.VpnTrigger

/**
 * Реализация VpnTrigger через Android Intent. Умеет два режима:
 *  1) action == null  → стандартный launch-intent приложения (просто «открыть VPN»);
 *  2) action задан     → собрать кастомный явный/неявный Intent (deeplink, extras и т.д.).
 */
class IntentVpnTrigger(private val context: Context) : VpnTrigger {

    override fun launch(vpnPackage: String?, config: VpnIntentConfig): Boolean {
        if (vpnPackage.isNullOrBlank()) return false
        val intent = buildIntent(vpnPackage, config) ?: return false

        // FLAG_ACTIVITY_NEW_TASK обязателен: startActivity вызывается из Service,
        // а не из Activity-контекста — иначе система кинет исключение.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)  // нет такого приложения / Activity не найдена — мягко возвращаем false
    }

    private fun buildIntent(vpnPackage: String, config: VpnIntentConfig): Intent? {
        // Режим 1: просто запустить приложение по его пакету.
        if (config.action == null) {
            return context.packageManager.getLaunchIntentForPackage(vpnPackage)
        }
        // Режим 2: собрать Intent вручную под конкретный VPN-сервис.
        return Intent(config.action).apply {
            if (config.explicitPackage) setPackage(vpnPackage)
            config.dataUri?.let { data = Uri.parse(it) }
            config.category?.let { addCategory(it) }
            config.extras.forEach { (key, value) -> putExtra(key, value) }
        }
    }
}
