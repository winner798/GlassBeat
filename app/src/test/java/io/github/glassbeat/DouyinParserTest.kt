package io.github.glassbeat

import io.github.glassbeat.importer.DouyinPageParser
import org.junit.Assert.*
import org.junit.Test
import java.net.URLEncoder

class DouyinParserTest {
    private val item = """{"aweme_detail":{"desc":"A title with {braces} and \"quotes\"","video":{"duration":12000,"play_addr":{"url_list":["https://v3.douyinvod.com/test.mp4"]}}}}"""
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
