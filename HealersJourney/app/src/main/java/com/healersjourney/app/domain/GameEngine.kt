package com.healersjourney.app.domain

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/** Outcome of a user action: it applied, or the UI should surface [Failure.message]. */
sealed interface Outcome {
    data object Applied : Outcome
    data class Failure(val message: String) : Outcome
}

data class CareItemStatus(
    val key: String,
    val label: String,
    val emoji: String,
    val cycleDays: Int,
    val weight: Int,
    val missedUnits: Int,
    val loss: Int,
    val days: Int,
    val last: String,
    val level: Int
)

data class CareStats(val score: Int, val rank: String, val items: List<CareItemStatus>)

data class TodoView(val key: String, val list: MutableList<TodoTask>, val viewingToday: Boolean)

data class UndoOutcome(val removed: Boolean, val message: String?)

object Ids {
    fun make(prefix: String): String =
        prefix + "_" + System.currentTimeMillis() + "_" + Random.nextInt(1_000_000)
}

/**
 * Every mutation of [GameState], ported one-for-one from the web app so XP, decay, streaks,
 * logs and undo behave identically. Functions are pure with respect to Android: the caller
 * persists the state afterwards.
 */
object GameEngine {

    private const val MAX_HISTORY = 4000
    private const val MAX_LOG = 100
    private const val MAX_DATES = 180
    private const val MAX_WEEKLY_BUCKETS = 12
    const val STUDY_CYCLE_CAP = 8.0
    const val CARE_SCORE_MAX = 60
    const val CARE_ACTIVITY_XP = 40

    private val WORKOUT_NAME = Regex("workout|exercise|fitness")
    private val LEGACY_MILESTONE_ID = Regex("^m([1-9]|1[0-2])$")
    private val CARE_DATE_ISO = Regex("done on\\s+(\\d{4}-\\d{2}-\\d{2})", RegexOption.IGNORE_CASE)
    private val CARE_DATE_PRETTY =
        Regex("done on\\s+(\\d{1,2})\\s+([A-Za-z]{3,})\\s+(\\d{2,4})", RegexOption.IGNORE_CASE)

    private val MONTH_INDEX = mapOf(
        "jan" to 0, "january" to 0, "feb" to 1, "february" to 1, "mar" to 2, "march" to 2,
        "apr" to 3, "april" to 3, "may" to 4, "jun" to 5, "june" to 5, "jul" to 6, "july" to 6,
        "aug" to 7, "august" to 7, "sep" to 8, "sept" to 8, "september" to 8, "oct" to 9,
        "october" to 9, "nov" to 10, "november" to 10, "dec" to 11, "december" to 11
    )

    fun numText(value: Double): String =
        if (value == floor(value) && !value.isInfinite()) value.toInt().toString() else value.toString()

    // ---- bootstrap ----

    fun ensureDefaults(state: GameState) {
        Dates.resetHour = state.dayResetHour
        if (state.decayStartDate.isEmpty()) state.decayStartDate = Dates.todayKey()
        ensureHobbies(state)
        ensureActivityDefs(state)
        ensureMilestones(state)
        // Older repeating tasks become editable saved activities rather than an orphaned list.
        state.recurringTasks.forEach { rule ->
            val lists = state.activityDefs.getOrPut(rule.skill) { ActivityList() }
            if ((lists.major + lists.other).none { Rules.activityLabel(rule.skill, it.name).equals(rule.name, true) })
                lists.other.add(ActivityDef(rule.id, rule.name, rule.xp))
        }
        ensurePersonalCare(state)
        ensureTodoTasks(state)
        syncDailyCoinBonuses(state, Dates.gameDayKey())
    }

    private fun ensureHobbies(state: GameState) {
        Defaults.STARTER_HOBBIES.forEach { h -> if (!state.hobbies.contains(h)) state.hobbies.add(h) }
    }

    // ---- action history and logs ----

    private fun creditCoins(state: GameState, amount: Int) {
        val paidToDebt = minOf(amount, state.coinDebt)
        state.coinDebt -= paidToDebt
        state.coins += amount - paidToDebt
    }

    private fun revokeCoins(state: GameState, amount: Int) {
        val returned = minOf(amount, state.coins)
        state.coins -= returned
        state.coinDebt += amount - returned
    }

    private fun syncDailyCoinBonuses(state: GameState, day: String) {
        DailyCoinBonus.goals.forEach { goal ->
            val key = DailyCoinBonus.claimKey(day, goal)
            val qualifies = DailyCoinBonus.completed(state, day, goal)
            if (qualifies && state.coinRewardDays.add(key)) creditCoins(state, goal.coins)
            else if (!qualifies && state.coinRewardDays.remove(key)) revokeCoins(state, goal.coins)
        }
    }

    private fun recordAction(
        state: GameState,
        skill: String,
        xp: Int,
        name: String,
        type: String,
        date: String = Dates.gameDayKey(),
        hours: Double = 0.0,
        hobby: String? = null,
        baseXp: Int = 0,
        decayPercent: Int = 0,
        monthlyInvestmentAmount: Double = 0.0,
        careKey: String? = null,
        previousCareLast: String? = null,
        newCareLast: String? = null
    ): ActionEntry {
        val action = ActionEntry(
            id = Ids.make("act"),
            ts = Dates.nowText(),
            date = date,
            week = Dates.weekKeyOf(date),
            month = date.take(7),
            skill = skill,
            xp = xp,
            baseXp = baseXp,
            decayPercent = decayPercent,
            type = type,
            name = name,
            hours = hours,
            hobby = hobby,
            monthlyInvestmentAmount = monthlyInvestmentAmount,
            careKey = careKey,
            previousCareLast = previousCareLast,
            newCareLast = newCareLast
        )
        if (skill in Rules.SKILL_KEYS && type in listOf("activity", "study", "hobby", "other")) {
            state.coinAwardedActionIds.add(action.id)
            creditCoins(state, 10)
        }
        state.actionHistory.add(0, action)
        if (skill in Rules.SKILL_KEYS && type in listOf("activity", "study", "hobby", "other", "workout")) {
            syncDailyCoinBonuses(state, date)
        }
        while (state.actionHistory.size > MAX_HISTORY) {
            state.coinAwardedActionIds.remove(state.actionHistory.removeAt(state.actionHistory.size - 1).id)
        }
        return action
    }

