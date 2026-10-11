package com.marvis.silentcamera.core

data class CommandResult(val exitCode: Int, val stdout: String = "", val stderr: String = "") {
    val isSuccess get() = exitCode == 0
    fun requireOutput(): String {
        check(isSuccess) { stderr.ifBlank { "Command failed ($exitCode)" } }
        return stdout.trim()
    }
}

fun interface CommandExecutor { fun execute(args: List<String>): CommandResult }

data class DeviceIdentity(val fingerprint: String, val userId: Int)
data class OriginalState(val identity: DeviceIdentity, val value: String?)
interface BackupStore {
    fun get(key: String): OriginalState?
    fun save(key: String, state: OriginalState): Boolean
    fun remove(key: String): Boolean
}
interface StateTarget {
    fun read(): String?
    fun write(value: String?)
}
enum class Outcome { APPLIED, RESTORED, FAILED, ROLLBACK_FAILED, NO_BACKUP, IDENTITY_MISMATCH }
data class OperationResult(val outcome: Outcome, val detail: String = "") {
    val successful get() = outcome == Outcome.APPLIED || outcome == Outcome.RESTORED
}
enum class SilenceMode { CSC, AUDIO_GROUP, APP_AUDIO, ROOT_PROPERTY, MAGISK, CAMERA }
enum class Availability { AVAILABLE, NEEDS_PERMISSION, EXPERIMENTAL, UNSUPPORTED, DETECT_REQUIRED }
data class AudioGroup(val id: Int, val muted: Boolean, val volumes: Map<Int, Int>)
