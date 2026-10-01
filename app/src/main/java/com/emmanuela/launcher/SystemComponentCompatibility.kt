package com.emmanuela.launcher

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import androidx.work.WorkerParameters

// Preserve registered Android component identities from v2.1 during in-place upgrades.
class LockAdminReceiver : DeviceAdminReceiver()
class EmmanuelaNotificationListener : com.emmanuela.launcher.platform.EmmanuelaNotificationListener()
class DigestWorker(context:Context,parameters:WorkerParameters) : com.emmanuela.launcher.platform.DigestWorker(context,parameters)
