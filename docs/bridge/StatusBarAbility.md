# StatusBarAbility

## Method

`lamps.common.statusBar`

配置当前 WebView 容器的状态栏。窗口始终沉浸（`fitsSystemWindows=false`，状态栏透明），用父容器背景色做出状态栏颜色。不再做横竖屏切换。

`showStatusBar` 只控制系统状态栏显隐；`statusBarImmersive` 只控制 WebView 是否铺到状态栏下面。

### 入参 `data`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `showStatusBar` | boolean | 否 | `true` 显示系统状态栏，`false` 隐藏。默认 `true` |
| `statusBarImmersive` | boolean | 否 | `true` 沉浸：WebView 铺满父容器，内容在状态栏下方。`false` 非沉浸：WebView `topMargin = statusBarHeight`，父容器不做顶部偏移。默认 `true` |
| `backgroundColor` | string | 否 | 状态栏背景色，须为 `Color.parseColor` 支持格式，如 `"#00000000"`、`"#FFFFFF"`。写在 WebView 父容器上。默认透明 |
| `statusBarFontStyle` | number | 否 | 状态栏图标/字体。`0` 浅色（白字），`1` 深色（黑字）。默认 `1` |

未传字段使用默认值，不沿用上一次配置。非法值回调 801，不应用配置。

### 默认值

```JSON
{
  "showStatusBar": true,
  "statusBarImmersive": true,
  "backgroundColor": "#00000000",
  "statusBarFontStyle": 1
}
```

### 布局组合

| showStatusBar | statusBarImmersive | 系统状态栏 | WebView |
|---|---|---|---|
| `true` | `true` | 显示 | 铺满，内容在状态栏下 |
| `true` | `false` | 显示 | 顶部留出状态栏高度，露出父容器底色 |
| `false` | `true` | 隐藏 | 铺满 |
| `false` | `false` | 隐藏 | 顶部留出状态栏高度 |

```JSON
{
  "showStatusBar": true,
  "statusBarImmersive": false,
  "backgroundColor": "#FFFFFF",
  "statusBarFontStyle": 1
}
```

## 返回值

成功：

```JSON
{
  "code": 0,
  "msg": "",
  "data": {}
}
```

失败 `code` 为 `801`：

| msg | 含义 |
|---|---|
| `activity not found` | 找不到宿主 Activity |
| `showStatusBar is invalid` | 不是 boolean |
| `statusBarImmersive is invalid` | 不是 boolean |
| `backgroundColor is invalid` | 无法被 `Color.parseColor` 解析 |
| `statusBarFontStyle is invalid` | 不是数字 `0` 或 `1` |
