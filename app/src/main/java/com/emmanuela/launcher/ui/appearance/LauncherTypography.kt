package com.emmanuela.launcher.ui.appearance

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun launcherFont(name:String,stamp:String,inherited:FontFamily?=FontFamily.SansSerif,italic:Boolean=false,weight:FontWeight=FontWeight.Normal):FontFamily {
    val context=LocalContext.current
    val custom by produceState<Typeface?>(null,stamp,name){
        if(name=="Custom")value=com.emmanuela.launcher.platform.TypefaceCache.load(context,stamp)
    }
    return remember(name,custom,inherited,italic,weight){
        if(name=="Inherit")inherited?:FontFamily.SansSerif else {
            val base=custom?.takeIf{name=="Custom"}?:Typeface.create(when(name){"Serif"->"serif";"Monospace"->"monospace";else->"sans-serif"},Typeface.NORMAL)
            val native=nativeStyledTypeface(base,weight,italic)
            FontFamily(androidx.compose.ui.text.font.Typeface(native))
        }
    }
}
fun nativeStyledTypeface(base:Typeface,weight:FontWeight,italic:Boolean):Typeface=
    if(android.os.Build.VERSION.SDK_INT>=28)Typeface.create(base,weight.weight,italic)
    else Typeface.create(base,(if(weight.weight>=600)Typeface.BOLD else Typeface.NORMAL) or (if(italic)Typeface.ITALIC else Typeface.NORMAL))
fun weightOf(value:String)=when(value){"Bold"->FontWeight.Bold;"Medium"->FontWeight.Medium;else->FontWeight.Normal}
fun styledTypography(source:Typography,family:FontFamily,weight:FontWeight?,italic:Boolean,scale:Float):Typography {
    fun TextStyle.adjust()=copy(fontFamily=family,fontStyle=if(italic)FontStyle.Italic else FontStyle.Normal,
        fontSynthesis=FontSynthesis.All,fontWeight=weight?:fontWeight,fontSize=fontSize*scale,lineHeight=lineHeight*scale)
    return Typography(displayLarge=source.displayLarge.adjust(),displayMedium=source.displayMedium.adjust(),displaySmall=source.displaySmall.adjust(),
        headlineLarge=source.headlineLarge.adjust(),headlineMedium=source.headlineMedium.adjust(),headlineSmall=source.headlineSmall.adjust(),
        titleLarge=source.titleLarge.adjust(),titleMedium=source.titleMedium.adjust(),titleSmall=source.titleSmall.adjust(),
        bodyLarge=source.bodyLarge.adjust(),bodyMedium=source.bodyMedium.adjust(),bodySmall=source.bodySmall.adjust(),
        labelLarge=source.labelLarge.adjust(),labelMedium=source.labelMedium.adjust(),labelSmall=source.labelSmall.adjust())
}
