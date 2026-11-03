package com.healersjourney.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import com.healersjourney.app.domain.*
import com.healersjourney.app.ui.*
import com.healersjourney.app.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun CategoryPicker(selected: String, onSelect: (String) -> Unit) {
    CategoryTags(Rules.SKILL_KEYS, selected, onSelect)
}

@Composable
fun CategoryTags(skills: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        skills.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { skill -> CategoryTag(skill, selected == skill, Modifier.weight(1f)) { onSelect(skill) } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun CategoryLabel(skill: String) {
    val accent = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(Rules.SKILL_COLORS[skill] ?: "#5E8C92"))
    Surface(shape = RoundedCornerShape(5.dp), color = accent.copy(alpha = .17f)) {
        Text(Rules.SKILL_LABELS[skill].orEmpty(), Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun CategoryTag(skill: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(Rules.SKILL_COLORS[skill] ?: "#5E8C92"))
    FilterChip(selected, onClick, modifier = modifier, label = { Text(Rules.SKILL_LABELS[skill] ?: "All", style = MaterialTheme.typography.labelSmall, maxLines = 1) },
        shape = RoundedCornerShape(7.dp),
        colors = FilterChipDefaults.filterChipColors(containerColor = accent.copy(alpha = .12f),
            selectedContainerColor = accent.copy(alpha = .28f)),
        border = BorderStroke(if (selected) 1.5.dp else .5.dp, accent.copy(alpha = if (selected) .9f else .3f)),
        leadingIcon = { Image(gameArtPainter(Art.skillIcon(skill)), null, Modifier.size(16.dp)) })
}

@Composable
fun WeekdayPicker(days: Set<Int>, onChange: (Set<Int>) -> Unit) {
    Text("Repeat on", style = MaterialTheme.typography.labelLarge)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, label ->
            val day = index + 1
            FilterChip(day in days, { onChange(if (day in days) days - day else days + day) },
                label = { Text(label) }, modifier = Modifier.width(40.dp))
        }
    }
}

@Composable
fun PersonalGoalsSection(snapshot: GameSnapshot, viewModel: GameViewModel, compact: Boolean = false) {
    val state = snapshot.state
    var editing by remember { mutableStateOf<PersonalGoal?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<PersonalGoal?>(null) }
    var commemorative by remember { mutableStateOf<PersonalGoal?>(null) }
    var filter by rememberSaveable { mutableStateOf("Active") }
    var menuId by remember { mutableStateOf<String?>(null) }
    val today = Dates.gameDayKey()
    val goals = state.personalGoals.sortedBy { it.end }.filter { goal ->
        val done = Planning.goalActions(state, goal).size >= goal.target
        if (compact) today in goal.start..goal.end else when (filter) {
            "Achieved" -> done
            "Past" -> goal.end < today && !done
            else -> goal.end >= today && !done
        }
    }
    SectionCard(title = "Personal goals", collapsible = compact, initiallyExpanded = !compact,
        trailing = { AppTextButton(onClick = { adding = true }) { Text("Add goal") } }) {
        if (!compact) {
            Text("Choose a target for the month. Log activities or mark a goal done today to move forward.", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Active", "Achieved", "Past").forEach { label -> FilterChip(filter == label, { filter = label }, label = { Text(label) }) }
            }
        }
        if (goals.isEmpty()) Text(if (state.personalGoals.isEmpty()) "Start small: exercise 12 times or read 4 chapters this month." else "No ${if (compact) "current" else filter.lowercase()} goals here yet.", style = MaterialTheme.typography.bodyMedium)
        goals.forEach { goal ->
            val actions = Planning.goalActions(state, goal)
            val count = actions.size
            val earned = count >= goal.target
            val checkedIn = state.actionHistory.any { it.activityId == Planning.goalCheckInId(goal.id, today) }
            val doneToday = actions.any { it.date == today }
            Surface(shape = RoundedCornerShape(18.dp), color = skillColor(goal.skill).copy(alpha = .07f),
                border = BorderStroke(1.dp, skillColor(goal.skill).copy(alpha = .18f))) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TrophyImage(goal.skill, Modifier.size(if (earned) 64.dp else 52.dp).clickable(enabled = earned) { commemorative = goal },
                    description = if (earned) "View earned goal trophy" else "Goal trophy in progress", alpha = if (earned) 1f else .7f)
                Column(Modifier.weight(1f).clickable { editing = goal }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(goal.title, style = MaterialTheme.typography.titleSmall)
                    Text("${Rules.SKILL_LABELS[goal.skill]} · ${goalMonthLabel(goal.start.take(7))}", style = MaterialTheme.typography.labelSmall)
                }
                Box {
                    IconButton(onClick = { menuId = goal.id }) { Icon(Icons.Filled.MoreVert, "Goal options") }
                    DropdownMenu(menuId == goal.id, { menuId = null }) {
                        DropdownMenuItem(text = { Text("Edit goal") }, onClick = { menuId = null; editing = goal })
                        DropdownMenuItem(text = { Text("Remove goal") }, onClick = { menuId = null; deleting = goal })
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${minOf(count, goal.target)} of ${goal.target}", style = MaterialTheme.typography.titleSmall)
                Text(if (earned) "Trophy earned" else "${(goal.target-count).coerceAtLeast(0)} to go", style = MaterialTheme.typography.labelLarge, color = skillColor(goal.skill))
            }
            SimpleProgress(progress = { count.toFloat() / goal.target }, color = skillColor(goal.skill), modifier = Modifier.fillMaxWidth())
            if (today in goal.start..goal.end) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (checkedIn) "Checked in today" else if (doneToday) "Activity counted today" else "0 XP · +10 coins", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                if (checkedIn) AppTextButton(onClick = { viewModel.run { Planning.undoGoalCheckIn(it, goal.id) } }) { Text("Undo today") }
                else AppTonalButton(enabled = !doneToday && !earned, onClick = { viewModel.run { Planning.checkInGoal(it, goal.id) } }) { Text(if (doneToday) "✓ Done today" else if (earned) "Achieved" else "Done today") }
            }
            }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
    if (adding || editing != null) GoalEditor(state, editing, onDismiss = { adding = false; editing = null }) { title, skill, activity, target, month ->
        viewModel.run { val result = Planning.saveGoal(it, editing?.id, title, skill, activity, target, month)
            if (result is Outcome.Applied) { adding = false; editing = null }; result }
    }
    commemorative?.let { goal ->
        val reached = Planning.goalActions(state, goal).sortedBy { it.ts }.getOrNull(goal.target - 1)
        AlertDialog(onDismissRequest = { commemorative = null }, title = { Text(goal.title) }, text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TrophyImage(goal.skill, Modifier.size(200.dp), description = "Commemorative goal trophy")
                Text("${goal.target} completions · ${goal.start.take(7)}", style = MaterialTheme.typography.titleMedium)
                Text(reached?.let { "Earned ${Dates.prettyCareDate(it.date)}" } ?: "Keep going to earn this trophy.")
            }
        }, confirmButton = { AppTextButton(onClick = { commemorative = null }) { Text("Close") } })
    }
    deleting?.let { goal -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Remove this goal?") },
        text = { Text("Your completed activities and rewards will stay.") }, confirmButton = { AppTextButton(onClick = {
            viewModel.runQuiet { it.personalGoals.removeAll { g -> g.id == goal.id } }; deleting = null
        }) { Text("Remove") } }, dismissButton = { AppTextButton(onClick = { deleting = null }) { Text("Keep") } }) }
}

