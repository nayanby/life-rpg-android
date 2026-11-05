package com.healersjourney.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shared dimensions; minimum heights allow larger accessibility text to grow. */
object AppLayout {
    val barFadeHeight = 21.dp
    val screenPadding = 16.dp
    val sectionGap = 20.dp
    val contentGap = 12.dp
    val smallGap = 8.dp
    val icon = 24.dp
    val illustration = 32.dp
    val avatar = 64.dp
    val ring = 48.dp
    val buttonHeight = 40.dp
    val controlHeight = 56.dp
    val touchTarget = 48.dp
    val cardRadius = 24.dp
    val controlRadius = 12.dp
}

@Composable
fun AppButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
              content: @Composable RowScope.() -> Unit) {
    androidx.compose.material3.Button(onClick = onClick,
        modifier = modifier.heightIn(min = AppLayout.buttonHeight),
        enabled = enabled, shape = CircleShape,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary, contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), content = content)
}

@Composable
fun AppTonalButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                   content: @Composable RowScope.() -> Unit) {
    androidx.compose.material3.FilledTonalButton(onClick = onClick,
        modifier = modifier.heightIn(min = AppLayout.buttonHeight),
        enabled = enabled, shape = CircleShape,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer, contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), content = content)
}

@Composable
fun AppOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                      content: @Composable RowScope.() -> Unit) {
    androidx.compose.material3.OutlinedButton(onClick = onClick,
        modifier = modifier.heightIn(min = AppLayout.buttonHeight),
        enabled = enabled, shape = CircleShape,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer, contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), content = content)
}

@Composable
fun AppTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                  content: @Composable RowScope.() -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick,
        modifier = modifier.heightIn(min = AppLayout.touchTarget), enabled = enabled,
        shape = CircleShape, content = content)
}
