package pw.x4.autovpn.domain.model

import pw.x4.autovpn.domain.vpn.LaunchMode

/** Глобальные настройки автоматизации. */
data class AutomationSettings(
    val automationEnabled: Boolean = false,
    /** Пакет VPN-приложения (например, com.happproxy). */
    val vpnPackage: String? = null,
    /** Broadcast-экшен тумблера VPN. По умолчанию — виджет Happ. */
    val toggleAction: String = DEFAULT_TOGGLE_ACTION,
    /** Поля диагностики (запасной путь «launch/silent-компонент»), основной поток их не использует. */
    val connectMode: LaunchMode = LaunchMode.OPEN_APP,
    val connectComponent: String? = null,
) {
    companion object {
        /** Имитация нажатия виджета Happ — фоновый тумблер VPN без UI. */
        const val DEFAULT_TOGGLE_ACTION = "com.happproxy.action.widget.click"
    }
}
