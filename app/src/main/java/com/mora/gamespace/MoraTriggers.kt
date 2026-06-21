package com.mora.gamespace

/**
 * Hardware shoulder-trigger mapping for a game, persisted as a system property that the
 * mora-perf daemon polls:
 *
 *     persist.mora.g.<package> = <left>;<right>
 *
 * Each side is "<enabled>.<x>.<y>" in RAW screen pixels (the same coordinate space the daemon
 * injects into via the Nubia sar nodes). A disabled side is written as just "0".
 *     enabled side:  "1.540.1200"
 *     disabled side: "0"           (the daemon also tolerates "0.0.0")
 *
 * left  -> KEY_F7, right -> KEY_F8 (handled by the daemon).
 *
 * Reads/writes go through android.os.SystemProperties via reflection. Reading always works;
 * writing persist.mora.* needs the privileged SELinux context granted by the permissions
 * bootstrap script, exactly like the other privileged settings this app touches.
 */
object MoraTriggers {

    const val PROP_PREFIX = "persist.mora.g."

    data class Side(val enabled: Boolean, val x: Int, val y: Int) {
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

    private fun encodeSide(s: Side): String =
        if (s.enabled && (s.x != 0 || s.y != 0)) "1.${s.x}.${s.y}" else "0"

    fun encode(t: Triggers): String = encodeSide(t.left) + ";" + encodeSide(t.right)

    private fun parseSide(raw: String?): Side {
        val v = (raw ?: "").trim()
        if (v.isEmpty() || v == "0" || v == "0.0.0") return Side.OFF
        val parts = v.split(".")
        if (parts.size < 3) return Side.OFF
        val enabled = parts[0] == "1"
        val x = parts[1].toIntOrNull() ?: 0
        val y = parts[2].toIntOrNull() ?: 0
        return Side(enabled, x, y)
    }

    fun decode(raw: String?): Triggers {
        val v = (raw ?: "").trim()
        if (v.isEmpty()) return Triggers.NONE
        val sides = v.split(";")
        return Triggers(parseSide(sides.getOrNull(0)), parseSide(sides.getOrNull(1)))
    }

    fun read(pkg: String): Triggers = decode(getProp(propName(pkg)))

    /** @return true if the underlying set call did not throw (SELinux may still reject it). */
    fun write(pkg: String, t: Triggers): Boolean = setProp(propName(pkg), encode(t))

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
