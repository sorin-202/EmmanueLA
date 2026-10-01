@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.emmanuela.launcher.ui.folders

import com.emmanuela.launcher.data.FolderPlacement
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.ui.components.VectorSymbol


import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clipToBounds

@Composable
fun EditableFolderGrid(data: LauncherData, modifier: Modifier, open: (String) -> Unit, place: (String, Int, Boolean) -> Unit) {
    val p = data.settings
    val snapPixels=with(LocalDensity.current){p.ui.v2.folderSnap.dp.toPx()}
    val freeform = p.ui.folderLayout in listOf("Freeform","Canvas")
    val occupied = remember(data.folders, freeform) {
        if (freeform) FolderPlacement.cells(data.folders) else data.folders.mapIndexed { i, f -> i to f }.toMap()
    }
    val count = if (freeform) ((occupied.keys.maxOrNull() ?: -1) + 5).coerceAtMost(2001) else occupied.size
    val bounds = remember { mutableMapOf<Int, Rect>() }
    var dragged by remember { mutableStateOf<String?>(null) }
    var delta by remember { mutableStateOf(Offset.Zero) }
    val currentPlace by rememberUpdatedState(place)
    val columns = when (p.ui.folderLayout) { "List" -> GridCells.Fixed(1); "Freeform", "Canvas" -> GridCells.Fixed(2); else -> GridCells.Adaptive(p.tileSize.dp) }
    BoxWithConstraints(modifier) {
    LazyVerticalGrid(columns, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(p.ui.experience.folderSpacing.dp), verticalArrangement = Arrangement.spacedBy(p.ui.experience.folderSpacing.dp)) {
        items(count, key = { occupied[it]?.id ?: "empty:$it" }) { cell ->
            val folder = occupied[cell]
            DisposableEffect(cell) { onDispose { bounds.remove(cell) } }
            if (folder == null) Box(Modifier.fillMaxWidth().height(150.dp).onGloballyPositioned { bounds[cell] = it.boundsInRoot() }
                .border(1.dp, if (dragged != null) MaterialTheme.colorScheme.outline.copy(alpha = .5f) else Color.Transparent))
            else {
                val accent = if (folder.color == 0L) MaterialTheme.colorScheme.onSurface else Color(folder.color)
                val fill = if (p.ui.folderFill) accent else Color.Transparent
                val foreground = if (p.ui.folderFill) { if (accent.luminance() > .45f) Color.Black else Color.White } else MaterialTheme.colorScheme.onSurface
                BoxWithConstraints(Modifier.animateItem().fillMaxWidth().zIndex(if(dragged==folder.id)100f else 0f)) {
                val tileModifier = if(folder.widthDp>0f)Modifier.width(folder.widthDp.dp.coerceAtMost(maxWidth)) else Modifier.fillMaxWidth()
                Column(tileModifier.height(folder.heightDp.dp).clipToBounds().onGloballyPositioned { bounds[cell] = it.boundsInRoot() }
                    .zIndex(if (dragged == folder.id) 1f else 0f)
                    .graphicsLayer { translationX = if (dragged == folder.id) delta.x else 0f; translationY = if (dragged == folder.id) delta.y else 0f }
                    .pointerInput(folder.id, cell, freeform) {
                        var origin = Offset.Zero
                        detectDragGesturesAfterLongPress(onDragStart = {
                            dragged = folder.id; delta = Offset.Zero; origin = bounds[cell]?.center ?: Offset.Zero
                        }, onDragCancel = { dragged = null; delta = Offset.Zero }, onDragEnd = {
                            val point = origin + delta
                            val target = bounds.entries.filter { it.key != cell && it.value.inflate(snapPixels).contains(point) }.minByOrNull{(it.value.center-point).getDistance()}?.key
                            if (target != null && target != cell) currentPlace(folder.id, target, freeform)
                            dragged = null; delta = Offset.Zero
                        }, onDrag = { change, offset -> change.consume(); delta += offset })
                    }.border(1.dp, accent).background(fill).clickable { open(folder.id) }.padding(16.dp)) {
                    if (p.symbolPlacement == "Top") VectorSymbol(folder.icon,if(p.ui.folderFill)foreground else accent,Modifier.padding(bottom=12.dp).size(p.symbolSize.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (p.symbolPlacement == "Left") VectorSymbol(folder.icon,if(p.ui.folderFill)foreground else accent,Modifier.padding(end=8.dp).size(p.symbolSize.dp))
                        Text(folder.name, color = foreground, maxLines=2, overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier=Modifier.weight(1f), style = MaterialTheme.typography.titleMedium.let{it.copy(fontSize=it.fontSize*p.ui.experience.folderLabelScale)})
                    }
                    val amount = folder.apps.count { data.policies[it.substringBefore('/')]?.hidden != true }
                    Text(if (folder.passwordHash.isNotEmpty()) "Locked folder" else "$amount apps", color = foreground.copy(alpha = .75f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
                }
            }
        }
    }
}
}
