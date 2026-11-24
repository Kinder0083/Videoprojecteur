#!/bin/sh
# Prépare un APK pour l'envoi par ADB via Home Assistant : gzip -9 + base64, morceaux de 740 caractères.
# Usage : tools/apk-chunks.sh <apk>   → build/chunks/<nom>/{payload.b64,chunk_00…}
set -e
APK=$1; N=$(basename "$APK" .apk); D=build/chunks/$N
rm -rf "$D"; mkdir -p "$D"
gzip -9 -c "$APK" | base64 -w0 > "$D/payload.b64"
split -b 740 -d -a 2 "$D/payload.b64" "$D/chunk_"
echo "dossier : $D"
echo "apk_sha16=$(sha256sum "$APK" | cut -c1-16) apk_octets=$(wc -c < "$APK")"
echo "b64_sha16=$(sha256sum "$D/payload.b64" | cut -c1-16) b64_octets=$(wc -c < "$D/payload.b64") morceaux=$(ls "$D"/chunk_* | wc -l)"