private fun goalMonthLabel(month: String): String = "${Dates.monthName(month.takeLast(2).toIntOrNull() ?: 1)} ${month.take(4)}"

@Composable
private fun GoalEditor(state: GameState, goal: PersonalGoal?, onDismiss: () -> Unit,
                       onSave: (String, String, String, Int, String) -> Unit) {
    var title by rememberSaveable { mutableStateOf(goal?.title ?: "") }
    var skill by rememberSaveable { mutableStateOf(goal?.skill ?: "strengthXp") }
    var activity by rememberSaveable { mutableStateOf(goal?.activity ?: "") }
    var target by rememberSaveable { mutableStateOf(goal?.target?.toString() ?: "12") }
    var month by rememberSaveable { mutableStateOf(goal?.start?.take(7) ?: Dates.gameDayKey().take(7)) }
    var specific by rememberSaveable { mutableStateOf(goal?.activity?.isNotBlank() ?: false) }
    var activitySearch by rememberSaveable { mutableStateOf("") }
    val currentMonth = Dates.gameDayKey().take(7)
    val nextMonth = Dates.keyToCal("$currentMonth-01")!!.apply { add(java.util.Calendar.MONTH, 1) }.let { Dates.dateKeyOf(it).take(7) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (goal == null) "Set a personal goal" else "Your goal") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it.take(100) }, label = { Text("Goal name") }, singleLine = true)
            CategoryPicker(skill) { skill = it; activity = "" }
            OutlinedTextField(target, { target = it.filter(Char::isDigit).take(4) }, label = { Text("Number of completions") }, singleLine = true)
            Text("When", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf(currentMonth, nextMonth) + month).distinct().forEach { choice -> FilterChip(month == choice, { month = choice }, label = { Text(goalMonthLabel(choice)) }) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Count a specific activity", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(specific, { specific = it; if (!it) activity = "" })
            }
            if (specific) {
                OutlinedTextField(activity, { activity = it.take(100); activitySearch = it }, label = { Text("Activity name") }, singleLine = true)
                val defs = state.activityDefs[skill]
                (defs?.major.orEmpty() + defs?.other.orEmpty()).distinctBy { it.name }
                    .filter { it.name.contains(activitySearch, true) }.take(5).forEach { def ->
                    val label = Rules.activityLabel(skill, def.name)
                    Text(label, Modifier.fillMaxWidth().clickable { activity = label; activitySearch = label }.padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(if (specific) "Only this activity counts. Done today also logs this activity." else "All ${Rules.SKILL_LABELS[skill]} activities count toward your target.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { AppTextButton(enabled = title.isNotBlank() && (target.toIntOrNull() ?: 0) in 1..1000 && (!specific || activity.isNotBlank()), onClick = { onSave(title, skill, if (specific) activity else "", target.toIntOrNull() ?: 0, month) }) { Text("Save goal") } },
        dismissButton = { AppTextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
fun RecurringTasksSection(snapshot: GameSnapshot, viewModel: GameViewModel) {
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RecurringTask?>(null) }
    var deleting by remember { mutableStateOf<RecurringTask?>(null) }
    SectionCard("Recurring tasks", collapsible = true, initiallyExpanded = false,
        trailing = { AppTextButton(onClick = { adding = true }) { Text("Add") } }) {
        if (snapshot.state.recurringTasks.isEmpty()) Text("Choose weekdays and let Today fill itself. Nothing is marked done automatically.", style = MaterialTheme.typography.bodySmall)
        snapshot.state.recurringTasks.forEach { rule ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable { editing = rule }.padding(vertical = 8.dp)) {
                    Text(rule.name, style = MaterialTheme.typography.bodyMedium)
                    Text(rule.weekdays.sorted().joinToString(" · ") { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[it - 1] }, style = MaterialTheme.typography.labelSmall)
                }
                AppTextButton(onClick = { deleting = rule }) { Text("Remove") }
            }
        }
        Text("Tap a schedule to edit it. Changes affect open tasks from today onward; completed days stay unchanged.", style = MaterialTheme.typography.labelSmall)
    }
    if (adding || editing != null) RecurringEditor(editing, { adding = false; editing = null }) { name, skill, xp, days ->
        viewModel.run { val result = Planning.saveRecurring(it, editing?.id, name, skill, xp, days)
            if (result is Outcome.Applied) { adding = false; editing = null }; result }
    }
    deleting?.let { rule -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Stop repeating ${rule.name}?") },
        text = { Text("Open scheduled copies from today onward will be removed. Completed tasks and rewards stay.") },
        confirmButton = { AppTextButton(onClick = { viewModel.runQuiet { Planning.removeRecurring(it, rule.id) }; deleting = null }) { Text("Stop repeating") } },
        dismissButton = { AppTextButton(onClick = { deleting = null }) { Text("Keep") } }) }
}

@Composable
private fun RecurringEditor(rule: RecurringTask?, onDismiss: () -> Unit, onSave: (String, String, Int, Set<Int>) -> Unit) {
    var name by rememberSaveable { mutableStateOf(rule?.name ?: "") }
    var skill by rememberSaveable { mutableStateOf(rule?.skill ?: Rules.SKILL_KEYS.first()) }
    var xp by rememberSaveable { mutableStateOf((rule?.xp ?: 20).toString()) }
    var days by remember { mutableStateOf(rule?.weekdays ?: setOf(1, 2, 3, 4, 5)) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Recurring task") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it.take(100) }, label = { Text("Task name") }, singleLine = true)
            CategoryPicker(skill) { skill = it }
            ActivityXpField(xp) { xp = it }
            WeekdayPicker(days) { days = it }
            Text("Starts today. Each scheduled day gets one editable task with the usual rewards.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { AppTextButton(onClick = { onSave(name, skill, xp.toIntOrNull() ?: -1, days) }) { Text("Save schedule") } },
        dismissButton = { AppTextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun FocusCard(snapshot: GameSnapshot, viewModel: GameViewModel) {
    val session = snapshot.state.focusSession ?: return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var cancel by remember { mutableStateOf(false) }
    LaunchedEffect(session) { while (true) { now = System.currentTimeMillis(); delay(500) } }
    val remaining = session.remaining(now)
    val seconds = (remaining + 999) / 1000
    SectionCard("Focus session") {
        Text(Planning.task(snapshot.state, session)?.name ?: "Task no longer available", style = MaterialTheme.typography.titleMedium)
        Text("%02d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.displaySmall)
        SimpleProgress(progress = { 1f - remaining.toFloat() / session.durationMs }, modifier = Modifier.fillMaxWidth())
        Text(if (remaining == 0L) "Session finished. Complete your task to collect its usual rewards." else if (session.running) "One task at a time. Your timer keeps running when you leave this screen." else "Paused. Resume whenever you're ready.", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (remaining == 0L) AppButton(onClick = { viewModel.run { Planning.finishFocus(it) } }) { Text("Finish & complete task") }
            else AppTonalButton(onClick = { viewModel.runQuiet { if (session.running) Planning.pauseFocus(it) else Planning.resumeFocus(it) } }) { Text(if (session.running) "Pause" else "Resume") }
            AppTextButton(onClick = { cancel = true }) { Text("Cancel") }
        }
    }
    if (cancel) AlertDialog(onDismissRequest = { cancel = false }, title = { Text("Cancel this session?") }, text = { Text("The task stays open. No XP or coins are awarded.") },
        confirmButton = { AppTextButton(onClick = { viewModel.runQuiet { it.focusSession = null }; cancel = false }) { Text("Cancel session") } },
        dismissButton = { AppTextButton(onClick = { cancel = false }) { Text("Keep focusing") } })
}

@Composable
fun StartFocusDialog(task: TodoTask, viewModel: GameViewModel, onDismiss: () -> Unit) {
    var minutes by rememberSaveable { mutableStateOf("25") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Focus on ${task.name}") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(3) }, label = { Text("Minutes (1–180)") }, singleLine = true)
            Text("When time is up, finish the session to complete this task once, with its normal XP and coins.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { AppTextButton(onClick = { viewModel.run { val result = Planning.startFocus(it, task, minutes.toIntOrNull() ?: 0)
        if (result is Outcome.Applied) onDismiss(); result } }) { Text("Start focus") } }, dismissButton = { AppTextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun EditPlannedTaskDialog(task: TodoTask, viewModel: GameViewModel, onRemove: () -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable(task.id) { mutableStateOf(task.name) }
    var skill by rememberSaveable(task.id) { mutableStateOf(task.skill) }
    var xp by rememberSaveable(task.id) { mutableStateOf(task.xp.toString()) }
    var repeat by rememberSaveable { mutableStateOf(false) }
    var days by remember { mutableStateOf(setOf(Planning.weekday(task.date))) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (task.completed) "Completed task" else "Edit task") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (task.completed) Text("${task.name}\nUndo this task before editing its details.") else {
                OutlinedTextField(name, { name = it.take(100) }, label = { Text("Task name") }, singleLine = true)
                CategoryPicker(skill) { skill = it }
                ActivityXpField(xp) { xp = it }
                if (!task.id.startsWith("todo-repeat-")) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(repeat, { repeat = it }); Text("Repeat on selected weekdays") }
                    if (repeat) WeekdayPicker(days) { days = it }
                } else Text("This edits this day's copy. To change future days, edit its recurring schedule below the task list.", style = MaterialTheme.typography.bodySmall)
            }
            AppTextButton(onClick = onRemove) { Text("Remove task", color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { if (!task.completed) AppTextButton(onClick = {
        viewModel.run { state ->
            if (repeat && days.isEmpty()) Outcome.Failure("Choose at least one weekday.") else {
                val result = GameEngine.updateTodoTask(state, task.id, name, skill, xp.toIntOrNull() ?: -1)
                if (result is Outcome.Applied) {
                    if (repeat) Planning.saveRecurring(state, null, name, skill, xp.toInt(), days)
                    onDismiss()
                }; result
            }
        }
    }) { Text("Save") } }, dismissButton = { AppTextButton(onClick = onDismiss) { Text("Close") } })
}
