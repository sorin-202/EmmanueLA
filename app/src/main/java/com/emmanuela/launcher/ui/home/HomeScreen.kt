@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.emmanuela.launcher.ui.home

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.LaunchableApp
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.data.styleForPhoto
import com.emmanuela.launcher.platform.WallpaperCache
import com.emmanuela.launcher.platform.badgeLabel
import com.emmanuela.launcher.ui.appearance.WallpaperLayer
import com.emmanuela.launcher.ui.components.VectorSymbol
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.navigation.HomeTapRouter
import com.emmanuela.launcher.ui.navigation.LocalGestureRegions
import com.emmanuela.launcher.ui.navigation.LocalHomeTapRouter
import com.emmanuela.launcher.ui.navigation.homeClick
import com.emmanuela.launcher.ui.settings.actionName


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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeScreen(data:LauncherData,apps:List<LaunchableApp>,model:LauncherViewModel,now:Long,battery:Int,
    action:(String)->Unit,settings:()->Unit,favorites:()->Unit,editApp:(String)->Unit,launch:(LaunchableApp)->Unit,arranging:Boolean=false){
    val e=data.settings.ui.experience
    val inherited=MaterialTheme.typography
    val family=com.emmanuela.launcher.ui.appearance.launcherFont(if(e.homeFont=="Inherit")data.settings.font else e.homeFont,data.settings.customFont,inherited.bodyLarge.fontFamily,if(e.homeStyleOverride)e.homeItalic else data.settings.ui.italic,if(e.homeTextWeight=="Inherit")inherited.bodyLarge.fontWeight?:FontWeight.Normal else com.emmanuela.launcher.ui.appearance.weightOf(e.homeTextWeight))
    val typography=remember(inherited,family,e.homeStyleOverride,e.homeTextWeight,e.homeItalic,e.homeTextScale){
        if(!e.homeStyleOverride)inherited else com.emmanuela.launcher.ui.appearance.styledTypography(inherited,family,
            if(e.homeTextWeight=="Inherit")null else com.emmanuela.launcher.ui.appearance.weightOf(e.homeTextWeight),e.homeItalic,e.homeTextScale)
    }
    MaterialTheme(typography=typography){ProvideTextStyle(typography.bodyLarge){HomeScreenContent(data,apps,model,now,battery,action,settings,favorites,editApp,launch,arranging)}}
}
@Composable
private fun HomeScreenContent(data: LauncherData, apps: List<LaunchableApp>, model: LauncherViewModel, now: Long, battery: Int,
                       action: (String) -> Unit, settings: () -> Unit, favorites: () -> Unit, editApp: (String) -> Unit, launch: (LaunchableApp) -> Unit, arranging:Boolean=false) {
    val p = data.settings
    val u = p.ui
    val context = LocalContext.current
    val screenTime by model.screenTime.collectAsStateWithLifecycle()
    val usageAllowed by model.usageAllowed.collectAsStateWithLifecycle()
    val date = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
    val wallpaperDay=date.minusMinutes(u.v2.wallpaperSwitchMinute.toLong()).toLocalDate().toEpochDay()
    val wallpaperSeed=if(u.experience.wallpaperOrder=="Random") (wallpaperDay*1103515245L+12345L).xor(wallpaperDay ushr 7) else wallpaperDay
    val photo=if(p.wallpapers.isEmpty())null else p.wallpapers[if(p.dailyWallpaper)Math.floorMod(wallpaperSeed,p.wallpapers.size.toLong()).toInt()else 0]
    var lastReminder by rememberSaveable(date.toLocalDate().toString()){mutableLongStateOf(0L)}
    LaunchedEffect(screenTime,u.experience.gentleReminder,u.screenTimeLimit){
        if(!arranging&&u.experience.gentleReminder&&screenTime!=null&&screenTime!!>=u.screenTimeLimit*60_000L*u.experience.warningPercent/100 && now-lastReminder>=u.experience.reminderMinutes*60_000L){lastReminder=now;model.error.value="A moment for yourself? Today's screen time is near your goal."}
    }
    val image by produceState<ImageBitmap?>(null, photo) { value = WallpaperCache.load(context, photo) }
    val scope = rememberCoroutineScope()
    val taps = remember(scope) { HomeTapRouter(scope) }
    val config = LocalViewConfiguration.current
    val density = LocalDensity.current
    val latestAction by rememberUpdatedState(action)
    val latestSettings by rememberUpdatedState(settings)
    val latestPrefs by rememberUpdatedState(p)
    SideEffect {
        taps.doubleEnabled = u.v2.gesturesEnabled && p.doubleTapAction != "none"; taps.tripleEnabled = u.v2.gesturesEnabled && u.tripleTapAction != "none"
        taps.timeout = config.doubleTapTimeoutMillis; taps.radius = with(density) { 48.dp.toPx() }
        taps.dispatch = { count -> latestAction(if (count == 3) latestPrefs.ui.tripleTapAction else latestPrefs.doubleTapAction) }
    }
    val regions = LocalGestureRegions.current
    DisposableEffect(taps, regions) { regions.onClaim = taps::cancel; onDispose { taps.cancel(); regions.onClaim = {} } }
    CompositionLocalProvider(LocalHomeTapRouter provides taps) {
        Box(Modifier.fillMaxSize().homeClick("Home", {}, { if(latestPrefs.ui.v2.gesturesEnabled) latestAction(latestPrefs.ui.v2.holdAction) })) {
            image?.let { val style=p.styleForPhoto(photo);WallpaperLayer(it,u.v2.copy(cropX=style.x,cropY=style.y,cropZoom=style.zoom),style.blur,style.dim,Modifier.fillMaxSize(),style.fit) }
            val baseColor = if(u.experience.textColor!=0L)Color(u.experience.textColor).copy(alpha=u.experience.textOpacity)else if (image != null) Color.White.copy(alpha=u.experience.textOpacity) else MaterialTheme.colorScheme.onBackground
            val textColor = if (u.widgetColor != 0L) Color(u.widgetColor) else baseColor
            val family = when (u.widgetFont) { "Sans" -> FontFamily.SansSerif; "Serif" -> FontFamily.Serif; "Monospace" -> FontFamily.Monospace; else -> MaterialTheme.typography.bodyLarge.fontFamily }
            CompositionLocalProvider(LocalContentColor provides baseColor) {
                Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Spacer(Modifier.weight(1f))
                    val align = when (p.alignment) { "Left" -> Alignment.Start; "Center" -> Alignment.CenterHorizontally; else -> Alignment.End }
                    val visible = data.favorites.filter { data.policies[it.id.substringBefore('/')]?.hidden != true }.take(p.favoriteCount)
                    Column(Modifier.weight(2f).fillMaxWidth().then(if(!u.experience.homeAlphabet||arranging)Modifier.verticalScroll(rememberScrollState())else Modifier), verticalArrangement = Arrangement.Center, horizontalAlignment = align) {
                        if(u.experience.homeAlphabet && !arranging) HomeAlphabetArea(data,apps,launch,editApp) else {
                        if (visible.isEmpty() && p.favoriteCount > 0) Text(localized("+ Choose favorites"), Modifier.homeClick("Choose favorites", favorites).padding(12.dp))
                        visible.forEach { favorite ->
                            val app = apps.find { it.id == favorite.id }
                            val name = app?.let{badgeLabel(it)} ?: data.appMetadata[favorite.id]?.alias?.takeIf { it.isNotBlank() } ?: "Unavailable app"
                            Text(name, style = MaterialTheme.typography.headlineSmall,
                                textAlign = when(p.alignment) { "Left" -> TextAlign.Start; "Center" -> TextAlign.Center; else -> TextAlign.End },
                                color = if (app != null) baseColor else baseColor.copy(alpha = .45f),
                                modifier = Modifier.homeClick(name, { app?.let(launch) }, { editApp(favorite.id) }).padding(vertical = u.experience.homeSpacing.dp), maxLines = 2)
                        }
                    }
                    }
                    if (u.bottomShortcuts) Row(
                        Modifier.fillMaxWidth(u.bottomBarWidth).height(u.bottomBarHeight.dp).align(Alignment.CenterHorizontally).offset(y=(-u.bottomBarOffset).dp).padding(horizontal=u.bottomBarPadding.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BottomShortcut(u.v2.shortcutMode,u.v2.leftVector,actionName(u.leftShortcut,apps),baseColor,u.bottomBarTextScale,u.bottomBarWeight,u.bottomBarPadding){action(u.leftShortcut)}
                        BottomShortcut(u.v2.shortcutMode,u.v2.rightVector,actionName(u.rightShortcut,apps),baseColor,u.bottomBarTextScale,u.bottomBarWeight,u.bottomBarPadding){action(u.rightShortcut)}
                    }
                    // Always reachable even if every custom gesture/shortcut is reassigned.
                    Text(localized("Settings"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp).homeClick("Launcher settings", settings).padding(12.dp))
                }
            }
            HomeWidgets(data,model,now,battery,action,Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).padding(bottom=100.dp),arranging)
        }
    }
}

@Composable
private fun BottomShortcut(mode:String,vector:String,label:String,color:Color,textScale:Float,weight:String,padding:Float,click:()->Unit) {
    val fontWeight=when(weight){"Light"->FontWeight.Light;"Medium"->FontWeight.Medium;"Bold"->FontWeight.Bold;else->FontWeight.Normal}
    Box(Modifier.sizeIn(minWidth=48.dp,minHeight=40.dp).homeClick(label,click).padding(horizontal=padding.dp.coerceAtMost(24.dp),vertical=6.dp),contentAlignment=Alignment.Center){
        if(mode=="Icon") VectorSymbol(vector,color,Modifier.size((28f * textScale).dp)) else Text(label,color=color,fontSize=(14f * textScale).sp,fontWeight=fontWeight,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

