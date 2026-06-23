package pw.x4.autovpn.domain.model

/** Глобальные настройки автоматизации. */
data class AutomationSettings(
    val automationEnabled: Boolean = false,
    /** Пакет VPN-приложения (например, com.happproxy). */
    val vpnPackage: String? = null,
    /** Broadcast-экшен тумблера VPN. По умолчанию — виджет Happ. */
    val toggleAction: String = DEFAULT_TOGGLE_ACTION,
    /** Проверять обновления при запуске. */
    val autoUpdateEnabled: Boolean = true,
    /**
     * true → не трогаем VPN, включённый вручную (не нами): «ручной VPN на всё вне правил».
     * false → жёсткий детерминизм: в не-target приложении VPN выключается, кто бы его ни включил.
     */
    val respectManualVpn: Boolean = false,
) {
    companion object {
        /** Имитация нажатия виджета Happ — фоновый тумблер VPN без UI. */
        const val DEFAULT_TOGGLE_ACTION = "com.happproxy.action.widget.click"
    }
}
