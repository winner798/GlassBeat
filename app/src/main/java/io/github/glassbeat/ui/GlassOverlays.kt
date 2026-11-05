package io.github.glassbeat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GlassSheet(dismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color.Black.copy(alpha = .32f),
        dragHandle = null,
        modifier = Modifier.padding(horizontal = 10.dp).imePadding()
    ) {
        GlassPanel(Modifier.fillMaxWidth()) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp, bottom = 6.dp)
                .size(34.dp, 4.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = .22f), RoundedCornerShape(2.dp)))
            content()
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
internal fun SheetHeader(title: String, subtitle: String, closeLabel: String, dismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp, top = 12.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
        }
        SoftFilledTonalIconButton(onClick = dismiss, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .06f))) {
            Icon(Icons.Rounded.Close, closeLabel, Modifier.size(20.dp))
        }
    }
}

@Composable
internal fun GlassDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * .82f
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GlassPanel(Modifier.widthIn(max = 520.dp).fillMaxWidth(.92f).heightIn(max = maxHeight).imePadding()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) { title() }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) { text() }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    dismissButton?.invoke()
                    Spacer(Modifier.width(8.dp))
                    confirmButton()
                }
            }
        }
    }
}

@Composable
internal fun OverlayIcon(icon: ImageVector, modifier: Modifier = Modifier, danger: Boolean = false) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Box(modifier.size(44.dp).background(tint.copy(alpha = .12f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(23.dp), tint = tint)
    }
}
