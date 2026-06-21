package com.mora.gamespace

/**
 * Reproduces EXACTLY the per-game value encoding that GameSpace (cn.nubia.gamelauncher)
 * uses. Each touch/performance key is a comma-separated list of "<package>+<value>" entries
 * (trailing comma after each). Multi-value params join ints with '&'.
 * Ported from the test-mora reference (NubiaTouchDb.java).
 */
object NubiaSettings {

    // per-game keys
    const val KEY_SAMPLE_RATE = "NubiaperformanceTouchSampleRate"
    const val KEY_SENSITIVE = "NubiaperformanceTouchSen"
    const val KEY_FOLLOW = "NubiaperformanceTouchFollow"
    const val KEY_MICRO = "NubiaperformanceTouchMicroSensitive"
    const val KEY_PERF_MODE = "NubiaperformanceMode"            // 1/2/3
    const val KEY_DISPLAY = "game_strengthen_mode_value"        // default/racing/shooting
    // global keys
    const val KEY_WIFI_LOW_LATENCY = "gsc_wifi_low_latency_mode" // 0/1
    const val KEY_STRENGTHEN_LIST = "db_game_strengthen_mode_list"

    const val MODE_BALANCE = 1
    const val MODE_BOOST = 2
    const val MODE_BEYOND = 3

    // Touch sliders store one of {-2,-1,0,1,2}; SeekBar/slider center index = 2.
    val TOUCH_SCREEN_LEVEL = intArrayOf(-2, -1, 0, 1, 2)

    fun encodeValue(values: IntArray): String {
        val sb = StringBuilder()
        for (i in values.indices) {
            if (i == 0) sb.append(values[0]) else sb.append('&').append(values[i])
        }
        return sb.toString()
    }

    fun buildSaveString(current: String?, pkg: String, value: String): String {
        var cur = current ?: ""
        if (cur.isNotEmpty() && cur.contains(pkg)) {
            val parts = cur.split(",")
            for (e in parts) {
                if (e.isNotEmpty() && e.contains(pkg)) {
                    val replacement = pkg + "+" + value
                    cur = cur.replace(e, replacement)
                    break
                }
            }
            return cur
        } else if (cur.isEmpty()) {
            return pkg + "+" + value + ","
        } else {
            return cur + pkg + "+" + value + ","
        }
    }

    fun buildSaveString(current: String?, pkg: String, values: IntArray): String =
        buildSaveString(current, pkg, encodeValue(values))

    fun parseString(raw: String?, pkg: String, def: String): String {
        if (raw.isNullOrEmpty()) return def
        if (!raw.contains(pkg)) return def
        for (e in raw.split(",")) {
            val t = e.trim()
            if (t.isEmpty() || !t.contains(pkg)) continue
            val from = t.indexOf("+")
            if (from == -1) return def
            return t.substring(from + 1)
        }
        return def
    }

    fun parse(raw: String?, pkg: String, def: IntArray): IntArray {
        if (raw.isNullOrEmpty()) return def
        if (!raw.contains(pkg)) return def
        var entry: String? = null
        for (e in raw.split(",")) {
            val t = e.trim()
            if (t.isEmpty()) continue
            if (t.contains(pkg)) { entry = t; break }
        }
        if (entry.isNullOrEmpty()) return def
        return try {
            val from = entry.indexOf("+") + 1
            val valStr = entry.substring(from)
            if (valStr.contains("&")) {
                valStr.split("&").map { it.toInt() }.toIntArray()
            } else {
                intArrayOf(valStr.toInt())
            }
        } catch (ex: Exception) {
            def
        }
    }

    /** Parse the global "pkg+index," list into an ordered list of package names. */
    fun parseStrengthenPackages(raw: String?): List<String> {
        if (raw.isNullOrEmpty()) return emptyList()
        val result = ArrayList<String>()
        for (e in raw.split(",")) {
            val t = e.trim()
            if (t.isEmpty() || !t.contains("+")) continue
            val pkg = t.substring(0, t.indexOf("+")).trim()
            if (pkg.isNotEmpty() && !result.contains(pkg)) result.add(pkg)
        }
        return result
    }

    /** Build the global "pkg+index," list from an ordered list of packages (index defaults to 0). */
    fun buildStrengthenList(packages: List<String>): String {
        val sb = StringBuilder()
        for (pkg in packages) {
            if (pkg.isBlank()) continue
            sb.append(pkg).append("+0,")
        }
        return sb.toString()
    }
}
