package com.marvis.silentcamera.core

import org.junit.Assert.*
import org.junit.Test

class TransactionsTest {
    private val identity = DeviceIdentity("device/firmware",10)
    private class Store : BackupStore {
        val states=mutableMapOf<String,OriginalState>()
        var writable=true
        override fun get(key:String)=states[key]
        override fun save(key:String,state:OriginalState):Boolean { if(!writable)return false;states[key]=state;return true }
        override fun remove(key:String):Boolean { if(!writable)return false;states.remove(key);return true }
    }
    private class Target(var value:String?):StateTarget {
        var unreadable=false
        var ignoreWrites=false
        val writes=mutableListOf<String?>()
        override fun read():String? { check(!unreadable){"Read denied"};return value }
        override fun write(value:String?) { writes.add(value);if(!ignoreWrites)this.value=value }
    }
    @Test fun aFailedReadPreventsAnyWrite() {
        val store=Store();val target=Target("1").apply{unreadable=true}
        val result=SettingTransaction("csc",identity,store,target).apply("0")
        assertEquals(Outcome.FAILED,result.outcome);assertTrue(target.writes.isEmpty());assertTrue(store.states.isEmpty())
    }
    @Test fun absentOriginalIsDeletedOnRestore() {
        val store=Store();val target=Target(null);val tx=SettingTransaction("csc",identity,store,target)
        assertEquals(Outcome.APPLIED,tx.apply("0").outcome)
        assertEquals(Outcome.RESTORED,tx.restore().outcome)
        assertNull(target.value);assertTrue(store.states.isEmpty())
    }
    @Test fun reapplyNeverOverwritesTheFirstBackup() {
        val store=Store();val target=Target("1");val tx=SettingTransaction("csc",identity,store,target)
        assertTrue(tx.apply("0").successful);assertTrue(tx.apply("0").successful)
        assertEquals("1",store.states["csc"]!!.value)
        assertTrue(tx.restore().successful);assertEquals("1",target.value)
    }
    @Test fun failureToPersistBackupPreventsMutation() {
        val store=Store().apply{writable=false};val target=Target("1")
        assertEquals(Outcome.FAILED,SettingTransaction("csc",identity,store,target).apply("0").outcome)
        assertTrue(target.writes.isEmpty())
    }
    @Test fun missingVerificationIsFailureAndRollsBack() {
        val store=Store();val target=Target(null).apply{ignoreWrites=true}
        assertEquals(Outcome.FAILED,SettingTransaction("csc",identity,store,target).apply("0").outcome)
        assertEquals(listOf("0",null),target.writes)
    }
    @Test fun restoreRefusesBackupFromAnotherUserOrFirmware() {
        val store=Store();val target=Target("0")
        for (other in listOf(DeviceIdentity(identity.fingerprint,0),DeviceIdentity("another",10))) {
            store.states["csc"]=OriginalState(other,"1")
            val tx=SettingTransaction("csc",identity,store,target)
            assertEquals(Outcome.IDENTITY_MISMATCH,tx.restore().outcome)
            assertEquals(Outcome.IDENTITY_MISMATCH,tx.apply("0").outcome)
        }
        assertTrue(target.writes.isEmpty())
    }
    @Test fun failedRestoreRetainsOriginalForRetry() {
        val store=Store();val target=Target("1");val tx=SettingTransaction("csc",identity,store,target)
        tx.apply("0");target.ignoreWrites=true
        assertEquals(Outcome.FAILED,tx.restore().outcome);assertEquals("1",store.states["csc"]!!.value)
    }
    @Test fun restoreWithoutBackupDoesNotInventADefault() {
        val target=Target("0")
        assertEquals(Outcome.NO_BACKUP,SettingTransaction("csc",identity,Store(),target).restore().outcome)
        assertTrue(target.writes.isEmpty())
    }
}
