package io.github.glassbeat.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import io.github.glassbeat.data.copyCancellable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

class AppUpdater(private val context: Context, private val scope: CoroutineScope) {
    private val mutable = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state = mutable.asStateFlow()
    private var job: Job? = null
    @Suppress("DEPRECATION")
    private fun packageInfo(): PackageInfo = context.packageManager.getPackageInfo(context.packageName, signingFlags())
    val installedVersion: String by lazy { packageInfo().versionName.orEmpty() }
    fun dismiss() { if (mutable.value !is UpdateState.Downloading && mutable.value != UpdateState.Checking) mutable.value = UpdateState.Idle }
    fun cancel() { job?.cancel() }
    fun check() {
        if (job?.isActive == true) return
        job = scope.launch {
            mutable.value = UpdateState.Checking
            try {
                val release = withContext(Dispatchers.IO) {
                    val connection = GitHubReleaseNetwork.open("https://api.github.com/repos/winner798/GlassBeat/releases/latest", true)
                    try {
                        val output = ByteArrayOutputStream()
                        connection.inputStream.use { copyCancellable(it, output, 2L * 1024 * 1024) }
                        ReleaseInfo.fromJson(output.toString("UTF-8"))
                    } finally { connection.disconnect() }
                }
                val installed = ReleaseVersion.parse(installedVersion.substringBefore('-')) ?: error("无法识别当前应用版本")
                mutable.value = if (ReleaseVersion.parse(release.version)!! > installed) UpdateState.Available(release)
                    else UpdateState.Latest("当前已是最新版本 $installedVersion")
            } catch (e: CancellationException) { mutable.value = UpdateState.Idle; throw e }
            catch (e: Exception) { mutable.value = UpdateState.Failed(e.message ?: "检查更新失败，请检查网络") }
        }
    }
    fun download(release: ReleaseInfo) {
        if (job?.isActive == true) return
        job = scope.launch {
            val directory = File(context.cacheDir, "updates").apply { mkdirs() }
            val target = File(directory, "GlassBeat-${release.version}.apk")
            val partial = File(directory, "download.part.apk")
            mutable.value = UpdateState.Downloading(release, 0f)
            try {
                withContext(Dispatchers.IO) {
                    require(ReleaseInfo.validAssetUrl(release.url)) { "更新来源无效" }
                    val connection = GitHubReleaseNetwork.open(release.url, false)
                    try {
                        connection.inputStream.use { input -> partial.outputStream().use { output ->
                            copyCancellable(input, output, 100L * 1024 * 1024) { copied -> mutable.value = UpdateState.Downloading(release, (copied.toFloat() / release.size).coerceIn(0f, 1f)) }
                        } }
                    } finally { connection.disconnect() }
                    require(partial.length() == release.size) { "更新文件下载不完整，请重试" }
                    release.sha256?.let { digest -> require(sha256(partial).equals(digest, true)) { "更新文件校验失败，请重试" } }
                    verifyPackage(partial)
                    if (target.exists()) target.delete()
                    require(partial.renameTo(target)) { "无法保存更新文件" }
                }
                mutable.value = UpdateState.Ready(release, target.absolutePath)
            } catch (e: CancellationException) { partial.delete(); mutable.value = UpdateState.Available(release); throw e }
            catch (e: Exception) { partial.delete(); mutable.value = UpdateState.Failed(e.message ?: "下载更新失败，请重试") }
        }
    }
    @Suppress("DEPRECATION")
    private fun signingFlags(): Int = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): Set<String> =
        (if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners.orEmpty() else info.signatures.orEmpty())
            .map { MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).joinToString("") { b -> "%02x".format(b) } }.toSet()
    @Suppress("DEPRECATION")
    internal fun verifyPackage(file: File) {
        val candidate = context.packageManager.getPackageArchiveInfo(file.absolutePath, signingFlags()) ?: error("更新文件不是有效的 APK")
        val installed = packageInfo()
        require(candidate.packageName == context.packageName) { "APK 包名与 GlassBeat 不一致，已拒绝安装" }
        val currentCode = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
        val newCode = if (Build.VERSION.SDK_INT >= 28) candidate.longVersionCode else candidate.versionCode.toLong()
        require(newCode > currentCode) { "此 APK 版本号没有高于当前安装版本" }
        val currentSigners = signers(installed)
        require(currentSigners.isNotEmpty() && currentSigners == signers(candidate)) { "APK 签名与当前应用不一致，已拒绝安装。请使用同一签名的正式发布包。" }
    }
    fun installerIntent(file: File): Intent {
        verifyPackage(file)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        return Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).also { it.clipData = android.content.ClipData.newRawUri("GlassBeat update", uri) }
    }
    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buffer = ByteArray(65536); while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) } }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
