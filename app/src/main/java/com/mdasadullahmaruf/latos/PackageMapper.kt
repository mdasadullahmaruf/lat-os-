package com.mdasadullahmaruf.latos

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import java.util.Locale

class PackageMapper(private val context: Context) {

    private val packageManager = context.packageManager
    private val appMap = mutableMapOf<String, String>()       // normalized name -> package
    private val packageToNameMap = mutableMapOf<String, String>() // package -> normalized name
    private val aliases = mutableMapOf<String, String>()      // alias -> package

    /**
     * Full app scan. Uses intent queries (Play-Store-safe, no QUERY_ALL_PACKAGES needed)
     * as the primary source, then falls back to package enumeration.
     */
    fun scanApps() {
        appMap.clear()
        packageToNameMap.clear()
        aliases.clear()

        // Primary: all launcher activities — works without QUERY_ALL_PACKAGES
        scanLauncherApps()

        // Secondary: anything with ACTION_MAIN (catches edge-case OEM apps)
        scanMainApps()

        // Tertiary: brute-force scan if we can see the packages
        scanAllPackages()

        // Hardcoded aliases so "google", "youtube", etc. never map to the wrong app
        addHardcodedAliases()

        // Remove self-reference so we don't accidentally open our own app
        val self = context.packageName
        appMap.entries.find { it.value == self }?.let { appMap.remove(it.key) }
        packageToNameMap.remove(self)
    }

