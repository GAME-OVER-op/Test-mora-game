# GameSpace

Каркас нового приложения GameSpace в стиле RedMagic Game Space.

## Исправления v7 — обновление Android/Gradle стека

Обновил сборочный стек и AndroidX/Compose зависимости.

### Почему не поставил Gradle 9.4.2

Я проверил документацию/релизы. Надёжно подтверждаются:

- AGP 9.1 требует Gradle минимум 9.3.1.
- AGP 9.2 использует Gradle 9.4.1 как default/minimum, но Kotlin Gradle Plugin 2.4.0 по опубликованной матрице совместимости рассчитан максимум до AGP 9.1.0.
- Gradle 9.4.2 как стабильная версия в документации не подтвердился, поэтому ставить его в GitHub Actions рискованно: workflow может упасть просто потому, что такой дистрибутив недоступен.

Поэтому для этой версии выбрана безопасная новая связка:

```text
Android Gradle Plugin: 9.1.0
Gradle в GitHub Actions: 9.4.1
Kotlin: 2.4.0
Compose Compiler Plugin: 2.4.0
compileSdk: 36
targetSdk: 36
buildTools: 36.0.0
```

### Обновлённые библиотеки

```text
androidx.core:core-ktx:1.18.0
androidx.activity:activity-compose:1.13.0
androidx.lifecycle:lifecycle-runtime-ktx:2.10.0
androidx.compose:compose-bom:2026.04.01
```

Compose теперь подключён через BOM, а Compose compiler — через официальный Kotlin Compose Compiler Gradle Plugin. Это правильный способ для Kotlin 2.x.

### Что осталось по видео

Оригинальное `start_animation.mp4` не перекодируется и не меняется.
Используется правильный runtime-rendering:

- `TextureView + MediaPlayer`;
- `Matrix` center-crop по реальным размерам видео и экрана;
- звук оригинального видео включён;
- GameLobby не рисуется до перехода, чтобы не грузить устройство одновременно с intro.

## Уже есть

- Landscape fullscreen UI.
- Главный экран в стиле RedMagic Game Space.
- Реальное время и процент батареи.
- Иконки батареи по уровню заряда.
- Нижние отдельные карточки Game Lobby / Высокопроизвод.
- Кнопки кулера и музыки у правого края.

## Пока не подключено

- Mora daemon API.
- Реальный `games.json`.
- Реальный список установленных игр.
- Реальные действия кулера/LED/touch/CPU/GPU.

## GitHub Actions

Workflow лежит тут:

```text
.github/workflows/build.yml
```

После загрузки в GitHub он собирает debug/release APK и кладёт их в artifact `GameSpace-apks`.
