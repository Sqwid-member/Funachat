#!/data/data/com.termux/files/usr/bin/bash
# audit-key.sh <base-url> <fk-key> [info|revoke|notify] [note]
#
# Перевіряє злитий API-ключ на конкретному пристрої FunaChat.
# Доступ:
#   * секрет аудиту пристрою — FUNA_AUDIT_SECRET або
#     ~/.config/funachat/audit_secret (скопіювати в застосунку:
#     Налаштування → API → Аудит ключів);
#   * ключ розробника — ~/.config/funachat/dev_audit_key.pem
#     (підпис ECDSA, працює на будь-якому пристрої).
#
# Приклади:
#   ./audit-key.sh https://example.trycloudflare.com fk-...
#   ./audit-key.sh https://example.trycloudflare.com fk-... revoke
#   ./audit-key.sh http://127.0.0.1:8787 fk-... notify "ключ у публічному репо"
set -eu

BASE="${1:-}"
KEY="${2:-}"
ACTION="${3:-info}"
NOTE="${4:-}"
if [ -z "$BASE" ] || [ -z "$KEY" ]; then
  sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'
  exit 2
fi
BASE="${BASE%/}"
case "$ACTION" in info|revoke|notify) ;; *) echo "action: info|revoke|notify" >&2; exit 2 ;; esac
case "$KEY" in fk-*) ;; *) echo "ключ має починатися з fk-" >&2; exit 2 ;; esac

OPT="$HOME/.config/funachat"
SECRET="${FUNA_AUDIT_SECRET:-}"
[ -z "$SECRET" ] && [ -r "$OPT/audit_secret" ] && SECRET=$(tr -d '\r\n' < "$OPT/audit_secret")
DEVKEY="$OPT/dev_audit_key.pem"
MODE=""
if [ "${FUNA_DEV_AUDIT:-0}" = "1" ]; then
  MODE="dev"
elif [ -n "$SECRET" ]; then
  MODE="secret"
elif [ -r "$DEVKEY" ]; then
  MODE="dev"
else
  echo "немає доступу: задайте FUNA_AUDIT_SECRET, ~/.config/funachat/audit_secret" >&2
  echo "або покладіть приватний ключ розробника в $DEVKEY" >&2
  exit 1
fi
[ "$MODE" = "dev" ] && { [ -r "$DEVKEY" ] || { echo "немає $DEVKEY" >&2; exit 1; }; }

if [ "$ACTION" = "revoke" ] && [ "${FUNA_YES:-0}" != "1" ]; then
  printf 'Відкликати ключ %s на %s? [y/N] ' "$KEY" "$BASE" >&2
  read -r ans
  case "$ans" in y|Y|yes|YES) ;; *) echo "скасовано" >&2; exit 1 ;; esac
fi

TMP=$(mktemp)
OUT=$(mktemp)
cleanup() { rm -f "$TMP" "$OUT"; }
trap cleanup EXIT

python3 - "$KEY" "$ACTION" "$NOTE" > "$TMP" <<'PY'
import json, secrets, sys, time
key, action, note = sys.argv[1:4]
print(json.dumps({
    "key": key, "action": action, "note": note,
    "ts": int(time.time() * 1000), "nonce": secrets.token_hex(16),
}, ensure_ascii=False, separators=(",", ":")))
PY

if [ "$MODE" = "dev" ]; then
  SIG=$(openssl dgst -sha256 -sign "$DEVKEY" "$TMP" | base64 -w0)
  AUTH=(-H "X-Funa-Dev-Signature: $SIG")
else
  AUTH=(-H "X-Funa-Audit: $SECRET")
fi

CODE=$(curl -sS -m 40 -o "$OUT" -w '%{http_code}' \
  -H "Content-Type: application/json" "${AUTH[@]}" \
  --data-binary @"$TMP" "$BASE/v1/audit/key") || {
  echo "мережа: не вдалося з'єднатися з $BASE" >&2; exit 1; }

python3 - "$OUT" "$CODE" "$MODE" <<'PY'
import datetime, json, sys
path, code, mode = sys.argv[1], sys.argv[2], sys.argv[3]
try:
    d = json.load(open(path, encoding="utf-8"))
except Exception:
    print("HTTP", code, "— відповідь не JSON"); sys.exit(1)
if not d.get("ok"):
    e = d.get("error") or {}
    print("HTTP", code, "—", e.get("code", "error") + ":", e.get("message", ""))
    sys.exit(1)
o = d.get("owner") or {}
k = d.get("key") or {}
def dt(v):
    try: return datetime.datetime.fromtimestamp(int(v) / 1000).strftime("%Y-%m-%d %H:%M")
    except Exception: return "—"
print("owner:    ", o.get("name") or "—", ("<" + o["email"] + ">") if o.get("email") else "")
if o.get("contact"): print("contact:  ", o["contact"])
print("device:   ", o.get("device") or "—", "| version:", d.get("version") or "—")
if o.get("publicUrl"): print("public:   ", o["publicUrl"])
print("key:      ", k.get("name") or "—", k.get("mask") or "", "| id:", k.get("id") or "—")
print("created:  ", dt(k.get("created")))
print("expires:  ", "never" if not k.get("exp") else dt(k.get("exp")) + (" (expired)" if k.get("expired") else ""))
print("last use: ", dt(k.get("used")) if k.get("used") else "—",
      ("from " + k["lastIp"]) if k.get("lastIp") else "", "| hits:", k.get("hits") or 0)
print("action:   ", d.get("action"), "| revoked:", d.get("revoked"),
      "| notified:", d.get("notified"), "| remaining:", d.get("remaining"))
print("mode:     ", "developer key" if mode == "dev" else "device audit secret")
PY
