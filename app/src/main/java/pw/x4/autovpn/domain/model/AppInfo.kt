package pw.x4.autovpn.domain.model

/** Установленное приложение (для списков выбора). Иконка тут не хранится —
 *  грузится в UI лениво через PackageManager, чтобы не тащить Drawable в domain. */
data class AppInfo(
    val packageName: String,
    val label: String,
)