    private fun pushTaskLog(state: GameState, text: String, actionId: String? = null) {
        state.taskLog.add(0, LogEntry(Ids.make("log"), text, actionId))
        while (state.taskLog.size > MAX_LOG) state.taskLog.removeAt(state.taskLog.size - 1)
    }

    private fun pushFinanceLog(state: GameState, text: String, actionId: String? = null) {
        state.financeLog.add(0, LogEntry(Ids.make("flog"), text, actionId))
        while (state.financeLog.size > MAX_LOG) state.financeLog.removeAt(state.financeLog.size - 1)
    }

    private fun addUniqueDate(state: GameState, listName: String, key: String) {
        val list = if (listName == "studyDates") state.studyDates else state.workoutDates
        if (!list.contains(key)) list.add(key)
        while (list.size > MAX_DATES) list.removeAt(0)
    }

    private fun trackTaskDay(state: GameState, skill: String, taskName: String, key: String) {
        val label = taskName.lowercase()
        if (skill == "knowledgeXp" || label.contains("study")) addUniqueDate(state, "studyDates", key)
        if (skill == "strengthXp" || label.contains("workout") || label.contains("exercise") ||
            label.contains("fitness")
        ) {
            addUniqueDate(state, "workoutDates", key)
        }
    }

    fun weeklyBucket(state: GameState, forKey: String = Dates.gameDayKey()): MutableMap<String, Int> {
        val week = Dates.weekKeyOf(forKey)
        return state.weeklyAttrXp.getOrPut(week) { mutableMapOf() }
    }

    fun weeklyXp(state: GameState, skill: String): Int = weeklyBucket(state)[skill] ?: 0

    private fun recordWeeklyXp(state: GameState, skill: String, xp: Int, forKey: String = Dates.gameDayKey()) {
        val bucket = weeklyBucket(state, forKey)
        bucket[skill] = (bucket[skill] ?: 0) + xp
        val keys = state.weeklyAttrXp.keys.sorted()
        var removeCount = keys.size - MAX_WEEKLY_BUCKETS
        var i = 0
        while (removeCount > 0 && i < keys.size) {
            state.weeklyAttrXp.remove(keys[i])
            i++
            removeCount--
        }
    }

    // ---- decay ----

    fun lastDateForAttribute(state: GameState, skill: String): String {
        val dates = state.actionHistory
            .filter { it.skill == skill && it.date.isNotEmpty() }
            .map { it.date }
            .toMutableList()
        if (skill == "knowledgeXp") {
            state.studyHoursByDate.forEach { (day, hours) -> if (hours > 0) dates.add(day) }
        }
        return dates.maxOrNull() ?: ""
    }

    fun daysSinceAttribute(state: GameState, skill: String): Int {
        val last = lastDateForAttribute(state, skill)
        if (last.isEmpty()) {
            return Dates.daysBetween(Dates.todayKey(), state.decayStartDate.ifEmpty { Dates.todayKey() })
        }
        return Dates.daysBetween(Dates.todayKey(), last)
    }

    /** Non-stacking: the single most neglected OTHER attribute sets the penalty. */
    fun calculateActivityDecay(state: GameState, currentSkill: String): Int =
        Rules.SKILL_KEYS.filter { it != currentSkill }
            .maxOfOrNull { Rules.decayPercentForMissedDays(daysSinceAttribute(state, it)) } ?: 0

    fun batteryLevelForSkill(state: GameState, skill: String): Int = when (val days = daysSinceAttribute(state, skill)) {
        in 0..3 -> 4
        in 4..6 -> 3
        in 7..9 -> 2
        in 10..15 -> 1
        else -> 0
    }

    /** Marginal daily reward: 200 base XP at 100%, next 200 at 50%, rest at 25%.
     * Cumulative rounding makes splitting a session yield the same total reward. */
    fun activityXpPreview(state: GameState, skill: String, baseXp: Int): Pair<Int, Int> =
        rewardPreview(state, skill, baseXp, Dates.gameDayKey())

    fun activityXpPreviewForDate(state: GameState, skill: String, baseXp: Int, day: String): Pair<Int, Int> = rewardPreview(state, skill, baseXp, day)

    private fun rewardPreview(state: GameState, skill: String, baseXp: Int, day: String): Pair<Int, Int> {
        val base = baseXp.coerceIn(0, 10000)
        val used = state.actionHistory.filter { it.date == day && it.skill == skill && it.type in listOf("activity", "study", "hobby", "other") }
            .sumOf { (if (it.baseXp > 0) it.baseXp else it.xp).toLong() }
        fun cumulative(value: Long): Long = minOf(value, 200L) * 4 + (value - 200).coerceIn(0L, 200L) * 2 + (value - 400).coerceAtLeast(0)
        val reward = ((cumulative(used + base) + 2) / 4 - (cumulative(used) + 2) / 4).toInt()
        return reward to if (base == 0) 0 else (100 - reward * 100.0 / base).roundToInt().coerceIn(0, 100)
    }

    // ---- core XP action ----

    fun addXp(
        state: GameState,
        skill: String,
        xp: Int,
        taskName: String,
        actionDateKey: String = Dates.gameDayKey()
    ): ActionEntry {
        val baseXp = xp.coerceIn(0, 10000)
        val (finalXp, decayPercent) = rewardPreview(state, skill, baseXp, actionDateKey)

        state.totalXp += finalXp
        state.addSkillXp(skill, finalXp)
        recordWeeklyXp(state, skill, finalXp, actionDateKey)
        state.streak += 1
        trackTaskDay(state, skill, taskName, actionDateKey)

        val action = recordAction(
            state, skill = skill, xp = finalXp, name = taskName, type = "activity",
            date = actionDateKey, baseXp = baseXp, decayPercent = decayPercent
        )
        val xpText = if (decayPercent > 0) "+$finalXp XP ($decayPercent% reduced)" else "+$finalXp XP"
        pushTaskLog(state, "${action.ts} • $taskName • $xpText", action.id)
        return action
    }

    fun completeQuest(state: GameState, index: Int): ActionEntry? {
        val quest = Defaults.QUESTS.getOrNull(index) ?: return null
        return addXp(state, quest.skill, quest.xp, quest.name)
    }

    fun completeCustomTask(state: GameState, name: String, skill: String, xp: Int): Outcome {
        if (xp !in 0..10000) return Outcome.Failure("Use 0–10,000 base XP.")
        addXp(state, skill, xp, name.ifBlank { "Custom Quest" })
        return Outcome.Applied
    }

    // ---- study ----

