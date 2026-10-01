package com.emmanuela.launcher.ui.apps

import com.emmanuela.launcher.ContextSafeOpen
import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.AppPolicy
import com.emmanuela.launcher.data.AppSearch
import com.emmanuela.launcher.data.LaunchableApp
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.data.NotificationMode
import com.emmanuela.launcher.data.PolicyRules
import com.emmanuela.launcher.platform.DigestRepository
import com.emmanuela.launcher.ui.components.AppIcon
import com.emmanuela.launcher.ui.components.Header
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.settings.SettingRow
import com.emmanuela.launcher.ui.settings.SettingsHeading
import com.emmanuela.launcher.ui.settings.ToggleRow


import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContextMenu(app: LaunchableApp, policy: AppPolicy, model: LauncherViewModel, dismiss: () -> Unit,
                   alias: () -> Unit, rules: () -> Unit, notifications: () -> Unit, addToFolder: () -> Unit, appSettings: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val secure = context.getSystemService(KeyguardManager::class.java).isDeviceSecure
    ModalBottomSheet(onDismissRequest = dismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment=Alignment.CenterHorizontally) { AppIcon(app, model); Spacer(Modifier.height(12.dp)); Text(app.label, style=MaterialTheme.typography.titleLarge) }
            Text(app.originalLabel, style = MaterialTheme.typography.bodySmall)
            ToggleRow("Block the app in EmmanueLA", policy.blocked) { checked -> scope.launch { model.changePolicies(setOf(app.packageName)) { it.copy(blocked = checked) } } }
            ToggleRow("Private app", policy.privateApp, secure) { checked -> scope.launch { model.changePolicies(setOf(app.packageName)) { it.copy(privateApp = checked) } } }
            ToggleRow("Hide the app", policy.hidden, secure) { checked -> scope.launch { if (model.changePolicies(setOf(app.packageName)) { it.copy(hidden = checked) }) dismiss() } }
            if (!secure) Text(localized("Set a device PIN/password before hiding apps."), style = MaterialTheme.typography.bodySmall)
            SettingRow("Add to folder", onClick = addToFolder)
            SettingRow("Name and tags", onClick = alias)
            ToggleRow("Launcher notification badges", !policy.badgesMuted) { checked -> scope.launch {
                model.changePolicies(setOf(app.packageName)) { it.copy(badgesMuted = !checked) }
            } }
            Text(localized("Filtering needs Notification Access and acts after arrival. For reliable silence use Android's notification settings."), style = MaterialTheme.typography.bodySmall)
            SettingRow("Timer, schedules and batching", onClick = rules)
            SettingRow("Silence notifications in Android", onClick = notifications)
            SettingRow("App Settings", onClick = appSettings)
            TextButton(onClick = { scope.launch { if (model.changePolicies(setOf(app.packageName)) { AppPolicy() }) dismiss() } }) { Text(localized("Reset app rules")) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun AppManagementScreen(data: LauncherData, apps: List<LaunchableApp>, model: LauncherViewModel,
                        back: () -> Unit, unlock: () -> Unit, edit: (Set<String>) -> Unit, detail:(String)->Unit={}) {
    DisposableEffect(Unit) { model.usageVisible = true; model.refreshAppUsage(); onDispose { model.usageVisible = false } }
    val unlocked by model.hiddenUnlocked.collectAsStateWithLifecycle()
    val usage by model.appUsage.collectAsStateWithLifecycle()
    val allowed by model.usageAllowed.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val visible = remember(apps, query, data.policies, unlocked) {
        AppSearch.filter(apps.filter { unlocked || data.policies[it.packageName]?.hidden != true }, query).distinctBy { it.packageName }
    }
    LaunchedEffect(unlocked, apps, data.policies) { selected = ArrayList(selected.filter { pkg -> apps.any { it.packageName == pkg } && (unlocked || data.policies[pkg]?.hidden != true) }) }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).padding(horizontal = 20.dp)) {
        SettingsHeading("App settings",back,data,model)
        if(data.settings.ui.experience.advanced)Text(localized("Rules apply to every launch through EmmanueLA. Other launchers, notifications and already-open apps are outside EmmanueLA's blocking control."), style = MaterialTheme.typography.bodySmall)
        if(data.settings.ui.experience.advanced)Row {
            TextButton(onClick = {
                ContextSafeOpen(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) { model.error.value = "Usage settings unavailable." }
            }) { Text(if (allowed) "Usage access: enabled" else "Enable usage access") }
            if (!unlocked && data.policies.values.any { it.hidden }) TextButton(onClick = unlock) { Text(localized("Unlock hidden")) }
        }
        if(data.settings.ui.experience.advanced)Row {
            TextButton(onClick = { ContextSafeOpen(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) { model.error.value = "Notification access settings unavailable." } }) { Text(localized("Notification access")) }
            if (Build.VERSION.SDK_INT >= 33) TextButton(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text(localized("Allow summaries")) }
        }
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(localized("Find apps or #tag")) })
        if(data.settings.ui.experience.advanced)Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { selected = ArrayList(visible.map { it.packageName }) }) { Text(localized("Select results")) }
            TextButton(onClick = { selected = arrayListOf() }) { Text(localized("Clear")) }
            TextButton(enabled = selected.isNotEmpty(), onClick = { edit(selected.toSet()) }) { Text("Edit ${selected.size}") }
            BulkActions(selected.toSet(),data,apps,model,edit)
        }
        LazyColumn(Modifier.weight(1f)) {
            items(visible, key = { it.packageName }) { app ->
                val p = data.policies[app.packageName] ?: AppPolicy()
                Row(Modifier.fillMaxWidth().clickable{detail(app.id)}.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if(data.settings.ui.experience.advanced)Checkbox(app.packageName in selected, {checked->selected=ArrayList(if(checked)selected+app.packageName else selected-app.packageName)})
                    Column(Modifier.weight(1f)) {
                        Text(app.label)
                        val minutes = usage?.let { (it[app.packageName] ?: 0L) / 60_000 }
                        if(data.settings.ui.experience.advanced)Text(if (minutes == null) "Usage unavailable" else "$minutes min today" + (p.dailyLimitMinutes?.let { " / $it min" } ?: ""), style = MaterialTheme.typography.bodySmall)
                        val statuses = listOfNotNull(if (p.hidden) "Hidden" else null, if (p.privateApp) "Private" else null, if (p.blocked) "Paused" else null, if (p.scheduleEnabled) "Scheduled" else null,
                            if (p.notifications != NotificationMode.NORMAL) p.notifications.name.lowercase() else null)
                        if (data.settings.ui.experience.advanced&&statuses.isNotEmpty()) Text(statuses.joinToString(" · "), style = MaterialTheme.typography.labelSmall)
                    }

                }
            }
        }
    }
}

