package io.github.showys.mimoandroid

object Proot {
    /** 传给 guest 的环境变量；顺序稳定，便于单测。 */
    val GUEST_ENV = listOf(
        "HOME=/root",
        "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
        "TERM=xterm-256color",
        "COLORTERM=truecolor",
        "LANG=C.UTF-8",
        "LC_ALL=C.UTF-8",
        "TMPDIR=/tmp",
        "MIMOCODE_DISABLE_AUTOUPDATE=1",
        "MIMOCODE_DISABLE_LSP_DOWNLOAD=1",
    )

    fun argv(
        nativeDir: String,
        rootfs: String,
        workdir: String,
        binds: List<String>,
        command: List<String>,
    ): List<String> = buildList {
        add("$nativeDir/libproot.so")
        add("--kill-on-exit")
        add("-0")                       // 假 root：只伪造 uid，不提权
        add("-r"); add(rootfs)
        add("-w"); add(workdir)
        for (b in binds) { add("-b"); add(b) }
        add("/usr/bin/env")
        addAll(GUEST_ENV)
        addAll(command)
    }
}
