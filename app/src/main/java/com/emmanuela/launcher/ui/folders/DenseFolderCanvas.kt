package com.emmanuela.launcher.ui.folders

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.ui.components.VectorSymbol
import kotlinx.coroutines.launch

@Composable
fun DenseFolderCanvas(data:LauncherData,modifier:Modifier,open:(String)->Unit,place:(String,Int,Int)->Unit){
    val p=data.settings;val density=LocalDensity.current;val scroll=rememberScrollState();val scope=rememberCoroutineScope()
    var dragged by remember{mutableStateOf<String?>(null)};var delta by remember{mutableStateOf(Offset.Zero)}
    var dragOrigin by remember{mutableStateOf(Offset.Zero)}
    var settling by remember{mutableStateOf(false)};val snap=remember{Animatable(Offset.Zero,Offset.VectorConverter)}
    var pending by remember{mutableStateOf<Pair<String,Int>?>(null)}
    val latestPlace by rememberUpdatedState(place)
    BoxWithConstraints(modifier){
        val columns=(maxWidth.value/56f).toInt().coerceIn(4,8);val gap=p.ui.experience.folderSpacing.dp.coerceAtMost(maxWidth/(columns*4))
        val unit=((maxWidth-gap*(columns-1))/columns).coerceAtLeast(1.dp)
        val cellPixels=with(density){(unit+gap).toPx()}
        val cells=remember(data.folders,columns,p.ui.v2.folderWidthUnits,p.ui.v2.folderHeightUnits){FolderGridLayout.pack(data.folders,columns,p.ui.v2.folderWidthUnits,p.ui.v2.folderHeightUnits)}
        val currentCells by rememberUpdatedState(cells)
        LaunchedEffect(pending,data.folders){val target=pending?:return@LaunchedEffect
            if(data.folders.find{it.id==target.first}?.gridCell!=target.second)kotlinx.coroutines.delay(2000)
            pending=null;settling=false;dragged=null;delta=Offset.Zero
        }
        val rows=cells.maxOfOrNull{it.row+it.height}?:1
        val total=(unit+gap)*rows
        @Composable fun tile(c:FolderCell,mod:Modifier){
            val f=c.folder;val accent=if(f.color==0L)MaterialTheme.colorScheme.onSurface else Color(f.color)
            val foreground=if(p.ui.folderFill){if(accent.luminance()>.45f)Color.Black else Color.White}else MaterialTheme.colorScheme.onSurface
            Column(mod.border(1.dp,accent).background(if(p.ui.folderFill)accent else Color.Transparent).padding(6.dp)){
                VectorSymbol(f.icon,foreground,Modifier.size(p.symbolSize.dp.coerceAtMost(unit)))
                Text(f.name,color=foreground,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,style=MaterialTheme.typography.bodyMedium)
                Text(if(f.isProtected)"Locked"else "${f.apps.count{data.policies[it.substringBefore('/')]?.hidden!=true}}",color=foreground,style=MaterialTheme.typography.labelSmall)
            }
        }
        Box(Modifier.fillMaxSize().verticalScroll(scroll)){
            Box(Modifier.fillMaxWidth().height(total).pointerInput(columns,cellPixels){
                detectDragGesturesAfterLongPress(onDragStart={point->if(!settling){val x=(point.x/cellPixels).toInt();val y=(point.y/cellPixels).toInt();val hit=currentCells.firstOrNull{it.covers(x,y)};dragged=hit?.folder?.id;dragOrigin=Offset((hit?.column?:0)*cellPixels,(hit?.row?:0)*cellPixels);delta=Offset.Zero}},onDrag={change,amount->if(dragged!=null&&!settling){change.consume();delta+=amount}},onDragCancel={dragged=null;delta=Offset.Zero},onDragEnd={
                    val id=dragged;val source=currentCells.find{it.folder.id==id}
                    if(source!=null){val x=(source.column+delta.x/cellPixels).toInt().coerceIn(0,columns-source.width);val y=(source.row+delta.y/cellPixels).toInt().coerceIn(0,2000/columns-source.height);val target=Offset((x-source.column)*cellPixels,(y-source.row)*cellPixels);val start=delta
                        scope.launch{snap.snapTo(start);settling=true;snap.animateTo(target,tween(if(p.ui.experience.folderAnimation=="Off"||p.ui.experience.reduceMotion)0 else 140));pending=source.folder.id to (y*columns+x);latestPlace(source.folder.id,y*columns+x,columns)}}
                })
            }){
                cells.forEach{c->key(c.folder.id){val x=(unit+gap)*c.column;val y=(unit+gap)*c.row
                    val offset by androidx.compose.animation.core.animateOffsetAsState(Offset(with(density){x.toPx()},with(density){y.toPx()}),tween(if(p.ui.experience.folderAnimation=="Off"||p.ui.experience.reduceMotion)0 else 140),label="Folder cell position")
                    tile(c,Modifier.width(unit*c.width+gap*(c.width-1)).height(unit*c.height+gap*(c.height-1)).zIndex(if(dragged==c.folder.id)100f else 0f).graphicsLayer{
                        val move=if(dragged==c.folder.id)if(settling)snap.value else delta else Offset.Zero
                        translationX=(if(dragged==c.folder.id)dragOrigin.x else offset.x)+move.x;translationY=(if(dragged==c.folder.id)dragOrigin.y else offset.y)+move.y
                        shadowElevation=if(dragged==c.folder.id)8.dp.toPx()else 0f
                    }.clickable(enabled=dragged==null){open(c.folder.id)})
                }}
            }
        }
    }
}
