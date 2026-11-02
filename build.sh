#!/data/data/com.termux/files/usr/bin/bash
set -e
cd "$(dirname "$0")"
ANDROID=$HOME/android-sdk/platforms/android-35/android.jar
R8=$HOME/android_tools/r8.jar
KS=$HOME/downloads/messenger_work/funa.keystore
OUT=${1:-/sdcard/Download/FunaChat.apk}

# Навички ШІ входять в APK як assets. Краще впасти на збірці, ніж віддати
# застосунок, де skill повертає «unknown skill» через забутий файл.
for s in docx pdf design docs; do
  for l in uk en ru; do
    f="assets/skills/$s.$l.md"
    [ -s "$f" ] || { echo "missing skill: $f" >&2; exit 1; }
  done
done

rm -rf build; mkdir -p build/gen build/classes build/dex
echo "== res =="
aapt2 compile --dir res -o build/res.zip >/dev/null
aapt2 link -o build/base.apk -I "$ANDROID" --manifest AndroidManifest.xml -A assets \
  --java build/gen build/res.zip --min-sdk-version 26 --target-sdk-version 34 \
  --version-code 108 --version-name 2.42.33 >/dev/null

echo "== javac =="
javac --release 8 -nowarn -cp "$ANDROID" -d build/classes $(find src -name '*.java')

echo "== d8 =="
java -cp "$R8" com.android.tools.r8.D8 --min-api 26 --lib "$ANDROID" --output build/dex \
  $(find build/classes -name '*.class')

echo "== pack =="
python3 - <<'PY2'
import zipfile
z=zipfile.ZipFile('build/base.apk','a',zipfile.ZIP_DEFLATED)
z.write('build/dex/classes.dex','classes.dex')
z.close()
PY2
zipalign -f -P 16 4 build/base.apk build/aligned.apk
rm -f "$OUT"
apksigner sign --ks "$KS" --ks-pass pass:funa123 --key-pass pass:funa123 \
  --min-sdk-version 26 --v1-signing-enabled false --v2-signing-enabled true --v3-signing-enabled true \
  --out "$OUT" build/aligned.apk
apksigner verify "$OUT"
ls -la "$OUT"