@Composable
fun AppPolicyEditor(packages: Set<String>, data: LauncherData, apps: List<LaunchableApp>, model: LauncherViewModel, dismiss: () -> Unit) {
    val identity = packages.sorted().joinToString("|")
    val bulk = packages.size > 1
    val initial = if (bulk) AppPolicy() else data.policies[packages.first()] ?: AppPolicy()
    var hidden by rememberSaveable(identity) { mutableStateOf(initial.hidden) }
    var blocked by rememberSaveable(identity) { mutableStateOf(initial.blocked) }
    var privateApp by rememberSaveable(identity) { mutableStateOf(initial.privateApp) }
    var limit by rememberSaveable(identity) { mutableStateOf(initial.dailyLimitMinutes?.toString() ?: "") }
    var scheduled by rememberSaveable(identity) { mutableStateOf(initial.scheduleEnabled) }
    var start by rememberSaveable(identity) { mutableStateOf(PolicyRules.timeLabel(initial.startMinute)) }
    var end by rememberSaveable(identity) { mutableStateOf(PolicyRules.timeLabel(initial.endMinute)) }
    var days by rememberSaveable(identity) { mutableStateOf(ArrayList(initial.days)) }
    var mode by rememberSaveable(identity) { mutableStateOf(initial.notifications.name) }
    var minutes by rememberSaveable(identity) { mutableIntStateOf(initial.digestMinutes) }
    var applyHide by rememberSaveable(identity) { mutableStateOf(!bulk) }
    var applyBlock by rememberSaveable(identity) { mutableStateOf(!bulk) }
    var applyPrivate by rememberSaveable(identity) { mutableStateOf(!bulk) }
    var applyLimit by rememberSaveable(identity) { mutableStateOf(!bulk) }
    var applySchedule by rememberSaveable(identity) { mutableStateOf(!bulk) }
    var applyNotifications by rememberSaveable(identity) { mutableStateOf(!bulk) }
    var saving by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val secure = context.getSystemService(KeyguardManager::class.java).isDeviceSecure
    val startMinute = PolicyRules.time(start)
    val endMinute = PolicyRules.time(end)
    val valid = (!applyLimit || limit.isBlank() || (limit.toIntOrNull() ?: 0) in 1..1440) &&
        (!applySchedule || !scheduled || (startMinute != null && endMinute != null && days.isNotEmpty())) &&
        (!applyHide || !hidden || secure) && (!applyPrivate || !privateApp || secure)
    val changed = applyHide || applyBlock || applyPrivate || applyLimit || applySchedule || applyNotifications
    AlertDialog(onDismissRequest = { if (!saving) dismiss() }, title = {
        Text(if (bulk) "Manage ${packages.size} apps" else apps.find { it.packageName == packages.first() }?.label ?: "App rules")
    }, text = {
        Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
            if (bulk) Text(localized("Only checked sections are applied. Other rules stay unchanged."))
            if (bulk) ToggleRow("Apply visibility", applyHide, !saving) { applyHide = it }
            ToggleRow("Hide app", hidden, !saving && applyHide && secure) { hidden = it }
            if (bulk) ToggleRow("Apply private access", applyPrivate, !saving) { applyPrivate = it }
            ToggleRow("Require device authentication to open", privateApp, !saving && applyPrivate && secure) { privateApp = it }
            if (!secure) Text(localized("Set a device PIN/password in Android before hiding apps."), style = MaterialTheme.typography.bodySmall)
            Text(localized("Hidden entries are removed from Home, ordinary folders, search and app selectors. Use Hidden Apps with device authentication. This does not hide the app from Android settings."), style = MaterialTheme.typography.bodySmall)
            if (bulk) ToggleRow("Apply pause", applyBlock, !saving) { applyBlock = it }
            ToggleRow("Pause app in EmmanueLA", blocked, !saving && applyBlock) { blocked = it }
            if (bulk) ToggleRow("Apply daily limit", applyLimit, !saving) { applyLimit = it }
            OutlinedTextField(limit, { if (it.length <= 4 && it.all(Char::isDigit)) limit = it }, enabled = !saving && applyLimit,
                label = { Text(localized("Daily minutes · blank removes limit")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(localized("Limits need Usage Access. If it is unavailable, limited apps stay blocked in EmmanueLA until access is restored or the limit is removed."), style = MaterialTheme.typography.bodySmall)
            if (bulk) ToggleRow("Apply schedule", applySchedule, !saving) { applySchedule = it }
            ToggleRow("Pause on schedule", scheduled, !saving && applySchedule) { scheduled = it }
            if (scheduled) {
                Row {
                    OutlinedTextField(start, { if (it.length <= 5) start = it }, label = { Text(localized("From HH:mm")) }, modifier = Modifier.weight(1f), singleLine = true, enabled = applySchedule && !saving)
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(end, { if (it.length <= 5) end = it }, label = { Text(localized("To HH:mm")) }, modifier = Modifier.weight(1f), singleLine = true, enabled = applySchedule && !saving)
                }
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEachIndexed { i, day ->
                        TextButton(enabled = applySchedule && !saving, onClick = { days = ArrayList(if (i + 1 in days) days - (i + 1) else days + (i + 1)) }) {
                            Text(if (i + 1 in days) "✓ $day" else day)
                        }
                    }
                }
                Text(localized("Uses local time. Overnight windows belong to their starting day; equal times mean all day."), style = MaterialTheme.typography.bodySmall)
            }
            if (bulk) ToggleRow("Apply notification rule", applyNotifications, !saving) { applyNotifications = it }
            NotificationMode.entries.forEach { option ->
                Row(Modifier.fillMaxWidth().toggleable(mode == option.name, enabled = applyNotifications && !saving, role = Role.RadioButton,
                    onValueChange = { mode = option.name }), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(mode == option.name, null)
                    Text(when (option) { NotificationMode.NORMAL -> "Normal notifications"; NotificationMode.DISMISS -> "Dismiss after arrival"; NotificationMode.DIGEST -> "Batch into count summary" })
                }
            }
            if (mode == NotificationMode.DIGEST.name) Row(Modifier.horizontalScroll(rememberScrollState())) {
                listOf(15, 30, 60, 120).forEach { n -> TextButton(enabled = applyNotifications && !saving, onClick = { minutes = n }) { Text(if (minutes == n) "✓ ${n}m" else "${n}m") } }
            }
            Text(localized("Filtering needs Notification Access. Notifications may sound or appear briefly before dismissal. Calls, alarms and ongoing notifications are excluded. Batching keeps counts only; original messages/actions are not retained. Delivery is approximate and may be delayed by Android."), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { ContextSafeOpen(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) { failure = "Notification access settings unavailable." } }) { Text(localized("Open notification access")) }
            if (!bulk) TextButton(onClick = { ContextSafeOpen(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packages.first())) { failure = "Notification settings unavailable." } }) { Text(localized("Silence using Android settings")) }
            if (!valid) Text(localized("Check the limit, schedule times, selected days and device lock."), color = MaterialTheme.colorScheme.error)
            failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(enabled = valid && changed && !saving, onClick = {
        saving = true
        scope.launch {
            val ok = model.changePolicies(packages) { old -> old.copy(
                hidden = if (applyHide) hidden else old.hidden,
                blocked = if (applyBlock) blocked else old.blocked,
                privateApp = if (applyPrivate) privateApp else old.privateApp,
                dailyLimitMinutes = if (applyLimit) limit.toIntOrNull() else old.dailyLimitMinutes,
                scheduleEnabled = if (applySchedule) scheduled else old.scheduleEnabled,
                startMinute = if (applySchedule) startMinute ?: old.startMinute else old.startMinute,
                endMinute = if (applySchedule) endMinute ?: old.endMinute else old.endMinute,
                days = if (applySchedule && days.isNotEmpty()) days.toSet() else old.days,
                notifications = if (applyNotifications) NotificationMode.valueOf(mode) else old.notifications,
                digestMinutes = if (applyNotifications) minutes else old.digestMinutes)
            }
            saving = false
            if (ok) dismiss() else failure = "Could not save app rules. Try again."
        }
    }) { Text(if (saving) "Saving…" else "Save") } }, dismissButton = { TextButton(enabled = !saving, onClick = dismiss) { Text(localized("Cancel")) } })
}

@Composable
fun NotificationDigestScreen(apps: List<LaunchableApp>, model: LauncherViewModel, back: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { DigestRepository(context) }
    val counts by repository.counts.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Header("Notification summary", back)
        Text(localized("Counts only, retained for up to seven days when new notifications arrive. Original notifications were dismissed. Open an app to read its messages."), style = MaterialTheme.typography.bodySmall)
        if (counts.isEmpty()) Text(localized("No batched notifications."), Modifier.padding(vertical = 24.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(counts, key = { it.packageName }) { row ->
                val app = apps.find { it.packageName == row.packageName }
                if (app != null) SettingRow(app.label, "${row.count}") { model.launch(app) }
            }
        }
        TextButton(onClick = { scope.launch { repository.clear() } }) { Text(localized("Clear counts")) }
    }
}
