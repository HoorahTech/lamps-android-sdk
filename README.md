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
    implementation("io.github.hoorahtech:sdk:<version>")    // 必选
    implementation("io.github.hoorahtech:pangle:<version>") // 可选
    implementation("io.github.hoorahtech:ylh:<version>")    // 可选
    implementation("io.github.hoorahtech:noah:<version>")   // 可选，已内置 Noah SDK
    debugImplementation("io.github.hoorahtech:sdk-tools:<version>") // 仅调试
}
```

AAR、POM、sources/Javadoc 和签名文件发布到 Maven Central：

```kotlin
repositories {
    mavenCentral()
}
```

发布目标由根目录 `gradle.properties` 的 `LAMPS_PUBLISH_TARGET` 决定，默认是 `maven`，发布到可写的 `https://nexus.hupu.io/repository/hupu-android/`；改为 `mavenCentral` 才会发布到 Maven Central。每次执行真实发布前，发布人必须确认当前目标和版本。Nexus 凭据使用 `hupuNexusUsername` / `hupuNexusPassword`，也可通过 `HUPU_NEXUS_USERNAME` / `HUPU_NEXUS_PASSWORD` 注入；Central 发布时必须将 `mavenCentralUsername` / `mavenCentralPassword` 作为 Gradle project properties 传入，或配置在 `~/.gradle/gradle.properties`，GPG 签名继续使用对应的 `signingInMemory*` 参数。所有凭据只放在本机配置或 CI Secret，不写入版本库。项目源码仓库为 `http://gitlab.hupu.com/HPBase/lamps-android-sdk.git`。

本地需要同时发布全部模块并准备交付给第三方的 AAR 时，执行：

```bash
./gradlew push
```

发布前先检查并确认目标：

```bash
grep '^LAMPS_PUBLISH_TARGET=' gradle.properties
```

也可以临时覆盖目标，例如 `./gradlew push -PLAMPS_PUBLISH_TARGET=mavenCentral -PmavenCentralUsername=<token-user> -PmavenCentralPassword=<token-password>`。未明确确认目标前，不执行真实上传。

该任务会将所有 release AAR 发布到所选仓库，并复制到根目录 `sdk_lib/`，文件名格式为 `lamps-artifactId-version.aar`，例如 `lamps-sdk-0.0.5.aar`。只构建并收集本地 AAR（不上传仓库）可执行 `./gradlew collectReleaseAars`。

Pangle 的定制二进制不随统一 `publishAll` 发布；需要单独执行对应 module 的 Maven Central 发布任务。坐标保持其 SDK 版本并使用 `pangle-` 前缀：`io.github.hoorahtech:pangle-ads-sdk-pro:7.6.1.2` 和 `io.github.hoorahtech:pangle-ads-sdk-tools:7.6.4.2`（Maven 版本不带 `-hupu`）。对应 release AAR 文件名为 `pangle-ads-sdk-pro-release.aar` 和 `pangle-ads-sdk-tools-release.aar`。因此 `pangle`、`sdk-tools` 的新版本不再依赖公司 Nexus 中的 `com.pangle.cn` 坐标。

## 初始化

应在隐私协议同意后初始化 SDK。调用顺序为 `init`，然后 `startAsync`。

```kotlin
import com.lamps.sdk.LampsSdk
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.config.NightMode
import com.lamps.sdk.core.InitCallback
import com.lamps.sdk.core.OaidProvider

val config = LampsConfig.Builder()
    .appId("your_app_id")
    .setOaidProvider(OaidProvider { mediaOaid }) // 可选；返回空值时继续初始化
    .setNightModeProvider { NightMode.DAY } // 可选；每次打开游戏中心时读取
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

`setNightModeProvider` 由宿主提供全局日夜间。每次 `navigateToGameCenter` / `getGameCenterView` 都会重新读取。`GameCenterConfig.setNightMode` 有值时优先用单次配置，未设再用这个全局值；都未设置时默认日间。

`startAsync` 完成前不要使用LampsSdk能力。可使用 `LampsSdk.isSdkReady()` 查询当前状态，使用 `LampsSdk.getSdkVersion()` 获取 SDK 版本。

## 游戏中心

打开游戏中心时可传入 `GameCenterConfig`。日夜间优先 `setNightMode`；未设则每次从 `LampsConfig.setNightModeProvider` 读取；仍没有则默认日间。后续扩展字段加在 Builder 上。

```kotlin
import com.lamps.sdk.config.GameCenterConfig
import com.lamps.sdk.config.NightMode

val gameCenterConfig = GameCenterConfig.Builder()
    .setNightMode(NightMode.NIGHT) // 可选；不设则用全局 provider
    .build()

LampsSdk.navigateToGameCenter(this, gameCenterConfig)
```

可获取 `GameCenterView` 并添加到宿主布局。实现方式如下：

```kotlin
val gameCenterView = LampsSdk.getGameCenterView(this, gameCenterConfig)
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

### 运行时更新配置

View 已经添加到宿主布局后，宿主自身状态发生变化（典型场景是宿主切换日夜间）时，调用 `GameCenterView.updateConfig` 同步给 SDK，调用时机由宿主控制：

```kotlin
// 宿主切换日夜间时调用
gameCenterView?.updateConfig(
    GameCenterConfig.Builder()
        .setNightMode(NightMode.DAY)
        .build()
)
```

说明：

- 目前生效字段为日夜间。传入的 `GameCenterConfig` 未设 `setNightMode` 时，按 `LampsConfig.setNightModeProvider` 现取；都没有则日间。
- 与当前值相同时不会重复下发，不会触发 H5 重复渲染。
- 可从任意线程调用；`destroy()` 之后调用无效果，不会抛异常。
- 整页模式 `navigateToGameCenter` 的日夜间在打开时确定，没有这个入口。
- H5 侧通过 `lamps.common.onnightmodechange` 事件接收变化，详见 [docs/bridge/NightModeEvent.md](docs/bridge/NightModeEvent.md)。

宿主负责在 View 不再会被复用时释放资源。判断标准是"这个 `GameCenterView` 实例之后还会不会被重新展示"：不会就必须 `destroy()`，`destroy()` 之后的实例不能再加回布局。

```kotlin
override fun onDestroyView() {
    gameCenterView?.destroy()
    gameCenterView = null
    super.onDestroyView()
}
```

几个容易漏的场景：

- `ViewPager2` + `FragmentStateAdapter`：ViewHolder 被回收时 Fragment 会被真正移除，所以 `onDestroyView` 里必须 `destroy()`，否则来回滑 tab 会攒出多个还在跑的 WebView。
- `RecyclerView`：在 `onViewRecycled` 中 `destroy()`，并确保销毁后的 View 不再重新绑定；更省心的做法是整个列表只持有一个实例并复用，不要在 `onBindViewHolder` / `getView` 里新建。
- 只是临时从父容器 detach（View 之后还会加回来）时不要 `destroy()`，`destroy()` 是一次性的。

`GameCenterView.destroy()` 可以重复调用，第二次及以后无效果，所以宿主"多兜一次"是安全的。

## 打开游戏

初始化完成后，可传入 `gameId` 打开游戏页。SDK 会用服务端下发的 `gamePlayPageTemplate` 将 `__GAMEID__` 替换为传入的 `gameId`，再跳转到游戏 WebView。

```kotlin
LampsSdk.navigateToGame(this, gameId)
```

`gameId` 为空、模板未下发或替换后不是完整 `http(s)` URL 时不会跳转。

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
