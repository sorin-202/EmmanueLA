@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.emmanuela.launcher.ui.folders

import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.ui.components.VectorSymbol
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The drag preview is a sibling of the lazy grid, outside its item clipping layer. */
@Composable
fun EditableFolderGrid(data:LauncherData,modifier:Modifier,open:(String)->Unit,place:(String,Int,Boolean)->Unit){
    val p=data.settings
    val density=LocalDensity.current
    val snap=with(density){p.ui.v2.folderSnap.dp.toPx()}
    val freeform=p.ui.folderLayout in listOf("Freeform","Canvas")
    val occupied=remember(data.folders,freeform){if(freeform)FolderPlacement.cells(data.folders)else data.folders.mapIndexed{i,f->i to f}.toMap()}
    val currentOccupied by rememberUpdatedState(occupied)
    val currentPlace by rememberUpdatedState(place)
    val count=if(freeform)((occupied.keys.maxOrNull()?:-1)+5).coerceAtMost(2001)else occupied.size
    val bounds=remember{mutableMapOf<Int,Rect>()}
    var viewport by remember{mutableStateOf(Rect.Zero)}
    var dragged by remember{mutableStateOf<AppFolder?>(null)}
    var from by remember{mutableIntStateOf(-1)}
    var origin by remember{mutableStateOf(Rect.Zero)}
    var delta by remember{mutableStateOf(Offset.Zero)}
    var settling by remember{mutableStateOf(false)}
    var pendingCell by remember{mutableStateOf<Int?>(null)}
    val dropOffset=remember{Animatable(Offset.Zero,Offset.VectorConverter)}
    val scope=rememberCoroutineScope()
    fun clear(){dragged=null;delta=Offset.Zero;settling=false;pendingCell=null}
    LaunchedEffect(occupied,pendingCell){val target=pendingCell;if(target!=null&&occupied[target]?.id==dragged?.id)clear()}
    val columns=when(p.ui.folderLayout){"List"->GridCells.Fixed(1);"Freeform","Canvas"->GridCells.Fixed(2);else->GridCells.Adaptive(p.tileSize.dp)}
    @Composable fun tile(folder:AppFolder,mod:Modifier){
        val accent=if(folder.color==0L)MaterialTheme.colorScheme.onSurface else Color(folder.color)
        val foreground=if(p.ui.folderFill){if(accent.luminance()>.45f)Color.Black else Color.White}else MaterialTheme.colorScheme.onSurface
        Column(mod.border(1.dp,accent).background(if(p.ui.folderFill)accent else Color.Transparent).padding(16.dp)){
            if(p.symbolPlacement=="Top")VectorSymbol(folder.icon,if(p.ui.folderFill)foreground else accent,Modifier.padding(bottom=12.dp).size(p.symbolSize.dp))
            Row(verticalAlignment=Alignment.CenterVertically){
                if(p.symbolPlacement=="Left")VectorSymbol(folder.icon,if(p.ui.folderFill)foreground else accent,Modifier.padding(end=8.dp).size(p.symbolSize.dp))
                Text(folder.name,color=foreground,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleMedium.let{it.copy(fontSize=it.fontSize*p.ui.experience.folderLabelScale)})
            }
            val amount=folder.apps.count{data.policies[it.substringBefore('/')]?.hidden!=true}
            Text(if(folder.isProtected)"Locked folder" else "$amount apps",color=foreground.copy(alpha=.75f),style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp))
        }
    }
    Box(modifier.onGloballyPositioned{viewport=it.boundsInRoot()}.pointerInput(freeform,snap){
        detectDragGesturesAfterLongPress(onDragStart={local->
            if(!settling){val point=local+viewport.topLeft;val hit=bounds.entries.firstOrNull{it.value.contains(point)&&currentOccupied[it.key]!=null}
                if(hit!=null){from=hit.key;origin=hit.value;dragged=currentOccupied[hit.key];delta=Offset.Zero}}
        },onDrag={change,amount->if(dragged!=null&&!settling){change.consume();delta+=amount}},onDragCancel={if(!settling)clear()},onDragEnd={
            val folder=dragged
            if(folder!=null&&!settling){
                val point=origin.center+delta
                val target=bounds.entries.filter{it.key!=from&&it.value.inflate(snap).contains(point)}.minByOrNull{(it.value.center-point).getDistance()}
                val destination=target?.value?.topLeft?:origin.topLeft
                val start=delta
                settling=true
                scope.launch{
                    dropOffset.snapTo(start)
                    dropOffset.animateTo(destination-origin.topLeft,tween(if(p.ui.experience.folderAnimation=="Off"||p.ui.experience.reduceMotion)0 else 140))
                    if(target!=null){pendingCell=target.key;currentPlace(folder.id,target.key,freeform);delay(1500);if(dragged?.id==folder.id)clear()}else clear()
                }
            }
        })
    }){
        LazyVerticalGrid(columns,modifier=Modifier.fillMaxSize(),userScrollEnabled=dragged==null||settling,contentPadding=PaddingValues(vertical=16.dp),horizontalArrangement=Arrangement.spacedBy(p.ui.experience.folderSpacing.dp),verticalArrangement=Arrangement.spacedBy(p.ui.experience.folderSpacing.dp)){
            items(count,key={occupied[it]?.id?:"empty:$it"}){cell->
                val folder=occupied[cell]
                DisposableEffect(cell,folder?.id){onDispose{bounds.remove(cell)}}
                if(folder==null)Box(Modifier.fillMaxWidth().height(150.dp).onGloballyPositioned{bounds[cell]=it.boundsInRoot()}.border(1.dp,if(dragged!=null)MaterialTheme.colorScheme.outline.copy(alpha=.5f)else Color.Transparent))
                else BoxWithConstraints(Modifier.animateItem().fillMaxWidth()){
                    val width=if(folder.widthDp>0f)Modifier.width(folder.widthDp.dp.coerceAtMost(maxWidth))else Modifier.fillMaxWidth()
                    tile(folder,width.height(folder.heightDp.dp).onGloballyPositioned{bounds[cell]=it.boundsInRoot()}.graphicsLayer{alpha=if(dragged?.id==folder.id)0f else 1f}.clickable(enabled=dragged==null){open(folder.id)})
                }
            }
        }
        dragged?.let{folder->
            tile(folder,Modifier.width(with(density){origin.width.toDp()}).height(with(density){origin.height.toDp()}).zIndex(100f).graphicsLayer{
                val offset=if(settling)dropOffset.value else delta
                translationX=origin.left-viewport.left+offset.x;translationY=origin.top-viewport.top+offset.y
                shadowElevation=with(density){8.dp.toPx()}
            }.background(MaterialTheme.colorScheme.surface))
        }
    }
}
