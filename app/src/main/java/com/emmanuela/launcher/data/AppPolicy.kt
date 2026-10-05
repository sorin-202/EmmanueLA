package com.emmanuela.launcher.data

import java.time.ZonedDateTime
import org.json.JSONArray
import org.json.JSONObject

enum class NotificationMode { NORMAL, DISMISS, DIGEST }
data class AppPolicy(
    val hidden: Boolean = false,
    val blocked: Boolean = false,
    val privateApp: Boolean = false,
    val dailyLimitMinutes: Int? = null,
    val scheduleEnabled: Boolean = false,
    val startMinute: Int = 22 * 60,
    val endMinute: Int = 7 * 60,
    val days: Set<Int> = (1..7).toSet(),
    val notifications: NotificationMode = NotificationMode.NORMAL,
    val digestMinutes: Int = 30,
    val badgesMuted: Boolean = false,
    val notificationRule:NotificationRule = NotificationRule()
)

object PolicyRules {
    /** A missing app row is zero usage; a missing snapshot means unavailable access. */
    fun reason(policy:AppPolicy,now:ZonedDateTime,usage:Map<String,Long>?,packageName:String):String? =
        reason(policy,now,usage?.getOrDefault(packageName,0L))
    fun scheduled(policy: AppPolicy, now: ZonedDateTime): Boolean {
        if (!policy.scheduleEnabled) return false
        val minute = now.hour * 60 + now.minute
        val day = now.dayOfWeek.value
        return when {
            policy.startMinute == policy.endMinute -> day in policy.days // All day on selected days.
            policy.startMinute < policy.endMinute -> day in policy.days && minute >= policy.startMinute && minute < policy.endMinute
            minute >= policy.startMinute -> day in policy.days
            minute < policy.endMinute -> now.minusDays(1).dayOfWeek.value in policy.days
            else -> false
        }
    }
    fun reason(policy: AppPolicy, now: ZonedDateTime, usedMillis: Long?): String? = when {
        policy.blocked -> "This app is paused in EmmanueLA."
        scheduled(policy, now) -> "This app is paused by your schedule."
        policy.dailyLimitMinutes != null && usedMillis == null -> "Usage access is unavailable. Restore access or remove this app's limit in App management."
        policy.dailyLimitMinutes != null && usedMillis != null && usedMillis >= policy.dailyLimitMinutes * 60_000L -> "Today's app limit has been reached."
        else -> null
    }
    fun time(text: String): Int? {
        val parts = text.split(':')
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
    }
    fun timeLabel(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)
}

object PolicyCodec {
    fun encode(policies: Map<String, AppPolicy>) = JSONObject().apply {
        policies.forEach { (pkg, p) -> put(pkg, JSONObject().apply {
            put("notificationRule",NotificationRules.encode(p.notificationRule))
            put("badgesMuted",p.badgesMuted); put("hidden", p.hidden); put("blocked", p.blocked); put("private", p.privateApp)
            put("limit", p.dailyLimitMinutes ?: JSONObject.NULL)
            put("scheduled", p.scheduleEnabled); put("start", p.startMinute); put("end", p.endMinute)
            put("days", JSONArray(p.days.sorted())); put("notifications", p.notifications.name); put("digestMinutes", p.digestMinutes)
        }) }
    }
    fun decode(root: JSONObject): Map<String, AppPolicy> {
        require(root.length() <= 5000)
        return root.keys().asSequence().associateWith { pkg ->
            require(pkg.isNotBlank() && pkg.length <= 256 && '/' !in pkg)
            val o = root.getJSONObject(pkg)
            val dayArray = o.getJSONArray("days")
            val days = (0 until dayArray.length()).map { dayArray.getInt(it) }.toSet()
            AppPolicy(o.optBoolean("hidden"), o.optBoolean("blocked"), o.optBoolean("private"),
                if (o.isNull("limit")) null else o.getInt("limit").also { require(it in 1..1440) },
                o.getBoolean("scheduled"), o.getInt("start").also { require(it in 0..1439) },
                o.getInt("end").also { require(it in 0..1439) },
                days.also { require(it.isNotEmpty() && it.all { d -> d in 1..7 }) },
                NotificationMode.valueOf(o.getString("notifications")),
                o.getInt("digestMinutes").also { require(it in listOf(15, 30, 60, 120)) }, o.optBoolean("badgesMuted"),NotificationRules.decode(o.optJSONObject("notificationRule")))
        }
    }
}
