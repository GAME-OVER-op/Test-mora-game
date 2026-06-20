# RedMagic Touch Settings (standalone)

Обычное Android-приложение, повторяющее экран «Регулировка параметров сенсорного экрана»
из Game Space (`cn.nubia.gamelauncher`). Без плавающего окна — экран открывается сразу при запуске.

## Что пишет
Те же ключи `Settings.Global`, что и оригинальная панель:

| Параметр | Ключ |
|---|---|
| Частота дискретизации | `NubiaperformanceTouchSampleRate` |
| Чувствительность | `NubiaperformanceTouchSen` |
| Плавность | `NubiaperformanceTouchFollow` |
| Стабилизация | `NubiaperformanceTouchMicroSensitive` |
| Гироскоп | `NubiaperformanceGyroSen` |
| Защита от касаний | `TouchProtectOpen` |

Формат значения: `<package>@<value>` когда задан пакет игры.
Запись выполняется через root (`su -c settings put global ...`).

> Примечание: на кастоме запись этих Settings-ключей может ничего не делать —
> реально применяет их системный сервис ZTE/nubia (TP game partition). Позже это
> заменим на прямую запись kernel-нод тачскрина.

## Сборка
Сборка автоматическая на GitHub Actions (`.github/workflows/build.yml`).
После push в `main`/`master` APK появится в Actions → Artifacts → `app-debug`.

Локально:
```
gradle assembleDebug
```
(используется Gradle 8.7 + AGP 8.5.2, JDK 17, compileSdk 34)
