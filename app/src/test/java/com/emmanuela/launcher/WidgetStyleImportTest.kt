package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class WidgetStyleImportTest {
    private fun json(style:String,widget:String="clock")="""{"schema":"emmanuela.widget-style","version":1,"widget":"$widget","style":$style}"""
    private fun parse(style:String)=WidgetStyleImport.parse(json(style).toByteArray(),"clock")
    private fun rejects(raw:String){
        try { WidgetStyleImport.parse(raw.toByteArray(),"clock");fail("Accepted invalid style: $raw") }
        catch (_:IllegalArgumentException) {} catch (_:IllegalStateException) {}
    }
    @Test fun importsEveryWhitelistedPropertyAndPersistsIndependently(){
        val style=parse("""{"font":"Serif","color":"#F6F0E8","background":"#CC101018","opacity":0.7,"radius":12,"scale":1.2,"italic":true,"weight":"Medium","alignment":"Right","borderColor":"#A6C9A8","borderWidth":2,"horizontalPadding":14,"verticalPadding":10,"letterSpacing":1.5,"width":260}""")
        assertEquals(0xFFF6F0E8L,style.color);assertEquals(0xCC101018L,style.background)
        assertEquals("Right",style.alignment);assertEquals(14f,style.horizontalPadding,0f)
        val sibling=WidgetStyle(font="Monospace",scale=.8f)
        val data=LauncherData(settings=Preferences(ui=UiPreferences(v2=V2Preferences(widgetStyles=mapOf("clock" to style,"battery" to sibling)))))
        val restored=ConfigurationCodec.decode(ConfigurationCodec.encode(data))
        assertEquals(style,restored.settings.styleForWidget("clock"));assertEquals(sibling,restored.settings.styleForWidget("battery"))
    }
    @Test fun optionalValuesUseDefaultsAndExplicitNullInheritsItalic(){
        assertEquals(WidgetStyle(),parse("{}"))
        assertNull(parse("""{"italic":null,"color":"auto"}""").italic)
    }
    @Test fun rejectsCodePathsUrlsUnknownPropertiesAndNestedObjects(){
        listOf("""{"script":"alert(1)"}""","""{"font":"file:///tmp/font"}""","""{"background":"https://example.org/image.png"}""",
            """{"onTap":"launch"}""","""{"color":{"red":1}}""","""{"style":{}}""").forEach{rejects(json(it))}
    }
    @Test fun rejectsWrongSchemaVersionWidgetAndExtraDocumentProperties(){
        rejects(json("{}").replace("emmanuela.widget-style","other"));rejects(json("{}").replace("\"version\":1","\"version\":2"))
        rejects(json("{}","battery"));rejects(json("{}").dropLast(1)+",\"script\":\"run\"}")
        rejects(json("{}").replace("\"version\":1","\"version\":\"1\""))
    }
    @Test fun rejectsWrongTypesNonFiniteAndOutOfBounds(){
        listOf("""{"scale":"1"}""","""{"scale":1.500000001}""","""{"scale":0.5}""","""{"opacity":2}""","""{"radius":41}""",
            """{"borderWidth":-1}""","""{"width":321}""","""{"letterSpacing":9}""","""{"horizontalPadding":33}""",
            """{"italic":1}""","""{"font":"Comic"}""","""{"weight":"Heavy"}""","""{"alignment":"Diagonal"}""",
            """{"color":4294967295}""","""{"color":"red"}""","""{"opacity":null}""","""{"scale":1e999}""").forEach{rejects(json(it))}
    }
    @Test fun rejectsJavaScriptSyntaxDuplicatesTrailingDataAndArrays(){
        listOf("{font:'Serif'}","""{"scale":1,}""","""{"scale":1,"scale":2}""","[]","""{/* hi */}""","""{"opacity":NaN}""").forEach{rejects(json(it))}
        rejects(json("{}")+"{}");rejects(json("{}").replace("\"version\":1","\"version\":01"))
    }
    @Test fun rejectsOversizedFilesAndMalformedUtf8(){
        try {WidgetStyleImport.parse(ByteArray(WidgetStyleImport.MAX_BYTES+1),"clock");fail("Oversized accepted")}catch(_:IllegalArgumentException){}
        try {WidgetStyleImport.parse(byteArrayOf(0xC3.toByte(),0x28),"clock");fail("Malformed UTF8 accepted")}catch(_:java.nio.charset.CharacterCodingException){}
    }
    @Test fun everyBuiltInHasDistinctSafePresetsAndLegacyStylesMigrate(){
        widgetIds.forEach{id->val presets=widgetStylePresets(id)
            assertTrue(presets.size>=3);assertEquals(presets.size,presets.values.distinct().size)
            presets.values.forEach{style->assertEquals(style,SurfaceStyleCodec.readWidgets(SurfaceStyleCodec.widgets(mapOf(id to style))).getValue(id))}
        }
        val legacy=SurfaceStyleCodec.readWidgets(JSONObject("""{"clock":{"font":"Serif","background":4278190080}}""")).getValue("clock")
        assertEquals("Serif",legacy.font);assertEquals(8f,legacy.horizontalPadding,0f);assertEquals(1f,legacy.borderWidth,0f);assertEquals("Left",legacy.alignment)
    }
}
