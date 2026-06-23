package pw.x4.autovpn.domain.vpn

/**
 * Способ доставки сигнала VPN-приложению. Разные VPN выставляют наружу разное:
 * у кого-то deeplink, у кого-то фоновый Service/Receiver (коннект без окна).
 */
enum class LaunchMode {
    OPEN_APP,            // launch-intent приложения — VPN откроется ПОВЕРХ (видно окно)
    DEEPLINK,            // ACTION_VIEW happ://... — обычно тоже открывает окно
    ACTIVITY,            // явная Activity по имени класса — окно мелькнёт
    SERVICE,             // явный Service — фон, БЕЗ окна
    FOREGROUND_SERVICE,  // явный foreground-Service — фон, БЕЗ окна
    BROADCAST,           // явный Receiver — фон, БЕЗ окна
}

/**
 * Конфиг построения Intent. Под каждый VPN-сервис можно задать свой режим/компонент/
 * Action/данные/Extras БЕЗ правок ядра.
 */
data class VpnIntentConfig(
    val mode: LaunchMode = LaunchMode.OPEN_APP,
    val componentClass: String? = null, // FQN экспортированного компонента (для ACTIVITY/SERVICE/BROADCAST)
    val action: String? = null,
    val dataUri: String? = null,        // напр. deeplink "happ://..."
    val extras: Map<String, String> = emptyMap(),
)

/** Абстракция «чем поднимаем VPN». Ядро зависит только от этого интерфейса. */
interface VpnTrigger {
    /** @return true, если запрос успешно отправлен системе (НЕ гарантирует, что VPN реально включился). */
    fun launch(vpnPackage: String?, config: VpnIntentConfig = VpnIntentConfig()): Boolean
}
