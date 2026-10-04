package com.emmanuela.launcher.ui.home

import com.emmanuela.launcher.data.AppNaming
import com.emmanuela.launcher.data.LaunchableApp
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.platform.badgeLabel
import com.emmanuela.launcher.ui.navigation.homeClick
import com.emmanuela.launcher.ui.navigation.ownsGesture
import com.emmanuela.launcher.ui.navigation.ownsVerticalScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import com.emmanuela.launcher.ui.navigation.alphabetEffect
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeAlphabetArea(data:LauncherData,apps:List<LaunchableApp>,launch:(LaunchableApp)->Unit,manage:(String)->Unit) {
    var section by remember{mutableStateOf("♡")}
    val p=data.settings;val e=p.ui.experience
    val effect=if(e.reduceMotion)"None" else e.homeAlphabetAnimation
    val view=LocalView.current
    val density=androidx.compose.ui.platform.LocalDensity.current
    val list=rememberLazyListState()
    val scope=rememberCoroutineScope()
    val haptic=com.emmanuela.launcher.platform.rememberLauncherHapticTick()
    val letters=remember{listOf("♡","#")+('A'..'Z').map{it.toString()}}
    val sections by produceState<Map<String,List<LaunchableApp>>>(emptyMap(),apps){value=withContext(Dispatchers.Default){apps.groupBy{AppNaming.folded(it.label.trim()).take(1).uppercase()}+("#" to apps.filter{it.tags.isNotEmpty()})}}
    val selectedApps=remember(apps,data.favorites,p.favoriteCount,section,sections){if(section=="♡")data.favorites.take(p.favoriteCount).mapNotNull{f->apps.find{it.id==f.id}}else sections[section].orEmpty()}
    var height by remember{mutableIntStateOf(1)};var finger by remember{mutableFloatStateOf(-1f)}
    fun select(letter:String,initial:Boolean=false){val changed=section!=letter;section=letter;if((changed||initial)&&e.homeAlphabetHaptics)haptic()}
    LaunchedEffect(section){list.scrollToItem(0)}
    val selectionAlpha by animateFloatAsState(if(effect=="Fade"&&finger>=0f).6f else 1f,tween(if(effect=="None")0 else 140),label="Home selection")
    val bubbleScale by animateFloatAsState(if(effect=="Bubble"&&finger>=0f)1f else 0f,tween(if(effect=="None")0 else 160),label="Home letter bubble")
    Box(Modifier.fillMaxSize()) {
    Row(Modifier.fillMaxSize()) {
        val rail:@Composable ()->Unit={Column(Modifier.width(if(e.homeAlphabetStyle=="Compact")28.dp else 36.dp).fillMaxHeight().ownsGesture("home-alphabet").onSizeChanged{height=it.height.coerceAtLeast(1)}.pointerInput(height,e.homeAlphabetHaptics,effect){awaitEachGesture{val down=awaitFirstDown(requireUnconsumed=false,pass=PointerEventPass.Initial);fun choose(y:Float,initial:Boolean=false){finger=y;select(letters[(y/height*letters.size).toInt().coerceIn(letters.indices)],initial)};choose(down.position.y,true);down.consume();try{do{val event=awaitPointerEvent(PointerEventPass.Initial);val change=event.changes.firstOrNull{it.id==down.id}?:break;if(!change.pressed)break;choose(change.position.y);change.consume()}while(true)}finally{finger=-1f}}},horizontalAlignment=Alignment.CenterHorizontally){letters.forEachIndexed{ordinal,letter->
            Box(Modifier.weight(1f).fillMaxWidth().semantics{contentDescription="Apps beginning with $letter";selected=letter==section;onClick{select(letter,true);true}}.graphicsLayer{val wave=if(effect=="Wave"&&finger>=0f)(1f-kotlin.math.abs((ordinal+.5f)*height/letters.size-finger)/(height/letters.size*3f)).coerceIn(0f,1f)else 0f;translationX=(if(e.homeAlphabetPosition=="Right")-with(density){e.homeWaveStrength.dp.toPx()} else with(density){e.homeWaveStrength.dp.toPx()})*wave;scaleX=1f+.85f*wave;scaleY=scaleX},contentAlignment=Alignment.Center){
                Text(letter,fontSize=if(e.homeAlphabetStyle=="Compact")10.sp else 12.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=if(letter==section)1f else if(letter in listOf("♡","#")||sections[letter]?.isNotEmpty()==true).65f else .3f))}
        }}}
        if(e.homeAlphabetPosition=="Left")rail()
        LazyColumn(Modifier.weight(1f).fillMaxHeight().ownsVerticalScroll("home-app-list").graphicsLayer{alpha=selectionAlpha},state=list,verticalArrangement=Arrangement.Center,horizontalAlignment=when(p.alignment){"Left"->Alignment.Start;"Center"->Alignment.CenterHorizontally;else->Alignment.End}){
            items(selectedApps,key={it.id}){app->Text(badgeLabel(app),style=MaterialTheme.typography.headlineSmall,modifier=Modifier.homeClick(app.label,{launch(app)},{manage(app.id)}).padding(vertical=e.homeSpacing.dp))}}
        if(e.homeAlphabetPosition=="Right")rail()
    }
    if(effect=="Bubble")Box(Modifier.align(if(e.homeAlphabetPosition=="Right")Alignment.CenterEnd else Alignment.CenterStart).padding(horizontal=42.dp).size(56.dp).graphicsLayer{scaleX=bubbleScale;scaleY=bubbleScale;alpha=bubbleScale}.background(MaterialTheme.colorScheme.onSurface,CircleShape),contentAlignment=Alignment.Center){Text(section,fontSize=28.sp,color=MaterialTheme.colorScheme.surface)}
    }
}
