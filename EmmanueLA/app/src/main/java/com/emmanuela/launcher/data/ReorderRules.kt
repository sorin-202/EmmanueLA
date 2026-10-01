package com.emmanuela.launcher.data

object ReorderRules {
    fun <T> mergeVisible(stored:List<T>,order:List<T>):List<T>{
        val available=stored.toSet()
        require(order.distinct().size==order.size && order.all{it in available})
        val visible=order.toSet();val replacement=order.iterator()
        return stored.map{if(it in visible)replacement.next()else it}
    }
    fun <T> move(values:List<T>,source:T,target:T):List<T>{
        val from=values.indexOf(source);val to=values.indexOf(target)
        if(from<0||to<0||from==to)return values
        return values.toMutableList().apply{add(to,removeAt(from))}
    }
}
