package com.healersjourney.app.domain

data class ActionEntry(
    val id: String,
    val ts: String,
    val date: String,
    val week: String,
    val month: String,
    val skill: String = "",
    val xp: Int = 0,
    val baseXp: Int = 0,
    val decayPercent: Int = 0,
    val type: String = "",
    val name: String = "",
    val originalName: String? = null,
    val hours: Double = 0.0,
    val hobby: String? = null,
    val monthlyInvestmentAmount: Double = 0.0,
    val careKey: String? = null,
    val previousCareLast: String? = null,
    val newCareLast: String? = null,
    val activityId: String? = null
)

data class LogEntry(
    val id: String,
    val text: String,
    val actionId: String? = null
)

data class TodoTask(
    val id: String,
    val name: String,
    val skill: String,
    val xp: Int,
    val date: String,
    val completed: Boolean = false,
    val completedAt: String = ""
)

data class ActivityDef(
    val id: String,
    val name: String,
    val xp: Int
)

data class ActivityList(
    val major: MutableList<ActivityDef> = mutableListOf(),
    val other: MutableList<ActivityDef> = mutableListOf()
)

data class CustomAchievement(
    val id: String,
    val name: String,
    val group: String,
    val metric: String,
    val target: Int,
    val createdAt: String
)

data class Milestone(
    val id: String,
    val title: String,
    val desc: String,
    val completed: Boolean = false,
    val completedDate: String = "",
    val showOnMain: Boolean = true,
    val image: String = ""
)

data class RemovedActivity(
    val skill: String,
    val id: String?,
    val name: String,
    val xp: Int,
    val removedAt: String
)

class GameState {
    val thoughts: MutableList<ThoughtEntry> = mutableListOf()
    val personalGoals: MutableList<PersonalGoal> = mutableListOf()
    val recurringTasks: MutableList<RecurringTask> = mutableListOf()
    val scheduledOccurrences: MutableSet<String> = mutableSetOf()
    var focusSession: FocusSession? = null
    var dayResetHour: Int = 5
    var playerName: String = "Nayan"
    var onboardingCompleted: Boolean = false
    var totalXp: Int = 0
    var streak: Int = 0
    var lastSleepHours: Double = 6.0

    var knowledgeXp: Int = 0
    var medicineXp: Int = 0
    var strengthXp: Int = 0
    var socialXp: Int = 0
    var hobbiesXp: Int = 0

    var bankBalance: Double = 0.0
    var savings: Double = 0.0
    var emergencyFund: Double = 0.0
    var investments: Double = 0.0
    var savingsGoal: Double = 0.0
    var bankGoal: Double = 0.0
    var emergencyGoal: Double = 0.0
    var investmentsGoal: Double = 0.0

    val hobbies: MutableList<String> = mutableListOf("Music", "Chess")

    val taskLog: MutableList<LogEntry> = mutableListOf()
    val financeLog: MutableList<LogEntry> = mutableListOf()

    val studyDates: MutableList<String> = mutableListOf()
    val workoutDates: MutableList<String> = mutableListOf()
    val studyHoursByDate: MutableMap<String, Double> = mutableMapOf()
    val weeklyAttrXp: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()

    val actionHistory: MutableList<ActionEntry> = mutableListOf()
    val activityDefs: MutableMap<String, ActivityList> = mutableMapOf()
    val customAchievements: MutableList<CustomAchievement> = mutableListOf()
    val milestones: MutableList<Milestone> = mutableListOf()
    val removedActivities: MutableList<RemovedActivity> = mutableListOf()
    val worldUnlocks: MutableSet<String> = mutableSetOf()
    val worldEquipment: MutableMap<String, String> = mutableMapOf()
    var coins: Int = 0
    val coinRewardDays: MutableSet<String> = mutableSetOf()
    val coinAwardedActionIds: MutableSet<String> = mutableSetOf()
    var coinDebt: Int = 0
    val adventureClaims: MutableSet<String> = mutableSetOf()
    var dailyQuestDate: String = ""
    val dailyQuestIds: MutableList<String> = mutableListOf()
    var soundEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    val careRoutines: MutableList<Rules.CareRule> = mutableListOf()
    var careRoutinesConfigured: Boolean = false
    var careDefaultsVersion: Int = 0
    val personalCareLast: MutableMap<String, String> = mutableMapOf()
    var personalCareResetBaseline: Boolean = true
    var decayStartDate: String = ""

    val todoTasks: MutableList<TodoTask> = mutableListOf()
    val todoHistory: MutableMap<String, MutableList<TodoTask>> = mutableMapOf()
    var todoDate: String = ""
    var todoViewOffset: Int = 0
    var todoViewMode: String = "today"

    fun skillXp(skill: String): Int = when (skill) {
        "knowledgeXp" -> knowledgeXp
        "medicineXp" -> medicineXp
        "strengthXp" -> strengthXp
        "socialXp" -> socialXp
        "hobbiesXp" -> hobbiesXp
        else -> 0
    }

    fun addSkillXp(skill: String, amount: Int) {
        when (skill) {
            "knowledgeXp" -> knowledgeXp += amount
            "medicineXp" -> medicineXp += amount
            "strengthXp" -> strengthXp += amount
            "socialXp" -> socialXp += amount
            "hobbiesXp" -> hobbiesXp += amount
        }
    }

    fun subtractSkillXp(skill: String, amount: Int) {
        when (skill) {
            "knowledgeXp" -> knowledgeXp = maxOf(0, knowledgeXp - amount)
            "medicineXp" -> medicineXp = maxOf(0, medicineXp - amount)
            "strengthXp" -> strengthXp = maxOf(0, strengthXp - amount)
            "socialXp" -> socialXp = maxOf(0, socialXp - amount)
            "hobbiesXp" -> hobbiesXp = maxOf(0, hobbiesXp - amount)
        }
    }
}
