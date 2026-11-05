# 参与贡献

请先在 Issue 中说明问题、Android 版本、设备与复现步骤；不要附带私人音频、账号 Cookie 或带凭据的链接。

使用 JDK 17 与 Android SDK 35。修改后运行 `./gradlew assembleDebug testDebugUnitTest lintDebug`，涉及数据库、导入和界面时同时运行 `connectedDebugAndroidTest`。

新增数据库结构必须提高 Room 版本并编写迁移，不能破坏已有音乐库。新依赖需补充许可证记录。禁止把 keystore、local.properties 或凭据提交到仓库。

链接适配器应与媒体播放服务保持独立，失败时提供明确原因和本地视频备用路径。不得实现登录验证、DRM 或付费访问限制的绕过。
