package com.emmanuela.launcher.ui.apps

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.LaunchableApp
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.folders.FolderUnlockDialog


import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun BulkActions(selected:Set<String>,data:LauncherData,apps:List<LaunchableApp>,model:LauncherViewModel,edit:(Set<String>)->Unit) {
    var expanded by remember{mutableStateOf(false)}
    var dialog by rememberSaveable{mutableStateOf("")}
    var tags by rememberSaveable{mutableStateOf("")}
    var folderUnlock by remember{mutableStateOf<String?>(null)}
    var queued by rememberSaveable{mutableStateOf(arrayListOf<String>())}
    var current by rememberSaveable{mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope()
    val uninstall=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        current=null;model.refresh(true)
    }
    Box {
        TextButton(enabled=selected.isNotEmpty(),onClick={expanded=true}){Text(localized("⋯"))}
        DropdownMenu(expanded,{expanded=false}) {
            listOf("Block","Hide","Set Timer","Add Tags","Assign to Folder","Batch Uninstall").forEach { item ->
                DropdownMenuItem(text={Text(item)},onClick={
                    expanded=false
                    when(item){
                        "Block"->scope.launch{model.changePolicies(selected){it.copy(blocked=true)}}
                        "Hide"->scope.launch{model.changePolicies(selected){it.copy(hidden=true)}}
                        "Set Timer"->edit(selected)
                        else->dialog=item
                    }
                })
            }
        }
    }
    if(dialog=="Add Tags") AlertDialog(onDismissRequest={dialog=""},title={Text("Add tags to ${selected.size} apps")},text={OutlinedTextField(tags,{tags=it},label={Text(localized("Comma / space separated tags"))})},confirmButton={TextButton(onClick={scope.launch{if(model.bulkTags(selected,tags)){dialog="";tags=""}}}){Text(localized("Save"))}},dismissButton={TextButton(onClick={dialog=""}){Text(localized("Cancel"))}})
    if(dialog=="Assign to Folder") AlertDialog(onDismissRequest={dialog=""},title={Text(localized("Choose folder"))},text={LazyColumn(Modifier.heightIn(max=360.dp)){
        items(data.folders,key={it.id}){folder->TextButton(onClick={
            if(folder.isProtected&&folder.id !in model.unlockedFolders.value)folderUnlock=folder.id
            else scope.launch{if(model.bulkFolder(selected,folder.id))dialog=""}
        }){Text(folder.name)}}
    }},confirmButton={TextButton(onClick={dialog=""}){Text(localized("Cancel"))}})
    folderUnlock?.let{id->FolderUnlockDialog(id,model,{folderUnlock=null}){folderUnlock=null;scope.launch{if(model.bulkFolder(selected,id))dialog=""}}}
    if(dialog=="Batch Uninstall") AlertDialog(onDismissRequest={dialog=""},title={Text("Uninstall ${selected.size} apps?")},text={Text(localized("Android will ask you to confirm each uninstall. App data may be deleted. Protected/system apps may not be removable. You can stop the queue at any time."))},confirmButton={TextButton(onClick={queued=ArrayList(selected.sorted());dialog=""}){Text(localized("Review queue"))}},dismissButton={TextButton(onClick={dialog=""}){Text(localized("Cancel"))}})
    if(queued.isNotEmpty()&&current==null) AlertDialog(onDismissRequest={queued=arrayListOf()},title={Text("Uninstall queue · ${queued.size} remaining")},text={Text(apps.find{it.packageName==queued.first()}?.label?:queued.first())},confirmButton={TextButton(onClick={
        val pkg=queued.first();queued=ArrayList(queued.drop(1));current=pkg
        try{uninstall.launch(Intent(Intent.ACTION_DELETE,Uri.parse("package:$pkg")).putExtra(Intent.EXTRA_RETURN_RESULT,true))}
        catch(_:Exception){current=null;model.error.value="Android could not open the uninstall screen."}
    }){Text(localized("Open Android confirmation"))}},dismissButton={TextButton(onClick={queued=arrayListOf()}){Text(localized("Stop"))}})
}
