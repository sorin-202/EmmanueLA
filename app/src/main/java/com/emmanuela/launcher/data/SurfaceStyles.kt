package com.emmanuela.launcher.data

import org.json.JSONObject

val widgetIds=listOf("clock","date","battery","usage","weather","location")
data class WidgetStyle(val font:String="Inherit",val color:Long=0L,val background:Long=0L,
    val opacity:Float=.25f,val radius:Float=18f,val scale:Float=1f,val italic:Boolean?=null,val weight:String="Inherit",
    val alignment:String="Left",val borderColor:Long=0L,val borderWidth:Float=0f,
    val horizontalPadding:Float=8f,val verticalPadding:Float=8f,val letterSpacing:Float=0f,val width:Float=0f)
data class PhotoStyle(val x:Float=0f,val y:Float=0f,val zoom:Float=1f,val dim:Float=.35f,val blur:Float=0f,val fit:String="Fill")
object SurfaceStyleCodec {
    fun widgets(values:Map<String,WidgetStyle>)=JSONObject().apply{values.forEach{(id,s)->put(id,JSONObject().apply{
        put("font",s.font);put("color",s.color);put("background",s.background);put("opacity",s.opacity.toDouble());put("radius",s.radius.toDouble());put("scale",s.scale.toDouble());put("italic",s.italic?:JSONObject.NULL);put("weight",s.weight)
        put("alignment",s.alignment);put("borderColor",s.borderColor);put("borderWidth",s.borderWidth.toDouble())
        put("horizontalPadding",s.horizontalPadding.toDouble());put("verticalPadding",s.verticalPadding.toDouble());put("letterSpacing",s.letterSpacing.toDouble());put("width",s.width.toDouble())
    })}}
    fun photos(values:Map<String,PhotoStyle>)=JSONObject().apply{values.forEach{(uri,s)->put(uri,JSONObject().apply{
        put("x",s.x.toDouble());put("y",s.y.toDouble());put("zoom",s.zoom.toDouble());put("dim",s.dim.toDouble());put("blur",s.blur.toDouble());put("fit",s.fit)
    })}}
    private fun JSONObject.float(key:String,default:Float,range:ClosedFloatingPointRange<Float>)=optDouble(key,default.toDouble()).toFloat().also{require(it in range)}
    fun readWidgets(o:JSONObject):Map<String,WidgetStyle>{require(o.length()<=6);return o.keys().asSequence().associateWith{id->
        require(id in widgetIds);val s=o.getJSONObject(id)
        WidgetStyle(s.optString("font","Inherit").also{require(it in listOf("Inherit","Sans","Serif","Monospace","Custom"))},s.optLong("color"),s.optLong("background"),
            s.float("opacity",.25f,0f..1f),s.float("radius",18f,0f..40f),s.float("scale",1f,.6f..1.5f),if(s.isNull("italic"))null else s.getBoolean("italic"),
            s.optString("weight","Inherit").also{require(it in listOf("Inherit","Regular","Medium","Bold"))},
            alignment=s.optString("alignment","Left").also{require(it in listOf("Left","Center","Right"))},
            borderColor=s.optLong("borderColor",if(s.optLong("background")!=0L)0x38FFFFFFL else 0L),borderWidth=s.float("borderWidth",if(s.optLong("background")!=0L)1f else 0f,0f..6f),
            horizontalPadding=s.float("horizontalPadding",8f,0f..32f),verticalPadding=s.float("verticalPadding",8f,0f..32f),
            letterSpacing=s.float("letterSpacing",0f,-1f..8f),width=s.float("width",0f,0f..320f).also{require(it==0f||it>=80f)})
    }}
    fun readPhotos(o:JSONObject):Map<String,PhotoStyle>{require(o.length()<=50);return o.keys().asSequence().associateWith{uri->
        require(uri.startsWith("content://")&&uri.length<=4096);val s=o.getJSONObject(uri)
        PhotoStyle(s.float("x",0f,-1f..1f),s.float("y",0f,-1f..1f),s.float("zoom",1f,1f..3f),s.float("dim",.35f,0f..1f),s.float("blur",0f,0f..30f),s.optString("fit","Fill").also{require(it in listOf("Fill","Fit"))})
    }}
}
fun Preferences.styleForWidget(id:String)=ui.v2.widgetStyles[id]?:WidgetStyle(ui.widgetFont,ui.widgetColor,ui.widgetBackground,ui.experience.widgetBackgroundOpacity,ui.experience.widgetCornerRadius,ui.widgetScale,
    borderColor=if(ui.widgetBackground!=0L)0x38FFFFFFL else 0L,borderWidth=if(ui.widgetBackground!=0L)1f else 0f)
fun Preferences.styleForPhoto(uri:String?)=uri?.let{ui.v2.photoStyles[it]}?:PhotoStyle(ui.v2.cropX,ui.v2.cropY,ui.v2.cropZoom,dim,ui.wallpaperBlur,ui.experience.wallpaperFit)
