package com.emmanuela.launcher.platform

import com.emmanuela.launcher.data.FocusWindows

/** Address field hints, not a guarantee that a browser/version exposes every URL. */
object BrowserAdapters {
    val ids=mapOf(
        "com.android.chrome" to listOf("url_bar"),
        "com.chrome.beta" to listOf("url_bar"),
        "com.brave.browser" to listOf("url_bar"),
        "com.microsoft.emmx" to listOf("url_bar"),
        "org.mozilla.firefox" to listOf("mozac_browser_toolbar_url_view","url_bar"),
        "com.sec.android.app.sbrowser" to listOf("location_bar_edit_text","location_bar_text_view")
    )
    fun address(browser:String,rootPackage:String?,values:List<String>):String? {
        if(browser !in ids||rootPackage!=browser)return null
        return values.firstOrNull{value->
            if(value.length>8192||FocusWindows.host(value)==null)false else {
                val raw=value.trim()
                val uri=try{java.net.URI(if(raw.contains("://"))raw else "https://$raw")}catch(_:java.net.URISyntaxException){null}
                uri!=null&&uri.scheme.lowercase() in setOf("http","https")&&uri.rawUserInfo==null
            }
        }?.trim()
    }
}

data class EnforcementStatus(val connected:Boolean=false,val browserPackage:String?=null,
    val addressReadable:Boolean?=null,val issue:String?=null)
