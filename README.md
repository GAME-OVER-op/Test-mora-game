# GameSpace

## Исправления v10 — Kotlin compile fix

Исправлена ошибка сборки:

```text
MainActivity.kt:238:33 'val' cannot be reassigned
MainActivity.kt:239:33 'val' cannot be reassigned
MainActivity.kt:243:33 'val' cannot be reassigned
MainActivity.kt:244:33 'val' cannot be reassigned
```

Причина: внутри блока `MediaPlayer().apply { ... }` имена `videoWidth` и `videoHeight` резолвились не в наши переменные listener-а, а в read-only свойства `MediaPlayer.videoWidth` / `MediaPlayer.videoHeight`. Kotlin 2.4 справедливо ругался, что `val` нельзя переassign-ить.

Исправление:

```kotlin
private var sourceVideoWidth = 0
private var sourceVideoHeight = 0
```

и дальше используются именно эти имена:

```kotlin
sourceVideoWidth = w
sourceVideoHeight = h
sourceVideoWidth = mediaPlayer.videoWidth
sourceVideoHeight = mediaPlayer.videoHeight
```

Остальное:

- AGP 9 built-in Kotlin без `org.jetbrains.kotlin.android`;
- Compose Compiler Plugin оставлен;
- оригинальное `start_animation.mp4` не перекодируется;
- отображение видео: `TextureView + MediaPlayer + Matrix center-crop`.
