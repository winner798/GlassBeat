# 依赖与许可

FavSoda 源码以 Apache-2.0 发布，未复制 ShuYin、Seal 或 Chocola 的源码，也未打包 yt-dlp 或 FFmpeg。

| 直接依赖 | 版本 | 上游许可 |
| --- | --- | --- |
| Android Gradle Plugin | 8.9.2 | Apache-2.0 |
| Kotlin / Compose Compiler | 2.1.20 | Apache-2.0 |
| KSP | 2.1.20-1.0.32 | Apache-2.0 |
| AndroidX Compose BOM | 2025.04.01 | Apache-2.0 |
| AndroidX Activity | 1.10.1 | Apache-2.0 |
| AndroidX Lifecycle | 2.8.7 | Apache-2.0 |
| AndroidX Core | 1.16.0 | Apache-2.0 |
| AndroidX Media3 | 1.6.1 | Apache-2.0 |
| AndroidX Room | 2.7.1 | Apache-2.0 |
| AndroidX DataStore | 1.1.4 | Apache-2.0 |
| Coil | 2.7.0 | Apache-2.0 |
| Haze | 1.5.3 | Apache-2.0 |

测试使用 JUnit 4（EPL-1.0）、AndroidX Test（Apache-2.0）、JSON-java（public domain）。测试素材是仓库生成的纯色画面和 440Hz 正弦波，不包含第三方音乐。FFmpeg 仅用于生成测试素材，不是应用依赖。

完整运行时依赖可通过 `./gradlew :app:dependencies --configuration debugRuntimeClasspath` 查看。标准开源许可原文见 [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0)。AndroidX 源码及许可：[AndroidX](https://android.googlesource.com/platform/frameworks/support/)；[Media3](https://github.com/androidx/media)；[Coil](https://github.com/coil-kt/coil)；[Haze](https://github.com/chrisbanes/haze)。

抖音公开 slidesinfo 兼容路径参考 [wujunwei928/parse-video-py](https://github.com/wujunwei928/parse-video-py)，MIT 声明保留于根目录 THIRD_PARTY_NOTICES，并随 APK 的 assets 分发。没有引入 Python 运行时或远程解析服务。

1.0.5 立体玻璃与导航的源码研究及兼容性说明见 [GLASS.md](GLASS.md)。AndroidLiquidGlass 为设计/算法思路参考，未作为打包依赖，也未复制其源码。

FavSoda Bubble 字标字体派生自 [Fredoka](https://github.com/google/fonts/tree/main/ofl/fredoka)，SIL OFL-1.1。已实例化为静态 Bold 字重、重命名并保留 Basic Latin 子集；字体及完整许可证随源码和 APK 分发。只用于品牌字标。

中文气泡页面标题派生自 Google Fonts 的 ZCOOL KuaiLe（SIL OFL-1.1），重命名并仅保留固定标题所需字符以减小体积；完整许可证随源码和 APK 分发。
