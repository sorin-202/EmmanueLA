@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.emmanuela.launcher.ui.apps

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.AppSearch
import com.emmanuela.launcher.data.AppSearchIndex
import com.emmanuela.launcher.data.LaunchableApp
import com.emmanuela.launcher.data.Preferences
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.navigation.ownsGesture


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

@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
fun AllApps(index: AppSearchIndex, loading: Boolean, p: Preferences, active: Boolean, model:LauncherViewModel,
                    launch: (LaunchableApp) -> Unit, rememberTags: (String,LaunchableApp) -> Unit, home: () -> Unit, folders: () -> Unit, settings: () -> Unit, editApp: (String) -> Unit, hidden: () -> Unit, openFolder: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val contacts = remember(context) { com.emmanuela.launcher.platform.ContactSearchRepository(context.applicationContext) }
    var permissionRevision by remember { mutableIntStateOf(0) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event -> if(event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) permissionRevision++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val data by model.data.collectAsStateWithLifecycle()
    val webMode = query.trimStart().startsWith("/")
    val folderResults = remember(query, data.folders, p.ui.tagSearch, webMode) {
        if (query.isBlank() || webMode || query.trimStart().startsWith("@") || (p.ui.tagSearch && AppSearch.isTagMode(query))) emptyList()
        else data.folders.mapNotNull { folder ->
            com.emmanuela.launcher.data.SearchRanking.score(com.emmanuela.launcher.data.SearchRanking.folded(query.trim()), listOf(com.emmanuela.launcher.data.SearchRanking.folded(folder.name)))?.let { folder to it }
        }.sortedWith(compareBy({ it.second }, { it.first.name }, { it.first.id })).map { it.first }
    }
    val contactMode = p.ui.experience.contactsSearch && query.trimStart().startsWith("@")
    val contactAllowed = remember(permissionRevision) { contacts.allowed() }
    val contactPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionRevision++
        if(!granted) model.error.value="Contacts access was denied. Enable it in Settings → System → Permissions."
    }
    val contactSnapshot by produceState("" to emptyList<com.emmanuela.launcher.platform.ContactMatch>(), contactMode, contactAllowed, p.ui.experience.contactPackages) {
        value="" to emptyList()
        if(contactMode && contactAllowed) snapshotFlow { query }.debounce(120).mapLatest { text ->
            val matches = try { contacts.search(text.trimStart(), p.ui.experience.contactPackages) }
                catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}
                catch(_:SecurityException){emptyList()}
                catch(_:IllegalArgumentException){emptyList()}
                catch(_:Exception){model.error.value="Contacts search is temporarily unavailable.";emptyList()}
            text to matches
        }.collect { value=it }
    }
    val searchEpoch by model.searchEpoch.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val manager = LocalFocusManager.current
    val searchSnapshot by produceState("" to index.all, index, p.ui.tagSearch, p.ui.experience.searchAliases, p.ui.experience.searchPackages, contactMode) {
        snapshotFlow { if(contactMode) "" else query }.debounce(60).mapLatest { text ->
            text to withContext(Dispatchers.Default) { index.search(text, p.ui.tagSearch, p.ui.experience.searchAliases, p.ui.experience.searchPackages) }
        }.collect { value = it }
    }
    val resolvedQuery = searchSnapshot.first
    val results = searchSnapshot.second
    val suggestions = if(p.ui.tagSuggestions && p.ui.tagSearch && AppSearch.isTagMode(query)) p.ui.v2.recentTags.filter(index::containsTag).take(3) else emptyList()
    var autoOpenedQuery by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query, resolvedQuery, results.apps, active, contactMode, webMode, folderResults, data.folders, p.ui.experience.autoLaunch, p.ui.experience.autoLaunchDelay,p.ui.v2.treeBranch) {
        val normalized = query.trim()
        if (!webMode && folderResults.isEmpty() && !contactMode && p.ui.experience.autoLaunch && active && normalized.isNotEmpty() && resolvedQuery.trim() == normalized && results.apps.size == 1 && (!p.ui.v2.treeBranch||data.folders.none{results.apps.single().id in it.apps}) && autoOpenedQuery != normalized) {
            if(p.ui.experience.autoLaunchDelay>0)kotlinx.coroutines.delay(p.ui.experience.autoLaunchDelay.toLong())
            val app = results.apps.single()
            autoOpenedQuery = normalized
            rememberTags(query, app)
            query = ""
            manager.clearFocus(); keyboard?.hide()
            launch(app)
        } else if (results.apps.size != 1) autoOpenedQuery = ""
    }
    LaunchedEffect(active) { if(!active) query="" }

    LaunchedEffect(searchEpoch,active){if(active&&searchEpoch>0&&p.ui.v2.searchEnabled){focus.requestFocus();keyboard?.show()}}
    LaunchedEffect(active, p.autoKeyboard) { if (active && p.autoKeyboard && p.ui.v2.searchEnabled) { focus.requestFocus(); keyboard?.show() } else if (!active) { manager.clearFocus(); keyboard?.hide() } }
    val accent = if (p.ui.searchColor != 0L) Color(p.ui.searchColor) else if (p.searchAccent) Color(0xFFCA6666) else MaterialTheme.colorScheme.primary
    val cursor = if (p.ui.searchCursorColor != 0L) Color(p.ui.searchCursorColor) else accent
    val field: @Composable () -> Unit = {
        CursorSearchField(query,{query=it},p,Modifier.fillMaxWidth().focusRequester(focus).padding(vertical=8.dp).ownsGesture("app-search"))
        if (suggestions.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).ownsGesture("tag-suggestions")) {
            suggestions.forEach { tag -> TextButton(onClick = {
                query = query.trimEnd().substringBeforeLast(' ', "").let { prefix -> if (prefix.isEmpty()) "#$tag " else "$prefix #$tag " }
            }) { Text("#$tag") } }
        }
    }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = home, modifier = Modifier.semantics { contentDescription = "Home" }) { Text(localized("⌂"), fontSize = 25.sp) }
            Text(localized("All Apps"), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = settings, modifier = Modifier.semantics { contentDescription = "Launcher settings" }) { Text(localized("⚙"), fontSize = 23.sp) }
        }
        if (p.ui.v2.searchEnabled && p.ui.searchPosition == "Top") field()
        if (loading && index.all.apps.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!webMode && !contactMode && !loading && results.apps.isEmpty() && folderResults.isEmpty()) Text(localized("No apps found"), Modifier.padding(vertical = 24.dp))
        if (!webMode && !contactMode && folderResults.isNotEmpty()) {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max=160.dp)) {
                items(folderResults, key={ it.id }) { folder ->
                    TextButton(onClick={ query=""; manager.clearFocus(); keyboard?.hide(); openFolder(folder.id) }) { Text("▦ ${folder.name}") }
                }
            }
        }
        if(webMode) {
            val webQuery = query.trimStart().removePrefix("/").trim()
            TextButton(enabled=webQuery.isNotEmpty(), onClick={
                model.launchPlatform(Intent(Intent.ACTION_WEB_SEARCH).putExtra(android.app.SearchManager.QUERY, webQuery))
                manager.clearFocus(); keyboard?.hide()
            }) { Text(localized("Search the web") + if(webQuery.isEmpty()) "" else ": $webQuery") }
            Spacer(Modifier.weight(1f))
        } else if(contactMode) {
            if(!contactAllowed) {
                Text(localized("Allow contacts access to search names with @."), Modifier.padding(vertical=12.dp))
                TextButton(onClick={
                    contactPermission.launch(android.Manifest.permission.READ_CONTACTS)
                }) { Text(localized("Allow contacts")) }
                TextButton(onClick={com.emmanuela.launcher.platform.SystemDestinations.open(context,listOf(com.emmanuela.launcher.platform.SystemDestinations.appDetails(context)))}) { Text(localized("Open app permissions")) }
            } else if(query.trim().length==1) Text(localized("Type a contact name after @"),Modifier.padding(vertical=12.dp))
            else if(contactSnapshot.first==query && contactSnapshot.second.isEmpty()) Text(localized("No contacts found"),Modifier.padding(vertical=12.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(if(contactSnapshot.first==query)contactSnapshot.second else emptyList(),key={it.id}) { contact ->
                    Column(Modifier.fillMaxWidth().clickable {
                        query=""; manager.clearFocus();keyboard?.hide();model.launchPlatform(contact.intent)
                    }.padding(vertical=14.dp)) {
                        Text(contact.name,style=MaterialTheme.typography.titleMedium)
                        Text(contact.source,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else if(p.ui.v2.treeBranch)TreeBranchList(results.apps,query,model,p.ui.experience.appIcons,{app->rememberTags(query,app);query="";launch(app)},editApp,Modifier.weight(1f).fillMaxWidth())
        else IndexedAppList(results.apps, Modifier.weight(1f).fillMaxWidth(), { app -> rememberTags(query,app);query="";launch(app) }, editApp, results, p.ui.alphabet && (query.isBlank() || (p.ui.tagSearch && AppSearch.isTagMode(query))), p.ui.alphabetAnimation, false, p.ui.experience, model)
        if (p.ui.v2.searchEnabled && p.ui.searchPosition == "Bottom") field()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = hidden) { Text(localized("Hidden Apps")) }
            if(p.ui.v2.foldersEnabled) TextButton(onClick = folders) { Text(localized("Organized Folders →")) }
        }
    }
}
