package com.mora.gamespace

import android.content.Context

/**
 * Hardware shoulder-trigger mapping for a game, persisted as a system property that the
 * mora-perf daemon polls:
 *
 *     persist.mora.g.<package> = <left>;<right>
 *
 * Each side is "<enabled>.<x>.<y>.<rot>.<w>.<h>": display pixels captured by the calibration
 * overlay plus the capture orientation (rot = Surface.ROTATION_*) and surface size (w x h).
 * The daemon maps that into the panel-fixed raw touch axes, so landscape captures land in the
 * right spot. A disabled side is written as just "0".
 *     enabled side:  "1.540.1200.0.1080.2400"
 *     disabled side: "0"           (daemon also tolerates "0.0.0" and the legacy "1.x.y")
 *
 * left  -> KEY_F7, right -> KEY_F8 (handled by the daemon).
 *
 * Reads/writes go through android.os.SystemProperties via reflection. Reading always works;
 * writing persist.mora.* needs the privileged SELinux context granted by the permissions
 * bootstrap script, exactly like the other privileged settings this app touches.
 */
object MoraTriggers {

    const val PROP_PREFIX = "persist.mora.g."

    /**
     * A trigger side. `x`/`y` are display pixels captured by the overlay in the
     * orientation given by `rot` (Surface.ROTATION_*: 0..3) and `w`/`h` (the
     * capture surface size in that orientation). The daemon uses rot+w+h to map
     * the point into the panel-fixed raw touch axes, so a coordinate captured in
     * landscape no longer lands at the wrong place. `w`/`h` of 0 means "unknown"
     * (legacy data) and the daemon falls back to portrait scaling.
     */
    data class Side(
        val enabled: Boolean,
        val x: Int,
        val y: Int,
        val rot: Int = 0,
        val w: Int = 0,
        val h: Int = 0,
    ) {
        /** A side is only meaningfully placed once it has a non-origin coordinate. */
        val hasCoords: Boolean get() = x > 0 || y > 0

        companion object {
            val OFF = Side(false, 0, 0)
        }
    }

    data class Triggers(val left: Side, val right: Side) {
        val anyEnabled: Boolean get() = left.enabled || right.enabled

        companion object {
            val NONE = Triggers(Side.OFF, Side.OFF)
        }
    }

    fun propName(pkg: String): String = PROP_PREFIX + sanitize(pkg)

    /** Property names accept only ASCII [a-zA-Z0-9._-]; map anything else to '_'. */
    private fun sanitize(pkg: String): String {
        val sb = StringBuilder(pkg.length)
        for (c in pkg) {
            val ok = (c in 'a'..'z') || (c in 'A'..'Z') || (c in '0'..'9') ||
                c == '.' || c == '_' || c == '-'
            sb.append(if (ok) c else '_')
        }
        return sb.toString()
    }

    // Encoded as "1.x.y.rot.w.h" (display px + capture frame). A disabled or
    // unplaced side is just "0"; the daemon also tolerates the legacy "1.x.y".
    private fun encodeSide(s: Side): String =
        if (s.enabled && (s.x != 0 || s.y != 0)) "1.${s.x}.${s.y}.${s.rot}.${s.w}.${s.h}" else "0"

    fun encode(t: Triggers): String = encodeSide(t.left) + ";" + encodeSide(t.right)

    private fun parseSide(raw: String?): Side {
        val v = (raw ?: "").trim()
        if (v.isEmpty() || v == "0" || v == "0.0.0") return Side.OFF
        val parts = v.split(".")
        if (parts.size < 3) return Side.OFF
        val enabled = parts[0] == "1"
        val x = parts[1].toIntOrNull() ?: 0
        val y = parts[2].toIntOrNull() ?: 0
        val rot = parts.getOrNull(3)?.toIntOrNull() ?: 0
        val w = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val h = parts.getOrNull(5)?.toIntOrNull() ?: 0
        return Side(enabled, x, y, rot, w, h)
    }

    fun decode(raw: String?): Triggers {
        val v = (raw ?: "").trim()
        if (v.isEmpty()) return Triggers.NONE
        val sides = v.split(";")
        return Triggers(parseSide(sides.getOrNull(0)), parseSide(sides.getOrNull(1)))
    }

    private const val PREFS = "mora_triggers"

    /**
     * The app UI's source of truth is SharedPreferences, which is always writable. We also mirror
     * the value into the persist.mora.g.<pkg> system property the daemon reads (best-effort): that
     * set only succeeds once the privileged SELinux policy is installed, but the app still
     * remembers the mapping in the meantime so coordinates are never lost.
     */
    fun read(context: Context, pkg: String): Triggers {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(pkg, null)
        if (stored != null) return decode(stored)
        return decode(getProp(propName(pkg)))
    }

    /** @return true if the system-property mirror succeeded (SELinux may still reject it). */
    fun write(context: Context, pkg: String, t: Triggers): Boolean {
        val encoded = encode(t)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(pkg, encoded).apply()
        return setProp(propName(pkg), encoded)
    }

    // --- android.os.SystemProperties via reflection -------------------------------------

    private fun getProp(name: String): String {
        return try {
            val cls = Class.forName("android.os.SystemProperties")
            val m = cls.getMethod("get", String::class.java, String::class.java)
            (m.invoke(null, name, "") as? String) ?: ""
        } catch (e: Throwable) {
            ""
        }
    }

    private fun setProp(name: String, value: String): Boolean {
        return try {
            val cls = Class.forName("android.os.SystemProperties")
            val m = cls.getMethod("set", String::class.java, String::class.java)
            m.invoke(null, name, value)
            true
        } catch (e: Throwable) {
            false
        }
    }
}
