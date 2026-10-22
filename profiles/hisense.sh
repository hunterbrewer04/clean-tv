# Hisense Google TV (tested: Android TV 11 "songshan", MediaTek m7632, US firmware).
# shellcheck shell=bash disable=SC2034
source "$ROOT/profiles/google.sh"

BATCH_ORDER=(handover ambient google-extras vidaa preloads)
BATCH_ambient+=(com.hisense.alexa)
# VIDAA content/app-store layer that Hisense ships next to Google TV.
BATCH_vidaa=(net.vidaatv.android.tv net.vidaatv.hisense.android.tv)
BATCH_preloads+=(
  com.haystack.android                      # Haystack News
  ar.tvplayer.tv                            # TVPlayer
)

PROTECT+=(
  # Disabling the SmartSense/AIoT pair can send this firmware family into an Android Recovery loop.
  com.hisense.tv.smartsense com.vt.tv.smartsense.ui com.vt.tv.aiot
  com.hisense.tv.hitvinput com.vt.tv.input                   # HDMI/AV/tuner inputs + the native source picker
  com.vt.tv.homekitproxy                                     # AirPlay/HomeKit (MediaTek AirPlay is covered by prefix)
  com.hisense.tv.customerlauncher com.hisense.tv.smartservice com.hisense.uifac com.vt.eula
  com.hisense.tv.ota com.hisense.remoteupgrade com.vt.tv.usbota com.vt.audio_update_service
)
PROTECT_PREFIX+=(com.hisense.kpad com.hisense.favkey com.vt.tv.)

# Seen on this firmware but left alone so far (role unclear): hibrowser, tv.protector, vt.tv.mediacenter, vt.miracast.
