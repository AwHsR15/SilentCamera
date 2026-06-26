package com.marvis.silentcamera

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 日志行级别，用于 UI 着色。 */
enum class LogLevel { HEADER, STEP, INFO, SUCCESS, WARN, ERROR }

data class LogLine(val text: String, val level: LogLevel = LogLevel.INFO)

/** 快门强制音设置的当前状态。 */
enum class ShutterState { SILENCED, FORCED, UNSET, UNKNOWN }

class MainViewModel : ViewModel() {

    companion object {
        const val CSC_KEY = "csc_pref_camera_forced_shuttersound_key"
        private val AUDIO_PATHS = listOf(
            "/system/media/audio/ui/shutter.ogg",
            "/system/media/audio/ui/shutter_multiple.ogg",
            "/system/media/audio/ui/camera_click.ogg",
            "/system/media/audio/ui/camera_click_01.ogg",
            "/system/media/audio/ui/Shutter.ogg",
            "/system/media/audio/ui/Shutter_multiple.ogg",
        )
    }

    // ── 可观察状态 ──
    val logs: SnapshotStateList<LogLine> = mutableListOf<LogLine>().toMutableStateList()
    val isBusy = mutableStateOf(false)
    val shizukuRunning = mutableStateOf(false)
    val shizukuGranted = mutableStateOf(false)
    val shutterState = mutableStateOf(ShutterState.UNKNOWN)

    // ── 日志辅助 ──
    private fun log(text: String, level: LogLevel = LogLevel.INFO) {
        logs.add(LogLine(text, level))
    }

    fun clearLog() = logs.clear()

    fun logText(): String = logs.joinToString("\n") { it.text }

    // ── Shizuku 状态刷新 ──
    fun refreshShizukuState() {
        shizukuRunning.value = ShizukuShell.isAvailable()
        shizukuGranted.value = ShizukuShell.isGranted()
        if (shizukuRunning.value && shizukuGranted.value) {
            refreshShutterState()
        }
    }

    fun refreshShutterState() {
        if (!ensureReady(silent = true)) return
        viewModelScope.launch {
            val value = withContext(Dispatchers.IO) { readCscValue() }
            shutterState.value = toShutterState(value)
        }
    }

    private fun ensureReady(silent: Boolean = false): Boolean {
        if (!ShizukuShell.isAvailable()) {
            if (!silent) log("[!] Shizuku 未运行，请先启动 Shizuku 服务", LogLevel.ERROR)
            shizukuRunning.value = false
            return false
        }
        shizukuRunning.value = true
        if (!ShizukuShell.isGranted()) {
            if (!silent) log("[!] 尚未获得 Shizuku 权限，请先点击「请求权限」", LogLevel.ERROR)
            shizukuGranted.value = false
            return false
        }
        shizukuGranted.value = true
        return true
    }

