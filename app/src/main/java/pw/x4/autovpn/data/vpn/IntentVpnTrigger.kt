package pw.x4.autovpn.data.vpn

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import pw.x4.autovpn.domain.vpn.LaunchMode
import pw.x4.autovpn.domain.vpn.VpnIntentConfig
import pw.x4.autovpn.domain.vpn.VpnTrigger

/**
 * Реализация VpnTrigger через Android Intent. Поддерживает все режимы LaunchMode:
 * открыть приложение, deeplink, явная Activity/Service/foreground-Service/Broadcast.
 *
 * Запуск из foreground-сервиса (с overlay-доступом) разрешён для всех режимов:
 * startActivity снимается overlay-разрешением, startService/sendBroadcast — тем, что
 * у приложения уже работает свой foreground-сервис.
 */
class IntentVpnTrigger(private val context: Context) : VpnTrigger {

    override fun launch(vpnPackage: String?, config: VpnIntentConfig): Boolean {
        if (vpnPackage.isNullOrBlank()) return false
        return runCatching {
            when (config.mode) {
                LaunchMode.OPEN_APP -> {
                    val intent = context.packageManager.getLaunchIntentForPackage(vpnPackage)
                        ?: return false
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                LaunchMode.DEEPLINK -> {
                    val uri = config.dataUri ?: return false
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                        .setPackage(vpnPackage)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    applyExtras(intent, config)
                    context.startActivity(intent)
                }
                LaunchMode.ACTIVITY -> {
                    val intent = explicit(vpnPackage, config).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
                LaunchMode.SERVICE -> context.startService(explicit(vpnPackage, config))
                LaunchMode.FOREGROUND_SERVICE ->
                    ContextCompat.startForegroundService(context, explicit(vpnPackage, config))
                LaunchMode.BROADCAST -> context.sendBroadcast(explicit(vpnPackage, config))
            }
            true
        }.getOrDefault(false)
    }

    /** Явный Intent на конкретный компонент пакета (или просто на пакет, если класс не задан). */
    private fun explicit(pkg: String, config: VpnIntentConfig): Intent {
        val intent = Intent()
        if (config.componentClass != null) intent.setClassName(pkg, config.componentClass)
        else intent.setPackage(pkg)
        config.action?.let { intent.action = it }
        config.dataUri?.let { intent.data = Uri.parse(it) }
        applyExtras(intent, config)
        return intent
    }

    private fun applyExtras(intent: Intent, config: VpnIntentConfig) {
        config.extras.forEach { (key, value) -> intent.putExtra(key, value) }
    }
}
