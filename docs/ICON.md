# FavSoda 图标

1.0.7 图标以玻璃音符融合字母 f 的形态，配一颗气泡。前景用图像生成工具制作，按 Android 资源规格导出 512px 透明 PNG；背景与主题单色版本使用矢量资源。1.0.8 将背景调整为珍珠白到淡蓝的浅色渐变（`#FAFCFF` → `#EDF3FC` → `#DCE7F7`）。

- [彩色前景](../app/src/main/res/drawable-nodpi/ic_launcher_art.png)
- [渐变背景](../app/src/main/res/drawable/ic_launcher_background.xml)
- [主题单色版本](../app/src/main/res/drawable/ic_launcher_monochrome.xml)
- [圆形预览](screenshots/icon-circle.png) · [圆角方形预览](screenshots/icon-rounded-square.png)

应用通过 `mipmap-anydpi-v26` 接入自适应图标与圆形入口，通过 `mipmap-anydpi-v33` 提供主题单色图标。前景保留透明边距，并额外内缩 7dp，以适配 108dp 图层中的约 66dp 安全区域。主题单色版简化为可着色的音符/字母轮廓，不把深色底板绘入前景。

设备测试实际加载应用图标、检查可见前景安全区并渲染两种常见遮罩，确保主体在裁切时完整显示。桌面的最终遮罩由系统启动器决定。
