package com.hunterbrewer.livingroomlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.net.Uri
import android.os.Build

/**
 * Brand-agnostic input discovery and switching.
 *
 * Every Android TV / Google TV panel registers its HDMI, AV and tuner sources with TvInputManager, so the menu is
 * built from that list at runtime instead of per-model IDs. Switching uses the standard passthrough channel URI, which
 * the firmware's own Live TV app handles; the TV's native source picker is offered as a fallback entry.
 */
class TvInputSource(private val context: Context) {
    private val pm = context.packageManager

    fun load(): List<InputSource> {
        val manager = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
        val raw = try {
            manager?.tvInputList.orEmpty().map(::toRaw)
        } catch (_: RuntimeException) {
            emptyList() // some firmware throws instead of returning an empty list on boxes without tuners
        }
        val sources = TvInputs.select(raw)
        return if (panelIntent() != null) sources + InputSource(TvInputs.PANEL_ID, "All sources", "TV menu", false) else sources
    }

    private fun toRaw(info: TvInputInfo): TvInputs.Raw {
        val custom = if (Build.VERSION.SDK_INT >= 24) info.loadCustomLabel(context) else null
        val label = (custom ?: info.loadLabel(context))?.toString()
        val app = info.serviceInfo?.applicationInfo
        return TvInputs.Raw(
            id = info.id,
            label = label?.takeUnless(::looksLikeClassName),
            type = info.type,
            parentId = info.parentId,
            hidden = Build.VERSION.SDK_INT >= 24 && info.isHidden(context),
            systemApp = app != null && app.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            passthrough = info.isPassthroughInput
        )
    }

    /** Some firmware leaves the service class name as the label; fall back to the port kind instead. */
    private fun looksLikeClassName(label: String) = label.contains("Service") || label.contains('.')

    /** Intent that switches to [input], or null if nothing on this TV can handle it. */
    fun intentFor(input: InputSource): Intent? {
        if (input.id == TvInputs.PANEL_ID) return panelIntent()
        val uri = Uri.parse(TvInputs.passthroughUri(input.id))
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, CHANNEL_MIME)
        // Prefer the firmware's own Live TV app so a third-party player never pops a chooser.
        @Suppress("DEPRECATION")
        val handlers = pm.queryIntentActivities(intent, 0)
        val liveTv = handlers.firstOrNull { it.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 }
            ?.activityInfo?.packageName
        // The passthrough URI is only specified for HDMI/AV-style inputs. Firmware Live TV apps (MediaTek's on the
        // tested Hisense) do accept it for their own tuner and open Live TV. Without a system Live TV app to hand
        // it to, don't gamble: open the TV's own source picker instead.
        if (input.isTuner && liveTv == null) return panelIntent()
        // If visibility rules hide the handler we still send the plain intent; startActivity doesn't need visibility.
        return if (liveTv != null) intent.setPackage(liveTv) else intent
    }

    /** The TV's native source picker (Android TV's `VIEW_INPUTS` action), if the firmware ships one. */
    private fun panelIntent(): Intent? {
        val intent = Intent(VIEW_INPUTS)
        @Suppress("DEPRECATION")
        return if (pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) intent else null
    }

    companion object {
        private const val CHANNEL_MIME = "vnd.android.cursor.item/channel"
        private const val VIEW_INPUTS = "com.android.tv.action.VIEW_INPUTS"
    }
}
