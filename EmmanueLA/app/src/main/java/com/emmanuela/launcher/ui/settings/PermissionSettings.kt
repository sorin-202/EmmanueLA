package com.emmanuela.launcher.ui.settings

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.app.NotificationManager
import android.provider.Settings
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.emmanuela.launcher.EmmanuelaNotificationListener
import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.platform.*

@Composable
fun PermissionSettings(model:LauncherViewModel){
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current
    var revision by remember{mutableIntStateOf(0)}
    var pending by remember{mutableStateOf("")}
    val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        revision++
        if(!granted){model.error.value="Permission was not granted. You can change it in Android App permissions."}
    }
    DisposableEffect(lifecycle){val listener=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME){revision++;model.refreshUsage()}};lifecycle.lifecycle.addObserver(listener);onDispose{lifecycle.lifecycle.removeObserver(listener)}}
    fun open(candidates:List<Intent>){if(!SystemDestinations.open(context,candidates+SystemDestinations.appDetails(context)))model.error.value="Android permission settings are unavailable."}
    fun runtime(permission:String){
        val activity=context.fragmentActivity()
        val prefs=context.getSharedPreferences("permission_requests",android.content.Context.MODE_PRIVATE)
        if(ContextCompat.checkSelfPermission(context,permission)==PackageManager.PERMISSION_GRANTED)open(listOf(SystemDestinations.appDetails(context)))
        else if(prefs.getBoolean(permission,false)&&activity!=null&&!androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity,permission))open(listOf(SystemDestinations.appDetails(context)))
        else {prefs.edit().putBoolean(permission,true).apply();pending=permission;request.launch(permission)}
    }
    fun granted(permission:String):String {revision;return if(ContextCompat.checkSelfPermission(context,permission)==PackageManager.PERMISSION_GRANTED)"Granted" else "Not granted"}
    SettingRow("Usage access",if(context.hasUsageAccess())"Granted" else "Not granted"){open(listOf(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).setData(android.net.Uri.parse("package:${context.packageName}")),Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)))}
    SettingRow("Notification access",if(context.notificationAccess())"Granted" else "Not granted"){
        val direct=if(Build.VERSION.SDK_INT>=30)listOf(Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,ComponentName(context,EmmanuelaNotificationListener::class.java).flattenToString()))else emptyList()
        open(direct+Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
    if(Build.VERSION.SDK_INT>=33)SettingRow("Allow notifications",granted(Manifest.permission.POST_NOTIFICATIONS)){runtime(Manifest.permission.POST_NOTIFICATIONS)}
    SettingRow("Screen locking",if(context.lockReady())"Granted" else "Not granted"){open(listOf(context.lockSetupIntent(),Intent(Settings.ACTION_SECURITY_SETTINGS)))}
    SettingRow("Approximate location",granted(Manifest.permission.ACCESS_COARSE_LOCATION)){runtime(Manifest.permission.ACCESS_COARSE_LOCATION)}
    SettingRow("Contacts",granted(Manifest.permission.READ_CONTACTS)){runtime(Manifest.permission.READ_CONTACTS)}
    SettingRow("Flashlight / camera",granted(Manifest.permission.CAMERA)){runtime(Manifest.permission.CAMERA)}
    SettingRow("Do Not Disturb",if(context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted)"Granted" else "Not granted"){open(listOf(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)))}
    SettingRow("App permissions"){open(listOf(SystemDestinations.appDetails(context)))}
}
