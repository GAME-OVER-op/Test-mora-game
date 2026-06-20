# GameSpace

Новый каркас приложения GameSpace в стиле RedMagic Game Space.

## Что уже есть

- Landscape-only fullscreen лаунчер.
- Название приложения: **GameSpace**.
- Splash/start экран через `start_animation.mp4`.
- Главный экран с обоями `gamespace_wallpaper.png`.
- Левая панель игр.
- Центральный Mora/RedMagic-style реактор.
- Правая панель выбранной игры с кнопкой запуска.
- Нижняя панель быстрых функций.
- Ресурсы из `drawable-nodpi` уже добавлены в проект.
- Manifest заранее содержит разрешения для будущего подключения Mora daemon/API, root/system-настроек и списка приложений.
- GitHub Actions workflow собирает debug и release APK.

## Что пока НЕ подключено

- Mora daemon API.
- `games.json` из основного проекта.
- Реальные настройки CPU/GPU/fan/LED/touch.
- Реальный список установленных игр.

Это специально: сначала каркас, потом подключение к основному проекту.

## Сборка на GitHub

Загрузи проект в репозиторий и запусти Actions. APK появятся в artifact `GameSpace-apks`.

Локально, если установлен Gradle 8.7 и Android SDK:

```bash
gradle :app:assembleDebug
```
