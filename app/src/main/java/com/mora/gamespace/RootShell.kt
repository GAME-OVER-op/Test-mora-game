package com.mora.gamespace

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * Minimal root helper. Runs commands via `su`. Writing Settings.Global needs root
 * (or WRITE_SECURE_SETTINGS granted through the SELinux/permission script).
 * Ported from the test-mora reference (RootShell.java).
 */
object RootShell {

    fun runOne(command: String): String {
        val out = StringBuilder()
        var p: Process? = null
        try {
            p = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(p.outputStream)
            os.writeBytes(command + "\n")
            os.writeBytes("exit\n")
            os.flush()
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            var line: String? = reader.readLine()
            while (line != null) {
                out.append(line).append('\n')
                line = reader.readLine()
            }
            p.waitFor()
        } catch (e: Exception) {
            return "ERROR: " + e.message
        } finally {
            p?.destroy()
        }
        return out.toString()
    }

    fun isRootAvailable(): Boolean = runOne("id").contains("uid=0")

    /** Reads a Settings.Global value as root. Returns "" when unset. */
    fun getGlobal(key: String): String {
        var r = runOne("settings get global " + key)
        r = r.trim()
        if (r.isEmpty() || r == "null" || r.startsWith("ERROR")) return ""
        return r
    }

    /** Writes a raw Settings.Global value as root (single-quoted to protect & + ,). */
    fun putGlobal(key: String, value: String): String {
        val safe = value.replace("'", "'\\''")
        return runOne("settings put global " + key + " '" + safe + "'")
    }

    fun getGlobalInt(key: String, def: Int): Int {
        val r = getGlobal(key)
        return try {
            if (r.isEmpty()) def else r.replace(Regex("[^0-9-]"), "").toInt()
        } catch (e: Exception) {
            def
        }
    }
}
