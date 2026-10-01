package com.emmanuela.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.selectionMarker(selected:Boolean):Modifier=background(if(selected)MaterialTheme.colorScheme.primary.copy(alpha=.16f)else Color.Transparent,RoundedCornerShape(12.dp))
    .border(1.dp,if(selected)MaterialTheme.colorScheme.primary else Color.Transparent,RoundedCornerShape(12.dp))
@Composable
fun SubSettingRow(label:String,value:String="",onClick:()->Unit){
    Row(Modifier.fillMaxWidth().padding(start=12.dp)){
        Text("↳",Modifier.padding(top=17.dp,end=8.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.weight(1f)){SettingRow(label,value,onClick)}
    }
}
