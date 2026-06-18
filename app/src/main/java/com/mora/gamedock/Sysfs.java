package com.mora.gamedock;

import java.io.BufferedReader;
import java.io.FileReader;

/** Прямое чтение sysfs без root (для mir-readable нод). */
public class Sysfs {

    public static String readString(String path) {
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append("\n");
            return sb.toString().trim();
        } catch (Exception e) {
            return null;
        }
    }

    public static long readLong(String path, long def) {
        String s = readString(path);
        if (s == null || s.isEmpty()) return def;
        try {
            return Long.parseLong(s.trim().split("\\s+")[0]);
        } catch (Exception e) {
            return def;
        }
    }
}
