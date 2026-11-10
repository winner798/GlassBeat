package io.github.glassbeat.ui

import android.os.Build
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithCache
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
fun GlassPanel(modifier: Modifier = Modifier, subtle: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val t = LocalAppearance.current
    val shape = RoundedCornerShape(28.dp)
    val surface = MaterialTheme.colorScheme.surface
    val nativeBlur = Build.VERSION.SDK_INT >= 31
    val tintAlpha = (t.opacity * if (subtle) .55f else .72f).coerceIn(.10f, .78f)
    val haze = if (nativeBlur && !t.lowPower && t.blur > 0) Modifier.hazeEffect(LocalGlass.current,
        HazeStyle(backgroundColor = MaterialTheme.colorScheme.background,
            tints = listOf(HazeTint(surface.copy(alpha = tintAlpha))), blurRadius = t.blur.dp,
            noiseFactor = .006f, fallbackTint = HazeTint(surface.copy(alpha = (.16f + t.opacity * .56f).coerceIn(.24f, .78f)))))
        else Modifier.background(surface.copy(alpha = tintAlpha))
    Box(modifier.then(if (nativeBlur && !t.lowPower) Modifier.shadow(if (subtle) 10.dp else 24.dp, shape, clip = false,
        ambientColor = Color.Black.copy(alpha = if (subtle) .12f else .26f), spotColor = Color.Black.copy(alpha = if (subtle) .18f else .38f)) else Modifier).clip(shape)) {
        // This layer contains only the backdrop, so refraction cannot warp text or icons.
        Box(Modifier.matchParentSize().glassLens(!subtle && !t.lowPower && t.blur > 0).then(haze)
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = if (subtle) .08f else if (t.light) .30f else .16f), Color.Transparent, Color.White.copy(alpha = if (subtle) .025f else .055f)))))
        Column(Modifier.fillMaxWidth().drawWithCache {
            val radius = minOf(28.dp.toPx(), size.minDimension * .5f)
            val reflection = Brush.radialGradient(listOf(Color.White.copy(alpha = if (subtle) .045f else if (t.light) .19f else .10f), Color.Transparent),
                center = Offset(size.width * .08f, 0f), radius = size.width.coerceAtLeast(1f) * .85f)
            val edge = Brush.linearGradient(listOf(Color.White.copy(alpha = if (subtle) .34f else .80f), Color.White.copy(alpha = if (subtle) .06f else .18f),
                Color.White.copy(alpha = .025f), Color.White.copy(alpha = if (subtle) .16f else .48f)))
            onDrawWithContent {
                drawRect(reflection)
                drawContent()
                // A single rim catches light; an inset stroke looks like a second white box.
                drawRoundRect(edge, topLeft = Offset(.6.dp.toPx(), .6.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size((size.width - 1.2.dp.toPx()).coerceAtLeast(0f), (size.height - 1.2.dp.toPx()).coerceAtLeast(0f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius), style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx()))
            }
        }, content = content)
    }
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
                val ribbon = androidx.compose.ui.graphics.Path().apply {
                    moveTo(-size.width * .15f, size.height * .32f)
                    cubicTo(size.width * .42f, size.height * .06f, size.width * .22f, size.height * .84f, size.width * 1.2f, size.height * .61f)
                }
                drawPath(ribbon, Brush.linearGradient(listOf(accent.copy(alpha = .14f), Color(0xFF9BE8EF).copy(alpha = .08f), Color.Transparent)),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(size.width * .055f))
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .42f), Color.Transparent),
                    center = Offset(size.width * (.10f + drift), size.height * .13f), radius = size.width * .9f), radius = size.width * .9f, center = Offset(size.width * (.10f + drift), size.height * .13f))
                drawCircle(Brush.radialGradient(listOf(Color(0xFF60D6CE).copy(alpha = .24f), Color.Transparent),
                    center = Offset(size.width * .95f, size.height * (.62f + drift)), radius = size.width * .85f), radius = size.width * .85f, center = Offset(size.width * .95f, size.height * (.62f + drift)))
            }
        }
    }
}
