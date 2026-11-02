#!/data/data/com.termux/files/usr/bin/bash
# Завантажує zrok2 (linux/arm64) у ~/zrok. Працює через proot: бінарник glibc.
set -eu
VER="${1:-2.0.9}"
mkdir -p "$HOME/zrok"
cd "$HOME/zrok"
URL="https://github.com/openziti/zrok/releases/download/v${VER}/zrok_${VER}_linux_arm64.tar.gz"
echo "download $URL"
curl -fL --retry 3 -o zrok.tar.gz "$URL"
tar xzf zrok.tar.gz
chmod +x zrok2
./zrok2 version 2>/dev/null || proot-distro login debian -- "$HOME/zrok/zrok2" version
echo "ok: $HOME/zrok/zrok2"
