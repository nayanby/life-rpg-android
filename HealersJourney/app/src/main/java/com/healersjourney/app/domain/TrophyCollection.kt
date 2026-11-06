package com.healersjourney.app.domain

/** Current-period collections derived from earned history, including empty intervals. */
object TrophyCollection {
    enum class Range { Week, Month, Year }
    data class Period(val start: String, val end: String, val entries: List<ActionEntry>, val coins: Int) {
        val xp: Int get() = entries.sumOf { it.xp }
        val activeDays: Int get() = entries.map { it.date }.distinct().size
        fun count(skill: String): Int = entries.count { it.skill == skill }
    }

    private fun monthEnd(start: String): String {
        val calendar = requireNotNull(Dates.keyToCal(start))
        calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        return Dates.dateKeyOf(calendar)
    }

    fun cards(state: GameState, range: Range, today: String = Dates.gameDayKey()): List<Period> {
        val intervals = when (range) {
            Range.Week -> {
                val monday = Dates.weekKeyOf(today)
                (0..6).map { Dates.addDays(monday, it).let { day -> day to day } }
            }
            Range.Month -> {
                val first = today.take(7) + "-01"
                val last = monthEnd(first)
                val weeks = mutableListOf<Pair<String, String>>()
                var start = first
                while (start <= last) {
                    val end = minOf(Dates.addDays(Dates.weekKeyOf(start), 6), last)
                    weeks.add(start to end)
                    start = Dates.addDays(end, 1)
                }
                weeks
            }
            Range.Year -> (1..12).map { month ->
                val first = today.take(4) + "-" + month.toString().padStart(2, '0') + "-01"
                first to monthEnd(first)
            }
        }
        val history = state.actionHistory.filter { DailyShowcase.collection(it.skill) != null &&
            it.type in setOf("activity", "study", "hobby", "workout", "other") }.distinctBy { it.id }
        return intervals.map { (start, end) ->
            val actions = history.filter { it.date in start..end }.sortedBy { it.date }
            val bonus = actions.map { it.date }.distinct().sumOf { day ->
                DailyCoinBonus.goals.filter { DailyCoinBonus.claimKey(day, it) in state.coinRewardDays }.sumOf { it.coins }
            }
            Period(start, end, actions, actions.count { it.id in state.coinAwardedActionIds } * 10 + bonus)
        }
    }
}
