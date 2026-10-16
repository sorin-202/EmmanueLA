package com.emmanuela.launcher.ui.settings

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.ui.appearance.launcherFont
import com.emmanuela.launcher.ui.appearance.weightOf
import com.emmanuela.launcher.ui.appearance.styledTypography
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.home.ClockFace
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun widgetTextStyle(data:LauncherData,id:String,scaleFactor:Float=1f):TextStyle {
    val p=data.settings;val e=p.ui.experience;val s=p.styleForWidget(id)
    val base=if(id=="clock")MaterialTheme.typography.displayLarge else MaterialTheme.typography.bodyLarge
    val font=if(s.font=="Inherit")if(e.homeStyleOverride&&e.homeFont!="Inherit")e.homeFont else p.font else s.font
    val italic=s.italic?:if(e.homeStyleOverride)e.homeItalic else p.ui.italic
    val weight=if(s.weight=="Inherit")weightOf(if(e.homeStyleOverride&&e.homeTextWeight!="Inherit")e.homeTextWeight else if(p.bold)"Bold" else e.textWeight)else weightOf(s.weight)
    return base.copy(fontFamily=launcherFont(font,p.customFont,base.fontFamily,italic,weight),fontStyle=if(italic)FontStyle.Italic else FontStyle.Normal,fontWeight=weight,fontSize=base.fontSize*s.scale*scaleFactor,lineHeight=base.lineHeight*s.scale*scaleFactor,fontSynthesis=FontSynthesis.All,
        textAlign=when(s.alignment){"Center"->TextAlign.Center;"Right"->TextAlign.Right;else->TextAlign.Left},letterSpacing=s.letterSpacing.sp)
}
fun Modifier.widgetBackground(s:WidgetStyle):Modifier {
    val shape=RoundedCornerShape(s.radius.dp)
    return background(if(s.background==0L)Color.Transparent else Color(s.background).let{it.copy(alpha=it.alpha*s.opacity)},shape)
        .then(if(s.borderWidth>0f)Modifier.border(s.borderWidth.dp,if(s.borderColor==0L)Color.White.copy(alpha=.5f)else Color(s.borderColor),shape)else Modifier)
}
fun Modifier.widgetFrame(s:WidgetStyle)=then(if(s.width>0f)Modifier.width(s.width.dp)else Modifier)
fun Modifier.widgetPadding(s:WidgetStyle)=padding(horizontal=s.horizontalPadding.dp,vertical=s.verticalPadding.dp)

@Composable
private fun WidgetStylePreview(id:String,data:LauncherData,style:WidgetStyle){
    val preview=data.copy(settings=data.settings.copy(ui=data.settings.ui.copy(v2=data.settings.ui.v2.copy(widgetStyles=data.settings.ui.v2.widgetStyles+(id to style)))))
    val p=preview.settings
    // HomeScreen applies its typography override before calling widgetTextStyle. Preview
    // must use that same typography helper so both font size and line height agree.
    val e=p.ui.experience;val inherited=MaterialTheme.typography
    val family=launcherFont(if(e.homeFont=="Inherit")p.font else e.homeFont,p.customFont,inherited.bodyLarge.fontFamily,
        if(e.homeStyleOverride)e.homeItalic else p.ui.italic,if(e.homeTextWeight=="Inherit")inherited.bodyLarge.fontWeight?:FontWeight.Normal else weightOf(e.homeTextWeight))
    val typography=remember(inherited,family,e.homeStyleOverride,e.homeTextWeight,e.homeItalic,e.homeTextScale){
        if(!e.homeStyleOverride)inherited else styledTypography(inherited,family,if(e.homeTextWeight=="Inherit")null else weightOf(e.homeTextWeight),e.homeItalic,e.homeTextScale)
    }
    val inheritedColor=if(p.wallpapers.isNotEmpty())Color.White else MaterialTheme.colorScheme.onBackground
    Box(Modifier.fillMaxWidth().heightIn(min=180.dp).background(if(p.wallpapers.isNotEmpty())Color(0xFF202028)else MaterialTheme.colorScheme.background,RoundedCornerShape(16.dp)).border(1.dp,MaterialTheme.colorScheme.outline,RoundedCornerShape(16.dp)).clipToBounds(),contentAlignment=Alignment.Center){
        Text(localized("Preview"),Modifier.align(Alignment.TopStart).padding(12.dp),style=MaterialTheme.typography.labelSmall,color=inheritedColor)
        MaterialTheme(typography=typography){
            val textStyle=widgetTextStyle(preview,id,p.ui.experience.widgetScales[id]?:1f)
            val color=if(style.color==0L)inheritedColor else Color(style.color)
            val modifier=Modifier.padding(top=32.dp,bottom=12.dp).semantics{contentDescription="${widgetName(id)} style preview"}.widgetFrame(style).widgetBackground(style).widgetPadding(style)
            if(id=="clock")ClockFace(1_759_431_660_000L,p.ui.v2,color,textStyle,modifier)
            else Text(when(id){"date"->"Thu, 1 Oct";"battery"->"82%";"usage"->"1h 23m";"weather"->"22°C · Clear";else->"44.427, 26.103"},style=textStyle,color=color,modifier=modifier)
        }
    }
}

