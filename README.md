# AutoVPN Trigger

Лёгкий аналог iOS Shortcuts под одну задачу: при открытии выбранных приложений
(Instagram и т.п.) автоматически поднимать стороннее VPN-приложение (Happ и др.)
через Android Intent.

## Стек
Kotlin · Jetpack Compose (Material3) · Clean Architecture (data/domain/ui) ·
Room (список триггеров) · DataStore (настройки) · Foreground Service + UsageStatsManager.

## Архитектура (слои)
```
domain/   модели + интерфейсы (TriggerRepository, SettingsRepository, VpnTrigger)
data/     Room, DataStore, репозитории, InstalledAppsProvider, IntentVpnTrigger
service/  ForegroundAppDetector, AppMonitorService (ядро), BootReceiver
ui/       MainActivity, MainScreen (Compose), MainViewModel (MVVM)
AutoVpnApp.kt — ручной DI-контейнер (без Hilt, чтобы держать сборку лёгкой)
```
Поток: `AppMonitorService` опрашивает `ForegroundAppDetector` (UsageEvents) → если
текущий пакет есть в Room-«чёрном списке» → `VpnTrigger.launch(vpnPackage)`.

## Ключевые разрешения (как просить)
| Разрешение | Тип | Как выдаётся |
|---|---|---|
| `PACKAGE_USAGE_STATS` | особое (appop) | НЕ диалогом. Кнопка ведёт на `Settings.ACTION_USAGE_ACCESS_SETTINGS`; статус проверяем через `AppOpsManager` (`PermissionUtils.hasUsageAccess`). |
| `POST_NOTIFICATIONS` | runtime (API 33+) | `rememberLauncherForActivityResult(RequestPermission())`. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | normal | объявлено в манифесте; тип FGS передаём в `startForeground`. |

UI сам показывает карточку-предупреждение, пока разрешение не выдано, и обновляет
статус на `ON_RESUME` (возврат из системных настроек).

## Сборка (GitHub Actions)
Локально не собираем. Workflow `.github/workflows/android.yml`:
1. качает Gradle 8.11.1, им же генерит wrapper (бинарь jar в репо не храним);
2. `./gradlew assembleDebug`;
3. кладёт `app-debug.apk` в артефакты.

Запуск: push в `main`/`master` или вручную (`workflow_dispatch`).

## Пошаговый план
1. **Создать репозиторий** на GitHub, запушить этот каталог.
2. **CI** соберёт debug-APK → скачать из артефактов, поставить на устройство.
3. **Выдать разрешения**: Usage Access + уведомления (экран сам подскажет).
4. **Выбрать VPN-службу** (например, Happ) и отметить триггер-приложения.
5. **Включить тумблер** → поднимается foreground-служба.
6. **Проверить**: открыть Instagram → должен запуститься VPN.

## Кастомизация под конкретный VPN
`VpnTrigger`/`VpnIntentConfig` рассчитаны на расширение. Пример — запуск Happ по
deeplink вместо обычного открытия:
```kotlin
vpnTrigger.launch(
    vpnPackage = "com.happproxy",
    config = VpnIntentConfig(action = Intent.ACTION_VIEW, dataUri = "happ://")
)
```
Сейчас сервис вызывает `launch(vpnPackage)` (режим «просто открыть приложение»).
Когда определишь точный Action/deeplink нужного VPN — пробрасывается через `VpnIntentConfig`.

## Что осталось доделать (следующие этапы)
- иконки приложений в списке (сейчас текст; грузить `Drawable` через PackageManager);
- хранить выбранный `VpnIntentConfig` в настройках (UI выбора режима запуска);
- battery-optimization whitelist (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) для надёжности фона;
- релиз-workflow с подписанным release-APK.
```
