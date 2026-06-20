package com.redmagic.touchsettings;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Reads the SAME kernel frequency nodes that GameSpace's PerformanceCircleView
 * uses to draw the CPU / GPU rings, so the values match the original screen.
 *
 *   CPU (big core / cpu7):  /sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq  (kHz)
 *   CPU max:                /sys/devices/system/cpu/cpu7/cpufreq/cpuinfo_max_freq  (kHz)
 *   GPU cur (Qualcomm kgsl): /sys/class/kgsl/kgsl-3d0/gpuclk                       (Hz)
 *   GPU max:                 /sys/class/kgsl/kgsl-3d0/max_gpuclk                    (Hz)
 *
 * Most of these nodes are world-readable, so we try a direct file read first and
 * only fall back to `su -c cat` when that is denied.
 */
public final class FreqReader {

    private FreqReader() {}

    // CPU big core (matches PerformanceCircleView PATH_CUR_CPU_MAIN / PATH_MAX_CPU_MAIN)
    public static final String CPU_CUR = "/sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq";
    public static final String CPU_MAX = "/sys/devices/system/cpu/cpu7/cpufreq/cpuinfo_max_freq";
    public static final String CPU_MID = "/sys/devices/system/cpu/cpu4/cpufreq/scaling_cur_freq";
    public static final String CPU_MIN = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";

    // GPU (Qualcomm kgsl; values in Hz)
    public static final String GPU_CUR = "/sys/class/kgsl/kgsl-3d0/gpuclk";
    public static final String GPU_MAX = "/sys/class/kgsl/kgsl-3d0/max_gpuclk";
    // MediaTek fallback (values in Hz)
    public static final String GPU_CUR_MTK = "/sys/devices/platform/soc/soc:mm/23100000.gpu/devfreq/23100000.gpu/cur_freq";
    public static final String GPU_MAX_MTK = "/sys/devices/platform/soc/soc:mm/23100000.gpu/devfreq/23100000.gpu/max_freq";

    /** Returns node value as long, or -1 if unreadable. */
    public static long read(String path) {
        // 1) direct read
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(path));
            String line = r.readLine();
            if (line != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    return Long.parseLong(extractFirstNumber(line));
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (r != null) try { r.close(); } catch (Exception ignored) {}
        }
        // 2) root fallback
        try {
            String out = RootShell.runOne("cat " + path).trim();
            if (!out.isEmpty() && !out.startsWith("ERROR")) {
                return Long.parseLong(extractFirstNumber(out));
            }
        } catch (Exception ignored) {}
        return -1L;
    }

    private static String extractFirstNumber(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') sb.append(c);
            else if (sb.length() > 0) break;
        }
        return sb.length() == 0 ? "-1" : sb.toString();
    }

    public static long cpuCur() { return read(CPU_CUR); }

    public static long cpuMax() {
        long m = read(CPU_MAX);
        return m > 0 ? m : 3187200L; // sane default (~3.19 GHz) if node hidden
    }

    public static long gpuCur() {
        long v = read(GPU_CUR);
        if (v > 0) return v;
        return read(GPU_CUR_MTK);
    }

    public static long gpuMax() {
        long v = read(GPU_MAX);
        if (v > 0) return v;
        v = read(GPU_MAX_MTK);
        return v > 0 ? v : 1000000000L;
    }

    /** kHz -> human string. >=1 GHz shows GHz, else MHz. unit[0] returned separately. */
    public static String formatKHz(long khz, String[] unitOut) {
        if (khz <= 0) { unitOut[0] = ""; return "--"; }
        if (khz >= 1000000L) {
            unitOut[0] = "ГГц";
            return String.format(java.util.Locale.US, "%.2f", khz / 1000000.0);
        }
        unitOut[0] = "МГц";
        return String.valueOf(Math.round(khz / 1000.0));
    }

    /** Hz -> human string. >=1 GHz shows GHz, else MHz. */
    public static String formatHz(long hz, String[] unitOut) {
        if (hz <= 0) { unitOut[0] = ""; return "--"; }
        if (hz >= 1000000000L) {
            unitOut[0] = "ГГц";
            return String.format(java.util.Locale.US, "%.2f", hz / 1000000000.0);
        }
        unitOut[0] = "МГц";
        return String.valueOf(Math.round(hz / 1000000.0));
    }
}
