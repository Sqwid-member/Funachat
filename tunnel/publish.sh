#!/data/data/com.termux/files/usr/bin/bash
# publish.sh <apk> <version> <code> [notes...]
# Кладе APK у dist/ і переписує release.json — edge (proxy.js) віддає його
# як /updates/latest.json з абсолютним посиланням на поточний хост.
set -eu
cd "$(dirname "$0")/.."
APK="${1:?apk}"
VER="${2:?version}"
CODE="${3:?code}"
shift 3
NOTES="${*:-}"

mkdir -p dist
cp -f "$APK" dist/FunaChat.apk
MD5=$(md5sum dist/FunaChat.apk | cut -d' ' -f1)
SIZE=$(stat -c %s dist/FunaChat.apk)
python3 - "$VER" "$CODE" "$MD5" "$SIZE" "$NOTES" <<'PY'
import json, sys, datetime
ver, code, md5, size, notes = sys.argv[1:6]
d = {"version": ver, "code": int(code), "file": "FunaChat.apk",
     "md5": md5, "size": int(size), "notes": notes,
     "date": datetime.datetime.now().strftime("%Y-%m-%d")}
open("dist/release.json", "w").write(json.dumps(d, ensure_ascii=False, indent=2) + "\n")
print("published", ver, code, md5, size)
PY
