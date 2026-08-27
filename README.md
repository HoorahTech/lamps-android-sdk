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
