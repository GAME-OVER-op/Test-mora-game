# GameSpace Panel Preview

Минимальный Android-проект для GitHub Actions. Он показывает тестовое окно, похожее на внутриигровую свайп-панель Game Space / Game Control Center.

Используемые исходные ресурсы из разобранного GameSpace:

- `panel_item_red_right.png`
- `panel_item_red_right_end.png`
- `gamecontrol_title_background.png`
- `gamecontrol_title_close.png`
- `nubia_game_strengthen_mask.png`

Это не оригинальный сервис и не вызывает системные функции. Это только визуальный preview, чтобы проверить, та ли это карточка/панель.

## Как собрать через GitHub

1. Создай новый репозиторий на GitHub.
2. Загрузи все файлы из этого архива в корень репозитория.
3. Открой вкладку **Actions**.
4. Запусти workflow **Build Android Debug APK** вручную через **Run workflow**.
5. После сборки скачай artifact `GameSpacePanelPreview-debug-apk`.
6. Установи `app-debug.apk` на телефон.

## Локальная сборка

Если установлен Android SDK и Gradle:

```bash
gradle assembleDebug
```

APK будет тут:

```text
app/build/outputs/apk/debug/app-debug.apk
```
