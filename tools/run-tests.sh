#!/bin/sh
# Tests JVM de la logique pure du launcher (aucun import android.*).
set -e
cd "$(dirname "$0")/.."
OUT=build/test
rm -rf "$OUT"; mkdir -p "$OUT"
SRC=""
for c in HomeConfig Tabs Greeting StatusParser DockModel KioskScript IdlePolicy; do
  f=launcher/src/com/hy/home/$c.java
  [ -f "$f" ] && SRC="$SRC $f"
done
# shellcheck disable=SC2086
javac --release 8 -encoding UTF-8 -nowarn -d "$OUT" $SRC launcher/test/com/hy/home/*.java 2>&1 | grep -vE '^warning: \[options\]|^[0-9]+ warnings?$|JAVA_TOOL_OPTIONS|_JAVA_OPTIONS' || true
[ -f "$OUT/com/hy/home/TestRunner.class" ] || { echo "ECHEC de compilation"; exit 1; }
status=0
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "$OUT" com.hy.home.TestRunner > build/test.out 2>&1 || status=$?
grep -vE 'JAVA_TOOL_OPTIONS|_JAVA_OPTIONS' build/test.out || true
[ "$status" -eq 0 ] || exit "$status"
if [ -f "$OUT/com/hy/home/KioskScriptDump.class" ]; then
  java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "$OUT" com.hy.home.KioskScriptDump 2>/dev/null > build/kiosk.js
  node --check build/kiosk.js && echo "kiosk.js : syntaxe JS OK"
fi
