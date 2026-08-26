# Lamps Android SDK

Lamps Android SDK 为 Android 应用提供统一的初始化、游戏中心页面、广告能力接入。


## 环境要求

- Android API 24 及以上
- Kotlin 1.9+ 或兼容的 Java/Kotlin Android 工程
- 网络权限：`android.permission.INTERNET`

## 安装

引入 sdk 和需要的广告渠道模块。未使用的渠道不要引入。

```kotlin
dependencies {
    implementation("com.lamps:sdk:<version>")    // 必选
    implementation("com.lamps:pangle:<version>") // 可选
    implementation("com.lamps:ylh:<version>")    // 可选
    implementation("com.lamps:noah:<version>")   // 可选
    debugImplementation("com.lamps:sdk-tools:<version>") // 仅调试
}
```

AAR 和 POM 发布到虎扑 Nexus：

```kotlin
repositories {
    maven("https://nexus.hupu.io/repository/hupu-android-public/")
}
```

发布凭据只配置在 CI 环境变量 `HUPU_NEXUS_USERNAME` / `HUPU_NEXUS_PASSWORD`，不写入项目文件。项目源码仓库为 `http://gitlab.hupu.com/HPBase/lamps-android-sdk.git`。

本地需要同时发布全部模块并准备交付给第三方的 AAR 时，执行：

```bash
./gradlew push
```

该任务会将所有 release AAR 发布到 Nexus，并复制到根目录 `sdk_lib/`，文件名格式为 `lamps-artifactId-version.aar`，例如 `lamps-sdk-0.0.5.aar`。只构建并收集本地 AAR（不上传 Nexus）可执行 `./gradlew collectReleaseAars`。

## 初始化

应在隐私协议同意后初始化 SDK。调用顺序为 `init`，然后 `startAsync`。

```kotlin
import com.lamps.sdk.LampsSdk
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.core.InitCallback
import com.lamps.sdk.core.OaidProvider

val config = LampsConfig.Builder()
    .appId("your_app_id")
    .setOaidProvider(OaidProvider { mediaOaid }) // 可选；返回空值时继续初始化
    .setDebug(false)
    .setCustomData(mapOf("source" to "your_app"))
    .build()

LampsSdk.init(application, config)
LampsSdk.startAsync(object : InitCallback {
    override fun success() {
        // SDK 已完成初始化
    }

    override fun fail(code: Int, message: String?) {
        // 处理初始化失败
    }
})
```

`setOaidProvider` 由宿主提供 OAID 读取逻辑。未设置 provider 或返回空字符串不会阻断初始化。

`startAsync` 完成前不要使用LampsSdk能力。可使用 `LampsSdk.isSdkReady()` 查询当前状态，使用 `LampsSdk.getSdkVersion()` 获取 SDK 版本。

## 游戏中心

可获取 `GameCenterView` 并添加到宿主布局。实现方式如下：

```kotlin
val gameCenterView = LampsSdk.getGameCenterView(this)
if (gameCenterView != null) {
    container.addView(
        gameCenterView,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )
}
```

宿主负责在页面永久销毁时释放资源；不要在普通的列表或 ViewPager detach 场景销毁：

```kotlin
override fun onDestroyView() {
    gameCenterView?.destroy()
    gameCenterView = null
    super.onDestroyView()
}
```

如果在 RecyclerView 中使用，建议在 `onViewRecycled` 中调用 `GameCenterView.destroy()`，并确保销毁后的 View 不再重新绑定。

## 初始化错误码

| code | 含义 |
| --- | --- |
| 1001 | 未调用 `init` |
| 1002 | `appId` 为空 |
| 1004 | Context 不可用 |
| 1005 | 服务端初始化配置请求失败，且无可用缓存 |
| 1006 | 初始化正在进行 |
| 1007 | 穿山甲 SDK 初始化失败 |
| 1008 | 优量汇 SDK 初始化失败 |
| 1009 | 汇川 SDK 初始化失败 |
| 1010 | 第三方 SDK 初始化分发失败 |

## 混淆与发布

SDK内部已经实现并管理好混淆规则，接入方无需关心

## 调试工具

`sdk-tools` 仅用于内部调试和 Demo 验证，不属于业务接入 API。正式应用不应依赖或调用其内部类。
