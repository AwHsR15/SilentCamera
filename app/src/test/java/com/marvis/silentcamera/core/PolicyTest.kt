package com.marvis.silentcamera.core

import org.junit.Assert.*
import org.junit.Test

class PolicyTest {
    @Test fun failedCscReadIsNeverAnAbsentSetting() {
        try { ModePolicy.cscValue(CommandResult(1, "", "Permission denied")); fail("Expected read failure") }
        catch (e: IllegalStateException) { assertTrue(e.message!!.contains("Permission denied")) }
    }
    @Test fun missingCscSettingIsNotZero() {
        assertNull(ModePolicy.cscValue(CommandResult(0, "null")))
        assertEquals("0", ModePolicy.cscValue(CommandResult(0, "0\n")))
    }
    @Test fun emptyOrInvalidCscReadFails() {
        for (value in listOf("", "2", "0\n1")) {
            try { ModePolicy.cscValue(CommandResult(0, value)); fail(value) }
            catch (_: IllegalStateException) { }
        }
    }
    @Test fun cscIsOnlyRecommendedForSamsung() {
        assertTrue(ModePolicy.supportsCsc("SAMSUNG"))
        for (name in listOf("Google", "Sony", "SHARP", "Fujitsu", "Kyocera")) assertFalse(ModePolicy.supportsCsc(name))
    }
    @Test fun shellMetacharactersAndFlagsAreNotPackages() {
        assertTrue(ModePolicy.validPackage("com.google.android.GoogleCamera"))
        for (name in listOf("", "--user", "com.camera;id", "$(id)", "com.camera\n", "com..camera", "com.camera/Activity")) assertFalse(name, ModePolicy.validPackage(name))
    }
    @Test fun groupIdComesFromTheDetectedGroupNotAConstant() {
        val group = AudioGroupParser.parse(dump(19, "false", "2 (speaker): 4, 40000000 (default): 4"))!!
        assertEquals(19, group.id)
        assertFalse(group.muted)
        assertEquals(mapOf(2 to 4, 0x40000000 to 4), group.volumes)
    }
    @Test fun ambiguousOrUnrestorableGroupsAreRejected() {
        assertNull(AudioGroupParser.parse(dump(7,"false","2 (speaker): 4") + dump(9,"true","2 (speaker): 0")))
        assertNull(AudioGroupParser.parse(dump(7,"false","2 (speaker): 4, 8 (headset): 2")))
        assertNull(AudioGroupParser.parse(dump(7,"false","")))
        assertNull(AudioGroupParser.parse(dump(7,"unknown","2 (speaker): 4")))
        assertNull(AudioGroupParser.parse("VOLUME GROUP AUDIO_STREAM_RING:\n Id: 2\n Muted: false\n Current: 2 (speaker): 4"))
    }
    @Test fun groupWithNoReliableIdIsRejected() {
        assertNull(AudioGroupParser.parse("- VOLUME GROUP AUDIO_STREAM_ENFORCED_AUDIBLE:\n Muted: true\n Current: 2 (speaker): 0"))
    }
    private fun dump(id:Int, muted:String, volumes:String) = """
        - VOLUME GROUP AUDIO_STREAM_ENFORCED_AUDIBLE:
            Id: $id
            Muted: $muted
            Min: 0
            Max: 7
            Current: $volumes
    """.trimIndent()+"\n"
}
