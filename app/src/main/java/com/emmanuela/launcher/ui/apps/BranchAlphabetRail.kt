package com.emmanuela.launcher.ui.apps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emmanuela.launcher.data.ExperiencePreferences
import com.emmanuela.launcher.platform.rememberLauncherHapticTick
import com.emmanuela.launcher.ui.navigation.ownsGesture

/** Precomputed section positions; touch events never scan the app catalogue. */
@Composable
fun BranchAlphabetRail(positions:Map<String,Int>,animation:String,e:ExperiencePreferences,jump:(Int)->Unit){
    val rail=remember(positions){(('A'..'Z').map{it.toString()}+positions.keys.filter{it !in "A".."Z"}).distinct()}
    var height by remember{mutableIntStateOf(1)};var finger by remember{mutableFloatStateOf(-1f)};var selected by remember{mutableStateOf<String?>(null)}
    val haptic=rememberLauncherHapticTick();val density=LocalDensity.current
    val latestJump by rememberUpdatedState(jump)
    val effect=if(e.reduceMotion)"None"else animation
    fun choose(index:Int,y:Float){finger=y;val letter=rail[index];val target=positions[letter]?:return;if(letter==selected)return;selected=letter;if(e.alphabetHaptics)haptic();latestJump(target)}
    Column(Modifier.width(40.dp).fillMaxHeight().ownsGesture("alphabet").onSizeChanged{height=it.height.coerceAtLeast(1)}.pointerInput(rail,positions,height,e.alphabetHaptics){
        awaitEachGesture{val down=awaitFirstDown();down.consume();choose((down.position.y/height*rail.size).toInt().coerceIn(rail.indices),down.position.y)
            try{while(true){val event=awaitPointerEvent();val change=event.changes.firstOrNull{it.id==down.id}?:break;if(!change.pressed)break;choose((change.position.y/height*rail.size).toInt().coerceIn(rail.indices),change.position.y);change.consume()}}finally{finger=-1f;selected=null}
        }
    },horizontalAlignment=Alignment.CenterHorizontally){rail.forEachIndexed{index,letter->
        val wave=if(effect=="Wave"&&finger>=0)(1f-kotlin.math.abs((index+.5f)*height/rail.size-finger)/(height.toFloat()/rail.size*3)).coerceIn(0f,1f)else 0f
        val scale by animateFloatAsState(if(effect=="Bubble"&&letter==selected)1.65f else 1f,tween(100),label="Branch letter selection")
        Box(Modifier.weight(1f).fillMaxWidth().graphicsLayer{translationX=-with(density){e.appWaveStrength.dp.toPx()}*wave;scaleX=scale+.85f*wave;scaleY=scaleX;alpha=if(letter in positions)1f else .3f}.semantics{contentDescription="Jump to $letter";role=Role.Button;if(letter in positions)onClick{positions[letter]?.let(latestJump);true}},contentAlignment=Alignment.Center){
            Text(letter,fontSize=12.sp,color=if(letter==selected)MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.background(if(letter==selected)MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Transparent,CircleShape).padding(horizontal=6.dp,vertical=2.dp))
        }
    }}
}
