package pw.x4.autovpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pw.x4.autovpn.AutoVpnApp
import pw.x4.autovpn.R
import pw.x4.autovpn.domain.model.AutomationSettings
import pw.x4.autovpn.ui.MainActivity

/**
 * Ядро автоматизации.
 *
 * Foreground Service с постоянным уведомлением — единственный надёжный способ держать
 * фоновый опрос живым: Android агрессивно убивает обычные сервисы, а foreground с
 * видимым уведомлением живёт пока работает приложение пользователя.
 *
 * Цикл: раз в POLL_INTERVAL_MS спрашиваем у детектора текущее приложение; если оно
 * сменилось и попало в «чёрный список» — отправляем Intent на запуск VPN.
 */
class AppMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val container by lazy { (application as AutoVpnApp).container }

    /**
     * Пакет, для которого VPN уже подняли в текущей «сессии» этого приложения.
     * Нужен, чтобы не дёргать VPN повторно, пока пользователь сидит в том же триггере.
     * Сбрасывается, как только фокус ушёл на не-триггерное приложение.
     */
    private var lastTriggeredPackage: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // На Android 14+ обязаны указать тип FGS при старте — иначе ANR/исключение.
        val fgsType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), fgsType)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMonitoring()
        // START_STICKY: если систему ужмёт по памяти и убьёт сервис — она его перезапустит.
        return START_STICKY
    }

    private fun startMonitoring() {
        scope.launch {
            var previousPackage: String? = null
            while (isActive) {
                val settings = container.settingsRepository.settings.first()

                // Тумблер выключили во время работы → сервису незачем жить.
                if (!settings.automationEnabled) {
                    stopSelf()
                    break
                }

                val current = container.foregroundAppDetector.currentForegroundPackage()
                if (current != null && current != previousPackage) {
                    handleForegroundChange(current, settings)
                    previousPackage = current
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun handleForegroundChange(packageName: String, settings: AutomationSettings) {
        // Игнорируем самих себя и сам VPN — иначе получим петлю запусков.
        if (packageName == getPackageName() || packageName == settings.vpnPackage) return

        if (container.triggerRepository.isTriggerApp(packageName)) {
            if (packageName != lastTriggeredPackage) {
                container.vpnTrigger.launch(settings.vpnPackage)
                lastTriggeredPackage = packageName
            }
        } else {
            // Фокус ушёл из триггера → при следующем входе снова разрешаем запуск.
            lastTriggeredPackage = null
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // ── Уведомление ────────────────────────────────────────────────────────────

    private fun buildNotification(): Notification {
        ensureChannel()
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(openApp)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notif_channel),
                    NotificationManager.IMPORTANCE_LOW, // тихое уведомление-индикатор
                ),
            )
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "vpn_automation"
        private const val POLL_INTERVAL_MS = 1_500L

        /** Старт сервиса как foreground (корректно для всех версий). */
        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AppMonitorService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppMonitorService::class.java))
        }
    }
}
