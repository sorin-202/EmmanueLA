package com.emmanuela.launcher.ui.appearance

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.settings.SettingRow


import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AlbumDirectoryRow(data:LauncherData,model:LauncherViewModel){val context=LocalContext.current;val scope=rememberCoroutineScope();var busy by remember{mutableStateOf(false)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){uri->if(uri!=null)scope.launch{busy=true
        try{val photos=withContext(Dispatchers.IO){context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);DocumentFile.fromTreeUri(context,uri)?.listFiles()?.filter{it.isFile&&it.type?.startsWith("image/")==true}?.sortedBy{it.name}?.take(50)?.map{it.uri.toString()}.orEmpty()}
            if(photos.isEmpty())model.error.value="No readable photos in this album."else model.settings{it.copy(wallpapers=photos,ui=it.ui.copy(v2=it.ui.v2.copy(wallpaperAlbum=uri.toString())))}}
        catch(_:Exception){model.error.value="Could not read the selected album."}finally{busy=false}}}
    SettingRow(if(busy)"Reading album…"else "Photo directory",data.settings.ui.v2.wallpaperAlbum){if(!busy)picker.launch(data.settings.ui.v2.wallpaperAlbum.takeIf{it.isNotEmpty()}?.let(Uri::parse))}
}
@Composable
fun HexColorRow(label:String,color:Long,save:(Long)->Unit){var show by remember{mutableStateOf(false)};SettingRow(label,"#%06X".format(color and 0xFFFFFF)){show=true}
    if(show){var text by remember(color){mutableStateOf("%06X".format(color and 0xFFFFFF))};AlertDialog(onDismissRequest={show=false},title={Text(label)},text={OutlinedTextField(text,{text=it.removePrefix("#").take(6)},label={Text(localized("RRGGBB"))})},confirmButton={TextButton(enabled=text.matches(Regex("[0-9a-fA-F]{6}")),onClick={save(0xFF000000L or text.toLong(16));show=false}){Text(localized("Apply"))}},dismissButton={TextButton(onClick={show=false}){Text(localized("Cancel"))}})}
}
