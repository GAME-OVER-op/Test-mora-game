# GameSpace

## Исправления v14 — причина лага 2го экрана

### Причина

Центральный круг (`ReactorView`) раньше анимировался через `Handler.postDelayed(33ms)` с
полным `invalidate()` каждый кадр. То есть 30 раз в секунду на главном потоке
заново рисовались 3 большие сглаженные окружности + 48 линий (anti-alias) на площади 420dp.

Это:
- постоянно грузило CPU/GPU → устройство грелось → тепловой троттлинг → лаги усиливались со временем;
- конкурировало с рендером Compose на главном потоке.

### Решение

Кольцо теперь рисуется **один раз** в hardware-слой, а движение идёт через GPU-трансформы:

```kotlin
setLayerType(LAYER_TYPE_HARDWARE, null)
ObjectAnimator.ofFloat(this, View.ROTATION, 0f, 360f) // вращение на GPU
ValueAnimator pulse → scaleX/scaleY                    // лёгкий пульс
```

Результат:
- `onDraw` больше **не вызывается каждый кадр**;
- нет `Handler`/`postDelayed`/покадрового `invalidate()`;
- вращение и пульс — чистые GPU-трансформы, почти ноль CPU;
- аниматоры останавливаются при `onDetachedFromWindow` и когда `animationsEnabled = false`.

### Остальное (из v13)

- музыка: `R.raw.bgm_loop` (AAC/M4A), старт только на `LaunchStage.Ready`;
- батарея — event-driven `BroadcastReceiver`, время — раз в минуту;
- видео `start_animation.mp4` не трогали (TextureView + Matrix center-crop).
