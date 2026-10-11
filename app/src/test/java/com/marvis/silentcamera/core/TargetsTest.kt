package com.marvis.silentcamera.core

import org.junit.Assert.*
import org.junit.Test

class TargetsTest {
    private class Executor(var result:CommandResult=CommandResult(0,"1")):CommandExecutor {
        val commands=mutableListOf<List<String>>()
        override fun execute(args:List<String>):CommandResult {commands.add(args);return result}
    }
    @Test fun cscReadAndDeleteExplicitlyUseCurrentUser() {
        val ex=Executor();val target=CscTarget(ex,10)
        assertEquals("1",target.read());target.write(null)
        assertEquals(listOf("settings","--user","10","delete","system",CommandPolicy.CSC_KEY),ex.commands.last())
    }
    @Test fun failedAppOpsReadCannotTurnIntoDefault() {
        val ex=Executor(CommandResult(1,"","denied"))
        try { AppAudioTarget(ex,10,"com.example.camera").read();fail() }catch(_:IllegalStateException){}
    }
    @Test fun appOpsParsesExplicitAndAbsentModesAndWritesScopedValue() {
        val ex=Executor(CommandResult(0,"PLAY_AUDIO: ignore; time=123"));val t=AppAudioTarget(ex,10,"com.example.camera")
        assertEquals("ignore",t.read());ex.result=CommandResult(0,"No operations.");assertEquals("default",t.read())
        t.write("allow");assertEquals(listOf("cmd","appops","set","--user","10","com.example.camera","PLAY_AUDIO","allow"),ex.commands.last())
    }
    @Test fun unknownAppOpsOutputIsRejected() {
        val t=AppAudioTarget(Executor(CommandResult(0,"unexpected output")),0,"com.example.camera")
        try {t.read();fail()}catch(_:IllegalStateException){}
    }
    @Test fun absentAppOpMayIncludeItsPlatformDefaultWithoutCreatingAnOriginalOverride() {
        val t=AppAudioTarget(Executor(CommandResult(0,"No operations.\nDefault mode: allow")),0,"com.example.camera")
        assertEquals("default",t.read())
    }
    @Test fun uidLevelAppOpCannotBeSavedAsAPackageOriginal() {
        val t=AppAudioTarget(Executor(CommandResult(0,"Uid mode: PLAY_AUDIO: ignore\nNo operations.\nDefault mode: allow")),0,"com.example.camera")
        try{t.read();fail()}catch(_:IllegalStateException){}
    }
    @Test fun rootRestoreUsesOnlyExactSupportedProperties() {
        val ex=Executor();val t=PropertyTarget(ex,listOf("ro.camera.sound.forced"))
        assertEquals("ro.camera.sound.forced=1",t.read());t.write("ro.camera.sound.forced=0")
        assertEquals(listOf("resetprop","ro.camera.sound.forced","0"),ex.commands.last())
        try {t.write("ro.serialno=0");fail()}catch(_:IllegalStateException){}
    }
    @Test fun audioGroupRestoreRefusesChangedGroupIdentity() {
        val group=AudioGroup(19,false,mapOf(2 to 4));val ex=Executor(CommandResult(0))
        val t=AudioGroupTarget(ex){group}
        assertTrue(t.read()!!.contains("19"));t.write(t.mutedState(group))
        assertEquals(listOf("cmd","audio","adj-group-volume","19","MUTE"),ex.commands.last())
        val other=AudioGroupTarget(ex){group.copy(id=20)}
        try {other.write(t.read());fail()}catch(_:IllegalStateException){}
    }
    @Test fun mutingMultipleRoutesPreservesEveryVolumeAndCanRestore() {
        var group=AudioGroup(19,false,mapOf(2 to 4,0x40000000 to 4))
        val commands=mutableListOf<List<String>>()
        val ex=CommandExecutor { args ->
            commands.add(args)
            if(args[2]=="set-group-volume")group=group.copy(volumes=group.volumes + (2 to args[4].toInt()))
            if(args[2]=="adj-group-volume")group=group.copy(muted=args[4]=="MUTE")
            CommandResult(0)
        }
        val t=AudioGroupTarget(ex){group};val original=t.read()
        val desired=t.mutedState(group);t.write(desired)
        assertEquals(desired,t.read());assertEquals(mapOf(2 to 4,0x40000000 to 4),group.volumes)
        t.write(original);assertEquals(original,t.read())
        assertTrue(commands.none{it[2]=="set-group-volume"})
    }
}
