# GamePageAbility

## Method

`lamps.game.open`

打开游戏页。H5 调用后由原生创建游戏 WebView 容器并加载指定 URL，通过页面跳转展示。

### 入参 `data`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `url` | string | 是 | 游戏页地址，须为完整 `http(s)` URL |

```JSON
{
  "url": "https://game.example.com/play"
}
```

## 返回值

通过 invoke 回调（`callbackSig`）返回。

成功：

```JSON
{
  "msg": "success"
}
```

失败：

```JSON
{
  "msg": "url 无效，须为完整 http(s) URL"
}
```
