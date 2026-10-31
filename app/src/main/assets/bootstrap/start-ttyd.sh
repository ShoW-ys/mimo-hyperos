#!/bin/sh
PORT=$(cat /mnt/runtime/ttyd.port 2>/dev/null || echo 7681)
PASS=$(cat /mnt/runtime/ttyd.pass 2>/dev/null || echo devpass)
exec /usr/bin/ttyd \
  -i 127.0.0.1 -p "$PORT" -W \
  -c "mimo:$PASS" \
  -t fontSize=13 \
  -t disableLeaveAlert=true \
  -t 'theme={"background":"#101014","foreground":"#e8e8ea"}' \
  /bin/bash -lc 'cd /root/projects 2>/dev/null || cd /root; export TERM=xterm-256color; exec /usr/local/bin/mimo'
