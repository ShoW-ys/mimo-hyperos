#!/bin/sh
# 每次启动都跑：补齐 guest 侧的小工具（幂等、<1 秒）
set -eu

# xdg-open 桥：把 mimo 想打开的 URL 交给 Android 侧用系统浏览器打开
cat > /usr/local/bin/xdg-open <<'EOF'
#!/bin/sh
for a in "$@"; do
  case "$a" in
    http://*|https://*)
      printf '%s' "$a" > /mnt/runtime/open-url.tmp
      mv /mnt/runtime/open-url.tmp /mnt/runtime/open-url
      exit 0
      ;;
  esac
done
echo "xdg-open: no url" >&2
exit 1
EOF
chmod 755 /usr/local/bin/xdg-open

mkdir -p /root/projects
exit 0
