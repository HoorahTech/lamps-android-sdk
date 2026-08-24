# NetworkAbility

## Method

`hupu.common.request`

通用 HTTP 网络请求，支持 GET 和 POST。使用客户端原生网络栈发起请求，不依赖 WebView 的 JS 网络能力。

### 入参 `data`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `url` | string | 是 | 请求地址，完整 http(s) URL |
| `method` | string | 是 | 请求方法，`"get"` 或 `"post"` |
| `data` | JSONObject | 否 | 请求参数。GET 时拼接到 URL query；POST 时根据 Content-Type 编码为 form 或 JSON body |
| `header` | JSONObject | 否 | 请求头。`Content-Type` 会忽略大小写匹配；`Referer` 会被忽略（不发送） |

```JSON
{
  "url": "https://api.example.com/v1/data",
  "method": "get",
  "data": {
    "page": "1",
    "size": "20"
  },
  "header": {
    "Content-Type": "application/json",
    "Authorization": "Bearer xxx"
  }
}
```

POST JSON 示例：

```JSON
{
  "url": "https://api.example.com/v1/submit",
  "method": "post",
  "data": {
    "name": "test",
    "value": 123
  },
  "header": {
    "Content-Type": "application/json",
    "Authorization": "Bearer xxx"
  }
}
```

POST form-urlencoded 示例：

```JSON
{
  "url": "https://api.example.com/v1/login",
  "method": "post",
  "data": {
    "username": "admin",
    "password": "123456"
  },
  "header": {
    "Content-Type": "application/x-www-form-urlencoded"
  }
}
```

## 返回值

通过 invoke 回调（`callbackSig`）返回。同步返回格式如下：

```JSON
{
  "code": 0,
  "msg": "",
  "data": {
    "status": 200,
    "statusText": "OK",
    "data": "<URL-encoded response body>"
  }
}
```

### `data` 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `status` | number | HTTP 状态码 |
| `statusText` | string | HTTP 状态文本 |
| `data` | string | URL Encode 后的响应体，H5 侧需 `decodeURIComponent` 解码 |

### `code` 错误码

| code | 含义 |
|---|---|
| 0 | 成功 |
| 801 | 参数解析失败（url/method 为空或缺少 Content-Type） |
| -1001 | 请求超时 |
| -1200 | SSL 错误 |
| -1009 | 网络不可达 |
| -1000 | 域名无法解析 |
| 100 | 未知网络错误 |
