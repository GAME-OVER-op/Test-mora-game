package com.mora.gamedock;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * Рамка GameSpace + боковые частотные рейки.
 *
 * Важно: на референсе это не одна угловая линия, а сегментированная панель:
 * [ данные  segmented ›    пустое поле    ‹ segmented  данные ]
 * Нижние сегменты заполняются ярким цветом пропорционально частоте CPU/GPU.
 */
public class FrameBorderView extends View {

    private int accent = 0xFFFF2840;
    private float density = 2f;
    private float cpuFill = 0f, gpuFill = 0f;

    private final Paint frameGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint frameLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint segGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint segOn = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint segOff = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint segStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path framePath = new Path();
    private final Path segPath = new Path();

    public FrameBorderView(Context c) { super(c); init(); }
    public FrameBorderView(Context c, AttributeSet a) { super(c, a); init(); }
    public FrameBorderView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        density = getResources().getDisplayMetrics().density;

        frameGlow.setStyle(Paint.Style.STROKE);
        frameGlow.setStrokeJoin(Paint.Join.ROUND);
        frameGlow.setStrokeCap(Paint.Cap.ROUND);

        frameLine.setStyle(Paint.Style.STROKE);
        frameLine.setStrokeJoin(Paint.Join.ROUND);
        frameLine.setStrokeCap(Paint.Cap.ROUND);

        segGlow.setStyle(Paint.Style.FILL);
        segOn.setStyle(Paint.Style.FILL);
        segOff.setStyle(Paint.Style.FILL);
        segStroke.setStyle(Paint.Style.STROKE);
        segStroke.setStrokeWidth(0.8f * density);

        applyAccent();
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static int mixBlack(int color, float k, int alpha) {
        int r = (int) (((color >> 16) & 0xFF) * k);
        int g = (int) (((color >> 8) & 0xFF) * k);
        int b = (int) ((color & 0xFF) * k);
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    public void setAccent(int c) {
        accent = c;
        applyAccent();
        invalidate();
    }

    /** 0..1: текущая частота / максимальная частота. */
    public void setFills(float cpu, float gpu) {
        cpuFill = clamp(cpu);
        gpuFill = clamp(gpu);
        invalidate();
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private void applyAccent() {
        frameGlow.setColor(withAlpha(accent, 145));
        frameLine.setColor(accent);

        segGlow.setColor(withAlpha(accent, 120));
        segGlow.setMaskFilter(new BlurMaskFilter(6f * density, BlurMaskFilter.Blur.NORMAL));

        segOn.setColor(accent);
        segOff.setColor(mixBlack(accent, 0.28f, 135));
        segStroke.setColor(mixBlack(accent, 0.55f, 135));
    }

    private void buildFrame(float w, float h) {
        float in = 9f * density;
        float c = 16f * density;
        float nd = 13f * density;
        float sh = 9f * density;
        float nL = w * 0.42f, nLb = w * 0.46f, nRb = w * 0.54f, nR = w * 0.58f;

        framePath.reset();
        framePath.moveTo(in + c, in + sh);
        framePath.lineTo(w * 0.20f, in);
        framePath.lineTo(nL, in);
        framePath.lineTo(nLb, in + nd);
        framePath.lineTo(nRb, in + nd);
        framePath.lineTo(nR, in);
        framePath.lineTo(w * 0.80f, in);
        framePath.lineTo(w - in - c, in + sh);
        framePath.lineTo(w - in, in + c + sh);
        framePath.lineTo(w - in, h - in - c);
        framePath.lineTo(w - in - c, h - in);
        framePath.lineTo(in + c, h - in);
        framePath.lineTo(in, h - in - c);
        framePath.lineTo(in, in + c + sh);
        framePath.close();
    }

    /** X координата внешней границы шеврона на заданной высоте. */
    private static float xAt(float y, float baseX, float top, float mid, float bot, float depth) {
        if (y <= mid) {
            float t = (y - top) / Math.max(1f, mid - top);
            return baseX + depth * t;
        } else {
            float t = (y - mid) / Math.max(1f, bot - mid);
            return baseX + depth * (1f - t);
        }
    }

    /**
     * Рисует не линию, а сегментированную рейку-панель.
     * left=true: данные слева, рейка смотрит вершиной вправо.
     * left=false: данные справа, рейка смотрит вершиной влево.
     */
    private void drawSegmentRail(Canvas canvas, boolean left, float fill) {
        float w = getWidth(), h = getHeight();

        // Геометрия подобрана под реф: вертикальная длинная рейка с вдавлением к центру на уровне частоты.
        float top = h * 0.135f;
        float bot = h * 0.875f;
        float mid = h * 0.42f;

        float baseX = left ? w * 0.322f : w * 0.678f;
        float depth = (left ? 1f : -1f) * Math.min(w * 0.034f, 40f * density);
        float band = 18f * density;       // ширина панели между двумя линиями
        float innerShift = left ? band : -band;

        int count = 19;
        float gap = 3.0f * density;
        float rawH = (bot - top) / count;
        float fillThreshold = bot - fill * (bot - top);

        for (int i = 0; i < count; i++) {
            float y0 = top + i * rawH + gap * 0.55f;
            float y1 = top + (i + 1) * rawH - gap * 0.55f;
            if (y1 <= y0) continue;

            float x0a = xAt(y0, baseX, top, mid, bot, depth);
            float x1a = xAt(y1, baseX, top, mid, bot, depth);
            float x0b = x0a + innerShift;
            float x1b = x1a + innerShift;

            segPath.reset();
            segPath.moveTo(x0a, y0);
            segPath.lineTo(x0b, y0);
            segPath.lineTo(x1b, y1);
            segPath.lineTo(x1a, y1);
            segPath.close();

            boolean on = y1 >= fillThreshold; // снизу вверх
            if (on) {
                canvas.drawPath(segPath, segGlow);
                canvas.drawPath(segPath, segOn);
            } else {
                canvas.drawPath(segPath, segOff);
            }
            canvas.drawPath(segPath, segStroke);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        buildFrame(w, h);

        // Наружная рамка.
        frameGlow.setStrokeWidth(6f * density);
        frameGlow.setMaskFilter(new BlurMaskFilter(6f * density, BlurMaskFilter.Blur.NORMAL));
        canvas.drawPath(framePath, frameGlow);
        frameLine.setStrokeWidth(2f * density);
        canvas.drawPath(framePath, frameLine);

        // Боковые сегментированные частотные панели.
        drawSegmentRail(canvas, true, cpuFill);
        drawSegmentRail(canvas, false, gpuFill);
    }
}
