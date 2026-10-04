package com.emmanuela.launcher.ui.apps

import com.emmanuela.launcher.ContextSafeOpen
import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.AppMetadata
import com.emmanuela.launcher.data.AppNaming
import com.emmanuela.launcher.data.AppPolicy
import com.emmanuela.launcher.data.LaunchableApp
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.data.NotificationMode
import com.emmanuela.launcher.platform.rememberDeviceAuthentication
import com.emmanuela.launcher.ui.folders.FolderMembershipPicker
import com.emmanuela.launcher.ui.components.AppIcon
import com.emmanuela.launcher.ui.components.VectorSymbol
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.settings.ChoiceRow
import com.emmanuela.launcher.ui.settings.SectionLabel
import com.emmanuela.launcher.ui.settings.SettingRow
import com.emmanuela.launcher.ui.settings.ToggleRow


import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AppDetailsContent(app:LaunchableApp,data:LauncherData,model:LauncherViewModel,metadataOnly:Boolean=false) {
    val meta=data.appMetadata[app.id]?:AppMetadata()
    var alias by remember(app.id){mutableStateOf(meta.alias)}
    var tags by remember(app.id){mutableStateOf(meta.tags.joinToString(" "))}
    var folders by remember(app.id){mutableStateOf(data.folders.filter{app.id in it.apps}.map{it.id}.toSet())}
    var addingTag by remember{mutableStateOf(false)}
    var saving by remember{mutableStateOf(false)}
    val validation=remember(alias,tags){runCatching{AppNaming.metadata(alias,tags)}.exceptionOrNull()?.message}
    val scope=rememberCoroutineScope();val context=LocalContext.current;val auth=rememberDeviceAuthentication(data.settings.ui.experience.authentication,model::authenticationChanged)
    val policy=data.policies[app.packageName]?:AppPolicy()
    AppIcon(app,model);Text(app.originalLabel,style=MaterialTheme.typography.titleLarge);Text(app.id,style=MaterialTheme.typography.bodySmall)
    SectionLabel("Name & tags")
    OutlinedTextField(alias,{alias=it.take(80)},label={Text(localized("Alias"))},singleLine=true,modifier=Modifier.fillMaxWidth())
    OutlinedTextField(tags,{tags=it.take(1000)},label={Text(localized("Tags, separated by commas or spaces"))},modifier=Modifier.fillMaxWidth())
    val tagList=runCatching{AppNaming.metadata("",tags).tags}.getOrDefault(emptyList())
    tagList.chunked(3).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{tag->InputChip(selected=true,onClick={tags=tagList.filterNot{it==tag}.joinToString(" ")},label={Text("#$tag ×")})}}}
    TextButton(onClick={addingTag=true}){Text(localized("+ Add tag"))}
    if(addingTag){var newTag by remember{mutableStateOf("")};AlertDialog(onDismissRequest={addingTag=false},title={Text(localized("Add tag"))},text={OutlinedTextField(newTag,{newTag=it.take(100)},singleLine=true)},confirmButton={TextButton(enabled=newTag.isNotBlank(),onClick={tags=(tagList+newTag).joinToString(" ");addingTag=false}){Text(localized("Add"))}},dismissButton={TextButton(onClick={addingTag=false}){Text(localized("Cancel"))}})}
    SectionLabel("Folders")
    FolderMembershipPicker(data,model,folders,!saving){folders=it}
    if(data.folders.isEmpty())Text(localized("Create a folder from the Folders page."))
    validation?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={alias="";tags=""}){Text(localized("Reset name & tags"))};TextButton(enabled=!saving&&validation==null,onClick={scope.launch{saving=true;model.saveAppDetails(app.id,alias,tags,folders);saving=false}}){Text(if(saving)"Saving…" else "Save")}}
    if(!metadataOnly){
    SectionLabel("Access")
    ToggleRow("Hidden",policy.hidden){yes->scope.launch{model.changePolicies(setOf(app.packageName)){it.copy(hidden=yes)}}}
    ToggleRow("Blocked",policy.blocked){yes->scope.launch{model.changePolicies(setOf(app.packageName)){it.copy(blocked=yes)}}}
    ToggleRow("Private",policy.privateApp){yes->val work={scope.launch{model.changePolicies(setOf(app.packageName)){it.copy(privateApp=yes)}};Unit};if(policy.privateApp&&!yes)auth("Change private access",work)else work()}
    if(data.settings.ui.experience.advanced){var timer by remember(policy.dailyLimitMinutes){mutableStateOf(policy.dailyLimitMinutes?.toString()?:"")}
        OutlinedTextField(timer,{timer=it.filter(Char::isDigit).take(4)},label={Text(localized("Daily minutes (empty = unlimited)"))},singleLine=true)
        TextButton(enabled=timer.isEmpty()||(timer.toIntOrNull()?:0) in 1..1440,onClick={scope.launch{model.changePolicies(setOf(app.packageName)){it.copy(dailyLimitMinutes=timer.toIntOrNull())}}}){Text(localized("Save timer"))}
        ChoiceRow("Notifications",policy.notifications.name,NotificationMode.entries.map{it.name}){value->scope.launch{model.changePolicies(setOf(app.packageName)){it.copy(notifications=NotificationMode.valueOf(value))}}}
        TextButton(onClick={auth("Reset app rules"){scope.launch{model.changePolicies(setOf(app.packageName)){AppPolicy()}}}}){Text(localized("Reset app rules"))}}
    SettingRow("System app info"){ContextSafeOpen(context,Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${app.packageName}"))){model.error.value="App information unavailable."}}
    }
}

/** Dedicated membership picker commits with existing metadata; protected folders remain checked by the repository. */
@Composable
fun FolderAssignmentsContent(app:LaunchableApp,data:LauncherData,model:LauncherViewModel) {
    val scope=rememberCoroutineScope()
    var selected by remember(app.id){mutableStateOf(data.folders.filter{app.id in it.apps}.map{it.id}.toSet())}
    var saving by remember{mutableStateOf(false)}
    FolderMembershipPicker(data,model,selected,!saving){selected=it}
    TextButton(enabled=!saving,onClick={scope.launch{
        saving=true
        val meta=model.data.value.appMetadata[app.id]?:AppMetadata()
        model.saveAppDetails(app.id,meta.alias,meta.tags.joinToString(" "),selected)
        saving=false
    }}){Text(localized("Save"))}
}
