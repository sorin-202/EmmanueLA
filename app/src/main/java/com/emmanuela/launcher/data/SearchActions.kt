package com.emmanuela.launcher.data

import org.json.JSONArray

object SearchActions {
    private val builtIn=setOf("home","drawer","apps","folders","search","settings","flashlight","dnd","airplane","wellbeing","notifications","lock","battery","clock","calendar","camera","phone")
    fun valid(value:String)=value in builtIn || (value.length<=512&&value.startsWith("app:")&&value.removePrefix("app:").matches(Regex("[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+")))
    fun decode(array:JSONArray?):Set<String> {
        if(array==null)return emptySet()
        require(array.length()<=16)
        return List(array.length()){array.getString(it).also{value->require(valid(value))}}.toSet()
    }
    fun matches(query:String,labels:Map<String,String>):List<String> {
        val text=query.trim()
        if(text.isEmpty()||text.first() in "/@#")return emptyList()
        val folded=SearchRanking.folded(text)
        return labels.mapNotNull{(id,label)->SearchRanking.score(folded,listOf(SearchRanking.folded(label)))?.let{Triple(id,label,it)}}
            .sortedWith(compareBy({it.third},{it.second},{it.first})).map{it.first}
    }
}
