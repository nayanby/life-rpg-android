package com.healersjourney.app.domain

data class Achievement(
    val group: String,
    val name: String,
    val desc: String,
    val unlocked: Boolean,
    val custom: Boolean = false,
    val id: String = "",
    val current: Double = if(unlocked) 1.0 else 0.0,
    val target: Double = 1.0
)

object Achievements {

    fun lifetimeActions(state: GameState, skill: String) =
        state.actionHistory.filter { it.skill == skill }

    fun lifetimeActionCount(state: GameState, skill: String) = lifetimeActions(state, skill).size

    fun lifetimeStudyHours(state: GameState): Double =
        state.studyHoursByDate.values.sum()

    fun currentWeekStudyHours(state: GameState): Double {
        val wk = Dates.currentWeekKeys().toSet()
        return state.studyHoursByDate.filterKeys { wk.contains(it) }.values.sum()
    }

    fun currentWeekWorkoutDays(state: GameState): Int {
        val wk = Dates.currentWeekKeys().toSet()
        return state.workoutDates.count { wk.contains(it) }
    }

    fun hobbyActionCountByName(state: GameState, text: String): Int {
        val t = text.lowercase()
        return lifetimeActions(state, "hobbiesXp")
            .count { (it.hobby ?: it.name).lowercase().contains(t) }
    }

    fun activityActionCount(state: GameState, skill: String, text: String): Int {
        val t = text.lowercase()
        return lifetimeActions(state, skill)
            .count { (it.name.ifEmpty { it.hobby ?: "" }).lowercase().contains(t) }
    }

    fun financeGoalReached(value: Double, goal: Double): Boolean = goal > 0 && value >= goal

    private fun weeklyBucket(state: GameState): Map<String, Int> =
        state.weeklyAttrXp[Dates.weekKeyOfNow()] ?: emptyMap()

    fun customMetricValue(state: GameState, metric: String): Double {
        val sleep = SleepStats.of(state)
        val weekly = weeklyBucket(state)
        return when (metric) {
            "totalXp" -> state.totalXp.toDouble()
            "level" -> Rules.level(state.totalXp).toDouble()
            "streak" -> state.streak.toDouble()
            "knowledgeXp" -> state.knowledgeXp.toDouble()
            "medicineXp" -> state.medicineXp.toDouble()
            "strengthXp" -> state.strengthXp.toDouble()
            "socialXp" -> state.socialXp.toDouble()
            "hobbiesXp" -> state.hobbiesXp.toDouble()
            "weeklyKnowledgeXp" -> (weekly["knowledgeXp"] ?: 0).toDouble()
            "weeklyMedicineXp" -> (weekly["medicineXp"] ?: 0).toDouble()
            "weeklyStrengthXp" -> (weekly["strengthXp"] ?: 0).toDouble()
            "weeklySocialXp" -> (weekly["socialXp"] ?: 0).toDouble()
            "weeklyHobbiesXp" -> (weekly["hobbiesXp"] ?: 0).toDouble()
            "studyHours" -> lifetimeStudyHours(state)
            "weeklyStudyHours" -> currentWeekStudyHours(state)
            "workoutDays" -> state.workoutDates.size.toDouble()
            "weeklyWorkoutDays" -> currentWeekWorkoutDays(state).toDouble()
            "hobbySessions" -> lifetimeActionCount(state, "hobbiesXp").toDouble()
            "musicSessions" -> hobbyActionCountByName(state, "music").toDouble()
            "chessSessions" -> hobbyActionCountByName(state, "chess").toDouble()
            "sleepHours" -> sleep.lifetimeHours
            "weeklySleepHours" -> sleep.weeklyHours
            "bankBalance" -> state.bankBalance
            "savings" -> state.savings
            "emergencyFund" -> state.emergencyFund
            "investments" -> state.investments
            else -> 0.0
        }
    }

