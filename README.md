# AutoVPN

Лёгкий аналог iOS Shortcuts под одну задачу: **автоматически включать VPN при открытии
выбранных приложений** (Instagram и т.п.) и выключать в остальных — без ручных действий
и без всплывающего окна VPN-клиента.

Управление VPN идёт через **фоновый broadcast-тумблер** Happ, поэтому VPN поднимается
прямо в фоне (UI клиента не открывается).

## Как это работает

- **Мониторинг.** Foreground-служба (`AppMonitorService`) раз в 1.5 c через
  `UsageStatsManager` узнаёт приложение на переднем плане.
- **Тихий коннект.** Вместо запуска UI шлём targeted broadcast
  `com.happproxy.action.widget.click` на пакет `com.happproxy` — имитация нажатия
  виджета Happ. Это **тумблер** (инвертирует состояние).
- **Состояние VPN** читаем через `ConnectivityManager` (есть ли сеть с
  `NetworkCapabilities.TRANSPORT_VPN`), плюс локальный кэш — чтобы не слать лишних
  broadcast'ов и не словить гонку, пока Happ переключается.
- **Детерминированный автомат** по фокусному пакету:

  | Категория | Условие | Действие |
  |---|---|---|
  | **Target** (из списка) | VPN выключен | включить |
  | **Neutral** (наше приложение / Happ / `com.android.systemui`) | — | ничего |
  | **AnyOther** (любое прочее: лаунчер, браузер, мессенджер) | VPN включён | выключить |

  То есть переключение из Instagram в обычное приложение сразу гасит VPN; переход
  между двумя target-приложениями VPN не трогает.

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

## Сборка и релизы (GitHub Actions)

Локально не собираем — всё через CI ([`.github/workflows/android.yml`](.github/workflows/android.yml)).
Wrapper не коммитим: CI качает Gradle 8.11.1 и генерит wrapper сам.

- **push / PR** → `assembleDebug`, debug-APK в артефактах (валидация компиляции).
- **тег `vX.Y.Z`** → подписанный `assembleRelease` + автоматический GitHub Release с
  `autovpn-<tag>.apk`.

Релиз подписывается стабильным ключом из GitHub Secrets
(`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
`ANDROID_KEY_PASSWORD`) — стабильная подпись обязательна, иначе OTA-обновление не
встанет поверх.

**Выпуск новой версии:** поднять `versionName` в [`app/build.gradle.kts`](app/build.gradle.kts)
и поставить тег `vX.Y.Z` — остальное сделает CI.
