# RigAbility

## Method

`hupu.common.bridgeRigPerf`

RIG 性能/事件上报，用于 WebView 端向 Native 上报性能数据、事件埋点等信息。数据通过 `RigSdk` 以 HTTP GET 请求上报到硬编码的服务端地址。

### 入参 `data`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `type` | string | 是 | 事件类型，用于标识上报事件的分类，如 `"page_load"`、`"api_cost"` |
| 其他自定义字段 | any | 否 | 除 `type` 外的所有字段会作为上报参数传递给服务端 |

```JSON
{
  "type": "page_load",
  "url": "https://example.com/page",
  "cost": "1200",
  "status": "success"
}
```

### 上报参数

`RigSdk.sendData` 在服务端发起 HTTP GET 请求，请求 URL 为 `https://rig.hupu.com/report`，拼接以下参数：

| 参数 | 类型 | 说明 |
|---|---|---|
| `type` | string | 事件类型，从入参 `type` 获取 |
| `cid` | string | 初始化时传入的 CID |
| `ts` | string | 当前时间戳（秒） |
| 自定义字段 | string | 入参中除 `type` 外的所有字段，值转为字符串 |

## 返回值

通过 invoke 回调（`callbackSig`）返回。同步返回格式如下：

```JSON
{
  "code": 0,
  "msg": "",
  "data": {}
}
```

## 前提条件

使用前需先初始化 `RigSdk`：

```kotlin
val config = RigSdkConfig.Builder()
    .setDebug(true)
    .setCid("your_cid")
    .build()
RigSdk.init(context, config)
```

## 注册方式

通过 `CommonAbilityInstaller` 注册到 WebView：

```kotlin
webView.registerAbilityInstaller(CommonAbilityInstaller(RigAbility()))
```

或通过构造器传入：

```kotlin
val installer = CommonAbilityInstaller(RigAbility())
webView.registerAbilityInstaller(installer)
```
