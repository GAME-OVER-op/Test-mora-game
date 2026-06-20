# GameSpace

## Исправления v9

Исправлена вероятная причина `CompilationErrorException` после перехода на AGP 9 / built-in Kotlin.

В `StartAnimation()` я упростил код `TextureView`:

- убрал нестабильный label receiver `textureView@`;
- убрал default args, которые ссылались на receiver внутри локальной функции;
- сделал явную переменную `val textureView = TextureView(viewContext)`;
- исправил вызов transform на явный:

```kotlin
textureView.setTransform(matrix)
```

Также в v8 уже было исправлено:

- удалён `org.jetbrains.kotlin.android`;
- оставлен `org.jetbrains.kotlin.plugin.compose`;
- `kotlinOptions` заменён на `kotlin { compilerOptions { ... } }`.

Текущий стек:

```text
Android Gradle Plugin: 9.1.0
Gradle: 9.4.1
Kotlin / Compose Compiler Plugin: 2.4.0
compileSdk: 36
targetSdk: 36
buildTools: 36.0.0
Compose BOM: 2026.04.01
```

Видео `start_animation.mp4` оригинальное, без перекодирования. Отображение — `TextureView + MediaPlayer + Matrix center-crop`.
