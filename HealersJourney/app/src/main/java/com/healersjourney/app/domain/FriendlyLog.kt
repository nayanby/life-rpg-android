package com.healersjourney.app.domain

/** Human-facing wording for activity history. Stored log text stays intact for legacy undo. */
object FriendlyLog {
    fun care(label: String, key: String? = null): String = when (key?.lowercase() ?: label.lowercase()) {
        "shower" -> "Took a shower"
        "haircut" -> "Got a haircut"
        "shaving" -> "Shaved"
        "teeth" -> "Brushed teeth"
        "hair_wash" -> "Washed hair"
        "skincare" -> "Did skincare"
        "sunscreen" -> "Put on sunscreen"
        "floss" -> "Flossed"
        "manscaping", "grooming" -> "Groomed"
        "nails" -> "Trimmed nails"
        else -> "Completed ${label.trim()}"
    }

    fun action(action: ActionEntry): String = if (action.careKey != null) care(action.name, action.careKey) else when (action.type) {
        "care" -> care(action.name, action.careKey)
        "sleep" -> "Slept for ${GameEngine.numText(action.hours)} hours"
        "study" -> "Studied for ${GameEngine.numText(action.hours)} hours"
        "finance" -> "Invested ${Rules.money(action.monthlyInvestmentAmount)}"
        else -> "Completed ${Rules.activityLabel(action.skill, action.name.ifBlank { "an activity" })}"
    }

    fun entry(entry: LogEntry, state: GameState): String {
        entry.actionId?.let { id -> state.actionHistory.firstOrNull { it.id == id }?.let { return action(it) } }
        val parts = entry.text.split(" • ")
        val event = parts.drop(1).firstOrNull().orEmpty().trim().ifEmpty { entry.text.trim() }
        return when {
            event.equals("Personal care", true) -> {
                val detail = parts.getOrNull(2).orEmpty().substringBefore(" done on ").trim()
                care(detail.ifEmpty { "a care routine" })
            }
            event.startsWith("Monthly investment ", true) -> "Invested ${event.substringAfter(' ').substringAfter(' ')}"
            event.startsWith("Added activity ", true) -> "Saved ${event.removePrefix("Added activity ").substringBefore(" under ")}"
            event.startsWith("Removed activity ", true) -> "Removed ${event.removePrefix("Removed activity ").substringBefore(';')}"
            event.startsWith("Added custom achievement: ", true) -> "Added achievement ${event.substringAfter(": ")}"
            else -> "Completed $event"
        }
    }
}
