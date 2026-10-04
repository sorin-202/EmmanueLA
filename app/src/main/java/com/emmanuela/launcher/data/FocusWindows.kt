package com.emmanuela.launcher.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.ZonedDateTime
import java.net.URI

data class FocusWindow(val days:Set<Int> = (1..7).toSet(),val start:Int=540,val end:Int=1020,val mode:String="Limit")
object FocusWindows {
    fun encode(windows:List<FocusWindow>)=JSONArray().apply{windows.forEach{put(JSONObject().put("days",JSONArray(it.days.toList())).put("start",it.start).put("end",it.end).put("mode",it.mode))}}
    fun decode(a:JSONArray):List<FocusWindow>{require(a.length()<=32);return List(a.length()){i->val o=a.getJSONObject(i);val d=o.getJSONArray("days");require(d.length() in 1..7);FocusWindow(List(d.length()){d.getInt(it).also{n->require(n in 1..7)}}.toSet(),o.getInt("start").also{require(it in 0..1439)},o.getInt("end").also{require(it in 0..1439)},o.getString("mode").also{require(it in listOf("Limit","Strict block"))})}}
    fun strings(o:JSONObject,key:String):Set<String>{val a=o.optJSONArray(key)?:JSONArray();require(a.length()<=200);return List(a.length()){a.getString(it).also{s->require(s.length in 1..512)}}.toSet()}
    fun active(w:FocusWindow,now:ZonedDateTime):Boolean{val m=now.hour*60+now.minute;return if(w.start==w.end)now.dayOfWeek.value in w.days else if(w.start<w.end)now.dayOfWeek.value in w.days&&m in w.start until w.end else(now.dayOfWeek.value in w.days&&m>=w.start)||(now.minusDays(1).dayOfWeek.value in w.days&&m<w.end)}
    fun limited(g:FocusGroup,now:ZonedDateTime)=g.windows.none{it.mode=="Limit"}||g.windows.any{it.mode=="Limit"&&active(it,now)}
    fun strict(g:FocusGroup,now:ZonedDateTime)=FocusCodec.scheduled(g,now)||g.windows.any{it.mode=="Strict block"&&active(it,now)}
    fun host(input:String):String?=runCatching{val raw=input.trim().lowercase();val uri=URI(if(raw.contains("://"))raw else "https://$raw");uri.host?.removeSuffix(".")?.takeIf{it.contains('.')&&!it.contains(' ')}}.getOrNull()
    fun matches(g:FocusGroup,url:String):Boolean{val actualHost=host(url)?:return false;return g.websites.any{site->val d=host(site);d!=null&&(actualHost==d||actualHost.endsWith(".$d"))}||g.keywords.any{url.contains(it,ignoreCase=true)}}
    fun nextBoundary(g:FocusGroup,now:ZonedDateTime):Long?{
        val windows=g.windows+if(g.schedule)listOf(FocusWindow(g.days,g.startMinute,g.endMinute,"Strict block"))else emptyList()
        return windows.flatMap{w->(-1..7).flatMap{offset->val day=now.toLocalDate().plusDays(offset.toLong());if(day.dayOfWeek.value !in w.days)emptyList()else{val start=if(w.start==w.end)day.atStartOfDay(now.zone)else day.atTime(w.start/60,w.start%60).atZone(now.zone);val endDay=day.plusDays(if(w.end<=w.start)1 else 0);val end=if(w.start==w.end)endDay.atStartOfDay(now.zone)else endDay.atTime(w.end/60,w.end%60).atZone(now.zone);listOf(start,end)}}}.filter{it.isAfter(now)}.minOfOrNull{java.time.Duration.between(now,it).toMillis()}
    }
    fun countKey(g:FocusGroup,pkg:String,day:String)="$day:${g.id}"+if(g.perApp)":$pkg"else ""
    fun reason(g:FocusGroup,now:ZonedDateTime,used:Long?,opens:Int,session:Long=0):String?=when{
        strict(g,now)->"${g.name}: strict block"
        !limited(g,now)->null
        g.dailyMinutes>0&&used==null->"Enable Usage Access for daily limits"
        g.dailyMinutes>0&&(used?:0)>=g.dailyMinutes*60_000L->"${g.name}: daily limit reached"
        g.maxOpens>0&&opens>=g.maxOpens->"${g.name}: daily opens reached"
        g.sessionMinutes>0&&session>=g.sessionMinutes*60_000L->"${g.name}: session limit reached"
        else->null
    }
}
