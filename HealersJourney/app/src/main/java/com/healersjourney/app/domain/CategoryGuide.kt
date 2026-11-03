package com.healersjourney.app.domain

/** Plain-language category guidance shared by the Home graph descriptions. */
object CategoryGuide {
    data class Description(val summary: String, val examples: List<String>)

    fun forSkill(skill: String): Description? = when (skill) {
        "knowledgeXp" -> Description(
            "Grow your understanding and learn something new. Use Study for studying, reading and building knowledge.",
            listOf("Read a book chapter", "Review study notes", "Take a course", "Practice a new language", "Learn a new skill")
        )
        "medicineXp" -> Description(
            "Make progress in your job, career or business. Use Work for professional tasks and responsibilities.",
            listOf("Finish a work task", "Reply to work emails", "Prepare for a meeting", "Work on a business project", "Plan tomorrow's work")
        )
        "strengthXp" -> Description(
            "Move your body and build strength, stamina and flexibility. Small amounts of movement count too.",
            listOf("Go for a walk", "Complete a workout", "Stretch for ten minutes", "Do a yoga session", "Play a sport")
        )
        "socialXp" -> Description(
            "Care for your connections and everyday life at home. Social includes time with people, helping others, personal care and household responsibilities.",
            listOf("Call family or meet a friend", "Send a thoughtful message", "Buy groceries or run an errand", "Do laundry or clean your home", "Shower, trim nails or do your care routines")
        )
        "hobbiesXp" -> Description(
            "Make room for hobbies, imagination and things you enjoy creating. Use Hobbies for expression, practice and playful projects.",
            listOf("Draw, paint or write", "Practice a musical instrument", "Work on a craft", "Take photographs", "Play chess or solve a puzzle")
        )
        else -> null
    }
}
