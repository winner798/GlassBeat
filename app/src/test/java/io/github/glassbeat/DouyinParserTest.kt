package io.github.glassbeat

import io.github.glassbeat.importer.DouyinPageParser
import org.junit.Assert.*
import org.junit.Test
import java.net.URLEncoder

class DouyinParserTest {
    private val item = """{"aweme_detail":{"desc":"A title with {braces} and \"quotes\"","video":{"duration":12000,"play_addr":{"url_list":["https://v3.douyinvod.com/test.mp4"]}}}}"""
    @Test fun parsesPosterWithoutRequiringItForAudioImport() {
        val withPoster = item.replace("\"duration\":12000", "\"cover\":{\"url_list\":[\"https://p3.example.com/cover.jpg\"]},\"duration\":12000")
        assertEquals("https://p3.example.com/cover.jpg", DouyinPageParser.parse(withPoster, "https://www.douyin.com/video/123")!!.coverUrl)
        assertNull(DouyinPageParser.parse(item, "https://www.douyin.com/video/123")!!.coverUrl)
    }
    @Test fun extractsUrlFromChineseShareText() {
        assertEquals("https://v.douyin.com/ABC/", DouyinPageParser.sharedUrl("3.14 复制链接 https://v.douyin.com/ABC/ 打开抖音"))
    }
    @Test fun rejectsLookalikeHost() {
        assertFalse(DouyinPageParser.isDouyin("douyin.com.attacker.com"))
        assertFalse(DouyinPageParser.isDouyin("evildouyin.com"))
        assertTrue(DouyinPageParser.isDouyin("www.iesdouyin.com"))
        assertThrows(IllegalArgumentException::class.java) { DouyinPageParser.sharedUrl("https://douyin.com.attacker.com/video/123") }
    }
    @Test fun parsesApiDetailWithoutChoosingMusicMetadata() {
        val parsed = DouyinPageParser.parse(item, "https://www.douyin.com/video/123")!!
        assertEquals(12000L, parsed.durationMs)
        assertEquals("https://v3.douyinvod.com/test.mp4", parsed.mediaUrl)
    }
    @Test fun downloadsAcceptRegionalAndPreviouslyUnknownPublicCdns() {
        val public = java.net.InetAddress.getByName("1.1.1.1")
        val network = io.github.glassbeat.importer.MediaNetwork(resolveAddresses = { listOf(public) })
        listOf("v3-dy-o.zjcdn.com", "v9-video.tiktokcdn.com", "regional-new.example.com").forEach { network.requireRemoteMediaHost(it) }
        // Some Android VPNs legitimately resolve public names into this synthetic range.
        assertTrue(network.isRemoteAddress(java.net.InetAddress.getByName("198.18.0.10")))
    }
    @Test fun refusesLocalMediaDestinationsIncludingRedirectTargets() {
        val network = io.github.glassbeat.importer.MediaNetwork()
        listOf("0.0.0.0", "127.0.0.1", "10.1.2.3", "172.16.0.1", "192.168.1.1", "169.254.169.254", "100.64.0.1", "224.0.0.1", "255.255.255.255", "::1", "fe80::1", "fc00::1", "::ffff:127.0.0.1").forEach {
            assertFalse(it, network.isRemoteAddress(java.net.InetAddress.getByName(it)))
        }
        val mixed = io.github.glassbeat.importer.MediaNetwork(resolveAddresses = { listOf(java.net.InetAddress.getByName("1.1.1.1"), java.net.InetAddress.getByName("127.0.0.1")) })
        assertThrows(IllegalArgumentException::class.java) { mixed.requireRemoteMediaHost("redirect.example.com") }
    }
    @Test fun parsesSlidesInfoResponseForOrdinaryVideo() {
        val wrapped = "{\"aweme_details\":[${org.json.JSONObject(item).getJSONObject("aweme_detail")}],\"status_code\":0}"
        val preview = DouyinPageParser.parse(wrapped, "https://www.iesdouyin.com/share/video/123/")!!
        assertEquals(12000L, preview.durationMs)
        assertTrue(preview.mediaUrl.startsWith("https://"))
    }
    @Test fun parsesRouterScriptWithNestedObjects() {
        val html = "<script>window._ROUTER_DATA = {\"loaderData\":{\"video\":$item}};</script>"
        assertNotNull(DouyinPageParser.parse(html, "source"))
    }
    @Test fun parsesUrlEncodedRenderData() {
        val html = "<script id=\"RENDER_DATA\" type=\"application/json\">${URLEncoder.encode(item, "UTF-8")}</script>"
        assertNotNull(DouyinPageParser.parse(html, "source"))
    }
    @Test fun gatedOrMalformedPagesHaveNoPreview() {
        assertNull(DouyinPageParser.parse("<html>请完成验证</html>", "source"))
        assertNull(DouyinPageParser.parse("window._ROUTER_DATA = {bad}", "source"))
        assertNull(DouyinPageParser.parse("{}", "source"))
    }
    @Test fun rejectsInsecureMediaScheme() {
        val data = item.replace("https://v3.douyinvod.com/test.mp4", "file:///etc/private")
        assertNull(DouyinPageParser.parse(data, "source"))
    }
}
