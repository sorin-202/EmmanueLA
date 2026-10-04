package com.emmanuela.launcher.data

data class FolderCell(val folder:AppFolder,val column:Int,val row:Int,val width:Int,val height:Int){fun covers(x:Int,y:Int)=x in column until column+width&&y in row until row+height}
object FolderGridLayout {
    fun pack(folders:List<AppFolder>,columns:Int,defaultWidth:Int,defaultHeight:Int,preferred:String?=null):List<FolderCell>{
        require(columns>=4)
        val used=mutableSetOf<Int>();val result=mutableListOf<FolderCell>()
        folders.sortedBy{if(it.id==preferred)-1 else it.gridCell?:Int.MAX_VALUE}.forEach{f->
            val w=(if(f.widthUnits>0)f.widthUnits else defaultWidth).coerceIn(1,4);val h=(if(f.heightUnits>0)f.heightUnits else defaultHeight).coerceIn(1,4)
            fun fits(anchor:Int):Boolean{val x=anchor%columns;val y=anchor/columns;return x+w<=columns&&(y until y+h).all{row->(x until x+w).all{col->row*columns+col !in used}}}
            var anchor=f.gridCell?:0
            if(!fits(anchor)){anchor=0;while(!fits(anchor))anchor++}
            val x=anchor%columns;val y=anchor/columns
            (y until y+h).forEach{row->(x until x+w).forEach{col->used+=row*columns+col}}
            result+=FolderCell(f,x,y,w,h)
        }
        return result
    }
}
