package io.github.glassbeat

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareIntentTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun coldAndWarmSharesProduceClearFailureWithoutBreakingActivityLifecycle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "not a link")
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            val error = "抖音解析失败：未找到有效抖音链接，请粘贴包含 https:// 的完整分享内容。\n可重试，或使用本地视频提取。"
            compose.waitUntil(8000) { compose.onAllNodesWithText(error).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("知道了").performClick()
            compose.waitForIdle()
            // Retain the launch action/type so ActivityScenario can track the reused activity.
            // Production onNewIntent intentionally updates Activity.intent to the shared intent.
            scenario.onActivity { activity ->
                activity.startActivity(Intent(intent).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(Intent.EXTRA_TEXT, "another invalid share"))
            }
            compose.waitUntil(8000) { compose.onAllNodesWithText(error).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("知道了").performClick()
        }
    }
}
