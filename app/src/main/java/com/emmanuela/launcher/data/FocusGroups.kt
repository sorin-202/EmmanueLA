package com.emmanuela.launcher.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.ZonedDateTime
import java.util.UUID

/** Shared focus rules for launcher admissions and the optional accessibility enforcement service. */
data class FocusGroup(
    val id:String=UUID.randomUUID().toString(), val name:String="", val packages:Set<String> = emptySet(),
    val pause:Boolean=true, val pauseSeconds:Int=10, val dailyMinutes:Int=60,
    val schedule:Boolean=false, val days:Set<Int> = (1..7).toSet(), val startMinute:Int=1320,
    val endMinute:Int=420, val strict:Boolean=false, val prompt:Boolean=true,
    val maxOpens:Int=0, val escalatingPause:Boolean=false,
    val sessionMinutes:Int=0, val requireAuthentication:Boolean=true,
    val windows:List<FocusWindow> = emptyList(), val websites:Set<String> = emptySet(),
    val keywords:Set<String> = emptySet(), val browsers:Set<String> = emptySet(),
    val perApp:Boolean=false, val cooldownSeconds:Int=60,
    val blockAlways:Boolean=false, val breakStartedAt:Long=0, val breakUntil:Long=0,
    val graceMinutes:Int=0, val warningMinutes:Int=0, val limitAction:String="Block",
    val requireIntention:Boolean=false
)
data class PendingPause(val app:LaunchableApp,val seconds:Int,val prompt:Boolean,val requireIntention:Boolean=false)
object FocusCodec {
    fun encode(groups:List<FocusGroup>)=JSONArray().apply { groups.forEach { g -> put(JSONObject().apply {
        put("graceMinutes",g.graceMinutes);put("warningMinutes",g.warningMinutes);put("limitAction",g.limitAction);put("requireIntention",g.requireIntention)
        put("blockAlways",g.blockAlways);put("breakStartedAt",g.breakStartedAt);put("breakUntil",g.breakUntil)
        put("windows",FocusWindows.encode(g.windows));put("websites",JSONArray(g.websites.toList()));put("keywords",JSONArray(g.keywords.toList()));put("browsers",JSONArray(g.browsers.toList()));put("perApp",g.perApp);put("cooldownSeconds",g.cooldownSeconds)
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
                o.optBoolean("escalatingPause"),o.optInt("sessionMinutes",0).also{require(it in 0..240)},o.optBoolean("requireAuthentication",true),
                FocusWindows.decode(o.optJSONArray("windows")?:JSONArray()),FocusWindows.strings(o,"websites"),FocusWindows.strings(o,"keywords"),FocusWindows.strings(o,"browsers"),
                o.optBoolean("perApp"),o.optInt("cooldownSeconds",60).also{require(it in 0..3600)},
                o.optBoolean("blockAlways"),o.optLong("breakStartedAt"),o.optLong("breakUntil"),
                o.optInt("graceMinutes",0).also{require(it in 0..30)},o.optInt("warningMinutes",0).also{require(it in 0..30)},
                o.optString("limitAction","Block").also{require(it in listOf("Block","Warn"))},o.optBoolean("requireIntention")).also {
                    require(it.breakStartedAt>=0 && it.breakUntil>=it.breakStartedAt && it.breakUntil-it.breakStartedAt<=24*60*60_000L)
                }
        }.also{groups->require(groups.map{it.id}.distinct().size==groups.size)}
    }
    fun pauseDelay(group:FocusGroup,opens:Int):Int = if(!group.pause)0 else
        (group.pauseSeconds.toLong() * if(group.escalatingPause)(1L+opens.coerceIn(0,1000)/3)else 1L).coerceAtMost(60L).toInt()
    fun scheduled(g:FocusGroup,now:ZonedDateTime):Boolean {
        if(!g.schedule)return false
        val minute=now.hour*60+now.minute
        return if(g.startMinute==g.endMinute)now.dayOfWeek.value in g.days else if(g.startMinute<g.endMinute) now.dayOfWeek.value in g.days && minute in g.startMinute until g.endMinute
        else (now.dayOfWeek.value in g.days && minute>=g.startMinute) || (now.minusDays(1).dayOfWeek.value in g.days && minute<g.endMinute)
    }
}
