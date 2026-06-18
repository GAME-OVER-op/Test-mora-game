package com.mora.gamedock;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Светящийся объёмный «тор» CPU/GPU в стиле RedMagic.
 */
public class GlowRingView extends View {

    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint prog = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float progress = 0f;
    private int accent = 0xFFFF2840;
    private float density = 2f;

    public GlowRingView(Context c) { super(c); init(); }
    public GlowRingView(Context c, AttributeSet a) { super(c, a); init(); }
    public GlowRingView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        density = getResources().getDisplayMetrics().density;
        glow.setStyle(Paint.Style.STROKE); glow.setStrokeCap(Paint.Cap.ROUND);
        p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND);
        prog.setStyle(Paint.Style.STROKE); prog.setStrokeCap(Paint.Cap.ROUND);
    }

    private static int withAlpha(int color, int a) { return (color & 0x00FFFFFF) | (a << 24); }
    private static int mix(int color, float f) { // f<1 darken, f>1 lighten
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * f));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((color & 0xFF) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public void setAccent(int c) { accent = c; invalidate(); }
    public void setProgress(float v) { progress = Math.max(0f, Math.min(1f, v)); invalidate(); }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float size = Math.min(w, h);
        float cx = w / 2f, cy = h / 2f;
        float r = size / 2f - 12f * density;
        rect.set(cx - r, cy - r, cx + r, cy + r);

        // свечение
        glow.setStrokeWidth(10f * density);
        glow.setColor(withAlpha(accent, 170));
        glow.setMaskFilter(new BlurMaskFilter(9f * density, BlurMaskFilter.Blur.NORMAL));
        canvas.drawCircle(cx, cy, r, glow);

        // тень тора
        p.setMaskFilter(null);
        p.setStrokeWidth(9f * density); p.setColor(mix(accent, 0.45f));
        canvas.drawCircle(cx, cy, r, p);
        // яркое тело
        p.setStrokeWidth(6f * density); p.setColor(accent);
        canvas.drawCircle(cx, cy, r, p);
        // блик
        p.setStrokeWidth(2f * density); p.setColor(mix(accent, 1.6f));
        canvas.drawCircle(cx, cy, r - 5f * density, p);
        // внутренний обод
        p.setStrokeWidth(2f * density); p.setColor(mix(accent, 0.5f));
        canvas.drawCircle(cx, cy, r - 14f * density, p);
        // дуга загрузки
        prog.setStrokeWidth(4f * density); prog.setColor(mix(accent, 1.7f));
        canvas.drawArc(rect, -90f, 360f * progress, false, prog);
    }
}
