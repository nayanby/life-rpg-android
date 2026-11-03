package com.healersjourney.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.OutlinedTextField
import com.healersjourney.app.ui.components.NumberField
import com.healersjourney.app.domain.Outcome
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import com.healersjourney.app.ui.components.AppButton as Button
import com.healersjourney.app.ui.components.AppIcon as Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.healersjourney.app.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.healersjourney.app.ui.components.AppLayout
import androidx.compose.ui.unit.dp
import com.healersjourney.app.domain.CareItemStatus
import com.healersjourney.app.domain.Dates
import com.healersjourney.app.domain.GameEngine
import com.healersjourney.app.domain.Rules
import com.healersjourney.app.ui.GameSnapshot
import com.healersjourney.app.ui.GameViewModel
import com.healersjourney.app.ui.components.Art
import com.healersjourney.app.ui.components.FreshnessRing
import com.healersjourney.app.ui.components.SectionCard
import com.healersjourney.app.ui.components.XpProgress
import com.healersjourney.app.ui.components.freshnessColor

/** Avatar key, rank name and the care-score band it covers, best first. */
private val AVATAR_STATES = listOf(
    Triple("pristine", "Pristine", "51 – 60"),
    Triple("grimy", "Grimy", "41 – 50"),
    Triple("dirty", "Dirty", "21 – 40"),
    Triple("filthy", "Filthy", "0 – 20")
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CareScreen(snapshot: GameSnapshot, viewModel: GameViewModel) {
    val state = snapshot.state
    val stats = GameEngine.personalCareStats(state)
    val context = LocalContext.current
    var showAvatars by remember { mutableStateOf(false) }
    var editorOpen by remember { mutableStateOf(false) }
    var addMenuOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CareItemStatus?>(null) }
    if (editorOpen) CareRoutineEditor(editing, viewModel) { editorOpen = false }

    Column(verticalArrangement = Arrangement.spacedBy(AppLayout.sectionGap)) {
        SectionCard(title = "A moment for yourself") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WorldCharacter(state, Modifier.size(132.dp)) { showAvatars = true }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stats.rank, style = MaterialTheme.typography.titleMedium)
                    XpProgress(stats.score.toFloat() / GameEngine.CARE_SCORE_MAX,
                        "${stats.score} / ${GameEngine.CARE_SCORE_MAX} care score", MaterialTheme.colorScheme.tertiary)
                    Text("Tap your character to compare care states.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        SectionCard(title = "Your routines") {
            Text("Hold a routine to edit its frequency or remove it.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            stats.items.forEachIndexed { index, item ->
                val done = item.last == Dates.gameDayKey()
                Row(Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClickLabel = "Edit routine", onLongClick = { editing = item; editorOpen = true }).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(com.healersjourney.app.ui.components.gameArtPainter(com.healersjourney.app.ui.components.AppIcons.care(item.key)), null, Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.label, style = MaterialTheme.typography.titleSmall)
                        Text("Every ${item.cycleDays}d · ${dueCaption(item, done)}", style = MaterialTheme.typography.bodySmall,
                            color = if (item.missedUnits > 0 && !done) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(4.dp))
                    if (done) Text("Done ✓", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    else Button(onClick = {
                        viewModel.run { GameEngine.completePersonalCare(it, item.key) }
                    }) { Text("Log") }
                }
                if (index < stats.items.lastIndex) androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
        TextButton(onClick = { addMenuOpen = true }) { Text("Add routine") }
        SectionCard(title = "Care history", collapsible = true, initiallyExpanded = false) {
            val entries = state.taskLog.filter { it.text.contains("personal care", true) }.take(10)
            if (entries.isEmpty()) Text("Your care check-ins will appear here.", style = MaterialTheme.typography.bodyMedium)
            entries.forEach { entry ->
                Text(com.healersjourney.app.domain.FriendlyLog.entry(entry, state), style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
    if (addMenuOpen) AlertDialog(onDismissRequest = { addMenuOpen = false },
        title = { Text("Add a care routine") },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Rules.CARE_SUGGESTIONS.forEach { rule ->
                    val added = state.careRoutines.any { it.key == rule.key || it.label.equals(rule.label, true) }
                    TextButton(onClick = {
                        viewModel.run { current ->
                            val result = GameEngine.addSuggestedCareRoutine(current, rule.key)
                            if (result is Outcome.Applied) addMenuOpen = false
                            result
                        }
                    }, enabled = !added, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Image(com.healersjourney.app.ui.components.gameArtPainter(
                                com.healersjourney.app.ui.components.AppIcons.care(rule.key)), null, Modifier.size(32.dp))
                            Column(Modifier.weight(1f)) {
                                Text(rule.label, style = MaterialTheme.typography.bodyMedium)
                                Text("Every ${rule.cycleDays} day${if (rule.cycleDays == 1) "" else "s"}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            if (added) Text("Added", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                androidx.compose.material3.HorizontalDivider()
                TextButton(onClick = {
                    addMenuOpen = false
                    editing = null
                    editorOpen = true
                }, modifier = Modifier.fillMaxWidth()) { Text("Create custom routine") }
            }
        },
        confirmButton = { TextButton(onClick = { addMenuOpen = false }) { Text("Close") } })
    if (showAvatars) CareAvatarDialog(state, stats.score) { showAvatars = false }
}
private fun dueCaption(item: CareItemStatus, doneToday: Boolean): String {
    val left = item.cycleDays - item.days
    return when {
        doneToday -> "Done today"
        left > 0 -> "Due in ${left}d"
        left == 0 -> "Due today"
        else -> "${-left}d over"
    }
}

@Composable
private fun CareAvatarDialog(state: com.healersjourney.app.domain.GameState, score: Int, onDismiss: () -> Unit) {
    var preview by remember { mutableStateOf(score) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Your character's care") },
        text = {
            Column {
                WorldCharacter(state, Modifier.size(144.dp), previewScore = preview)
                Text("Current state: ${GameEngine.personalCareStats(state).rank}. This preview does not change your care score.",
                    style = MaterialTheme.typography.bodySmall)
                listOf(60 to "Pristine · Score 51–60", 45 to "Grimy · Score 41–50", 30 to "Dirty · Score 21–40", 10 to "Filthy · Score 0–20").forEach { (value, label) ->
                    TextButton(onClick = { preview = value }) { Text(label, style = MaterialTheme.typography.labelMedium) }
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
private fun CareRoutineEditor(item: CareItemStatus?, viewModel: GameViewModel, onClose: () -> Unit) {
    var name by remember(item?.key) { mutableStateOf(item?.label ?: "") }
    var days by remember(item?.key) { mutableStateOf(item?.cycleDays?.toString() ?: "1") }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onClose, title = { Text(if (item == null) "New care routine" else "Edit care routine") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it.take(60) }, label = { Text("Routine name") }, singleLine = true)
            NumberField(value = days, onValueChange = { days = it }, label = "Repeat every (days)")
            Text("1–365 days. Your past check-ins stay in history.", style = MaterialTheme.typography.bodySmall)
            if (item != null) TextButton(onClick = { confirmDelete = true }) { Text("Remove routine", color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(onClick = { viewModel.run { state ->
            val result = GameEngine.saveCareRoutine(state, item?.key, name, days.toIntOrNull() ?: 0)
            if (result is Outcome.Applied) onClose()
            result
        } }) { Text("Save") } }, dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } })
    if (confirmDelete && item != null) AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text("Remove ${item.label}?") }, text = { Text("It will stop affecting care. Past check-ins remain in history.") },
        confirmButton = { TextButton(onClick = { viewModel.run { state ->
            val result = GameEngine.removeCareRoutine(state, item.key)
            if (result is Outcome.Applied) onClose()
            result
        } }) { Text("Remove") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } })
}
