package com.mdasadullahmaruf.latos

import android.content.Context
import android.content.Intent
import android.net.Uri

class DeepLinkRouter(private val context: Context) {

    fun openApp(appName: String): Boolean {
        val packageName = PackageMapper.findPackage(context, appName)

        if (packageName != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            }
        }

        return false
    }

    fun searchYouTube(query: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://results?search_query=${Uri.encode(query)}"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            searchGoogle("site:youtube.com $query")
        }
    }

    fun searchGoogle(query: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun callNumber(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