    fun studyHoursToday(state: GameState): Double =
        max(0.0, min(STUDY_CYCLE_CAP, state.studyHoursByDate[Dates.studyDateKey()] ?: 0.0))

    private fun addStudyHours(state: GameState, hours: Double, overwriteToday: Boolean) {
        val capped = max(0.0, min(STUDY_CYCLE_CAP, hours))
        val key = Dates.studyDateKey()
        val previous = state.studyHoursByDate[key] ?: 0.0
        val updated = if (overwriteToday) capped else min(STUDY_CYCLE_CAP, previous + capped)
        state.studyHoursByDate[key] = updated
        if (updated > 0) addUniqueDate(state, "studyDates", key)
    }

    fun addStudySession(state: GameState, hours: Double): Outcome {
        if (hours <= 0) return Outcome.Failure("Enter study hours first.")
        val key = Dates.studyDateKey()
        val previous = state.studyHoursByDate[key] ?: 0.0
        val allowed = max(0.0, min(hours, STUDY_CYCLE_CAP - previous))
        if (allowed <= 0) {
            return Outcome.Failure("This 5 AM to 5 AM study cycle already has 8 study hours logged.")
        }
        val base = (allowed * 50).roundToInt()
        val (xp, reduction) = rewardPreview(state, "knowledgeXp", base, key)
        addStudyHours(state, allowed, false)
        state.totalXp += xp
        state.knowledgeXp += xp
        recordWeeklyXp(state, "knowledgeXp", xp)
        state.streak += 1
        val label = "Study ${numText(allowed)} hour" + if (allowed == 1.0) "" else "s"
        val action = recordAction(
            state, skill = "knowledgeXp", xp = xp, name = label, type = "study",
            date = key, hours = allowed, baseXp = base, decayPercent = reduction
        )
        pushTaskLog(state, "${action.ts} • $label • +$xp Study XP", action.id)
        return Outcome.Applied
    }

    fun studyStreak(state: GameState): Int {
        val dates = state.studyDates.toSet()
        var count = 0
        var key = Dates.todayKey()
        while (dates.contains(key)) {
            count += 1
            key = Dates.addDays(key, -1)
        }
        return count
    }

    /** A workout streak survives one rest day: gaps of up to 2 days still chain. */
    fun workoutStreak(state: GameState): Int {
        val sorted = state.workoutDates.distinct().sortedDescending()
        if (sorted.isEmpty()) return 0
        val today = Dates.todayKey()
        val daysSinceLast = Dates.daysBetween(today, sorted.first())
        if (daysSinceLast > 1) return 0
        var streak = 1
        var previous = sorted.first()
        for (i in 1 until sorted.size) {
            val gap = Dates.daysBetween(previous, sorted[i])
            if (gap <= 2) {
                streak += 1
                previous = sorted[i]
            } else break
        }
        return streak
    }

    // ---- sleep ----

    fun sleepXpForHours(hours: Double): Int {
        val h = if (hours <= 0) 1.0 else hours
        return when {
            h <= 2 -> 5
            h == 3.0 -> 10
            h == 4.0 -> 20
            h == 5.0 -> 30
            h == 6.0 -> 40
            h == 7.0 -> 55
            else -> 70
        }
    }

    /** Hours already logged for last night, used to show the previous-night figure. */
    fun previousNightHours(state: GameState): Double =
        SleepStats.hoursByDate(state)[Dates.previousNightKeyOfNow()] ?: 0.0

    fun completeSleepQuest(state: GameState, hours: Double): ActionEntry {
        val xp = sleepXpForHours(hours)
        val sleepKey = Dates.previousNightKeyOfNow()
        state.lastSleepHours = hours
        state.totalXp += xp
        state.streak += 1
        val label = "Previous night sleep ${numText(hours)} hours"
        val action = recordAction(
            state, skill = "sleep", xp = xp, name = label, type = "sleep",
            date = sleepKey, hours = hours
        )
        pushTaskLog(state, "${action.ts} • $label", action.id)
        return action
    }

    // ---- hobbies ----

    fun addHobby(state: GameState, hobby: String): Outcome {
        val name = hobby.trim()
        if (name.isEmpty()) return Outcome.Failure("Enter a hobby name.")
        ensureHobbies(state)
        if (state.hobbies.none { it.lowercase() == name.lowercase() }) state.hobbies.add(name)
        return Outcome.Applied
    }

    fun completeHobbyActivity(state: GameState, hobby: String, note: String, xp: Int): Outcome {
        if (xp !in 0..10000) return Outcome.Failure("Use 0–10,000 base XP.")
        val picked = hobby.ifBlank { "Hobby" }
        val name = if (note.isNotBlank()) "$picked: ${note.trim()}" else "$picked practice"
        val base = xp.coerceIn(1, 10000)
        val (reward, reduction) = activityXpPreview(state, "hobbiesXp", base)
        state.totalXp += reward
        state.hobbiesXp += reward
        recordWeeklyXp(state, "hobbiesXp", reward)
        state.streak += 1
        val action = recordAction(state, skill = "hobbiesXp", xp = reward, name = name, type = "hobby", hobby = picked, baseXp = base, decayPercent = reduction)
        pushTaskLog(state, "${action.ts} • $name • +$reward Hobbies XP", action.id)
        return Outcome.Applied
    }

    // ---- finance ----

    fun updateFinance(state: GameState, input: FinanceInput) {
        input.bankBalance?.let { state.bankBalance = it }
        input.savings?.let { state.savings = it }
        input.emergencyFund?.let { state.emergencyFund = it }
        input.investments?.let { state.investments = it }
        input.savingsGoal?.let { state.savingsGoal = it }
        input.bankGoal?.let { state.bankGoal = it }
        input.emergencyGoal?.let { state.emergencyGoal = it }
        input.investmentsGoal?.let { state.investmentsGoal = it }

        val monthlyAmount = input.monthlyInvestment?.let { max(0.0, it) } ?: 0.0
        if (monthlyAmount > 0) {
            val action = recordAction(
                state, skill = "finance", xp = 0, name = "Monthly investment", type = "finance",
                monthlyInvestmentAmount = monthlyAmount
            )
            pushFinanceLog(state, "${action.ts} • Monthly investment ${Rules.money(monthlyAmount)}", action.id)
        }
    }

    fun monthlyInvestmentDone(state: GameState, monthKey: String = Dates.monthKeyOfNow()): Double =
        state.actionHistory
            .filter { it.type == "finance" && it.date.startsWith(monthKey) }
            .sumOf { max(0.0, it.monthlyInvestmentAmount) }

