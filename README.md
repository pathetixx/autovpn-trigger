# AutoVPN

Аналог iOS Shortcuts под одну задачу: **VPN включается при открытии выбранных приложений**
(Instagram и т.п.) и выключается в остальных. Управление через фоновый broadcast-тумблер
Happ — без всплывающего окна клиента.

## Как это работает

- Foreground-служба раз в 1.5 c через `UsageStatsManager` определяет приложение на переднем плане.
- Коннект — targeted broadcast `com.happproxy.action.widget.click` на пакет `com.happproxy`
  (тумблер, инвертирует состояние).
- Состояние VPN — через `ConnectivityManager` (`TRANSPORT_VPN`) + локальный кэш.
- Автомат по фокусному пакету:

  | Категория | Условие | Действие |
  |---|---|---|
  | **Target** (из списка) | VPN выключен | включить |
  | **Neutral** (само приложение / Happ / `com.android.systemui`) | — | — |
  | **AnyOther** (всё прочее) | VPN включён | выключить |

  Переход между двумя target-приложениями VPN не трогает.

## Установка

1. Скачать APK из [Releases](https://github.com/pathetixx/autovpn-trigger/releases/latest).
2. Поставить (нужно разрешить установку из неизвестных источников).
3. Выдать разрешения — приложение само показывает карточки:
   - **Доступ к данным об использовании** (Usage Access) — без него не видно открытое приложение;
   - **Уведомления** (Android 13+) — иначе система может убить фоновую службу;
   - **Отключить энергосбережение** для приложения — для надёжной работы в фоне.
4. Один раз **подключиться в Happ вручную** (выбрать сервер + выдать системный consent на VPN).
5. В AutoVPN выбрать **Happ** как VPN-службу, отметить нужные приложения, включить тумблер.

> ⚠️ При обновлении с раннего **debug**-APK на подписанный релиз — сначала удалить старую
> версию (разные подписи). Дальше обновления встают поверх через автообновление.

## Настройки (вкладка ⚙)

- **Автообновление** — проверять новые версии при запуске.
- **Проверить наличие обновлений** — сравнивает с последним GitHub Release; при новой
  версии качает APK и отдаёт системному установщику (OTA).
- **Уважать ручной VPN** — если включить, VPN, поднятый вручную в Happ, не выключается
  в не-target приложениях (работает «на всё вне правил»). По умолчанию выключено
  (жёсткий детерминизм).
- **Debug** — текущее состояние VPN и ручной тест broadcast-тумблера.

## Под другой VPN

Экшен тумблера хранится в настройках (`AutomationSettings.toggleAction`, дефолт —
виджет Happ) и переопределяется. Для другого клиента достаточно подставить его пакет
(VPN-служба) и broadcast-экшен его виджета/тумблера.

## Архитектура (Clean / MVVM)

```
domain/   модели + интерфейсы (TriggerRepository, SettingsRepository)
data/     Room (триггеры), DataStore (настройки), InstalledAppsProvider,
          BroadcastVpnTrigger (тумблер), UpdateManager (OTA)
service/  ForegroundAppDetector, AppMonitorService (автомат), BootReceiver
util/     VpnStateChecker (ConnectivityManager), PermissionUtils
ui/       AppRoot (нижняя навигация) → HomeScreen / SettingsScreen / DebugScreen, MainViewModel
AutoVpnApp.kt — ручной DI-контейнер (без Hilt)
```

## Сборка

GitHub Actions:

- push / PR → debug-APK в артефактах;
- тег `vX.Y.Z` → подписанный релиз с APK.

Новая версия: поднять `versionName` в [`app/build.gradle.kts`](app/build.gradle.kts) и поставить тег `vX.Y.Z`.
