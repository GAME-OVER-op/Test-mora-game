package com.mora.gamedock;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

/**
 * Постоянная su-сессия для чтения sysfs «вживую».
 * Если root недоступен — available = false, и чтение идёт напрямую (Sysfs).
 */
public class RootShell {

    private Process proc;
    private DataOutputStream out;
    private BufferedReader in;
    public volatile boolean available = false;

    private static final String MARKER = "__MORA_END__";

    public synchronized void start() {
        try {
            proc = Runtime.getRuntime().exec(new String[]{"su"});
            out = new DataOutputStream(proc.getOutputStream());
            in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            out.writeBytes("echo __MORA_OK__\n");
            out.flush();
            long t0 = System.currentTimeMillis();
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("__MORA_OK__")) { available = true; break; }
                if (System.currentTimeMillis() - t0 > 4000) break;
            }
        } catch (Exception e) {
            available = false;
        }
    }

    public synchronized String run(String cmd) {
        if (!available) return null;
        try {
            out.writeBytes(cmd + "\n");
            out.writeBytes("echo " + MARKER + "\n");
            out.flush();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains(MARKER)) break;
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            available = false;
            return null;
        }
    }

    public String cat(String path) {
        return run("cat " + path + " 2>/dev/null");
    }

    public long catLong(String path, long def) {
        String s = cat(path);
        if (s == null || s.isEmpty()) return def;
        try {
            return Long.parseLong(s.trim().split("\\s+")[0]);
        } catch (Exception e) {
            return def;
        }
    }

    public void stop() {
        try { if (out != null) { out.writeBytes("exit\n"); out.flush(); } } catch (Exception ignored) {}
        try { if (proc != null) proc.destroy(); } catch (Exception ignored) {}
    }
}
