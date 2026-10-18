package com.emmanuela.launcher.platform

import android.view.accessibility.AccessibilityNodeInfo
import com.emmanuela.launcher.data.FocusWindows
import java.net.URI

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
    enum class Unavailable { UNSUPPORTED_BROWSER, FOREIGN_WINDOW, MISSING_ADDRESS_FIELD, EDITING_ADDRESS, UNREADABLE_ADDRESS, AMBIGUOUS_ADDRESS, TREE_ACCESS_FAILED }
    data class Result(val address: String? = null, val unavailable: Unavailable? = null) {
        /** A visible origin does not prove that a browser exposed its current path or query. */
        val hostOnly: Boolean get() = address?.let { !BrowserAdapters.hasUrlDetails(it) } ?: false
    }

    private val pageClasses = setOf("android.webkit.WebView", "org.mozilla.geckoview.GeckoView", "org.mozilla.gecko.GeckoView")
    private data class Candidate(val address: String, val host: String, val port: Int, val details: List<String?>, val hasDetails: Boolean)

    /** Typing a URL is not navigation. Wait until the address control stops being an editor. */
    fun inspect(browser: String, rootPackage: String?, fields: List<Field>): Result {
        if (browser !in ids) return Result(unavailable = Unavailable.UNSUPPORTED_BROWSER)
        if (rootPackage != browser) return Result(unavailable = Unavailable.FOREIGN_WINDOW)
        if (fields.isEmpty()) return Result(unavailable = Unavailable.MISSING_ADDRESS_FIELD)
        if (fields.any { it.editing }) return Result(unavailable = Unavailable.EDITING_ADDRESS)
        val candidates = fields.flatMap { field ->
            listOfNotNull(field.text?.let(::webAddress)) +
                field.description?.let { descriptionAddresses(it, browser == "org.mozilla.firefox") }.orEmpty()
        }.distinct().mapNotNull(::candidate)
        if (candidates.isEmpty()) return Result(unavailable = Unavailable.UNREADABLE_ADDRESS)
        // An elided domain in text must not discard the same control's full URL in its
        // description. Equally, contradictory controls must never select an arbitrary page.
        val selected = candidates.maxWith(compareBy<Candidate> { it.hasDetails }
            .thenBy { it.address.contains("://") }.thenBy { it.address.length })
        val elidedHost = selected.host.takeIf { it.startsWith("www.") }?.removePrefix("www.")
        if (candidates.any { candidate ->
                candidate.host != selected.host && !(candidate.host == elidedHost &&
                    !candidate.hasDetails)
            } ||
            candidates.map { it.port }.filter { it >= 0 }.distinct().size > 1 ||
            candidates.filter { it.hasDetails }.map { it.details }.distinct().size > 1
        ) return Result(unavailable = Unavailable.AMBIGUOUS_ADDRESS)
        return Result(address = selected.address)
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
                try { if (isBrowserChromeNode(node)) field(node,owner) else null } finally { @Suppress("DEPRECATION") node.recycle() }
            }
        }
        if("ADDRESSBAR_URL_BOX" !in addressIds)return fields
        // Fenix's newer Compose toolbar publishes an unqualified test tag as its
        // resource ID. Accessibility view-ID queries do not find virtual nodes on
        // all Android versions. Inspect only bounded app chrome node metadata;
        // never descend into WebView page content or read arbitrary node text.
        val found=fields.toMutableList()
        var remaining=256
        fun visit(node:AccessibilityNodeInfo,depth:Int){
            if(remaining--<=0||depth>20||node.packageName?.toString()!=owner)return
            if(node.className?.toString() in pageClasses)return
            val resource=node.viewIdResourceName
            if(resource=="ADDRESSBAR_URL_BOX"||resource=="$owner:id/ADDRESSBAR_URL_BOX"){
                field(node,owner)?.let(found::add)
                return
            }
            for(index in 0 until node.childCount){
                if(remaining<=0)break
                val child=node.getChild(index)?:continue
                try { visit(child,depth+1) }finally { @Suppress("DEPRECATION") child.recycle() }
            }
        }
        visit(root,0)
        return found.distinct()
    }

    /** Qualified view-ID lookup can also traverse virtual page nodes; exclude page ancestry. */
    private fun isBrowserChromeNode(node: AccessibilityNodeInfo): Boolean {
        if (node.className?.toString() in pageClasses) return false
        var parent = node.parent
        var remaining = 32
        while (parent != null) {
            val ancestor = parent
            try {
                if (remaining-- <= 0 || ancestor.className?.toString() in pageClasses) return false
                parent = ancestor.parent
            } finally { @Suppress("DEPRECATION") ancestor.recycle() }
        }
        return true
    }

    private fun field(node:AccessibilityNodeInfo,owner:String):Field? =
        if(!node.isVisibleToUser||node.packageName?.toString()!=owner)null
        else Field(node.text?.toString()?.take(8193),node.contentDescription?.toString()?.take(8193),node.isEditable&&node.isFocused)

    private fun webAddress(value: String): String? = value.trim().takeIf {
        it.length in 1..8192 && FocusWindows.host(it) != null && it.none(Char::isWhitespace)
    }

    private fun descriptionAddresses(value: String, firefox: Boolean): List<String> {
        if (value.length > 8192) return emptyList()
        webAddress(value)?.let { return listOf(it) }
        val trimmed = value.trim()
        return trimmed.split(Regex("[\\s<>]+"))
            .map { token ->
                // Fenix formats this chrome label as "<URL>. <localized navigation hint>".
                // The separator period is not part of the navigated path. A plain URL
                // without the trailing hint has already returned above and stays intact.
                val labelAddress = if (firefox && token.endsWith('.') &&
                    trimmed.startsWith(token) && trimmed.getOrNull(token.length)?.isWhitespace() == true
                ) token.dropLast(1) else token
                var address = labelAddress
                val wrappers = mapOf('(' to ')', '[' to ']', '"' to '"', '\'' to '\'')
                while (address.length >= 2 && wrappers[address.first()] == address.last()) {
                    address = address.substring(1, address.length - 1)
                }
                address
            }.mapNotNull(::webAddress).distinct()
    }

    private fun uri(address: String) = URI(if (address.contains("://")) address else "https://$address")
    private fun hasUrlDetails(address: String): Boolean = runCatching {
        uri(address).let { it.rawPath.orEmpty() !in setOf("", "/") || it.rawQuery != null || it.rawFragment != null }
    }.getOrDefault(false)

    private fun candidate(address: String): Candidate? = runCatching {
        val parsed = uri(address)
        val host = FocusWindows.host(address) ?: return null
        fun normalized(value: String?) = value?.let { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFC) }
        Candidate(address, host, parsed.port, listOf(normalized(parsed.normalize().path.orEmpty().ifEmpty { "/" }), normalized(parsed.query), normalized(parsed.fragment)), hasUrlDetails(address))
    }.getOrNull()
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
