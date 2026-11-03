package com.healersjourney.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.healersjourney.app.Destination
import com.healersjourney.app.R

@Composable
fun PillNavigation(selected: Destination, onSelect: (Destination) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(Triple(Destination.Dashboard, R.drawable.gameart_icon_nav_world, "Home"),
                Triple(Destination.Todo, R.drawable.gameart_icon_nav_quests, "Today"),
                Triple(Destination.Character, R.drawable.gameart_icon_nav_store, "Store"),
                Triple(Destination.More, R.drawable.gameart_icon_nav_journal, "Journal")).forEach { (destination, icon, label) ->
                val active = selected == destination
                val pillColor = animateColorAsState(
                    if (active) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                    animationSpec = tween(200), label = "Navigation selection"
                )
                Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                    .selectable(active, role = Role.Tab, onClick = { onSelect(destination) })
                    .padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.width(60.dp).height(34.dp).clip(RoundedCornerShape(16.dp))
                        .background(pillColor.value),
                        contentAlignment = Alignment.Center) {
                        Image(gameArtPainter(icon, 128), null, Modifier.size(30.dp))
                    }
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
