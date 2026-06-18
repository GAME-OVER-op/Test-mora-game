package com.mora.gamedock;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * Скошенное «крыло» панели с светящимся LED-зигзагом по внутренней диагонали
 * (фирменный элемент RedMagic). Цвет LED зависит от режима. mirror=true — зеркало для правой стороны.
 */
public class WingView extends View {

    private boolean mirror = false;
    private int accent = 0xFF2EA6FF;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint led = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ledGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path wing = new Path();
    private final Path edge = new Path();

    public WingView(Context c) { super(c); init(); }
    public WingView(Context c, AttributeSet a) { super(c, a); init(); }
    public WingView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        fill.setColor(0x59070707); // полупрозрачный тёмный
        led.setStyle(Paint.Style.STROKE);
        led.setStrokeJoin(Paint.Join.ROUND);
        led.setStrokeCap(Paint.Cap.ROUND);
        ledGlow.setStyle(Paint.Style.STROKE);
        ledGlow.setStrokeJoin(Paint.Join.ROUND);
        ledGlow.setStrokeCap(Paint.Cap.ROUND);
        applyAccent();
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public void setMirror(boolean m) { mirror = m; invalidate(); }

    public void setAccent(int c) {
        accent = c;
        applyAccent();
        invalidate();
    }

    private void applyAccent() {
        led.setColor(accent);
        ledGlow.setColor(withAlpha(accent, 150));
    }

    private void buildZigzag(float x0, float y0, float x1, float y1) {
        edge.reset();
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.hypot(dx, dy);
        if (len == 0) return;
        float px = -dy / len, py = dx / len; // перпендикуляр
        int steps = 24;
        float amp = getWidth() * 0.020f;
        edge.moveTo(x0, y0);
        for (int i = 1; i < steps; i++) {
            float t = (float) i / steps;
            float bx = x0 + dx * t, by = y0 + dy * t;
            float dir = (i % 2 == 0) ? 1f : -1f;
            edge.lineTo(bx + px * amp * dir, by + py * amp * dir);
        }
        edge.lineTo(x1, y1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        canvas.save();
        if (mirror) canvas.scale(-1f, 1f, w / 2f, h / 2f);

        float topX = w * 0.96f, botX = w * 0.46f;
        wing.reset();
        wing.moveTo(0, 0);
        wing.lineTo(topX, 0);
        wing.lineTo(botX, h);
        wing.lineTo(0, h);
        wing.close();
        canvas.drawPath(wing, fill);

        buildZigzag(topX, 0, botX, h);
        ledGlow.setStrokeWidth(w * 0.030f);
        ledGlow.setMaskFilter(new BlurMaskFilter(w * 0.022f, BlurMaskFilter.Blur.NORMAL));
        canvas.drawPath(edge, ledGlow);
        led.setStrokeWidth(w * 0.012f);
        canvas.drawPath(edge, led);

        canvas.restore();
    }
}
