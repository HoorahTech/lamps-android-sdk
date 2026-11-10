# ThirdPartyPayAbility

第三方支付。当前只支持支付宝，对齐虎扑 `hupu.thirdPartyPay.order` 的 alipay 渠道。宿主需要依赖可选模块 `alipay`，该模块会传递官方支付宝 SDK，并直接调用 `PayTask.payV2` 拉起收银台。未依赖该模块时没有支付宝能力。

## Method

`lamps.thirdPartyPay.order`

H5 传入服务端签名后的支付宝订单串，客户端拉起支付宝 App 完成支付，结果通过本次调用的回调返回。

调用前先看 `lamps.common.bridgeReady` 的 `supportedBridges`：宿主依赖 `alipay` 模块时才包含本方法名，否则为空数组。列表里没有本方法时不要调用。

### 入参 `data`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `channel` | string | 是 | 支付渠道，当前仅支持 `alipay`（不区分大小写，两端空白会被去除） |
| `orderInfo` | string | 是 | 支付宝订单串（服务端签名后的 orderInfo），两端空白会被去除 |

```JSON
{
  "channel": "alipay",
  "orderInfo": "服务端签名后的支付宝订单串"
}
```

## 返回值

通过 invoke 回调（`callbackSig`）返回。`resultStatus` 为 `9000` 时 `msg` 为 `success`，`6001` 为 `cancel`，其它状态为 `fail`。这三种都会带完整结果体。

成功：

```JSON
{
  "msg": "success",
  "channel": "alipay",
  "resultStatus": "9000",
  "result": "支付宝原始返回的 result",
  "memo": ""
}
```

用户取消：

```JSON
{
  "msg": "cancel",
  "channel": "alipay",
  "resultStatus": "6001",
  "result": "",
  "memo": ""
}
```

支付失败：

```JSON
{
  "msg": "fail",
  "channel": "alipay",
  "resultStatus": "4000",
  "result": "",
  "memo": "支付宝原始返回的 memo"
}
```

调用前就失败（参数缺失、渠道未接入、页面不可用、拉起失败）时，只回 `msg`：

```JSON
{
  "msg": "orderInfo 为空"
}
```

### `msg` 错误说明

| msg | 含义 |
|---|---|
| `success` | 支付成功（`resultStatus` 为 `9000`） |
| `cancel` | 用户中途取消（`resultStatus` 为 `6001`） |
| `fail` | 支付失败（其它 `resultStatus`），具体原因看 `resultStatus` / `result` / `memo` |
| `缺少 channel` | `data` 未传 `channel` 或为空白 |
| `暂不支持该支付渠道: {channel}` | 没有对应渠道模块，或 `channel` 不是已注册渠道。未依赖 `alipay` 时，`alipay` 也会落在这里 |
| `orderInfo 为空` | `orderInfo` 未传或为空白 |
| `当前页面不可用` | 找不到可用 Activity，或页面已结束 |
| `支付宝支付失败` | 已接入支付宝，但 `PayTask.payV2` 抛错 |
| `支付失败` | 支付任务被拒绝，或渠道实现抛出未捕获异常 |
| `unsupported method: {method}` | `method` 与 `lamps.thirdPartyPay.order` 不匹配 |

### `resultStatus` 说明

| resultStatus | 含义 |
|---|---|
| `9000` | 支付成功 |
| `6001` | 用户中途取消 |
| 其它 | 支付失败，具体原因见支付宝原始返回的 `result` / `memo` |

支付成功与否请以本次回调的 `resultStatus` 为准。客户端结果仅作前端交互，最终以服务端支付结果为准。
