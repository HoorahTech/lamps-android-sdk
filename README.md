# Lamps Android SDK

Lamps Android SDK 为 Android 应用提供统一的初始化、服务端配置、游戏中心页面和广告能力接入。

稳定接入 API 包括：

- `com.lamps.sdk.LampsSdk`
- `com.lamps.sdk.config.LampsConfig`
- `com.lamps.sdk.core.OaidProvider`
- `com.lamps.sdk.core.InitCallback`
- `com.lamps.sdk.view.GameCenterView`

SDK 的 bridge、网络、缓存、广告渠道适配器和服务端响应模型均属于内部实现，不作为稳定 API 使用。

## 环境要求

- Android API 24 及以上
- Kotlin 1.9+ 或兼容的 Java/Kotlin Android 工程
- 网络权限：`android.permission.INTERNET`

## 安装

按需引入 core 和广告渠道模块。未使用的渠道不要引入。

```kotlin
dependencies {
    implementation("com.lamps:core:<version>")
    implementation("com.lamps:pangle:<version>") // 可选
    implementation("com.lamps:ylh:<version>")    // 可选
    implementation("com.lamps:noah:<version>")   // 可选
}
```

GitHub Packages 仓库需要配置 `read:packages` 权限：

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/HoorahTech/lamps-android-sdk")
        credentials {
            username = providers.gradleProperty("gpr.user").get()
            password = providers.gradleProperty("gpr.key").get()
        }
    }
}
```

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

`setOaidProvider` 由宿主提供 OAID 读取逻辑。SDK 不内置 MSA 证书；未设置 provider 或返回空字符串不会阻断初始化。

`startAsync` 完成前不要使用依赖服务端配置的能力。可使用 `LampsSdk.isSdkReady()` 查询当前状态，使用 `LampsSdk.getSdkVersion()` 获取 SDK 版本。

## 游戏中心

服务端下发有效的游戏中心页面地址后，可获取 `GameCenterView` 并添加到宿主布局。`GameCenterView` 内部的 WebView 实现不属于宿主 API。

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

正式构建应启用宿主应用的 R8/ProGuard。SDK release AAR 已启用 R8，并通过 consumer rules 保留上述稳定 API、Android Manifest 组件和 H5 bridge 必需成员；SDK 内部实现允许压缩和重命名。

宿主无需复制 SDK 内部 keep 规则，也不要对 `com.lamps.sdk.**` 添加全包 keep，否则会使内部实现重新暴露并降低混淆效果。

## 调试工具

`sdk-tools` 仅用于内部调试和 Demo 验证，不属于业务接入 API。正式应用不应依赖或调用其内部类。

## 版本与支持

版本号由仓库 `gradle.properties` 中的 `LAMPS_VERSION` 管理。发布到 GitHub Packages 后，同一版本不可覆盖，请递增版本号后重新发布。
