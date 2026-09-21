# BridgeReadyAbility

## Method

`lamps.common.bridgeReady`

WebView 加载完成后，H5 向 Native 请求宿主公共运行参数。Native 不校验入参，直接返回当前 WebView 对应的参数集合。

该能力由 `CommonAbilityInstaller` 注册，与 iOS 端共用同一方法名和字段集合。

## 返回值

成功：

```JSON
{
  "code": 0,
  "msg": "",
  "data": {
    "ua": "Mozilla/5.0 ...",
    "ip": "61.173.157.243",
    "mac": "02:00:00:00:00:00",
    "imei": "",
    "os": "Android",
    "platform": "Android",
    "appid": "10002",
    "sdkVersion": "1.0.0",
    "phoneBrand": "Xiaomi",
    "network": "wifi",
    "androidId": "...",
    "oaid": "",
    "appVer": "7.0.0",
    "osVer": "14",
    "env": "prd",
    "packageName": "com.example.app",
    "clientWidth": 1080,
    "clientHeight": 2340,
    "density": 3.0,
    "statusBarHeight": 90,
    "displayMode": "page",
    "night": 0
  }
}
```

### `data` 字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `ua` | string | 设备 User-Agent |
| `ip` | string | 初始化接口返回的客户端 IP；没有时为空字符串 |
| `mac` | string | 设备 MAC；不可用时为空字符串 |
| `imei` | string | 设备 IMEI；不可用时为空字符串 |
| `os` | string | 固定为 `Android` |
| `platform` | string | 固定为 `Android` |
| `appid` | string | 宿主应用 App ID |
| `sdkVersion` | string | SDK 版本号 |
| `phoneBrand` | string | 手机品牌 |
| `network` | string | 当前网络类型：`wifi`、`2g`、`3g`、`4g`、`5g` 或 `unknown` |
| `androidId` | string | Android ID；获取失败时使用并持久化 UUID |
| `oaid` | string | OAID；不可用时为空字符串 |
| `appVer` | string | 宿主应用版本号 |
| `osVer` | string | Android 系统版本号，如 `"14"` |
| `env` | string | SDK 环境：`dev`（测试）、`prd`（线上） |
| `packageName` | string | 宿主应用包名 |
| `clientWidth` | number | 屏幕像素宽度 |
| `clientHeight` | number | 屏幕像素高度 |
| `density` | number | 屏幕像素密度 |
| `statusBarHeight` | number | 状态栏高度（像素） |
| `displayMode` | string | 页面展示模式。游戏中心由 SDK 按打开方式写入 `GameCenterConfig`，见下表 |
| `night` | number | 日夜间。`1` 夜间，`0` 日间。优先 `GameCenterConfig.setNightMode`；未设则每次读 `LampsConfig.setNightModeProvider`；都没有则为 `0` |

### `displayMode` 取值

`displayMode` 表示当前 WebView 以何种形态展示。取值对应 `GameCenterConfig.DisplayMode`，由 SDK 内部写入，宿主不要通过 Builder 设置：

| 值 | 枚举 | 写入时机 | 含义 |
|---|---|---|---|
| `page` | `DisplayMode.PAGE` | `LampsSdk.navigateToGameCenter` | 整页模式 |
| `embed` | `DisplayMode.EMBED` | `LampsSdk.getGameCenterView` | 内嵌模式 |
| `""` | 无 | 其他页面 | 未指定 |

`night` 对应 `NightMode`：`DAY` 为 `0`，`NIGHT` 为 `1`。打开时优先单次 `GameCenterConfig`，否则现取全局 provider。

内嵌模式下宿主可在页面存活期间切换日夜间，变化通过 `lamps.common.onnightmodechange` 事件下发，见 [NightModeEvent](NightModeEvent.md)。本接口返回的始终是当前最新值。

## 注册方式

通过 `CommonAbilityInstaller` 注册到 WebView：

```kotlin
webView.registerAbilityInstaller(CommonAbilityInstaller())
```

`BridgeReadyAbility` 已内置在 `CommonAbilityInstaller` 中，无需单独传入。
