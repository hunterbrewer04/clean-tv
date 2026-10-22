package com.hunterbrewer.livingroomlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable

/** A launchable TV app. Intents and card art are resolved once during catalog refresh. */
data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val launchIntent: Intent,
    val card: Bitmap,
    val accent: Int,
    val hasBanner: Boolean,
    /** True only for apps the user installed (system/preloaded apps can't be uninstalled for the user). */
    val removable: Boolean
)

class AppCatalog(private val context: Context) {
    private val pm = context.packageManager
    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    /** Card art cache keyed by package + install timestamp, so onResume refreshes are cheap. */
    private class Art(val stamp: Long, val card: Bitmap, val accent: Int, val hasBanner: Boolean)
    private val artCache = HashMap<String, Art>()

    private fun art(pkg: String, activity: ActivityInfo?, icon: Drawable): Art {
        val stamp = try { pm.getPackageInfo(pkg, 0).lastUpdateTime } catch (_: PackageManager.NameNotFoundException) { 0L }
        artCache[pkg]?.takeIf { it.stamp == stamp }?.let { return it }
        val banner = try { activity?.loadBanner(pm) ?: pm.getApplicationBanner(pkg) } catch (_: Exception) { null }
        val (card, accent) = CardArt.render(banner, icon)
        return Art(stamp, card, accent, banner != null).also { artCache[pkg] = it }
    }

    fun load(): MutableList<AppEntry> {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        @Suppress("DEPRECATION")
        val resolved = pm.queryIntentActivities(query, PackageManager.GET_META_DATA)
        val byPackage = linkedMapOf<String, AppEntry>()
        for (info in resolved) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName || pkg in stockLaunchers) continue
            val launch = pm.getLeanbackLaunchIntentForPackage(pkg)
                ?: pm.getLaunchIntentForPackage(pkg)
                ?: Intent(query).setClassName(pkg, info.activityInfo.name)
            if (!byPackage.containsKey(pkg)) {
                val label = if (pkg == "com.android.vending") "Get Apps" else info.loadLabel(pm).toString()
                val icon = info.loadIcon(pm)
                val a = art(pkg, info.activityInfo, icon)
                val removable = info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
                byPackage[pkg] = AppEntry(pkg, label, icon, launch, a.card, a.accent, a.hasBanner, removable)
            }
        }

        // Some firmware hides Play Store from third-party launchers even though it has a TV activity.
        if (!byPackage.containsKey("com.android.vending")) addExplicit(byPackage, "com.android.vending", "Get Apps")

        val installed = byPackage.keys.toList()
        val saved = prefs.getString("app_order", null)?.split('|')?.filter { it.isNotBlank() }
        val order = if (saved == null) AppOrder.merge(installed, preferred) else AppOrder.merge(installed, saved)
        artCache.keys.retainAll(byPackage.keys)
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
            val icon = pm.getApplicationIcon(info)
            val a = art(packageName, null, icon)
            target[packageName] = AppEntry(packageName, pm.getApplicationLabel(info).toString().ifBlank { fallback }, icon, intent, a.card, a.accent, a.hasBanner, info.flags and ApplicationInfo.FLAG_SYSTEM == 0)
        } catch (_: PackageManager.NameNotFoundException) {
            // App is not installed; it should not leave a dead tile behind.
        }
    }

    companion object {
        /** Stock home screens that also register a Leanback tile; launching one from here is just confusing. */
        private val stockLaunchers = setOf(
            "com.google.android.apps.tv.launcherx",   // Google TV
            "com.google.android.tvlauncher",          // Android TV 9+
            "com.google.android.leanbacklauncher"     // older Android TV
        )

        /** Default first-run order for common streaming apps. Anything else is appended; users can rearrange. */
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
            "com.apple.atve.androidtv.appletv",
            "com.plexapp.android",
            "com.spotify.tv.android",
            "com.android.vending"
        )
    }
}