    val METRIC_LABELS = mapOf(
        "totalXp" to "Total XP", "level" to "Level", "streak" to "Action streak",
        "knowledgeXp" to "Study XP", "medicineXp" to "Work XP",
        "strengthXp" to "Fitness XP", "socialXp" to "Social XP", "hobbiesXp" to "Hobbies XP",
        "weeklyKnowledgeXp" to "Weekly Study XP", "weeklyMedicineXp" to "Weekly Work XP",
        "weeklyStrengthXp" to "Weekly Fitness XP", "weeklySocialXp" to "Weekly Social XP",
        "weeklyHobbiesXp" to "Weekly Hobbies XP",
        "studyHours" to "Lifetime study hours", "weeklyStudyHours" to "Current week study hours",
        "workoutDays" to "Lifetime workout days", "weeklyWorkoutDays" to "Current week workout days",
        "hobbySessions" to "Lifetime hobby sessions", "musicSessions" to "Music sessions",
        "chessSessions" to "Chess sessions", "sleepHours" to "Lifetime sleep hours",
        "weeklySleepHours" to "Current week sleep hours", "bankBalance" to "Bank balance",
        "savings" to "Savings", "emergencyFund" to "Emergency fund", "investments" to "Investments"
    )

    fun metricLabel(metric: String): String = METRIC_LABELS[metric] ?: metric

    fun customList(state: GameState): List<Achievement> = state.customAchievements.map { a ->
        val value = customMetricValue(state, a.metric)
        Achievement(
            group = Rules.categoryLabel(a.group),
            name = a.name,
            desc = "${metricLabel(a.metric)} ${Rules.formatMetricValue(value)} / ${Rules.formatMetricValue(a.target.toDouble())}.",
            unlocked = value >= a.target.toDouble(),
            custom = true,
            id = a.id, current = value, target = a.target.toDouble()
        )
    }

