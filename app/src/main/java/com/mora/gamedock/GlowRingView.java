package com.mora.gamedock;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Наклонённое кольцо-«воронка» CPU/GPU в перспективе (как в GameSpace):
 * несколько концентрических светящихся эллипсов, цифра парит над ними.
 */
public class GlowRingView extends View {

    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private float progress = 0f;
    private int accent = 0xFFFF2840;
    private float density = 2f;

    private static final float RATIO = 0.46f; // вертикальное сжатие (наклон)
    private static final float[] FR = {1f, 0.82f, 0.64f, 0.47f, 0.32f, 0.18f};

    public GlowRingView(Context c) { super(c); init(); }
    public GlowRingView(Context c, AttributeSet a) { super(c, a); init(); }
    public GlowRingView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        density = getResources().getDisplayMetrics().density;
        glow.setStyle(Paint.Style.STROKE); glow.setStrokeCap(Paint.Cap.ROUND);
        p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND);
    }

    private static int withAlpha(int color, int a) { return (color & 0x00FFFFFF) | (a << 24); }
    private static int mix(int color, float f) {
        int rr = Math.min(255, (int) (((color >> 16) & 0xFF) * f));
        int gg = Math.min(255, (int) (((color >> 8) & 0xFF) * f));
        int bb = Math.min(255, (int) ((color & 0xFF) * f));
        return 0xFF000000 | (rr << 16) | (gg << 8) | bb;
    }

    public void setAccent(int c) { accent = c; invalidate(); }
    public void setProgress(float v) { progress = Math.max(0f, Math.min(1f, v)); invalidate(); }

    private void oval(Canvas c, Paint pt, float cx, float cy, float rx, float ry, float wd) {
        pt.setStrokeWidth(wd);
        r.set(cx - rx, cy - ry, cx + rx, cy + ry);
        c.drawOval(r, pt);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float rx = Math.min(w / 2f - 4f * density, (h / 2f - 4f * density) / RATIO);
        float lift = rx * 0.05f;

        // свечение двух внешних эллипсов
        glow.setMaskFilter(new BlurMaskFilter(10f * density, BlurMaskFilter.Blur.NORMAL));
        glow.setColor(withAlpha(accent, 170));
        oval(canvas, glow, cx, cy, rx, rx * RATIO, 9f * density);
        oval(canvas, glow, cx, cy - lift, rx * 0.82f, rx * 0.82f * RATIO, 7f * density);

        // концентрические эллипсы (воронка)
        p.setMaskFilter(null);
        for (int i = 0; i < FR.length; i++) {
            float rxi = rx * FR[i];
            float ryi = rxi * RATIO;
            float oy = cy - i * lift;
            int col; float wd;
            if (i == 0) { col = accent; wd = 4f * density; }
            else if (i == 1) { col = accent; wd = 6f * density; }
            else { col = mix(accent, 0.6f - i * 0.07f); wd = 2f * density; }
            p.setColor(col);
            oval(canvas, p, cx, oy, rxi, ryi, wd);
        }

        // блик-полумесяц на главном кольце
        p.setStrokeWidth(2f * density);
        p.setColor(mix(accent, 1.6f));
        float mrx = rx * 0.82f, mry = mrx * RATIO, moy = cy - lift;
        r.set(cx - mrx, moy - mry, cx + mrx, moy + mry);
        canvas.drawArc(r, 185f, 170f, false, p);

        // дуга загрузки на внешнем кольце
        p.setStrokeWidth(4f * density);
        p.setColor(mix(accent, 1.7f));
        r.set(cx - rx, cy - rx * RATIO, cx + rx, cy + rx * RATIO);
        canvas.drawArc(r, -90f, 360f * progress, false, p);
    }
}
