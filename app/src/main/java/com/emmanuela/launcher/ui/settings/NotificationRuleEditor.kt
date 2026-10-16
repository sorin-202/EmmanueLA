package com.emmanuela.launcher.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.platform.NotificationDiagnostics
import com.emmanuela.launcher.platform.notificationAccess
import com.emmanuela.launcher.ui.mindful.TimeChoice
import com.emmanuela.launcher.ui.mindful.rememberFocusTime
import kotlinx.coroutines.launch

@Composable
fun NotificationSpamDefaults(data:LauncherData,model:LauncherViewModel){
    val current=data.settings.ui.experience.spamMode
    val labels=mapOf(SpamMode.OFF to "Off",SpamMode.CONSERVATIVE to "Conservative",SpamMode.STRONG to "Stronger")
    ChoiceRow("Spam filtering",labels.getValue(current),labels.values.toList()){label->
        model.experienceSettings{it.copy(spamMode=labels.entries.first{it.value==label}.key)}
    }
    Text("Local filtering only. Conservative catches repeated promotions; Stronger also catches similar repeated messages and multiple promotional signals. Override the default or allowlist an app in its notification rule. Filtered counts and reasons are always in Notification summary, including when timed summaries are off.",style=MaterialTheme.typography.bodySmall)
}

@Composable
fun NotificationConnectionStatus(){
    val context=androidx.compose.ui.platform.LocalContext.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val status by NotificationDiagnostics.status.collectAsStateWithLifecycle()
    val granted=remember(lifecycle,status){context.notificationAccess()}
    Text(when{!granted->"Notification Access is off";status.connected->"Notification listener connected";else->"Access granted; waiting for Android to connect the listener"},style=MaterialTheme.typography.bodySmall)
    status.issue?.let{Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
    if(granted&&!status.connected)TextButton(onClick={
        try{android.service.notification.NotificationListenerService.requestRebind(android.content.ComponentName(context,com.emmanuela.launcher.EmmanuelaNotificationListener::class.java))}
        catch(_:SecurityException){NotificationDiagnostics.update(false,"Notification Access was revoked. Enable it in Android settings.")}
    }){Text("Reconnect notification listener")}
}

@Composable
fun NotificationRuleEditor(app:LaunchableApp,data:LauncherData,model:LauncherViewModel,open:(Intent)->Unit){
    val policy=data.policies[app.packageName]?:AppPolicy()
    var mode by remember(app.packageName,policy.notifications){mutableStateOf(policy.notifications)}
    var draft by remember(app.packageName,policy.notificationRule){mutableStateOf(policy.notificationRule)}
    var digestMinutes by remember(app.packageName,policy.digestMinutes){mutableIntStateOf(policy.digestMinutes)}
    var words by remember(app.packageName,policy.notificationRule){mutableStateOf(policy.notificationRule.keywords.joinToString("\n"))}
    var spamWords by remember(app.packageName,policy.notificationRule){mutableStateOf(policy.notificationRule.spamKeywords.joinToString("\n"))}
    var allowWords by remember(app.packageName,policy.notificationRule){mutableStateOf(policy.notificationRule.spamAllowWords.joinToString("\n"))}
    val scope=rememberCoroutineScope()
    var busy by remember{mutableStateOf(false)}
    val now=rememberFocusTime().toInstant().toEpochMilli()
    val keywords=words.lines().map{it.trim()}.filter{it.isNotEmpty()}.toSet()
    val spamKeywords=spamWords.lines().map{it.trim()}.filter{it.isNotEmpty()}.toSet()
    val spamAllowWords=allowWords.lines().map{it.trim()}.filter{it.isNotEmpty()}.toSet()
    val valid=listOf(keywords,spamKeywords,spamAllowWords).all{set->set.size<=20&&set.all{it.length<=80}}&&draft.days.isNotEmpty()
    NotificationConnectionStatus()
    TextButton(onClick={model.openDigest.value=true}){Text("View notification summary")}
    ChoiceRow("Delivery",mode.name,NotificationMode.entries.map{it.name}){mode=NotificationMode.valueOf(it)}
    Text("NORMAL leaves notifications unchanged. DISMISS removes matching notifications. DIGEST saves a count for a later summary, without message content.",style=MaterialTheme.typography.bodySmall)
    if(mode==NotificationMode.DIGEST){
        ToggleRow("Enable notification summaries",data.settings.ui.experience.digestEnabled){enabled->model.experienceSettings{it.copy(digestEnabled=enabled)}}
        ChoiceRow("Summary interval (minutes)",NotificationRules.digestMinutes(policy.copy(digestMinutes=digestMinutes,notificationRule=draft),data.settings.ui.experience.digestInterval).toString(),listOf("15","30","60","120")){value->digestMinutes=value.toInt();draft=draft.copy(digestIntervalOverride=true)}
        TextButton(onClick={digestMinutes=30;draft=draft.copy(digestIntervalOverride=false)}){Text("Use default summary interval")}
        Text("Android delivers background summaries approximately; battery restrictions can delay them. Counts remain available in Notification summary.",style=MaterialTheme.typography.bodySmall)
    }
    SectionLabel("Spam")
    val spamOptions=mapOf<SpamMode?,String>(null to "Use default (${data.settings.ui.experience.spamMode.name.lowercase()})",SpamMode.OFF to "Off",SpamMode.CONSERVATIVE to "Conservative",SpamMode.STRONG to "Stronger")
    ChoiceRow("Spam filtering",spamOptions.getValue(draft.spamMode),spamOptions.values.toList()){label->draft=draft.copy(spamMode=spamOptions.entries.first{it.value==label}.key)}
    ToggleRow("Allowlist this app for spam",draft.spamAllowed){draft=draft.copy(spamAllowed=it)}
    Text("The spam allowlist preserves this app's notifications from the classifier. Your explicit delivery rule and temporary suppression still apply. Calls, alarms, media, navigation, system and ongoing service notifications are preserved, together with recognized security/authentication messages.",style=MaterialTheme.typography.bodySmall)
    val spamEnabled=!draft.spamAllowed&&(draft.spamMode?:data.settings.ui.experience.spamMode)!=SpamMode.OFF
    if(spamEnabled){
        OutlinedTextField(spamWords,{spamWords=it.take(1620)},label={Text("Spam keywords, one per line")},modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=4,isError=!valid)
        OutlinedTextField(allowWords,{allowWords=it.take(1620)},label={Text("Allow words, one per line")},modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=4,isError=!valid)
        Text("Allow words take priority over spam keywords. Matching is local and ignores case; up to 20 entries of 80 characters in each list. Message text is never saved.",style=MaterialTheme.typography.bodySmall)
    }
    if(mode!=NotificationMode.NORMAL||spamEnabled){
        val labels=mapOf(NotificationScope.ALWAYS to "Always",NotificationScope.SCHEDULE to "Schedule",NotificationScope.FOCUS to "During Strict Block")
        ChoiceRow("Apply rule",labels.getValue(draft.scope),labels.values.toList()){label->draft=draft.copy(scope=labels.entries.first{it.value==label}.key)}
        if(draft.scope==NotificationScope.SCHEDULE){
            Row(Modifier.fillMaxWidth()){(1..7).forEach{day->
                TextButton(modifier=Modifier.weight(1f),contentPadding=PaddingValues(0.dp),onClick={draft=draft.copy(days=if(day in draft.days)draft.days-day else draft.days+day)}){
                    Text(listOf("M","T","W","T","F","S","S")[day-1],color=if(day in draft.days)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
            }}
            TimeChoice("Start",draft.startMinute){draft=draft.copy(startMinute=it)}
            TimeChoice("End",draft.endMinute){draft=draft.copy(endMinute=it)}
            Text("Equal times mean all day. Overnight rules use the day they start.",style=MaterialTheme.typography.bodySmall)
        }
        if(draft.scope==NotificationScope.FOCUS)Text("Applies when this app belongs to an active Strict Block group in Live the Moment. A Break suspends a continuous block; scheduled Strict Block takes priority.",style=MaterialTheme.typography.bodySmall)
        if(mode!=NotificationMode.NORMAL){
        SectionLabel("Optional delivery keywords")
        OutlinedTextField(words,{words=it.take(1620)},label={Text("One keyword per line")},modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=4,isError=!valid)
        Text("Empty matches all notifications. Otherwise, any keyword can match title or text, ignoring case. Matching stays on this device and message text is never saved. Android may hide sensitive content.",style=MaterialTheme.typography.bodySmall)
        }
    }
    TextButton(enabled=valid&&!busy,onClick={scope.launch{busy=true;try{model.changePolicies(setOf(app.packageName)){current->current.copy(notifications=mode,digestMinutes=digestMinutes,notificationRule=draft.copy(keywords=keywords,spamKeywords=spamKeywords,spamAllowWords=spamAllowWords,suppressUntil=current.notificationRule.suppressUntil))}}finally{busy=false}}}){Text("Save notification rule")}
    SectionLabel("Temporary suppression")
    Text("Dismiss this app's clearable notifications for 30 minutes, regardless of the rule above. Removed notifications are not restored.",style=MaterialTheme.typography.bodySmall)
    val suppressing=policy.notificationRule.suppressUntil>now
    if(suppressing)Text("${(policy.notificationRule.suppressUntil-now+59_999)/60_000} min remaining")
    TextButton(enabled=!busy,onClick={scope.launch{model.changePolicies(setOf(app.packageName)){it.copy(notificationRule=it.notificationRule.copy(suppressUntil=if(suppressing)0 else System.currentTimeMillis()+30*60_000))}}}){Text(if(suppressing)"End suppression" else "Suppress for 30 minutes")}
    SectionLabel("Quiet delivery")
    Text("Filtering happens after arrival and cannot undo sound or heads-up alerts. Set this app's notification channels to Silent in Android for quiet delivery. Calls, alarms, media and ongoing notifications are preserved.",style=MaterialTheme.typography.bodySmall)
    SettingRow("Android notification channels"){open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,app.packageName))}
}
