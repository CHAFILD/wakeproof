# WakeProof

Будильник, который нельзя выключить без решения задач. Android, minSdk 28,
Kotlin + Jetpack Compose (Material 3), MVVM + Clean Architecture, Room,
ручной DI (без Hilt).

Единственный источник правды по требованиям — `ТЗ_WakeProof_v1.1.md`.

## Сборка

```bash
./gradlew assembleDebug        # debug APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # unit-тесты (JUnit 5)
```

Требования к окружению:

- JDK 17+ (проект собирается на Temurin 21).
- Android SDK с platform android-35 и build-tools 34.0.0.
- Путь к SDK — в `local.properties` (`sdk.dir=...`), файл не коммитится.

Версии стека (см. `gradle/libs.versions.toml`):

| Компонент | Версия |
|---|---|
| Gradle | 8.9 |
| AGP | 8.7.3 |
| Kotlin | 2.0.21 |
| KSP | 2.0.21-1.0.27 |
| Compose BOM | 2024.12.01 |
| Room | 2.6.1 |
| Navigation Compose | 2.8.5 |
| JUnit Jupiter | 5.11.4 |

compileSdk/targetSdk = 35 (последняя платформа, установленная в локальном SDK),
minSdk = 28.

## Структура проекта

```
app/src/main/java/com/wakeproof/alarm/
├── WakeProofApplication.kt      # Application, владеет AppContainer
├── MainActivity.kt              # единственная Compose-activity (NavHost)
├── data/
│   ├── local/                   # Room: WakeProofDatabase (v1, пока без сущностей)
│   └── repository/              # репозитории (появятся на этапе 1)
├── domain/
│   ├── model/                   # доменные модели
│   └── usecase/                 # бизнес-логика
├── presentation/
│   ├── navigation/              # Destinations + WakeProofApp (NavHost)
│   ├── theme/                   # всегда тёмная тема, янтарный акцент
│   └── ui/
│       ├── alarms/              # главный экран: список будильников + FAB
│       ├── ringing/             # экран звонка (этап 2+)
│       ├── stats/               # статистика (заглушка)
│       └── sleep/               # сон (заглушка)
├── di/AppContainer.kt           # ручной DI
├── service/                     # foreground-сервисы (этап 2+)
├── receiver/                    # BroadcastReceiver'ы (этап 2+)
└── util/                        # утилиты
```

Навигация — плоский NavHost (Будильники → Статистика / Сон), без bottom
navigation: переходы через иконки в top bar главного экрана.

## Правила проекта

- Имена в коде и комментарии — английские; все пользовательские строки —
  русские и только через `strings.xml`.
- Тема всегда тёмная (фон ~#0E0F14, янтарный акцент), крупные шрифты.
- Новые зависимости — только с явного разрешения этапа. Никаких сетевых,
  рекламных, аналитических SDK.
