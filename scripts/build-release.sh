#!/usr/bin/env bash
# Tests, lint, and a signed R8 release APK in dist/.
#
# Needs JDK 17 and an Android SDK with platform 34. Point SIGNING_PROPERTIES at a file like:
#   storeFile=/abs/path/release.jks
#   storePassword=...
#   keyAlias=launcher
#   keyPassword=...
# Make a keystore once and keep it (and the passwords) out of Git. Losing it means uninstall + reinstall to update:
#   keytool -genkeypair -v -keystore release.jks -alias launcher -keyalg RSA -keysize 4096 -validity 10000
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
[[ -n "${JAVA_HOME:-}" || ! -d "$HOME/Android/jdk17" ]] || export JAVA_HOME="$HOME/Android/jdk17"
[[ -z "${JAVA_HOME:-}" ]] || export PATH="$JAVA_HOME/bin:$PATH"   # apksigner runs whatever `java` is on PATH
export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/sdk}}"
export SIGNING_PROPERTIES="${SIGNING_PROPERTIES:-$HOME/Android/signing/clean-tv.properties}"
[[ -d "$ANDROID_HOME/platforms/android-34" ]] || { echo "Missing Android SDK platform 34 under ANDROID_HOME=$ANDROID_HOME" >&2; exit 1; }
[[ -f "$SIGNING_PROPERTIES" ]] || { echo "Missing signing properties: $SIGNING_PROPERTIES (see the top of this script)" >&2; exit 1; }
cd "$ROOT"
VERSION="$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' launcher/app/build.gradle.kts)"
[[ -n "$VERSION" ]] || { echo "Couldn't read versionName from launcher/app/build.gradle.kts" >&2; exit 1; }
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > launcher/local.properties
(cd launcher && ./gradlew clean testDebugUnitTest lintDebug assembleRelease --no-daemon)
OUT="dist/clean-tv-v$VERSION.apk"
mkdir -p dist
cp launcher/app/build/outputs/apk/release/app-release.apk "$OUT"
(cd dist && sha256sum "$(basename "$OUT")" > "$(basename "$OUT").sha256")
BT="$(ls -d "$ANDROID_HOME"/build-tools/* 2>/dev/null | sort -V | tail -n 1)"
if [[ -x "$BT/apksigner" ]]; then "$BT/apksigner" verify --print-certs "$OUT" | grep -E 'Signer #1 certificate (DN|SHA-256)'; fi
echo "Built: $ROOT/$OUT"
