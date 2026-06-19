# GameSpace Control Center Preview

Вторая попытка. Это не `GameStrengthen` с левым меню, а preview для **Game Space Control Center / QS cards** — той панели, которая относится к `cc_tiles_game`, `cc_tools_game`, `control_center` и `ic_qs_*` ресурсам.

Использованы реальные ресурсы из APK:

- `control_center_ing.png`
- `control_center_dising.png`
- `game_control_panel_for_card.png`
- `red_magic_time_for_card.png`
- `more_select_for_card.png`
- `userdefine_*`
- `ic_qs_*`
- `game_space_window_bg.png`

Это визуальный preview, не системный overlay.

## Сборка GitHub

1. Загрузи содержимое этой папки в новый репозиторий.
2. Actions → `Build Android Debug APK` → Run workflow.
3. Скачай artifact `GameSpaceControlCenterPreview-debug-apk`.
4. Установи `app-debug.apk`.

Тап по правой/левой половине экрана меняет сторону панели.
