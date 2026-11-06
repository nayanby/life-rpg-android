package com.healersjourney.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.healersjourney.app.domain.*
import com.healersjourney.app.ui.components.*

@Composable
fun TrophyDetail(state: GameState, action: ActionEntry, onDismiss: () -> Unit) {
    val label = Rules.activityLabel(action.skill, action.originalName ?: action.name)
    val count = state.actionHistory.count { it.skill == action.skill && Rules.activityLabel(it.skill, it.originalName ?: it.name).equals(label, true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(label) }, text = {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TrophyImage(action.skill, Modifier.size(200.dp), description = "${Rules.SKILL_LABELS[action.skill]} earned trophy")
            Text(Rules.SKILL_LABELS[action.skill] ?: "Activity", style = MaterialTheme.typography.titleMedium)
            Text("Completed ${Dates.prettyCareDate(action.date)}", style = MaterialTheme.typography.bodyMedium)
            Text("${action.xp} XP Â· ${if (action.id in state.coinAwardedActionIds) 10 else 0} coins", style = MaterialTheme.typography.titleMedium)
            Text("Completed $count ${if (count == 1) "time" else "times"} so far", style = MaterialTheme.typography.bodySmall)
            Text("Category bonuses are shown separately on the shelf.", style = MaterialTheme.typography.labelSmall)
        }
    }, confirmButton = { AppTextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
fun TrophyCollectionDialog(state: GameState, onDismiss: () -> Unit) {
    var rangeIndex by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<ActionEntry?>(null) }
    val range = TrophyCollection.Range.entries[rangeIndex]
    val periods = TrophyCollection.cards(state, range)
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Collection", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                    AppTextButton(onClick = onDismiss) { Text("Close") }
                }
                Surface(Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("THIS WEEK", "THIS MONTH", "THIS YEAR").forEachIndexed { index, label ->
                            Surface(onClick = { rangeIndex = index }, modifier = Modifier.weight(1f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                                color = if (rangeIndex == index) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = if (rangeIndex == index) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) {
                                Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                        }
                    }
                }
                key(rangeIndex) {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (periods.isEmpty()) item {
                            Text("Complete an activity to start your collection.", Modifier.padding(vertical = 24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        items(periods, key = { it.start }) { period ->
                            CollectionPeriodCard(period, range) { selected = it }
                        }
                    }
                }
            }
        }
    }
    selected?.let { TrophyDetail(state, it) { selected = null } }
}

@Composable
private fun CollectionPeriodCard(period: TrophyCollection.Period, range: TrophyCollection.Range, onSelect: (ActionEntry) -> Unit) {
    var expanded by androidx.compose.runtime.saveable.rememberSaveable(period.start, range) { mutableStateOf(false) }
    Surface(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val month = period.start.substring(5, 7).toInt()
            val weekday = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")[Planning.weekday(period.start) - 1]
            val heading = when (range) {
                TrophyCollection.Range.Week -> "$weekday · ${Dates.prettyCareDate(period.start)}"
                TrophyCollection.Range.Month -> "Week of ${Dates.prettyCareDate(Dates.weekKeyOf(period.start))}"
                TrophyCollection.Range.Year -> "${Dates.monthName(month)} ${period.start.take(4)}"
            }
            Text(heading, style = MaterialTheme.typography.titleMedium)
            if (range == TrophyCollection.Range.Month) Text("${Dates.prettyCareDate(period.start)} – ${Dates.prettyCareDate(period.end)}",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (range == TrophyCollection.Range.Week) "${period.entries.size} trophies" else "${period.entries.size} trophies · ${period.activeDays} active days", style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${period.xp} XP ·", style = MaterialTheme.typography.labelLarge)
                Image(gameArtPainter(com.healersjourney.app.R.drawable.gameart_icon_coin), null, Modifier.size(18.dp))
                Text("${period.coins} coins", style = MaterialTheme.typography.labelLarge)
            }
            if (range == TrophyCollection.Range.Year) {
                DailyShowcase.collections.forEach { category ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TrophyImage(category.skill, Modifier.size(36.dp))
                        Text(category.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("${period.count(category.skill)}", style = MaterialTheme.typography.titleSmall)
                    }
                }
            } else {
                if (period.entries.isEmpty()) Text("No trophies yet.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                val visible = if (expanded) period.entries else period.entries.take(24)
                visible.chunked(6).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { action ->
                            Box(Modifier.weight(1f).aspectRatio(1f).clickable(onClickLabel = "View trophy") { onSelect(action) },
                                contentAlignment = Alignment.Center) {
                                TrophyImage(action.skill, Modifier.fillMaxSize().padding(3.dp),
                                    description = Rules.activityLabel(action.skill, action.originalName ?: action.name))
                            }
                        }
                        repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                if (period.entries.size > 24) AppTextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Show fewer trophies" else "Show all ${period.entries.size} trophies")
                }
            }
        }
    }
}
