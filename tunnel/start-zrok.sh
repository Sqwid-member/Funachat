#!/data/data/com.termux/files/usr/bin/bash
# Резервний канал: zrok (openziti). Безкоштовно, без картки, свій HTTPS.
#
# Одноразово:
#   1. зареєструватися на https://myzrok.io (email або GitHub) — картка не потрібна
#   2. скопіювати enable-токен
#   3. ./zrok-enable.sh <токен>
#
# Далі просто: ./start-zrok.sh
set -u
cd "$(dirname "$0")"
PORT="${PORT:-8790}"
ZROK="${ZROK:-$HOME/zrok/zrok2}"
DISTRO="${DISTRO:-debian}"
DL="${DL:-/sdcard/Download}"
[ -d "$DL" ] || DL="$HOME/storage/downloads"

Z() { proot-distro login "$DISTRO" -- "$ZROK" "$@"; }

if [ ! -x "$ZROK" ]; then
  echo "немає $ZROK — спершу завантаж zrok (див. README)" >&2
  exit 1
fi

if [ "$#" -ge 1 ] && [ "$1" = "enable" ]; then
  shift
  Z enable "$@" --headless || exit 1
fi

mkdir -p "$DL/FunaFiles"

if ! curl -s -m 2 "http://127.0.0.1:$PORT/updates/health" >/dev/null 2>&1; then
  nohup node proxy.js >> proxy.log 2>&1 &
  sleep 1
fi

pkill -f "[z]rok2 share public" 2>/dev/null
: > zrok.log
nohup proot-distro login "$DISTRO" -- "$ZROK" share public "http://127.0.0.1:$PORT" \
  --headless >> zrok.log 2>&1 &

U=""
for _ in $(seq 1 40); do
  U=$(grep -oE '[a-z0-9-]+\.(shares\.)?zrok\.io' zrok.log 2>/dev/null | head -1)
  [ -n "$U" ] && { U="https://$U"; break; }
  sleep 2
done

if [ -z "$U" ]; then
  echo "zrok: не отримав адресу, див. tunnel/zrok.log" >&2
  tail -5 zrok.log >&2
  exit 1
fi

echo "$U" > url-zrok.txt
echo "$U" > "$DL/FunaFiles/tunnel-zrok.txt"
echo "$U"
