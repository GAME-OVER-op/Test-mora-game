# GameSpace

## v16 — музыка через движок VLC (libVLC)

### Проблема
Фоновая музыка играла только в VLC, а в других приложениях/через системный MediaPlayer
либо молчала, либо лагала. Причина — медиа-стек прошивки (RedMagic/Nubia)
плохо работает с кодеком дорожки.

### Решение
Встроен **libVLC** (`org.videolan.android:libvlc-all:3.6.0`) — это «VLC внутри приложения»
со своими кодеками (FFmpeg). Музыка теперь декодируется движком VLC, мимо системных кодеков.

- `BackgroundMusic` копирует `R.raw.bgm_loop` в cache и играет через `LibVLC` + `MediaPlayer`.
- Цикл: опция медиа `:input-repeat=65535`.
- Старт только на `LaunchStage.Ready`; полный release на dispose.
- ABI: `arm64-v8a` (RedMagic). Для 32-бит добавь `armeabi-v7a` в `abiFilters`.

⚙️ При сборке GitHub Actions скачает нативные библиотеки libVLC с Maven Central
(нужен интернет при сборке). APK вырастёт на ~30–40 МБ из-за кодеков VLC.

### Крутящийся элемент
Вентилятор только равномерно вращается (GPU `View.rotation`), без эффекта «дыхания».

### Не трогали
`start_animation.mp4` — оригинал.
