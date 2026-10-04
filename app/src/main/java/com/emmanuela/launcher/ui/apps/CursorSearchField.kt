package com.emmanuela.launcher.ui.apps

import com.emmanuela.launcher.data.AppSearch
import com.emmanuela.launcher.data.Preferences
import com.emmanuela.launcher.ui.components.localized


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Draw-only caret customization; IME, selection and semantics remain BasicTextField's. */
@Composable
fun CursorSearchField(query:String,change:(String)->Unit,p:Preferences,modifier:Modifier=Modifier,placeholder:String="") {
    val u=p.ui;val e=u.experience
    val hint=placeholder.ifEmpty{if(e.contactsSearch)"Search apps, #tag or @contact" else "Search apps or #tag"}
    var value by remember{mutableStateOf(TextFieldValue(query))}
    if(value.text!=query)value=value.copy(text=query,selection=androidx.compose.ui.text.TextRange(query.length))
    var layout by remember{mutableStateOf<TextLayoutResult?>(null)}
    var focused by remember{mutableStateOf(false)}
    var visible by remember{mutableStateOf(true)}
    val cursorPaint=remember{android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)}
    LaunchedEffect(focused,value.selection,u.searchCursorBlinkMs){visible=true;if(focused&&u.searchCursorBlinkMs>0)while(true){delay(u.searchCursorBlinkMs.toLong());visible=!visible}}
    val accent=if(u.searchColor!=0L)Color(u.searchColor)else MaterialTheme.colorScheme.primary
    val cursor=if(u.searchCursorColor!=0L)Color(u.searchCursorColor)else accent
    val shape=RoundedCornerShape(if(u.searchStyle=="Pill")50 else if(u.searchStyle=="Box")0 else 12)
    Row(modifier.heightIn(min=52.dp).then(if(u.searchStyle in listOf("Pill","Box","Outline","Floating Outline"))Modifier.border(1.dp,if(focused)accent else MaterialTheme.colorScheme.outline,shape)else Modifier),verticalAlignment=Alignment.CenterVertically){
        if((u.tagSearch&&AppSearch.isTagMode(query)) || (e.contactsSearch&&query.trimStart().startsWith("@")))Box(Modifier.padding(start=12.dp).size(26.dp).border(1.dp,MaterialTheme.colorScheme.onSurface,CircleShape),contentAlignment=Alignment.Center){Text(if(query.trimStart().startsWith("@"))"@" else "#")}
        BasicTextField(value,{value=it;change(it.text)},singleLine=true,textStyle=MaterialTheme.typography.bodyLarge.copy(color=MaterialTheme.colorScheme.onSurface),cursorBrush=SolidColor(Color.Transparent),onTextLayout={layout=it},
            modifier=Modifier.weight(1f).padding(14.dp).onFocusChanged{focused=it.isFocused}.drawWithContent{
                drawContent()
                val result=layout
                if(focused&&visible&&value.selection.collapsed&&result!=null){val rect=result.getCursorRect(value.selection.start.coerceIn(0,value.text.length));val w=when(e.cursorStyle){"Thin"->1.dp.toPx();"Thick"->4.dp.toPx();"Block"->8.dp.toPx();else->u.searchCursorWidth.dp.toPx()}
                    when(e.cursorStyle){"Underline"->drawRect(cursor,Offset(rect.left,rect.bottom-2.dp.toPx()),Size(8.dp.toPx(),2.dp.toPx()))
                        "Custom"->drawContext.canvas.nativeCanvas.apply{drawText(e.cursorCharacter,rect.left,rect.bottom,cursorPaint.apply{color=cursor.toArgb();textSize=rect.height})}
                        else->drawRect(cursor.copy(alpha=if(e.cursorStyle=="Block").55f else 1f),Offset(rect.left,rect.top),Size(w,rect.height))}
                }
            },decorationBox={inner->Box{if(query.isEmpty())Text(localized(hint),color=MaterialTheme.colorScheme.onSurfaceVariant);inner()}})
        if(query.isNotEmpty())TextButton(onClick={change("")}){Text(localized("Clear"))}
    }
    if(u.searchStyle=="Underline")HorizontalDivider(color=if(focused)accent else MaterialTheme.colorScheme.outline)
}
