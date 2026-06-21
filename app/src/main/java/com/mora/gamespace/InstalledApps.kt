package com.mora.gamespace

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
)

object InstalledApps {

    /** All launchable, user-installed (non-system) apps, sorted by label. */
    fun loadUserApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val result = ArrayList<InstalledApp>()
        val packages = try { pm.getInstalledApplications(0) } catch (e: Exception) { emptyList<ApplicationInfo>() }
        for (app in packages) {
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            if (isSystem && !isUpdatedSystem) continue
            if (app.packageName == context.packageName) continue
            pm.getLaunchIntentForPackage(app.packageName) ?: continue
            val label = try { pm.getApplicationLabel(app).toString() } catch (e: Exception) { app.packageName }
            val icon = try { drawableToImageBitmap(pm.getApplicationIcon(app)) } catch (e: Exception) { continue }
            result.add(InstalledApp(app.packageName, label, icon))
        }
        result.sortBy { it.label.lowercase() }
        return result
    }

    private fun drawableToImageBitmap(drawable: android.graphics.drawable.Drawable, size: Int = 128): ImageBitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bmp.asImageBitmap()
    }
}
