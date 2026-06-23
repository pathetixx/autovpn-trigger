package pw.x4.autovpn.service

import android.app.AlarmManager
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
import android.os.SystemClock
import android.widget.Toast
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
import kotlinx.coroutines.withContext
import pw.x4.autovpn.AutoVpnApp
import pw.x4.autovpn.R
import pw.x4.autovpn.domain.model.AutomationSettings
import pw.x4.autovpn.ui.MainActivity
import pw.x4.autovpn.util.VpnStateMonitor

/**
 * Ядро автоматизации. Foreground-служба раз в POLL_INTERVAL_MS сверяет нужное состояние
 * VPN с фактическим (VpnStateMonitor, событийно) и при рассинхроне шлёт broadcast-тумблер.
 * Решение принимает чистый [AutomationDecider]. Тумблер инвертирует — после отправки
 * держим кулдаун, пока состояние не устаканится.
 */
class AppMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val container by lazy { (application as AutoVpnApp).container }
    private val vpnState by lazy { VpnStateMonitor(this) }

    private var ownsVpn = false       // VPN включили МЫ (для режима «уважать ручной VPN»)
    private var cooldownUntil = 0L
    private var statusText = "Слежу за приложениями"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val fgsType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), fgsType)
        vpnState.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // «Стоп» из уведомления — выключаем автоматизацию и гасим службу.
            scope.launch {
                container.settingsRepository.setAutomationEnabled(false)
                stopSelf()
            }
            return START_NOT_STICKY
        }
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        scope.launch {
            while (isActive) {
                val settings = container.settingsRepository.settings.first()
                if (!settings.automationEnabled) {
                    stopSelf()
                    break
                }
                evaluate(settings)
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun evaluate(settings: AutomationSettings) {
        val vpnPackage = settings.vpnPackage ?: return
        val current = container.foregroundAppDetector.currentForegroundPackage() ?: return
        val now = SystemClock.elapsedRealtime()
        if (now < cooldownUntil) return // после тумбла ждём, пока VPN переключится

        val vpnOn = vpnState.isActive
        if (!vpnOn) ownsVpn = false

        val action = AutomationDecider.decide(
            isNeutral = isNeutral(current, vpnPackage),
            isTrigger = container.triggerRepository.isTriggerApp(current),
            vpnOn = vpnOn,
            respectManualVpn = settings.respectManualVpn,
            ownsVpn = ownsVpn,
        )
        when (action) {
            VpnAction.NONE -> Unit
            VpnAction.TURN_ON -> {
                sendToggle(vpnPackage, settings.toggleAction, now)
                ownsVpn = true
                updateNotification("VPN включён")
            }
            VpnAction.TURN_OFF -> {
                sendToggle(vpnPackage, settings.toggleAction, now)
                ownsVpn = false
                updateNotification("VPN выключен")
            }
        }
    }

    private suspend fun sendToggle(vpnPackage: String, toggleAction: String, now: Long) {
        container.broadcastVpnTrigger.sendToggle(vpnPackage, toggleAction)
        cooldownUntil = now + TOGGLE_COOLDOWN_MS
        toast("VPN: переключаю")
    }

    /** Экраны, которые не должны влиять на состояние VPN. */
    private fun isNeutral(pkg: String, vpnPackage: String): Boolean =
        pkg == packageName || pkg == vpnPackage || pkg == SYSTEM_UI_PACKAGE

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Пользователь смахнул приложение из «Недавних» — агрессивные OEM валят службу.
        // Планируем рестарт через AlarmManager (foreground-service).
        val restart = PendingIntent.getForegroundService(
            this,
            REQ_RESTART,
            Intent(this, AppMonitorService::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        getSystemService(AlarmManager::class.java)
            ?.set(AlarmManager.RTC, System.currentTimeMillis() + RESTART_DELAY_MS, restart)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        vpnState.stop()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun toast(message: String) = withContext(Dispatchers.Main) {
        Toast.makeText(this@AppMonitorService, message, Toast.LENGTH_SHORT).show()
    }

    // ── Уведомление ────────────────────────────────────────────────────────────

    private fun updateNotification(status: String) {
        statusText = status
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        ensureChannel()
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            REQ_STOP,
            Intent(this, AppMonitorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(openApp)
            .addAction(0, "Стоп", stop)
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
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "vpn_automation"
        private const val POLL_INTERVAL_MS = 1_500L
        private const val TOGGLE_COOLDOWN_MS = 8_000L
        private const val RESTART_DELAY_MS = 2_000L
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        private const val REQ_STOP = 2
        private const val REQ_RESTART = 3
        const val ACTION_STOP = "pw.x4.autovpn.action.STOP"

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
