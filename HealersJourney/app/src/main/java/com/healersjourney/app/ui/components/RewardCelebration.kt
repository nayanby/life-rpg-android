package com.healersjourney.app.ui.components

import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.healersjourney.app.R
import com.healersjourney.app.domain.Rules
import com.healersjourney.app.ui.GameReward
import com.healersjourney.app.ui.GameViewModel
import com.healersjourney.app.ui.screens.WorldCharacter
import kotlinx.coroutines.delay

/** Transient events, not snapshot XP changes, prevent replays on rotation or import. */
@Composable
fun RewardCelebration(viewModel: GameViewModel) {
    val snapshot by viewModel.snapshot.collectAsState()
    var reward by remember { mutableStateOf<GameReward?>(null) }
    val animation = remember { Animatable(0f) }
    val context = LocalContext.current
    val view = LocalView.current
    val sounds = remember(context) { RewardSounds(context) }
    DisposableEffect(sounds) { onDispose { sounds.close() } }
    LaunchedEffect(viewModel) {
        viewModel.rewards.collect { event ->
            reward = event
            animation.snapTo(0f)
            val state = viewModel.snapshot.value.state
            if (state.hapticsEnabled) view.performHapticFeedback(if (event.leveledUp) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP)
            try {
                if (state.soundEnabled) sounds.play(event.unlocks.isNotEmpty() || event.categoryBonus)
                animation.animateTo(1f, tween(1100))
                delay(if (event.leveledUp || event.unlocks.isNotEmpty()) 2400 else 600)
            } finally {
                reward = null
            }
        }
    }
    reward?.let { event ->
        val milestone = event.leveledUp || event.unlocks.isNotEmpty() || (event.skill.isEmpty() && !event.care && event.xp == 0)
        if (!milestone) Popup(alignment = Alignment.BottomCenter, properties = PopupProperties(focusable = false)) {
            Surface(Modifier.padding(horizontal = 20.dp).padding(bottom = 100.dp), shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 8.dp) {
                Row(Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite }, verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(gameArtPainter(if(event.skill.isNotEmpty()) Art.skillIcon(event.skill) else GameArt.avatar(snapshot.state.worldEquipment["outfit"], snapshot.state.worldEquipment["character"])), null, Modifier.size(42.dp))
                    Column {
                        Text(if(event.care) "Care refreshed" else "Activity complete", style = MaterialTheme.typography.titleSmall)
                        if (event.coins > 0) Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("+${event.xp} XP ·", style = MaterialTheme.typography.bodyMedium)
                            Image(gameArtPainter(R.drawable.gameart_icon_coin), null, Modifier.size(20.dp))
                            Text("+${event.coins} coins", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(com.healersjourney.app.domain.DailyShowcase.collection(event.skill)?.let { "${it.title} badge added to your day" } ?: "A little effort. A growing world.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else Popup(alignment = Alignment.Center, onDismissRequest = { reward = null }, properties = PopupProperties(focusable = true)) {
            Box(Modifier.fillMaxSize().background(Color(0xFF111D35).copy(alpha = .82f)).clickable { reward = null }, contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    repeat(24) { i ->
                        val from = Offset(size.width * (.1f + (i * 13 % 80) / 100f), size.height * (.75f + (i % 4) * .04f))
                        val to = Offset(size.width * .5f, size.height * .36f)
                        val t = ((animation.value - i % 6 * .05f) / .7f).coerceIn(0f, 1f)
                        val point = Offset(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t)
                        if (t < 1f) drawCircle(if (i % 2 == 0) Color(0xFFFFD577) else Color(0xFF99DFDA), 3.dp.toPx(), point)
                    }
                }
                Column(Modifier.padding(28.dp).clip(RoundedCornerShape(30.dp)).background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp).semantics { liveRegion = LiveRegionMode.Polite }, horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (event.leveledUp) Image(painterResource(Art.badgeFor(Rules.title(event.level))), null, Modifier.size(100.dp))
                    else WorldCharacter(snapshot.state, Modifier.size(150.dp))
                    Text(if (event.leveledUp) "Level ${event.level}!" else if (event.care) "A little care goes a long way" else if (event.unlocks.isNotEmpty()) "A new discovery!" else if (event.xp > 0) "Activity complete!" else "Adventure complete!",
                        style = MaterialTheme.typography.headlineSmall)
                    if (event.xp > 0) Text("+${event.xp} XP", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                    if (event.coins > 0) Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Image(gameArtPainter(R.drawable.gameart_icon_coin), null, Modifier.size(28.dp))
                        Text("+${event.coins} coins", style = MaterialTheme.typography.titleMedium)
                    }
                    if (event.leveledUp) Text("Your world is growing with you.")
                    if (event.care) Text("Your character reflects your latest care routines.", style = MaterialTheme.typography.bodySmall)
                    event.unlocks.forEach { Text("Unlocked: $it", style = MaterialTheme.typography.titleSmall) }
                    Text("Tap to continue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
