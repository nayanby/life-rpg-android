package com.healersjourney.app.ui.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Clavis viewer's eased opacity curve, mirrored at the bottom of the page. */
fun barFadeBrush(color: Color, fadesDown: Boolean): Brush {
    if (fadesDown) {
        // Zero slope at both ends removes visible joins into solid and clear areas.
        return Brush.verticalGradient(*(0..32).map { index ->
            val t = index / 32f
            val smooth = t * t * t * (t * (t * 6f - 15f) + 10f)
            t to color.copy(alpha = color.alpha * (1f - smooth))
        }.toTypedArray())
    }
    val stops = listOf(0f to 255, .143f to 245, .314f to 225, .571f to 140, .771f to 50, 1f to 0)
    val ordered = if (fadesDown) stops else stops.reversed().map { (position, alpha) -> 1f - position to alpha }
    return Brush.verticalGradient(*ordered.map { (position, alpha) ->
        position to color.copy(alpha = color.alpha * alpha / 255f)
    }.toTypedArray())
}
