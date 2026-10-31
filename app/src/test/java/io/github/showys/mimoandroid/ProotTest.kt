package io.github.showys.mimoandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProotTest {
    @Test
    fun `argv 以 nativeLibraryDir 里的 libproot 开头并带假 root`() {
        val argv = Proot.argv(
            nativeDir = "/data/app/x/lib/arm64",
            rootfs = "/data/user/0/pkg/files/rootfs",
            workdir = "/root",
            binds = listOf("/dev", "/proc", "/data/user/0/pkg/files/runtime:/mnt/runtime"),
            command = listOf("/bin/sh", "/mnt/bootstrap/provision.sh"),
        )
        assertEquals("/data/app/x/lib/arm64/libproot.so", argv.first())
        assertTrue(argv.containsAll(listOf("-0", "-r", "/data/user/0/pkg/files/rootfs", "-w", "/root")))
        assertTrue(argv.containsAll(listOf("-b", "/dev", "-b", "/proc")))
        assertEquals(listOf("/bin/sh", "/mnt/bootstrap/provision.sh"), argv.takeLast(2))
    }

    @Test
    fun `guest 环境固定 HOME 并禁用自动更新`() {
        val argv = Proot.argv("/n", "/r", "/root", emptyList(), listOf("/bin/true"))
        assertTrue(argv.contains("HOME=/root"))
        assertTrue(argv.contains("MIMOCODE_DISABLE_AUTOUPDATE=1"))
        assertTrue(argv.contains("TERM=xterm-256color"))
    }

    @Test
    fun `命令里不出现裸 mimo 路径`() {
        val argv = Proot.argv("/n", "/r", "/root", emptyList(), listOf("/usr/bin/env", "mimo", "--version"))
        assertTrue(argv.none { it.startsWith("/data/") && it.endsWith("/mimo") })
    }
}
