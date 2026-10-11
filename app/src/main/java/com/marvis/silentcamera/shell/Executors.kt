package com.marvis.silentcamera.shell

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.marvis.silentcamera.BuildConfig
import com.marvis.silentcamera.IShellService
import com.marvis.silentcamera.core.*
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ShizukuExecutor(context: Context) : CommandExecutor {
    @Volatile private var service: IShellService? = null
    @Volatile private var pending: CountDownLatch? = null
    private val main = Handler(Looper.getMainLooper())
    private val args = Shizuku.UserServiceArgs(ComponentName(context, ShellService::class.java))
        .daemon(false).processNameSuffix("shell").version(BuildConfig.VERSION_CODE).debuggable(BuildConfig.DEBUG)
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IShellService.Stub.asInterface(binder)
            pending?.countDown()
        }
        override fun onServiceDisconnected(name: ComponentName?) { service = null; pending?.countDown() }
    }

    private fun readyService(): IShellService {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Shell calls must run off the UI thread" }
        check(available() && granted()) { "Shizuku is not running or authorized" }
        service?.takeIf { it.asBinder().pingBinder() }?.let { return it }
        val latch = CountDownLatch(1)
        pending = latch
        main.post {
            try { Shizuku.bindUserService(args, connection) }
            catch (_: Exception) { latch.countDown() }
        }
        check(latch.await(8, TimeUnit.SECONDS)) { "Shizuku UserService connection timed out" }
        return service?.takeIf { it.asBinder().pingBinder() } ?: error("Shizuku UserService is unavailable")
    }

    override fun execute(args: List<String>): CommandResult {
        if (!CommandPolicy.allowed(args)) return CommandResult(126, stderr = "Command rejected")
        return try {
            val bundle = readyService().execute(args.toTypedArray())
            check(bundle.containsKey("code")) { "Incomplete service response" }
            CommandResult(bundle.getInt("code"), bundle.getString("out").orEmpty(), bundle.getString("err").orEmpty())
        } catch (e: Exception) { service = null; CommandResult(-1, stderr = e.message.orEmpty()) }
    }
    fun audioGroups(): String = readyService().audioGroups()
    fun close() {
        main.post { runCatching { Shizuku.unbindUserService(args, connection, true) }; service = null }
    }
    companion object {
        fun available() = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        fun granted() = runCatching { !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
    }
}

class RootExecutor : CommandExecutor {
    override fun execute(args: List<String>): CommandResult {
        if (!CommandPolicy.allowed(args)) return CommandResult(126, stderr = "Command rejected")
        val escaped = args.joinToString(" ") { "'" + it.replace("'", "'\\''") + "'" }
        return ProcessRunner.run(listOf("su", "-c", escaped), timeoutMs = 12_000, outputLimit = 196_608)
    }
}
