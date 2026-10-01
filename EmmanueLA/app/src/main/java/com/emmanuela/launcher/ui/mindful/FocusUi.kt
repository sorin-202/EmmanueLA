@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.emmanuela.launcher.ui.mindful

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.FocusGroup
import com.emmanuela.launcher.data.LaunchableApp
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.data.PendingPause
import com.emmanuela.launcher.platform.rememberDeviceAuthentication
import com.emmanuela.launcher.ui.components.AppIcon
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.settings.SectionLabel
import com.emmanuela.launcher.ui.settings.SettingRow
import com.emmanuela.launcher.ui.settings.SliderRow
import com.emmanuela.launcher.ui.settings.ToggleRow


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FocusGroupEditor(id:String,data:LauncherData,apps:List<LaunchableApp>,model:LauncherViewModel,done:()->Unit) {
    val original=data.focusGroups.find{it.id==id}
    var draft by remember(id){mutableStateOf(original?:FocusGroup())}
    var busy by remember{mutableStateOf(false)}
    val scope=rememberCoroutineScope();val auth=rememberDeviceAuthentication(data.settings.ui.experience.authentication,model::authenticationChanged)
    fun commit(delete:Boolean=false){val work={scope.launch{busy=true;if(if(delete)model.deleteFocusGroup(draft.id)else model.saveFocusGroup(draft))done();busy=false};Unit}
        if(original?.strict==true&&original.requireAuthentication)auth("Edit strict group"){model.authorizeStrictGroup(draft.id);work()}else work()}
    OutlinedTextField(draft.name,{draft=draft.copy(name=it.take(40))},label={Text(localized("Group name"))},modifier=Modifier.fillMaxWidth())
    SectionLabel("Apps")
    Column(Modifier.heightIn(max=220.dp).verticalScroll(rememberScrollState())){apps.filter{data.policies[it.packageName]?.hidden!=true}.distinctBy{it.packageName}.forEach{app->
        Row(Modifier.fillMaxWidth().clickable{draft=draft.copy(packages=if(app.packageName in draft.packages)draft.packages-app.packageName else draft.packages+app.packageName)},verticalAlignment=Alignment.CenterVertically){Checkbox(app.packageName in draft.packages,null);Text(app.label)}}}
    val advanced=data.settings.ui.experience.advanced
    SectionLabel("Pause before opening")
    ToggleRow("Enabled",draft.pause){draft=draft.copy(pause=it)}
    if(draft.pause){
        if(advanced)SliderRow("Delay (seconds)",draft.pauseSeconds.toFloat(),0f..60f){draft=draft.copy(pauseSeconds=it.toInt())}
        else Text("${draft.pauseSeconds} seconds",style=MaterialTheme.typography.bodyMedium)
        if(advanced)ToggleRow("Increase after repeated opens",draft.escalatingPause){draft=draft.copy(escalatingPause=it)}
    }
    SectionLabel("Usage")
    ToggleRow("Daily limit",draft.dailyMinutes>0){draft=draft.copy(dailyMinutes=if(it)60 else 0)}
    if(draft.dailyMinutes>0)SliderRow("Daily limit (minutes)",draft.dailyMinutes.toFloat(),1f..240f){draft=draft.copy(dailyMinutes=it.toInt())}
    if(advanced){
        ToggleRow("Session limit",draft.sessionMinutes>0){draft=draft.copy(sessionMinutes=if(it)15 else 0)}
        if(draft.sessionMinutes>0)SliderRow("Session limit (minutes)",draft.sessionMinutes.toFloat(),1f..240f){draft=draft.copy(sessionMinutes=it.toInt())}
        ToggleRow("Opens per day",draft.maxOpens>0){draft=draft.copy(maxOpens=if(it)10 else 0)}
        if(draft.maxOpens>0)SliderRow("Maximum opens",draft.maxOpens.toFloat(),1f..100f){draft=draft.copy(maxOpens=it.toInt())}
    }
    SectionLabel("Schedule")
    ToggleRow("Enabled",draft.schedule){draft=draft.copy(schedule=it)}
    if(draft.schedule){
        Row{(1..7).forEach{day->TextButton(modifier=Modifier.weight(1f),contentPadding=PaddingValues(0.dp),onClick={draft=draft.copy(days=if(day in draft.days)draft.days-day else draft.days+day)}){Text(listOf("M","T","W","T","F","S","S")[day-1],color=if(day in draft.days)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)}}}
        TimeChoice("Start",draft.startMinute){draft=draft.copy(startMinute=it)};TimeChoice("End",draft.endMinute){draft=draft.copy(endMinute=it)}
    }
    if(advanced){SectionLabel("Mindful prompt");ToggleRow("Enabled",draft.prompt){draft=draft.copy(prompt=it)}}
    SectionLabel("Strict mode")
    ToggleRow("Enabled",draft.strict){yes->if(yes&&draft.requireAuthentication)auth("Enable strict group"){draft=draft.copy(strict=true)}else draft=draft.copy(strict=yes)}
    if(advanced)ToggleRow("Require authentication",draft.requireAuthentication){yes->draft=draft.copy(requireAuthentication=yes)}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){if(original!=null)TextButton(enabled=!busy,onClick={commit(true)}){Text(localized("Delete"))};TextButton(onClick=done){Text(localized("Cancel"))};TextButton(enabled=!busy&&draft.name.isNotBlank()&&draft.packages.isNotEmpty()&&(!draft.schedule||draft.days.isNotEmpty()),onClick={commit()}){Text(localized("Save"))}}
}
@Composable
fun TimeChoice(label:String,minute:Int,save:(Int)->Unit){var show by remember{mutableStateOf(false)};SettingRow(label,"%02d:%02d".format(minute/60,minute%60)){show=true};if(show){val picker=androidx.compose.material3.rememberTimePickerState(minute/60,minute%60,true);AlertDialog(onDismissRequest={show=false},title={Text(label)},text={androidx.compose.material3.TimeInput(picker)},confirmButton={TextButton(onClick={save(picker.hour*60+picker.minute);show=false}){Text(localized("Save"))}},dismissButton={TextButton(onClick={show=false}){Text(localized("Cancel"))}})}}
@Composable
fun PauseScreen(pending:PendingPause,model:LauncherViewModel){var remaining by remember(pending){mutableIntStateOf(pending.seconds)};LaunchedEffect(pending){while(remaining>0){delay(1000);remaining--}}
    Surface(Modifier.fillMaxSize()){Column(Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        AppIcon(pending.app,model);Text(pending.app.label,Modifier.padding(top=24.dp),style=MaterialTheme.typography.headlineMedium);Text(localized("Take a moment"),Modifier.padding(top=16.dp),style=MaterialTheme.typography.titleLarge)
        if(pending.prompt)Text(localized("What would you like to do in this app?"),Modifier.padding(vertical=16.dp))
        if(remaining>0)Text(remaining.toString(),fontSize=48.sp)else TextButton(onClick=model::confirmPause){Text(localized("Open app"))}
        TextButton(onClick=model::cancelPause){Text(localized("Cancel"))}
    }}
}
