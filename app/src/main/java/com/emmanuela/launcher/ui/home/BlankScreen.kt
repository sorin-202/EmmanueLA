package com.emmanuela.launcher.ui.home

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.ui.components.localized
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A deliberate empty state; Settings remains reachable even with all pages disabled. */
@Composable
fun BlankScreen(data:LauncherData,model:LauncherViewModel,settings:()->Unit){
    var editing by remember{mutableStateOf(false)}
    var note by remember(data.settings.note){mutableStateOf(data.settings.note)}
    Box(Modifier.fillMaxSize().safeDrawingPadding()){
        Column(Modifier.align(Alignment.Center).padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Text("EmmanueLA",style=MaterialTheme.typography.headlineLarge)
            Text(localized("Minimal. Personal. Yours."),Modifier.padding(top=12.dp))
            TextButton(onClick={editing=true}){Text(localized("Note to self"))}
            if(data.settings.note.isNotBlank())Text(data.settings.note,Modifier.padding(top=12.dp),maxLines=5,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        TextButton(onClick=settings,modifier=Modifier.align(Alignment.BottomCenter)){Text(localized("Settings"))}
    }
    if(editing)AlertDialog(onDismissRequest={editing=false},title={Text(localized("Note to self"))},text={OutlinedTextField(note,{note=it.take(10000)},modifier=Modifier.fillMaxWidth())},confirmButton={TextButton(onClick={model.settings{it.copy(note=note)};editing=false}){Text(localized("Save"))}},dismissButton={TextButton(onClick={editing=false}){Text(localized("Cancel"))}})
}