    fun financePercent(value: Double, goal: Double): Int =
        if (goal > 0) max(0, min(100, (value / goal * 100).roundToInt())) else 0

    // ---- activity definitions ----

    fun ensureActivityIds(state: GameState, skill: String) {
        val lists = state.activityDefs[skill] ?: return
        listOf("major" to lists.major, "other" to lists.other).forEach { (listName, defs) ->
            defs.forEachIndexed { index, def ->
                if (def.id.isEmpty()) defs[index] = def.copy(id = stableActivityId(skill, listName, index, def.name))
            }
        }
    }

    private fun stableActivityId(skill: String, list: String, index: Int, name: String) =
        "${skill}_${list}_${index}_${normalizeActivityName(name)}"

    private fun normalizeActivityName(name: String): String {
        val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').take(40)
        return slug.ifEmpty { "activity" }
    }

    fun ensureActivityDefs(state: GameState) {
        val defaults = Defaults.activityDefs()
        Rules.SKILL_KEYS.forEach { skill ->
            if (state.activityDefs[skill] == null) {
                state.activityDefs[skill] = defaults.getValue(skill)
            }
            ensureActivityIds(state, skill)
        }

    }

    fun defsFor(state: GameState, skill: String, list: String): List<ActivityDef> {
        val lists = state.activityDefs[skill] ?: return emptyList()
        return if (list == "major") lists.major else lists.other
    }

    fun completeActivity(state: GameState, skill: String, list: String, index: Int): Outcome {
        ensureActivityDefs(state)
        val activity = defsFor(state, skill, list).getOrNull(index) ?: return Outcome.Failure("Activity not found.")
        val action = addXp(state, skill, activity.xp, activity.name.ifEmpty { "Activity" })
        val historyIndex = state.actionHistory.indexOfFirst { it.id == action.id }
        if (historyIndex >= 0) state.actionHistory[historyIndex] = action.copy(activityId = activity.id)
        return Outcome.Applied
    }

    fun addActivity(state: GameState, skill: String, name: String, xp: Int): Outcome {
        ensureActivityDefs(state)
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Outcome.Failure("Enter activity name.")
        if (skill !in Rules.SKILL_KEYS || xp !in 0..10000) return Outcome.Failure("Choose a category and 0–10,000 base XP.")
        val lists = state.activityDefs.getOrPut(skill) { ActivityList() }
        lists.other.add(ActivityDef("${skill}_custom_${System.currentTimeMillis()}_${Random.nextInt(100_000)}", trimmed, xp))
        pushTaskLog(
            state,
            "${Dates.nowText()} • Added activity $trimmed under ${Rules.SKILL_LABELS[skill] ?: skill} • +$xp XP assigned"
        )
        return Outcome.Applied
    }

    fun updateActivityById(state: GameState, skill: String, id: String, name: String, xp: Int): Outcome {
        ensureActivityDefs(state)
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Outcome.Failure("Enter activity name.")
        if (xp !in 0..10000) return Outcome.Failure("Use 0–10,000 base XP.")
        val lists = state.activityDefs[skill] ?: return Outcome.Failure("Activity list not found.")
        val source = listOf(lists.major, lists.other).firstOrNull { defs -> defs.any { it.id == id } }
            ?: return Outcome.Failure("Activity not found.")
        val index = source.indexOfFirst { it.id == id }
        source[index] = source[index].copy(name = trimmed, xp = xp)
        return Outcome.Applied
    }
    /**
     * Removing an activity keeps its earned XP: past history rows are re-labelled to
     * "<Skill>: Others" and the definition is parked in [GameState.removedActivities].
     */
    fun removeActivityById(state: GameState, skill: String, id: String): Outcome {
        ensureActivityDefs(state)
        val lists = state.activityDefs[skill] ?: return Outcome.Failure("Activity list not found.")
        val source = listOf(lists.major, lists.other).firstOrNull { defs -> defs.any { it.id == id } }
            ?: return Outcome.Failure("Activity not found. It may already be removed.")
        val removed = source.removeAt(source.indexOfFirst { it.id == id })
        state.removedActivities.add(
            0,
            RemovedActivity(skill, id, removed.name, removed.xp, Dates.nowText())
        )
        relabelHistoryAsOthers(state, skill, removed.name)
        pushTaskLog(state, "${Dates.nowText()} • Removed activity ${removed.name}; previous XP preserved under Others")
        return Outcome.Applied
    }

    private fun relabelHistoryAsOthers(state: GameState, skill: String, name: String) {
        val label = "${Rules.SKILL_LABELS[skill] ?: skill}: Others"
        val rewritten = state.actionHistory.map { a ->
            if (a.skill == skill && a.name == name) {
                a.copy(originalName = a.name, name = label, type = "other")
            } else a
        }
        state.actionHistory.clear()
        state.actionHistory.addAll(rewritten)
    }

    // ---- to-do lists ----

    fun ensureTodoTasks(state: GameState) {
        Dates.resetHour = state.dayResetHour
        if (state.todoDate.isEmpty()) state.todoDate = Dates.gameDayKey()
        val today = Dates.gameDayKey()
        if (state.todoDate != today) {
            val oldDate = state.todoDate
            if (oldDate.isNotEmpty()) {
                state.todoHistory[oldDate] = state.todoTasks.map { it.copy() }.toMutableList()
            }
            state.todoTasks.clear()
            state.todoTasks.addAll(state.todoHistory.remove(today).orEmpty())
            state.todoDate = today
            state.todoViewOffset = 0
        }
        Planning.materialize(state, today, state.todoTasks)
        materializeCareTasks(state)
    }

    fun setDayResetHour(state: GameState, hour: Int): Outcome {
        if (hour !in 0..23) return Outcome.Failure("Choose an hour from 0 to 23.")
        state.dayResetHour = hour
        Dates.resetHour = hour
        ensureTodoTasks(state)
        return Outcome.Applied
    }

    fun careTaskId(key: String, day: String) = "todo-care-$key-$day"

