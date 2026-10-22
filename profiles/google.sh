# shellcheck shell=bash
# Baseline profile: any Google TV or Android TV. Brand profiles source this, then add to it.
#
# Batches only list Google packages that are the same on every brand. Vendor packages are NEVER disabled by the
# baseline: survey your TV (scripts/survey.sh), decide what you don't need, and put it in a brand profile or run
# `scripts/debloat.sh extra <pkg>...` one package at a time.
#
# Everything here is disabled with `pm disable-user --user 0`. Nothing is uninstalled and no app data is touched.

# shellcheck disable=SC2034  # variables are read by the scripts that source this

BATCH_ORDER=(handover ambient google-extras preloads)

# The stock home screen. Anything else that out-ranks our launcher for HOME (usually the setup wizard,
# com.google.android.tungsten.setupwraith) is detected at runtime and added to this batch by debloat.sh.
BATCH_handover=(
  com.google.android.apps.tv.launcherx      # Google TV home
  com.google.android.tvlauncher             # Android TV home
  com.google.android.leanbacklauncher       # older Android TV home
)

# Google screensaver + Assistant. Gated: the screensaver must point at our clock first.
BATCH_ambient=(
  com.google.android.apps.tv.dreamx         # Google TV ambient mode
  com.google.android.backdrop               # Android TV ambient mode
  com.google.android.katniss                # Google Assistant / voice search
)

BATCH_google_extras=(
  com.google.android.play.games
  com.google.android.videos                 # Google TV "Shop" / Play Movies
  com.google.android.feedback
  com.google.android.tvrecommendations
)

# Preinstalled streaming apps. Only run this if nobody in the house uses them.
BATCH_preloads=(
  com.google.android.youtube.tvunplugged    # YouTube TV
  com.google.android.youtube.tvmusic        # YouTube Music
  tv.pluto.android
)

# Never disabled, by any batch or `extra`. Breaking one of these costs you casting, remotes, inputs, the Play Store,
# or your Wi-Fi. Brand profiles append their own.
PROTECT=(
  com.google.android.gms com.google.android.gsf com.android.vending
  com.google.android.apps.mediashell                         # Chromecast built-in
  com.google.android.tv.remote.service                       # Google TV / Home Assistant remote
  com.google.android.webview com.android.webview
  com.google.android.inputmethod.latin com.google.android.leanback.ime
  com.android.tv.settings com.android.providers.tv com.android.tv
  com.google.android.packageinstaller com.android.packageinstaller com.google.android.permissioncontroller
  com.android.bluetooth com.android.systemui com.android.shell
  com.google.android.tv.frameworkpackagestubs com.google.android.ext.services
  "$PKG"
)
# Exact packages a brand profile has tested and lets through a protected prefix. Never overrides PROTECT.
ALLOW=()

# Whole namespaces that run inputs, tuners, picture/audio, and remotes on the common TV chipsets.
PROTECT_PREFIX=(
  android com.android.                                       # AOSP framework (incl. android.auto_generated_rro_*)
  com.mediatek. com.mstar. com.realtek. com.amlogic. com.droidlogic. com.novatek.
)
