package com.healersjourney.app.ui.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Mirrored fades ease into both the transparent page and the solid bar without a seam. */
fun barFadeBrush(color: Color, fadesDown: Boolean): Brush {
    // Zero slope at both ends removes visible joins into solid and clear areas.
    return Brush.verticalGradient(*(0..32).map { index ->
        val t = index / 32f
        val smooth = t * t * t * (t * (t * 6f - 15f) + 10f)
        t to color.copy(alpha = color.alpha * if (fadesDown) 1f - smooth else smooth)
    }.toTypedArray())
}
