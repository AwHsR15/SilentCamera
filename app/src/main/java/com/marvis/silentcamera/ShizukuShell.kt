package com.marvis.silentcamera

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 对 Shizuku 的轻量封装：状态查询 + shell 命令执行。
 *
 * 注意：[Shizuku.newProcess] 不会经过 shell 解释，因此 `&&`、`|`、通配符等
 * 都不会生效。需要 shell 语义时务必使用 [execShell]（内部走 `sh -c`）。
 */
object ShizukuShell {

    data class Result(val exitCode: Int, val stdout: String, val stderr: String) {
        val isSuccess: Boolean get() = exitCode == 0
    }

    /** Shizuku 服务（binder）是否在线。 */
    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    /** 是否已授予本应用 Shizuku 权限。 */
    fun isGranted(): Boolean = try {
        !Shizuku.isPreV11() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    // Shizuku.newProcess 在公开 API 中被标记为 private/@hide，只能通过反射调用。
    private val newProcessMethod by lazy {
        Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java,
        ).apply { isAccessible = true }
    }

    /** 直接以参数数组执行（不经过 shell）。 */
    fun exec(vararg cmd: String): Result = try {
        val process = newProcessMethod.invoke(
            null, arrayOf(*cmd), null, null
        ) as Process
        val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText().trim()
        val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText().trim()
        val code = process.waitFor()
        Result(code, stdout, stderr)
    } catch (e: Throwable) {
        val cause = (e as? java.lang.reflect.InvocationTargetException)?.targetException ?: e
        Result(-1, "", cause.message ?: "未知错误")
    }

    /** 通过 `sh -c` 执行脚本，支持 `&&`、管道、通配符等 shell 语法。 */
    fun execShell(script: String): Result = exec("sh", "-c", script)
}
