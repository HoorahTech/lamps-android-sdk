# NightModeEvent

日夜间的运行时同步事件。SDK 内嵌到宿主页面时，宿主自身可能在页面存活期间切换日夜间，H5 需要跟随变化。

初始值仍由 `lamps.common.bridgeReady` 的 `night` 字段下发，本事件只负责“打开之后的变化”。

## Method

### `lamps.common.onnightmodechange`

Native → H5 事件，不是 H5 可调用的方法。

宿主调用 `GameCenterView.updateConfig(GameCenterConfig)` 且解析后的日夜间与当前值不同时发送。与当前值相同时不发送。

payload：

```JSON
{
  "night": 1
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `night` | number | `1` 夜间，`0` 日间。取值含义与 `lamps.common.bridgeReady` 的 `night` 一致 |

## H5 侧处理要求

- 收到事件后就地切换主题，不要重新加载页面。
- 事件可能在页面存活期间多次到达。
- 事件发送后，`night` 也会被写回 WebView，之后再次调用 `lamps.common.bridgeReady` 拿到的是最新值。
- 页面尚未加载完成时宿主就切换的场景，Native 只更新存储值，H5 在 `bridgeReady` 中直接拿到新值，不会收到本事件。

## Native 侧实现

- 存值与派发都在 `LampsWebView.updateNight(Boolean)`：同值去重，非主线程调用时 `post` 回主线程，再通过 `LampsWebView.send` 走统一的 Native→H5 通道。
- 方法名常量定义在 `NightModeEvent.SEND_NIGHT_MODE_CHANGE`。
- 对外入口是 `GameCenterView.updateConfig`，仅内嵌模式（`DisplayMode.EMBED`）可用；整页模式的日夜间在 `navigateToGameCenter` 打开时确定。
