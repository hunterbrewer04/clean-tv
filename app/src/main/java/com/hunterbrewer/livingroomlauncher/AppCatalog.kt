package com.hunterbrewer.livingroomlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/** A launchable TV app. Intents are resolved once during catalog refresh. */
data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val launchIntent: Intent
)

class AppCatalog(private val context: Context) {
    private val pm = context.packageManager
    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    fun load(): MutableList<AppEntry> {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        @Suppress("DEPRECATION")
        val resolved = pm.queryIntentActivities(query, PackageManager.GET_META_DATA)
        val byPackage = linkedMapOf<String, AppEntry>()
        for (info in resolved) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName || pkg == "com.google.android.apps.tv.launcherx") continue
            val launch = pm.getLeanbackLaunchIntentForPackage(pkg)
                ?: pm.getLaunchIntentForPackage(pkg)
                ?: Intent(query).setClassName(pkg, info.activityInfo.name)
            if (!byPackage.containsKey(pkg)) {
                val label = if (pkg == "com.android.vending") "Get Apps" else info.loadLabel(pm).toString()
                byPackage[pkg] = AppEntry(pkg, label, info.loadIcon(pm), launch)
            }
        }

        // Some firmware hides Play Store from third-party launchers even though it has a TV activity.
        if (!byPackage.containsKey("com.android.vending")) addExplicit(byPackage, "com.android.vending", "Get Apps")

        val installed = byPackage.keys.toList()
        val saved = prefs.getString("app_order", null)?.split('|')?.filter { it.isNotBlank() }
        val order = if (saved == null) AppOrder.merge(installed, preferred) else AppOrder.merge(installed, saved)
        return order.mapNotNull { byPackage[it] }.toMutableList()
    }

    fun saveOrder(entries: List<AppEntry>) {
        prefs.edit().putString("app_order", entries.joinToString("|") { it.packageName }).apply()
    }

    private fun addExplicit(target: MutableMap<String, AppEntry>, packageName: String, fallback: String) {
        try {
            val info = pm.getApplicationInfo(packageName, 0)
            val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
                ?: pm.getLaunchIntentForPackage(packageName)
                ?: return
            target[packageName] = AppEntry(packageName, pm.getApplicationLabel(info).toString().ifBlank { fallback }, pm.getApplicationIcon(info), intent)
        } catch (_: PackageManager.NameNotFoundException) {
            // App is not installed; it should not leave a dead tile behind.
        }
    }

    companion object {
        private val preferred = listOf(
            "org.jellyfin.androidtv",
            "com.netflix.ninja",
            "com.disney.disneyplus",
            "com.amazon.amazonvideo.livingroom",
            "com.tubitv",
            "com.fubo.firetv.screen",
            "com.hulu.livingroomplus",
            "com.wbd.stream",
            "com.peacocktv.peacockandroid",
            "com.google.android.youtube.tv",
            "com.cbs.ott",
            "com.espn.score_center",
            "com.foxsports.android",
            "com.ncaa.mmlive.app",
            "com.apple.atve.androidtv.appletv",
            "com.plexapp.android",
            "com.spotify.tv.android",
            "com.tailscale.ipn",
            "com.android.vending"
        )
    }
}
