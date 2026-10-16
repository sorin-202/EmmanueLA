package com.emmanuela.launcher.data

import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** Data only: imported files cannot name resources, paths, URLs, actions or executable code. */
object WidgetStyleImport {
    const val MAX_BYTES = 32 * 1024
    const val SCHEMA = "emmanuela.widget-style"
    private val properties = setOf("font", "color", "background", "opacity", "radius", "scale", "italic", "weight",
        "alignment", "borderColor", "borderWidth", "horizontalPadding", "verticalPadding", "letterSpacing", "width")

    fun parse(bytes: ByteArray, widgetId: String): WidgetStyle {
        require(bytes.size <= MAX_BYTES) { "Style file must be 32 KiB or smaller." }
        require(widgetId in widgetIds) { "Unknown widget." }
        val raw = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
        val document = StyleJson(raw).read()
        require(document.keys().asSequence().toSet() == setOf("schema", "version", "widget", "style")) { "Expected schema, version, widget and style only." }
        require(document.get("schema") == SCHEMA) { "Unsupported style schema." }
        require(document.get("version") == 1L) { "Unsupported style version." }
        require(document.get("widget") == widgetId) { "This file is for a different widget. Open that widget's Style options to import it." }
        val style = document.optJSONObject("style") ?: error("Style must be an object.")
        require(style.keys().asSequence().all { it in properties }) { "Unknown style property. Only documented appearance properties are allowed." }
        style.keys().asSequence().toList().forEach { key ->
            val value = style.get(key)
            when (key) {
                "color", "background", "borderColor" -> {
                    require(value is String && (value == "auto" || value.matches(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")))) { "$key must be auto, #RRGGBB or #AARRGGBB." }
                    val color = value as String
                    style.put(key, if (color == "auto") 0L else color.substring(1).toLong(16).let { if (color.length == 7) it or 0xFF000000L else it })
                }
                "font", "weight", "alignment" -> require(value is String) { "$key must be text." }
                "italic" -> require(value is Boolean || value == JSONObject.NULL) { "italic must be true, false or null." }
                else -> {
                    require(value is Number && value.toDouble().isFinite()) { "$key must be a finite number." }
                    val range = when(key) {
                        "opacity" -> 0.0..1.0; "radius" -> 0.0..40.0; "scale" -> .6..1.5
                        "borderWidth" -> 0.0..6.0; "horizontalPadding", "verticalPadding" -> 0.0..32.0
                        "letterSpacing" -> -1.0..8.0; "width" -> 0.0..320.0
                        else -> error("Unsupported property.")
                    }
                    require(value.toDouble() in range) { "$key is outside its supported range." }
                }
            }
        }
        // File defaults are the documented style defaults, not the legacy background-border
        // migration used when loading an older launcher backup.
        if (!style.has("borderWidth")) style.put("borderWidth", 0)
        if (!style.has("borderColor")) style.put("borderColor", 0L)
        return try {
            SurfaceStyleCodec.readWidgets(JSONObject().put(widgetId, style)).getValue(widgetId)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("A style property is outside its supported range or choices.")
        }
    }

    // Android's JSONObject accepts JavaScript-like syntax. Validate real JSON first, including
    // duplicate keys, before turning the two permitted objects into persistence values.
    private class StyleJson(private val source: String) {
        private var index = 0
        fun read(): JSONObject {
            val result = objectValue(0)
            whitespace()
            require(index == source.length) { "Unexpected content after the style object." }
            return result
        }
        private fun objectValue(depth: Int): JSONObject {
            require(depth <= 1) { "Nested style objects are not allowed." }
            whitespace(); expect('{'); whitespace()
            val result = JSONObject()
            if (take('}')) return result
            while (true) {
                whitespace(); val key = string()
                require(!result.has(key)) { "Duplicate property: $key." }
                whitespace(); expect(':'); whitespace()
                val value: Any = when (source.getOrNull(index)) {
                    '{' -> objectValue(depth + 1)
                    '"' -> string()
                    't' -> literal("true", true)
                    'f' -> literal("false", false)
                    'n' -> literal("null", JSONObject.NULL)
                    else -> number()
                }
                result.put(key, value); whitespace()
                if (take('}')) return result
                expect(',')
            }
        }
        private fun string(): String {
            expect('"'); val text = StringBuilder()
            while (index < source.length) {
                val char = source[index++]
                if (char == '"') return text.toString()
                require(char >= ' ') { "Unescaped control character." }
                if (char != '\\') text.append(char) else {
                    require(index < source.length) { "Incomplete escape." }
                    when (val escape = source[index++]) {
                        '"', '\\', '/' -> text.append(escape)
                        'b' -> text.append('\b')
                        'f' -> text.append('\u000C')
                        'n' -> text.append('\n')
                        'r' -> text.append('\r')
                        't' -> text.append('\t')
                        'u' -> {
                            val hex = source.substring(index, (index + 4).coerceAtMost(source.length))
                            require(hex.length == 4 && hex.all { it in "0123456789abcdefABCDEF" }) { "Invalid unicode escape." }
                            text.append(hex.toInt(16).toChar()); index += 4
                        }
                        else -> error("Invalid string escape.")
                    }
                }
            }
            error("Unterminated string.")
        }
        private fun number(): Number {
            val start = index
            while (source.getOrNull(index)?.let { it in "0123456789+-.eE" } == true) index++
            val token = source.substring(start, index)
            require(token.matches(Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"))) { "Invalid JSON value." }
            return if (token.none { it in ".eE" }) token.toLongOrNull() ?: error("Number too large.")
                else token.toDouble().also { require(it.isFinite()) { "Number too large." } }
        }
        private fun literal(token: String, value: Any): Any {
            require(source.startsWith(token, index)) { "Invalid JSON value." }; index += token.length; return value
        }
        private fun whitespace() { while (source.getOrNull(index)?.let { it in " \t\r\n" } == true) index++ }
        private fun take(char: Char) = if (source.getOrNull(index) == char) { index++; true } else false
        private fun expect(char: Char) { require(take(char)) { "Invalid JSON: expected $char." } }
    }
}

/** Every preset is an independent value for one widget; applying it never changes siblings. */
fun widgetStylePresets(id: String): Map<String, WidgetStyle> {
    require(id in widgetIds)
    val detail = id in listOf("weather", "location")
    return linkedMapOf(
        "Minimal" to WidgetStyle(scale = if (detail) .85f else 1f),
        (if (id == "clock" || id == "date") "Editorial" else "Instrument") to WidgetStyle(
            font = if (id == "clock" || id == "date") "Serif" else "Monospace", weight = "Medium",
            italic = id == "date", scale = if (id == "clock") 1.15f else .9f, letterSpacing = if (detail) 0f else 1f,
            alignment = "Center", width = if (id == "clock") 280f else if (detail) 260f else 200f),
        "Soft card" to WidgetStyle(background = 0xFF101018L, color = 0xFFF6F0E8L, opacity = .75f,
            radius = 24f, horizontalPadding = 20f, verticalPadding = 12f, weight = "Medium", scale = if (detail) .85f else 1f),
        "Outlined" to WidgetStyle(borderColor = 0xFFA6C9A8L, borderWidth = 1.5f, radius = 4f,
            horizontalPadding = 16f, verticalPadding = 10f, alignment = "Right", width = if (detail) 260f else 220f,
            font = if (id in listOf("battery", "usage", "location")) "Monospace" else "Sans")
    )
}
