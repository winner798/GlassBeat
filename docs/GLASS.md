# FavSoda 立体玻璃与导航

1.0.5 参考 [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) 的实际源码，而非仅参考截图。研究快照：`65ab177e90e5c1d8c62e70cf7755841982da65f6`（Apache-2.0）。

- [LiquidBottomTabs](https://github.com/Kyant0/AndroidLiquidGlass/blob/65ab177e90e5c1d8c62e70cf7755841982da65f6/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidBottomTabs.kt)：滑动透镜、按压扩张、弹簧与触摸高光。
- [Lens](https://github.com/Kyant0/AndroidLiquidGlass/blob/65ab177e90e5c1d8c62e70cf7755841982da65f6/backdrop/src/commonMain/kotlin/com/kyant/backdrop/effects/Lens.kt)：对背景使用圆角矩形透镜及深度效果。
- [InnerShadow](https://github.com/Kyant0/AndroidLiquidGlass/blob/65ab177e90e5c1d8c62e70cf7755841982da65f6/backdrop/src/commonMain/kotlin/com/kyant/backdrop/shadow/InnerShadow.kt)：用内阴影补足边缘厚度。

本项目未直接引入该库或复制其组件、着色器源码。当前工程仍使用既有 Compose / Haze 依赖，独立实现圆角矩形距离场着色器、宽边倒角与导航交互，避免引入另一套 Kotlin / Compose 版本要求。

材质由共享 Haze 背景、清透色调、背景专用透镜、内边倒角、外边反射和投影组成。Android 13+ 的 RuntimeShader 仅应用于背景层，前景文字、图标和触摸区域不受扭曲。Android 12 以下使用绘制的高光和阴影；低性能模式关闭折射与投影。透明度设置仍控制底色浓度。

1.0.5 初版导航高亮在三个标签间弹性移动，按下时最多扩张约 12%，图标放大到 1.28 倍并上浮；松手完成点击才切换页面并触发轻触反馈。取消触摸恢复原位置。关闭动效或开启低性能模式时立即更新选中状态，不执行弹簧动画。

设备验证涵盖 Android 10 的兼容分支、Android 15 的实际 RuntimeShader 编译和绘制、深浅材质、取消按压与减少动效。测试中的图像差异检查用于确认导航反馈足够明显，不代表所有实体设备的帧率评测。


## 1.0.6 列表与控件

滚动列表共用 `GlassList.kt` 的轻量卡片边界：22dp 圆角、纵向渐变、斜向高光边与轻微投影，歌曲之间留出 10dp 间距。当前播放行使用强调色边框、加粗标题和封面角标。此层不进行背景捕获或运行折射着色器。

`SoftControls.kt` 中的图标、文字、主操作与筛选控件共用 16dp 圆角底座及高光边，维持原有按压弹簧与系统轻触反馈。图标控件保留至少 48dp 的触摸区域；导航继续保留大幅选中胶囊反馈。危险操作保留语义色彩差异。


## 1.0.7 导航收敛

底部导航单独使用轻材质参数：减弱投影、边缘与面反光，去除宽倒角和内层玻璃透镜，保留 Haze 背景层；选中区域改为浅强调色渐变。其他页面的玻璃层次保持原方案。导航高度为 72dp，按压时选中背景约扩张 5%，图标放大到 1.18 倍，弹簧阻尼提高以减少过冲。
