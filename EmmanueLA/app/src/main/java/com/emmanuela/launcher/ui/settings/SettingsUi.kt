package com.emmanuela.launcher.ui.settings

import com.emmanuela.launcher.ContextSafeOpen
import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.AppSearch
import com.emmanuela.launcher.data.ConfigurationCodec
import com.emmanuela.launcher.data.LaunchableApp
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.platform.lockSetupIntent
import com.emmanuela.launcher.symbol
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.folders.FavoritesEditor


import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Set these only when the project has real public destinations.
object PublicLinks { const val GITHUB = ""; const val SOCIAL = ""; const val STORE = "" }

@Composable
fun SettingRow(label: String, value: String = "", onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(localized(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (value.isNotEmpty()) Text(value, Modifier.widthIn(max = 150.dp).padding(start = 16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable
fun ToggleRow(label: String, value: Boolean, enabled: Boolean = true, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().selectionMarker(value).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(localized(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(value, change, enabled = enabled, modifier = Modifier.semantics { contentDescription = label })
    }
}
@Composable
fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int = 0, save: (Float) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.padding(vertical = 8.dp)) {
        Text("${localized(label)}: ${"%.2f".format(local)}", style = MaterialTheme.typography.bodyLarge)
        Slider(local, { local = it }, valueRange = range, steps = steps, onValueChangeFinished = { save(local) }, modifier = Modifier.semantics { contentDescription = label })
    }
}
@Composable
fun SectionLabel(label: String, prominent:Boolean=false) {
    Text(localized(label), style = if(prominent)MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 28.dp, bottom = 10.dp))
    if(!prominent)HorizontalDivider(color=MaterialTheme.colorScheme.outline.copy(alpha=.12f))
}

@Composable
fun SettingsScreen(page: String, data: LauncherData, apps: List<LaunchableApp>, model: LauncherViewModel,
                   navigate: (String) -> Unit, close: () -> Unit, chooseHome: () -> Unit, editApp: (String) -> Unit) {
    val context = LocalContext.current
    val p = data.settings
    val u = p.ui
    val scope = rememberCoroutineScope()
    val usageAllowed by model.usageAllowed.collectAsStateWithLifecycle()
    val adminAllowed by model.adminAllowed.collectAsStateWithLifecycle()
    var choice by rememberSaveable { mutableStateOf<String?>(null) }
    var gesture by rememberSaveable { mutableStateOf<String?>(null) }
    var permission by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<LauncherData?>(null) }
    var resetConfirm by remember { mutableStateOf(false) }
    var ioBusy by remember { mutableStateOf(false) }
    fun fail(message: String) { model.error.value = message }
    val photos = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val retained = uris.take(50).mapNotNull { uri -> runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); uri.toString()
            }.getOrNull() }
            if (retained.isNotEmpty()) { model.settings { it.copy(wallpapers = retained) };navigate("wallpaper-editor") } else fail("Could not keep access to these photos. Choose local photos from Files.")
        }
    }
    val font = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            ioBusy = true
            try {
                withContext(Dispatchers.IO) {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytesLimited(5_000_000) } ?: error("Missing font")
                    val temp = File(context.filesDir, "font-import.tmp"); temp.writeBytes(bytes)
                    try {
                        Typeface.createFromFile(temp) // Validate before replacing the working font.
                        val target = File(context.filesDir, "custom-font.ttf")
                        check(temp.renameTo(target)) { "Could not replace font" }
                    } finally { temp.delete() }
                }
                model.settings { it.copy(font = "Custom", customFont = System.currentTimeMillis().toString()) }
            } catch (_: Exception) { fail("Could not import font. Choose a valid TTF or OTF file under 5 MB.") }
            finally { ioBusy = false }
        }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            ioBusy = true
            try {
                val snapshot=model.data.value
                val raw = withContext(Dispatchers.IO){ConfigurationCodec.encode(ConfigurationCodec.portable(snapshot))}
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(raw.toByteArray()) } ?: error("Cannot write") }
                fail("Backup saved.")
            } catch (_: Exception) { fail("Could not save backup.") }
            finally { ioBusy = false }
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            ioBusy = true
            try {
                val restored = withContext(Dispatchers.IO) {
                    val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytesLimited(2_000_000).toString(Charsets.UTF_8) } ?: error("Cannot read")
                    ConfigurationCodec.portable(ConfigurationCodec.decode(raw))
                }
                pendingRestore = restored
            } catch (_: Exception) { fail("This file is not a valid EmmanueLA backup. Your settings have not changed.") }
            finally { ioBusy = false }
        }
    }
    fun shareApk() { scope.launch {
        ioBusy = true
        try {
            val file = withContext(Dispatchers.IO) {
                File(context.cacheDir, "shared/EmmanueLA.apk").apply { parentFile?.mkdirs(); File(context.applicationInfo.sourceDir).copyTo(this, overwrite = true) }
            }
            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
            ContextSafeOpen(context, Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"; putExtra(Intent.EXTRA_STREAM, uri);clipData=android.content.ClipData.newRawUri("EmmanueLA APK",uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Share EmmanueLA")) { fail("No sharing app available.") }
        } catch (_: Exception) { fail("Could not share the app.") }
        finally { ioBusy = false }
    } }
    if (page == "favorites") { FavoritesEditor(data, apps, model) { navigate("home-settings") }; return }
    StructuredSettingsContent(page,data,apps,model,navigate,close,chooseHome,editApp,
        { choice=it }, { gesture=it }, { permission=it },
        { photos.launch(arrayOf("image/*")) }, { font.launch(arrayOf("*/*")) },
        { if(!ioBusy) export.launch("EmmanueLA-backup.json") },
        { if(!ioBusy) restore.launch(arrayOf("application/json","text/plain","application/octet-stream")) },
        { resetConfirm=true }, ioBusy,::shareApk)
    if (choice != null) {
        val selectedChoice=when(choice){
            "widget-position"->u.widgetPosition;"widget-font"->u.widgetFont
            "widget-color"->uiColors.entries.firstOrNull{it.value==u.widgetColor}?.key
            "widget-background"->uiColors.entries.firstOrNull{it.value==u.widgetBackground}?.key
            "search-color"->uiColors.entries.firstOrNull{it.value==u.searchColor}?.key
            "search-cursor-color"->uiColors.entries.firstOrNull{it.value==u.searchCursorColor}?.key
            "palette"->u.palette;"search-position"->u.searchPosition;"search-style"->u.searchStyle
            "alphabet-animation"->u.alphabetAnimation;"folder-layout"->u.folderLayout;"nav-weight"->u.bottomBarWeight
            "theme"->p.theme;"alignment"->p.alignment;"count"->p.favoriteCount.toString();"symbol"->p.symbolPlacement;else->p.font
        }

        val values = when (choice) {
            "widget-position" -> listOf("Top", "Center", "Bottom")
            "widget-font" -> listOf("Inherit", "Sans", "Serif", "Monospace")
            "widget-color", "widget-background", "search-color", "search-cursor-color" -> uiColors.keys.toList()
            "left-icon", "right-icon" -> listOf("↑", "⚙", "☎", "♡", "★", "◇", "♫", "▤", "")
            "palette" -> listOf("Neutral", "Sage", "Ocean", "Rose")
            "search-position" -> listOf("Top", "Bottom")
            "search-style" -> listOf("Pill", "Box", "Underline", "Floating Outline", "Filled")
            "alphabet-animation" -> listOf("Wave", "Bubble", "Fade", "None")
            "folder-layout" -> listOf("Adaptive", "List", "Freeform")
            "nav-weight" -> listOf("Light", "Normal", "Medium", "Bold")
 "theme" -> listOf("Light", "Dark", "System", "Custom"); "alignment" -> listOf("Left", "Center", "Right"); "count" -> (0..8).map(Int::toString); "symbol" -> listOf("Top", "Left", "Hidden"); else -> listOf("Sans", "Serif", "Monospace") + if (p.customFont.isNotEmpty()) listOf("Custom") else emptyList() }
        AlertDialog(onDismissRequest = { choice = null }, title = { Text(localized("Choose an option")) }, text = {
            Column { values.forEach { value -> TextButton(onClick = {
                when (choice) {
                    "widget-position" -> model.uiSettings { it.copy(widgetPosition = value) }
                    "widget-font" -> model.uiSettings { it.copy(widgetFont = value) }
                    "widget-color" -> model.uiSettings { it.copy(widgetColor = uiColors.getValue(value)) }
                    "widget-background" -> model.uiSettings { it.copy(widgetBackground = uiColors.getValue(value)) }
                    "search-color" -> model.uiSettings { it.copy(searchColor = uiColors.getValue(value)) }
                    "search-cursor-color" -> model.uiSettings { it.copy(searchCursorColor = uiColors.getValue(value)) }
                    "left-icon" -> model.uiSettings { it.copy(leftSymbol = value) }
                    "right-icon" -> model.uiSettings { it.copy(rightSymbol = value) }
                    "palette" -> model.uiSettings { it.copy(palette = value) }
                    "search-position" -> model.uiSettings { it.copy(searchPosition = value) }
                    "search-style" -> model.uiSettings { it.copy(searchStyle = value) }
                    "alphabet-animation" -> model.uiSettings { it.copy(alphabetAnimation = value) }
                    "folder-layout" -> model.uiSettings { it.copy(folderLayout = value) }
                    "nav-weight" -> model.uiSettings { it.copy(bottomBarWeight = value) }
                    "theme" -> model.settings { it.copy(theme = value) }
                    "alignment" -> model.settings { it.copy(alignment = value) }
                    "count" -> model.settings { it.copy(favoriteCount = value.toInt()) }
                    "symbol" -> model.settings { it.copy(symbolPlacement = value) }
                    "font" -> model.settings { it.copy(font = value) }
                }; choice = null
            }, modifier = Modifier.fillMaxWidth().selectionMarker(value==selectedChoice)) { Text(localized(value)) } } }
        }, confirmButton = { TextButton(onClick = { choice = null }) { Text(localized("Cancel")) } })
    }
    if (gesture != null) ActionPicker(apps, { gesture = null },when(gesture){
        "Up"->p.swipeUp;"Down"->p.swipeDown;"Left"->p.swipeLeft;"Right"->p.swipeRight;"Double tap"->p.doubleTapAction;"Triple tap"->u.tripleTapAction
        "Hold"->u.v2.holdAction;"Weather"->u.v2.weatherAction;"Clock"->u.clockAction;"Date"->u.dateAction;"Battery"->u.batteryAction;"Screen time"->u.screenTimeAction
        "Bottom left"->u.leftShortcut;"Bottom right"->u.rightShortcut;else->"none"
    }) { action ->
        when (gesture) {
            "Up" -> model.settings { it.copy(swipeUp = action) }; "Down" -> model.settings { it.copy(swipeDown = action) }
            "Left" -> model.settings { it.copy(swipeLeft = action) }; "Right" -> model.settings { it.copy(swipeRight = action) }
            "Double tap" -> model.settings { it.copy(doubleTapAction = action) }
            "Hold" -> model.v2Settings { it.copy(holdAction=action) }
            "Weather" -> model.v2Settings { it.copy(weatherAction=action) }
            "Triple tap" -> model.uiSettings { it.copy(tripleTapAction = action) }
            "Clock" -> model.uiSettings { it.copy(clockAction = action) }
            "Date" -> model.uiSettings { it.copy(dateAction = action) }
            "Battery" -> model.uiSettings { it.copy(batteryAction = action) }
            "Screen time" -> model.uiSettings { it.copy(screenTimeAction = action) }
            "Bottom left" -> model.uiSettings { it.copy(leftShortcut = action) }
            "Bottom right" -> model.uiSettings { it.copy(rightShortcut = action) }
        }; gesture = null
        if (action == "lock" && !adminAllowed) permission = "lock"
    }
    if (permission != null) AlertDialog(onDismissRequest = { permission = null }, title = { Text(if (permission == "usage") "Enable screen time" else "Enable screen locking") },
        text = { Text(if (permission == "usage") "Allow Usage Access for EmmanueLA in Android settings. EmmanueLA reads screen, lock and app activity events to estimate screen time and app usage limits. Nothing is uploaded."
            else "Allow EmmanueLA as a device administrator for screen locking only. Android may require your PIN after locking. Revoke access in Gestures before uninstalling.") },
        confirmButton = { TextButton(onClick = { val intent = if (permission == "usage") Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS) else context.lockSetupIntent(); permission = null
            ContextSafeOpen(context, intent) { fail("This settings page is unavailable.") }
        }) { Text(localized("Open Android settings")) } }, dismissButton = { TextButton(onClick = { permission = null }) { Text(localized("Cancel")) } })
    if (pendingRestore != null || resetConfirm) AlertDialog(onDismissRequest = { pendingRestore = null; resetConfirm = false },
        title = { Text(if (resetConfirm) "Reset EmmanueLA?" else "Restore backup?") },
        text = { Text(if(resetConfirm) "This removes all launcher configuration, rules, folders, note, notification counts, weather cache, imported font and photo grants. Installed apps and Android permission grants remain unchanged." else "This replaces aliases, tags, folders, favorites, app rules, gestures, settings and note. Installed apps are unaffected.") },
        confirmButton = { TextButton(onClick = {
            val replacement = pendingRestore ?: LauncherData(); scope.launch {
                if (if(resetConfirm) model.factoryReset() else model.restore(replacement)) { pendingRestore = null; resetConfirm = false; navigate("settings") }
            }
        }) { Text(localized("Confirm")) } }, dismissButton = { TextButton(onClick = { pendingRestore = null; resetConfirm = false }) { Text(localized("Cancel")) } })
}

