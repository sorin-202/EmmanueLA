package com.emmanuela.launcher.platform

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityManager

object BackgroundRuleAccess {
    /** The OS permission and current service connection are deliberately separate facts. */
    fun enabled(context: Context): Boolean {
        val wanted=ComponentName(context,RuleEnforcementService::class.java)
        return context.getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
                ComponentName(it.resolveInfo.serviceInfo.packageName,it.resolveInfo.serviceInfo.name)==wanted
            }
    }
    fun settingsIntent(context: Context)=Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS")
        .putExtra(Intent.EXTRA_COMPONENT_NAME,ComponentName(context,RuleEnforcementService::class.java))
}
