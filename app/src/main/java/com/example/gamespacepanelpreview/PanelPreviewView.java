package com.example.gamespacepanelpreview;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

public class PanelPreviewView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap itemRed, itemEnd, titleBg, closeIcon, mask;
    private int selected = 1;
    private float panelLeft, panelTop, panelW, panelH, menuW, titleH;

    private final String[] menu = {
        "操作设置", "性能增强", "GPU 设置", "显示增强", "声音增强", "网络设置", "功能", "资源库", "插件"
    };
    private final String[][] cards = {
        {"Touch keys", "Shoulder trigger / macro / gyro preview", "L / R / M mapping preview"},
        {"Performance", "CPU 1.02 GHz", "GPU 399 MHz", "Rise mode"},
        {"GPU", "Adreno tuning card", "Quality / Stability / FPS"},
        {"Screen", "Default / Car / Shoot / MOBA", "Color and sharpen preview"},
        {"Voice", "DTS / headset profiles", "Shoot / Music / Movie"},
        {"Network", "Wi‑Fi optimization", "Ordinary notification mode"},
        {"Function", "Fan / RedMagic Time / AI", "Quick switches"},
        {"Resource", "TGK config library", "Import / rename / share cases"},
        {"Plug", "Game plugins grid", "Record / Aim / Cast / Notes"}
    };

    public PanelPreviewView(Context c) {
        super(c);
        setFocusable(true);
        text.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        itemRed = load(c, R.drawable.panel_item_red_right);
        itemEnd = load(c, R.drawable.panel_item_red_right_end);
        titleBg = load(c, R.drawable.gamecontrol_title_background);
        closeIcon = load(c, R.drawable.gamecontrol_title_close);
        mask = load(c, R.drawable.nubia_game_strengthen_mask);
    }

    private static Bitmap load(Context c, int id) {
        Drawable d = c.getResources().getDrawable(id, c.getTheme());
        int w = Math.max(1, d.getIntrinsicWidth());
        int h = Math.max(1, d.getIntrinsicHeight());
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        d.setBounds(0, 0, w, h);
        d.draw(canvas);
        return b;
    }

    @Override protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        drawGameMock(c, w, h);
        drawOverlayMask(c, w, h);
        calculatePanel(w, h);
        drawPanel(c);
        drawHint(c, w, h);
    }

    private void drawGameMock(Canvas c, int w, int h) {
        LinearGradient bg = new LinearGradient(0,0,w,h, new int[]{0xff060812,0xff101328,0xff05060b}, null, Shader.TileMode.CLAMP);
        p.setShader(bg); c.drawRect(0,0,w,h,p); p.setShader(null);
        p.setColor(0x22ffffff); p.setStrokeWidth(dp(1));
        for (int i=0;i<12;i++) c.drawLine(i*w/12f,0,w-i*w/15f,h,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setColor(0x40ff3355);
        c.drawCircle(w*0.50f, h*0.43f, Math.min(w,h)*0.16f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawOverlayMask(Canvas c, int w, int h) {
        p.setColor(0x99000000); c.drawRect(0,0,w,h,p);
        p.setColor(0x33ff2448); c.drawRect(0,0,dp(7),h,p); c.drawRect(w-dp(7),0,w,h,p);
    }

    private void calculatePanel(int w, int h) {
        panelW = Math.min(w * 0.86f, dp(1160));
        panelH = Math.min(h * 0.82f, dp(660));
        panelLeft = (w - panelW) / 2f;
        panelTop = (h - panelH) / 2f + dp(18); // другое положение: немного ниже центра, чтобы сравнить с оверлеем
        menuW = Math.max(dp(185), panelW * 0.23f);
        titleH = Math.max(dp(52), panelH * 0.15f);
    }

    private void drawPanel(Canvas c) {
        RectF panel = new RectF(panelLeft, panelTop, panelLeft + panelW, panelTop + panelH);
        p.setColor(0xcc070811); c.drawRoundRect(panel, dp(6), dp(6), p);
        p.setColor(0xff141726); c.drawRect(panelLeft + menuW, panelTop + titleH, panel.right, panel.bottom, p);

        RectF title = new RectF(panelLeft, panelTop, panel.right, panelTop + titleH);
        c.drawBitmap(titleBg, null, title, null);
        p.setColor(0xaa0b0d18); c.drawRect(title, p);
        drawText(c, "王者荣耀 · 控制面板  /  Game Space Control Center", panelLeft + dp(26), panelTop + titleH * 0.62f, dp(21), 0xffffffff, true);
        c.drawBitmap(closeIcon, null, new RectF(panel.right - dp(55), panelTop + dp(12), panel.right - dp(20), panelTop + dp(47)), null);

        p.setColor(0xff252733); c.drawRect(panelLeft, panelTop + titleH, panelLeft + menuW, panel.bottom, p);
        drawMenu(c);
        drawContent(c);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1)); p.setColor(0x66ff2448); c.drawRoundRect(panel, dp(6), dp(6), p); p.setStyle(Paint.Style.FILL);
    }

    private void drawMenu(Canvas c) {
        float rowH = (panelH - titleH) / menu.length;
        for (int i=0;i<menu.length;i++) {
            float top = panelTop + titleH + i * rowH;
            RectF r = new RectF(panelLeft, top, panelLeft + menuW, top + rowH);
            if (i == selected) {
                p.setColor(0xff1b1d2a); c.drawRect(r, p);
                c.drawBitmap(itemRed, null, r, null);
                c.drawBitmap(itemEnd, null, new RectF(r.right-dp(4), r.top, r.right, r.bottom), null);
                p.setColor(0xffff2448); c.drawRect(r.left, r.top, r.left + dp(4), r.bottom, p);
            } else {
                p.setColor(i%2==0 ? 0xff20222e : 0xff1a1c27); c.drawRect(r,p);
            }
            drawText(c, menu[i], r.left + dp(18), r.centerY() + dp(6), dp(16), i==selected ? 0xffffffff : 0xff9ba0ad, i==selected);
        }
    }

    private void drawContent(Canvas c) {
        float x = panelLeft + menuW + dp(30), y = panelTop + titleH + dp(30);
        float cw = (panelW - menuW - dp(90)) / 3f;
        float ch = dp(150);
        String[] d = cards[selected];
        for (int i=0;i<6;i++) {
            int col = i % 3, row = i / 3;
            RectF r = new RectF(x + col*(cw+dp(15)), y + row*(ch+dp(18)), x + col*(cw+dp(15)) + cw, y + row*(ch+dp(18)) + ch);
            LinearGradient g = new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{0xff242838,0xff121520}, null, Shader.TileMode.CLAMP);
            p.setShader(g); c.drawRoundRect(r, dp(12), dp(12), p); p.setShader(null);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1)); p.setColor(0x44ff3355); c.drawRoundRect(r, dp(12), dp(12), p); p.setStyle(Paint.Style.FILL);
            p.setColor(0x22ff3355); c.drawCircle(r.right-dp(32), r.top+dp(32), dp(34), p);
            drawText(c, i==0 ? d[0] : sampleTitle(i), r.left+dp(16), r.top+dp(35), dp(17), 0xffffffff, true);
            drawText(c, i==0 ? d[1] : sampleSub(i), r.left+dp(16), r.top+dp(70), dp(13), 0xffb9becd, false);
            drawText(c, i==0 ? d[2] : "Preview card", r.left+dp(16), r.top+dp(95), dp(13), 0xff7f8495, false);
        }
    }

    private String sampleTitle(int i){ return new String[]{"Wi‑Fi","Hz","Fan","Record","Aim","Info"}[Math.min(i-1,5)]; }
    private String sampleSub(int i){ return new String[]{"399 MHz","Default","Rise","Screen record","Crosshair","Quick setting"}[Math.min(i-1,5)]; }

    private void drawHint(Canvas c, int w, int h) {
        drawText(c, "Tap left menu to switch cards. This is a preview clone, not original service.", dp(18), h - dp(18), dp(13), 0x99ffffff, false);
    }

    private void drawText(Canvas c, String s, float x, float y, float size, int color, boolean bold) {
        text.setTextSize(size); text.setColor(color); text.setTypeface(Typeface.create(Typeface.SANS_SERIF, bold ? Typeface.BOLD : Typeface.NORMAL));
        c.drawText(s, x, y, text);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            if (e.getX() >= panelLeft && e.getX() <= panelLeft+menuW && e.getY() >= panelTop+titleH && e.getY() <= panelTop+panelH) {
                int idx = (int)((e.getY() - panelTop - titleH) / ((panelH-titleH)/menu.length));
                if (idx >=0 && idx < menu.length) { selected = idx; invalidate(); }
            }
        }
        return true;
    }
    private float dp(float v){ return v * getResources().getDisplayMetrics().density; }
}
