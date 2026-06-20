package com.redmagic.touchsettings;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * A circular frequency gauge that mimics the CPU/GPU rings on the GameSpace
 * performance screen: a dim track ring, a bright gradient progress arc, a large
 * current-frequency value with its unit, and a label underneath (CPU / GPU).
 */
public class RingView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint unitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    private float progress = 0f;       // 0..1
    private String value = "--";
    private String unit = "";
    private String label = "";
    private int accent = Color.parseColor("#FFE60012");

    public RingView(Context c) { this(c, null); }

    public RingView(Context c, @Nullable AttributeSet a) {
        super(c, a);
        float d = getResources().getDisplayMetrics().density;

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(7 * d);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(Color.parseColor("#FF343747"));

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(7 * d);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        arcPaint.setColor(accent);

        valuePaint.setColor(Color.WHITE);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);
        valuePaint.setTextSize(26 * d);

        unitPaint.setColor(Color.parseColor("#B3FFFFFF"));
        unitPaint.setTextAlign(Paint.Align.CENTER);
        unitPaint.setTextSize(11 * d);

        labelPaint.setColor(Color.parseColor("#B3FFFFFF"));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(13 * d);
    }

    public void setAccent(int color) {
        this.accent = color;
        invalidate();
    }

    /** cur/max set the arc fill; value/unit/label set the text. */
    public void setData(float cur, float max, String value, String unit, String label) {
        if (max <= 0) max = 1;
        float p = cur / max;
        if (p < 0) p = 0;
        if (p > 1) p = 1;
        this.progress = p;
        this.value = value;
        this.unit = unit;
        this.label = label;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        float pad = 10 * d;
        float w = getWidth();
        float h = getHeight();
        float size = Math.min(w, h) - pad * 2;
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = size / 2f;

        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

        // gradient on the progress arc
        arcPaint.setShader(new LinearGradient(
                arcRect.left, arcRect.top, arcRect.right, arcRect.bottom,
                accent, Color.parseColor("#FFFF5A5F"), Shader.TileMode.CLAMP));

        // full track
        canvas.drawArc(arcRect, 135, 270, false, trackPaint);
        // progress (270deg sweep span)
        canvas.drawArc(arcRect, 135, 270 * progress, false, arcPaint);

        // center value + unit
        float baseY = cy + (valuePaint.getTextSize() / 3f);
        canvas.drawText(value, cx, baseY - 6 * d, valuePaint);
        canvas.drawText(unit, cx, baseY + 14 * d, unitPaint);

        // label below the ring
        canvas.drawText(label, cx, cy + radius - 2 * d, labelPaint);
    }
}
