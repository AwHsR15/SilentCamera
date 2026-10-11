package com.marvis.silentcamera.core

class CscTarget(private val executor: CommandExecutor, private val user: Int): StateTarget {
    override fun read():String? = ModePolicy.cscValue(executor.execute(listOf("settings","--user",user.toString(),"get","system",CommandPolicy.CSC_KEY)))
    override fun write(value:String?) {
        check(value==null || value in setOf("0","1")){"Invalid CSC state"}
        val args=listOf("settings","--user",user.toString(),if(value==null)"delete" else "put","system",CommandPolicy.CSC_KEY)
        executor.execute(if(value==null)args else args+value).requireOutput()
    }
}

class AppAudioTarget(private val executor: CommandExecutor, private val user:Int, private val pkg:String):StateTarget {
    init { require(ModePolicy.validPackage(pkg)) }
    override fun read():String {
        val output=executor.execute(listOf("cmd","appops","get","--user",user.toString(),pkg,"PLAY_AUDIO")).requireOutput()
        if(output.trim()=="No operations.")return "default"
        val modes=Regex("PLAY_AUDIO:\\s*(?:mode=)?(default|allow|ignore|deny|foreground)\\b").findAll(output).map{it.groupValues[1]}.toList()
        check(modes.size==1){"Unrecognized or ambiguous PLAY_AUDIO response"}
        return modes.single()
    }
    override fun write(value:String?) {
        check(value in setOf("default","allow","ignore","deny","foreground")){"Invalid audio operation state"}
        executor.execute(listOf("cmd","appops","set","--user",user.toString(),pkg,"PLAY_AUDIO",value!!)).requireOutput()
    }
}

class PropertyTarget(private val executor: CommandExecutor, private val keys:List<String>):StateTarget {
    init {require(keys.isNotEmpty() && keys.distinct().size==keys.size && keys.all{it in CommandPolicy.properties})}
    override fun read():String = keys.joinToString("\n"){key ->
        val value=executor.execute(listOf("getprop",key)).requireOutput()
        check(value in setOf("0","1","true","false")){"Unsupported property value for $key"}
        "$key=$value"
    }
    override fun write(value:String?) {
        check(value!=null){"Property originals cannot be missing"}
        val entries=value.lines().map{it.split('=',limit=2)}
        check(entries.all{it.size==2} && entries.map{it[0]}==keys){"Property identity changed"}
        check(entries.all{it[1] in setOf("0","1","true","false")}){"Invalid property value"}
        entries.forEach {executor.execute(listOf("resetprop",it[0],it[1])).requireOutput()}
    }
}

class AudioGroupTarget(private val executor:CommandExecutor, private val readGroup:()->AudioGroup):StateTarget {
    override fun read():String = encode(readGroup())
    override fun write(value:String?) {
        check(value!=null){"Audio group original cannot be missing"}
        val parts=value.split('|');check(parts.size==3){"Invalid audio group state"}
        val id=parts[0].toIntOrNull();val muted=parts[1].toBooleanStrictOrNull()
        check(id!=null && muted!=null){"Invalid audio group identity"}
        val entries=parts[2].split(',').map { pair ->
            val p=pair.split('=');check(p.size==2)
            val route=p[0].toIntOrNull();val volume=p[1].toIntOrNull()
            check(route!=null && route>=0 && volume!=null && volume in 0..1000)
            route to volume
        }
        val current=readGroup()
        check(current.id==id && entries.map{it.first}.toSet()==current.volumes.keys && entries.distinctBy{it.first}.size==entries.size){"Audio group or output routes changed"}
        check(entries.map{it.second}.distinct().size==1){"Cannot restore differing route volumes"}
        executor.execute(listOf("cmd","audio","set-group-volume",id.toString(),entries.first().second.toString())).requireOutput()
        executor.execute(listOf("cmd","audio","adj-group-volume",id.toString(),if(muted)"MUTE" else "UNMUTE")).requireOutput()
    }
    fun mutedState(group:AudioGroup):String = encode(group.copy(muted=true,volumes=group.volumes.mapValues{0}))
    private fun encode(group:AudioGroup):String = "${group.id}|${group.muted}|${group.volumes.toSortedMap().entries.joinToString(","){"${it.key}=${it.value}"}}"
}