    fun all(state: GameState): List<Achievement> {
        val sleep = SleepStats.of(state)
        val studyHours = lifetimeStudyHours(state)
        val history = state.actionHistory
        val totalLoggedActivities = history.size
        val uniqueActionDays = history.map { it.date }.filter { it.isNotEmpty() }.distinct().size

        fun skillActionCount(skill: String) = history.count { it.skill == skill }
        fun skillDayCount(skill: String) =
            history.filter { it.skill == skill }.map { it.date }.filter { it.isNotEmpty() }.distinct().size
        fun countActivity(skill: String, terms: List<String>) =
            history.filter { it.skill == skill }.count { a ->
                val name = (a.name.ifEmpty { a.hobby ?: a.type }).lowercase()
                terms.any { name.contains(it.lowercase()) }
            }

        val knowledgeActions = skillActionCount("knowledgeXp")
        val knowledgeDays = skillDayCount("knowledgeXp")
        val medActions = skillActionCount("medicineXp")
        val medDays = skillDayCount("medicineXp")
        val wardRounds = countActivity("medicineXp", listOf("ward", "round"))
        val casesLearned = countActivity("medicineXp", listOf("case", "clinical case"))
        val strengthActions = skillActionCount("strengthXp")
        val strengthDays = skillDayCount("strengthXp")
        val workoutActions = countActivity("strengthXp", listOf("workout", "exercise", "fitness", "gym", "walk"))
        val socialActions = skillActionCount("socialXp")
        val socialDays = skillDayCount("socialXp")
        val meetFriendActions = countActivity("socialXp", listOf("meet friend", "meet with friend", "friend"))
        val callFamilyActions = countActivity("socialXp", listOf("call family", "family call", "family"))
        val birthdayActions = countActivity("socialXp", listOf("birthday", "celebrate birthday"))
        val hobbyActions = skillActionCount("hobbiesXp")
        val hobbyDays = skillDayCount("hobbiesXp")
        val musicSessions = countActivity("hobbiesXp", listOf("music"))
        val chessSessions = countActivity("hobbiesXp", listOf("chess"))
        val hobbyCount = state.hobbies.size
        val financeChanges = state.financeLog.size
        val savings = state.savings
        val bankBalance = state.bankBalance
        val emergencyFund = state.emergencyFund
        val investments = state.investments

        val base = mutableListOf<Achievement>()
        fun add(group: String, name: String, desc: String, unlocked: Boolean, current: Double = if(unlocked) 1.0 else 0.0, target: Double = 1.0) =
            base.add(Achievement(group, name, desc, unlocked, current = current, target = target))

        // Overall and consistency
        add("Overall", "First Step Taken", "Complete your first logged activity.", totalLoggedActivities >= 1, totalLoggedActivities.toDouble(), 1.0)
        add("Overall", "Ten Actions Logged", "Complete 10 total activities across the game.", totalLoggedActivities >= 10, totalLoggedActivities.toDouble(), 10.0)
        add("Overall", "Daily Momentum", "Complete 25 total activities across the game.", totalLoggedActivities >= 25, totalLoggedActivities.toDouble(), 25.0)
        add("Overall", "Habit Engine", "Complete 50 total activities across the game.", totalLoggedActivities >= 50, totalLoggedActivities.toDouble(), 50.0)
        add("Overall", "Life Builder", "Complete 100 total activities across the game.", totalLoggedActivities >= 100, totalLoggedActivities.toDouble(), 100.0)
        add("Overall", "System Keeper", "Complete 250 total activities across the game.", totalLoggedActivities >= 250, totalLoggedActivities.toDouble(), 250.0)
        add("Overall", "Three-Day Flame", "Maintain a 3-action streak.", state.streak >= 3, state.streak.toDouble(), 3.0)
        add("Overall", "Seven-Day Flame", "Maintain a 7-action streak.", state.streak >= 7, state.streak.toDouble(), 7.0)
        add("Overall", "Thirty-Day Flame", "Maintain a 30-action streak.", state.streak >= 30, state.streak.toDouble(), 30.0)
        add("Overall", "Hundred-Day Flame", "Maintain a 100-action streak.", state.streak >= 100, state.streak.toDouble(), 100.0)
        add("Overall", "Active Today", "Log at least one activity today.", uniqueActionDays >= 1, uniqueActionDays.toDouble(), 1.0)
        add("Overall", "Fifty Active Days", "Be active on 50 different days.", uniqueActionDays >= 50, uniqueActionDays.toDouble(), 50.0)
        add("Overall", "Hundred Active Days", "Be active on 100 different days.", uniqueActionDays >= 100, uniqueActionDays.toDouble(), 100.0)

        // Knowledge
        add("Study", "First Lesson", "Complete your first study activity.", knowledgeActions >= 1, knowledgeActions.toDouble(), 1.0)
        add("Study", "Scholar in Training", "Complete 10 study activities.", knowledgeActions >= 10, knowledgeActions.toDouble(), 10.0)
        add("Study", "Study Machine", "Complete 25 study activities.", knowledgeActions >= 25, knowledgeActions.toDouble(), 25.0)
        add("Study", "Study Keeper", "Complete 50 study activities.", knowledgeActions >= 50, knowledgeActions.toDouble(), 50.0)
        add("Study", "Master Scholar", "Complete 100 study activities.", knowledgeActions >= 100, knowledgeActions.toDouble(), 100.0)
        add("Study", "First Study Day", "Log study on one day.", knowledgeDays >= 1, knowledgeDays.toDouble(), 1.0)
        add("Study", "Study Week", "Log study on 7 different days.", knowledgeDays >= 7, knowledgeDays.toDouble(), 7.0)
        add("Study", "Study Month", "Log study on 30 different days.", knowledgeDays >= 30, knowledgeDays.toDouble(), 30.0)

        // Medicine
        add("Work", "First Work Step", "Complete your first work activity.", medActions >= 1, medActions.toDouble(), 1.0)
        add("Work", "Getting Things Done", "Complete 10 work activities.", medActions >= 10, medActions.toDouble(), 10.0)
        add("Work", "Problem Solver", "Complete 25 work activities.", medActions >= 25, medActions.toDouble(), 25.0)
        add("Work", "Reliable Professional", "Complete 50 work activities.", medActions >= 50, medActions.toDouble(), 50.0)
        add("Work", "Work Master", "Complete 100 work activities.", medActions >= 100, medActions.toDouble(), 100.0)
        add("Work", "First Focus Session", "Complete one focused work session.", wardRounds >= 1, wardRounds.toDouble(), 1.0)
        add("Work", "Focus Habit", "Complete 10 focused work sessions.", wardRounds >= 10, wardRounds.toDouble(), 10.0)
        add("Work", "First Milestone", "Complete one project milestone.", casesLearned >= 1, casesLearned.toDouble(), 1.0)
        add("Work", "Project Builder", "Complete 10 project milestones.", casesLearned >= 10, casesLearned.toDouble(), 10.0)
        add("Work", "Project Expert", "Complete 25 project milestones.", casesLearned >= 25, casesLearned.toDouble(), 25.0)
        add("Work", "Work Week", "Log work on 7 different days.", medDays >= 7, medDays.toDouble(), 7.0)

        // Strength
        add("Fitness", "First Movement", "Complete your first fitness activity.", strengthActions >= 1, strengthActions.toDouble(), 1.0)
        add("Fitness", "Mover", "Complete 10 fitness activities.", strengthActions >= 10, strengthActions.toDouble(), 10.0)
        add("Fitness", "Body Builder", "Complete 25 fitness activities.", strengthActions >= 25, strengthActions.toDouble(), 25.0)
        add("Fitness", "Iron Habit", "Complete 50 fitness activities.", strengthActions >= 50, strengthActions.toDouble(), 50.0)
        add("Fitness", "Fitness Legend", "Complete 100 fitness activities.", strengthActions >= 100, strengthActions.toDouble(), 100.0)
        add("Fitness", "First Workout", "Complete one workout.", workoutActions >= 1, workoutActions.toDouble(), 1.0)
        add("Fitness", "Workout Week", "Complete 7 workouts.", workoutActions >= 7, workoutActions.toDouble(), 7.0)
        add("Fitness", "Workout Month", "Complete 30 workouts.", workoutActions >= 30, workoutActions.toDouble(), 30.0)
        add("Fitness", "Fitness Week", "Log fitness on 7 different days.", strengthDays >= 7, strengthDays.toDouble(), 7.0)

        // Social
        add("Social", "First Connection", "Complete your first social activity.", socialActions >= 1, socialActions.toDouble(), 1.0)
        add("Social", "Friendly Presence", "Complete 10 social activities.", socialActions >= 10, socialActions.toDouble(), 10.0)
        add("Social", "Community Builder", "Complete 25 social activities.", socialActions >= 25, socialActions.toDouble(), 25.0)
        add("Social", "Social Anchor", "Complete 50 social activities.", socialActions >= 50, socialActions.toDouble(), 50.0)
        add("Social", "First Friend Meeting", "Meet a friend once.", meetFriendActions >= 1, meetFriendActions.toDouble(), 1.0)
        add("Social", "Friend Circle", "Meet friends 10 times.", meetFriendActions >= 10, meetFriendActions.toDouble(), 10.0)
        add("Social", "Family Line Open", "Call family 5 times.", callFamilyActions >= 5, callFamilyActions.toDouble(), 5.0)
        add("Social", "Roots Keeper", "Call family 10 times.", callFamilyActions >= 10, callFamilyActions.toDouble(), 10.0)
        add("Social", "Family Anchor", "Call family 50 times.", callFamilyActions >= 50, callFamilyActions.toDouble(), 50.0)
        add("Social", "Home Pillar", "Call family 100 times.", callFamilyActions >= 100, callFamilyActions.toDouble(), 100.0)
        add("Social", "First Celebration", "Celebrate a birthday once.", birthdayActions >= 1, birthdayActions.toDouble(), 1.0)
        add("Social", "Memory Maker", "Celebrate 5 birthdays.", birthdayActions >= 5, birthdayActions.toDouble(), 5.0)
        add("Social", "Joy Keeper", "Celebrate 10 birthdays.", birthdayActions >= 10, birthdayActions.toDouble(), 10.0)
        add("Social", "Celebration Circle", "Celebrate 20 birthdays.", birthdayActions >= 20, birthdayActions.toDouble(), 20.0)
        add("Social", "Festival Heart", "Celebrate 50 birthdays.", birthdayActions >= 50, birthdayActions.toDouble(), 50.0)

        // Hobbies
        add("Hobbies", "Hobby Door Opened", "Complete your first hobby session.", hobbyActions >= 1, hobbyActions.toDouble(), 1.0)
        add("Hobbies", "Hobby Explorer", "Complete 10 hobby sessions.", hobbyActions >= 10, hobbyActions.toDouble(), 10.0)
        add("Hobbies", "Hobby Builder", "Complete 25 hobby sessions.", hobbyActions >= 25, hobbyActions.toDouble(), 25.0)
        add("Hobbies", "Hobby Keeper", "Complete 50 hobby sessions.", hobbyActions >= 50, hobbyActions.toDouble(), 50.0)
        add("Hobbies", "Hobby Master", "Complete 100 hobby sessions.", hobbyActions >= 100, hobbyActions.toDouble(), 100.0)
        add("Hobbies", "First Song Session", "Complete your first music session.", musicSessions >= 1, musicSessions.toDouble(), 1.0)
        add("Hobbies", "Music Beginner", "Complete 5 music sessions.", musicSessions >= 5, musicSessions.toDouble(), 5.0)
        add("Hobbies", "Rhythm Keeper", "Complete 10 music sessions.", musicSessions >= 10, musicSessions.toDouble(), 10.0)
        add("Hobbies", "Melody Builder", "Complete 25 music sessions.", musicSessions >= 25, musicSessions.toDouble(), 25.0)
        add("Hobbies", "First Chess Session", "Complete your first chess session.", chessSessions >= 1, chessSessions.toDouble(), 1.0)
        add("Hobbies", "Chess Trainee", "Complete 5 chess sessions.", chessSessions >= 5, chessSessions.toDouble(), 5.0)
        add("Hobbies", "Tactical Thinker", "Complete 10 chess sessions.", chessSessions >= 10, chessSessions.toDouble(), 10.0)
        add("Hobbies", "Board Strategist", "Complete 25 chess sessions.", chessSessions >= 25, chessSessions.toDouble(), 25.0)
        add("Hobbies", "Hobby Shelf", "Keep 3 hobbies in your hobby list.", hobbyCount >= 3, hobbyCount.toDouble(), 3.0)
        add("Hobbies", "Creative Toolkit", "Keep 5 hobbies in your hobby list.", hobbyCount >= 5, hobbyCount.toDouble(), 5.0)
        add("Hobbies", "Passion Library", "Keep 10 hobbies in your hobby list.", hobbyCount >= 10, hobbyCount.toDouble(), 10.0)

        // Sleep
        add("Sleep", "Sleep Logger", "Log sleep once.", sleep.entries >= 1, sleep.entries.toDouble(), 1.0)
        add("Sleep", "Sleep Repeater", "Log sleep 3 times.", sleep.entries >= 3, sleep.entries.toDouble(), 3.0)
        add("Sleep", "Sleep Consistency", "Log sleep 7 times.", sleep.entries >= 7, sleep.entries.toDouble(), 7.0)
        add("Sleep", "Sleep Tracker", "Log sleep 14 times.", sleep.entries >= 14, sleep.entries.toDouble(), 14.0)
        add("Sleep", "Sleep Month Builder", "Log sleep 20 times this month.", sleep.monthlyEntries >= 20, sleep.monthlyEntries.toDouble(), 20.0)
        add("Sleep", "Hundred-Hour Rest", "Log 100 lifetime sleep hours.", sleep.lifetimeHours >= 100, sleep.lifetimeHours.toDouble(), 100.0)
        add("Sleep", "Two-Hundred-Hour Rest", "Log 200 lifetime sleep hours.", sleep.lifetimeHours >= 200, sleep.lifetimeHours.toDouble(), 200.0)
        add("Sleep", "Balanced Sleep Week", "Log at least 49 sleep hours this week.", sleep.weeklyHours >= 49, sleep.weeklyHours.toDouble(), 49.0)
        add("Sleep", "Seven-Night Record", "Log sleep on 7 different entries.", sleep.entries >= 7, sleep.entries.toDouble(), 7.0)
        add("Sleep", "Fourteen-Night Record", "Log sleep on 14 different entries.", sleep.entries >= 14, sleep.entries.toDouble(), 14.0)
        add("Sleep", "Rest Habit", "Log sleep 30 times.", sleep.entries >= 30, sleep.entries.toDouble(), 30.0)
        add("Sleep", "Rest Discipline", "Log sleep 60 times.", sleep.entries >= 60, sleep.entries.toDouble(), 60.0)

        // Finance
        add("Finance", "Finance Note Started", "Add your first finance log entry.", financeChanges >= 1, financeChanges.toDouble(), 1.0)
        add("Finance", "Bank Balance Logged", "Enter a bank balance above ₹0.", bankBalance > 0)
        add("Finance", "Bank Goal Achieved", "Bank balance reaches your bank goal.", financeGoalReached(bankBalance, state.bankGoal))
        add("Finance", "Savings Spark", "Reach ₹50,000 savings.", savings >= 50000, savings.toDouble(), 50000.0)
        add("Finance", "Reserve Builder", "Reach ₹1,00,000 savings.", savings >= 100000, savings.toDouble(), 100000.0)
        add("Finance", "Savings Shield", "Reach ₹1,50,000 savings.", savings >= 150000, savings.toDouble(), 150000.0)
        add("Finance", "Savings Stronghold", "Reach ₹2,00,000 savings.", savings >= 200000, savings.toDouble(), 200000.0)
        add("Finance", "Savings Goal Achieved", "Savings reach your savings goal.", financeGoalReached(savings, state.savingsGoal))
        add("Finance", "Safety Anchor", "Reach ₹1,00,000 emergency fund.", emergencyFund >= 100000, emergencyFund.toDouble(), 100000.0)
        add("Finance", "Crisis Wall", "Reach ₹2,00,000 emergency fund.", emergencyFund >= 200000, emergencyFund.toDouble(), 200000.0)
        add("Finance", "Emergency Fortress", "Reach ₹3,00,000 emergency fund.", emergencyFund >= 300000, emergencyFund.toDouble(), 300000.0)
        add("Finance", "Emergency Ready", "Emergency fund reaches your emergency goal.", financeGoalReached(emergencyFund, state.emergencyGoal))
        add("Finance", "Investment Seed", "Reach ₹1,00,000 investments.", investments >= 100000, investments.toDouble(), 100000.0)
        add("Finance", "Portfolio Builder", "Reach ₹1,50,000 investments.", investments >= 150000, investments.toDouble(), 150000.0)
        add("Finance", "Wealth Engine", "Reach ₹2,00,000 investments.", investments >= 200000, investments.toDouble(), 200000.0)
        add("Finance", "Investor Vanguard", "Reach ₹5,00,000 investments.", investments >= 500000, investments.toDouble(), 500000.0)

        return base + customList(state)
    }

