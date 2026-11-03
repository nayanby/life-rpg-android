package com.healersjourney.app.data.backup

import com.healersjourney.app.domain.ActionEntry
import com.healersjourney.app.domain.ActivityDef
import com.healersjourney.app.domain.ActivityList
import com.healersjourney.app.domain.CustomAchievement
import com.healersjourney.app.domain.GameState
import com.healersjourney.app.domain.LogEntry
import com.healersjourney.app.domain.Milestone
import com.healersjourney.app.domain.RemovedActivity
import com.healersjourney.app.domain.TodoTask
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * JSON codec for the whole [GameState], using the exact key names the web app stores in
 * localStorage so an existing browser backup can be imported and Drive saves round-trip.
 */
object StateJson {

    const val APP_NAME = "Life RPG"
    const val STORAGE_KEY = "healersJourneyLifeRpgV1"
    const val BACKUP_VERSION = 40
    const val BACKUP_TYPE = "complete-local-storage-json"

    /** Fields added after the original Room tables; kept in the same database transaction. */
    fun encodeLocalExtras(state: GameState): JSONObject {
        val all = encode(state)
        val extras = JSONObject()
        listOf("dayResetHour", "thoughts", "planning", "onboardingCompleted", "worldUnlocks", "worldEquipment", "coins", "coinRewardDays", "coinAwardedActionIds", "coinDebt", "adventureClaims", "dailyQuestDate", "dailyQuestIds",
            "soundEnabled", "hapticsEnabled", "careRoutines", "careDefaultsVersion").forEach { extras.put(it, all.get(it)) }
        extras.put("actionActivityIds", JSONObject().apply {
            state.actionHistory.forEach { action -> action.activityId?.let { put(action.id, it) } }
        })
        return extras
    }

    fun restoreLocalExtras(state: GameState, text: String) {
        val extras = JSONObject(text)
        val saved = decode(text)
        PlanningJson.restore(state, extras.optJSONObject("planning"))
        state.thoughts.addAll(saved.thoughts)
        state.dayResetHour = saved.dayResetHour
        state.onboardingCompleted = saved.onboardingCompleted
        state.worldUnlocks.addAll(saved.worldUnlocks)
        state.worldEquipment.putAll(saved.worldEquipment)
        state.coins = saved.coins
        state.coinRewardDays.addAll(saved.coinRewardDays)
        state.coinAwardedActionIds.addAll(saved.coinAwardedActionIds)
        state.coinDebt = saved.coinDebt
        state.adventureClaims.addAll(saved.adventureClaims)
        state.dailyQuestDate = saved.dailyQuestDate
        state.dailyQuestIds.addAll(saved.dailyQuestIds)
        state.soundEnabled = saved.soundEnabled
        state.hapticsEnabled = saved.hapticsEnabled
        state.careRoutinesConfigured = saved.careRoutinesConfigured
        state.careDefaultsVersion = saved.careDefaultsVersion
        state.careRoutines.addAll(saved.careRoutines)
        extras.optJSONObject("actionActivityIds")?.let { ids ->
            state.actionHistory.indices.forEach { index ->
                val action = state.actionHistory[index]
                state.actionHistory[index] = action.copy(activityId = ids.optString(action.id).ifEmpty { null })
            }
        }
    }

    fun backupEnvelope(state: GameState): String = JSONObject()
        .put("app", APP_NAME)
        .put("backupType", BACKUP_TYPE)
        .put("version", BACKUP_VERSION)
        .put("exportedAt", isoNow())
        .put("storageKey", STORAGE_KEY)
        .put("state", encode(state))
        .toString()

    private fun isoNow(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }

