#!/usr/bin/env bash
# Install or update the launcher APK and open it. Changes nothing else; `install -r` keeps your saved tile order.
#   TV=... scripts/install.sh                     (dry run: shows which APK and target)
#   TV=... APPLY=1 scripts/install.sh             newest dist/*.apk
#   TV=... APPLY=1 APK=path/to/launcher.apk scripts/install.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
APK="${APK:-$(find "$ROOT/dist" -maxdepth 1 -name '*.apk' -printf '%T@ %p\n' 2>/dev/null | sort -rn | awk 'NR==1 {print $2}')}"
[[ -f "$APK" ]] || die "No APK. Build one (scripts/build-release.sh) or pass APK=path."
if [[ -f "$APK.sha256" ]]; then (cd "$(dirname "$APK")" && sha256sum -c --quiet "$(basename "$APK").sha256") || die "Checksum mismatch: $APK"; fi
echo "Install $(basename "$APK") on $TV (Home is currently $(home_resolved))"
require_apply
a install -r "$APK"
a shell am start -n "$OURS" >/dev/null
if [[ "$(home_resolved)" == "$OURS" ]]; then echo "Updated the live launcher."
else echo "Installed alongside the stock Home. Nothing was disabled and Home wasn't changed."; fi
