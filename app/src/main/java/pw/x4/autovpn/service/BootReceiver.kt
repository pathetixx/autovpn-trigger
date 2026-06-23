package pw.x4.autovpn.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Поднимает мониторинг после перезагрузки устройства.
 * Сам сервис при старте проверит флаг automationEnabled и остановится, если выключено,
 * — поэтому здесь не нужно читать настройки.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AppMonitorService.start(context)
        }
    }
}