    private fun materializeCareTasks(state: GameState) {
        ensurePersonalCare(state)
        val day = state.todoDate
        val enabledIds = state.careRoutines.map { careTaskId(it.key, day) }.toSet()
        state.todoTasks.removeAll { it.id.startsWith("todo-care-") && !it.completed && it.id !in enabledIds }
        state.careRoutines.forEach { rule ->
            val last = state.personalCareLast[rule.key].orEmpty()
            val due = last.isEmpty() || Dates.daysBetween(day, last) >= rule.cycleDays
            val id = careTaskId(rule.key, day)
            if (due && state.scheduledOccurrences.add(id) && state.todoTasks.none { it.id == id })
                state.todoTasks.add(TodoTask(id, rule.label, "socialXp", CARE_ACTIVITY_XP, day))
            val index = state.todoTasks.indexOfFirst { it.id == id && !it.completed }
            if (index >= 0) state.todoTasks[index] = state.todoTasks[index].copy(name = rule.label, skill = "socialXp", xp = CARE_ACTIVITY_XP)
        }
    }

    fun todoDateForOffset(offset: Int): String = Dates.addDays(Dates.gameDayKey(), offset)

    fun getTodoListForView(state: GameState, createIfMissing: Boolean = false): TodoView {
        ensureTodoTasks(state)
        val offset = state.todoViewOffset
        val key = todoDateForOffset(offset)
        if (key >= state.todoDate && offset != 0) {
            Planning.materialize(state, key, state.todoHistory.getOrPut(key) { mutableListOf() })
        }
        if (offset == 0) return TodoView(key, state.todoTasks, true)
        val existing = state.todoHistory[key]
        if (existing != null) return TodoView(key, existing, false)
        if (createIfMissing) {
            val created = mutableListOf<TodoTask>()
            state.todoHistory[key] = created
            return TodoView(key, created, false)
        }
        return TodoView(key, mutableListOf(), false)
    }

    fun showPreviousTodoDay(state: GameState) {
        ensureTodoTasks(state)
        state.todoViewOffset -= 1
    }

    fun showNextTodoDay(state: GameState) {
        ensureTodoTasks(state)
        state.todoViewOffset += 1
    }

    fun openTodoWindow(state: GameState) {
        ensureTodoTasks(state)
        state.todoViewOffset = 0
    }

    fun addTodoTask(state: GameState, name: String, skill: String, xp: Int): Outcome {
        ensureTodoTasks(state)
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Outcome.Failure("Enter task name first.")
        if (skill !in Rules.SKILL_KEYS || xp !in 0..10000) return Outcome.Failure("Choose a category and 0–10,000 base XP.")
        val view = getTodoListForView(state, true)
        if (view.list.any { it.skill == skill && it.name.equals(trimmed, true) }) return Outcome.Failure("That task is already on this day's list.")
        view.list.add(
            TodoTask(
                id = Ids.make("todo"),
                name = trimmed,
                skill = skill.ifEmpty { "knowledgeXp" },
                xp = xp,
                date = view.key,
                completed = false,
                completedAt = ""
            )
        )
        return Outcome.Applied
    }

    fun updateTodoTask(state: GameState, id: String, name: String, skill: String, xp: Int): Outcome {
        val tasks = getTodoListForView(state, true).list
        val index = tasks.indexOfFirst { it.id == id }
        if (index < 0) return Outcome.Failure("Task not found.")
        if (tasks[index].completed) return Outcome.Failure("Undo this task before editing it.")
        if (name.isBlank() || skill !in Rules.SKILL_KEYS || xp !in 0..10000) return Outcome.Failure("Enter a task name, category and 0–10,000 base XP.")
        if (tasks.any { it.id != id && it.skill == skill && it.name.equals(name.trim(), true) }) return Outcome.Failure("This task is already on your day.")
        tasks[index] = tasks[index].copy(name = name.trim().take(100), skill = skill, xp = xp)
        return Outcome.Applied
    }

    fun completeTodoTask(state: GameState, id: String): Outcome {
        ensureTodoTasks(state)
        val view = getTodoListForView(state, true)
        return completePlannedTask(state, view.list, id)
    }

    fun completePlannedTask(state: GameState, tasks: MutableList<TodoTask>, id: String): Outcome {
        val index = tasks.indexOfFirst { it.id == id }
        if (index < 0) return Outcome.Failure("Task not found.")
        val task = tasks[index]
        if (task.date > Dates.gameDayKey()) return Outcome.Failure("Future tasks can be completed on their planned day.")
        if (task.completed) return Outcome.Applied
        val careRule = state.careRoutines.firstOrNull { careTaskId(it.key, task.date) == task.id }
        if (careRule != null) return completePersonalCare(state, careRule.key, task.date)
        tasks[index] = task.copy(completed = true, completedAt = Dates.nowText())
        if (state.focusSession?.taskId == id) state.focusSession = null
        val action = addXp(
            state,
            task.skill.ifEmpty { "knowledgeXp" },
            task.xp,
            task.name.ifEmpty { "To-do task" },
            task.date.ifEmpty { Dates.gameDayKey() }
        )
        val actionIndex = state.actionHistory.indexOfFirst { it.id == action.id }
        state.actionHistory[actionIndex] = action.copy(activityId = task.id)
        return Outcome.Applied
    }

    fun deleteTodoTask(state: GameState, id: String): Outcome {
        ensureTodoTasks(state)
        val view = getTodoListForView(state, true)
        val index = view.list.indexOfFirst { it.id == id }
        if (index < 0) return Outcome.Failure("Task not found.")
        if (state.focusSession?.taskId == id) state.focusSession = null
        val action = state.actionHistory.firstOrNull { it.activityId == id }
        if (action != null) {
            undoActionById(state, action.id)
            state.taskLog.removeAll { it.actionId == action.id }
        }
        view.list.removeAt(index)
        return Outcome.Applied
    }

    fun undoTodoTask(state: GameState, id: String): Outcome {
        ensureTodoTasks(state)
        val view = getTodoListForView(state, true)
        val index = view.list.indexOfFirst { it.id == id }
        if (index < 0) return Outcome.Failure("Task not found.")
        if (!view.list[index].completed) return Outcome.Failure("This task is already open.")
        val action = state.actionHistory.firstOrNull { it.activityId == id }
            ?: return Outcome.Failure("The completion record for this task is missing.")
        undoActionById(state, action.id)
        state.taskLog.removeAll { it.actionId == action.id }
        return Outcome.Applied
    }

    // ---- personal care ----

    fun defaultPersonalCareDates(): Map<String, String> {
        val today = Dates.gameDayKey()
        return Rules.CARE_RULES.associate { it.key to Dates.addDays(today, -(it.cycleDays * 2)) }
    }

