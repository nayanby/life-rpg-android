@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.healersjourney.app.ui.screens

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import com.healersjourney.app.ui.components.AppButton as Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.healersjourney.app.ui.components.AppLayout
import androidx.compose.ui.unit.dp
import com.healersjourney.app.domain.Achievements
import com.healersjourney.app.domain.GameEngine
import com.healersjourney.app.domain.GameState
import com.healersjourney.app.domain.Rules
import com.healersjourney.app.ui.GameSnapshot
import com.healersjourney.app.ui.GameViewModel
import com.healersjourney.app.ui.components.EmptyHint
import com.healersjourney.app.ui.components.NumberField
import com.healersjourney.app.ui.components.SectionCard
import com.healersjourney.app.ui.components.StatLine
import com.healersjourney.app.ui.components.skillColor

@Composable
fun HobbiesScreen(snapshot: GameSnapshot, viewModel: GameViewModel) {
    val state = snapshot.state
    GameEngine.ensureDefaults(state)
    val hobbies = state.hobbies.toList()
    var selected by remember { mutableStateOf(hobbies.firstOrNull() ?: "") }
    if (selected !in hobbies) selected = hobbies.firstOrNull() ?: ""
    var note by remember { mutableStateOf("") }
    var xp by remember { mutableStateOf("25") }
    var newHobby by remember { mutableStateOf("") }
    val color = skillColor("hobbiesXp")

    Column(verticalArrangement = Arrangement.spacedBy(AppLayout.sectionGap)) {
        SectionCard(title = "Log a creative session") {
            if (hobbies.isEmpty()) {
                EmptyHint("Add a creative interest below first.")
            } else {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppLayout.smallGap)) {
                    hobbies.take(6).forEach { hobby ->
                        FilterChip(
                            selected = hobby == selected,
                            onClick = { selected = hobby },
                            label = { Text(hobby) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("What did you do? (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(AppLayout.contentGap))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumberField(
                        value = xp,
                        onValueChange = { xp = it },
                        label = "XP",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(AppLayout.contentGap))
                    Button(onClick = {
                        val picked = selected
                        viewModel.run { GameEngine.completeHobbyActivity(it, picked, note, xp.toIntOrNull() ?: 0) }
                        note = ""
                        xp = "25"
                    }) { Text("Complete") }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (note.isBlank()) "Logs as \"$selected practice\"." else "Logs as \"$selected: ${note.trim()}\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SectionCard(title = "Your creative interests", collapsible = true, initiallyExpanded = false) {
            if (hobbies.isEmpty()) {
                EmptyHint("No creative interests yet.")
            } else {
                hobbies.forEach { hobby ->
                    StatLine(
                        hobby,
                        "${Achievements.hobbyActionCountByName(state, hobby)} sessions",
                        color
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newHobby,
                    onValueChange = { newHobby = it },
                    label = { Text("New creative interest") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(AppLayout.contentGap))
                Button(onClick = {
                    val name = newHobby
                    viewModel.run { GameEngine.addHobby(it, name) }
                    newHobby = ""
                }) { Text("Add") }
            }
        }

        SectionCard(title = "Creative progress", collapsible = true, initiallyExpanded = false) {
            StatLine("Hobbies XP", "${state.hobbiesXp}", color)
            StatLine(
                "This week",
                "${GameEngine.weeklyXp(state, "hobbiesXp")} / ${Rules.WEEKLY_TARGETS["hobbiesXp"]}"
            )
            StatLine("Lifetime sessions", "${Achievements.lifetimeActionCount(state, "hobbiesXp")}")
            StatLine("Creative days", "${state.actionHistory.filter { it.skill == "hobbiesXp" }.map { it.date }.distinct().size}")
        }

        SkillActivityCard(snapshot, viewModel, "hobbiesXp")
    }
}
