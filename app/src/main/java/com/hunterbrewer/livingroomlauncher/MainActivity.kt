package com.hunterbrewer.livingroomlauncher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast

class MainActivity : Activity(), LauncherView.Actions {
    private lateinit var catalog: AppCatalog
    private lateinit var launcherView: LauncherView

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refreshApps()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.BLACK
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )

        catalog = AppCatalog(this)
        launcherView = LauncherView(this, this)
        setContentView(launcherView)
        refreshApps()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(packageReceiver, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(packageReceiver, filter)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::launcherView.isInitialized) {
            refreshApps()
            launcherView.requestFocus()
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(packageReceiver) } catch (_: IllegalArgumentException) {}
        super.onDestroy()
    }

    private fun refreshApps() {
        if (::launcherView.isInitialized) launcherView.setApps(catalog.load())
    }

    override fun launch(entry: AppEntry) {
        try {
            startActivity(Intent(entry.launchIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (error: Exception) {
            Toast.makeText(this, "Could not open ${entry.label}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun saveOrder(entries: List<AppEntry>) = catalog.saveOrder(entries)

    override fun openWifi() = startSafely(Intent(Settings.ACTION_WIFI_SETTINGS), "Wi-Fi settings")

    override fun openSettings() = startSafely(Intent(Settings.ACTION_SETTINGS), "TV settings")

    override fun switchInput(input: LauncherView.TvInput) {
        val uri = Uri.parse(TvInputs.passthroughUri(input.id))
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.mediatek.wwtv.tvcenter")
            setDataAndType(uri, "vnd.android.cursor.item/channel")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startSafely(intent, input.label)
    }

    override fun openJellyfin() {
        val intent = packageManager.getLeanbackLaunchIntentForPackage("org.jellyfin.androidtv")
            ?: packageManager.getLaunchIntentForPackage("org.jellyfin.androidtv")
        if (intent != null) startSafely(intent, "Jellyfin")
    }

    private fun startSafely(intent: Intent, label: String) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            Toast.makeText(this, "$label is not available", Toast.LENGTH_SHORT).show()
        }
    }
}
