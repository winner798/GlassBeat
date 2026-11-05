# 签名与可重复构建

公开源码不包含任何私钥。开发构建由各构建环境的 Android debug keystore 签名，因此不同环境的 Debug APK 可能不能互相覆盖安装。

正式构建支持以下环境变量：

- `GLASSBEAT_KEYSTORE`：本地 keystore 路径
- `GLASSBEAT_STORE_PASSWORD`
- `GLASSBEAT_KEY_ALIAS`
- `GLASSBEAT_KEY_PASSWORD`

配置后运行 `./gradlew assembleRelease`。未配置时输出未签名包。Release 开启 R8 和资源裁剪，关闭 Android 调试标记。

初始手机验证包来自当前开发环境。为保证这次覆盖升级保留音乐库，当前提供给发起人的升级包会沿用该安装身份；这不是 Google Play 发布密钥方案。长期发行前应由项目所有者备份并接管签名密钥，再把签名材料以 GitHub Secrets 形式配置到可信发布工作流。签名私钥不得放入公开仓库或 CI 普通产物。

变更签名通常需要卸载重装，除非实现 Android 签名轮换。更换前先导出音乐。