    fun ensurePersonalCare(state: GameState) {
        if (!state.careRoutinesConfigured) {
            state.careRoutines.addAll(Rules.CARE_RULES)
            state.careRoutinesConfigured = true
            state.careDefaultsVersion = 1
        }
        if (state.careDefaultsVersion < 1) {
            val oldDefaults = setOf(
                Triple("haircut", "Haircut", 15),
                Triple("teeth", "Brush teeth", 1),
                Triple("manscaping", "Grooming", 15),
                Triple("shaving", "Shaving", 7)
            )
            state.careRoutines.removeAll { Triple(it.key, it.label, it.cycleDays) in oldDefaults }
            state.careDefaultsVersion = 1
        }
        state.careRoutines.forEach { rule ->
            if (!state.personalCareLast.containsKey(rule.key)) {
                state.personalCareLast[rule.key] = Dates.addDays(Dates.gameDayKey(), -(rule.cycleDays * 2))
            }
        }
    }

    fun addSuggestedCareRoutine(state: GameState, key: String): Outcome {
        ensurePersonalCare(state)
        val rule = Rules.CARE_SUGGESTIONS.firstOrNull { it.key == key }
            ?: return Outcome.Failure("Unknown care routine.")
        if (state.careRoutines.any { it.key == key || it.label.equals(rule.label, true) }) {
            return Outcome.Failure("This routine is already in your list.")
        }
        state.careRoutines.add(rule)
        state.personalCareLast[key] = ""
        return Outcome.Applied
    }

    fun saveCareRoutine(state: GameState, key: String?, label: String, cycleDays: Int): Outcome {
        ensurePersonalCare(state)
        val name = label.trim()
        if (name.isEmpty() || name.length > 60) return Outcome.Failure("Enter a name of 1–60 characters.")
        if (cycleDays !in 1..365) return Outcome.Failure("Choose a frequency from 1 to 365 days.")
        if (state.careRoutines.any { it.key != key && it.label.equals(name, true) }) return Outcome.Failure("A routine already has that name.")
        val index = state.careRoutines.indexOfFirst { it.key == key }
        if (key != null && index < 0) return Outcome.Failure("This routine was removed.")
        if (index >= 0) state.careRoutines[index] = state.careRoutines[index].copy(label = name, cycleDays = cycleDays)
        else {
            val id = "care_" + java.util.UUID.randomUUID().toString()
            state.careRoutines.add(Rules.CareRule(id, name, "", cycleDays, 2))
            state.personalCareLast[id] = ""
        }
        return Outcome.Applied
    }

    fun removeCareRoutine(state: GameState, key: String): Outcome {
        ensurePersonalCare(state)
        if (!state.careRoutines.removeAll { it.key == key }) return Outcome.Failure("This routine was already removed.")
        // Keep historical dates and actions so undo and backups retain the record.
        return Outcome.Applied
    }

    fun careMissedUnits(state: GameState, key: String): Int {
        ensurePersonalCare(state)
        val rule = state.careRoutines.firstOrNull { it.key == key } ?: return 0
        val last = state.personalCareLast[key] ?: ""
        if (last.isEmpty()) return 1
        val days = Dates.daysBetween(Dates.gameDayKey(), last)
        return max(0, days / rule.cycleDays)
    }

    fun personalCareStats(state: GameState): CareStats {
        ensurePersonalCare(state)
        val items = state.careRoutines.map { rule ->
            val missedUnits = careMissedUnits(state, rule.key)
            val loss = missedUnits * rule.weight
            val last = state.personalCareLast[rule.key] ?: ""
            CareItemStatus(
                key = rule.key,
                label = rule.label,
                emoji = rule.emoji,
                cycleDays = rule.cycleDays,
                weight = rule.weight,
                missedUnits = missedUnits,
                loss = loss,
                days = if (last.isEmpty()) rule.cycleDays else Dates.daysBetween(Dates.gameDayKey(), last),
                last = last,
                level = Rules.careItemLevel(missedUnits)
            )
        }
        val missedAverage = if (items.isEmpty()) 0.0 else items.map { it.missedUnits.coerceIn(0, 3) }.average()
        val score = (CARE_SCORE_MAX - missedAverage * (CARE_SCORE_MAX / 3.0)).roundToInt().coerceIn(0, CARE_SCORE_MAX)
        return CareStats(score, Rules.careRank(score), items)
    }

    @JvmOverloads
    fun completePersonalCare(state: GameState, key: String, completedDate: String = Dates.gameDayKey()): Outcome {
        ensureTodoTasks(state)
        val rule = state.careRoutines.firstOrNull { it.key == key } ?: return Outcome.Failure("Unknown care item.")
        val safeDate = minOf(completedDate, Dates.gameDayKey())
        if (state.actionHistory.any { it.careKey == key && it.date == safeDate }) return Outcome.Applied
        val previous = state.personalCareLast[key]
        state.personalCareLast[key] = safeDate
        state.personalCareResetBaseline = false
        val action = addXp(state, "socialXp", CARE_ACTIVITY_XP, rule.label, safeDate)
        val id = careTaskId(key, safeDate)
        val index = state.actionHistory.indexOfFirst { it.id == action.id }
        state.actionHistory[index] = action.copy(careKey = key, previousCareLast = previous, newCareLast = safeDate, activityId = id)
        val tasks = if (safeDate == state.todoDate) state.todoTasks else state.todoHistory.getOrPut(safeDate) { mutableListOf() }
        val taskIndex = tasks.indexOfFirst { it.id == id }
        val done = TodoTask(id, rule.label, "socialXp", CARE_ACTIVITY_XP, safeDate, true, Dates.nowText())
        if (taskIndex >= 0) tasks[taskIndex] = done else tasks.add(done)
        state.scheduledOccurrences.add(id)
        return Outcome.Applied
    }

    // ---- milestones ----

    fun ensureMilestones(state: GameState) {
        val kept = state.milestones
            .filter { !LEGACY_MILESTONE_ID.matches(it.id) }
            .mapIndexed { i, m ->
                m.copy(
                    id = m.id.ifEmpty { "m_custom_${i}_${System.currentTimeMillis()}" },
                    title = m.title.ifEmpty { "Milestone ${i + 1}" },
                    desc = m.desc.takeUnless { it == "A custom real-life milestone." }.orEmpty()
                )
            }
        state.milestones.clear()
        state.milestones.addAll(kept)
    }

    fun visibleMilestones(state: GameState): List<Milestone> {
        ensureMilestones(state)
        return state.milestones.filter { it.showOnMain }
    }

