# TCL Google TV. Starts as the Google baseline plus protection for TCL's own system layer.
#
# TCL firmware differs a lot between model years, so this profile deliberately disables nothing TCL-specific yet.
# Run scripts/survey.sh, look at the "review candidates" list, and add proven-safe packages to a batch here
# (or try one at a time with `scripts/debloat.sh extra <pkg>`).
# shellcheck shell=bash disable=SC2034
source "$ROOT/profiles/google.sh"

# Inputs, picture/sound, the source panel, remote pairing, and OTA all live under TCL namespaces.
PROTECT_PREFIX+=(com.tcl. com.tcl.tv. com.tcl.android.)
