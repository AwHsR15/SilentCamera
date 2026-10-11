package com.marvis.silentcamera.core

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

object ProcessRunner {
    fun run(args: List<String>, timeoutMs: Long = 12_000, outputLimit: Int = 32_768): CommandResult {
        require(args.isNotEmpty() && timeoutMs > 0 && outputLimit > 0)
        var process: Process? = null
        try {
            process = ProcessBuilder(args).start()
            process.outputStream.close()
            val stdout = Collector(process.inputStream, outputLimit)
            val stderr = Collector(process.errorStream, outputLimit)
            val outThread = Thread(stdout, "shell-stdout").apply { isDaemon = true; start() }
            val errThread = Thread(stderr, "shell-stderr").apply { isDaemon = true; start() }
            val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!completed) process.destroyForcibly()
            outThread.join(250)
            errThread.join(250)
            return CommandResult(if (completed) process.exitValue() else 124, stdout.text(),
                if (completed) stderr.text() else "Command timed out; ${stderr.text()}".take(outputLimit))
        } catch (e: InterruptedException) {
            process?.destroyForcibly()
            Thread.currentThread().interrupt()
            return CommandResult(130, stderr = "Command interrupted")
        } catch (e: Exception) {
            process?.destroyForcibly()
            return CommandResult(-1, stderr = e.message.orEmpty().take(outputLimit))
        }
    }

    private class Collector(private val input: InputStream, private val limit: Int) : Runnable {
        private val buffer = ByteArrayOutputStream()
        override fun run() {
            try {
                input.use { stream ->
                    val bytes = ByteArray(4096)
                    while (true) {
                        val read = stream.read(bytes)
                        if (read < 0) break
                        synchronized(buffer) {
                            val remaining = limit - buffer.size()
                            if (remaining > 0) buffer.write(bytes, 0, minOf(read, remaining))
                        }
                    }
                }
            } catch (_: Exception) { /* Destruction closes a process pipe. */ }
        }
        fun text(): String = synchronized(buffer) { buffer.toString("UTF-8").trim() }
    }
}

object CommandPolicy {
    const val CSC_KEY = "csc_pref_camera_forced_shuttersound_key"
    val properties = setOf("ro.camera.sound.forced", "audio.camerasound.force")
    private fun number(value: String) = Regex("[0-9]{1,9}").matches(value)
    fun allowed(args: List<String>): Boolean {
        if (args.isEmpty() || args.any { it.length > 256 || it.contains('\u0000') }) return false
        if (args == listOf("id", "-u") || args == listOf("resetprop", "--help") || args == listOf("cmd", "activity", "get-current-user") ||
            args == listOf("dumpsys", "audio") || args == listOf("cmd", "audio", "help")) return true
        if (args.size == 2 && args[0] == "getprop") return args[1] in properties
        if (args.size == 3 && args[0] == "resetprop") return args[1] in properties && args[2] in setOf("0", "1", "true", "false")
        if (args.size in 6..7 && args.take(2) == listOf("settings", "--user") && number(args[2]) &&
            args[4] == "system" && args[5] == CSC_KEY) {
            return (args.size == 6 && args[3] in setOf("get", "delete")) ||
                (args.size == 7 && args[3] == "put" && args[6] in setOf("0", "1"))
        }
        if (args.size in 7..8 && args.take(2) == listOf("cmd", "appops") && args[3] == "--user" &&
            number(args[4]) && ModePolicy.validPackage(args[5]) && args[6] == "PLAY_AUDIO") {
            return (args.size == 7 && args[2] == "get") || (args.size == 8 && args[2] == "set" && args[7] in setOf("default", "allow", "ignore", "deny", "foreground"))
        }
        if (args.size == 5 && args.take(2) == listOf("cmd", "audio") && number(args[3])) {
            return (args[2] == "set-group-volume" && number(args[4])) ||
                (args[2] == "adj-group-volume" && args[4] in setOf("MUTE", "UNMUTE"))
        }
        return false
    }
}
