package com.emmanuela.launcher.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.emmanuela.launcher.data.V2Preferences
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ClockFace(now:Long,p:V2Preferences,color:Color,style:TextStyle,modifier:Modifier){
    var tick by remember{mutableLongStateOf(now)}
    LaunchedEffect(now,p.clockSeconds){tick=now;if(p.clockSeconds)while(true){delay(1000-System.currentTimeMillis()%1000);tick=System.currentTimeMillis()}}
    val time=Instant.ofEpochMilli(tick).atZone(ZoneId.systemDefault())
    val hour=if(p.clockAmPm)(time.hour%12).let{if(it==0)12 else it}else time.hour
    val minute="%02d".format(time.minute)
    when(p.clockFormat){
        "Hands"->{val dimension=with(LocalDensity.current){style.fontSize.toDp()*3f}.coerceAtLeast(56.dp)
            Canvas(modifier.size(dimension)){
                val center=Offset(size.width/2,size.height/2);val radius=size.minDimension*.45f
                fun hand(angle:Double,length:Float,width:Float){val radians=angle*Math.PI/180.0-Math.PI/2;drawLine(color,center,center+Offset((cos(radians)*length).toFloat(),(sin(radians)*length).toFloat()),strokeWidth=width,cap=StrokeCap.Round)}
                hand(time.hour%12*30.0+time.minute*.5,radius*.6f,3.dp.toPx())
                hand(time.minute*6.0+if(p.clockSeconds)time.second*.1 else 0.0,radius,2.dp.toPx())
            }
        }
        "Compact"->Text(buildAnnotatedString{append("%02d".format(hour));withStyle(SpanStyle(fontSize=style.fontSize*.6f,baselineShift=BaselineShift.Superscript)){append(minute)};if(p.clockSeconds)append(":%02d".format(time.second));if(p.clockAmPm)append(if(time.hour<12)" AM"else" PM")},modifier=modifier,color=color,style=style)
        else->Text(time.format(DateTimeFormatter.ofPattern(if(p.clockAmPm)if(p.clockSeconds)"hh:mm:ss a"else"hh:mm a"else if(p.clockSeconds)"HH:mm:ss"else"HH:mm")),modifier=modifier,color=color,style=style)
    }
}
