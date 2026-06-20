# GameSpace

Каркас нового приложения GameSpace в стиле RedMagic Game Space.

## Исправления v8 — fix сборки AGP 9

Ошибка GitHub Actions была из-за AGP 9 built-in Kotlin:

```text
The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0.
Solution: Remove the 'org.jetbrains.kotlin.android' plugin
```

Исправлено:

- удалён `org.jetbrains.kotlin.android` из root `build.gradle.kts`;
- удалён `org.jetbrains.kotlin.android` из `app/build.gradle.kts`;
- оставлен `org.jetbrains.kotlin.plugin.compose`, потому что Compose Compiler Plugin всё ещё нужен для Compose на Kotlin 2.x;
- старый `android { kotlinOptions { ... } }` заменён на новый AGP 9 / built-in Kotlin DSL:

```kotlin
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
```

Текущий стек:

```text
Android Gradle Plugin: 9.1.0
Gradle в GitHub Actions: 9.4.1
Kotlin/Compose Compiler Plugin: 2.4.0
compileSdk: 36
targetSdk: 36
buildTools: 36.0.0
Compose BOM: 2026.04.01
```

## Видео

Оригинальное `start_animation.mp4` не перекодируется и не меняется.
Используется `TextureView + MediaPlayer + Matrix center-crop`.

## GitHub Actions

Workflow лежит тут:

```text
.github/workflows/build.yml
```

После загрузки в GitHub он собирает debug/release APK и кладёт их в artifact `GameSpace-apks`.