    fun encode(state: GameState): JSONObject = JSONObject()
        .put("thoughts", JSONArray().apply { state.thoughts.forEach { entry -> put(JSONObject()
            .put("id", entry.id).put("date", entry.date).put("text", entry.text)) } })
        .put("planning", PlanningJson.encode(state))
        .put("dayResetHour", state.dayResetHour)
        .put("playerName", state.playerName)
        .put("onboardingCompleted", state.onboardingCompleted)
        .put("worldUnlocks", JSONArray(state.worldUnlocks.toList()))
        .put("worldEquipment", encodeStringMap(state.worldEquipment))
        .put("coins", state.coins)
        .put("coinRewardDays", JSONArray(state.coinRewardDays.toList()))
        .put("coinAwardedActionIds", JSONArray(state.coinAwardedActionIds.toList()))
        .put("coinDebt", state.coinDebt)
        .put("adventureClaims", JSONArray(state.adventureClaims.toList()))
        .put("dailyQuestDate", state.dailyQuestDate)
        .put("dailyQuestIds", JSONArray(state.dailyQuestIds))
        .put("soundEnabled", state.soundEnabled)
        .put("hapticsEnabled", state.hapticsEnabled)
        .put("totalXp", state.totalXp)
        .put("streak", state.streak)
        .put("lastSleepHours", state.lastSleepHours)
        .put("knowledgeXp", state.knowledgeXp)
        .put("medicineXp", state.medicineXp)
        .put("strengthXp", state.strengthXp)
        .put("socialXp", state.socialXp)
        .put("hobbiesXp", state.hobbiesXp)
        .put("bankBalance", state.bankBalance)
        .put("savings", state.savings)
        .put("emergencyFund", state.emergencyFund)
        .put("investments", state.investments)
        .put("savingsGoal", state.savingsGoal)
        .put("bankGoal", state.bankGoal)
        .put("emergencyGoal", state.emergencyGoal)
        .put("investmentsGoal", state.investmentsGoal)
        .put("decayStartDate", state.decayStartDate)
        .put("personalCareResetBaseline", state.personalCareResetBaseline)
        .put("todoDate", state.todoDate)
        .put("todoViewOffset", state.todoViewOffset)
        .put("todoViewMode", state.todoViewMode)
        .put("hobbies", JSONArray(state.hobbies))
        .put("taskLog", encodeLogs(state.taskLog))
        .put("financeLog", encodeLogs(state.financeLog))
        .put("studyDates", JSONArray(state.studyDates))
        .put("workoutDates", JSONArray(state.workoutDates))
        .put("studyHoursByDate", encodeDoubleMap(state.studyHoursByDate))
        .put("weeklyAttrXp", encodeWeeklyXp(state.weeklyAttrXp))
        .put("actionHistory", encodeActions(state.actionHistory))
        .put("activityDefs", encodeActivityDefs(state.activityDefs))
        .put("customAchievements", encodeCustomAchievements(state.customAchievements))
        .put("milestones", encodeMilestones(state.milestones))
        .put("removedActivities", encodeRemovedActivities(state.removedActivities))
        .put("careRoutines", if (state.careRoutinesConfigured) JSONArray().apply {
            state.careRoutines.forEach { rule -> put(JSONObject().put("key", rule.key).put("label", rule.label)
                .put("emoji", rule.emoji).put("cycleDays", rule.cycleDays).put("weight", rule.weight)) }
        } else JSONObject.NULL)
        .put("careDefaultsVersion", state.careDefaultsVersion)
        .put("personalCareLast", encodeStringMap(state.personalCareLast))
        .put("todoTasks", encodeTodos(state.todoTasks))
        .put("todoHistory", encodeTodoHistory(state.todoHistory))

    /** Accepts either a backup envelope or a bare state object. */
    fun decode(text: String): GameState {
        val root = JSONObject(text)
        val json = root.optJSONObject("state") ?: root
        return decodeState(json)
    }

    private fun encodeLogs(logs: List<LogEntry>): JSONArray {
        val arr = JSONArray()
        logs.forEach { l ->
            arr.put(JSONObject().put("id", l.id).put("text", l.text).put("actionId", l.actionId ?: JSONObject.NULL))
        }
        return arr
    }

