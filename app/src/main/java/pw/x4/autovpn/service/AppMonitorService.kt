package pw.x4.autovpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

    // Пакет лаунчера (домашний экран) — определяем один раз, это «нейтральный» экран.
    private val launcherPackage: String? by lazy {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
    }

    // Состояние автомата.
    private var offPendingSince = 0L  // когда впервые увидели «нужно выключить»
    private var hasOffPending = false
    private var cooldownUntil = 0L    // до этого момента не трогаем VPN (ждём смены состояния)

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

    /** Один такт автомата. */
    private suspend fun evaluate(settings: AutomationSettings) {
        val vpnPackage = settings.vpnPackage ?: return // нечего тумблить
        val current = container.foregroundAppDetector.currentForegroundPackage() ?: return

        // Нейтральные экраны (наше приложение, сам Happ, домашний экран, системный UI/
        // «Недавние») НЕ меняют состояние — это и есть защита от дребезга при сворачивании.
        if (isNeutral(current, settings)) return

        val now = SystemClock.elapsedRealtime()
        if (now < cooldownUntil) return // после тумбла ждём, пока VPN реально переключится

        val wantVpn = container.triggerRepository.isTriggerApp(current)
        val vpnOn = VpnStateChecker.isVpnActive(this)

        // Уже в нужном состоянии (в т.ч. переход триггер→триггер: want=on, vpnOn=on).
        if (wantVpn == vpnOn) {
            hasOffPending = false
            return
        }

        if (wantVpn) {
            // Нужно ВКЛЮЧИТЬ — реагируем сразу.
            hasOffPending = false
            sendToggle(vpnPackage, settings.toggleAction, turningOn = true, now)
        } else {
            // Нужно ВЫКЛЮЧИТЬ — с дебаунсом, чтобы краткий выход не дёргал VPN.
            if (!hasOffPending) {
                hasOffPending = true
                offPendingSince = now
                return
            }
            if (now - offPendingSince >= OFF_DEBOUNCE_MS) {
                hasOffPending = false
                sendToggle(vpnPackage, settings.toggleAction, turningOn = false, now)
            }
        }
    }

    private suspend fun sendToggle(vpnPackage: String, action: String, turningOn: Boolean, now: Long) {
        container.broadcastVpnTrigger.sendToggle(vpnPackage, action)
        cooldownUntil = now + TOGGLE_COOLDOWN_MS
        toast(if (turningOn) "VPN: включаю" else "VPN: выключаю")
    }

    /** Экраны, которые не должны влиять на состояние VPN. */
    private fun isNeutral(pkg: String, settings: AutomationSettings): Boolean =
        pkg == packageName ||
            pkg == settings.vpnPackage ||
            pkg == launcherPackage ||
            pkg == SYSTEM_UI_PACKAGE

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
        private const val POLL_INTERVAL_MS = 1_500L
        private const val OFF_DEBOUNCE_MS = 3_000L      // выход из триггера держим 3с до выключения
        private const val TOGGLE_COOLDOWN_MS = 8_000L   // после тумбла даём VPN устаканиться (Happ под троттлом коннектится не мгновенно)
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
