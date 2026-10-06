package com.emmanuela.launcher.data

/** Lower scores sort first. Input names and query are folded once by the caller. */
object SearchRanking {
    private val words = Regex("[^\\p{L}\\p{N}]+")
    private val marks = Regex("\\p{M}+")
    private val whitespace = Regex("\\s+")
    class Query(val text:String) { val tokens=if(text.isBlank())emptyList()else text.split(whitespace).filter(String::isNotEmpty) }
    class Name(val text:String) { val tokens=text.split(words) }
    fun folded(value: String): String = java.text.Normalizer.normalize(AppNaming.folded(value), java.text.Normalizer.Form.NFD).replace(marks, "")
    fun score(query: String, names: List<String>): Int? {
        return score(Query(query),names.map(::Name))
    }
    fun score(query:Query,names:List<Name>):Int? {
        if(query.tokens.isEmpty())return null
        if(names.any{it.text==query.text})return 0
        if(names.any{it.text.startsWith(query.text)})return 1
        if(query.tokens.all{token->names.any{name->name.tokens.any{it.startsWith(token)}}})return 2
        return if(query.tokens.all{token->names.any{token in it.text}})3 else null
    }
}
