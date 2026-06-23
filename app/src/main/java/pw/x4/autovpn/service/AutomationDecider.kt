package pw.x4.autovpn.service

/** Что сделать с VPN на текущем такте. */
enum class VpnAction { NONE, TURN_ON, TURN_OFF }

/**
 * Чистая (без Android) логика автомата — вынесена ради юнит-тестов.
 *
 * Neutral → не трогаем. Target → включить, если выключен. AnyOther → выключить, если
 * включён (но в режиме respectManualVpn не трогаем VPN, поднятый не нами).
 */
object AutomationDecider {
    fun decide(
        isNeutral: Boolean,
        isTrigger: Boolean,
        vpnOn: Boolean,
        respectManualVpn: Boolean,
        ownsVpn: Boolean,
    ): VpnAction = when {
        isNeutral -> VpnAction.NONE
        isTrigger -> if (!vpnOn) VpnAction.TURN_ON else VpnAction.NONE
        !vpnOn -> VpnAction.NONE
        respectManualVpn && !ownsVpn -> VpnAction.NONE
        else -> VpnAction.TURN_OFF
    }
}
