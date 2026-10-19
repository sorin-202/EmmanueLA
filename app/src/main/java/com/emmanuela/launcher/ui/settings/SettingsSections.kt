package com.emmanuela.launcher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.emmanuela.launcher.ui.components.localized

/** Disclosure state changes presentation only; closing a section never resets its settings. */
@Composable
fun SettingsSection(
    title: String,
    summary: String = "",
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val clickLabel = localized(if (expanded) "Collapse" else "Expand")
    val accessibleTitle = localized(title)
    val expandedState = localized(if(expanded) "Expanded" else "Collapsed")
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .55f)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                    contentDescription = accessibleTitle
                    stateDescription = expandedState
                }.clickable(role = Role.Button, onClickLabel = clickLabel) { expanded = !expanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(localized(title), style = MaterialTheme.typography.titleMedium)
                    if (summary.isNotBlank()) Text(localized(summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp))
                }
                Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 12.dp))
            }
            if (expanded) Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), content = content)
        }
    }
}

@Composable
fun SettingsDestination(title: String, description: String, onClick: () -> Unit) {
    val accessibleTitle = localized(title)
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .55f)
    ) {
        Row(Modifier.fillMaxWidth().semantics { contentDescription = accessibleTitle }
            .clickable(role = Role.Button, onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(localized(title), style = MaterialTheme.typography.titleMedium)
                Text(localized(description), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp))
            }
            Text("›", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 12.dp))
        }
    }
}
