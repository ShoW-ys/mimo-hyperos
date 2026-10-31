# 第三方组件与许可证

本项目自身代码为 MIT（见 `LICENSE`）。**构建出的 APK 里打包了 GPLv2 的 proot**，
因此分发 APK 时需要同时提供该组件的对应源码（下方给了上游链接）。

| 组件 | 用途 | 许可证 | 来源 |
|---|---|---|---|
| MiMoCode（`mimo` 二进制） | 终端编程助手 | MIT + Use Restrictions | https://github.com/XiaomiMiMo/MiMo-Code |
| Ubuntu base 24.04 (arm64) | 无 root Linux 用户态 | 各软件包各自许可证（GPL/LGPL/MIT/BSD…） | https://cdimage.ubuntu.com/ubuntu-base/ |
| Termux `proot` | 用户态 chroot（ptrace） | **GPL-2.0-or-later** | https://github.com/termux/proot |
| `libtalloc` | proot 依赖 | LGPL-3.0-or-later | https://talloc.samba.org/ |
| `libandroid-shmem` | proot 依赖（SysV shm 兼容） | 见上游 | https://github.com/termux/libandroid-shmem |
| `ttyd` | 在 guest 内把 TUI 暴露为 HTTP/WebSocket | MIT | https://github.com/tsl0922/ttyd |
| xterm.js（随 ttyd 分发） | 浏览器端终端渲染 | MIT | https://github.com/xtermjs/xterm.js |

本项目**不是**小米官方项目，与小米公司无隶属或背书关系；不使用小米商标与 Logo。
