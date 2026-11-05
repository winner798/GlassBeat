# GlassBeat

开源、离线优先的 Android 音乐播放器。Kotlin · Jetpack Compose · Media3。

> 当前为 **0.1.0 / M1 开发原型**，不是已完成的 V1.0。玻璃卡片目前使用半透明材质，尚无实时折射或背景模糊。

## 已实现

- 系统文件选择器批量导入音频，复制至应用私有目录，不依赖源文件授权。
- 本地 JSON 索引持久保存音乐库；搜索歌曲。
- Media3 前台播放服务、媒体通知和系统媒体会话；音频焦点与耳机断开暂停。
- 全屏播放、迷你播放器、进度调整、上一首/下一首、随机与单曲循环。
- 深浅色主题，设置重启保留。
- ACTION_SEND 文本分享入口，明确说明当前未接入链接解析。

## 构建

Android Studio，JDK 17，Android SDK 35。最低 Android 8.0（API 26）。

```sh
./gradlew assembleDebug lintDebug
```

APK：`app/build/outputs/apk/debug/app-debug.apk`。每次 push 的 GitHub Actions 也会构建并上传 debug APK。

## 路线图

- [ ] M1：Room + DataStore 迁移，MVVM 分层、播放错误反馈与设备验收。
- [ ] M2：Media3 Transformer 本地视频音轨提取、导出音乐、可取消导入任务。
- [ ] M2：独立链接解析适配器及抖音兼容性测试；解析失败引导本地视频导入。
- [ ] M3：收藏、歌单、重命名、删除和封面编辑。
- [ ] M3：Haze/液态玻璃评估，自定义背景、字体、透明度及低性能模式。
- [ ] M4：真机测试、截图、签名发布和依赖许可证审查。

不包含在线音乐平台、账号或自建服务。仅处理用户有权保存的内容。

## 手动验收

1. 选择 MP3/M4A/FLAC 等音频，等待导入成功，重启后确认歌曲仍存在。
2. 播放歌曲、调整进度、切歌、随机与循环；锁屏及切至后台检查系统控制。
3. 拔除耳机、测试蓝牙媒体按钮与音频焦点。
4. 切换深浅色并重启；分享文本到 GlassBeat，确认出现未接入解析的说明。
5. 测试空文件、损坏音频、存储空间不足与大文件。可播放格式受设备解码器支持限制。

已在开发环境通过 `assembleDebug` 与 `lintDebug`（JDK 17 / SDK 35）；尚未完成真机验收。卸载应用会删除导入副本；当前尚无导出功能，请保留原文件。

## 参考

独立新建工程，未复制第三方播放器源码。

- [Media3 后台播放官方文档](https://developer.android.com/media/media3/session/background-playback)
- [Compose](https://developer.android.com/compose)
- [规划中的参考项目与许可证记录](docs/DEPENDENCIES.md)

Apache-2.0，见 LICENSE。隐私说明见 [PRIVACY.md](PRIVACY.md)。
