#!/bin/sh
set -eu
export DEBIAN_FRONTEND=noninteractive
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

echo "STEP resolv"
rm -f /etc/resolv.conf
printf 'nameserver 223.5.5.5\nnameserver 1.1.1.1\n' > /etc/resolv.conf

echo "STEP apt-sources"
. /etc/os-release
mkdir -p /etc/apt/sources.list.d
: > /etc/apt/sources.list
cat > /etc/apt/sources.list.d/ubuntu.sources <<EOF
Types: deb
URIs: http://mirrors.tuna.tsinghua.edu.cn/ubuntu-ports/
Suites: ${VERSION_CODENAME} ${VERSION_CODENAME}-updates
Components: main universe
EOF

echo "STEP apt-update"
apt-get -o Acquire::Retries=3 update

echo "STEP dpkg-repair"
dpkg --configure -a || true
apt-get -y -f install || true

echo "STEP apt-install"
apt-get -y --no-install-recommends install \
  ca-certificates curl git ripgrep ttyd less file unzip xz-utils

echo "STEP locale"
ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime || true

echo "STEP mimo"
tar -xzf /mnt/runtime/mimo.tgz -C /usr/local/bin
chmod 755 /usr/local/bin/mimo
mkdir -p /root/projects
/usr/local/bin/mimo --version

echo "STEP done"
touch /root/.provisioned
