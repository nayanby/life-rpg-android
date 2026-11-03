package com.healersjourney.app.domain

/** One earned badge per real completion, derived from history so undo and backups stay exact. */
object DailyShowcase {
    data class Collection(val skill: String, val title: String)
    val collections = listOf(
        Collection("knowledgeXp", "Study"),
        Collection("medicineXp", "Work"),
        Collection("strengthXp", "Fitness"),
        Collection("socialXp", "Social"),
        Collection("hobbiesXp", "Hobbies")
    )
    fun collection(skill: String) = collections.firstOrNull { it.skill == skill }
    fun entries(state: GameState, day: String): List<ActionEntry> = state.actionHistory.asReversed().filter {
        it.date == day && collection(it.skill) != null && it.type in setOf("activity", "study", "hobby", "workout", "other")
    }.distinctBy { it.id }
}
