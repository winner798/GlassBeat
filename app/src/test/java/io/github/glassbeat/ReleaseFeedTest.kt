package io.github.glassbeat

import io.github.glassbeat.update.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ReleaseFeedTest {
    private val release = """{"tag_name":"v1.0.3","draft":false,"prerelease":false,"assets":[{"name":"GlassBeat-v1.0.3.apk","size":1234,"url":"https://api.github.com/repos/winner798/GlassBeat/releases/assets/123","browser_download_url":"https://github.com/winner798/GlassBeat/releases/download/v1.0.3/GlassBeat-v1.0.3.apk"}]}"""
    private val manifest = """{"packageName":"io.github.glassbeat","version":"1.0.3","size":1234,"sha256":"${"a".repeat(64)}","downloadUrls":["https://raw.githubusercontent.com/winner798/GlassBeat/release-artifacts/v1.0.3/dist/GlassBeat-v1.0.3.apk"]}"""
    @Test fun latest404FallsBackToPublishedReleaseList() = runBlocking {
        val calls = mutableListOf<String>()
        val result = ReleaseFeed { url -> calls += url; if (url == ReleaseFeed.LATEST) error("HTTP 404") else "[$release]" }.latest("1.0.1")
        assertEquals("1.0.3", result.version)
        assertEquals(listOf(ReleaseFeed.LATEST, ReleaseFeed.LIST), calls)
    }
    @Test fun blockedApisUseRawRepositoryManifest() = runBlocking {
        val result = ReleaseFeed { if (it == ReleaseFeed.MANIFEST) manifest else error("HTTP 404") }.latest("1.0.1")
        assertTrue(result.url.startsWith("https://raw.githubusercontent.com/"))
        assertEquals("1.0.3", result.version)
        assertEquals("a".repeat(64), result.sha256)
    }
    @Test fun staleLatestDoesNotHideNewManifest() = runBlocking {
        val old = release.replace("1.0.3", "1.0.1")
        val result = ReleaseFeed { when (it) { ReleaseFeed.LATEST -> old; ReleaseFeed.LIST -> "[$old]"; else -> manifest } }.latest("1.0.1")
        assertEquals("1.0.3", result.version)
    }
    @Test fun allSourcesFailWithoutClaimingRepositoryHasNoRelease() = runBlocking {
        try { ReleaseFeed { error("HTTP 404") }.latest("1.0.1"); fail("must report unavailable sources") }
        catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("不能据此判断"))
            assertTrue(e.message!!.contains("HTTP 404"))
        }
    }
    @Test fun ignoresPreviewReleasesAndSelectsHighestStableVersion() = runBlocking {
        val preview = release.replace("false,\"assets\"", "true,\"assets\"")
        val older = release.replace("1.0.3", "1.0.2")
        val result = ReleaseFeed { if (it == ReleaseFeed.LATEST) error("404") else "[$preview,$older,$release]" }.latest("1.0.1")
        assertEquals("1.0.3", result.version)
    }
    @Test fun manifestCannotRedirectToAnotherRepository() {
        assertThrows(IllegalArgumentException::class.java) { ReleaseFeed.parseManifest(manifest.replace("winner798/GlassBeat", "attacker/app")) }
    }
}
