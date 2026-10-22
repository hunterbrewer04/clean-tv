# TCL Google TV (tested: "Smart TV" / device BeyondTV4, product G03_4K_US, Realtek, Android 11, US firmware AR24).
#
# TCL firmware varies a lot between model years. Batches below only list packages disabled on the tested set (and on
# the same G03 platform in the community cleanyourtv list); absent packages are skipped automatically. On a different
# TCL, run scripts/survey.sh first and trim what doesn't apply.
#
# Remote app buttons: Netflix/YouTube/Prime Video/Apple TV and the gear are TCL keycodes that only Google's stock
# launcher answers. After the handover they do nothing until the launcher's "Remote app buttons" accessibility
# service is on (APPLY=1 scripts/app-keys.sh).
# shellcheck shell=bash disable=SC2034
source "$ROOT/profiles/google.sh"

BATCH_ORDER=(handover ambient google-extras tcl-telemetry tcl-content preloads)

BATCH_ambient+=(
  com.google.android.tv.assistant           # Assistant for TV
  com.tcl.assistant                         # TCL voice service
  com.tcl.smartalexa                        # Alexa bridge
)
BATCH_google_extras+=(
  com.google.android.partnersetup           # partner-app attribution
  com.google.android.onetimeinitializer
  android.autoinstalls.config.google.gtvpai # silently installs partner apps
  com.google.android.syncadapters.calendar
)
BATCH_tcl_telemetry=(
  com.tcl.bi                                # TCL analytics
  com.tcl.xian.StartandroidService          # background reporting
  com.tcl.guard                             # "TGuard" usage tracking / memory cleaner
  com.tcl.usercenter                        # TCL account nags
)
BATCH_tcl_content=(
  com.tcl.dashboard                         # TCL Home panel, promos
  com.tcl.partnercustomizer                 # TCL rows on the stock home
  com.tcl.messagebox                        # pushed promos
  com.tcl.esticker                          # retail demo stickers
  com.tcl.waterfall.overseas                # TCL content portal
  com.tcl.tv.plus                           # TCL+ / TCL Channel content app
  com.tcl.tv.tclchannel                     # TCL Channel
  com.tcl.channelplus                       # Channel Plus free live channels (registers a fake "tuner" input)
  com.tcl.ocean.instructions                # e-manual
)
BATCH_preloads+=(
  com.tcl.gallery
  com.tcl.ui_mediaCenter                    # USB media player; re-enable to play files from a USB drive
)

# Every TCL package not named above stays protected. Load-bearing on this platform (do not add to a batch):
#   com.tcl.tv + com.tcl.tvinput (Live TV, HDMI/AV), com.tcl.suspension (Input key source panel), com.tcl.settings,
#   com.tcl.system.server, com.tcl.providers.config, com.tcl.initsetup, com.tcl.factory.view, com.tcl.autopair +
#   com.tcl.tcl_bt_rcu_service (remote), com.tcl.keyhelp, com.tcl.versionUpdateApp (firmware updates).
PROTECT_PREFIX+=(com.tcl.)
PROTECT+=(com.tvos com.tcl.tv com.tcl.tvinput com.tcl.suspension com.tcl.settings com.tcl.system.server
  com.tcl.providers.config com.tcl.initsetup com.tcl.factory.view com.tcl.autopair com.tcl.tcl_bt_rcu_service
  com.tcl.keyhelp com.tcl.versionUpdateApp)
# The com.tcl. prefix stays protected; these exact packages (tested above) are let through it.
ALLOW+=("${BATCH_tcl_telemetry[@]}" "${BATCH_tcl_content[@]}" com.tcl.assistant com.tcl.smartalexa com.tcl.gallery com.tcl.ui_mediaCenter
  android.autoinstalls.config.google.gtvpai)   # partner-app auto-installer; sits under the protected "android" prefix
