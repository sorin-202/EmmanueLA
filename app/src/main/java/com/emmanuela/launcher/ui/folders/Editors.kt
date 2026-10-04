package com.emmanuela.launcher.ui.folders
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.AppFolder
import com.emmanuela.launcher.data.AppMetadata
import com.emmanuela.launcher.data.AppNaming
import com.emmanuela.launcher.data.AppSearch
import com.emmanuela.launcher.data.Favorite
import com.emmanuela.launcher.data.LaunchableApp
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.folderColors
import com.emmanuela.launcher.ui.components.AppIcon
import com.emmanuela.launcher.ui.components.VectorSymbol
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.components.vectorNames
import com.emmanuela.launcher.ui.settings.SectionLabel
import com.emmanuela.launcher.ui.settings.selectionMarker
import com.emmanuela.launcher.ui.settings.ChoiceRow
import com.emmanuela.launcher.ui.settings.SettingsHeading
import com.emmanuela.launcher.ui.settings.ToggleRow


import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun FolderEditor(existing: AppFolder?, apps: List<LaunchableApp>, model: LauncherViewModel,
                 dismiss: () -> Unit, deleted: () -> Unit) {
    val id = rememberSaveable(existing?.id) { existing?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable(id) { mutableStateOf(existing?.name ?: "") }
    var icon by rememberSaveable(id) { mutableStateOf(existing?.icon ?: "folder") }
    var layout by rememberSaveable(id){mutableStateOf(existing?.layout?:"Inherit")}
    var manualOrder by rememberSaveable(id){mutableStateOf(existing?.manualOrder?:false)}
    var tileWidth by rememberSaveable(id){mutableFloatStateOf(existing?.widthDp?:0f)}
    var widthUnits by rememberSaveable(id){mutableIntStateOf(existing?.widthUnits?:0)}
    var heightUnits by rememberSaveable(id){mutableIntStateOf(existing?.heightUnits?:0)}
    var biometricUnlock by rememberSaveable(id){mutableStateOf(existing?.biometricUnlock?:true)}
    var tileHeight by rememberSaveable(id){mutableFloatStateOf(existing?.heightDp?:150f)}
    var color by rememberSaveable(id) { mutableLongStateOf(existing?.color ?: 0L) }
    var selectedApps by rememberSaveable(id) { mutableStateOf(ArrayList(existing?.apps ?: emptyList())) }
    var password by remember { mutableStateOf("") }
    var lockFolder by rememberSaveable(id){mutableStateOf(existing?.isProtected==true)}
    var removePassword by rememberSaveable(id) { mutableStateOf(false) }
    var choosingApps by rememberSaveable(id){mutableStateOf(false)}
    var query by rememberSaveable(id) { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    var deleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val visible = remember(apps, query) { AppSearch.filter(apps, query) }
    AlertDialog(onDismissRequest = { if (!saving) dismiss() }, title = { Text(existing?.name?:"New folder") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { if (it.length <= 40) name = it }, label = { Text(localized("Folder name")) }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
                SectionLabel("Icon")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    vectorNames.forEach { key ->
                        TextButton(onClick = { icon = key }, enabled = !saving, modifier = Modifier.width(48.dp).selectionMarker(icon==key).semantics { contentDescription = "$key icon"; selected = icon == key }, contentPadding = PaddingValues(0.dp)) {
                            VectorSymbol(key,if(icon==key)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.5f))
                        }
                    }
                }
                SectionLabel("Color")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    folderColors.forEachIndexed { index, value ->
                        val swatch = if (value == 0L) MaterialTheme.colorScheme.onSurface else Color(value)
                        Box(Modifier.size(48.dp).selectionMarker(color==value).semantics { contentDescription = if (index == 0) "Monochrome" else "Folder color $index"; selected = color == value }
                            .clickable(enabled = !saving) { color = value }, contentAlignment = Alignment.Center) {
                            Box(Modifier.size(if (color == value) 22.dp else 15.dp).background(swatch, androidx.compose.foundation.shape.CircleShape))
                            if (color == value) Text(localized("✓"), color = Color.Black, fontSize = 12.sp)
                        }
                    }
                }
                ChoiceRow("Width cells (0 = global)",widthUnits.toString(),(0..4).map{it.toString()}){widthUnits=it.toInt()}
                ChoiceRow("Height cells (0 = global)",heightUnits.toString(),(0..4).map{it.toString()}){heightUnits=it.toInt()}
                ToggleRow("Allow device authentication",biometricUnlock){biometricUnlock=it}
                ChoiceRow("In-folder layout",layout,listOf("Inherit","Text","Icons","Grid")){layout=it}
                ToggleRow("Keep custom app order",manualOrder){manualOrder=it}
                SectionLabel("This folder's size")
                Text(if(tileWidth==0f)"Width: use the available column" else "Width: ${tileWidth.toInt()} dp",style=MaterialTheme.typography.bodySmall)
                Slider(if(tileWidth==0f)160f else tileWidth,{tileWidth=it},valueRange=100f..360f,enabled=!saving)
                Text("Height: ${tileHeight.toInt()} dp",style=MaterialTheme.typography.bodySmall)
                Slider(tileHeight,{tileHeight=it},valueRange=100f..360f,enabled=!saving)
                Text(localized("Width fits within this folder's column. Other folders retain their own sizes."),style=MaterialTheme.typography.bodySmall)
                TextButton(onClick={tileWidth=0f;tileHeight=150f},enabled=!saving){Text(localized("Reset this folder's size"))}
                ToggleRow("Lock folder",lockFolder){lockFolder=it}
                if(lockFolder)OutlinedTextField(password, { if (it.length <= 128) password = it }, label = { Text(if (existing?.passwordHash?.isNotEmpty() == true) "New folder password (optional)" else "Folder password (optional)") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
                if (existing?.passwordHash?.isNotEmpty() == true) ToggleRow("Remove password", removePassword, !saving) { removePassword = it }
                Text(localized("Use at least 4 characters. This protects this folder in EmmanueLA, not apps opened elsewhere. Passwords cannot be recovered."), style = MaterialTheme.typography.bodySmall)
                SectionLabel("Apps")
                selectedApps.mapNotNull{id->apps.find{it.id==id}}.forEach{app->Text(app.label,Modifier.padding(vertical=6.dp))}
                TextButton(onClick={choosingApps=!choosingApps}){Text(localized(if(choosingApps)"Done" else "+ Add apps"))}
                if(choosingApps){
                OutlinedTextField(query, { query = it }, label = { Text(localized("Find apps")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 220.dp)) {
                    items(visible, key = { it.id }) { app ->
                        Row(Modifier.fillMaxWidth().toggleable(app.id in selectedApps, enabled = !saving, role = Role.Checkbox,
                            onValueChange = { check -> selectedApps = ArrayList(if (check) selectedApps + app.id else selectedApps - app.id) }).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(app.id in selectedApps, null, enabled = !saving)
                            AppIcon(app, model)
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(app.packageName, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                if (visible.isEmpty()) Text(localized("No matching apps"))
                }
                if (selectedApps.any { saved -> apps.none { it.id == saved } }) TextButton(enabled = !saving, onClick = { selectedApps = ArrayList(selectedApps.filter { saved -> apps.any { it.id == saved } }) }) { Text(localized("Remove unavailable apps")) }
                failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (existing != null) TextButton(onClick = { deleteConfirm = true }, enabled = !saving) { Text(localized("Delete folder")) }
            }
        }, confirmButton = { TextButton(enabled = name.isNotBlank() && !saving && (!lockFolder || password.length >= 4 || (password.isEmpty()&&(biometricUnlock||existing?.passwordHash?.isNotEmpty()==true))), onClick = {
            saving = true
            scope.launch { val ok = model.saveFolder(AppFolder(id, name.trim(), icon, selectedApps.distinct(), color, existing?.passwordSalt ?: "", existing?.passwordHash ?: "", existing?.gridCell,layout,tileWidth,tileHeight,manualOrder,widthUnits,heightUnits,biometricUnlock,lockFolder&&password.isEmpty()&&existing?.passwordHash.isNullOrEmpty()), if(lockFolder)password else "", removePassword||!lockFolder); saving = false
                if (ok) dismiss() else failure = "Save failed. Try again." }
        }) { Text(if (saving) "Saving…" else "Save") } },
        dismissButton = { TextButton(onClick = dismiss, enabled = !saving) { Text(localized("Cancel")) } })
    if (deleteConfirm) AlertDialog(onDismissRequest = { if (!saving) deleteConfirm = false }, title = { Text(localized("Delete folder?")) },
        text = { Text(localized("The apps in this folder will stay installed.")) },
        confirmButton = { TextButton(enabled = !saving, onClick = { saving = true; scope.launch {
            val ok = model.deleteFolder(id); saving = false
            if (ok) deleted() else { deleteConfirm = false; failure = "Delete failed. Try again." }
        } }) { Text(localized("Delete")) } }, dismissButton = { TextButton(enabled = !saving, onClick = { deleteConfirm = false }) { Text(localized("Cancel")) } })
}

private val FavoriteSaver = listSaver<List<Favorite>, String>(
    save = { it.map { favorite -> favorite.id } },
    restore = { it.map(::Favorite) }
)
@Composable
fun FavoritesEditor(data: LauncherData, apps: List<LaunchableApp>, model: LauncherViewModel, back: () -> Unit) {
    var draft by rememberSaveable(stateSaver = FavoriteSaver) { mutableStateOf(data.favorites) }
    var query by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val visible = remember(apps, query) { AppSearch.filter(apps, query) }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).padding(horizontal = 20.dp)) {
        SettingsHeading("Favorites",back,data,model)
        Row { TextButton(enabled = !saving, onClick = {
            saving = true; scope.launch { val ok = model.saveFavorites(draft); saving = false; if (ok) back() else failure = true }
        }) { Text(localized("Save")) } }
        Text("${draft.size}/8 selected · first ${data.settings.favoriteCount} shown on Home", style = MaterialTheme.typography.bodySmall)
        if (failure) Text(localized("Could not save. Try again."), color = MaterialTheme.colorScheme.error)
        LazyColumn(Modifier.weight(1f)) {
            items(draft, key = { "favorite:" + it.id }) { favorite ->
                val index = draft.indexOf(favorite)
                val name = if (data.policies[favorite.id.substringBefore('/')]?.hidden == true) "Hidden app" else apps.find { it.id == favorite.id }?.label ?: data.appMetadata[favorite.id]?.alias?.takeIf { it.isNotBlank() } ?: "Unavailable app"
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, modifier = Modifier.weight(1f).padding(vertical = 16.dp), maxLines = 2)
                    TextButton(enabled = index > 0 && !saving, onClick = { draft = draft.toMutableList().apply { java.util.Collections.swap(this, index, index - 1) } }, modifier = Modifier.semantics { contentDescription = "Move $name up" }) { Text(localized("↑")) }
                    TextButton(enabled = !saving, onClick = { draft = draft.filterNot { it.id == favorite.id } }, modifier = Modifier.semantics { contentDescription = "Remove $name" }) { Text(localized("×")) }
                }
            }
            item { Text(localized("To change a name everywhere, long-press the app in All Apps, or open Settings → App aliases and tags."), style = MaterialTheme.typography.bodySmall); Spacer(Modifier.height(16.dp)) }
            item { OutlinedTextField(query, { query = it }, label = { Text(localized("Find apps")) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            items(visible, key = { "app:" + it.id }) { app ->
                val checked = draft.any { it.id == app.id }
                Row(Modifier.fillMaxWidth().toggleable(checked, enabled = !saving && (checked || draft.size < 8), role = Role.Checkbox,
                    onValueChange = { check -> draft = if (check) draft + Favorite(app.id) else draft.filterNot { it.id == app.id } }).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked, null); Text(app.label)
                }
            }
        }
        TextButton(onClick = back, enabled = !saving) { Text(localized("Cancel")) }
    }
}

@Composable
fun AppMetadataEditor(app: LaunchableApp, current: AppMetadata, data: LauncherData, model: LauncherViewModel, dismiss: () -> Unit) {
    var alias by rememberSaveable(app.id) { mutableStateOf(current.alias) }
    var tags by rememberSaveable(app.id) { mutableStateOf(current.tags.joinToString(" ")) }
    var saving by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    var folderIds by rememberSaveable(app.id) { mutableStateOf(ArrayList(data.folders.filter { app.id in it.apps }.map { it.id })) }
    val scope = rememberCoroutineScope()
    val validation = remember(alias, tags) { runCatching { AppNaming.metadata(alias, tags) }.exceptionOrNull()?.message }
    AlertDialog(onDismissRequest = { if (!saving) dismiss() }, title = { Text(localized("App name and tags")) }, text = {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text(app.originalLabel, style = MaterialTheme.typography.titleMedium)
            Text(app.id, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(alias, { if (it.length <= AppNaming.MAX_ALIAS_LENGTH) alias = it },
                label = { Text(localized("Alias")) }, placeholder = { Text(app.originalLabel) },
                singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
            Text(localized("Shown everywhere in EmmanueLA. Leave blank to use the original app name."), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(tags, { if (it.length <= 1200) tags = it }, label = { Text(localized("Tags")) },
                placeholder = { Text(localized("social-media work")) }, enabled = !saving, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Text(localized("Separate tags with spaces or commas. Search #social-media in All Apps. Tags stay hidden in app lists."), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Text(localized("Folders"), style = MaterialTheme.typography.titleSmall)
            if (data.folders.isEmpty()) Text(localized("No folders created yet."), style = MaterialTheme.typography.bodySmall)
            FolderMembershipPicker(data,model,folderIds.toSet(),!saving){folderIds=ArrayList(it)}
            Text(localized("You can assign the same app to multiple folders. Protected folders must be unlocked before membership can change."), style = MaterialTheme.typography.labelSmall)
            TextButton(enabled = !saving, onClick = { alias = ""; tags = ""; scope.launch { saving=true; if(model.saveAppMetadata(app.id,"",""))dismiss();saving=false } }) { Text(localized("Reset name and tags")) }
            (validation ?: failure)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        TextButton(enabled = !saving && validation == null, onClick = {
            saving = true
            scope.launch {
                val metadataOk = model.saveAppDetails(app.id, alias, tags, folderIds.toSet())
                val foldersOk = metadataOk
                saving = false
                if (metadataOk && foldersOk) dismiss() else failure = "Could not save changes. Unlock protected folders if needed and try again."
            }
        }) { Text(if (saving) "Saving…" else "Save") }
    }, dismissButton = { TextButton(enabled = !saving, onClick = dismiss) { Text(localized("Cancel")) } })
}

@Composable
fun FolderUnlockDialog(id: String, model: LauncherViewModel, dismiss: () -> Unit, success: () -> Unit) {
    val state by model.data.collectAsStateWithLifecycle()
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val auth=com.emmanuela.launcher.platform.rememberDeviceAuthentication(state.settings.ui.experience.authentication,model::authenticationChanged)
    AlertDialog(onDismissRequest = { if (!busy) dismiss() }, title = { Text(localized("Unlock folder")) }, text = {
        Column {
            if(state.folders.find{it.id==id}?.biometricUnlock==true)TextButton(enabled=!busy,onClick={auth("Unlock folder"){if(model.unlockFolderWithDevice(id))success()}}){Text("Use biometrics / device credential")}
            if(state.folders.find{it.id==id}?.passwordHash?.isNotEmpty()==true)OutlinedTextField(password, { if (it.length <= 128) password = it }, label = { Text(localized("Folder password")) }, singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), enabled = !busy)
            if (failure) Text(localized("Incorrect password, or retry delay active."), color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { TextButton(enabled = !busy && password.isNotEmpty(), onClick = {
        busy = true; scope.launch { val ok = model.unlockFolder(id, password); busy = false; if (ok) success() else failure = true }
    }) { Text(if (busy) "Checking…" else "Unlock") } }, dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text(localized("Cancel")) } })
}
