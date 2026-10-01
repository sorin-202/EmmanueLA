package com.emmanuela.launcher.platform

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Opt-in, approximate forecast conditions. One hourly request while visible; cached on failure. */
object WeatherRepository {
    private val mutex=Mutex()
    data class LocationResult(val name:String,val country:String,val latitude:String,val longitude:String)
    suspend fun searchLocation(query:String):List<LocationResult> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        runCatching {
            val encoded=java.net.URLEncoder.encode(query.trim(), "UTF-8")
            val conn=URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=8&language=en&format=json").openConnection() as HttpsURLConnection
            conn.connectTimeout=5000;conn.readTimeout=5000
            try {
                check(conn.responseCode==200)
                val raw=conn.inputStream.bufferedReader().use{it.readText()}
                val array=JSONObject(raw).optJSONArray("results") ?: return@runCatching emptyList()
                List(array.length()){i->array.getJSONObject(i)}.map{row->LocationResult(row.optString("name"),row.optString("country"),row.getDouble("latitude").toString(),row.getDouble("longitude").toString())}
            } finally { conn.disconnect() }
        }.getOrDefault(emptyList())
    }

    suspend fun current(context:Context, latitude:String, longitude:String):String = withContext(Dispatchers.IO) {
        mutex.withLock {
            val lat=latitude.toDoubleOrNull();val lon=longitude.toDoubleOrNull()
            if(lat==null||lon==null) return@withLock "Set weather location"
            val key="$lat,$lon"
            val prefs=context.applicationContext.getSharedPreferences("weather_cache",Context.MODE_PRIVATE)
            val now=System.currentTimeMillis()
            val same=prefs.getString("location","")==key
            val cached=if(same)prefs.getString("label",null)else null
            if(same && now-prefs.getLong("attempt",0)<3_600_000L) return@withLock cached?.let{if(now-prefs.getLong("fetched",0)>3_600_000L)"$it · cached" else it} ?: "Weather unavailable"
            prefs.edit().putString("location",key).putLong("attempt",now).apply()
            if(!same)prefs.edit().remove("label").apply()
            val result=runCatching {
                val conn=URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code&forecast_days=1").openConnection() as HttpsURLConnection
                conn.connectTimeout=5000;conn.readTimeout=5000
                try {
                    check(conn.responseCode==200)
                    val raw=conn.inputStream.bufferedReader().use { reader ->
                        val out=StringBuilder();val buffer=CharArray(4096)
                        while(true){val n=reader.read(buffer);if(n<0)break;require(out.length+n<100_000);out.append(buffer,0,n)}
                        out.toString()
                    }
                    val current=JSONObject(raw).getJSONObject("current")
                    val code=current.getInt("weather_code")
                    val condition=when(code){0->"Clear";in 1..3->"Cloudy";45,48->"Fog";in 51..67->"Rain";in 71..77->"Snow";in 80..82->"Showers";85,86->"Snow showers";in 95..99->"Thunderstorm";else->"Weather"}
                    "${kotlin.math.round(current.getDouble("temperature_2m")).toInt()}°C · $condition"
                } finally {conn.disconnect()}
            }.getOrNull()
            if(result!=null){prefs.edit().putString("label",result).putLong("fetched",now).apply();result}
            else cached?.let{"$it · cached"} ?: "Weather unavailable"
        }
    }
}