    private fun encodeActions(actions: List<ActionEntry>): JSONArray {
        val arr = JSONArray()
        actions.forEach { a ->
            arr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("ts", a.ts)
                    .put("date", a.date)
                    .put("week", a.week)
                    .put("month", a.month)
                    .put("skill", a.skill)
                    .put("xp", a.xp)
                    .put("baseXp", a.baseXp)
                    .put("decayPercent", a.decayPercent)
                    .put("type", a.type)
                    .put("name", a.name)
                    .put("originalName", a.originalName ?: JSONObject.NULL)
                    .put("hours", a.hours)
                    .put("hobby", a.hobby ?: JSONObject.NULL)
                    .put("monthlyInvestmentAmount", a.monthlyInvestmentAmount)
                    .put("careKey", a.careKey ?: JSONObject.NULL)
                    .put("previousCareLast", a.previousCareLast ?: JSONObject.NULL)
                    .put("newCareLast", a.newCareLast ?: JSONObject.NULL)
                    .put("activityId", a.activityId ?: JSONObject.NULL)
            )
        }
        return arr
    }

    private fun encodeActivityDefs(defs: Map<String, ActivityList>): JSONObject {
        val root = JSONObject()
        defs.forEach { (skill, lists) ->
            root.put(
                skill,
                JSONObject()
                    .put("major", encodeDefs(lists.major))
                    .put("other", encodeDefs(lists.other))
            )
        }
        return root
    }

    private fun encodeDefs(defs: List<ActivityDef>): JSONArray {
        val arr = JSONArray()
        defs.forEach { d -> arr.put(JSONObject().put("id", d.id).put("name", d.name).put("xp", d.xp)) }
        return arr
    }

    private fun encodeCustomAchievements(items: List<CustomAchievement>): JSONArray {
        val arr = JSONArray()
        items.forEach { c ->
            arr.put(
                JSONObject().put("id", c.id).put("name", c.name).put("group", c.group)
                    .put("metric", c.metric).put("target", c.target).put("createdAt", c.createdAt)
            )
        }
        return arr
    }

    private fun encodeMilestones(items: List<Milestone>): JSONArray {
        val arr = JSONArray()
        items.forEach { m ->
            arr.put(
                JSONObject().put("id", m.id).put("title", m.title).put("desc", m.desc)
                    .put("completed", m.completed).put("completedDate", m.completedDate)
                    .put("showOnMain", m.showOnMain).put("image", m.image)
            )
        }
        return arr
    }

    private fun encodeRemovedActivities(items: List<RemovedActivity>): JSONArray {
        val arr = JSONArray()
        items.forEach { r ->
            arr.put(
                JSONObject().put("skill", r.skill).put("id", r.id ?: JSONObject.NULL)
                    .put("name", r.name).put("xp", r.xp).put("removedAt", r.removedAt)
            )
        }
        return arr
    }

    private fun encodeTodos(items: List<TodoTask>): JSONArray {
        val arr = JSONArray()
        items.forEach { t ->
            arr.put(
                JSONObject().put("id", t.id).put("name", t.name).put("skill", t.skill)
                    .put("xp", t.xp).put("date", t.date).put("completed", t.completed)
                    .put("completedAt", t.completedAt)
            )
        }
        return arr
    }

    private fun encodeTodoHistory(history: Map<String, List<TodoTask>>): JSONObject {
        val root = JSONObject()
        history.forEach { (date, tasks) -> root.put(date, encodeTodos(tasks)) }
        return root
    }

    private fun encodeDoubleMap(map: Map<String, Double>): JSONObject {
        val root = JSONObject()
        map.forEach { (k, v) -> root.put(k, v) }
        return root
    }

    private fun encodeStringMap(map: Map<String, String>): JSONObject {
        val root = JSONObject()
        map.forEach { (k, v) -> root.put(k, v) }
        return root
    }

    private fun encodeWeeklyXp(map: Map<String, Map<String, Int>>): JSONObject {
        val root = JSONObject()
        map.forEach { (week, skills) ->
            val weekJson = JSONObject()
            skills.forEach { (skill, xp) -> weekJson.put(skill, xp) }
            root.put(week, weekJson)
        }
        return root
    }

    private fun decodeState(json: JSONObject): GameState {
        val state = GameState()
        json.optJSONArray("thoughts")?.let { entries -> (0 until entries.length()).forEach { index ->
            val entry = entries.optJSONObject(index) ?: return@forEach
            com.healersjourney.app.domain.Thoughts.save(state, entry.optString("date"), entry.optString("text"))
            val savedIndex = state.thoughts.indexOfFirst { it.date == entry.optString("date") }
            if (savedIndex >= 0 && entry.optString("id").isNotBlank()) state.thoughts[savedIndex] = state.thoughts[savedIndex].copy(id = entry.getString("id"))
        } }
        PlanningJson.restore(state, json.optJSONObject("planning"))
        state.dayResetHour = json.optInt("dayResetHour", 5).coerceIn(0, 23)
        state.playerName = json.optString("playerName", state.playerName)
        // Existing saves predate onboarding and should open without an extra setup step.
        state.onboardingCompleted = json.optBoolean("onboardingCompleted", true)
        state.worldUnlocks.addAll(stringArray(json.optJSONArray("worldUnlocks")))
        json.optJSONObject("worldEquipment")?.let { equipment -> equipment.keys().forEach { key -> state.worldEquipment[key] = equipment.optString(key) } }
        state.coins = json.optInt("coins").coerceAtLeast(0)
        state.coinRewardDays.addAll(stringArray(json.optJSONArray("coinRewardDays")))
        state.coinAwardedActionIds.addAll(stringArray(json.optJSONArray("coinAwardedActionIds")))
        state.coinDebt = json.optInt("coinDebt").coerceAtLeast(0)
        state.adventureClaims.addAll(stringArray(json.optJSONArray("adventureClaims")))
        state.dailyQuestDate = json.optString("dailyQuestDate")
        state.dailyQuestIds.addAll(stringArray(json.optJSONArray("dailyQuestIds")))
        state.soundEnabled = json.optBoolean("soundEnabled", true)
        state.hapticsEnabled = json.optBoolean("hapticsEnabled", true)
        state.totalXp = json.optInt("totalXp")
        state.streak = json.optInt("streak")
        state.lastSleepHours = json.optDouble("lastSleepHours", state.lastSleepHours)
        state.knowledgeXp = json.optInt("knowledgeXp")
        state.medicineXp = json.optInt("medicineXp")
        state.strengthXp = json.optInt("strengthXp")
        state.socialXp = json.optInt("socialXp")
        state.hobbiesXp = json.optInt("hobbiesXp")
        state.bankBalance = json.optDouble("bankBalance")
        state.savings = json.optDouble("savings")
        state.emergencyFund = json.optDouble("emergencyFund")
        state.investments = json.optDouble("investments")
        state.savingsGoal = json.optDouble("savingsGoal")
        state.bankGoal = json.optDouble("bankGoal")
        state.emergencyGoal = json.optDouble("emergencyGoal")
        state.investmentsGoal = json.optDouble("investmentsGoal")
        state.decayStartDate = json.optString("decayStartDate")
        state.personalCareResetBaseline = json.optBoolean("personalCareResetBaseline", true)
        state.todoDate = json.optString("todoDate")
        state.todoViewOffset = json.optInt("todoViewOffset")
        state.todoViewMode = json.optString("todoViewMode", "today")

        state.hobbies.clear()
        state.hobbies.addAll(stringArray(json.optJSONArray("hobbies")))

        state.taskLog.addAll(decodeLogs(json.optJSONArray("taskLog")))
        state.financeLog.addAll(decodeLogs(json.optJSONArray("financeLog")))

        state.studyDates.clear()
        state.studyDates.addAll(stringArray(json.optJSONArray("studyDates")))
        state.workoutDates.clear()
        state.workoutDates.addAll(stringArray(json.optJSONArray("workoutDates")))

        json.optJSONObject("studyHoursByDate")?.let { hours ->
            hours.keys().forEach { key -> state.studyHoursByDate[key] = hours.optDouble(key) }
        }

        json.optJSONObject("weeklyAttrXp")?.let { weekly ->
            weekly.keys().forEach { week ->
                val bucket = mutableMapOf<String, Int>()
                weekly.optJSONObject(week)?.let { skills ->
                    skills.keys().forEach { skill -> bucket[skill] = skills.optInt(skill) }
                }
                state.weeklyAttrXp[week] = bucket
            }
        }

        json.optJSONArray("actionHistory")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                state.actionHistory.add(
                    ActionEntry(
                        id = o.optString("id"),
                        ts = o.optString("ts"),
                        date = o.optString("date"),
                        week = o.optString("week"),
                        month = o.optString("month"),
                        skill = o.optString("skill"),
                        xp = o.optInt("xp"),
                        baseXp = o.optInt("baseXp"),
                        decayPercent = o.optInt("decayPercent"),
                        type = o.optString("type"),
                        name = o.optString("name"),
                        originalName = o.optString("originalName").ifEmpty { null },
                        hours = o.optDouble("hours"),
                        hobby = o.optString("hobby").ifEmpty { null },
                        monthlyInvestmentAmount = o.optDouble("monthlyInvestmentAmount"),
                        careKey = o.optString("careKey").ifEmpty { null },
                        previousCareLast = o.optString("previousCareLast").ifEmpty { null },
                        newCareLast = o.optString("newCareLast").ifEmpty { null },
                        activityId = if (o.isNull("activityId")) null else o.optString("activityId").ifEmpty { null }
                    )
                )
            }
        }

        json.optJSONObject("activityDefs")?.let { defs ->
            defs.keys().forEach { skill ->
                val lists = defs.optJSONObject(skill) ?: return@forEach
                state.activityDefs[skill] = ActivityList(
                    major = decodeDefs(lists.optJSONArray("major")),
                    other = decodeDefs(lists.optJSONArray("other"))
                )
            }
        }

        json.optJSONArray("customAchievements")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                state.customAchievements.add(
                    CustomAchievement(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        group = o.optString("group", "Overall"),
                        metric = o.optString("metric", "totalXp"),
                        target = o.optInt("target"),
                        createdAt = o.optString("createdAt")
                    )
                )
            }
        }

        json.optJSONArray("milestones")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                state.milestones.add(
                    Milestone(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        desc = o.optString("desc"),
                        completed = o.optBoolean("completed"),
                        completedDate = o.optString("completedDate"),
                        showOnMain = o.optBoolean("showOnMain", true),
                        image = o.optString("image")
                    )
                )
            }
        }

        json.optJSONArray("removedActivities")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                state.removedActivities.add(
                    RemovedActivity(
                        skill = o.optString("skill"),
                        id = o.optString("id").ifEmpty { null },
                        name = o.optString("name"),
                        xp = o.optInt("xp"),
                        removedAt = o.optString("removedAt")
                    )
                )
            }
        }

        json.optJSONArray("careRoutines")?.let { arr ->
            state.careRoutinesConfigured = true
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val key = item.optString("key").trim()
                val label = item.optString("label").trim().take(60)
                if (key.isNotEmpty() && label.isNotEmpty() && state.careRoutines.none { it.key == key }) {
                    state.careRoutines.add(com.healersjourney.app.domain.Rules.CareRule(key, label,
                        item.optString("emoji", ""), item.optInt("cycleDays", 1).coerceIn(1, 365),
                        item.optInt("weight", 2).coerceIn(1, 10)))
                }
            }
        }
        state.careDefaultsVersion = json.optInt("careDefaultsVersion", 0)
        json.optJSONObject("personalCareLast")?.let { care ->
            care.keys().forEach { key -> state.personalCareLast[key] = care.optString(key) }
        }

        json.optJSONArray("todoTasks")?.let { arr -> state.todoTasks.addAll(decodeTodos(arr)) }

        json.optJSONObject("todoHistory")?.let { history ->
            history.keys().forEach { date ->
                history.optJSONArray(date)?.let { arr ->
                    state.todoHistory[date] = decodeTodos(arr).toMutableList()
                }
            }
        }

        return state
    }

    private fun stringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotEmpty() }
    }

    private fun decodeLogs(arr: JSONArray?): List<LogEntry> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            LogEntry(o.optString("id"), o.optString("text"), o.optString("actionId").ifEmpty { null })
        }
    }

    private fun decodeDefs(arr: JSONArray?): MutableList<ActivityDef> {
        if (arr == null) return mutableListOf()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            ActivityDef(o.optString("id"), o.optString("name"), o.optInt("xp"))
        }.toMutableList()
    }

    private fun decodeTodos(arr: JSONArray): List<TodoTask> =
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            TodoTask(
                id = o.optString("id"),
                name = o.optString("name"),
                skill = o.optString("skill"),
                xp = o.optInt("xp"),
                date = o.optString("date"),
                completed = o.optBoolean("completed"),
                completedAt = o.optString("completedAt")
            )
        }
}
