package com.healersjourney.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.healersjourney.app.Destination
import com.healersjourney.app.R
import com.healersjourney.app.domain.*
import com.healersjourney.app.ui.*
import com.healersjourney.app.ui.components.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WorldHome(snapshot: GameSnapshot, viewModel: GameViewModel, onOpen: (Destination) -> Unit) {
    val state = snapshot.state
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Box(Modifier.fillMaxWidth().height(252.dp)) {
                Image(gameArtPainter(GameArt.backdrop(state.worldEquipment["backdrop"])), null,
                    Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Row(Modifier.fillMaxWidth().height(214.dp).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                WorldCharacter(state, Modifier.size(176.dp), onTap = { onOpen(Destination.Care) }, showCompanion = false)
                state.worldEquipment["companion"]?.takeIf { it != "none" }?.let { companion ->
                    val place = when (companion) {
                        "owl", "star", "cloud", "firefly" -> Alignment.TopCenter
                        "cat", "dragon", "fox", "bunny" -> Alignment.BottomCenter
                        else -> Alignment.Center
                    }
                    Box(Modifier.size(width = 96.dp, height = 176.dp),
                        contentAlignment = place) {
                        CompanionArt(companion, Modifier.size(if (companion == "dragon") 96.dp else 80.dp))
                    }
                }
                }


            }
        }
        GrowthRadar(state, snapshot.revision)
        val tasks = state.todoTasks.filter { it.date == Dates.gameDayKey() }
        val completed = tasks.count { it.completed }
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (tasks.isEmpty()) "Make room for today" else "Your next task", style = MaterialTheme.typography.titleMedium)
                Text(tasks.firstOrNull { !it.completed }?.name ?: if (tasks.isEmpty()) "Choose what matters to you today." else "You finished everything you planned.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AppButton(onClick = { onOpen(Destination.Todo) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (tasks.isEmpty()) "Plan today" else "Today's tasks · $completed/${tasks.size}")
                }
            }
        }
    }

}

private enum class GrowthRange(val label: String) { Day("Day"), Week("Week"), Month("Month"), Year("Year"), Lifetime("Lifetime") }

private fun growthValues(state: GameState, range: GrowthRange, day: String = Dates.gameDayKey()): List<Int> {
    val skills = Rules.SKILL_KEYS
    if (range == GrowthRange.Lifetime) return skills.map { state.skillXp(it).coerceAtLeast(0) }
    val actions = state.actionHistory.filter { action ->
        action.skill in skills && action.type in setOf("activity", "study", "hobby", "other") && when (range) {
            GrowthRange.Day -> action.date == day
            GrowthRange.Week -> action.week == Dates.weekKeyOf(day)
            GrowthRange.Month -> action.month == day.take(7)
            GrowthRange.Year -> action.date.startsWith(day.take(4))
            GrowthRange.Lifetime -> true
        }
    }
    return skills.map { skill -> actions.filter { it.skill == skill }.sumOf { it.xp } }
}