    fun addMilestone(state: GameState, title: String, desc: String): Outcome {
        ensureMilestones(state)
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return Outcome.Failure("Write a milestone title first.")
        state.milestones.add(
            Milestone(
                id = "m_custom_${System.currentTimeMillis()}",
                title = trimmed,
                desc = desc.trim()
            )
        )
        return Outcome.Applied
    }

    fun completeMilestoneById(state: GameState, id: String): Outcome {
        ensureMilestones(state)
        val index = state.milestones.indexOfFirst { it.id == id }
        if (index < 0) return Outcome.Failure("Milestone not found.")
        val milestone = state.milestones[index]
        state.milestones[index] = milestone.copy(
            completed = true,
            completedDate = milestone.completedDate.ifEmpty { Dates.todayKey() }
        )
        return Outcome.Applied
    }

    fun saveMilestone(state: GameState, id: String?, title: String, desc: String, completed: Boolean, pinned: Boolean): Outcome {
        if (title.isBlank()) return Outcome.Failure("Give this milestone a name.")
        val previous = state.milestones.firstOrNull { it.id == id }
        val milestone = Milestone(id ?: Ids.make("m_custom"), title.trim().take(100), desc.trim().take(4000), completed,
            if (completed) previous?.completedDate?.ifEmpty { Dates.todayKey() } ?: Dates.todayKey() else "", pinned, previous?.image.orEmpty())
        state.milestones.removeAll { it.id == milestone.id }
        state.milestones.add(milestone)
        return Outcome.Applied
    }

    /** Validate the whole request before adding either the task or its reusable activity. */
    fun addPlannedActivity(state: GameState, name: String, skill: String, xp: Int, savePreset: Boolean, weekdays: Set<Int>): Outcome {
        if (name.isBlank() || skill !in Rules.SKILL_KEYS || xp !in 0..10000 || weekdays.any { it !in 1..7 })
            return Outcome.Failure("Enter a task, category and 0–10,000 base XP.")
        val result = addTodoTask(state, name.trim(), skill, xp)
        if (result is Outcome.Failure) return result
        if (savePreset && (defsFor(state, skill, "major") + defsFor(state, skill, "other")).none { Rules.activityLabel(skill, it.name).equals(name.trim(), true) })
            addActivity(state, skill, name.trim(), xp)
        if (weekdays.isNotEmpty()) Planning.saveRecurring(state, null, name.trim(), skill, xp, weekdays)
        return Outcome.Applied
    }

    fun toggleMilestoneMain(state: GameState, id: String): Outcome {
        ensureMilestones(state)
        val index = state.milestones.indexOfFirst { it.id == id }
        if (index < 0) return Outcome.Failure("Milestone not found.")
        state.milestones[index] = state.milestones[index].copy(showOnMain = !state.milestones[index].showOnMain)
        return Outcome.Applied
    }

    fun deleteMilestoneById(state: GameState, id: String) {
        ensureMilestones(state)
        state.milestones.removeAll { it.id == id }
    }

    // ---- custom achievements ----

    fun addCustomAchievement(state: GameState, name: String, group: String, metric: String, target: Int): Outcome {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Outcome.Failure("Enter achievement name.")
        if (target <= 0) return Outcome.Failure("Enter a target value more than 0.")
        state.customAchievements.add(
            0,
            CustomAchievement(Ids.make("ach"), trimmed, group, metric, target, Dates.nowText())
        )
        pushTaskLog(state, "${Dates.nowText()} • Added custom achievement: $trimmed")
        return Outcome.Applied
    }

    fun removeCustomAchievement(state: GameState, id: String) {
        state.customAchievements.removeAll { it.id == id }
    }

    // ---- undo ----

    private fun decrementWeeklyXp(state: GameState, action: ActionEntry) {
        if (action.week.isEmpty() || action.skill.isEmpty() ||
            action.skill == "sleep" || action.skill == "finance"
        ) {
            return
        }
        val bucket = state.weeklyAttrXp[action.week] ?: return
        bucket[action.skill] = max(0, (bucket[action.skill] ?: 0) - action.xp)
    }

    private fun recomputeDatesFromHistory(state: GameState) {
        val studyDates = linkedSetOf<String>()
        val workoutDates = linkedSetOf<String>()
        val hours = linkedMapOf<String, Double>()
        state.actionHistory.forEach { a ->
            if ((a.type == "study" || a.skill == "knowledgeXp") && a.hours > 0) {
                studyDates.add(a.date)
                hours[a.date] = min(STUDY_CYCLE_CAP, (hours[a.date] ?: 0.0) + a.hours)
            }
            if (a.type == "workout" || a.skill == "strengthXp" || WORKOUT_NAME.containsMatchIn(a.name.lowercase())) {
                workoutDates.add(a.date)
            }
        }
        state.studyDates.clear()
        state.studyDates.addAll(studyDates.toList().takeLast(MAX_DATES))
        state.workoutDates.clear()
        state.workoutDates.addAll(workoutDates.toList().takeLast(MAX_DATES))
        state.studyHoursByDate.clear()
        state.studyHoursByDate.putAll(hours)
    }

    private fun reverseActionEffects(state: GameState, action: ActionEntry) {
        if (state.coinAwardedActionIds.remove(action.id)) {
            revokeCoins(state, 10)
        }
        if (!action.careKey.isNullOrEmpty()) {
            ensurePersonalCare(state)
            val key = action.careKey
            val previous = action.previousCareLast
            if (previous != null) {
                state.personalCareLast[key] = previous
            } else {
                state.personalCareLast.remove(key)
                ensurePersonalCare(state)
            }
            if (action.type == "care") return
        }

        if (action.xp != 0) state.totalXp = max(0, state.totalXp - action.xp)
        if (action.skill in Rules.SKILL_KEYS) state.subtractSkillXp(action.skill, action.xp)
        if (action.type != "finance" && action.type != "care" && action.type != "milestone") {
            state.streak = max(0, state.streak - 1)
        }
        decrementWeeklyXp(state, action)
    }

    fun undoActionByIndex(state: GameState, index: Int): Boolean {
        if (index !in state.actionHistory.indices) return false
        val action = state.actionHistory[index]
        reverseActionEffects(state, action)
        if (action.activityId?.startsWith("todo") == true) {
            (listOf(state.todoTasks) + state.todoHistory.values).forEach { tasks ->
                val taskIndex = tasks.indexOfFirst { it.id == action.activityId }
                if (taskIndex >= 0) tasks[taskIndex] = tasks[taskIndex].copy(completed = false, completedAt = "")
            }
        }
        state.actionHistory.removeAt(index)
        if (action.skill in Rules.SKILL_KEYS) syncDailyCoinBonuses(state, action.date)
        recomputeDatesFromHistory(state)
        return true
    }

