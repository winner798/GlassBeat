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
    return then(if (!theme.lowPower) Modifier.shadow(if (selected) 7.dp else 3.dp, shape,
        ambientColor = Color.Black.copy(alpha = .16f), spotColor = Color.Black.copy(alpha = .24f)) else Modifier)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(
            if (selected) primary.copy(alpha = .20f) else MaterialTheme.colorScheme.onSurface.copy(alpha = if (theme.light) .06f else .075f),
            if (selected) primary.copy(alpha = .08f) else MaterialTheme.colorScheme.surface.copy(alpha = .30f))))
        .border(if (selected) 1.4.dp else 1.dp, Brush.linearGradient(listOf(
            if (selected) primary.copy(alpha = .65f) else Color.White.copy(alpha = if (theme.light) .80f else .25f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = .045f),
            if (selected) primary.copy(alpha = .24f) else MaterialTheme.colorScheme.onSurface.copy(alpha = .13f))), shape)
}

@Composable
internal fun GlassListCard(modifier: Modifier = Modifier, selected: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.listCard(selected), content = content)
}

@Composable
internal fun Modifier.controlSkin(shape: Shape = RoundedCornerShape(16.dp)): Modifier {
    return clip(shape).background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .09f), MaterialTheme.colorScheme.onSurface.copy(alpha = .035f))))
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .34f), MaterialTheme.colorScheme.onSurface.copy(alpha = .06f), MaterialTheme.colorScheme.onSurface.copy(alpha = .16f))), shape)
}
