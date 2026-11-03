package com.healersjourney.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.healersjourney.app.domain.*
import com.healersjourney.app.ui.GameSnapshot
import com.healersjourney.app.ui.GameViewModel
import com.healersjourney.app.ui.components.ActivityXpField

/** Opens either a new-preset form or actions for exactly one existing preset. */
@Composable
fun PresetEditorDialog(skill: String, snapshot: GameSnapshot, viewModel: GameViewModel, initialActivity: ActivityDef? = null, initialName: String = "", onDismiss: () -> Unit) {
    var mode by remember { mutableStateOf(if (initialActivity == null) "form" else "actions") }
    var name by remember { mutableStateOf(initialActivity?.let { Rules.activityLabel(skill, it.name) } ?: initialName) }
    var xp by remember { mutableStateOf(initialActivity?.xp?.toString() ?: "20") }
    var selectedSkill by remember { mutableStateOf(skill) }
    val existingRule = snapshot.state.recurringTasks.firstOrNull { it.id == "saved-${initialActivity?.id}" ||
        (initialActivity != null && it.skill == skill && it.name.equals(Rules.activityLabel(skill, initialActivity.name), true)) }
    var repeat by remember { mutableStateOf(existingRule != null) }
    var days by remember { mutableStateOf(existingRule?.weekdays ?: setOf(1, 2, 3, 4, 5)) }
    when (mode) {
        "actions" -> AlertDialog(onDismissRequest = onDismiss,
            title = { Text(Rules.activityLabel(skill, initialActivity!!.name)) },
            confirmButton = { TextButton(onClick = { mode = "form" }) { Text("Edit") } },
            dismissButton = { TextButton(onClick = { mode = "delete" }) { Text("Delete") } })
        "delete" -> AlertDialog(onDismissRequest = onDismiss,
            title = { Text("Delete preset?") },
            text = { Text("Delete ${Rules.activityLabel(skill, initialActivity!!.name)}? Previously earned XP will be kept.") },
            confirmButton = { TextButton(onClick = {
                viewModel.run { state ->
                    val result = GameEngine.removeActivityById(state, skill, initialActivity!!.id)
                    if (result is Outcome.Applied) existingRule?.let { Planning.removeRecurring(state, it.id) }
                    if (result is Outcome.Applied) onDismiss()
                    result
                }
            }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
        else -> AlertDialog(onDismissRequest = onDismiss,
            title = { Text(if (initialActivity == null) "Add preset" else "Edit preset") },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Activity name") }, singleLine = true)
                if (initialActivity == null) CategoryPicker(selectedSkill) { selectedSkill = it }
                ActivityXpField(xp) { xp = it }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(repeat, { repeat = it }); Text("Add automatically on selected days")
                }
                if (repeat) WeekdayPicker(days) { days = it }
                Text("Changes apply to future logs. Earned XP stays the same.", style = MaterialTheme.typography.bodySmall)
            } },
            confirmButton = { TextButton(enabled = name.isNotBlank() && (xp.toIntOrNull() ?: -1) in 0..10000 && (!repeat || days.isNotEmpty()), onClick = {
                viewModel.run { state ->
                    val savedName = if (initialActivity != null && name.trim() == Rules.activityLabel(skill, initialActivity.name)) initialActivity.name else name
                    val result = if (initialActivity == null) GameEngine.addActivity(state, selectedSkill, savedName, xp.toInt())
                        else GameEngine.updateActivityById(state, skill, initialActivity.id, savedName, xp.toInt())
                    if (result is Outcome.Applied) {
                        val activity = initialActivity ?: (state.activityDefs[selectedSkill]?.let { it.major + it.other }).orEmpty().firstOrNull { it.name == savedName.trim() }
                        val ruleId = existingRule?.id ?: "saved-${activity?.id}"
                        if (repeat) Planning.saveRecurring(state, ruleId, Rules.activityLabel(selectedSkill, savedName.trim()), selectedSkill, xp.toInt(), days)
                        else existingRule?.let { Planning.removeRecurring(state, it.id) }
                        onDismiss()
                    }
                    result
                }
            }) { Text("Save") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
    }
}
