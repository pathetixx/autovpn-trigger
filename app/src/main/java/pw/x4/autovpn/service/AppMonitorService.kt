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
import pw.x4.autovpn.util.VpnStateChecker

/**
 * Ядро автоматизации — двусторонний конечный автомат.
 *
 * Каждые POLL_INTERVAL_MS:
 *  1) узнаём приложение на переднем плане;
 *  2) считаем НУЖНОЕ состояние VPN: вошли в триггер → ON, иначе → OFF;
 *  3) сверяем с ФАКТИЧЕСКИМ (ConnectivityManager / TRANSPORT_VPN);
 *  4) если расходятся — шлём broadcast-тумблер Happ.
 *
 * Тумблер инвертирует состояние, поэтому критично слать его ТОЛЬКО при рассинхроне,
 * иначе выключим рабочий VPN. Плюс защита от дребезга: нейтральные экраны игнорим,
 * выключение — с дебаунсом, после любого тумбла — кулдаун (ждём, пока VPN устаканится).
 */
class AppMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val container by lazy { (application as AutoVpnApp).container }

    // Локальный кэш состояния VPN — решения строим на нём, реальный CM дёргаем лишь
    // периодически (ловим ручное вкл/выкл) и не во время «перехода» после тумбла.
    private var lastKnownVpnState: Boolean? = null
    private var lastSyncAt = 0L
    private var transitionUntil = 0L
    private var ownsVpn = false       // VPN включили МЫ (для режима «уважать ручной VPN»)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val fgsType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), fgsType)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

    /**
     * Один такт автомата — строгая детерминированная логика Target / Neutral / Other.
     * Без дебаунса: сама категоризация фокусного пакета гасит дребезг.
     */
    private suspend fun evaluate(settings: AutomationSettings) {
        val vpnPackage = settings.vpnPackage ?: return // нечего тумблить
        val current = container.foregroundAppDetector.currentForegroundPackage() ?: return
        val now = SystemClock.elapsedRealtime()

        // Синхронизация кэша с реальным состоянием: при старте, периодически (ловим
        // ручное вкл/выкл) — но НЕ во время перехода после тумбла (CM ещё врёт).
        if (lastKnownVpnState == null ||
            (now >= transitionUntil && now - lastSyncAt >= SYNC_INTERVAL_MS)
        ) {
            lastKnownVpnState = VpnStateChecker.isVpnActive(this)
            lastSyncAt = now
        }
        val vpnOn = lastKnownVpnState == true
        if (!vpnOn) ownsVpn = false // VPN не активен — владеть нечем

        when {
            // NeutralApps — наше приложение, сам VPN, системный UI (шторка/Недавние).
            // Состояние не трогаем, чтобы они не сбивали логику.
            current == packageName ||
                current == vpnPackage ||
                current == SYSTEM_UI_PACKAGE -> return

            // TargetApps + VPN выключен → включаем.
            container.triggerRepository.isTriggerApp(current) -> {
                if (!vpnOn) {
                    applyToggle(vpnPackage, settings.toggleAction, turnOn = true, now)
                    ownsVpn = true
                }
            }

            // AnyOtherApp + VPN включён → выключаем.
            else -> {
                if (vpnOn) {
                    // Режим «уважать ручной VPN»: чужой (не наш) VPN не гасим.
                    if (settings.respectManualVpn && !ownsVpn) return
                    applyToggle(vpnPackage, settings.toggleAction, turnOn = false, now)
                }
            }
        }
    }

    private suspend fun applyToggle(vpnPackage: String, action: String, turnOn: Boolean, now: Long) {
        container.broadcastVpnTrigger.sendToggle(vpnPackage, action)
        lastKnownVpnState = turnOn          // обновляем кэш сразу после отправки
        transitionUntil = now + TRANSITION_MS
        lastSyncAt = now
        toast(if (turnOn) "VPN: включаю" else "VPN: выключаю")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun toast(message: String) = withContext(Dispatchers.Main) {
        Toast.makeText(this@AppMonitorService, message, Toast.LENGTH_SHORT).show()
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
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "vpn_automation"
        private const val POLL_INTERVAL_MS = 1_500L     // 1.5с достаточно; Target/Neutral/Other сам гасит дребезг
        private const val SYNC_INTERVAL_MS = 4_500L     // как часто сверяем кэш с реальным CM (ловим ручное вкл/выкл)
        private const val TRANSITION_MS = 7_000L        // после тумбла CM врёт (Happ коннектится) — верим кэшу
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"

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
