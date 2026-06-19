package com.example.gamespaceccpreview;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

public class ControlCenterPreviewView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap bg, ccOn, ccOff;
    private final Bitmap[] icons;
    private boolean rightSide = true;

    private final String[] labels = {
        "Super Snap", "Screen Record", "Touch lock", "Counter",
        "Somatosensory", "Ordinary Noti", "No Calls", "Fan",
        "Frame-Rate", "Dock", "Custom", "More"
    };
    private final int[] iconIds = {
        R.drawable.userdefine_snap, R.drawable.ic_qs_manual_record_normal, R.drawable.userdefine_mis_operate, R.drawable.game_counter_h_normal,
        R.drawable.control_center_ing, R.drawable.game_control_panel_for_card, R.drawable.more_select_for_card, R.drawable.userdefine_fan,
        R.drawable.ic_qs_framerate_display_normal, R.drawable.userdefine_dock, R.drawable.userdefine_custom_in, R.drawable.more_select_for_card
    };

    public ControlCenterPreviewView(Context c) {
        super(c);
        bg = load(c, R.drawable.game_space_window_bg);
        ccOn = load(c, R.drawable.control_center_ing);
        ccOff = load(c, R.drawable.control_center_dising);
        icons = new Bitmap[iconIds.length];
        for (int i=0;i<iconIds.length;i++) icons[i] = load(c, iconIds[i]);
        t.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
    }
    private static Bitmap load(Context c, int id) {
        Drawable d = c.getResources().getDrawable(id, c.getTheme());
        int w = Math.max(1, d.getIntrinsicWidth()), h = Math.max(1, d.getIntrinsicHeight());
        Bitmap b = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b); d.setBounds(0,0,w,h); d.draw(canvas); return b;
    }

    @Override protected void onDraw(Canvas c) {
        int w=getWidth(), h=getHeight();
        c.drawBitmap(bg, null, new RectF(0,0,w,h), null);
        p.setColor(0x77000000); c.drawRect(0,0,w,h,p);
        drawSideHandles(c,w,h);
        drawPanel(c,w,h);
        drawMiniLeftCard(c,w,h);
    }

    private void drawSideHandles(Canvas c, int w, int h) {
        float hw = dp(34), hh = dp(118), cy = h/2f;
        drawHandle(c, 0, cy-hh/2, hw, cy+hh/2, true);
        drawHandle(c, w-hw, cy-hh/2, w, cy+hh/2, false);
    }
    private void drawHandle(Canvas c, float l, float top, float r, float b, boolean left) {
        Path path = new Path();
        if (left) { path.moveTo(l,top+dp(12)); path.quadTo(l,top,r-dp(5),top); path.lineTo(r,b); path.quadTo(l,b,l,b-dp(12)); }
        else { path.moveTo(r,top+dp(12)); path.quadTo(r,top,l+dp(5),top); path.lineTo(l,b); path.quadTo(r,b,r,b-dp(12)); }
        path.close();
        LinearGradient g = new LinearGradient(l,top,r,b,0xcc1b1d26,0xcc05060a,Shader.TileMode.CLAMP);
        p.setShader(g); c.drawPath(path,p); p.setShader(null);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1)); p.setColor(0xaaff1438); c.drawPath(path,p); p.setStyle(Paint.Style.FILL);
        drawText(c, left ? ">" : "<", (l+r)/2-dp(5), (top+b)/2+dp(9), dp(28), 0xffff3355, true);
    }

    private void drawPanel(Canvas c, int w, int h) {
        float panelW = Math.min(dp(760), w*0.60f);
        float panelH = Math.min(dp(560), h*0.78f);
        float left = rightSide ? w - panelW - dp(72) : dp(72);
        float top = (h-panelH)/2f;
        RectF panel = new RectF(left, top, left+panelW, top+panelH);
        p.setColor(0xea080a10); c.drawRoundRect(panel, dp(24), dp(24), p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1)); p.setColor(0x99ff2448); c.drawRoundRect(panel, dp(24), dp(24), p); p.setStyle(Paint.Style.FILL);

        drawHeader(c, panel);
        drawPerfRow(c, panel);
        drawGrid(c, panel);
    }

    private void drawHeader(Canvas c, RectF pRect) {
        drawText(c, "Game Space", pRect.left+dp(28), pRect.top+dp(38), dp(18), 0xffdce0ea, true);
        drawText(c, "Control Center", pRect.left+dp(28), pRect.top+dp(67), dp(30), 0xffffffff, true);
        drawText(c, "×", pRect.right-dp(42), pRect.top+dp(48), dp(28), 0xffbfc4d2, false);
    }

    private void drawPerfRow(Canvas c, RectF pRect) {
        float y = pRect.top+dp(96), h = dp(98), gap = dp(12);
        float x = pRect.left+dp(24), total = pRect.width()-dp(48);
        float bw = (total-gap*3)/4f;
        String[][] vals = {{"CUBE", "Rise"}, {"CPU", "1.02GHz"}, {"GPU", "399MHz"}, {"Hz", "120"}};
        for(int i=0;i<4;i++) {
            RectF r = new RectF(x+i*(bw+gap), y, x+i*(bw+gap)+bw, y+h);
            LinearGradient g = new LinearGradient(r.left,r.top,r.right,r.bottom,0xff1c2030,0xff10131d,Shader.TileMode.CLAMP);
            p.setShader(g); c.drawRoundRect(r,dp(16),dp(16),p); p.setShader(null);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1)); p.setColor(0x55ff2448); c.drawRoundRect(r,dp(16),dp(16),p); p.setStyle(Paint.Style.FILL);
            drawText(c, vals[i][0], r.left+dp(14), r.top+dp(30), dp(13), 0xff9097a8, true);
            drawText(c, vals[i][1], r.left+dp(14), r.top+dp(68), dp(22), i==0?0xffff3658:0xffffffff, true);
        }
    }

    private void drawGrid(Canvas c, RectF pRect) {
        float left = pRect.left+dp(24), top = pRect.top+dp(220);
        float cellW = (pRect.width()-dp(48))/4f;
        float cellH = dp(96);
        for(int i=0;i<labels.length;i++) {
            int row=i/4, col=i%4;
            float cx = left+col*cellW+cellW/2f, cy=top+row*cellH+dp(26);
            RectF ib = new RectF(cx-dp(25), cy-dp(25), cx+dp(25), cy+dp(25));
            p.setColor(0xff171a26); c.drawRoundRect(new RectF(cx-dp(42), cy-dp(36), cx+dp(42), cy+dp(58)), dp(18), dp(18), p);
            p.setColor(0x24ff2448); c.drawCircle(cx,cy,dp(32),p);
            c.drawBitmap(icons[i], null, ib, null);
            drawTextCenter(c, labels[i], cx, cy+dp(50), dp(11), 0xffd9ddea, false);
        }
    }

    private void drawMiniLeftCard(Canvas c, int w, int h) {
        // Маленькая боковая карточка-заглушка, чтобы проверить именно свайповые [><] края.
        RectF r = new RectF(dp(48), h/2f-dp(105), dp(190), h/2f+dp(105));
        p.setColor(0xd20b0d14); c.drawRoundRect(r, dp(18), dp(18), p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1)); p.setColor(0x88ff2448); c.drawRoundRect(r, dp(18), dp(18), p); p.setStyle(Paint.Style.FILL);
        c.drawBitmap(ccOff, null, new RectF(r.left+dp(35), r.top+dp(18), r.right-dp(35), r.top+dp(88)), null);
        drawTextCenter(c,"Swipe card", r.centerX(), r.top+dp(122), dp(13),0xffdce0ea,true);
        drawTextCenter(c,"[ >  < ]", r.centerX(), r.top+dp(158), dp(22),0xffff3355,true);
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getAction()==MotionEvent.ACTION_UP){ rightSide = e.getX() > getWidth()/2f; invalidate(); }
        return true;
    }
    private void drawText(Canvas c,String s,float x,float y,float size,int color,boolean bold){
        t.setTextSize(size); t.setColor(color); t.setTypeface(Typeface.create(Typeface.SANS_SERIF,bold?Typeface.BOLD:Typeface.NORMAL)); c.drawText(s,x,y,t);
    }
    private void drawTextCenter(Canvas c,String s,float x,float y,float size,int color,boolean bold){
        t.setTextSize(size); t.setColor(color); t.setTypeface(Typeface.create(Typeface.SANS_SERIF,bold?Typeface.BOLD:Typeface.NORMAL)); t.setTextAlign(Paint.Align.CENTER); c.drawText(s,x,y,t); t.setTextAlign(Paint.Align.LEFT);
    }
    private float dp(float v){ return v*getResources().getDisplayMetrics().density; }
}
