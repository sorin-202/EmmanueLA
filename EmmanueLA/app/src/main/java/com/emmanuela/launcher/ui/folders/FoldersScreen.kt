@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.emmanuela.launcher.ui.folders

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.AppFolder
import com.emmanuela.launcher.data.LaunchableApp
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.platform.badgeLabel
import com.emmanuela.launcher.ui.components.AppIcon
import com.emmanuela.launcher.ui.components.Header
import com.emmanuela.launcher.ui.components.localized


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
fun FoldersScreen(data: LauncherData, home: () -> Unit, shuffle: () -> Unit, open: (String) -> Unit, create: () -> Unit, place: (String, Int, Boolean) -> Unit,settings:()->Unit) {
    val p = data.settings
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).padding(horizontal = 20.dp)) {
        Header("Organized Folders", home) { TextButton(onClick = create, modifier = Modifier.semantics { contentDescription = "New folder" }) { Text(localized("+"), fontSize = 30.sp) } }
        if (data.folders.isEmpty()) Column(Modifier.padding(top = 80.dp)) {
            Text(localized("A place for everything."), style = MaterialTheme.typography.headlineSmall)
            Text(localized("Create your first folder."), Modifier.padding(top = 12.dp))
        }
        EditableFolderGrid(data, Modifier.weight(1f).fillMaxWidth(), open, place)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if(p.ui.experience.homeEnabled)TextButton(onClick = home) { Text(localized("Home")) }
            else TextButton(onClick=settings){Text(localized("Settings"))}
            if (data.settings.ui.appListEnabled&&!p.ui.experience.homeAlphabet) TextButton(onClick = shuffle) { Text(localized("All Apps →")) }
        }
    }
}
@Composable
fun FolderScreen(folder: AppFolder, apps: List<LaunchableApp>, launch: (LaunchableApp) -> Unit, back: () -> Unit, edit: () -> Unit, editApp: (String) -> Unit, showIcons: Boolean, model: LauncherViewModel) {
    var arranging by rememberSaveable(folder.id){mutableStateOf(false)}
    val sort=model.data.value.settings.ui.experience.folderSort
    val entries=remember(folder,apps,sort){if(folder.manualOrder||sort=="Manual")folder.apps.mapNotNull{id->apps.find{it.id==id}}else apps.filter{it.id in folder.apps}}
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).padding(horizontal=24.dp)){
        Header(folder.name,back){TextButton(onClick=edit){Text(localized("Edit"))}}
        TextButton(onClick={arranging=!arranging}){Text(localized(if(arranging)"Done" else "Arrange apps"))}
        if(arranging)Text(localized("Hold and drag to snap apps into place"),style=MaterialTheme.typography.bodySmall)
        if(entries.isEmpty())Text(localized("No available apps. Tap Edit to choose apps."),Modifier.padding(vertical=24.dp))
        val iconMode=when(folder.layout){"Text"->false;"Icons"->true;else->showIcons}
        MagneticFolderApps(folder,entries,arranging,iconMode,model,launch,editApp,Modifier.weight(1f).fillMaxWidth())
    }
}
