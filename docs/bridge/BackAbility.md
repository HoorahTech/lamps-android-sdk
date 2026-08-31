# BackAbility

对齐 `comp_basic_webview` 的返回能力。H5 可关闭当前页面，或接管系统返回键。

`hupu.common.markh5back` 是 `CommonWebViewActivity`、`GameWebViewActivity` 这类容器 Activity 的能力，不写在 `LampsWebView` 上。关闭页面、标记 H5 回退、系统返回分发都集中在 `BackAbilityUtil`。

## Methods

### `hupu.ui.back`

直接关闭当前 WebView 所在 Activity。游戏操作栏的退出/关闭仍直接 `finish()`，不受 H5 接管影响。

无入参。

成功回调：

```JSON
{
  "code": 0,
  "msg": "",
  "data": {}
}
```

### `hupu.common.markh5back`

标记当前容器 Activity 由 H5 控制回退。标记后，该 Activity 的系统返回不再执行 `WebView.goBack()` 或关闭页面，改为向 H5 发送 `hupu.common.onback`。

无入参。成功回调格式同上。

标记后不可取消；Activity 回收后自动失效。

### `hupu.common.onback`

Native → H5 事件，不是 H5 可调用的方法。仅在已调用 `hupu.common.markh5back` 后，用户点击系统返回时发送。H5 自行处理页面栈，需要关闭容器时再调用 `hupu.ui.back`。

payload 为空对象 `{}`。

## 容器行为

未标记时，系统返回优先 `WebView.goBack()`，没有历史则关闭 Activity。`GameWebViewActivity`、`CommonWebViewActivity` 的 `onBackPressed` 只转发到 `BackAbilityUtil.handleBackPressed`。
