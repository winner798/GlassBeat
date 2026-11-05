package io.github.glassbeat

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.glassbeat.data.LibraryRepository
import io.github.glassbeat.data.Song
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class UiSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LibraryRepository(context) }
    private lateinit var audio: File
    @Before fun seed() = runBlocking {
        audio = File(context.filesDir, "ui-sample.mp4")
        InstrumentationRegistry.getInstrumentation().context.assets.open("test-video.mp4").use { source -> audio.outputStream().use { source.copyTo(it) } }
        repo.dao.save(Song("ui-test", "First Light", audio.absolutePath, "GlassBeat · Test Tone", 1500))
    }
    @After fun clean() = runBlocking { repo.dao.delete("ui-test"); audio.delete(); Unit }
    private fun screenshot(name: String) {
        val directory = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun navigationThemeAndImportAreUsable() {
        compose.onNodeWithText("GlassBeat").assertIsDisplayed()
        compose.waitUntil(5000) { compose.onAllNodesWithText("First Light").fetchSemanticsNodes().isNotEmpty() }
        screenshot("library")
        compose.onNodeWithText("外观").performClick()
        compose.onNodeWithText("外观工作室").assertIsDisplayed()
        screenshot("themes")
        compose.onNodeWithText("检查更新").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("浅色玻璃").performScrollTo()
        compose.onNodeWithText("浅色玻璃").performClick()
        compose.onNodeWithText("歌单", useUnmergedTree = true).performClick()
        compose.onNodeWithText("＋ 新建歌单").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Device smoke test")
        compose.onNodeWithText("保存").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Device smoke test").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("音乐库", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("导入音乐").performClick()
        compose.onNodeWithText("本地视频提取音轨").assertIsDisplayed()
        compose.onNodeWithText("关闭").performClick()
        compose.onNodeWithText("First Light").performClick()
        compose.waitUntil(8000) { compose.onAllNodesWithContentDescription("收起播放器").fetchSemanticsNodes().isNotEmpty() }
        screenshot("player")
        compose.waitUntil(8000) { compose.onAllNodesWithContentDescription("播放").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("播放").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("暂停").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("收起播放器").performClick()
    }
    @Test fun malformedSharedTextProducesClearFailure() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.startActivity(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "not a link"))
        }
        compose.waitUntil(8000) { compose.onAllNodesWithText("抖音解析失败：未找到有效抖音链接，请粘贴包含 https:// 的完整分享内容。\n可重试，或使用本地视频提取。").fetchSemanticsNodes().isNotEmpty() }
    }
}
