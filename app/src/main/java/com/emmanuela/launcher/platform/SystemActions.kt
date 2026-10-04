package com.emmanuela.launcher.platform

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Exported Android destinations only; never substitutes Usage Access for Digital Wellbeing. */
object SystemDestinations {
    fun open(context:Context,intents:List<Intent>):Boolean {
        for(candidate in intents)try{context.startActivity(Intent(candidate).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return true}
        catch(_:android.content.ActivityNotFoundException){}catch(_:SecurityException){}
        return false
    }
    fun appDetails(context:Context)=Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:${context.packageName}"))
    fun wellbeing(context:Context,component:String=""):Boolean {
        val candidates=mutableListOf<Intent>()
        ComponentName.unflattenFromString(component)?.let{candidates+=Intent(Intent.ACTION_MAIN).setComponent(it)}
        candidates+=Intent("android.settings.WELLBEING_SETTINGS")
        listOf(
            "com.google.android.apps.wellbeing/com.google.android.apps.wellbeing.settings.TopLevelSettingsActivity",
            "com.android.settings/com.android.settings.Settings\$DigitalWellbeingDashboardActivity"
        ).forEach{flat->ComponentName.unflattenFromString(flat)?.let{candidates+=Intent(Intent.ACTION_MAIN).setComponent(it)}}
        listOf("com.google.android.apps.wellbeing","com.samsung.android.forest","com.oneplus.opwellbeing").forEach{pkg->context.packageManager.getLaunchIntentForPackage(pkg)?.let{candidates+=it}}
        return open(context,candidates)
    }
    fun battery(context:Context)=open(context,listOf(Intent(Intent.ACTION_POWER_USAGE_SUMMARY),Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),Intent(Settings.ACTION_SETTINGS)))
    @Suppress("DEPRECATION")
    suspend fun wellbeingOptions(context:Context):List<Pair<String,String>> = withContext(Dispatchers.IO){
        val result=mutableListOf<Pair<String,String>>()
        listOf("com.android.settings","com.google.android.apps.wellbeing","com.samsung.android.forest","com.oneplus.opwellbeing").forEach{pkg->
            runCatching{context.packageManager.getPackageInfo(pkg,PackageManager.GET_ACTIVITIES).activities.orEmpty().filter{it.exported&&it.permission.isNullOrEmpty()}.forEach{info->
                val label=info.loadLabel(context.packageManager).toString()
                val component=ComponentName(info.packageName,info.name).flattenToString()
                result+=label to component
            }}
        }
        result.distinctBy{it.second}.sortedBy{it.first}
    }
}
class SystemActionController(private val context:Context):AutoCloseable {
    private val camera=context.getSystemService(CameraManager::class.java)
    private val torchMutex=Mutex()
    private val dndMutex=Mutex()
    @Volatile private var torchId:String?=null
    @Volatile private var enabled=false
    private val callback=object:CameraManager.TorchCallback(){
        override fun onTorchModeChanged(id:String,on:Boolean){if(id==torchId)enabled=on}
        override fun onTorchModeUnavailable(id:String){if(id==torchId)enabled=false}
    }
    init{runCatching{camera.registerTorchCallback(callback,Handler(Looper.getMainLooper()))}}
    suspend fun toggleTorch():Boolean=withContext(Dispatchers.IO){torchMutex.withLock{
        val id=torchId?:camera.cameraIdList.firstOrNull{candidate->
            val c=camera.getCameraCharacteristics(candidate)
            c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)==true&&c.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_BACK
        }?.also{torchId=it}?:return@withContext false
        val next=!enabled
        camera.setTorchMode(id,next);enabled=next;true
    }}
    suspend fun toggleDnd():Boolean=withContext(Dispatchers.IO){dndMutex.withLock{
        val manager=context.getSystemService(NotificationManager::class.java)
        if(!manager.isNotificationPolicyAccessGranted)return@withContext false
        val prefs=context.getSharedPreferences("system_actions",Context.MODE_PRIVATE)
        val currentlyOn=if(Build.VERSION.SDK_INT>=35)prefs.getBoolean("own_dnd",false)else manager.currentInterruptionFilter!=NotificationManager.INTERRUPTION_FILTER_ALL
        manager.setInterruptionFilter(if(currentlyOn)NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        prefs.edit().putBoolean("own_dnd",!currentlyOn).apply();true
    }}
    override fun close(){runCatching{camera.unregisterTorchCallback(callback)}}
}
