package com.emmanuela.launcher.platform

import android.view.accessibility.AccessibilityNodeInfo
import com.emmanuela.launcher.data.FocusWindows

/** Only address controls belonging to a known browser are read; never search page text. */
object BrowserAdapters {
    val ids = mapOf(
        "com.android.chrome" to listOf("url_bar"),
        "com.chrome.beta" to listOf("url_bar"),
        "com.brave.browser" to listOf("url_bar"),
        "com.microsoft.emmx" to listOf("url_bar"),
        "org.mozilla.firefox" to listOf("mozac_browser_toolbar_url_view", "mozac_browser_toolbar_edit_url_view", "url_bar", "ADDRESSBAR_URL_BOX"),
        "com.sec.android.app.sbrowser" to listOf("location_bar_edit_text", "location_bar_text_view")
    )

    data class Field(val text: String? = null, val description: String? = null, val editing: Boolean = false)
    enum class Unavailable { UNSUPPORTED_BROWSER, FOREIGN_WINDOW, MISSING_ADDRESS_FIELD, EDITING_ADDRESS, UNREADABLE_ADDRESS, TREE_ACCESS_FAILED }
    data class Result(val address: String? = null, val unavailable: Unavailable? = null)

    /** Typing a URL is not navigation. Wait until the address control stops being an editor. */
    fun inspect(browser: String, rootPackage: String?, fields: List<Field>): Result {
        if (browser !in ids) return Result(unavailable = Unavailable.UNSUPPORTED_BROWSER)
        if (rootPackage != browser) return Result(unavailable = Unavailable.FOREIGN_WINDOW)
        if (fields.isEmpty()) return Result(unavailable = Unavailable.MISSING_ADDRESS_FIELD)
        if (fields.any { it.editing }) return Result(unavailable = Unavailable.EDITING_ADDRESS)
        fields.forEach { field ->
            field.text?.let(::webAddress)?.let { return Result(address = it) }
            // Firefox exposes the display address in contentDescription in some versions.
            // Accept a complete address or a single address token inside that control's label.
            field.description?.let(::descriptionAddress)?.let { return Result(address = it) }
        }
        return Result(unavailable = Unavailable.UNREADABLE_ADDRESS)
    }

    fun address(browser: String, rootPackage: String?, values: List<String>): String? =
        inspect(browser, rootPackage, values.map { Field(text = it) }).address

    fun read(root: AccessibilityNodeInfo?, browser: String): Result {
        val owner = root?.packageName?.toString()
        if (browser !in ids || owner != browser) return inspect(browser, owner, emptyList())
        return inspect(browser, owner, readFields(root, browser, ids.getValue(browser)))
    }

    /** Kept separate so Android tests can verify real accessibility controls with fixture IDs. */
    fun readFields(root: AccessibilityNodeInfo?, owner: String, addressIds: List<String>): List<Field> {
        if (root?.packageName?.toString() != owner) return emptyList()
        val fields=addressIds.flatMap { id ->
            root.findAccessibilityNodeInfosByViewId("$owner:id/$id").orEmpty().mapNotNull { node ->
                try { field(node,owner) } finally { @Suppress("DEPRECATION") node.recycle() }
            }
        }
        if(fields.isNotEmpty()||"ADDRESSBAR_URL_BOX" !in addressIds)return fields
        // Fenix's newer Compose toolbar publishes an unqualified test tag as its
        // resource ID. Accessibility view-ID queries do not find virtual nodes on
        // all Android versions. Inspect only bounded app chrome node metadata;
        // never descend into WebView page content or read arbitrary node text.
        val found=mutableListOf<Field>()
        var remaining=256
        fun visit(node:AccessibilityNodeInfo,depth:Int){
            if(remaining--<=0||depth>20||node.packageName?.toString()!=owner)return
            val resource=node.viewIdResourceName
            if(resource=="ADDRESSBAR_URL_BOX"||resource=="$owner:id/ADDRESSBAR_URL_BOX"){
                field(node,owner)?.let(found::add)
                return
            }
            if(node.className?.toString() in setOf("android.webkit.WebView","org.mozilla.geckoview.GeckoView","org.mozilla.gecko.GeckoView"))return
            for(index in 0 until node.childCount){
                if(remaining<=0)break
                val child=node.getChild(index)?:continue
                try { visit(child,depth+1) }finally { @Suppress("DEPRECATION") child.recycle() }
            }
        }
        visit(root,0)
        return found
    }

    private fun field(node:AccessibilityNodeInfo,owner:String):Field? =
        if(!node.isVisibleToUser||node.packageName?.toString()!=owner)null
        else Field(node.text?.toString()?.take(8193),node.contentDescription?.toString()?.take(8193),node.isEditable&&node.isFocused)

    private fun webAddress(value: String): String? = value.trim().takeIf {
        it.length in 1..8192 && FocusWindows.host(it) != null && it.none(Char::isWhitespace)
    }

    private fun descriptionAddress(value: String): String? {
        if (value.length > 8192) return null
        webAddress(value)?.let { return it }
        val candidates = value.split(Regex("[\\s,;\\[\\]<>]+"))
            .map { it.trim('(', ')', '"', '\'') }.mapNotNull(::webAddress).distinct()
        // Ambiguous labels must never produce a false positive.
        return candidates.singleOrNull()
    }
}

enum class EnforcementOutcome { IDLE, ALLOWED, OVERLAY_SHOWN, HOME_REQUESTED, FAILED }
data class EnforcementStatus(
    val connected: Boolean = false,
    val browserPackage: String? = null,
    val addressReadable: Boolean? = null,
    val issue: String? = null,
    val foregroundPackage: String? = null,
    val outcome: EnforcementOutcome = EnforcementOutcome.IDLE,
    val reason: String? = null
)
