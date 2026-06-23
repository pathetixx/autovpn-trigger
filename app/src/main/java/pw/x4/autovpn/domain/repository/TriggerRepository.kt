package pw.x4.autovpn.domain.repository

import kotlinx.coroutines.flow.Flow

/** Хранилище «триггер-приложений» — тех, при открытии которых поднимаем VPN. */
interface TriggerRepository {
    /** Реактивный набор выбранных пакетов (для UI-чекбоксов). */
    val triggerPackages: Flow<Set<String>>

    /** Быстрая точечная проверка (используется в сервисе на каждый кадр опроса). */
    suspend fun isTriggerApp(packageName: String): Boolean

    suspend fun setTrigger(packageName: String, enabled: Boolean)
}
