package com.emmanuela.launcher.ui.settings

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.ui.appearance.launcherFont
import com.emmanuela.launcher.ui.appearance.weightOf
import com.emmanuela.launcher.ui.components.localized
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.dp

@Composable
fun widgetTextStyle(data:LauncherData,id:String,scaleFactor:Float=1f):TextStyle {
    val p=data.settings;val e=p.ui.experience;val s=p.styleForWidget(id)
    val base=if(id=="clock")MaterialTheme.typography.displayLarge else MaterialTheme.typography.bodyLarge
    val font=if(s.font=="Inherit")if(e.homeStyleOverride&&e.homeFont!="Inherit")e.homeFont else p.font else s.font
    val italic=s.italic?:if(e.homeStyleOverride)e.homeItalic else p.ui.italic
    val weight=if(s.weight=="Inherit")weightOf(if(e.homeStyleOverride&&e.homeTextWeight!="Inherit")e.homeTextWeight else if(p.bold)"Bold" else e.textWeight)else weightOf(s.weight)
    return base.copy(fontFamily=launcherFont(font,p.customFont,base.fontFamily,italic,weight),fontStyle=if(italic)FontStyle.Italic else FontStyle.Normal,fontWeight=weight,fontSize=base.fontSize*s.scale*scaleFactor,fontSynthesis=FontSynthesis.All)
}
fun Modifier.widgetBackground(s:WidgetStyle)=background(if(s.background==0L)Color.Transparent else Color(s.background).copy(alpha=s.opacity),RoundedCornerShape(s.radius.dp))
    .then(if(s.background==0L)Modifier else Modifier.border(1.dp,Color.White.copy(alpha=.22f),RoundedCornerShape(s.radius.dp)))
@Composable
fun WidgetStyleControls(id:String,data:LauncherData,model:LauncherViewModel){
    val p=data.settings;val s=p.styleForWidget(id)
    fun save(change:(WidgetStyle)->WidgetStyle){model.settings{current->current.copy(ui=current.ui.copy(v2=current.ui.v2.copy(widgetStyles=current.ui.v2.widgetStyles+(id to change(current.styleForWidget(id))))))}}
    Box(Modifier.fillMaxWidth().height(200.dp).border(1.dp,MaterialTheme.colorScheme.outline,RoundedCornerShape(16.dp)).clipToBounds(),contentAlignment=Alignment.Center){
        Text(localized("Preview"),Modifier.align(Alignment.TopStart).padding(12.dp),style=MaterialTheme.typography.labelSmall)
        Text(when(id){"clock"->"19:01";"date"->"Thu, 1 Oct";"battery"->"82%";"usage"->"1h 23m";"weather"->"22°C · Clear";else->"44.427, 26.103"},
            style=widgetTextStyle(data,id,(p.ui.experience.widgetScales[id]?:1f)*(if(p.ui.experience.homeStyleOverride)p.ui.experience.homeTextScale else 1f)),color=if(s.color==0L)MaterialTheme.colorScheme.onSurface else Color(s.color),modifier=Modifier.widgetBackground(s).padding(8.dp))
    }
    ChoiceRow("Font",s.font,listOf("Inherit","Sans","Serif","Monospace")+if(p.customFont.isNotEmpty())listOf("Custom")else emptyList()){value->save{it.copy(font=value)}}
    ChoiceRow("Color",uiColors.entries.firstOrNull{it.value==s.color}?.key?:"Custom",uiColors.keys.toList()){value->save{it.copy(color=uiColors.getValue(value))}}
    ChoiceRow("Background",uiColors.entries.firstOrNull{it.value==s.background}?.key?:"Custom",uiColors.keys.toList()){value->save{it.copy(background=uiColors.getValue(value))}}
    SliderRow("Background opacity",s.opacity,0f..1f){n->save{it.copy(opacity=n)}}
    SliderRow("Rounded corners",s.radius,0f..40f){n->save{it.copy(radius=n)}}
    SliderRow("Scale",s.scale,.6f..1.5f){n->save{it.copy(scale=n)}}
    ChoiceRow("Italic",when(s.italic){null->"Inherit";true->"On";false->"Off"},listOf("Inherit","On","Off")){value->save{it.copy(italic=when(value){"On"->true;"Off"->false;else->null})}}
    ChoiceRow("Weight",s.weight,listOf("Inherit","Regular","Medium","Bold")){value->save{it.copy(weight=value)}}
    TextButton(onClick={model.v2Settings{it.copy(widgetStyles=it.widgetStyles-id)}}){Text(localized("Reset"))}
}
