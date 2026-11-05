package io.github.glassbeat

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.glassbeat.update.AppUpdater
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class UpdaterPolicyTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Test fun rejectsSameVersionAndWrongPackageBeforeInstallation() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            val updater = AppUpdater(context, scope)
            val same = assertThrows(IllegalArgumentException::class.java) { updater.verifyPackage(File(context.applicationInfo.sourceDir)) }
            assertTrue(same.message!!.contains("版本号"))
            val testPackage = InstrumentationRegistry.getInstrumentation().context.applicationInfo.sourceDir
            val wrong = assertThrows(IllegalArgumentException::class.java) { updater.verifyPackage(File(testPackage)) }
            assertTrue(wrong.message!!.contains("包名"))
        } finally { scope.cancel() }
    }
    @Test fun fileProviderOnlyExposesUpdateCache() {
        val file = File(File(context.cacheDir, "updates").apply { mkdirs() }, "fixture.apk").apply { writeText("fixture") }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
            assertEquals("content", uri.scheme)
            context.contentResolver.openInputStream(uri)!!.use { assertEquals("fixture", it.reader().readText()) }
            assertThrows(IllegalArgumentException::class.java) { FileProvider.getUriForFile(context, "${context.packageName}.updates", File(context.filesDir, "private-music")) }
        } finally { file.delete() }
    }
}
