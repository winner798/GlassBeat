package io.github.glassbeat.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.chrisbanes.haze.*
import io.github.glassbeat.data.ThemeSettings
import java.io.File

val LocalGlass = staticCompositionLocalOf { HazeState() }
val LocalAppearance = staticCompositionLocalOf { ThemeSettings() }

@Composable
fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val t = LocalAppearance.current
    val shape = RoundedCornerShape(28.dp)
    val surface = MaterialTheme.colorScheme.surface
    val haze = if (!t.lowPower && t.blur > 0) Modifier.hazeEffect(LocalGlass.current,
        HazeStyle(backgroundColor = MaterialTheme.colorScheme.background,
            tints = listOf(HazeTint(surface.copy(alpha = t.opacity))), blurRadius = t.blur.dp,
            noiseFactor = .015f, fallbackTint = HazeTint(surface.copy(alpha = .88f))))
        else Modifier.background(surface.copy(alpha = t.opacity.coerceAtLeast(.65f)))
    Column(modifier.clip(shape).then(haze)
        .background(Brush.linearGradient(listOf(Color.White.copy(alpha = if (t.light) .35f else .10f), Color.Transparent, Color.White.copy(alpha = .03f))))
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .35f), Color.White.copy(alpha = .04f), Color.White.copy(alpha = .14f))), shape), content = content)
}

@Composable
fun GlassBackground(theme: ThemeSettings, haze: HazeState) {
    val accent = Color(theme.accent)
    val base = if (theme.light) Color(0xFFF4F1FB) else Color(0xFF11121E)
    var drift = 0f
    if (theme.motion && !theme.lowPower) {
        val transition = rememberInfiniteTransition(label = "ambient")
        val offset by transition.animateFloat(-.06f, .06f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "drift")
        drift = offset
    }
    Box(Modifier.fillMaxSize().hazeSource(haze).background(base)) {
        if (theme.background.isNotBlank()) {
            AsyncImage(File(theme.background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(base.copy(alpha = if (theme.light) .50f else .62f)))
        } else {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .42f), Color.Transparent),
                    center = Offset(size.width * (.10f + drift), size.height * .13f), radius = size.width * .9f), radius = size.width * .9f, center = Offset(size.width * (.10f + drift), size.height * .13f))
                drawCircle(Brush.radialGradient(listOf(Color(0xFF60D6CE).copy(alpha = .24f), Color.Transparent),
                    center = Offset(size.width * .95f, size.height * (.62f + drift)), radius = size.width * .85f), radius = size.width * .85f, center = Offset(size.width * .95f, size.height * (.62f + drift)))
            }
        }
    }
}
