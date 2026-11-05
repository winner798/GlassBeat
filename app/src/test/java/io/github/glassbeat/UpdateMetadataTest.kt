package io.github.glassbeat

import io.github.glassbeat.update.*
import org.junit.Assert.*
import org.junit.Test

class UpdateMetadataTest {
    private fun release(tag: String = "v1.1.0", url: String = "https://github.com/winner798/GlassBeat/releases/download/v1.1.0/GlassBeat-v1.1.0.apk", draft: Boolean = false, prerelease: Boolean = false) =
        """{"tag_name":"$tag","body":"Changes","draft":$draft,"prerelease":$prerelease,"assets":[{"name":"GlassBeat-v1.1.0.apk","size":1234,"browser_download_url":"$url","digest":"sha256:${"a".repeat(64)}"}]}"""
    @Test fun comparesVersionsNumerically() {
        assertTrue(ReleaseVersion.parse("v1.10.0")!! > ReleaseVersion.parse("1.9.9")!!)
        assertEquals(ReleaseVersion.parse("v1.0.0"), ReleaseVersion.parse("1.0.0"))
        assertNull(ReleaseVersion.parse("1.0.0-rc1"))
        assertNull(ReleaseVersion.parse("release-2026"))
    }
    @Test fun acceptsOwnerReleaseApkAndDigest() {
        val value = ReleaseInfo.fromJson(release())
        assertEquals("1.1.0", value.version)
        assertEquals(1234L, value.size)
        assertEquals("a".repeat(64), value.sha256)
    }
    @Test fun rejectsOtherRepositoriesAndInsecureDownloads() {
        listOf("https://github.com/attacker/app/releases/download/v1/app.apk", "http://github.com/winner798/GlassBeat/releases/download/v1/app.apk", "https://github.com.attacker.com/winner798/GlassBeat/releases/download/v1/app.apk").forEach { url ->
            assertThrows(IllegalArgumentException::class.java) { ReleaseInfo.fromJson(release(url = url)) }
        }
    }
    @Test fun prefersOfficialAssetApiForDownloads() {
        val root = org.json.JSONObject(release())
        val api = "https://api.github.com/repos/winner798/GlassBeat/releases/assets/123"
        root.getJSONArray("assets").getJSONObject(0).put("url", api)
        assertEquals(api, ReleaseInfo.fromJson(root.toString()).url)
    }
    @Test fun rejectsOtherRepositoryAssetApis() {
        val root = org.json.JSONObject(release())
        root.getJSONArray("assets").getJSONObject(0).put("url", "https://api.github.com/repos/attacker/app/releases/assets/123")
        assertThrows(IllegalArgumentException::class.java) { ReleaseInfo.fromJson(root.toString()) }
    }
    @Test fun rejectsDraftAndPreviewReleases() {
        assertThrows(IllegalArgumentException::class.java) { ReleaseInfo.fromJson(release(draft = true)) }
        assertThrows(IllegalArgumentException::class.java) { ReleaseInfo.fromJson(release(prerelease = true)) }
    }
}
