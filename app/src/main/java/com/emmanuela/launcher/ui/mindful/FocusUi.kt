@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.emmanuela.launcher.ui.mindful

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.ui.settings.ChoiceRow
import com.emmanuela.launcher.platform.BrowserAdapters
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
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FocusGroupEditor(id:String,data:LauncherData,apps:List<LaunchableApp>,model:LauncherViewModel,done:()->Unit) {
    val original=data.focusGroups.find{it.id==id}
    var draft by remember(id){mutableStateOf(original?:FocusGroup())}
    var busy by remember{mutableStateOf(false)}
    val scope=rememberCoroutineScope();val auth=rememberDeviceAuthentication(data.settings.ui.experience.authentication,model::authenticationChanged)
    fun commit(delete:Boolean=false){val work={scope.launch{busy=true;if(if(delete)model.deleteFocusGroup(draft.id)else model.saveFocusGroup(draft.copy(breakStartedAt=original?.breakStartedAt?:0L,breakUntil=original?.breakUntil?:0L)))done();busy=false};Unit}
        if(original?.strict==true&&original.requireAuthentication)auth("Edit strict group"){model.authorizeStrictGroup(draft.id);work()}else work()}
    OutlinedTextField(draft.name,{draft=draft.copy(name=it.take(40))},label={Text(localized("Group name"))},modifier=Modifier.fillMaxWidth())
    var blockTab by remember{mutableStateOf("Apps")}
    var newEntry by remember{mutableStateOf("")}
    ChoiceRow("Blocklist",blockTab,listOf("Apps","Sites","Keywords")){blockTab=it}
    if(blockTab=="Apps")Column(Modifier.heightIn(max=220.dp).verticalScroll(rememberScrollState())){apps.filter{data.policies[it.packageName]?.hidden!=true}.distinctBy{it.packageName}.forEach{app->
        Row(Modifier.fillMaxWidth().clickable{draft=draft.copy(packages=if(app.packageName in draft.packages)draft.packages-app.packageName else draft.packages+app.packageName)},verticalAlignment=Alignment.CenterVertically){Checkbox(app.packageName in draft.packages,null);Text(app.label)}}}
    else {
        OutlinedTextField(newEntry,{newEntry=it.take(512)},label={Text(if(blockTab=="Sites")"Website domain" else "URL keyword")},singleLine=true,modifier=Modifier.fillMaxWidth())
        TextButton(enabled=(if(blockTab=="Sites")draft.websites.size else draft.keywords.size)<200,onClick={val value=if(blockTab=="Sites")FocusWindows.host(newEntry)else newEntry.trim().takeIf{it.isNotEmpty()};if(value!=null){draft=if(blockTab=="Sites")draft.copy(websites=draft.websites+value)else draft.copy(keywords=draft.keywords+value);newEntry=""}}){Text("Add")}
        (if(blockTab=="Sites")draft.websites else draft.keywords).forEach{entry->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(entry,Modifier.weight(1f));TextButton(onClick={draft=if(blockTab=="Sites")draft.copy(websites=draft.websites-entry)else draft.copy(keywords=draft.keywords-entry)}){Text("Remove")}}}
        Text("Sites match domains and subdomains. Keywords match the URL exposed by a supported browser, not encrypted page content.",style=MaterialTheme.typography.bodySmall)
        SectionLabel("Browsers with address adapters")
        apps.distinctBy{it.packageName}.filter{it.packageName in BrowserAdapters.ids}.forEach{app->ToggleRow(app.label,app.packageName in draft.browsers){yes->draft=draft.copy(browsers=if(yes)draft.browsers+app.packageName else draft.browsers-app.packageName)}}
    }
    val advanced=data.settings.ui.experience.advanced
    SectionLabel("Strict Block")
    ToggleRow("Block continuously",draft.blockAlways){draft=draft.copy(blockAlways=it)}
    Text("Blocks this group's apps and sites until disabled or a Break is allowed. Other app and group rules still apply.",style=MaterialTheme.typography.bodySmall)
    if(original!=null)FocusBreakControls(original,data,model)
    SectionLabel("Pause before opening")
    ToggleRow("Enabled",draft.pause){draft=draft.copy(pause=it)}
    if(draft.pause){
        SliderRow("Delay (seconds)",draft.pauseSeconds.toFloat(),0f..60f){draft=draft.copy(pauseSeconds=it.toInt())}
        ToggleRow("Enter an intention",draft.requireIntention){draft=draft.copy(requireIntention=it)}
        if(draft.requireIntention)Text("Your intention is used only for this launch and is not saved.",style=MaterialTheme.typography.bodySmall)
        if(advanced)ToggleRow("Increase after repeated opens",draft.escalatingPause){draft=draft.copy(escalatingPause=it)}
    }
    SectionLabel("Usage")
    ToggleRow("Daily limit",draft.dailyMinutes>0){draft=draft.copy(dailyMinutes=if(it)60 else 0)}
    if(draft.dailyMinutes>0)SliderRow("Daily limit (minutes)",draft.dailyMinutes.toFloat(),1f..240f){draft=draft.copy(dailyMinutes=it.toInt())}
    ChoiceRow("When a limit is reached",draft.limitAction,listOf("Block","Warn")){draft=draft.copy(limitAction=it)}
    Text("Block lasts until the daily reset or session cooldown. Warn allows continued use. Strict Block always takes priority.",style=MaterialTheme.typography.bodySmall)
    if(advanced){
        SliderRow("Warning before limit (minutes)",draft.warningMinutes.toFloat(),0f..30f){draft=draft.copy(warningMinutes=it.toInt())}
        SliderRow("Grace period (minutes)",draft.graceMinutes.toFloat(),0f..30f){draft=draft.copy(graceMinutes=it.toInt())}
        Text("Grace extends daily and session time allowances. Warnings appear at launch, and during use when background rule access is enabled.",style=MaterialTheme.typography.bodySmall)
    }
    run {
        ChoiceRow("Track daily limit",if(draft.perApp)"Per app"else "Group",listOf("Per app","Group")){draft=draft.copy(perApp=it=="Per app")}
        ToggleRow("Session limit",draft.sessionMinutes>0){draft=draft.copy(sessionMinutes=if(it)15 else 0)}
        if(draft.sessionMinutes>0)SliderRow("Session limit (minutes)",draft.sessionMinutes.toFloat(),1f..240f){draft=draft.copy(sessionMinutes=it.toInt())}
        if(draft.sessionMinutes>0)SliderRow("Break between sessions (seconds)",draft.cooldownSeconds.toFloat(),1f..600f){draft=draft.copy(cooldownSeconds=it.toInt())}
        ToggleRow("Opens per day",draft.maxOpens>0){draft=draft.copy(maxOpens=if(it)10 else 0)}
        if(draft.maxOpens>0)SliderRow("Maximum opens",draft.maxOpens.toFloat(),1f..100f){draft=draft.copy(maxOpens=it.toInt())}
    }
    SectionLabel("Time windows")
    draft.windows.forEachIndexed{index,window->
        ChoiceRow("Window ${index+1}",window.mode,listOf("Limit","Strict block","Break")){mode->draft=draft.copy(windows=draft.windows.mapIndexed{i,w->if(i==index)w.copy(mode=mode)else w})}
        Row{(1..7).forEach{day->TextButton(modifier=Modifier.weight(1f),contentPadding=PaddingValues(0.dp),onClick={val days=if(day in window.days)window.days-day else window.days+day;if(days.isNotEmpty())draft=draft.copy(windows=draft.windows.mapIndexed{i,w->if(i==index)w.copy(days=days)else w})}){Text(listOf("M","T","W","T","F","S","S")[day-1],color=if(day in window.days)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)}}}
        TimeChoice("Start",window.start){value->draft=draft.copy(windows=draft.windows.mapIndexed{i,w->if(i==index)w.copy(start=value)else w})}
        TimeChoice("End",window.end){value->draft=draft.copy(windows=draft.windows.mapIndexed{i,w->if(i==index)w.copy(end=value)else w})}
        TextButton(onClick={draft=draft.copy(windows=draft.windows.filterIndexed{i,_->i!=index})}){Text("Remove window")}
    }
    TextButton(enabled=draft.windows.size<32,onClick={draft=draft.copy(windows=draft.windows+FocusWindow())}){Text("+ Add time window")}
    Text("Scheduled Strict Block wins over Break. Break suspends this group's continuous block and limits. Limits otherwise apply inside Limit windows, or all day when none are defined.",style=MaterialTheme.typography.bodySmall)
    SectionLabel("Scheduled block")
    ToggleRow("Enabled",draft.schedule){draft=draft.copy(schedule=it)}
    if(draft.schedule){
        Row{(1..7).forEach{day->TextButton(modifier=Modifier.weight(1f),contentPadding=PaddingValues(0.dp),onClick={draft=draft.copy(days=if(day in draft.days)draft.days-day else draft.days+day)}){Text(listOf("M","T","W","T","F","S","S")[day-1],color=if(day in draft.days)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)}}}
        TimeChoice("Start",draft.startMinute){draft=draft.copy(startMinute=it)};TimeChoice("End",draft.endMinute){draft=draft.copy(endMinute=it)}
    }
    if(advanced){SectionLabel("Mindful prompt");ToggleRow("Enabled",draft.prompt){draft=draft.copy(prompt=it)}}
    SectionLabel("Protect rule changes")
    Text("Requires device authentication to edit this group or start a Break. This does not itself block apps.",style=MaterialTheme.typography.bodySmall)
    ToggleRow("Enabled",draft.strict){yes->if(yes&&draft.requireAuthentication)auth("Enable strict group"){draft=draft.copy(strict=true)}else draft=draft.copy(strict=yes)}
    if(advanced)ToggleRow("Require authentication",draft.requireAuthentication){yes->draft=draft.copy(requireAuthentication=yes)}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){if(original!=null)TextButton(enabled=!busy,onClick={commit(true)}){Text(localized("Delete"))};TextButton(onClick=done){Text(localized("Cancel"))};TextButton(enabled=!busy&&draft.name.isNotBlank()&&(draft.packages.isNotEmpty()||((draft.websites.isNotEmpty()||draft.keywords.isNotEmpty())&&draft.browsers.isNotEmpty()))&&(!draft.schedule||draft.days.isNotEmpty()),onClick={commit()}){Text(localized("Save"))}}
}
@Composable
fun rememberFocusTime():java.time.ZonedDateTime {
    val owner=androidx.lifecycle.compose.LocalLifecycleOwner.current
    val time by produceState(java.time.ZonedDateTime.now(),owner) {
        owner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while(true){value=java.time.ZonedDateTime.now();delay(1000)}
        }
    }
    return time
}

