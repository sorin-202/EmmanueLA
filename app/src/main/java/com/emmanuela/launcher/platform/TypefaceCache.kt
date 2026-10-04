package com.emmanuela.launcher.platform

import android.content.Context
import android.graphics.Typeface
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One imported native face shared by Home, previews and widgets; no duplicate font reads. */
object TypefaceCache {
    private val mutex=Mutex()
    private var key=""
    private var face:Typeface?=null
    suspend fun load(context:Context,stamp:String):Typeface?=withContext(Dispatchers.IO){mutex.withLock{
        val file=File(context.filesDir,"custom-font.ttf")
        val next=file.path+":"+stamp
        if(next!=key){face=runCatching{Typeface.createFromFile(file)}.getOrNull();key=next}
        face
    }}
}
