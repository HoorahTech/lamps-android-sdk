# Lamps Android SDK

独立 Android SDK，当前版本提供初始化。OAID 由媒体传入，SDK 不内置 MSA 证书。

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
    .setOaidProvider { mediaOaid } // 必传；start 时读取，空则失败
    .setDebug(false) // 只控制日志
    .setCustomData(mapOf("k" to "v")) // 可选，本期配置接口不携带
    .build()

LampsSdk.init(application, config)
LampsSdk.start(object : InitCallback {
    override fun success() { }
    override fun fail(code: Int, message: String?) { }
})
```

已拿到 OAID 时，在 Provider 里返回即可。

`start` 成功前不要使用后续能力。可用 `LampsSdk.isSdkReady()` 查询状态。

`start()` 内部会请求 `/v1/lamps/config` 并缓存结果，当前不对外暴露。

## 错误码

| code | 含义 |
| --- | --- |
| 1001 | 未先调用 `init` |
| 1002 | `appId` 为空 |
| 1003 | OAID 为空 |
| 1004 | Context 非法 |
| 1005 | 初始化数据请求失败（无可用缓存时） |
| 1006 | 启动进行中 |

## 构建

```bash
./gradlew :sdk:assembleDebug :demo:assembleDebug
```
