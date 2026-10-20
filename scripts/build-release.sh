#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-$HOME/Android/jdk17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/sdk}"
export LIVING_ROOM_SIGNING_PROPERTIES="${LIVING_ROOM_SIGNING_PROPERTIES:-$HOME/Android/signing/living-room-launcher.properties}"
[[ -x "$JAVA_HOME/bin/java" ]] || { echo "Missing JDK: $JAVA_HOME" >&2; exit 1; }
[[ -d "$ANDROID_HOME/platforms/android-34" ]] || { echo "Missing Android 34 SDK: $ANDROID_HOME" >&2; exit 1; }
[[ -f "$LIVING_ROOM_SIGNING_PROPERTIES" ]] || { echo "Missing signing properties: $LIVING_ROOM_SIGNING_PROPERTIES" >&2; exit 1; }
cd "$ROOT"
printf 'sdk.dir=%s
' "$ANDROID_HOME" > local.properties
./gradlew clean testDebugUnitTest lintDebug assembleRelease --no-daemon
mkdir -p dist
cp app/build/outputs/apk/release/app-release.apk dist/living-room-launcher-v0.3.0.apk
(cd dist && sha256sum living-room-launcher-v0.3.0.apk > living-room-launcher-v0.3.0.apk.sha256)
echo "Built: $ROOT/dist/living-room-launcher-v0.3.0.apk"
