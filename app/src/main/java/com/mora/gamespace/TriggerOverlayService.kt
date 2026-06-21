package com.mora.gamespace

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.hypot

/**
 * Full-screen calibration overlay shown ON TOP of the running game. The user drags the
 * left/right markers onto the in-game buttons; tapping the corner button SAVES the
 * coordinates (writes the persist.mora.g.<pkg> property the daemon reads) and returns to
 * GameSpace. Requires the "draw over other apps" privilege, granted by the permissions
 * script. Runs as a foreground service so the system keeps the window alive.
 */
class TriggerOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlay: OverlayView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()

        val pkg = intent?.getStringExtra(EXTRA_PKG) ?: ""
        val leftEnabled = intent?.getBooleanExtra(EXTRA_LEFT, false) ?: false
        val rightEnabled = intent?.getBooleanExtra(EXTRA_RIGHT, false) ?: false

        if (pkg.isEmpty() || (!leftEnabled && !rightEnabled)) {
            stopSelf()
            return START_NOT_STICKY
        }

        removeOverlay()

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val saved = MoraTriggers.read(pkg)
        val view = OverlayView(this, leftEnabled, rightEnabled, saved)
        view.onSave = { triggers ->
            MoraTriggers.write(pkg, triggers)
            reopenTriggers(pkg)
            stopSelf()
        }
        overlay = view

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = 0
        lp.y = 0

        try {
            wm.addView(view, lp)
        } catch (e: Throwable) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun reopenTriggers(pkg: String) {
        TriggerBridge.request(pkg)
        val i = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            putExtra(EXTRA_PKG, pkg)
        }
        try {
            startActivity(i)
        } catch (e: Throwable) {
        }
    }

    private fun removeOverlay() {
        val v = overlay
        overlay = null
        if (v != null) {
            try {
                windowManager?.removeView(v)
            } catch (e: Throwable) {
            }
        }
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    private fun startInForeground() {
        val channelId = "mora_trigger_overlay"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Настройка триггеров", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Настройка триггеров")
            .setContentText("Перетащите метки на кнопки в игре")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIF_ID, notification)
            }
        } catch (e: Throwable) {
        }
    }

    companion object {
        const val EXTRA_PKG = "pkg"
        const val EXTRA_LEFT = "left"
        const val EXTRA_RIGHT = "right"
        private const val NOTIF_ID = 4711
    }
}

/**
 * The actual drawn overlay. Coordinates are kept in raw view pixels; the window covers the
 * whole display at (0,0), so view-local coordinates equal screen coordinates.
 */
