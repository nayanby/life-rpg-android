package com.healersjourney.app.domain

/** Collection periods use earned history, so undo and imported saves stay consistent. */
object TrophyCollection {
    data class Period(val start: String, val end: String, val entries: List<ActionEntry>, val coins: Int) {
        val xp: Int get() = entries.sumOf { it.xp }
        val activeDays: Int get() = entries.map { it.date }.distinct().size
        fun count(skill: String): Int = entries.count { it.skill == skill }
    }

    fun periods(state: GameState, monthly: Boolean): List<Period> = state.actionHistory
        .filter { DailyShowcase.collection(it.skill) != null && it.type in setOf("activity", "study", "hobby", "workout", "other") }
        .distinctBy { it.id }
        .groupBy { if (monthly) it.date.take(7) + "-01" else Dates.weekKeyOf(it.date) }
        .toSortedMap(reverseOrder())
        .map { (start, actions) ->
            val end = if (monthly) {
                val calendar = Dates.keyToCal(start)!!
                calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
                Dates.dateKeyOf(calendar)
            } else Dates.addDays(start, 6)
            val bonus = actions.map { it.date }.distinct().sumOf { day ->
                DailyCoinBonus.goals.filter { DailyCoinBonus.claimKey(day, it) in state.coinRewardDays }.sumOf { it.coins }
            }
            Period(start, end, actions.sortedByDescending { it.date },
                actions.count { it.id in state.coinAwardedActionIds } * 10 + bonus)
        }
}
