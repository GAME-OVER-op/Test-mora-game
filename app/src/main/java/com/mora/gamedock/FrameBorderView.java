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
 * Правильная логика по референсу:
 * - это НЕ центральная V-линия;
 * - это диагональный сегментированный край боковой панели данных;
 * - сегменты заполняются снизу вверх по частоте CPU/GPU.
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
        segStroke.setStrokeWidth(0.75f * density);

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

        segGlow.setColor(withAlpha(accent, 90));
        segGlow.setMaskFilter(new BlurMaskFilter(4.5f * density, BlurMaskFilter.Blur.NORMAL));

        segOn.setColor(withAlpha(accent, 235));
        segOff.setColor(mixBlack(accent, 0.22f, 120));
        segStroke.setColor(mixBlack(accent, 0.50f, 95));
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

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Диагональная рейка-край панели.
     * left=true: левая панель, рейка идёт \ вниз вправо.
     * left=false: правая панель, рейка зеркальная / вниз влево.
     */
    private void drawSideRail(Canvas canvas, boolean left, float fill) {
        float w = getWidth(), h = getHeight();

        // Рейка должна быть рядом с блоком данных, не в пустом центре.
        // На 1024px это примерно: левая 330→390, правая 694→634.
        float top = h * 0.12f;
        float bot = h * 0.89f;
        float xTop = left ? w * 0.315f : w * 0.685f;
        float xBot = left ? w * 0.382f : w * 0.618f;

        // Ширина полосы меньше, чем в прошлом варианте — как узкий край панели.
        float band = 15f * density;
        float innerShift = left ? band : -band;

        int count = 21;
        float gap = 2.2f * density;
        float step = (bot - top) / count;
        float threshold = bot - fill * (bot - top);

        for (int i = 0; i < count; i++) {
            float y0 = top + i * step + gap * 0.55f;
            float y1 = top + (i + 1) * step - gap * 0.55f;
            if (y1 <= y0) continue;

            float t0 = (y0 - top) / Math.max(1f, bot - top);
            float t1 = (y1 - top) / Math.max(1f, bot - top);
            float xa0 = lerp(xTop, xBot, t0);
            float xa1 = lerp(xTop, xBot, t1);
            float xb0 = xa0 + innerShift;
            float xb1 = xa1 + innerShift;

            // Отдельная трапеция-сегмент.
            segPath.reset();
            segPath.moveTo(xa0, y0);
            segPath.lineTo(xb0, y0);
            segPath.lineTo(xb1, y1);
            segPath.lineTo(xa1, y1);
            segPath.close();

            boolean on = y1 >= threshold; // fill снизу вверх
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

        frameGlow.setStrokeWidth(6f * density);
        frameGlow.setMaskFilter(new BlurMaskFilter(6f * density, BlurMaskFilter.Blur.NORMAL));
        canvas.drawPath(framePath, frameGlow);

        frameLine.setStrokeWidth(2f * density);
        canvas.drawPath(framePath, frameLine);

        drawSideRail(canvas, true, cpuFill);
        drawSideRail(canvas, false, gpuFill);
    }
}