@Composable
fun WidgetStyleControls(id:String,data:LauncherData,model:LauncherViewModel){
    val p=data.settings;val s=p.styleForWidget(id)
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var imported by remember(id){mutableStateOf<WidgetStyle?>(null)}
    var importError by remember(id){mutableStateOf<String?>(null)}
    var importing by remember(id){mutableStateOf(false)}
    fun save(change:(WidgetStyle)->WidgetStyle){model.settings{current->current.copy(ui=current.ui.copy(v2=current.ui.v2.copy(widgetStyles=current.ui.v2.widgetStyles+(id to change(current.styleForWidget(id))))))}}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)scope.launch {
            importing=true;importError=null
            try {
                imported=withContext(Dispatchers.IO){
                    val bytes=context.contentResolver.openInputStream(uri)?.use{input->
                        val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(4096)
                        while(true){val count=input.read(buffer);if(count<0)break
                            require(output.size()+count<=WidgetStyleImport.MAX_BYTES){"Style file must be 32 KiB or smaller."};output.write(buffer,0,count)}
                        output.toByteArray()
                    }?:error("Cannot open the selected style file.")
                    WidgetStyleImport.parse(bytes,id)
                }
            }catch(error:Exception){if(error is CancellationException)throw error;importError=when(error){
                is java.nio.charset.CharacterCodingException->"Style file must contain valid UTF-8 JSON."
                is SecurityException->"The document provider did not allow this file to be read. Please select it again."
                is java.io.IOException->"The style file could not be read. Please select an available local file."
                else->error.message?:"Invalid style file. Choose a documented EmmanueLA widget style JSON file."
            }.take(240)}finally{importing=false}
        }
    }
    WidgetStylePreview(id,data,s)
    val presets=remember(id){widgetStylePresets(id)}
    ChoiceRow("Preset",presets.entries.firstOrNull{it.value==s}?.key?:"Custom",presets.keys.toList()){name->save{presets.getValue(name)}}
    ChoiceRow("Font",s.font,listOf("Inherit","Sans","Serif","Monospace")+if(p.customFont.isNotEmpty())listOf("Custom")else emptyList()){value->save{it.copy(font=value)}}
    ChoiceRow("Color",uiColors.entries.firstOrNull{it.value==s.color}?.key?:"Custom",uiColors.keys.toList()){value->save{it.copy(color=uiColors.getValue(value))}}
    ChoiceRow("Background",uiColors.entries.firstOrNull{it.value==s.background}?.key?:"Custom",uiColors.keys.toList()){value->save{it.copy(background=uiColors.getValue(value))}}
    SliderRow("Background opacity",s.opacity,0f..1f){n->save{it.copy(opacity=n)}}
    SliderRow("Rounded corners",s.radius,0f..40f){n->save{it.copy(radius=n)}}
    val borderColors=mapOf("Auto" to 0L)+uiColors.filterValues{it!=0L}
    ChoiceRow("Border color",borderColors.entries.firstOrNull{it.value==s.borderColor}?.key?:"Custom",borderColors.keys.toList()){value->save{it.copy(borderColor=borderColors.getValue(value))}}
    SliderRow("Border width",s.borderWidth,0f..6f){n->save{it.copy(borderWidth=n)}}
    SliderRow("Text size",s.scale,.6f..1.5f){n->save{it.copy(scale=n)}}
    ChoiceRow("Italic",when(s.italic){null->"Inherit";true->"On";false->"Off"},listOf("Inherit","On","Off")){value->save{it.copy(italic=when(value){"On"->true;"Off"->false;else->null})}}
    ChoiceRow("Weight",s.weight,listOf("Inherit","Regular","Medium","Bold")){value->save{it.copy(weight=value)}}
    ChoiceRow("Text alignment",s.alignment,listOf("Left","Center","Right")){value->save{it.copy(alignment=value,width=if(it.width==0f)240f else it.width)}}
    ToggleRow("Fixed width",s.width>0f){yes->save{it.copy(width=if(yes)240f else 0f)}}
    if(s.width>0f)SliderRow("Width",s.width,80f..320f){n->save{it.copy(width=n)}}
    SliderRow("Horizontal padding",s.horizontalPadding,0f..32f){n->save{it.copy(horizontalPadding=n)}}
    SliderRow("Vertical padding",s.verticalPadding,0f..32f){n->save{it.copy(verticalPadding=n)}}
    SliderRow("Letter spacing",s.letterSpacing,-1f..8f){n->save{it.copy(letterSpacing=n)}}
    if(id=="clock"&&p.ui.v2.clockFormat=="Hands")Text("Typography and text alignment apply to digital clock formats. Colors, background, border and spacing also apply to Hands.",style=MaterialTheme.typography.bodySmall)
    SectionLabel("Import style")
    Text("Choose a local EmmanueLA widget style JSON file. Only validated appearance values are read. Review the preview before applying.",style=MaterialTheme.typography.bodySmall)
    TextButton(enabled=!importing,onClick={picker.launch(arrayOf("application/json","text/plain","application/octet-stream"))}){Text(if(importing)"Reading style…"else"Import JSON style")}
    importError?.let{Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
    TextButton(onClick={model.v2Settings{it.copy(widgetStyles=it.widgetStyles-id)}}){Text(localized("Reset"))}
    imported?.let{candidate->AlertDialog(onDismissRequest={imported=null},title={Text("Import ${widgetName(id)} style")},text={Column{WidgetStylePreview(id,data,candidate);Text("Apply this appearance to ${widgetName(id)} only?",Modifier.padding(top=12.dp))}},
        confirmButton={TextButton(onClick={save{candidate};imported=null}){Text("Apply style")}},dismissButton={TextButton(onClick={imported=null}){Text("Cancel")}})}
}
