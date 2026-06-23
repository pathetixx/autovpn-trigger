package pw.x4.autovpn.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pw.x4.autovpn.AutoVpnApp

/** Плитка «Быстрых настроек»: вкл/выкл автоматизацию AutoVPN из шторки. */
class AutomationTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val container by lazy { (application as AutoVpnApp).container }

    override fun onStartListening() {
        scope.launch { setTile(automationEnabled()) }
    }

    override fun onClick() {
        scope.launch {
            val next = !automationEnabled()
            container.settingsRepository.setAutomationEnabled(next)
            if (next) AppMonitorService.start(this@AutomationTileService)
            else AppMonitorService.stop(this@AutomationTileService)
            setTile(next)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun automationEnabled(): Boolean =
        container.settingsRepository.settings.first().automationEnabled

    private fun setTile(active: Boolean) {
        qsTile?.apply {
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "AutoVPN"
            updateTile()
        }
    }
}
