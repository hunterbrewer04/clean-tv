# Debloat batches for the living-room Hisense (songshan). Sourced by debloat.sh and restore.sh.
# Every entry is disabled with `pm disable-user --user 0`: nothing is uninstalled and no app data is touched.
# NEVER add: smartsense / vt.tv.smartsense.ui / vt.tv.aiot (Recovery loop), anything com.mediatek.*, mediashell,
# AirPlay/HomeKit, tv.remote.service, autopair, hitvinput / vt.tv.input / tvcenter / providers.tv, tv.settings,
# GMS/GSF/Play Store, WebView, keyboard, Hisense customerlauncher/kpad/favkey, OTA packages, or any used streaming app.

BATCH_ORDER=(handover ambient google-extras vidaa preloads)

# Stock home + the setup wizard's RecoveryActivity, which would otherwise grab HOME (priority 1 beats our 0)
# and can re-enable launcherx. Only safe because the saved Wi-Fi is owned by android.uid.system (checked below).
BATCH_handover=(com.google.android.tungsten.setupwraith com.google.android.apps.tv.launcherx)
# Google ambient screensaver, Assistant/voice, Hisense Alexa.
BATCH_ambient=(com.google.android.apps.tv.dreamx com.google.android.katniss com.hisense.alexa)
BATCH_google_extras=(com.google.android.play.games com.google.android.videos com.google.android.feedback)
BATCH_vidaa=(net.vidaatv.android.tv net.vidaatv.hisense.android.tv)
# Unused preloads Hunter approved removing.
BATCH_preloads=(com.haystack.android ar.tvplayer.tv com.google.android.youtube.tvunplugged tv.pluto.android com.google.android.youtube.tvmusic)

batch_packages() { local v="BATCH_${1//-/_}[@]"; printf '%s\n' "${!v}"; }
