package pw.x4.autovpn.data.installed

import android.content.pm.PackageManager

enum class ComponentType { ACTIVITY, SERVICE, RECEIVER }

/** Экспортированный компонент стороннего приложения, который МЫ можем дёрнуть. */
data class ExposedComponent(
    val type: ComponentType,
    val className: String,
)

/**
 * Читает манифест стороннего приложения через PackageManager и возвращает его
 * экспортированные (и не защищённые permission) компоненты. Нужно, чтобы найти у VPN
 * недокументированный фоновый «коннект» (Service/Receiver) без adb.
 *
 * Требует видимости пакета — у нас есть QUERY_ALL_PACKAGES.
 */
class AppComponentScanner(private val packageManager: PackageManager) {

    fun scanExported(packageName: String): List<ExposedComponent> = runCatching {
        @Suppress("DEPRECATION") // int-flags overload; на API 33+ есть PackageInfoFlags, но и так работает
        val info = packageManager.getPackageInfo(
            packageName,
            PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS,
        )
        buildList {
            // Сервисы/ресиверы вперёд — они срабатывают без окна, это приоритетные кандидаты.
            info.services?.forEach {
                if (it.exported && it.permission == null) add(ExposedComponent(ComponentType.SERVICE, it.name))
            }
            info.receivers?.forEach {
                if (it.exported && it.permission == null) add(ExposedComponent(ComponentType.RECEIVER, it.name))
            }
            info.activities?.forEach {
                if (it.exported && it.permission == null) add(ExposedComponent(ComponentType.ACTIVITY, it.name))
            }
        }
    }.getOrDefault(emptyList())
}