    fun undoActionById(state: GameState, actionId: String): Boolean {
        if (actionId.isEmpty()) return false
        val index = state.actionHistory.indexOfFirst { it.id == actionId }
        return if (index >= 0) undoActionByIndex(state, index) else false
    }

    private fun findActionIndexForLogText(state: GameState, text: String, type: String): Int =
        state.actionHistory.indexOfFirst { a ->
            val typeMatches = if (type == "finance") a.type == "finance" else a.type != "finance"
            typeMatches && text.contains(a.ts) &&
                (a.name.isEmpty() || text.lowercase().contains(a.name.lowercase().take(24)))
        }

    private fun parseCareDateFromLogText(text: String): String {
        CARE_DATE_ISO.find(text)?.let { return it.groupValues[1] }
        val match = CARE_DATE_PRETTY.find(text) ?: return ""
        val day = match.groupValues[1].toIntOrNull() ?: return ""
        val monthIndex = MONTH_INDEX[match.groupValues[2].lowercase()] ?: return ""
        var year = match.groupValues[3].toIntOrNull() ?: return ""
        if (year < 100) year += 2000
        return "%04d-%02d-%02d".format(year, monthIndex + 1, day)
    }

    /**
     * Older care logs predate the saved action row, so the previous date is recovered by
     * scanning the remaining care log lines for the same item.
     */
    private fun undoCareVisibleLog(state: GameState, visibleText: String, deletedIndex: Int): Boolean {
        ensurePersonalCare(state)
        val lower = visibleText.lowercase()
        val legacyShaving = Rules.CareRule("shaving", "Shaving", "🪒", 7, 3)
        val rule = (state.careRoutines + Rules.CARE_RULES + Rules.CARE_SUGGESTIONS + legacyShaving)
            .firstOrNull { lower.contains(it.label.lowercase()) } ?: return false
        var restored = Dates.addDays(Dates.gameDayKey(), -(rule.cycleDays * 2))
        for (i in state.taskLog.indices) {
            if (i == deletedIndex) continue
            val line = state.taskLog[i].text
            if (!line.contains("personal care", true)) continue
            if (!line.lowercase().contains(rule.label.lowercase())) continue
            val parsed = parseCareDateFromLogText(line)
            if (parsed.isNotEmpty()) {
                restored = parsed
                break
            }
        }
        state.personalCareLast[rule.key] = restored
        return true
    }

    fun deleteLogEntry(state: GameState, type: String, index: Int): UndoOutcome {
        val list = if (type == "finance") state.financeLog else state.taskLog
        if (index !in list.indices) {
            return UndoOutcome(false, "Could not undo this log entry. Please try again.")
        }
        val entry = list[index]
        var undone = entry.actionId?.let { undoActionById(state, it) } ?: false
        if (!undone) {
            val matchIndex = findActionIndexForLogText(state, entry.text, type)
            if (matchIndex >= 0) undone = undoActionByIndex(state, matchIndex)
        }
        if (!undone && type != "finance" && entry.text.contains("personal care", true)) {
            undone = undoCareVisibleLog(state, entry.text, index)
        }
        list.removeAt(index)
        val message = if (undone) null else
            "This visible log was removed, but it was an older entry without enough saved data to fully undo. New entries will fully undo."
        return UndoOutcome(true, message)
    }

    // ---- dashboard read models ----

    fun todayActionCount(state: GameState): Int {
        val today = Dates.todayLabel()
        return (state.taskLog + state.financeLog).count { it.text.contains(today) }
    }

    fun skillDoneToday(state: GameState, skillKey: String, label: String): Boolean {
        val today = Dates.todayKey()
        val labelLower = label.lowercase()
        val todayActions = state.actionHistory.filter { it.date == today }
        if (todayActions.any { it.skill == skillKey }) return true
        if (todayActions.any { a ->
                a.name.lowercase().contains(labelLower) ||
                    a.type.lowercase().contains(labelLower) ||
                    (a.hobby?.lowercase()?.contains(labelLower) == true)
            }
        ) {
            return true
        }
        if (skillKey == "knowledgeXp") {
            if ((state.studyHoursByDate[today] ?: 0.0) > 0) return true
            if (state.studyDates.contains(today)) return true
            if (todayActions.any { Regex("study|read|learning|knowledge").containsMatchIn(it.name.lowercase()) }) return true
        }
        if (skillKey == "strengthXp") {
            if (state.workoutDates.contains(today)) return true
            if (todayActions.any {
                    Regex("workout|exercise|fitness|gym|walk|strength").containsMatchIn(it.name.lowercase())
                }
            ) {
                return true
            }
        }
        return false
    }

    fun attributePercent(xp: Int, target: Int): Int =
        max(0, min(100, (xp.toDouble() / max(1, target) * 100).roundToInt()))

    fun weeklySkills(state: GameState): List<Triple<String, Int, Int>> =
        Rules.SKILL_KEYS.map { skill ->
            Triple(Rules.SKILL_LABELS[skill] ?: skill, weeklyXp(state, skill), Rules.WEEKLY_TARGETS[skill] ?: 0)
        }

    fun bestWeeklySkill(state: GameState): Triple<String, Int, Int>? =
        weeklySkills(state).filter { it.second > 0 }.maxByOrNull { it.second }

    // ---- profile ----

    fun renamePlayer(state: GameState, name: String): Outcome {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Outcome.Failure("Enter a player name.")
        state.playerName = trimmed
        return Outcome.Applied
    }

    fun resetGame(state: GameState): GameState {
        val fresh = GameState()
        fresh.decayStartDate = Dates.todayKey()
        fresh.personalCareLast.putAll(defaultPersonalCareDates())
        fresh.personalCareResetBaseline = true
        ensureDefaults(fresh)
        return fresh
    }
}

/** Finance panel inputs; a null field means "leave the saved value untouched". */
data class FinanceInput(
    val bankBalance: Double? = null,
    val savings: Double? = null,
    val emergencyFund: Double? = null,
    val investments: Double? = null,
    val monthlyInvestment: Double? = null,
    val savingsGoal: Double? = null,
    val bankGoal: Double? = null,
    val emergencyGoal: Double? = null,
    val investmentsGoal: Double? = null
)
