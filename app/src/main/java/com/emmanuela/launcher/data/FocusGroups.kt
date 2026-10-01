package com.emmanuela.launcher.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.ZonedDateTime
import java.util.UUID

/** Rules apply at launcher entry points; this never impersonates an OS app blocker. */
data class FocusGroup(
    val id:String=UUID.randomUUID().toString(), val name:String="", val packages:Set<String> = emptySet(),
    val pause:Boolean=true, val pauseSeconds:Int=10, val dailyMinutes:Int=60,
    val schedule:Boolean=false, val days:Set<Int> = (1..7).toSet(), val startMinute:Int=1320,
    val endMinute:Int=420, val strict:Boolean=false, val prompt:Boolean=true,
    val maxOpens:Int=0, val escalatingPause:Boolean=false,
    val sessionMinutes:Int=0, val requireAuthentication:Boolean=true
)
data class PendingPause(val app:LaunchableApp,val seconds:Int,val prompt:Boolean)
object FocusCodec {
    fun encode(groups:List<FocusGroup>)=JSONArray().apply { groups.forEach { g -> put(JSONObject().apply {
        put("id",g.id);put("name",g.name);put("packages",JSONArray(g.packages.toList()))
        put("pause",g.pause);put("pauseSeconds",g.pauseSeconds);put("dailyMinutes",g.dailyMinutes)
        put("schedule",g.schedule);put("days",JSONArray(g.days.toList()));put("startMinute",g.startMinute)
        put("endMinute",g.endMinute);put("strict",g.strict);put("prompt",g.prompt);put("maxOpens",g.maxOpens);put("escalatingPause",g.escalatingPause);put("sessionMinutes",g.sessionMinutes);put("requireAuthentication",g.requireAuthentication)
    }) } }
    fun decode(a:JSONArray):List<FocusGroup> {
        require(a.length()<=100)
        return List(a.length()) { i -> val o=a.getJSONObject(i);val pk=o.getJSONArray("packages");val days=o.optJSONArray("days")?:JSONArray((1..7).toList())
            require(pk.length()<=1000&&days.length()<=7)
            FocusGroup(o.getString("id").also{require(it.length in 1..100)},o.getString("name").also{require(it.length in 1..40)},
                List(pk.length()){pk.getString(it).also{p->require(p.length<=255)}}.toSet(),o.optBoolean("pause",true),
                o.optInt("pauseSeconds",10).also{require(it in 0..60)},o.optInt("dailyMinutes",60).also{require(it in 0..1440)},
                o.optBoolean("schedule"),List(days.length()){days.getInt(it).also{d->require(d in 1..7)}}.toSet(),
                o.optInt("startMinute",1320).also{require(it in 0..1439)},o.optInt("endMinute",420).also{require(it in 0..1439)},
                o.optBoolean("strict"),o.optBoolean("prompt",true),o.optInt("maxOpens",0).also{require(it in 0..1000)},
                o.optBoolean("escalatingPause"),o.optInt("sessionMinutes",0).also{require(it in 0..240)},o.optBoolean("requireAuthentication",true))
        }.also{groups->require(groups.map{it.id}.distinct().size==groups.size)}
    }
    fun pauseDelay(group:FocusGroup,opens:Int):Int = if(!group.pause)0 else
        (group.pauseSeconds.toLong() * if(group.escalatingPause)(1L+opens.coerceIn(0,1000)/3)else 1L).coerceAtMost(60L).toInt()
    fun scheduled(g:FocusGroup,now:ZonedDateTime):Boolean {
        if(!g.schedule)return false
        val minute=now.hour*60+now.minute
        return if(g.startMinute<=g.endMinute) now.dayOfWeek.value in g.days && minute in g.startMinute until g.endMinute
        else (now.dayOfWeek.value in g.days && minute>=g.startMinute) || (now.minusDays(1).dayOfWeek.value in g.days && minute<g.endMinute)
    }
}
