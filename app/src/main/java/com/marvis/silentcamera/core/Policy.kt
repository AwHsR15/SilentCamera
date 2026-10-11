package com.marvis.silentcamera.core

object ModePolicy {
    fun isolatedEnforcedStream(dump:String):Boolean {
        if(!dump.contains("Stream volumes (device: index)"))return false
        val headers=Regex("(?m)^\\s*- (STREAM_[A-Z_]+)(?: \\(aliased to: (STREAM_[A-Z_]+)\\))?:\\s*$").findAll(dump).toList()
        val names=headers.map{it.groupValues[1]}
        if(names.distinct().size!=names.size)return false
        if(!names.containsAll(listOf("STREAM_VOICE_CALL","STREAM_RING","STREAM_MUSIC","STREAM_ALARM","STREAM_NOTIFICATION","STREAM_SYSTEM_ENFORCED")))return false
        // AOSP omits the alias suffix when a stream maps to itself.
        fun alias(m:MatchResult)=m.groupValues[2].ifEmpty{m.groupValues[1]}
        val target="STREAM_SYSTEM_ENFORCED"
        val enforced=headers.singleOrNull{it.groupValues[1]==target} ?: return false
        return alias(enforced)==target && headers.none{it.groupValues[1]!=target && alias(it)==target}
    }
    fun validPackage(value: String): Boolean =
        Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+").matches(value)

    fun cscValue(result: CommandResult): String? = when (val value = result.requireOutput()) {
        "null" -> null
        "0", "1" -> value
        else -> error("Unexpected CSC response: ${value.take(80)}")
    }

    fun supportsCsc(manufacturer: String): Boolean = manufacturer.equals("samsung", ignoreCase = true)
}

object AudioGroupParser {
    fun parse(dump: String): AudioGroup? {
        val header = Regex("(?m)^[ \\t]*-[ \\t]*VOLUME GROUP AUDIO_STREAM_ENFORCED_AUDIBLE:[ \\t]*$")
        val matches = header.findAll(dump).toList()
        if (matches.size != 1) return null
        val start = matches.single().range.last + 1
        val rest = dump.substring(start)
        val end = Regex("(?m)^[ \\t]*-[ \\t]*VOLUME GROUP ").find(rest)?.range?.first ?: rest.length
        val block = rest.substring(0, end)
        val ids = Regex("(?m)^[ \\t]*Id:[ \\t]*(\\d+)[ \\t]*$").findAll(block).toList()
        if (ids.size != 1) return null
        val id = ids.single().groupValues[1].toIntOrNull() ?: return null
        val muted = Regex("(?m)^[ \\t]*Muted:[ \\t]*(true|false)[ \\t]*$")
            .find(block)?.groupValues?.get(1)?.toBooleanStrictOrNull() ?: return null
        val current = Regex("(?m)^[ \\t]*Current:[ \\t]*(.*)$").find(block)?.groupValues?.get(1) ?: return null
        if (current.isBlank()) return null
        val entries = current.split(',').map { part ->
            val m = Regex("[ \\t]*([0-9a-fA-F]+)[ \\t]*(?:\\([^)]*\\))?:[ \\t]*(\\d+)[ \\t]*").matchEntire(part) ?: return null
            val device = m.groupValues[1].toLongOrNull(16)?.takeIf { it <= Int.MAX_VALUE }?.toInt() ?: return null
            val volume = m.groupValues[2].toIntOrNull()?.takeIf { it in 0..1000 } ?: return null
            device to volume
        }
        if (entries.distinctBy { it.first }.size != entries.size || entries.map { it.second }.distinct().size != 1) return null
        return AudioGroup(id, muted, entries.toMap())
    }
}
