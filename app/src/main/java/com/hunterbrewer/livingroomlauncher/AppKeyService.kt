package com.hunterbrewer.livingroomlauncher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Makes the remote's app buttons (Netflix, YouTube, ...) work without the stock home screen.
 *
 * On some firmware (TCL Google TV) those buttons are vendor keycodes that the system turns into a broadcast only
 * Google's stock launcher answers, so they go dead once it's disabled. This service only filters key events: it
 * reads no screen content, and any key it doesn't recognise passes straight through. Off until enabled
 * (Settings > Accessibility, or scripts/app-keys.sh).
 */
class AppKeyService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val target = AppKeys.targetFor(event.keyCode) ?: return false
        val intent = intentFor(target) ?: return false   // app not installed/disabled: let the system have the key
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            try {
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
                return false
            }
        }
        return true // consume both down and up so the dead stock broadcast never fires
    }

    private fun intentFor(target: String): Intent? = when (target) {
        AppKeys.SETTINGS -> Intent(Settings.ACTION_SETTINGS)
        // The TV's own source panel; if the firmware has none, leave the key to the system.
        AppKeys.INPUTS -> Intent("com.android.tv.action.VIEW_INPUTS")
            .takeIf { packageManager.resolveActivity(it, 0) != null }
        else -> packageManager.getLeanbackLaunchIntentForPackage(target) ?: packageManager.getLaunchIntentForPackage(target)
    }
}
