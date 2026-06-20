# GameSpace

Каркас нового приложения GameSpace в стиле RedMagic Game Space.

## В этой версии

- Исправлена стартовая анимация: `start_animation.mp4` выводится fullscreen crop-fill с приближением, чтобы убрать боковые вырезы/полосы.
- Добавлен фон `gamespace_wallpaper.png`.
- Центральный реактор получил более плавное вращение: медленнее, linear easing, больше сегментов.
- В центр добавлен логотип `rm_logo.png`.
- Нижняя панель:
  - `gamelobby_icon.png` для Game Lobby;
  - `top_indicator_expand.png` для высокопроизводительной базы;
  - две круглые кнопки: кулер и музыка.
- Добавлена фоновая музыка `bgm.ogg` с переключателем.
- Кнопки кулера/музыки используют состояния:
  - `cooling_fan_on.png` / `cooling_fan_off.png`;
  - `bgm_on.png` / `bgm_off.png`.
- Кнопка запуска игры использует отправленные стрелки:
  - `exo_ic_chevron_left.png`;
  - `exo_ic_chevron_right.png`.
- Правая кнопка рядом с запуском заменена на `exo_ic_speed.png`.
- Верхняя правая точка убрана.
- Две кнопки справа перенесены в верхнюю панель и заменены на:
  - `add_shortcut_icon.png`;
  - `chat_assistant_settings.png`.

## Что пока не подключено

- Mora daemon API.
- Реальный `games.json`.
- Реальный список установленных игр.
- Реальные действия кулера/LED/touch/CPU/GPU.

Это всё ещё UI-каркас. Подключение к основному проекту делаем следующим этапом.

## GitHub Actions

Workflow лежит тут:

```text
.github/workflows/build.yml
```

После загрузки в GitHub он собирает debug/release APK и кладёт их в artifact `GameSpace-apks`.
