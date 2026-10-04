@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.emmanuela.launcher.ui.folders

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.platform.badgeLabel
import com.emmanuela.launcher.ui.components.AppIcon
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@Composable
fun MagneticFolderApps(folder:AppFolder,entries:List<LaunchableApp>,arranging:Boolean,icons:Boolean,model:LauncherViewModel,
    launch:(LaunchableApp)->Unit,manage:(String)->Unit,modifier:Modifier){
    val density=LocalDensity.current
    val grid=rememberLazyGridState()
    val bounds=remember{mutableMapOf<String,Rect>()}
    var viewport by remember{mutableStateOf(Rect.Zero)}
    var dragged by remember{mutableStateOf<LaunchableApp?>(null)}
    var origin by remember{mutableStateOf(Rect.Zero)}
    var delta by remember{mutableStateOf(Offset.Zero)}
    val state by model.data.collectAsStateWithLifecycle()
    val spacing=state.settings.ui.experience.folderSpacing
    val snap=with(density){state.settings.ui.v2.folderSnap.dp.toPx()}
    var optimisticOrder by remember(folder.id){mutableStateOf<List<String>?>(null)}
    val displayed=remember(entries,optimisticOrder){optimisticOrder?.let{order->val catalog=entries.associateBy{it.id};order.mapNotNull(catalog::get)}?:entries}
    val currentEntries by rememberUpdatedState(displayed)
    val scope=rememberCoroutineScope()
    LaunchedEffect(entries){if(entries.map{it.id}==optimisticOrder)optimisticOrder=null}
    LaunchedEffect(arranging){if(!arranging){dragged=null;delta=Offset.Zero}}
    LaunchedEffect(dragged?.id){
        var previous=withFrameNanos{it}
        if(dragged!=null)while(true){
            val now=withFrameNanos{it};val seconds=((now-previous)/1_000_000_000f).coerceIn(0f,.05f);previous=now
            val y=origin.center.y+delta.y
            val edge=with(density){48.dp.toPx()}
            val speed=when{y<viewport.top+edge->-420f;y>viewport.bottom-edge->420f;else->0f}
            if(speed!=0f)grid.scrollBy(speed*density.density*seconds)
        }
    }
    @Composable fun tile(app:LaunchableApp,mod:Modifier){
        if(folder.layout=="Grid")Column(mod.padding(spacing.dp),horizontalAlignment=Alignment.CenterHorizontally){if(icons)AppIcon(app,model);Text(badgeLabel(app))}
        else Row(mod.padding(vertical=spacing.dp),verticalAlignment=Alignment.CenterVertically){if(icons){AppIcon(app,model);Spacer(Modifier.width(12.dp))};Text(badgeLabel(app),style=MaterialTheme.typography.headlineSmall)}
    }
    val gesture=if(arranging)Modifier.pointerInput(folder.id,arranging){
            detectDragGesturesAfterLongPress(onDragStart={local->
                val point=local+viewport.topLeft
                val item=bounds.entries.firstOrNull{it.value.contains(point)}
                dragged=currentEntries.firstOrNull{it.id==item?.key};origin=item?.value?:Rect.Zero;delta=Offset.Zero
            },onDrag={change,amount->if(dragged!=null){change.consume();delta+=amount}},onDragCancel={dragged=null;delta=Offset.Zero},onDragEnd={
                val app=dragged
                val point=origin.center+delta
                val target=bounds.entries.filter{it.key!=app?.id&&it.value.inflate(snap).contains(point)}.minByOrNull{(it.value.center-point).getDistance()}?.key
                if(app!=null&&target!=null){
                    val order=ReorderRules.move(currentEntries.map{it.id},app.id,target)
                    optimisticOrder=order
                    scope.launch{if(!model.moveFolderApps(folder.id,order))optimisticOrder=null}
                }
                dragged=null;delta=Offset.Zero
            })
        } else Modifier
    Box(modifier.onGloballyPositioned{viewport=it.boundsInRoot()}.then(gesture)){
        LazyVerticalGrid(GridCells.Fixed(if(folder.layout=="Grid")2 else 1),state=grid,modifier=Modifier.fillMaxSize()){
            items(displayed,key={it.id}){app->
                DisposableEffect(app.id){onDispose{bounds.remove(app.id)}}
                val touch=if(arranging)Modifier else Modifier.combinedClickable(onClick={launch(app)},onLongClick={manage(app.id)})
                tile(app,Modifier.animateItem().fillMaxWidth().onGloballyPositioned{bounds[app.id]=it.boundsInRoot()}.graphicsLayer{alpha=if(dragged?.id==app.id)0f else 1f}
                    .then(touch))
            }
        }
        dragged?.let{app->
            tile(app,Modifier.width(with(density){origin.width.toDp()}).height(with(density){origin.height.toDp()}).zIndex(100f)
                .graphicsLayer{translationX=origin.left-viewport.left+delta.x;translationY=origin.top-viewport.top+delta.y;shadowElevation=with(density){8.dp.toPx()}}
                .background(MaterialTheme.colorScheme.surface).border(1.dp,MaterialTheme.colorScheme.primary))
        }
    }
}
