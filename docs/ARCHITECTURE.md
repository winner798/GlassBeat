# 架构

单 Activity + Jetpack Compose + AndroidViewModel。数据通过 Room Flow 与 DataStore Flow 提供；界面使用生命周期感知的订阅。

| 层 | 职责 |
| --- | --- |
| MainActivity | 系统分享与 MediaController 生命周期 |
| ui/ | 音乐库、歌单、播放器、主题编辑器、Haze 玻璃材质 |
| LibraryModel | 页面状态、可取消导入、用户操作编排 |
| data/LibraryRepository | 文件复制、媒体元数据、导出、旧版迁移 |
| data/LibraryDatabase | Room 歌曲、歌单与带外键级联的成员关系 |
| data/ThemeStore | DataStore 外观设置与 SharedPreferences 升级迁移 |
| importer/AudioExtractor | 在主 Looper 调用 Media3 Transformer，输出 AAC/M4A |
| importer/DouyinResolver | 解析公开 JSON/网页数据，返回标题、时长与媒体地址 |
| importer/MediaNetwork | HTTPS、站点白名单、有限重定向、超时及下载上限 |
| PlaybackService | ExoPlayer、MediaSession、音频焦点和耳机断开处理 |

## 升级与一致性

0.1.0 的 library.json 会在 Room 事务中导入，成功后改名保留为 library.json.migrated。使用原歌曲 ID 和音频位置，不重新复制已有音乐；重复执行不会覆盖已有编辑。原 SharedPreferences 的 light 字段自动迁入 DataStore。

音频导入完成媒体检查后才写入数据库。失败与取消会清理未入库文件；清理前查询数据库引用，避免取消发生在提交边界时删除成功入库的文件。歌曲删除先移除数据库记录，再清理副本。Room 外键自动清理歌单关联。

## 导入生命周期

导入由 ViewModel 内的协程串行执行，旋转屏幕、切换标签和正常切后台时仍可继续。用户可取消；Transformer 的创建、进度查询、取消均在主 Looper。强制停止应用或系统结束进程会中断未完成任务，当前版本不支持断点续传。后台播放由独立媒体前台服务负责。

## 视觉兼容

Haze 1.5.3 提供背景采样与模糊；边缘渐变高光模拟玻璃反光。Android 12 以下或低性能模式回退到半透明面板。未集成 AndroidLiquidGlass：当前实现不声称具有物理准确的光学折射。模糊只用在导航、迷你播放条、播放控制面板和少量主题卡片。

## 链接处理边界

当前实现支持抖音分享短链、公开页面路由数据、RENDER_DATA 与公开详情 JSON；网站可能要求验证或变更响应结构。不存在账号登录、验证码绕过或长期可用性保证。解析器失败返回可读说明，用户可选择本地视频。视频音轨即视频的完整声音，不替换为平台标注的原曲。
