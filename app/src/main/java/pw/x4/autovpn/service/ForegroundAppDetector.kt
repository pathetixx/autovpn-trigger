package pw.x4.autovpn.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager

/**
 * Определяет приложение на переднем плане через события UsageStatsManager.
 *
 * Почему queryEvents, а не queryUsageStats: агрегированная статистика слишком грубая
 * для realtime-реакции. События же дают точный момент MOVE_TO_FOREGROUND — берём
 * самое свежее за короткое окно.
 *
 * Требует разрешения PACKAGE_USAGE_STATS (см. PermissionUtils.hasUsageAccess).
 */
class ForegroundAppDetector(private val usageStatsManager: UsageStatsManager) {

    fun currentForegroundPackage(lookbackMs: Long = LOOKBACK_MS): String? {
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - lookbackMs, now)

        val event = UsageEvents.Event()
        var foregroundPackage: String? = null
        var latestTs = 0L

        // Прокручиваем все события окна и запоминаем пакет последнего «выхода на передний план».
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND && event.timeStamp >= latestTs) {
                latestTs = event.timeStamp
                foregroundPackage = event.packageName
            }
        }
        return foregroundPackage
    }

    private companion object {
        const val LOOKBACK_MS = 10_000L
    }
}