@Composable
private fun GrowthRadar(state: GameState, revision: Int) {
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    selectedCategory?.let { skill ->
        CategoryDescriptionDialog(skill) { selectedCategory = null }
    }
    val skills = Rules.SKILL_KEYS
    val ranges = GrowthRange.entries
    val pager = rememberPagerState(initialPage = 1, pageCount = { ranges.size + 1 })
    val day = Dates.gameDayKey()
    val valuesByRange = remember(revision, day) { listOf(growthValues(state, GrowthRange.Day, Dates.addDays(day, -1))) + ranges.map { growthValues(state, it) } }
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Your growth", style = MaterialTheme.typography.titleMedium)
                Text(if (pager.currentPage == 0) "Yesterday" else ranges[pager.currentPage - 1].label, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
            HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().height(390.dp)) { page ->
                val values = valuesByRange[page]
                val ceiling = maxOf(40, values.maxOrNull() ?: 0)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${values.sum()} XP across five categories", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    BoxWithConstraints(Modifier.fillMaxWidth().height(350.dp).clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF203B4D), Color(0xFF192D41))))) {
                val angles = skills.indices.map { -Math.PI / 2 + it * 2 * Math.PI / skills.size }
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension * .25f
                    fun point(index: Int, fraction: Float): Offset {
                        val angle = angles[index]
                        return Offset(center.x + cos(angle).toFloat() * radius * fraction,
                            center.y + sin(angle).toFloat() * radius * fraction)
                    }
                    drawCircle(Color(0x1655C7C2), radius * 1.12f, center)
                    listOf(.25f, .5f, .75f, 1f).forEach { ring ->
                        val path = Path().apply {
                            skills.indices.forEach { index ->
                                val p = point(index, ring)
                                if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                            }
                            close()
                        }
                        if (ring == 1f) drawPath(path, Color(0x142FB9AE))
                        drawPath(path, Color(0xFF8FB6BE).copy(alpha = if (ring == 1f) .85f else .5f),
                            style = Stroke(width = if (ring == 1f) 1.5.dp.toPx() else 1.dp.toPx()))
                    }
                    skills.indices.forEach { drawLine(Color(0x658FB6BE), center, point(it, 1f), 1.dp.toPx()) }
                    val progress = Path().apply {
                        values.indices.forEach { index ->
                            val p = point(index, values[index].toFloat() / ceiling)
                            if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                        }
                        close()
                    }
                    drawPath(progress, Brush.verticalGradient(listOf(Color(0xBB66DCC9), Color(0x996A9FF5))))
                    drawPath(progress, Color(0xFF87E8D7), style = Stroke(width = 3.dp.toPx()))
                    values.indices.forEach {
                        val location = point(it, values[it].toFloat() / ceiling)
                        drawCircle(Color(0xFF1B5160), 6.dp.toPx(), location)
                        drawCircle(Color(0xFFB9FFF0), 3.dp.toPx(), location)
                    }
                }
                skills.forEachIndexed { index, skill ->
                    val angle = angles[index]
                    val x = maxWidth / 2 + 126.dp * cos(angle).toFloat() - 40.dp
                    val y = if (index == 0) 4.dp else 175.dp + 126.dp * sin(angle).toFloat() - 16.dp
                    Column(Modifier.offset(x = x, y = y).width(80.dp).clip(RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button, onClickLabel = "About ${Rules.SKILL_LABELS[skill]}") { selectedCategory = skill },
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF294B5B)), contentAlignment = Alignment.Center) {
                            Image(gameArtPainter(Art.skillIcon(skill), 128), null, Modifier.size(32.dp))
                        }
                        Text(Rules.SKILL_LABELS[skill].orEmpty(), style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE4F2F2),
                            textAlign = TextAlign.Center, maxLines = 1)
                        Text("${values[index]} XP", style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9DDAD3), textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                (0..ranges.size).forEach { index ->
                    Box(Modifier.size(width = if (index == pager.currentPage) 16.dp else 6.dp, height = 6.dp)
                        .clip(CircleShape)
                        .background(if (index == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                }
            }
        }
    }
}

@Composable
private fun CategoryDescriptionDialog(skill: String, onDismiss: () -> Unit) {
    val description = CategoryGuide.forSkill(skill) ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Image(gameArtPainter(Art.skillIcon(skill), 256), null, Modifier.size(76.dp)) },
        title = { Text(Rules.SKILL_LABELS[skill].orEmpty()) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(description.summary, style = MaterialTheme.typography.bodyLarge)
                Text("Things you can log", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                description.examples.forEach { example ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", color = MaterialTheme.colorScheme.primary)
                        Text(example, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { AppTextButton(onClick = onDismiss) { Text("Got it") } }
    )
}

@Composable
@NonSkippableComposable
fun WorldCharacter(state: GameState, modifier: Modifier = Modifier, previewScore: Int? = null,
                   showCompanion: Boolean = true, onTap: (() -> Unit)? = null) {
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(60_000); clock = System.currentTimeMillis() } }
    val score = previewScore ?: clock.let { GameEngine.personalCareStats(state).score }
    val key = Rules.careAvatarKey(score)
    val outfit = state.worldEquipment["outfit"] ?: "traveler"
    Box(if (onTap != null) modifier.clickable(role = Role.Button, onClick = onTap) else modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawOval(Color(0xFF081824).copy(alpha = .16f),
                Offset(size.width * .31f, size.height * .955f),
                Size(size.width * .40f, size.height * .045f))
            drawOval(Color(0xFF081824).copy(alpha = .22f),
                Offset(size.width * .37f, size.height * .96f),
                Size(size.width * .28f, size.height * .03f))
        }
        Image(gameArtPainter(GameArt.careAvatar(outfit, state.worldEquipment["character"], key)), "Your character: $key, ${WorldRules.items.firstOrNull { it.id == outfit }?.name ?: "Traveler"}",
            Modifier.fillMaxSize())

        state.worldEquipment["companion"]?.takeIf { showCompanion && it != "none" }?.let {
            CompanionArt(it, Modifier.size(40.dp).align(Alignment.BottomEnd))
        }
        if (state.actionHistory.firstOrNull()?.type == "sleep") Text("z Z z", Modifier.align(Alignment.TopEnd),
            color = Color(0xFF495E91), style = MaterialTheme.typography.titleSmall)
    }
}