@Composable
fun FocusBreakControls(group:FocusGroup,data:LauncherData,model:LauncherViewModel) {
    val now=rememberFocusTime()
    var show by remember(group.id){mutableStateOf(false)}
    var minutes by remember(group.id){mutableIntStateOf(10)}
    var busy by remember{mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    val auth=rememberDeviceAuthentication(data.settings.ui.experience.authentication,model::authenticationChanged)
    fun change(duration:Int){
        val work={scope.launch{busy=true;try{if(model.setFocusBreak(group.id,duration))show=false}finally{busy=false}};Unit}
        if(group.strict&&group.requireAuthentication)auth("Change Break"){model.authorizeStrictGroup(group.id);work()}else work()
    }
    val remaining=(group.breakUntil-now.toInstant().toEpochMilli()).coerceAtLeast(0)/1000
    if(remaining>0){
        Text("Break: ${remaining/60}m ${remaining%60}s remaining"+(if(FocusWindows.onBreak(group,now))"" else " · scheduled block takes priority"))
        TextButton(enabled=!busy,onClick={change(0)}){Text("End Break")}
    }else TextButton(enabled=!busy,onClick={show=true}){Text("Take a Break")}
    if(show)AlertDialog(onDismissRequest={if(!busy)show=false},title={Text("Temporary access")},text={Column{
        Text("Only this group's rules are suspended. Scheduled Strict Block and other groups still apply. Usage allowances are not reset.")
        Row{listOf(5,10,15).forEach{value->TextButton(onClick={minutes=value}){Text("$value min")}}}
        SliderRow("Minutes",minutes.toFloat(),1f..120f){minutes=it.toInt()}
    }},confirmButton={TextButton(enabled=!busy,onClick={change(minutes)}){Text("Start $minutes-minute Break")}},dismissButton={TextButton(enabled=!busy,onClick={show=false}){Text("Cancel")}})
}
@Composable
fun TimeChoice(label:String,minute:Int,save:(Int)->Unit){var show by remember{mutableStateOf(false)};SettingRow(label,"%02d:%02d".format(minute/60,minute%60)){show=true};if(show){val picker=androidx.compose.material3.rememberTimePickerState(minute/60,minute%60,true);AlertDialog(onDismissRequest={show=false},title={Text(label)},text={androidx.compose.material3.TimeInput(picker)},confirmButton={TextButton(onClick={save(picker.hour*60+picker.minute);show=false}){Text(localized("Save"))}},dismissButton={TextButton(onClick={show=false}){Text(localized("Cancel"))}})}}
@Composable
fun PauseScreen(pending:PendingPause,model:LauncherViewModel){var remaining by remember(pending){mutableIntStateOf(pending.seconds)};var intention by remember(pending){mutableStateOf("")};LaunchedEffect(pending){while(remaining>0){delay(1000);remaining--}}
    Surface(Modifier.fillMaxSize()){Column(Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        AppIcon(pending.app,model);Text(pending.app.label,Modifier.padding(top=24.dp),style=MaterialTheme.typography.headlineMedium);Text("Look around",Modifier.padding(top=16.dp),style=MaterialTheme.typography.titleLarge)
        if(pending.prompt)Text(localized("What would you like to do in this app?"),Modifier.padding(vertical=16.dp))
        if(pending.requireIntention)OutlinedTextField(intention,{intention=it.take(200)},label={Text("Your intention")},modifier=Modifier.fillMaxWidth())
        if(remaining>0)Text(remaining.toString(),fontSize=48.sp)else TextButton(enabled=!pending.requireIntention||intention.isNotBlank(),onClick={model.confirmPause(intention)}){Text(localized("Open app"))}
        TextButton(onClick=model::cancelPause){Text(localized("Cancel"))}
    }}
}
