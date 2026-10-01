package com.emmanuela.launcher.ui.appearance

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.data.V2Preferences
import com.emmanuela.launcher.data.PhotoStyle
import com.emmanuela.launcher.data.styleForPhoto
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.emmanuela.launcher.platform.WallpaperCache
import com.emmanuela.launcher.ui.components.localized
import com.emmanuela.launcher.ui.settings.SliderRow
import com.emmanuela.launcher.ui.settings.selectionMarker


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.blur

@Composable
fun WallpaperLayer(image:ImageBitmap,v:V2Preferences,blur:Float,dim:Float,modifier:Modifier=Modifier,fit:String="Fill") {
    Box(modifier.clipToBounds()) {
        Image(image,null,Modifier.fillMaxSize().graphicsLayer{scaleX=v.cropZoom;scaleY=v.cropZoom}.blur(blur.dp),
            contentScale=if(fit=="Fit")ContentScale.Fit else ContentScale.Crop,alignment=BiasAlignment(v.cropX,v.cropY))
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=dim)))
    }
}
@Composable
fun WallpaperEditor(data:LauncherData,model:LauncherViewModel,back:()->Unit) {
    val p=data.settings;val v=p.ui.v2
    var selected by rememberSaveable{mutableIntStateOf(0)}
    val uri=p.wallpapers.getOrNull(selected)?:p.wallpapers.firstOrNull()
    val original=p.styleForPhoto(uri)
    var x by remember(uri){mutableFloatStateOf(original.x)};var y by remember(uri){mutableFloatStateOf(original.y)};var zoom by remember(uri){mutableFloatStateOf(original.zoom)}
    var fit by remember(uri){mutableStateOf(original.fit)}
    var dim by remember(uri){mutableFloatStateOf(original.dim)};var blur by remember(uri){mutableFloatStateOf(original.blur)}
    var controls by rememberSaveable{mutableStateOf(true)}
    val context=LocalContext.current
    val image by produceState<ImageBitmap?>(null,uri){value=null;value=WallpaperCache.load(context,uri)}
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.fillMaxSize().pointerInput(uri){detectTransformGestures{_,pan,scale,_->
            x=(x-pan.x/size.width.coerceAtLeast(1)*2).coerceIn(-1f,1f);y=(y-pan.y/size.height.coerceAtLeast(1)*2).coerceIn(-1f,1f);zoom=(zoom*scale).coerceIn(1f,3f)
        }}) { image?.let{WallpaperLayer(it,v.copy(cropX=x,cropY=y,cropZoom=zoom),blur,dim,Modifier.fillMaxSize(),fit)} }
        if(!controls)TextButton(onClick={controls=true},modifier=Modifier.align(Alignment.BottomCenter).navigationBarsPadding().background(Color.Black.copy(alpha=.7f))){Text(localized("Show controls"),color=Color.White)}
        else Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth(),color=MaterialTheme.colorScheme.surface.copy(alpha=.94f)) {
            Column(Modifier.navigationBarsPadding().padding(horizontal=12.dp,vertical=8.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(localized("Wallpaper preview"),Modifier.align(Alignment.CenterVertically),style=MaterialTheme.typography.labelLarge)
                    TextButton(onClick={controls=false}){Text(localized("Hide controls"))}
                }
                if(p.wallpapers.size>1)Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())){
                    p.wallpapers.forEachIndexed{index,_ ->TextButton(onClick={selected=index},modifier=Modifier.combinedSelection(index==selected)){Text("${localized("Photo")} ${index+1}")}}
                }
                SliderRow("Dimming",dim,0f..1f){dim=it};SliderRow("Blur",blur,0f..30f){blur=it}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
                    TextButton(onClick={fit="Fit";zoom=1f},modifier=Modifier.combinedSelection(fit=="Fit")){Text(localized("Fit"))}
                    TextButton(onClick={fit="Fill"},modifier=Modifier.combinedSelection(fit=="Fill")){Text(localized("Fill"))}
                    TextButton(onClick={x=0f;y=0f;zoom=1f}){Text(localized("Reset"))}
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick=back){Text(localized("Done"))};TextButton(enabled=image!=null&&uri!=null,onClick={
                    val photo=uri?:return@TextButton;val draft=PhotoStyle(x,y,zoom,dim,blur,fit)
                    model.v2Settings{it.copy(photoStyles=it.photoStyles+(photo to draft))}
                    model.error.value="Wallpaper adjustment saved for this photo."
                }){Text(localized("Save"))}}
            }
        }
    }
}
@Composable
private fun Modifier.combinedSelection(selected:Boolean)=this.selectionMarker(selected)
