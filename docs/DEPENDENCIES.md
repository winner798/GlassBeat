# 依赖与许可

GlassBeat 源码以 Apache-2.0 发布，未复制 ShuYin、Seal 或 Chocola 的源码，也未打包 yt-dlp 或 FFmpeg。

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
