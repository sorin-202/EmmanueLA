package com.emmanuela.launcher.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.data.HomeShortcuts as HomeShortcutRules
import com.emmanuela.launcher.ui.components.AppIcon
import com.emmanuela.launcher.ui.components.localized

/** Shares Home's bounds, placement units, gesture handling and authorized app launch callback. */
@Composable
fun HomeShortcuts(data:LauncherData,apps:List<LaunchableApp>,model:LauncherViewModel,launch:(LaunchableApp)->Unit,modifier:Modifier=Modifier) {
    val density=LocalDensity.current
    val v=data.settings.ui.v2
    BoxWithConstraints(modifier) {
        val width=maxWidth.value;val height=maxHeight.value
        v.homeShortcuts.filter{HomeShortcutRules.visible(it.appId,data.policies)}.forEach{shortcut->
            key(shortcut.appId) {
                val app=apps.firstOrNull{it.id==shortcut.appId}
                val label=app?.label?:"Unavailable app"
                var point by remember(shortcut.appId){mutableStateOf(Offset(shortcut.position.x,shortcut.position.y))}
                var origin by remember(shortcut.appId){mutableStateOf(point)}
                var dragging by remember(shortcut.appId){mutableStateOf(false)}
                var measured by remember{mutableStateOf(IntSize.Zero)}
                var options by remember{mutableStateOf(false)}
                val maxX=(width-with(density){measured.width.toDp().value}).coerceAtLeast(0f)
                val maxY=(height-with(density){measured.height.toDp().value}).coerceAtLeast(0f)
                val latestMaxX by rememberUpdatedState(maxX);val latestMaxY by rememberUpdatedState(maxY)
                LaunchedEffect(shortcut.position){if(!dragging)point=Offset(shortcut.position.x,shortcut.position.y)}
                val save by rememberUpdatedState<(WidgetPlacement)->Unit>({position->model.v2Settings{prefs->prefs.copy(homeShortcuts=prefs.homeShortcuts.map{if(it.appId==shortcut.appId)it.copy(position=position)else it})}})
                Column(Modifier.zIndex(if(dragging)10f else 0f).onSizeChanged{measured=it}
                    .graphicsLayer{translationX=point.x.coerceIn(0f,maxX)*density.density;translationY=point.y.coerceIn(0f,maxY)*density.density}
                    .homeItemDrag("app:${shortcut.appId}","$label, Home shortcut",{if(app!=null)launch(app)else model.error.value="This app is unavailable. Remove its shortcut or install the app again."},{options=true},
                        start={origin=point;dragging=true},
                        move={delta->point=Offset((point.x+delta.x/density.density).coerceIn(0f,latestMaxX),(point.y+delta.y/density.density).coerceIn(0f,latestMaxY))},
                        drop={
                            if(point!=origin){val final=HomePlacement.dropped(WidgetPlacement(point.x,point.y),latestMaxX,latestMaxY,v.widgetGrid);point=Offset(final.x,final.y);if(point!=origin)save(final)}
                            dragging=false
                        },cancel={point=origin;dragging=false})
                    .then(if(dragging)Modifier.shadow(8.dp,RoundedCornerShape(8.dp)).border(2.dp,MaterialTheme.colorScheme.outline,RoundedCornerShape(8.dp))else Modifier)
                    .width(76.dp).padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    if(app!=null)AppIcon(app,model)else Text("?",style=MaterialTheme.typography.headlineMedium)
                    Text(label,style=MaterialTheme.typography.labelSmall,textAlign=TextAlign.Center,maxLines=2,overflow=TextOverflow.Ellipsis)
                    if(data.policies[shortcut.appId.substringBefore('/')]?.privateApp==true)Text(localized("Private"),style=MaterialTheme.typography.labelSmall)
                }
                if(options)AlertDialog(onDismissRequest={options=false},title={Text(label)},text={Text(localized("Hold and drag to move this icon. Removing it from Home keeps the app installed."))},
                    confirmButton={TextButton(onClick={model.v2Settings{it.copy(homeShortcuts=it.homeShortcuts.filterNot{row->row.appId==shortcut.appId})};options=false}){Text(localized("Remove from Home"))}},
                    dismissButton={TextButton(onClick={options=false}){Text(localized("Done"))}})
            }
        }
    }
}

@Composable
fun HomeShortcutPicker(data:LauncherData,apps:List<LaunchableApp>,model:LauncherViewModel,width:Float,height:Float,dismiss:()->Unit) {
    var query by remember{mutableStateOf("")}
    val saved=data.settings.ui.v2.homeShortcuts
    val candidates=HomeShortcutRules.candidates(apps,saved,data.policies).filter{it.label.contains(query,ignoreCase=true)}
    AlertDialog(onDismissRequest=dismiss,title={Text(localized("Add app to Home"))},text={Column{
        OutlinedTextField(query,{query=it},label={Text(localized("Find installed app"))},singleLine=true)
        if(saved.size>=HomeShortcutRules.MAX_COUNT)Text(localized("Home is full. Remove an icon before adding another."))
        else LazyColumn(Modifier.heightIn(max=360.dp)) {
            items(candidates,key={it.id}){app->Row(Modifier.fillMaxWidth().semantics{contentDescription="Add ${app.label} to Home"}.clickable {
                // Recheck current persisted rules and duplicates in the atomic settings update.
                if(model.data.value.policies[app.packageName]?.hidden!=true)model.v2Settings{prefs->
                    if(prefs.homeShortcuts.size>=HomeShortcutRules.MAX_COUNT||prefs.homeShortcuts.any{it.appId==app.id})prefs
                    else {
                        val point=HomeShortcutRules.availablePosition(prefs.homeShortcuts.map{it.position},width,height)
                        prefs.copy(homeShortcuts=prefs.homeShortcuts+HomeShortcut(app.id,point))
                    }
                }
                dismiss()
            }.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){AppIcon(app,model);Text(app.label,Modifier.padding(start=12.dp))}}
        }
        if(candidates.isEmpty())Text(localized("No more matching apps. Hidden apps stay off Home."))
    }},confirmButton={TextButton(onClick=dismiss){Text(localized("Done"))}})
}
