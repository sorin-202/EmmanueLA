package com.emmanuela.launcher.ui.home

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.data.WidgetPlacement
import com.emmanuela.launcher.data.styleForWidget
import com.emmanuela.launcher.ui.settings.widgetTextStyle
import com.emmanuela.launcher.ui.settings.widgetBackground
import com.emmanuela.launcher.platform.WeatherRepository
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.navigation.homeClick
import com.emmanuela.launcher.ui.settings.ChoiceRow
import com.emmanuela.launcher.ui.settings.setWidget
import com.emmanuela.launcher.ui.settings.widgetName


import android.provider.Settings
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.clickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun HomeWidgets(data:LauncherData,model:LauncherViewModel,now:Long,battery:Int,action:(String)->Unit,modifier:Modifier=Modifier,arranging:Boolean=false) {
    val p=data.settings;val u=p.ui;val v=u.v2
    val context=LocalContext.current;val density=LocalDensity.current
    val time by model.screenTime.collectAsStateWithLifecycle()
    val allowed by model.usageAllowed.collectAsStateWithLifecycle()
    val weather by produceState("Weather unavailable",v.weatherEnabled,v.latitude,v.longitude,now/3_600_000L) {
        if(v.weatherEnabled)value=WeatherRepository.current(context,v.latitude,v.longitude)
    }
    val locationText by produceState("Location permission needed", v.locationWidgetEnabled, now/60_000L) {
        if (!v.locationWidgetEnabled) return@produceState
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!granted) return@produceState
        value = withContext(Dispatchers.IO) { runCatching {
            val manager = context.getSystemService(LocationManager::class.java)
            val location = manager.getProviders(true).asSequence().mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }.maxByOrNull { it.time }
            if (location == null) "Approx. location unavailable" else "%.3f, %.3f".format(location.latitude, location.longitude)
        }.getOrDefault("Approx. location unavailable") }
    }
    val font=when(u.widgetFont){"Serif"->FontFamily.Serif;"Monospace"->FontFamily.Monospace;"Sans"->FontFamily.SansSerif;else->MaterialTheme.typography.bodyLarge.fontFamily}
    val base=if(u.widgetColor!=0L)Color(u.widgetColor)else if(p.wallpapers.isNotEmpty())Color.White else MaterialTheme.colorScheme.onBackground
    val minutes=(time?:0L)/60_000f
    val entries=listOfNotNull(
        if(p.showClock)Triple("clock",android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(now)),u.clockAction)else null,
        if(p.showDate)Triple("date",Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE, d MMM",if(u.experience.language=="system")java.util.Locale.getDefault()else java.util.Locale.forLanguageTag(u.experience.language))),u.dateAction)else null,
        if(p.showBattery)Triple("battery",if(battery<0)"Battery" else "$battery%",u.batteryAction)else null,
        if(p.showScreenTime)Triple("usage",if(!allowed)"Enable screen time" else if(time==null)"Screen time unavailable" else "${minutes.toInt()/60}h ${minutes.toInt()%60}m",if(allowed)u.screenTimeAction else "usage")else null,
        if(v.weatherEnabled)Triple("weather",weather,v.weatherAction)else null,
        if(v.locationWidgetEnabled)Triple("location",locationText,"none")else null
    )
    BoxWithConstraints(modifier) {
        val width=maxWidth.value;val height=maxHeight.value
        entries.forEach { (id,label,target)->
            val appearance=p.styleForWidget(id)
            val widgetColor=if(appearance.color!=0L)Color(appearance.color)else if(p.wallpapers.isNotEmpty())Color.White else MaterialTheme.colorScheme.onBackground
            val initial=v.widgetPositions[id] ?: when(id){"clock"->WidgetPlacement(width*.33f,48f);"date"->WidgetPlacement(width*.28f,114f);"battery"->WidgetPlacement(width*.44f,156f);"usage"->WidgetPlacement(16f,4f);"location"->WidgetPlacement(width*.2f,236f);else->WidgetPlacement(width*.2f,196f)}
            var point by remember(id){mutableStateOf(Offset(initial.x,initial.y))}
            var measured by remember{mutableStateOf(IntSize.Zero)}
            val savedScale=u.experience.widgetScales[id]?:1f
            var scale by remember(id){mutableFloatStateOf(savedScale)}
            var dragging by remember(id){mutableStateOf(false)}
            LaunchedEffect(initial,savedScale){if(!dragging){point=Offset(initial.x,initial.y);scale=savedScale}}
            var options by remember{mutableStateOf(false)}
            val maxX=(width-with(density){measured.width.toDp().value}*(if(arranging)scale/savedScale else 1f)).coerceAtLeast(0f)
            val maxY=(height-with(density){measured.height.toDp().value}*(if(arranging)scale/savedScale else 1f)).coerceAtLeast(0f)
            val latestMaxX by rememberUpdatedState(maxX)
            val latestMaxY by rememberUpdatedState(maxY)
            val currentSave by rememberUpdatedState<(Offset)->Unit>({ xy->model.v2Settings{it.copy(widgetPositions=it.widgetPositions+(id to WidgetPlacement(xy.x,xy.y)))} })
            val currentScaleSave by rememberUpdatedState<(Float)->Unit>({ n->model.experienceSettings{it.copy(widgetScales=it.widgetScales+(id to n))} })
            val drag=if(arranging)Modifier.pointerInput(id,v.widgetGrid,width,height){
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed=false)
                    dragging=true
                    var moved=false
                    try{
                    do {
                        val event=awaitPointerEvent()
                        val pan=event.calculatePan();val zoom=event.calculateZoom()
                        if(pan.getDistance()>0f || zoom!=1f){moved=true;event.changes.forEach{it.consume()}
                            point=Offset((point.x+pan.x/density.density).coerceIn(0f,width-24f),(point.y+pan.y/density.density).coerceIn(0f,height-24f))
                            scale=(scale*zoom).coerceIn(.5f,2f)
                        }
                    }while(event.changes.any{it.pressed})
                    if(moved){val grid=v.widgetGrid.toFloat();point=Offset(((point.x/grid).roundToInt()*grid).coerceIn(0f,latestMaxX),((point.y/grid).roundToInt()*grid).coerceIn(0f,latestMaxY));val finalPoint=WidgetPlacement(point.x,point.y);val finalScale=scale;model.uiSettings{ui->ui.copy(v2=ui.v2.copy(widgetPositions=ui.v2.widgetPositions+(id to finalPoint)),experience=ui.experience.copy(widgetScales=ui.experience.widgetScales+(id to finalScale)))}}else options=true
                    }finally{dragging=false}
                }
            }else Modifier.homeClick(id,{action(target)},{if(v.gesturesEnabled)action(v.holdAction)})
            if(options)AlertDialog(onDismissRequest={options=false},title={Text(widgetName(id))},text={Column{
                ChoiceRow("Size",if(scale<.9f)"Small" else if(scale>1.2f)"Large" else "Medium",listOf("Small","Medium","Large")){value->scale=when(value){"Small"->.75f;"Large"->1.5f;else->1f};currentScaleSave(scale)}
                ChoiceRow("Alignment",u.experience.widgetAlignments[id]?:"Left",listOf("Left","Center","Right")){value->
                    point=point.copy(x=when(value){"Center"->maxX/2f;"Right"->maxX;else->0f});currentSave(point);model.experienceSettings{it.copy(widgetAlignments=it.widgetAlignments+(id to value))}}
                TextButton(onClick={setWidget(model,id,false);options=false}){Text(localized("Remove"))}
            }},confirmButton={TextButton(onClick={options=false}){Text(localized("Done"))}})

            Text(label,color=if(id=="usage"&&allowed)lerp(widgetColor,Color(0xFFFF1744),(minutes/u.screenTimeLimit).coerceIn(0f,1f))else widgetColor,
                style=widgetTextStyle(data,id,if(arranging)savedScale else scale),
                modifier=Modifier.onSizeChanged{measured=it}
                    .graphicsLayer { transformOrigin=androidx.compose.ui.graphics.TransformOrigin(0f,0f);scaleX=if(arranging)scale/savedScale else 1f;scaleY=scaleX;translationX = with(density) { point.x.coerceIn(0f,maxX).dp.toPx() }; translationY = with(density) { point.y.coerceIn(0f,maxY).dp.toPx() } }
                    .then(drag)
                    .widgetBackground(appearance)
                    .then(if(arranging)Modifier.border(1.dp,MaterialTheme.colorScheme.outline)else Modifier).padding(8.dp))
        }
    }
}

@Composable
fun WidgetLayoutScreen(data:LauncherData,model:LauncherViewModel,now:Long,battery:Int,done:()->Unit) {
    val apps by model.apps.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        HomeScreen(data,apps,model,now,battery,{},{},{},{},{},arranging=true)
        Surface(Modifier.fillMaxWidth().align(androidx.compose.ui.Alignment.BottomCenter),color=MaterialTheme.colorScheme.surface.copy(alpha=.94f)) {
            Row(Modifier.navigationBarsPadding().fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                TextButton(onClick={model.uiSettings{it.copy(v2=it.v2.copy(widgetPositions=emptyMap()),experience=it.experience.copy(widgetScales=emptyMap(),widgetAlignments=emptyMap()))}}){Text(localized("Reset"))}
                Text(localized("Drag · pinch · tap"),Modifier.align(androidx.compose.ui.Alignment.CenterVertically),style=MaterialTheme.typography.bodySmall)
                TextButton(onClick=done){Text(localized("Done"))}
            }
        }
    }
}

fun openDigitalWellbeing(context:android.content.Context,onFailure:()->Unit) {
    if(!com.emmanuela.launcher.platform.SystemDestinations.wellbeing(context))onFailure()
}
