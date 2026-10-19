@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.emmanuela.launcher.ui.mindful

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.ContextSafeOpen
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
import com.emmanuela.launcher.ui.settings.SettingsSection
import com.emmanuela.launcher.ui.settings.SettingRow
import com.emmanuela.launcher.ui.settings.SliderRow
import com.emmanuela.launcher.ui.settings.ToggleRow


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FocusGroupEditor(id:String,data:LauncherData,apps:List<LaunchableApp>,model:LauncherViewModel,done:()->Unit) {
    val original=data.focusGroups.find{it.id==id}
    val draftSaver=remember(id){androidx.compose.runtime.saveable.listSaver<FocusGroup,String>(
        save={group->listOf(group.name,FocusCodec.encode(listOf(group.copy(name=group.name.ifEmpty{"Draft"}))).toString())},
        restore={saved->runCatching{FocusCodec.decode(org.json.JSONArray(saved[1])).single().copy(name=saved[0])}.getOrNull()}
    )}
    var draft by rememberSaveable(id,stateSaver=draftSaver){mutableStateOf(original?:FocusGroup())}
    var busy by remember{mutableStateOf(false)}
    var blockTab by rememberSaveable(id){mutableStateOf("Apps")}
    var pendingSite by rememberSaveable(id){mutableStateOf("")}
    var pendingKeyword by rememberSaveable(id){mutableStateOf("")}
    var appQuery by rememberSaveable(id){mutableStateOf("")}
    var validationError by remember(id){mutableStateOf<String?>(null)}
    val context=LocalContext.current
    val scope=rememberCoroutineScope();val auth=rememberDeviceAuthentication(data.settings.ui.experience.authentication,model::authenticationChanged)
    fun withPendingEntries(site:String=pendingSite,keyword:String=pendingKeyword):FocusGroup?{
        var next=draft
        if(site.isNotEmpty()){
            val value=FocusWebsiteRules.site(site)
            if(value==null){validationError="Enter a valid website or URL path without a query or fragment, such as youtube.com/shorts. Use URL keywords for query matching.";return null}
            if(value !in next.websites&&next.websites.size>=200){validationError="A group can contain at most 200 websites.";return null}
            next=next.copy(websites=next.websites+value)
        }
        if(keyword.isNotEmpty()){
            val value=FocusWebsiteRules.keyword(keyword)
            if(value==null){validationError="Enter a URL keyword of 1–512 characters without control characters.";return null}
            if(value !in next.keywords&&next.keywords.size>=200){validationError="A group can contain at most 200 URL keywords.";return null}
            next=next.copy(keywords=next.keywords+value)
        }
        return next
    }
    fun commit(delete:Boolean=false){
        val next=if(delete)draft else withPendingEntries()?:return
        if(!delete){
            validationError=when{
                next.name.isBlank()->"Enter a group name."
                next.packages.isEmpty()&&!FocusWebsiteRules.hasWebTargets(next)->"Select an app or enter a website or URL keyword."
                next.schedule&&next.days.isEmpty()->"Select at least one day for the scheduled block."
                else->FocusWebsiteRules.configurationError(next,BrowserAdapters.ids.keys)
            }
            if(validationError!=null)return
            draft=next;pendingSite="";pendingKeyword=""
        }
        val work={scope.launch{
            busy=true
            try{
                val saved=if(delete)model.deleteFocusGroup(next.id)else model.saveFocusGroup(next.copy(breakStartedAt=original?.breakStartedAt?:0L,breakUntil=original?.breakUntil?:0L))
                if(saved)done()else validationError=model.error.value?:"Could not save changes. Please try again."
            }finally{busy=false}
        };Unit}
        if(original?.strict==true&&original.requireAuthentication)auth("Edit strict group"){model.authorizeStrictGroup(draft.id);work()}else work()}
    Text(localized("Choose what to block, then set when you need a little more space."),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(bottom=16.dp))
    OutlinedTextField(draft.name,{draft=draft.copy(name=it.take(40))},label={Text(localized("Group name"))},singleLine=true,modifier=Modifier.fillMaxWidth())
    SectionLabel("What to block")
    ChoiceRow("Blocklist",blockTab,listOf("Apps","Sites","Keywords")){blockTab=it}
    if(blockTab=="Apps"){
        OutlinedTextField(appQuery,{appQuery=it.take(100)},label={Text(localized("Find an app"))},singleLine=true,modifier=Modifier.fillMaxWidth())
        Text("${draft.packages.size} selected",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=8.dp))
        val visibleApps=apps.filter{data.policies[it.packageName]?.hidden!=true}.distinctBy{it.packageName}.filter{appQuery.isBlank()||it.label.contains(appQuery.trim(),ignoreCase=true)}
        Column(Modifier.heightIn(max=220.dp).verticalScroll(rememberScrollState())){visibleApps.forEach{app->
            Row(Modifier.fillMaxWidth().clickable{draft=draft.copy(packages=if(app.packageName in draft.packages)draft.packages-app.packageName else draft.packages+app.packageName)}.padding(vertical=2.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(app.packageName in draft.packages,null);Text(app.label)}}}
        if(visibleApps.isEmpty())Text(localized("No matching apps"),style=MaterialTheme.typography.bodySmall)
    }
    else {
        val sitesTab=blockTab=="Sites"
        OutlinedTextField(if(sitesTab)pendingSite else pendingKeyword,{
            if(sitesTab)pendingSite=it.take(512)else pendingKeyword=it.take(512)
            validationError=null
        },label={Text(localized(if(sitesTab)"Website or URL path" else "URL keyword"))},singleLine=true,modifier=Modifier.fillMaxWidth())
        TextButton(enabled=!busy,onClick={
            if((if(sitesTab)pendingSite else pendingKeyword).isEmpty())validationError=if(sitesTab)"Enter a website or URL path."else "Enter a URL keyword."
            else{
                val next=if(sitesTab)withPendingEntries(keyword="")else withPendingEntries(site="")
                if(next!=null){draft=next;if(sitesTab)pendingSite=""else pendingKeyword="";validationError=null}
            }
        }){Text(localized("Add"))}
        validationError?.let{Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
        (if(blockTab=="Sites")draft.websites else draft.keywords).forEach{entry->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(entry,Modifier.weight(1f));TextButton(onClick={draft=if(blockTab=="Sites")draft.copy(websites=draft.websites-entry)else draft.copy(keywords=draft.keywords-entry)}){Text(localized("Remove"))}}}
        Text(localized(if(sitesTab)"Use youtube.com/shorts to block Shorts while keeping /watch available. A domain by itself blocks the entire site and its subdomains." else "Keywords match anywhere in the exposed URL, including its query. Page content is not inspected."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(localized("Save includes entries you have typed in either tab."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    val websiteEditor=blockTab!="Apps"||FocusWebsiteRules.hasWebTargets(draft)||pendingSite.isNotEmpty()||pendingKeyword.isNotEmpty()
    if(websiteEditor){
        SectionLabel("Browsers")
        Text(localized("Choose where to apply this group's website rules."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(apps.none{it.packageName in BrowserAdapters.ids})Text(localized("No browser with an address adapter is installed."),style=MaterialTheme.typography.bodySmall)
        apps.distinctBy{it.packageName}.filter{it.packageName in BrowserAdapters.ids}.forEach{app->ToggleRow(app.label,app.packageName in draft.browsers){yes->draft=draft.copy(browsers=if(yes)draft.browsers+app.packageName else draft.browsers-app.packageName);validationError=null}}
        SectionLabel("Background access")
        ToggleRow("Enforce outside launcher",data.settings.ui.v2.backgroundRules){yes->model.v2Settings{it.copy(backgroundRules=yes)}}
        FocusEnforcementStatus(data.settings.ui.v2.backgroundRules){intent->
            ContextSafeOpen(context,intent){validationError="This Android settings page is unavailable."}
        }
    }
    SectionLabel("Blocking")
    ToggleRow("Block continuously",draft.blockAlways){draft=draft.copy(blockAlways=it)}
    Text(localized(if(draft.blockAlways)"This group stays blocked until you turn this off or take an allowed Break." else "For immediate blocking, turn this on. Otherwise this group's limits and schedules apply."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    if(!draft.blockAlways)Text("Current daily allowance: ${if(draft.dailyMinutes>0)"${draft.dailyMinutes} minutes (${draft.limitAction} at the limit)"else "no daily limit"}.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    if(original!=null)FocusBreakControls(original,data,model)
    SectionLabel("Optional rules")
    SettingsSection("Usage limits",buildList {
        if(draft.dailyMinutes>0)add("${draft.dailyMinutes} min/day")
        if(draft.sessionMinutes>0)add("${draft.sessionMinutes} min/session")
        if(draft.maxOpens>0)add("${draft.maxOpens} opens/day")
    }.joinToString(" · ").ifEmpty{"No usage limits"}) {
        ToggleRow("Daily limit",draft.dailyMinutes>0){draft=draft.copy(dailyMinutes=if(it)60 else 0)}
        if(draft.dailyMinutes>0)SliderRow("Daily limit (minutes)",draft.dailyMinutes.toFloat(),1f..maxOf(240,draft.dailyMinutes).toFloat()){draft=draft.copy(dailyMinutes=it.toInt())}
        ChoiceRow("When a limit is reached",draft.limitAction,listOf("Block","Warn")){draft=draft.copy(limitAction=it)}
        Text(localized("Block lasts until the daily reset or session cooldown. Warn allows continued use. Continuous and scheduled blocks take priority."),style=MaterialTheme.typography.bodySmall)
        ChoiceRow("Track daily limit",if(draft.perApp)"Per app"else "Group",listOf("Per app","Group")){draft=draft.copy(perApp=it=="Per app")}
        ToggleRow("Session limit",draft.sessionMinutes>0){draft=draft.copy(sessionMinutes=if(it)15 else 0)}
        if(draft.sessionMinutes>0){
            SliderRow("Session limit (minutes)",draft.sessionMinutes.toFloat(),1f..240f){draft=draft.copy(sessionMinutes=it.toInt())}
            SliderRow("Break between sessions (seconds)",draft.cooldownSeconds.toFloat(),1f..maxOf(600,draft.cooldownSeconds).toFloat()){draft=draft.copy(cooldownSeconds=it.toInt())}
        }
        ToggleRow("Opens per day",draft.maxOpens>0){draft=draft.copy(maxOpens=if(it)10 else 0)}
        if(draft.maxOpens>0)SliderRow("Maximum opens",draft.maxOpens.toFloat(),1f..maxOf(100,draft.maxOpens).toFloat()){draft=draft.copy(maxOpens=it.toInt())}
        SliderRow("Warning before limit (minutes)",draft.warningMinutes.toFloat(),0f..30f){draft=draft.copy(warningMinutes=it.toInt())}
        SliderRow("Grace period (minutes)",draft.graceMinutes.toFloat(),0f..30f){draft=draft.copy(graceMinutes=it.toInt())}
        Text(localized("Grace extends daily and session allowances. Background access enables warnings during use."),style=MaterialTheme.typography.bodySmall)
    }
    SettingsSection("Pause & intention",if(draft.pause)"${draft.pauseSeconds}-second pause"+(if(draft.requireIntention)" · intention required"else "") else "No pause before opening") {
        ToggleRow("Pause before opening",draft.pause){draft=draft.copy(pause=it)}
        if(draft.pause){
            SliderRow("Delay (seconds)",draft.pauseSeconds.toFloat(),0f..60f){draft=draft.copy(pauseSeconds=it.toInt())}
            ToggleRow("Enter an intention",draft.requireIntention){draft=draft.copy(requireIntention=it)}
            if(draft.requireIntention)Text(localized("Your intention is used only for this launch and is not saved."),style=MaterialTheme.typography.bodySmall)
            ToggleRow("Increase after repeated opens",draft.escalatingPause){draft=draft.copy(escalatingPause=it)}
        }
        ToggleRow("Mindful prompt",draft.prompt){draft=draft.copy(prompt=it)}
    }
    SettingsSection("Schedules",buildList {
        if(draft.schedule)add("Scheduled block")
        if(draft.windows.isNotEmpty())add("${draft.windows.size} time window${if(draft.windows.size==1)""else "s"}")
    }.joinToString(" · ").ifEmpty{"No scheduled rules"}) {
        ToggleRow("Scheduled block",draft.schedule){draft=draft.copy(schedule=it)}
        if(draft.schedule){
            Row{(1..7).forEach{day->TextButton(modifier=Modifier.weight(1f),contentPadding=PaddingValues(0.dp),onClick={draft=draft.copy(days=if(day in draft.days)draft.days-day else draft.days+day)}){Text(listOf("M","T","W","T","F","S","S")[day-1],color=if(day in draft.days)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)}}}
            TimeChoice("Start",draft.startMinute){draft=draft.copy(startMinute=it)}
            TimeChoice("End",draft.endMinute){draft=draft.copy(endMinute=it)}
        }
        draft.windows.forEachIndexed{index,window->
            HorizontalDivider(Modifier.padding(vertical=12.dp),color=MaterialTheme.colorScheme.outline.copy(alpha=.15f))
            ChoiceRow("Window ${index+1}",window.mode,listOf("Limit","Strict block","Break")){mode->draft=draft.copy(windows=draft.windows.mapIndexed{i,w->if(i==index)w.copy(mode=mode)else w})}
            Row{(1..7).forEach{day->TextButton(modifier=Modifier.weight(1f),contentPadding=PaddingValues(0.dp),onClick={val days=if(day in window.days)window.days-day else window.days+day;if(days.isNotEmpty())draft=draft.copy(windows=draft.windows.mapIndexed{i,w->if(i==index)w.copy(days=days)else w})}){Text(listOf("M","T","W","T","F","S","S")[day-1],color=if(day in window.days)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)}}}
            TimeChoice("Start",window.start){value->draft=draft.copy(windows=draft.windows.mapIndexed{i,w->if(i==index)w.copy(start=value)else w})}
            TimeChoice("End",window.end){value->draft=draft.copy(windows=draft.windows.mapIndexed{i,w->if(i==index)w.copy(end=value)else w})}
            TextButton(onClick={draft=draft.copy(windows=draft.windows.filterIndexed{i,_->i!=index})}){Text(localized("Remove window"))}
        }
        TextButton(enabled=draft.windows.size<32,onClick={draft=draft.copy(windows=draft.windows+FocusWindow())}){Text(localized("+ Add time window"))}
        Text(localized("Scheduled Strict Block wins over Break. Break suspends this group's continuous block and limits. Limits apply inside Limit windows, or all day when none are defined."),style=MaterialTheme.typography.bodySmall)
    }
    SettingsSection("Protection",if(draft.strict&&draft.requireAuthentication)"Device authentication required for changes and Break" else "Changes are not protected by authentication") {
        Text(localized("Protect edits and Break with your device authentication. This does not itself block apps."),style=MaterialTheme.typography.bodySmall)
        ToggleRow("Protect rule changes",draft.strict){yes->if(yes&&draft.requireAuthentication)auth("Enable strict group"){draft=draft.copy(strict=true)}else draft=draft.copy(strict=yes)}
        ToggleRow("Require authentication",draft.requireAuthentication){yes->draft=draft.copy(requireAuthentication=yes)}
    }
    validationError?.let{Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
    Row(Modifier.fillMaxWidth().padding(top=20.dp,bottom=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
        if(original!=null)TextButton(enabled=!busy,onClick={commit(true)}){Text(localized("Delete"))}
        Spacer(Modifier.weight(1f))
        TextButton(enabled=!busy,onClick=done){Text(localized("Cancel"))}
        Button(enabled=!busy,onClick={commit()}){Text(localized("Save"))}
    }
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
        TextButton(enabled=!busy,onClick={change(0)}){Text(localized("End Break"))}
    }else TextButton(enabled=!busy,onClick={show=true}){Text(localized("Take a Break"))}
    if(show)AlertDialog(onDismissRequest={if(!busy)show=false},title={Text(localized("Temporary access"))},text={Column{
        Text(localized("Only this group's rules are suspended. Scheduled Strict Block and other groups still apply. Usage allowances are not reset."))
        Row{listOf(5,10,15).forEach{value->TextButton(onClick={minutes=value}){Text("$value min")}}}
        SliderRow("Minutes",minutes.toFloat(),1f..120f){minutes=it.toInt()}
    }},confirmButton={TextButton(enabled=!busy,onClick={change(minutes)}){Text("Start $minutes-minute Break")}},dismissButton={TextButton(enabled=!busy,onClick={show=false}){Text(localized("Cancel"))}})
}
@Composable
fun TimeChoice(label:String,minute:Int,save:(Int)->Unit){var show by remember{mutableStateOf(false)};SettingRow(label,"%02d:%02d".format(minute/60,minute%60)){show=true};if(show){val picker=androidx.compose.material3.rememberTimePickerState(minute/60,minute%60,true);AlertDialog(onDismissRequest={show=false},title={Text(label)},text={androidx.compose.material3.TimeInput(picker)},confirmButton={TextButton(onClick={save(picker.hour*60+picker.minute);show=false}){Text(localized("Save"))}},dismissButton={TextButton(onClick={show=false}){Text(localized("Cancel"))}})}}
@Composable
fun PauseScreen(pending:PendingPause,model:LauncherViewModel){var remaining by remember(pending){mutableIntStateOf(pending.seconds)};var intention by remember(pending){mutableStateOf("")};LaunchedEffect(pending){while(remaining>0){delay(1000);remaining--}}
    Surface(Modifier.fillMaxSize()){Column(Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        AppIcon(pending.app,model);Text(pending.app.label,Modifier.padding(top=24.dp),style=MaterialTheme.typography.headlineMedium);Text(localized("Look around"),Modifier.padding(top=16.dp),style=MaterialTheme.typography.titleLarge)
        if(pending.prompt)Text(localized("What would you like to do in this app?"),Modifier.padding(vertical=16.dp))
        if(pending.requireIntention)OutlinedTextField(intention,{intention=it.take(200)},label={Text(localized("Your intention"))},modifier=Modifier.fillMaxWidth())
        if(remaining>0)Text(remaining.toString(),fontSize=48.sp)else TextButton(enabled=!pending.requireIntention||intention.isNotBlank(),onClick={model.confirmPause(intention)}){Text(localized("Open app"))}
        TextButton(onClick=model::cancelPause){Text(localized("Cancel"))}
    }}
}
