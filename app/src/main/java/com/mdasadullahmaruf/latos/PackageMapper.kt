package com.mdasadullahmaruf.latos

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log

object PackageMapper {

    private val TAG = "LatOS_PackageMapper"

    private var cachedApps: List<Pair<String, String>>? = null
    private var cacheTimestamp: Long = 0
    private val CACHE_TTL = 5 * 60 * 1000

    // FIXED: Better known apps map with more accurate entries
    private val knownApps = mapOf(
        // Google Apps - CORRECTED
        "google" to "com.google.android.googlequicksearchbox",
        "google search" to "com.google.android.googlequicksearchbox",
        "google app" to "com.google.android.googlequicksearchbox",
        "youtube" to "com.google.android.youtube",
        "youtube music" to "com.google.android.apps.youtube.music",
        "gmail" to "com.google.android.gm",
        "google maps" to "com.google.android.apps.maps",
        "maps" to "com.google.android.apps.maps",
        "google drive" to "com.google.android.apps.docs",
        "drive" to "com.google.android.apps.docs",
        "google photos" to "com.google.android.apps.photos",
        "photos" to "com.google.android.apps.photos",
        "google meet" to "com.google.android.apps.meetings",
        "meet" to "com.google.android.apps.meetings",
        "google calendar" to "com.google.android.calendar",
        "calendar" to "com.google.android.calendar",
        "google translate" to "com.google.android.apps.translate",
        "translate" to "com.google.android.apps.translate",
        "play store" to "com.android.vending",
        "google play" to "com.android.vending",
        "app store" to "com.android.vending",
        "gemini" to "com.google.android.apps.bard",
        "google gemini" to "com.google.android.apps.bard",
        "chrome" to "com.android.chrome",
        "google chrome" to "com.android.chrome",
        "google assistant" to "com.google.android.googlequicksearchbox",

        // Social & Messaging
        "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "messenger" to "com.facebook.orca",
        "facebook messenger" to "com.facebook.orca",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "snapchat" to "com.snapchat.android",
        "tiktok" to "com.zhiliaoapp.musically",
        "linkedin"
