package com.mdasadullahmaruf.latos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

class DeepLinkRouter(private val context: Context) {

    private val TAG = "LatOS_DeepLink"

    /**
     * Open app by name - finds package then launches
     * FIXED: Added try-catch, returns detailed error info
     */
    fun openApp(appName: String): Pair<Boolean, String> {
        return try {
            val packageName = PackageMapper.findPackage(context, appName)

            if (packageName != null) {
                Log.d(TAG, "Found package for '$appName': $packageName")
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Log.d(TAG, "Launched $packageName successfully")
                    Pair(true, packageName)
                } else {
                    Log.w(TAG, "No launch intent for $packageName")
                    Pair(false, "$packageName (no launch intent)")
                }
            } else {
                Log.w(TAG, "Package not found for '$appName'")
                Pair(false, "not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app '$appName'", e)
            Pair(false, "error: ${e.message}")
        }
    }

    /**
     * Open app by exact package name
     */
    fun openPackage(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening package $packageName", e)
            false
        }
    }

    /**
     * Search YouTube
     */
    fun searchYouTube(query: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://results?search_query=${Uri.encode(query)}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                // Fallback: open YouTube app
                val (success, _) = openApp("youtube")
                if (!success) {
                    searchGoogle("site:youtube.com $query")
                }
                success
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching YouTube", e)
            false
        }
    }

    /**
     * Search Google
     */
    fun searchGoogle(query: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error searching Google", e)
            false
        }
    }

    /**
     * Call phone number
     */
    fun callNumber(number: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error dialing $number", e)
            false
        }
    }
}
