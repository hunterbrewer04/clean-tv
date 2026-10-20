#!/usr/bin/env bash
set -euo pipefail
ADB="${ADB:-$HOME/.local/bin/adb}"
TV="${TV:-192.168.1.92:5555}"
PACKAGES=(
  com.google.android.apps.tv.launcherx
  com.google.android.apps.tv.dreamx
  com.google.android.katniss
  com.hisense.alexa
)
printf 'Phase 2 would disable for user 0:
'; printf '  %s
' "${PACKAGES[@]}"
[[ "${APPLY:-}" == "1" ]] || { echo 'Dry run only. APPLY=1 is allowed only after launcher, Play Store, inputs, casting, AirPlay, and HA remote pass.'; exit 0; }
"$ADB" connect "$TV" >/dev/null
HOME_RESULT=$("$ADB" -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1 | tr -d '')
[[ "$HOME_RESULT" == "com.hunterbrewer.livingroomlauncher/.MainActivity" ]] || { echo "Refusing: custom launcher is not resolved Home ($HOME_RESULT)" >&2; exit 1; }
for pkg in "${PACKAGES[@]}"; do
  "$ADB" -s "$TV" shell pm disable-user --user 0 "$pkg"
  sleep 15
done
# setupwraith is intentionally not touched here. We only disable it after confirming its HOME behavior on this exact build.
