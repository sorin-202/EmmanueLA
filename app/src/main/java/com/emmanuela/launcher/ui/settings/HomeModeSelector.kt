package com.emmanuela.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emmanuela.launcher.data.HomeModes
import com.emmanuela.launcher.ui.components.localized
import kotlin.math.roundToInt

/** Keep drag previews local and persist once when a slider gesture or labeled selection completes. */
@Composable
fun HomeModeSelector(mode: String, onModeChange: (String) -> Unit) {
    val initial = HomeModes.values.indexOf(mode).coerceAtLeast(0)
    var position by remember(mode) { mutableFloatStateOf(initial.toFloat()) }
    val preview = HomeModes.values[position.roundToInt().coerceIn(HomeModes.values.indices)]
    val sliderLabel = localized("Home layout")
    val sliderState = localized(preview)
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(localized("Home layout"), style = MaterialTheme.typography.titleMedium)
        HomeModePreview(preview, Modifier.fillMaxWidth().padding(top = 12.dp))
        Slider(position, { position = it }, valueRange = 0f..2f, steps = 1,
            onValueChangeFinished = {
                val selected = HomeModes.values[position.roundToInt().coerceIn(HomeModes.values.indices)]
                if (selected != mode) onModeChange(selected)
            },
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = sliderLabel
                stateDescription = sliderState
            })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            HomeModes.values.forEachIndexed { index, option ->
                Text(localized(option), style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (preview == option) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (preview == option) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .background(if (preview == option) MaterialTheme.colorScheme.onSurface.copy(alpha = .07f) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(12.dp))
                        .selectable(selected = preview == option, role = Role.RadioButton, onClick = {
                            position = index.toFloat()
                            if (option != mode) onModeChange(option)
                        }).wrapContentSize(Alignment.Center))
            }
        }
        Text(localized(when (preview) {
            HomeModes.NORMAL -> "Your favorite apps, with icons."
            HomeModes.ALPHABET -> "All your apps in a list, with an alphabet rail."
            else -> "Your favorite apps, as a simple text list."
        }), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun HomeModePreview(mode: String, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    Box(modifier.height(116.dp).background(ink.copy(alpha = .035f), RoundedCornerShape(16.dp))
        .border(1.dp, ink.copy(alpha = .10f), RoundedCornerShape(16.dp))
        .semantics { contentDescription = "Home layout preview: $mode" }, contentAlignment = Alignment.Center) {
        when (mode) {
            HomeModes.NORMAL -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        repeat(3) { column ->
                            Box(Modifier.size(22.dp).background(ink.copy(alpha = .12f + (row + column) * .035f), CircleShape)
                                .border(1.dp, ink.copy(alpha = .22f), CircleShape))
                        }
                    }
                }
            }
            HomeModes.ALPHABET -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(38.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("Browser", "Camera", "Messages").forEach { Text(localized(it), style = MaterialTheme.typography.bodyMedium) }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    listOf("A", "B", "C", "D").forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = ink.copy(alpha = .6f)) }
                }
            }
            else -> Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf("Camera", "Messages", "Music").forEach { Text(localized(it), style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}
