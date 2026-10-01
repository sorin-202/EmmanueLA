package com.emmanuela.launcher.ui.settings

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.platform.SystemDestinations
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.components.VectorSymbol
import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomControlsPreview(data:LauncherData,apps:List<LaunchableApp>){
    val u=data.settings.ui
    Column(Modifier.fillMaxWidth().height(168.dp).border(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.5f),RoundedCornerShape(16.dp)).padding(12.dp).clipToBounds()){
        Text(localized("Preview"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.BottomCenter){
            val weight=when(u.bottomBarWeight){"Bold"->FontWeight.Bold;"Medium"->FontWeight.Medium;else->FontWeight.Normal}
            Row(Modifier.fillMaxWidth(u.bottomBarWidth).height(u.bottomBarHeight.dp).offset(y=(-u.bottomBarOffset/3f).dp).padding(horizontal=u.bottomBarPadding.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                if(u.v2.shortcutMode=="Icon"){
                    VectorSymbol(u.v2.leftVector,MaterialTheme.colorScheme.onSurface,Modifier.size((28f*u.bottomBarTextScale).dp));VectorSymbol(u.v2.rightVector,MaterialTheme.colorScheme.onSurface,Modifier.size((28f*u.bottomBarTextScale).dp))
                }else{
                    Text(actionName(u.leftShortcut,apps),fontSize=(14f*u.bottomBarTextScale).sp,fontWeight=weight,maxLines=1)
                    Text(actionName(u.rightShortcut,apps),fontSize=(14f*u.bottomBarTextScale).sp,fontWeight=weight,maxLines=1)
                }
            }
        }
    }
}
@Composable
fun WeatherWebsiteRow(data:LauncherData,model:LauncherViewModel){
    var website by remember(data.settings.ui.experience.weatherWebsite){mutableStateOf(data.settings.ui.experience.weatherWebsite)}
    OutlinedTextField(website,{website=it.take(2048)},label={Text(localized("Weather website"))},singleLine=true,modifier=Modifier.fillMaxWidth())
    val valid=website.startsWith("https://")||website.startsWith("http://")
    TextButton(enabled=valid,onClick={model.uiSettings{it.copy(v2=it.v2.copy(weatherAction="weather-web"),experience=it.experience.copy(weatherWebsite=website))}}){Text(localized("Use website on tap"))}
}
@Composable
fun WellbeingDestinationPicker(data:LauncherData,model:LauncherViewModel){
    val context=LocalContext.current
    var query by remember{mutableStateOf("")}
    val options by produceState<List<Pair<String,String>>>(emptyList()){value=SystemDestinations.wellbeingOptions(context)}
    Box(Modifier.selectionMarker(data.settings.ui.experience.wellbeingComponent.isEmpty())){SettingRow("Use automatic destination",if(data.settings.ui.experience.wellbeingComponent.isEmpty())"✓" else ""){model.experienceSettings{it.copy(wellbeingComponent="")}}}
    OutlinedTextField(query,{query=it},label={Text(localized("Search Android settings"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
    LazyColumn(Modifier.fillMaxWidth().heightIn(min=120.dp,max=440.dp)){
        items(options.filter{it.first.contains(query,true)||it.second.contains(query,true)},key={it.second}){(name,component)->
            Box(Modifier.selectionMarker(data.settings.ui.experience.wellbeingComponent==component)){SettingRow(name,if(data.settings.ui.experience.wellbeingComponent==component)"✓" else ""){model.experienceSettings{it.copy(wellbeingComponent=component)}}}
        }
    }
    TextButton(onClick={if(!SystemDestinations.wellbeing(context,model.data.value.settings.ui.experience.wellbeingComponent))model.error.value="This destination is not exported by your phone. Select a different Android settings page."}){Text(localized("Test shortcut"))}
}
@Composable
fun ContactSearchSettings(data:LauncherData,apps:List<LaunchableApp>,model:LauncherViewModel){
    val e=data.settings.ui.experience
    val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(!granted)model.error.value="Allow Contacts in System → Permissions to use @ search."}
    ToggleRow("Search contacts with @",e.contactsSearch){yes->model.experienceSettings{it.copy(contactsSearch=yes)};if(yes)request.launch(Manifest.permission.READ_CONTACTS)}
    SettingRow("Contacts permission"){request.launch(Manifest.permission.READ_CONTACTS)}
    Text(localized("Phone contacts are included by default. Other apps appear only when they expose contact integrations to Android."),style=MaterialTheme.typography.bodySmall)
    LazyColumn(Modifier.fillMaxWidth().heightIn(min=120.dp,max=420.dp)){
        items(apps.distinctBy{it.packageName},key={it.packageName}){app->
            ToggleRow(app.label,app.packageName in e.contactPackages){yes->model.experienceSettings{it.copy(contactPackages=if(yes)it.contactPackages+app.packageName else it.contactPackages-app.packageName)}}
        }
    }
}
