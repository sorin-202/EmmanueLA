package com.emmanuela.launcher.ui.privatespace

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.platform.rememberDeviceAuthentication
import com.emmanuela.launcher.ui.settings.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.emmanuela.launcher.ui.components.localized

@Composable
fun PrivateSpaceSettings(page:String,data:LauncherData,apps:List<LaunchableApp>,model:LauncherViewModel,navigate:(String)->Unit) {
    val e=data.settings.ui.experience;val advanced=e.advanced
    val context=LocalContext.current;val scope=rememberCoroutineScope();val auth=rememberDeviceAuthentication(data.settings.ui.experience.authentication,model::authenticationChanged)
    var query by rememberSaveable(page){mutableStateOf("")}
    when {
                page=="private-space" -> {listOf("Hidden apps" to "hidden","Blocked apps" to "blocked-apps","Private apps" to "private-apps","Security" to "security").forEach{(label,target)->SettingRow(label,"→"){navigate(target)}}}
                page=="private-apps" -> {
                    OutlinedTextField(query,{query=it},singleLine=true,label={Text(localized("Search apps"))},modifier=Modifier.fillMaxWidth())
                    val privateApps=apps.filter{data.policies[it.packageName]?.privateApp==true&&it.label.contains(query,true)}
                    if(privateApps.isEmpty())Text(localized("No private apps"))
                    LazyColumn(Modifier.fillMaxWidth().heightIn(min=100.dp,max=440.dp)){
                        items(privateApps,key={it.id}){app->SettingRow(app.label,"→"){navigate("launch-app:${app.id}")}}
                    }
                    SettingRow("Manage private apps"){navigate("private-manager")}
                }
                page in listOf("hidden-manager","blocked-apps","private-manager") -> {
                    OutlinedTextField(query,{query=it},singleLine=true,label={Text(localized("Search apps"))},modifier=Modifier.fillMaxWidth())
                    LazyColumn(Modifier.fillMaxWidth().heightIn(min=120.dp,max=560.dp)){items(apps.filter{it.label.contains(query,true)&&it.packageName!=context.packageName}.distinctBy{it.packageName},key={it.packageName}){app->
                        val policy=data.policies[app.packageName]?:AppPolicy();val value=when(page){"hidden-manager"->policy.hidden;"blocked-apps"->policy.blocked;else->policy.privateApp}
                        if(page=="blocked-apps")SettingRow(app.label,if(value)"ON" else "OFF"){navigate("blocked-detail:${app.id}")}else ToggleRow(app.label,value){yes->val work={scope.launch{model.changePolicies(setOf(app.packageName)){when(page){"hidden-manager"->it.copy(hidden=yes);"blocked-apps"->it.copy(blocked=yes);else->it.copy(privateApp=yes)}}};Unit};if(page=="private-manager"&&value&&!yes)auth("Change private access",work)else work()}
                    }}
                    if(advanced&&page=="hidden-manager")ToggleRow("Hide from search",e.hideFromSearch){yes->model.experienceSettings{it.copy(hideFromSearch=yes)}}
                }
                page=="security" -> {
                    if(advanced)ChoiceRow("Authentication",e.authentication,listOf("Biometric + device credential","Device credential")){value->model.experienceSettings{it.copy(authentication=value)}}else SettingRow("Authentication",e.authentication){}
                    if(advanced){ChoiceRow("Auto lock",e.autoLock,listOf("Immediately","30 seconds","Screen off")){value->model.experienceSettings{it.copy(autoLock=value)}}
                        ToggleRow("Hide private apps from search",e.hidePrivateSearch){yes->model.experienceSettings{it.copy(hidePrivateSearch=yes)}}}
                }
    }
}
