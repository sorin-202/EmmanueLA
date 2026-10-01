package com.emmanuela.launcher.ui.appearance

import com.emmanuela.launcher.LauncherViewModel
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.ui.components.localized


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

private data class WeatherPlace(val name:String,val latitude:Double,val longitude:Double)
@Composable
fun WeatherLocationPicker(data:LauncherData,model:LauncherViewModel){
    var query by remember{mutableStateOf("")};var places by remember{mutableStateOf<List<WeatherPlace>>(emptyList())};var loading by remember{mutableStateOf(false)};var message by remember{mutableStateOf("")}
    LaunchedEffect(query){places=emptyList();message="";if(query.trim().length<2)return@LaunchedEffect;delay(300);loading=true
        try{places=withContext(Dispatchers.IO){val connection=URL("https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(query.trim(),"UTF-8")}&count=5&format=json").openConnection().let{it as javax.net.ssl.HttpsURLConnection}.apply{connectTimeout=5000;readTimeout=5000}
            try{check(connection.responseCode==200);val body=connection.getInputStream().bufferedReader().use{it.readText()};val results=JSONObject(body).optJSONArray("results")
            if(results==null)emptyList()else List(results.length()){i->val item=results.getJSONObject(i);WeatherPlace(listOf(item.getString("name"),item.optString("admin1"),item.optString("country")).filter{it.isNotBlank()}.distinct().joinToString(", "),item.getDouble("latitude"),item.getDouble("longitude"))}}finally{connection.disconnect()}}
            if(places.isEmpty())message="No locations found."
        }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(_:Exception){message="Could not search locations. Try again."}finally{loading=false}}
    Text(if(data.settings.ui.v2.latitude.isEmpty())localized("No location selected")else data.settings.ui.experience.weatherLocationName.ifEmpty{"%.2f, %.2f".format(data.settings.ui.v2.latitude.toDoubleOrNull()?:0.0,data.settings.ui.v2.longitude.toDoubleOrNull()?:0.0)},style=MaterialTheme.typography.bodySmall)
    OutlinedTextField(query,{query=it.take(100)},label={Text(localized("Search city or town"))},singleLine=true,modifier=Modifier.fillMaxWidth())
    if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
    places.forEach{place->Text(place.name,Modifier.fillMaxWidth().clickable{model.uiSettings{it.copy(v2=it.v2.copy(latitude=place.latitude.toString(),longitude=place.longitude.toString()),experience=it.experience.copy(weatherLocationName=place.name.take(160)))};query="";places=emptyList()}.padding(vertical=16.dp))}
    if(message.isNotEmpty())Text(message)
    Text(localized("City search uses Open-Meteo. Your GPS location is not sent."),style=MaterialTheme.typography.bodySmall)
}
