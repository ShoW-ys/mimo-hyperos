package io.github.showys.mimoandroid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.io.File

class RuntimeService : Service() {
    private var worker: Thread? = null
    private var ttyd: Process? = null

    @Volatile
    private var running = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, notification("MiMo 正在启动…"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (worker == null) {
            worker = Thread { boot() }.also { it.start() }
        }
        return START_STICKY
    }

    private fun boot() {
        val ins = RuntimeInstaller(this)
        try {
            ins.extractIfNeeded { emit(it) }

            if (!ins.isProvisioned()) {
                emit("首次配置：安装 git / ripgrep / ttyd（需联网，3–8 分钟）…")
                val code = Runtime.runBlocking(
                    this, ins, listOf("/bin/sh", "/mnt/bootstrap/provision.sh"),
                ) { emit(it) }
                check(code == 0) { "首次配置失败，退出码 $code" }
                ins.marker.writeText("ok")
            }

            val port = (20000..40000).random()
            val pass = (1..24).map { ALPHABET.random() }.joinToString("")
            File(ins.runtime, "ttyd.port").writeText(port.toString())
            File(ins.runtime, "ttyd.pass").writeText(pass)

            Runtime.runBlocking(
                this, ins, listOf("/bin/sh", "/mnt/bootstrap/setup-userland.sh"),
            ) { emit(it) }
            startUrlWatcher(ins)

            emit("启动终端服务（端口 $port）…")
            ttyd = Runtime.spawn(this, ins, listOf("/bin/sh", "/mnt/bootstrap/start-ttyd.sh"))
            Runtime.waitForPort(port, 120_000)
            Log.i(TAG, "TTYD_READY port=$port")
            emit("就绪：http://127.0.0.1:$port/")
            onReady?.invoke(port, pass)
        } catch (t: Throwable) {
            Log.e(TAG, "boot failed", t)
            emit("失败：${t.message}")
        }
    }

    private fun emit(line: String) {
        Log.i(TAG, line)
        onLog?.invoke(line)
    }

    /**
     * guest 里的 xdg-open 会把登录 URL 写到 files/runtime/open-url，
     * 这里轮询并交给系统浏览器（小米账号通常已登录，等于一键授权）。
     */
    private fun startUrlWatcher(ins: RuntimeInstaller) {
        val file = File(ins.runtime, "open-url")
        Thread {
            var last = ""
            while (running) {
                try {
                    if (file.exists()) {
                        val url = file.readText().trim()
                        file.delete()
                        if (url.startsWith("http") && url != last) {
                            last = url
                            Log.i(TAG, "OPEN_URL $url")
                            openInBrowser(url)
                        }
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "url watcher: ${t.message}")
                }
                Thread.sleep(400)
            }
        }.apply { isDaemon = true }.start()
    }

    private fun openInBrowser(url: String) {
        // 默认应用内 WebView（不触发 HyperOS 链式启动）；用户在登录页勾了「以后用浏览器」则直接走系统浏览器
        if (Prefs(this).loginMode != Prefs.LOGIN_BROWSER) {
            val inApp = Intent(this, LoginActivity::class.java)
                .putExtra(LoginActivity.EXTRA_URL, url)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { startActivity(inApp) }.isSuccess) return
        }

        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val ok = runCatching { startActivity(intent) }.isSuccess
        if (!ok) {
            val cm = getSystemService(ClipboardManager::class.java)
            cm.setPrimaryClip(ClipData.newPlainText("mimo-login", url))
            emit("没有可用浏览器，登录链接已复制到剪贴板")
        }
    }

    private fun notification(text: String): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "MiMo 运行时", NotificationManager.IMPORTANCE_LOW),
        )
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("MiMo 终端")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        running = false
        ttyd?.destroy()
        super.onDestroy()
    }

    companion object {
        const val TAG = Runtime.TAG
        const val CHANNEL = "mimo_runtime"
        const val NOTIF_ID = 42
        private const val ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789"

        var onLog: ((String) -> Unit)? = null
        var onReady: ((Int, String) -> Unit)? = null
    }
}
