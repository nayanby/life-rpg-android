package com.healersjourney.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.healersjourney.app.domain.*
import com.healersjourney.app.ui.*
import com.healersjourney.app.ui.components.*

@Composable
fun MilestonesScreen(snapshot: GameSnapshot, viewModel: GameViewModel) {
    val state = snapshot.state
    var section by rememberSaveable { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Milestone?>(null) }
    var deleting by remember { mutableStateOf<Milestone?>(null) }
    var menuId by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TabRow(selectedTabIndex = section) {
            Tab(section == 0, { section = 0 }, text = { Text("Milestones") })
            Tab(section == 1, { section = 1 }, text = { Text("Personal goals") })
        }
        if (section == 1) { PersonalGoalsSection(snapshot, viewModel); return@Column }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Moments that matter", style = MaterialTheme.typography.titleLarge)
                Text("Keep something to look forward to, or remember a moment you've reached.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AppButton(onClick = { adding = true }) { Text("Add") }
        }
        val milestones = state.milestones.sortedWith(compareByDescending<Milestone> { it.showOnMain }.thenByDescending { it.completedDate })
        if (milestones.isEmpty()) SectionCard("Your milestones") {
            Text("Add meaningful moments to remember: finishing a course, a first 5K, or a special day.")
        }
        milestones.forEach { milestone ->
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Image(gameArtPainter(com.healersjourney.app.R.drawable.color_milestone), null, Modifier.size(40.dp))
                        Column(Modifier.weight(1f).clickable { editing = milestone }) {
                            Text(milestone.title, style = MaterialTheme.typography.titleMedium)
                            Text(if (milestone.completed) "Reached ${Dates.prettyCareDate(milestone.completedDate)}" else "Still ahead",
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Box {
                            IconButton(onClick = { menuId = milestone.id }) { Icon(Icons.Filled.MoreVert, "Milestone options") }
                            DropdownMenu(menuId == milestone.id, { menuId = null }) {
                                DropdownMenuItem(text = { Text("Edit milestone") }, onClick = { menuId = null; editing = milestone })
                                DropdownMenuItem(text = { Text(if (milestone.showOnMain) "Unpin from Home" else "Pin to Home") }, onClick = {
                                    menuId = null; viewModel.run { GameEngine.toggleMilestoneMain(it, milestone.id) }
                                })
                                DropdownMenuItem(text = { Text("Delete milestone") }, onClick = { menuId = null; deleting = milestone })
                            }
                        }
                    }
                    if (milestone.desc.isNotBlank()) Text(milestone.desc, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (milestone.showOnMain) {
                            Icon(Icons.Filled.PushPin, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp)); Text("On Home", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.weight(1f))
                        AppTextButton(onClick = { if (milestone.completed) editing = milestone else viewModel.run { GameEngine.completeMilestoneById(it, milestone.id) } }) {
                            Text(if (milestone.completed) "View memory" else "I've reached this")
                        }
                    }
                }
            }
        }
    }
    if (adding || editing != null) MilestoneEditor(editing, onDismiss = { adding = false; editing = null }) { title, desc, completed, pinned ->
        viewModel.run { val result = GameEngine.saveMilestone(it, editing?.id, title, desc, completed, pinned)
            if (result is Outcome.Applied) { adding = false; editing = null }; result }
    }
    deleting?.let { milestone -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete this milestone?") },
        text = { Text(milestone.title) }, confirmButton = { AppTextButton(onClick = {
            viewModel.runQuiet { GameEngine.deleteMilestoneById(it, milestone.id) }; deleting = null
        }) { Text("Delete") } }, dismissButton = { AppTextButton(onClick = { deleting = null }) { Text("Keep") } }) }
}

@Composable
private fun MilestoneEditor(milestone: Milestone?, onDismiss: () -> Unit, onSave: (String, String, Boolean, Boolean) -> Unit) {
    var title by rememberSaveable { mutableStateOf(milestone?.title.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(milestone?.desc.orEmpty()) }
    var completed by rememberSaveable { mutableStateOf(milestone?.completed ?: false) }
    var pinned by rememberSaveable { mutableStateOf(milestone?.showOnMain ?: true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (milestone == null) "Add a milestone" else "Your milestone") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(title, { title = it.take(100) }, label = { Text("What matters to you?") }, singleLine = true)
            OutlinedTextField(description, { description = it.take(4000) }, label = { Text("A few words to remember (optional)") }, minLines = 3)
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(completed, { completed = it }); Text("I've already reached this") }
            if (completed) Text("Reached ${Dates.prettyCareDate(milestone?.completedDate?.ifBlank { Dates.todayKey() } ?: Dates.todayKey())}", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(pinned, { pinned = it }); Text("Pin to Home") }
        }
    }, confirmButton = { AppTextButton(enabled = title.isNotBlank(), onClick = { onSave(title, description, completed, pinned) }) { Text("Save milestone") } },
        dismissButton = { AppTextButton(onClick = onDismiss) { Text("Close") } })
}
