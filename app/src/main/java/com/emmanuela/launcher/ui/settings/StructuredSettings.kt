package com.emmanuela.launcher.ui.settings

import com.emmanuela.launcher.ui.privatespace.PrivateSpaceSettings

import com.emmanuela.launcher.platform.notificationAccess
import com.emmanuela.launcher.BuildConfig
import com.emmanuela.launcher.ContextSafeOpen
import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.AppPolicy
import com.emmanuela.launcher.data.ExperiencePreferences
import com.emmanuela.launcher.data.Favorite
import com.emmanuela.launcher.data.LaunchableApp
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.platform.rememberDeviceAuthentication
import com.emmanuela.launcher.ui.appearance.AlbumDirectoryRow
import com.emmanuela.launcher.ui.appearance.HexColorRow
import com.emmanuela.launcher.ui.appearance.WeatherLocationPicker
import com.emmanuela.launcher.ui.apps.AppDetailsContent
import com.emmanuela.launcher.ui.apps.CursorSearchField
import com.emmanuela.launcher.ui.components.VectorSymbol
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.components.vectorNames
import com.emmanuela.launcher.ui.home.openDigitalWellbeing
import com.emmanuela.launcher.ui.mindful.FocusGroupEditor
import com.emmanuela.launcher.ui.mindful.TimeChoice
import com.emmanuela.launcher.ui.apps.FolderAssignmentsContent
import com.emmanuela.launcher.ui.navigation.motionDuration
import com.emmanuela.launcher.ui.navigation.motionEasing
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.shape.RoundedCornerShape


import android.Manifest
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

