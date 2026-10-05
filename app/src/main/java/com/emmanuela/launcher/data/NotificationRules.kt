package com.emmanuela.launcher.data

import java.time.ZonedDateTime
import org.json.JSONArray
import org.json.JSONObject

enum class NotificationScope { ALWAYS, SCHEDULE, FOCUS }
data class NotificationRule(
    val scope:NotificationScope=NotificationScope.ALWAYS,
    val days:Set<Int> = (1..7).toSet(),
    val startMinute:Int=22*60,
    val endMinute:Int=7*60,
    val keywords:Set<String> = emptySet(),
    val suppressUntil:Long=0
)

/** Content is supplied lazily only for an opted-in, currently applicable keyword rule. */
object NotificationRules {
    fun action(policy:AppPolicy,packageName:String,groups:List<FocusGroup>,now:ZonedDateTime,
               digestEnabled:Boolean,content:()->String={""}):NotificationMode {
        val rule=policy.notificationRule
        if(now.toInstant().toEpochMilli()<rule.suppressUntil)return NotificationMode.DISMISS
        val mode=policy.notifications
        if(mode==NotificationMode.NORMAL||mode==NotificationMode.DIGEST&&!digestEnabled)return NotificationMode.NORMAL
        val active=when(rule.scope){
            NotificationScope.ALWAYS->true
            NotificationScope.SCHEDULE->PolicyRules.scheduled(AppPolicy(scheduleEnabled=true,days=rule.days,startMinute=rule.startMinute,endMinute=rule.endMinute),now)
            NotificationScope.FOCUS->groups.any{packageName in it.packages&&FocusWindows.strict(it,now)}
        }
        if(!active)return NotificationMode.NORMAL
        if(rule.keywords.isNotEmpty()){
            val text=content()
            if(rule.keywords.none{text.contains(it,ignoreCase=true)})return NotificationMode.NORMAL
        }
        return mode
    }
    fun encode(rule:NotificationRule)=JSONObject().apply {
        put("scope",rule.scope.name);put("days",JSONArray(rule.days.sorted()))
        put("start",rule.startMinute);put("end",rule.endMinute)
        put("keywords",JSONArray(rule.keywords.sorted()));put("until",rule.suppressUntil)
    }
    fun decode(o:JSONObject?):NotificationRule {
        if(o==null)return NotificationRule()
        val days=o.getJSONArray("days");val keywords=o.getJSONArray("keywords")
        require(days.length() in 1..7&&keywords.length()<=20)
        return NotificationRule(NotificationScope.valueOf(o.getString("scope")),
            List(days.length()){days.getInt(it).also{day->require(day in 1..7)}}.toSet(),
            o.getInt("start").also{require(it in 0..1439)},o.getInt("end").also{require(it in 0..1439)},
            List(keywords.length()){keywords.getString(it).also{word->require(word.isNotBlank()&&word.length<=80)}}.toSet(),
            o.optLong("until").also{require(it>=0)})
    }
}
