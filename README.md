# Mora GameDock

Прототип игровой панели (в стиле nubia GameAssist) для RedMagic 9 Pro / проекта **mora**.
Показывает **живые** показатели CPU/GPU, батарею/температуру, переключатель режимов и сетку карточек-тумблеров.

## Что внутри
- `CircleGaugeView` — круговой индикатор частот/загрузки (аналог `CpuGpuView`).
- `RootShell` + `Sysfs` — чтение sysfs через `su` (или напрямую, если root недоступен).
- `MainActivity` — опрос раз в секунду и обновление UI.
- `TileAdapter` — карточки-плитки (аналог `TileView`/`DockTile`).

## Источники данных (sysfs)
- CPU частота: `/sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq` (prime), `cpu0` (fallback)
- CPU загрузка: `/proc/stat`
- GPU загрузка: `/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage`
- GPU частота: `/sys/class/kgsl/kgsl-3d0/gpuclk` (или `devfreq/cur_freq`)
- Батарея: `/sys/class/power_supply/battery/capacity`, темп.: `.../battery/temp`

## Сборка в CI (GitLab)
1. Создай пустой репозиторий на GitLab.
2. Залей туда всё содержимое этой папки (включая `.gitlab-ci.yml`).
3. GitLab CI сам скачает Android SDK и соберёт APK.
4. Готовый APK будет в **CI/CD → Jobs → build_apk → Browse/Download artifacts**: `app/build/outputs/apk/debug/app-debug.apk`.

### Альтернатива: GitHub Actions
Файл `.github/workflows/build.yml` уже готов — при push APK появится в **Actions → ран → Artifacts → app-debug**.

## Установка
```sh
adb install -r app-debug.apk
```
Затем в Magisk/SU разреши root для `com.mora.gamedock` — иначе часть показателей будет пустой.

## Сборка локально
```sh
gradle assembleDebug
# или в Android Studio: Run
```

## Дальше (интеграция в mora)
- Перевести панель в overlay-окно (`TYPE_APPLICATION_OVERLAY`) для вызова поверх игры.
- Подключить тумблеры к реальным действиям и к perf_daemon.
