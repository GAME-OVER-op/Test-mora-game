package com.mora.gamedock;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * Угловатая «гексагональная» рамка GameSpace с тонким светящимся кантом.
 * Скошенные углы + трапециевидный вырез сверху по центру. Цвет — по режиму.
 */
public class FrameBorderView extends View {

    private int accent = 0xFFFF2D46;
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

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public void setAccent(int c) {
        accent = c;
        applyAccent();
        invalidate();
    }

    private void applyAccent() {
        glow.setColor(withAlpha(accent, 150));
        line.setColor(accent);
    }

    private void buildPath(float w, float h) {
        float in = 10f * density;   // отступ от края
        float c = 26f * density;    // скос углов
        float nd = 22f * density;   // глубина выреза сверху
        float nL = w * 0.43f, nLb = w * 0.47f, nRb = w * 0.53f, nR = w * 0.57f;

        path.reset();
        path.moveTo(in + c, in);
        path.lineTo(nL, in);              // верх до выреза
        path.lineTo(nLb, in + nd);         // вниз во вырез
        path.lineTo(nRb, in + nd);         // дно выреза
        path.lineTo(nR, in);               // вверх из выреза
        path.lineTo(w - in - c, in);       // верх до правого скоса
        path.lineTo(w - in, in + c);       // правый верхний скос
        path.lineTo(w - in, h - in - c);   // правый бок
        path.lineTo(w - in - c, h - in);   // правый нижний скос
        path.lineTo(in + c, h - in);       // низ
        path.lineTo(in, h - in - c);       // левый нижний скос
        path.lineTo(in, in + c);           // левый бок
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
