package com.emmanuela.launcher.ui.mindful

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emmanuela.launcher.platform.BackgroundRuleAccess
import com.emmanuela.launcher.platform.EnforcementBridge
import com.emmanuela.launcher.platform.EnforcementOutcome
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.settings.SettingsSection
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
    Text(localized("Accessibility lets rules apply to apps opened from Recents, notifications and other launchers. Website rules also need this access."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    SettingRow("Background rule access",localized(if(permission)"Allowed by Android"else "Permission required")){
        val details=BackgroundRuleAccess.settingsIntent(context)
        open(if(details.resolveActivity(context.packageManager)!=null)details else Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
    Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.45f),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(localized(when {
                !rulesEnabled->"Background enforcement is off"
                !permission->"Accessibility permission needed"
                !status.connected->"Waiting for Android service connection"
                else->"Background rule service connected"
            }),style=MaterialTheme.typography.titleSmall)
            Text(localized(when {
                !rulesEnabled->"App launch checks inside EmmanueLA still apply. Turn on Enforce outside launcher for website rules and other launch paths."
                !permission->"Open Background rule access and enable EmmanueLA focus rules in Android Accessibility settings."
                !status.connected->"Permission is allowed. Re-enable the service if the connection does not return."
                else->"Rules can be checked outside the launcher. Website detection depends on the browser exposing its address."
            }),style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=4.dp))
        }
    }
    // Problems remain visible without requiring someone to discover the diagnostics panel.
    status.issue?.let { Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp)) }
    if(status.outcome==EnforcementOutcome.FAILED)Text(status.reason?:"Enforcement failed",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
    SettingsSection("Browser compatibility & diagnostics","Full addresses are needed for URL paths and keywords") {
        Text(localized("Website rules require a readable HTTP(S) address in Chrome, Brave, Edge, Firefox or Samsung Internet. Private tabs, hidden bars and browser updates can prevent detection. Opera and other browsers have no adapter."),style=MaterialTheme.typography.bodySmall)
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
