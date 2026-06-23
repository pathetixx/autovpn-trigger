package pw.x4.autovpn.domain.vpn

/**
 * Конфиг построения Intent для запуска VPN. Вынесен в отдельную модель, чтобы под
 * каждый VPN-сервис можно было задать свой Action / данные / Extras БЕЗ правок ядра.
 *
 * Примеры:
 *  - Happ по deeplink:  VpnIntentConfig(action = Intent.ACTION_VIEW, dataUri = "happ://")
 *  - просто открыть приложение: VpnIntentConfig() — берётся launch-intent пакета.
 */
data class VpnIntentConfig(
    val action: String? = null,            // null → стандартный launch-intent приложения
    val dataUri: String? = null,           // напр. deeplink "happ://connect"
    val category: String? = null,
    val extras: Map<String, String> = emptyMap(),
    val explicitPackage: Boolean = true,   // true = явный Intent (setPackage), false = неявный
)

/**
 * Абстракция «чем поднимаем VPN». Ядро (сервис) зависит только от этого интерфейса —
 * завтра можно добавить реализацию через Accessibility/shell, не трогая логику мониторинга.
 */
interface VpnTrigger {
    /** @return true, если Intent успешно отправлен системе. */
    fun launch(vpnPackage: String?, config: VpnIntentConfig = VpnIntentConfig()): Boolean
}
