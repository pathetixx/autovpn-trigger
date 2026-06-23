package pw.x4.autovpn.service

import org.junit.Assert.assertEquals
import org.junit.Test

class AutomationDeciderTest {

    @Test
    fun neutral_does_nothing() {
        assertEquals(
            VpnAction.NONE,
            AutomationDecider.decide(isNeutral = true, isTrigger = true, vpnOn = false, respectManualVpn = false, ownsVpn = false),
        )
    }

    @Test
    fun trigger_with_vpn_off_turns_on() {
        assertEquals(
            VpnAction.TURN_ON,
            AutomationDecider.decide(isNeutral = false, isTrigger = true, vpnOn = false, respectManualVpn = false, ownsVpn = false),
        )
    }

    @Test
    fun trigger_with_vpn_on_does_nothing() {
        assertEquals(
            VpnAction.NONE,
            AutomationDecider.decide(isNeutral = false, isTrigger = true, vpnOn = true, respectManualVpn = false, ownsVpn = true),
        )
    }

    @Test
    fun other_app_with_vpn_on_turns_off() {
        assertEquals(
            VpnAction.TURN_OFF,
            AutomationDecider.decide(isNeutral = false, isTrigger = false, vpnOn = true, respectManualVpn = false, ownsVpn = true),
        )
    }

    @Test
    fun other_app_with_vpn_off_does_nothing() {
        assertEquals(
            VpnAction.NONE,
            AutomationDecider.decide(isNeutral = false, isTrigger = false, vpnOn = false, respectManualVpn = false, ownsVpn = false),
        )
    }

    @Test
    fun respect_manual_keeps_foreign_vpn() {
        // VPN включён не нами (ownsVpn=false) + режим уважения → не трогаем.
        assertEquals(
            VpnAction.NONE,
            AutomationDecider.decide(isNeutral = false, isTrigger = false, vpnOn = true, respectManualVpn = true, ownsVpn = false),
        )
    }

    @Test
    fun respect_manual_still_off_for_owned() {
        assertEquals(
            VpnAction.TURN_OFF,
            AutomationDecider.decide(isNeutral = false, isTrigger = false, vpnOn = true, respectManualVpn = true, ownsVpn = true),
        )
    }
}
