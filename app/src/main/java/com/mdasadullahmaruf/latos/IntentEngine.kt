package com.mdasadullahmaruf.latos

/**
 * Intent classifier - extracts command and app name
 * FIXED: More robust parsing, handles edge cases
 */
class IntentEngine {

    data class Intent(
        val action: String,
        val target: String,
        val query: String
    )

    fun parseCommand(text: String): Intent? {
        val lower = text.lowercase().trim()
        
        // Remove extra whitespace and punctuation
        val clean = lower.replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()
        
        return when {
            // Open / Launch commands
            clean.startsWith("open") || clean.startsWith("launch") || clean.startsWith("start") -> {
                val appName = clean.removePrefix("open").removePrefix("launch").removePrefix("start").trim()
                if (appName.isNotEmpty()) {
                    Intent("open_app", appName, "")
                } else null
            }

            // Search commands
            clean.startsWith("search") || clean.startsWith("find") || clean.startsWith("look up") || clean.startsWith("google") -> {
                val remaining = clean.removePrefix("search").removePrefix("find").removePrefix("look up").removePrefix("google").trim()
                
                // Check if search is for a specific app: "search youtube for cats" or "search cats on youtube"
                val app = extractAppFromSearch(remaining)
                val query = extractQueryFromSearch(remaining, app)
                
                Intent("search", app, query)
            }

            // Call commands
            clean.startsWith("call") || clean.startsWith("dial") || clean.startsWith("phone") -> {
                val contact = clean.removePrefix("call").removePrefix("dial").removePrefix("phone").trim()
                Intent("call", "phone", contact)
            }

            // Tap / Click commands
            clean.startsWith("tap") || clean.startsWith("click") || clean.startsWith("press") -> {
                val target = clean.removePrefix("tap").removePrefix("click").removePrefix("press").trim()
                Intent("tap", "", target)
            }

            // Type / Write commands
            clean.startsWith("type") || clean.startsWith("write") || clean.startsWith("enter") || clean.startsWith("input") -> {
                val text = clean.removePrefix("type").removePrefix("write").removePrefix("enter").removePrefix("input").trim()
                Intent("type", "", text)
            }

            // Scroll commands
            clean.contains("scroll down") || clean.contains("swipe up") -> {
                Intent("scroll", "", "down")
            }
            clean.contains("scroll up") || clean.contains("swipe down") -> {
                Intent("scroll", "", "up")
            }

            // Default: if it looks like just an app name, try to open it
            else -> {
                // If single word or short phrase, assume it's an app name
                if (clean.split(" ").size <= 3 && clean.length < 30) {
                    Intent("open_app", clean, "")
                } else {
                    // Otherwise treat as search
                    Intent("search", "", clean)
                }
            }
        }
    }

    private fun extractAppFromSearch(text: String): String {
        // Patterns: "search cats on youtube", "search youtube for cats", "youtube search cats"
        val patterns = listOf(
            Regex("on (\\w+)$"),
            Regex("for (\\w+)$"),
            Regex("^(\\w+) search"),
            Regex("^(\\w+) for")
        )
        
        for (pattern in patterns) {
            pattern.find(text)?.groupValues?.get(1)?.let { return it }
        }
        
        // Check for known app names in the text
        val knownApps = listOf("youtube", "google", "maps", "chrome", "browser", "gmail", "spotify", "netflix", "amazon", "flipkart")
        for (app in knownApps) {
            if (text.contains(app)) return app
        }
        
        return ""
    }

    private fun extractQueryFromSearch(text: String, app: String): String {
        var query = text
        if (app.isNotEmpty()) {
            query = query.replace("on $app", "")
                .replace("for $app", "")
                .replace("$app search", "")
                .replace("$app for", "")
                .replace(app, "")
                .trim()
        }
        return query
    }
}
