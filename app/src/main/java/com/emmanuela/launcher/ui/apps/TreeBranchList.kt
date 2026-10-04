@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.emmanuela.launcher.ui.apps

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.ui.components.VectorSymbol
import com.emmanuela.launcher.ui.components.AppIcon
import com.emmanuela.launcher.ui.folders.FolderUnlockDialog
import kotlinx.coroutines.launch

sealed interface BranchRow { val key:String
    data class Folder(val folder:AppFolder,val apps:List<LaunchableApp>):BranchRow{override val key="folder:${folder.id}"}
    data class App(val app:LaunchableApp,val parent:String?):BranchRow{override val key="app:${app.id}"}
}
fun drawerBranches(apps:List<LaunchableApp>,folders:List<AppFolder>, ranked:Boolean=false):List<BranchRow>{
    val owner=mutableMapOf<String,String>();folders.forEach{f->f.apps.forEach{owner.putIfAbsent(it,f.id)}}
    val roots=mutableListOf<BranchRow>()
    folders.forEach{f->val matches=apps.filter{owner[it.id]==f.id};if(matches.isNotEmpty())roots+=BranchRow.Folder(f,matches)}
    apps.filter{it.id !in owner}.forEach{roots+=BranchRow.App(it,null)}
    if(ranked){val positions=apps.mapIndexed{i,app->app.id to i}.toMap();return roots.sortedBy{row->when(row){is BranchRow.App->positions.getValue(row.app.id);is BranchRow.Folder->row.apps.minOf{positions.getValue(it.id)}}}}
    return roots.sortedBy{when(it){is BranchRow.Folder->it.folder.name.lowercase();is BranchRow.App->it.app.label.lowercase()}}
}
@Composable
fun TreeBranchList(apps:List<LaunchableApp>,query:String,model:LauncherViewModel,icons:Boolean,launch:(LaunchableApp)->Unit,manage:(String)->Unit,modifier:Modifier){
    val data by model.data.collectAsStateWithLifecycle()
    val unlocked by model.unlockedFolders.collectAsStateWithLifecycle()
    var expanded by remember{mutableStateOf(emptySet<String>())}
    var unlocking by remember{mutableStateOf<String?>(null)}
    var contextFolder by remember{mutableStateOf<AppFolder?>(null)}
    var pendingMenu by remember{mutableStateOf(false)}
    val roots=remember(apps,data.folders,query){drawerBranches(apps,data.folders,query.isNotBlank())}
    val list=rememberLazyListState();val scrollScope=rememberCoroutineScope()
    var scrollJob by remember{mutableStateOf<kotlinx.coroutines.Job?>(null)}
    val positions=remember(roots,expanded,query,unlocked){buildMap<String,Int>{var index=0;roots.forEach{root->
        val name=when(root){is BranchRow.App->root.app.label;is BranchRow.Folder->root.folder.name}
        val letter=name.firstOrNull()?.uppercaseChar()?.toString()?:"#";putIfAbsent(letter,index);index++
        if(root is BranchRow.Folder&&(!root.folder.isProtected||root.folder.id in unlocked)&&(root.folder.id in expanded||query.trim().isNotEmpty()))index+=root.apps.size
    }}}
    Row(modifier){
    LazyColumn(Modifier.weight(1f).fillMaxHeight(),state=list){roots.forEach{root->when(root){
        is BranchRow.App->item(key=root.key){BranchApp(root.app,false,icons,model,launch,manage)}
        is BranchRow.Folder->{
            val folder=root.folder;val allowed=!folder.isProtected||folder.id in unlocked
            val open=allowed&&(folder.id in expanded||query.trim().isNotEmpty())
            item(key=root.key){Row(Modifier.fillMaxWidth().combinedClickable(onClick={if(!allowed){pendingMenu=false;unlocking=folder.id}else expanded=if(folder.id in expanded)expanded-folder.id else expanded+folder.id},onLongClick={if(!allowed){pendingMenu=true;unlocking=folder.id}else contextFolder=folder}).padding(vertical=14.dp)){
                if(icons){VectorSymbol(folder.icon,MaterialTheme.colorScheme.onSurface);Spacer(Modifier.width(12.dp))}
                Text((if(open)"− "else"+ ")+folder.name,style=MaterialTheme.typography.headlineSmall)
            }}
            if(open)root.apps.forEach{app->item(key="branch:${folder.id}:${app.id}"){BranchApp(app,true,icons,model,launch,manage)}}
        }
    }}}
    if(data.settings.ui.alphabet&&query.isBlank()&&roots.isNotEmpty())BranchAlphabetRail(positions,data.settings.ui.alphabetAnimation,data.settings.ui.experience){index->scrollJob?.cancel();scrollJob=scrollScope.launch{list.scrollToItem(index)}}
    }
    unlocking?.let{id->FolderUnlockDialog(id,model,{unlocking=null}){unlocking=null;if(pendingMenu)contextFolder=data.folders.find{it.id==id}else expanded=expanded+id}}
    contextFolder?.let{DrawerFolderEditor(it,data,model){contextFolder=null}}
}
@Composable
private fun BranchApp(app:LaunchableApp,child:Boolean,icons:Boolean,model:LauncherViewModel,launch:(LaunchableApp)->Unit,manage:(String)->Unit){
    Row(Modifier.fillMaxWidth().combinedClickable(onClick={launch(app)},onLongClick={manage(app.id)}).padding(start=if(child)20.dp else 0.dp,top=12.dp,bottom=12.dp)){
        if(child)Text("└ ",color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(icons){AppIcon(app,model);Spacer(Modifier.width(12.dp))};Text(app.label,style=MaterialTheme.typography.headlineSmall)
    }
}
@Composable
private fun DrawerFolderEditor(folder:AppFolder,data:LauncherData,model:LauncherViewModel,close:()->Unit){
    val catalog by model.apps.collectAsStateWithLifecycle();val scope=rememberCoroutineScope()
    var mode by remember{mutableStateOf("")};var name by remember{mutableStateOf(folder.name)};var members by remember{mutableStateOf(folder.apps.toSet())};var busy by remember{mutableStateOf(false)}
    AlertDialog(onDismissRequest={if(!busy)close()},title={Text(folder.name)},text={Column{
        if(mode.isEmpty())listOf("Add apps","Remove apps","Rename folder","Delete folder").forEach{action->TextButton(onClick={mode=action}){Text(action)}}
        if(mode=="Rename folder")OutlinedTextField(name,{name=it.take(40)},label={Text("Folder name")})
        if(mode in listOf("Add apps","Remove apps"))Column(Modifier.heightIn(max=300.dp).verticalScroll(rememberScrollState())){catalog.filter{data.policies[it.packageName]?.hidden!=true&&(mode=="Add apps"||it.id in folder.apps)}.forEach{app->Row{Checkbox(app.id in members,{yes->members=if(yes)members+app.id else members-app.id});Text(app.label)}}}
        if(mode=="Delete folder")Text("Delete this folder? Apps remain installed.")
    }},confirmButton={if(mode.isNotEmpty())TextButton(enabled=!busy&&name.isNotBlank(),onClick={busy=true;scope.launch{val ok=if(mode=="Delete folder")model.deleteFolder(folder.id)else model.saveFolder(folder.copy(name=name.trim(),apps=folder.apps.filter{it in members}+members.filter{it !in folder.apps}));if(ok)close();busy=false}}){Text(if(mode=="Delete folder")"Delete"else"Save")}},dismissButton={TextButton(enabled=!busy,onClick=close){Text("Cancel")}})
}
