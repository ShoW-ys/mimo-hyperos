package io.github.showys.mimoandroid

import android.content.Context
import android.util.Log
import java.io.File

class RuntimeInstaller(ctx: Context) {
    val files: File = ctx.filesDir
    val rootfs = File(files, "rootfs")
    val runtime = File(files, "runtime")
    val bootstrap = File(files, "bootstrap")
    val native = File(files, "native")
    val tmp = File(files, "tmp")
    val marker = File(files, "provisioned.ok")

    private val ctx: Context = ctx.applicationContext

    fun isProvisioned(): Boolean = marker.exists() && File(rootfs, "bin/bash").exists()

    fun extractIfNeeded(log: (String) -> Unit) {
        listOf(runtime, bootstrap, native, tmp).forEach { it.mkdirs() }

        copyAsset("runtime/ubuntu-base.tar.gz", File(runtime, "ubuntu-base.tar.gz"))
        copyAsset("runtime/mimo-linux-arm64.tar.gz", File(runtime, "mimo.tar.gz"))
        copyAsset("runtime/libtalloc.so.2", File(native, "libtalloc.so.2"))
        copyAsset("runtime/libandroid-shmem.so", File(native, "libandroid-shmem.so"))
        copyAsset("bootstrap/provision.sh", File(bootstrap, "provision.sh"))
        copyAsset("bootstrap/start-ttyd.sh", File(bootstrap, "start-ttyd.sh"))

        if (!File(rootfs, "bin/bash").exists()) {
            log("解包 Ubuntu rootfs（约 1.2 GB，首次较慢）…")
            rootfs.mkdirs()
            val p = ProcessBuilder(
                "/system/bin/tar", "-xzf",
                File(runtime, "ubuntu-base.tar.gz").path, "-C", rootfs.path,
            ).redirectErrorStream(true).start()
            p.inputStream.bufferedReader().forEachLine { Log.i(Runtime.TAG, "tar: $it") }
            check(p.waitFor() == 0) { "rootfs 解包失败" }
            log("rootfs 解包完成")
        }
    }

    private fun copyAsset(asset: String, dst: File) {
        if (dst.exists() && dst.length() > 0) return
        Log.i(Runtime.TAG, "释放 $asset")
        dst.parentFile?.mkdirs()
        ctx.assets.open(asset).use { input ->
            dst.outputStream().use { out -> input.copyTo(out, 1 shl 20) }
        }
    }
}
