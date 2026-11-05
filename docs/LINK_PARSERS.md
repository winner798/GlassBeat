# 分享解析实现调研（2026-11-05）

| 仓库 | 当前观察到的最近推送 | 许可 | 对 GlassBeat 的判断 |
| --- | --- | --- | --- |
| [wujunwei928/parse-video-py](https://github.com/wujunwei928/parse-video-py) | 2026-11-04 | MIT | 优先参考。实现中新增公开 slidesinfo 路径，可在旧分享页无媒体时获取普通视频详情。本次给定链接已实测返回数据。Kotlin 独立适配，不打包 Python。 |
| [jiji262/douyin-downloader](https://github.com/jiji262/douyin-downloader) | 2026-11-01 | MIT | README 明确说明单视频等 CLI 下载受当前请求验证阻断，浏览器备用也不保证成功；不能因 Star 多就当作可直接替换的解决方案。 |
| [Johnserf-Seed/f2](https://github.com/Johnserf-Seed/f2) | 2026-10-19 | Apache-2.0 | Python 多平台工具，可作为接口维护参考；不直接增加到轻量 Android 应用。未用本次视频验证其完整下载能力。 |
| [Evil0ctal/Douyin_TikTok_Download_API](https://github.com/Evil0ctal/Douyin_TikTok_Download_API) | 2026-11-02 | Apache-2.0 | 自托管 API，架构与 V1 不搭服务器的约束不符。仅作为兼容性研究来源，未接入它的服务。 |

日期是 GitHub 的 pushed_at，不等于该解析算法最近一次修复日期，也不证明所有链接有效。项目状态可能继续变化。

## 采用的路径

1. 接收分享文案并展开短链。
2. 解析网页已有的公开媒体信息。
3. 页面信息不完整时，使用 `https://www.iesdouyin.com/web/api/v2/aweme/slidesinfo/?aweme_ids=[视频ID]`（实际请求对方括号进行 URL 编码）。
4. 解析 aweme_details，得到视频的完整音轨来源；确认后下载视频并通过 Media3 提取音轨。
5. 接口不返回数据时，提供失败说明和本地视频备用路径。

不剥离水印、不伪造账号 Cookie、不运行验证码或登录验证代码。完整视频音轨与平台标注原曲是不同内容。

MIT 归属与许可见 [THIRD_PARTY_NOTICES](../THIRD_PARTY_NOTICES)。Android 实现位于 `importer/DouyinResolver.kt`。

## 本次验证结果

不只是调用外部脚本：在本地临时 JVM 测试中直接调用 GlassBeat 的 `DouyinResolver.resolve` 与 `MediaNetwork.download`，成功解析并下载发起人提供的视频。时长 358956 ms，下载 72747738 字节。测试只在开发环境执行，不把真实分享链接写入自动测试源码或把视频打包到 APK。
