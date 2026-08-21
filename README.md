# Lamps Android SDK

独立 Android SDK，提供初始化、WebView Bridge 和可插拔激励视频能力。OAID 由媒体传入，SDK 不内置 MSA 证书。

广告网络按 GroMore 方式拆 module：宿主只依赖需要的联盟。

## 工程

- `core/`：初始化、Bridge、激励分发与监测。坐标 `com.lamps:core`
- `pangle/`：穿山甲。坐标 `com.lamps:pangle`
- `ylh/`：优量汇。坐标 `com.lamps:ylh`
- `noah/`：汇川。坐标 `com.lamps:noah`
- `demo/`：接入示例，可按需注释掉某个联盟 module
- `sdk-tools/`：调试页

`pangle` / `ylh` / `noah` 依赖 `core`，进程启动时通过 ContentProvider 向 core 注册。`startAsync` 按远端 `rewardAdSlots` 把初始化、激励 load/show、竞价回传分发给**已引入且匹配 channel 的** module。

## 接入

Gradle 按需勾选联盟（不要的 module 不要写进依赖即可）：

```kotlin
implementation("com.lamps:core:0.1.0")
implementation("com.lamps:pangle:0.1.0")
implementation("com.lamps:ylh:0.1.0")
implementation("com.lamps:noah:0.1.0")
debugImplementation("com.lamps:sdk-tools:0.1.0")
```

本地工程：

```kotlin
implementation(project(":core"))
implementation(project(":pangle"))
implementation(project(":ylh"))
implementation(project(":noah"))
```

隐私协议同意后调用，顺序与穿山甲一致：先 `init`，再 `start`。

```kotlin
import com.lamps.sdk.LampsSdk
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.core.InitCallback

val config = LampsConfig.Builder()
    .appId("your_app_id")
    .setOaidProvider { mediaOaid } // 可选；未设置或为空不阻断 start
    .setDebug(false) // 只控制日志
    .setCustomData(mapOf("k" to "v")) // 可选，本期配置接口不携带
    .build()

LampsSdk.init(application, config)
LampsSdk.startAsync(object : InitCallback {
    override fun success() { }
    override fun fail(code: Int, message: String?) { }
})
```

已拿到 OAID 时，在 Provider 里返回即可。未设置 Provider 或返回空串时，初始化请求和监测宏里的 OAID 为空，SDK 仍可进入 Ready。

`startAsync` 成功前不要使用后续能力。可用 `LampsSdk.isSdkReady()` 查询状态。

`startAsync()` 内部会请求 `/v1/lamps/config` 并缓存结果，当前不对外暴露。
远端配置请求成功后，会根据 `rewardAdSlots` 分发初始化已配置且 **Gradle 已引入** 的穿山甲、优量汇和汇川 SDK；对应平台初始化完成后 Lamps SDK 才进入 Ready。未引入的联盟会被跳过。

## 激励视频

最低支持 Android API 24。首次广告请求时，SDK 根据 `rewardAdSlots` 的 `channelName` 选择已注册 Provider，并使用同一条配置中的 `appId`、`slotId` 加载广告。

Native 接入先 `loadReward`，在成功回调中拿到 `LampsRewardAd` 后再 `show`：

```kotlin
import com.lamps.sdk.LampsSdk
import com.lamps.sdk.reward.LampsRewardAd
import com.lamps.sdk.reward.RewardAdLoadCallback
import com.lamps.sdk.reward.RewardAdShowCallback

LampsSdk.loadReward(activity, object : RewardAdLoadCallback {
    override fun onAdLoadSuccess(ad: LampsRewardAd) {
        ad.show(activity, object : RewardAdShowCallback {
            override fun onAdShown() = Unit
            override fun onAdRewarded() = Unit
            override fun onAdClosed() = Unit
            override fun onAdShowFailed(code: Int, message: String?) = Unit
        })
    }

    override fun onAdLoadFailed(code: Int, message: String?) = Unit
})
```

`LampsRewardAd` 对外只暴露 `price`、`slotId`、`channelName`、`isValid` 和 `show()`。也可调用 `LampsSdk.showReward(activity, ad, callback)`。

如果宿主已经接入某个平台，Gradle 会与对应 module 传递的同名依赖对齐；也可以不引入该 Lamps 联盟 module，改用宿主自己的 SDK。

若应用设置了 `android:allowBackup="false"`，因穿山甲 Manifest 自带 `allowBackup="true"`，宿主 Manifest 需要在 `<application>` 添加 `tools:replace="android:allowBackup"`。

H5 使用 postMessage 协议调用：

```javascript
await bridge.invoke("hra.ad.showRewardedVideo", {})
```

SDK 会并发加载已引入 Provider 可处理的 slot，并通过 `hoorah.ad.rewardedVideoStatus` 事件回传 `onLoadSuccess`、`onShowSuccess`、`onRewardArrived`、`onClose`、`onLoadError` 或 `onShowError`。激励视频 Bridge 字段见 [RewardAdAbility](docs/bridge/RewardAdAbility.md)。

## 错误码

| code | 含义 |
| --- | --- |
| 1001 | 未先调用 `init` |
| 1002 | `appId` 为空 |
| 1003 | 已废弃（OAID 为空不再导致启动失败） |
| 1004 | Context 非法 |
| 1005 | 初始化数据请求失败（无可用缓存时） |
| 1006 | 启动进行中 |
| 1007 | 穿山甲 SDK 初始化失败 |
| 1008 | 优量汇 SDK 初始化失败 |
| 1009 | 汇川 SDK 初始化失败 |
| 1010 | 第三方 SDK 初始化分发失败 |

## 构建

```bash
./gradlew :core:assembleDebug :pangle:assembleDebug :ylh:assembleDebug :noah:assembleDebug :demo:assembleDebug
```

## 发布

对齐 HPWebview：发到虎扑 Nexus 的 `hupu-android` 仓库。版本号改 `gradle.properties` 里的 `LAMPS_VERSION`。

```bash
./gradlew :core:publish :pangle:publish :ylh:publish :noah:publish :sdk-tools:publish
```

| 模块 | 坐标 | 仓库 |
| --- | --- | --- |
| `:core` | `com.lamps:core:0.1.0` | `https://nexus.hupu.io/repository/hupu-android/` |
| `:pangle` | `com.lamps:pangle:0.1.0` | 同上 |
| `:ylh` | `com.lamps:ylh:0.1.0` | 同上 |
| `:noah` | `com.lamps:noah:0.1.0` | 同上 |
| `:sdk-tools` | `com.lamps:sdk-tools:0.1.0` | 同上 |

宿主从 `https://nexus.hupu.io/repository/hupu-android-public/` 拉取。

