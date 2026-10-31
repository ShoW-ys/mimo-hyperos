# 设备审计（2026-10-31，实测）

| 项 | 值 | 命令 |
|---|---|---|
| 机型 | M367FC（Xiaomi Pad 9 Pro Max / yingtian） | `adb shell getprop ro.product.model` |
| 系统 | HyperOS OS4.0，Android 17，SDK 37 | `getprop ro.build.version.release` / `.sdk` |
| ABI | arm64-v8a（仅此一个） | `getprop ro.product.cpu.abilist` |
| 页面大小 | 4096（不是 16 KB，二进制兼容性无忧） | `getconf PAGESIZE` |
| 内置 Linux 终端 | **有**：`com.android.virtualization.terminal` | `pm list packages` |
| /data | 467 G，已用 38 G | `df -h /data` |
| 内存 | 15.7 GB | `/proc/meminfo` |
| 无线调试 | 192.168.10.58:44143，配对端口 38079 | `adb pair` / `adb connect` |

## 实测结论

- `proot` + Ubuntu 24.04 + `mimo 0.1.16` 在本机可运行；TUI 正常渲染（WebView + ttyd + xterm.js）。
- 首次配置（apt 装 git/ripgrep/ttyd + 解包 rootfs）约 4 分钟，镜像走清华 `ubuntu-ports` HTTP。
- **Android 17 / HyperOS 4 在 app 私有目录禁用了 hardlink**（`link()` → EACCES），影响 dpkg 与 rootfs 解包，
  已用 `proot --link2symlink` + 构建期硬链接展开解决（见 troubleshooting）。
