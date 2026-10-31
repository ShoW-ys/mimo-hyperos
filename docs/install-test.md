# 安装 / 卸载 实测（2026-10-31，Xiaomi Pad 9 Pro Max / HyperOS 4）

## 1. 卸载

```bash
adb uninstall io.github.showys.mimoandroid
```

结果：`Success`。逐项校验：

| 检查 | 命令 | 结果 |
|---|---|---|
| 包已移除 | `pm list packages \| grep mimoandroid` | 无输出 |
| 数据目录已删 | `ls /data/user/0/io.github.showys.mimoandroid` | No such file or directory |
| 进程已停 | `pidof io.github.showys.mimoandroid` | 无输出 |
| APK 目录已删 | `ls /data/app/*mimoandroid*` | No such file or directory |
| 磁盘回吐 | `df -h /data` | 39G → 38G（rootfs+数据约 0.7 GB 全清） |

没有写 `/sdcard`，因此主存储无残留。唯一需要手动还原的是系统设置里的电池策略/自启动白名单。

## 2. 全新安装（干净设备）

```bash
adb install app-debug.apk
```

**注意**：HyperOS 会弹「USB安装提示 / 正在通过 USB 安装此应用，是否继续？」，必须点一次 **继续安装**
（约 5 秒倒计时，超时即 `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`）。
若要免确认：开发者选项里开启「USB 调试（安全设置）」（需登录小米账号）。

冷启动实测 **68 秒**：

```
STEP resolv → apt-sources → apt-update → dpkg-repair → apt-install → locale → mimo → 0.1.16 → done
TTYD_READY port=34347
```

随后 `401`（无口令）/ `200`（带口令）；TUI 出现「Quick safety check」→ 接受后出现 Terms & Privacy。

## 3. 覆盖安装（升级）

```bash
adb install -r app-debug.apk   # 同样要点一次「继续安装」
```

数据保留（`files/rootfs`、`provisioned.ok`、`~/.mimocode` 均在），再次启动 **2–3 秒** 出 `TTYD_READY`，
日志里没有 `STEP`——即不会重复 apt。

## 4. 自动化点击安装确认框

```bash
# 安装放到后台跑，前台轮询焦点并点「继续安装」
adb shell dumpsys window | grep -q com.miui.securitycenter && adb shell input tap 1493 1410
```

（坐标按 3408×2272 横屏；按钮文字用 `uiautomator dump` 可精确取得。）

## 5. 本次测试暴露并修复的问题

- 通知权限弹窗原本在 `onCreate` 就请求，会盖住终端并让后续 `am start` 变成
  “intent delivered to currently running top-most instance”。已改为 **TUI 就绪后**再请求。
