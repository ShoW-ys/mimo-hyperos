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

        copyAsset("runtime/ubuntu-base.tgz", File(runtime, "ubuntu-base.tgz"))
        copyAsset("runtime/mimo.tgz", File(runtime, "mimo.tgz"))
        copyAsset("runtime/libtalloc.so.2", File(native, "libtalloc.so.2"))
        copyAsset("runtime/libandroid-shmem.so", File(native, "libandroid-shmem.so"))
        copyAsset("bootstrap/provision.sh", File(bootstrap, "provision.sh"), force = true)
        copyAsset("bootstrap/start-ttyd.sh", File(bootstrap, "start-ttyd.sh"), force = true)
        copyAsset("bootstrap/setup-userland.sh", File(bootstrap, "setup-userland.sh"), force = true)

        if (!File(rootfs, "bin/bash").exists()) {
            log("解包 Ubuntu rootfs（约 1.2 GB，首次较慢）…")
            rootfs.mkdirs()
            val p = ProcessBuilder(
                "/system/bin/tar", "-xzf",
                File(runtime, "ubuntu-base.tgz").path, "-C", rootfs.path,
            ).redirectErrorStream(true).start()
            p.inputStream.bufferedReader().forEachLine { Log.i(Runtime.TAG, "tar: $it") }
            check(p.waitFor() == 0) { "rootfs 解包失败" }
            log("rootfs 解包完成")
        }
    }

    private fun copyAsset(asset: String, dst: File, force: Boolean = false) {
        val expected = runCatching { ctx.assets.openFd(asset).use { it.declaredLength } }.getOrNull()
        if (!force && dst.exists() && dst.length() > 0 && (expected == null || dst.length() == expected)) return
        Log.i(Runtime.TAG, "释放 $asset")
        dst.parentFile?.mkdirs()
        openAsset(asset).use { input ->
            dst.outputStream().use { out -> input.copyTo(out, 1 shl 20) }
        }
        // Windows 上被转成 CRLF 的 .sh 在 dash 里会 `set: Illegal option -`，这里强制 LF
        if (dst.name.endsWith(".sh")) {
            val text = dst.readText().replace("\r\n", "\n")
            dst.writeText(text)
        }
    }

    /** aapt 在老版本行为下会把 .gz 资源改名去掉后缀，这里两种名字都试。 */
    private fun openAsset(asset: String): java.io.InputStream = try {
        ctx.assets.open(asset)
    } catch (_: java.io.FileNotFoundException) {
        ctx.assets.open(asset.removeSuffix(".gz"))
    }
}
