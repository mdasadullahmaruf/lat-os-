package com.mdasadullahmaruf.latos

/**
 * Local LLM-based intent classifier
 * 
 * For now: rule-based fallback
 * TODO: Integrate llama.cpp with Gemma 2B GGUF model
 */
class IntentEngine {

    data class Intent(
        val action: String,      // "open_app", "search", "tap", "type", "call", "scroll"
        val target: String,      // "youtube", "contacts", etc.
        val query: String,       // "FIFA World Cup", "Mom", etc.
        val coordinates: Pair<Float, Float>? = null
    )

    fun parseCommand(text: String): Intent? {
        val lower = text.lowercase()

        return when {
            // Open app patterns
            lower.contains("open") || lower.contains("launch") -> {
                val app = extractAppName(lower)
                Intent("open_app", app, "")
            }

            // Search patterns
            lower.contains("search") || lower.contains("find") -> {
                val (app, query) = extractSearchQuery(lower)
                Intent("search", app, query)
            }

            // Tap/click patterns
            lower.contains("tap") || lower.contains("click") || lower.contains("press") -> {
                val target = extractTarget(lower)
                Intent("tap", "", target)
            }

            // Type patterns
            lower.contains("type") || lower.contains("write") || lower.contains("enter") -> {
                val text = extractTypeText(lower)
                Intent("type", "", text)
            }

            // Call patterns
            lower.contains("call") -> {
                val contact = extractContact(lower)
                Intent("call", "phone", contact)
            }

            // Scroll patterns
            lower.contains("scroll down") || lower.contains("swipe up") -> {
                Intent("scroll", "", "down")
            }

            lower.contains("scroll up") || lower.contains("swipe down") -> {
                Intent("scroll", "", "up")
            }

            // Play/pause media
            lower.contains("play") || lower.contains("pause") -> {
                Intent("media", "", lower)
            }

            else -> null
        }
    }

    private fun extractAppName(text: String): String {
        val apps = mapOf(
            "youtube" to "com.google.android.youtube",
            "maps" to "com.google.android.apps.maps",
            "chrome" to "com.android.chrome",
            "whatsapp" to "com.whatsapp",
            "phone" to "com.android.dialer",
            "contacts" to "com.android.contacts",
            "messages" to "com.android.messaging",
            "settings" to "com.android.settings",
            "camera" to "com.android.camera"
        )
        
        for ((name, packageName) in apps) {
            if (text.contains(name)) return packageName
        }
        return ""
    }

    private fun extractSearchQuery(text: String): Pair<String, String> {
        var app = "com.google.android.youtube"
        var query = text
        
        if (text.contains("on youtube")) {
            app = "com.google.android.youtube"
            query = text.replace("search on youtube", "").replace("search youtube", "").trim()
        } else if (text.contains("on google")) {
            app = "com.android.chrome"
            query = text.replace("search on google", "").replace("search google", "").trim()
        } else {
            query = text.replace("search", "").replace("find", "").trim()
        }
        
        return Pair(app, query)
    }

    private fun extractTarget(text: String): String {
        return text.replace("tap", "").replace("click", "").replace("press", "").trim()
    }

    private fun extractTypeText(text: String): String {
        return text.replace("type", "").replace("write", "").replace("enter", "").trim()
    }

    private fun extractContact(text: String): String {
        return text.replace("call", "").trim()
    }
}
