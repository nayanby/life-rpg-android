package com.healersjourney.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.healersjourney.app.domain.*
import com.healersjourney.app.ui.components.*

/** A dated, interactive display of actual completions; nothing is earned by merely planning. */
@Composable
@NonSkippableComposable
fun DayShowcase(state: GameState, day: String, revision: Int) {
    val entries = remember(revision, day) { DailyShowcase.entries(state, day) }
    var selectedId by remember(day) { mutableStateOf<String?>(null) }
    var collectionOpen by remember { mutableStateOf(false) }
    if (collectionOpen) TrophyCollectionDialog(state) { collectionOpen = false }
    state.actionHistory.firstOrNull { it.id == selectedId }?.let { action ->
        TrophyDetail(state, action) { selectedId = null }
    }
    var page by rememberSaveable(day) { mutableIntStateOf(0) }
    val pages = maxOf(1, (entries.size + 23) / 24)
    val currentPage = page.coerceIn(0, pages - 1)
    val visible = entries.drop(currentPage * 24).take(24)
    var knownIds by remember(day) { mutableStateOf(entries.map { it.id }.toSet()) }
    LaunchedEffect(day, entries.map { it.id }) {
        val added = entries.lastOrNull { it.id !in knownIds }
        if(added != null) {
            page = entries.indexOfFirst { it.id == added.id } / 24
        }
        knownIds = entries.map { it.id }.toSet()
    }
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < .5f
    val gold = if (dark) Color(0xFFFFE1A5) else Color(0xFF8A5A00)
    val slotFill = if (dark) Color(0xFF2F4A5C) else Color.White
    val slotEdge = if (dark) Color(0x40FFE1A5) else Color(0x1F000000)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = colors.surfaceVariant, border = BorderStroke(1.dp, slotEdge)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(gameArtPainter(com.healersjourney.app.R.drawable.gameart_decor_trophy), null, Modifier.size(22.dp))
                Text("Daily trophies", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = gold)
                AppTextButton(onClick = { collectionOpen = true }) { Text("Collection", color = gold) }
            }
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val slot = ((maxWidth - 30.dp) / 6).coerceAtMost(52.dp)
                if (entries.isEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(6) { TrophySlot(slot, slotFill, slotEdge, null, null) {} }
                } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    visible.chunked(6).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowItems.forEach { action ->
                                TrophySlot(slot, slotFill, slotEdge, action.skill,
                                    "${DailyShowcase.collection(action.skill)?.title} trophy: ${action.originalName ?: action.name}") {
                                    selectedId = action.id
                                }
                            }
                            repeat(6 - rowItems.size) { TrophySlot(slot, slotFill, slotEdge, null, null) {} }
                        }
                    }
                }
            }
            if(pages > 1) Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
                AppTextButton(enabled=currentPage>0,onClick={page=currentPage-1}) { Text("Previous",color=colors.onSurface) }
                Text("Display ${currentPage+1} / $pages",color=colors.onSurfaceVariant,style=MaterialTheme.typography.labelSmall)
                AppTextButton(enabled=currentPage<pages-1,onClick={page=currentPage+1}) { Text("Next",color=colors.onSurface) }
            }
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = slotFill) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Coins earned today", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    Image(gameArtPainter(com.healersjourney.app.R.drawable.gameart_icon_coin), null, Modifier.size(18.dp))
                    Text("${DailyCoinBonus.earnedToday(state, day)}", style = MaterialTheme.typography.titleSmall, color = gold)
                }
            }
            DailyCoinBonus.goals.forEach { goal ->
                val done = DailyCoinBonus.claimKey(day, goal) in state.coinRewardDays
                val progress = DailyCoinBonus.progress(state, day, goal).toFloat() / goal.categoriesRequired
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(goal.label, Modifier.weight(1f), color = if (done) colors.tertiary else colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None)
                    if (done) Text("Done", color = colors.tertiary, style = MaterialTheme.typography.labelMedium)
                    else Box(Modifier.width(56.dp).height(6.dp).clip(RoundedCornerShape(3.dp))) {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(),
                            color = gold, trackColor = slotEdge)
                    }
                    Image(gameArtPainter(com.healersjourney.app.R.drawable.gameart_icon_coin), null, Modifier.size(16.dp))
                    Text("+${goal.coins}", color = gold, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text("Tap a trophy to remember the activity.", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

/** A single display square: earned trophies are tappable, empty ones stay as quiet placeholders. */
@Composable
private fun TrophySlot(size: Dp, fill: Color, edge: Color, skill: String?, description: String?, onClick: () -> Unit) {
    var appeared by remember(skill) { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(if (appeared || skill == null) 1f else .3f, label = "Badge arrival")
    if (skill == null) {
        Box(Modifier.size(size).clip(RoundedCornerShape(16.dp)).border(BorderStroke(1.dp, edge), RoundedCornerShape(16.dp)))
    } else Surface(Modifier.size(size).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp)).clickable(onClickLabel = "View earned trophy", onClick = onClick),
        shape = RoundedCornerShape(16.dp), color = fill, border = BorderStroke(1.dp, edge)) {
        TrophyImage(skill, Modifier.padding(3.dp).fillMaxSize(), description = description)
    }
}
