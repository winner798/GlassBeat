package io.github.glassbeat.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
internal fun GlassNavigation(selected: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    val appearance = LocalAppearance.current
    val motion = appearance.motion && !appearance.lowPower
    var pressedIndex by remember { mutableIntStateOf(-1) }
    val target = if (pressedIndex >= 0) pressedIndex else selected
    val position = animateFloatAsState(target.toFloat(), if (motion) spring(.86f, 420f) else snap(), label = "navigation-lens-position")
    val press = animateFloatAsState(if (pressedIndex >= 0 && motion) 1f else 0f, if (motion) spring(.82f, 480f) else snap(), label = "navigation-lens-press")
    GlassPanel(modifier.testTag("glass-navigation"), subtle = true) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(72.dp).padding(8.dp)) {
            val width = maxWidth / 3
            val selectedShape = RoundedCornerShape(20.dp)
            Box(Modifier.width(width - 6.dp).fillMaxHeight().padding(vertical = 2.dp).graphicsLayer {
                translationX = position.value * width.toPx() + 3.dp.toPx()
                val travel = abs(target - position.value).coerceAtMost(1f)
                scaleX = 1f + press.value * .05f + if (motion) travel * .035f else 0f
                scaleY = 1f + press.value * .05f - if (motion) travel * .015f else 0f
            }.clip(selectedShape).background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = .13f),
                MaterialTheme.colorScheme.primary.copy(alpha = .065f))))
                .border(.7.dp, MaterialTheme.colorScheme.primary.copy(alpha = .14f), selectedShape))
            Row(Modifier.fillMaxSize().selectableGroup()) {
                val items = listOf(Icons.Rounded.LibraryMusic to "音乐库", Icons.AutoMirrored.Rounded.QueueMusic to "歌单", Icons.Rounded.Palette to "外观")
                items.forEachIndexed { index, (icon, label) ->
                    val source = remember { MutableInteractionSource() }
                    val down by source.collectIsPressedAsState()
                    val view = LocalView.current
                    LaunchedEffect(down) { if (down) pressedIndex = index else if (pressedIndex == index) pressedIndex = -1 }
                    val emphasis = animateFloatAsState(if (!motion) 1f else if (down) 1.18f else if (selected == index) 1.06f else 1f,
                        if (motion) spring(.82f, 480f) else snap(), label = "navigation-icon-$index")
                    Column(Modifier.weight(1f).fillMaxHeight().selectable(selected == index, role = Role.Tab, interactionSource = source, indication = null,
                        onClick = { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); onSelected(index) }),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(icon, null, Modifier.size(23.dp).graphicsLayer {
                            scaleX = emphasis.value; scaleY = emphasis.value
                            translationY = if (motion) -(emphasis.value - 1f) * 14.dp.toPx() else 0f
                        }, tint = if (selected == index || down) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected == index || down) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
        }
    }
}
