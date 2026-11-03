package com.healersjourney.app.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.healersjourney.app.R
import com.healersjourney.app.domain.Achievement

@Composable
fun AchievementMedal(achievement: Achievement, tier: Int) {
    val colors = listOf(Color(0xFFB48366), Color(0xFF5EADA3), Color(0xFF6C9DC5), Color(0xFFAE85C4), Color(0xFFE2B75F))
    val rim = colors[tier.coerceAtMost(colors.lastIndex)]
    val skill = when(achievement.group) {
        "Study", "Learning" -> "knowledgeXp"; "Work" -> "medicineXp"; "Fitness" -> "strengthXp"
        "Social" -> "socialXp"; "Creativity", "Hobbies" -> "hobbiesXp"; "Sleep" -> "sleep"; else -> ""
    }
    Box(Modifier.size(64.dp).background(rim.copy(alpha=if(achievement.unlocked) .25f else .09f),CircleShape)
        .border(if(achievement.unlocked) 4.dp else 2.dp,rim.copy(alpha=if(achievement.unlocked) 1f else .35f),CircleShape),contentAlignment=Alignment.Center) {
        Image(gameArtPainter(if(skill.isNotEmpty()) DailyShowcaseArt.resource(skill) else R.drawable.gameart_decor_trophy),null,
            Modifier.size(45.dp).alpha(if(achievement.unlocked) 1f else .38f))
        if(achievement.unlocked) Text("✓",Modifier.align(Alignment.BottomEnd).background(rim,CircleShape).padding(horizontal=4.dp),color=Color.White,style=MaterialTheme.typography.labelSmall)
    }
}
