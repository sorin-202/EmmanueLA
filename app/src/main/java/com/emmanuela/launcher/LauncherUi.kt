@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.emmanuela.launcher

import com.emmanuela.launcher.data.AppMetadata
import com.emmanuela.launcher.data.AppPolicy
import com.emmanuela.launcher.data.LaunchableApp
import com.emmanuela.launcher.platform.lockDevice
import com.emmanuela.launcher.platform.lockSetupIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import com.emmanuela.launcher.ui.navigation.motionDuration
import com.emmanuela.launcher.ui.navigation.motionEasing
import com.emmanuela.launcher.ui.navigation.PagePolicy
import com.emmanuela.launcher.platform.SystemDestinations
import com.emmanuela.launcher.ui.home.BlankScreen
import com.emmanuela.launcher.platform.rememberDeviceAuthentication
import com.emmanuela.launcher.ui.appearance.EmmanuelaTheme
import com.emmanuela.launcher.ui.appearance.WallpaperEditor
import com.emmanuela.launcher.ui.apps.AllApps
import com.emmanuela.launcher.ui.apps.AppContextMenu
import com.emmanuela.launcher.ui.apps.AppManagementScreen
import com.emmanuela.launcher.ui.apps.AppPolicyEditor
import com.emmanuela.launcher.ui.apps.IndexedAppList
import com.emmanuela.launcher.ui.apps.NotificationDigestScreen
import com.emmanuela.launcher.ui.components.Header
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.folders.AppMetadataEditor
import com.emmanuela.launcher.ui.folders.FolderEditor
import com.emmanuela.launcher.ui.folders.FolderScreen
import com.emmanuela.launcher.ui.folders.FolderUnlockDialog
import com.emmanuela.launcher.ui.folders.FoldersScreen
import com.emmanuela.launcher.ui.home.HomeScreen
import com.emmanuela.launcher.ui.home.WidgetLayoutScreen
import com.emmanuela.launcher.ui.home.openDigitalWellbeing
import com.emmanuela.launcher.ui.mindful.PauseScreen
import com.emmanuela.launcher.ui.navigation.LauncherSurface
import com.emmanuela.launcher.ui.navigation.LauncherSurfaceHost
import com.emmanuela.launcher.ui.settings.SettingsRoutes
import com.emmanuela.launcher.ui.settings.SettingsScreen


import android.content.Intent
import android.app.Activity
import android.app.KeyguardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

val folderSymbols = linkedMapOf("phone" to "☎", "mail" to "✉", "cloud" to "☁", "sun" to "☀", "moon" to "☾", "triangle" to "△", "circle" to "○", "plus" to "+", "flag" to "⚑", "gear" to "⚙", "air" to "✈", "check" to "✓", "folder" to "▤", "work" to "▣", "star" to "★", "heart" to "♡", "calendar" to "▦", "music" to "♫", "games" to "◇", "book" to "▥")
val folderColors = listOf(0L, 0xFFE8A9A9, 0xFFEACB8E, 0xFFA6C9A8, 0xFF9EC3E6, 0xFFC5A9DE, 0xFFF5F5F5, 0xFF777777, 0xFFFFA96B, 0xFF7AD7D0, 0xFFF3D1DC, 0xFFD4E6F1, 0xFFE2F0CB, 0xFFEADCF8, 0xFFFFDAC1, 0xFFB5EAD7, 0xFFC7CEEA, 0xFFF8E8B8)
fun symbol(key: String) = folderSymbols[key] ?: "▤"
fun ContextSafeOpen(context: android.content.Context, intent: Intent, onFailure: () -> Unit) {
    try { context.startActivity(intent) }
    catch (_: android.content.ActivityNotFoundException) { onFailure() }
    catch (_: SecurityException) { onFailure() }
}

