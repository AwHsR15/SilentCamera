package com.marvis.silentcamera.shell

import android.os.Bundle
import com.marvis.silentcamera.IShellService
import com.marvis.silentcamera.core.CommandPolicy
import com.marvis.silentcamera.core.CommandResult
import com.marvis.silentcamera.core.ProcessRunner
import org.json.JSONArray
import org.json.JSONObject

/** Started by Shizuku as shell/root. It is deliberately not an exported Android service. */
class ShellService : IShellService.Stub() {
    override fun execute(argv: Array<out String>): Bundle {
        val args = argv.toList()
        val result = if (CommandPolicy.allowed(args)) ProcessRunner.run(args, outputLimit = 196_608)
            else CommandResult(126, stderr = "Command rejected by allowlist")
        return Bundle().apply {
            putInt("code", result.exitCode)
            putString("out", result.stdout)
            putString("err", result.stderr)
        }
    }

    override fun audioGroups(): String = try {
        // Read the actual platform mapping; group IDs vary by OEM and Android version.
        val type = Class.forName("android.media.audiopolicy.AudioVolumeGroup")
        val groups = type.getMethod("getAudioVolumeGroups").invoke(null) as List<*>
        JSONArray().apply {
            groups.filterNotNull().forEach { group ->
                put(JSONObject().apply {
                    put("name", type.getMethod("name").invoke(group))
                    put("id", type.getMethod("getId").invoke(group))
                    put("streams", JSONArray((type.getMethod("getLegacyStreamTypes").invoke(group) as IntArray).toList()))
                })
            }
        }.toString()
    } catch (_: Exception) { "[]" }

    override fun destroy() { kotlin.system.exitProcess(0) }
}
