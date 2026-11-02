#!/data/data/com.termux/files/usr/bin/bash
# Один вхід: спершу Cloudflare (швидко, без акаунта), якщо не вийшло — zrok.
set -u
cd "$(dirname "$0")"

if ./start-cf.sh; then
  echo "channel: cloudflare"
  exit 0
fi

echo "cloudflare failed, trying zrok" >&2
if ./start-zrok.sh; then
  echo "channel: zrok"
  exit 0
fi

echo "no public channel available" >&2
exit 1