    private fun scanLauncherApps() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            } else {
                @Suppress("DEPRECATION")
                PackageManager.MATCH_ALL
            }
            val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(intent, flags)
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, flags)
            }
            for (resolveInfo in apps) {
                addResolveInfo(resolveInfo)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanMainApps() {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            } else {
                @Suppress("DEPRECATION")
                PackageManager.MATCH_ALL
            }
            val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(intent, flags)
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, flags)
            }
            for (resolveInfo in apps) {
                addResolveInfo(resolveInfo)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanAllPackages() {
        // Fallback: requires QUERY_ALL_PACKAGES on Android 11+ to see everything,
        // but system apps and apps we interact with will still show up.
        try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong())
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_ACTIVITIES
            }
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(flags)
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(flags)
            }
            for (packageInfo in packages) {
                val packageName = packageInfo.packageName
                if (packageName in packageToNameMap) continue

                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    try {
                        val label = packageManager.getApplicationLabel(packageInfo.applicationInfo)
                            .toString()
                            .lowercase(Locale.getDefault())
                            .trim()
                        if (label.isNotBlank()) {
                            appMap[label] = packageName
                            packageToNameMap[packageName] = label
                        }
                    } catch (e: Exception) {
                        // ignore unreadable labels
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addResolveInfo(resolveInfo: ResolveInfo) {
        try {
            val packageName = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(packageManager)
                .toString()
                .lowercase(Locale.getDefault())
                .trim()

            if (label.isBlank()) return

            // If we already have this package, keep the shorter label (usually the real app name,
            // not an activity-specific name like "YouTube Main Activity")
            val existingLabel = packageToNameMap[packageName]
            if (existingLabel == null || label.length < existingLabel.length) {
                existingLabel?.let { appMap.remove(it) }
                appMap[label] = packageName
                packageToNameMap[packageName] = label
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addHardcodedAliases() {
        val known = mapOf(
            "google" to listOf(
                "com.google.android.googlequicksearchbox",
                "com.android.chrome"
            ),
            "chrome" to listOf("com.android.chrome"),
            "youtube" to listOf("com.google.android.youtube"),
            "maps" to listOf("com.google.android.apps.maps"),
            "google maps" to listOf("com.google.android.apps.maps"),
            "messages" to listOf("com.google.android.apps.messaging"),
            "phone" to listOf("com.google.android.dialer"),
            "photos" to listOf("com.google.android.apps.photos"),
            "gallery" to listOf("com.vivo.gallery", "com.google.android.apps.photos"),
            "play store" to listOf("com.android.vending"),
            "google play" to listOf("com.android.vending"),
            "app store" to listOf("com.android.vending"),
            "browser" to listOf("com.vivo.browser", "com.android.chrome"),
            "calculator" to listOf(
                "com.vivo.calculator",
                "com.google.android.calculator",
                "com.android.calculator2"
            ),
            "camera" to listOf(
                "com.android.camera",
                "com.vivo.camera",
                "com.google.android.GoogleCamera"
            ),
            "clock" to listOf("com.android.deskclock", "com.google.android.deskclock"),
            "settings" to listOf("com.android.settings"),
            "whatsapp" to listOf("com.whatsapp"),
            "claude" to listOf("com.anthropic.claude"),
            "deepseek" to listOf("com.deepseek.chat"),
            "file manager" to listOf(
                "com.android.filemanager",
                "com.google.android.apps.nbu.files"
            ),
            "music" to listOf(
                "com.android.bbkmusic",
                "com.vivo.music",
                "com.google.android.music"
            ),
            "notes" to listOf("com.vivo.notes", "com.google.android.keep"),
            "recorder" to listOf("com.vivo.soundrecorder", "com.android.soundrecorder"),
            "contacts" to listOf("com.android.contacts", "com.google.android.contacts"),
            "gmail" to listOf("com.google.android.gm"),
            "drive" to listOf("com.google.android.apps.docs"),
            "calendar" to listOf("com.google.android.calendar", "com.android.calendar"),
            "weather" to listOf("com.vivo.weather", "com.google.android.apps.weather"),
            "compass" to listOf("com.vivo.compass"),
            "feedback" to listOf("com.vivo.feedback"),
            "easyshare" to listOf("com.vivo.easyshare"),
            "imanager" to listOf("com.vivo.imanager"),
            "lock" to listOf("com.android.bbk.lockscreen3"),
            "simple view" to listOf("com.vivo.simplelauncher"),
            "smart remote" to listOf("com.vivo.vhome"),
            "digital wellbeing" to listOf("com.google.android.apps.wellbeing")
        )

        for ((alias, candidates) in known) {
            for (pkg in candidates) {
                if (isPackageInstalled(pkg)) {
                    aliases[alias] = pkg
                    // Also ensure the canonical name is in the main map
                    try {
                        val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            packageManager.getApplicationInfo(
                                pkg,
                                PackageManager.ApplicationInfoFlags.of(0)
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            packageManager.getApplicationInfo(pkg, 0)
                        }
                        val label = packageManager.getApplicationLabel(appInfo)
                            .toString()
                            .lowercase(Locale.getDefault())
                            .trim()
                        if (label.isNotBlank()) {
                            appMap[label] = pkg
                            packageToNameMap[pkg] = label
                        }
                    } catch (e: Exception) {
                        appMap[alias] = pkg
                        packageToNameMap[pkg] = alias
                    }
                    break // use first available candidate
                }
            }
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Finds the best package for a query.
     * Priority: alias -> exact name -> prefix -> whole-word -> contains -> package name.
     */
    fun findApp(query: String): String? {
        val normalized = query.lowercase(Locale.getDefault()).trim()

        // 1. Exact alias
        aliases[normalized]?.let { return it }

        // 2. Exact scanned name
        appMap[normalized]?.let { return it }

        // 3. Raw package name
        if (isPackageInstalled(normalized)) return normalized

        // 4. Score-based search
        var bestPackage: String? = null
        var bestScore = 0

        for ((name, pkg) in appMap) {
            val score = calculateScore(normalized, name, pkg)
            if (score > bestScore) {
                bestScore = score
                bestPackage = pkg
            }
        }

        // 5. Score against aliases too
        for ((alias, pkg) in aliases) {
            val score = calculateScore(normalized, alias, pkg)
            if (score > bestScore) {
                bestScore = score
                bestPackage = pkg
            }
        }

        return if (bestScore >= 50) bestPackage else null
    }

    private fun calculateScore(query: String, candidateName: String, packageName: String): Int {
        // Exact match
        if (candidateName == query) return 10000

        // Starts with query (e.g. "youtube" vs "youtube music")
        if (candidateName.startsWith(query)) {
            return 600 + (100 / (candidateName.length - query.length + 1))
        }

        val candidateWords = candidateName.split(Regex("\\s+"))
        val queryWords = query.split(Regex("\\s+"))

        // Single word exact match
        for (word in candidateWords) {
            if (word == query) return 2000
        }

        var score = 0

        // Whole word match
        val wholeWord = Regex("""(^|\s)$query($|\s)""", RegexOption.IGNORE_CASE)
        if (wholeWord.containsMatchIn(candidateName)) {
            score += 500
        }

        // Contains query
        if (candidateName.contains(query)) {
            score += 150
            val idx = candidateName.indexOf(query)
            // Penalize if not a word boundary
            if (idx > 0 && candidateName[idx - 1] != ' ') score -= 40
            if (idx + query.length < candidateName.length && candidateName[idx + query.length] != ' ') {
                score -= 40
            }
        }

        // Package name match
        if (packageName.contains(query)) {
            score += 60
        }

        // Multi-word matching
        var matchedWords = 0
        for (qWord in queryWords) {
            if (qWord.length <= 2) continue
            for (cWord in candidateWords) {
                when {
                    cWord == qWord -> {
                        score += 200
                        matchedWords++
                    }
                    cWord.startsWith(qWord) -> {
                        score += 100
                        matchedWords++
                    }
                    cWord.contains(qWord) -> score += 50
                }
            }
        }

        // Bonus if all significant query words matched
        val significantWords = queryWords.count { it.length > 2 }
        if (matchedWords > 0 && matchedWords >= significantWords) {
            score += 300
        }

        // Penalize very short candidates that are just substring matches
        if (candidateName.length < query.length + 3 && candidateName != query) {
            score -= 60
        }

        return score
    }

    fun getAllApps(): Map<String, String> = appMap.toSortedMap()

    fun getAppCount(): Int = appMap.size

    fun getAppName(packageName: String): String? = packageToNameMap[packageName]

    fun getAliases(): Map<String, String> = aliases.toMap()
}
