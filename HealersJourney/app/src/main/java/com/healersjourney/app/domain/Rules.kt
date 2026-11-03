package com.healersjourney.app.domain

object Rules {

    const val XP_PER_LEVEL = 500

    /** Presentation aliases keep saved skill IDs and activity history compatible. */
    fun activityLabel(skill: String, name: String): String = when (skill to name) {
        "knowledgeXp" to "MCQ practice" -> "Read a book chapter"
        "knowledgeXp" to "Revision" -> "Review today's notes"
        "knowledgeXp" to "Make notes" -> "Write study notes"
        "medicineXp" to "Case learned" -> "Finish a work task"
        "medicineXp" to "Ward round" -> "Plan tomorrow's work"
        "medicineXp" to "Read patient diagnosis" -> "Reply to work emails"
        "medicineXp" to "Procedure learning" -> "Learn a work skill"
        "strengthXp" to "Workout" -> "Complete a workout"
        "strengthXp" to "Walk 30 minutes" -> "Walk for 30 minutes"
        "strengthXp" to "Stretching" -> "Stretch for ten minutes"
        "strengthXp" to "Yoga" -> "Do a yoga session"
        "socialXp" to "Call family" -> "Call a family member"
        "socialXp" to "Meet friend" -> "Meet up with a friend"
        "socialXp" to "Celebrate birthday" -> "Celebrate someone's birthday"
        "socialXp" to "Help someone" -> "Help a friend"
        "socialXp" to "Important message" -> "Send a thoughtful message"
        "hobbiesXp" to "Music practice" -> "Practice a musical instrument"
        "hobbiesXp" to "Chess practice" -> "Play a chess game"
        "hobbiesXp" to "Puzzle training" -> "Solve a puzzle"
        "hobbiesXp" to "Hobby session" -> "Work on a hobby"
        else -> name
    }

    fun categoryLabel(label: String): String = when (label) {
        "Medicine" -> "Work"
        "Knowledge", "Learning", "Study" -> "Study"
        "Strength" -> "Fitness"
        "Creativity", "Hobbies" -> "Hobbies"
        else -> label
    }

    val SKILL_KEYS = listOf("knowledgeXp", "medicineXp", "strengthXp", "socialXp", "hobbiesXp")

    val SKILL_LABELS = mapOf(
        "knowledgeXp" to "Study",
        "medicineXp" to "Work",
        "strengthXp" to "Fitness",
        "socialXp" to "Social",
        "hobbiesXp" to "Hobbies"
    )

    val SKILL_COLORS = mapOf(
        "knowledgeXp" to "#4f6df5",
        "medicineXp" to "#e05656",
        "strengthXp" to "#2fa36b",
        "socialXp" to "#e08b3a",
        "hobbiesXp" to "#8b5cf6"
    )

    val WEEKLY_TARGETS = mapOf(
        "knowledgeXp" to 2100,
        "medicineXp" to 350,
        "strengthXp" to 280,
        "socialXp" to 175,
        "hobbiesXp" to 175
    )

    /** Ascending unlock level for every title; [title] and the title shelf both read this. */
    val TITLE_LEVELS = listOf(
        1 to "Initiate",
        5 to "Disciplined Seeker",
        10 to "Focused Builder",
        20 to "Momentum Keeper",
        30 to "Master of Routine",
        50 to "Iron Discipline",
        75 to "Relentless Achiever",
        100 to "Legend of Consistency"
    )

    fun level(totalXp: Int): Int = totalXp / XP_PER_LEVEL + 1

    fun title(level: Int): String = TITLE_LEVELS.lastOrNull { level >= it.first }?.second ?: "Initiate"

    fun decayPercentForMissedDays(days: Int): Int = when {
        days >= 15 -> 80
        days >= 9 -> 40
        days >= 6 -> 20
        days >= 3 -> 10
        else -> 0
    }

    fun xpForHours(hours: Double): Int = hours.toInt() * 50

    fun formatMetricValue(value: Double, suffix: String = ""): String {
        val isWhole = value == Math.floor(value) && !value.isInfinite()
        val text = if (isWhole) value.toInt().toString() else "%.1f".format(value)
        return "$text$suffix"
    }

    fun money(value: Double): String {
        val negative = value < 0
        var n = Math.abs(value).toLong()
        if (n < 1000) return (if (negative) "-" else "") + "₹$n"
        val last3 = (n % 1000).toString().padStart(3, '0')
        n /= 1000
        val parts = mutableListOf<String>()
        while (n > 0) {
            parts.add(0, (n % 100).toString().padStart(2, '0'))
            n /= 100
        }
        return (if (negative) "-" else "") + "₹" + parts.joinToString(",") + "," + last3
    }

    // ---- personal care ----

    data class CareRule(
        val key: String,
        val label: String,
        val emoji: String,
        val cycleDays: Int,
        val weight: Int
    )

    val CARE_RULES = listOf(
        CareRule("shower", "Shower", "🚿", 2, 1),
        CareRule("nails", "Nails", "✂️", 7, 1)
    )

    val CARE_SUGGESTIONS = listOf(
        CareRule("teeth", "Brush teeth", "🪥", 1, 3),
        CareRule("hair_wash", "Wash hair", "🧴", 3, 2),
        CareRule("haircut", "Haircut", "💈", 30, 3),
        CareRule("manscaping", "Grooming", "🧴", 14, 2),
        CareRule("shaving", "Shave", "🪒", 7, 2),
        CareRule("skincare", "Skincare", "🧴", 1, 2),
        CareRule("sunscreen", "Sunscreen", "☀️", 1, 2),
        CareRule("floss", "Floss", "🦷", 1, 2)
    )

    fun careAvatarKey(score: Int): String = when {
        score > 50 -> "pristine"
        score > 40 -> "grimy"
        score > 20 -> "dirty"
        else -> "filthy"
    }

    fun careRank(score: Int): String = when {
        score > 50 -> "Pristine"
        score > 40 -> "Grimy"
        score > 20 -> "Dirty"
        else -> "Filthy"
    }

    fun careItemLevel(missedUnits: Int): Int = when {
        missedUnits <= 0 -> 4
        missedUnits == 1 -> 3
        missedUnits == 2 -> 2
        missedUnits == 3 -> 1
        else -> 0
    }

    // ---- attribute ranks ----

    val ATTRIBUTE_KEYS = listOf("knowledge", "medicine", "strength", "social", "hobbies")

    val ATTRIBUTE_LABELS = mapOf(
        "knowledge" to "Study",
        "medicine" to "Work",
        "strength" to "Fitness",
        "social" to "Social",
        "hobbies" to "Hobbies"
    )

    val ATTRIBUTE_COLORS = mapOf(
        "knowledge" to "#4f6df5",
        "medicine" to "#e05656",
        "strength" to "#2fa36b",
        "social" to "#e08b3a",
        "hobbies" to "#8b5cf6"
    )

    fun attributeRank(percent: Int): String = when {
        percent >= 90 -> "Excellent"
        percent >= 75 -> "Strong"
        percent >= 60 -> "Good"
        percent >= 40 -> "Building"
        percent >= 20 -> "Low"
        else -> "Very Low"
    }

    fun attributeColorPercent(percent: Int): Long = when {
        percent >= 90 -> 0xFF1A9850
        percent >= 75 -> 0xFF91CF60
        percent >= 60 -> 0xFFD9EF8B
        percent >= 40 -> 0xFFFEE08B
        percent >= 20 -> 0xFFFC8D59
        else -> 0xFFD73027
    }
}
