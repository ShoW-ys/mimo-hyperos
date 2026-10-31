package io.github.showys.mimoandroid

import android.content.Context
import android.util.Log
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

object Runtime {
    const val TAG = "MiMoRuntime"

    fun binds(ins: RuntimeInstaller): List<String> = listOf(
        "/dev",
        "/proc",
        "/sys",
        "${ins.runtime.path}:/mnt/runtime",
        "${ins.bootstrap.path}:/mnt/bootstrap",
    )

    fun argv(ctx: Context, ins: RuntimeInstaller, command: List<String>): List<String> =
        Proot.argv(ctx.applicationInfo.nativeLibraryDir, ins.rootfs.path, "/root", binds(ins), command)

    private fun processBuilder(ctx: Context, ins: RuntimeInstaller, command: List<String>): ProcessBuilder {
        val pb = ProcessBuilder(argv(ctx, ins, command)).redirectErrorStream(true)
        pb.environment().apply {
            put("PROOT_LOADER", File(ctx.applicationInfo.nativeLibraryDir, "libproot-loader.so").path)
            put("PROOT_TMP_DIR", ins.tmp.path)
            put("LD_LIBRARY_PATH", ins.native.path)
        }
        return pb
    }

    fun runBlocking(ctx: Context, ins: RuntimeInstaller, command: List<String>, onLine: (String) -> Unit): Int {
        Log.i(TAG, "exec: " + argv(ctx, ins, command).joinToString(" "))
        val p = processBuilder(ctx, ins, command).start()
        p.inputStream.bufferedReader().forEachLine { line ->
            Log.i(TAG, line)
            onLine(line)
        }
        val code = p.waitFor()
        Log.i(TAG, "exit=$code")
        return code
    }

    fun spawn(ctx: Context, ins: RuntimeInstaller, command: List<String>): Process {
        Log.i(TAG, "spawn: " + argv(ctx, ins, command).joinToString(" "))
        val p = processBuilder(ctx, ins, command).start()
        Thread {
            p.inputStream.bufferedReader().use { r ->
                r.forEachLine { Log.i(TAG, it) }
            }
        }.apply { isDaemon = true }.start()
        return p
    }

    fun waitForPort(port: Int, timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 500) }
                return
            } catch (_: Exception) {
                Thread.sleep(400)
            }
        }
        error("端口 $port 在 ${timeoutMs}ms 内未就绪")
    }
}
