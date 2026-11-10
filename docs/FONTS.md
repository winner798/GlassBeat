# 按需字体

外观 → 发现字体。目录只保存名称、大小、固定版本地址和摘要；浏览不发起下载。用户选择后下载完整字体与对应 OFL，下载完成自动应用。已有 TTF/OTF 导入保留，最高 32 MiB。

| 字体 | 约大小 | 来源 |
| --- | --- | --- |
| Noto Sans SC | 17.8 MB | Google Fonts / ofl/notosanssc |
| Noto Serif SC | 25.1 MB | Google Fonts / ofl/notoserifsc |
| 马善政楷书 | 5.9 MB | Google Fonts / ofl/mashanzheng |
| 站酷快乐体 | 1.5 MB | Google Fonts / ofl/zcoolkuaile |

固定 Google Fonts 提交：`5bb28733cec313b63fbd415b574b5ae35c8aa8b8`。字体及许可证 SHA-256 见 `FontCatalog.kt`。均使用 SIL Open Font License，许可证与字体一同保存，可通过每项的「开源许可」查看上游文本。上游：[google/fonts](https://github.com/google/fonts/tree/5bb28733cec313b63fbd415b574b5ae35c8aa8b8/ofl)。这些字体的完整二进制文件不进入源码仓库或 APK。

下载在 IO 线程运行，不持有音乐库操作锁。先在临时目录验证许可证、完整字节数、字体 SHA-256 及 Android Typeface，成功后原子重命名，最后更新主题。取消或出错清理临时目录，旧字体继续使用。进程被终止后，下次启动清理未完成的临时目录。

已下载字体离线应用时仍校验完整性；切换/恢复系统字体会保留下载缓存以便复用。删除下载字体释放存储，若删除当前字体则恢复系统字体。仅保留一份导入字体，替换后清理旧导入文件。品牌展示字标独立于正文设置。

测试覆盖：缓存离线复用、许可证留存、摘要不符、取消、无效许可证和字体加载失败的清理，以及 Android 实际 Typeface 加载。设备界面测试同时检查字体入口、目录及关闭交互。