    // ── 一键静音 ──
    fun executeSilence() {
        if (isBusy.value) return
        clearLog()
        log(">> 三星相机快门静音 <<", LogLevel.HEADER)
        if (!ensureReady()) return

        isBusy.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 1. 底层音频文件
                    log("[1/4] 检测系统底层快门音频文件…", LogLevel.STEP)
                    val files = findShutterAudioFiles()
                    if (files.isNotEmpty()) {
                        log("  发现快门音频文件（${files.size} 个）：")
                        files.forEach { log("    · $it") }
                    } else {
                        log("  未发现独立快门音频文件（多内嵌于相机 APK）")
                    }

                    // 2. 读取当前值
                    log("[2/4] 读取当前 CSC 设置…", LogLevel.STEP)
                    val before = readCscValue()
                    log("  当前值：$CSC_KEY = $before")

                    // 3. 写入
                    log("[3/4] 写入「关闭强制快门音」…", LogLevel.STEP)
                    val put = ShizukuShell.exec(
                        "settings", "put", "system", CSC_KEY, "0"
                    )
                    if (put.isSuccess) log("  [✓] 命令执行成功", LogLevel.SUCCESS)
                    else log("  [✗] 命令失败：${put.stderr.ifBlank { "exit ${put.exitCode}" }}", LogLevel.ERROR)

                    // 4. 验证
                    log("[4/4] 校验写入结果…", LogLevel.STEP)
                    val after = readCscValue()
                    log("  写入后值：$CSC_KEY = $after")
                    shutterState.value = toShutterState(after)

                    log("──────────────────────────────")
                    when {
                        (after == "0" || after == "null") && put.isSuccess -> {
                            log("[✓] 快门强制音已关闭！", LogLevel.SUCCESS)
                            log("  快门声现在跟随媒体/系统音量。")
                        }
                        after == "1" -> {
                            log("[✗] 修改未生效，设置值仍为 1", LogLevel.ERROR)
                            log("  可能系统策略阻止写入，请重试或重启后再试", LogLevel.WARN)
                        }
                        else -> log("[?] 状态不确定，当前值：$after", LogLevel.WARN)
                    }
                }
            } catch (e: Exception) {
                log("[!] 发生异常：${e.message}", LogLevel.ERROR)
            } finally {
                isBusy.value = false
            }
        }
    }

    // ── 状态检查 ──
    fun checkStatus() {
        if (isBusy.value) return
        clearLog()
        log(">> 状态检查 <<", LogLevel.HEADER)
        isBusy.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val running = ShizukuShell.isAvailable()
                    val granted = ShizukuShell.isGranted()
                    shizukuRunning.value = running
                    shizukuGranted.value = granted

                    log("[Shizuku]", LogLevel.STEP)
                    log("  服务运行：${if (running) "是" else "否"}", if (running) LogLevel.SUCCESS else LogLevel.ERROR)
                    log("  已授权：${if (granted) "是" else "否"}", if (granted) LogLevel.SUCCESS else LogLevel.WARN)

                    if (!running || !granted) {
                        log("  → 请先启动 Shizuku 并授权后再检查", LogLevel.WARN)
                        return@withContext
                    }

                    log("[系统设置]", LogLevel.STEP)
                    val value = readCscValue()
                    shutterState.value = toShutterState(value)
                    log("  $CSC_KEY = $value")
                    when (value) {
                        "0" -> log("  状态：强制快门音已关闭 ✓", LogLevel.SUCCESS)
                        "1" -> log("  状态：强制快门音开启（日版默认）", LogLevel.WARN)
                        "null" -> log("  状态：未设置（通常视为已关闭）", LogLevel.INFO)
                        else -> log("  状态：异常值（$value）", LogLevel.WARN)
                    }

                    log("[底层文件]", LogLevel.STEP)
                    val files = findShutterAudioFiles()
                    if (files.isNotEmpty()) {
                        log("  存在 ${files.size} 个快门音频文件：")
                        files.forEach { log("    · $it") }
                        log("  提示：删除这些文件需 ROOT", LogLevel.INFO)
                    } else {
                        log("  未发现独立快门音频文件")
                    }
                }
                log("──────────────────────────────")
                log("[✓] 检查完成", LogLevel.SUCCESS)
            } catch (e: Exception) {
                log("[!] 检查异常：${e.message}", LogLevel.ERROR)
            } finally {
                isBusy.value = false
            }
        }
    }

    // ── 底层操作 ──
    private fun readCscValue(): String {
        val r = ShizukuShell.exec("settings", "get", "system", CSC_KEY)
        return if (r.isSuccess) r.stdout.ifBlank { "null" } else "null"
    }

    private fun findShutterAudioFiles(): List<String> {
        // 一条 sh -c 内完成全部探测，存在的文件逐行 echo 出来
        val script = AUDIO_PATHS.joinToString(" ; ") { "[ -f \"$it\" ] && echo \"$it\"" }
        val r = ShizukuShell.execShell(script)
        return r.stdout.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun toShutterState(value: String): ShutterState = when (value) {
        "0" -> ShutterState.SILENCED
        "1" -> ShutterState.FORCED
        "null" -> ShutterState.UNSET
        else -> ShutterState.UNKNOWN
    }
}
