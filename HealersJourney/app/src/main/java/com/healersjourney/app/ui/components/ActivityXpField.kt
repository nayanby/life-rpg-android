package com.healersjourney.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun ActivityXpField(value: String, onChange: (String) -> Unit) {
    val presets = listOf(20, 40, 60, 80)
    // Older tasks retain their stored rewards until edited and saved.
    val selected = presets.minBy { abs(it - (value.toIntOrNull() ?: 20)) }
    LaunchedEffect(value) {
        if (value.toIntOrNull() !in presets) onChange(selected.toString())
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("XP reward", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            presets.forEach { amount -> FilterChip(selected == amount,
                { onChange(amount.toString()) }, label = { Text("$amount XP") }) }
        }
    }
}