    val GROUP_ORDER = listOf("Overall", "Study", "Work", "Fitness", "Social", "Hobbies", "Sleep", "Finance")

    fun grouped(state: GameState): Map<String, List<Achievement>> {
        val all = all(state)
        val groups = all.groupBy { it.group }.toMutableMap()
        val ordered = linkedMapOf<String, List<Achievement>>()
        GROUP_ORDER.forEach { g -> groups.remove(g)?.let { ordered[g] = it } }
        groups.keys.sorted().forEach { g -> ordered[g] = groups.getValue(g) }
        return ordered
    }

    fun unlockedCount(state: GameState): Int = all(state).count { it.unlocked }

    /** Ranking score used to pick the most valuable unlocked achievements for the dashboard. */
    fun valueScore(a: Achievement): Double {
        val txt = "${a.name} ${a.desc}".lowercase()
        val numbers = Regex("\\d[\\d,]*").findAll(txt)
            .map { it.value.replace(",", "").toDoubleOrNull() ?: 0.0 }
            .toList()
        val biggest = numbers.maxOrNull() ?: 0.0
        val groupWeight = when (a.group) {
            "Finance" -> 9000.0
            "Overall" -> 8000.0
            "Work" -> 7600.0
            "Study" -> 7200.0
            "Fitness" -> 6800.0
            "Social" -> 6400.0
            "Hobbies" -> 6000.0
            "Sleep" -> 5600.0
            else -> 4000.0
        }
        var scaleScore = Math.log10(biggest + 1) * 1000
        if (Regex("lifetime|total|hundred|legend|master|relentless|consistency|collector|investment|emergency|savings").containsMatchIn(txt)) scaleScore += 1200
        if (Regex("50|100|150|200|300|400|500|1000").containsMatchIn(txt)) scaleScore += 700
        return groupWeight + scaleScore
    }

    fun topUnlocked(state: GameState, limit: Int = 8): List<Achievement> =
        all(state).filter { it.unlocked }.sortedByDescending { valueScore(it) }.take(limit)
}
