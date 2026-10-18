package com.emmanuela.launcher.data

import java.net.URI
import java.text.Normalizer

/** Declarative domain/path targets. Queries remain the responsibility of URL keywords. */
object FocusWebsiteRules {
    fun hasWebTargets(group: FocusGroup): Boolean =
        group.websites.isNotEmpty() || group.keywords.isNotEmpty()

    fun needsUrlDetails(group: FocusGroup): Boolean = group.keywords.isNotEmpty() ||
        group.websites.any { input -> site(input)?.let { uri(it).rawPath.orEmpty().isNotEmpty() } == true }

    fun site(input: String): String? = runCatching {
        val value = input.trim()
        require(value.length in 1..512)
        val host = FocusWindows.host(value) ?: return null
        val uri = uri(value)
        require(uri.rawQuery == null && uri.rawFragment == null)
        val path = uri.normalize().rawPath.orEmpty().trimEnd('/')
        (host + path).takeIf { it.length <= 512 }
    }.getOrNull()

    fun keyword(input: String): String? = input.trim().takeIf {
        it.length in 1..512 && it.none(Char::isISOControl)
    }

    fun configurationError(group: FocusGroup, supportedBrowsers: Set<String>): String? = when {
        group.websites.size > 200 || group.keywords.size > 200 -> "Use at most 200 websites and 200 URL keywords."
        group.websites.any { site(it) == null } ->
            "Enter a valid website or URL path without a query or fragment. Use URL keywords for query matching."
        group.keywords.any { keyword(it) == null } -> "Enter a nonempty URL keyword without control characters."
        hasWebTargets(group) && group.browsers.none { it in supportedBrowsers } ->
            "Choose at least one supported browser for website and URL keyword rules."
        else -> null
    }

    /** Call only after validation, so invalid targets cannot silently disappear. */
    fun normalized(group: FocusGroup): FocusGroup = group.copy(
        websites = group.websites.map { requireNotNull(site(it)) }.toSet(),
        keywords = group.keywords.map { requireNotNull(keyword(it)) }.toSet()
    )

    fun matchesAnySite(sites: Set<String>, address: String): Boolean {
        if (sites.isEmpty()) return false
        val actualHost = FocusWindows.host(address) ?: return false
        val actualPath = runCatching { path(uri(address)) }.getOrNull() ?: return false
        return sites.any { input ->
            val target = site(input) ?: return@any false
            val targetHost = FocusWindows.host(target) ?: return@any false
            if (actualHost != targetHost && !actualHost.endsWith(".$targetHost")) return@any false
            val targetPath = runCatching { path(uri(target)) }.getOrNull() ?: return@any false
            targetPath.isEmpty() || actualPath == targetPath || actualPath.startsWith("$targetPath/")
        }
    }

    private fun uri(input: String): URI = URI(if (input.contains("://")) input else "https://$input")
    private fun path(uri: URI): String =
        Normalizer.normalize(uri.normalize().path.orEmpty(), Normalizer.Form.NFC).trimEnd('/')
}
