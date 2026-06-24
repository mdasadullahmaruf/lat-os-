package com.mdasadullahmaruf.latos

import android.content.Context
import android.content.Intent
import android.net.Uri

class DeepLinkRouter(private val context: Context) {

    private val appPackages = mapOf(
        "youtube" to listOf("com.google.android.youtube", "com.google.android.apps.youtube.music"),
        "chrome" to listOf("com.android.chrome", "org.chromium.chrome.browser", "com.chrome.beta"),
        "maps" to listOf("com.google.android.apps.maps"),
        "phone" to listOf("com.android.dialer", "com.google.android.dialer", "com.android.contacts"),
        "contacts" to listOf("com.android.contacts", "com.google.android.contacts"),
        "messages" to listOf("com.android.messaging", "com.google.android.apps.messaging", "com.vivo.mms"),
        "settings" to listOf("com.android.settings", "com.vivo.settings"),
        "camera" to listOf("com.android.camera", "com.vivo.camera", "com.oppo.camera"),
        "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
        "gallery" to listOf("com.android.gallery3d", "com.vivo.gallery", "com.google.android.apps.photos"),
        "browser" to listOf("com.android.browser", "com.vivo.browser", "com.oppo.browser")
    )

    /**
     * Open app by name, tries multiple package names
     */
    fun openApp(appName: String): Boolean {
        val packages = appPackages[appName.lowercase()]
        
        if (packages != null) {
            for (pkg in packages) {
                val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                }
            }
        }
        
        // Fallback: try the input directly as package name
        val directIntent = context.packageManager.getLaunchIntentForPackage(appName)
        if (directIntent != null) {
            directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(directIntent)
            return true
        }
        
        return false
    }

    /**
     * Search YouTube - uses web fallback if app not found
     */
    fun searchYouTube(query: String) {
        // Try to open YouTube app with search
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://results?search_query=${Uri.encode(query)}"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback: open in browser
            searchGoogle("site:youtube.com $query")
        }
    }

    /**
     * Search Google
     */
    fun searchGoogle(query: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Call phone number
     */
    fun callNumber(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Open any app by package name directly
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
}
