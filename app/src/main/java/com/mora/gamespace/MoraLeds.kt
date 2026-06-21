package com.mora.gamespace

import android.content.Context

/**
 * LED color settings + battery-saver master switch, shared with the mora-perf daemon
 * through system properties. SharedPreferences is the always-writable source of truth for
 * the UI; values are mirrored into the props (best-effort) so the daemon picks them up once
 * the privileged SELinux policy is installed.
 *
 * Encodings MUST match the daemon's led_props.rs exactly:
 *   persist.perf.saver.status = "1"|"0"
 *   persist.perf.led.charging = "<enabled>;<fan>;<ext>"
 *   persist.perf.led.notif    = "<enabled>;<stopkind>;<seconds>;<extmode>.<extcolor>"
 *   persist.perf.led.normal   = "<fan>;<ext>"
 *   persist.perf.led.gaming   = "<fan>;<ext>"
 * where <fan>/<ext> = "-" (none) or "<modecode>.<colorcode>";
 *   stopkind 0 = until_screen_on, 1 = for_seconds.
 */
object MoraLeds {

    const val PROP_SAVER = "persist.perf.saver.status"
    const val PROP_CHARGING = "persist.perf.led.charging"
    const val PROP_NOTIF = "persist.perf.led.notif"
    const val PROP_NORMAL = "persist.perf.led.normal"
    const val PROP_GAMING = "persist.perf.led.gaming"

    private const val PREFS = "mora_leds"

    // Defaults mirror the daemon's config defaults.
    private const val DEF_SAVER = "0"
    private const val DEF_CHARGING = "1;-;-"
    private const val DEF_NOTIF = "0;0;10;2.3"
    private const val DEF_NORMAL = "0.13;-"
    private const val DEF_GAMING = "2.0;-"

    // ---- data model ----
    data class Fan(val mode: Int, val color: Int)
    data class Ext(val mode: Int, val color: Int)
    data class Charging(val enabled: Boolean, val fan: Fan?, val ext: Ext?)
    data class Notif(val enabled: Boolean, val forSeconds: Boolean, val seconds: Int, val ext: Ext)
    data class Profile(val fan: Fan?, val ext: Ext?)

    // ---- option labels (mode/color code -> label) for the UI ----
    val FAN_MODES = listOf(1 to "Поток", 2 to "Дыхание", 3 to "Мигание", 4 to "Статика")
    val FAN_COLORS = listOf(
        0 to "Розовый", 1 to "Жёлтый", 2 to "Зелёный", 3 to "Синий",
        4 to "Голубой", 5 to "Фиолетовый", 6 to "Оранжевый", 13 to "Белый",
    )
    val EXT_MODES = listOf(0 to "Статика", 1 to "Дыхание", 2 to "Мигание")
    val EXT_COLORS = listOf(
        0 to "Мульти", 1 to "Красный", 2 to "Жёлтый", 3 to "Синий", 4 to "Зелёный",
        5 to "Голубой", 6 to "Белый", 7 to "Фиолетовый", 8 to "Розовый", 9 to "Оранжевый",
    )

    // ---- saver ----
    fun saverOn(context: Context): Boolean = raw(context, PROP_SAVER, DEF_SAVER).trim() == "1"
    fun setSaver(context: Context, on: Boolean) = store(context, PROP_SAVER, if (on) "1" else "0")

    // ---- charging ----
    fun charging(context: Context): Charging = parseCharging(raw(context, PROP_CHARGING, DEF_CHARGING))
    fun setCharging(context: Context, c: Charging) = store(context, PROP_CHARGING, encCharging(c))

    // ---- notifications ----
    fun notif(context: Context): Notif = parseNotif(raw(context, PROP_NOTIF, DEF_NOTIF))
    fun setNotif(context: Context, n: Notif) = store(context, PROP_NOTIF, encNotif(n))

    // ---- profiles ----
    fun normal(context: Context): Profile = parseProfile(raw(context, PROP_NORMAL, DEF_NORMAL))
    fun setNormal(context: Context, p: Profile) = store(context, PROP_NORMAL, encProfile(p))
    fun gaming(context: Context): Profile = parseProfile(raw(context, PROP_GAMING, DEF_GAMING))
    fun setGaming(context: Context, p: Profile) = store(context, PROP_GAMING, encProfile(p))

    // ---- storage (prefs = source of truth, prop = best-effort mirror) ----
    private fun raw(context: Context, prop: String, def: String): String {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(prop, null)
        if (stored != null && stored.isNotEmpty()) return stored
        val p = getProp(prop)
        if (p.isNotEmpty()) return p
        return def
    }

    private fun store(context: Context, prop: String, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(prop, value).apply()
        setProp(prop, value)
    }

    // ---- encode / parse (mirror daemon) ----
    private fun encFanTok(f: Fan?): String = if (f == null) "-" else "${f.mode}.${f.color}"
    private fun encExtTok(e: Ext?): String = if (e == null) "-" else "${e.mode}.${e.color}"

    private fun parseFanTok(tok: String): Fan? {
        val t = tok.trim()
        if (t.isEmpty() || t == "-") return null
        val parts = t.split(".")
        val m = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
        val c = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 13
        return Fan(m, c)
    }

    private fun parseExtTok(tok: String): Ext? {
        val t = tok.trim()
        if (t.isEmpty() || t == "-") return null
        val parts = t.split(".")
        val m = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
        val c = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 6
        return Ext(m, c)
    }

    private fun encCharging(c: Charging): String =
        "${if (c.enabled) 1 else 0};${encFanTok(c.fan)};${encExtTok(c.ext)}"

    private fun parseCharging(v: String): Charging {
        val p = v.split(";")
        val en = p.getOrNull(0)?.trim() == "1"
        val fan = parseFanTok(p.getOrNull(1) ?: "-")
        val ext = parseExtTok(p.getOrNull(2) ?: "-")
        return Charging(en, fan, ext)
    }

    private fun encNotif(n: Notif): String =
        "${if (n.enabled) 1 else 0};${if (n.forSeconds) 1 else 0};${n.seconds};${n.ext.mode}.${n.ext.color}"

    private fun parseNotif(v: String): Notif {
        val p = v.split(";")
        val en = p.getOrNull(0)?.trim() == "1"
        val fs = p.getOrNull(1)?.trim() == "1"
        val sec = p.getOrNull(2)?.trim()?.toIntOrNull() ?: 10
        val ext = parseExtTok(p.getOrNull(3) ?: "2.3") ?: Ext(2, 3)
        return Notif(en, fs, sec, ext)
    }

    private fun encProfile(p: Profile): String = "${encFanTok(p.fan)};${encExtTok(p.ext)}"

    private fun parseProfile(v: String): Profile {
        val p = v.split(";")
        val fan = parseFanTok(p.getOrNull(0) ?: "-")
        val ext = parseExtTok(p.getOrNull(1) ?: "-")
        return Profile(fan, ext)
    }

    // ---- system property reflection ----
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
