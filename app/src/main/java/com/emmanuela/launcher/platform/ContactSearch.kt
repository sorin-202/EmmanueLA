package com.emmanuela.launcher.platform

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** Phone plus third-party contact Data rows which Android actually exposes. No invented account availability. */
data class ContactMatch(val id:String,val name:String,val source:String,val intent:Intent,val dedupeKey:String=id)
class ContactSearchRepository(private val context:Context){
    fun allowed()=ContextCompat.checkSelfPermission(context,Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED
    @Suppress("DEPRECATION")
    suspend fun search(input:String,sources:Set<String>):List<ContactMatch> = withContext(Dispatchers.IO){
        if(!allowed())return@withContext emptyList()
        val query=input.removePrefix("@").trim()
        if(query.isEmpty())return@withContext emptyList()
        val result=mutableListOf<ContactMatch>()
        val uri=Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,Uri.encode(query))
        val dialer=context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
        val sourceName=dialer?.let{runCatching{context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(it,0)).toString()}.getOrNull()}?:"Phone"
        context.contentResolver.query(uri,arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,ContactsContract.CommonDataKinds.Phone.NUMBER),null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY+" COLLATE LOCALIZED ASC")?.use{cursor->
            while(cursor.moveToNext()&&result.size<200){coroutineContext.ensureActive()
                val id=cursor.getLong(0);val name=cursor.getString(1).orEmpty();val number=cursor.getString(2).orEmpty()
                if(number.isNotEmpty())result+=ContactMatch("phone:$id",name,sourceName,Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",number,null)).apply{dialer?.let{setPackage(it)}},"phone:"+android.telephony.PhoneNumberUtils.normalizeNumber(number).ifEmpty{id.toString()})
            }
        }
        if(sources.isNotEmpty()){
            // Escaped LIKE selection; integration exists only when a third-party Data MIME handler is exported.
            val escaped=query.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")
            context.contentResolver.query(ContactsContract.Data.CONTENT_URI,arrayOf(ContactsContract.Data._ID,ContactsContract.Data.DISPLAY_NAME_PRIMARY,ContactsContract.Data.MIMETYPE,ContactsContract.Data.CONTACT_ID),
                "${ContactsContract.Data.DISPLAY_NAME_PRIMARY} LIKE ? ESCAPE '\\'",arrayOf("%$escaped%"),ContactsContract.Data.DISPLAY_NAME_PRIMARY+" COLLATE LOCALIZED ASC")?.use{cursor->
                var scanned=0
                val handlerCache=mutableMapOf<String,List<Pair<String,String>>>()
                while(cursor.moveToNext()&&result.size<300&&scanned++<2000){coroutineContext.ensureActive()
                    val mime=cursor.getString(2).orEmpty()
                    if(mime.startsWith("vnd.android.cursor.item/")&&!mime.contains("com.")&&!mime.contains("org."))continue
                    val record=ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI,cursor.getLong(0))
                    val base=Intent(Intent.ACTION_VIEW).setDataAndType(record,mime)
                    val handlers=handlerCache.getOrPut(mime){context.packageManager.queryIntentActivities(base,PackageManager.MATCH_DEFAULT_ONLY).filter{it.activityInfo.exported&&it.activityInfo.packageName in sources}.map{it.activityInfo.packageName to it.loadLabel(context.packageManager).toString()}}
                    handlers.forEach{(pkg,label)->result+=ContactMatch("$pkg:${cursor.getLong(3)}",cursor.getString(1).orEmpty(),label,Intent(base).setPackage(pkg))}
                }
            }
        }
        result.distinctBy{it.id}.distinctBy{it.dedupeKey}
    }
}
