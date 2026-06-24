package com.mdasadullahmaruf.latos

class IntentEngine {

    data class Intent(
        val action: String,
        val target: String,
        val query: String
    )

    fun parseCommand(text: String): Intent? {
        val lower = text.lowercase().trim()

        return when {
            lower.contains("open") || lower.contains("launch") -> {
                val appName = extractAfterCommand(lower, listOf("open", "launch"))
                Intent("open_app", appName, "")
            }

            lower.contains("search") || lower.contains("find") -> {
                val (app, query) = extractSearchQuery(lower)
                Intent("search", app, query)
            }

            lower.contains("call") -> {
                val contact = extractAfterCommand(lower, listOf("call"))
                Intent("call", "phone", contact)
            }

            lower.contains("tap") || lower.contains("click") -> {
                val target = extractAfterCommand(lower, listOf("tap", "click", "press"))
                Intent("tap", "", target)
            }

            lower.contains("type") || lower.contains("write") -> {
                val text = extractAfterCommand(lower, listOf("type", "write", "enter"))
                Intent("type", "", text)
            }

            lower.contains("scroll down") || lower.contains("swipe up") -> {
                Intent("scroll", "", "down")
            }

            lower.contains("scroll up") || lower.contains("swipe down") -> {
                Intent("scroll", "", "up")
            }

            else -> null
        }
    }

    private fun extractAfterCommand(text: String, commands: List<String>): String {
        var result = text
        for (cmd in commands) {
            result = result.replace(cmd, "")
        }
        return result.trim()
    }

    private fun extractSearchQuery(text: String): Pair<String, String> {
        var app = ""
        var query = text

        if (text.contains("on youtube") || text.contains("youtube")) {
            app = "youtube"
            query = extractAfterCommand(text, listOf("search", "find", "on youtube", "youtube"))
        } else if (text.contains("on google") || text.contains("google")) {
            app = "google"
            query = extractAfterCommand(text, listOf("search", "find", "on google", "google"))
        } else {
            query = extractAfterCommand(text, listOf("search", "find"))
        }

        return Pair(app, query.trim())
    }
}
