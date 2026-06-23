package pw.x4.autovpn.domain.model

/** Глобальные настройки автоматизации. */
data class AutomationSettings(
    val automationEnabled: Boolean = false,
    /** Пакет VPN-приложения, которое поднимаем (например, com.happproxy). */
    val vpnPackage: String? = null,
)
