package com.emmanuela.launcher.ui.folders

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.ui.settings.selectionMarker
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.ui.components.VectorSymbol
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** One authentication path for assigning and removing apps from protected folders. */
@Composable
fun FolderMembershipPicker(data:LauncherData,model:LauncherViewModel,selected:Set<String>,enabled:Boolean=true,change:(Set<String>)->Unit){
    val unlocked by model.unlockedFolders.collectAsStateWithLifecycle()
    var unlocking by remember{mutableStateOf<String?>(null)}
    fun toggle(id:String){change(if(id in selected)selected-id else selected+id)}
    data.folders.forEach{folder->
        Row(Modifier.fillMaxWidth().clickable(enabled=enabled){
            if(folder.isProtected&&folder.id !in unlocked)unlocking=folder.id else toggle(folder.id)
        }.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
            Checkbox(folder.id in selected,null,enabled=enabled)
            VectorSymbol(folder.icon,MaterialTheme.colorScheme.onSurface)
            Text(folder.name,Modifier.weight(1f).padding(start=12.dp))
            if(folder.isProtected)VectorSymbol("lock",MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    unlocking?.let{id->FolderUnlockDialog(id,model,{unlocking=null}){unlocking=null;toggle(id)}}
}
