#!/data/data/com.termux/files/usr/bin/bash
# telemetry.sh [--once|--loop [sec]|--check|--detect]
#
# Надсилає знеособлені діагностичні звіти FunaChat у Telegram через
# @funachat_bot. Токен і chat_id лежать поза репозиторієм:
#   ~/.config/funachat/telegram_bot
#   ~/.config/funachat/telegram_diag_chat
#
# Застосунок кладе файли в /sdcard/Download/FunaFiles/telemetry.
# Після успішної відправки файл переїжджає в telemetry/sent.
set -eu

DIR="${FUNA_TELEMETRY_DIR:-/sdcard/Download/FunaFiles/telemetry}"
[ -d "$DIR" ] || DIR="$HOME/storage/downloads/FunaFiles/telemetry"
CFG="$HOME/.config/funachat"
TOKEN="${FUNA_TG_TOKEN:-}"
CHAT="${FUNA_TG_CHAT:-}"
API="${FUNA_TG_API:-https://api.telegram.org}"
SILENT="${FUNA_TG_SILENT:-1}"
MODE="once"
SLEEP=60

case "${1:-}" in
  --loop) MODE="loop"; [ -n "${2:-}" ] && SLEEP="$2" ;;
  --check) MODE="check" ;;
  --detect) MODE="detect" ;;
  --once|"") ;;
  *) echo "usage: $0 [--once|--loop [sec]|--check|--detect]" >&2; exit 2 ;;
esac

[ -z "$TOKEN" ] && [ -r "$CFG/telegram_bot" ] && TOKEN=$(tr -d '\r\n' < "$CFG/telegram_bot")
[ -z "$CHAT" ] && [ -r "$CFG/telegram_diag_chat" ] && CHAT=$(tr -d '\r\n' < "$CFG/telegram_diag_chat")
if [ -z "$TOKEN" ] || [ -z "$CHAT" ]; then
  echo "немає токена або chat_id: $CFG/telegram_bot, $CFG/telegram_diag_chat" >&2
  exit 1
fi

api() { printf '%s/bot%s/%s' "$API" "$TOKEN" "$1"; }

if [ "$MODE" = "check" ]; then
  echo "bot:  $(curl -sS -m 20 "$(api getMe)")"
  echo "chat: $(curl -sS -m 20 "$(api "getChat?chat_id=$CHAT")")"
  exit 0
fi

# Показати чати, у яких бот бачив активність: власник має натиснути Start
# у @funachat_bot або додати бота в приватну групу й написати там.
if [ "$MODE" = "detect" ]; then
  python3 - "$(api getUpdates)" <<'PY2'
import json, sys, urllib.request
try:
    d = json.load(urllib.request.urlopen(sys.argv[1], timeout=25))
except Exception as e:
    print("не вдалося отримати getUpdates:", e)
    sys.exit(1)
if not d.get("ok"):
    print("Telegram:", d.get("description"))
    sys.exit(1)
seen = {}
for u in d.get("result", []):
    for key in ("message", "edited_message", "channel_post", "my_chat_member"):
        m = u.get(key)
        if not m:
            continue
        c = m.get("chat") or {}
        cid = c.get("id")
        if cid is None:
            continue
        name = c.get("title") or c.get("username") or c.get("first_name") or ""
        seen[cid] = (c.get("type", ""), name)
if not seen:
    print("бот не бачив жодного чату. Натисніть Start у @funachat_bot або напишіть у приватній групі з ботом.")
else:
    for cid, (typ, name) in seen.items():
        print(f"{cid}\t{typ}\t{name}")
    print("\nЗапишіть потрібний id: echo '<id>' > ~/.config/funachat/telegram_diag_chat")
PY2
  exit 0
fi

TMP=$(mktemp)
cleanup() { rm -f "$TMP"; }
trap cleanup EXIT

caption() {
  python3 - "$1" <<'PY' 2>/dev/null || basename "$1"
import json, sys
try:
    d = json.load(open(sys.argv[1], encoding="utf-8"))
except Exception:
    print("FunaChat diagnostics")
    sys.exit(0)
a = d.get("app") or {}
dv = d.get("device") or {}
s = d.get("state") or {}
parts = ["FunaChat", str(a.get("version") or "?")]
parts.append(str(dv.get("manufacturer") or "") + " " + str(dv.get("model") or ""))
if s.get("crash"): parts.append("CRASH")
parts.append(str(d.get("source") or "auto"))
print(" · ".join(p for p in parts if p.strip()))
PY
}

send_one() {
  f="$1"
  cap=$(caption "$f")
  silent=(); [ "$SILENT" = "1" ] && silent=(-F disable_notification=true)
  code=$(curl -sS -m 60 -o "$TMP" -w '%{http_code}' \
    -F "chat_id=$CHAT" \
    -F "document=@$f;type=application/json" \
    -F "caption=$cap" \
    "${silent[@]}" \
    "$(api sendDocument)") || { echo "мережа: $f" >&2; return 1; }
  if [ "$code" = "200" ] && grep -q '"ok":true' "$TMP"; then
    mkdir -p "$DIR/sent"
    mv "$f" "$DIR/sent/"
    echo "sent: $cap"
    return 0
  fi
  echo "telegram HTTP $code: $(head -c 300 "$TMP")" >&2
  return 1
}

run_once() {
  [ -d "$DIR" ] || { echo "немає теки $DIR — увімкни доступ до файлів у FunaChat" >&2; return 1; }
  found=0
  for f in "$DIR"/*.json; do
    [ -e "$f" ] || continue
    found=1
    send_one "$f" || return 1
  done
  [ "$found" = "1" ] || echo "черга порожня"
}

if [ "$MODE" = "loop" ]; then
  echo "telemetry relay: $DIR → chat $CHAT, кожні ${SLEEP}s"
  while :; do run_once || true; sleep "$SLEEP"; done
else
  run_once
fi
