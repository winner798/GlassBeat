package io.github.glassbeat.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.glassbeat.LibraryModel
import io.github.glassbeat.update.*
import java.io.File

@Composable
fun AppUpdateDialog(model: LibraryModel) {
    val state by model.updater.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    fun install() {
        val ready = model.updater.state.value as? UpdateState.Ready ?: return
        try { context.startActivity(model.updater.installerIntent(File(ready.path))) }
        catch (e: Exception) { model.message = e.message ?: "无法打开安装页面，请从发布页下载。" }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (context.packageManager.canRequestPackageInstalls()) install()
        else model.message = "尚未允许 FavSoda 安装更新。你可以重新点击安装，或从仓库发布页下载 APK。"
    }
    if (state == UpdateState.Idle) return
    val busy = state == UpdateState.Checking || state is UpdateState.Downloading
    GlassDialog(onDismissRequest = { if (busy) model.updater.cancel() else model.updater.dismiss() }, title = {
        Text(when (state) { UpdateState.Checking -> "正在检查更新"; is UpdateState.Available -> "发现新版本 ${(state as UpdateState.Available).release.version}"; is UpdateState.Downloading -> "正在下载更新"; is UpdateState.Ready -> "更新已准备好"; is UpdateState.Failed -> "更新提示"; else -> "应用更新" })
    }, text = {
        Column(Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("当前版本 ${model.updater.installedVersion}", style = MaterialTheme.typography.bodySmall)
            when (val value = state) {
                UpdateState.Checking -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("从 GitHub Releases 获取最新正式版。") }
                is UpdateState.Latest -> Text(value.message)
                is UpdateState.Failed -> Text(value.message)
                is UpdateState.Available -> {
                    Text("下载大小：%.1f MB".format(value.release.size / 1024f / 1024f))
                    Text(value.release.notes.ifBlank { "此版本未填写更新说明。" })
                    Text("下载后核对包名、版本号与签名，再由系统确认覆盖安装。音乐库和设置会保留。", style = MaterialTheme.typography.bodySmall)
                }
                is UpdateState.Downloading -> { LinearProgressIndicator(progress = { value.progress }, modifier = Modifier.fillMaxWidth()); Text("${(value.progress * 100).toInt()}% · 请保持应用运行，可随时取消。") }
                is UpdateState.Ready -> Text("${value.release.version} 已下载并通过校验。点击安装后，按系统提示完成升级。")
                else -> Unit
            }
        }
    }, confirmButton = {
        when (val value = state) {
            is UpdateState.Available -> TextButton(onClick = { model.updater.download(value.release) }) { Text("下载更新") }
            is UpdateState.Ready -> TextButton(onClick = {
                if (context.packageManager.canRequestPackageInstalls()) install()
                else try { permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))) }
                catch (_: Exception) { model.message = "无法打开安装权限设置，请从系统设置允许 FavSoda 安装应用。" }
            }) { Text("安装更新") }
            UpdateState.Checking, is UpdateState.Downloading -> TextButton(onClick = model.updater::cancel) { Text("取消") }
            else -> TextButton(onClick = model.updater::dismiss) { Text("知道了") }
        }
    }, dismissButton = {
        if (!busy) TextButton(onClick = { uriHandler.openUri(ReleaseInfo.RELEASES) }) { Text("打开发布页") }
    })
}
