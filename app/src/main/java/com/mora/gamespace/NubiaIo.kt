package com.mora.gamespace

import android.content.Context
import android.provider.Settings

/**
 * Direct, privileged access to the Nubia/RedMagic game-tuning values that live in
 * Settings.Global. NO root / su is used. GameSpace is a privileged system app that
 * holds WRITE_SECURE_SETTINGS (granted by the SELinux policy / sepolicy.rule), exactly
 * like the stock cn.nubia.gamelauncher. Reads always work; writes work once privileged.
 */
object NubiaIo {

    fun getGlobal(context: Context, key: String): String {
        val r = try {
            Settings.Global.getString(context.contentResolver, key)
        } catch (e: Exception) {
            null
        }
        if (r == null) return ""
        val t = r.trim()
        return if (t == "null") "" else t
    }

    fun putGlobal(context: Context, key: String, value: String): Boolean {
        return try {
            Settings.Global.putString(context.contentResolver, key, value)
        } catch (e: Exception) {
            false
        }
    }

    fun getGlobalInt(context: Context, key: String, def: Int): Int {
        val r = getGlobal(context, key)
        return try {
            if (r.isEmpty()) def else r.replace(Regex("[^0-9-]"), "").toInt()
        } catch (e: Exception) {
            def
        }
    }
}
