package com.redmagic.touchsettings;

import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Minimal root helper. Runs commands via `su -c`.
 * Writing Settings.Global requires root (or WRITE_SECURE_SETTINGS).
 */
public final class RootShell {

    private RootShell() {}

    public static boolean isRootAvailable() {
        return runOne("id").contains("uid=0");
    }

    /** Runs a single command as root, returns combined stdout+stderr. */
    public static String runOne(String command) {
        StringBuilder out = new StringBuilder();
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
            p.waitFor();
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
        return out.toString();
    }

    /** Reads a Settings.Global value as root. Returns "" when unset. */
    public static String getGlobal(String key) {
        String r = runOne("settings get global " + key);
        if (r == null) return "";
        r = r.trim();
        if (r.isEmpty() || r.equals("null") || r.startsWith("ERROR")) return "";
        return r;
    }

    /**
     * Writes a raw Settings.Global value as root.
     * The value is single-quoted so '&', '+' and ',' are not interpreted by the shell.
     */
    public static String putGlobal(String key, String value) {
        String safe = value.replace("'", "'\\''");
        return runOne("settings put global " + key + " '" + safe + "'");
    }
}
