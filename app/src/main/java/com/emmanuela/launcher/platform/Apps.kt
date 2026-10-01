package com.emmanuela.launcher.platform

import com.emmanuela.launcher.data.LaunchableApp


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

class AppRepository(private val context: Context) {
    private val pm = context.packageManager
    // Bounded ~2.4 MB cache; icons are only requested by visible grid/picker cells.
    private val icons = LruCache<String, Bitmap>(64)
    @Suppress("DEPRECATION")
    suspend fun load(): List<LaunchableApp> = withContext(Dispatchers.IO) {
        val collator = Collator.getInstance()
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName != context.packageName && it.activityInfo.exported }
            .map {
                val info = it.activityInfo
                LaunchableApp(ComponentName(info.packageName, info.name).flattenToString(),
                    it.loadLabel(pm).toString(), info.packageName)
            }.distinctBy { it.id }.sortedWith { a, b ->
                collator.compare(a.label, b.label).takeIf { it != 0 } ?: a.id.compareTo(b.id)
            }
    }
    fun invalidateIcons() = icons.evictAll()
    suspend fun icon(id: String): Bitmap? = withContext(Dispatchers.IO) {
        icons.get(id) ?: try {
            pm.getActivityIcon(ComponentName.unflattenFromString(id)!!).toBitmap(96, 96)
                .also { icons.put(id, it) }
        } catch (_: PackageManager.NameNotFoundException) { null }
          catch (_: SecurityException) { null }
    }
    fun launch(app: LaunchableApp): Boolean = try {
        context.startActivity(Intent.makeMainActivity(ComponentName.unflattenFromString(app.id))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
        true
    } catch (_: android.content.ActivityNotFoundException) { false }
      catch (_: SecurityException) { false }
}
