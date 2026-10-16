package com.emmanuela.launcher.ui.mindful

import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emmanuela.launcher.platform.BackgroundRuleAccess
import com.emmanuela.launcher.platform.EnforcementBridge
import com.emmanuela.launcher.platform.EnforcementOutcome
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
    Text("Optional Accessibility access detects apps opened through Recents, notifications and other launchers. Supported browser address controls are read locally; URLs and page contents are not saved.",style=MaterialTheme.typography.bodySmall)
    SettingRow("Background rule access",if(permission)"Allowed by Android"else "Permission required"){
        val details=BackgroundRuleAccess.settingsIntent(context)
        open(if(details.resolveActivity(context.packageManager)!=null)details else Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
    Text(when {
        !rulesEnabled->"Background enforcement is switched off."
        !permission->"Enable EmmanueLA focus rules in Android Accessibility settings to enforce outside the launcher."
        !status.connected->"Permission is allowed; waiting for Android to connect the service. Re-enable the service if this persists."
        else->"Background rule service connected. Android restores the enabled service after reboot and unlock; manufacturer battery restrictions can delay it."
    },style=MaterialTheme.typography.bodySmall)
    status.browserPackage?.let { pkg ->
        Text("Last browser check: $pkg · ${if(status.addressReadable==true)"address readable"else "address unavailable"}",style=MaterialTheme.typography.bodySmall)
    }
    status.issue?.let { Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall) }
    if(status.outcome in setOf(EnforcementOutcome.OVERLAY_SHOWN,EnforcementOutcome.HOME_REQUESTED,EnforcementOutcome.FAILED)){
        Text(when(status.outcome){
            EnforcementOutcome.OVERLAY_SHOWN->"Blocking overlay displayed: ${status.reason.orEmpty()}"
            EnforcementOutcome.HOME_REQUESTED->status.reason?:"Return Home requested"
            else->status.reason?:"Enforcement failed"
        },style=MaterialTheme.typography.bodySmall)
    }
    Text("Website rules require a readable HTTP(S) address in Chrome, Brave, Edge, Firefox or Samsung Internet. Private tabs, hidden bars and browser updates can prevent detection. Opera and other browsers have no adapter. Website usage limits count browser usage, because Android does not provide per-site usage totals. System permission, lock-screen and emergency controls remain accessible.",style=MaterialTheme.typography.bodySmall)
}
