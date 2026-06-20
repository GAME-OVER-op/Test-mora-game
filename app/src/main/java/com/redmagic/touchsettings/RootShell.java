package com.redmagic.touchsettings;

import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Minimal root helper. Runs commands via `su -c`.
 * NOTE: writing Settings.Global from a normal app requires root (WRITE_SECURE_SETTINGS).
 * This is a placeholder mechanism — later this can be replaced by writing kernel
 * touch-driver nodes directly instead of Settings keys.
 */
public final class RootShell {

    private RootShell() {}

    public static boolean isRootAvailable() {
        return runOne("id").contains("uid=0");
    }

    /** Runs a single command as root, returns combined stdout. */
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

    /** Writes a Settings.Global value as root. */
    public static String putGlobal(String key, String value) {
        return runOne("settings put global " + key + " '" + value + "'");
    }
}
