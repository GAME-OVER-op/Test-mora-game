package com.mora.gamedock;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Светящееся кольцо (в стиле RedMagic GameSpace CPU/GPU).
 * Цвет задаётся извне (зависит от режима). Прогресс 0..1 — яркая дуга поверх свечения.
 */
public class GlowRingView extends View {

    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint prog = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private float progress = 0f;
    private int accent = 0xFF2EA6FF;

    public GlowRingView(Context c) { super(c); init(); }
    public GlowRingView(Context c, AttributeSet a) { super(c, a); init(); }
    public GlowRingView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        // BlurMaskFilter не работает на hardware canvas — переводим вью в software-слой.
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeCap(Paint.Cap.ROUND);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeCap(Paint.Cap.ROUND);
        prog.setStyle(Paint.Style.STROKE);
        prog.setStrokeCap(Paint.Cap.ROUND);
        inner.setStyle(Paint.Style.STROKE);
        applyAccent();
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public void setAccent(int c) {
        accent = c;
        applyAccent();
        invalidate();
    }

    private void applyAccent() {
        glow.setColor(accent);
        track.setColor(withAlpha(accent, 38));
        prog.setColor(accent);
        inner.setColor(withAlpha(0xFFFFFFFF, 120));
    }

    public void setProgress(float p) {
        progress = Math.max(0f, Math.min(1f, p));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float size = Math.min(w, h);
        float stroke = size * 0.11f;
        float glowStroke = size * 0.14f;
        float pad = glowStroke + size * 0.05f;
        float cx = w / 2f, cy = h / 2f;
        float radius = size / 2f - pad;
        rect.set(cx - radius, cy - radius, cx + radius, cy + radius);

        // внешнее свечение (полное кольцо)
        glow.setStrokeWidth(glowStroke);
        glow.setMaskFilter(new BlurMaskFilter(size * 0.07f, BlurMaskFilter.Blur.NORMAL));
        canvas.drawCircle(cx, cy, radius, glow);

        // тёмная дорожка
        track.setStrokeWidth(stroke);
        canvas.drawCircle(cx, cy, radius, track);

        // яркая дуга прогресса
        prog.setStrokeWidth(stroke);
        canvas.drawArc(rect, -90f, 360f * progress, false, prog);

        // внутренний тонкий блик
        inner.setStrokeWidth(size * 0.012f);
        canvas.drawCircle(cx, cy, radius - stroke * 0.62f, inner);
    }
}
