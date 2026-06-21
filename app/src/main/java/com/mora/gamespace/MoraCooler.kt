package com.mora.gamespace

import android.content.Context

/**
 * Master cooler on/off switch, shared with the mora-perf daemon through the system
 * property persist.perf.fan.status (1 = on, 0 = off).
 *
 * SharedPreferences is the always-writable source of truth for the UI; we also mirror the
 * value into the system property (best-effort) so the daemon picks it up once the privileged
 * SELinux policy is installed. The daemon itself defaults the property to "1" if it is unset.
 */
object MoraCooler {

    const val PROP = "persist.perf.fan.status"
    private const val PREFS = "mora_cooler"
    private const val KEY = "status"

    /** Default ON: only an explicit "0" means disabled; empty/unset is treated as enabled. */
    fun isOn(context: Context): Boolean {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        if (stored != null) return stored == "1"
        return getProp(PROP) != "0"
    }

    /** @return true if the system-property mirror succeeded (SELinux may still reject it). */
    fun setOn(context: Context, on: Boolean): Boolean {
        val v = if (on) "1" else "0"
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, v).apply()
        return setProp(PROP, v)
    }

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