object SettingsRoutes {
    fun parent(page:String)=when {
        page.startsWith("folder-select:") -> "folder-assignments"
        page.startsWith("blocked-detail:") -> "blocked-apps"
        page.startsWith("app-detail:") -> page.substringAfter('|',"apps-hub")
        page.startsWith("widget-style:") -> "widgets"
        page.startsWith("widget:") -> "widgets"
        page.startsWith("group:") -> "live-moment"
        page in listOf("widgets","bottom-controls","home-alphabet","widget-style","home-text-style") -> "home-settings"
        page in listOf("cursor","contacts-search") -> "app-settings"
        page=="locked-folders" -> "folder-settings"
        page in listOf("daily-wallpaper","motion","wallpaper-editor") -> "appearance"
        page in listOf("metadata","folder-assignments","notification-filter","management") -> "apps-hub"
        page in listOf("hidden","hidden-manager","blocked-apps","private-apps","private-manager","security") -> "private-space"
        page in listOf("more","permissions","language","wellbeing-target") -> "system"
        page in listOf("privacy","faqs") -> "about"
        else -> "settings"
    }
}
@Composable
fun ChoiceRow(label:String,value:String,options:List<String>,save:(String)->Unit) {
    var show by remember { mutableStateOf(false) }
    SettingRow(label,value){show=true}
    if(show)AlertDialog(onDismissRequest={show=false},title={Text(localized(label))},text={
        Column(Modifier.verticalScroll(rememberScrollState())) {options.forEach { option->
            Row(Modifier.fillMaxWidth().selectionMarker(option==value).clickable{save(option);show=false}.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
                RadioButton(option==value,{save(option);show=false});Text(localized(option))
            }
        }}
    },confirmButton={TextButton(onClick={show=false}){Text(localized("Cancel"))}})
}
@Composable
fun SettingsHeading(title:String,back:()->Unit,data:LauncherData,model:LauncherViewModel) {
    val advanced=data.settings.ui.experience.advanced
    Row(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
        TextButton(onClick=back){Text(localized("←"),fontSize=24.sp)}
        Text(localized(title).uppercase(),Modifier.weight(1f),fontSize=28.sp,fontWeight=FontWeight.Medium)
        TextButton(onClick={model.experienceSettings{it.copy(advanced=!advanced)};model.error.value=if(advanced)"Advanced options disabled" else "Advanced options enabled"}) {
            Text(if(advanced)"◆" else "◇",fontSize=24.sp)
        }
    }
}
@Composable
fun StructuredSettingsContent(page:String,data:LauncherData,apps:List<LaunchableApp>,model:LauncherViewModel,
    navigate:(String)->Unit,close:()->Unit,chooseHome:()->Unit,editApp:(String)->Unit,
    choice:(String)->Unit,gesture:(String)->Unit,permission:(String)->Unit,
    photos:()->Unit,font:()->Unit,export:()->Unit,restore:()->Unit,reset:()->Unit,busy:Boolean,share:()->Unit) {
    val p=data.settings;val u=p.ui;val v=u.v2;val e=u.experience;val advanced=e.advanced
    val context=LocalContext.current;val scope=rememberCoroutineScope();val auth=rememberDeviceAuthentication(data.settings.ui.experience.authentication,model::authenticationChanged)
    val usageAllowed by model.usageAllowed.collectAsStateWithLifecycle()
    val time by model.screenTime.collectAsStateWithLifecycle()
    val usage by model.appUsage.collectAsStateWithLifecycle()
    var query by rememberSaveable(page){mutableStateOf("")}
    val notifications=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(!granted)model.error.value="Notifications remain disabled."}
    val location=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        model.v2Settings{it.copy(locationWidgetEnabled=granted)}
        if(!granted)model.error.value="Location remains off. You can enable approximate location in Android settings."
    }
    fun open(intent:Intent)=ContextSafeOpen(context,intent){model.error.value="This Android settings page is unavailable."}
    val selectedApp=apps.find{it.id==page.substringAfter(':').substringBefore('|')}
    val selectedGroup=data.focusGroups.find{it.id==page.substringAfter(':')}
    val title=when {
        page.startsWith("widget-style:")->"Widget style"
        page.startsWith("widget:")->widgetName(page.substringAfter(':'))
        page.startsWith("app-detail:")||page.startsWith("blocked-detail:")->selectedApp?.label?:"App"
        page.startsWith("folder-select:")->"Select folders"
        page.startsWith("group:")->selectedGroup?.name?:"Create group"
        page=="mindful"->"Mindful Use"
        else->when(page){"settings"->"SETTINGS";"system"->"System";"about"->"About";"home-settings"->"Home";"app-settings"->"App List";"folder-settings"->"Folders";"apps-hub"->"Apps";"live-moment"->"Live the Moment";"private-space"->"Private Space";"notification-filter"->"Notifications";"bottom-controls"->"Bottom controls";"home-alphabet"->"Home alphabet";"folder-assignments"->"Folder assignments";"metadata"->"Aliases & tags";"daily-wallpaper"->"Daily wallpaper";"hidden-manager"->"Hidden apps";else->page.substringAfter(':').replaceFirstChar{it.uppercase()}.replace('-',' ')}
    }
    LaunchedEffect(page){if(page=="mindful") {model.refreshAppUsage();model.refreshUsage()} }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).padding(horizontal=24.dp)) {
        SettingsHeading(title,{if(page=="settings")close()else navigate(SettingsRoutes.parent(page))},data,model)
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        // Scroll state is keyed only by route; toggling Advanced retains page and scroll position.
        key(page) { Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom=24.dp)) {
            when {
                page=="settings" -> {
                    SectionLabel("General",true);SettingRow("System","→"){navigate("system")};SettingRow("About","→"){navigate("about")}
                    SectionLabel("Your space",true)
                    listOf("Home" to "home-settings","App List" to "app-settings","Folders" to "folder-settings","Appearance" to "appearance","Gestures" to "gestures","Apps" to "apps-hub","Mindful Use" to "mindful","Live the Moment" to "live-moment","Private Space" to "private-space").forEach{(label,target)->SettingRow(label,"→"){navigate(target)}}
                }
                page=="system" -> {
                    SettingRow("Default launcher"){chooseHome()}
                    SettingRow("Language",languageNames[e.language]?:"Follow system language"){navigate("language")}
                    SettingRow("Permissions"){navigate("permissions")}
                    SettingRow("Battery optimization"){open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))}
                    SettingRow("Backup & restore"){navigate("more")}
                    SettingRow("Digital Wellbeing destination"){navigate("wellbeing-target")}
                }
                page=="language" -> {
                    languageNames.forEach{(code,label)->Box(Modifier.selectionMarker(e.language==code)){SettingRow(label,if(e.language==code)"✓" else ""){model.experienceSettings{it.copy(language=code)}}}}
                }
                page=="permissions" -> PermissionSettings(model)
                page=="about" -> {
                    Text(localized("EmmanueLA"),style=MaterialTheme.typography.headlineLarge);Text(localized("Minimal. Personal. Yours."),Modifier.padding(vertical=12.dp))
                    Text("Version ${BuildConfig.VERSION_NAME}",style=MaterialTheme.typography.bodyMedium)
                    SettingRow("Share the app",onClick=share)
                    SettingRow("Quick guide / FAQs"){navigate("faqs")}
                    SettingRow("GitHub"){open(Intent(Intent.ACTION_VIEW,Uri.parse(PublicLinks.GITHUB)))}
                    SettingRow("Privacy policy"){navigate("privacy")};SettingRow("Version information","${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"){}
                }
                page=="faqs" -> {Text(localized("Swipe up on Home to open Apps. Swipe left to switch Apps and Folders; right returns Home. Long-press an app for its options. Search #tag for tagged apps.\n\n◇ shows advanced controls. Rules apply when opening apps through EmmanueLA; Android settings and other launchers remain separate. Private Space uses your device authentication.\n\nTo uninstall, revoke screen-lock administrator access and select another default launcher first."))}
                page=="more" -> {
                    SettingRow("Export configuration","JSON",export);SettingRow("Restore configuration","JSON",restore)
                    Text(localized("Backups include launcher settings and local rules. Select photos, fonts and permissions again on another device."),style=MaterialTheme.typography.bodySmall)
                    SettingRow("Factory reset",onClick=reset)
                }
                page=="home-settings" -> {
                    ToggleRow("Enable Home",e.homeEnabled){yes->model.experienceSettings{it.copy(homeEnabled=yes)}}
                    SectionLabel("Layout");SettingRow("Favorites","${data.favorites.size}"){navigate("favorites")}
                    SettingRow("Favorite count",p.favoriteCount.toString()){choice("count")};SettingRow("Alignment",p.alignment){choice("alignment")}
                    SliderRow("Spacing",e.homeSpacing.toFloat(),0f..32f){n->model.experienceSettings{it.copy(homeSpacing=n.toInt())}}
                    SettingRow("Text style"){navigate("home-text-style")}
                    SectionLabel("Widgets",true);SubSettingRow("Manage widgets"){navigate("widgets")};SubSettingRow("Arrange widgets"){navigate("widget-layout")}
                    SectionLabel("Bottom controls");SettingRow("Left shortcut",actionName(u.leftShortcut,apps)){gesture("Bottom left")};SettingRow("Right shortcut",actionName(u.rightShortcut,apps)){gesture("Bottom right")}
                    SettingRow("Style & size"){navigate("bottom-controls")}
                    SectionLabel("System");ToggleRow("Show status bar",p.notificationBar){yes->model.settings{it.copy(notificationBar=yes)}}
                    if(advanced){ToggleRow("Edge to edge",e.edgeToEdge){yes->model.experienceSettings{it.copy(edgeToEdge=yes)}};SettingRow("Home alphabet"){navigate("home-alphabet")}}
                }
                page=="home-text-style" -> {
                    ToggleRow("Use custom Home text style",e.homeStyleOverride){yes->model.experienceSettings{it.copy(homeStyleOverride=yes)}}
                    ChoiceRow("Font",e.homeFont,listOf("Inherit","Sans","Serif","Monospace")+if(p.customFont.isNotEmpty())listOf("Custom")else emptyList()){value->model.experienceSettings{it.copy(homeFont=value,homeStyleOverride=true)}}
                    SliderRow("Text size",e.homeTextScale,.7f..1.5f){n->model.experienceSettings{it.copy(homeTextScale=n,homeStyleOverride=true)}}
                    ChoiceRow("Weight",e.homeTextWeight,listOf("Inherit","Regular","Medium","Bold")){value->model.experienceSettings{it.copy(homeTextWeight=value,homeStyleOverride=true)}}
                    ToggleRow("Italic",e.homeItalic){yes->model.experienceSettings{it.copy(homeItalic=yes,homeStyleOverride=true)}}
                    val family=com.emmanuela.launcher.ui.appearance.launcherFont(if(e.homeFont=="Inherit")p.font else e.homeFont,p.customFont,MaterialTheme.typography.bodyLarge.fontFamily,e.homeItalic,com.emmanuela.launcher.ui.appearance.weightOf(if(e.homeTextWeight=="Inherit")e.textWeight else e.homeTextWeight))
                    Text("EmmanueLA",Modifier.padding(vertical=24.dp),style=MaterialTheme.typography.headlineSmall.copy(fontFamily=family,fontSynthesis=androidx.compose.ui.text.font.FontSynthesis.All,fontStyle=if(e.homeItalic)androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,fontWeight=if(e.homeTextWeight=="Inherit")MaterialTheme.typography.headlineSmall.fontWeight else com.emmanuela.launcher.ui.appearance.weightOf(e.homeTextWeight),fontSize=MaterialTheme.typography.headlineSmall.fontSize*e.homeTextScale))
                }
                page=="wellbeing-target" -> WellbeingDestinationPicker(data,model)
                page=="contacts-search" -> ContactSearchSettings(data,apps,model)
                page=="widgets" -> {
                    listOf("clock" to p.showClock,"date" to p.showDate,"weather" to v.weatherEnabled,"usage" to p.showScreenTime,"battery" to p.showBattery,"location" to v.locationWidgetEnabled).forEach{(id,enabled)->
                        Row(verticalAlignment=Alignment.CenterVertically){VectorSymbol(if(id=="usage")"timer" else id,MaterialTheme.colorScheme.onSurface)
                            Text(localized(widgetName(id)),Modifier.weight(1f).clickable{navigate("widget-style:$id")}.padding(16.dp));Switch(enabled,{yes->if(id!="location"||!yes)setWidget(model,id,yes);if(yes&&id=="usage"&&!usageAllowed)permission("usage");if(yes&&id=="location")location.launch(Manifest.permission.ACCESS_COARSE_LOCATION)})}
                    }
                }
                page.startsWith("widget:") -> {
                    val id=page.substringAfter(':');val enabled=when(id){"clock"->p.showClock;"date"->p.showDate;"usage"->p.showScreenTime;"battery"->p.showBattery;"weather"->v.weatherEnabled;else->v.locationWidgetEnabled}
                    ToggleRow("Enabled",enabled){yes->if(id!="location"||!yes)setWidget(model,id,yes);if(yes&&id=="location")location.launch(Manifest.permission.ACCESS_COARSE_LOCATION);if(yes&&id=="usage"&&!usageAllowed)permission("usage")}
                    if(id=="clock"){ChoiceRow("Clock format",v.clockFormat,listOf("Digital","Compact","Hands")){value->model.v2Settings{it.copy(clockFormat=value)}};ToggleRow("Show seconds",v.clockSeconds){yes->model.v2Settings{it.copy(clockSeconds=yes)}};ToggleRow("AM/PM",v.clockAmPm){yes->model.v2Settings{it.copy(clockAmPm=yes)}}}
                    if(id=="weather")WeatherLocationPicker(data,model)
                    SettingRow("Appearance","Default"){navigate("widget-style:$id")}
                    SettingRow("Tap action"){gesture(when(id){"usage"->"Screen time";else->widgetName(id)})}
                    if(id=="usage")SettingRow("Digital Wellbeing destination"){navigate("wellbeing-target")}
                    if(id=="weather")WeatherWebsiteRow(data,model)
                }
                page.startsWith("widget-style:") -> {
                    val id=page.substringAfter(':')
                    if(id in com.emmanuela.launcher.data.widgetIds){
                        WidgetStyleControls(id,data,model)
                        SubSettingRow("Widget options"){navigate("widget:$id")}
                    }
                }
                page=="bottom-controls" -> {
                    if(advanced)ToggleRow("Enabled",u.bottomShortcuts){yes->model.uiSettings{it.copy(bottomShortcuts=yes)}}
                    SettingRow("Left",actionName(u.leftShortcut,apps)){gesture("Bottom left")};SettingRow("Right",actionName(u.rightShortcut,apps)){gesture("Bottom right")}
                    ChoiceRow("Style",v.shortcutMode,listOf("Text","Icon")){value->model.v2Settings{it.copy(shortcutMode=value)}}
                    if(v.shortcutMode=="Icon") {VectorChoice("Left icon",v.leftVector){value->model.v2Settings{it.copy(leftVector=value)}};VectorChoice("Right icon",v.rightVector){value->model.v2Settings{it.copy(rightVector=value)}}}
                    ChoiceRow("Size",e.bottomSize,listOf("Small","Medium","Large")){value->
                        model.uiSettings{it.copy(bottomBarHeight=when(value){"Small"->44f;"Large"->80f;else->56f},bottomBarTextScale=when(value){"Small"->.8f;"Large"->1.3f;else->1f},experience=it.experience.copy(bottomSize=value))}}
                    SliderRow("Position from bottom",u.bottomBarOffset,0f..120f){n->model.uiSettings{it.copy(bottomBarOffset=n)}}
                    BottomControlsPreview(data,apps)
                    SliderRow("Width",u.bottomBarWidth,.5f..1f){n->model.uiSettings{it.copy(bottomBarWidth=n)}}
                    SliderRow("Height",u.bottomBarHeight,40f..120f){n->model.uiSettings{it.copy(bottomBarHeight=n)}}
                    SliderRow("Padding",u.bottomBarPadding,0f..40f){n->model.uiSettings{it.copy(bottomBarPadding=n)}}
                    SliderRow("Text size",u.bottomBarTextScale,.7f..1.8f){n->model.uiSettings{it.copy(bottomBarTextScale=n)}}
                    ChoiceRow("Text weight",if(u.bottomBarWeight in listOf("Normal","Light"))"Regular" else u.bottomBarWeight,listOf("Regular","Medium","Bold")){value->model.uiSettings{it.copy(bottomBarWeight=if(value=="Regular")"Normal" else value)}}
                }
                page=="app-settings" -> {
                    SectionLabel("Layout");ToggleRow("Tree Branch folders",v.treeBranch){yes->model.v2Settings{it.copy(treeBranch=yes)}};ToggleRow("Enable App list",u.appListEnabled&&!e.homeAlphabet){yes->model.uiSettings{it.copy(appListEnabled=yes,experience=if(yes)it.experience.copy(homeAlphabet=false)else it.experience)}}
                    ChoiceRow("Alignment",e.appAlignment,listOf("Left","Center","Right")){value->model.experienceSettings{it.copy(appAlignment=value)}}
                    ToggleRow("Show app icons",e.appIcons){yes->model.experienceSettings{it.copy(appIcons=yes)}}
                    SliderRow("Spacing",e.appSpacing.toFloat(),0f..32f){n->model.experienceSettings{it.copy(appSpacing=n.toInt())}}
                    if(advanced)SliderRow("Text size",e.appTextScale,.7f..1.5f){n->model.experienceSettings{it.copy(appTextScale=n)}}
                    SectionLabel("Search");ToggleRow("Search bar",v.searchEnabled){yes->model.v2Settings{it.copy(searchEnabled=yes)}}
                    SettingRow("Position",u.searchPosition){choice("search-position")}
                    SearchStylePicker(u.searchStyle){value->model.uiSettings{it.copy(searchStyle=value)}}
                    ToggleRow("Auto keyboard",p.autoKeyboard){yes->model.settings{it.copy(autoKeyboard=yes)}}
                    ToggleRow("Auto-launch single result",e.autoLaunch){yes->model.experienceSettings{it.copy(autoLaunch=yes)}}
                    if(advanced){SliderRow("Auto-launch delay (ms)",e.autoLaunchDelay.toFloat(),0f..1500f){n->model.experienceSettings{it.copy(autoLaunchDelay=n.toInt())}}
                        ToggleRow("Search aliases",e.searchAliases){yes->model.experienceSettings{it.copy(searchAliases=yes)}}
                        ToggleRow("Search tags",u.tagSearch){yes->model.uiSettings{it.copy(tagSearch=yes)}}
                        ToggleRow("Tag suggestions",u.tagSuggestions){yes->model.uiSettings{it.copy(tagSuggestions=yes)}}
                        ToggleRow("Search package names",e.searchPackages){yes->model.experienceSettings{it.copy(searchPackages=yes)}}
                        SettingRow("Cursor style"){navigate("cursor")}}
                    SettingRow("Contact search (@)"){navigate("contacts-search")}
                    SectionLabel("Alphabet");ToggleRow("Alphabet rail",u.alphabet){yes->model.uiSettings{it.copy(alphabet=yes)}}
                    ChoiceRow("Animation",u.alphabetAnimation,listOf("None","Wave","Bubble","Fade")){value->model.uiSettings{it.copy(alphabetAnimation=value)}}
                    if(u.alphabetAnimation=="Wave")SliderRow("Wave movement",e.appWaveStrength,16f..80f){n->model.experienceSettings{it.copy(appWaveStrength=n)}}
                    ToggleRow("Haptics",e.alphabetHaptics){yes->model.experienceSettings{it.copy(alphabetHaptics=yes)}};com.emmanuela.launcher.platform.HapticTestRow()
                }
                page=="cursor" -> {
                    CursorStylePicker(e.cursorStyle){value->model.experienceSettings{it.copy(cursorStyle=value)}}
                    if(e.cursorStyle=="Custom"){var character by remember(e.cursorCharacter){mutableStateOf(e.cursorCharacter)};OutlinedTextField(character,{value->if(value.length<=2){character=value;model.experienceSettings{it.copy(cursorCharacter=value)}}},label={Text(localized("Character"))})}
                    SettingRow("Color"){choice("search-cursor-color")}
                    SliderRow("Thickness",u.searchCursorWidth,1f..6f){n->model.uiSettings{it.copy(searchCursorWidth=n)}}
                    ChoiceRow("Blink",if(u.searchCursorBlinkMs==0)"Off" else if(u.searchCursorBlinkMs>=900)"Slow" else "Normal",listOf("Normal","Slow","Off")){value->model.uiSettings{it.copy(searchCursorBlinkMs=when(value){"Off"->0;"Slow"->1000;else->500})}}
                    CursorSearchField(query,{query=it},p,Modifier.fillMaxWidth(),"Preview cursor")
                }
                page=="home-alphabet" -> {
                    ToggleRow("Show alphabet",e.homeAlphabet){yes->model.uiSettings{ui->ui.copy(appListEnabled=if(yes)false else ui.experience.drawerBeforeHomeAlphabet,experience=ui.experience.copy(homeAlphabet=yes,drawerBeforeHomeAlphabet=if(yes)ui.appListEnabled else ui.experience.drawerBeforeHomeAlphabet))}}
                    Text(localized("Home alphabet replaces the App List page."),style=MaterialTheme.typography.bodySmall)
                    ChoiceRow("Position",e.homeAlphabetPosition,listOf("Left","Right")){value->model.experienceSettings{it.copy(homeAlphabetPosition=value)}}
                    ChoiceRow("Style",e.homeAlphabetStyle,listOf("Minimal","Compact")){value->model.experienceSettings{it.copy(homeAlphabetStyle=value)}}
                    if(e.homeAlphabetAnimation=="Wave")SliderRow("Wave movement",e.homeWaveStrength,16f..80f){n->model.experienceSettings{it.copy(homeWaveStrength=n)}}
                    ChoiceRow("Animation",e.homeAlphabetAnimation,listOf("None","Wave","Bubble","Fade")){value->model.experienceSettings{it.copy(homeAlphabetAnimation=value)}};ToggleRow("Haptics",e.homeAlphabetHaptics){yes->model.experienceSettings{it.copy(homeAlphabetHaptics=yes)}};com.emmanuela.launcher.platform.HapticTestRow()
                    Text(localized("♡ shows favorites. # shows tagged apps. Letters filter apps inside the Home app area."),style=MaterialTheme.typography.bodyMedium)
                }
                page=="folder-settings" -> {
                    ChoiceRow("Folder width (cells)",v.folderWidthUnits.toString(),(1..4).map{it.toString()}){value->model.v2Settings{it.copy(folderWidthUnits=value.toInt())}}
                    ChoiceRow("Folder height (cells)",v.folderHeightUnits.toString(),(1..4).map{it.toString()}){value->model.v2Settings{it.copy(folderHeightUnits=value.toInt())}}
                    SectionLabel("Layout");ToggleRow("Enable Folders",v.foldersEnabled){yes->model.v2Settings{it.copy(foldersEnabled=yes)}}
                    FolderLayoutPicker(if(u.folderLayout in listOf("Canvas","Freeform"))"Freeform" else if(u.folderLayout=="List")"List" else "Grid"){value->model.uiSettings{it.copy(folderLayout=if(value=="Grid")"Adaptive" else value)}}
                    ToggleRow("In-folder icons",u.folderAppIcons){yes->model.uiSettings{it.copy(folderAppIcons=yes)}}
                    SliderRow("Spacing",e.folderSpacing.toFloat(),0f..32f){n->model.experienceSettings{it.copy(folderSpacing=n.toInt())}}
                    if(advanced)ChoiceRow("Sort apps",e.folderSort,listOf("Name","Manual")){value->model.experienceSettings{it.copy(folderSort=value)}}
                    SectionLabel("Appearance");ToggleRow("Background fill",u.folderFill){yes->model.uiSettings{it.copy(folderFill=yes)}}
                    if(advanced)SliderRow("Label size",e.folderLabelScale,.7f..1.5f){n->model.experienceSettings{it.copy(folderLabelScale=n)}}
                    SectionLabel("Behavior");ToggleRow("Close after launch",e.folderClose){yes->model.experienceSettings{it.copy(folderClose=yes)}}
                    if(advanced){ChoiceRow("Folder animation",e.folderAnimation,listOf("Off","Standard")){value->model.experienceSettings{it.copy(folderAnimation=value)}};SectionLabel("Security");SettingRow("Locked folders"){navigate("locked-folders")}}
                }
                page=="locked-folders" -> {data.folders.filter{it.isProtected}.forEach{folder->SettingRow(folder.name,"→"){navigate("open-folder:${folder.id}")}}}
                page=="appearance" -> {
                    SectionLabel("Theme")
                    ChoiceRow("Theme",p.theme,if(advanced)listOf("System","Light","Dark","Custom")else listOf("System","Light","Dark")){value->model.settings{it.copy(theme=value)}}
                    if(advanced&&p.theme=="Custom")HexColorRow("Background color",v.customBackground){value->model.v2Settings{it.copy(customBackground=value)}}
                    SectionLabel("Colors");SettingRow("Accent color",u.palette){choice("palette")}
                    if(advanced){
                        HexColorRow("Accent color",if(e.accentColor==0L)0xFF9EC3E6 else e.accentColor){value->model.experienceSettings{it.copy(accentColor=value)}}
                        HexColorRow("Text color",if(e.textColor==0L)if(p.theme=="Light")0xFF000000 else 0xFFFFFFFF else e.textColor){value->model.experienceSettings{it.copy(textColor=value)}}
                        SettingRow("Reset custom colors"){model.experienceSettings{it.copy(accentColor=0L,textColor=0L)}}
                    }
                    SectionLabel("Typography");SettingRow("Font",p.font){choice("font")}
                    if(advanced)SettingRow("Custom font","TTF / OTF",font)
                    SliderRow("Text size",p.textScale,.7f..1.5f){n->model.settings{it.copy(textScale=n)}}
                    ChoiceRow("Weight",e.textWeight,listOf("Regular","Medium","Bold")){value->model.settings{it.copy(bold=false,ui=it.ui.copy(experience=it.ui.experience.copy(textWeight=value)))}}
                    ToggleRow("Italic",u.italic){yes->model.uiSettings{it.copy(italic=yes)}}
                    if(e.homeStyleOverride)Text(localized("Home uses its own text style"),style=MaterialTheme.typography.bodySmall)
                    if(advanced)SliderRow("Transparency",e.textOpacity,.4f..1f){n->model.experienceSettings{it.copy(textOpacity=n)}}
                    SectionLabel("Wallpaper");SettingRow("Choose wallpaper",onClick=photos);SettingRow("Adjust wallpaper"){navigate("wallpaper-editor")}
                    if(advanced)SettingRow("Daily new wallpaper"){navigate("daily-wallpaper")}
                    SectionLabel("Motion");SettingRow("Animations"){navigate("motion")}
                }
                page=="daily-wallpaper" -> {
                    ToggleRow("Enabled",p.dailyWallpaper,p.wallpapers.isNotEmpty()){yes->model.settings{it.copy(dailyWallpaper=yes)}}
                    AlbumDirectoryRow(data,model)
                    SettingRow("Select individual photos","${p.wallpapers.size} photos",photos)
                    TimeChoice("Switch time",v.wallpaperSwitchMinute){n->model.v2Settings{it.copy(wallpaperSwitchMinute=n)}}
                    ChoiceRow("Order",e.wallpaperOrder,listOf("Random","Sequential")){value->model.experienceSettings{it.copy(wallpaperOrder=value)}}
                }
                page=="motion" -> {
                    ChoiceRow("Motion",e.motion,listOf("Off","Standard","Fluid")){value->model.experienceSettings{it.copy(motion=value)}}
                    if(advanced){ChoiceRow("Speed",e.motionSpeed,listOf("Fast","Normal","Slow")){value->model.experienceSettings{it.copy(motionSpeed=value)}};ToggleRow("Reduce motion",e.reduceMotion){yes->model.experienceSettings{it.copy(reduceMotion=yes)}}}
                    MotionPreview(e)
                }
                page=="gestures" -> {
                    GestureDiagram(gesture)
                    ToggleRow("Enabled",v.gesturesEnabled){yes->model.v2Settings{it.copy(gesturesEnabled=yes)}}
                    listOf("Up" to p.swipeUp,"Down" to p.swipeDown,"Left" to p.swipeLeft,"Right" to p.swipeRight,"Double tap" to p.doubleTapAction,"Hold" to v.holdAction).forEach{(trigger,target)->SettingRow(trigger,actionName(target,apps)){gesture(trigger)}}
                    if(advanced){SettingRow("Triple tap",actionName(u.tripleTapAction,apps)){gesture("Triple tap")};ChoiceRow("Sensitivity",e.gestureSensitivity,listOf("Low","Medium","High")){value->model.experienceSettings{it.copy(gestureSensitivity=value)}};ToggleRow("Haptics",e.gestureHaptics){yes->model.experienceSettings{it.copy(gestureHaptics=yes)}};com.emmanuela.launcher.platform.HapticTestRow()}
                    SettingRow("Screen lock permission"){permission("lock")}
                }
                page=="apps-hub" -> {listOf("App settings" to "management","Aliases & tags" to "metadata","Folder assignments" to "folder-assignments","Notifications" to "notification-filter").forEach{(label,target)->SettingRow(label,"→"){navigate(target)}}}
                page in listOf("metadata","folder-assignments") -> {
                    OutlinedTextField(query,{query=it},singleLine=true,label={Text(localized("Search apps"))},modifier=Modifier.fillMaxWidth())
                    LazyColumn(Modifier.fillMaxWidth().heightIn(min=120.dp,max=560.dp)){items(apps.filter{data.policies[it.packageName]?.hidden!=true && it.label.contains(query,true)},key={it.id}){app->
                        val folders=data.folders.filter{app.id in it.apps}.joinToString{it.name}
                        if(page=="folder-assignments"){
                            Column(Modifier.fillMaxWidth().clickable{navigate("folder-select:${app.id}")}.padding(vertical=14.dp)){Text(app.label);Text(folders,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                        }else SettingRow((if(data.appMetadata[app.id]?.alias?.isNotBlank()==true)"~ " else "")+app.label){editApp(app.id)}
                    }}
                }
                page.startsWith("folder-select:") -> { if(selectedApp!=null)FolderAssignmentsContent(selectedApp,data,model) }
                page.startsWith("blocked-detail:") -> { if(selectedApp!=null){val policy=data.policies[selectedApp.packageName]?:AppPolicy();ToggleRow("Blocked",policy.blocked){yes->scope.launch{model.changePolicies(setOf(selectedApp.packageName)){it.copy(blocked=yes)}}}} }
                page=="notification-filter" -> {
                    Text(if(context.notificationAccess())"Notification Access enabled"else "Enable Notification Access to apply filtering",style=MaterialTheme.typography.bodySmall)
                    ToggleRow("Launcher badges",v.showBadges){yes->model.v2Settings{it.copy(showBadges=yes)}}
                    ToggleRow("Notification filter",v.notificationFilter){yes->model.v2Settings{it.copy(notificationFilter=yes)}}
                    ToggleRow("Notification digest",e.digestEnabled){yes->model.experienceSettings{it.copy(digestEnabled=yes)}}
                    SettingRow("Android notification settings"){open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))}
                    if(advanced&&e.digestEnabled)ChoiceRow("Digest interval",e.digestInterval.toString(),listOf("15","30","60","120")){value->model.experienceSettings{it.copy(digestInterval=value.toInt())}}
                    if(android.os.Build.VERSION.SDK_INT>=33)SettingRow("Allow notification summaries"){notifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)}
                    SectionLabel("Per-app rules")
                    LazyColumn(Modifier.fillMaxWidth().heightIn(min=120.dp,max=480.dp)){items(apps.distinctBy{it.packageName},key={it.packageName}){app->val mode=data.policies[app.packageName]?.notifications?:NotificationMode.NORMAL
                        ChoiceRow(app.label,mode.name,listOf("NORMAL","MUTE","DIGEST")){value->scope.launch{model.changePolicies(setOf(app.packageName)){it.copy(notifications=NotificationMode.valueOf(value))}}}
                    }}
                    if(advanced)Text(localized("Filtering happens after notification arrival. Android can alert before dismissal."),style=MaterialTheme.typography.bodySmall)
                }
                page=="mindful" -> {
                    Text(localized("Today"),style=MaterialTheme.typography.titleMedium);Text(time?.let{"${it/3_600_000}h ${it/60_000%60}m"}?:"Usage access needed",style=MaterialTheme.typography.displaySmall)
                    SettingRow("Daily goal","${u.screenTimeLimit} min"){}
                    SectionLabel("Most used");usage?.entries?.sortedByDescending{it.value}?.take(5)?.forEach{(pkg,ms)->Text("${apps.firstOrNull{it.packageName==pkg}?.label?:pkg} · ${ms/60_000} min",Modifier.padding(vertical=8.dp))}
                    SettingRow("Open Digital Wellbeing"){if(!com.emmanuela.launcher.platform.SystemDestinations.wellbeing(context,e.wellbeingComponent))navigate("wellbeing-target")}
                    if(advanced){SliderRow("Daily goal (minutes)",u.screenTimeLimit.toFloat(),15f..720f){n->model.uiSettings{it.copy(screenTimeLimit=n.toInt())}}
                        SliderRow("Warning threshold (%)",e.warningPercent.toFloat(),50f..100f){n->model.experienceSettings{it.copy(warningPercent=n.toInt())}}
                        ToggleRow("Gentle reminder on Home",e.gentleReminder){yes->model.experienceSettings{it.copy(gentleReminder=yes)}}
                        SliderRow("Reminder interval (minutes)",e.reminderMinutes.toFloat(),5f..120f){n->model.experienceSettings{it.copy(reminderMinutes=n.toInt())}}
                        ToggleRow("Show widget",p.showScreenTime){yes->model.settings{it.copy(showScreenTime=yes)}}}
                }
                page=="live-moment" -> {
                    ToggleRow("Enforce outside launcher",v.backgroundRules){yes->model.v2Settings{it.copy(backgroundRules=yes)}}
                    Text("Optional Accessibility access detects foreground apps and reads only supported browser address fields. No URLs or page contents are saved. Rules need this access to apply outside EmmanueLA.",style=MaterialTheme.typography.bodySmall)
                    SettingRow("Enable background rule access"){open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}
                    Text(if(com.emmanuela.launcher.platform.EnforcementBridge.connected)"Background rule service connected"else "Background rule service not connected",style=MaterialTheme.typography.bodySmall)
                    SectionLabel("Groups")
                    data.focusGroups.forEach{g->Column(Modifier.fillMaxWidth().clickable{navigate("group:${g.id}")}.padding(vertical=16.dp)){
                        Text(g.name,style=MaterialTheme.typography.titleMedium)
                        Text(apps.filter{it.packageName in g.packages}.distinctBy{it.packageName}.joinToString{it.label},style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if(g.dailyMinutes>0)"${g.dailyMinutes} min/day" else "Unlimited",style=MaterialTheme.typography.bodySmall)
                        HorizontalDivider(Modifier.padding(top=16.dp),color=MaterialTheme.colorScheme.outline.copy(alpha=.15f))
                    }}
                    SettingRow("+ Create group"){navigate("group:new")}
                }
                page.startsWith("group:") -> FocusGroupEditor(page.substringAfter(':'),data,apps,model){navigate("live-moment")}
                page.startsWith("app-detail:") -> {
                    val app=apps.find{it.id==page.substringAfter(':').substringBefore('|')}
                    if(app!=null)AppDetailsContent(app,data,model,metadataOnly=page.substringAfter('|')=="metadata") else Text(localized("This app is unavailable."))
                }
                page in listOf("private-space","hidden","hidden-manager","blocked-apps","private-apps","private-manager","security") -> PrivateSpaceSettings(page,data,apps,model,navigate)
                page=="privacy" -> Text(localized("Your configuration and usage rules remain on your device. There are no accounts, ads or analytics. Optional weather sends the selected coordinates to Open-Meteo. Notification Access reads package metadata and counts, never message content. Usage Access is optional and used locally. Backups contain configuration, not usage history. Private Space protects launcher entry points, not access from other Android surfaces."))
            }
        }}
    }
}
fun widgetName(id:String)=when(id){"usage"->"Screen time";"clock"->"Clock";"date"->"Date";"weather"->"Weather";"battery"->"Battery";else->"Location"}
fun setWidget(model:LauncherViewModel,id:String,yes:Boolean){when(id){"clock"->model.settings{it.copy(showClock=yes)};"date"->model.settings{it.copy(showDate=yes)};"usage"->model.settings{it.copy(showScreenTime=yes)};"battery"->model.settings{it.copy(showBattery=yes)};"weather"->model.v2Settings{it.copy(weatherEnabled=yes)};"location"->model.v2Settings{it.copy(locationWidgetEnabled=yes)}}}
@Composable
fun VectorChoice(label:String,value:String,save:(String)->Unit){var open by remember{mutableStateOf(false)};SettingRow(label,value){open=true};if(open)AlertDialog(onDismissRequest={open=false},title={Text(label)},text={Column(Modifier.heightIn(max=350.dp).verticalScroll(rememberScrollState())){vectorNames.chunked(5).forEach{names->Row{names.forEach{name->Box(Modifier.size(48.dp).selectionMarker(name==value).clickable{save(name);open=false},contentAlignment=Alignment.Center){VectorSymbol(name,MaterialTheme.colorScheme.onSurface)}}}}}},confirmButton={TextButton(onClick={open=false}){Text(localized("Cancel"))}})}
val languageNames=linkedMapOf("system" to "Follow system language","en" to "English","ro" to "Română","de" to "Deutsch","fr" to "Français","es" to "Español","it" to "Italiano","pl" to "Polski")
@Composable
fun SearchStylePicker(value:String,save:(String)->Unit){
    SectionLabel("Search style")
    listOf("Pill","Box","Underline","Minimal").chunked(2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
        row.forEach{style->Column(Modifier.weight(1f).selectionMarker(style==value).clickable{save(style)}.padding(vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally){
            val border=if(style in listOf("Pill","Box"))Modifier.border(1.dp,MaterialTheme.colorScheme.outline,RoundedCornerShape(if(style=="Pill")50 else 0))else Modifier
            Box(Modifier.fillMaxWidth().height(40.dp).then(border),contentAlignment=Alignment.Center){Text(if(style=="Underline")"Search ______" else "Search",fontSize=14.sp)}
            Text(if(style=="Underline")"Line" else style,fontWeight=if(style==value)FontWeight.Bold else FontWeight.Normal)
        }}
    }}
}
@Composable
fun FolderLayoutPicker(value:String,save:(String)->Unit){
    SectionLabel("Layout style")
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
        listOf("List","Grid","Freeform").forEach{style->Column(Modifier.weight(1f).background(if(style==value)MaterialTheme.colorScheme.primary.copy(alpha=.16f)else androidx.compose.ui.graphics.Color.Transparent,RoundedCornerShape(12.dp)).border(1.dp,if(style==value)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha=.15f),RoundedCornerShape(12.dp)).clickable{save(style)}.padding(vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Box(Modifier.fillMaxWidth().height(62.dp),contentAlignment=Alignment.Center){Text(when(style){"List"->"━━━━\n━━━━\n━━━━";"Grid"->"□ □ □\n□ □ □";else->"□     □\n   □"},fontSize=16.sp)}
            Text(style,fontWeight=if(style==value)FontWeight.Bold else FontWeight.Normal)
        }}
    }
}
@Composable
fun GestureDiagram(select:(String)->Unit){
    Column(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally){
        TextButton(onClick={select("Up")}){Text("↑",fontSize=26.sp)}
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(24.dp)){
            TextButton(onClick={select("Left")}){Text("←",fontSize=26.sp)};Text("HOME");TextButton(onClick={select("Right")}){Text("→",fontSize=26.sp)}
        }
        TextButton(onClick={select("Down")}){Text("↓",fontSize=26.sp)}
    }
}
@Composable
fun MotionPreview(e:ExperiencePreferences){
    var second by remember{mutableStateOf(false)}
    val fraction by androidx.compose.animation.core.animateFloatAsState(if(second)1f else 0f,androidx.compose.animation.core.tween(e.motionDuration(),easing=e.motionEasing()),label="Page preview")
    BoxWithConstraints(Modifier.fillMaxWidth().height(100.dp).clipToBounds().clickable{second=!second}){
        val width=maxWidth
        Box(Modifier.fillMaxSize().offset(x=width*(-fraction)).border(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.3f)),contentAlignment=Alignment.Center){Text("Apps")}
        Box(Modifier.fillMaxSize().offset(x=width*(1f-fraction)).border(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.3f)),contentAlignment=Alignment.Center){Text("Folders")}
    }
}

@Composable
fun CursorStylePicker(selected:String,save:(String)->Unit){
    SectionLabel("Style")
    listOf("Normal" to "│","Block" to "█","Underline" to "_","Thin" to "|","Thick" to "▮","Custom" to "_").forEach{(style,glyph)->
        Row(Modifier.fillMaxWidth().selectionMarker(style==selected).clickable{save(style)}.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
            RadioButton(selected==style,{save(style)});Text(localized(style),Modifier.weight(1f));Text("Search apps $glyph",color=MaterialTheme.colorScheme.primary)
        }
    }
}
