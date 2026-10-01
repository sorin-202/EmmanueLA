package com.emmanuela.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview(name="EmmanueLA outline vectors",widthDp=420,heightDp=940,showBackground=true,backgroundColor=0xFF101018)
@Composable
fun VectorPackPreview(){MaterialTheme(colorScheme=darkColorScheme()){Column(Modifier.fillMaxSize().background(Color(0xFF101018)).padding(12.dp)){
    Text("EmmanueLA · ${vectorNames.size} vectors",Modifier.padding(bottom=16.dp),color=Color.White)
    vectorNames.chunked(6).forEach{names->Row(Modifier.fillMaxWidth()){names.forEach{name->Column(Modifier.weight(1f).height(92.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){VectorSymbol(name,Color(0xFFCDD9BD),Modifier.size(28.dp));Text(name,Modifier.padding(top=12.dp),fontSize=10.sp,color=Color(0xFFF6F0E8))}}}}
}}}
