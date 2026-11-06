# 播放队列一致性

## 顺序与操作映射

队列读取播放器 Timeline 的实际顺序：从 `getFirstWindowIndex(shuffleModeEnabled)` 开始，以 `getNextWindowIndex(index, REPEAT_MODE_OFF, shuffleModeEnabled)` 遍历。单曲循环不截断显示列表。中间态不完整时回退到原索引列表，避免丢失项目。

每个 `QueueEntry` 保留原始 `timelineIndex` 和 `MediaItem`。界面显示位置与播放器命令索引分开处理：点击播放和移出歌曲使用原始索引；高亮及“定位当前”使用当前项目在显示列表中的位置。

设备测试使用固定随机顺序 `Three → One → Two`，验证显示顺序、当前行位置，以及点击和移出后实际操作的是对应歌曲。

## 媒体资料更新

资料库歌曲变化后，`syncQueueMetadata` 比较标题、艺术家、封面 URI 和时长。只有这些字段不同才更新媒体信息；收藏状态等变化不触发替换。

更新通过原项目 `buildUpon().setMediaMetadata(...)` 构造，保留原始媒体来源，不重建整个队列。设备测试核对标题、艺术家与封面更新，并验证当前进度、播放意图和随机顺序保持一致。

字体、图标和玻璃样式沿用现有资源。本轮改动重点是队列行为与验收一致性。