@Composable
fun LauncherApp(model: LauncherViewModel, homeEpoch: Int, now: Long, battery: Int,
                chooseHome: () -> Unit, bars: (Boolean, Boolean) -> Unit) {
    val data by model.data.collectAsStateWithLifecycle()
    val apps by model.apps.collectAsStateWithLifecycle()
    val unlockedFolders by model.unlockedFolders.collectAsStateWithLifecycle()
    val unlocked by model.hiddenUnlocked.collectAsStateWithLifecycle()
    val pause by model.pendingPause.collectAsStateWithLifecycle()
    val deviceAuth=rememberDeviceAuthentication(data.settings.ui.experience.authentication,model::authenticationChanged)
    val blockedMessage by model.blockedMessage.collectAsStateWithLifecycle()
    val openDigest by model.openDigest.collectAsStateWithLifecycle()
    val visibleApps = remember(apps, data.policies) { apps.filter { data.policies[it.packageName]?.hidden != true } }
    val drawerIndex by model.drawerIndex.collectAsStateWithLifecycle()
    val ready by model.ready.collectAsStateWithLifecycle()
    val loading by model.loading.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    var overlay by rememberSaveable { mutableStateOf("") }
    var editor by rememberSaveable { mutableStateOf<String?>(null) }
    var lockExplain by rememberSaveable { mutableStateOf(false) }
    var handledEpoch by rememberSaveable { mutableIntStateOf(homeEpoch) }
    var surface by rememberSaveable { mutableStateOf(LauncherSurface.HOME) }
    var direction by remember { mutableIntStateOf(1) }
    var appEditor by rememberSaveable { mutableStateOf<String?>(null) }
    var appMenu by rememberSaveable { mutableStateOf<String?>(null) }
    var policyPackages by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var folderUnlock by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingFolderLaunch by rememberSaveable{mutableStateOf<String?>(null)}
    var pendingShortcut by remember{mutableStateOf<LauncherViewModel.ProtectedShortcut?>(null)}
    var pendingAddApp by rememberSaveable { mutableStateOf<String?>(null) }
    var quickAddApp by rememberSaveable { mutableStateOf<String?>(null) }
    val uiScope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(model){model.successfulLaunches.collect{if(overlay.startsWith("folder:")&&model.data.value.settings.ui.experience.folderClose)overlay=""}}
    fun navigate(target: LauncherSurface, motion: Int = 1) {
        focus.clearFocus(); keyboard?.hide(); direction = motion; surface = PagePolicy.resolve(target,data.settings)
    }
    fun editApp(id: String) { focus.clearFocus(); keyboard?.hide(); appMenu = id }
    fun unlockHidden(destination:String) { if(unlocked||data.policies.values.none{it.hidden}){overlay=destination;return};deviceAuth("Private Space"){model.hiddenUnlocked.value=true;overlay=destination} }
    fun secureLaunch(app:LaunchableApp, platformIntent:Intent?=null) {
        val protected=model.data.value.folders.firstOrNull{it.isProtected&&it.id !in model.unlockedFolders.value&&it.apps.any{id->id.substringBefore('/')==app.packageName}}
        if(protected!=null){pendingFolderLaunch=app.id;pendingShortcut=platformIntent?.let{LauncherViewModel.ProtectedShortcut(app,Intent(it))};pendingAddApp=null;folderUnlock=protected.id;return}
        val policy=model.data.value.policies[app.packageName]
        fun openAuthorized(){if(platformIntent!=null)model.launchPlatformAuthorized(app.packageName,platformIntent)else if(policy?.privateApp==true)model.launchPrivateAuthorized(app.id)else model.launch(app)}
        if(policy?.hidden==true&&!model.hiddenUnlocked.value){deviceAuth("Hidden app"){model.hiddenUnlocked.value=true;openAuthorized()};return}
        if(policy?.privateApp==true)deviceAuth("Open ${app.label}",::openAuthorized)
        else if(platformIntent!=null)model.launchPlatform(platformIntent)else model.launch(app)
    }
    val privateRequest by model.privateRequest.collectAsStateWithLifecycle()
    LaunchedEffect(privateRequest,apps,ready){val pkg=privateRequest;if(pkg!=null&&ready){val app=apps.firstOrNull{it.packageName==pkg};if(app!=null){model.privateRequest.value=null;secureLaunch(app)}}}
    val currentSecureShortcut by rememberUpdatedState<(LauncherViewModel.ProtectedShortcut)->Unit>({request->secureLaunch(request.app,request.intent)})
    LaunchedEffect(model){model.protectedShortcuts.collect{currentSecureShortcut(it)}}
    LaunchedEffect(unlocked, data.policies) {
        if (!unlocked) {
            
            if (appMenu?.let { data.policies[it.substringBefore('/')]?.hidden } == true) appMenu = null
            if (appEditor?.let { data.policies[it.substringBefore('/')]?.hidden } == true) appEditor = null
            policyPackages = ArrayList(policyPackages.filter { data.policies[it]?.hidden != true })
        }
    }
    LaunchedEffect(openDigest) { if (openDigest) { overlay = "digest"; model.openDigest.value = false } }
    fun openFolder(id: String, addApp: String? = null) {
        pendingFolderLaunch=null;pendingShortcut=null
        val folder = data.folders.find { it.id == id } ?: return
        if (folder.isProtected && id !in unlockedFolders) { folderUnlock = id; pendingAddApp = addApp }
        else if (addApp != null) uiScope.launch { model.addToFolder(id, addApp) }
        else overlay = "folder:$id"
    }
    LaunchedEffect(unlockedFolders, data.folders) {
        if (overlay.startsWith("folder:")) {
            val folder = data.folders.find { it.id == overlay.removePrefix("folder:") }
            if (folder != null && folder.isProtected && folder.id !in unlockedFolders) overlay = ""
        }
    }
    LaunchedEffect(unlockedFolders, editor) {
        val edited = data.folders.find { it.id == editor }
        if (edited != null && edited.isProtected && edited.id !in unlockedFolders) editor = null
    }
    val cameraPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted)model.toggleTorch{model.error.value="Camera access is unavailable."}else model.error.value="Allow camera access in System → Permissions to use the flashlight."}
    fun lock() { if (!context.lockDevice()) lockExplain = true }
    fun action(key: String) {
        when {
            key == "home" -> navigate(LauncherSurface.HOME,-1)
            key == "search" -> {model.searchEpoch.value++;navigate(LauncherSurface.APPS)}
            key == "dnd" -> model.toggleDnd{if(!SystemDestinations.open(context,listOf(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS),SystemDestinations.appDetails(context))))model.error.value="DND access settings unavailable."}
            key == "airplane" -> ContextSafeOpen(context,Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)){model.error.value="Airplane settings unavailable."}
            key == "flashlight" -> model.toggleTorch{cameraPermission.launch(android.Manifest.permission.CAMERA)}
            key == "notifications" -> ContextSafeOpen(context,Intent(Settings.ACTION_SETTINGS)){model.error.value="Android settings unavailable."}
            key == "drawer" -> navigate(PagePolicy.drawer(data.settings))
            key == "apps" -> navigate(LauncherSurface.APPS)
            key == "folders" -> navigate(LauncherSurface.FOLDERS)
            key == "settings" -> overlay = "settings"
            key == "lock" -> lock()
            key == "camera" -> model.launchPlatform(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            key == "phone" -> model.launchPlatform(Intent(Intent.ACTION_DIAL))
            key == "clock" -> model.launchPlatform(Intent(AlarmClock.ACTION_SHOW_ALARMS))
            key == "calendar" -> model.launchPlatform(Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time/$now")))
            key == "battery" -> if(!SystemDestinations.battery(context))model.error.value="Battery settings unavailable."
            key == "wellbeing" -> if(!SystemDestinations.wellbeing(context,data.settings.ui.experience.wellbeingComponent)){overlay="wellbeing-target";model.error.value="Choose your phone’s Digital Wellbeing destination."}
            key == "usage" -> ContextSafeOpen(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) { model.error.value = "Usage access settings unavailable." }
            key == "weather-web" -> data.settings.ui.experience.weatherWebsite.takeIf{it.isNotBlank()}?.let{model.launchPlatform(Intent(Intent.ACTION_VIEW,Uri.parse(it)))}
            key.startsWith("app:") -> apps.find { it.id == key.removePrefix("app:") }?.let({app -> secureLaunch(app)})
        }
    }
    LaunchedEffect(homeEpoch) {
        if (handledEpoch != homeEpoch) {
            model.cancelPause();overlay = ""; editor = null; appEditor = null; appMenu = null; policyPackages = arrayListOf(); model.hiddenUnlocked.value = false; model.unlockedFolders.value = emptySet(); folderUnlock = null; pendingFolderLaunch=null;pendingShortcut=null;pendingAddApp=null;quickAddApp = null; lockExplain = false; navigate(LauncherSurface.HOME, -1); handledEpoch = homeEpoch
        }
    }
    LaunchedEffect(overlay, editor, appEditor, appMenu, policyPackages) {
        if (overlay.isNotEmpty() || editor != null || appEditor != null || appMenu != null || policyPackages.isNotEmpty()) { focus.clearFocus(); keyboard?.hide() }
    }
    LaunchedEffect(data.settings.ui.appListEnabled,data.settings.ui.v2.foldersEnabled,data.settings.ui.experience.homeEnabled,data.settings.ui.experience.homeAlphabet){val resolved=PagePolicy.resolve(surface,data.settings);if(resolved!=surface)navigate(resolved)}
    LaunchedEffect(data.settings.showScreenTime) { model.refreshUsage() }
    LaunchedEffect(error) { error?.let { snackbar.showSnackbar(it); if (model.error.value == it) model.error.value = null } }
    EmmanuelaTheme(data.settings.copy(notificationBar=data.settings.notificationBar || surface!=LauncherSurface.HOME || (overlay.isNotEmpty()&&overlay!="widget-layout")), bars) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = if(data.settings.ui.experience.edgeToEdge)WindowInsets(0, 0, 0, 0)else WindowInsets.safeDrawing) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
                if (!ready) Column(Modifier.align(Alignment.Center).padding(24.dp)) {
                    Text(localized("EmmanueLA"), style = MaterialTheme.typography.headlineLarge)
                    Text(localized("Loading your space…"))
                    TextButton(onClick = model::observe) { Text(localized("Retry")) }
                } else {
                    LauncherSurfaceHost(surface, direction, data.settings.ui.v2.gesturesEnabled && overlay.isEmpty() && editor == null && appEditor == null && appMenu == null && policyPackages.isEmpty() && folderUnlock == null && quickAddApp == null,
                        { gesture -> action(when (gesture) { "up" -> data.settings.swipeUp; "down" -> data.settings.swipeDown; "left" -> data.settings.swipeLeft; else -> data.settings.swipeRight }) },
                        { target, motion -> navigate(target, motion) }, data.settings.ui.experience) { current, settled ->
                        when (current) {
                            LauncherSurface.BLANK -> BlankScreen(data,model){overlay="settings"}
                            LauncherSurface.HOME -> HomeScreen(data, visibleApps, model, now, battery, ::action,
                                { overlay = "settings" }, { overlay = "favorites" }, ::editApp, {app -> secureLaunch(app)})
                            LauncherSurface.APPS -> AllApps(drawerIndex, loading, data.settings,
                                settled && overlay.isEmpty() && editor == null && appEditor == null && appMenu == null && policyPackages.isEmpty() && folderUnlock == null && quickAddApp == null,
                                model, {app -> secureLaunch(app)}, model::rememberTags, { navigate(LauncherSurface.HOME, -1) },
                                { navigate(LauncherSurface.FOLDERS) }, { overlay = "settings" }, ::editApp, { overlay="hidden" }, { openFolder(it) })
                            LauncherSurface.FOLDERS -> FoldersScreen(data, { navigate(LauncherSurface.HOME, -1) },
                                { navigate(LauncherSurface.APPS) }, { openFolder(it) }, { editor = "new" }, model::placeFolder,{overlay="settings"},model::placeDenseFolder)
                        }
                    }
                    val activeFolder=data.folders.find { it.id == overlay.removePrefix("folder:") }
                    var lastFolder by remember { mutableStateOf<com.emmanuela.launcher.data.AppFolder?>(null) }
                    SideEffect { if(overlay.startsWith("folder:") && activeFolder!=null)lastFolder=activeFolder }
                    val folderDuration=if(data.settings.ui.experience.folderAnimation=="Off")0 else data.settings.ui.experience.motionDuration()
                    AnimatedVisibility(overlay.startsWith("folder:"),
                        enter=fadeIn(tween(folderDuration))+slideInHorizontally(tween(folderDuration,easing=data.settings.ui.experience.motionEasing())){it/8},
                        exit=fadeOut(tween(folderDuration))+slideOutHorizontally(tween(folderDuration,easing=data.settings.ui.experience.motionEasing())){it/8}) {
                        val folder = activeFolder ?: lastFolder
                        Surface(Modifier.fillMaxSize()) {
                            if (folder != null && (!folder.isProtected || folder.id in unlockedFolders)) FolderScreen(folder, visibleApps, { app -> secureLaunch(app) }, { overlay = "" }, { editor = folder.id }, ::editApp, data.settings.ui.folderAppIcons, model)
                            else Column { Header("Folder", { overlay = "" }); Text(localized("This folder is no longer available.")) }
                        }
                    }
                    if (overlay == "widget-layout") Surface(Modifier.fillMaxSize()) {
                        WidgetLayoutScreen(data,model,now,battery){overlay="home-settings"}
                    } else if (overlay == "wallpaper-editor") Surface(Modifier.fillMaxSize()) {
                        WallpaperEditor(data,model){overlay="appearance"}
                    } else if (overlay == "management") Surface(Modifier.fillMaxSize()) {
                        AppManagementScreen(data, apps, model, { overlay = "apps-hub" }, { unlockHidden("management") }, { policyPackages = ArrayList(it) }, {overlay="app-detail:$it|$overlay"})
                    } else if (overlay == "digest") Surface(Modifier.fillMaxSize()) {
                        NotificationDigestScreen(visibleApps, model) { overlay = "settings" }
                    } else if (overlay.isNotEmpty() && !overlay.startsWith("folder:")) Surface(Modifier.fillMaxSize()) {
                        SettingsScreen(overlay, data, if(unlocked||overlay in listOf("hidden","hidden-manager","blocked-apps","private-manager","private-apps"))apps else visibleApps, model, { if(it.startsWith("launch-app:"))apps.find{app->app.id==it.removePrefix("launch-app:")}?.let{app->secureLaunch(app)}else if(it.startsWith("open-folder:"))openFolder(it.substringAfter(':'))else if(it in listOf("private-apps","private-manager"))deviceAuth("Private Apps"){overlay=it}else overlay=it }, {overlay=""}, chooseHome, {overlay="app-detail:$it|$overlay"})
                    }
                    val editedFolder = data.folders.find { it.id == editor }
                    if (editor != null && (editedFolder == null || !editedFolder.isProtected || editedFolder.id in unlockedFolders)) FolderEditor(editedFolder, visibleApps, model,
                        { editor = null }, { editor = null; overlay = "" })
                }
                appMenu?.let { id ->
                    val app = apps.find { it.id == id }
                    if (app != null && (unlocked || data.policies[app.packageName]?.hidden != true)) AppContextMenu(app, data.policies[app.packageName] ?: AppPolicy(), model,
                        { appMenu = null }, { appMenu = null; appEditor = id },
                        { appMenu = null; policyPackages = arrayListOf(app.packageName) },
                        { appMenu = null; ContextSafeOpen(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, app.packageName)) { model.error.value = "Notification settings unavailable." } },
                        { appMenu = null; quickAddApp = id },
                        { appMenu = null; ContextSafeOpen(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))) { model.error.value = "App settings unavailable." } })
                }
                quickAddApp?.let { appId -> AlertDialog(onDismissRequest = { quickAddApp = null }, title = { Text(localized("Add to folder")) }, text = {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        if (data.folders.isEmpty()) item { Text(localized("Create a folder in Organized Folders first.")) }
                        items(data.folders, key = { it.id }) { folder -> TextButton(onClick = { quickAddApp = null; openFolder(folder.id, appId) }) { Text(folder.name) } }
                    }
                }, confirmButton = { TextButton(onClick = { quickAddApp = null }) { Text(localized("Cancel")) } }) }
                folderUnlock?.let { id -> FolderUnlockDialog(id, model, { folderUnlock = null; pendingAddApp = null;pendingFolderLaunch=null;pendingShortcut=null }) {
                    folderUnlock = null
                    val pendingLaunch=pendingFolderLaunch;pendingFolderLaunch=null
                    val shortcut=pendingShortcut;pendingShortcut=null
                    val add = pendingAddApp; pendingAddApp = null
                    if(shortcut!=null)secureLaunch(shortcut.app,shortcut.intent)
                    else if(pendingLaunch!=null)apps.find{it.id==pendingLaunch}?.let{secureLaunch(it)}
                    else if (add != null) uiScope.launch { model.addToFolder(id, add) } else overlay = "folder:$id"
                } }
                val editablePackages = policyPackages.filter { unlocked || data.policies[it]?.hidden != true }.toSet()
                if (editablePackages.isNotEmpty()) AppPolicyEditor(editablePackages, data, apps, model) { policyPackages = arrayListOf() }
                blockedMessage?.let { message -> AlertDialog(onDismissRequest = { model.blockedMessage.value = null }, title = { Text(localized("App unavailable in EmmanueLA")) },
                    text = { Text(message) }, confirmButton = { TextButton(onClick = { model.blockedMessage.value = null; overlay = "live-moment" }) { Text(localized("App management")) } },
                    dismissButton = { TextButton(onClick = { model.blockedMessage.value = null }) { Text(localized("Close")) } }) }
                appEditor?.let { id ->
                    val app = apps.find { it.id == id }
                        ?: LaunchableApp(id, "Unavailable app", id.substringBefore('/'))
                    if (unlocked || data.policies[app.packageName]?.hidden != true) AppMetadataEditor(app, data.appMetadata[id] ?: AppMetadata(), data, model) { appEditor = null }
                }
                pause?.let { PauseScreen(it,model) }
                if (lockExplain) AlertDialog(onDismissRequest = { lockExplain = false }, title = { Text(localized("Enable screen locking")) },
                    text = { Text(localized("EmmanueLA needs device administrator access only to lock the screen. It cannot read or erase your data. Android may require your PIN after locking. You can revoke this access in Gestures before uninstalling.")) },
                    confirmButton = { TextButton(onClick = { lockExplain = false; ContextSafeOpen(context, context.lockSetupIntent()) { model.error.value = "Screen lock setup is unavailable." } }) { Text(localized("Continue")) } },
                    dismissButton = { TextButton(onClick = { lockExplain = false }) { Text(localized("Cancel")) } })
            }
        }
    }
    BackHandler(editor == null && appEditor == null && appMenu == null && policyPackages.isEmpty() && folderUnlock == null && quickAddApp == null && blockedMessage == null && !lockExplain) {
        when {
            overlay.startsWith("folder:") -> overlay = ""
            overlay == "settings" -> overlay = ""
            overlay.isNotEmpty() -> overlay = SettingsRoutes.parent(overlay)
            surface != LauncherSurface.HOME -> navigate(LauncherSurface.HOME, -1)
        }
    }
    BackHandler(pause!=null){model.cancelPause()}
}
