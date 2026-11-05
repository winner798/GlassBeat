package io.github.glassbeat.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected

private class PressFeedback(val source: MutableInteractionSource, val modifier: Modifier, val click: () -> Unit)

@Composable
private fun pressFeedback(onClick: () -> Unit): PressFeedback {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val appearance = LocalAppearance.current
    val animate = appearance.motion && !appearance.lowPower
    val scale = animateFloatAsState(if (pressed && animate) .96f else 1f,
        animationSpec = if (animate) spring(dampingRatio = .78f, stiffness = 650f) else snap(), label = "soft-press")
    val view = LocalView.current
    return PressFeedback(source, Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
        // Honor the device's touch-feedback setting. Only completed clicks tick, never scrubbing.
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onClick()
    }
}

@Composable
internal fun SoftIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable () -> Unit) {
    val press = pressFeedback(onClick)
    IconButton(press.click, modifier.then(press.modifier).controlSkin(), enabled = enabled, interactionSource = press.source, content = content)
}

@Composable
internal fun SoftFilledIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable () -> Unit) {
    val press = pressFeedback(onClick)
    FilledIconButton(press.click, modifier.then(press.modifier).controlSkin(), enabled = enabled, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), interactionSource = press.source, content = content)
}

@Composable
internal fun SoftFilledTonalIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(), content: @Composable () -> Unit) {
    val press = pressFeedback(onClick)
    FilledTonalIconButton(press.click, modifier.then(press.modifier).controlSkin(), enabled = enabled, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = colors, interactionSource = press.source, content = content)
}

@Composable
internal fun SoftButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), content: @Composable RowScope.() -> Unit) {
    val press = pressFeedback(onClick)
    Button(press.click, modifier.then(press.modifier).controlSkin(), enabled = enabled, shape = shape, interactionSource = press.source, content = content)
}

@Composable
internal fun SoftOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val press = pressFeedback(onClick)
    OutlinedButton(press.click, modifier.then(press.modifier).controlSkin(), enabled = enabled, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), interactionSource = press.source, content = content)
}

@Composable
internal fun SoftTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(), content: @Composable RowScope.() -> Unit) {
    val press = pressFeedback(onClick)
    TextButton(press.click, modifier.then(press.modifier).controlSkin(), enabled = enabled, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = colors, interactionSource = press.source, content = content)
}

@Composable
internal fun SoftSurface(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape, color: Color, content: @Composable () -> Unit) {
    val press = pressFeedback(onClick)
    Surface(press.click, modifier.then(press.modifier).listCard(), enabled = enabled, shape = shape, color = color, interactionSource = press.source, content = content)
}

@Composable
internal fun Modifier.softClickable(onClick: () -> Unit): Modifier {
    val press = pressFeedback(onClick)
    return then(press.modifier).clickable(interactionSource = press.source, indication = LocalIndication.current, onClick = press.click)
}

@Composable
internal fun SoftFilterChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit, leadingIcon: (@Composable () -> Unit)? = null) {
    val press = pressFeedback(onClick)
    Surface(onClick = press.click, modifier = Modifier.then(press.modifier).controlSkin().semantics { this.selected = selected },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .20f) else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        interactionSource = press.source) {
        androidx.compose.foundation.layout.Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            leadingIcon?.let { it(); androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp)) }
            label()
        }
    }
}
