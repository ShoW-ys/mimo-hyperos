# 排障：本项目实际踩过的坑

## 1. `assets/runtime/ubuntu-base.tar.gz` 找不到（FileNotFoundException）

aapt 会把 `.gz` 资源**压缩并去掉后缀**，装进 APK 后变成 `ubuntu-base.tar`。
→ 资产一律用 `.tgz` 命名（`scripts/fetch-runtime.py` 已按此生成），并保留 `openAsset()` 的后缀回退。

## 2. `tar: can't link 'usr/bin/perl5.38.2' -> 'usr/bin/perl': Permission denied`

Android 17 / HyperOS 4 的 SELinux **禁止 app 数据目录里创建硬链接**（普通 app UID 也一样，
试过 `run-as … ln a b` 直接失败）。两处受影响：

- 解包 rootfs 的 tar → 构建期用 Python 把硬链接条目展开成独立文件（`materialize_hardlinks`）。
- dpkg 的备份链接 → proot 加 `--link2symlink`，让它用符号链接模拟。

## 3. `Certificate verification failed` / `No system certificates available`

ubuntu-base 镜像**没有 ca-certificates**，`https://` 镜像源必然失败。
→ 首次配置改用 `http://mirrors.tuna.tsinghua.edu.cn/ubuntu-ports/`，`ca-certificates` 装好后再需要 HTTPS。

## 4. dpkg 被中断后一直 `dpkg was interrupted`

`provision.sh` 里先跑 `dpkg --configure -a || true` 与 `apt-get -y -f install || true` 再安装。
彻底脏了就把 `files/rootfs` 整个删掉重启 App（会重新解包）。

## 5. 改了 assets 但设备上还是旧文件

`RuntimeInstaller.copyAsset` 早期只看「文件存在」，导致升级后仍用旧的 provision.sh / rootfs。
→ 现在用 `AssetFileDescriptor.declaredLength` 比对长度，长度不同就重写；bootstrap 脚本强制覆盖。

## 6. WebView 里键盘不生效

先点一下终端区域让 xterm 的隐藏 textarea 获得焦点，再发键值（`adb shell input keyevent 66`）。
底部工具条按钮就是给没有 Esc/Tab 的手机键盘用的。

## 7. 快速定位

```bash
adb logcat -d -s MiMoRuntime:I | tail -40     # 运行时日志（STEP/PROOT_OK/TTYD_READY）
adb shell run-as io.github.showys.mimoandroid ls files/rootfs/root/.mimocode
adb shell run-as io.github.showys.mimoandroid cat files/runtime/ttyd.pass
```
