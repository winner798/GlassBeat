package io.github.glassbeat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Lightweight separation for scrolling rows: no per-row backdrop capture or shader. */
@Composable
internal fun Modifier.listCard(selected: Boolean = false): Modifier {
    val theme = LocalAppearance.current
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(22.dp)
    return clip(shape)
        .background(Brush.verticalGradient(listOf(
            if (selected) primary.copy(alpha = .14f) else MaterialTheme.colorScheme.surface.copy(alpha = if (theme.light) .25f else .18f),
            if (selected) primary.copy(alpha = .06f) else MaterialTheme.colorScheme.surface.copy(alpha = .08f))))
        .border(1.dp, Brush.linearGradient(listOf(
            if (selected) primary.copy(alpha = .50f) else Color.White.copy(alpha = if (theme.light) .68f else .30f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = .045f),
            if (selected) primary.copy(alpha = .20f) else MaterialTheme.colorScheme.onSurface.copy(alpha = .10f))), shape)
}

@Composable
internal fun GlassListCard(modifier: Modifier = Modifier, selected: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.listCard(selected), content = content)
}

@Composable
internal fun Modifier.controlSkin(shape: Shape = RoundedCornerShape(16.dp)): Modifier {
    return clip(shape).background(Brush.linearGradient(listOf(Color.White.copy(alpha = .075f), Color.Transparent)))
        .border(.8.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .28f), Color.Transparent, MaterialTheme.colorScheme.onSurface.copy(alpha = .10f))), shape)
}
