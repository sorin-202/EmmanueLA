package com.emmanuela.launcher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Release only grants owned by a discarded folder import, retaining shared document references. */
object FolderImageGrants {
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun referenced(state: LauncherData): Set<String> =
        (state.folders.map { it.imageUri } + state.settings.wallpapers + state.settings.ui.v2.wallpaperAlbum).filter { it.isNotEmpty() }.toSet()

    fun cleanup(context: Context, candidates: Collection<String>) {
        val documents = candidates.filter { it.isNotEmpty() }.toSet()
        if (documents.isEmpty()) return
        val app = context.applicationContext
        cleanupScope.launch {
            runCatching {
                val retained = referenced(ConfigurationRepository(app).data.first())
                documents.filterNot { it in retained }.forEach { value ->
                    runCatching { app.contentResolver.releasePersistableUriPermission(Uri.parse(value), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                }
            }
        }
    }
}
