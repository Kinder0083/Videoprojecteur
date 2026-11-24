#!/bin/sh
# Construit et signe un APK sans Gradle.
# Usage : tools/build-apk.sh <dossier-app> <apk-sortie>
# Variables : SDK_BT (d8, aapt2, zipalign, apksigner), ANDROID_JAR, KEYSTORE, KS_PASS, KS_ALIAS
set -e
APP=$1; OUT=$2
: "${SDK_BT:?SDK_BT manquant}" "${ANDROID_JAR:?ANDROID_JAR manquant}" "${KEYSTORE:?KEYSTORE manquant}" "${KS_PASS:?KS_PASS manquant}" "${KS_ALIAS:?KS_ALIAS manquant}"
W=build/apk-$(basename "$APP"); rm -rf "$W"; mkdir -p "$W/classes" "$(dirname "$OUT")"
find "$APP/src" -name '*.java' > "$W/sources.txt"
javac --release 8 -encoding UTF-8 -nowarn -cp "$ANDROID_JAR" -d "$W/classes" @"$W/sources.txt" 2>&1 | grep -vE '^warning: \[options\]|^[0-9]+ warnings?$|JAVA_TOOL_OPTIONS|_JAVA_OPTIONS|deprecat|Recompile' || true
CLASSES=$(find "$W/classes" -name '*.class')
[ -n "$CLASSES" ] || { echo "ECHEC javac"; exit 1; }
if find "$W/classes" -name '*$[0-9]*.class' | grep -q .; then echo "ECHEC : classe anonyme interdite (d8)"; find "$W/classes" -name '*$[0-9]*.class'; exit 1; fi
# shellcheck disable=SC2086
"$SDK_BT/d8" --min-api 26 --lib "$ANDROID_JAR" --output "$W" $CLASSES 2>&1 | grep -vE 'JAVA_TOOL_OPTIONS|_JAVA_OPTIONS' || true
[ -f "$W/classes.dex" ] || { echo "ECHEC d8"; exit 1; }
"$SDK_BT/aapt2" link -I "$ANDROID_JAR" --manifest "$APP/AndroidManifest.xml" -o "$W/base.apk"
cp "$W/base.apk" "$W/work.apk"; (cd "$W" && zip -q work.apk classes.dex)
"$SDK_BT/zipalign" -p -f 4 "$W/work.apk" "$W/aligned.apk"
"$SDK_BT/apksigner" sign --ks "$KEYSTORE" --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" --ks-key-alias "$KS_ALIAS" --min-sdk-version 26 --out "$OUT" "$W/aligned.apk" 2>&1 | grep -vE 'JAVA_TOOL_OPTIONS|_JAVA_OPTIONS|WARNING' || true
"$SDK_BT/apksigner" verify "$OUT" 2>&1 | grep -vE 'JAVA_TOOL_OPTIONS|_JAVA_OPTIONS|WARNING' || true
echo "APK $OUT : $(wc -c < "$OUT") octets, sha256 $(sha256sum "$OUT" | cut -c1-16)"
