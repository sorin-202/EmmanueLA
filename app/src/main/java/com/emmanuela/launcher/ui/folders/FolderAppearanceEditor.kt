package com.emmanuela.launcher.ui.folders

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emmanuela.launcher.data.AppFolder
import com.emmanuela.launcher.data.FolderAppearance
import com.emmanuela.launcher.data.FolderImageGrants
import com.emmanuela.launcher.folderColors
import com.emmanuela.launcher.ui.components.VectorSymbol
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.settings.ToggleRow
import com.emmanuela.launcher.ui.settings.selectionMarker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun FolderAppearanceEditor(folder: AppFolder, enabled: Boolean, fill: Boolean, colorChanged: (Long) -> Unit, solidChanged: (Boolean) -> Unit,
                                    imageChanged: (String) -> Unit, busyChanged: (Boolean) -> Unit, grantAcquired: (String) -> Unit,
                                    coverIconChanged: (Boolean) -> Unit, coverNameChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var red by rememberSaveable(folder.id, folder.color) { mutableStateOf(((folder.color shr 16) and 255).toString()) }
    var green by rememberSaveable(folder.id, folder.color) { mutableStateOf(((folder.color shr 8) and 255).toString()) }
    var blue by rememberSaveable(folder.id, folder.color) { mutableStateOf((folder.color and 255).toString()) }
    val rgb = FolderAppearance.rgb(red, green, blue)
    var failure by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var editingRgb by rememberSaveable(folder.id) { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            loading = true; busyChanged(true); failure = null
            scope.launch {
                var acquired = false
                var accepted = false
                try {
                    withContext(Dispatchers.IO) {
                        val alreadyGranted = context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
                        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        acquired = !alreadyGranted
                    }
                    if (acquired) grantAcquired(uri.toString())
                    if (FolderImages.load(context, uri.toString()) == null) {
                        failure = "Cannot read this image. Choose a supported local image up to 40 MB and 100 megapixels."
                    } else { imageChanged(uri.toString()); accepted = true }
                } catch (cancel: CancellationException) { throw cancel
                } catch (_: Exception) {
                    failure = "Cannot keep access to this image. Choose a document from another provider."
                } finally {
                    if (acquired && !accepted) FolderImageGrants.cleanup(context, listOf(uri.toString()))
                    loading = false; busyChanged(false)
                }
            }
        }
    }
    Text(localized("Preview"), style = MaterialTheme.typography.labelMedium)
    FolderSurface(folder, fill, Modifier.fillMaxWidth().height(116.dp).testTag("folder-appearance-preview"), cover = true) { coverVisible ->
        Column(Modifier.padding(16.dp)) {
            if (!coverVisible || folder.showCoverIcon) VectorSymbol(folder.icon, folderForeground(folder, fill), Modifier.size(24.dp).testTag("folder-cover-icon:${folder.id}"))
            if (!coverVisible || folder.showCoverName) Text(folder.name.ifBlank { "Folder" }, color = folderForeground(folder, fill), style = MaterialTheme.typography.titleMedium)
        }
    }
    if (folder.imageUri.isNotEmpty() && folderImage(folder.imageUri) == null && !loading) {
        Text(localized("Image unavailable or loading. A readable fallback is shown when the document cannot be opened."), style = MaterialTheme.typography.bodySmall)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(enabled = enabled && !loading, onClick = { picker.launch(arrayOf("image/*")) }) { Text(localized(if (loading) "Opening image…" else "Choose cover photo")) }
        if (folder.imageUri.isNotEmpty()) TextButton(enabled = enabled && !loading, onClick = { imageChanged("") }) { Text(localized("Remove image")) }
    }
    if (folder.imageUri.isNotEmpty()) {
        ToggleRow("Show folder icon on cover", folder.showCoverIcon, enabled && !loading, coverIconChanged)
        ToggleRow("Show folder name on cover", folder.showCoverName, enabled && !loading, coverNameChanged)
        Text(localized("If the photo is unavailable, the name and icon reappear. Images are center cropped; the original document is unchanged."), style = MaterialTheme.typography.bodySmall)
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        folderColors.forEachIndexed { index, value ->
            val checked = folder.color == value && folder.imageUri.isEmpty()
            Box(Modifier.size(48.dp).selectionMarker(checked).semantics {
                contentDescription = if (index == 0) "Monochrome" else "Folder color $index"; selected = checked
            }.clickable(enabled && !loading) { colorChanged(value); imageChanged("") }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(if (checked) 22.dp else 15.dp).background(if (value == 0L) MaterialTheme.colorScheme.onSurface else Color(value), CircleShape))
            }
        }
    }
    if(folder.imageUri.isEmpty())ToggleRow("Solid color background", folder.solidBackground, enabled && !loading, solidChanged)
    TextButton(enabled = enabled && !loading, onClick = { editingRgb = !editingRgb }) { Text(if(editingRgb)localized("Done") else "RGB · ${localized("Custom")}") }
    if(editingRgb) {
        Text("Custom RGB (0–255)", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Triple("R", red, { value: String -> red = value }), Triple("G", green, { value: String -> green = value }), Triple("B", blue, { value: String -> blue = value })).forEach { (label, value, change) ->
                OutlinedTextField(value, { if (it.length <= 3) change(it) }, label = { Text(label) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = value.toIntOrNull()?.let { it in 0..255 } != true, enabled = enabled && !loading, modifier = Modifier.weight(1f))
            }
        }
        if (rgb == null) Text("Enter a whole number from 0 to 255 in each channel.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        TextButton(enabled = enabled && !loading && rgb != null, onClick = { colorChanged(requireNotNull(rgb)); imageChanged("") }) { Text("Apply RGB") }
    }
    failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}
