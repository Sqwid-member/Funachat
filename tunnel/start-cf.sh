#!/data/data/com.termux/files/usr/bin/bash
# Публічний доступ до FunaChat через Cloudflare quick tunnel.
# Ні акаунта, ні картки: cloudflared сам видає https://<random>.trycloudflare.com
# Адреса живе, поки живий процес; зміну підхоплює кнопка «З файлу» в застосунку.
set -u
cd "$(dirname "$0")"
PORT="${PORT:-8790}"
APP_PORT="${APP_PORT:-8787}"
DL="${DL:-/sdcard/Download}"
[ -d "$DL" ] || DL="$HOME/storage/downloads"

mkdir -p "$DL/FunaFiles"

if ! curl -s -m 2 "http://127.0.0.1:$PORT/updates/health" >/dev/null 2>&1; then
  APP_PORT="$APP_PORT" PORT="$PORT" nohup node proxy.js >> proxy.log 2>&1 &
  sleep 1
fi

if curl -s -m 2 "http://127.0.0.1:$PORT/updates/health" >/dev/null 2>&1; then
  echo "edge: ok (:$PORT -> :$APP_PORT)"
else
  echo "edge: FAILED, див. tunnel/proxy.log" >&2
  exit 1
fi

pkill -f "[c]loudflared tunnel --url http://127.0.0.1:$PORT" 2>/dev/null
: > cf.log
nohup cloudflared tunnel --url "http://127.0.0.1:$PORT" --no-autoupdate >> cf.log 2>&1 &

U=""
for _ in $(seq 1 40); do
  U=$(grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' cf.log 2>/dev/null | head -1)
  [ -n "$U" ] && break
  sleep 2
done

if [ -z "$U" ]; then
  echo "cloudflared: не отримав адресу, див. tunnel/cf.log" >&2
  exit 1
fi

echo "$U" > url.txt
echo "$U" > "$DL/FunaFiles/tunnel.txt"
echo "$U"
