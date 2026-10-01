package com.emmanuela.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp

/** Original 24-unit outline vector pack; no font glyph or bitmap dependency. */
val vectorNames = listOf("apps","settings","folder","star","heart","phone","mail","sun","moon","music","book","camera","lock","home","cloud","check","triangle","circle","plus","work","flag","calendar","air","games","clock","date","battery","location","timer","tag","search","eye","eye-off","shield","leaf","coffee","headphones","bell","bell-off","download","upload","map","compass","globe","image","sliders","terminal","key","bookmark","diamond","archive","wallet","bulb","link")
@Composable
fun VectorSymbol(name:String, color:Color, modifier:Modifier=Modifier.size(24.dp)) {
    Canvas(modifier) {
        scale(size.width/24f,size.height/24f,pivot=Offset.Zero) {
            val stroke=Stroke(1.5f)
            fun line(a:Float,b:Float,c:Float,d:Float)=drawLine(color,Offset(a,b),Offset(c,d),1.5f)
            fun path(points:List<Offset>,close:Boolean=true) { val p=Path();points.forEachIndexed{i,v->if(i==0)p.moveTo(v.x,v.y)else p.lineTo(v.x,v.y)};if(close)p.close();drawPath(p,color,style=stroke) }
            when(name) {
                "heart" -> { val p=Path().apply{moveTo(12f,21f);cubicTo(1f,14f,0f,6f,6f,4f);cubicTo(9f,3f,11f,5f,12f,7f);cubicTo(16f,0f,24f,5f,21f,12f);cubicTo(19f,16f,15f,19f,12f,21f)};drawPath(p,color,style=stroke) }
                "star" -> path(List(10){i->val a=Math.PI*i/5-Math.PI/2;val r=if(i%2==0)10 else 4;Offset(12+(kotlin.math.cos(a)*r).toFloat(),12+(kotlin.math.sin(a)*r).toFloat())})
                "folder" -> path(listOf(Offset(2f,5f),Offset(9f,5f),Offset(12f,8f),Offset(22f,8f),Offset(22f,20f),Offset(2f,20f)))
                "work" -> {drawRect(color,Offset(2f,7f),Size(20f,14f),style=stroke);drawRect(color,Offset(8f,3f),Size(8f,4f),style=stroke);line(2f,13f,22f,13f)}
                "air" -> path(listOf(Offset(12f,1f),Offset(14f,10f),Offset(22f,15f),Offset(22f,18f),Offset(14f,15f),Offset(14f,21f),Offset(10f,21f),Offset(10f,15f),Offset(2f,18f),Offset(2f,15f),Offset(10f,10f)))
                "games" -> {drawRoundRect(color,Offset(2f,6f),Size(20f,13f),androidx.compose.ui.geometry.CornerRadius(4f),style=stroke);line(5f,12f,11f,12f);line(8f,9f,8f,15f);drawCircle(color,1f,Offset(17f,10f));drawCircle(color,1f,Offset(19f,14f))}
                "mail" -> {drawRect(color,Offset(2f,5f),Size(20f,14f),style=stroke);line(2f,5f,12f,13f);line(12f,13f,22f,5f)}
                "sun","settings" -> { drawCircle(color,5f,Offset(12f,12f),style=stroke);repeat(8){i->val a=i*Math.PI/4;line(12+(kotlin.math.cos(a)*8).toFloat(),12+(kotlin.math.sin(a)*8).toFloat(),12+(kotlin.math.cos(a)*11).toFloat(),12+(kotlin.math.sin(a)*11).toFloat())} }
                "moon" -> { val p=Path().apply{moveTo(17f,2f);cubicTo(0f,0f,0f,24f,19f,20f);cubicTo(7f,20f,5f,6f,17f,2f)};drawPath(p,color,style=stroke) }
                "phone" -> {drawRoundRect(color,Offset(6f,2f),Size(12f,20f),androidx.compose.ui.geometry.CornerRadius(2f),style=stroke);line(10f,19f,14f,19f)}
                "camera" -> {drawRect(color,Offset(2f,6f),Size(20f,15f),style=stroke);drawCircle(color,4f,Offset(12f,13f),style=stroke);line(7f,3f,17f,3f)}
                "book" -> {drawRect(color,Offset(3f,3f),Size(18f,18f),style=stroke);line(12f,3f,12f,21f)}
                "music" -> {line(10f,4f,10f,18f);line(10f,4f,20f,2f);line(20f,2f,20f,16f);drawCircle(color,3f,Offset(7f,18f),style=stroke);drawCircle(color,3f,Offset(17f,16f),style=stroke)}
                "lock" -> {drawRect(color,Offset(4f,10f),Size(16f,12f),style=stroke);drawArc(color,180f,180f,false,Offset(7f,2f),Size(10f,16f),style=stroke)}
                "home" -> path(listOf(Offset(2f,11f),Offset(12f,2f),Offset(22f,11f),Offset(19f,11f),Offset(19f,22f),Offset(5f,22f),Offset(5f,11f)))
                "check" -> {line(3f,12f,9f,18f);line(9f,18f,21f,5f)}
                "triangle" -> path(listOf(Offset(12f,2f),Offset(22f,21f),Offset(2f,21f)))
                "circle" -> drawCircle(color,9f,Offset(12f,12f),style=stroke)
                "plus" -> {line(2f,12f,22f,12f);line(12f,2f,12f,22f)}
                "flag" -> {line(4f,2f,4f,22f);path(listOf(Offset(4f,3f),Offset(21f,3f),Offset(17f,8f),Offset(21f,13f),Offset(4f,13f)))}
                "calendar" -> {drawRect(color,Offset(3f,4f),Size(18f,18f),style=stroke);line(3f,9f,21f,9f);line(7f,1f,7f,6f);line(17f,1f,17f,6f)}
                "cloud" -> {val p=Path().apply{moveTo(5f,19f);cubicTo(-2f,17f,2f,8f,7f,10f);cubicTo(8f,0f,20f,2f,19f,11f);cubicTo(27f,12f,23f,20f,18f,19f);close()};drawPath(p,color,style=stroke)}
                "clock","timer" -> {drawCircle(color,9f,Offset(12f,13f),style=stroke);line(12f,6f,12f,13f);line(12f,13f,17f,16f);if(name=="timer"){line(8f,1f,16f,1f);line(12f,1f,12f,4f)}}
                "date" -> {drawRect(color,Offset(3f,4f),Size(18f,18f),style=stroke);line(3f,9f,21f,9f);drawCircle(color,2f,Offset(12f,15f),style=stroke)}
                "battery" -> {drawRoundRect(color,Offset(2f,7f),Size(18f,10f),androidx.compose.ui.geometry.CornerRadius(2f),style=stroke);line(22f,10f,22f,14f);line(6f,10f,6f,14f);line(10f,10f,10f,14f)}
                "location" -> {val p=Path().apply{moveTo(12f,22f);cubicTo(0f,9f,5f,2f,12f,2f);cubicTo(19f,2f,24f,9f,12f,22f)};drawPath(p,color,style=stroke);drawCircle(color,3f,Offset(12f,9f),style=stroke)}
                "tag" -> {path(listOf(Offset(2f,3f),Offset(12f,3f),Offset(22f,13f),Offset(13f,22f),Offset(2f,11f)));drawCircle(color,1.5f,Offset(7f,7f),style=stroke)}
                "search" -> {drawCircle(color,7f,Offset(10f,10f),style=stroke);line(15f,15f,22f,22f)}
                "eye","eye-off" -> {val p=Path().apply{moveTo(1f,12f);quadraticBezierTo(12f,-1f,23f,12f);quadraticBezierTo(12f,25f,1f,12f)};drawPath(p,color,style=stroke);drawCircle(color,3f,Offset(12f,12f),style=stroke);if(name=="eye-off")line(2f,2f,22f,22f)}
                "shield" -> {path(listOf(Offset(3f,5f),Offset(12f,2f),Offset(21f,5f),Offset(20f,16f),Offset(12f,23f),Offset(4f,16f)));line(7f,12f,11f,16f);line(11f,16f,17f,8f)}
                "leaf" -> {val p=Path().apply{moveTo(4f,19f);cubicTo(0f,7f,12f,3f,22f,2f);cubicTo(22f,15f,17f,24f,4f,19f)};drawPath(p,color,style=stroke);line(2f,22f,18f,6f)}
                "coffee" -> {path(listOf(Offset(3f,9f),Offset(17f,9f),Offset(16f,20f),Offset(4f,20f)));drawArc(color,-90f,180f,false,Offset(14f,9f),Size(8f,8f),style=stroke);line(6f,2f,6f,5f);line(11f,2f,11f,5f)}
                "headphones" -> {drawArc(color,180f,180f,false,Offset(3f,2f),Size(18f,20f),style=stroke);drawRect(color,Offset(2f,12f),Size(5f,9f),style=stroke);drawRect(color,Offset(17f,12f),Size(5f,9f),style=stroke)}
                "bell","bell-off" -> {val p=Path().apply{moveTo(3f,18f);lineTo(5f,15f);lineTo(5f,9f);cubicTo(5f,0f,19f,0f,19f,9f);lineTo(19f,15f);lineTo(21f,18f);close()};drawPath(p,color,style=stroke);drawArc(color,0f,180f,false,Offset(9f,18f),Size(6f,4f),style=stroke);if(name=="bell-off")line(2f,2f,22f,22f)}
                "download","upload" -> {line(3f,17f,3f,22f);line(3f,22f,21f,22f);line(21f,22f,21f,17f);line(12f,2f,12f,17f);val y=if(name=="download")17f else 2f;val o=if(name=="download")-5f else 5f;line(7f,y+o,12f,y);line(12f,y,17f,y+o)}
                "map" -> path(listOf(Offset(2f,5f),Offset(8f,2f),Offset(16f,5f),Offset(22f,2f),Offset(22f,19f),Offset(16f,22f),Offset(8f,19f),Offset(2f,22f)))
                "compass" -> {drawCircle(color,10f,Offset(12f,12f),style=stroke);path(listOf(Offset(16f,7f),Offset(14f,14f),Offset(7f,17f),Offset(10f,10f)))}
                "globe" -> {drawCircle(color,10f,Offset(12f,12f),style=stroke);drawOval(color,Offset(7f,2f),Size(10f,20f),style=stroke);line(2f,12f,22f,12f)}
                "image" -> {drawRect(color,Offset(2f,3f),Size(20f,18f),style=stroke);drawCircle(color,2f,Offset(16f,8f),style=stroke);path(listOf(Offset(3f,20f),Offset(9f,11f),Offset(14f,17f),Offset(17f,14f),Offset(22f,20f)),false)}
                "sliders" -> repeat(3){i->val x=5f+i*7;line(x,2f,x,22f);drawCircle(color,3f,Offset(x,if(i==1)16f else 8f),style=stroke)}
                "terminal" -> {drawRect(color,Offset(2f,3f),Size(20f,18f),style=stroke);line(6f,8f,10f,12f);line(10f,12f,6f,16f);line(13f,16f,18f,16f)}
                "key" -> {drawCircle(color,5f,Offset(7f,7f),style=stroke);line(11f,11f,22f,22f);line(16f,16f,19f,13f);line(19f,19f,22f,16f)}
                "bookmark" -> path(listOf(Offset(5f,2f),Offset(19f,2f),Offset(19f,22f),Offset(12f,17f),Offset(5f,22f)))
                "diamond" -> path(listOf(Offset(12f,2f),Offset(22f,12f),Offset(12f,22f),Offset(2f,12f)))
                "archive" -> {drawRect(color,Offset(2f,2f),Size(20f,5f),style=stroke);drawRect(color,Offset(4f,7f),Size(16f,15f),style=stroke);line(9f,11f,15f,11f)}
                "wallet" -> {drawRect(color,Offset(2f,5f),Size(20f,16f),style=stroke);drawRect(color,Offset(15f,10f),Size(7f,6f),style=stroke);drawCircle(color,.8f,Offset(18f,13f))}
                "bulb" -> {drawCircle(color,7f,Offset(12f,9f),style=stroke);line(8f,15f,8f,19f);line(16f,15f,16f,19f);line(8f,19f,16f,19f);line(9f,22f,15f,22f)}
                "link" -> {drawRoundRect(color,Offset(2f,6f),Size(13f,10f),androidx.compose.ui.geometry.CornerRadius(5f),style=stroke);drawRoundRect(color,Offset(9f,8f),Size(13f,10f),androidx.compose.ui.geometry.CornerRadius(5f),style=stroke)}
                else -> repeat(4){i->drawRect(color,Offset(3f+(i%2)*11,3f+(i/2)*11),Size(7f,7f),style=stroke)}
            }
        }
    }
}
