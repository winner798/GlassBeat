package io.github.glassbeat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.glassbeat.R

private val ChineseBubbleFamily = FontFamily(Font(R.font.favsoda_bubble_cjk))
private val BubbleFamily = FontFamily(Font(R.font.favsoda_bubble, FontWeight.Bold))

/** Static layered lettering: one accessible brand label, no continuous animation. */
@Composable
internal fun BrandWordmark(modifier: Modifier = Modifier, text: String = "FavSoda") {
    val appearance = LocalAppearance.current
    val primary = MaterialTheme.colorScheme.primary
    val surfaceText = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current
    val face = if (appearance.light) listOf(lerp(primary, surfaceText, .65f), lerp(primary, surfaceText, .85f), lerp(primary, surfaceText, .55f))
        else listOf(Color.White, lerp(primary, Color.White, .80f), lerp(primary, Color.White, .32f))
    val style = TextStyle(fontFamily = if (text == "FavSoda") BubbleFamily else ChineseBubbleFamily, fontWeight = FontWeight.Bold, fontSize = if (text == "FavSoda") 38.sp else 32.sp, lineHeight = 44.sp, letterSpacing = .2.sp)
    Box(modifier) {
        Text(text, modifier = Modifier.offset(y = 1.6.dp).clearAndSetSemantics {},
            style = style, color = lerp(primary, Color.Black, if (appearance.light) .26f else .55f).copy(alpha = .46f),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text, modifier = Modifier.semantics { heading() },
            style = style.copy(brush = Brush.verticalGradient(face),
                shadow = if (appearance.lowPower) null else Shadow(Color.Black.copy(alpha = if (appearance.light) .14f else .30f),
                    Offset(0f, with(density) { 1.dp.toPx() }), with(density) { 2.dp.toPx() })),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
