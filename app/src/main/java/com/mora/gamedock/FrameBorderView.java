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
 * Ступенчатые шевроны-разделители [ данные ›  пусто  ‹ данные ]
 * заполняются снизу вверх пропорционально частоте (левый = CPU, правый = GPU).
 */
public class FrameBorderView extends View {

    private int accent = 0xFFFF2840;
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private float density = 2f;
    private float cpuFill = 0f, gpuFill = 0f; // 0..1

    public FrameBorderView(Context c) { super(c); init(); }
    public FrameBorderView(Context c, AttributeSet a) { super(c, a); init(); }
    public FrameBorderView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        density = getResources().getDisplayMetrics().density;
        for (Paint p : new Paint[]{glow, line, dim}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeCap(Paint.Cap.ROUND);
        }
        applyAccent();
    }

    private static int withAlpha(int color, int alpha) { return (color & 0x00FFFFFF) | (alpha << 24); }

    // затемнённый вариант цвета (смешивание с чёрным)
    private static int darken(int color, float k) {
        int r = (int) (((color >> 16) & 0xFF) * k);
        int g = (int) (((color >> 8) & 0xFF) * k);
        int b = (int) ((color & 0xFF) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public void setAccent(int c) { accent = c; applyAccent(); invalidate(); }

    /** Доли частоты 0..1 для заполнения лесенок. */
    public void setFills(float cpu, float gpu) {
        cpuFill = Math.max(0f, Math.min(1f, cpu));
        gpuFill = Math.max(0f, Math.min(1f, gpu));
        invalidate();
    }

    private void applyAccent() {
        glow.setColor(withAlpha(accent, 150));
        line.setColor(accent);
        dim.setColor(darken(accent, 0.30f));
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

    // лесенка от (x0,y0) к (x1,y1): n ступеней (горизонталь+вертикаль)
    private static float[] stair(float x0, float y0, float x1, float y1, int n) {
        float dx = (x1 - x0) / n, dy = (y1 - y0) / n;
        float[] p = new float[(2 * n + 1) * 2];
        int k = 0; float cx = x0, cy = y0;
        p[k++] = cx; p[k++] = cy;
        for (int i = 0; i < n; i++) {
            cx += dx; p[k++] = cx; p[k++] = cy;
            cy += dy; p[k++] = cx; p[k++] = cy;
        }
        return p;
    }

    // шеврон: верхняя рука к вершине + нижняя; y монотонно растёт (сверху вниз)
    private static float[] chevron(float xb, float top, float mid, float bot, float dep) {
        float[] a = stair(xb, top, xb + dep, mid, 7);
        float[] b = stair(xb + dep, mid, xb, bot, 7);
        float[] out = new float[a.length + b.length - 2];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 2, out, a.length, b.length - 2);
        return out;
    }

    private static float[] shiftX(float[] p, float d) {
        float[] o = new float[p.length];
        for (int i = 0; i < p.length; i += 2) { o[i] = p[i] + d; o[i + 1] = p[i + 1]; }
        return o;
    }

    // Path из точек, только те у которых y >= yThr (яркое заполнение снизу)
    private static Path toPath(float[] p, float yThr) {
        Path path = new Path();
        boolean started = false;
        for (int i = 0; i < p.length; i += 2) {
            if (p[i + 1] < yThr) continue;
            if (!started) { path.moveTo(p[i], p[i + 1]); started = true; }
            else path.lineTo(p[i], p[i + 1]);
        }
        return path;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        buildPath(w, h);

        // рамка
        glow.setStrokeWidth(6f * density);
        glow.setMaskFilter(new BlurMaskFilter(6f * density, BlurMaskFilter.Blur.NORMAL));
        canvas.drawPath(path, glow);
        line.setStrokeWidth(2f * density);
        canvas.drawPath(path, line);

        // ступенчатые шевроны-индикаторы
        float top = h * 0.15f, bot = h * 0.85f, mid = h * 0.42f, dep = 24f * density;
        float off = 13f * density;
        float[] cpuA = chevron(w * 0.33f, top, mid, bot, dep);
        float[] cpuB = shiftX(cpuA, off);
        float[] gpuA = chevron(w * 0.67f, top, mid, bot, -dep);
        float[] gpuB = shiftX(gpuA, -off);

        float cpuThr = bot - cpuFill * (bot - top);
        float gpuThr = bot - gpuFill * (bot - top);

        dim.setStrokeWidth(2f * density);
        line.setStrokeWidth(2f * density);
        glow.setStrokeWidth(5f * density);

        // тусклый фон всех четырёх линий
        for (float[] pl : new float[][]{cpuA, cpuB, gpuA, gpuB}) canvas.drawPath(toPath(pl, top), dim);
        // яркое заполнение + свечение
        for (float[] pl : new float[][]{cpuA, cpuB}) { canvas.drawPath(toPath(pl, cpuThr), glow); canvas.drawPath(toPath(pl, cpuThr), line); }
        for (float[] pl : new float[][]{gpuA, gpuB}) { canvas.drawPath(toPath(pl, gpuThr), glow); canvas.drawPath(toPath(pl, gpuThr), line); }
    }
}
