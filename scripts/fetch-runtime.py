#!/usr/bin/env python3
"""下载并落位 APK 需要的全部运行时资产。

用法：python scripts/fetch-runtime.py
产物：
  app/src/main/assets/runtime/ubuntu-base.tar.gz
  app/src/main/assets/runtime/mimo-linux-arm64.tar.gz
  app/src/main/assets/runtime/libtalloc.so.2
  app/src/main/assets/runtime/manifest.json
  app/src/main/jniLibs/arm64-v8a/libproot.so
  app/src/main/jniLibs/arm64-v8a/libproot-loader.so
"""
import hashlib
import io
import json
import lzma
import os
import tarfile
import time
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, ".cache")
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets", "runtime")
JNI = os.path.join(ROOT, "app", "src", "main", "jniLibs", "arm64-v8a")

MIMO_VERSION = "0.1.16"
MIMO_URL = (
    "https://mimocode.cnbj1.mi-fds.com/mimocode/mimocode/releases/"
    f"v{MIMO_VERSION}/mimocode-linux-arm64.tar.gz"
)
MIMO_SHA = "220c621eb61a175ba0bc8e9c45c217abfed8dace1f17111c3717bceb95ad2ab3"

UBUNTU_URL = (
    "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/"
    "ubuntu-base-24.04.5-base-arm64.tar.gz"
)
UBUNTU_SHA = "a91d5a93010193712d346d761372b7c9db6dfcf093893161c64ca107f05914f2"

TERMUX_INDEXES = [
    "https://packages.termux.dev/apt/termux-main/dists/stable/main/binary-aarch64/Packages",
    "https://packages-cf.termux.dev/apt/termux-main/dists/stable/main/binary-aarch64/Packages",
]
TERMUX_BASE = "https://packages.termux.dev/apt/termux-main/"
PREFIX = "data/data/com.termux/files/usr/"
ROOTS = ["proot", "libtalloc"]


def fetch(url, tries=4):
    for i in range(tries):
        try:
            print(f"  GET {url}", flush=True)
            req = urllib.request.Request(url, headers={"User-Agent": "mimo-android-fetch/1.0"})
            with urllib.request.urlopen(req, timeout=300) as r:
                return r.read()
        except Exception as e:  # noqa: BLE001
            print(f"  ! {e} (try {i + 1}/{tries})", flush=True)
            time.sleep(2 + 3 * i)
    raise SystemExit(f"下载失败：{url}")


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def parse_index(text):
    out = {}
    for block in text.split("\n\n"):
        fields = {}
        for line in block.splitlines():
            if not line or line[0] in " #" or ":" not in line:
                continue
            k, _, v = line.partition(":")
            fields[k.strip()] = v.strip()
        if "Package" in fields:
            out[fields["Package"]] = fields
    return out


def resolve(pkgs, roots):
    seen, order, queue = set(), [], list(roots)
    while queue:
        name = queue.pop(0)
        if not name or name in seen:
            continue
        f = pkgs.get(name)
        if not f:
            raise SystemExit(f"Termux 源里找不到包：{name}")
        seen.add(name)
        order.append(name)
        for dep in (f.get("Depends") or "").split(","):
            dep = dep.split("(")[0].split("|")[0].strip()
            if dep:
                queue.append(dep)
    return order


def ar_members(data):
    assert data[:8] == b"!<arch>\n", "不是 ar 归档"
    off, out = 8, {}
    while off + 60 <= len(data):
        head = data[off:off + 60]
        name = head[0:16].decode().strip().rstrip("/")
        size = int(head[48:58].decode().strip())
        off += 60
        out[name] = data[off:off + size]
        off += size + (size % 2)
    return out


def deb_tar(data):
    for name, payload in ar_members(data).items():
        if name.startswith("data.tar"):
            if name.endswith(".xz"):
                return tarfile.open(fileobj=io.BytesIO(lzma.decompress(payload)), mode="r:")
            if name.endswith(".gz"):
                return tarfile.open(fileobj=io.BytesIO(payload), mode="r:gz")
            raise SystemExit(f"不支持的 data.tar 压缩格式：{name}")
    raise SystemExit("deb 里没有 data.tar")