@Composable
private fun CompanionArt(id: String, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "Companion")
    val bob by transition.animateFloat(0f, 4f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "Companion float")
    Image(gameArtPainter(GameArt.companion(id)), null, modifier.offset(y = bob.dp))
}

@Composable
private fun WorldWardrobe(snapshot: GameSnapshot, viewModel: GameViewModel) {
    var slot by remember { mutableStateOf("outfit") }
    var purchasedId by rememberSaveable { mutableStateOf<String?>(null) }
    var justPurchased by rememberSaveable { mutableStateOf(false) }
    val state = snapshot.state
    WorldRules.items.firstOrNull { it.id == purchasedId }?.let { purchased ->
        AlertDialog(onDismissRequest = { purchasedId = null },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (justPurchased) "Unlocked!" else "Your collection", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(purchased.name, style = MaterialTheme.typography.headlineSmall)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .4f)) {
                        Image(gameArtPainter(GameArt.item(purchased.slot, purchased.id, state.worldEquipment["character"])),
                            purchased.name, Modifier.fillMaxWidth().height(if (purchased.slot == "backdrop") 220.dp else 280.dp).padding(12.dp),
                            contentScale = ContentScale.Fit)
                    }
                    Text(if (state.worldEquipment[purchased.slot] == purchased.id) "Equipped and ready." else "Part of your collection.", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = { AppButton(onClick = { purchasedId = null }) { Text("Done") } })
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("outfit" to "Outfits", "companion" to "Companions", "backdrop" to "Backdrops").forEach { (key, label) ->
                        FilterChip(selected = slot == key, onClick = { slot = key }, label = { Text(label, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                val items = WorldRules.items.filter { it.slot == slot }.sortedWith(compareBy<WorldItem> { it.adventures }.thenBy { it.xp })
                items.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { item ->
                            val unlocked = item.id in state.worldUnlocks
                            val equipped = state.worldEquipment[slot] == item.id
                            Surface(Modifier.weight(1f).clickable(enabled = unlocked, role = Role.Button, onClickLabel = "Preview ${item.name}") {
                                justPurchased = false
                                purchasedId = item.id
                            }, shape = RoundedCornerShape(16.dp),
                                color = if (equipped) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (equipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (item.id != "none") Image(gameArtPainter(GameArt.item(slot, item.id, state.worldEquipment["character"])), null,
                                        Modifier.height(72.dp).fillMaxWidth(), contentScale = if (slot == "backdrop") ContentScale.Crop else ContentScale.Fit)
                                    else Box(Modifier.height(72.dp), contentAlignment = Alignment.Center) { Text("Solo journey", style = MaterialTheme.typography.labelSmall) }
                                    Text(item.name, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1)
                                    Text(if (unlocked) "Owned" else "Not owned",
                                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1)
                                    AppTonalButton(enabled = !equipped && (unlocked || state.coins >= WorldRules.coinPrice(item)),
                                        onClick = { viewModel.run { current ->
                                            val result = if (unlocked) WorldRules.equip(current, item.id) else WorldRules.buyAndEquip(current, item.id)
                                            if (!unlocked && result is Outcome.Applied) { justPurchased = true; purchasedId = item.id }
                                            result
                                        } },
                                        modifier = Modifier.fillMaxWidth()) {
                                        if (unlocked) Text(if (equipped) "Equipped" else "Equip")
                                        else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Image(gameArtPainter(R.drawable.gameart_icon_coin), null, Modifier.size(20.dp))
                                            Text("${WorldRules.coinPrice(item)}")
                                        }
                                    }
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
    }
}

@Composable
fun CharacterScreen(snapshot: GameSnapshot, viewModel: GameViewModel) = WorldWardrobe(snapshot, viewModel)
