package com.mora.gamedock;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * Угловая рамка GameSpace: скошенные углы, плечи сверху и трапециевидный вырез по центру.
 */
public class FrameBorderView extends View {

    private int accent = 0xFFFF2840;
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private float density = 2f;

    public FrameBorderView(Context c) { super(c); init(); }
    public FrameBorderView(Context c, AttributeSet a) { super(c, a); init(); }
    public FrameBorderView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        density = getResources().getDisplayMetrics().density;
        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeJoin(Paint.Join.ROUND);
        glow.setStrokeCap(Paint.Cap.ROUND);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setStrokeCap(Paint.Cap.ROUND);
        applyAccent();
    }

    private static int withAlpha(int color, int alpha) { return (color & 0x00FFFFFF) | (alpha << 24); }

    public void setAccent(int c) { accent = c; applyAccent(); invalidate(); }

    private void applyAccent() {
        glow.setColor(withAlpha(accent, 150));
        line.setColor(accent);
    }

    private void buildPath(float w, float h) {
        float in = 9f * density, c = 16f * density, nd = 13f * density, sh = 9f * density;
        float nL = w * 0.42f, nLb = w * 0.46f, nRb = w * 0.54f, nR = w * 0.58f;
        path.reset();
        path.moveTo(in + c, in + sh);
        path.lineTo(w * 0.20f, in);
        path.lineTo(nL, in);
        path.lineTo(nLb, in + nd);
        path.lineTo(nRb, in + nd);
        path.lineTo(nR, in);
        path.lineTo(w * 0.80f, in);
        path.lineTo(w - in - c, in + sh);
        path.lineTo(w - in, in + c + sh);
        path.lineTo(w - in, h - in - c);
        path.lineTo(w - in - c, h - in);
        path.lineTo(in + c, h - in);
        path.lineTo(in, h - in - c);
        path.lineTo(in, in + c + sh);
        path.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        buildPath(getWidth(), getHeight());
        glow.setStrokeWidth(6f * density);
        glow.setMaskFilter(new BlurMaskFilter(6f * density, BlurMaskFilter.Blur.NORMAL));
        canvas.drawPath(path, glow);
        line.setStrokeWidth(2f * density);
        canvas.drawPath(path, line);
    }
}