def materialize_hardlinks(src_path, dst_path):
    """Android 17 拒绝 app 数据目录里的 hardlink（link() → EACCES），
    所以把 rootfs 里的硬链接条目展开成独立文件，再打回 tgz。"""
    src = tarfile.open(src_path, "r:gz")
    members = src.getmembers()
    by_name = {m.name.lstrip("./"): m for m in members}
    out = tarfile.open(dst_path, "w:gz")
    expanded = 0
    for m in members:
        if m.islnk():
            target = by_name.get(m.linkname.lstrip("./"))
            if target is None:
                print(f"  ! hardlink 目标缺失: {m.name} -> {m.linkname}")
                continue
            info = tarfile.TarInfo(m.name)
            info.mode = target.mode
            info.mtime = target.mtime
            info.uid, info.gid = m.uid, m.gid
            info.uname, info.gname = m.uname, m.gname
            info.size = target.size
            out.addfile(info, src.extractfile(target))
            expanded += 1
        elif m.issym() or m.isreg() or m.isdir():
            out.addfile(m, src.extractfile(m) if m.isreg() else None)
        else:
            out.addfile(m)
    out.close()
    src.close()
    print(f"  hardlink 展开 {expanded} 个 -> {dst_path}")


def main():
    os.makedirs(ASSETS, exist_ok=True)
    os.makedirs(JNI, exist_ok=True)
    os.makedirs(CACHE, exist_ok=True)

    ubuntu_src = os.path.join(CACHE, "ubuntu-base-src.tar.gz")
    ubuntu_path = os.path.join(ASSETS, "ubuntu-base.tgz")
    if not (os.path.exists(ubuntu_src) and sha256(open(ubuntu_src, "rb").read()) == UBUNTU_SHA):
        ubuntu = fetch(UBUNTU_URL)
        assert sha256(ubuntu) == UBUNTU_SHA, "ubuntu-base sha256 不匹配"
        open(ubuntu_src, "wb").write(ubuntu)
    if not os.path.exists(ubuntu_path) or os.path.getmtime(ubuntu_path) < os.path.getmtime(ubuntu_src):
        materialize_hardlinks(ubuntu_src, ubuntu_path)
    else:
        print("  cached ubuntu-base.tgz")

    mimo_path = os.path.join(ASSETS, "mimo.tgz")
    if os.path.exists(mimo_path) and sha256(open(mimo_path, "rb").read()) == MIMO_SHA:
        print("  cached mimo-linux-arm64.tar.gz")
    else:
        mimo = fetch(MIMO_URL)
        assert sha256(mimo) == MIMO_SHA, "mimo sha256 不匹配"
        open(mimo_path, "wb").write(mimo)

    index = None
    for url in TERMUX_INDEXES:
        try:
            index = parse_index(fetch(url, tries=2).decode("utf-8", "replace"))
            print(f"  index ok: {url} ({len(index)} packages)")
            break
        except SystemExit:
            continue
    if not index:
        raise SystemExit("Termux Packages 索引全部下载失败（可设置 HTTPS_PROXY 后重试）")

    versions = {}
    extra = {}
    for pkg in resolve(index, ROOTS):
        f = index[pkg]
        versions[pkg] = f["Version"]
        deb = fetch(TERMUX_BASE + f["Filename"])
        tar = deb_tar(deb)
        for member in tar.getmembers():
            if not member.isfile() or not member.name.startswith("./" + PREFIX):
                continue
            rel = member.name[len("./" + PREFIX):]
            payload = tar.extractfile(member).read()
            if rel == "bin/proot":
                open(os.path.join(JNI, "libproot.so"), "wb").write(payload)
                print(f"  -> libproot.so ({len(payload)} bytes)")
            elif rel == "libexec/proot/loader":
                open(os.path.join(JNI, "libproot-loader.so"), "wb").write(payload)
                print(f"  -> libproot-loader.so ({len(payload)} bytes)")
            elif rel.startswith("lib/libtalloc.so"):
                if len(payload) > len(extra.get("libtalloc.so.2", b"")):
                    extra["libtalloc.so.2"] = payload
            elif rel.startswith("lib/libandroid-shmem.so"):
                if len(payload) > len(extra.get("libandroid-shmem.so", b"")):
                    extra["libandroid-shmem.so"] = payload

    for name, payload in extra.items():
        open(os.path.join(ASSETS, name), "wb").write(payload)
        print(f"  -> {name} ({len(payload)} bytes)")

    for name in ("libtalloc.so.2", "libandroid-shmem.so"):
        assert os.path.exists(os.path.join(ASSETS, name)), f"缺少 {name}"

    for name in ("libproot.so", "libproot-loader.so"):
        with open(os.path.join(JNI, name), "rb") as fh:
            assert fh.read(4) == b"\x7fELF", f"{name} 不是 ELF"

    manifest = {
        "mimo_version": MIMO_VERSION,
        "mimo_sha256": MIMO_SHA,
        "ubuntu": {"file": "ubuntu-base.tgz", "sha256": UBUNTU_SHA},
        "termux": versions,
    }
    with open(os.path.join(ASSETS, "manifest.json"), "w", encoding="utf-8") as fh:
        json.dump(manifest, fh, indent=2, ensure_ascii=False)
    print(json.dumps(manifest, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
