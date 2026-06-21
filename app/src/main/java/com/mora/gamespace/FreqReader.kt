package com.mora.gamespace

import java.io.BufferedReader
import java.io.FileReader

/**
 * Reads the kernel frequency nodes for the CPU / GPU rings via a plain file read.
 * No root: SELinux grants our privileged app read access to these sysfs paths.
 */
object FreqReader {

    const val CPU_CUR = "/sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq"
    const val CPU_MAX = "/sys/devices/system/cpu/cpu7/cpufreq/cpuinfo_max_freq"
    const val GPU_CUR = "/sys/class/kgsl/kgsl-3d0/gpuclk"
    const val GPU_MAX = "/sys/class/kgsl/kgsl-3d0/max_gpuclk"
    const val GPU_CUR_MTK = "/sys/devices/platform/soc/soc:mm/23100000.gpu/devfreq/23100000.gpu/cur_freq"
    const val GPU_MAX_MTK = "/sys/devices/platform/soc/soc:mm/23100000.gpu/devfreq/23100000.gpu/max_freq"

    const val FAN_LEVEL = "/sys/kernel/fan/fan_speed_level"
    const val FAN_ENABLE = "/sys/kernel/fan/fan_enable"

    fun read(path: String): Long {
        return try {
            BufferedReader(FileReader(path)).use { r ->
                val line = r.readLine()?.trim()
                if (line.isNullOrEmpty()) -1L else extractFirstNumber(line)
            }
        } catch (e: Exception) {
            -1L
        }
    }

    private fun extractFirstNumber(s: String): Long {
        val sb = StringBuilder()
        for (c in s) {
            if (c in '0'..'9') sb.append(c) else if (sb.isNotEmpty()) break
        }
        return if (sb.isEmpty()) -1L else sb.toString().toLong()
    }

    fun cpuCur(): Long = read(CPU_CUR)
    fun cpuMax(): Long { val m = read(CPU_MAX); return if (m > 0) m else 3187200L }
    fun gpuCur(): Long { val v = read(GPU_CUR); return if (v > 0) v else read(GPU_CUR_MTK) }
    fun gpuMax(): Long { var v = read(GPU_MAX); if (v <= 0) v = read(GPU_MAX_MTK); return if (v > 0) v else 1_000_000_000L }

    /** Physical cooler speed level 0..5 (0 when the fan is disabled or unreadable). */
    fun fanLevel(): Int {
        if (read(FAN_ENABLE) == 0L) return 0
        val lvl = read(FAN_LEVEL)
        return if (lvl < 0) 0 else lvl.toInt().coerceIn(0, 5)
    }
}
