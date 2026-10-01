@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.emmanuela.launcher.ui.apps

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.AlphabetIndex
import com.emmanuela.launcher.data.AlphabetSection
import com.emmanuela.launcher.data.ExperiencePreferences
import com.emmanuela.launcher.data.IndexedApps
import com.emmanuela.launcher.data.LaunchableApp
import com.emmanuela.launcher.platform.badgeLabel
import com.emmanuela.launcher.ui.components.AppIcon
import com.emmanuela.launcher.ui.navigation.ownsGesture
import com.emmanuela.launcher.ui.navigation.alphabetEffect


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun IndexedAppList(apps: List<LaunchableApp>, modifier: Modifier, launch: (LaunchableApp) -> Unit, manage: (String) -> Unit,
                   prepared: IndexedApps? = null, showAlphabet: Boolean = true, animation: String = "Bubble", fullAlphabet: Boolean = false, experience:ExperiencePreferences=ExperiencePreferences(), model:LauncherViewModel?=null) {
    val effect=if(experience.reduceMotion)"None" else animation
    val density=androidx.compose.ui.platform.LocalDensity.current
    val view=androidx.compose.ui.platform.LocalView.current
    val fallback by produceState(IndexedApps(emptyList(), emptyList()), apps, prepared) {
        value = prepared ?: kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { AlphabetIndex.build(apps) }
    }
    val indexed = prepared ?: fallback
    val rail = remember(indexed.sections,fullAlphabet) {
        if(!fullAlphabet) indexed.sections else {
            val active=indexed.sections.associateBy{it.label}
            (('A'..'Z').map{it.toString()} + indexed.sections.map{it.label}.filter{it !in "A".."Z"}).distinct().map{active[it] ?: AlphabetSection(it,-1)}
        }
    }
    var fingerY by remember { mutableFloatStateOf(-1f) }
    var railHeight by remember { mutableIntStateOf(1) }
    val haptic=com.emmanuela.launcher.platform.rememberLauncherHapticTick()
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    var pressedLetter by remember { mutableStateOf<String?>(null) }
    val selected by remember(indexed, list) { derivedStateOf {
        indexed.sections.lastOrNull { it.firstIndex <= list.firstVisibleItemIndex }?.label
    } }
    val opacity = remember { Animatable(1f) }
    LaunchedEffect(effect){if(effect=="None"){scrollJob?.cancel();opacity.snapTo(1f)}}
    val bubbleScale by animateFloatAsState(if (effect == "Bubble" && pressedLetter != null) 1f else 0f, tween(if(effect=="None")0 else 180), label = "Letter bubble")
    fun jump(section: AlphabetSection) {
        if (section.firstIndex < 0 || pressedLetter == section.label) return
        if(experience.alphabetHaptics)haptic()
        pressedLetter = section.label
        scrollJob?.cancel()
        scrollJob = scope.launch {
            list.scrollToItem(section.firstIndex)
            if (effect != "None") { opacity.snapTo(.65f); opacity.animateTo(1f, tween(140)) } else opacity.snapTo(1f)
        }
    }
    Box(modifier) {
        Row(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.weight(1f).fillMaxHeight().graphicsLayer { alpha = opacity.value }, state = list) {
                items(indexed.apps, key = { it.id }) { app ->
                    Row(Modifier.fillMaxWidth().combinedClickable(onClick={launch(app)},onLongClickLabel="Manage app",onLongClick={manage(app.id)})
                        .padding(vertical=experience.appSpacing.dp),verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=when(experience.appAlignment){"Center"->Arrangement.Center;"Right"->Arrangement.End;else->Arrangement.Start}){
                        if(experience.appIcons&&model!=null){AppIcon(app,model);Spacer(Modifier.width(12.dp))}
                        Text(badgeLabel(app),style=MaterialTheme.typography.headlineSmall.let{it.copy(fontSize=it.fontSize*experience.appTextScale)},modifier=Modifier.semantics{contentDescription="${app.label}, ${app.originalLabel}, ${app.packageName}"})
                    }
                }
            }
            if (showAlphabet && rail.isNotEmpty()) {
                var height by remember { mutableIntStateOf(1) }
                Column(Modifier.width(40.dp).fillMaxHeight().ownsGesture("alphabet")
                    .onSizeChanged { height = it.height.coerceAtLeast(1); railHeight = height }
                    .pointerInput(rail, height, effect,experience.alphabetHaptics) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            fun choose(y: Float) {
                                val index = (y / height * rail.size).toInt().coerceIn(rail.indices)
                                fingerY = y
                                jump(rail[index])
                            }
                            down.consume(); choose(down.position.y)
                            try {
                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    choose(change.position.y); change.consume()
                                } while (true)
                            } finally { pressedLetter = null; fingerY = -1f }
                        }
                    }, horizontalAlignment = Alignment.CenterHorizontally) {
                    rail.forEachIndexed { ordinal, section ->
                        val highlighted = (pressedLetter ?: selected) == section.label
                        val target = if (highlighted) MaterialTheme.colorScheme.onSurface else Color.Transparent
                        val background = if (effect == "None") target else animateColorAsState(target, label = "Alphabet selection").value
                        Box(Modifier.weight(1f).fillMaxWidth().graphicsLayer {
                            val center=(ordinal+.5f)*railHeight/rail.size
                            val wave=if(effect=="Wave"&&fingerY>=0f)(1f-kotlin.math.abs(center-fingerY)/(railHeight/rail.size*3f)).coerceIn(0f,1f)else 0f
                            translationX=-with(density){experience.appWaveStrength.dp.toPx()}*wave;scaleX=1f+.85f*wave;scaleY=scaleX
                            alpha=if(section.firstIndex<0).3f else 1f
                        }.semantics {
                            contentDescription = "Jump to ${section.label}"
                            role = Role.Button
                            if(section.firstIndex>=0) onClick { jump(section); pressedLetter = null; true }
                        }, contentAlignment = Alignment.Center) {
                            Text(section.label, fontSize = 12.sp,
                                color = if (highlighted) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.background(background, CircleShape).padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
        if (showAlphabet && effect == "Bubble") Box(Modifier.align(Alignment.CenterEnd).padding(end = 52.dp).size(64.dp)
            .graphicsLayer { scaleX = bubbleScale; scaleY = bubbleScale; alpha = bubbleScale }
            .background(MaterialTheme.colorScheme.onSurface, CircleShape), contentAlignment = Alignment.Center) {
            Text(pressedLetter ?: selected.orEmpty(), fontSize = 30.sp, color = MaterialTheme.colorScheme.surface)
        }
    }
}
