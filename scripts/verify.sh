#!/usr/bin/env bash
# 装 APK → 启动 → 抓日志 → 转发端口 → 断言
set -uo pipefail
cd "$(dirname "$0")/.."
PKG=io.github.showys.mimoandroid
APK=app/build/outputs/apk/debug/app-debug.apk

adb install -r "$APK" || exit 1
adb shell am force-stop "$PKG"
adb logcat -c
adb shell am start -n "$PKG/.MainActivity"

echo "== 等待 TTYD_READY（最多 6 分钟）"
for _ in $(seq 1 90); do
  if adb logcat -d -s MiMoRuntime:I | grep -q "TTYD_READY"; then break; fi
  sleep 4
done

echo "== 关键日志"
adb logcat -d -s MiMoRuntime:I | tail -40

echo "== 端口断言"
PORT=$(adb logcat -d -s MiMoRuntime:I | grep -o 'TTYD_READY port=[0-9]*' | tail -1 | cut -d= -f2)
if [ -z "${PORT:-}" ]; then echo "FAIL: 没有 TTYD_READY"; exit 2; fi
adb forward "tcp:$PORT" "tcp:$PORT" >/dev/null
CODE=$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$PORT/")
echo "curl -> $CODE (期望 401)"
PASS=$(adb shell run-as "$PKG" cat files/runtime/ttyd.pass | tr -d '\r')
CODE2=$(curl -s -o /dev/null -w '%{http_code}' -u "mimo:$PASS" "http://127.0.0.1:$PORT/")
echo "curl(auth) -> $CODE2 (期望 200)"
[ "$CODE" = "401" ] && [ "$CODE2" = "200" ] || exit 3
echo "OK"
