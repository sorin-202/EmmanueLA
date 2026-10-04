package com.emmanuela.launcher

import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : androidx.fragment.app.FragmentActivity() {
    private val model: LauncherViewModel by viewModels()
    private var now by mutableLongStateOf(System.currentTimeMillis())
    private var battery by mutableIntStateOf(-1)
    private val packages = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { model.refresh(true) }
    }
    private val status = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            now = System.currentTimeMillis()
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                battery = if (level >= 0 && scale > 0) level * 100 / scale else -1
            } else model.refreshUsage()
        }
    }
    private val screenOff=object:BroadcastReceiver(){override fun onReceive(context:Context?,intent:Intent?){if(intent?.action==Intent.ACTION_SCREEN_OFF)model.relockPrivateSpace()}}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("open_digest", false)) model.openDigest.value = true
        intent.getStringExtra("authenticate_package")?.let{model.privateRequest.value=it;intent.removeExtra("authenticate_package")}
        enableEdgeToEdge()
        ContextCompat.registerReceiver(this, packages, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED); addAction(Intent.ACTION_PACKAGE_REPLACED); addDataScheme("package")
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(this,screenOff,IntentFilter(Intent.ACTION_SCREEN_OFF),ContextCompat.RECEIVER_EXPORTED)
        setContent {
            val epoch by model.homeEpoch.collectAsStateWithLifecycle()
            LauncherApp(model, epoch, now, battery, ::chooseHome, ::setBars)
        }
    }
    override fun onStart() {
        super.onStart()
        // These are protected framework broadcasts. Registered only while the launcher is visible.
        ContextCompat.registerReceiver(this, status, IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK); addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED); addAction(Intent.ACTION_BATTERY_CHANGED)
        }, ContextCompat.RECEIVER_EXPORTED)
    }
    override fun onResume() { super.onResume(); now = System.currentTimeMillis(); model.refreshUsage() }
    override fun onStop() { model.backgroundSecurity(); unregisterReceiver(status); super.onStop() }
    override fun onDestroy(){unregisterReceiver(packages);unregisterReceiver(screenOff);super.onDestroy()}
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME) && !model.isAuthenticationReturn()) model.homeEpoch.value++
        if (intent.getBooleanExtra("open_digest", false)) model.openDigest.value = true
        intent.getStringExtra("authenticate_package")?.let{model.privateRequest.value=it;intent.removeExtra("authenticate_package")}
    }
    private fun setBars(visible: Boolean, dark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (visible) show(WindowInsetsCompat.Type.statusBars()) else hide(WindowInsetsCompat.Type.statusBars())
        }
    }
    private fun chooseHome() {
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "Settings → Apps → Default apps → Home app", Toast.LENGTH_LONG).show()
        }
    }
}
