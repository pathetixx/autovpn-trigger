package pw.x4.autovpn.domain.model

import pw.x4.autovpn.domain.vpn.LaunchMode

/** Глобальные настройки автоматизации. */
data class AutomationSettings(
    val automationEnabled: Boolean = false,
    /** Пакет VPN-приложения (например, com.happproxy). */
    val vpnPackage: String? = null,
    /** Как именно дёргать VPN. OPEN_APP = просто открыть (по умолчанию). */
    val connectMode: LaunchMode = LaunchMode.OPEN_APP,
    /** FQN компонента для тихого коннекта (Service/Receiver/Activity), если найден в диагностике. */
    val connectComponent: String? = null,
)