private class OverlayView(
    context: Context,
    private val leftEnabled: Boolean,
    private val rightEnabled: Boolean,
    private val saved: MoraTriggers.Triggers,
) : View(context) {

    var onSave: ((MoraTriggers.Triggers) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density

    private val markerBmp = decode(R.drawable.free_change_key_h_select)
    private val closeBmp = decode(R.drawable.tgk_preview_close_btn_bg)
    private val leftPointerBmp = decode(R.drawable.pionter_balance)
    private val rightPointerBmp = decode(R.drawable.pionter_infinite)

    private val markerSize = dp(64f)
    private val closeSize = dp(54f)

    private val dimPaint = Paint().apply { color = Color.argb(110, 0, 0, 0) }
    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val leftRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        color = Color.parseColor("#35C7F0")
    }
    private val rightRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        color = Color.parseColor("#FF4D4D")
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = dp(13f)
        textAlign = Paint.Align.CENTER
    }

    private var lx = 0f
    private var ly = 0f
    private var rx = 0f
    private var ry = 0f
    private var initialized = false
    private var dragging = 0 // 0 none, 1 left, 2 right
    private val closeRect = Rect()

    private fun decode(resId: Int): Bitmap? =
        try {
            BitmapFactory.decodeResource(resources, resId)
        } catch (e: Throwable) {
            null
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!initialized && w > 0 && h > 0) {
            lx = if (saved.left.hasCoords) saved.left.x.toFloat() else w * 0.30f
            ly = if (saved.left.hasCoords) saved.left.y.toFloat() else h * 0.55f
            rx = if (saved.right.hasCoords) saved.right.x.toFloat() else w * 0.70f
            ry = if (saved.right.hasCoords) saved.right.y.toFloat() else h * 0.55f
            initialized = true
        }
        val pad = dp(16f).toInt()
        val cs = closeSize.toInt()
        closeRect.set(w - pad - cs, pad, w - pad, pad + cs)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, dimPaint)

        drawPointers(canvas)

        if (leftEnabled) drawMarker(canvas, lx, ly, leftRing, "Л")
        if (rightEnabled) drawMarker(canvas, rx, ry, rightRing, "П")

        closeBmp?.let { canvas.drawBitmap(it, null, closeRect, bmpPaint) }
    }

    private fun drawMarker(canvas: Canvas, cx: Float, cy: Float, ring: Paint, label: String) {
        val half = markerSize / 2f
        canvas.drawCircle(cx, cy, half * 0.80f, ring)
        markerBmp?.let {
            val dst = Rect(
                (cx - half).toInt(),
                (cy - half).toInt(),
                (cx + half).toInt(),
                (cy + half).toInt(),
            )
            canvas.drawBitmap(it, null, dst, bmpPaint)
        }
        canvas.drawText(label, cx, cy - half - dp(6f), labelPaint)
    }

    private fun drawPointers(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val portrait = h >= w
        val pLen = dp(150f)
        val pThick = dp(30f)
        if (portrait) {
            // Held vertically: both sticks hug the right edge.
            // top = left trigger (balance), bottom = right trigger (infinite).
            val x2 = w - dp(6f)
            val x1 = x2 - pThick
            leftPointerBmp?.let {
                val top = h * 0.16f
                canvas.drawBitmap(it, null, Rect(x1.toInt(), top.toInt(), x2.toInt(), (top + pLen).toInt()), bmpPaint)
            }
            rightPointerBmp?.let {
                val bottom = h * 0.84f
                canvas.drawBitmap(it, null, Rect(x1.toInt(), (bottom - pLen).toInt(), x2.toInt(), bottom.toInt()), bmpPaint)
            }
        } else {
            // Held horizontally: sticks lie along the top edge.
            val y1 = dp(6f)
            val y2 = y1 + pThick
            leftPointerBmp?.let {
                val left = w * 0.16f
                canvas.drawBitmap(it, null, Rect(left.toInt(), y1.toInt(), (left + pLen).toInt(), y2.toInt()), bmpPaint)
            }
            rightPointerBmp?.let {
                val right = w * 0.84f
                canvas.drawBitmap(it, null, Rect((right - pLen).toInt(), y1.toInt(), right.toInt(), y2.toInt()), bmpPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (closeRect.contains(x.toInt(), y.toInt())) {
                    save()
                    return true
                }
                dragging = nearestMarker(x, y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging == 1) {
                    lx = clampX(x); ly = clampY(y); invalidate()
                } else if (dragging == 2) {
                    rx = clampX(x); ry = clampY(y); invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = 0
            }
        }
        return true
    }

    private fun nearestMarker(x: Float, y: Float): Int {
        val hit = markerSize.toDouble()
        val dl = if (leftEnabled) hypot((x - lx).toDouble(), (y - ly).toDouble()) else Double.MAX_VALUE
        val dr = if (rightEnabled) hypot((x - rx).toDouble(), (y - ry).toDouble()) else Double.MAX_VALUE
        return when {
            dl <= hit && dl <= dr -> 1
            dr <= hit -> 2
            else -> 0
        }
    }

    private fun clampX(v: Float): Float = v.coerceIn(0f, width.toFloat())
    private fun clampY(v: Float): Float = v.coerceIn(0f, height.toFloat())

    private fun save() {
        val left = MoraTriggers.Side(leftEnabled, lx.toInt(), ly.toInt())
        val right = MoraTriggers.Side(rightEnabled, rx.toInt(), ry.toInt())
        onSave?.invoke(MoraTriggers.Triggers(left, right))
    }
}
