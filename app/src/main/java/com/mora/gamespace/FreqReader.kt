package com.mora.gamespace

import java.io.BufferedReader
import java.io.FileReader

/**
 * Reads the same kernel frequency nodes GameSpace uses for the CPU / GPU rings.
 * Tries a direct file read first, falls back to `su -c cat` when denied.
 * Ported from the test-mora reference (FreqReader.java).
 */
object FreqReader {

    const val CPU_CUR = "/sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq"
    const val CPU_MAX = "/sys/devices/system/cpu/cpu7/cpufreq/cpuinfo_max_freq"
    const val GPU_CUR = "/sys/class/kgsl/kgsl-3d0/gpuclk"
    const val GPU_MAX = "/sys/class/kgsl/kgsl-3d0/max_gpuclk"
    const val GPU_CUR_MTK = "/sys/devices/platform/soc/soc:mm/23100000.gpu/devfreq/23100000.gpu/cur_freq"
    const val GPU_MAX_MTK = "/sys/devices/platform/soc/soc:mm/23100000.gpu/devfreq/23100000.gpu/max_freq"

    fun read(path: String): Long {
        try {
            BufferedReader(FileReader(path)).use { r ->
                val line = r.readLine()?.trim()
                if (!line.isNullOrEmpty()) return extractFirstNumber(line)
            }
        } catch (ignored: Exception) {
        }
        try {
            val out = RootShell.runOne("cat " + path).trim()
            if (out.isNotEmpty() && !out.startsWith("ERROR")) return extractFirstNumber(out)
        } catch (ignored: Exception) {
        }
        return -1L
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
    fun gpuMax(): Long { var v = read(GPU_MAX); if (v <= 0) v = read(GPU_MAX_MTK); return if (v > 0) v else 1000000000L }
}
