package com.marvis.silentcamera.core

class SettingTransaction(
    private val key: String,
    private val identity: DeviceIdentity,
    private val store: BackupStore,
    private val target: StateTarget,
) {
    fun apply(desired: String?): OperationResult {
        var before: String? = null
        var changed = false
        try {
            val backup = store.get(key)
            if (backup != null && backup.identity != identity) return OperationResult(Outcome.IDENTITY_MISMATCH)
            before = target.read()
            if (backup == null) check(store.save(key, OriginalState(identity, before))) { "Cannot persist original state" }
            changed = true
            target.write(desired)
            check(target.read() == desired) { "Read-back does not match requested configuration" }
            return OperationResult(Outcome.APPLIED)
        } catch (e: Exception) {
            return if (changed) rollback(before, e) else OperationResult(Outcome.FAILED, e.message.orEmpty())
        }
    }

    fun restore(): OperationResult {
        var before: String? = null
        var changed = false
        try {
            val backup = store.get(key) ?: return OperationResult(Outcome.NO_BACKUP)
            if (backup.identity != identity) return OperationResult(Outcome.IDENTITY_MISMATCH)
            before = target.read()
            changed = true
            target.write(backup.value)
            check(target.read() == backup.value) { "Original state was not restored; backup retained" }
            // A failed cleanup never undoes a successfully restored original.
            if (!store.remove(key)) return OperationResult(Outcome.FAILED, "Original restored but backup cleanup failed; retry restore")
            return OperationResult(Outcome.RESTORED)
        } catch (e: Exception) {
            return if (changed) rollback(before, e) else OperationResult(Outcome.FAILED, e.message.orEmpty())
        }
    }

    private fun rollback(before: String?, failure: Exception): OperationResult = try {
        target.write(before)
        check(target.read() == before) { "Rollback read-back failed" }
        OperationResult(Outcome.FAILED, "${failure.message.orEmpty()}; previous state restored")
    } catch (rollback: Exception) {
        OperationResult(Outcome.ROLLBACK_FAILED, "${failure.message.orEmpty()}; rollback: ${rollback.message.orEmpty()}")
    }
}
