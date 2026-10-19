package com.emmanuela.launcher.platform

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.emmanuela.launcher.data.WebSearchEngine

object WebSearch {
    /** Respect the configured Android search handler; fall back to the default HTTPS browser. */
    fun intent(context: Context, query: String, engine: WebSearchEngine = WebSearchEngine.ANDROID_DEFAULT): Intent {
        if (engine != WebSearchEngine.ANDROID_DEFAULT) return browserIntent(query, engine)
        val search = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
        val resolved = context.packageManager.resolveActivity(search, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolved != null && resolved.activityInfo.packageName != "android") return search
        return browserIntent(query)
    }

    fun browserIntent(query: String, engine: WebSearchEngine = WebSearchEngine.DUCKDUCKGO): Intent = Intent(Intent.ACTION_VIEW,
        Uri.Builder().scheme("https").authority(engine.host).apply {
            if (engine == WebSearchEngine.GOOGLE || engine == WebSearchEngine.BING) path("search")
        }.appendQueryParameter(engine.queryParameter, query).build()
    ).addCategory(Intent.CATEGORY_BROWSABLE)
}
