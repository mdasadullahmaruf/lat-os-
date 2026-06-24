package com.mdasadullahmaruf.latos

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object PackageMapper {

    private val knownApps = mapOf(
        // Google Apps
        "google" to "com.google.android.googlequicksearchbox",
        "youtube" to "com.google.android.youtube",
        "gmail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "drive" to "com.google.android.apps.docs",
        "photos" to "com.google.android.apps.photos",
        "play store" to "com.android.vending",
        "chrome" to "com.android.chrome",
        "google chrome" to "com.android.chrome",
        "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "twitter" to "com.twitter.android",
        "tiktok" to "com.zhiliaoapp.musically",
        "telegram" to "org.telegram.messenger",
        "phone" to "com.android.dialer",
        "contacts" to "com.android.contacts",
        "messages" to "com.android.messaging",
        "gallery" to "com.android.gallery3d",
        "browser" to "com.android.browser",
        "calculator" to "com.android.calculator2",
        "camera" to "com.android.camera",
        "clock" to "com.android.deskclock",
        "file manager" to "com.android.filemanager",
        "music" to "com.android.bbkmusic",
        "settings" to "com.android.settings",
        "netflix" to "com.netflix.mediaclient",
        "spotify" to "com.spotify.music",
        "zoom" to "us.zoom.videomeetings",
        "teams" to "com.microsoft.teams",
        "chatgpt" to "com.openai.chatgpt",
        "flipkart" to "com.flipkart.android",
        "gpay" to "com.google.android.apps.nbu.paisa.user",
        "paytm" to "net.one97.paytm",

        // Vivo / OriginOS specific
        "vivo gallery" to "com.vivo.gallery",
        "vivo browser" to "com.vivo.browser",
        "vivo camera" to "com.android.camera",
        "vivo settings" to "com.android.settings",
        "vivo phone" to "com.android.dialer",
        "vivo messages" to "com.android.messaging",
        "vivo music" to "com.android.bbkmusic",
        "vivo file manager" to "com.android.filemanager",
        "vivo clock" to "com.android.deskclock",
        "vivo weather" to "com.vivo.weather",
        "vivo calculator" to "com.android.calculator2",
        "vivo calendar" to "com.android.calendar",
        "vivo video" to "com.android.bbkvideoplayer",
        "vivo themes" to "com.bbk.theme",
        "vivo app store" to "com.vivo.appstore",
        "vivo store" to "com.vivo.website",
        "vivo imanager" to "com.vivo.imanager",
        "vivo tips" to "com.vivo.Tips",
        "vivo easyshare" to "com.vivo.easyshare",
        "vivo compass" to "com.vivo.compass",
        "vivo notes" to "com.vivo.notes",
        "vivo recorder" to "com.vivo.soundrecorder",
        "vivo smart remote" to "com.vivo.vhome",

        // Common alternative package names
        "browser" to "com.android.browser",
        "browser" to "com.vivo.browser",
        "gallery" to "com.vivo.gallery",
        "gallery" to "com.android.gallery3d",
        "chrome" to "com.chrome.beta",
        "chrome" to "com.google.android.apps.chrome",
        "youtube" to "com.google.android.youtube.tv",
        "youtube" to "com.google.android.apps.youtube.music"
    )

    fun findPackage(context: Context, query: String): String? {
        val q = query.lowercase().trim()
            .replace(Regex("[^a-z0-9 ]"), "")

        // 1. Exact match in known map
        knownApps[q]?.let { pkg ->
            if (isInstalled(context, pkg)) return pkg
        }

        // 2. Partial match in known map - check ALL matching entries
        val matchingEntries = knownApps.filter { (name, _) ->
            q == name || q.contains(name) || name.contains(q)
        }
        for ((_, pkg) in matchingEntries) {
            if (isInstalled(context, pkg)) return pkg
        }

        // 3. Scan ALL installed apps on device
        val allApps = getAllPackages(context)

        // Exact label match
        for ((label, pkg) in allApps) {
            if (label == q) return pkg
        }

        // Label contains query
        for ((label, pkg) in allApps) {
            if (label.contains(q)) return pkg
        }

        // Query contains label
        for ((label, pkg) in allApps) {
            if (q.contains(label)) return pkg
        }

        // Package name contains query
        for ((_, pkg) in allApps) {
            val pkgLower = pkg.lowercase()
            val queryNoSpace = q.replace(" ", "")
            if (pkgLower.contains(queryNoSpace)) return pkg
            if (pkgLower.substringAfterLast(".").contains(queryNoSpace)) return pkg
        }

        // 4. Fuzzy Levenshtein
        var bestPkg: String? = null
        var bestDist = Int.MAX_VALUE
        for ((label, pkg) in allApps) {
            val dist = levenshtein(q, label)
            if (dist < bestDist && dist <= 3) {
                bestDist = dist
                bestPkg = pkg
            }
        }
        return bestPkg
    }

    // Get list of all installed apps for debugging
    fun getInstalledAppsList(context: Context): List<String> {
        val allApps = getAllPackages(context)
        return allApps.map { "${it.first} -> ${it.second}" }.sorted()
    }

    private fun getAllPackages(context: Context): List<Pair<String, String>> {
        val pm = context.packageManager
        val results = mutableListOf<Pair<String, String>>()

        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            pm.queryIntentActivities(intent, 0).forEach { info ->
                try {
                    val label = info.loadLabel(pm).toString()
                        .lowercase().trim()
                    val pkg = info.activityInfo.packageName
                    if (results.none { it.second == pkg }) {
                        results.add(Pair(label, pkg))
                    }
                } catch (e: Exception) { }
            }
        } catch (e: Exception) { }

        return results.distinctBy { it.second }
    }

    fun isInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        } catch (e: Exception) {
            false
        }
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[a.length][b.length]
    }
}
