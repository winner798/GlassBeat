# 当前架构与演进

单 Activity + Compose。LibraryModel 使用 AndroidViewModel 管理导入和 UI 状态；IO 在 Dispatchers.IO 执行。

- MainActivity：连接 MediaController、系统文件选择器、音乐库与全屏播放界面。
- LibraryModel：复制音频、写入本地索引、持久化主题。
- PlaybackService：独立于界面的 ExoPlayer 和 MediaSession 生命周期。

当前为小型 M1 原型，使用 JSON 索引和 SharedPreferences，未声称已经实现规划中的 Room/DataStore。索引通过同目录临时文件重命名更新。后续先抽取 LibraryRepository，迁移 Room 与 DataStore，再增加导入任务队列。

分享入口只接收文本并提供明确反馈；不存在可用的抖音解析器。未来 LinkResolver 应返回结构化成功/失败结果，网络解析不能耦合播放器服务。

后台播放与系统控制仍需 Android 8/13/15 真机或模拟器验收；当前不实现重启后的播放队列恢复。