private fun java.io.InputStream.readBytesLimited(max: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) { val count = read(buffer); if (count < 0) break; require(output.size() + count <= max); output.write(buffer, 0, count) }
    return output.toByteArray()
}
fun actionName(action: String, apps: List<LaunchableApp>): String = when (action) {
    "none" -> "No action"; "apps" -> "All Apps"; "folders" -> "Folders"; "settings" -> "Settings"; "lock" -> "Lock screen"; "camera" -> "Camera"; "phone" -> "Phone"; "clock" -> "Clock"; "calendar" -> "Calendar"; "battery" -> "Battery settings"; "wellbeing" -> "Digital Wellbeing"; "usage" -> "Usage access"; "home" -> "Home"; "search" -> "Search"; "flashlight" -> "Flashlight"; "dnd" -> "Do not disturb"; "airplane" -> "Airplane mode"; "notifications" -> "Notification settings"
    else -> apps.find { it.id == action.removePrefix("app:") }?.label ?: "Unavailable app"
}
@Composable
fun ActionPicker(apps: List<LaunchableApp>, dismiss: () -> Unit, currentAction:String="",select: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var showApps by rememberSaveable { mutableStateOf(false) }
    val groups=remember{listOf(
        "Launcher" to listOf("home","apps","folders","search","settings"),
        "System" to listOf("flashlight","dnd","airplane","wellbeing","notifications","lock","battery","clock","calendar","camera","phone"),
        "Other" to listOf("none")
    )}
    AlertDialog(onDismissRequest = dismiss, title = { Text(localized(if(showApps)"Open app" else "Choose action")) }, text = {
        Column {
            if(showApps)OutlinedTextField(query,{query=it},label={Text(localized("Search apps"))},singleLine=true)
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                if(showApps)items(AppSearch.filter(apps,query),key={it.id}){app->
                    Box(Modifier.selectionMarker(currentAction=="app:"+app.id)){SettingRow(app.label){select("app:"+app.id)}}
                }else{
                    item{SectionLabel("Launcher")}
                    items(groups[0].second){action->Box(Modifier.selectionMarker(currentAction==action)){SettingRow(actionName(action,apps)){select(action)}}}
                    item{SectionLabel("Apps");SettingRow("Open app…"){showApps=true}}
                    item{SectionLabel("System")}
                    items(groups[1].second){action->Box(Modifier.selectionMarker(currentAction==action)){SettingRow(actionName(action,apps)){select(action)}}}
                    item{SectionLabel("Other");Box(Modifier.selectionMarker(currentAction=="none")){SettingRow("None"){select("none")}}}
                }
            }
        }
    }, confirmButton = { TextButton(onClick = {if(showApps){showApps=false;query=""}else dismiss()}) { Text(localized(if(showApps)"Back" else "Cancel")) } })
}

val uiColors = linkedMapOf("Auto / transparent" to 0L, "White" to 0xFFFFFFFFL, "Black" to 0xFF000000L, "Sage" to 0xFFA6C9A8L, "Ocean" to 0xFF9EC3E6L, "Rose" to 0xFFE8A9A9L, "Gold" to 0xFFEACB8EL, "Violet" to 0xFFC5A9DEL)
