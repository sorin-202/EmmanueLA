package com.emmanuela.launcher.ui.mindful

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emmanuela.launcher.platform.BackgroundRuleAccess
import com.emmanuela.launcher.platform.EnforcementBridge
import com.emmanuela.launcher.platform.EnforcementOutcome
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.settings.SettingRow

@Composable
fun FocusEnforcementStatus(rulesEnabled:Boolean,open:(Intent)->Unit){
    val context=LocalContext.current
    val owner=LocalLifecycleOwner.current
    val status by EnforcementBridge.status.collectAsStateWithLifecycle()
    var permission by remember { mutableStateOf(BackgroundRuleAccess.enabled(context)) }
    DisposableEffect(owner,context){
        val observer=LifecycleEventObserver { _,event ->
            if(event==Lifecycle.Event.ON_RESUME)permission=BackgroundRuleAccess.enabled(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(status.connected){permission=BackgroundRuleAccess.enabled(context)}
    SettingRow("Background rule access",localized(if(permission)"Allowed by Android"else "Permission required")){
        val details=BackgroundRuleAccess.settingsIntent(context)
        open(if(details.resolveActivity(context.packageManager)!=null)details else Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
    val stateLabel=when {
        !rulesEnabled->"Background enforcement is off"
        !permission->"Accessibility permission needed"
        !status.connected->"Waiting for Android service connection"
        else->"Background rule service connected"
    }
    val stateHint=when {
        !rulesEnabled->"App launch checks inside EmmanueLA still apply. Turn on Enforce outside launcher for website rules and other launch paths."
        !permission->"Open Background rule access and enable EmmanueLA focus rules in Android Accessibility settings."
        !status.connected->"Permission is allowed. Re-enable the service if the connection does not return."
        else->"Rules can be checked outside the launcher. Website detection depends on the browser exposing its address."
    }
    Text(localized(stateLabel),style=MaterialTheme.typography.bodySmall,
        color=if(rulesEnabled&&(!permission||!status.connected))MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
    // Problems remain visible without requiring someone to discover the diagnostics panel.
    status.issue?.let { Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp)) }
    if(status.outcome==EnforcementOutcome.FAILED)Text(status.reason?:"Enforcement failed",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
    // Keep setup and the actual service state in view; explanations are available
    // on demand without nesting another large card inside the permission card.
    var expanded by rememberSaveable { mutableStateOf(false) }
    val detailsLabel=localized("Browser compatibility & diagnostics")
    val detailsState=localized(if(expanded)"Expanded"else"Collapsed")
    TextButton(onClick={expanded=!expanded},modifier=Modifier.fillMaxWidth().semantics{
        contentDescription=detailsLabel
        stateDescription=detailsState
    },contentPadding=PaddingValues(vertical=8.dp)) {
        Text(detailsLabel,Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
        Text(if(expanded)"−"else"+",modifier=Modifier.padding(start=12.dp))
    }
    if(expanded)Column {
        Text(localized(stateHint),style=MaterialTheme.typography.bodySmall)
        Text(localized("Accessibility lets rules apply to apps opened from Recents, notifications and other launchers. Website rules also need this access."),style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp))
        Text(localized("Website rules require a readable HTTP(S) address in Chrome, Brave, Edge, Firefox or Samsung Internet. Private tabs, hidden bars and browser updates can prevent detection. Opera and other browsers have no adapter."),style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp))
        Text(localized("Path rules such as youtube.com/shorts and URL path/query keywords need the full address, not just the domain. Browser addresses are read locally; URLs and page contents are not saved."),style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp))
        Text(localized("Website usage limits count browser usage, because Android does not provide per-site usage totals. Android controls service reconnection after reboot and unlock; manufacturer battery restrictions can delay it. System permission, lock-screen and emergency controls remain accessible."),style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp))
        status.browserPackage?.let { pkg ->
            Text("Last browser check: $pkg · ${if(status.addressReadable==true)"address readable"else "address unavailable"}",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp))
        }
        if(status.outcome in setOf(EnforcementOutcome.OVERLAY_SHOWN,EnforcementOutcome.HOME_REQUESTED)){
            Text(if(status.outcome==EnforcementOutcome.OVERLAY_SHOWN)"Blocking overlay displayed: ${status.reason.orEmpty()}"else status.reason?:"Return Home requested",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp))
        }
    }
}
