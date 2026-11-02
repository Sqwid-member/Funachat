#!/data/data/com.termux/files/usr/bin/bash
# release-gh.sh <version> <code> [notes]
# Публікує APK і latest.json у GitHub Releases репозиторію Funachat.
# Токен — у ~/.config/funachat/gh_token (chmod 600), у репозиторій не йде.
set -eu
cd "$(dirname "$0")/.."
VER="${1:?version}"
CODE="${2:?code}"
shift 2
NOTES="${*:-FunaChat $VER}"

REPO="Sqwid-member/Funachat"
TOKEN_FILE="${GH_TOKEN_FILE:-$HOME/.config/funachat/gh_token}"
[ -r "$TOKEN_FILE" ] || { echo "немає токена: $TOKEN_FILE" >&2; exit 1; }
T=$(tr -d '\r\n' < "$TOKEN_FILE")
API="https://api.github.com/repos/$REPO"
TAG="v$CODE"
APK="dist/FunaChat.apk"
[ -f "$APK" ] || { echo "немає $APK — спершу ./tunnel/publish.sh" >&2; exit 1; }

MD5=$(md5sum "$APK" | cut -d' ' -f1)
SIZE=$(stat -c %s "$APK")
URL="https://github.com/$REPO/releases/download/$TAG/FunaChat.apk"
python3 - "$VER" "$CODE" "$URL" "$MD5" "$SIZE" "$NOTES" <<'PY'
import json, sys, datetime
ver, code, url, md5, size, notes = sys.argv[1:7]
m = {"version": ver, "code": int(code), "url": url, "md5": md5,
     "size": int(size), "notes": notes,
     "date": datetime.datetime.now().strftime("%Y-%m-%d")}
open("dist/latest.json", "w").write(json.dumps(m, ensure_ascii=False, indent=2) + "\n")
open("dist/release-body.md", "w").write(
    f"FunaChat {ver} (code {code})\n\n{notes}\n\n"
    f"- APK: `FunaChat.apk` (md5 `{md5}`, {size} B)\n"
    f"- Маніфест: `latest.json`\n")
print(json.dumps(m, ensure_ascii=False))
PY

REL=$(python3 - "$TAG" "$VER" "$CODE" <<'PY'
import json, os, sys
tag, ver, code = sys.argv[1:4]
body = open("dist/release-body.md").read()
print(json.dumps({"tag_name": tag, "name": f"FunaChat {ver} (v{code})",
                  "body": body, "draft": False, "prerelease": False}))
PY
)

RESP=$(curl -s -m 60 -X POST -H "Authorization: Bearer $T" -H "Accept: application/vnd.github+json" \
  "$API/releases" -d "$REL")
RID=$(printf '%s' "$RESP" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("id","") if not d.get("message") else "")')
if [ -z "$RID" ]; then
  echo "release failed: $(printf '%s' "$RESP" | head -c 300)" >&2
  exit 1
fi
echo "release id $RID"

UP="https://uploads.github.com/repos/$REPO/releases/$RID/assets"
for pair in "FunaChat.apk:$APK:application/vnd.android.package-archive" \
            "latest.json:dist/latest.json:application/json"; do
  NAME="${pair%%:*}"; REST="${pair#*:}"; FILE="${REST%%:*}"; TYPE="${REST#*:}"
  printf 'upload %s (%s)\n' "$NAME" "$TYPE"
  curl -s -m 300 -X POST -H "Authorization: Bearer $T" -H "Content-Type: $TYPE" \
    --data-binary "@$FILE" "$UP?name=$NAME" \
    | python3 -c 'import sys,json; d=json.load(sys.stdin); print("  ", d.get("name"), d.get("size"), d.get("state", d.get("message","")))'
done

echo "https://github.com/$REPO/releases/tag/$TAG"
