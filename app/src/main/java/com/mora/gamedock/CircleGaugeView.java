package com.mora.gamedock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Круговой индикатор (аналог CpuGpuView / HostPerformanceCircleView из GameAssist).
 * Рисует фоновую дугу 270°, поверх неё дугу прогресса,
 * в центре процент, сверху подпись (CPU/GPU), снизу частота.
 */
public class CircleGaugeView extends View {

    private final Paint bgArc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fgArc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    private float progress = 0f;
    private String label = "CPU";
    private String sub = "";
    private int accent = 0xFFE93363;

    private static final float START_ANGLE = 135f;
    private static final float SWEEP_MAX = 270f;

    public CircleGaugeView(Context c) { super(c); init(); }
    public CircleGaugeView(Context c, AttributeSet a) { super(c, a); init(); }
    public CircleGaugeView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        bgArc.setStyle(Paint.Style.STROKE);
        bgArc.setStrokeCap(Paint.Cap.ROUND);
        bgArc.setColor(0x33FFFFFF);
        fgArc.setStyle(Paint.Style.STROKE);
        fgArc.setStrokeCap(Paint.Cap.ROUND);
        fgArc.setColor(accent);
        centerText.setColor(0xFFFFFFFF);
        centerText.setTextAlign(Paint.Align.CENTER);
        centerText.setFakeBoldText(true);
        labelText.setColor(0xFFB9B8B8);
        labelText.setTextAlign(Paint.Align.CENTER);
        subText.setColor(0xFFB9B8B8);
        subText.setTextAlign(Paint.Align.CENTER);
    }

    public void setAccent(int c) { accent = c; fgArc.setColor(c); invalidate(); }
    public void setLabel(String l) { label = l; invalidate(); }

    /** progress 0..1, sub — подпись с частотой. */
    public void setProgress(float p, String subValue) {
        progress = Math.max(0f, Math.min(1f, p));
        sub = subValue;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float size = Math.min(w, h);
        float stroke = size * 0.09f;
        bgArc.setStrokeWidth(stroke);
        fgArc.setStrokeWidth(stroke);
        float pad = stroke / 2f + size * 0.04f;
        arcRect.set(pad, pad, w - pad, h - pad);

        canvas.drawArc(arcRect, START_ANGLE, SWEEP_MAX, false, bgArc);
        canvas.drawArc(arcRect, START_ANGLE, SWEEP_MAX * progress, false, fgArc);

        centerText.setTextSize(size * 0.26f);
        labelText.setTextSize(size * 0.13f);
        subText.setTextSize(size * 0.11f);

        float cx = w / 2f;
        canvas.drawText(Math.round(progress * 100) + "%", cx,
                h / 2f + centerText.getTextSize() * 0.34f, centerText);
        canvas.drawText(label, cx, h * 0.30f, labelText);
        if (sub != null && !sub.isEmpty()) {
            canvas.drawText(sub, cx, h * 0.82f, subText);
        }
    }
}
