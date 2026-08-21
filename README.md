# Lamps Android SDK

独立 Android SDK，提供初始化、WebView Bridge 和可插拔激励视频能力。OAID 由媒体传入，SDK 不内置 MSA 证书。

## 工程

- `sdk/`：对外 Library
- `demo/`：接入示例

SDK 源码目录：

- `config/`：本地初始化配置（`LampsConfig`）
- `data/init/`：内部初始化数据（不对外）
- `utils/`：日志、设备信息、HttpUtils
- `core/`：初始化运行时
- `LampsSdk`：对外门面

## 接入

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
远端配置请求成功后，会根据 `rewardAdSlots` 分发初始化已配置且依赖存在的穿山甲、优量汇和汇川 SDK；对应平台初始化完成后 Lamps SDK 才进入 Ready。

## 激励视频

`sdk` 内置穿山甲、优量汇和汇川三个激励视频 Provider，最低支持 Android API 24。首次广告请求时，SDK 根据 `rewardAdSlots` 的 `channelName/channelId` 选择 Provider，并使用同一条配置中的 `appId`、`slotId` 懒初始化和加载广告。

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

```kotlin
dependencies {
    implementation("com.lamps:sdk:0.1.0")
}
```

如果宿主已经接入某个平台，可以排除 Lamps SDK 传递的同名依赖，Gradle 将使用宿主已有版本：

```kotlin
implementation("com.lamps:sdk:0.1.0") {
    exclude(group = "com.pangle.cn", module = "ads-sdk-pro")
    exclude(group = "com.qq.e.union", module = "union")
    exclude(group = "com.noah", module = "noah")
}
```

SDK 通过类存在性检查懒加载各 Provider；被排除且宿主未提供的广告平台会被跳过。若应用设置了 `android:allowBackup="false"`，因穿山甲 Manifest 自带 `allowBackup="true"`，宿主 Manifest 需要在 `<application>` 添加 `tools:replace="android:allowBackup"`。

H5 使用 postMessage 协议调用：

```javascript
await bridge.invoke("hra.ad.showRewardedVideo", {})
```

SDK 会从服务端下发顺序中选择第一个已安装 Provider 可处理的 slot，并通过 `hoorah.ad.rewardedVideoStatus` 事件回传 `onLoadSuccess`、`onShowSuccess`、`onRewardArrived`、`onClose`、`onLoadError` 或 `onShowError`。激励视频 Bridge 字段见 [RewardAdAbility](docs/bridge/RewardAdAbility.md)。

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
./gradlew :sdk:assembleDebug :demo:assembleDebug
```

## 发布

对齐 HPWebview：发到虎扑 Nexus 的 `hupu-android` 仓库。版本号改 `gradle.properties` 里的 `LAMPS_VERSION`。

```bash
./gradlew :sdk:publish :sdk-tools:publish
```

| 模块 | 坐标 | 仓库 |
| --- | --- | --- |
| `:sdk` | `com.lamps:sdk:0.1.0` | `https://nexus.hupu.io/repository/hupu-android/` |
| `:sdk-tools` | `com.lamps:sdk-tools:0.1.0` | 同上 |

宿主从 `https://nexus.hupu.io/repository/hupu-android-public/` 拉取：

```kotlin
implementation("com.lamps:sdk:0.1.0")
debugImplementation("com.lamps:sdk-tools:0.1.0")
```
