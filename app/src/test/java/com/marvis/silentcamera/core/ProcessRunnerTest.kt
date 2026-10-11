package com.marvis.silentcamera.core

import org.junit.Assert.*
import org.junit.Test

class ProcessRunnerTest {
    @Test fun readsStderrConcurrentlyWithoutBlockingOnStdout() {
        val r=ProcessRunner.run(listOf("sh","-c","i=0; while [ \"\$i\" -lt 10000 ]; do echo warning >&2; i=\$((i+1)); done; echo done"),5000,200)
        assertTrue(r.isSuccess);assertEquals("done",r.stdout.trim());assertTrue(r.stderr.length<=200)
    }
    @Test fun timeoutTerminatesTheProcess() {
        val start=System.nanoTime()
        val r=ProcessRunner.run(listOf("sh","-c","sleep 10"),150)
        assertEquals(124,r.exitCode);assertTrue((System.nanoTime()-start)/1_000_000<2500)
    }
    @Test fun processFailureKeepsItsActualExitCode() {
        val r=ProcessRunner.run(listOf("sh","-c","echo denied >&2; exit 7"))
        assertEquals(7,r.exitCode);assertTrue(r.stderr.contains("denied"))
    }
    @Test fun shellAndArbitrarySettingsAreNotExposedByTheService() {
        assertTrue(CommandPolicy.allowed(listOf("settings","--user","10","get","system","csc_pref_camera_forced_shuttersound_key")))
        assertTrue(CommandPolicy.allowed(listOf("cmd","appops","set","--user","0","com.google.android.GoogleCamera","PLAY_AUDIO","ignore")))
        for(args in listOf(listOf("sh","-c","id"),listOf("settings","put","secure","unknown","0"),listOf("getprop","ro.serialno"),listOf("cmd","audio","set-group-volume","2;id","0"))) assertFalse(CommandPolicy.allowed(args))
    }
}
