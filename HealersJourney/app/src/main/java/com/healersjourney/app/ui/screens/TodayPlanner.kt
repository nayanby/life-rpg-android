package com.healersjourney.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.healersjourney.app.domain.*
import com.healersjourney.app.ui.*
import com.healersjourney.app.ui.components.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class SavedChoice(
    val skill: String,
    val activity: ActivityDef,
    val uses: Int,
    val categoryOrder: Int,
    val sourceOrder: Int
)

@Composable
private fun CompactTodayCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

private fun savedChoices(state: GameState, category: String, query: String): Pair<List<SavedChoice>, List<SavedChoice>> {
    val countsById = state.actionHistory.asSequence().filter { it.skill in Rules.SKILL_KEYS && it.activityId != null }
        .groupingBy { it.skill to it.activityId }.eachCount()
    val countsByName = state.actionHistory.asSequence().filter { it.skill in Rules.SKILL_KEYS }
        .groupingBy { it.skill to Rules.activityLabel(it.skill, it.name).trim().lowercase() }.eachCount()
    val search = query.trim()
    val rankedByCategory = Rules.SKILL_KEYS.withIndex()
        .filter { category == "all" || category == it.value }
        .map { (categoryOrder, skill) ->
            val lists = state.activityDefs[skill]
            (lists?.major.orEmpty() + lists?.other.orEmpty()).withIndex()
                .mapNotNull { (sourceOrder, activity) ->
                    val label = Rules.activityLabel(skill, activity.name)
                    if (!label.contains(search, ignoreCase = true)) null else SavedChoice(
                        skill, activity,
                        maxOf(countsById[skill to activity.id] ?: 0,
                            countsByName[skill to label.trim().lowercase()] ?: 0),
                        categoryOrder, sourceOrder
                    )
                }
                .sortedWith(compareByDescending<SavedChoice> { it.uses }.thenBy { it.sourceOrder })
        }
    val featured = if (search.isEmpty()) rankedByCategory.mapNotNull { it.firstOrNull() } else emptyList()
    val remaining = rankedByCategory.flatMap { if (search.isEmpty()) it.drop(1) else it }
        .sortedWith(compareByDescending<SavedChoice> { it.uses }
            .thenBy { it.categoryOrder }.thenBy { it.sourceOrder })
    return featured to remaining
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoScreen(snapshot: GameSnapshot, viewModel: GameViewModel, initialSkill: String? = null,
               screenScroll: ScrollState, scrollViewportTopPx: Float, onOpenSleep: () -> Unit = {}) {
    val state = snapshot.state
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshPlanning()
            kotlinx.coroutines.delay(30_000)
        }
    }
    val view = remember(snapshot.revision) { GameEngine.getTodoListForView(state) }
    var adding by rememberSaveable(view.key, initialSkill) { mutableStateOf(initialSkill in Rules.SKILL_KEYS) }
    var mode by rememberSaveable { mutableStateOf("saved") }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable(initialSkill) { mutableStateOf(initialSkill?.takeIf { it in Rules.SKILL_KEYS } ?: "all") }
    var visibleExtra by rememberSaveable(view.key, query, category) { mutableIntStateOf(0) }
    var pickerButtonTopPx by remember { mutableIntStateOf(0) }
    val scrollScope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<Pair<String, ActivityDef>?>(null) }
    var newPreset by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<TodoTask?>(null) }
    var daySettings by remember { mutableStateOf(false) }
    var editTask by remember { mutableStateOf<TodoTask?>(null) }
    if (daySettings) AlertDialog(onDismissRequest = { daySettings = false }, title = { Text("Day starts at") },
        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            Text("Tasks, daily rewards and the growth graph follow this time. Existing history keeps its dates.")
            (0..23).forEach { hour ->
                Row(Modifier.fillMaxWidth().clickable {
                    viewModel.run { GameEngine.setDayResetHour(it, hour) }; daySettings = false
                }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(hour == state.dayResetHour, onClick = {
                        viewModel.run { GameEngine.setDayResetHour(it, hour) }; daySettings = false
                    })
                    Text("%02d:00".format(hour))
                }
            }
        } }, confirmButton = { TextButton(onClick = { daySettings = false }) { Text("Close") } })
    editTask?.let { task -> EditPlannedTaskDialog(task, viewModel, onRemove = { editTask = null; removing = task }) { editTask = null } }
    selected?.let { (skill, activity) -> PresetEditorDialog(skill, snapshot, viewModel, activity) { selected = null } }
    if (newPreset) PresetEditorDialog(category.takeIf { it in Rules.SKILL_KEYS } ?: Rules.SKILL_KEYS.first(), snapshot, viewModel, initialName = query.trim()) { newPreset = false }
    removing?.let { task -> AlertDialog(onDismissRequest = { removing = null }, title = { Text("Remove ${task.name}?") },
        text = { Text(if (task.completed) "This will also remove the XP, coins and trophy earned from this task." else "This task will be removed from your day.") },
        confirmButton = { AppTextButton(onClick = { viewModel.run { GameEngine.deleteTodoTask(it, task.id) }; removing = null }) { Text("Remove") } },
        dismissButton = { AppTextButton(onClick = { removing = null }) { Text("Keep") } }) }
    Column(Modifier.pointerInput(view.key) {
        var drag = 0f
        detectHorizontalDragGestures(onDragStart = { drag = 0f }, onHorizontalDrag = { change, amount -> change.consume(); drag += amount },
            onDragEnd = {
                if (drag > 72.dp.toPx()) viewModel.runQuiet { GameEngine.showPreviousTodoDay(it) }
                else if (drag < -72.dp.toPx()) viewModel.runQuiet { GameEngine.showNextTodoDay(it) }
            })
    }, verticalArrangement = Arrangement.spacedBy(AppLayout.sectionGap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(onClick = { viewModel.runQuiet { GameEngine.showPreviousTodoDay(it) } },
                modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous day", modifier = Modifier.size(22.dp))
            }
            AnimatedContent(targetState = view.key, modifier = Modifier.weight(1f),
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(300)) { it * direction } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(300)) { -it * direction } + fadeOut(tween(180)))
                }, label = "Day heading") { day ->
                val tasks = if (day == state.todoDate) state.todoTasks else state.todoHistory[day].orEmpty()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Dates.todoDisplayDate(day), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text("${tasks.count { it.completed }} / ${tasks.size} done · ${state.actionHistory.filter { it.date == day }.sumOf { it.xp }} XP · ${state.coins} coins", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
            FilledIconButton(onClick = { viewModel.runQuiet { GameEngine.showNextTodoDay(it) } },
                modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next day", modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = { daySettings = true }) { Icon(Icons.Filled.Settings, "Day reset settings") }
        }
        if (!view.viewingToday) AppTextButton(onClick = { viewModel.runQuiet { GameEngine.openTodoWindow(it) } }) { Text("Back to today") }
        if (initialSkill == "sleep") AppTonalButton(onClick = onOpenSleep) { Text("Open sleep tracker") }
        AnimatedContent(targetState = view.key,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                ((slideInHorizontally(tween(360)) { it * direction } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(360)) { -it * direction } + fadeOut(tween(200))))
                    .using(SizeTransform(clip = true))
            }, label = "Day shelf slide") { day ->
            DayShowcase(state, day, snapshot.revision)
        }
        if (view.list.isEmpty()) EmptyHint("Your day, your pace. Add a task or choose an activity you've saved.")
        else CompactTodayCard(title = "Today's tasks") {
            view.list.sortedBy { it.completed }.forEach { task ->
                val earnedCoins = state.actionHistory.firstOrNull { it.activityId == task.id }
                    ?.let { if (it.id in state.coinAwardedActionIds) 10 else 0 } ?: 0
                Row(Modifier.fillMaxWidth().height(60.dp).combinedClickable(onClick = { editTask = task }, onLongClickLabel = "Edit or remove task", onLongClick = { editTask = task }).padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(gameArtPainter(Art.skillIcon(task.skill)), null, Modifier.size(26.dp))
                    Column(Modifier.weight(1f)) {
                        Text(task.name, style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            maxLines=1, overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(if (task.completed) "${Rules.SKILL_LABELS[task.skill] ?: task.skill} · Completed${if (earnedCoins > 0) " · +$earnedCoins coins" else ""}"
                            else "${Rules.SKILL_LABELS[task.skill] ?: task.skill} · +${GameEngine.activityXpPreviewForDate(state, task.skill, task.xp, view.key).first} XP",
                            style = MaterialTheme.typography.labelSmall, maxLines=1)
                    }
                    if (task.completed) AppTextButton(onClick = { viewModel.run { GameEngine.undoTodoTask(it, task.id) } }) { Text("Undo") }
                    else {
                    AppTonalButton(onClick = { viewModel.run { GameEngine.completeTodoTask(it, task.id) } }) {
                        Text("Done")
                    }
                    }
                }
            }
        }
        if (!adding) AppButton(onClick = {
                mode = "saved"
                query = ""
                category = initialSkill?.takeIf { it in Rules.SKILL_KEYS } ?: "all"
                visibleExtra = 0
                adding = true
                scrollScope.launch {
                    withFrameNanos { }
                    screenScroll.animateScrollTo(pickerButtonTopPx.coerceAtLeast(0), androidx.compose.animation.core.tween(240))
                    // The picker expands during the first scroll, increasing the available range.
                    screenScroll.animateScrollTo(pickerButtonTopPx.coerceAtLeast(0), androidx.compose.animation.core.tween(160))
                }
        }, modifier = Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
            pickerButtonTopPx = (coordinates.positionInRoot().y - scrollViewportTopPx + screenScroll.value).roundToInt()
        }) { Text("Add to your day") }
        else Row(Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
            pickerButtonTopPx = (coordinates.positionInRoot().y - scrollViewportTopPx + screenScroll.value).roundToInt()
        }, verticalAlignment = Alignment.CenterVertically) {
            Text("Add to your day", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { adding = false }) {
                Icon(Icons.Filled.Close, contentDescription = "Close task picker")
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = adding,
            enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.tween(240)) +
                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(180)),
            exit = androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.tween(200)) +
                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(140))
        ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == "saved", onClick = { mode = "saved" }, label = { Text("Saved activities") })
                FilterChip(selected = mode == "new", onClick = { mode = "new" }, label = { Text("New task") })
            }
            if (mode == "new") NewTodayTask(viewModel, query.trim(), category) { adding = false }
            else CompactTodayCard(title = "Find an activity") {
                OutlinedTextField(query, { query = it }, label = { Text("Search saved activities") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                CategoryTags(listOf("all") + Rules.SKILL_KEYS, category) { category = it }
                val (featured, remaining) = remember(snapshot.revision, category, query) {
                    savedChoices(state, category, query)
                }
                val initialResults = if (query.isBlank()) 0 else 10
                val shownOthers = (initialResults + visibleExtra).coerceAtMost(remaining.size)
                val visible = featured + remaining.take(shownOthers)
                if (visible.isEmpty()) Text("No matching activities. Create a new task or save a new activity.")
                visible.forEach { (skill, activity, uses) ->
                    val name = Rules.activityLabel(skill, activity.name)
                    val added = view.list.any { it.skill == skill && it.name.equals(name, true) }
                    Row(Modifier.fillMaxWidth().height(58.dp).combinedClickable(onClick = {}, onLongClickLabel = "Edit or delete saved activity", onLongClick = { selected = skill to activity }).padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(gameArtPainter(Art.skillIcon(skill)), null, Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.bodyMedium, maxLines=1, overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text("${Rules.SKILL_LABELS[skill]} · ${if (uses > 0) "$uses uses · " else ""}${activity.xp} base XP", style = MaterialTheme.typography.labelSmall)
                        }
                        AppTextButton(enabled = !added, onClick = { viewModel.run { GameEngine.addTodoTask(it, name, skill, activity.xp) } }) { Text(if (added) "Added" else "Add") }
                    }
                }
                if (shownOthers < remaining.size) AppTextButton(onClick = { visibleExtra += 10 },
                    modifier = Modifier.fillMaxWidth()) { Text("More activities (next ${minOf(10, remaining.size - shownOthers)})") }
                Text("Hold an activity to edit or delete it.", style = MaterialTheme.typography.bodySmall)
                AppTextButton(onClick = { newPreset = true }) { Text("Save a new activity${if (category in Rules.SKILL_KEYS) " in ${Rules.SKILL_LABELS[category]}" else ""}") }
            }
        }
        }
        PersonalGoalsSection(snapshot, viewModel, compact = true)

    }
}

@Composable
private fun NewTodayTask(viewModel: GameViewModel, initialName: String, initialSkill: String, onAdded: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var skill by rememberSaveable { mutableStateOf(initialSkill.takeIf { it in Rules.SKILL_KEYS } ?: Rules.SKILL_KEYS.first()) }
    var xp by rememberSaveable { mutableStateOf("20") }
    var save by rememberSaveable { mutableStateOf(false) }
    SectionCard(title = "New task") {
        OutlinedTextField(name, { name = it.take(100) }, label = { Text("What will you do?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        CategoryPicker(skill) { skill = it }
        ActivityXpField(xp) { xp = it }
        Text("Choose the XP reward that matches the effort.", style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(save, { save = it }); Text("Save as a new activity") }
        AppButton(onClick = { viewModel.run { state ->
            val amount = xp.toIntOrNull() ?: -1
            val result = GameEngine.addPlannedActivity(state, name, skill, amount, save, emptySet())
            if (result is Outcome.Applied) {
                onAdded()
            }
            result
        } }) { Text("Add task") }
    }
}
