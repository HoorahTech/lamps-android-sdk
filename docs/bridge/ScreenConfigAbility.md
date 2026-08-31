# ScreenConfigAbility

## Method

`lamps.common.screenConfig`

配置当前 WebView 容器 Activity 的屏幕表现。只接受下表四个字段，其余字段忽略。未传字段保持当前值。

### 入参 `data`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `immersive` | boolean | 否 | 沉浸式模式。单独传 `true` 时隐藏状态栏；同时传了 `statusBarColor` 或 `statusBarFontDark` 时，在沉浸式布局上显示状态栏 |
| `statusBarColor` | string | 否 | 状态栏背景色，须为 `Color.parseColor` 支持格式，如 `"#00000000"`、`"#FFFFFF"`。在 `immersive: true` 时传入，表示要显示状态栏 |
| `statusBarFontDark` | boolean | 否 | 状态栏字体是否深色。`true`=黑字，`false`=白字。在 `immersive: true` 时传入，表示要显示状态栏 |
| `orientation` | string | 否 | 屏幕方向，取值见下表 |

`orientation` 取值：

| 值 | 含义 |
|---|---|
| `"portrait"` | 竖屏 |
| `"landscape"` | 横屏 |
| `"auto"` | 跟随传感器 |
| `"default"` | 不指定 |

前端必须按上表传字段名和取值：布尔只能是 JSON `true` / `false`，方向只能是上表四个小写字符串。非法值回调错误，不应用配置。

已废弃并忽略：`fullScreen`、`statusBarOverlay`、`statusBarVisible`、`statusBarStyle`、`statusBarFontColor`，以及 `"0"` / `"1"` / `"vertical"` / `"horizontal"` / `"sensor"` / `"unspecified"` 等旧方向别名。

### 默认值

页面打开时（H5 未调用本方法）等价于：

```JSON
{
  "immersive": true
}
```

全屏沉浸，不显示状态栏。`orientation` 保持 Manifest 竖屏 `"portrait"`。

| 字段 | 默认值 | 说明 |
|---|---|---|
| `immersive` | `true` | 默认沉浸式 |
| `statusBarColor` | 不传 | 未传表示不想显示状态栏；传入后在沉浸式上显示状态栏，缺省颜色为 `"#00000000"` |
| `statusBarFontDark` | 不传 | 未传表示不想显示状态栏；传入后在沉浸式上显示状态栏，缺省为 `true`（黑字） |
| `orientation` | `"portrait"` | 与容器 Activity Manifest 竖屏一致。未传时不改方向 |

沉浸式与状态栏的组合：

```JSON
{ "immersive": true }
```

全屏沉浸，隐藏状态栏。

```JSON
{
  "immersive": true,
  "statusBarColor": "#FFFFFF",
  "statusBarFontDark": true
}
```

内容仍沉浸铺满，但显示状态栏，并应用颜色和字体。只传其中一个状态栏字段也视为要显示状态栏，另一个用缺省值。

```JSON
{
  "immersive": false,
  "statusBarColor": "#FFFFFF",
  "statusBarFontDark": true,
  "orientation": "portrait"
}
```

非沉浸式，系统栏占位显示。

## 返回值

通过 invoke 回调（`callbackSig`）返回。

成功：

```JSON
{
  "code": 0,
  "msg": "",
  "data": {}
}
```

失败：

```JSON
{
  "code": 801,
  "msg": "orientation is invalid",
  "data": {}
}
```

| msg | 含义 |
|---|---|
| `activity not found` | 找不到宿主 Activity |
| `immersive is invalid` | `immersive` 不是 boolean |
| `statusBarFontDark is invalid` | `statusBarFontDark` 不是 boolean |
| `statusBarColor is invalid` | `statusBarColor` 无法被 `Color.parseColor` 解析 |
| `orientation is invalid` | `orientation` 不是 `portrait` / `landscape` / `auto` / `default` |
