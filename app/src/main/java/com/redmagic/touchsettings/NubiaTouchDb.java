package com.redmagic.touchsettings;

/**
 * Reproduces EXACTLY the per-game value encoding that GameSpace
 * (cn.nubia.gamelauncher) uses in
 * PerformanceUtils.saveOperationParamToDB / getOperationParamFromDB.
 *
 * Why this matters: GameSpace stores every Settings.Global touch key as a
 * comma-separated list of "<package>+<value>" entries (trailing comma after
 * each entry). Multi-value params (e.g. gyro X/Y) join their ints with '&'.
 *
 *   NubiaperformanceTouchSen = "com.game.a+2,com.game.b+-1,"
 *   NubiaperformanceGyroSen  = "com.game.a+120&80,"
 *
 * The previous build wrote "<package>@<value>" which GameSpace cannot parse:
 * indexOf("+") fails, parseInt throws, and it falls back to the default value
 * (0 for the sensitivity sliders). That is exactly the "resets to 0" bug.
 *
 * This helper does a read-modify-write so other games' entries are preserved,
 * identical to the original implementation.
 */
public final class NubiaTouchDb {

    private NubiaTouchDb() {}

    /** Join ints with '&' exactly like saveOperationParamToDB. */
    public static String encodeValue(int[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i == 0) {
                sb.append(values[0]);
            } else {
                sb.append('&').append(values[i]);
            }
        }
        return sb.toString();
    }

    /**
     * Build the new Settings.Global string given the current stored string,
     * target package and value ints. Mirrors saveOperationParamToDB byte-for-byte.
     */
    public static String buildSaveString(String current, String pkg, int[] values) {
        String value = encodeValue(values);
        if (current == null) current = "";

        if (!current.isEmpty() && current.contains(pkg)) {
            // Replace the existing entry for this package, keep the rest.
            String[] parts = current.split(",");
            for (String e : parts) {
                if (!e.isEmpty() && e.contains(pkg)) {
                    String replacement = pkg + "+" + value;
                    current = current.replace(e, replacement);
                    break;
                }
            }
            return current;
        } else if (current.isEmpty()) {
            return pkg + "+" + value + ",";
        } else {
            return current + pkg + "+" + value + ",";
        }
    }

    /**
     * Parse the stored string back to the int[] for a package.
     * Mirrors getOperationParamFromDB + parserParamFromDB.
     */
    public static int[] parse(String raw, String pkg, int[] def) {
        if (raw == null || raw.isEmpty()) return def;
        if (raw.indexOf(pkg) == -1) return def;
        String entry = null;
        for (String e : raw.split(",")) {
            String t = e.trim();
            if (t.isEmpty()) continue;
            if (t.indexOf(pkg) != -1) { entry = t; break; }
        }
        if (entry == null || entry.isEmpty()) return def;
        try {
            int from = entry.indexOf("+") + 1;
            String valStr = entry.substring(from);
            if (valStr.indexOf("&") != -1) {
                String[] vs = valStr.split("&");
                int[] out = new int[vs.length];
                for (int i = 0; i < vs.length; i++) out[i] = Integer.parseInt(vs[i]);
                return out;
            }
            return new int[]{ Integer.parseInt(valStr) };
        } catch (Exception ex) {
            return def;
        }
    }
}
