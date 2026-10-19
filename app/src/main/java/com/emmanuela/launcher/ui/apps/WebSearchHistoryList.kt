package com.emmanuela.launcher.ui.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.emmanuela.launcher.data.AppFolder
import com.emmanuela.launcher.data.WebSearchEngine
import com.emmanuela.launcher.ui.components.VectorSymbol
import com.emmanuela.launcher.ui.components.localized

@Composable
fun WebSearchHistoryList(queries: List<String>, search: (String) -> Unit, clear: () -> Unit, modifier: Modifier = Modifier,
    engine: WebSearchEngine = WebSearchEngine.ANDROID_DEFAULT, chooseEngine: (WebSearchEngine) -> Unit = {}) {
    var choosingEngine by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(localized("Type after / and press Search"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
        TextButton(onClick = { choosingEngine = true }) { Text("${localized("Search engine")}: ${localized(engine.title)}") }
        if (queries.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(localized("Recent web searches"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = clear) { Text(localized("Clear history")) }
            }
            LazyColumn(Modifier.fillMaxWidth()) {
                items(queries, key = { it }) { query ->
                    Row(Modifier.fillMaxWidth().clickable { search(query) }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("↗", modifier = Modifier.padding(end = 12.dp))
                        Text(query, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
    if (choosingEngine) AlertDialog(onDismissRequest = { choosingEngine = false }, title = { Text(localized("Search engine")) }, text = {
        Column {
            Text(localized("Android default uses the system web search handler, with DuckDuckGo in your default browser as a fallback."), style = MaterialTheme.typography.bodySmall)
            WebSearchEngine.entries.forEach { option ->
                TextButton(onClick = { chooseEngine(option); choosingEngine = false }) { Text(localized(option.title)) }
            }
        }
    }, confirmButton = { TextButton(onClick = { choosingEngine = false }) { Text(localized("Cancel")) } })
}

/** Folder matches never render their children. Opening delegates to the normal authenticated path. */
@Composable
fun CompactFolderSearchResult(folder: AppFolder, open: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
        contentDescription = "Folder: ${folder.name}${if (folder.isProtected) ", protected" else ""}"
    }.clickable { open(folder.id) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        VectorSymbol("folder", MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(folder.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(localized(if (folder.isProtected) "Protected folder" else "Folder"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
