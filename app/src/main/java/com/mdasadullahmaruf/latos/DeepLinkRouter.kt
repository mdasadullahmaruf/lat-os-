package com.mdasadullahmaruf.latos

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * FIXED DeepLinkRouter - uses found package directly
 */
class DeepLinkRouter(private val context: Context) {

    /**
     * Open app by name - finds package then launches
     * FIXED: Returns package name for logging
     */
    fun openApp(appName: String): Pair<Boolean, String> {
        val packageName = PackageMapper.findPackage(context, appName)

        if (packageName != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return Pair(true, packageName)
            }
        }

        return Pair(false, packageName ?: "not found")
    }

    /**
     * Open app by exact package name (when already known)
     */
    fun openPackage(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } else {
            false
        }
    }

    /**
     * Search YouTube
     */
    fun searchYouTube(query: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://results?search_query=${Uri.encode(query)}"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            true
        } else {
            // Fallback: open YouTube app first, then search via accessibility
            val (success, _) = openApp("youtube")
            if (!success) {
                // Final fallback: browser
                searchGoogle("site:youtube.com $query")
            }
            success
        }
    }

    /**
     * Search Google
     */
    fun searchGoogle(query: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    /**
     * Call phone number
     */
    fun callNumber(number: String): Boolean {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }
}
